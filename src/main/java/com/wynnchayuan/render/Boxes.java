package com.wynnchayuan.render;

import com.wynnchayuan.WynnChaYuan;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 所有小框共用的外觀。
 *
 * <p>集中在一處，改主題色時三個小框（對話、追蹤、名牌）會一起跟著變，
 * 不必記得三個地方都要改。
 */
public final class Boxes {

    private Boxes() {}

    public static void draw(GuiGraphics g, int x, int y, int w, int h) {
        int bg = WynnChaYuan.config().backgroundARGB();
        int border = WynnChaYuan.config().accentARGB();
        g.fill(x, y, x + w, y + h, bg);
        g.fill(x, y, x + w, y + 1, border);
        g.fill(x, y + h - 1, x + w, y + h, border);
        g.fill(x, y, x + 1, y + h, border);
        g.fill(x + w - 1, y, x + w, y + h, border);
    }
}
