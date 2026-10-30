package com.wynnchayuan.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class T {

    private T() {}

    private static final String PREFIX = "wynnchayuan.";
    private static final String PATH = "/assets/wynnchayuan/lang/";

    private static final Map<String, Map<String, String>> LOADED =
            new ConcurrentHashMap<>();

    public static MutableComponent c(String key, Object... args) {
        String pattern = pinned(key);
        if (pattern == null) {
            return Component.translatable(PREFIX + key, args);
        }
        return Component.literal(format(pattern, args));
    }

    public static String s(String key, Object... args) {
        String pattern = pinned(key);
        return pattern == null ? c(key, args).getString() : format(pattern, args);
    }

    private static String pinned(String key) {
        String lang = pinnedLanguage();
        if (lang == null || lang.isBlank()) {
            return null;
        }
        String value = load(lang).get(PREFIX + key);
        return value != null ? value : load("en_us").get(PREFIX + key);
    }

    public static String pinnedLanguage() {
        try {
            String lang = com.wynnchayuan.WynnChaYuan.config().uiLanguage();
            if (lang == null || lang.isBlank()) {
                lang = com.wynnchayuan.WynnChaYuan.config().language();
            }
            return lang == null || lang.isBlank() ? null : lang;
        } catch (Exception e) {
            return null;
        }
    }

    private static Map<String, String> load(String lang) {
        return LOADED.computeIfAbsent(lang, T::read);
    }

    public static java.util.List<String> available() {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String lang : com.wynnchayuan.translate.Languages.known()) {
            if (!load(lang).isEmpty()) {
                out.add(lang);
            }
        }
        return out;
    }

    private static Map<String, String> read(String lang) {
        try (InputStream in = T.class.getResourceAsStream(PATH + lang + ".json")) {
            if (in == null) {
                return Map.of();
            }
            JsonObject root = JsonParser.parseString(
                    new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            Map<String, String> out = new java.util.HashMap<>();
            for (String key : root.keySet()) {
                out.put(key, root.get(key).getAsString());
            }
            return Map.copyOf(out);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private static String format(String pattern, Object... args) {
        if (args == null || args.length == 0) {
            return pattern;
        }
        StringBuilder out = new StringBuilder(pattern.length() + 16);
        int at = 0;
        for (int i = 0; i < pattern.length(); i++) {
            if (pattern.charAt(i) == '%' && i + 1 < pattern.length()
                    && pattern.charAt(i + 1) == 's' && at < args.length) {
                out.append(args[at++]);
                i++;
            } else {
                out.append(pattern.charAt(i));
            }
        }
        return out.toString();
    }
}
