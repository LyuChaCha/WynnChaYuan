package com.wynnchayuan.client;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.listener.MarketListener;
import com.wynnchayuan.render.Colors;
import com.wynnchayuan.translate.ErrorDebug;
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

public final class MarketPicker {

    private static final int MAX_ROWS = 8;
    private static final int ROW_H = 11;
    private static final int PAD = 4;
    private static final int COLUMN_GAP = 10;
    private static final int ENGLISH = 0xFFAAAAAA;

    private MarketPicker() {}

    // Fabric 重新 init 時會清掉畫面上的事件，每次都要重接
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

    private static final class State {

        private final ChatScreen chat;
        private String lastValue;
        private List<MarketSearch.Suggestion> all = List.of();
        private List<MarketSearch.Suggestion> rows = List.of();
        private int selected;

        State(ChatScreen chat) {
            this.chat = chat;
        }

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
                ErrorDebug.note("MarketPicker#State", null, t);
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
                ErrorDebug.note("MarketPicker#State", null, t);
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
            int accent = WynnChaYuan.config().accentARGB();
            int picked = (accent & 0x00FFFFFF) | 0x40000000;
            g.nextStratum();
            g.fill(at.left, at.top, at.left + at.width, at.bottom,
                    WynnChaYuan.config().backgroundARGB());
            g.renderOutline(at.left, at.top, at.width, at.bottom - at.top, accent);

            int hover = rowAt(mouseX, mouseY, at);
            for (int i = 0; i < rows.size(); i++) {
                MarketSearch.Suggestion row = rows.get(i);
                int y = at.top + PAD + i * ROW_H;
                if (i == selected || i == hover) {
                    g.fill(at.left + 1, y - 1, at.left + at.width - 1, y + ROW_H - 1, picked);
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
            refresh();
        }

        private record Layout(int left, int top, int bottom, int width, int zhWidth) {}

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
