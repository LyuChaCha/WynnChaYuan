package com.wynnchayuan.translate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.capture.PlaceNames;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class RespellPlacesTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path root = Path.of("src/main/resources/assets/wynnchayuan");
        Set<String> corpus = corpus(root.resolve("translations"));
        report("語料讀得到（" + corpus.size() + " 條字串）", corpus.size() > 100_000);

        places(root, corpus);
        respell(corpus);

        System.out.println(failures == 0 ? "\n地名與拼法加速：全部通過"
                : "\n地名與拼法加速：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void places(Path root, Set<String> corpus) throws IOException {
        List<String> names = new ArrayList<>();
        JsonObject places = JsonParser.parseString(Files.readString(
                root.resolve("places.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        for (JsonElement el : places.getAsJsonArray("places")) {
            String name = el.getAsString().strip();
            if (!name.isEmpty()) {
                names.add(name);
            }
        }
        Pattern old = oldPattern(names);

        List<String> inputs = new ArrayList<>(corpus);
        for (String name : names) {
            inputs.add(name);
            inputs.add("[You are now entering " + name + "]");
            inputs.add(name + " Citizen");
            inputs.add("Go to " + name + ".");
            inputs.add(name + "s");
            inputs.add("x" + name);
            inputs.add(name.toLowerCase(java.util.Locale.ROOT) + " road");
            inputs.add(name + name);
            inputs.add("12" + name + "34");
            inputs.add(name.substring(0, Math.max(1, name.length() - 1)));
        }
        inputs.add("");
        inputs.add(" ");

        int differ = 0;
        int withPlaces = 0;
        String firstDiff = null;
        for (String text : inputs) {
            List<String> want = matches(old.matcher(text));
            List<String> got = scanned(text);
            if (!want.isEmpty()) {
                withPlaces++;
            }
            if (!want.equals(got)) {
                differ++;
                if (firstDiff == null) {
                    firstDiff = text + " 原本 " + want + " 現在 " + got;
                }
            }
        }
        report("對照組裡真的有地名（" + withPlaces + " 條）", withPlaces > 1000);
        report("★ 地名比對跟原本的比對式逐條相同（" + inputs.size() + " 條，不同 " + differ + " 條"
                + (firstDiff == null ? "" : "，例如 " + firstDiff) + "）", differ == 0);
    }

    private static Pattern oldPattern(List<String> names) {
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(Comparator.comparingInt(String::length).reversed());
        StringBuilder sb = new StringBuilder();
        for (String name : sorted) {
            if (!sb.isEmpty()) {
                sb.append('|');
            }
            sb.append(Pattern.quote(name));
        }
        return Pattern.compile("(?<![\\p{L}\\p{N}])(?:" + sb + ")(?![\\p{L}\\p{N}])");
    }

    private static List<String> scanned(String text) {
        List<String> out = new ArrayList<>();
        int from = 0;
        for (PlaceNames.Span place = PlaceNames.next(text, 0); place != null;
                place = PlaceNames.next(text, from)) {
            out.add(place.start() + ":" + place.end() + ":"
                    + text.substring(place.start(), place.end()));
            from = place.end();
        }
        return out;
    }

    private static List<String> matches(Matcher m) {
        List<String> out = new ArrayList<>();
        if (m == null) {
            return out;
        }
        while (m.find()) {
            out.add(m.start() + ":" + m.end() + ":" + m.group());
        }
        return out;
    }

    private static void respell(Set<String> corpus) throws Exception {
        var field = TranslationStore.class.getDeclaredField("SPELLING");
        field.setAccessible(true);
        String[][] spelling = (String[][]) field.get(null);

        List<String> inputs = new ArrayList<>(corpus);
        for (String[] pair : spelling) {
            String w = pair[0];
            inputs.add(w);
            inputs.add(Character.toUpperCase(w.charAt(0)) + w.substring(1));
            inputs.add(w.toUpperCase(java.util.Locale.ROOT));
            inputs.add("The " + w + "s of the " + w + "ed");
            inputs.add("+12% " + Character.toUpperCase(w.charAt(0)) + w.substring(1) + " Bonus");
        }
        int changed = 0;
        int differ = 0;
        String firstDiff = null;
        for (String text : inputs) {
            String want = oldRespell(text, spelling);
            String got = TranslationStore.respell(text);
            if (!want.equals(text)) {
                changed++;
            }
            if (!want.equals(got)) {
                differ++;
                if (firstDiff == null) {
                    firstDiff = text + " 原本 " + want + " 現在 " + got;
                }
            }
        }
        report("對照組裡真的有英式拼法（" + changed + " 條）", changed > 100);
        report("★ 拼法換算跟原本逐條相同（" + inputs.size() + " 條，不同 " + differ + " 條"
                + (firstDiff == null ? "" : "，例如 " + firstDiff) + "）", differ == 0);
    }

    private static String oldRespell(String text, String[][] spelling) {
        String out = text;
        for (String[] pair : spelling) {
            out = out.replace(pair[0], pair[1]);
            out = out.replace(upper(pair[0]), upper(pair[1]));
        }
        return out;
    }

    private static String upper(String word) {
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    static Set<String> corpus(Path dir) throws IOException {
        Set<String> out = new LinkedHashSet<>();
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                String name = file.toString().replace('\\', '/');
                if (!name.endsWith(".json") || name.contains("/quest/")) {
                    continue;
                }
                try {
                    walk(JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)), out);
                } catch (RuntimeException e) {
                }
            }
        }
        return out;
    }

    private static void walk(JsonElement el, Set<String> out) {
        if (el.isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet()) {
                if (e.getKey().startsWith("_")) {
                    continue;
                }
                out.add(e.getKey());
                walk(e.getValue(), out);
            }
        } else if (el.isJsonArray()) {
            el.getAsJsonArray().forEach(child -> walk(child, out));
        } else if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
            out.add(el.getAsString());
        }
    }

    private static void report(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
