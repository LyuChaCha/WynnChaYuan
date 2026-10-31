package com.wynnchayuan.render;

import com.wynnchayuan.ModLog;
import com.wynnchayuan.ThirdPartyField;
import com.wynnchayuan.translate.WmsBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.lang.reflect.Method;

// WynnMarketSearch 的 WynnItem 編譯時沒有，用反射取名
public final class WmsRows {

    private WmsRows() {}

    private static Method getItem;
    private static Method getName;
    private static boolean broken;

    private static final ThirdPartyField X = new ThirdPartyField("x");
    private static final ThirdPartyField Y = new ThirdPartyField("y");
    private static final ThirdPartyField WIDTH = new ThirdPartyField("width");
    private static final ThirdPartyField HEIGHT = new ThirdPartyField("height");

    private static final int INK = 0xFFA9B5C3;
    private static final int STAR_ROOM = 20;
    private static final int NAME_X = 24;

    public static void draw(GuiGraphics g, Object widget) {
        if (broken || widget == null) {
            return;
        }
        if (!WIDTH.available(widget) || !HEIGHT.available(widget)
                || !X.available(widget) || !Y.available(widget)) {
            broken = true;
            return;
        }
        try {
            int x = X.getInt(widget, 0);
            int y = Y.getInt(widget, 0);
            int width = WIDTH.getInt(widget, 0);
            int height = HEIGHT.getInt(widget, 0);
            String english = name(widget);
            String shown = WmsBridge.translatedName(english);
            if (shown == null || shown.equalsIgnoreCase(english)) {
                return;
            }
            Font font = Minecraft.getInstance().font;
            int right = x + width - STAR_ROOM;
            int room = right - (x + NAME_X + font.width(english) + 10);
            if (room < 18) {
                return;
            }
            if (font.width(shown) > room) {
                shown = font.plainSubstrByWidth(shown, room - font.width("…")) + "…";
            }
            g.drawString(font, shown, right - font.width(shown), y + (height - 8) / 2, INK, false);
        } catch (Throwable t) {
            broken = true;
            ModLog.info("WynnMarketSearch 的結果列畫不上譯名，停用這一項"
                    + "（搜尋本身不受影響）：" + t);
        }
    }

    private static String name(Object widget) throws ReflectiveOperationException {
        if (getItem == null) {
            getItem = widget.getClass().getMethod("getItem");
        }
        Object item = getItem.invoke(widget);
        if (item == null) {
            return null;
        }
        if (getName == null) {
            getName = item.getClass().getMethod("getName");
        }
        Object value = getName.invoke(item);
        return value == null ? null : value.toString();
    }
}
