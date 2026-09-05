package com.wynnchayuan.listener;

import java.util.List;

/**
 * 整段翻好了的話，它的每一行都不該被列成缺口。
 *
 * <h2>怎麼發現的</h2>
 * 實機那一份 {@code captured.json} 列了 123 條缺口，逐條對回語料之後，
 * <b>70 條是假的</b>——整段早就翻好了，只是 tooltip 把它照寬度切成好幾行，
 * 而判斷「翻了沒」是逐行問的：
 *
 * <pre>
 *   畫面  Contains one of several
 *         exclusive ingredients in
 *         high quantities.
 *   語料  Contains one of several exclusive ingredients in high quantities.
 *         內含數種專屬素材之一，數量豐沛。
 * </pre>
 *
 * 三行單獨問都是「沒翻」，於是三行都進了清單。
 *
 * <h2>為什麼不是只有雜訊</h2>
 * captured.json 是翻譯團隊的工作清單。照著補，補出來的是<b>逐行</b>條目，
 * 而逐行條目會蓋掉整段那條路——畫面上就成了半中半英。先前 {@code misc.json}
 * 裡那兩百多條屬性列就是這樣長出來的。
 *
 * <h2>這條測試在盯什麼</h2>
 * 段落怎麼切、切出來的整段有沒有去問。段落的界線是<b>空白行與純圖示行</b>，
 * 那是 tooltip 自己的排版，不是猜的。
 */
public final class GuiBlockGapTest {

    private static int failures = 0;

    /** 假語料：只認得這兩段。 */
    private static boolean known(List<String> lines) {
        String joined = String.join(" ", lines);
        return joined.equals("Contains one of several exclusive ingredients in high quantities.")
                || String.join("\n", lines).equals("Increases your maximum orbs\n"
                        + "from Ophanim by +{~} and\nreduces their damage.");
    }

    public static void main(String[] args) {
        // 空白接法（tooltip 照寬度切開的一段話）
        List<String> bag = java.util.Arrays.asList(
                "Packed Crafter Bag [{~}/{~}]",
                null,
                "Contains one of several",
                "exclusive ingredients in",
                "high quantities.",
                null,
                "Rare Item");
        boolean[] out = GuiTextCapture.covered(bag, GuiBlockGapTest::known);
        report("標題不算在段落裡", !out[0]);
        report("整段翻好的三行都不列成缺口", out[2] && out[3] && out[4]);
        report("後面那行不受影響", !out[6]);

        // 換行接法（語料收成一個帶 \n 的鍵）
        List<String> node = java.util.Arrays.asList(
                "Divination",
                null,
                "Increases your maximum orbs",
                "from Ophanim by +{~} and",
                "reduces their damage.");
        boolean[] two = GuiTextCapture.covered(node, GuiBlockGapTest::known);
        report("換行接法也認得", two[2] && two[3] && two[4]);
        report("技能名還是要收", !two[0]);

        // 反面：整段查不到就一行都不能擋掉，不然真的缺口會消失
        List<String> unknown = java.util.Arrays.asList(
                "Quickly find parties", "for raids and others!");
        boolean[] none = GuiTextCapture.covered(unknown, GuiBlockGapTest::known);
        report("整段查不到就照舊列成缺口", !none[0] && !none[1]);

        // 反面：只有一行的段落交給原本那條路，這裡不插手
        List<String> single = java.util.Arrays.asList("Contains one of several");
        report("單行段落不處理",
                !GuiTextCapture.covered(single, GuiBlockGapTest::known)[0]);

        System.out.println(failures == 0
                ? "整段缺口：全部通過" : "整段缺口：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
