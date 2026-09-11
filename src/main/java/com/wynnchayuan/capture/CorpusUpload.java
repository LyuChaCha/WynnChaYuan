package com.wynnchayuan.capture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 把「還沒翻到的句子」分享回收集站，讓語料不必靠一個人跑遍全圖。
 *
 * <h2>為什麼要有這件事</h2>
 * {@code captured.json} 是很好用的缺口清單，但它只長在<b>玩的人自己那一台</b>。
 * 要把一段任務對話收進語料，就得有人真的去把那段任務跑一遍；沒人跑過的區域
 * 永遠是空白。維護者一個人的遊玩時數，實際上就是這個模組翻譯進度的上限。
 *
 * <p>分享出來之後，一百個人各跑各的，語料是所有人的總和。
 *
 * <h2>三道關卡</h2>
 * 這條路會把字串送到別人的機器上，所以每一關都<b>寧可少收</b>：
 *
 * <ol>
 *   <li><b>預設關閉</b>。要玩家自己到 F6 打開，旁邊寫清楚會送出什麼。</li>
 *   <li><b>只送特定來源</b>。見 {@link #shareable}：任務對話、介面、物品說明、
 *       名牌，以及伺服器自己的公告。公會／隊伍／喊話那些<b>別人打的字</b>
 *       一律不送——價值在前面幾類，風險全在後面這類。</li>
 *   <li><b>送出前再過一次個資濾網</b>。{@link PlayerDataFilter} 與
 *       {@link SelfNames} 在收集時已經擋過一次，這裡擋第二次，進倉庫前
 *       {@code tools/import-captured.py} 再擋第三次。濾網寫三遍是刻意的：
 *       這種東西漏一次就收不回來。</li>
 * </ol>
 *
 * <h2>總開關在倉庫上</h2>
 * 端點位址與啟用與否都寫在倉庫的 {@code collect.json} 裡，跟譯文一樣是線上讀的
 * （見 {@code RemoteSync}）。這樣收集站要搬家、要暫停、要限制只收哪個版本，
 * 都不必重新發一次 jar；讀不到那個檔就什麼都不做，等於預設關閉。
 */
public final class CorpusUpload {

    private CorpusUpload() {}

    /** 總開關與端點位址。跟譯文同一組來源，raw 在前、CDN 備援。 */
    private static final List<String> MANIFESTS = List.of(
            "https://raw.githubusercontent.com/LyuChaCha/WynnChaYuan/main/collect.json",
            "https://cdn.jsdelivr.net/gh/LyuChaCha/WynnChaYuan@main/collect.json");

    /** 一批最多送幾條。伺服器那邊也有自己的上限，兩邊都擋。 */
    private static final int BATCH = 100;

    /** 超過這個長度的不送——正常的一句台詞不會這麼長，那多半是黏在一起的雜訊。 */
    private static final int MAX_LEN = 600;

    /** 總開關重新確認的間隔。中途要停掉收集，玩家不必重開遊戲。 */
    private static final Duration MANIFEST_TTL = Duration.ofMinutes(30);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static volatile CaptureStore store;
    private static volatile String version = "";
    private static volatile String language = "";
    private static volatile java.util.function.BooleanSupplier enabled = () -> false;

    /** 已經送過的，不再送第二次。鍵跟 {@link CaptureStore} 同一套雜湊。 */
    private static final Set<String> sent = new LinkedHashSet<>();
    private static Path sentFile;

    private static volatile String endpoint;
    private static volatile long manifestAt;

    public static void init(Path configDir, CaptureStore captureStore,
                            String modVersion, String lang,
                            java.util.function.BooleanSupplier toggle) {
        store = captureStore;
        version = modVersion == null ? "" : modVersion;
        language = lang == null ? "" : lang;
        enabled = toggle == null ? () -> false : toggle;
        sentFile = configDir.resolve("shared.json");
        readSent();
    }

    /**
     * 送一批出去。由背景執行緒定期呼叫，失敗就算了，下一輪再說。
     *
     * <p>整支都包在 try 裡：分享語料失敗絕對不該影響到玩遊戲。
     */
    public static void push() {
        try {
            if (store == null || !enabled.getAsBoolean()) {
                return;
            }
            String where = endpoint();
            if (where == null) {
                return;                 // 收集站沒開，或連不上
            }
            List<CaptureStore.Captured> batch = pick();
            if (batch.isEmpty()) {
                return;
            }
            if (post(where, batch)) {
                for (CaptureStore.Captured c : batch) {
                    sent.add(CaptureStore.hash(c.src));
                }
                writeSent();
                System.out.println("[WynnChaYuan] 已分享 " + batch.size() + " 條待譯字串");
            }
        } catch (Exception e) {
            // 靜靜失敗。網路不通、端點掛掉、防火牆擋住，都不該在聊天室洗版。
            System.err.println("[WynnChaYuan] 分享語料失敗：" + e.getMessage());
        }
    }

    /** 這一輪要送哪些。 */
    private static List<CaptureStore.Captured> pick() {
        List<CaptureStore.Captured> out = new ArrayList<>();
        for (CaptureStore.Captured c : store.snapshot()) {
            if (out.size() >= BATCH) {
                break;
            }
            if (c.src == null || sent.contains(CaptureStore.hash(c.src))) {
                continue;
            }
            if (shareable(c)) {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * 這一條可以送出去嗎。
     *
     * <h2>放行的來源</h2>
     * <ul>
     *   <li>{@code dialogue/…} 任務對話——語料的大宗，也是最需要人去跑的。</li>
     *   <li>{@code gui/…}、{@code tooltip/…} 介面與物品說明。</li>
     *   <li>{@code npc/…}、{@code label/…} 名牌。這兩類會夾到玩家名字，
     *       所以另外過 {@link PlayerDataFilter#looksPlayerNamed}。</li>
     *   <li>{@code chat/INFO} 伺服器自己的公告。</li>
     * </ul>
     *
     * <h2>擋掉的來源</h2>
     * {@code chat/} 底下除了 {@code INFO} 以外<b>全部</b>不送：公會、隊伍、喊話、
     * 私訊裡是別人打的字，裡面有名字、有閒聊、有什麼都可能。那些東西的翻譯
     * 價值趨近於零，而風險是把陌生人的話散到公開倉庫裡——完全不成比例。
     */
    static boolean shareable(CaptureStore.Captured c) {
        if (c == null || c.src == null || c.src.isBlank() || c.src.length() > MAX_LEN) {
            return false;
        }
        if (!GlyphSplitter.hasLetter(c.src)) {
            return false;
        }
        String ctx = c.ctx == null ? "" : c.ctx;
        boolean ok = ctx.startsWith("dialogue/") || ctx.startsWith("gui/")
                || ctx.startsWith("tooltip/") || ctx.startsWith("npc/")
                || ctx.startsWith("label/") || ctx.equals("chat/INFO");
        if (!ok) {
            return false;
        }
        // 個資濾網再跑一次。收集時擋過了，但濾網一直在補，而這一條是<b>送出去</b>，
        // 拿現在這一版的規則重新問一次才算數。
        if (PlayerDataFilter.carriesPlayerData(c.src)) {
            return false;
        }
        if ((ctx.startsWith("npc/") || ctx.startsWith("label/"))
                && PlayerDataFilter.looksPlayerNamed(c.src)) {
            return false;
        }
        if (ctx.startsWith("gui/title") && PlayerDataFilter.looksAccountNamed(c.src)) {
            return false;
        }
        // 自己的名字（含暱稱）不管出現在哪裡都不送。
        return SelfNames.find(c.src) == null;
    }

    private static boolean post(String where, List<CaptureStore.Captured> batch)
            throws Exception {
        JsonArray items = new JsonArray();
        for (CaptureStore.Captured c : batch) {
            JsonObject one = new JsonObject();
            one.addProperty("src", c.src);
            one.addProperty("role", c.role == null ? "desc" : c.role);
            one.addProperty("domain", c.domain == null ? "" : c.domain);
            one.addProperty("ctx", c.ctx == null ? "" : c.ctx);
            one.addProperty("seen", c.seen);
            items.add(one);
        }
        JsonObject body = new JsonObject();
        body.addProperty("v", version);
        body.addProperty("lang", language);
        body.add("items", items);

        HttpRequest request = HttpRequest.newBuilder(URI.create(where))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .header("User-Agent", "WynnChaYuan/" + version)
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(),
                                                          StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response =
                HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return response.statusCode() / 100 == 2;
    }

    /**
     * 收集站現在在哪裡，以及到底有沒有開。
     *
     * @return 端點位址；沒開、讀不到、或這一版被擋下時回傳 {@code null}
     */
    private static String endpoint() {
        long now = System.currentTimeMillis();
        if (endpoint != null && now - manifestAt < MANIFEST_TTL.toMillis()) {
            return endpoint;
        }
        manifestAt = now;
        endpoint = null;
        for (String url : MANIFESTS) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .header("User-Agent", "WynnChaYuan/" + version)
                        .GET().build();
                HttpResponse<String> response = HTTP.send(
                        request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() / 100 != 2) {
                    continue;
                }
                JsonObject o = JsonParser.parseString(response.body()).getAsJsonObject();
                if (!o.has("enabled") || !o.get("enabled").getAsBoolean()) {
                    return null;       // 總開關關著
                }
                if (!o.has("endpoint")) {
                    return null;
                }
                String where = o.get("endpoint").getAsString();
                if (!where.startsWith("https://")) {
                    return null;       // 只走 https，免得被中間人改成別的地方
                }
                endpoint = where;
                return endpoint;
            } catch (Exception e) {
                // 換下一個來源
            }
        }
        return null;
    }

    private static void readSent() {
        if (sentFile == null || !Files.exists(sentFile)) {
            return;
        }
        try {
            JsonObject o = JsonParser.parseString(
                    Files.readString(sentFile, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var e : o.getAsJsonArray("hashes")) {
                sent.add(e.getAsString());
            }
        } catch (Exception e) {
            // 壞掉就當沒送過。最壞情況是重送一次，伺服器那邊會去重。
        }
    }

    private static void writeSent() {
        if (sentFile == null) {
            return;
        }
        try {
            JsonArray hashes = new JsonArray();
            for (String h : sent) {
                hashes.add(h);
            }
            JsonObject root = new JsonObject();
            root.addProperty("note", "已經分享過的字串雜湊，避免重複送。刪掉只會重送一次。");
            root.add("hashes", hashes);
            Files.writeString(sentFile, root.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 寫不進去就下次重送，沒有比較糟
        }
    }
}
