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

/**
 * 在遊戲裡把模組更新到最新版（使用者 2026-10-12：「不用去 GitHub 重新下載」）。
 *
 * <h2>做什麼</h2>
 * 玩家在「更新說明」按下更新 → 背景下載 GitHub Releases 上那一版的 jar → 確認它真的是
 * 這個模組的那一版 → <b>關閉遊戲的時候</b>蓋掉 mods 資料夾裡現在這個檔。下次開遊戲就是新版。
 *
 * <h2>為什麼等到關遊戲才換</h2>
 * 遊戲開著的時候 jar 是被打開的：之後第一次用到的類別要從裡面讀，檔案中途被換掉，
 * 讀出來就是壞的（{@code ZipException}）。這個專案自己踩過——測試時在遊戲開著的時候換 jar，
 * 之後開某個畫面就崩了。所以下載放在別的資料夾，等 JVM 要結束了才動 mods 裡那個檔。
 *
 * <h2>為什麼是「蓋過去」而不是「放新檔、刪舊檔」</h2>
 * Windows 上打開中的檔案刪不掉，但寫得進去。把新版的內容寫進舊檔原本的位置，是 Wynntils
 * 用了很多年的做法。代價是<b>檔名不會變</b>：mods 裡還是舊版號的檔名，裡面是新版
 * （F6 與模組清單顯示的版本是對的）。
 *
 * <h2>只下載，不送東西</h2>
 * 網址是寫死的 GitHub Releases，版本號來自 {@link Releases#newer}。不照線上任何欄位去別的
 * 地方下載——能被遠端改的網址加上會被執行的檔案，是最不該放在一起的兩件事。
 * 也不會自己動：一定是玩家按了才下載。
 */
public final class SelfUpdate {

    private SelfUpdate() {}

    /** 現在在哪一步。 */
    public enum State { IDLE, DOWNLOADING, READY, FAILED }

    private static final String BASE = "https://github.com/LyuChaCha/WynnChaYuan/releases/download/";

    /** 一個 jar 不該比這個大。現在大約 45 MB；讀到離譜的大小就不要了。 */
    private static final long MAX_BYTES = 300L * 1024 * 1024;

    private static volatile State state = State.IDLE;
    private static volatile int percent = -1;
    private static volatile String target;

    /** 下載好、等關遊戲時套用的那一份：新檔在哪、要蓋掉的舊檔在哪。 */
    private record Pending(Path fresh, Path old) {}

    private static volatile Pending pending;

    public static State state() {
        return state;
    }

    /** 下載到幾成（0～100）；還不知道總長度時是 −1。 */
    public static int percent() {
        return percent;
    }

    /** 正在下載、或已經下載好等著套用的是哪一版；沒有就回 {@code null}。 */
    public static String target() {
        return target;
    }

    /** 這一版的 jar 在 GitHub Releases 的網址。tag 是 {@code v版本}，檔名是 {@code wynnchayuan-版本.jar}。 */
    static String assetUrl(String version) {
        return BASE + "v" + version + "/wynnchayuan-" + version + ".jar";
    }

    /** 版本號只會有這些字。別的東西不拿去拼網址與檔名。 */
    static boolean plausibleVersion(String version) {
        return version != null && version.matches("[0-9A-Za-z._+-]{1,40}") && !version.contains("..");
    }

    /**
     * 能不能在遊戲裡更新：要找得到自己那個 jar。
     *
     * <p>開發環境裡模組是一個資料夾不是 jar，沒有東西可以換；那時候這個功能不出現。
     */
    public static boolean available() {
        return ownJar() != null;
    }

    /** mods 資料夾裡現在跑的這個 jar；找不到（或不是一個檔）就回 {@code null}。 */
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

    /** 下載放在哪：設定資料夾底下，不放 mods——半截的檔出現在 mods 裡，下次開遊戲會被當成模組載入。 */
    static Path holding(Path configDir, String version) {
        return configDir.resolve("update").resolve("wynnchayuan-" + version + ".jar");
    }

