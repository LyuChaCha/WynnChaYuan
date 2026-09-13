package com.wynnchayuan.client;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.listener.MarketListener;
import com.wynnchayuan.render.Colors;
import com.wynnchayuan.translate.MarketSearch;
import com.wynnchayuan.translate.TranslationStore;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;

import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 市集搜尋打字時，在聊天框上方列出候選讓玩家自己點。
 *
 * <h2>要解決什麼</h2>
 * 一個中文名對到好幾個物品時（「石頭」），{@link MarketListener} 不敢猜，
 * 只能原樣送出、在聊天裡印一串英文名叫玩家重打。可是那一次搜尋已經送出去了，
 * 市集拿中文去搜只會是空的，玩家得重新開一次搜尋。
 *
 * <p>改成<b>送出之前</b>就讓玩家挑：打字的同時列出候選，點一下（或 Tab）
 * 把英文名填進輸入框，再按 Enter 送出。送出去的已經是英文，
 * {@link MarketListener#rewrite} 看到拉丁字母就不會再動它。
 *
 * <h2>什麼時候出現</h2>
 * 只在市集正在等搜尋字（{@link MarketListener#active()}）而且設定有開的時候。
 * 一般聊天打中文不會跳任何東西。指令、英文、空字串也不列。
 *
 * <h2>為什麼不攔 Enter</h2>
 * 單一對到的情況 {@link MarketListener} 本來就會自己換；攔下 Enter 的話，
 * 玩家打完名字直接送出這個最常見的動作反而要多按一次。
 */
public final class MarketPicker {

    /** 最多列幾個。再多就請玩家多打幾個字。 */
    private static final int MAX_ROWS = 8;
    private static final int ROW_H = 11;
    private static final int PAD = 4;
    private static final int COLUMN_GAP = 10;
    private static final int PANEL_BG = 0xE0101018;
    private static final int PANEL_BORDER = 0xFF3A1E5C;
    private static final int ROW_SELECTED = 0x40FFD24A;
    private static final int ENGLISH = 0xFFAAAAAA;

    private MarketPicker() {}

    /**
     * 掛上聊天畫面。每次聊天畫面初始化都會重新接一次，
     * Fabric 會在畫面重新 init 時清掉上一輪註冊在這個畫面上的事件。
     */
    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof ChatScreen chat) {
                attach(chat);
            }
        });
    }

    private static void attach(ChatScreen chat) {
        State state = new State(chat);
        ScreenEvents.afterRender(chat).register(
                (screen, g, mouseX, mouseY, partial) -> state.render(g, mouseX, mouseY));
        ScreenMouseEvents.allowMouseClick(chat).register(
                (screen, event) -> !state.click(event.x(), event.y()));
        ScreenKeyboardEvents.allowKeyPress(chat).register(
                (screen, event) -> !state.key(event.key()));
    }

    /** 一個聊天畫面上的候選清單。 */
    private static final class State {

        private final ChatScreen chat;
        private String lastValue;
        private List<MarketSearch.Suggestion> all = List.of();
        private List<MarketSearch.Suggestion> rows = List.of();
        private int selected;

        State(ChatScreen chat) {
            this.chat = chat;
        }

        /** 輸入框的字變了才重查；每一幀都查會浪費。 */
        private void refresh() {
            EditBox input = chat.input;
            if (input == null || !enabled()) {
                clear();
                return;
            }
            String value = input.getValue();
            if (value.equals(lastValue)) {
                return;
            }
            lastValue = value;
            selected = 0;
            if (value.isBlank() || value.startsWith("/") || latin(value)) {
                all = List.of();
                rows = List.of();
                return;
            }
            try {
                all = WynnChaYuan.translations().market().suggestions(value);
            } catch (Throwable t) {
                // 語料重新載入到一半之類的狀況：這一輪不列，不能讓聊天畫面壞掉
                all = List.of();
            }
            rows = all.subList(0, Math.min(MAX_ROWS, all.size()));
        }

        private void clear() {
            lastValue = null;
            all = List.of();
            rows = List.of();
        }

        private static boolean enabled() {
            try {
                TranslationStore store = WynnChaYuan.translations();
                return store != null && WynnChaYuan.config().marketSearch()
                        && MarketListener.active();
            } catch (Throwable t) {
                return false;
            }
        }

        void render(GuiGraphics g, int mouseX, int mouseY) {
            refresh();
            if (rows.isEmpty()) {
                return;
            }
            Font font = chat.getFont();
            Layout at = layout(font);
            g.nextStratum();
            g.fill(at.left, at.top, at.left + at.width, at.bottom, PANEL_BG);
            g.renderOutline(at.left, at.top, at.width, at.bottom - at.top, PANEL_BORDER);

            int hover = rowAt(mouseX, mouseY, at);
            for (int i = 0; i < rows.size(); i++) {
                MarketSearch.Suggestion row = rows.get(i);
                int y = at.top + PAD + i * ROW_H;
                if (i == selected || i == hover) {
                    g.fill(at.left + 1, y - 1, at.left + at.width - 1, y + ROW_H - 1, ROW_SELECTED);
                }
                g.drawString(font, row.chinese(), at.left + PAD, y, Colors.TEXT, false);
                g.drawString(font, row.english(), at.left + PAD + at.zhWidth + COLUMN_GAP, y,
                        ENGLISH, false);
            }
            int hintY = at.top + PAD + rows.size() * ROW_H + 1;
            Component hint = all.size() > rows.size()
                    ? T.c("market.pick.more", all.size() - rows.size())
                    : T.c("market.pick.hint");
            g.drawString(font, hint.copy().withStyle(ChatFormatting.DARK_GRAY),
                    at.left + PAD, hintY, Colors.FAINT, false);
        }

        /** 點到候選就填進去，回傳 {@code true} 讓聊天畫面不再處理這一下。 */
        boolean click(double mouseX, double mouseY) {
            refresh();
            if (rows.isEmpty()) {
                return false;
            }
            int index = rowAt(mouseX, mouseY, layout(chat.getFont()));
            if (index < 0) {
                return false;
            }
            fill(index);
            return true;
        }

        /**
         * 上下選、Tab 填入。清單沒出現時一個鍵都不攔——
         * 上下鍵平常是翻聊天紀錄，不能被吃掉。
         */
        boolean key(int key) {
            refresh();
            if (rows.isEmpty()) {
                return false;
            }
            switch (key) {
                case GLFW.GLFW_KEY_UP -> selected = (selected - 1 + rows.size()) % rows.size();
                case GLFW.GLFW_KEY_DOWN -> selected = (selected + 1) % rows.size();
                case GLFW.GLFW_KEY_TAB -> fill(selected);
                default -> {
                    return false;
                }
            }
            return true;
        }

        private void fill(int index) {
            if (index < 0 || index >= rows.size()) {
                return;
            }
            EditBox input = chat.input;
            input.setValue(rows.get(index).english());
            input.moveCursorToEnd(false);
            refresh();                          // 填進去的是英文，清單自己收起來
        }

        private record Layout(int left, int top, int bottom, int width, int zhWidth) {}

        /** 貼著輸入框上緣往上長，寬度跟著內容走。 */
        private Layout layout(Font font) {
            EditBox input = chat.input;
            int zhWidth = 0;
            int enWidth = 0;
            for (MarketSearch.Suggestion row : rows) {
                zhWidth = Math.max(zhWidth, font.width(row.chinese()));
                enWidth = Math.max(enWidth, font.width(row.english()));
            }
            int hintWidth = font.width(all.size() > rows.size()
                    ? T.c("market.pick.more", all.size() - rows.size())
                    : T.c("market.pick.hint"));
            int width = Math.max(zhWidth + COLUMN_GAP + enWidth, hintWidth) + PAD * 2;
            int left = input.getX() - 2;
            width = Math.min(width, chat.width - left - 2);
            int bottom = input.getY() - 3;
            int top = bottom - (rows.size() * ROW_H + ROW_H + PAD * 2);
            return new Layout(left, top, bottom, width, zhWidth);
        }

        private int rowAt(double mouseX, double mouseY, Layout at) {
            if (mouseX < at.left || mouseX > at.left + at.width) {
                return -1;
            }
            for (int i = 0; i < rows.size(); i++) {
                int y = at.top + PAD + i * ROW_H;
                if (mouseY >= y - 1 && mouseY < y + ROW_H - 1) {
                    return i;
                }
            }
            return -1;
        }

        private static boolean latin(String text) {
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) > 0x7F) {
                    return false;
                }
            }
            return true;
        }
    }
}
