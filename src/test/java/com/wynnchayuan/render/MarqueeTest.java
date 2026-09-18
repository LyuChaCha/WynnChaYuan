package com.wynnchayuan.render;

/**
 * 對話選項跑馬燈：捲動中的每一格都要沿用第一格的譯文。見 {@link Marquee}。
 *
 * <p>下面的視窗是實機截圖上那四個選項的真實樣子（2026-09-18 回報）。
 */
public final class MarqueeTest {

    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== 選項跑馬燈 ===");
        Marquee.clear();

        String full = "Do you remember anything from before you joined?";
        int width = 28;
        Marquee.remember("choice_0", full.substring(0, width).strip(), "你還記得以前的事嗎？");
        boolean all = true;
        for (int s = 1; s + width <= full.length(); s++) {
            String window = full.substring(s, s + width).strip();
            if (!"你還記得以前的事嗎？".equals(Marquee.follow("choice_0", window))) {
                System.out.println("    沒接上：「" + window + "」");
                all = false;
            }
        }
        report("★ 捲到最後一格都沿用第一格的譯文", all);

        // 實機截圖上那一格
        Marquee.clear();
        Marquee.remember("choice_3", "Aren't you scared of what's", "你不怕外面的世界嗎？");
        report("★ 「ren't you scared of what's o」接得上",
               "你不怕外面的世界嗎？".equals(
                       Marquee.follow("choice_3", "ren't you scared of what's o")));

        // 別列的不能借用
        report("別的列不借用這一列的譯文",
               Marquee.follow("choice_1", "ren't you scared of what's o") == null);

        // 完全不相干的字不能被當成同一個選項
        Marquee.clear();
        Marquee.remember("choice_0", "What are you going to do now", "你接下來打算怎麼辦？");
        report("不相干的一格不沿用",
               Marquee.follow("choice_0", "Tell me about the Wynn Province") == null);
        report("只重疊幾個字母不算",
               !Marquee.continues("abcdefgh", "cdefXYZ"));
        report("同一格重送照樣沿用",
               "你接下來打算怎麼辦？".equals(
                       Marquee.follow("choice_0", "What are you going to do now")));

        System.out.println(failures == 0 ? "選項跑馬燈：全部通過" : "選項跑馬燈：" + failures + " 項失敗");
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
