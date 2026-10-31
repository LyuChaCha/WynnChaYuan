package com.wynnchayuan.client;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.render.Colors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class Cards {

    private static final int CARD_BG = 0xC0121A24;

    private Cards() {}

    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        int edge = (WynnChaYuan.config().themeARGB() & 0x00FFFFFF) | 0x50000000;

        g.fill(x + 1, y, x + w - 1, y + h, CARD_BG);
        g.fill(x, y + 1, x + 1, y + h - 1, CARD_BG);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, CARD_BG);

        g.fill(x + 1, y, x + w - 1, y + 1, edge);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, edge);
        g.fill(x, y + 1, x + 1, y + h - 1, edge);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, edge);
    }

    public static void title(GuiGraphics g, Font font, int x, int y, String text) {
        int accent = WynnChaYuan.config().themeARGB();
        g.drawString(font, Component.literal(text), x, y, accent);
        int w = font.width(text);
        g.fill(x, y + 10, x + w, y + 11, accent);
    }

    public static String fit(Font font, String text, int maxW) {
        if (maxW <= 0 || font.width(text) <= maxW) {
            return text;
        }
        String cut = font.plainSubstrByWidth(text, Math.max(0, maxW - font.width("…")));
        return cut + "…";
    }

    public static void hint(GuiGraphics g, Font font, int x, int y, String text) {
        g.drawString(font, Component.literal(text), x, y, Colors.HINT);
    }

    private static final Identifier ICON = Identifier.fromNamespaceAndPath(
                    "wynnchayuan", "textures/gui/icon.png");

    private static final int ICON_SIZE = 22;

    public static void header(GuiGraphics g, Font font, int screenW, String title, String subtitle) {
        int accent = WynnChaYuan.config().themeARGB();
        g.fill(0, 0, screenW, 46, 0xD00B1119);
        g.fill(0, 46, screenW, 47, accent);

        int gap = 6;
        int total = ICON_SIZE + gap + font.width(title);
        int x = (screenW - total) / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, ICON,
                x, 3, 0f, 0f, ICON_SIZE, ICON_SIZE, 256, 256, 256, 256);
        g.drawString(font, Component.literal(title),
                x + ICON_SIZE + gap, 10, accent);
        g.drawCenteredString(font, Component.literal(subtitle), screenW / 2, 28, Colors.HINT);
    }
}
