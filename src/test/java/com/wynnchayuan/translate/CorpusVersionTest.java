package com.wynnchayuan.translate;

import com.wynnchayuan.translate.TranslationUpdate.State;

/**
 * F6 的「譯文版本」那一列該說什麼。
 *
 * <h2>為什麼值得一支測試</h2>
 * 使用者的回報是「我已經更新到最新版了，但還是有沒翻到的（Halcyon）」。
 * 模組版本看得到，<b>譯文版本看不到</b>——而譯文是從 GitHub 同步的，跟模組
 * 版本無關：jar 是最新的，譯文卻可能是兩週前那一份。
 *
 * <p>這一列就是把那件事講出來。而它每一個分支都有「說錯話」的後果，
 * 最貴的一種是<b>把「問不到」講成「最新」</b>：那等於在玩家手上明明是舊譯文的
 * 時候告訴他沒事，於是他繼續回報「翻譯有問題」，而我們繼續查不到原因。
 *
 * <p>這種錯在實機上只表現成「畫面看起來沒事」，玩遊戲抓不到。
 */
public final class CorpusVersionTest {

    private static int failures = 0;

    private static final String LOCAL = "1e7245081234567890abcdef";
    private static final String NEWER = "2739a9ce1234567890abcdef";

    public static void main(String[] args) {
        // 來源設成本機檔案：沒有「最新」可以比，不該假裝有
        eq("本機檔案不比版本", State.LOCAL,
                TranslationUpdate.verdict(false, false, true, LOCAL, NEWER));

        // 正在問的時候就說正在問，別先給一個等一下會變的答案
        eq("查詢中", State.CHECKING,
                TranslationUpdate.verdict(true, true, false, LOCAL, null));
        eq("查詢中優先於已經問到的舊答案", State.CHECKING,
                TranslationUpdate.verdict(true, true, true, LOCAL, LOCAL));

        // 兩邊同一個 commit 才叫最新
        eq("同一個 commit 是最新", State.LATEST,
                TranslationUpdate.verdict(true, false, true, LOCAL, LOCAL));
        eq("遠端比較新", State.BEHIND,
                TranslationUpdate.verdict(true, false, true, LOCAL, NEWER));

        // ★ 最貴的那一個：問不到不能說最新
        eq("沒問到就是不確定", State.UNKNOWN,
                TranslationUpdate.verdict(true, false, false, LOCAL, null));
        eq("問到了但回傳空字串也是不確定", State.UNKNOWN,
                TranslationUpdate.verdict(true, false, true, LOCAL, ""));
        eq("asked 是 true 但 remote 是 null（不該發生，照樣不猜）", State.UNKNOWN,
                TranslationUpdate.verdict(true, false, true, LOCAL, null));

        // ★ 從來沒同步過的人手上是 jar 內建那一份。它對應哪一個 commit
        //   沒有記錄，所以即使問到了遠端版本，也不能說「最新」——
        //   內建那份<b>正是</b>最可能舊的一份（發版之後語料又改了好幾次）。
        eq("沒同步過不能說最新", State.UNKNOWN,
                TranslationUpdate.verdict(true, false, true, "", NEWER));
        eq("沒同步過（null）不能說最新", State.UNKNOWN,
                TranslationUpdate.verdict(true, false, true, null, NEWER));
        eq("沒同步過，而且遠端剛好跟空字串比不出來", State.UNKNOWN,
                TranslationUpdate.verdict(true, false, true, "  ", NEWER));

        System.out.println(failures == 0
                ? "譯文版本狀態：全部通過"
                : "譯文版本狀態：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void eq(String what, State want, State got) {
        boolean ok = want == got;
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what
                + "（要 " + want + "，實際 " + got + "）");
        if (!ok) {
            failures++;
        }
    }
}
