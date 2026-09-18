package com.wynnchayuan.client;

/**
 * F6 每一種顯示方式的顏色，見 {@link ModeColours}。
 *
 * <p>釘住的是「分得出來、讀得清楚」：另開與就地取代不能是同一個色，
 * 每個顏色畫在按鈕上都要夠亮，主題色太暗時也一樣。
 */
public final class ModeColoursTest {

    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== F6 模式顏色 ===");

        int accent = 0xFF6FA8D8;                    // 預設的主題色（淡藍）
        int sep = ModeColours.separate(accent);
        int rep = ModeColours.replace(accent);
        int both = ModeColours.both(accent);

        check("另開面板偏藍（藍 > 紅）", blue(sep) > red(sep));
        check("就地取代是補色，偏橘（紅 > 藍）", red(rep) > blue(rep));
        check("原文加譯文跟兩者都不同", both != sep && both != rep);
        check("另開面板讀得清楚", ModeColours.luminance(sep) >= 96);
        check("就地取代讀得清楚", ModeColours.luminance(rep) >= 96);
        check("原文加譯文讀得清楚", ModeColours.luminance(both) >= 96);
        check("關閉跟其他三個都不同",
              ModeColours.OFF != sep && ModeColours.OFF != rep && ModeColours.OFF != both);

        int dark = 0xFF101830;                      // 深藍：畫在按鈕上看不清楚
        check("主題色太暗時另開面板仍讀得清楚",
              ModeColours.luminance(ModeColours.separate(dark)) >= 96);
        check("主題色太暗時就地取代仍讀得清楚",
              ModeColours.luminance(ModeColours.replace(dark)) >= 96);

        int red = 0xFFE05050;                       // 換主題色，整組跟著換
        check("換成紅色主題，另開面板偏紅",
              red(ModeColours.separate(red)) > blue(ModeColours.separate(red)));
        check("換成紅色主題，就地取代換成青色系",
              blue(ModeColours.replace(red)) > red(ModeColours.replace(red)));

        check("HSV 來回轉換不走樣",
              near(ModeColours.rgb(ModeColours.hsv(0xFF6FA8D8)[0],
                      ModeColours.hsv(0xFF6FA8D8)[1],
                      ModeColours.hsv(0xFF6FA8D8)[2]), 0xFF6FA8D8));

        System.out.println(failures == 0
                ? "F6 模式顏色：全部通過"
                : "F6 模式顏色：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static int red(int c) {
        return (c >> 16) & 0xFF;
    }

    private static int blue(int c) {
        return c & 0xFF;
    }

    private static boolean near(int a, int b) {
        return Math.abs(red(a) - red(b)) <= 1
                && Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF)) <= 1
                && Math.abs(blue(a) - blue(b)) <= 1;
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
