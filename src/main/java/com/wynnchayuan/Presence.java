package com.wynnchayuan;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 登記站：誰有裝 WynnChaYuan、誰的披風要給別人看、上傳自己的披風。
 *
 * <h2>為什麼要有這個（使用者 2026-10-12）</h2>
 * 「每個玩家有裝 WynnChaYuan 都會有預設的灰色徽章」「如果是上傳披風呢」——金、青、綠三種
 * 葉子看的是 GitHub 上的名單，但「誰有裝」沒有名單可看，模組自己不說，別人就無從知道；
 * 披風要不要給別人看、穿哪一件，也都是本人的選擇，得有地方記。
 *
 * <h2>送出去的東西</h2>
 * <b>這是整個模組唯一會往外送資料的地方</b>（{@code CorpusExportTest} 釘住「只有這一個檔、
 * 只送到這一個網址」，{@code PresenceTest} 釘住送什麼）：
 * <ul>
 *   <li><b>登入</b>（{@link #loginBody}）：帳號名，加一串這次隨機產生的字。登記站拿這兩樣去問
 *       Mojang「這個帳號剛剛是不是加入了這串字」——跟每一台多人伺服器確認玩家身分是同一套
 *       做法，登入憑證不會離開遊戲。登記站因此知道這個人的 UUID。</li>
 *   <li><b>報到</b>（{@link #helloBody}）：要不要讓別人看到我的披風。就這一欄。</li>
 *   <li><b>上傳披風</b>：玩家在 F6 按了「上傳」才送，送的是他自己放進資料夾的那張圖。</li>
 * </ul>
 * 沒有所在的世界、沒有任何遊戲內容。F6 可以關掉報到；關掉之後照樣看得到別人，
 * 只是別人看不到你。
 *
 * <h2>名單上是代碼不是 UUID</h2>
 * 登記站公開的名單裡放的是 UUID 雜湊出來的代碼（{@link #idOf}）。模組把畫面上那個人的
 * UUID 算成代碼再去名單裡找，所以名單不必把所有人的 UUID 攤開。
 */
public final class Presence {

    private Presence() {}

    /** 登記站。沿用先前語料收集站那一支 Worker，程式在 {@code tools/presence}。 */
    static final String BASE = "https://wynnchayuan-corpus.green09good.workers.dev";

    /** 名單多久抓一次。 */
    private static final long LIST_EVERY_MS = 5 * 60_000L;

    /** 沒有變動的話多久重新報到一次（登記十四天過期，一天兩次綽綽有餘）。 */
    private static final long HELLO_EVERY_MS = 12 * 3600_000L;

    /** 失敗之後隔多久再試。 */
    private static final long RETRY_MS = 60_000L;

    /** 登入失敗（離線帳號、Mojang 連不上）之後隔多久再試。 */
    private static final long LOGIN_RETRY_MS = 10 * 60_000L;

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private static final int MAX_LIST_BYTES = 4 * 1024 * 1024;
    private static final int MAX_CAPE_BYTES = 2 * 1024 * 1024;

    /**
     * 名單。
     *
     * @param users  有裝的人
     * @param capes  有上傳披風、而且要給別人看的人 → 披風的版本
     * @param hidden 關掉「讓別人看到我的披風」的人
     */
    public record Roster(Set<String> users, Map<String, Long> capes, Set<String> hidden) {
        public static final Roster EMPTY = new Roster(Set.of(), Map.of(), Set.of());
    }

    private static volatile Roster roster = Roster.EMPTY;

    /** UUID → 代碼。名牌每一幀都會問，雜湊不必每次重算。 */
    private static final Map<UUID, String> IDS = new ConcurrentHashMap<>();
    private static final int IDS_LIMIT = 4000;

    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "WynnChaYuan-presence");
        t.setDaemon(true);
        return t;
    });

    private static volatile String token;
    private static volatile long loginFailedAt;
    private static volatile boolean busy;
    private static volatile long lastHello;
    private static volatile boolean lastCape;
    private static volatile long lastHelloTry;
    private static volatile long lastList;
    private static volatile long lastListTry;

    // ------------------------------------------------------------ 代碼與名單

    /**
     * UUID 的代碼：SHA-256（「wynnchayuan:」加上沒有連字號的小寫 UUID）取前 12 個位元組。
     * 跟登記站的 {@code publicId} 是同一條算式。
     *
     * @return 24 個小寫十六進位字元；沒有 UUID 回 {@code null}
     */
    public static String idOf(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        String have = IDS.get(uuid);
        if (have != null) {
            return have;
        }
        try {
            String plain = uuid.toString().replace("-", "");
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(("wynnchayuan:" + plain).getBytes(StandardCharsets.UTF_8));
            String id = hex(hash, 12);
            if (IDS.size() >= IDS_LIMIT) {
                IDS.clear();
            }
            IDS.put(uuid, id);
            return id;
        } catch (Exception e) {
            return null;
        }
    }

    private static String hex(byte[] bytes, int count) {
        StringBuilder out = new StringBuilder(count * 2);
        for (int i = 0; i < count; i++) {
            out.append(Character.forDigit((bytes[i] >> 4) & 0xF, 16))
               .append(Character.forDigit(bytes[i] & 0xF, 16));
        }
        return out.toString();
    }

    /** 這個人在名單上嗎（有裝、而且沒有關掉報到）。 */
    public static boolean installed(UUID uuid) {
        String id = idOf(uuid);
        return id != null && roster.users().contains(id);
    }

    /** 這個人上傳的披風是哪一版；沒有上傳、或他關掉了，回 0。 */
    public static long capeVersion(UUID uuid) {
        String id = idOf(uuid);
        Long version = id == null ? null : roster.capes().get(id);
        return version == null ? 0 : version;
    }

    /** 這個人關掉了「讓別人看到我的披風」嗎。 */
    public static boolean capeHidden(UUID uuid) {
        String id = idOf(uuid);
        return id != null && roster.hidden().contains(id);
    }

    /** 讀登記站回的名單。格式不對的整份不要。 */
    static Roster parse(String json) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            Set<String> users = new HashSet<>();
            if (root.has("users")) {
                for (JsonElement el : root.getAsJsonArray("users")) {
                    if (isId(el.getAsString())) {
                        users.add(el.getAsString());
                    }
                }
            }
            Map<String, Long> capes = new HashMap<>();
            if (root.has("capes")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("capes").entrySet()) {
                    long version = e.getValue().getAsLong();
                    if (users.contains(e.getKey()) && version > 0) {
                        capes.put(e.getKey(), version);
                    }
                }
            }
            Set<String> hidden = new HashSet<>();
            if (root.has("hidden")) {
                for (JsonElement el : root.getAsJsonArray("hidden")) {
                    if (users.contains(el.getAsString())) {
                        hidden.add(el.getAsString());
                    }
                }
            }
            capes.keySet().removeAll(hidden);
            return new Roster(Set.copyOf(users), Map.copyOf(capes), Set.copyOf(hidden));
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isId(String text) {
        return text != null && text.matches("[0-9a-f]{24}");
    }

    /** 測試用：直接放一份名單。 */
    public static void load(Roster next) {
        roster = next == null ? Roster.EMPTY : next;
    }

    // ------------------------------------------------------------ 送出去的內容

    /** 登入時送的全部內容：帳號名與這次隨機產生的那串字。 */
    static String loginBody(String name, String serverId) {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("serverId", serverId);
        return o.toString();
    }

    /** 報到時送的全部內容。 */
    static String helloBody(boolean cape) {
        JsonObject o = new JsonObject();
        o.addProperty("cape", cape);
        return o.toString();
    }

    /** 給 Mojang 與登記站對的那串字：40 個隨機的十六進位字元，每次登入都不一樣。 */
    static String newServerId() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return hex(bytes, 20);
    }

    // ------------------------------------------------------------ 定時的事

    /**
     * 每個 tick 來問一次：該報到、該抓名單了嗎。真的要連線的時候丟給背景執行緒。
     *
     * @param report 設定裡開著「讓別人知道我有裝」
     * @param cape   要讓別人看到我的披風
     */
    public static void tick(boolean report, boolean cape) {
        if (busy) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean helloDue = report && now - lastHelloTry >= RETRY_MS
                && now - loginFailedAt >= LOGIN_RETRY_MS
                && (lastHello == 0 || cape != lastCape || now - lastHello >= HELLO_EVERY_MS);
        boolean listDue = now - lastListTry >= RETRY_MS && now - lastList >= LIST_EVERY_MS;
        if (!helloDue && !listDue) {
            return;
        }
        busy = true;
        WORKER.execute(() -> {
            try {
                if (helloDue) {
                    lastHelloTry = System.currentTimeMillis();
                    if (hello(cape)) {
                        lastHello = System.currentTimeMillis();
                        lastCape = cape;
                        lastList = 0;              // 剛報到完，名單馬上抓一次才看得到自己
                    }
                }
                if (listDue || lastList == 0) {
                    refresh();
                }
            } catch (Throwable t) {
                com.wynnchayuan.translate.ErrorDebug.note("presence", null, t);
            } finally {
                busy = false;
            }
        });
    }

    private static void refresh() throws Exception {
        lastListTry = System.currentTimeMillis();
        HttpResponse<byte[]> response = send(HttpRequest.newBuilder(URI.create(BASE + "/list"))
                .GET(), false);
        if (response.statusCode() == 200 && response.body().length <= MAX_LIST_BYTES) {
            Roster next = parse(new String(response.body(), StandardCharsets.UTF_8));
            if (next != null) {
                roster = next;
                lastList = System.currentTimeMillis();
            }
        }
    }

    // ------------------------------------------------------------ 連線

    /**
     * 向登記站證明「我是這個帳號」，換一張通行證。
     *
     * <p>先向 Mojang 說「我要加入 {@code serverId}」（原版加入多人伺服器時做的同一件事），
     * 再把帳號名與 {@code serverId} 交給登記站，由它去問 Mojang。登入憑證只給 Mojang。
     */
    private static boolean login() throws Exception {
        if (token != null) {
            return true;
        }
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            net.minecraft.client.User user = mc.getUser();
            String serverId = newServerId();
            mc.services().sessionService().joinServer(
                    user.getProfileId(), user.getAccessToken(), serverId);
            HttpResponse<byte[]> response = send(
                    HttpRequest.newBuilder(URI.create(BASE + "/login"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(
                                    loginBody(user.getName(), serverId), StandardCharsets.UTF_8)),
                    false);
            if (response.statusCode() == 200) {
                token = JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8))
                        .getAsJsonObject().get("token").getAsString();
                return true;
            }
        } catch (Exception e) {
            com.wynnchayuan.translate.ErrorDebug.note("presence.login", null, e);
        }
        loginFailedAt = System.currentTimeMillis();
        return false;
    }

    private static boolean hello(boolean cape) throws Exception {
        if (!login()) {
            return false;
        }
        HttpResponse<byte[]> response = send(HttpRequest.newBuilder(URI.create(BASE + "/hello"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(helloBody(cape), StandardCharsets.UTF_8)),
                true);
        return response.statusCode() == 200;
    }

    /** 送一個請求。通行證過期（401）的話丟掉它，下一輪會重新登入。 */
    private static HttpResponse<byte[]> send(HttpRequest.Builder request, boolean withToken)
            throws Exception {
        request.timeout(TIMEOUT).header("User-Agent", "WynnChaYuan");
        if (withToken && token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build()) {
            HttpResponse<byte[]> response =
                    client.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
            if (withToken && response.statusCode() == 401) {
                token = null;
            }
            return response;
        }
    }

    // ------------------------------------------------------------ 披風

    /** 上傳／移除披風的結果。 */
    public enum Result {
        DONE, TOO_LARGE, BAD_SIZE, BAD_FORMAT, TOO_MANY_FRAMES, NOT_LISTED, BANNED, OFF,
        SLOW_DOWN, LOGIN_FAILED, FAILED
    }

    /** 登記站回的理由 → 結果。 */
    static Result resultOf(int status, String body) {
        if (status == 200) {
            return Result.DONE;
        }
        String why = "";
        try {
            why = JsonParser.parseString(body).getAsJsonObject().get("why").getAsString();
        } catch (Exception e) {
            // 不是我們的格式（閘道的錯誤頁）
        }
        return switch (why) {
            case "too large" -> Result.TOO_LARGE;
            case "bad size" -> Result.BAD_SIZE;
            case "bad format" -> Result.BAD_FORMAT;
            case "too many frames" -> Result.TOO_MANY_FRAMES;
            case "not listed" -> Result.NOT_LISTED;
            case "banned" -> Result.BANNED;
            case "uploads off" -> Result.OFF;
            case "slow down" -> Result.SLOW_DOWN;
            case "login first" -> Result.LOGIN_FAILED;
            default -> Result.FAILED;
        };
    }

    /**
     * 上傳自己的披風。只有玩家在 F6 按了「上傳」才會走到這裡。
     *
     * @param bytes 圖檔本體（呼叫端已經照 {@code CapeRules} 驗過一次）
     * @param done  結果，在背景執行緒上叫
     */
    public static void upload(byte[] bytes, String contentType, Consumer<Result> done) {
        WORKER.execute(() -> done.accept(change(HttpRequest.newBuilder(
                URI.create(BASE + "/cape")).header("Content-Type", contentType)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes)))));
    }

    /** 拿掉自己上傳的披風。 */
    public static void remove(Consumer<Result> done) {
        WORKER.execute(() -> done.accept(change(HttpRequest.newBuilder(
                URI.create(BASE + "/cape")).DELETE())));
    }

    private static Result change(HttpRequest.Builder request) {
        try {
            if (!login()) {
                return Result.LOGIN_FAILED;
            }
            HttpResponse<byte[]> response = send(request, true);
            Result result = resultOf(response.statusCode(),
                    new String(response.body(), StandardCharsets.UTF_8));
            if (result == Result.DONE) {
                refresh();                         // 自己的披風馬上換上
            }
            return result;
        } catch (Throwable t) {
            com.wynnchayuan.translate.ErrorDebug.note("presence.cape", null, t);
            return Result.FAILED;
        }
    }

    /**
     * 下載某個人上傳的披風。
     *
     * @return 圖檔；沒有、太大、或連不上回 {@code null}
     */
    public static byte[] capeBytes(String id, long version) {
        if (!isId(id)) {
            return null;
        }
        try {
            HttpResponse<byte[]> response = send(HttpRequest.newBuilder(
                    URI.create(BASE + "/cape/" + id + "?v=" + version)).GET(), false);
            return response.statusCode() == 200 && response.body().length <= MAX_CAPE_BYTES
                    ? response.body() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
