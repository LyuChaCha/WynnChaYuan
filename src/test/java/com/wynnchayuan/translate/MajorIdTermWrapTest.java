package com.wynnchayuan.translate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.ToIntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MajorIdTermWrapTest {

    private static int failures = 0;

    private static PrintStream out;

    private static final Path MAJOR_ID = Path.of(
            "src/main/resources/assets/wynnchayuan/translations",
            Languages.DEFAULT, "major-id.json");

    private static final Pattern WORD = Pattern.compile("[A-Za-z][A-Za-z'-]+");

    private static final Pattern NESTED = Pattern.compile("\\([^()]*\\([^()]*\\)\\)");

    private static final Set<String> KNOWN = Set.of();

    private record Measure(String label, ToIntFunction<Component> width) {}

    private static final List<Measure> MEASURES = List.of(
            new Measure("方塊字 12", MajorIdWrapTest::width),
            new Measure("遊戲字寬", MajorIdTermWrapTest::gameWidth));

    public static void main(String[] args) throws Exception {
        Files.createDirectories(Path.of("build"));
        out = new PrintStream(new java.io.FileOutputStream("build/majorid-term-wrap.txt"),
                true, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));
        JsonObject entries = JsonParser.parseString(
                Files.readString(MAJOR_ID, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("entries");

        reported(store, entries);
        gearNamesAreNotTheCause(store);
        corpus(store, entries);
        say("-- 技能名稱附原文開著 --");
        store.setAbilityNamesWithOriginal(true);
        try {
            corpus(store, entries);
        } finally {
            store.setAbilityNamesWithOriginal(false);
        }

        say(failures == 0 ? "MajorIdTermWrap: 全部通過" : "MajorIdTermWrap: " + failures + " 項失敗");
        out.close();
        if (failures > 0) {
            System.exit(1);
        }
    }

    private record Case(String item, String name, String indent, String srcStart,
                        String[] want, String[] notWant) {}

    private static void reported(TranslationStore store, JsonObject entries) {
        Case[] cases = {
            new Case("The Incomprehensible", "Wormhole", MajorIdWrapTest.INDENT,
                    "Clones instead cast Meteor with Last Laugh",
                    new String[] {"蟲洞", "幻象", "餘戲", "隕石", "譏世弄人", "詭計"},
                    new String[] {"Last", "Laugh", "Malicious", "Mockery", "Meteor", "Trick",
                                  "Shadow", "Clone"}),
            new Case("Eschaton", "Death Sentence", "",
                    "Courage will immediately combust Guardian Angels",
                    new String[] {"終焉審判", "勇氣", "守護天使", "萬箭齊發"},
                    new String[] {"守護者", "Guardian", "Angels", "Courage", "Buckshot"}),
            new Case("Samsara", "Mana Surge", MajorIdWrapTest.INDENT,
                    "Curse instead releases a calming wave",
                    new String[] {"魔力湧動", "魔力回復", "魔力竊取"},
                    new String[] {"Mana", "Regen", "Steal"}),
        };
        for (Case c : cases) {
            say("-- " + c.item() + " / " + c.name() + " --");
            JsonObject desc = null;
            for (String key : entries.keySet()) {
                JsonObject e = entries.getAsJsonObject(key);
                if ("desc".equals(str(e, "role")) && str(e, "src").startsWith(c.srcStart())) {
                    desc = e;
                }
            }
            check("★ 語料裡找得到這一條說明", desc != null);
            if (desc == null) {
                continue;
            }
            String sample = MajorIdWrapTest.sample(str(desc, "_raw"), str(desc, "src"));
            List<String> rows = MajorIdWrapTest.wrap(c.name(), sample);
            say("  Wynncraft 送來的：");
            say("    " + c.name() + ": " + rows.get(0));
            for (int i = 1; i < rows.size(); i++) {
                say("    " + c.indent() + rows.get(i));
            }
            for (Measure measure : MEASURES) {
                List<String> shown = descRows(render(store, c.name(), rows, c.indent(), measure));
                say("  畫出來的（" + measure.label() + "）：");
                for (String row : shown) {
                    say("    " + row);
                }
                String label = "［" + measure.label() + "］";
                String joined = String.join("", shown);
                for (String w : c.want()) {
                    check(label + "畫面上有「" + w + "」", joined.contains(w));
                }
                for (String w : c.notWant()) {
                    check(label + "畫面上沒有「" + w + "」", !containsWord(joined, w));
                }
                List<String> problems = rowProblems(shown, store);
                check(label + "沒有被拆開或只換一半的名稱" + (problems.isEmpty() ? "" : "：" + problems),
                        problems.isEmpty());
                if (c.name().equals("Wormhole")) {
                    check(label + "「譏世弄人使」不會自己佔一行",
                            shown.stream().noneMatch(r -> r.strip().equals("譏世弄人使")));
                }
            }
        }
    }

    private static void gearNamesAreNotTheCause(TranslationStore store) {
        say("-- 裝備名守門不是原因 --");
        for (String name : new String[] {"Guardian", "Guardian Angels", "Courage", "Last Laugh",
                "Meteor", "Buckshot", "Malicious Mockery", "Mana Steal", "Mana Regen"}) {
            say("  " + name + "：還沒翻的裝備名 " + store.isBareGearName(name)
                    + "、詞表 " + store.lookupTerm(name));
        }
        check("★ Guardian 確實也是裝備名（上面 Death Sentence 那一條才測得到撞名）",
                store.isBareGearName("Guardian") || store.gearOwnName("Guardian") != null);
        check("Guardian Angels 在詞表裡是守護天使", "守護天使".equals(store.lookupTerm("Guardian Angels")));
        check("Mana Steal 在詞表裡", store.lookupTerm("Mana Steal") != null);
    }

    private static void corpus(TranslationStore store, JsonObject entries) {
        say("-- 整份 major-id.json --");
        List<String[]> names = new ArrayList<>();
        for (String key : entries.keySet()) {
            JsonObject e = entries.getAsJsonObject(key);
            if ("name".equals(str(e, "role")) && !str(e, "dst").isBlank()) {
                names.add(new String[] {str(e, "src"), str(e, "dst")});
            }
        }
        int descs = 0;
        for (Measure measure : MEASURES) {
            for (String indent : new String[] {"", MajorIdWrapTest.INDENT}) {
                String mode = (indent.isEmpty() ? "一般" : "À 縮排") + "＋" + measure.label();
                List<String> failed = new ArrayList<>();
                int unexpected = 0;
                int borrow = 0;
                descs = 0;
                for (String key : entries.keySet()) {
                    JsonObject e = entries.getAsJsonObject(key);
                    String dst = str(e, "dst");
                    if (!"desc".equals(str(e, "role")) || dst.isBlank()) {
                        continue;
                    }
                    descs++;
                    String name = null;
                    if (key.endsWith("::desc") && entries.has(key.substring(0, key.length() - 6))) {
                        name = str(entries.getAsJsonObject(key.substring(0, key.length() - 6)), "src");
                    }
                    if (name == null || name.isBlank()) {
                        name = names.get(borrow++ % names.size())[0];
                    }
                    String sample = MajorIdWrapTest.sample(str(e, "_raw"), str(e, "src"));
                    List<String> rows = MajorIdWrapTest.wrap(name, sample);
                    List<String> shown = descRows(render(store, name, rows, indent, measure));
                    List<String> problems = rowProblems(shown, store);
                    Matcher nested = NESTED.matcher(String.join(" ", shown));
                    if (nested.find()) {
                        problems.add("括號裡又換了一次「" + nested.group() + "」");
                    }
                    Set<String> expected = words(swapWhole(dst, store));
                    expected.addAll(words(name));
                    String nameDst = store.lookup(name);
                    if (nameDst != null) {
                        expected.addAll(words(nameDst));
                    }
                    for (String word : words(String.join(" ", shown))) {
                        if (!expected.contains(word)) {
                            problems.add("多留了英文「" + word + "」");
                        }
                    }
                    if (problems.isEmpty()) {
                        continue;
                    }
                    boolean known = KNOWN.contains(key);
                    failed.add((known ? "(已知) " : "") + key + "：" + problems + "｜"
                            + String.join(" ⏎ ", shown));
                    if (!known) {
                        unexpected++;
                    }
                }
                say("  [" + mode + "] " + (descs - failed.size()) + "/" + descs + " 條沒有拆壞的名稱");
                for (String f : failed) {
                    say("      " + f);
                }
                check("[" + mode + "] 沒有預期外的失敗（" + unexpected + " 條）", unexpected == 0);
            }
        }
        check("說明有被掃到（實際 " + descs + " 條）", descs >= 100);
    }

    static List<String> rowProblems(List<String> rows, TranslationStore store) {
        List<String> problems = new ArrayList<>();
        List<String[]> multi = new ArrayList<>();
        for (String term : store.termNames()) {
            if (term.indexOf(' ') > 0) {
                multi.add(term.split(" "));
            }
        }
        for (int i = 0; i + 1 < rows.size(); i++) {
            String a = rows.get(i).strip();
            String b = rows.get(i + 1).strip();
            if (a.isEmpty() || b.isEmpty()) {
                continue;
            }
            if (latin(a.charAt(a.length() - 1)) && latin(b.charAt(0)) && (hasHan(a) || hasHan(b))) {
                problems.add("英文詞串被拆開「" + tail(a) + " ⏎ " + head(b) + "」");
            }
            for (String[] words : multi) {
                for (int k = 1; k < words.length; k++) {
                    String front = String.join(" ", java.util.Arrays.copyOfRange(words, 0, k));
                    String back = String.join(" ", java.util.Arrays.copyOfRange(words, k, words.length));
                    if (!startsWithWord(b, back)) {
                        continue;
                    }
                    String zh = store.lookupTerm(front);
                    if (endsWithWord(a, front) || (zh != null && a.endsWith(zh))) {
                        problems.add("「" + String.join(" ", words) + "」被拆到兩行（"
                                + tail(a) + " ⏎ " + head(b) + "）");
                    }
                }
            }
        }
        String joined = String.join("\n", rows);
        for (String[] words : multi) {
            for (int k = 1; k < words.length; k++) {
                String zh = store.lookupTerm(String.join(" ", java.util.Arrays.copyOfRange(words, 0, k)));
                if (zh == null) {
                    continue;
                }
                String back = String.join(" ", java.util.Arrays.copyOfRange(words, k, words.length));
                Matcher m = Pattern.compile(Pattern.quote(zh) + "[ \\n]?" + Pattern.quote(back)
                        + "(?![A-Za-z])").matcher(joined);
                if (m.find()) {
                    problems.add("「" + String.join(" ", words) + "」只換了前半（"
                            + m.group().replace("\n", " ⏎ ") + "）");
                }
            }
        }
        return problems;
    }

    static int gameWidth(Component c) {
        int px = 0;
        for (int cp : c.getString().codePoints().toArray()) {
            px += cp == ' ' || cp == 'À' ? 4 : cp >= 0x2E80 ? 9 : 6;
        }
        return px;
    }

    private static String render(TranslationStore store, String name, List<String> rows,
                                 String indent, Measure measure) {
        List<Component> tip = MajorIdWrapTest.tooltip(
                MajorIdWrapTest.majorIdRows(name, rows.toArray(String[]::new), indent));
        return MajorIdWrapTest.render(tip, store, measure.width());
    }

    private static List<String> descRows(String rendered) {
        String[] lines = rendered.split("\n");
        List<String> rows = new ArrayList<>();
        for (int i = 2; i < lines.length - 2; i++) {
            rows.add(lines[i]);
        }
        return rows;
    }

    private static String swapWhole(String text, TranslationStore store) {
        StringBuilder sb = new StringBuilder();
        int at = 0;
        while (at < text.length()) {
            TranslationStore.Term term = store.findTerm(text, at);
            if (term == null) {
                sb.append(text, at, text.length());
                break;
            }
            sb.append(text, at, term.start()).append(term.translation());
            at = term.end();
        }
        return sb.toString();
    }

    private static Set<String> words(String text) {
        Set<String> set = new TreeSet<>();
        Matcher m = WORD.matcher(text.replaceAll("\\{[^}]*\\}", " "));
        while (m.find()) {
            set.add(m.group());
        }
        return set;
    }

    private static boolean containsWord(String text, String word) {
        return Pattern.compile("(?<![A-Za-z])" + Pattern.quote(word) + "(?![A-Za-z])")
                .matcher(text).find();
    }

    private static boolean startsWithWord(String text, String word) {
        return text.startsWith(word)
                && (text.length() == word.length() || !latin(text.charAt(word.length())));
    }

    private static boolean endsWithWord(String text, String word) {
        int at = text.length() - word.length();
        return at >= 0 && text.endsWith(word) && (at == 0 || !latin(text.charAt(at - 1)));
    }

    private static boolean latin(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean hasHan(String text) {
        return text.codePoints().anyMatch(cp ->
                Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN);
    }

    private static String tail(String row) {
        return row.length() > 8 ? "…" + row.substring(row.length() - 8) : row;
    }

    private static String head(String row) {
        return row.length() > 8 ? row.substring(0, 8) + "…" : row;
    }

    private static String str(JsonObject e, String field) {
        return e.has(field) && e.get(field).isJsonPrimitive() ? e.get(field).getAsString() : "";
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
