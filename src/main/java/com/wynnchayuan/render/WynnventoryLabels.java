package com.wynnchayuan.render;

import com.wynnchayuan.translate.WynnventoryBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.util.Set;

/**
 * Wynnventory 獎勵畫面上的一塊標籤：有譯文的由這裡畫。
 *
 * <p>每一幀問一次要不要翻，所以按住 Shift 馬上就是原文，不必等它重排畫面。
 * 畫法照抄它的 {@code TextWidget#renderContents}：移到元件的位置、縮放、畫字。
 */
public final class WynnventoryLabels {

    private WynnventoryLabels() {}

    /**
     * 置中的標籤。它擺元件的時候是照<b>英文</b>的寬度算的置中，譯文寬度不一樣就會偏一邊，
     * 這幾個照寬度差的一半挪回來。其餘的標籤（分區標題）是靠左的，不用動。
     */
    private static final Set<String> CENTRED = Set.of("Filters");

    /** 畫了就回 {@code true}，那個元件原本的畫法就不必跑。 */
    public static boolean draw(GuiGraphics graphics, AbstractWidget widget, Component text,
                               int colour, float scale) {
        try {
            Component shown = WynnventoryBridge.label(text);
            if (shown == null) {
                return false;
            }
            Font font = Minecraft.getInstance().font;
            float dx = CENTRED.contains(text.getString().strip())
                    ? (font.width(text) - font.width(shown)) * scale / 2f : 0f;
            graphics.pose().pushMatrix();
            graphics.pose().translate(widget.getX() + dx, widget.getY());
            graphics.pose().scale(scale, scale);
            graphics.drawString(font, shown, 0, 0, colour);
            graphics.pose().popMatrix();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
