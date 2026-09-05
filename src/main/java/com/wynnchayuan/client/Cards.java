package com.wynnchayuan.client;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.render.Colors;
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
        g.drawString(font, Component.literal(text), x, y, Colors.HINT);
    }

    /**
     * 模組圖示。跟模組清單上那顆是同一張圖，只是放到 {@code textures/} 底下
     * 讓貼圖管理員找得到——那邊是 Fabric 自己讀檔，不走貼圖那條路。
     */
    private static final net.minecraft.resources.Identifier ICON =
            net.minecraft.resources.Identifier.fromNamespaceAndPath(
                    "wynnchayuan", "textures/gui/icon.png");

    /** 圖示畫多大。原圖是 256×256，縮到這個尺寸。 */
    private static final int ICON_SIZE = 22;

    /**
     * 畫面頂端的標題列。
     *
     * <p>圖示與標題當成<b>一組</b>置中，不是各自置中——分開算的話標題一長
     * 兩個就會疊在一起。
     */
    public static void header(GuiGraphics g, Font font, int screenW, String title, String subtitle) {
        int accent = WynnChaYuan.config().accentARGB();
        g.fill(0, 0, screenW, 46, 0xD00B1119);
        g.fill(0, 46, screenW, 47, accent);

        int gap = 6;
        int total = ICON_SIZE + gap + font.width(title);
        int x = (screenW - total) / 2;
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, ICON,
                x, 3, 0f, 0f, ICON_SIZE, ICON_SIZE, 256, 256, 256, 256);
        g.drawString(font, Component.literal(title),
                x + ICON_SIZE + gap, 10, accent);
        g.drawCenteredString(font, Component.literal(subtitle), screenW / 2, 28, Colors.HINT);
    }
}
