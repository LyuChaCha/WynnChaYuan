package com.wynnchayuan.translate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

public final class MajorIdWrapTest {

    private static int failures = 0;

    private static PrintStream out;

    private static final int WYNN_ROW_CHARS = 29;

    private static final Style PINK = Style.EMPTY.withColor(TextColor.fromRgb(0xE0B3E6));
    private static final Style GREY = Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA));
    private static final Style ICON = Style.EMPTY.withFont(
            new net.minecraft.network.chat.FontDescription.Resource(
                    net.minecraft.resources.Identifier.withDefaultNamespace("common")));

    static final String INDENT = "ÀÀ";

    private static final String BULLET = "";

    private static final String ICON_CHAR = "";

    private static final Set<String> KNOWN = Set.of();

    private static final Path MAJOR_ID = Path.of(
            "src/main/resources/assets/wynnchayuan/translations",
            Languages.DEFAULT, "major-id.json");

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("build"));
        out = new PrintStream(new java.io.FileOutputStream("build/majorid-wrap.txt"),
                true, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        airInACan(store);
        halfParagraphStaysEnglish(store);
        corpus(store);

        say(failures == 0 ? "MajorIdWrap: 全部通過" : "MajorIdWrap: " + failures + " 項失敗");
        out.close();
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void airInACan(TranslationStore store) {
        say("-- Air In A Can / Freerunner --");
        String[] rows = {"When your sprint", "bar is under 30% full,",
                         "increase your sprint speed by", "+150%."};
        for (String indent : new String[] {"", INDENT}) {
            List<Component> tip = tooltip(majorIdRows("Freerunner", rows, indent));
            String all = render(tip, store, null);
            String label = indent.isEmpty() ? "續行沒有縮排" : "續行帶 À 縮排";
            check(label + "：名稱翻了（實際：" + oneLine(all) + "）", all.contains("自由跑者"));
            check(label + "：說明翻了", all.contains("衝刺條"));
            check(label + "：沒有留下英文說明", !all.contains("When your sprint")
                    && !all.contains("bar is under") && !all.contains("increase your sprint"));
            check(label + "：數值填回去了", all.contains("30%") && all.contains("+150%"));
            String wrapped = render(tip, store, MajorIdWrapTest::width);
            check(label + "（重新折行）：說明翻了（實際：" + oneLine(wrapped) + "）",
                    wrapped.contains("衝刺條") && !wrapped.contains("When your sprint"));
            check(label + "（重新折行）：行數沒有比原文多",
                    rowsOf(tip, store, MajorIdWrapTest::width) <= tip.size());
        }
        Component indented = majorIdRows("Freerunner", rows, INDENT).get(1);
        check("只有縮排的片段不會被當成翻好了",
                LineTranslator.translate(StyledText.fromComponent(indented), store, false, false)
                        == null);
    }

    private static void halfParagraphStaysEnglish(TranslationStore store) {
        say("-- 查不到的說明整段留英文 --");
        String[] rows = {"When your sprint", "bar is under 30% empty,",
                         "increase your sprint speed by", "+150%."};
        String all = render(tooltip(majorIdRows("Freerunner", rows, INDENT)), store, null);
        check("不會只把名稱換成中文（實際：" + oneLine(all) + "）", !all.contains("自由跑者"));
    }

    private record Pair(String name, String nameDst, String key, String src,
                        String sample, String descDst) {}

    private static void corpus(TranslationStore store) throws Exception {
        say("-- 整份 major-id.json --");
        JsonObject entries = JsonParser.parseString(
                Files.readString(MAJOR_ID, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("entries");
        List<String[]> names = new ArrayList<>();
        List<Pair> pairs = new ArrayList<>();
        List<JsonObject> descs = new ArrayList<>();
        List<String> descKeys = new ArrayList<>();
        for (String key : entries.keySet()) {
            JsonObject e = entries.getAsJsonObject(key);
            String role = e.has("role") ? e.get("role").getAsString() : "";
            if ("name".equals(role)) {
                names.add(new String[] {e.get("src").getAsString(),
                        e.has("dst") ? e.get("dst").getAsString() : ""});
            } else if ("desc".equals(role)) {
                descs.add(e);
                descKeys.add(key);
            }
        }
        for (int i = 0; i < descs.size(); i++) {
            JsonObject e = descs.get(i);
            String[] name = names.get(i % names.size());
            String src = e.get("src").getAsString();
            String dst = e.has("dst") ? e.get("dst").getAsString() : "";
            String raw = e.has("_raw") ? e.get("_raw").getAsString() : null;
            if (name[1].isBlank() || dst.isBlank()) {
                continue;
            }
            pairs.add(new Pair(name[0], name[1], descKeys.get(i), src, sample(raw, src), dst));
        }
        say("  名稱 " + names.size() + " 條、有譯文的說明 " + pairs.size() + " 條");

        String[] layouts = {"", INDENT};
        List<ToIntFunction<Component>> measures = new ArrayList<>();
        measures.add(null);
        measures.add(MajorIdWrapTest::width);
        for (String indent : layouts) {
            for (ToIntFunction<Component> measure : measures) {
                String mode = (indent.isEmpty() ? "一般" : "À 縮排")
                        + (measure == null ? "" : "＋重新折行");
                List<String> failed = new ArrayList<>();
                int unexpected = 0;
                for (Pair p : pairs) {
                    List<String> rows = wrap(p.name(), p.sample());
                    List<Component> tip = tooltip(
                            majorIdRows(p.name(), rows.toArray(String[]::new), indent));
                    String why = verdict(p, rows, render(tip, store, measure), store);
                    if (why == null) {
                        continue;
                    }
                    boolean known = KNOWN.contains(p.key());
                    failed.add((known ? "(已知) " : "") + p.key() + "（借名 " + p.name() + "）：" + why);
                    if (!known) {
                        unexpected++;
                    }
                }
                say("  [" + mode + "] " + (pairs.size() - failed.size()) + "/" + pairs.size()
                        + " 條說明翻得出來");
                for (String f : failed) {
                    say("      " + f);
                }
                check("[" + mode + "] 沒有預期外的失敗（" + unexpected + " 條）", unexpected == 0);
            }
        }
    }

    static String sample(String raw, String src) {
        if (raw != null) {
            String filled = raw.replace("{#}", ICON_CHAR);
            if (template(filled).equals(src)) {
                return filled;
            }
        }
        return src.replace("{~}", "12").replace("{#}", ICON_CHAR);
    }

    private static String verdict(Pair p, List<String> rows, String all, TranslationStore store) {
        String joined = all.replace("\n", "");
        String longest = longestHan(strip(p.descDst()));
        boolean nameOk = joined.contains(strip(p.nameDst()));
        boolean descOk = longest.length() < 2 || joined.contains(longest);
        boolean english = false;
        for (int i = 1; i < rows.size(); i++) {
            String row = GlyphSplitter.stripGlyphChars(rows.get(i)).strip();
            if (row.chars().filter(Character::isLetter).count() >= 6 && all.contains(row)) {
                english = true;
            }
        }
        if (nameOk && descOk && !english) {
            return null;
        }
        String reason = store.lookupFlat(template(p.sample())) == null
                ? "語料查不到這句說明（" + template(p.sample()) + "）" : "程式路徑";
        return (nameOk ? "" : "名稱沒翻 ") + (descOk ? "" : "說明沒翻 ")
                + (english ? "留著英文續行 " : "") + "— " + reason + "｜輸出：" + oneLine(all);
    }

    static String template(String text) {
        return com.wynnchayuan.capture.LineParts.of(StyledText.fromComponent(parts(text, GREY)))
                .template();
    }

    static List<String> wrap(String name, String desc) {
        List<String> rows = new ArrayList<>();
        StringBuilder row = new StringBuilder();
        int prefix = name.length() + 2;
        for (String word : desc.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            String tried = row.length() == 0 ? word : row + " " + word;
            int budget = WYNN_ROW_CHARS - (rows.isEmpty() ? prefix : 0);
            if (row.length() > 0 && counted(tried) > budget) {
                rows.add(row.toString());
                row.setLength(0);
                row.append(word);
            } else {
                row.setLength(0);
                row.append(tried);
            }
        }
        rows.add(row.toString());
        return rows;
    }

    private static int counted(String row) {
        String core = row.replaceAll("[.,]+$", "");
        return (int) core.codePoints().filter(cp -> !GlyphSplitter.isGlyphCodePoint(cp)).count();
    }

    static List<Component> majorIdRows(String name, String[] rows, String indent) {
        List<Component> out = new ArrayList<>();
        MutableComponent first = Component.empty();
        first.append(Component.literal(BULLET).withStyle(ICON.withColor(PINK.getColor())));
        first.append(Component.literal(SpaceOffset.encode(2)).withStyle(SpaceOffset.styleFor(PINK)));
        first.append(Component.literal(name + ": ").withStyle(PINK));
        first.append(parts(rows[0], GREY));
        out.add(first);
        for (int i = 1; i < rows.length; i++) {
            MutableComponent row = Component.empty();
            if (!indent.isEmpty()) {
                row.append(Component.literal(indent).withStyle(GREY));
            }
            row.append(parts(rows[i], GREY));
            out.add(row);
        }
        return out;
    }

    private static MutableComponent parts(String text, Style style) {
        MutableComponent line = Component.empty();
        StringBuilder run = new StringBuilder();
        boolean glyph = false;
        for (int cp : text.codePoints().toArray()) {
            boolean g = GlyphSplitter.isGlyphCodePoint(cp);
            if (run.length() > 0 && g != glyph) {
                line.append(Component.literal(run.toString()).withStyle(glyph ? ICON : style));
                run.setLength(0);
            }
            glyph = g;
            run.appendCodePoint(cp);
        }
        if (run.length() > 0) {
            line.append(Component.literal(run.toString()).withStyle(glyph ? ICON : style));
        }
        return line;
    }

    static List<Component> tooltip(List<Component> majorId) {
        List<Component> tip = new ArrayList<>();
        tip.add(Component.literal("Sample Bow").withStyle(PINK));
        tip.add(Component.literal(""));
        tip.addAll(majorId);
        tip.add(Component.literal(""));
        tip.add(Component.literal("Fabled Item").withStyle(PINK));
        return tip;
    }

    static String render(List<Component> tip, TranslationStore store,
                                 ToIntFunction<Component> measure) {
        LineTranslator.measureForTest = measure;
        try {
            List<Component> got = com.wynnchayuan.render.TooltipPanel.translateLines(tip, store);
            if (got.isEmpty()) {
                got = tip;
            }
            StringBuilder sb = new StringBuilder();
            for (Component c : got) {
                sb.append(c.getString()).append('\n');
            }
            return sb.toString();
        } finally {
            LineTranslator.measureForTest = null;
        }
    }

    private static int rowsOf(List<Component> tip, TranslationStore store,
                              ToIntFunction<Component> measure) {
        LineTranslator.measureForTest = measure;
        try {
            List<Component> got = com.wynnchayuan.render.TooltipPanel.translateLines(tip, store);
            return got.isEmpty() ? tip.size() : got.size();
        } finally {
            LineTranslator.measureForTest = null;
        }
    }

    static int width(Component c) {
        int px = 0;
        for (int cp : c.getString().codePoints().toArray()) {
            px += cp == ' ' || cp == 'À' ? 4 : cp >= 0x2E80 ? 12 : 6;
        }
        return px;
    }

    private static String strip(String text) {
        return text.replaceAll("\\{[^}]*\\}", "");
    }

    private static String longestHan(String text) {
        String best = "";
        StringBuilder run = new StringBuilder();
        for (int cp : text.codePoints().toArray()) {
            if (Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN) {
                run.appendCodePoint(cp);
            } else {
                if (run.length() > best.length()) {
                    best = run.toString();
                }
                run.setLength(0);
            }
        }
        return run.length() > best.length() ? run.toString() : best;
    }

    private static String oneLine(String text) {
        String one = text.replace('\n', '|');
        return one.length() > 90 ? one.substring(0, 90) + "…" : one;
    }

    private static void say(String line) {
        System.out.println(line);
        out.println(line);
    }

    private static void check(String what, boolean ok) {
        say("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
