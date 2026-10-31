package com.wynnchayuan;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.capture.OwnOutputs;
import com.wynnchayuan.client.T;
import com.wynnchayuan.translate.ErrorDebug;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class Releases {

    private Releases() {}

    private static final List<String> SOURCES = List.of(
            "https://raw.githubusercontent.com/LyuChaCha/WynnChaYuan/main/version.json",
            "https://cdn.jsdelivr.net/gh/LyuChaCha/WynnChaYuan@main/version.json");

    private static final String BUNDLED = "/assets/wynnchayuan/version.json";

    public record Notes(String headline, List<String> items) {}

    private static volatile JsonObject data;
    private static volatile boolean fromNetwork;

    private static volatile boolean checked;

    public static void init() {
        data = bundled();
    }

    public static void fetchLatest() {
        if (!NetworkGate.online()) {
            return;
        }
        Thread fetch = new Thread(Releases::refresh, WynnChaYuan.MOD_ID + "-version");
        fetch.setDaemon(true);
        fetch.start();
    }

    private static JsonObject bundled() {
        try (InputStream in = Releases.class.getResourceAsStream(BUNDLED)) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseString(new String(in.readAllBytes(),
                    StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            ErrorDebug.note("Releases#bundled", null, e);
            return null;
        }
    }

    private static void refresh() {
        try {
            fetch();
        } finally {
            checked = true;
        }
    }

    private static void fetch() {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        for (String url : SOURCES) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(15))
                        .header("User-Agent", "WynnChaYuan/" + WynnChaYuan.version())
                        .GET().build();
                HttpResponse<String> response = http.send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() / 100 != 2) {
                    continue;
                }
                JsonObject fresh = JsonParser.parseString(response.body()).getAsJsonObject();
                if (fresh.has("latest")) {
                    data = fresh;
                    fromNetwork = true;
                }
                return;
            } catch (Exception e) {
                ErrorDebug.note("Releases#fetch", null, e);
            }
        }
    }

    public static String newer() {
        JsonObject o = data;
        if (o == null || !fromNetwork || !o.has("latest")) {
            return null;
        }
        String latest = o.get("latest").getAsString();
        return latest.equals(WynnChaYuan.version()) ? null : latest;
    }

    static final String DOWNLOAD = "https://github.com/LyuChaCha/WynnChaYuan/releases/latest";

    public static String downloadUrl() {
        return DOWNLOAD;
    }

    public static void tellOnce(Minecraft client) {
        if (told || client == null || client.player == null) {
            return;
        }
        String latest = newer();
        if (latest == null) {
            told = checked;
            return;
        }
        told = true;
        if (latest.equals(WynnChaYuan.config().notifiedVersion())) {
            return;
        }
        WynnChaYuan.config().notifiedVersion(latest);

        String url = downloadUrl();
        tell(client, 
                T.c("chat.update.line1", latest, WynnChaYuan.version())
                        .withStyle(ChatFormatting.AQUA), false);
        Notes notes = notesFor(latest);
        if (notes != null && !notes.headline().isBlank()) {
            tell(client, Component.literal("  " + notes.headline())
                    .withStyle(ChatFormatting.GRAY), false);
        }
        if (SelfUpdate.available()) {
            tell(client,
                    T.c("chat.update.direct").withStyle(style -> style
                            .withColor(ChatFormatting.GREEN).withUnderlined(true)
                            .withClickEvent(new ClickEvent.RunCommand("/wcy update"))
                            .withHoverEvent(new HoverEvent.ShowText(
                                    T.c("chat.update.direct.hover")))), false);
        }
        tell(client, 
                T.c("chat.update.link").withStyle(style -> style.withColor(ChatFormatting.YELLOW)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(url)))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(url)))), false);
        tell(client, 
                T.c("chat.update.where").withStyle(ChatFormatting.DARK_GRAY), false);
    }

    private static void tell(Minecraft client, Component line, boolean overlay) {
        OwnOutputs.note(line);
        client.player.displayClientMessage(line, overlay);
    }

    private static volatile boolean told;

    public static Notes running() {
        return notesFor(WynnChaYuan.version());
    }

    private static JsonObject localised(JsonObject one) {
        String lang = T.pinnedLanguage();
        if (lang == null) {
            try {
                lang = Minecraft.getInstance().getLanguageManager().getSelected();
            } catch (Exception e) {
                ErrorDebug.note("Releases#localised", null, e);
                return one;
            }
        }
        return lang != null && one.has(lang) && one.get(lang).isJsonObject()
                ? one.getAsJsonObject(lang) : one;
    }

    public static List<String> versions() {
        JsonObject o = data;
        if (o == null || !o.has("notes")) {
            return List.of();
        }
        List<String> all = new ArrayList<>(o.getAsJsonObject("notes").keySet());
        all.sort((a, b) -> compare(b, a));
        return List.copyOf(all);
    }

    private static int compare(String a, String b) {
        String[] left = a.split("[._]");
        String[] right = b.split("[._]");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            int x = i < left.length ? number(left[i]) : 0;
            int y = i < right.length ? number(right[i]) : 0;
            if (x != y) {
                return Integer.compare(x, y);
            }
        }
        return 0;
    }

    private static int number(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static Notes notesFor(String version) {
        return notesFor(version, null);
    }

    public static Notes notesFor(String version, String lang) {
        JsonObject o = data;
        if (o == null || !o.has("notes")) {
            return null;
        }
        JsonObject all = o.getAsJsonObject("notes");
        if (!all.has(version)) {
            return null;
        }
        JsonObject raw = all.getAsJsonObject(version);
        JsonObject one = lang == null ? localised(raw)
                : raw.has(lang) && raw.get(lang).isJsonObject() ? raw.getAsJsonObject(lang)
                : raw;
        List<String> items = new ArrayList<>();
        if (one.has("items")) {
            JsonArray rows = one.getAsJsonArray("items");
            for (int i = 0; i < rows.size(); i++) {
                items.add(rows.get(i).getAsString());
            }
        }
        String headline = one.has("headline") ? one.get("headline").getAsString() : "";
        return new Notes(headline, List.copyOf(items));
    }
}
