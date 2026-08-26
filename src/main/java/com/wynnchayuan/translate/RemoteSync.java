package com.wynnchayuan.translate;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;

/**
 * 從 GitHub 同步譯文，讓所有人看到同一份最新翻譯。
 *
 * <h2>為什麼需要</h2>
 * 譯文如果只打包在 jar 裡，每翻好一批就得重新發布一次模組，玩家也得重新下載。
 * 改成從 GitHub 讀取之後，譯者把 PR 合進去，所有人下次進遊戲就更新了，
 * 模組本身不必動。
 *
 * <h2>離線與失敗處理</h2>
 * 抓不到就用<b>上一次抓到的快取</b>；沒有快取就用 jar 內建的版本。
 * 所以斷網、GitHub 掛掉、或防火牆擋住都只是「沒有最新的」，不會變成沒有翻譯。
 *
 * <p>整個過程在背景執行緒進行，不會拖慢進遊戲的速度。
 */
public final class RemoteSync {

    /** 譯文檔在 repo 裡的路徑。 */
    private static final String PATH = "src/main/resources/assets/wynnchayuan/translations/";

    /**
     * 依序嘗試的來源。
     *
     * <h2>為什麼 raw 在前、CDN 在後</h2>
     * 原本反過來，結果是譯者在 GitHub 上合併之後<b>最多 12 小時</b>遊戲裡才會變——
     * jsDelivr 的快取就是這麼久，而且它從來不會「失敗」，所以永遠退不到備援那條。
     *
     * <p>對這個專案來說，翻譯剛改完就要看得到比省流量重要得多：譯者改完進遊戲
     * 對照，是唯一能確認自己翻對的方法。改成 raw 在前之後，合併完重開就生效。
     *
     * <p>jsDelivr 留作備援，扛的是 raw 被擋掉或流量受限的情況——那時候拿到
     * 稍微舊一點的譯文，總比完全沒有好。
     */
    private static final List<String> SOURCES = List.of(
            "https://raw.githubusercontent.com/LyuChaCha/WynnChaYuan/main/" + PATH,
            "https://cdn.jsdelivr.net/gh/LyuChaCha/WynnChaYuan@main/" + PATH);

    /** 要同步的檔案。清單來自 _index.json，新增譯文檔不必改這裡。 */
    private static List<String> files(String lang) {
        List<String> names = new java.util.ArrayList<>();
        names.add("_index.json");              // 先更新清單，才知道還有哪些新檔案
        names.addAll(FileIndex.bundled(lang));
        return names;
    }

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private static volatile String lastResult = "尚未同步";

    private RemoteSync() {}

    /** 依序試每個來源，任一成功就算數。 */
    private static boolean fetchOne(HttpClient client, Path cacheDir,
                                    String lang, String name) {
        for (String base : SOURCES) {
            try {
                HttpRequest request = HttpRequest.newBuilder(
                        URI.create(base + Languages.path(lang) + name))
                        .timeout(TIMEOUT)
                        .header("User-Agent", "WynnChaYuan")
                        // 中間還可能有公司／ISP 的快取代理，一併請它們別給舊的
                        .header("Cache-Control", "no-cache")
                        .header("Pragma", "no-cache")
                        .GET()
                        .build();
                HttpResponse<InputStream> response =
                        client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                if (response.statusCode() != 200) {
                    continue;
                }
                Path tmp = cacheDir.resolve(name + ".tmp");
                // 清單裡的名字可能帶資料夾（`ability/mage.json`）。資料夾不存在的話
                // 這裡會丟 NoSuchFileException，兩個來源都失敗，那個檔就<b>整個沒下載</b>——
                // GitHub 上明明有譯文，遊戲裡卻一條都沒有。
                Files.createDirectories(tmp.getParent());
                try (InputStream in = response.body()) {
                    Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                }
                // 先確認是合法 JSON 再蓋上去。半截或錯誤頁面蓋掉舊檔的話，
                // 使用者會從「翻譯有點舊」變成「翻譯全沒了」。
                String body = Files.readString(tmp, StandardCharsets.UTF_8);
                if (body.isBlank() || !body.stripLeading().startsWith("{")) {
                    Files.deleteIfExists(tmp);
                    continue;
                }
                Files.move(tmp, cacheDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (Exception e) {
                // 這個來源不通就試下一個
            }
        }
        return false;
    }

    public static String lastResult() {
        return lastResult;
    }

    /**
     * 把遠端譯文抓下來放進 {@code cacheDir}。
     *
     * <p>每個檔案先寫暫存檔再原子搬移，中途失敗不會留下半截 JSON 蓋掉舊的好檔案。
     *
     * @return 成功更新的檔案數
     */
    public static int fetchInto(Path cacheDir) {
        return fetchInto(cacheDir, Languages.DEFAULT);
    }

    /**
     * @param lang 只抓這一種語言。其他語言的檔案不會被下載——
     *             多語言對玩家來說因此是零流量成本。
     */
    public static int fetchInto(Path cacheDir, String lang) {
        int ok = 0;
        int failed = 0;
        try {
            Files.createDirectories(cacheDir);
        } catch (Exception e) {
            lastResult = "無法建立快取資料夾：" + e.getMessage();
            return 0;
        }

        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()) {

            for (String name : files(lang)) {
                if (fetchOne(client, cacheDir, lang, name)) {
                    ok++;
                } else {
                    failed++;
                }
            }
        } catch (Exception e) {
            lastResult = "連線失敗：" + e.getMessage() + "（改用本機快取）";
            return ok;
        }

        lastResult = failed == 0
                ? "已從 GitHub 同步 " + ok + " 個檔案"
                : "同步 " + ok + " 個檔案，" + failed + " 個失敗（失敗的用舊快取）";
        return ok;
    }
}
