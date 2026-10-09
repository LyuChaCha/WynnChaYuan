package com.wynnchayuan.render;

import com.wynnchayuan.translate.WynnventoryBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * Wynnventory 獎勵畫面上的一塊標籤：有譯文的由這裡畫。
 *
 * <p>每一幀問一次要不要翻，所以按住 Shift 馬上就是原文，不必等它重排畫面。
 * 畫法照抄它的 {@code TextWidget#renderContents}：移到元件的位置、縮放、畫字。
 *
 * <h2>為什麼用反射讀它的欄位</h2>
 * 那個元件的字、顏色、縮放是它自己的私有欄位。用 mixin 的 {@code @Shadow} 去接的話，
 * 它哪天把欄位改名，套用 mixin 的那一刻就整個炸掉——遊戲開不起來。反射讀不到只是
 * 這一塊不翻（{@link #broken}），其餘照常。一塊標籤一幀讀三個欄位，量不出成本。
 */
public final class WynnventoryLabels {

    private WynnventoryLabels() {}

    /**
     * 置中的標籤。它擺元件的時候是照<b>英文</b>的寬度算的置中，譯文寬度不一樣就會偏一邊，
     * 這幾個照寬度差的一半挪回來。其餘的標籤（分區標題）是靠左的，不用動。
     */
    private static final Set<String> CENTRED = Set.of("Filters");

    private static Field textField;
    private static Field colourField;
    private static Field scaleField;
    private static boolean broken;

    /**
     * @param widget 它的 {@code TextWidget}
     * @return 畫了就回 {@code true}，那個元件原本的畫法就不必跑
     */
    public static boolean draw(GuiGraphics graphics, Object widget) {
        if (broken || !WynnventoryBridge.active() || !(widget instanceof AbstractWidget placed)) {
            return false;
        }
        Component text;
        int colour;
        float scale;
        try {
            if (textField == null) {
                Class<?> type = widget.getClass();
                Field t = type.getDeclaredField("text");
                Field c = type.getDeclaredField("color");
                Field s = type.getDeclaredField("scale");
                t.setAccessible(true);
                c.setAccessible(true);
                s.setAccessible(true);
                colourField = c;
                scaleField = s;
                textField = t;
            }
            text = (Component) textField.get(widget);
            colour = colourField.getInt(widget);
            scale = scaleField.getFloat(widget);
        } catch (Throwable t) {
            broken = true;
            System.out.println("[WynnChaYuan] 讀不到 Wynnventory 標籤的欄位（它改版了？），"
                    + "獎勵畫面的分區標題不翻，其餘照常：" + t);
            return false;
        }
        Component shown = WynnventoryBridge.label(text);
        if (shown == null) {
            return false;
        }
        Font font = Minecraft.getInstance().font;
        float dx = CENTRED.contains(text.getString().strip())
                ? (font.width(text) - font.width(shown)) * scale / 2f : 0f;
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(placed.getX() + dx, placed.getY());
            graphics.pose().scale(scale, scale);
            graphics.drawString(font, shown, 0, 0, colour);
        } finally {
            graphics.pose().popMatrix();
        }
        return true;
    }
}
