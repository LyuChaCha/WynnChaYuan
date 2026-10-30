package com.wynnchayuan.translate;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;

public final class RemoteSync {

    private static final String PATH = "src/main/resources/assets/wynnchayuan/translations/";

    private static final List<String> SOURCES = List.of(
            "https://raw.githubusercontent.com/LyuChaCha/WynnChaYuan/main/" + PATH,
            "https://cdn.jsdelivr.net/gh/LyuChaCha/WynnChaYuan@main/" + PATH);

    private static final String INDEX = "_index.json";

    private static List<String> files(Path cacheDir, String lang) {
        java.util.LinkedHashSet<String> names =
                new java.util.LinkedHashSet<>(FileIndex.bundled(lang));
        names.addAll(FileIndex.inDirectory(cacheDir));
        names.addAll(FileIndex.SCOPED);
        return List.copyOf(names);
    }

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private static volatile String lastResult;
    private static volatile boolean blocked;

    private static boolean allowed() {
        blocked = !com.wynnchayuan.NetworkGate.online();
        return !blocked;
    }

    private RemoteSync() {}

    private static final int FAILED = 0;

    private static final int SAME = 1;

    private static final int CHANGED = 2;

    private static int fetchOne(HttpClient client, Path cacheDir,
                                String lang, String name) {
        Path target = cacheDir.resolve(name);
        Path tmp = cacheDir.resolve(name + ".tmp");
        for (String base : SOURCES) {
            try {
                HttpRequest request = HttpRequest.newBuilder(
                        URI.create(base + Languages.path(lang) + name))
                        .timeout(TIMEOUT)
                        .header("User-Agent", "WynnChaYuan")
                        .header("Cache-Control", "no-cache")
                        .header("Pragma", "no-cache")
                        .GET()
                        .build();
                HttpResponse<InputStream> response =
                        client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    continue;
                }
                Files.createDirectories(tmp.getParent());
                try (InputStream in = response.body()) {
                    Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                }
                if (!com.wynnchayuan.SafeFiles.parsesAsObject(tmp)) {
                    Files.deleteIfExists(tmp);
                    continue;
                }
                if (Files.isRegularFile(target) && Files.mismatch(tmp, target) == -1L) {
                    Files.deleteIfExists(tmp);
                    return SAME;
                }
                com.wynnchayuan.SafeFiles.moveAtomically(tmp, target);
                return CHANGED;
            } catch (Exception e) {
                ErrorDebug.note("RemoteSync#fetchOne", null, e);
                try {
                    Files.deleteIfExists(tmp);
                } catch (Exception failure) {
                    ErrorDebug.note("RemoteSync#fetchOne", null, failure);
                }
            }
        }
        return FAILED;
    }

    public static String lastResult() {
        if (blocked) {
            return com.wynnchayuan.client.T.s("status.sync_blocked");
        }
        return lastResult != null ? lastResult : com.wynnchayuan.client.T.s("status.sync_none");
    }

    public static String remoteVersion() {
        if (!allowed()) {
            return null;
        }
        String url = "https://api.github.com/repos/LyuChaCha/WynnChaYuan/commits"
                + "?path=" + PATH + "&per_page=1";
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "WynnChaYuan")
                    .header("Accept", "application/vnd.github+json")
                    .GET()
                    .build();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }
            com.google.gson.JsonElement root =
                    com.google.gson.JsonParser.parseString(response.body());
            if (!root.isJsonArray() || root.getAsJsonArray().isEmpty()) {
                return null;
            }
            com.google.gson.JsonObject commit =
                    root.getAsJsonArray().get(0).getAsJsonObject();
            com.google.gson.JsonElement sha = commit.get("sha");
            if (sha == null || !sha.isJsonPrimitive()) {
                return null;
            }
            lastDate = dateOf(commit);
            return sha.getAsString();
        } catch (Exception e) {
            ErrorDebug.note("RemoteSync#remoteVersion", null, e);
            return null;
        }
    }

    private static String dateOf(com.google.gson.JsonObject commit) {
        try {
            String date = commit.getAsJsonObject("commit")
                    .getAsJsonObject("committer").get("date").getAsString();
            int t = date.indexOf('T');
            return t > 0 ? date.substring(0, t) : date;
        } catch (Exception e) {
            ErrorDebug.note("RemoteSync#dateOf", null, e);
            return null;
        }
    }

    public static String lastRemoteDate() {
        return lastDate;
    }

    private static volatile String lastDate;

    public static int fetchInto(Path cacheDir) {
        return fetchInto(cacheDir, Languages.DEFAULT);
    }

    public static int fetchInto(Path cacheDir, String lang) {
        return fetchInto(cacheDir, lang, null);
    }

    private static final List<String> CREDITS = List.of(
            "https://raw.githubusercontent.com/LyuChaCha/WynnChaYuan/main/"
                    + "src/main/resources/assets/wynnchayuan/credits.json",
            "https://cdn.jsdelivr.net/gh/LyuChaCha/WynnChaYuan@main/"
                    + "src/main/resources/assets/wynnchayuan/credits.json");

    public static boolean fetchCredits(Path configDir) {
        if (!allowed()) {
            return false;
        }
        Path target = configDir.resolve(com.wynnchayuan.client.Credits.FILE);
        Path tmp = configDir.resolve(com.wynnchayuan.client.Credits.FILE + ".tmp");
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()) {
            for (String url : CREDITS) {
                try {
                    HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                            .timeout(TIMEOUT)
                            .header("User-Agent", "WynnChaYuan")
                            .header("Cache-Control", "no-cache")
                            .header("Pragma", "no-cache")
                            .GET()
                            .build();
                    HttpResponse<InputStream> response =
                            client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    if (response.statusCode() != 200) {
                        continue;
                    }
                    try (InputStream in = response.body()) {
                        Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                    }
                    if (!com.wynnchayuan.SafeFiles.parsesAsObject(tmp)) {
                        Files.deleteIfExists(tmp);
                        continue;
                    }
                    if (Files.isRegularFile(target) && Files.mismatch(tmp, target) == -1L) {
                        Files.deleteIfExists(tmp);
                        return false;
                    }
                    com.wynnchayuan.SafeFiles.moveAtomically(tmp, target);
                    return true;
                } catch (Exception e) {
                    ErrorDebug.note("RemoteSync#fetchCredits", null, e);
                    try {
                        Files.deleteIfExists(tmp);
                    } catch (Exception failure) {
                        ErrorDebug.note("RemoteSync#fetchCredits", null, failure);
                    }
                }
            }
        } catch (Exception e) {
            ErrorDebug.note("RemoteSync#fetchCredits", null, e);
        }
        return false;
    }

    @FunctionalInterface
    public interface Progress {
        void at(int done, int total);
    }

    public static int fetchInto(Path cacheDir, String lang, Progress progress) {
        if (!allowed()) {
            return 0;
        }
        int ok = 0;
        int failed = 0;
        int changed = 0;
        try {
            Files.createDirectories(cacheDir);
        } catch (Exception e) {
            lastResult = com.wynnchayuan.client.T.s("status.sync_cache_failed", e.getMessage());
            return 0;
        }

        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()) {

            int index = fetchOne(client, cacheDir, lang, INDEX);
            if (index == CHANGED) {
                changed++;
            }

            List<String> names = files(cacheDir, lang);
            java.util.Set<String> fetched = new java.util.HashSet<>();
            if (progress != null) {
                progress.at(0, names.size());
            }
            for (String name : names) {
                int got = fetchOne(client, cacheDir, lang, name);
                if (got == FAILED) {
                    failed++;
                } else {
                    ok++;
                    fetched.add(name);
                    if (got == CHANGED) {
                        changed++;
                    }
                }
                if (progress != null) {
                    progress.at(ok + failed, names.size());
                }
            }
            changed += TranslationCache.record(cacheDir, fetched, names, index != FAILED);
        } catch (Exception e) {
            lastResult = com.wynnchayuan.client.T.s("status.sync_offline", e.getMessage());
            return changed;
        }

        lastResult = failed == 0
                ? com.wynnchayuan.client.T.s("status.sync_ok", ok, changed)
                : com.wynnchayuan.client.T.s("status.sync_partial", ok, failed);
        return changed;
    }
}
