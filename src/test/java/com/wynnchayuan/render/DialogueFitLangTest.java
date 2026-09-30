package com.wynnchayuan.render;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 每一種語言各有多少台詞塞不進對話框。
 *
 * <h2>為什麼要分語言量</h2>
 * {@link DialogueFitTest} 只量繁中，而繁中一個字抵兩三個英文字母，標準框寬下
 * <b>一條都不會塞不下</b>。於是「譯文太長整句掉回英文」這個回報看起來不存在。
 *
 * <p>但回報的人（yool141，#902）翻的是<b>日文</b>：假名一個字跟漢字一樣寬，
 * 一句話的字數卻跟英文差不多，長度差了一截。俄文的單字又不能從中間切。
 * 所以要分語言量，才知道這件事到底有多大。
 *
 * <h2>這支不當成門檻</h2>
 * 塞不下的數量會隨譯文措辭浮動，訂一個上限只會在別人潤稿時無故變紅。
 * 這裡只印出來，讓人看得到比例；唯一的斷言是「每一種語言都讀得到語料」。
 */
public final class DialogueFitLangTest {

    private static int failures = 0;

    /** 標準框寬，以及有頭像時比較窄的那一種。 */
    private static final int[] WIDTHS = {232, 208};

    private static final String[] LANGS =
            {"zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es"};

    public static void main(String[] args) throws Exception {
        DialogueRewriter.widthForTest = DialogueFitLangTest::realWidth;
        System.out.printf("%-8s %8s %10s %10s%n",
                "語言", "可比對", "232 塞不下", "208 塞不下");
        Map<String, int[]> table = new LinkedHashMap<>();
        for (String lang : LANGS) {
            Path file = Path.of("src/main/resources/assets/wynnchayuan/translations",
                    lang, "quest-dialogue.json");
            List<String[]> rows = new ArrayList<>();
            collect(file, rows);
            check(lang + " 讀得到語料（" + rows.size() + " 條）", rows.size() > 1000);
            int[] counts = new int[WIDTHS.length + 1];
            for (String[] pair : rows) {
                String src = pair[0];
                String dst = pair[1];
                if (src.isEmpty() || dst.isEmpty()
                        || src.contains("{") || dst.contains("{")) {
                    continue;                  // 佔位符模擬不出來
                }
                counts[0]++;
                int need = rowsFor(src);
                for (int w = 0; w < WIDTHS.length; w++) {
                    if (DialogueRewriter.wrap(dst, need, null, WIDTHS[w]) == null) {
                        counts[w + 1]++;
                    }
                }
            }
            table.put(lang, counts);
            System.out.printf("%-8s %8d %10d %10d%n",
                    lang, counts[0], counts[1], counts[2]);
        }

        System.out.println(failures == 0 ? "\n各語言塞得下：全部通過"
                                         : "\n各語言塞得下：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** 原文佔幾行：照對話框標準寬度折一次。 */
    private static int rowsFor(String src) {
        List<String> laid = DialogueRewriter.wrap(src, 5, null, 232);
        if (laid == null) {
            return 5;
        }
        int used = 0;
        for (String row : laid) {
            if (!row.isBlank()) {
                used++;
            }
        }
        return Math.max(1, used);
    }

    /** 中日韓一個字 10px，其餘照 Wynncraft 的對話字型算 6px。 */
    private static int realWidth(String text) {
        int w = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            w += c >= 0x1100 && c <= 0xFFDC ? 10 : 6;
        }
        return w;
    }

    private static void collect(Path file, List<String[]> out) throws IOException {
        if (!Files.exists(file)) {
            return;
        }
        String text = Files.readString(file, StandardCharsets.UTF_8);
        int at = 0;
        while (true) {
            int s = text.indexOf("\"src\":", at);
            if (s < 0) {
                return;
            }
            int d = text.indexOf("\"dst\":", s);
            if (d < 0) {
                return;
            }
            String src = jsonString(text, text.indexOf('"', s + 6));
            String dst = jsonString(text, text.indexOf('"', d + 6));
            out.add(new String[] {src, dst});
            at = d + 6;
        }
    }

    /** 從開頭的引號讀一個 JSON 字串，處理跳脫。 */
    private static String jsonString(String text, int quote) {
        StringBuilder out = new StringBuilder();
        for (int i = quote + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"') {
                return out.toString();
            }
            if (c != '\\') {
                out.append(c);
                continue;
            }
            char next = text.charAt(++i);
            switch (next) {
                case 'n' -> out.append('\n');
                case 't' -> out.append('\t');
                case 'u' -> {
                    out.append((char) Integer.parseInt(text.substring(i + 1, i + 5), 16));
                    i += 4;
                }
                default -> out.append(next);
            }
        }
        return out.toString();
    }

    private static void check(String what, boolean ok) {
        if (!ok) {
            System.out.println("  [FAIL] " + what);
            failures++;
        }
    }
}
