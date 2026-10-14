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
 *   <li><b>登入</b>（{@link #loginBody}）：UUID、Mojang 替這個帳號的玩家金鑰簽的憑證
 *       （公鑰、到期時間、Mojang 的簽章）、對登記站那道題的簽名，以及 Mojang 簽過的
 *       個人檔案（名字與外觀，本來就是公開資料）。私鑰與登入憑證不會離開遊戲。
 *       為什麼是這一套，見 {@link #login}。</li>
 *   <li><b>報到</b>（{@link #helloBody}）：要不要讓別人看到我的披風，以及「我的徽章」的選擇
 *       （葉子要不要彩色、要展示哪幾個身分）——別人畫你的名牌時照這個畫。</li>
 *   <li><b>綁定網站帳號</b>（{@link #linkBody}）：玩家自己打 {@code /wcy link <綁定碼>} 才送，
 *       送的就是他打的那組碼。</li>
 *   <li><b>上傳披風</b>：模組不送圖了，玩家在網站上傳（見 {@link #gallery}）。</li>
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

    /** WynnChaYuan 的網站。程式在 {@code tools/presence}（一支 Cloudflare Worker）。 */
    static final String BASE = "https://wynnchayuan.com";

    /** 登記站的 API，跟網站同一個地方。 */
    private static final String API = BASE + "/api";

    /** 名單多久抓一次。 */
    private static final long LIST_EVERY_MS = 5 * 60_000L;

    /** 沒有變動的話多久重新報到一次（登記十四天過期，一天兩次綽綽有餘）。 */
    private static final long HELLO_EVERY_MS = 12 * 3600_000L;

    /** 失敗之後隔多久再試。 */
    private static final long RETRY_MS = 60_000L;

    /** 登入失敗（離線帳號、Mojang 連不上）之後隔多久再試。 */
    private static final long LOGIN_RETRY_MS = 10 * 60_000L;

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    /** 傳披風、抓披風的時限。圖檔最大 20 MB，慢一點的線路要幾十秒。 */
    private static final Duration CAPE_TIMEOUT = Duration.ofMinutes(3);

    private static final int MAX_LIST_BYTES = 4 * 1024 * 1024;
    private static final int MAX_CAPE_BYTES = 21 * 1024 * 1024;

    /**
     * 名單。
     *
     * @param users  有裝的人
     * @param capes  有上傳披風、而且要給別人看的人 → 披風的版本
     * @param hidden 關掉「讓別人看到我的披風」的人
     */
    public record Roster(Set<String> users, Map<String, Long> capes, Set<String> hidden,
                         Set<String> rainbow, Map<String, java.util.List<String>> shown) {
        public static final Roster EMPTY = new Roster(Set.of(), Map.of(), Set.of());

        /** 沒有徽章選擇的名單（舊格式、測試）。 */
        public Roster(Set<String> users, Map<String, Long> capes, Set<String> hidden) {
            this(users, capes, hidden, Set.of(), Map.of());
        }
    }

    /**
     * 別人替自己的徽章做的選擇（使用者 2026-10-12：「我有開啟全彩模式，他看我也只是金色而已」）。
     *
     * @param rainbow 葉子用彩色
     * @param shown   要展示哪幾個身分；{@code null} 表示他沒有挑，照名單上登記的
     */
    public record BadgeChoice(boolean rainbow, java.util.List<String> shown) {
        public static final BadgeChoice NONE = new BadgeChoice(false, null);
    }

    /** 這個人替自己的徽章做的選擇；不在名單上、或他沒有報過，就是 {@link BadgeChoice#NONE}。 */
    public static BadgeChoice badgeChoice(UUID uuid) {
        String id = idOf(uuid);
        Roster now = roster;
        if (id == null || !now.users().contains(id)) {
            return BadgeChoice.NONE;
        }
        boolean rainbow = now.rainbow().contains(id);
        java.util.List<String> shown = now.shown().get(id);
        return rainbow || shown != null ? new BadgeChoice(rainbow, shown) : BadgeChoice.NONE;
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
    /** 上一次報到時說的內容（披風開關與徽章的選擇），變了才重新報到。 */
    private static volatile String lastSaid;
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
            Set<String> rainbow = new HashSet<>();
            if (root.has("rainbow")) {
                for (JsonElement el : root.getAsJsonArray("rainbow")) {
                    if (users.contains(el.getAsString())) {
                        rainbow.add(el.getAsString());
                    }
                }
            }
            Map<String, java.util.List<String>> shown = new HashMap<>();
            if (root.has("show")) {
                for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("show").entrySet()) {
                    if (!users.contains(e.getKey())) {
                        continue;
                    }
                    java.util.List<String> roles = new java.util.ArrayList<>();
                    for (String role : e.getValue().getAsString().split(",")) {
                        if (role.matches("[a-z]{1,24}")) {
                            roles.add(role);
                        }
                    }
                    shown.put(e.getKey(), java.util.List.copyOf(roles));
                }
            }
            return new Roster(Set.copyOf(users), Map.copyOf(capes), Set.copyOf(hidden),
                    Set.copyOf(rainbow), Map.copyOf(shown));
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

    /** 登入要簽的那串字的開頭：綁住網域。跟 {@code worker.js} 的 {@code LOGIN_PREFIX} 是同一串。 */
    static final String LOGIN_PREFIX = "wynnchayuan.com login\n";

    /** 登入要簽的內容：開頭、登記站這一次出的題、自己的 UUID。 */
    static byte[] loginMessage(String challenge, String uuid) {
        return (LOGIN_PREFIX + challenge + "\n" + uuid).getBytes(StandardCharsets.UTF_8);
    }

    /** 用玩家金鑰的私鑰答題。私鑰只在這裡用一下，不會被送出去。 */
    static byte[] signLogin(java.security.PrivateKey key, String challenge, String uuid)
            throws java.security.GeneralSecurityException {
        java.security.Signature signer = java.security.Signature.getInstance("SHA256withRSA");
        signer.initSign(key);
        signer.update(loginMessage(challenge, uuid));
        return signer.sign();
    }

    /**
     * 登入時送的全部內容。
     *
     * @param uuid             沒有連字號的小寫 UUID
     * @param expiresAt        憑證的到期時間（毫秒）
     * @param publicKey        玩家金鑰的公鑰（X.509）
     * @param keySignature     Mojang 替那把公鑰簽的章
     * @param challenge        登記站出的題
     * @param signature        用私鑰對那道題簽的名（{@link #signLogin}）
     * @param texturesValue    個人檔案的 textures 屬性（Mojang 簽過的公開資料，裡面有名字）
     * @param texturesSignature 它的簽章
     */
    static String loginBody(String uuid, long expiresAt, byte[] publicKey, byte[] keySignature,
                            String challenge, byte[] signature,
                            String texturesValue, String texturesSignature) {
        java.util.Base64.Encoder b64 = java.util.Base64.getEncoder();
        JsonObject o = new JsonObject();
        o.addProperty("uuid", uuid);
        o.addProperty("expiresAt", expiresAt);
        o.addProperty("publicKey", b64.encodeToString(publicKey));
        o.addProperty("keySignature", b64.encodeToString(keySignature));
        o.addProperty("challenge", challenge);
        o.addProperty("signature", b64.encodeToString(signature));
        JsonObject textures = new JsonObject();
        textures.addProperty("value", texturesValue);
        textures.addProperty("signature", texturesSignature);
        o.add("textures", textures);
        return o.toString();
    }

    /** 綁定網站帳號時送的全部內容：玩家打的那組綁定碼。 */
    static String linkBody(String code) {
        JsonObject o = new JsonObject();
        o.addProperty("code", code);
        return o.toString();
    }

    /**
     * 報到時送的全部內容。
     *
     * @param cape    要不要讓別人看到我的披風
     * @param rainbow 葉子要不要彩色（「我的徽章」的彩色模式）
     * @param show    要展示哪幾個身分的葉子；不在名單上的人沒有身分可挑，傳 {@code null} 就不送這一欄
     */
    static String helloBody(boolean cape, boolean rainbow, java.util.List<String> show) {
        JsonObject o = new JsonObject();
        o.addProperty("cape", cape);
        o.addProperty("rainbow", rainbow);
        if (show != null) {
            com.google.gson.JsonArray roles = new com.google.gson.JsonArray();
            for (String role : show) {
                roles.add(role);
            }
            o.add("show", roles);
        }
        return o.toString();
    }

    // ------------------------------------------------------------ 定時的事

    /**
     * 每個 tick 來問一次：該報到、該抓名單了嗎。真的要連線的時候丟給背景執行緒。
     *
     * @param report  要不要報到。實機一律是 {@code true}——那個開關拿掉了，每個使用者都有葉子
     * @param cape    要讓別人看到我的披風
     * @param rainbow 「我的徽章」開著彩色模式
     * @param show    「我的徽章」要展示哪幾個身分；不在名單上的人傳 {@code null}
     */
    public static void tick(boolean report, boolean cape, boolean rainbow,
                            java.util.List<String> show) {
        if (busy) {
            return;
        }
        long now = System.currentTimeMillis();
        // 披風開關或徽章的選擇一改就重新報到，不必等到明天
        String said = cape + "|" + rainbow + "|" + (show == null ? "-" : String.join(",", show));
        boolean helloDue = report && now - lastHelloTry >= RETRY_MS
                && now - loginFailedAt >= LOGIN_RETRY_MS
                && (lastHello == 0 || !said.equals(lastSaid) || now - lastHello >= HELLO_EVERY_MS);
        boolean listDue = now - lastListTry >= RETRY_MS && now - lastList >= LIST_EVERY_MS;
        if (!helloDue && !listDue) {
            return;
        }
        busy = true;
        WORKER.execute(() -> {
            try {
                if (helloDue) {
                    lastHelloTry = System.currentTimeMillis();
                    if (hello(cape, rainbow, show)) {
                        lastHello = System.currentTimeMillis();
                        lastSaid = said;
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
        HttpResponse<byte[]> response = send(HttpRequest.newBuilder(URI.create(API + "/list"))
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
     * <h2>為什麼不是「加入伺服器」那一套</h2>
     * 第一版用的是 Minecraft 多人伺服器的做法：這裡向 Mojang 說「我要加入 X」，登記站再問
     * Mojang「他加入 X 了嗎」。部署之後所有人都登不進來（使用者 2026-10-12：「登入還是
     * 沒辦法」）——登記站在 Cloudflare Worker 上，而 Mojang 把 Worker 發出去的請求一律
     * 擋掉（403），那個問題永遠問不到答案。
     *
     * <h2>現在的做法</h2>
     * 1.19 起每個正版帳號都有一對 Mojang 發的金鑰（聊天簽章用的那一對，遊戲自己會去拿），
     * 公鑰連同 UUID 與到期時間由 Mojang 簽過。登記站出一道題，這裡用私鑰簽；它拿 Mojang 的
     * 公鑰驗憑證、拿憑證裡的公鑰驗答案，全程不必連 Mojang。私鑰要帳號的登入憑證才拿得到，
     * 所以簽得出來就是本人。
     *
     * <p>名字不在憑證裡，另外附 Mojang 簽過的個人檔案（textures 屬性：UUID、名字、外觀）。
     * 那是公開資料，只用來回答「這個 UUID 叫什麼名字」。
     *
     * <p>送出去的：公鑰、兩份 Mojang 的簽章、對題目的簽名、公開的個人檔案。
     * <b>私鑰與登入憑證不會離開遊戲。</b>
     */
    private static boolean login() throws Exception {
        if (token != null) {
            return true;
        }
        String problem;
        try {
            problem = proveAccount();
        } catch (Exception e) {
            com.wynnchayuan.translate.ErrorDebug.note("presence.login", null, e);
            problem = e.toString();
        }
        if (problem == null) {
            return true;
        }
        loginFailedAt = System.currentTimeMillis();
        if (!problem.equals(lastProblem)) {
            // 同一個原因只說一次：十分鐘重試一輪，不要把 log 洗掉
            lastProblem = problem;
            System.out.println("[WynnChaYuan] 登記站登入沒有成功：" + problem);
        }
        return false;
    }

    /** 上一次登入失敗的原因（給 log 去重複用）。 */
    private static volatile String lastProblem;

    /** 拿玩家金鑰最多等多久。遊戲通常早就拿好了，這裡只是不要卡住背景那條執行緒。 */
    private static final long KEY_WAIT_SECONDS = 20;

    /** @return 成功回 {@code null}（通行證已經記下）；失敗回原因 */
    private static String proveAccount() throws Exception {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        UUID id = mc.getUser().getProfileId();
        if (id == null) {
            return "沒有 UUID";
        }
        java.util.Optional<net.minecraft.world.entity.player.ProfileKeyPair> pair =
                mc.getProfileKeyPairManager().prepareKeyPair()
                        .get(KEY_WAIT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
        if (pair == null || pair.isEmpty()) {
            return "拿不到 Mojang 發的玩家金鑰（離線帳號，或是 Mojang 暫時連不上）";
        }
        net.minecraft.world.entity.player.ProfilePublicKey.Data certificate =
                pair.get().publicKey().data();
        com.mojang.authlib.properties.Property textures = signedTextures(mc, id);
        if (textures == null) {
            return "拿不到 Mojang 簽過的個人檔案";
        }
        HttpResponse<byte[]> asked = send(HttpRequest.newBuilder(URI.create(API + "/challenge"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}", StandardCharsets.UTF_8)), false);
        if (asked.statusCode() != 200) {
            return "登記站沒有出題（" + asked.statusCode() + "）";
        }
        String challenge = JsonParser.parseString(new String(asked.body(), StandardCharsets.UTF_8))
                .getAsJsonObject().get("challenge").getAsString();
        String uuid = id.toString().replace("-", "");
        String body = loginBody(uuid, certificate.expiresAt().toEpochMilli(),
                certificate.key().getEncoded(), certificate.keySignature(), challenge,
                signLogin(pair.get().privateKey(), challenge, uuid),
                textures.value(), textures.signature());
        HttpResponse<byte[]> response = send(HttpRequest.newBuilder(URI.create(API + "/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)), false);
        String answer = new String(response.body(), StandardCharsets.UTF_8);
        if (response.statusCode() != 200) {
            return "登記站回 " + response.statusCode() + " "
                    + (answer.length() > 120 ? answer.substring(0, 120) : answer);
        }
        token = JsonParser.parseString(answer).getAsJsonObject().get("token").getAsString();
        member = memberOf(answer);
        lastProblem = null;
        return null;
    }

    /**
     * 登記站說這個人上傳有沒有成員的待遇（GIF、20 MB）。
     *
     * <p>名單上的成員模組自己看得出來（{@code credits.json}）；這個旗標是給<b>特別名單</b>的——
     * 開發者在管理頁手動加的 UUID，不在 {@code credits.json} 上、沒有葉子，只有登記站知道
     * （使用者 2026-10-13）。登入之前不知道，當成沒有。
     */
    private static volatile boolean member;

    /** 見 {@link #member}。登記站會再擋一次，那一次才算數。 */
    public static boolean member() {
        return member;
    }

    /** 登入的回覆裡有沒有 {@code "member": true}。舊的登記站沒有這一欄，當成沒有。 */
    static boolean memberOf(String answer) {
        try {
            JsonObject root = JsonParser.parseString(answer).getAsJsonObject();
            return root.has("member") && root.get("member").isJsonPrimitive()
                    && root.get("member").getAsJsonPrimitive().isBoolean()
                    && root.get("member").getAsBoolean();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 自己的個人檔案裡 Mojang 簽過的那份 textures 屬性。
     *
     * <p>遊戲啟動時就抓過一份帶簽章的（畫自己的外觀用的），通常直接有；啟動時剛好沒抓到的話，
     * 請遊戲自己的服務再抓一次——那是對 Mojang 的查詢，跟畫任何玩家的外觀時做的一樣。
     */
    private static com.mojang.authlib.properties.Property signedTextures(
            net.minecraft.client.Minecraft mc, UUID id) {
        com.mojang.authlib.properties.Property found = signedTextures(mc.getGameProfile());
        if (found == null) {
            com.mojang.authlib.yggdrasil.ProfileResult fetched =
                    mc.services().sessionService().fetchProfile(id, true);
            found = fetched == null ? null : signedTextures(fetched.profile());
        }
        return found;
    }

    private static com.mojang.authlib.properties.Property signedTextures(
            com.mojang.authlib.GameProfile profile) {
        if (profile == null || profile.properties() == null) {
            return null;
        }
        for (com.mojang.authlib.properties.Property property : profile.properties().get("textures")) {
            if (property.hasSignature() && property.value() != null) {
                return property;
            }
        }
        return null;
    }

    private static boolean hello(boolean cape, boolean rainbow, java.util.List<String> show)
            throws Exception {
        if (!login()) {
            return false;
        }
        HttpResponse<byte[]> response = send(HttpRequest.newBuilder(URI.create(API + "/hello"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(helloBody(cape, rainbow, show),
                        StandardCharsets.UTF_8)),
                true);
        return response.statusCode() == 200;
    }

    /** 送一個請求。通行證過期（401）的話丟掉它，下一輪會重新登入。 */
    private static HttpResponse<byte[]> send(HttpRequest.Builder request, boolean withToken)
            throws Exception {
        boolean cape = request.build().uri().getPath().startsWith("/api/cape");
        request.timeout(cape ? CAPE_TIMEOUT : TIMEOUT).header("User-Agent", "WynnChaYuan");
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

    // ------------------------------------------------------------ 綁定網站帳號

    /** 綁定的結果。 */
    public enum LinkResult {
        DONE, BAD_CODE, UNKNOWN_CODE, LOGIN_FAILED, FAILED
    }

    /** 綁定碼的樣子：8 個字，沒有長得像的 0／O、1／I。跟 {@code worker.js} 的 LINK_CODE 同一條。 */
    private static final java.util.regex.Pattern LINK_CODE =
            java.util.regex.Pattern.compile("[A-HJ-NP-Z2-9]{8}");

    /** 玩家打的字整理成綁定碼（大小寫、前後空白不計較）；不像綁定碼就回 {@code null}。 */
    static String linkCode(String typed) {
        if (typed == null) {
            return null;
        }
        String code = typed.strip().toUpperCase(java.util.Locale.ROOT);
        return LINK_CODE.matcher(code).matches() ? code : null;
    }

    static LinkResult linkResultOf(int status) {
        return switch (status) {
            case 200 -> LinkResult.DONE;
            case 400 -> LinkResult.BAD_CODE;
            case 404 -> LinkResult.UNKNOWN_CODE;
            case 401 -> LinkResult.LOGIN_FAILED;
            default -> LinkResult.FAILED;
        };
    }

    /**
     * 把這個遊戲帳號綁到網站帳號（使用者 2026-10-12：「像 Wynntils 一樣先註冊，然後再綁定
     * 遊戲 ID」）。
     *
     * <p>網站帳號是用 Discord 註冊的，它證明不了遊戲帳號是誰的；所以網站給一組綁定碼，
     * 玩家在遊戲裡打 {@code /wcy link <碼>}，由這裡帶著驗過身分的通行證把碼送回去。
     * 只有玩家自己打了指令才會走到這裡。
     *
     * @param typed 玩家打的那組碼
     * @param done  結果（在背景執行緒上叫）
     */
    public static void link(String typed, Consumer<LinkResult> done) {
        String code = linkCode(typed);
        if (code == null) {
            done.accept(LinkResult.BAD_CODE);
            return;
        }
        WORKER.execute(() -> {
            LinkResult result;
            try {
                if (!login()) {
                    result = LinkResult.LOGIN_FAILED;
                } else {
                    HttpResponse<byte[]> response = send(HttpRequest.newBuilder(
                            URI.create(API + "/link"))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(linkBody(code),
                                    StandardCharsets.UTF_8)), true);
                    result = linkResultOf(response.statusCode());
                }
            } catch (Throwable t) {
                com.wynnchayuan.translate.ErrorDebug.note("presence.link", null, t);
                result = LinkResult.FAILED;
            }
            done.accept(result);
        });
    }

    // ------------------------------------------------------------ 披風

    /** 移除披風的結果（理由跟網站上傳共用一套，登記站回的都認得）。 */
    public enum Result {
        DONE, TOO_LARGE, BAD_SIZE, BAD_FORMAT, TOO_MANY_FRAMES, NOT_LISTED, GIF_MEMBERS_ONLY,
        BANNED, OFF,
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
            case "gif members only" -> Result.GIF_MEMBERS_ONLY;
            case "banned" -> Result.BANNED;
            case "uploads off" -> Result.OFF;
            case "slow down" -> Result.SLOW_DOWN;
            case "login first" -> Result.LOGIN_FAILED;
            default -> Result.FAILED;
        };
    }

    /** 拿掉自己上傳的披風。 */
    public static void remove(Consumer<Result> done) {
        WORKER.execute(() -> done.accept(change(HttpRequest.newBuilder(
                URI.create(API + "/cape")).DELETE())));
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
     * 披風一覽的網址（網站上的一頁，列出所有上傳的披風）。
     *
     * <p>有開報到的人拿到的是<b>順便替瀏覽器登入</b>的網址：網站沒辦法自己確認 Minecraft
     * 帳號，所以由模組拿通行證換一條一天有效的網址。登入的憑證放在網址 {@code #} 後面，
     * 那一段不會送到任何伺服器。登入之後可以在網站上傳、移除自己的披風；名單上的開發者
     * 還看得到管理的頁面。沒開報到、或驗證沒過，就是一般的網址。
     *
     * @param signIn 要不要替瀏覽器登入（設定裡開著「讓別人知道我有裝」）
     * @param open   拿到網址之後叫（在背景執行緒上）
     */
    public static void gallery(boolean signIn, Consumer<String> open) {
        site("/capes", signIn, open);
    }

    /**
     * 網站上的某一頁，跟 {@link #gallery} 一樣順便替瀏覽器登入。
     *
     * <p>F6 的「上傳披風」開的是帳號頁（{@code /account}）：上傳在那裡（使用者 2026-10-14：
     * 「他們在 F6 上傳那部分可以直接引導到網頁」）。登記站給的登入網址指著披風一覽，
     * 網站每一頁都認網址 {@code #} 後面的通行證，所以換掉路徑就好。
     *
     * @param page 網站上的路徑，像 {@code /account}
     */
    public static void site(String page, boolean signIn, Consumer<String> open) {
        WORKER.execute(() -> {
            String url = BASE + page;
            if (signIn) {
                try {
                    if (login()) {
                        HttpResponse<byte[]> response = send(HttpRequest.newBuilder(
                                URI.create(API + "/web-link"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString("{}")), true);
                        String link = onPage(signedInLink(response.statusCode(),
                                new String(response.body(), StandardCharsets.UTF_8)), page);
                        if (link != null) {
                            url = link;
                        }
                    }
                } catch (Throwable t) {
                    com.wynnchayuan.translate.ErrorDebug.note("presence.gallery", null, t);
                }
            }
            open.accept(url);
        });
    }

    /** 把披風一覽的登入網址換成網站上另一頁的（通行證照帶）；沒有登入網址回 {@code null}。 */
    static String onPage(String link, String page) {
        String gallery = BASE + "/capes";
        if (link == null || !link.startsWith(gallery + "#")) {
            return null;
        }
        return BASE + page + link.substring(gallery.length());
    }

    /** 登記站回的登入網址。只認指回我們網站的——別的網址不開。 */
    static String signedInLink(int status, String body) {
        if (status != 200) {
            return null;
        }
        try {
            String url = JsonParser.parseString(body).getAsJsonObject().get("url").getAsString();
            return url.startsWith(BASE + "/capes#t=") ? url : null;
        } catch (Exception e) {
            return null;
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
                    URI.create(API + "/cape/" + id + "?v=" + version)).GET(), false);
            return response.statusCode() == 200 && response.body().length <= MAX_CAPE_BYTES
                    ? response.body() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
