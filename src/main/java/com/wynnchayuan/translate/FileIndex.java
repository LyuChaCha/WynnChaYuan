package com.wynnchayuan.translate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 譯文檔的清單，來自 {@code translations/_index.json}。
 *
 * <h2>為什麼要有這個</h2>
 * 檔名原本寫死在兩個地方（{@link StarterFiles} 與 {@link RemoteSync}）。
 * 新增一個翻譯檔就得記得兩邊都改，漏掉其中一邊的後果很難察覺——
 * 例如檔案有被放出去卻不會同步，或反過來。
 *
 * <p>改成讀清單之後，加檔案只要動 {@code _index.json}，程式端完全不用改。
 * 這對「未來還會不斷新增翻譯領域」的專案是必要的。
 *
 * <p>清單讀不到時退回內建的最小集合，至少介面標籤還在。
 */
public final class FileIndex {

    /** 語言資料夾裡的清單。見 {@link Languages}。 */
    private static String resource(String lang) {
        return Languages.resource(lang) + "_index.json";
    }

    /** 清單本身壞掉時的退路。 */
    private static final List<String> FALLBACK = List.of("ui-labels.json", "npc.json", "quest.json");

    private static final java.util.Map<String, List<String>> cached =
            new java.util.concurrent.ConcurrentHashMap<>();

    private FileIndex() {}

    /** 打包在 jar 裡的清單。 */
    public static List<String> bundled() {
        return bundled(Languages.DEFAULT);
    }

    /** 打包在 jar 裡的清單。各語言的清單是獨立的——收的檔案可以不一樣多。 */
    public static List<String> bundled(String lang) {
        List<String> hit = cached.get(lang);
        if (hit != null) {
            return hit;
        }
        List<String> found = read(() -> FileIndex.class.getResourceAsStream(resource(lang)));
        cached.put(lang, found);
        return found;
    }

    /**
     * 使用者 config 目錄裡的清單，讀不到就用內建的。
     *
     * <p>讓使用者能自己加檔案——例如額外翻某個伺服器活動的字串。
     */
    public static List<String> forDirectory(Path dir) {
        Path local = dir.resolve("_index.json");
        if (Files.isRegularFile(local)) {
            List<String> found = read(() -> Files.newInputStream(local));
            if (!found.isEmpty()) {
                return found;
            }
        }
        return bundled();
    }

    private static List<String> read(StreamSource source) {
        List<String> names = new ArrayList<>();
        try (InputStream in = source.open()) {
            if (in == null) {
                return FALLBACK;
            }
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("files");
            if (arr == null) {
                return FALLBACK;
            }
            for (JsonElement el : arr) {
                String name = el.getAsString().strip();
                if (name.endsWith(".json") && !name.startsWith("_")) {
                    names.add(name);
                }
            }
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 譯文清單讀取失敗，改用內建集合: " + e.getMessage());
            return FALLBACK;
        }
        return names.isEmpty() ? FALLBACK : List.copyOf(names);
    }

    @FunctionalInterface
    private interface StreamSource {
        InputStream open() throws Exception;
    }
}
