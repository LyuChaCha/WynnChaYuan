package com.wynnchayuan.client;

/**
 * F6 裡每一種顯示方式各用什麼顏色。
 *
 * <h2>為什麼要分顏色</h2>
 * 先前「開著」的值一律是主題色，「另開面板」跟「就地取代」長得一模一樣——
 * 要讀字才知道這一項是哪一種。兩種方式對畫面的影響完全不同（一個多一個框、
 * 一個直接改掉原文），掃一眼就該分得出來。
 *
 * <h2>為什麼從主題色推</h2>
 * 顏色都從〈面板〉→〈框線顏色〉那個主題色算出來，換主題色時整組一起換，
 * 不會出現一個自己亂跳的顏色：
 *
 * <ul>
 *   <li>另開面板／小框：主題色本身——面板的框線就是這個色，一看就對得上；</li>
 *   <li>就地取代：主題色的<b>對面</b>（色相轉半圈），預設的藍配橘；</li>
 *   <li>原文加譯文：兩者的中間，本來就是兩種都有；</li>
 *   <li>關閉：灰色，不隨主題變。</li>
 * </ul>
 *
 * 三個顏色都拉到同一個亮度區間，畫在按鈕的底色上一樣清楚，也不會有哪一個
 * 特別刺眼。
 */
public final class ModeColours {

    private ModeColours() {
    }

    /** 關閉。 */
    public static final int OFF = 0xFFA0A0A0;

    /** 主題色太暗、看不清楚時的替代色：柔和的青藍。 */
    static final int SOFT = 0xFF8FC3E6;

    /** 另開面板、另開小框、注視時小框，以及一般的「開啟」。 */
    public static int separate(int accent) {
        return soften(readable(accent) ? accent : SOFT);
    }

    /** 就地取代：主題色的補色。 */
    public static int replace(int accent) {
        float[] hsv = hsv(separate(accent));
        return soften(rgb((hsv[0] + 0.5f) % 1f, hsv[1], hsv[2]));
    }

    /** 原文加譯文：兩種方式的中間色。 */
    public static int both(int accent) {
        return mix(separate(accent), replace(accent));
    }

    /**
     * 主題色夠不夠亮，畫在按鈕上看得清楚。
     *
     * <p>用加權亮度，不是三通道平均——綠色對亮度的貢獻遠大於藍色，
     * 取平均會把深藍判成夠亮。
     */
    static boolean readable(int argb) {
        return luminance(argb) >= 96;
    }

    static int luminance(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return (r * 299 + g * 587 + b * 114) / 1000;
    }

    /** 飽和度不超過 0.55、明度不低於 0.82：柔和、在按鈕上讀得清楚。 */
    static int soften(int argb) {
        float[] hsv = hsv(argb);
        return rgb(hsv[0], Math.min(hsv[1], 0.55f), Math.max(hsv[2], 0.82f));
    }

    static int mix(int a, int b) {
        int r = (((a >> 16) & 0xFF) + ((b >> 16) & 0xFF)) / 2;
        int g = (((a >> 8) & 0xFF) + ((b >> 8) & 0xFF)) / 2;
        int bl = ((a & 0xFF) + (b & 0xFF)) / 2;
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    static float[] hsv(int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h;
        if (d == 0) {
            h = 0;
        } else if (max == r) {
            h = ((g - b) / d) % 6f;
        } else if (max == g) {
            h = (b - r) / d + 2f;
        } else {
            h = (r - g) / d + 4f;
        }
        h /= 6f;
        if (h < 0) {
            h += 1f;
        }
        return new float[] {h, max == 0 ? 0 : d / max, max};
    }

    static int rgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1 - Math.abs((h * 6f) % 2f - 1));
        float m = v - c;
        float r;
        float g;
        float b;
        switch ((int) (h * 6f) % 6) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return 0xFF000000
                | (Math.round((r + m) * 255) << 16)
                | (Math.round((g + m) * 255) << 8)
                | Math.round((b + m) * 255);
    }
}
