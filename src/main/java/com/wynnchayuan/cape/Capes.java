package com.wynnchayuan.cape;

import com.mojang.blaze3d.platform.NativeImage;
import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.NetworkGate;
import com.wynnchayuan.Presence;
import com.wynnchayuan.PresenceAccount;
import com.wynnchayuan.SafeFiles;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.Credits;
import com.wynnchayuan.translate.ErrorDebug;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class Capes {

    private Capes() {}

    private static final List<String> SOURCES = List.of(
            "https://raw.githubusercontent.com/LyuChaCha/WynnChaYuan/main/capes/",
            "https://cdn.jsdelivr.net/gh/LyuChaCha/WynnChaYuan@main/capes/");

    static final Pattern FILE = Pattern.compile("[a-z0-9][a-z0-9_-]{0,40}\\.(png|gif)");

    static final int MAX_BYTES = 21 * 1024 * 1024;

    private static final long REFRESH_MS = 24 * 3600_000L;

    private static final long RETRY_MS = 5 * 60_000L;


    private static final Duration TIMEOUT = Duration.ofSeconds(20);

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

    private record Pending(long retryAt) {}

    private static final Map<String, Object> STATE = new ConcurrentHashMap<>();

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "WynnChaYuan-capes");
        t.setDaemon(true);
        return t;
    });

    private static volatile List<Credits.Section> tableFor;
    private static volatile Map<String, String> table = Map.of();

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

    private static Path folder() {
        Path dir = WynnChaYuan.configDir();
        return dir == null ? null : dir.resolve("capes");
    }

    public static boolean hasCape(String account, UUID uuid) {
        return registered(account) != null || Presence.capeVersion(uuid) > 0;
    }

    private static String uploadedKey(UUID uuid) {
        long version = Presence.capeVersion(uuid);
        return version > 0 ? "up:" + Presence.idOf(uuid) + ":" + version : null;
    }

    public static Identifier textureFor(String account, UUID uuid, boolean self,
                                        CollectorConfig config, long now) {
        if (account == null || config == null || !config.capes()
                || !NetworkGate.registry(config)) {
            return null;
        }
        if (self ? !config.ownCape() : Presence.capeHidden(uuid)) {
            return null;
        }
        String key = uploadedKey(uuid);
        if (key == null) {
            key = registered(account);
        }
        if (key == null) {
            return null;
        }
        return loaded(key, now);
    }

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
            ErrorDebug.note("Capes#swap", null, t);
            return null;
        }
    }

    public static boolean overrides(Player player) {
        try {
            return textureFor(player) != null;
        } catch (Throwable t) {
            ErrorDebug.note("Capes#overrides", null, t);
            return false;
        }
    }

    private static void load(String key) {
        try {
            byte[] bytes = key.startsWith("up:") ? readUploaded(key) : readRemote(key);
            List<CapeImage.Frame> frames = bytes == null ? null : decode(bytes);
            if (frames == null || frames.isEmpty()) {
                STATE.put(key, new Pending(System.currentTimeMillis() + RETRY_MS));
                return;
            }
            Minecraft.getInstance().execute(() -> register(key, frames));
        } catch (Throwable t) {
            ErrorDebug.note("cape.load", key, t);
            STATE.put(key, new Pending(System.currentTimeMillis() + RETRY_MS));
        }
    }

    private static byte[] readUploaded(String key) throws Exception {
        String[] parts = key.split(":");
        Path cache = folder().resolve("cache").resolve("up-" + parts[1] + "-" + parts[2]);
        if (Files.isRegularFile(cache) && Files.size(cache) <= MAX_BYTES) {
            return Files.readAllBytes(cache);
        }
        byte[] fresh = PresenceAccount.capeBytes(parts[1], Long.parseLong(parts[2]));
        if (fresh != null) {
            Files.createDirectories(cache.getParent());
            Path tmp = cache.resolveSibling(cache.getFileName() + ".tmp");
            Files.write(tmp, fresh);
            SafeFiles.moveAtomically(tmp, cache);
        }
        return fresh;
    }

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
            SafeFiles.moveAtomically(tmp, cache);
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
                    ErrorDebug.note("Capes#download", null, e);
                }
            }
        } catch (Exception e) {
            ErrorDebug.note("Capes#download", null, e);
        }
        return null;
    }

    static List<CapeImage.Frame> decode(byte[] bytes) throws Exception {
        List<CapeImage.Frame> out = new ArrayList<>();
        if (Gif.looksLikeGif(bytes)) {
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

    public static int forgetSelf(UUID me) {
        String id = Presence.idOf(me);
        Path dir = folder();
        if (id == null || dir == null) {
            return 0;
        }
        for (String key : List.copyOf(STATE.keySet())) {
            if (key.startsWith("up:" + id + ":")) {
                release(STATE.remove(key));
            }
        }
        return deleteUploaded(dir.resolve("cache"), id);
    }

    public static int deleteUploaded(Path cache, String id) {
        int removed = 0;
        if (!Files.isDirectory(cache)) {
            return 0;
        }
        try (Stream<Path> files = Files.list(cache)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (name.startsWith("up-" + id + "-")) {
                    try {
                        Files.deleteIfExists(file);
                        removed++;
                    } catch (Exception failure) {
                        ErrorDebug.note("Capes#deleteUploaded", null, failure);
                    }
                }
            }
        } catch (Exception failure) {
            ErrorDebug.note("Capes#deleteUploaded", null, failure);
        }
        return removed;
    }

    // 貼圖必須在算繪執行緒上建立
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
        } catch (Throwable t) {
            ErrorDebug.note("cape.register", key, t);
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
