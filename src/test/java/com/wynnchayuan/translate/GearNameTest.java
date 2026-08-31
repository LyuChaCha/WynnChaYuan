package com.wynnchayuan.translate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 裝備名是專有名詞，別的領域不能替它翻譯。
 *
 * <h2>先前壞在哪</h2>
 * 實機回報：神話矛「Guardian」的名稱被翻成「守護者」。那個譯名來自
 * {@code major-id.json}——Wynncraft 真的有一個叫 Guardian 的 Major ID。
 *
 * <p>武器那一筆在 {@code gear-weapon.json} 裡 {@code dst} 是空的，而空譯文
 * 根本不會載入，於是沒有任何地方知道「Guardian 是裝備名」，Major ID 的譯名
 * 就順理成章地蓋上去了。
 *
 * <p>全庫掃過去這種撞名有 <b>50 組</b>：{@code Curse}、{@code Fissure}、
 * {@code Plague}、{@code Momentum}⋯⋯所以不能靠一條一條排除。
 *
 * <h2>這裡釘住什麼</h2>
 * 兩個方向。裝備名沒有自己的譯文時擋下來；<b>有</b>自己的譯文時照常翻——
 * 那是翻譯團隊刻意翻的。只測前者的話，把裝備名全部擋死也會過。
 */
public final class GearNameTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-gear");

        // 裝備檔：一個沒翻、一個翻好了。gearNames 旗標跟出貨的檔案一致。
        Files.writeString(dir.resolve("gear-weapon.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "a1": {"src": "Guardian", "dst": "", "role": "name"},
                   "a2": {"src": "Idol", "dst": "神像", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);

        // 別的領域剛好有同名的詞。
        Files.writeString(dir.resolve("major-id.json"),
                "{\"Guardian\": \"守護者\", \"Idol\": \"偶像\"}", StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        check("★ 沒翻的裝備名不會被別的領域頂替（實際 "
                      + store.lookup("Guardian") + "）",
              store.lookup("Guardian") == null);

        // 反方向：裝備自己翻好的照常用，而且用的是<b>裝備的</b>譯名。
        String idol = store.lookup("Idol");
        check("翻好的裝備名照常翻（實際 " + idol + "）",
              "神像".equals(idol));

        // 沒撞名的一般詞不受影響。
        Files.writeString(dir.resolve("misc.json"),
                "{\"Fissure\": \"地裂\"}", StandardCharsets.UTF_8);
        store = new TranslationStore();
        store.loadAll(dir);
        check("不是裝備名的照常翻（實際 " + store.lookup("Fissure") + "）",
              "地裂".equals(store.lookup("Fissure")));

        report();
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            System.out.println("裝備名：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("裝備名：全部通過");
    }
}
