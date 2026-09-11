package com.wynnchayuan.translate;

import java.nio.file.Path;
import java.util.List;

/**
 * 簡體中文沒翻到的地方，要看到<b>繁體</b>而不是英文。
 *
 * <h2>先前壞在哪</h2>
 * 啟動流程原本是連呼叫兩次 {@code loadAll}：
 *
 * <pre>
 *   translations.loadAll(fallback);   // 繁體
 *   translations.loadAll(trDir);      // 簡體
 * </pre>
 *
 * 而 {@code loadAll} 開頭就 {@code entries.clear()}——第二次把第一次載入的整片
 * 清掉了。實測疊完只剩 449 條（簡體自己那些），繁體那三萬多條一條都沒留下。
 *
 * <p>畫面上的後果是：簡中玩家在還沒翻到的地方看到<b>英文</b>。而那段程式碼
 * 自己的註解寫得很清楚，墊底就是為了避免這件事——
 * 「多一種語言反而害了他」。它從來沒有生效過。
 *
 * <p>這一條測試釘的就是「疊完之後，底下那一層還在」。
 */
public final class LanguageFallbackTest {

    private static int failures = 0;

    private static final Path ROOT =
            Path.of("src/main/resources/assets/wynnchayuan/translations");

    /** 一句只有繁體有、簡體還沒翻的台詞。 */
    private static final String ONLY_TW =
            "Good luck in there, recruits! You're gonna need it...";

    /** 一條簡體已經翻好的介面標籤。 */
    private static final String BOTH = "Combat Level";

    public static void main(String[] args) {
        TranslationStore tw = new TranslationStore();
        tw.loadAll(ROOT.resolve("zh_tw"));
        int alone = tw.size();
        check("繁體本來就有那一句（" + alone + " 條）", tw.lookup(ONLY_TW) != null);

        TranslationStore cn = new TranslationStore();
        cn.loadAll(ROOT.resolve("zh_cn"));
        check("簡體還沒翻那一句", cn.lookup(ONLY_TW) == null);
        check("簡體翻好了介面標籤", "战斗等级".equals(cn.lookup(BOTH)));

        // ---- 疊起來 ----
        TranslationStore both = new TranslationStore();
        both.loadAll(List.of(ROOT.resolve("zh_tw"), ROOT.resolve("zh_cn")));

        check("★ 疊完之後，繁體那一層還在（先前這裡是 null）",
                "祝你們好運，新兵們！你們會需要的……".equals(both.lookup(ONLY_TW)));
        check("★ 簡體蓋過繁體，不是反過來（拿到 "
                        + both.lookup(BOTH) + "）",
                "战斗等级".equals(both.lookup(BOTH)));
        check("疊完的條目數應該接近繁體那一層（疊完 " + both.size()
                        + "、繁體 " + alone + "）",
                both.size() >= alone);

        // 順序反過來就該是繁體勝出——證明「後面的蓋前面的」不是碰巧
        TranslationStore flipped = new TranslationStore();
        flipped.loadAll(List.of(ROOT.resolve("zh_cn"), ROOT.resolve("zh_tw")));
        check("順序反過來就換繁體勝出（拿到 " + flipped.lookup(BOTH) + "）",
                "戰鬥等級".equals(flipped.lookup(BOTH)));

        // 只有一層時行為不變
        TranslationStore one = new TranslationStore();
        one.loadAll(List.of(ROOT.resolve("zh_tw")));
        check("單層的結果跟舊的單參數版本一樣", one.size() == alone);

        report();
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        System.out.println(failures == 0
                ? "LanguageFallback: 全部通過"
                : "LanguageFallback: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
