package com.wynnchayuan.translate;

import java.nio.file.Path;

/**
 * 實機截圖裡那三行查出來到底是什麼。
 *
 * <h2>回報</h2>
 * 簡體的技能 tooltip 上，傷害細項那兩行印成
 * 「{@code ( 总伤害: 200%)}」與「{@code 火属性伤害: 100%)}」，
 * 而原文是「{@code ( Damage: 200%)}」與「{@code ( Fire: 100%)}」。
 *
 * <p>也就是說標籤查到了<b>更長的那一條</b>：{@code Damage:} 查成
 * {@code Total Damage:}、{@code Fire:} 查成 {@code Fire Damage:}。
 * 語料裡兩邊都有正確的整行條目，所以問題在查表那一路。
 *
 * <p>這支只印結果，不判對錯——先看清楚查到什麼，再決定要修哪裡。
 */
public final class StatLabelProbe {

    private static final String[] LINES = {
        "{#} Total Damage: {~} (of your DPS, per Wisp)",
        "{#}({#}{#}Damage: {~})",
        "{#}({#}{#}Fire: {~})",
        "{#} Total Heal: {~} (of max health)",
        "{#} Area of Effect: {~} Blocks (Circle-Shaped)",
        "{#} Range: {~} Blocks",
    };

    public static void main(String[] args) {
        for (String lang : new String[] {"zh_tw", "zh_cn"}) {
            TranslationStore store = new TranslationStore();
            store.loadAll(Path.of(
                    "src/main/resources/assets/wynnchayuan/translations", lang));
            System.out.println("=== " + lang + "（" + store.size() + " 條）");
            for (String line : LINES) {
                System.out.println("  " + line);
                System.out.println("      整行查：" + show(store.lookup(line)));
                System.out.println("      走完整條路：" + show(
                        LineTranslator.lookup(line, store, false)));
            }
        }
    }

    private static String show(String text) {
        return text == null ? "（查不到）" : "「" + text + "」";
    }
}
