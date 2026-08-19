package com.wynnchayuan.render;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 暫存這一幀要畫的 tooltip 內容，等原始 tooltip 畫完之後再用。
 *
 * <h2>為什麼要繞這一圈</h2>
 * Wynntils 的 {@code ItemTooltipRenderEvent.Pre} 是<b>唯一</b>拿得到 tooltip 行列表的地方，
 * 但它在原始 tooltip 畫出來<b>之前</b>觸發——在那裡繪製，畫出來的東西會立刻被原始
 * tooltip 蓋掉，症狀就是「明明翻譯載入了卻什麼都沒看到」。
 *
 * <p>{@code ItemTooltipRenderEvent.Post} 看起來是答案，但 Wynntils 從來沒有發送過它
 * （整個原始碼裡只有類別定義，沒有任何 post 呼叫），所以用不了。
 *
 * <p>因此拆成兩步：{@code Pre} 只<b>記下</b>內容，實際繪製交給 Fabric 的
 * {@code ScreenEvents.afterRender}——那個時機在整個畫面（含 tooltip）都畫完之後。
 */
public final class PendingTooltip {

    private static volatile List<Component> lines = List.of();
    private static volatile int mouseX;
    private static volatile int mouseY;

    private PendingTooltip() {}

    /** 在 {@code ItemTooltipRenderEvent.Pre} 呼叫，只記錄不繪製。 */
    public static void set(List<Component> tooltip, int x, int y) {
        lines = tooltip == null ? List.of() : List.copyOf(tooltip);
        mouseX = x;
        mouseY = y;
    }

    /**
     * 取出並清空。
     *
     * <p>取完就清掉是刻意的：滑鼠移開物品後 {@code Pre} 就不再觸發，
     * 沒有這個清除動作的話面板會留在畫面上不消失。
     */
    public static List<Component> take() {
        List<Component> current = lines;
        lines = List.of();
        return current;
    }

    public static int mouseX() {
        return mouseX;
    }

    public static int mouseY() {
        return mouseY;
    }
}
