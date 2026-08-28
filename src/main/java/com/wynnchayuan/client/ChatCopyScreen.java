package com.wynnchayuan.client;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.ChatLog;
import com.wynnchayuan.render.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 「複製聊天」的挑選畫面。
 *
 * <h2>為什麼不是滑鼠指哪就複製哪</h2>
 * 那需要問 Minecraft「游標下面是第幾行聊天」。1.21.11 的 {@code ChatComponent}
 * 沒有公開這件事，資料是私有的，得靠 mixin 挖，還要自己重算它的排版幾何——
 * 而這個模組到目前為止一個 mixin 都沒有。
 *
 * <p>改成列出最近的訊息讓玩家挑，反而多了一個好處：<b>捲得回去</b>。
 * 聊天視窗刷過去的訊息，在這裡還找得到。
 *
 * <h2>複製哪一份</h2>
 * 照「聊天訊息」那個設定走：只顯示原文就複製原文，就地取代就複製譯文，
 * 兩份都顯示就兩份都複製。畫面上看到什麼，複製起來就是什麼。
 */
public final class ChatCopyScreen extends Screen {

    private static final int ROW_H = 22;
    private static final int PAD = 8;
    private static final int PANEL_BG = 0xD0101018;
    private static final int PANEL_BORDER = 0xFF3A1E5C;
    private static final int ROW_HOVER = 0x40FFD24A;
    private static final int COPIED_MS = 1200;

    private List<ChatLog.Entry> rows = List.of();
    private int scroll;
    private int copiedRow = -1;
    private long copiedAt;

    public ChatCopyScreen() {
        super(Component.literal("複製聊天"));
    }

    @Override
    protected void init() {
        rows = ChatLog.recent();
        scroll = 0;
        addRenderableWidget(Button.builder(Component.literal("關閉"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20).build());
    }

    /** 一頁放得下幾列。 */
    private int perPage() {
        return Math.max(1, (this.height - 96) / ROW_H);
    }

    private int panelLeft() {
        return Math.max(12, this.width / 2 - 320);
    }

    private int panelWidth() {
        return Math.min(640, this.width - 24);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        g.drawCenteredString(this.font, this.title, this.width / 2, 16, Colors.TEXT);

        if (rows.isEmpty()) {
            g.drawCenteredString(this.font,
                    Component.literal(WynnChaYuan.config().chatCopy()
                            ? "還沒有收到聊天訊息"
                            : "「複製聊天」目前是關閉的，到 F6 設定裡打開")
                            .withStyle(ChatFormatting.GRAY),
                    this.width / 2, this.height / 2 - 4, Colors.SUBTLE);
            return;
        }

        int left = panelLeft();
        int width = panelWidth();
        int top = 34;
        int shown = Math.min(perPage(), rows.size() - scroll);
        int height = shown * ROW_H + PAD * 2;

        g.fill(left, top, left + width, top + height, PANEL_BG);
        g.renderOutline(left, top, width, height, PANEL_BORDER);

        for (int i = 0; i < shown; i++) {
            ChatLog.Entry entry = rows.get(scroll + i);
            int y = top + PAD + i * ROW_H;
            boolean over = mouseX >= left && mouseX <= left + width
                    && mouseY >= y - 3 && mouseY < y + ROW_H - 3;
            if (over) {
                g.fill(left + 1, y - 3, left + width - 1, y + ROW_H - 3, ROW_HOVER);
            }
            // 原文一行、譯文一行。譯文用暗一階的顏色，一眼分得出哪個是哪個。
            g.drawString(this.font, trim(entry.original(), width - PAD * 2),
                         left + PAD, y, Colors.TEXT, false);
            String zh = entry.translated();
            g.drawString(this.font,
                    zh == null || zh.isBlank() ? "—" : trim(zh, width - PAD * 2),
                    left + PAD, y + 10,
                    zh == null || zh.isBlank() ? Colors.FAINT : Colors.HIGHLIGHT, false);
        }

        if (copiedRow >= 0 && System.currentTimeMillis() - copiedAt < COPIED_MS) {
            g.drawCenteredString(this.font,
                    Component.literal("已複製").withStyle(ChatFormatting.GREEN),
                    this.width / 2, this.height - 44, Colors.TEXT);
        }
        g.drawCenteredString(this.font,
                Component.literal("點一列複製　·　滾輪捲動　·　複製的內容跟著「聊天訊息」設定走")
                        .withStyle(ChatFormatting.DARK_GRAY),
                this.width / 2, this.height - 58, Colors.FAINT);
    }

    /** 太長的截掉，畫面外的字沒有意義。 */
    private String trim(String text, int room) {
        String one = text.replace('\n', ' ');
        if (this.font.width(one) <= room) {
            return one;
        }
        return this.font.plainSubstrByWidth(one, room - this.font.width("…")) + "…";
    }

    // 1.21.11 把滑鼠事件包成 MouseButtonEvent，不再是三個散的參數。
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,
                                boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int left = panelLeft();
        int width = panelWidth();
        int top = 34;
        int shown = Math.min(perPage(), Math.max(0, rows.size() - scroll));
        for (int i = 0; i < shown; i++) {
            int y = top + PAD + i * ROW_H;
            if (mouseX >= left && mouseX <= left + width
                    && mouseY >= y - 3 && mouseY < y + ROW_H - 3) {
                copy(scroll + i);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void copy(int index) {
        if (index < 0 || index >= rows.size() || this.minecraft == null) {
            return;
        }
        CollectorConfig.ChatMode mode = WynnChaYuan.config().chatMode();
        String text = rows.get(index).forMode(
                mode == CollectorConfig.ChatMode.REPLACE,
                mode == CollectorConfig.ChatMode.BOTH);
        this.minecraft.keyboardHandler.setClipboard(text);
        copiedRow = index;
        copiedAt = System.currentTimeMillis();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        int max = Math.max(0, rows.size() - perPage());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(dy)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
