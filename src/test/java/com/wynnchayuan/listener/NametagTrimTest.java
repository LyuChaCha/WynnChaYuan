package com.wynnchayuan.listener;

import com.wynnchayuan.capture.GlyphSplitter;

/**
 * 名牌尾巴的狀態列不能被當成語料收進來。
 *
 * <h2>先前壞在哪</h2>
 * 怪物名牌的下半是血條與狀態圖示，而那一行會隨著身上掛了哪些增益／減益、
 * 各自剩幾秒而<b>每一種組合都不一樣</b>。整塊當成一個鍵記下來，同一隻怪就
 * 被拆成幾十條——實機打一輪突襲，{@code Grootslang Whelp} 一隻收了 95 條，
 * 1,074 條漂浮字裡有 919 條是這樣來的，收合起來其實只有 292 個不同的鍵。
 *
 * <p>譯者打開檔案看到的是一片幾乎一模一樣的東西，而真正缺的字串被埋在裡面。
 *
 * <h2>這裡釘住什麼</h2>
 * 兩個方向都要測。砍太少就白做；砍太多會把<b>真的有第二行</b>的告示看板
 * （{@code Teleporter / to dock}、{@code Binding Seal / {#} to activate}）
 * 砍成半句，而那正是突襲裡最需要翻的一批字。
 */
public final class NametagTrimTest {

    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== 名牌狀態列 ===");

        // --- 狀態列要砍掉 -----------------------------------------------
        same("怪物名牌只留名字",
             "Jagaubis {#}{#}\n{#}\n{#} {#} {~} ❁ {#}s ⬣ {~}s", "Jagaubis {#}{#}");
        same("只有血條那一行", "Lozeg\n{#}{#}", "Lozeg");
        same("血條分成兩行", "{p} Dwarf {#}{#}\n{#}", "{p} Dwarf {#}{#}");
        same("狀態列裡的 s 是秒不是字",
             "Onyx Axon {#}{#}\n{#}\n☠ {~} {#} {~}s", "Onyx Axon {#}{#}");
        same("整條狀態列只有圖示",
             "Knightmare Guard {#}{#}\n{#}", "Knightmare Guard {#}{#}");

        // --- 真的有第二行的不能動 ---------------------------------------
        keep("傳送點的第二行是字", "Teleporter\nto dock");
        keep("突襲提示的第二行是字", "Binding Seal\n{#} to activate");
        keep("三行都有字", "Combat Dummy\nNPC\nAverage DPS: {~} / {~}");
        keep("第一行是圖示、第二行才是名字", "{#}{#}\nThe Place Condensed");
        keep("單行原樣不動", "Grootslang Whelp {#}{#}");
        keep("突襲的長提示",
             "{#}{#}\nSlay any void holes before they spawn a Boss!");

        // 尾巴砍掉之後前面那幾行照留：這一條同時測「只從尾巴砍」
        same("有字的中間行要留著",
             "Combat Dummy\nNPC\nAverage DPS: {~} / {~}\n✹ {~}s",
             "Combat Dummy\nNPC\nAverage DPS: {~} / {~}");

        // --- hasWord 本身 -----------------------------------------------
        check("兩個字母才算字：NPC", GlyphSplitter.hasWord("NPC"));
        check("兩個字母才算字：to dock", GlyphSplitter.hasWord("to dock"));
        check("漢字一個就算字", GlyphSplitter.hasWord("秒"));
        check("單獨的 s 是單位不算字", !GlyphSplitter.hasWord("{#}s ⬣ {~}s"));
        check("純圖示與數值不算字", !GlyphSplitter.hasWord("{#} {#} {~} ☠ {~}"));
        check("空字串不算", !GlyphSplitter.hasWord(""));
        check("null 不會炸", !GlyphSplitter.hasWord(null));

        System.out.println(failures == 0
                ? "名牌狀態列：全部通過"
                : "名牌狀態列：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void same(String what, String input, String want) {
        String got = CaptureListener.trimStatusLines(input);
        check(what + "（實際：" + got.replace("\n", "⏎") + "）", want.equals(got));
    }

    private static void keep(String what, String input) {
        same(what, input, input);
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
