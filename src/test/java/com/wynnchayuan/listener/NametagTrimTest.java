package com.wynnchayuan.listener;

import com.wynnchayuan.capture.GlyphSplitter;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class NametagTrimTest {

    public static void main(String[] args) {
        System.out.println("=== 名牌狀態列 ===");

        same("怪物名牌只留名字",
             "Jagaubis {#}{#}\n{#}\n{#} {#} {~} ❁ {#}s ⬣ {~}s", "Jagaubis {#}{#}");
        same("只有血條那一行", "Lozeg\n{#}{#}", "Lozeg");
        same("血條分成兩行", "{p} Dwarf {#}{#}\n{#}", "{p} Dwarf {#}{#}");
        same("狀態列裡的 s 是秒不是字",
             "Onyx Axon {#}{#}\n{#}\n☠ {~} {#} {~}s", "Onyx Axon {#}{#}");
        same("整條狀態列只有圖示",
             "Knightmare Guard {#}{#}\n{#}", "Knightmare Guard {#}{#}");
        same("★ 狀態列的 Distorted 也砍",
             "Annihilation {#}{#}\n{#}\n{#} {#} {#} ✜ {~} ☠ {~}k {#} {~}s Distorted {~}s",
             "Annihilation {#}{#}");
        same("只有 Distorted 的狀態列",
             "Fluffmuncher {#}{#}\n{#}\nDistorted {~}s", "Fluffmuncher {#}{#}");
        keep("Distorted 後面還有別的字就不是狀態列", "Strange Altar\nDistorted Memories");

        keep("傳送點的第二行是字", "Teleporter\nto dock");
        keep("突襲提示的第二行是字", "Binding Seal\n{#} to activate");
        keep("三行都有字", "Combat Dummy\nNPC\nAverage DPS: {~} / {~}");
        keep("第一行是圖示、第二行才是名字", "{#}{#}\nThe Place Condensed");
        keep("單行原樣不動", "Grootslang Whelp {#}{#}");
        keep("突襲的長提示",
             "{#}{#}\nSlay any void holes before they spawn a Boss!");

        same("有字的中間行要留著",
             "Combat Dummy\nNPC\nAverage DPS: {~} / {~}\n✹ {~}s",
             "Combat Dummy\nNPC\nAverage DPS: {~} / {~}");

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

}
