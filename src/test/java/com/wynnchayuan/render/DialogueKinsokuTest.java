package com.wynnchayuan.render;

import java.util.List;

public final class DialogueKinsokuTest {

    private static int failures = 0;

    private static final String FERNDOR =
            "多年前被一個海盜從我們這裡偷走了！你要是能把 [Abysso Galoshes] 帶來幫我們，我可以給你報酬！";

    private static final String OPENING = "([{<「『【《（";

    private static final String CLOSING = ")]}>」』】》），。、；：！？";

    public static void main(String[] args) {
        String text = "把 [Abysso Galoshes] 帶來";
        int bracket = text.indexOf('[');
        check("★ 斷在 [ 後面時，[ 跟著下一行走",
                DialogueRewriter.kinsoku(text, 0, bracket + 1) == bracket);
        check("★ 斷在 ] 前面時，把前一個字一起帶下去",
                DialogueRewriter.kinsoku(text, 0, text.indexOf(']'))
                        == text.indexOf(']') - 1);
        check("沒有頭尾問題的斷點不動",
                DialogueRewriter.kinsoku(text, 0, bracket) == bracket);
        check("挪到整行變空時就不挪",
                DialogueRewriter.kinsoku("[abc", 0, 1) == 1);
        check("★ 最多只往前挪 3 個字元，不會為了避頭尾把整行搬空",
                DialogueRewriter.kinsoku("中文字」』）】", 0, 6) == 3);

        int bad = 0;
        int checked = 0;
        for (int limit = 60; limit <= 360; limit += 6) {
            List<String> rows = DialogueRewriter.wrap(FERNDOR, 40, null, limit);
            if (rows == null) {
                continue;
            }
            checked++;
            for (String row : rows) {
                if (row.isEmpty()) {
                    continue;
                }
                if (OPENING.indexOf(row.charAt(row.length() - 1)) >= 0) {
                    bad++;
                    System.out.println("    行尾留了開括號：" + row);
                }
                if (CLOSING.indexOf(row.charAt(0)) >= 0) {
                    bad++;
                    System.out.println("    行首是閉括號或標點：" + row);
                }
            }
        }
        check("掃過 " + checked + " 種寬度都有量到", checked > 30);
        check("★ 沒有一行的結尾是開括號、開頭是閉括號（" + bad + " 處）", bad == 0);

        List<String> rows = DialogueRewriter.wrap(FERNDOR, 40, null, 120);
        check("斷行沒有漏字",
                rows != null && String.join("", rows).replace(" ", "")
                        .equals(FERNDOR.replace(" ", "")));

        System.out.println(failures == 0 ? "對話避頭尾：全部通過"
                                         : "對話避頭尾：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
