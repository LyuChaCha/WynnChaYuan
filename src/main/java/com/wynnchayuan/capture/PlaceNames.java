package com.wynnchayuan.capture;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.wynnchayuan.ModLog;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PlaceNames {

    public record Span(int start, int end) {}

    private static final List<String> NAMES;

    private static final String[][] BY_FIRST = new String[256][];

    static {
        List<String> names = load();
        NAMES = List.copyOf(names);
        List<String> longestFirst = new ArrayList<>(names);
        longestFirst.sort(Comparator.comparingInt(String::length).reversed());
        List<List<String>> buckets = new ArrayList<>();
        for (int i = 0; i < BY_FIRST.length; i++) {
            buckets.add(new ArrayList<>());
        }
        for (String name : longestFirst) {
            buckets.get(name.charAt(0) & 0xFF).add(name);
        }
        for (int i = 0; i < BY_FIRST.length; i++) {
            BY_FIRST[i] = buckets.get(i).isEmpty() ? null : buckets.get(i).toArray(new String[0]);
        }
    }

    private PlaceNames() {}

    private static List<String> load() {
        List<String> names = new ArrayList<>();
        try (InputStream in = PlaceNames.class.getResourceAsStream(
                "/assets/wynnchayuan/places.json")) {
            if (in == null) {
                return names;
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("places");
            for (JsonElement el : arr) {
                String name = el.getAsString().strip();
                if (!name.isEmpty()) {
                    names.add(name);
                }
            }
        } catch (Exception e) {
            ModLog.warn("地名清單載入失敗，將不做地名參數化: " + e.getMessage());
        }
        return names;
    }

    public static Span next(String text, int from) {
        int length = text.length();
        for (int at = from; at < length; at++) {
            String[] names = BY_FIRST[text.charAt(at) & 0xFF];
            if (names == null || (at > 0 && inWord(text.charAt(at - 1)))) {
                continue;
            }
            for (String name : names) {
                int end = at + name.length();
                if (text.startsWith(name, at)
                        && (end == length || !inWord(text.codePointAt(end)))) {
                    return new Span(at, end);
                }
            }
        }
        return null;
    }

    private static boolean inWord(int cp) {
        return switch (Character.getType(cp)) {
            case Character.UPPERCASE_LETTER, Character.LOWERCASE_LETTER, Character.TITLECASE_LETTER,
                 Character.MODIFIER_LETTER, Character.OTHER_LETTER,
                 Character.DECIMAL_DIGIT_NUMBER, Character.LETTER_NUMBER,
                 Character.OTHER_NUMBER -> true;
            default -> false;
        };
    }

    public static boolean anyStartsWith(String partial) {
        if (partial == null || partial.isEmpty()) {
            return true;
        }
        for (String name : NAMES) {
            if (name.startsWith(partial)) {
                return true;
            }
        }
        return false;
    }

    public static int size() {
        return NAMES.size();
    }
}
