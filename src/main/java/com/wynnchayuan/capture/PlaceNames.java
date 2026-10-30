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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceNames {

    private static final Pattern PATTERN;

    private static final List<String> NAMES;

    private static final java.util.Map<Character, String[]> BY_FIRST;

    static {
        List<String> names = load();
        NAMES = List.copyOf(names);
        PATTERN = names.isEmpty() ? null : compile(names);
        java.util.Map<Character, List<String>> grouped = new java.util.HashMap<>();
        for (String name : NAMES) {
            grouped.computeIfAbsent(name.charAt(0), k -> new ArrayList<>()).add(name);
        }
        java.util.Map<Character, String[]> index = new java.util.HashMap<>();
        grouped.forEach((first, list) -> index.put(first, list.toArray(new String[0])));
        BY_FIRST = java.util.Map.copyOf(index);
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

    private static Pattern compile(List<String> names) {
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

    public static Matcher matcher(String text) {
        if (PATTERN == null) {
            return null;
        }
        return PATTERN.matcher(text == null || mayContain(text) ? text : "");
    }

    static boolean mayContain(String text) {
        for (int i = 0; i < text.length(); i++) {
            String[] names = BY_FIRST.get(text.charAt(i));
            if (names == null) {
                continue;
            }
            for (String name : names) {
                if (text.startsWith(name, i)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isPlace(String text) {
        return text != null && NAMES.contains(text.strip());
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
