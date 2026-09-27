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

    /**
     * 一句只有繁體有、簡體還沒翻的台詞。
     *
     * <p>從語料<b>當場挑</b>，不寫死：簡體一直在補，寫死的那一句遲早會被翻掉，
     * 測試就跟著紅（King's Recruit 那一句就是這樣被 #679 翻掉的）。
     */
    static String pickOnlyTw(TranslationStore tw, TranslationStore cn) {
        for (String key : new java.util.TreeSet<>(tw.sourceKeys())) {
            if (key.length() >= 20 && Character.isLetter(key.charAt(0)) && key.indexOf(' ') > 0
                    && key.indexOf('{') < 0 && key.indexOf('\n') < 0
                    && tw.lookup(key) != null && !cn.hasTranslation(key)) {
                return key;
            }
        }
        return null;
    }

    /** 一條簡體已經翻好的介面標籤。 */
    private static final String BOTH = "Combat Level";

    /**
     * 一條<b>夠長</b>、兩種語言都翻好而且翻得不一樣的條目。
     *
     * <p>要夠長才會進 flat 索引（{@code MIN_FLAT_LENGTH} 是 24）。
     *
     * <p>期望值<b>不寫死</b>，當場跟兩個單層 store 問。先前寫死的是
     * 「土属性普攻伤害:」，語料後來把用詞統一成「地」，測試就紅了——
     * 而壞掉的是措辭，不是疊層。同一個坑 {@link #pickOnlyTw} 上面那段
     * 註解也記過一次。
     */
    private static final String LONG = "Earth Main Attack Damage:";

    public static void main(String[] args) throws Exception {
        TranslationStore tw = new TranslationStore();
        tw.loadAll(ROOT.resolve("zh_tw"));
        int alone = tw.size();

        Path cnDir = ROOT.resolve("zh_cn");
        TranslationStore cn = new TranslationStore();
        cn.loadAll(cnDir);
        String onlyTw = pickOnlyTw(tw, cn);
        if (onlyTw == null) {
            // 簡體已經跟繁體一樣多，語料裡挑不出缺口——自己造一個，見 CorpusGap
            onlyTw = pickOnlyTw(tw, new TranslationStore());
            cnDir = com.wynnchayuan.CorpusGap.without(cnDir, onlyTw);
            cn = new TranslationStore();
            cn.loadAll(cnDir);
        }
        check("找得到一句繁體有、簡體沒翻的台詞（" + onlyTw + "）", onlyTw != null);
        String twLine = onlyTw == null ? null : tw.lookup(onlyTw);
        check("繁體本來就有那一句（" + alone + " 條）", twLine != null);
        check("簡體還沒翻那一句", onlyTw != null && cn.lookup(onlyTw) == null);
        // 期望值跟語料當場問，不寫死措辭。兩種語言必須<b>翻得不一樣</b>，
        // 否則下面那幾條「誰蓋過誰」的斷言等於沒在守。
        String twLabel = tw.lookup(BOTH);
        String cnLabel = cn.lookup(BOTH);
        String twLong = tw.lookupFlat(LONG);
        String cnLong = cn.lookupFlat(LONG);
        check("簡體翻好了介面標籤（" + cnLabel + "）", cnLabel != null);
        check("兩種語言的介面標籤翻得不一樣（" + twLabel + " / " + cnLabel + "）",
                twLabel != null && !twLabel.equals(cnLabel));
        check("兩種語言的長句翻得不一樣（" + twLong + " / " + cnLong + "）",
                twLong != null && cnLong != null && !twLong.equals(cnLong));

        // ---- 疊起來 ----
        TranslationStore both = new TranslationStore();
        both.loadAll(List.of(ROOT.resolve("zh_tw"), cnDir));

        check("★ 疊完之後，繁體那一層還在（先前這裡是 null）",
                twLine != null && twLine.equals(both.lookup(onlyTw)));
        check("★ 簡體蓋過繁體，不是反過來（拿到 "
                        + both.lookup(BOTH) + "）",
                cnLabel != null && cnLabel.equals(both.lookup(BOTH)));
        check("疊完的條目數應該接近繁體那一層（疊完 " + both.size()
                        + "、繁體 " + alone + "）",
                both.size() >= alone);

        // 順序反過來就該是繁體勝出——證明「後面的蓋前面的」不是碰巧
        TranslationStore flipped = new TranslationStore();
        flipped.loadAll(List.of(cnDir, ROOT.resolve("zh_tw")));
        check("順序反過來就換繁體勝出（拿到 " + flipped.lookup(BOTH) + "）",
                twLabel != null && twLabel.equals(flipped.lookup(BOTH)));

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
                cnLong != null && cnLong.equals(both.lookupFlat(LONG)));
        check("順序反過來時長句換繁體勝出（拿到 "
                        + flipped.lookupFlat(LONG) + "）",
                twLong != null && twLong.equals(flipped.lookupFlat(LONG)));
        check("長句在單層時本來就查得到（" + cnLong + "）", cnLong != null);

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
