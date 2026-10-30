package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AbilityTermTest {

    private static int failures = 0;

    private static final int NAME_COLOUR = 0xAAAAAA;

    private static final int BODY_COLOUR = 0xFFFFFF;

    private static List<StyledText> block(String name, String rest) {
        MutableComponent line = Component.empty();
        line.append(Component.literal(name).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(NAME_COLOUR)).withUnderlined(true)));
        line.append(Component.literal(rest).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR))));
        List<StyledText> out = new ArrayList<>();
        out.add(StyledText.fromComponent(line));
        return out;
    }

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        swaps(store, "Bash", " gains +2x as many hits.", "重擊");
        swaps(store, "Charge", " deals damage and knocks back enemies.", "衝鋒");
        insideAccent(store);
        underlineStopsAtName(store);
        forcedSpans();

        System.out.println(failures == 0
                ? "技能名稱替換：全部通過" : "技能名稱替換：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void swaps(TranslationStore store, String name, String rest,
                              String want) {
        String term = store.lookup(name);
        report("詞表裡有「" + name + "」（實際：" + term + "）", want.equals(term));

        Component built = LineTranslator.translate(block(name, rest).get(0), store);
        if (built == null) {
            System.out.println("  [跳過] 語料裡沒有「" + name + rest + "」這一句");
            return;
        }
        String all = built.getString();
        report("畫面上是「" + want + "」（實際：" + all + "）", all.contains(want));
        report("畫面上沒有留著英文的「" + name + "」（實際：" + all + "）", !all.contains(name));
    }

    private static final int TERM_COLOUR = 0x55FFFF;

    private static void insideAccent(TranslationStore store) {
        String key = "Ophanim also applies +{~} Crystallized {#}.";
        report("★ 語料收著這一句（實際：" + store.lookup(key) + "）", store.lookup(key) != null);

        MutableComponent line = Component.empty();
        Object[] parts = {"Ophanim also applies ", 0xAAAAAA, "+2", 0xFFFFFF, " ", 0xAAAAAA,
                          "Crystallized", TERM_COLOUR, " ", 0xAAAAAA, "", TERM_COLOUR,
                          ".", 0xAAAAAA};
        for (int i = 0; i < parts.length; i += 2) {
            line.append(Component.literal((String) parts[i]).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb((Integer) parts[i + 1]))));
        }
        StyledText styled = StyledText.fromComponent(line);
        String template = com.wynnchayuan.capture.LineParts.of(styled).template();
        report("★ 模板就是語料的鍵，走的是整行命中（實際：" + template + "）",
               key.equals(template));

        Component built = LineTranslator.translate(styled, store);
        String all = built == null ? "" : built.getString();
        report("整行翻得出來（實際：" + all + "）", built != null);
        report("★ 水藍色那段裡的名稱換成「結晶化」（實際：" + all + "）",
               all.contains("結晶化") && !all.contains("Crystallized"));
        report("前面的技能名照舊換掉（實際：" + all + "）",
               all.contains("奧法尼姆") && !all.contains("Ophanim"));
        boolean[] coloured = {false};
        if (built != null) {
            built.visit((style, text) -> {
                if (text.contains("結晶化") && style.getColor() != null
                        && style.getColor().getValue() == TERM_COLOUR) {
                    coloured[0] = true;
                }
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        report("換上去的名稱保留水藍色", coloured[0]);
        Integer layer = colourAt(built, "層");
        report("★ 譯者加的「層」是正文的灰色，不是名稱的水藍色（實際 "
                        + (layer == null ? "null" : String.format("#%06X", layer)) + "）",
               layer != null && layer == 0xAAAAAA);
    }

    private static void underlineStopsAtName(TranslationStore store) {
        System.out.println("=== Arcane Speed 的底線 ===");
        Style grey = Style.EMPTY.withColor(TextColor.fromRgb(NAME_COLOUR));
        Style underlined = grey.withUnderlined(true);
        MutableComponent second = Component.empty();
        second.append(Component.literal("casting ").withStyle(grey));
        second.append(Component.literal("Heal").withStyle(underlined));
        second.append(Component.literal(" or ").withStyle(grey));
        second.append(Component.literal("Arcane Transfer").withStyle(underlined));
        second.append(Component.literal(".").withStyle(grey));
        List<StyledText> run = List.of(
                StyledText.fromComponent(Component.literal("You gain a walk speed buff after")
                                                  .withStyle(grey)),
                StyledText.fromComponent(second));
        LineTranslator.measureForTest = c -> {
            int px = 0;
            for (int cp : c.getString().codePoints().toArray()) {
                px += cp == ' ' ? 4 : cp >= 0x2E80 ? 12 : 6;
            }
            return px;
        };
        List<Component> out;
        try {
            out = LineTranslator.translateBlock(run, store, new boolean[run.size()]);
        } finally {
            LineTranslator.measureForTest = null;
        }
        report("整段查得到", out != null && !out.isEmpty());
        if (out == null || out.isEmpty()) {
            return;
        }
        for (int r = 0; r < out.size(); r++) {
            StringBuilder sb = new StringBuilder("    [" + r + "] ");
            out.get(r).visit((style, text) -> {
                sb.append(style.isUnderlined() ? "_" + text + "_" : text);
                return java.util.Optional.empty();
            }, Style.EMPTY);
            System.out.println(sb);
        }
        report("「治療」有底線", underlined(out, "治療") == Boolean.TRUE);
        report("「祕法回流」有底線", underlined(out, "祕法回流") == Boolean.TRUE);
        report("「和」沒有底線", underlined(out, "和") == Boolean.FALSE);
        report("★ 名稱後面的「額外獲得」沒有底線", underlined(out, "額外獲得") == Boolean.FALSE);
        report("★ 「移動速度。」沒有底線", underlined(out, "移動速度") == Boolean.FALSE);
    }

    private static Boolean underlined(List<Component> rows, String needle) {
        Boolean[] found = {null};
        for (Component row : rows) {
            row.visit((style, text) -> {
                if (found[0] == null && text.contains(needle)) {
                    found[0] = style.isUnderlined();
                }
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        return found[0];
    }

    private static void forcedSpans() throws java.io.IOException {
        System.out.println("=== 指定顏色的色段裡也換詞 ===");
        Path dir = java.nio.file.Files.createTempDirectory("wcy-forced-terms");
        java.nio.file.Files.writeString(dir.resolve("misc.json"), """
                {
                  "Reduces Fetch Hound's cooldown by -{~}s.": "{c2}Fetch Hound{c1} 的冷卻減少 {c3}{~} 秒{c1}。",
                  "Hit a Sapped foe to gain {~} Overguard.": "{c1}攻擊 {c2}Sapped{c1} 的敵人獲得 {c3}{~} {c2}Overguard{c1}。",
                  "Fetch Hound grants Overguard to you.": "Fetch Hound 會給你 Overguard。"
                }
                """, java.nio.charset.StandardCharsets.UTF_8);
        java.nio.file.Files.writeString(dir.resolve("ability-terms.json"),
                "{\"Fetch Hound\": \"尋獵犬\", \"Sapped\": \"枯竭\", \"Overguard\": \"溢盾\"}",
                java.nio.charset.StandardCharsets.UTF_8);
        TranslationStore fixture = new TranslationStore();
        fixture.loadAll(dir);

        int grey = 0xAAAAAA;
        int name = 0x55FF55;
        int value = 0xFFFFFF;
        Component hound = LineTranslator.translate(StyledText.fromComponent(row(
                "Reduces ", grey, "Fetch Hound's", name, " cooldown by ", grey,
                "-2s", value, ".", grey)), fixture);
        String a = hound == null ? "" : hound.getString();
        report("翻得出來（實際：" + a + "）", hound != null && a.contains("冷卻"));
        report("★ 色段裡的名稱換成譯名（實際：" + a + "）",
               a.contains("尋獵犬") && !a.contains("Fetch Hound"));
        report("★ 色段外面那個空格照一般內文的規則拿掉（實際：" + a + "）",
               a.contains("尋獵犬的冷卻"));
        Integer houndColour = colourAt(hound, "尋獵犬");
        report("譯名保住譯者指定的顏色（實際 " + hex(houndColour) + "）",
               houndColour != null && houndColour == name);

        Component sapped = LineTranslator.translate(StyledText.fromComponent(row(
                "Hit a ", grey, "Sapped", name, " foe to gain ", grey, "3", value, " ", grey,
                "Overguard", name, ".", grey)), fixture);
        String b = sapped == null ? "" : sapped.getString();
        report("★ 一行兩個色段都換（實際：" + b + "）",
               b.contains("枯竭") && b.contains("溢盾")
                       && !b.contains("Sapped") && !b.contains("Overguard"));
        report("★ 色段前面那個空格也照規則拿掉（實際：" + b + "）", b.contains("攻擊枯竭的敵人"));
        report("數值後面的空格留著（實際：" + b + "）", b.contains("3 溢盾"));

        Component plain = LineTranslator.translate(StyledText.fromComponent(row(
                "Fetch Hound grants Overguard to you.", grey)), fixture);
        String c = plain == null ? "" : plain.getString();
        report("對照組：一般內文照舊換（實際：" + c + "）",
               c.contains("尋獵犬") && c.contains("溢盾"));
    }

    private static MutableComponent row(Object... parts) {
        MutableComponent line = Component.empty();
        for (int i = 0; i < parts.length; i += 2) {
            line.append(Component.literal((String) parts[i]).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb((Integer) parts[i + 1]))));
        }
        return line;
    }

    private static String hex(Integer rgb) {
        return rgb == null ? "null" : String.format("#%06X", rgb);
    }

    private static Integer colourAt(Component row, String needle) {
        if (row == null) {
            return null;
        }
        Integer[] found = {null};
        row.visit((style, text) -> {
            if (found[0] == null && text.contains(needle)) {
                found[0] = style.getColor() == null ? 0 : style.getColor().getValue();
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
