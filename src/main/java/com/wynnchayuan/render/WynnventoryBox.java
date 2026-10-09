package com.wynnchayuan.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import org.joml.Vector2i;

import java.util.List;

/**
 * Wynnventory 的價格框這一幀畫在哪裡，我們的譯文面板好讓開。
 *
 * <h2>為什麼會撞</h2>
 * 兩邊都把自己的框放在「物品說明的右邊」：它隔 18 px，我們隔使用者設的間距（預設 12）。
 * 物品翻譯的預設又是「另開面板」，所以裝了 Wynnventory 的人一把游標移到有價格的物品上，
 * 兩個框就疊在一起。
 *
 * <h2>誰讓誰</h2>
 * 我們讓。它的事件優先序比我們高（LOW 對 LOWEST），同一幀裡它先算好位置、先畫，
 * 輪到我們的時候已經知道它在哪（{@link #note}）；反過來要它等我們就得改它的流程。
 *
 * <p>這個類別不認得 Wynnventory 的任何類別——位置是 mixin 轉過來的一個點與一串
 * tooltip 元件，都是原版的型別。沒裝的人永遠沒有人呼叫 {@link #note}，
 * {@link #place} 就原樣回傳。
 */
public final class WynnventoryBox {

    private WynnventoryBox() {}

    /** 記下來的框多久之內還算數。一幀最多幾十毫秒；游標移開之後不能一直讓。 */
    private static final long FRESH_NANOS = 120_000_000L;

    /** tooltip 的底色與框線比文字區往外多出來的量。 */
    private static final int PAD = 4;

    private static int[] box;
    private static long notedAt;

    /**
     * 它算好位置了。
     *
     * @param at    {@code RenderUtils#calculateTooltipCoords} 的回傳值。它畫的時候整個
     *              座標系會乘上縮放，所以真正的位置是這個點再乘一次
     * @param lines 價格框的每一行
     */
    public static void note(Vector2i at, List<ClientTooltipComponent> lines) {
        try {
            if (at == null || lines == null || lines.isEmpty()) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            Font font = mc.font;
            int w = 0;
            int h = 0;
            for (ClientTooltipComponent c : lines) {
                w = Math.max(w, c.getWidth(font));
                h += c.getHeight(font);
            }
            // 跟它一樣：太高、放不進畫面的時候整個縮小
            int screenH = mc.getWindow().getGuiScaledHeight();
            float scale = h + 10 > screenH ? screenH / (float) (h + 10) : 1f;
            note(Math.round(at.x * scale) - PAD, Math.round(at.y * scale) - PAD,
                    Math.round(w * scale) + PAD * 2, Math.round(h * scale) + PAD * 2,
                    System.nanoTime());
        } catch (Throwable t) {
            // 量不出來就當作沒有：頂多回到會疊在一起的老樣子
        }
    }

    static void note(int x, int y, int w, int h, long now) {
        box = new int[] {x, y, w, h};
        notedAt = now;
    }

    /** 給測試用。 */
    static void forget() {
        box = null;
    }

    /**
     * 譯文面板的橫向位置：原本算好的那個位置會蓋到價格框的話，換一個不會蓋到的。
     *
     * <p>照順序試：物品說明的另一邊 → 價格框的外側（跟物品說明同一邊，再往外）。
     * 都放不下就留在原位——疊在一起總比面板跑出畫面、或蓋到物品說明好。
     *
     * @param x         原本算好的位置（已經夾在畫面內）
     * @param leftSide  面板放物品說明左邊時的位置
     * @param rightSide 面板放物品說明右邊時的位置
     * @param gap       使用者設的間距
     */
    public static int place(int x, int y, int w, int h, int leftSide, int rightSide,
                            int screenW, int gap) {
        return place(x, y, w, h, leftSide, rightSide, screenW, gap, System.nanoTime());
    }

    static int place(int x, int y, int w, int h, int leftSide, int rightSide,
                     int screenW, int gap, long now) {
        int[] b = box;
        if (b == null || now - notedAt > FRESH_NANOS || !hits(b, x, y, w, h)) {
            return x;
        }
        // 物品說明自己佔的那一段：左邊界是「面板放左邊」再往右一個面板加間距，右邊界同理
        int tipLeft = leftSide + w + gap;
        int tipRight = rightSide - gap;
        boolean wasRight = Math.abs(x - rightSide) <= Math.abs(x - leftSide);
        int other = wasRight ? leftSide : rightSide;
        // 價格框的外側：它在物品說明右邊就再往右，在左邊就再往左。它被固定在畫面邊上、
        // 跟物品說明沒有左右關係的時候沒有「外側」可言，只試另一邊。
        int beyond = b[0] >= tipRight ? b[0] + b[2] + gap
                : b[0] + b[2] <= tipLeft ? b[0] - gap - w
                : Integer.MIN_VALUE;
        for (int candidate : new int[] {other, beyond}) {
            if (candidate >= 0 && candidate + w <= screenW && !hits(b, candidate, y, w, h)) {
                return candidate;
            }
        }
        return x;
    }

    private static boolean hits(int[] b, int x, int y, int w, int h) {
        return x < b[0] + b[2] && b[0] < x + w && y < b[1] + b[3] && b[1] < y + h;
    }
}
