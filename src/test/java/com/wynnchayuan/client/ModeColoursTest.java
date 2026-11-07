package com.wynnchayuan.client;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class ModeColoursTest {

    public static void main(String[] args) {
        System.out.println("=== F6 模式顏色 ===");

        int leaf = 0xFF7FD08A;
        check("預設的葉綠主題：另外顯示就是葉綠", ModeColours.separate(leaf) == 0xFF7FD08A);
        check("預設的葉綠主題：就地取代是金色", ModeColours.replace(leaf) == 0xFFE8C876);
        check("預設的葉綠主題：原文加譯文是青色", ModeColours.both(leaf) == 0xFF6FE0EC);
        check("預設的葉綠主題：漸層的深色端照設計稿",
              ModeColours.separateDeep(leaf) == 0xFF3A9F4C
                      && ModeColours.replaceDeep(leaf) == 0xFFB98A3C
                      && ModeColours.bothDeep(leaf) == 0xFF2F9FB4);

        int accent = 0xFF6FA8D8;
        int sep = ModeColours.separate(accent);
        int rep = ModeColours.replace(accent);
        int both = ModeColours.both(accent);
        check("換成藍色主題，另外顯示跟著偏藍（藍 > 紅）", blue(sep) > red(sep));
        check("三種模式顏色互不相同", sep != rep && rep != both && both != sep);
        check("另外顯示讀得清楚", ModeColours.luminance(sep) >= 96);
        check("就地取代讀得清楚", ModeColours.luminance(rep) >= 96);
        check("原文加譯文讀得清楚", ModeColours.luminance(both) >= 96);
        check("關閉跟其他三個都不同",
              ModeColours.OFF != sep && ModeColours.OFF != rep && ModeColours.OFF != both);

        int grey = 0xFF808080;
        check("灰色主題（沒有色相）就用預設的三色",
              ModeColours.separate(grey) == 0xFF7FD08A && ModeColours.replace(grey) == 0xFFE8C876);

        int red = 0xFFE05050;
        check("換成紅色主題，另外顯示偏紅",
              red(ModeColours.separate(red)) > blue(ModeColours.separate(red)));
        check("換成紅色主題，就地取代跟另外顯示仍然分得開",
              ModeColours.replace(red) != ModeColours.separate(red));

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

}
