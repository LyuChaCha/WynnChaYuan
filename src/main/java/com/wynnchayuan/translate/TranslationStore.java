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
     * 技能名稱 → 譯名。
     *
     * <h2>為什麼需要</h2>
     * 技能說明裡到處都在提別的技能：「{@code Bash will hit a second time}」。
     * 那個 {@code Bash} 是<b>同一個東西</b>，卻散在幾百條敘述裡各翻各的——
     * 翻譯團隊要一條一條改，而且改完還是會不一致。
     *
     * <p>所以只翻一次：{@code ability.json} 裡 {@code Bash} 這一條翻好，
     * 所有提到它的敘述自動跟著換。
     *
     * <p>用<b>名稱本身</b>當參照，不用座標代號。技能樹的位置會隨版本改動，
     * 而且譯者在語料裡看到 {@code Bash} 一眼就懂，看到 {@code warrior-1-1-5}
     * 只能去查表。
     */
    private final Map<String, String> terms = new ConcurrentHashMap<>();

    /** 一個術語最多幾個字。用來限制比對時往前看多遠。 */
    private volatile int maxTermWords = 1;

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
        unindented.clear();
        speakers.clear();
        prefixIndex.clear();
        terms.clear();
        maxTermWords = 1;
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

        try {
            // 依 _index.json 的順序載入。順序有意義：後載入的會覆蓋先前同鍵的譯文，
            // 所以專用檔（npc、quest）排在通用檔之後才能勝出。
            //
            // 直接照清單解析路徑，不用 Files.list——後者<b>不會遞迴</b>，
            // 技能樹依職業拆成 `ability/mage.json` 之後就整片讀不到了。
            List<String> order = FileIndex.forDirectory(dir);
            java.util.LinkedHashSet<Path> wanted = new java.util.LinkedHashSet<>();
            for (String name : order) {
                if (name.startsWith("_") || !name.endsWith(".json")) {
                    continue;
                }
                Path file = dir.resolve(name);
                if (Files.isRegularFile(file)) {
                    wanted.add(file);
                }
            }
            // 清單沒提到、但使用者自己丟進來的也要讀，排在最後
            try (var extra = Files.list(dir)) {
                extra.filter(p -> p.getFileName().toString().endsWith(".json"))
                     .filter(p -> !p.getFileName().toString().startsWith("_"))
                     .sorted()
                     .forEach(wanted::add);
            }
            wanted.forEach(this::load);
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
                // 檔名以 -terms.json 結尾的，內容是<b>可以在別的句子裡自動替換的詞</b>
                // （魔力儲庫、失衡、裂隙之裔…），不是一整行的譯文。
                //
                // 不是每個扁平檔都能這樣用：`gui.json` 那種收的是整行，
                // 把它們全部當成詞會在不相干的句子裡亂換。所以用檔名明說。
                readFlat(obj, file.getFileName().toString().endsWith("-terms.json"));
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
                if (srcKey.length() >= MIN_PREFIX_LENGTH) {
                    prefixIndex.put(srcKey, dst.strip());
                }
                noteBlockSize(srcKey);
                noteFlat(srcKey, dst.strip());
                noteIndented(srcKey, dst.strip());
                String who = optString(e, "speaker");
                if (who != null && !who.isBlank()) {
                    speakers.put(srcKey, who.strip());
                }
                if ("name".equals(optString(e, "role"))) {
                    if (itemNames) {
                        nameKeys.add(srcKey);
                    } else {
                        // 技能名稱與 Major ID 名稱會出現在別的敘述裡；
                        // 裝備名稱不會，收進來只會亂替換。
                        noteTerm(srcKey, dst.strip());
                    }
                }
            }
        }
    }

    /**
     * @param asTerms 這個檔收的是<b>可替換的詞</b>而不是整行的譯文。
     *                見呼叫處：靠檔名 {@code *-terms.json} 判斷。
     */
    private void readFlat(JsonObject obj, boolean asTerms) {
        for (String key : obj.keySet()) {
            if (key.startsWith("_")) {
                continue;                     // _meta 之類的欄位
            }
            JsonElement v = obj.get(key);
            if (v.isJsonPrimitive() && !v.getAsString().isBlank()) {
                entries.put(key.strip(), v.getAsString().strip());
                // 逐字打字時靠這個索引找「目前打到一半的是哪一句」。
                //
                // 先前只有 workspace 格式的檔案會進來，而台詞幾乎都放在扁平格式的
                // quest.json 裡——於是整句打完才查得到，打字中途一律落空。玩家看到的
                // 「英文跑完才忽然跳成中文」就是這裡漏掉的一行。
                // 詞彙表不收：短詞進來只會讓前綴變得分不出是哪一句。
                if (!asTerms && key.strip().length() >= MIN_PREFIX_LENGTH) {
                    prefixIndex.put(key.strip(), v.getAsString().strip());
                }
                noteBlockSize(key.strip());
                noteFlat(key.strip(), v.getAsString().strip());
                noteIndented(key.strip(), v.getAsString().strip());
                if (asTerms) {
                    noteTerm(key.strip(), v.getAsString().strip());
                }
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
        // 不能只收「正規化之後有變」的。語料裡的長句本來就沒有換行——
        // 會斷行的是<b>畫面</b>，不是語料。查詢端才是那個帶著換行進來的。
        flat.putIfAbsent(normalise(key), value);
    }

    /** 把所有連續空白（含換行）壓成一個空格。 */
    public static String normalise(String text) {
        return text == null ? "" : text.strip().replaceAll("\\s+", " ");
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

    /**
     * 記一個可以在別的敘述裡自動替換的名稱。
     *
     * <p>太短的不收：{@code Aid}、{@code Ice} 這種在一般句子裡到處都是，
     * 換錯比沒換更糟。含佔位符或換行的也不收，那不是一個名稱。
     */
    private void noteTerm(String name, String translation) {
        if (name.length() < MIN_TERM_LENGTH || translation.isBlank()
                || name.indexOf('{') >= 0 || name.indexOf('\n') >= 0) {
            return;
        }
        terms.put(name, translation);
        maxTermWords = Math.max(maxTermWords, name.split(" ").length);
    }

    /** 見 {@link #noteTerm}。四個字元大約是最短不會誤中的名稱。 */
    private static final int MIN_TERM_LENGTH = 4;

    /** 在 {@code text} 裡從 {@code from} 起找第一個技能名稱。 */
    public Term findTerm(String text, int from) {
        if (terms.isEmpty()) {
            return null;
        }
        for (int i = from; i < text.length(); i++) {
            if (!startsWord(text, i) || !Character.isUpperCase(text.charAt(i))) {
                continue;                      // 名稱一律是大寫開頭的專有名詞
            }
            int end = i;
            for (int words = 0; words < maxTermWords && end <= text.length(); words++) {
                end = wordEnd(text, end);
                String candidate = text.substring(i, end);
                String hit = terms.get(candidate);
                if (hit != null) {
                    return new Term(i, end, hit);
                }
                if (end >= text.length() || text.charAt(end) != ' ') {
                    break;                     // 後面不是空格，接不下去了
                }
                end++;
            }
        }
        return null;
    }

    /** 這一整段剛好就是一個技能名稱的話，回傳它的譯名。 */
    /**
     * 查一個可替換的詞。
     *
     * <h2>為什麼要處理尾巴</h2>
     * Wynncraft 把資源符號跟詞放在<b>同一個色段</b>裡：
     *
     * <pre>
     *   §7adds §f+8§7 Mana to your §bMana Bank ✺
     * </pre>
     *
     * 藍色那一段是「{@code Mana Bank ✺}」，連著符號。拿整段去查當然查不到，
     * 而使用者看到的就是「詞典裡明明有，畫面上還是英文」。
     *
     * <p>剝掉尾巴查到之後<b>把尾巴接回去</b>——符號跟詞是同一個色段，
     * 一起換掉才會連顏色都對。
     */
    public String lookupTerm(String text) {
        if (text == null) {
            return null;
        }
        String key = text.strip();
        String hit = terms.get(key);
        if (hit != null) {
            return hit;
        }
        int end = key.length();
        while (end > 0 && !Character.isLetterOrDigit(key.charAt(end - 1))) {
            end--;
        }
        if (end == key.length() || end == 0) {
            return null;
        }
        String core = terms.get(key.substring(0, end).strip());
        return core == null ? null : core + key.substring(end);
    }

    /** 找到的名稱在原文的哪一段，以及它的譯名。 */
    public record Term(int start, int end, String translation) {}

    private static boolean startsWord(String text, int at) {
        return at == 0 || !Character.isLetterOrDigit(text.charAt(at - 1));
    }

    private static int wordEnd(String text, int from) {
        int i = from;
        while (i < text.length() && (Character.isLetterOrDigit(text.charAt(i))
                || text.charAt(i) == '\'' || text.charAt(i) == '-')) {
            i++;
        }
        return i;
    }

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
    /**
     * 這句話是誰講的。
     *
     * <h2>為什麼要有</h2>
     * 對話疊層只顯示一段文字，玩家看不出來是誰在說——尤其是同一個場景裡
     * 好幾個 NPC 輪流講話的時候。語料裡本來就有 `speaker`，只是沒被帶進遊戲。
     *
     * @return 說話者的名字，沒有就回 null
     */
    public String speakerOf(String template) {
        return template == null ? null : speakers.get(template.strip());
    }

    /** 每一句對話的說話者。見 {@link #speakerOf}。 */
    private final Map<String, String> speakers = new ConcurrentHashMap<>();

    /**
     * 半句話也查得到：拿<b>開頭</b>去比對。
     *
     * <h2>為什麼需要</h2>
     * NPC 的對話是一個字一個字打出來的，打到一半的句子當然不在語料裡——
     * 於是譯文要等整句打完才出現，而玩家常常已經按 shift 跳過去了。
     *
     * <p>只要開頭夠長、而且<b>只有一條</b>語料以它開頭，就可以確定是哪一句，
     * 直接把整句譯文先顯示出來。有兩條以上以它開頭就再等等——寧可晚一點出現，
     * 也不要先顯示錯的那一句再跳掉。
     *
     * @return 整句的譯文，還不能確定就回 null
     */
    public String lookupPrefix(String partial) {
        String source = matchPrefix(partial);
        return source == null ? null : prefixIndex.get(source);
    }

    /** 半句話也認得出說話者。見 {@link #matchPrefix}。 */
    public String speakerOfPrefix(String partial) {
        String source = matchPrefix(partial);
        return source == null ? null : speakers.get(source);
    }

    /**
     * 這半句是<b>哪一條</b>原文的開頭。
     *
     * <h2>為什麼要把原文也交出去</h2>
     * NPC 是一個字一個字打出來的，同一句話在打完之前會以幾十種長度進來。
     * 光拿到譯文的話，呼叫端分不出「這是新的一句」還是「還是剛才那句、只是又長了」，
     * 於是每一次都重算一遍——算出來的東西時有時無，畫面就閃。
     * 知道命中的是哪一條原文，就能直接判斷：新進來的還是它的開頭，那就什麼都不用做。
     *
     * @return 完整的原文；開頭還不夠獨特、分不出是哪一句時回傳 {@code null}
     */
    public String matchPrefix(String partial) {
        return matchPrefix(partial, partial == null ? 0 : partial.strip().length());
    }

    /**
     * 同上，但門檻另外算。
     *
     * <p>參數化會把玩家名收成 {@code {u}}，模板因此比畫面上實際打出來的字短一大截。
     * 拿模板長度當門檻，開頭那一小段就會查不到而先閃出英文。呼叫端知道畫面上
     * 究竟打了多少字，就用那個數字當「證據夠不夠」的依據。
     *
     * @param evidence 畫面上已經打出來的字數
     */
    public String matchPrefix(String partial, int evidence) {
        if (partial == null) {
            return null;
        }
        String key = partial.strip();
        if (key.isEmpty() || evidence < MIN_PREFIX_LENGTH) {
            return null;
        }
        Map.Entry<String, String> first = prefixIndex.ceilingEntry(key);
        if (first == null || !first.getKey().startsWith(key)) {
            return null;
        }
        String next = prefixIndex.higherKey(first.getKey());
        if (next != null && next.startsWith(key)) {
            return null;                       // 還分不出是哪一句
        }
        return first.getKey();
    }

    /**
     * 開頭要多長才敢猜。太短的話「I」「Well,」會撞到幾百句。
     */
    private static final int MIN_PREFIX_LENGTH = 12;

    /** 依原文排序，才能用 ceiling/higher 做前綴比對。見 {@link #lookupPrefix}。 */
    private final java.util.TreeMap<String, String> prefixIndex = new java.util.TreeMap<>();

    public String lookup(String template) {
        if (template == null) {
            return null;
        }
        String key = template.strip();
        if (!translateNames && nameKeys.contains(key)) {
            return null;                       // 使用者選擇不翻物品名稱
        }
        String hit = entries.get(key);
        return hit != null ? hit : lookupIndented(key);
    }

    /**
     * Wynncraft 拿來做<b>縮排</b>的字元。在它的字型裡 {@code À} 是一格固定寬度的空白，
     * 用來把「效果」那類的細項推到右邊對齊。
     */
    private static final char INDENT = 'À';

    /** 去掉開頭縮排之後的索引。見 {@link #lookupIndented}。 */
    private final Map<String, String> unindented = new ConcurrentHashMap<>();

    private static int indentOf(String text) {
        int n = 0;
        while (n < text.length() && text.charAt(n) == INDENT) {
            n++;
        }
        return n;
    }

    private void noteIndented(String src, String dst) {
        int n = indentOf(src);
        if (n > 0) {
            unindented.putIfAbsent(src.substring(n), dst.substring(indentOf(dst)));
        }
    }

    /**
     * 縮排<b>幾格不算內容</b>，所以查表時不該把它算進去。
     *
     * <h2>為什麼需要這一層</h2>
     * 語料是從 CDN 來的，同一句話在那裡的縮排是 38、39、40 格；遊戲實際送出來的
     * 卻是 4 格或 42 格。兩邊字面上永遠不相等，於是
     * {@code Knockback Immunity to Allies} 這種句子一直維持英文——不是漏翻，
     * 是<b>查不到</b>。
     *
     * <p>縮排幾格取決於畫面寬度與同一組細項裡最長的那一行，是<b>排版</b>不是語意。
     * 所以剝掉開頭的縮排來查，查到之後把<b>呼叫端自己的那一串</b>接回去——
     * 用遊戲當下的格數，對齊才不會跑掉。
     */
    private String lookupIndented(String key) {
        int indent = indentOf(key);
        if (indent == 0) {
            return null;
        }
        String body = unindented.get(key.substring(indent));
        return body == null ? null : key.substring(0, indent) + body;
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
        // 攤平查表的對象是「依畫面寬度自動斷行」的長敘述——它在語料裡是<b>一行</b>，
        // 所以 maxBlockLines 對它一無所知。先前就是這樣：Major ID 的說明在畫面上
        // 佔四行，而 maxBlockLines 只有 2，那四行從來沒有被併起來試過。
        return flat.isEmpty() ? maxBlockLines : Math.max(maxBlockLines, FLOWED_SCAN_LINES);
    }

    /**
     * 攤平查表最多把幾行併起來試。
     *
     * <h2>為什麼是這個數字</h2>
     * 這個上限決定了「多長的段落還救得回來」。先前是 8，而<b>裝備的背景敘述
     * 普遍比這長</b>——語料裡最長的一條 346 個字元，在 tooltip 裡是十一行。
     * 併不到十一行就永遠湊不出那個鍵，於是譯文明明在檔案裡，畫面上還是整段英文。
     *
     * <p>現在照著語料裡最長的那一條抓：316 條攤平條目裡有 26 條超過八行，
     * 沒有一條超過十二行。留兩行餘裕給窄一點的畫面。
     *
     * <p>成本是每個位置最多多查十三次，而查一次只是雜湊表比對；
     * 真正會擋下來的是 {@code translateBlock} 開頭那個「有空行就放棄」的判斷，
     * 段落之間的空行讓大部分長度連查都不會查。
     */
    private static final int FLOWED_SCAN_LINES = 14;

    public int size() {
        return entries.size();
    }

    public int loadedFiles() {
        return loadedFiles;
    }
}
