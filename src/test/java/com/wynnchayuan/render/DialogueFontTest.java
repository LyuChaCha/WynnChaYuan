package com.wynnchayuan.render;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class DialogueFontTest {

    private static final int DEFAULT_ASCENT = 7;

    private static final Map<String, Integer> ASCENT = new LinkedHashMap<>();

    static {
        ASCENT.put("body_0", 34);
        ASCENT.put("body_1", 22);
        ASCENT.put("body_2", 10);
        ASCENT.put("body_3", -2);
        ASCENT.put("body_4", -14);
        ASCENT.put("choice_0", 110);
        ASCENT.put("choice_1", 97);
        ASCENT.put("choice_2", 84);
        ASCENT.put("choice_3", 71);
        ASCENT.put("nameplate", 50);
        ASCENT.put("control", -38);
    }

    private static String[] langs() throws IOException {
        Path manifest = Path.of("src/main/resources/assets/wynnchayuan"
                + "/translations/_languages.json");
        String json = Files.readString(manifest, StandardCharsets.UTF_8);
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"languages\"\\s*:\\s*\\[([^]]*)]").matcher(json);
        if (!m.find()) {
            return new String[0];
        }
        java.util.List<String> out = new java.util.ArrayList<>();
        java.util.regex.Matcher one = java.util.regex.Pattern
                .compile("\"([a-z]{2}_[a-z]{2})\"").matcher(m.group(1));
        while (one.find()) {
            out.add(one.group(1));
        }
        return out.toArray(new String[0]);
    }

    private static final Path ROOT = Path.of(
            "src/main/resources/assets/wynnchayuan/font");

    private DialogueFontTest() {}

    public static void main(String[] args) throws IOException {
        System.out.println("=== 對話字型 ===");

        check("授權檔在（SIL OFL 隨模組散布必須附上）",
                Files.isRegularFile(ROOT.resolve("ofl-fusion.txt")));

        String[] langs = langs();
        check("讀得到會打包的語言：" + java.util.Arrays.toString(langs),
                langs.length > 0);

        for (String lang : langs) {
            Path cover = ROOT.resolve("dialogue").resolve(lang)
                    .resolve("coverage.txt");
            check(lang + " 的覆蓋率表在", Files.isRegularFile(cover));
            if (Files.isRegularFile(cover)) {
                long lines = Files.readAllLines(cover).stream()
                        .filter(l -> !l.isBlank() && !l.startsWith("#"))
                        .count();
                check(lang + " 覆蓋率表不是空的（" + lines + " 段）", lines > 100);
            }
            checkLanguage(lang);
        }

        System.out.println(failures == 0
                ? "DialogueFont: 全部通過"
                : "DialogueFont: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void checkLanguage(String lang) throws IOException {
        Path dir = ROOT.resolve("dialogue").resolve(lang);
        for (Map.Entry<String, Integer> row : ASCENT.entrySet()) {
            String name = row.getKey();
            Path file = dir.resolve(name + ".json");
            if (!Files.isRegularFile(file)) {
                check(name + ".json 存在", false);
                continue;
            }
            String json = Files.readString(file, StandardCharsets.UTF_8)
                    .replaceAll("\\s+", "");

            check(name + " 先參照 Wynncraft 自己的字型（ASCII 外觀與高度不能變）",
                    json.contains("\"type\":\"reference\""));
            java.util.regex.Matcher ref = java.util.regex.Pattern
                    .compile("\"file\":\"wynnchayuan:([^\"]+)\"").matcher(json);
            check(name + " 指名了一份中日韓 ttf", ref.find());
            if (ref.reset().find()) {
                check(name + " 指到的 " + ref.group(1) + " 真的在",
                        Files.isRegularFile(ROOT.resolve(ref.group(1))));
            }
            check(name + " size 是 10 的倍數（Fusion Pixel 的設計格）",
                    json.matches(".*\"size\":(10|20|30),.*"));
            check(name + " oversample 是 1（點陣字再降採樣只會更糊）",
                    json.contains("\"oversample\":1"));
            // MC 前面的 provider 優先，reference 要排在 ttf 前
            check(name + " reference 排在 ttf 前面",
                    json.indexOf("\"reference\"") < json.indexOf("\"ttf\""));

            int want = -(row.getValue() - DEFAULT_ASCENT);
            check(name + " 位移是 " + want + "（ascent " + row.getValue() + " − 7）",
                    json.contains("\"shift\":[0," + want + "]"));
        }

    }

}
