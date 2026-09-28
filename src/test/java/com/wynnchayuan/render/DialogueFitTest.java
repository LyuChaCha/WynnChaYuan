package com.wynnchayuan.render;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 避頭尾不可以害整句掉回英文。
 *
 * <h2>實機回報（issue #864）</h2>
 * 「譯文比原文長的時候，英文全部打完的那一刻，中文就消失、變回英文。」
 *
 * <p>成因在 {@code DialogueRewriter.line} 最後那一關：講完了就拿<b>完整</b>譯文
 * 去問 {@link DialogueRewriter#wrap}，攤不進原文佔的行數就 {@code drop()}。
 * 而打字打到一半時只有譯文的前面一截，那一截塞得下——所以中文會一路正常，
 * 直到最後一幀才整句跳掉。
 *
 * <p>行數不能多要：那幾行是 Wynncraft 自己送來的文字實體，我們只換內容。
 * 所以真正能做的是<b>別讓自己的排版把空間吃掉</b>。0.2.4 加的避頭尾
 * （{@link DialogueRewriter#kinsoku}）會把斷點往前挪，每一行因此少放幾個字——
 * 排版好看是加分，整句變英文是減分。
 *
 * <h2>這支釘住什麼</h2>
 * 拿<b>真的語料</b>（兩萬多句任務台詞）跑：只要不做避頭尾攤得進去，
 * 做了避頭尾的那一支就<b>也必須</b>攤得進去——也就是 {@code wrap} 得自己退回去。
 *
 * <h2>字寬</h2>
 * 測試裡沒有 Minecraft，{@code DialogueRewriter.width} 會退回「一個字元 6px」，
 * 而中日韓實機是一個字 10px。不修正的話，譯文裡留著的英文名字
 * （{@code Corrupter of World Cave}）會被當成中文那樣寬，量出一堆假的「塞不下」。
 * 所以掛一個真的字寬進去（{@link DialogueRewriter#widthForTest}）。
 */
public final class DialogueFitTest {

    private static int failures = 0;

    /** 對話框一行放得下的像素，跟 DialogueTypingTest 同一個數字。 */
    private static final int WIDTH = 232;

    /**
     * 也要量比較窄的框。
     *
     * <p>有頭像的對話文字是從頭像右邊起算的，可用寬度少了一截
     *（見 {@code DialogueRewriter.rowWidth}）。標準寬度下整份語料都塞得下，
     * 窄的時候才看得出差別——而回報的人遇到的正是塞不下的那種。
     */
    private static final int[] WIDTHS = {232, 220, 200, 180, 160};

    public static void main(String[] args) throws Exception {
        Path base = Path.of("src/main/resources/assets/wynnchayuan/translations/zh_tw");
        List<String[]> rows = new ArrayList<>();
        collect(base.resolve("quest-dialogue.json"), rows);
        DialogueRewriter.widthForTest = DialogueFitTest::realWidth;

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
        int strict = 0;       // 一律避頭尾（0.2.4 的行為）攤不進去的
        int relaxed = 0;      // 不做避頭尾攤不進去的
        int actual = 0;       // 現在這一支（塞不下會自己退回去）攤不進去的
        String worst = null;
        for (String[] pair : rows) {
            String src = pair[0];
            String dst = pair[1];
            if (src.isEmpty() || dst.isEmpty() || src.contains("{") || dst.contains("{")) {
                continue;                      // 佔位符模擬不出來
            }
            total++;
            int need = rowsFor(src);
            boolean withK = DialogueRewriter.wrap(dst, need, null, width, true) != null;
            boolean withoutK = DialogueRewriter.wrap(dst, need, null, width, false) != null;
            boolean now = DialogueRewriter.wrap(dst, need, null, width) != null;
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

    /** 中日韓一個字 10px，其餘照 Wynncraft 的對話字型算 6px。 */
    private static int realWidth(String text) {
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            w += (c >= 0x2E80 && c <= 0xFFEF) ? 10 : 6;
        }
        return w;
    }

    /** 原文在對話框裡佔幾行。 */
    private static int rowsFor(String raw) {
        for (int n = 1; n <= 5; n++) {
            if (DialogueRewriter.wrap(raw, n, null, WIDTH) != null) {
                return n;
            }
        }
        return 5;
    }

    /** 從合併檔裡撈 src/dst。格式固定，不必為了這支引 JSON 函式庫。 */
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
