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
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));
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

        // --- 「名稱：說明」拆解時冒號不能先切掉 ---
        //
        // 語料裡這些標籤<b>只</b>收了帶冒號的形式（"Range:"、"Total Damage:"），
        // 而先前拆解是切在冒號前面再查，於是只有同時也收了無冒號版的
        // "Duration" 查得到，其餘一律落空——技能面板整行掉回英文。
        //
        // 這一條刻意用真正的語料：bug 的關鍵就是「語料裡收了哪一種形式」，
        // 自己造一份假語料就把要驗的東西驗掉了。
        for (String[] pair : new String[][] {
                {"Range:", "施放範圍"}, {"Total Damage:", "總傷害"},
                {"Duration:", "持續時間"}}) {
            String got = LineTranslator.labelHead(glyph + " " + pair[0], store);
            check("只收帶冒號版本的標籤也查得到：" + pair[0] + "（拿到：[" + got + "]）",
                    got != null && got.contains(pair[1]));
        }
        // 反面：不認得的標籤不能硬翻
        check("不認得的標籤還是 null",
                LineTranslator.labelHead(glyph + " Nonexistent Label:", store) == null);
        // 名稱自帶冒號時，接起來不能變成兩個冒號
        String head = LineTranslator.labelHead(glyph + " Range:", store);
        check("名稱自帶冒號（" + head + "）", head != null && head.endsWith(":"));

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
