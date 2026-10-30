package com.wynnchayuan.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.wynnchayuan.capture.GlyphSplitter;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ThirdPartySections {

    private static final String RESOURCE = "/assets/wynnchayuan/third-party-sections.json";

    private static final int MAX_BLOCK = 12;

    private static Set<String> badges = Set.of();

    private ThirdPartySections() {}

    public static void load(Path configDir) {
        Set<String> found = new LinkedHashSet<>();
        Path local = configDir.resolve("third-party-sections.json");
        if (Files.isRegularFile(local)) {
            read(() -> Files.newInputStream(local), found);
        }
        if (found.isEmpty()) {
            read(() -> ThirdPartySections.class.getResourceAsStream(RESOURCE), found);
        }
        badges = Set.copyOf(found);
    }

    public static List<Component> strip(List<Component> lines) {
        if (badges.isEmpty() || lines == null || lines.isEmpty()) {
            return lines;
        }
        boolean[] drop = new boolean[lines.size()];
        boolean any = false;

        for (int i = 0; i < lines.size(); i++) {
            if (drop[i] || !isBadge(lines.get(i))) {
                continue;
            }
            any = true;
            if (i > 0 && !hasReadable(lines.get(i - 1))) {
                drop[i - 1] = true;
            }
            int limit = Math.min(lines.size(), i + MAX_BLOCK);
            for (int j = i; j < limit; j++) {
                drop[j] = true;
                if (!isBadge(lines.get(j)) && !hasReadable(lines.get(j))) {
                    break;
                }
            }
        }
        if (!any) {
            return lines;
        }
        List<Component> out = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            if (!drop[i]) {
                out.add(lines.get(i));
            }
        }
        return out;
    }

    private static boolean isBadge(Component line) {
        return badges.contains(readable(line).toLowerCase(java.util.Locale.ROOT));
    }

    private static String readable(Component line) {
        StringBuilder sb = new StringBuilder();
        line.getString().codePoints().forEach(cp -> {
            if (!GlyphSplitter.isGlyphCodePoint(cp)) {
                sb.appendCodePoint(cp);
            }
        });
        return sb.toString().strip();
    }

    private static boolean hasReadable(Component line) {
        return !readable(line).isEmpty();
    }

    private static void read(StreamSource source, Set<String> into) {
        try (InputStream in = source.open()) {
            if (in == null) {
                return;
            }
            JsonElement root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            JsonArray arr = root.getAsJsonObject().getAsJsonArray("sections");
            if (arr == null) {
                return;
            }
            for (JsonElement el : arr) {
                String name = el.getAsString().strip().toLowerCase(java.util.Locale.ROOT);
                if (!name.isEmpty()) {
                    into.add(name);
                }
            }
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 第三方區塊清單讀取失敗: " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface StreamSource {
        InputStream open() throws Exception;
    }
}
