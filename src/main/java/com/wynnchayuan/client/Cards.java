package com.wynnchayuan.client;

import com.wynnchayuan.WynnChaYuan;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * 設定畫面的卡片樣式。
 *
 * <p>深色底 + 主題色細邊，分區用卡片而不是一長串按鈕——
 * 純按鈕堆疊看不出哪些設定是一組的。
 *
 * <p>Minecraft 的 {@code fill} 只能畫矩形，做不出圓角，
 * 所以改用「四角各缺一格」來暗示圓角，遠看夠像。
 */
public final class Cards {

    /** 卡片底色：比畫面暗景再深一點，讓卡片浮起來。 */
    private static final int CARD_BG = 0xC0121A24;

    private Cards() {}

    /** 一張卡片：深色底、主題色細邊、四角內縮。 */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        int edge = (WynnChaYuan.config().accentARGB() & 0x00FFFFFF) | 0x50000000;

        g.fill(x + 1, y, x + w - 1, y + h, CARD_BG);
        g.fill(x, y + 1, x + 1, y + h - 1, CARD_BG);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, CARD_BG);

        g.fill(x + 1, y, x + w - 1, y + 1, edge);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, edge);
        g.fill(x, y + 1, x + 1, y + h - 1, edge);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, edge);
    }

    /** 卡片標題：主題色文字加一條短底線。 */
    public static void title(GuiGraphics g, Font font, int x, int y, String text) {
        int accent = WynnChaYuan.config().accentARGB();
        g.drawString(font, Component.literal(text), x, y, accent);
        int w = font.width(text);
        g.fill(x, y + 10, x + w, y + 11, accent);
    }

    /** 按鈕下方的說明文字。 */
    public static void hint(GuiGraphics g, Font font, int x, int y, String text) {
        g.drawString(font, Component.literal(text), x, y, 0x7A8794);
    }

    /** 畫面頂端的標題列。 */
    public static void header(GuiGraphics g, Font font, int screenW, String title, String subtitle) {
        int accent = WynnChaYuan.config().accentARGB();
        g.fill(0, 0, screenW, 40, 0xD00B1119);
        g.fill(0, 40, screenW, 41, accent);
        g.drawCenteredString(font, Component.literal(title), screenW / 2, 12, accent);
        g.drawCenteredString(font, Component.literal(subtitle), screenW / 2, 25, 0x7A8794);
    }
}
