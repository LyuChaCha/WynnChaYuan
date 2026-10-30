package com.wynnchayuan.translate;

import com.wynnchayuan.translate.TranslationUpdate.State;

public final class CorpusVersionTest {

    private static int failures = 0;

    private static final String LOCAL = "1e7245081234567890abcdef";
    private static final String NEWER = "2739a9ce1234567890abcdef";

    public static void main(String[] args) {
        eq("本機檔案不比版本", State.LOCAL,
                TranslationUpdate.verdict(false, false, true, LOCAL, NEWER));

        eq("查詢中", State.CHECKING,
                TranslationUpdate.verdict(true, true, false, LOCAL, null));
        eq("查詢中優先於已經問到的舊答案", State.CHECKING,
                TranslationUpdate.verdict(true, true, true, LOCAL, LOCAL));

        eq("同一個 commit 是最新", State.LATEST,
                TranslationUpdate.verdict(true, false, true, LOCAL, LOCAL));
        eq("遠端比較新", State.BEHIND,
                TranslationUpdate.verdict(true, false, true, LOCAL, NEWER));

        eq("沒問到就是不確定", State.UNKNOWN,
                TranslationUpdate.verdict(true, false, false, LOCAL, null));
        eq("問到了但回傳空字串也是不確定", State.UNKNOWN,
                TranslationUpdate.verdict(true, false, true, LOCAL, ""));
        eq("asked 是 true 但 remote 是 null（不該發生，照樣不猜）", State.UNKNOWN,
                TranslationUpdate.verdict(true, false, true, LOCAL, null));

        eq("沒同步過不能說最新", State.UNKNOWN,
                TranslationUpdate.verdict(true, false, true, "", NEWER));
        eq("沒同步過（null）不能說最新", State.UNKNOWN,
                TranslationUpdate.verdict(true, false, true, null, NEWER));
        eq("沒同步過，而且遠端剛好跟空字串比不出來", State.UNKNOWN,
                TranslationUpdate.verdict(true, false, true, "  ", NEWER));

        labels();

        System.out.println(failures == 0
                ? "譯文版本狀態：全部通過"
                : "譯文版本狀態：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void labels() {
        String date = "2026-09-30";
        same("有日期就印日期", date,
                TranslationUpdate.label(LOCAL, date, "內建"));
        same("沒同步過說內建，日期有沒有都一樣", "內建",
                TranslationUpdate.label("", date, "內建"));
        same("沒同步過（null）也說內建", "內建",
                TranslationUpdate.label(null, null, "內建"));
        same("空白的日期不算有日期，退回短碼", "1e72450",
                TranslationUpdate.label(LOCAL, "   ", "內建"));
        same("舊版升上來（日期是 null）退回短碼", "1e72450",
                TranslationUpdate.label(LOCAL, null, "內建"));

        String shown = TranslationUpdate.label(LOCAL, date, "內建");
        boolean clean = !shown.startsWith(LOCAL.substring(0, 7));
        System.out.println("  [" + (clean ? "PASS" : "FAIL")
                + "] ★ 有日期時畫面上沒有 SHA（實際：" + shown + "）");
        if (!clean) {
            failures++;
        }
    }

    private static void same(String what, String want, String got) {
        boolean ok = want.equals(got);
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what
                + "（要 " + want + "，實際 " + got + "）");
        if (!ok) {
            failures++;
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
