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

    /** 譯文檔在 repo 裡的位置。 */
    private static final String BASE =
            "https://raw.githubusercontent.com/LyuChaCha/WynnChaYuan/main/"
            + "src/main/resources/assets/wynnchayuan/translations/";

    /** 要同步的檔案。與內建的那份一致。 */
    private static final List<String> FILES = List.of(
            "ui-labels.json",
            "gear-weapon.json",
            "gear-armour.json",
            "gear-accessory.json",
            "ability.json",
            "ingredient.json",
            "material.json",
            "tome.json",
            "aspect.json",
            "charm.json");

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private static volatile String lastResult = "尚未同步";

    private RemoteSync() {}

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

            for (String name : FILES) {
                try {
                    HttpRequest request = HttpRequest.newBuilder(URI.create(BASE + name))
                            .timeout(TIMEOUT)
                            .header("User-Agent", "WynnChaYuan")
                            .GET()
                            .build();
                    HttpResponse<InputStream> response =
                            client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                    if (response.statusCode() != 200) {
                        failed++;
                        continue;
                    }
                    Path tmp = cacheDir.resolve(name + ".tmp");
                    try (InputStream in = response.body()) {
                        Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                    }
                    // 先驗證是合法 JSON 再蓋上去，免得半截檔案讓整份譯文失效
                    String body = Files.readString(tmp, StandardCharsets.UTF_8);
                    if (body.isBlank() || !body.stripLeading().startsWith("{")) {
                        Files.deleteIfExists(tmp);
                        failed++;
                        continue;
                    }
                    Files.move(tmp, cacheDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
                    ok++;
                } catch (Exception e) {
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
