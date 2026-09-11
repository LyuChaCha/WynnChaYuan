package com.wynnchayuan;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 「有新版了」與「這一版改了什麼」。
 *
 * <h2>為什麼要有</h2>
 * 譯文會自己同步，所以大部分更新玩家不必做任何事——但<b>程式</b>改了就得換 jar，
 * 而沒有人會每天去逛下載頁。實際的結果是：修好的 bug 玩家還在踩，回報進來的
 * 問題其實上一版就解決了。
 *
 * <p>另一半是「這一版到底改了什麼」。放在 GitHub 的 release notes 只有會去看的人
 * 看得到；放在 F6 裡，開設定的人順手就看到了。
 *
 * <h2>一份檔案，兩個用途</h2>
 * 倉庫根目錄的 {@code version.json} 同時是：
 *
 * <ul>
 *   <li><b>線上</b>那一份——「現在最新是哪一版」的真相。發了新版只要改這個檔，
 *       玩家下次進遊戲就會被告知，不必等他們自己去看下載頁。</li>
 *   <li><b>打包進 jar</b> 的那一份——斷網時 F6 的「本版更新內容」照樣看得到。
 *       那件事講的是<b>手上這個 jar</b>，本來就不需要連線。</li>
 * </ul>
 *
 * <p>兩份是同一個檔案（見 {@code build.gradle} 的 processResources），所以不會
 * 分岔。線上讀得到就用線上的（比較新），讀不到就用內建的。
 */
public final class Releases {

    private Releases() {}

    /** 線上那一份。跟譯文同一套來源：raw 在前、CDN 備援。 */
    private static final List<String> SOURCES = List.of(
            "https://raw.githubusercontent.com/LyuChaCha/WynnChaYuan/main/version.json",
            "https://cdn.jsdelivr.net/gh/LyuChaCha/WynnChaYuan@main/version.json");

    private static final String BUNDLED = "/assets/wynnchayuan/version.json";

    /** 一則更新說明：一句話的標題，加上幾條細項。 */
    public record Notes(String headline, List<String> items) {}

    private static volatile JsonObject data;
    private static volatile boolean fromNetwork;

    /** 線上那一份問過了沒（成功或失敗都算）。見 {@link #tellOnce}。 */
    private static volatile boolean checked;

    /**
     * 先讀 jar 內建那一份，之後在背景換成線上的。
     *
     * <p>順序是刻意的：F6 隨時可能被打開，而內建那份<b>一定</b>讀得到。
     * 先把它擺上去，畫面就不會有「載入中」這種狀態。
     */
    public static void init() {
        data = bundled();
        Thread fetch = new Thread(Releases::refresh, WynnChaYuan.MOD_ID + "-version");
        fetch.setDaemon(true);
        fetch.start();
    }

