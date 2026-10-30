package com.wynnchayuan.render;

import java.util.List;

public final class DialogueNewlineTest {

    private static int failures = 0;

    public static void main(String[] args) {
        check("沒有換行就原樣",
                DialogueTyping.oneLine("Merhaba, {u}!").equals("Merhaba, {u}!"));
        check("拉丁字之間換成一個空白",
                DialogueTyping.oneLine("Bu çok uzun\nbir cümle.").equals("Bu çok uzun bir cümle."));
        check("換行旁邊已經有空白就不再加",
                DialogueTyping.oneLine("Bu çok uzun \nbir cümle.").equals("Bu çok uzun bir cümle."));
        check("中文字之間直接接上",
                DialogueTyping.oneLine("這句話很長\n所以換行了").equals("這句話很長所以換行了"));
        check("\\r\\n 也一樣",
                DialogueTyping.oneLine("один\r\nдва").equals("один два"));
        check("連續兩個換行不會變成兩個空白",
                DialogueTyping.oneLine("A\n\nB").equals("A B"));

        String flat = DialogueTyping.oneLine("Bu çok uzun bir cümle\nve ikinci satır burada.");
        List<String> rows = DialogueLayout.wrap(flat, 2, null, 150);
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
