package com.wynnchayuan;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// 遊戲開著換 jar 會 ZipException；Windows 刪不掉開著的檔
public final class SelfUpdate {

    private SelfUpdate() {}

    public enum State { IDLE, DOWNLOADING, READY, FAILED }

    private static final String BASE = "https://github.com/LyuChaCha/WynnChaYuan/releases/download/";

    private static final long MAX_BYTES = 300L * 1024 * 1024;

    private static volatile State state = State.IDLE;
    private static volatile int percent = -1;
    private static volatile String target;

    private record Pending(Path fresh, Path old) {}

    private static volatile Pending pending;

    public static State state() {
        return state;
    }

    public static int percent() {
        return percent;
    }

    public static String target() {
        return target;
    }

    static String assetUrl(String version) {
        return BASE + "v" + version + "/wynnchayuan-" + version + ".jar";
    }

    static boolean plausibleVersion(String version) {
        return version != null && version.matches("[0-9A-Za-z._+-]{1,40}") && !version.contains("..");
    }

    public static boolean available() {
        return NetworkGate.online() && ownJar() != null;
    }

    static void trace(String message) {
        String line = java.time.LocalDateTime.now().withNano(0) + " " + message;
        System.out.println("[WynnChaYuan] 更新：" + message);
        Path dir = WynnChaYuan.configDir();
        if (dir == null) {
            return;
        }
        try {
            Path file = dir.resolve(LOG);
            java.util.List<String> lines = Files.isRegularFile(file)
                    ? new java.util.ArrayList<>(Files.readAllLines(file, StandardCharsets.UTF_8))
                    : new java.util.ArrayList<>();
            lines.add(line);
            if (lines.size() > LOG_LINES) {
                lines = lines.subList(lines.size() - LOG_LINES, lines.size());
            }
            Files.write(file, lines, StandardCharsets.UTF_8);
        } catch (Throwable ignored) {
        }
    }

    public static void noteScreenOpened() {
        String newer = Releases.newer();
        if (newer == null || newer.equals(screenNoted)) {
            return;
        }
        screenNoted = newer;
        trace("打開更新說明：有新版 " + newer + "，「直接更新」"
                + (available() ? "會出現（jar 在 " + ownJar() + "）" : "不會出現：找不到自己的 jar（模組來源 " + originOf() + "）"));
    }

    private static volatile String screenNoted;

    private static final String LOG = "update-log.txt";
    private static final int LOG_LINES = 200;

    private static String originOf() {
        try {
            return net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getModContainer(WynnChaYuan.MOD_ID)
                    .map(mod -> {
                        var origin = mod.getOrigin();
                        String paths;
                        try {
                            paths = String.valueOf(origin.getPaths());
                        } catch (Throwable t) {
                            paths = "（沒有路徑：" + t.getClass().getSimpleName() + "）";
                        }
                        return origin.getKind() + " " + paths;
                    })
                    .orElse("（找不到模組）");
        } catch (Throwable t) {
            return t.toString();
        }
    }

    static Path ownJar() {
        try {
            return net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getModContainer(WynnChaYuan.MOD_ID)
                    .map(mod -> mod.getOrigin().getPaths())
                    .filter(paths -> paths.size() == 1)
                    .map(paths -> paths.get(0))
                    .filter(path -> Files.isRegularFile(path)
                            && path.getFileName().toString().endsWith(".jar"))
                    .orElse(null);
        } catch (Throwable t) {
            return null;
        }
    }

    static Path holding(Path configDir, String version) {
        return configDir.resolve("update").resolve("wynnchayuan-" + version + ".jar");
    }

    public static synchronized void start(String version) {
        trace("按下更新：要更新到 " + version + "，目前是 " + WynnChaYuan.version() + "，狀態 " + state);
        if (state == State.DOWNLOADING || state == State.READY) {
            return;
        }
        if (!NetworkGate.online()) {
            trace("連線功能沒有同意，不下載");
            return;
        }
        if (!plausibleVersion(version)) {
            trace("版本號不合理，不下載：" + version);
            return;
        }
        Path old = ownJar();
        Path configDir = WynnChaYuan.configDir();
        if (old == null || configDir == null) {
            trace("沒辦法更新：自己的 jar＝" + old + "（模組來源 " + originOf() + "），設定資料夾＝" + configDir);
            state = State.FAILED;
            return;
        }
        state = State.DOWNLOADING;
        percent = -1;
        target = version;
        Thread worker = new Thread(() -> run(version, old, holding(configDir, version)),
                "WynnChaYuan-update");
        worker.setDaemon(true);
        worker.start();
    }

    private static void run(String version, Path old, Path fresh) {
        Path part = fresh.resolveSibling(fresh.getFileName() + ".part");
        try {
            Files.createDirectories(fresh.getParent());
            trace("開始下載 " + assetUrl(version));
            download(assetUrl(version), part);
            if (!isRelease(part, version)) {
                throw new java.io.IOException("下載回來的不是 " + version + " 的 jar");
            }
            SafeFiles.moveAtomically(part, fresh);
            pending = new Pending(fresh, old);
            percent = 100;
            state = State.READY;
            trace("下載完成（" + Files.size(fresh) + " bytes），關遊戲時寫進 " + old);
        } catch (Throwable t) {
            trace("下載失敗：" + t);
            try {
                Files.deleteIfExists(part);
            } catch (Exception ignored) {
            }
            com.wynnchayuan.translate.ErrorDebug.note("update.download", version, t);
            state = State.FAILED;
        }
    }

