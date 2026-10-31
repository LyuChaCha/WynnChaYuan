package com.wynnchayuan.render;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class DialogueFitTest {

    private static final int WIDTH = 232;

    private static final int[] WIDTHS = {232, 220, 200, 180, 160};

    public static void main(String[] args) throws Exception {
        dialogueNewline();
        dialogueKinsoku();
        Path base = Path.of("src/main/resources/assets/wynnchayuan/translations/zh_tw");
        List<String[]> rows = new ArrayList<>();
        collect(base.resolve("quest-dialogue.json"), rows);
        DialogueLayout.widthForTest = DialogueFitTest::realWidth;

        check("語料讀得到（" + rows.size() + " 條）", rows.size() > 1000);
        System.out.printf("%n%8s %8s %10s %10s %8s%n",
                "框寬", "可比對", "有避頭尾", "沒避頭尾", "退回去的");
        for (int width : WIDTHS) {
            sweep(rows, width);
        }

        System.out.println(failures == 0 ? "\n對話塞得下：全部通過"
                                         : "\n對話塞得下：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void sweep(List<String[]> rows, int width) {
        int total = 0;
        int strict = 0;
        int relaxed = 0;
        int actual = 0;
        String worst = null;
        for (String[] pair : rows) {
            String src = pair[0];
            String dst = pair[1];
            if (src.isEmpty() || dst.isEmpty() || src.contains("{") || dst.contains("{")) {
                continue;
            }
            total++;
            int need = rowsFor(src);
            boolean withK = DialogueLayout.wrap(dst, need, null, width, true) != null;
            boolean withoutK = DialogueLayout.wrap(dst, need, null, width, false) != null;
            boolean now = DialogueLayout.wrap(dst, need, null, width) != null;
            if (!withK) {
                strict++;
            }
            if (!withoutK) {
                relaxed++;
            }
            if (!now) {
                actual++;
                if (worst == null && withoutK) {
                    worst = src + "\n      → " + dst;
                }
            }
        }
        System.out.printf("%8d %8d %10d %10d %8d%n",
                width, total, strict, relaxed, strict - actual);
        check("★ 框寬 " + width + "：不避頭尾攤得進去的，避頭尾之後也要攤得進去"
              + (worst == null ? "" : "（例如 " + worst + "）"),
              actual <= relaxed);
    }

    private static int realWidth(String text) {
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            w += (c >= 0x2E80 && c <= 0xFFEF) ? 10 : 6;
        }
        return w;
    }

    private static int rowsFor(String raw) {
        for (int n = 1; n <= 5; n++) {
            if (DialogueLayout.wrap(raw, n, null, WIDTH) != null) {
                return n;
            }
        }
        return 5;
    }

    private static void collect(Path file, List<String[]> out) throws IOException {
        if (!Files.exists(file)) {
            return;
        }
        String src = null;
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String t = line.strip();
            if (t.startsWith("\"src\":")) {
                src = value(t);
            } else if (t.startsWith("\"dst\":") && src != null) {
                out.add(new String[] {src, value(t)});
                src = null;
            }
        }
    }

    private static String value(String line) {
        int a = line.indexOf('"', line.indexOf(':'));
        int b = line.lastIndexOf('"');
        if (a < 0 || b <= a) {
            return "";
        }
        return line.substring(a + 1, b)
                .replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static void dialogueNewline() throws Exception {
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
    }

    private static void dialogueKinsoku() throws Exception {
        String text = "把 [Abysso Galoshes] 帶來";
        int bracket = text.indexOf('[');
        check("★ 斷在 [ 後面時，[ 跟著下一行走",
                DialogueLayout.kinsoku(text, 0, bracket + 1) == bracket);
        check("★ 斷在 ] 前面時，把前一個字一起帶下去",
                DialogueLayout.kinsoku(text, 0, text.indexOf(']'))
                        == text.indexOf(']') - 1);
        check("沒有頭尾問題的斷點不動",
                DialogueLayout.kinsoku(text, 0, bracket) == bracket);
        check("挪到整行變空時就不挪",
                DialogueLayout.kinsoku("[abc", 0, 1) == 1);
        check("★ 最多只往前挪 3 個字元，不會為了避頭尾把整行搬空",
                DialogueLayout.kinsoku("中文字」』）】", 0, 6) == 3);

        int bad = 0;
        int checked = 0;
        for (int limit = 60; limit <= 360; limit += 6) {
            List<String> rows = DialogueLayout.wrap(FERNDOR, 40, null, limit);
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

        List<String> rows = DialogueLayout.wrap(FERNDOR, 40, null, 120);
        check("斷行沒有漏字",
                rows != null && String.join("", rows).replace(" ", "")
                        .equals(FERNDOR.replace(" ", "")));
    }

    private static final String FERNDOR =
            "多年前被一個海盜從我們這裡偷走了！你要是能把 [Abysso Galoshes] 帶來幫我們，我可以給你報酬！";

    private static final String OPENING = "([{<「『【《（";

    private static final String CLOSING = ")]}>」』】》），。、；：！？";
}