    /**
     * 開始下載這一版。已經在下載、或已經下載好了就不重來。
     *
     * @param version 要更新到的版本（{@link Releases#newer} 給的）
     */
    public static synchronized void start(String version) {
        if (state == State.DOWNLOADING || state == State.READY || !plausibleVersion(version)) {
            return;
        }
        Path old = ownJar();
        Path configDir = WynnChaYuan.configDir();
        if (old == null || configDir == null) {
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
            download(assetUrl(version), part);
            if (!isRelease(part, version)) {
                throw new java.io.IOException("下載回來的不是 " + version + " 的 jar");
            }
            SafeFiles.moveAtomically(part, fresh);
            pending = new Pending(fresh, old);
            percent = 100;
            state = State.READY;
        } catch (Throwable t) {
            try {
                Files.deleteIfExists(part);
            } catch (Exception ignored) {
                // 留著也只是設定資料夾裡的一個暫存檔
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

    /**
     * 這個檔是不是這個模組的那一版。
     *
     * <p>讀得開的 zip（半截的檔沒有目錄區，開不起來）、裡面有 {@code fabric.mod.json}、
     * 模組 ID 是我們、版本就是要的那一版。錯誤頁面、別的模組、別的版本都過不了。
     */
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

    /**
     * 關遊戲時叫的：有下載好的新版就蓋到舊檔上。
     *
     * <p>由模組<b>自己那個</b>關機掛鉤在存完檔之後叫（見 {@code WynnChaYuan#onInitializeClient}），
     * 不另外掛一個——JVM 的關機掛鉤是同時跑的，另外掛的話可能在我們還在存 captured.json 的
     * 時候就把 jar 換掉，存檔要用的類別若還沒載入過，就會從換掉的檔裡讀出壞東西。
     */
    public static void applyPending() {
        Pending p = pending;
        if (p != null) {
            pending = null;
            apply(p.fresh(), p.old());
        }
    }

    /**
     * 把新版的內容寫進舊檔的位置，成功就把暫放的那一份刪掉。
     *
     * <p>用「打開舊檔、清空、寫入」而不是「刪掉再放」——見類別說明。失敗的話兩個檔都留著：
     * 舊的照常能用，新的還在設定資料夾，{@link #leftover} 會在下次開遊戲時看到它。
     *
     * @return 套用成功
     */
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
            return false;
        }
    }

    /**
     * 上一次下載好、卻沒有套用成功的那一版（關遊戲時寫不進去）。
     *
     * <p>已經在跑的那一版留下的檔案是套用成功後沒刪乾淨的，順手清掉。
     *
     * @return 還躺在設定資料夾裡的新版 jar；沒有就回 {@code null}
     */
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
                        Files.deleteIfExists(file);          // 半截的 .part
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

    /**
     * 上次下載的更新沒有套用成功的話，在聊天室說一次檔案在哪。
     *
     * <p>平常走不到：關遊戲時寫得進去，檔案就被刪掉了。寫不進去（檔案唯讀、防毒軟體擋著）
     * 的時候，玩家會以為更新了而其實沒有——至少要讓他知道，還有手動怎麼換。
     */
    public static void tellLeftover(net.minecraft.client.Minecraft client) {
        if (leftoverTold || client == null || client.player == null) {
            return;
        }
        leftoverTold = true;
        if (state != State.IDLE) {
            return;      // 這場已經按過更新了：資料夾裡的是正在下載、或等著套用的那一份
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

    /** 測試用：回到還沒開始的樣子。 */
    static void reset() {
        state = State.IDLE;
        percent = -1;
        target = null;
        pending = null;
    }

    /** 測試用：把檔案搬成另一個（給 {@link #leftover} 的測試準備資料）。 */
    static void copy(Path from, Path to) throws java.io.IOException {
        Files.createDirectories(to.getParent());
        Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);
    }
}
