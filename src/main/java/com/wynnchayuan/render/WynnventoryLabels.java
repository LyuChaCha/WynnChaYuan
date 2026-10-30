package com.wynnchayuan.render;

import com.wynnchayuan.ModLog;
import com.wynnchayuan.translate.WynnventoryBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.Set;

// 用反射不用 @Shadow：Wynnventory 改名時遊戲會開不起來
public final class WynnventoryLabels {

    private WynnventoryLabels() {}

    private static final Set<String> CENTRED = Set.of("Filters");

    private static Field textField;
    private static Field colourField;
    private static Field scaleField;
    private static boolean broken;

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
            ModLog.info("讀不到 Wynnventory 標籤的欄位（它改版了？），"
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
