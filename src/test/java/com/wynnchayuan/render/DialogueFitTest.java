package com.wynnchayuan.render;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DialogueFitTest {

    private static int failures = 0;

    private static final int WIDTH = 232;

    private static final int[] WIDTHS = {232, 220, 200, 180, 160};

    public static void main(String[] args) throws Exception {
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

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
