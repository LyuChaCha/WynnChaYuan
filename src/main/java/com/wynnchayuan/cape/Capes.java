package com.wynnchayuan.cape;

import com.mojang.blaze3d.platform.NativeImage;
import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.Presence;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.Credits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * WynnChaYuan 的披風：誰有、圖從哪裡來、現在該畫哪一格。
 *
 * <h2>誰有披風</h2>
 * 跟葉子徽章同一份名單——{@code credits.json} 的成員多一欄 {@code "cape": "檔名"}，
 * 圖檔放在倉庫根目錄的 {@code capes/}。名單與圖都是下載來的，模組不為披風送任何東西。
 *
 * <p>另外，自己的資料夾 {@code config/wynnchayuan/capes/} 裡放一個 {@code self.png} 或
 * {@code self.gif}，自己身上就會穿著它——<b>只有自己看得到</b>，給還沒登記的人先看效果、
 * 給做圖的人邊改邊看（存檔之後幾秒內就換上）。
 *
 * <h2>上傳的披風</h2>
 * 玩家也可以在 F6 上傳自己的披風（見 {@link Presence#upload}）。登記站的名單會寫誰傳了、
 * 是哪一版；有上傳的人用上傳的那一件，沒有才看 {@code credits.json} 登記的。
 *
 * <h2>別人看不看得到</h2>
 * 預設大家都看得到。本人在 F6 關掉「穿上我的披風」，那個選擇跟著報到送出去，
 * 名單上會記著；別人那邊就不畫他的披風了。名單抓不到的時候照預設畫。
 *
 * <h2>怎麼畫上去</h2>
 * 原版的披風層畫的是 {@code state.skin.cape()} 那張貼圖。{@code CapeLayerMixin} 在它開畫
 * 之前把那一欄換成我們的、畫完換回來。Wynntils 自己的披風另有一層，
 * {@code WynntilsCapeMixin} 讓它對穿著我們披風的人讓開。
 */
public final class Capes {

    private Capes() {}

    private static final List<String> SOURCES = List.of(
            "https://raw.githubusercontent.com/LyuChaCha/WynnChaYuan/main/capes/",
            "https://cdn.jsdelivr.net/gh/LyuChaCha/WynnChaYuan@main/capes/");

    /** 檔名只收這個樣子：它要拿去拼網址與路徑。 */
    static final Pattern FILE = Pattern.compile("[a-z0-9][a-z0-9_-]{0,40}\\.(png|gif)");

    /** 圖檔的大小上限。 */
    static final int MAX_BYTES = 21 * 1024 * 1024;

    /** 下載來的圖放多久再重抓一次。 */
    private static final long REFRESH_MS = 24 * 3600_000L;

    /** 失敗之後多久再試。 */
    private static final long RETRY_MS = 5 * 60_000L;

    /** 自己資料夾裡那張預覽圖多久看一次有沒有換。 */
    private static final long LOCAL_CHECK_MS = 2000;

    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    /** 一件載好的披風：每一格的貼圖，與每一格結束在動畫的第幾毫秒。 */
    private record Loaded(Identifier[] textures, int[] ends) {
        Identifier at(long now) {
            if (textures.length == 1) {
                return textures[0];
            }
            int total = ends[ends.length - 1];
            int t = (int) Math.floorMod(now, (long) total);
            for (int i = 0; i < ends.length; i++) {
                if (t < ends[i]) {
                    return textures[i];
                }
            }
            return textures[textures.length - 1];
        }
    }

    /** 正在載、或載不起來（幾點之後可以再試）。 */
    private record Pending(long retryAt) {}

    private static final Map<String, Object> STATE = new ConcurrentHashMap<>();

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "WynnChaYuan-capes");
        t.setDaemon(true);
        return t;
    });

    // ------------------------------------------------------------ 誰穿哪一件

    private static volatile List<Credits.Section> tableFor;
    private static volatile Map<String, String> table = Map.of();

    /** 名單上這個人登記的披風檔名；沒有就回 {@code null}。 */
    public static String registered(String account) {
        if (account == null) {
            return null;
        }
        List<Credits.Section> sections = Credits.sections();
        if (sections != tableFor) {
            table = build(sections);
            tableFor = sections;
        }
        return table.get(account.toLowerCase(Locale.ROOT));
    }

    static Map<String, String> build(List<Credits.Section> sections) {
        Map<String, String> out = new HashMap<>();
        for (Credits.Section section : sections) {
            for (Credits.Member member : section.members()) {
                if (member.hasHead() && member.cape() != null
                        && FILE.matcher(member.cape()).matches()) {
                    out.putIfAbsent(member.mc().toLowerCase(Locale.ROOT), member.cape());
                }
            }
        }
        return Map.copyOf(out);
    }

    /** 放自己預覽圖的資料夾。 */
    public static Path folder() {
        Path dir = WynnChaYuan.configDir();
        return dir == null ? null : dir.resolve("capes");
    }

    private static volatile long localChecked;
    private static volatile String localKey;

    /** 自己資料夾裡的預覽圖（{@code self.gif} 優先）；沒有就回 {@code null}。隔幾秒才真的去看檔案。 */
    private static String localKey() {
        long now = System.currentTimeMillis();
        if (now - localChecked < LOCAL_CHECK_MS) {
            return localKey;
        }
        localChecked = now;
        String found = null;
        try {
            Path dir = folder();
            if (dir != null) {
                for (String name : selfFiles()) {
                    Path file = dir.resolve(name);
                    if (Files.isRegularFile(file)) {
                        // 改過的檔時間不同，鍵就不同：存檔之後自動換上新的
                        found = "local:" + name + ":" + Files.getLastModifiedTime(file).toMillis();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            found = null;
        }
        localKey = found;
        return found;
    }

    /**
     * 自己資料夾裡認哪幾個檔名。
     *
     * <p>會動的披風（GIF）是名單成員才有的（使用者 2026-10-12：「應該是只能用 PNG，否則別人
     * 上傳 gif 一般用戶也能使用」）：不在名單上的人只認 {@code self.png}，放了 {@code self.gif}
     * 也不會穿上——不然從一覽把別人的 GIF 抓下來放進資料夾，自己身上就有了。
     */
    static String[] selfFiles() {
        return selfFiles(!com.wynnchayuan.render.Badges.registered(
                com.wynnchayuan.render.Badges.selfName()).isEmpty()
                || Presence.member());
    }

    static String[] selfFiles(boolean member) {
        return member ? new String[] {"self.gif", "self.png"} : new String[] {"self.png"};
    }

    /** 自己資料夾裡那張預覽圖的路徑（{@code self.gif} 優先）；沒有就回 {@code null}。 */
    public static Path localFile() {
        try {
            Path dir = folder();
            if (dir != null) {
                for (String name : selfFiles()) {
                    if (Files.isRegularFile(dir.resolve(name))) {
                        return dir.resolve(name);
                    }
                }
            }
        } catch (Exception e) {
            // 當成沒有
        }
        return null;
    }

    /** 這個人有披風嗎（自己的話，資料夾裡的預覽圖也算）。 */
    public static boolean hasCape(String account, java.util.UUID uuid, boolean self) {
        return registered(account) != null || Presence.capeVersion(uuid) > 0
                || (self && localKey() != null);
    }

    /** 上傳的披風在這裡的鍵：代碼加版本，換了一件鍵就不同。 */
    private static String uploadedKey(java.util.UUID uuid) {
        long version = Presence.capeVersion(uuid);
        return version > 0 ? "up:" + Presence.idOf(uuid) + ":" + version : null;
    }

    /**
     * 這個人現在該畫哪一張貼圖。
     *
     * @return 貼圖；沒有披風、被關掉、或還沒載好時回 {@code null}（照原本的畫）
     */
    public static Identifier textureFor(String account, java.util.UUID uuid, boolean self,
                                        CollectorConfig config, long now) {
        if (account == null || config == null || !config.capes()) {
            return null;
        }
        String key;
        if (self) {
            if (!config.ownCape()) {
                return null;
            }
            // 資料夾裡的預覽圖最優先：那是正在改的那一張
            key = localKey();
            if (key == null) {
                key = uploadedKey(uuid);
            }
            if (key == null) {
                key = registered(account);
            }
        } else {
            if (Presence.capeHidden(uuid)) {
                return null;               // 本人關掉了
            }
            key = uploadedKey(uuid);
            if (key == null) {
                key = registered(account);
            }
        }
        if (key == null) {
            return null;
        }
        return loaded(key, now);
    }

    /** 這個鍵的披風載好了就回現在那一格；還沒載就開始載、先回 {@code null}。 */
    private static Identifier loaded(String key, long now) {
        Object state = STATE.get(key);
        if (state instanceof Loaded loaded) {
            return loaded.at(now);
        }
        if (state == null || (state instanceof Pending pending && pending.retryAt() > 0
                && now >= pending.retryAt())) {
            STATE.put(key, new Pending(0));
            WORKER.execute(() -> load(key));
        }
        return null;
    }

    // ------------------------------------------------------------ 掛點用

    private static Player playerOf(AvatarRenderState state) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || state == null) {
            return null;
        }
        Entity entity = mc.level.getEntity(state.id);
        return entity instanceof Player player ? player : null;
    }

    private static Identifier textureFor(Player player) {
        if (player == null) {
            return null;
        }
        String account = player.getGameProfile().name();
        return textureFor(account, player.getUUID(), isSelf(account), WynnChaYuan.config(),
                System.currentTimeMillis());
    }

    private static boolean isSelf(String account) {
        Minecraft mc = Minecraft.getInstance();
        return account != null && mc != null && account.equalsIgnoreCase(mc.getUser().getName());
    }

    /**
     * 把這個玩家的披風貼圖換成我們的。
     *
     * @return 換之前的 skin（畫完要換回去）；沒有換就回 {@code null}
     */
    public static PlayerSkin swap(AvatarRenderState state) {
        try {
            Identifier texture = textureFor(playerOf(state));
            if (texture == null || state.skin == null) {
                return null;
            }
            PlayerSkin before = state.skin;
            state.skin = new PlayerSkin(before.body(),
                    new ClientAsset.ResourceTexture(texture, texture),
                    before.elytra(), before.model(), before.secure());
            return before;
        } catch (Throwable t) {
            return null;                   // 這只是裝飾，出事就照原本的畫
        }
    }

    /** Wynntils 要不要對這個玩家讓開：他穿著我們的披風，而且貼圖已經載好了。 */
    public static boolean overrides(Player player) {
        try {
            return textureFor(player) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    // ------------------------------------------------------------ 載入

    private static void load(String key) {
        try {
            byte[] bytes = key.startsWith("local:") ? readLocal(key.split(":")[1])
                    : key.startsWith("up:") ? readUploaded(key) : readRemote(key);
            List<CapeImage.Frame> frames = bytes == null ? null : decode(bytes);
            if (frames == null || frames.isEmpty()) {
                STATE.put(key, new Pending(System.currentTimeMillis() + RETRY_MS));
                return;
            }
            Minecraft.getInstance().execute(() -> register(key, frames));
        } catch (Throwable t) {
            com.wynnchayuan.translate.ErrorDebug.note("cape.load", key, t);
            STATE.put(key, new Pending(System.currentTimeMillis() + RETRY_MS));
        }
    }

    private static byte[] readLocal(String name) throws Exception {
        Path file = folder().resolve(name);
        return Files.size(file) > MAX_BYTES ? null : Files.readAllBytes(file);
    }

    /** 別人上傳的披風：鍵裡帶著版本，同一版的內容不會變，抓過一次就一直用本機那一份。 */
    private static byte[] readUploaded(String key) throws Exception {
        String[] parts = key.split(":");
        Path cache = folder().resolve("cache").resolve("up-" + parts[1] + "-" + parts[2]);
        if (Files.isRegularFile(cache) && Files.size(cache) <= MAX_BYTES) {
            return Files.readAllBytes(cache);
        }
        byte[] fresh = Presence.capeBytes(parts[1], Long.parseLong(parts[2]));
        if (fresh != null) {
            Files.createDirectories(cache.getParent());
            Path tmp = cache.resolveSibling(cache.getFileName() + ".tmp");
            Files.write(tmp, fresh);
            com.wynnchayuan.SafeFiles.moveAtomically(tmp, cache);
        }
        return fresh;
    }

    /** 下載來的圖在本機留一份；一天之內不重抓，抓不到就用舊的。 */
    private static byte[] readRemote(String file) throws Exception {
        if (!FILE.matcher(file).matches()) {
            return null;
        }
        Path cache = folder().resolve("cache").resolve(file);
        boolean have = Files.isRegularFile(cache) && Files.size(cache) <= MAX_BYTES;
        if (have && System.currentTimeMillis() - Files.getLastModifiedTime(cache).toMillis()
                < REFRESH_MS) {
            return Files.readAllBytes(cache);
        }
        byte[] fresh = download(file);
        if (fresh != null) {
            Files.createDirectories(cache.getParent());
            Path tmp = cache.resolveSibling(file + ".tmp");
            Files.write(tmp, fresh);
            com.wynnchayuan.SafeFiles.moveAtomically(tmp, cache);
            return fresh;
        }
        return have ? Files.readAllBytes(cache) : null;
    }

    private static byte[] download(String file) {
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL).build()) {
            for (String base : SOURCES) {
                try {
                    HttpRequest request = HttpRequest.newBuilder(URI.create(base + file))
                            .timeout(TIMEOUT)
                            .header("User-Agent", "WynnChaYuan")
                            .GET()
                            .build();
                    HttpResponse<InputStream> response =
                            client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    try (InputStream in = response.body()) {
                        if (response.statusCode() != 200) {
                            continue;
                        }
                        byte[] bytes = in.readNBytes(MAX_BYTES + 1);
                        if (bytes.length <= MAX_BYTES && bytes.length > 0) {
                            return bytes;
                        }
                    }
                } catch (Exception e) {
                    // 換下一個來源
                }
            }
        } catch (Exception e) {
            // 連不上就當沒抓到
        }
        return null;
    }

    /** 圖檔 → 排好版型的每一格。GIF 自己讀，PNG 交給遊戲。 */
    static List<CapeImage.Frame> decode(byte[] bytes) throws Exception {
        List<CapeImage.Frame> out = new ArrayList<>();
        if (Gif.looksLikeGif(bytes)) {
            // 解一格排一格：大張的動畫不把每一格原圖都留著（見 Gif#each）
            int count = Math.max(1, Math.min(CapeImage.MAX_FRAMES, Gif.countFrames(bytes)));
            Gif.each(bytes, CapeImage.MAX_FRAMES, (canvas, width, height, delayMs) ->
                    out.add(CapeImage.layoutDetached(canvas, width, height, count, delayMs)));
            return out;
        }
        try (NativeImage image = NativeImage.read(bytes)) {
            int w = image.getWidth();
            int h = image.getHeight();
            if (w > 4096 || h > 8192) {
                return out;
            }
            int[] pixels = new int[w * h];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    pixels[y * w + x] = image.getPixel(x, y);
                }
            }
            if (CapeImage.stripFrames(w, h) > 0) {
                out.addAll(CapeImage.strip(pixels, w, h));
            } else {
                out.add(CapeImage.layout(pixels, w, h, 1, 0));
            }
        }
        return out;
    }

    /** 在畫圖的執行緒上把每一格做成貼圖。 */
    private static void register(String key, List<CapeImage.Frame> frames) {
        try {
            Minecraft mc = Minecraft.getInstance();
            String safe = Integer.toHexString(key.hashCode());
            Identifier[] textures = new Identifier[frames.size()];
            int[] ends = new int[frames.size()];
            int clock = 0;
            for (int i = 0; i < frames.size(); i++) {
                CapeImage.Frame frame = frames.get(i);
                NativeImage image = new NativeImage(frame.width(), frame.height(), false);
                for (int y = 0; y < frame.height(); y++) {
                    for (int x = 0; x < frame.width(); x++) {
                        image.setPixel(x, y, frame.argb()[y * frame.width() + x]);
                    }
                }
                Identifier id = Identifier.fromNamespaceAndPath(
                        WynnChaYuan.MOD_ID, "cape/" + safe + "/" + i);
                String label = "wynnchayuan cape " + safe + " #" + i;
                mc.getTextureManager().register(id, new DynamicTexture(() -> label, image));
                textures[i] = id;
                clock += Math.max(20, frame.delayMs());
                ends[i] = clock;
            }
            Object old = STATE.put(key, new Loaded(textures, ends));
            release(old);
            // 預覽圖換了新的：舊的那幾張貼圖不會再用到
            if (key.startsWith("local:")) {
                STATE.entrySet().removeIf(e -> {
                    boolean stale = e.getKey().startsWith("local:") && !e.getKey().equals(key);
                    if (stale) {
                        release(e.getValue());
                    }
                    return stale;
                });
            }
        } catch (Throwable t) {
            com.wynnchayuan.translate.ErrorDebug.note("cape.register", key, t);
            STATE.put(key, new Pending(System.currentTimeMillis() + RETRY_MS));
        }
    }

    private static void release(Object state) {
        if (state instanceof Loaded loaded) {
            for (Identifier id : loaded.textures()) {
                Minecraft.getInstance().getTextureManager().release(id);
            }
        }
    }
}
