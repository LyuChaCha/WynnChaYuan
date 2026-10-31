package com.wynnchayuan.render;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class DialogueFitLangTest {

    private static final int[] WIDTHS = {232, 208};

    private static final String[] LANGS =
            {"zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr"};

    public static void main(String[] args) throws Exception {
        DialogueLayout.widthForTest = DialogueFitLangTest::realWidth;
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
                    continue;
                }
                counts[0]++;
                int need = rowsFor(src);
                for (int w = 0; w < WIDTHS.length; w++) {
                    if (DialogueLayout.wrap(dst, need, null, WIDTHS[w]) == null) {
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

    private static int rowsFor(String src) {
        List<String> laid = DialogueLayout.wrap(src, 5, null, 232);
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

}
