package com.wynnchayuan.translate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class MajorIdTest {

    private static final String AS_RENDERED =
            "✦ Altruism: Whenever you gain\n"
            + "health from your Life Steal\n"
            + "and Health Regen IDs, allies\n"
            + "within {~} blocks will receive\n"
            + "the same amount.";

    public static void main(String[] args) throws Exception {
        majorIdTerms();
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        check("語料本身讀得到名稱", "利他主義".equals(store.lookup("Altruism")));

        String flowed = BlockTranslator.lookupFlowed(AS_RENDERED, store);
        check("整段（含 ✦ 與自動斷行）查得到", flowed != null);
        if (flowed != null) {
            check("名稱有翻到", flowed.contains("利他主義"));
            check("說明有翻到", flowed.contains("格內的友軍"));
            check("佔位符數量守住", numberSlots(flowed) == numberSlots(AS_RENDERED));
        }

        String blinding = BlockTranslator.lookupFlowed(BLINDING, store);
        check("帶兩個偏移字元的整段也查得到（實際：" + shorten(blinding) + "）",
                blinding != null);
        if (blinding != null) {
            check("名稱有翻到", blinding.contains("眩目之光"));
        }

        check("不認得的名稱不會硬翻",
                BlockTranslator.lookupFlowed(
                        "✦ Nonexistent: Allies within {~} blocks gain {~} of the "
                        + "health you gain from Health Regen and Life Steal.", store) == null);

        System.out.println(failures == 0
                ? "MajorId: 全部通過" : "MajorId: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static final String BLINDING =
            "{#}{#}Blinding Lights: All Ophanim\n"
            + "orbs deal {~} damage, gain\n"
            + "+{~} Crystallize per hit, and\n"
            + "instead orbit around you.\n"
            + "Using Main Attack causes all\n"
            + "orbs to travel faster.";

    private static String shorten(String text) {
        if (text == null) {
            return "null";
        }
        String one = text.replace('\n', ' ');
        return one.length() > 40 ? one.substring(0, 40) + "…" : one;
    }

    private static int numberSlots(String text) {
        return text.split("\\{~\\d?\\}", -1).length - 1;
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    private static void majorIdTerms() throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        JsonObject root = JsonParser.parseString(
                Files.readString(MAJOR_ID, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject entries = root.getAsJsonObject("entries");

        int descs = 0;
        int swapped = 0;
        TreeMap<String, String> leftover = new TreeMap<>();
        List<String> blank = new ArrayList<>();

        for (String key : entries.keySet()) {
            JsonElement el = entries.get(key);
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject e = el.getAsJsonObject();
            String dst = e.has("dst") ? e.get("dst").getAsString() : "";
            if (dst.isBlank()) {
                blank.add(key);
                continue;
            }
            if (!"desc".equals(e.has("role") ? e.get("role").getAsString() : "")) {
                continue;
            }
            descs++;
            Swap swap = render(dst, store);
            swapped += swap.count();
            Matcher m = WORD.matcher(swap.text());
            while (m.find()) {
                String word = m.group();
                String bare = word.endsWith("'s") ? word.substring(0, word.length() - 2) : word;
                if (PROPER_NOUNS.contains(bare) || KEYS.contains(bare)) {
                    continue;
                }
                leftover.putIfAbsent(word, key);
            }
        }

        check("每一條 Major ID 都有譯文（沒有的：" + blank + "）", blank.isEmpty());
        check("敘述有被掃到（實際 " + descs + " 條）", descs >= 100);
        check("詞表真的有在換（換掉 " + swapped + " 處）", swapped >= descs);
        for (var entry : leftover.entrySet()) {
            check("畫面上不該留著「" + entry.getKey() + "」（" + entry.getValue() + "）", false);
        }
        if (leftover.isEmpty()) {
            System.out.println("  [PASS] 替換後沒有多餘的英文");
        }
    }

    private static final Set<String> PROPER_NOUNS = Set.of(
            "Twain",
            "Aubri",
            "Flain",
            "Bakal",
            "Orphion",
            "Bovemists");

    private static final Set<String> KEYS = Set.of("Shift", "Ctrl", "Alt");

    private static final Pattern WORD = Pattern.compile("[A-Za-z][A-Za-z'-]+");

    private static final Path MAJOR_ID = Path.of(
            "src/main/resources/assets/wynnchayuan/translations",
            Languages.DEFAULT, "major-id.json");


    private record Swap(String text, int count) {}

    private static Swap render(String text, TranslationStore store) {
        StringBuilder out = new StringBuilder(text.length());
        int at = 0;
        int count = 0;
        while (at < text.length()) {
            TranslationStore.Term term = store.findTerm(text, at);
            if (term == null) {
                out.append(text, at, text.length());
                break;
            }
            out.append(text, at, term.start()).append(term.translation());
            at = term.end();
            count++;
        }
        return new Swap(out.toString(), count);
    }
}
