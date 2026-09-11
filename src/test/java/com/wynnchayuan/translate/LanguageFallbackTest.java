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

    /**
     * 一條<b>夠長</b>、兩種語言都翻好而且翻得不一樣的條目。
     *
     * <p>要夠長才會進 flat 索引（{@code MIN_FLAT_LENGTH} 是 24）。
     * 「地」與「土」的差別剛好一眼看得出是哪一層勝出。
     */
    private static final String LONG = "Earth Main Attack Damage:";

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

        // ---- 輔助索引也要照同一個順序 ----
        //
        // 主查表（entries）是「後載入的蓋前面的」，所以簡體勝出。但長句還有
        // 另一條路：畫面會把長句自動斷行，查表前要先把幾行併回一段，
        // 那條路走的是 flat 索引（見 lookupFlat）。
        //
        // 而 flat 用的是 putIfAbsent——<b>先寫的贏</b>。疊層時繁體先載入，
        // 於是每一條長句都被繁體先佔走，簡體那一份永遠寫不進去。
        // 實機的症狀是「簡體明明翻好了，畫面上卻是繁體」，而且<b>只發生在長句</b>，
        // 短標籤完全正常——因為短的走 entries，長的走 flat。
        //
        // 層內先到先贏是刻意的（同一層裡撞鍵時，排前面的檔案勝出）；
        // 要改的是<b>跨層</b>：後面那一層必須蓋得掉前面那一層。
        check("★ 長句也要簡體勝出（flat 索引，拿到 "
                        + both.lookupFlat(LONG) + "）",
                "土属性普攻伤害:".equals(both.lookupFlat(LONG)));
        check("順序反過來時長句換繁體勝出（拿到 "
                        + flipped.lookupFlat(LONG) + "）",
                "地屬性普攻傷害:".equals(flipped.lookupFlat(LONG)));
        check("長句在單層時本來就查得到",
                "土属性普攻伤害:".equals(cn.lookupFlat(LONG)));

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
