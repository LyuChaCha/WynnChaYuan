package com.wynnchayuan;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.wynnchayuan.translate.ErrorDebug;
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

public final class Presence {

    private Presence() {}

    static final String BASE = "https://wynnchayuan.com";

    static final String API = BASE + "/api";

    public static final String PRIVACY_URL = BASE + "/privacy";

    public static final String GALLERY_URL = BASE + "/capes";

    private static final long LIST_EVERY_MS = 5 * 60_000L;

    private static final long HELLO_EVERY_MS = 12 * 3600_000L;

    private static final long RETRY_MS = 60_000L;

    private static final long LOGIN_RETRY_MS = 10 * 60_000L;

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private static final Duration CAPE_TIMEOUT = Duration.ofMinutes(3);

    private static final int MAX_LIST_BYTES = 4 * 1024 * 1024;
    static final int MAX_CAPE_BYTES = 21 * 1024 * 1024;

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

    static volatile Roster roster = Roster.EMPTY;

    private static final Map<UUID, String> IDS = new ConcurrentHashMap<>();
    private static final int IDS_LIMIT = 4000;

    static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "WynnChaYuan-presence");
        t.setDaemon(true);
        return t;
    });

    static volatile String token;
    static volatile long loginFailedAt;
    private static volatile boolean busy;
    static volatile long lastHello;
    static volatile String lastSaid;
    private static volatile long lastHelloTry;
    static volatile long lastList;
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
            ErrorDebug.note("Presence#idOf", null, e);
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
            ErrorDebug.note("Presence#parse", null, e);
            return null;
        }
    }

    static boolean isId(String text) {
        return text != null && text.matches("[0-9a-f]{24}");
    }

    public static void load(Roster next) {
        roster = next == null ? Roster.EMPTY : next;
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

    static void refresh() throws Exception {
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

    static volatile boolean member;

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
            ErrorDebug.note("Presence#memberOf", null, e);
            return false;
        }
    }

    private static boolean hello(boolean cape, boolean rainbow, java.util.List<String> show)
            throws Exception {
        if (!PresenceLogin.login()) {
            return false;
        }
        HttpResponse<byte[]> response = send(HttpRequest.newBuilder(URI.create(API + "/hello"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(helloBody(cape, rainbow, show),
                        StandardCharsets.UTF_8)),
                true);
        return response.statusCode() == 200;
    }

    static HttpResponse<byte[]> send(HttpRequest.Builder request, boolean withToken)
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
}
