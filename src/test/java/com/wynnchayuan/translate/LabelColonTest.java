package com.wynnchayuan.translate;

import java.nio.file.Path;

/**
 * 面板裡的冒號只能有一種。
 *
 * <h2>為什麼要有這條測試</h2>
 * {@code ability-labels.json} 的鍵本來就帶冒號（{@code "Damage:"}），因為技能樹
 * 需要比物品欄更短的譯法。但查表剝尾巴時<b>把冒號跟空白一起剝掉</b>，那個鍵
 * 於是永遠對不到，退回 {@code ui-labels.json} 的無冒號版，再把原文的半形
 * 「{@code : }」接回去。
 *
 * <p>畫面上的症狀是同一個面板裡「持續時間：」與「傷害: 」並存——一個全形一個
 * 半形，間距也不一樣，看起來就是排版壞掉。標籤前面有沒有圖示會決定走到哪條路，
 * 所以兩種都要試。
 */
public final class LabelColonTest {

    private static int failures = 0;

    public static void main(String[] args) {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations"));
        String glyph = com.wynnchayuan.capture.GlyphSplitter.GLYPH_PLACEHOLDER;

        check("帶冒號的鍵查得到", "傷害：".equals(store.lookup("Damage:")));

        // 標籤自己一段：原本就會走精準命中
        check("單獨的標籤", "傷害：".equals(look("Damage: ", store)));

        // 前面黏著圖示：精準命中會落空，得靠剝尾巴那條路
        check("圖示 + 標籤", (glyph + " 傷害：").equals(look(glyph + " Damage: ", store)));
        check("圖示 + 兩個字的元素標籤",
                (glyph + " 水：").equals(look(glyph + " Water: ", store)));

        // 只有 ui-labels 有的標籤：沒有帶冒號的鍵，也不該接回半形的「: 」
        String manaRegen = look(glyph + " Mana Regen: ", store);
        check("沒有專用鍵時也是全形冒號",
                manaRegen != null && !manaRegen.contains(":") && manaRegen.endsWith("："));

        // 全形冒號後面不留空白，留白疊留白會變成兩倍寬
        for (String key : new String[] {"Damage: ", "Duration: ", "Cooldown: ", "Water: "}) {
            String got = look(glyph + " " + key, store);
            check(key.trim() + " 後面沒有多餘空白",
                    got != null && got.equals(got.stripTrailing()));
        }

        System.out.println(failures == 0
                ? "LabelColon: 全部通過" : "LabelColon: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static String look(String template, TranslationStore store) {
        return LineTranslator.lookup(template, store, false);
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
