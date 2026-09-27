package com.wynnchayuan.render;

import java.util.List;

/**
 * 對話框的避頭尾：成對的東西不要被拆在兩行。
 *
 * <h2>實機回報</h2>
 * Ferndor 那句話原本長這樣（截圖）：
 *
 * <pre>
 *   多年前被一個海盜從我們這裡偷走了！你要是能把 [
 *   Abysso Galoshes] 帶來幫我們，我可以給你報酬！
 * </pre>
 *
 * 斷字邏輯只管「英文單字不要從中間切」，所以它退到了 {@code A}——
 * {@code [} 不是字母，就被留在上一行的結尾。方括號在 Wynncraft 裡是
 * 「這是一件東西」的記號，跟名字是一組的，不該自己孤零零留在行尾。
 *
 * <h2>測試怎麼量寬度</h2>
 * 測試裡沒有 Minecraft 實例，{@code DialogueRewriter.width} 會退回
 * 「一個字元 6px」。所以 {@code limit} 除以 6 就是一行放得下幾個字元，
 * 掃一整排 limit 等於把斷點掃過整句話的每一個位置。
 */
public final class DialogueKinsokuTest {

    private static int failures = 0;

    /** 截圖那一句。前面的「多年前⋯⋯」是上一句的結尾，跟著一起送進來的。 */
    private static final String FERNDOR =
            "多年前被一個海盜從我們這裡偷走了！你要是能把 [Abysso Galoshes] 帶來幫我們，我可以給你報酬！";

    private static final String OPENING = "([{<「『【《（";

    private static final String CLOSING = ")]}>」』】》），。、；：！？";

    public static void main(String[] args) {
        // 純函式的部分：不需要量寬度，直接看斷點挪到哪裡
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

        // 整句掃過去：每一個斷點位置都不該留下孤零零的括號
        int bad = 0;
        int checked = 0;
        for (int limit = 60; limit <= 360; limit += 6) {
            List<String> rows = DialogueRewriter.wrap(FERNDOR, 40, null, limit);
            if (rows == null) {
                continue;                      // 這個寬度攤不進 40 行，跳過
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

        // 挪斷點不能把字吃掉
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
