package com.wynnchayuan.render;

import java.util.List;

/**
 * 譯文裡手寫的換行在就地取代模式下要攤平。issue #1298。
 *
 * <p>小框模式會照 {@code \n} 換行，於是有人在譯文裡寫了 {@code \n}；
 * 同一句在就地取代模式下卻畫成亂碼——遊戲的對話框是動作列上的一行字，
 * {@code \n} 不是換行，是一個字型裡沒有的字。列數由 wrap 照寬度重分，
 * 所以換行只要變成一般的字距就好。
 */
public final class DialogueNewlineTest {

    private static int failures = 0;

    public static void main(String[] args) {
        check("沒有換行就原樣",
                DialogueRewriter.oneLine("Merhaba, {u}!").equals("Merhaba, {u}!"));
        check("拉丁字之間換成一個空白",
                DialogueRewriter.oneLine("Bu çok uzun\nbir cümle.").equals("Bu çok uzun bir cümle."));
        check("換行旁邊已經有空白就不再加",
                DialogueRewriter.oneLine("Bu çok uzun \nbir cümle.").equals("Bu çok uzun bir cümle."));
        check("中文字之間直接接上",
                DialogueRewriter.oneLine("這句話很長\n所以換行了").equals("這句話很長所以換行了"));
        check("\\r\\n 也一樣",
                DialogueRewriter.oneLine("один\r\nдва").equals("один два"));
        check("連續兩個換行不會變成兩個空白",
                DialogueRewriter.oneLine("A\n\nB").equals("A B"));

        // 攤平之後照樣攤得進原本的列數，而且沒有字被吃掉
        String flat = DialogueRewriter.oneLine("Bu çok uzun bir cümle\nve ikinci satır burada.");
        List<String> rows = DialogueRewriter.wrap(flat, 2, null, 150);
        check("攤平的譯文攤得進兩列",
                rows != null && rows.stream().noneMatch(r -> r.indexOf('\n') >= 0));
        check("沒有漏字",
                rows != null && String.join(" ", rows).replace("  ", " ").strip().equals(flat));

        System.out.println(failures == 0 ? "對話換行：全部通過"
                                         : "對話換行：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
