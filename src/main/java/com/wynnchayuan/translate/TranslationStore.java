package com.wynnchayuan.translate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 譯文查詢表：模板 → 譯文。
 *
 * <p>吃的是跟 {@code corpus/workspace/} 與 {@code captured.json} 同一種格式，
 * 所以離線抽出來的靜態語料與遊戲內收集的內容可以放在一起，不必轉檔：
 *
 * <pre>
 *   { "entries": { "&lt;hash&gt;": { "src": "...", "dst": "..." } } }
 * </pre>
 *
 * <p>也接受最單純的平面格式，方便手寫小量測試：
 *
 * <pre>
 *   { "Total Damage: {~}": "總傷害: {~}" }
 * </pre>
 *
 * <p>{@code dst} 空白的條目會被略過——那代表還沒翻，應該顯示灰色原文而不是空字串。
 */
public final class TranslationStore {

    private final Map<String, String> entries = new ConcurrentHashMap<>();

    /**
     * 來自 {@code role: "name"} 條目的鍵。
     *
     * <p>裝備名稱多半是專有名詞，翻了反而對不上社群討論與 wiki，
     * 所以要能單獨關掉。工作檔已經標好 role，這裡照著記就好。
     */
    private final java.util.Set<String> nameKeys =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
    private volatile int loadedFiles = 0;

    /** 上一次載入的結果，供設定面板顯示。 */
    private volatile String lastResult = "尚未載入";

    /**
     * 讀取一個目錄下的所有 .json。
     *
     * <p>目錄不存在就<b>自動建立</b>並講清楚——先前這裡是直接 return，
     * 結果資料夾被刪掉時完全沒有任何提示，只會看到「翻譯沒反應」而查不出原因。
     *
     * <p>檔案格式錯誤只會被略過，不中斷遊戲。
     */
    public void loadAll(Path dir) {
        entries.clear();
        nameKeys.clear();
        loadedFiles = 0;

        if (!Files.isDirectory(dir)) {
            try {
                Files.createDirectories(dir);
                lastResult = "已建立譯文資料夾，把 json 放進去再按重新載入";
                System.out.println("[WynnChaYuan] 譯文資料夾不存在，已建立：" + dir);
            } catch (Exception e) {
                lastResult = "無法建立譯文資料夾：" + e.getMessage();
                System.err.println("[WynnChaYuan] 建立譯文資料夾失敗 " + dir + ": " + e.getMessage());
            }
            return;
        }

        try (var files = Files.list(dir)) {
            files.filter(p -> p.getFileName().toString().endsWith(".json"))
                 .sorted()
                 .forEach(this::load);
        } catch (Exception e) {
            lastResult = "讀取失敗：" + e.getMessage();
            System.err.println("[WynnChaYuan] 讀取譯文目錄失敗 " + dir + ": " + e.getMessage());
            return;
        }

        if (loadedFiles == 0) {
            lastResult = "資料夾裡沒有 .json 檔案";
            System.out.println("[WynnChaYuan] " + dir + " 裡沒有譯文檔");
        } else if (entries.isEmpty()) {
            // 檔案讀到了但一條也沒有 —— 幾乎都是 dst 全部留空
            lastResult = "讀了 " + loadedFiles + " 個檔案，但沒有任何已填寫的 dst";
            System.out.println("[WynnChaYuan] " + lastResult);
        } else {
            lastResult = "已載入 " + entries.size() + " 條譯文（" + loadedFiles + " 個檔案）";
            System.out.println("[WynnChaYuan] " + lastResult);
        }
    }

    /** 上一次載入的人話結果，直接顯示給使用者看。 */
    public String lastResult() {
        return lastResult;
    }

    private void load(Path file) {
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(r);
            if (!root.isJsonObject()) {
                return;
            }
            JsonObject obj = root.getAsJsonObject();
            int before = entries.size();

            if (obj.has("entries") && obj.get("entries").isJsonObject()) {
                readWorkspace(obj.getAsJsonObject("entries"));
            } else {
                readFlat(obj);
            }
            loadedFiles++;
            System.out.println("[WynnChaYuan] 載入譯文 " + file.getFileName()
                    + "：+" + (entries.size() - before) + " 條");
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 略過 " + file.getFileName() + "：" + e.getMessage());
        }
    }

    private void readWorkspace(JsonObject entriesObj) {
        for (String key : entriesObj.keySet()) {
            JsonElement el = entriesObj.get(key);
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject e = el.getAsJsonObject();
            String src = optString(e, "src");
            String dst = optString(e, "dst");
            if (src != null && dst != null && !dst.isBlank()) {
                String srcKey = src.strip();
                entries.put(srcKey, dst.strip());
                if ("name".equals(optString(e, "role"))) {
                    nameKeys.add(srcKey);
                }
            }
        }
    }

    private void readFlat(JsonObject obj) {
        for (String key : obj.keySet()) {
            if (key.startsWith("_")) {
                continue;                     // _meta 之類的欄位
            }
            JsonElement v = obj.get(key);
            if (v.isJsonPrimitive() && !v.getAsString().isBlank()) {
                entries.put(key.strip(), v.getAsString().strip());
            }
        }
    }

    private static String optString(JsonObject o, String field) {
        JsonElement el = o.get(field);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : null;
    }

    /**
     * 是否翻譯 {@code role: "name"} 的條目。
     *
     * <p>做成旗標而不是直接去讀全域設定：查詢表不該伸手抓 mod 狀態，
     * 那樣會讓它離開遊戲環境就無法運作（測試會直接 NPE）。
     */
    private volatile boolean translateNames = true;

    public void setTranslateNames(boolean value) {
        this.translateNames = value;
    }

    /** @return 譯文；沒有對應條目時回傳 {@code null} */
    public String lookup(String template) {
        if (template == null) {
            return null;
        }
        String key = template.strip();
        if (!translateNames && nameKeys.contains(key)) {
            return null;                       // 使用者選擇不翻物品名稱
        }
        return entries.get(key);
    }

    public int size() {
        return entries.size();
    }

    public int loadedFiles() {
        return loadedFiles;
    }
}