    private static void download(String url, Path to) throws Exception {
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMinutes(10))
                    .header("User-Agent", "WynnChaYuan")
                    .GET()
                    .build();
            HttpResponse<InputStream> response =
                    client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() != 200) {
                throw new java.io.IOException("HTTP " + response.statusCode());
            }
            long total = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
            if (total > MAX_BYTES) {
                throw new java.io.IOException("檔案大得不合理：" + total);
            }
            long done = 0;
            try (InputStream in = response.body();
                 OutputStream out = Files.newOutputStream(to, StandardOpenOption.CREATE,
                         StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
                byte[] buffer = new byte[64 * 1024];
                int n;
                while ((n = in.read(buffer)) >= 0) {
                    out.write(buffer, 0, n);
                    done += n;
                    if (done > MAX_BYTES) {
                        throw new java.io.IOException("檔案大得不合理");
                    }
                    if (total > 0) {
                        percent = (int) Math.min(99, done * 100 / total);
                    }
                }
            }
            if (total > 0 && done != total) {
                throw new java.io.IOException("只下載到 " + done + " / " + total);
            }
        }
    }

    static boolean isRelease(Path jar, String version) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry entry = zip.getEntry("fabric.mod.json");
            if (entry == null) {
                return false;
            }
            try (InputStreamReader reader = new InputStreamReader(
                    zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                JsonObject mod = JsonParser.parseReader(reader).getAsJsonObject();
                return WynnChaYuan.MOD_ID.equals(mod.get("id").getAsString())
                        && version.equals(mod.get("version").getAsString());
            }
        } catch (Throwable t) {
            return false;
        }
    }

    public static void applyPending() {
        Pending p = pending;
        if (p != null) {
            pending = null;
            trace("關遊戲，把 " + p.fresh() + " 寫進 " + p.old());
            trace(apply(p.fresh(), p.old()) ? "套用成功，下次開遊戲就是新版" : "套用失敗，新檔留在 update 資料夾");
        }
    }

    static boolean apply(Path fresh, Path old) {
        try {
            if (!Files.isRegularFile(fresh) || !Files.isRegularFile(old)) {
                return false;
            }
            try (InputStream in = Files.newInputStream(fresh);
                 OutputStream out = Files.newOutputStream(old,
                         StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                in.transferTo(out);
            }
            Files.deleteIfExists(fresh);
            return true;
        } catch (Throwable t) {
            trace("寫不進去：" + t);
            return false;
        }
    }

    public static Path leftover(Path configDir, String running) {
        try {
            Path dir = configDir.resolve("update");
            if (!Files.isDirectory(dir)) {
                return null;
            }
            Path found = null;
            try (var files = Files.list(dir)) {
                for (Path file : (Iterable<Path>) files::iterator) {
                    String name = file.getFileName().toString();
                    if (!name.startsWith("wynnchayuan-") || !name.endsWith(".jar")) {
                        Files.deleteIfExists(file);
                        continue;
                    }
                    String version = name.substring("wynnchayuan-".length(),
                            name.length() - ".jar".length());
                    if (version.equals(running) || !isRelease(file, version)) {
                        Files.deleteIfExists(file);
                    } else {
                        found = file;
                    }
                }
            }
            return found;
        } catch (Throwable t) {
            return null;
        }
    }

    private static volatile boolean leftoverTold;

    public static void tellLeftover(net.minecraft.client.Minecraft client) {
        if (leftoverTold || client == null || client.player == null) {
            return;
        }
        leftoverTold = true;
        if (state != State.IDLE) {
            return;
        }
        Path configDir = WynnChaYuan.configDir();
        Path file = configDir == null ? null : leftover(configDir, WynnChaYuan.version());
        if (file == null) {
            return;
        }
        net.minecraft.network.chat.Component line = com.wynnchayuan.client.T.c(
                "chat.update.leftover", file.getFileName().toString())
                .withStyle(net.minecraft.ChatFormatting.YELLOW);
        com.wynnchayuan.capture.OwnOutputs.note(line);
        client.player.displayClientMessage(line, false);
    }

    public static void startFromChat(net.minecraft.client.Minecraft client) {
        if (!NetworkGate.online()) {
            chat(client, com.wynnchayuan.client.T.c("chat.update.offline"));
            return;
        }
        String latest = Releases.newer();
        if (latest == null) {
            chat(client, com.wynnchayuan.client.T.c("chat.update.none"));
            return;
        }
        if (!available()) {
            chat(client, com.wynnchayuan.client.T.c("chat.update.unavailable"));
            return;
        }
        if (state == State.READY) {
            chat(client, com.wynnchayuan.client.T.c("chat.update.ready", latest));
            return;
        }
        start(latest);
        if (state == State.FAILED) {
            chat(client, com.wynnchayuan.client.T.c("chat.update.failed"));
            return;
        }
        announce = true;
        chat(client, com.wynnchayuan.client.T.c("chat.update.started", latest));
    }

    public static void announceProgress(net.minecraft.client.Minecraft client) {
        if (!announce || client == null || client.player == null) {
            return;
        }
        if (state == State.READY) {
            announce = false;
            chat(client, com.wynnchayuan.client.T.c("chat.update.ready", target));
        } else if (state == State.FAILED) {
            announce = false;
            chat(client, com.wynnchayuan.client.T.c("chat.update.failed"));
        }
    }

    private static volatile boolean announce;

    private static void chat(net.minecraft.client.Minecraft client,
                             net.minecraft.network.chat.MutableComponent line) {
        if (client == null || client.player == null) {
            return;
        }
        net.minecraft.network.chat.Component shown = line.withStyle(net.minecraft.ChatFormatting.AQUA);
        com.wynnchayuan.capture.OwnOutputs.note(shown);
        client.player.displayClientMessage(shown, false);
    }

    static void reset() {
        state = State.IDLE;
        percent = -1;
        target = null;
        pending = null;
    }

    static void copy(Path from, Path to) throws java.io.IOException {
        Files.createDirectories(to.getParent());
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    }
}
