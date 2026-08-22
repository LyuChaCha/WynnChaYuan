package com.wynnchayuan.translate;

import java.nio.file.Path;

/**
 * 技能樹標籤的查表。
 *
 * <h2>這裡踩過的坑</h2>
 * <ol>
 *   <li>{@code ability-labels.json} 的鍵本來就帶冒號（{@code "Damage:"}），
 *       查表剝尾巴時把冒號跟空白一起剝掉，那個鍵永遠對不到，退回
 *       {@code ui-labels.json} 的無冒號版</li>
 *   <li>百分比版本的鍵是 {@code "Fire Damage:%"}。模板尾端有空格時接出來的是
 *       {@code "Fire Damage: %"}，對不到，於是<b>靜默退回非百分比的譯法</b>——
 *       畫面上「+15%」會被標成「火屬性傷害」而不是「火屬性傷害百分比」</li>
 *   <li>100% 的物品名稱前面會多一個 {@code Perfect}，整個名稱查不到</li>
 * </ol>
 *
 * <p>三種的症狀都是「看起來就是沒翻好」，所以用真正的語料跑一次。
 */
public final class LabelColonTest {

    private static int failures = 0;

    public static void main(String[] args) {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations"));
        String glyph = com.wynnchayuan.capture.GlyphSplitter.GLYPH_PLACEHOLDER;

        // --- 帶冒號的鍵 ---
        check("帶冒號的鍵查得到", "傷害:".equals(store.lookup("Damage:")));
        check("單獨的標籤（拿到：[" + look("Damage: ", store, false) + "]）", "傷害: ".equals(look("Damage: ", store, false)));
        check("圖示 + 標籤", (glyph + " 傷害: ").equals(look(glyph + " Damage: ", store, false)));

        // --- 百分比變體 ---
        check("百分比的鍵存在", "火屬性傷害百分比:".equals(store.lookup("Fire Damage:%")));
        String pct = look("Fire Damage: ", store, true);
        check("百分比的行要用百分比的譯法（實際拿到：" + pct + "）",
                pct != null && pct.startsWith("火屬性傷害百分比:"));
        String raw = look("Fire Damage: ", store, false);
        check("非百分比的行用一般譯法（實際拿到：" + raw + "）",
                raw != null && raw.startsWith("火屬性傷害:") && !raw.contains("百分比"));

        // --- 冒號後面要留得下數值 ---
        for (String key : new String[] {"Damage: ", "Duration: ", "Cooldown: ", "Water: "}) {
            String got = look(glyph + " " + key, store, false);
            check(key.trim() + " 後面保留原文的空白（拿到：[" + got + "]）",
                    got != null && got.endsWith(" ") && !got.contains("："));
        }

        // --- 品質前綴 ---
        check("滿滾的物品名稱查得到（實際拿到："
                        + store.lookup("Perfect Ephemeral Tome of Mysticism II") + "）",
                "完美流光秘法書卷 II".equals(
                        look("Perfect Ephemeral Tome of Mysticism II", store, false)));
        check("最低滾的也查得到",
                look("Defective Ephemeral Tome of Mysticism II", store, false) != null);
        check("前綴後面不認得的名稱不硬翻",
                look("Perfect Nonexistent Item Name", store, false) == null);
        // 本來就以 Perfect 開頭的真名稱，不能被前綴處理改掉
        check("本來就叫 Perfect 的不受影響",
                java.util.Objects.equals(store.lookup("Perfect Recall"),
                        look("Perfect Recall", store, false)));

        System.out.println(failures == 0
                ? "LabelColon: 全部通過" : "LabelColon: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static String look(String template, TranslationStore store, boolean percent) {
        return LineTranslator.lookup(template, store, percent);
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
