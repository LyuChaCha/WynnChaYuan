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

public final class Presence {

    private Presence() {}

    static final String BASE = "https://wynnchayuan.com";

    private static final String API = BASE + "/api";

    public static final String PRIVACY_URL = BASE + "/privacy";

    public static final String GALLERY_URL = BASE + "/capes";

    private static final long LIST_EVERY_MS = 5 * 60_000L;

    private static final long HELLO_EVERY_MS = 12 * 3600_000L;

    private static final long RETRY_MS = 60_000L;

    private static final long LOGIN_RETRY_MS = 10 * 60_000L;

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private static final Duration CAPE_TIMEOUT = Duration.ofMinutes(3);

    private static final int MAX_LIST_BYTES = 4 * 1024 * 1024;
    private static final int MAX_CAPE_BYTES = 21 * 1024 * 1024;

    public record Roster(Set<String> users, Map<String, Long> capes, Set<String> hidden,
                         Set<String> rainbow, Map<String, java.util.List<String>> shown) {
        public static final Roster EMPTY = new Roster(Set.of(), Map.of(), Set.of());

        public Roster(Set<String> users, Map<String, Long> capes, Set<String> hidden) {
            this(users, capes, hidden, Set.of(), Map.of());
        }
    }

    public record BadgeChoice(boolean rainbow, java.util.List<String> shown) {
        public static final BadgeChoice NONE = new BadgeChoice(false, null);
    }

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
    private static volatile String lastSaid;
    private static volatile long lastHelloTry;
    private static volatile long lastList;
    private static volatile long lastListTry;

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

    public static boolean installed(UUID uuid) {
        String id = idOf(uuid);
        return id != null && roster.users().contains(id);
    }

    public static long capeVersion(UUID uuid) {
        String id = idOf(uuid);
        Long version = id == null ? null : roster.capes().get(id);
        return version == null ? 0 : version;
    }

    public static boolean capeHidden(UUID uuid) {
        String id = idOf(uuid);
        return id != null && roster.hidden().contains(id);
    }

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

    public static void load(Roster next) {
        roster = next == null ? Roster.EMPTY : next;
    }

    static final String LOGIN_PREFIX = "wynnchayuan.com login\n";

    static byte[] loginMessage(String challenge, String uuid) {
        return (LOGIN_PREFIX + challenge + "\n" + uuid).getBytes(StandardCharsets.UTF_8);
    }

    static byte[] signLogin(java.security.PrivateKey key, String challenge, String uuid)
            throws java.security.GeneralSecurityException {
        java.security.Signature signer = java.security.Signature.getInstance("SHA256withRSA");
        signer.initSign(key);
        signer.update(loginMessage(challenge, uuid));
        return signer.sign();
    }

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

    static String linkBody(String code) {
        JsonObject o = new JsonObject();
        o.addProperty("code", code);
        return o.toString();
    }

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

    public static void tick(boolean report, boolean cape, boolean rainbow,
                            java.util.List<String> show) {
        if (busy || !NetworkGate.registry()) {
            return;
        }
        long now = System.currentTimeMillis();
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
                        lastList = 0;
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

    // Mojang 擋 Cloudflare Worker 的請求（403），改用玩家金鑰簽名
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
            lastProblem = problem;
            System.out.println("[WynnChaYuan] 登記站登入沒有成功：" + problem);
        }
        return false;
    }

    private static volatile String lastProblem;

    private static final long KEY_WAIT_SECONDS = 20;

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

    private static volatile boolean member;

    public static boolean member() {
        return member;
    }

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

    public enum LinkResult {
        DONE, BAD_CODE, UNKNOWN_CODE, LOGIN_FAILED, FAILED, OFF
    }

    private static final java.util.regex.Pattern LINK_CODE =
            java.util.regex.Pattern.compile("[A-HJ-NP-Z2-9]{8}");

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

    public static void link(String typed, Consumer<LinkResult> done) {
        if (!NetworkGate.registry()) {
            done.accept(LinkResult.OFF);
            return;
        }
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

    public enum Result {
        DONE, TOO_LARGE, BAD_SIZE, BAD_FORMAT, TOO_MANY_FRAMES, NOT_LISTED, GIF_MEMBERS_ONLY,
        BANNED, OFF,
        SLOW_DOWN, LOGIN_FAILED, FAILED, PRIVACY
    }

    static Result resultOf(int status, String body) {
        if (status == 200) {
            return Result.DONE;
        }
        String why = "";
        try {
            why = JsonParser.parseString(body).getAsJsonObject().get("why").getAsString();
        } catch (Exception e) {
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

    public static void remove(Consumer<Result> done) {
        if (!NetworkGate.registry()) {
            done.accept(Result.PRIVACY);
            return;
        }
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
                refresh();
            }
            return result;
        } catch (Throwable t) {
            com.wynnchayuan.translate.ErrorDebug.note("presence.cape", null, t);
            return Result.FAILED;
        }
    }

    public static void gallery(boolean signIn, Consumer<String> open) {
        site("/capes", signIn, open);
    }

    public static void site(String page, boolean signIn, Consumer<String> open) {
        WORKER.execute(() -> {
            String url = BASE + page;
            if (signIn && NetworkGate.registry()) {
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

    static String onPage(String link, String page) {
        String gallery = BASE + "/capes";
        if (link == null || !link.startsWith(gallery + "#")) {
            return null;
        }
        return BASE + page + link.substring(gallery.length());
    }

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

    public static byte[] capeBytes(String id, long version) {
        if (!isId(id) || !NetworkGate.registry()) {
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

    public enum DeleteResult {
        DONE, NOT_FOUND, LOGIN_FAILED, FAILED
    }

    static DeleteResult deleteResultOf(int status, String body) {
        if (status == 404) {
            return DeleteResult.NOT_FOUND;
        }
        if (status == 401) {
            return DeleteResult.LOGIN_FAILED;
        }
        if (status != 200) {
            return DeleteResult.FAILED;
        }
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            return root.has("ok") && root.get("ok").isJsonPrimitive()
                    && root.get("ok").getAsJsonPrimitive().isBoolean()
                    && root.get("ok").getAsBoolean() ? DeleteResult.DONE : DeleteResult.FAILED;
        } catch (Exception e) {
            return DeleteResult.FAILED;
        }
    }

    public static void deleteMyData(Consumer<DeleteResult> done) {
        WORKER.execute(() -> {
            DeleteResult result;
            try {
                boolean hadToken = token != null;
                result = askToDelete();
                if (result == DeleteResult.LOGIN_FAILED && hadToken) {
                    result = askToDelete();
                }
            } catch (Throwable t) {
                com.wynnchayuan.translate.ErrorDebug.note("presence.delete", null, t);
                result = DeleteResult.FAILED;
            }
            if (result == DeleteResult.DONE) {
                forget();
            }
            done.accept(result);
        });
    }

    private static DeleteResult askToDelete() throws Exception {
        if (!login()) {
            return DeleteResult.LOGIN_FAILED;
        }
        HttpResponse<byte[]> response = send(HttpRequest.newBuilder(URI.create(API + "/me/delete"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{}", StandardCharsets.UTF_8)), true);
        return deleteResultOf(response.statusCode(),
                new String(response.body(), StandardCharsets.UTF_8));
    }

    public static void forget() {
        token = null;
        member = false;
        lastHello = 0;
        lastSaid = null;
        lastList = 0;
        roster = Roster.EMPTY;
    }
}