    private static JsonObject bundled() {
        try (InputStream in = Releases.class.getResourceAsStream(BUNDLED)) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseString(new String(in.readAllBytes(),
                    StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return null;      // 內建的壞掉就當沒有，不要讓它擋住啟動
        }
    }

    private static void refresh() {
        try {
            fetch();
        } finally {
            checked = true;       // 成功或失敗都算問過了，見 #tellOnce
        }
    }

    private static void fetch() {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        for (String url : SOURCES) {
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(15))
                        .header("User-Agent", "WynnChaYuan/" + WynnChaYuan.version())
                        .GET().build();
                HttpResponse<String> response = http.send(request,
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() / 100 != 2) {
                    continue;
                }
                JsonObject fresh = JsonParser.parseString(response.body()).getAsJsonObject();
                if (fresh.has("latest")) {
                    data = fresh;
                    fromNetwork = true;
                }
                return;
            } catch (Exception e) {
                // 換下一個來源；都失敗就用內建那份
            }
        }
    }

    /**
     * 有比手上這一版更新的嗎。
     *
     * <h2>為什麼是「不一樣」而不是「比較大」</h2>
     * 版本號在 Beta 期間可能長成 {@code 0.1.0_1}，那不是語意化版本，
     * 比大小要自己寫一套規則——而規則寫錯的後果是「明明有新版卻不提示」，
     * 那正是這支程式存在的理由。
     *
     * <p>「線上說的最新版跟我手上這版不一樣」已經足夠。唯一會誤報的情況是
     * 玩家裝了比線上還新的 jar（自己建置的），那本來就該讓他知道兩邊對不上。
     *
     * @return 線上說的最新版本；已經是最新、或讀不到線上那份時回傳 {@code null}
     */
    public static String newer() {
        JsonObject o = data;
        if (o == null || !fromNetwork || !o.has("latest")) {
            return null;      // 沒連上線就不要亂講「有新版」
        }
        String latest = o.get("latest").getAsString();
        return latest.equals(WynnChaYuan.version()) ? null : latest;
    }

    /** 下載頁。CurseForge 優先——使用者指定的。 */
    public static String downloadUrl() {
        JsonObject o = data;
        if (o != null && o.has("download")) {
            JsonObject urls = o.getAsJsonObject("download");
            for (String key : new String[] {"curseforge", "modrinth", "github"}) {
                if (urls.has(key) && !urls.get(key).getAsString().isBlank()) {
                    return urls.get(key).getAsString();
                }
            }
        }
        return "https://github.com/LyuChaCha/WynnChaYuan/releases/latest";
    }

    /**
     * 有新版時在聊天室說一次，附下載連結。
     *
     * <h2>為什麼講在聊天室</h2>
     * 這是唯一一個玩家一定會看的地方。放在 F6 裡的話，只有本來就會去開設定的人
     * 看得到——而「不知道有新版」的人恰好就是不會去開設定的那些人。
     *
     * <p>同一版只講一次（記在 config 的 {@code notifiedVersion}）。每次進遊戲
     * 都跳一次的提示，第三次之後就沒有人在看了。
     */
    public static void tellOnce(net.minecraft.client.Minecraft client) {
        if (told || client == null || client.player == null) {
            return;
        }
        String latest = newer();
        if (latest == null) {
            // 還沒問到就下一次 tick 再試；問過了而沒有新版就到此為止，
            // 不要每一幀都再問一次。
            told = checked;
            return;
        }
        told = true;
        if (latest.equals(WynnChaYuan.config().notifiedVersion())) {
            return;           // 這一版提示過了
        }
        WynnChaYuan.config().notifiedVersion(latest);

        String url = downloadUrl();
        client.player.displayClientMessage(
                com.wynnchayuan.client.T.c("chat.update.line1",
                                latest, WynnChaYuan.version())
                        .withStyle(net.minecraft.ChatFormatting.AQUA), false);
        Notes notes = notesFor(latest);
        if (notes != null && !notes.headline().isBlank()) {
            client.player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                    "  " + notes.headline())
                    .withStyle(net.minecraft.ChatFormatting.GRAY), false);
        }
        client.player.displayClientMessage(
                com.wynnchayuan.client.T.c("chat.update.link")
                .withStyle(style -> style
                        .withColor(net.minecraft.ChatFormatting.YELLOW)
                        .withUnderlined(true)
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent.OpenUrl(
                                URI.create(url)))
                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(
                                net.minecraft.network.chat.Component.literal(url)))),
                false);
        client.player.displayClientMessage(
                com.wynnchayuan.client.T.c("chat.update.where")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY), false);
    }

    /** 這一場講過了沒。見 {@link #tellOnce}。 */
    private static volatile boolean told;

    /** 手上這一版改了什麼。查不到就回傳 {@code null}，呼叫端自己決定要不要空著。 */
    public static Notes running() {
        return notesFor(WynnChaYuan.version());
    }

    /**
     * 這一版的更新說明，挑玩家看得懂的那一種語言。
     *
     * <p>每一版預設寫英文；要哪一種語言另一套說法，就在那一版底下加一個
     * 語言代碼的物件。查不到就用英文那一份——跟介面文字同一個規則
     * （見 {@code client.T}）。
     */
    private static JsonObject localised(JsonObject one) {
        String lang;
        try {
            lang = net.minecraft.client.Minecraft.getInstance()
                    .getLanguageManager().getSelected();
        } catch (Exception e) {
            return one;        // 還沒有遊戲實例（測試、啟動極早期）
        }
        return lang != null && one.has(lang) && one.get(lang).isJsonObject()
                ? one.getAsJsonObject(lang) : one;
    }

    /**
     * 有更新說明的版本，新的排前面。
     *
     * <p>排序用「拆成數字一段一段比」，不是字串比大小——字串比的話
     * {@code 0.1.10} 會排在 {@code 0.1.2} 前面，而那是錯的。
     * 底線那一段（{@code 0.1.2_1}）當成又一段數字。
     */
    public static List<String> versions() {
        JsonObject o = data;
        if (o == null || !o.has("notes")) {
            return List.of();
        }
        List<String> all = new ArrayList<>(o.getAsJsonObject("notes").keySet());
        all.sort((a, b) -> compare(b, a));
        return List.copyOf(all);
    }

    private static int compare(String a, String b) {
        String[] left = a.split("[._]");
        String[] right = b.split("[._]");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            int x = i < left.length ? number(left[i]) : 0;
            int y = i < right.length ? number(right[i]) : 0;
            if (x != y) {
                return Integer.compare(x, y);
            }
        }
        return 0;
    }

    private static int number(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static Notes notesFor(String version) {
        JsonObject o = data;
        if (o == null || !o.has("notes")) {
            return null;
        }
        JsonObject all = o.getAsJsonObject("notes");
        if (!all.has(version)) {
            return null;
        }
        JsonObject one = localised(all.getAsJsonObject(version));
        List<String> items = new ArrayList<>();
        if (one.has("items")) {
            JsonArray rows = one.getAsJsonArray("items");
            for (int i = 0; i < rows.size(); i++) {
                items.add(rows.get(i).getAsString());
            }
        }
        String headline = one.has("headline") ? one.get("headline").getAsString() : "";
        return new Notes(headline, List.copyOf(items));
    }
}
