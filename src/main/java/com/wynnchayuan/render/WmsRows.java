package com.wynnchayuan.render;

import com.wynnchayuan.translate.WmsBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.lang.reflect.Method;

/**
 * 在 WynnMarketSearch 的搜尋結果上，把譯名畫在英文名的右邊。
 *
 * <h2>為什麼要畫</h2>
 * 打「之心」會找到好幾件，但那個面板列出來的全是英文名——找得到卻認不出哪一件
 * 是要的，等於只做了一半。英文名留著（點下去送出的是它，也對得上 wiki），
 * 譯名用淡一點的顏色靠右放，兩邊都看得到。
 *
 * <h2>為什麼用反射</h2>
 * 那一列手上的物品是那個模組自己的型別（{@code me.a0g.api.WynnItem}），我們編譯的
 * 時候沒有它。只需要問一個名字，所以用反射拿——方法找到一次就留著，之後每一幀
 * 只是呼叫。那個模組改版、方法不見了的話，第一次失敗就整個停用，不會每一幀都丟例外。
 *
 * <p>放在 {@code render} 而不是 {@code mixin}：mixin 那個套件裡的類別不能被注入進去的
 * 程式碼直接引用，實機會崩（見 {@code build.gradle} 的 {@code checkMixinPackage}）。
 */
public final class WmsRows {

    private WmsRows() {}

    private static Method getItem;
    private static Method getName;
    private static boolean broken;

    /** 譯名的顏色：比英文名淡，一眼分得出哪個是主、哪個是註。 */
    private static final int INK = 0xFFA9B5C3;
    /** 右邊留給那個模組畫最愛星號的位置。 */
    private static final int STAR_ROOM = 20;
    /** 英文名從這一列的左緣往右多少開始（圖示之後）。 */
    private static final int NAME_X = 24;

    /**
     * @param widget 那個模組的一列（{@code ItemButtonWidget}）
     */
    public static void draw(GuiGraphics g, Object widget, int x, int y, int width, int height) {
        if (broken || widget == null) {
            return;
        }
        try {
            String english = name(widget);
            String shown = WmsBridge.translatedName(english);
            if (shown == null || shown.equalsIgnoreCase(english)) {
                return;
            }
            Font font = Minecraft.getInstance().font;
            int right = x + width - STAR_ROOM;
            int room = right - (x + NAME_X + font.width(english) + 10);
            if (room < 18) {
                return;                        // 英文名已經把這一列佔滿了
            }
            if (font.width(shown) > room) {
                shown = font.plainSubstrByWidth(shown, room - font.width("…")) + "…";
            }
            g.drawString(font, shown, right - font.width(shown), y + (height - 8) / 2, INK, false);
        } catch (Throwable t) {
            broken = true;
            System.out.println("[WynnChaYuan] WynnMarketSearch 的結果列畫不上譯名，停用這一項"
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
