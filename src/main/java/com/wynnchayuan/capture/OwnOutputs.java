package com.wynnchayuan.capture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class OwnOutputs {

    private static final String ASSETS = "/assets/wynnchayuan/translations/";

    private static volatile long[] translations = new long[0];

    private static final Map<Long, Long> messages = new ConcurrentHashMap<>();

    private static final long MESSAGE_TTL_MS = 10 * 60 * 1000L;

    private static final Pattern MARKUP = Pattern.compile("\\{c(?:\\d+|:[^}]*)}|\\{/}|\\{w\\d+}");
    private static final Pattern NUMBERED = Pattern.compile("\\{~\\d+}");
    private static final Pattern CONTINUATION = Pattern.compile("\\s*\\n\\s*(?:\\{#}\\s*)?");
    private static final Pattern DIGITS = Pattern.compile("\\d+(?:[.,]\\d+)*");

    private OwnOutputs() {}

    public static void buildAsync() {
        Thread t = new Thread(OwnOutputs::build, "WynnChaYuan-own-outputs");
        t.setDaemon(true);
        t.start();
    }

    static void build() {
        List<Long> out = new ArrayList<>(1 << 18);
        for (String lang : com.wynnchayuan.translate.Languages.bundled()) {
            for (String file : files(lang)) {
                JsonElement root = read(ASSETS + lang + "/" + file);
                if (root != null && root.isJsonObject()) {
                    collect(root.getAsJsonObject(), out);
                }
            }
        }
        long[] sorted = new long[out.size()];
        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = out.get(i);
        }
        Arrays.sort(sorted);
        translations = sorted;
        System.out.println("[WynnChaYuan] 收集時會略過我們自己的譯文：" + sorted.length + " 行");
    }

    static void addForTest(JsonObject root) {
        List<Long> out = new ArrayList<>();
        collect(root, out);
        long[] merged = Arrays.copyOf(translations, translations.length + out.size());
        for (int i = 0; i < out.size(); i++) {
            merged[translations.length + i] = out.get(i);
        }
        Arrays.sort(merged);
        translations = merged;
    }

    private static void collect(JsonObject root, List<Long> out) {
        if (root.has("entries") && root.get("entries").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("entries").entrySet()) {
                if (e.getValue().isJsonObject()) {
                    JsonElement dst = e.getValue().getAsJsonObject().get("dst");
                    if (dst != null && dst.isJsonPrimitive()) {
                        add(dst.getAsString(), out);
                    }
                }
            }
            return;
        }
        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
            if (!e.getKey().startsWith("_") && e.getValue().isJsonPrimitive()) {
                add(e.getValue().getAsString(), out);
            }
        }
    }

    private static void add(String dst, List<Long> out) {
        if (dst == null || dst.isBlank()) {
            return;
        }
        String whole = canon(dst);
        if (!whole.isEmpty()) {
            out.add(hash(whole));
        }
        if (dst.indexOf('\n') >= 0) {
            for (String line : dst.split("\n")) {
                String one = canon(line);
                if (!one.isEmpty()) {
                    out.add(hash(one));
                }
            }
        }
    }

    public static boolean isOwn(String template) {
        if (template == null || template.isBlank()) {
            return false;
        }
        long h = hash(canon(template));
        if (Arrays.binarySearch(translations, h) >= 0) {
            return true;
        }
        Long at = messages.get(h);
        return at != null && System.currentTimeMillis() - at < MESSAGE_TTL_MS;
    }

    public static void note(Component message) {
        if (message == null) {
            return;
        }
        long now = System.currentTimeMillis();
        messages.entrySet().removeIf(e -> now - e.getValue() > MESSAGE_TTL_MS);
        String text = message.getString();
        messages.put(hash(canon(DIGITS.matcher(text).replaceAll("{~}"))), now);
        for (String line : text.split("\n")) {
            messages.put(hash(canon(DIGITS.matcher(line).replaceAll("{~}"))), now);
        }
    }

    static String canon(String s) {
        String t = MARKUP.matcher(s).replaceAll("");
        t = NUMBERED.matcher(t).replaceAll("{~}");
        t = CONTINUATION.matcher(t).replaceAll(" ");
        return t.strip().replaceAll("\\s+", " ");
    }

    static long hash(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }

    private static List<String> files(String lang) {
        JsonElement index = read(ASSETS + lang + "/_index.json");
        List<String> out = new ArrayList<>();
        if (index != null && index.isJsonObject() && index.getAsJsonObject().has("files")) {
            for (JsonElement f : index.getAsJsonObject().getAsJsonArray("files")) {
                out.add(f.getAsString());
            }
        }
        return out;
    }

    private static JsonElement read(String resource) {
        try (InputStream in = OwnOutputs.class.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }
}
