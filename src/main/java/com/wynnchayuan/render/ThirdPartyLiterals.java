package com.wynnchayuan.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.translate.ErrorDebug;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// WynnMod RaidState.onTitle 靠 Raid Completed／Failed 判勝負
public final class ThirdPartyLiterals {

    private static final String RESOURCE = "/assets/wynnchayuan/third-party-literals.json";

    private static List<List<String>> active = List.of();

    private ThirdPartyLiterals() {
    }

    public static void load(Path configDir) {
        active = groups(read(configDir),
                id -> FabricLoader.getInstance().isModLoaded(id),
                ThirdPartyLiterals::classPresent);
    }

    private static final String UNLESS = "@unless:";

    // 新版 WynnMod 在封包層先讀聊天；標題它讀在後，要留英文
    public static List<List<String>> groups(JsonObject root,
                                            java.util.function.Predicate<String> modLoaded,
                                            java.util.function.Predicate<String> classPresent) {
        List<List<String>> out = new ArrayList<>();
        if (root != null) {
            for (Map.Entry<String, JsonElement> mod : root.entrySet()) {
                if (mod.getKey().startsWith("_") || !mod.getValue().isJsonArray()) {
                    continue;
                }
                String id = mod.getKey();
                String unless = null;
                int at = id.indexOf(UNLESS);
                if (at > 0) {
                    unless = id.substring(at + UNLESS.length()).strip();
                    id = id.substring(0, at);
                }
                if (!modLoaded.test(id)) {
                    continue;
                }
                if (unless != null && !unless.isEmpty() && classPresent.test(unless)) {
                    continue;
                }
                for (JsonElement literal : mod.getValue().getAsJsonArray()) {
                    List<String> group = new ArrayList<>();
                    if (literal.isJsonArray()) {
                        for (JsonElement part : literal.getAsJsonArray()) {
                            String text = part.getAsString();
                            if (!text.isBlank()) {
                                group.add(text);
                            }
                        }
                    } else {
                        String text = literal.getAsString();
                        if (!text.isBlank()) {
                            group.add(text);
                        }
                    }
                    if (!group.isEmpty()) {
                        out.add(List.copyOf(group));
                    }
                }
            }
        }
        return List.copyOf(out);
    }

    private static boolean classPresent(String name) {
        try {
            return ThirdPartyLiterals.class.getClassLoader()
                    .getResource(name.replace('.', '/') + ".class") != null;
        } catch (Throwable t) {
            ErrorDebug.note("ThirdPartyLiterals#classPresent", null, t);
            return false;
        }
    }

    public static boolean reserved(String text) {
        if (active.isEmpty() || text == null || text.isEmpty()) {
            return false;
        }
        for (List<String> group : active) {
            boolean all = true;
            for (String part : group) {
                if (!text.contains(part)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                return true;
            }
        }
        return false;
    }

    private static JsonObject read(Path configDir) {
        Path override = configDir == null ? null : configDir.resolve("third-party-literals.json");
        try {
            if (override != null && Files.isRegularFile(override)) {
                return JsonParser.parseString(Files.readString(override, StandardCharsets.UTF_8))
                        .getAsJsonObject();
            }
        } catch (Exception e) {
            ErrorDebug.note("ThirdPartyLiterals#read", null, e);
        }
        try (InputStream in = ThirdPartyLiterals.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            ErrorDebug.note("ThirdPartyLiterals#read", null, e);
            return null;
        }
    }

    public static void forTest(List<String> literals) {
        List<List<String>> out = new ArrayList<>();
        for (String literal : literals) {
            out.add(List.of(literal));
        }
        active = List.copyOf(out);
    }

    public static void forTestGroups(List<List<String>> groups) {
        active = List.copyOf(groups);
    }

    public static List<List<String>> activeLiterals() {
        return active;
    }
}
