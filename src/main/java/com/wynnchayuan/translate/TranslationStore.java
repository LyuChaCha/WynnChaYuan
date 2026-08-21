package com.wynnchayuan.translate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

    /** 見 {@link #maxBlockLines()}。載入時一併算出來，查詢時不必再掃一次。 */
    private volatile int maxBlockLines = 1;

    /**
     * 把所有空白（含換行）壓成單一空格之後的索引。
     *
     * <h2>為什麼需要另一份索引</h2>
     * 長敘述在遊戲裡是<b>依 tooltip 寬度自動斷行</b>的，斷在哪裡取決於玩家的
     * 畫面設定；語料裡存的則是沒斷行的完整句子。兩者永遠不會逐字相等，
     * 所以 Major ID 的說明翻了也不會生效。
     *
     * <p>把兩邊的空白都正規化之後就對得上了——差別只在空白，文字是一樣的。
     */
    private final Map<String, String> flat = new ConcurrentHashMap<>();

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
        flat.clear();
        maxBlockLines = 1;
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
            // 依 _index.json 的順序載入。順序有意義：後載入的會覆蓋先前同鍵的譯文，
            // 所以專用檔（npc、quest）排在通用檔之後才能勝出。
            List<String> order = FileIndex.forDirectory(dir);
            files.filter(p -> p.getFileName().toString().endsWith(".json"))
                 .filter(p -> !p.getFileName().toString().startsWith("_"))
                 .sorted(java.util.Comparator.comparingInt(
                         p -> indexOf(order, p.getFileName().toString())))
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

    /** 不在清單裡的檔案排到最後，但仍然會載入。 */
    private static int indexOf(List<String> order, String name) {
        int i = order.indexOf(name);
        return i < 0 ? Integer.MAX_VALUE : i;
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
                readWorkspace(obj.getAsJsonObject("entries"), itemNames(obj));
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

    /**
     * 這個檔案裡的 {@code role: "name"} 算不算「物品名稱」。
     *
     * <h2>為什麼要問這個</h2>
     * F6 的「翻譯物品名稱」是為了讓裝備名稱保持英文——對得上 wiki、
     * 交易市場與社群討論。但先前的判斷是<b>只要 role 是 name 就算</b>，
     * 於是 {@code ability.json} 裡的技能名稱也被一起關掉了；
     * 技能名稱跟交易市場毫無關係，不該受這個開關影響。
     *
     * <p>判斷寫在資料裡（{@code _meta.itemNames}）而不是在這裡列一份 domain 清單，
     * 是為了維持「新增一個譯文檔只要加進 _index.json，不必改 Java」。
     * 沒寫的話當成物品——漏標一個裝備檔會讓開關<b>無聲失效</b>，
     * 漏標一個技能檔頂多是名稱跟著關掉，後者看得出來。
     */
    private static boolean itemNames(JsonObject root) {
        JsonElement meta = root.get("_meta");
        if (meta == null || !meta.isJsonObject()) {
            return true;
        }
        JsonElement flag = meta.getAsJsonObject().get("itemNames");
        return flag == null || !flag.isJsonPrimitive() || flag.getAsBoolean();
    }

    private void readWorkspace(JsonObject entriesObj, boolean itemNames) {
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
                noteBlockSize(srcKey);
                noteFlat(srcKey, dst.strip());
                if (itemNames && "name".equals(optString(e, "role"))) {
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
                noteBlockSize(key.strip());
                noteFlat(key.strip(), v.getAsString().strip());
            }
        }
    }

    /**
     * 空白正規化之後也記一份。
     *
     * <p>只收<b>夠長</b>的條目。短詞正規化之後很容易跟別的東西撞在一起，
     * 而短詞本來就不會被自動斷行，不需要走這條路。
     */
    private void noteFlat(String key, String value) {
        if (key.length() < MIN_FLAT_LENGTH) {
            return;
        }
        String normalised = normalise(key);
        if (!normalised.equals(key)) {
            flat.putIfAbsent(normalised, value);
        }
    }

    /** 把所有連續空白（含換行）壓成一個空格。 */
    public static String normalise(String text) {
        return text == null ? "" : text.strip().replaceAll("\s+", " ");
    }

    /**
     * 查一段<b>可能被自動斷行</b>的長文字。
     *
     * @param text 已經把幾行併起來的模板
     * @return 譯文；沒有對應條目時回傳 {@code null}
     */
    public String lookupFlat(String text) {
        if (text == null || text.length() < MIN_FLAT_LENGTH) {
            return null;
        }
        return flat.get(normalise(text));
    }

    /** 短於這個長度的不進正規化索引，見 {@link #noteFlat}。 */
    private static final int MIN_FLAT_LENGTH = 24;

    /** 這個鍵跨了幾行，比目前記錄的多就更新。 */
    private void noteBlockSize(String key) {
        int lines = 1;
        for (int i = key.indexOf('\n'); i >= 0; i = key.indexOf('\n', i + 1)) {
            lines++;
        }
        if (lines > maxBlockLines) {
            maxBlockLines = lines;
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

    /**
     * 跨行條目最多幾行。
     *
     * <p>技能說明那類的原文<b>本來就是一整段</b>，CDN 給的是含換行的一大串。
     * 逐行查表永遠查不到——遊戲畫面上是五行，鍵是那五行黏在一起的樣子，
     * 兩者不可能相等。{@code ability.json} 翻了卻沒反應就是這個原因。
     *
     * <p>所以呼叫端得先試著把連續幾行併起來查。這裡回報最多要試到幾行，
     * 沒有跨行條目時是 1，呼叫端就會跳過整段比對。
     */
    public int maxBlockLines() {
        return maxBlockLines;
    }

    public int size() {
        return entries.size();
    }

    public int loadedFiles() {
        return loadedFiles;
    }
}
