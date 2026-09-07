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
    /**
     * 所有裝備名，<b>不論翻了沒</b>。
     *
     * <p>只收<b>自己還沒翻</b>的那些。翻好的走 {@link #nameKeys}（那管的是 F6
     * 開關），本來就會拿到裝備自己的譯名。
     *
     * <p>還沒翻的那些同樣需要保護：語料裡有 50 組裝備名跟別的領域撞名（技能詞、
     * Major ID、介面詞），沒有這份清單的話，別的檔案的譯文會替裝備名頂上去。
     *
     * <p>不能用「entries 裡有沒有這個鍵」來判斷——撞名的那一方<b>就是</b>把鍵
     * 放進 entries 的人，那樣問等於永遠問不出來。第一版就是這樣寫的，測試抓到了。
     */
    private final java.util.Set<String> gearNameKeys = new java.util.HashSet<>();

    /**
     * 中文物品名 → 英文原文，給市集搜尋用。見 {@link MarketSearch}。
     *
     * <p>跟其他索引一起在載入時建好——市集在等你打字的那一秒才建會卡一下，
     * 而那正是最不能卡的時候。
     */
    private final MarketSearch market = new MarketSearch();

    public MarketSearch market() {
        return market;
    }

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
        unwrapped.clear();
        unindented.clear();
        speakers.clear();
        prefixIndex.clear();
        terms.clear();
        maxTermWords = 1;
        maxBlockLines = 1;
        nameKeys.clear();
        gearNameKeys.clear();
        market.clear();
        ordered.clear();
        byQuest.clear();
        fromWiki.clear();
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
                readWorkspace(obj.getAsJsonObject("entries"), itemNames(obj), gearNames(obj));
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
     * 名稱要怎麼收，是<b>兩個各自獨立的問題</b>，用兩個旗標分開問。
     *
     * <h2>一、會不會出現在別的句子裡（{@code _meta.itemNames}）</h2>
     * 技能名稱與 Major ID 名稱會被別的敘述引用（「Bash 會增加範圍」），
     * 所以要收進<b>可替換的詞</b>表。裝備與素材名稱不會，收進來只會亂替換——
     * v1.99.71 就是這樣讓素材 Dark Matter 的譯名蓋到同名盔甲上的。
     *
     * <h2>二、受不受 F6「翻譯物品名稱」管（{@code _meta.gearNames}）</h2>
     * 那個開關是為了讓<b>裝備</b>名稱保持英文——對得上 wiki、交易市場與社群討論。
     * 素材、材料、典籍、面向、護符跟交易市場無關，不該被它關掉；
     * {@code ingredient.json} 的 {@code _meta.note} 從一開始就這樣寫著。
     *
     * <h2>為什麼要拆開</h2>
     * 這兩件事本來共用 {@code itemNames} 一個旗標。六個道具檔標成 false，
     * 原意是第二個問題（「不受開關管」），但那個值同時觸發了第一個問題，
     * 於是道具名全變成可替換的詞。v1.99.71 把它們改成 true 修好了替換，
     * 卻讓素材名稱一起落進裝備開關底下——玩家看到的就是「素材翻譯全部消失」。
     * 一個旗標答兩個問題，怎麼填都會錯一半，所以拆成兩個。
     *
     * <p>兩個都預設「當成裝備」：漏標一個裝備檔會讓開關<b>無聲失效</b>，
     * 漏標一個非裝備檔頂多是名稱跟著開關關掉——後者看得出來，前者看不出來。
     */
    private static boolean itemNames(JsonObject root) {
        return metaFlag(root, "itemNames");
    }

    /** 這個檔的名稱受不受 F6「翻譯物品名稱」管。見 {@link #itemNames}。 */
    private static boolean gearNames(JsonObject root) {
        return metaFlag(root, "gearNames");
    }

    /** 沒寫、寫壞、或不是布林值都算 true——見 {@link #itemNames} 談預設值。 */
    private static boolean metaFlag(JsonObject root, String name) {
        JsonElement meta = root.get("_meta");
        if (meta == null || !meta.isJsonObject()) {
            return true;
        }
        JsonElement flag = meta.getAsJsonObject().get(name);
        return flag == null || !flag.isJsonPrimitive() || flag.getAsBoolean();
    }

    private void readWorkspace(JsonObject entriesObj, boolean itemNames, boolean gearNames) {
        for (String key : entriesObj.keySet()) {
            JsonElement el = entriesObj.get(key);
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject e = el.getAsJsonObject();
            String src = optString(e, "src");
            String dst = optString(e, "dst");
            // 還沒翻的裝備名也要記下來。它是專有名詞，不能讓別的檔案裡剛好同名
            // 的條目替它翻譯——實機踩到的是武器「Guardian」被 Major ID 的
            // 「守護者」蓋掉。全庫掃過去這種撞名有 50 組。見 #gearNameKeys。
            if (gearNames && src != null && !src.isBlank()
                    && (dst == null || dst.isBlank())
                    && "name".equals(optString(e, "role"))) {
                gearNameKeys.add(src.strip());
            }
            if (src != null && dst != null && !dst.isBlank()) {
                String srcKey = src.strip();
                entries.put(srcKey, dst.strip());
                ordered.add(srcKey);
                if (srcKey.length() >= MIN_PREFIX_LENGTH) {
                    prefixIndex.put(srcKey, dst.strip());
                }
                noteBlockSize(srcKey);
                noteFlat(srcKey, dst.strip());
                noteUnwrapped(srcKey, dst.strip());
                noteIndented(srcKey, dst.strip());
                noteMarked(srcKey, dst.strip());
                // 同一個任務的台詞另外建一份索引。全庫裡「Hey, {u}」撞到幾十句，
                // 單一個任務裡通常只有一句。見 #matchPrefix(String, int, String)。
                String quest = optString(e, "quest");
                if (quest != null && !quest.isBlank()) {
                    byQuest.computeIfAbsent(quest.strip(),
                            q -> new java.util.TreeMap<>()).put(srcKey, dst.strip());
                }
                if ("wiki".equals(optString(e, "source"))) {
                    fromWiki.add(srcKey);      // 見 #curatedRival
                }
                String who = optString(e, "speaker");
                if (who != null && !who.isBlank()) {
                    speakers.put(srcKey, who.strip());
                }
                if ("name".equals(optString(e, "role"))) {
                    market.add(srcKey, dst.strip());
                    if (!itemNames) {
                        // 技能名稱與 Major ID 名稱會出現在別的敘述裡；
                        // 裝備與素材名稱不會，收進來只會亂替換。
                        noteTerm(srcKey, dst.strip());
                    } else if (gearNames) {
                        nameKeys.add(srcKey);       // 受 F6 開關管
                    }
                    // 兩個都不是（素材、材料、典籍…）：照一般條目翻，
                    // 既不會被拿去替換別的句子，也不受裝備開關影響。
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
                market.add(key.strip(), v.getAsString().strip());
                ordered.add(key.strip());
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
                noteUnwrapped(key.strip(), v.getAsString().strip());
                noteIndented(key.strip(), v.getAsString().strip());
                noteMarked(key.strip(), v.getAsString().strip());
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
     * 續行的<b>行首圖示</b>也一併拿掉之後的索引。
     *
     * <h2>為什麼 {@link #flat} 不夠</h2>
     * 聊天訊息是伺服器<b>先斷好行</b>送過來的，而且每一行開頭都掛著頻道圖示：
     *
     * <pre>
     *   {#} The Karoshi Union World Event starts in {~}m {~}s! ({~}
     *   {#} blocks away) Click to track
     * </pre>
     *
     * 斷在哪裡取決於事件名多長、以及玩家自己設定的聊天欄寬度。
     * {@link #normalise} 只壓空白，那個續行的 {@code {#}} 會留在原地——
     * 而它的位置正好隨著斷點移動，於是同一句話的每一種斷法都是不同的鍵。
     *
     * <p>七十個世界事件 × 三種斷點，一條一條收是收不完的（而且斷點算不出來）。
     * 把換行與續行的圖示一起拿掉，同一句話就只剩一個鍵。
     */
    private final Map<String, String> unwrapped = new ConcurrentHashMap<>();

    /**
     * 把換行與續行的行首圖示都拿掉，併成一句。
     *
     * <p>只拿掉<b>緊接在換行後面</b>的圖示。句子中間的圖示是內容的一部分
     * （{@code craft {#} Boots}），拿掉會改變語意，也會讓佔位符數量對不上。
     */
    public static String unwrap(String text) {
        if (text == null) {
            return "";
        }
        String glyph = com.wynnchayuan.capture.GlyphSplitter.GLYPH_PLACEHOLDER;
        StringBuilder out = new StringBuilder(text.length());
        int at = 0;
        while (at < text.length()) {
            if (text.charAt(at) != '\n') {
                out.append(text.charAt(at));
                at++;
                continue;
            }
            at++;                              // 吃掉換行
            while (at < text.length()) {
                if (text.startsWith(glyph, at)) {
                    at += glyph.length();      // 續行的行首圖示
                } else if (Character.isWhitespace(text.charAt(at))) {
                    at++;
                } else {
                    break;
                }
            }
            out.append(' ');                   // 接起來的兩截之間要有一個空白
        }
        return normalise(out.toString());
    }

    /** 見 {@link #unwrapped}。索引<b>所有</b>夠長的條目，語料那邊不必也帶著換行。 */
    private void noteUnwrapped(String key, String value) {
        if (key.length() >= MIN_FLAT_LENGTH) {
            unwrapped.putIfAbsent(unwrap(key), value);
        }
    }

    /**
     * 把斷好行的整則訊息當成一句話查。
     *
     * @param text 帶著換行與續行圖示的完整模板
     * @return 譯文；沒有對應條目時回傳 {@code null}
     */
    public String lookupUnwrapped(String text) {
        if (text == null || text.length() < MIN_FLAT_LENGTH) {
            return null;
        }
        return unwrapped.get(unwrap(text));
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

    /**
     * 在 {@code text} 裡從 {@code from} 起找第一個技能名稱。
     *
     * <h2>同一個起點取最長的</h2>
     * 詞表裡有不少名稱是另一個名稱的開頭：{@code Guardian}（守護者）跟
     * {@code Guardian Angels}（守護天使）、{@code Meteor}（隕石）跟
     * {@code Meteor Shower}（流星雨）、{@code Mask}（假面）跟
     * {@code Mask of the Lunatic}（赤狂假面）。
     *
     * <p>先前是比中哪個就回哪個，而字數是從少往多試的——於是<b>短的永遠贏</b>，
     * 畫面上是「守護者Angels」「隕石 Shower」「假面 of the Lunatic」。
     * 語料裡兩邊都翻好了，玩家看到的卻是半個英文名字，實測有 37 個名稱長這樣。
     *
     * <p>改成把這個起點能接的字都試完，留最長的那一個。這跟
     * {@code LineTranslator#appendText} 拿詞表跟重點段比的時候已經在用的
     * 「同一個位置取比較長的」是同一條規則。
     */
    public Term findTerm(String text, int from) {
        if (terms.isEmpty()) {
            return null;
        }
        for (int i = from; i < text.length(); i++) {
            if (!startsWord(text, i) || !Character.isUpperCase(text.charAt(i))) {
                continue;                      // 名稱一律是大寫開頭的專有名詞
            }
            int end = i;
            int bestEnd = -1;
            String best = null;
            for (int words = 0; words < maxTermWords && end <= text.length(); words++) {
                end = wordEnd(text, end);
                String hit = terms.get(text.substring(i, end));
                if (hit != null) {
                    best = hit;                // 不能就此回去，後面可能還有更長的
                    bestEnd = end;
                }
                if (end >= text.length() || text.charAt(end) != ' ') {
                    break;                     // 後面不是空格，接不下去了
                }
                end++;
            }
            if (best != null) {
                return new Term(i, bestEnd, best);
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
        return at == 0 || !wordChar(text.charAt(at - 1));
    }

    private static int wordEnd(String text, int from) {
        int i = from;
        while (i < text.length() && (wordChar(text.charAt(i))
                || text.charAt(i) == '\'' || text.charAt(i) == '-')) {
            i++;
        }
        return i;
    }

    /**
     * 這個字元算不算「同一個詞的一部分」。
     *
     * <h2>中日韓文字不算</h2>
     * Java 認為漢字是字母，所以「{@code 降低Meteor的魔力消耗}」裡的
     * {@code Meteor} 兩邊都被當成同一個詞的一部分：開頭那個 M 前面是「低」，
     * {@link #startsWord} 判定它不是詞首；就算過得了那一關，{@link #wordEnd}
     * 也會一路吃到「{@code Meteor的魔力消耗}」，跟詞表裡的 {@code Meteor}
     * 對不上。
     *
     * <p>結果是<b>詞表整個不觸發</b>——技能名在 {@code ability/*.json} 明明
     * 翻好了，敘述裡照樣顯示英文。實測語料裡有 77 條長這樣（薩滿 40、法師 26、
     * 弓手 11），差別只在譯者有沒有在名字前面留一個空格。
     *
     * <p>中文本來就不用空格分詞，緊貼著寫是<b>正常的中文</b>，不該被當成同一個
     * 詞。界線用 {@code 0x2E80}，跟 {@code LineTranslator#isLatin} 同一條。
     */
    private static boolean wordChar(char c) {
        return c < 0x2E80 && Character.isLetterOrDigit(c);
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
        return matchPrefix(partial, evidence, null);
    }

    /**
     * 同上，但先在<b>目前這個任務</b>裡找。
     *
     * <h2>為什麼要分任務</h2>
     * 卡住翻譯的不是長度門檻，是<b>撞句</b>：{@code Hey, {u}} 在全庫裡撞到幾十句，
     * {@code matchPrefix} 分不出是哪一句就只好回 {@code null}，於是英文一路跑到
     * 二十幾個字、開頭夠獨特了才忽然跳成中文。
     *
     * <p>但玩家正在做的任務是<b>知道的</b>——追蹤器上就寫著。同一個任務裡
     * {@code Hey, {u}} 通常只有一句，第一個字打出來就分得出來了。
     *
     * <p>範圍窄到一個任務之後長度門檻也不需要了：{@code MIN_PREFIX_LENGTH} 是
     * 為「全庫幾萬句」定的，一個任務只有一百多句，撞不到就是撞不到。
     * 只要求至少一個字，避免空字串命中第一條。
     *
     * @param quest 目前追蹤中的任務原名；{@code null} 就跟以前一樣只查全庫
     */
    public String matchPrefix(String partial, int evidence, String quest) {
        if (partial == null) {
            return null;
        }
        String key = partial.strip();
        if (key.isEmpty()) {
            return null;
        }
        if (quest != null && !quest.isBlank() && key.length() >= MIN_QUEST_PREFIX) {
            // 追蹤器送來的名稱可能帶著圖示（任務類型的小標誌），
            // 語料裡的 quest 欄位是乾淨的，不剔會永遠對不上。
            java.util.TreeMap<String, String> scoped = byQuest.get(
                    com.wynnchayuan.capture.GlyphSplitter.stripGlyphChars(quest).strip());
            if (scoped != null) {
                String hit = unique(scoped, key);
                if (hit != null) {
                    String better = curatedRival(hit, key);
                    return better != null ? better : hit;
                }
            }
        }
        if (evidence < MIN_PREFIX_LENGTH) {
            return null;
        }
        return unique(prefixIndex, key);
    }

    /**
     * 這個開頭在這份索引裡是不是<b>只</b>對得上一句。
     *
     * @return 那一句的完整原文；對不上或分不出是哪一句時回傳 {@code null}
     */
    private String unique(java.util.TreeMap<String, String> index, String key) {
        Map.Entry<String, String> first = index.ceilingEntry(key);
        if (first == null || !first.getKey().startsWith(key)) {
            String other = respell(key);        // 見 respell：英式拼法對不上語料
            if (other.equals(key)) {
                return null;
            }
            first = index.ceilingEntry(other);
            if (first == null || !first.getKey().startsWith(other)) {
                return null;
            }
            key = other;
        }
        String next = index.higherKey(first.getKey());
        if (next != null && next.startsWith(key)) {
            return settle(index, key, first.getKey());
        }
        return first.getKey();
    }

    /**
     * 候選不只一條，但它們可能只是<b>同一句話的新舊版本</b>。
     *
     * <h2>為什麼要分這個</h2>
     * 官方改過詞、wiki 還沒跟上時，同一句台詞在庫裡有兩條。逐字打字打到
     * 「Hey, {u}!」，前綴比對看到兩條就回 {@code null}——玩家眼前的英文於是
     * 一路跑到岔開的那個字才忽然跳成中文。但這裡根本不必猶豫：兩條講的是
     * 同一句話，人工校訂過的那條就是答案。
     *
     * <p>只在<b>全部</b>候選都是同一句話、而且校訂版<b>只有一條</b>時才敢答。
     * 少一個條件都可能貼上完全無關的台詞。
     *
     * @return 校訂版的原文；真的分不出來時回傳 {@code null}
     */
    private String settle(java.util.TreeMap<String, String> index, String key, String first) {
        String curated = null;
        int scanned = 0;
        for (Map.Entry<String, String> e : index.tailMap(key).entrySet()) {
            if (!e.getKey().startsWith(key)) {
                break;
            }
            if (++scanned > RIVAL_SCAN || !sameLine(first, e.getKey())) {
                return null;                   // 真的是不同的句子，分不出來
            }
            if (!fromWiki.contains(e.getKey())) {
                if (curated != null) {
                    return null;               // 兩條都是校訂版，也分不出來
                }
                curated = e.getKey();
            }
        }
        return curated;
    }

    /**
     * 開頭要多長才敢猜。太短的話「I」「Well,」會撞到幾百句。
     */
    private static final int MIN_PREFIX_LENGTH = 12;

    /**
     * 限定任務之後的門檻。
     *
     * <h2>為什麼還是要留一道</h2>
     * 追蹤中的任務<b>不保證</b>就是這段對話所屬的任務——玩家可能追著 A
     * 卻順手跟 B 的 NPC 講話（見 {@code CurrentQuest} 的「限制」）。
     * 一兩個字就敢猜的話，那種時候會很有把握地貼上<b>完全無關</b>的譯文。
     *
     * <p>四個字幾乎不花成本：實測 King's Recruit 全部 102 句台詞，
     * 限定任務後平均第 4 個字就分得出來（全庫要 16 個字）。
     */
    private static final int MIN_QUEST_PREFIX = 4;

    /**
     * 同一句話的<b>校訂版</b>；沒有就回傳 {@code null}。
     *
     * <h2>為什麼需要</h2>
     * wiki 抄來的台詞會跟遊戲裡實際跑的字<b>不一樣</b>——官方改過詞、wiki 還沒跟上。
     * King's Recruit 第一句實測就是這樣：
     *
     * <pre>
     *   wiki   Hey, {u}! You alright in there? Looks like we hit something.
     *   實機   Hey, {u}! Are you alright in there? It looks like we've hit something.
     * </pre>
     *
     * <p>兩條都在庫裡（校訂版放在 quest.json，是人工對著遊戲補的）。
     * NPC 逐字打字時，打到「Hey, {u}」限定任務的索引<b>只看得到 wiki 那條</b>
     * ——校訂版沒有 quest 欄位——於是先貼上「嘿，」；再多打幾個字岔開了，
     * 全庫索引接手換成校訂版的「喂，」。玩家看到的就是講到一半中文自己換了一個字。
     *
     * <p>所以限定任務命中 wiki 條目時，先問一句：全庫裡有沒有一條<b>同一句話</b>
     * 但不是 wiki 來的。有的話用那條——從第一幀就是校訂版，中途不會再換。
     *
     * <h2>怎麼算「同一句話」</h2>
     * 共同前綴不管用：上面兩條在 {@code Hey, {u}! } 之後立刻岔開。
     * 改看<b>用字的重疊比例</b>（Jaccard）：改詞頂多動幾個字，重疊會很高；
     * 剛好同開頭的另一句台詞則低得多。
     */
    private String curatedRival(String wikiHit, String key) {
        if (!fromWiki.contains(wikiHit)) {
            return null;                       // 命中的本來就是校訂版，不必再找
        }
        int scanned = 0;
        for (Map.Entry<String, String> e : prefixIndex.tailMap(key).entrySet()) {
            if (!e.getKey().startsWith(key) || ++scanned > RIVAL_SCAN) {
                break;
            }
            if (fromWiki.contains(e.getKey()) || e.getKey().equals(wikiHit)) {
                continue;
            }
            if (sameLine(wikiHit, e.getKey())) {
                return e.getKey();
            }
        }
        return null;
    }

    /** 兩條原文是不是同一句話的兩個版本。見 {@link #curatedRival}。 */
    static boolean sameLine(String a, String b) {
        java.util.Set<String> left = words(a);
        java.util.Set<String> right = words(b);
        if (left.isEmpty() || right.isEmpty()) {
            return false;
        }
        int both = 0;
        for (String w : left) {
            if (right.contains(w)) {
                both++;
            }
        }
        int union = left.size() + right.size() - both;
        return both * 100 >= union * SAME_LINE_PERCENT;
    }

    /** 只留字母與數字，大小寫不分。撇號、逗號那些正是改詞時會動的東西。 */
    private static java.util.Set<String> words(String text) {
        java.util.Set<String> out = new java.util.HashSet<>();
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = Character.toLowerCase(text.charAt(i));
            if (Character.isLetterOrDigit(c)) {
                word.append(c);
            } else if (word.length() > 0) {
                out.add(word.toString());
                word.setLength(0);
            }
        }
        if (word.length() > 0) {
            out.add(word.toString());
        }
        return out;
    }

    /**
     * 用字重疊到幾成才算同一句。
     *
     * <h2>六成五怎麼來的</h2>
     * 兩邊都是實測量出來的，取中間：
     *
     * <ul>
     *   <li><b>七成一</b>——官方改詞的那兩條
     *       （{@code Are you alright} 對 {@code You alright}），必須算同一句</li>
     *   <li><b>六成</b>——{@code Enter the castle at [...]} 對
     *       {@code Enter the vault at [...]}，是兩個不同任務的不同目標，
     *       不能算同一句。門檻原本就是六成，剛好把它收進來，
     *       畫面上「進入 [-」會閃一下變成「進入 [1234」</li>
     *   <li><b>兩成</b>——同樣以「Hey, {u}」開頭的另一句台詞</li>
     * </ul>
     *
     * <p>逐字模擬全部語料（{@code tools/typing-audit.py}）：六成有一句會改口，
     * 六成三以上都是零。取六成五，兩邊各留一點餘裕。
     */
    private static final int SAME_LINE_PERCENT = 65;

    /** 最多往後看幾條。同一個開頭的候選本來就沒幾條，掃太多只是白花時間。 */
    private static final int RIVAL_SCAN = 16;

    /**
     * 從 wiki 抄下來的條目。
     *
     * <p>不是「品質比較差」的意思——絕大多數 wiki 條目都是對的。
     * 只有在<b>同一句話有兩個版本</b>時才拿它來分高下：人工校訂過的那條
     * 是對著遊戲畫面打的，一定比較新。見 {@link #curatedRival}。
     */
    private final java.util.Set<String> fromWiki =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    /**
     * 語料裡還有<b>更長</b>的原文以這一段開頭嗎。
     *
     * <h2>為什麼呼叫端需要問這個</h2>
     * NPC 是一個字一個字打出來的，而打到一半的那半句<b>本身</b>常常剛好就是
     * 語料裡的另一條。逐字模擬全部 5749 句台詞抓出來的例子：
     *
     * <pre>
     *   打到「Block」      → ability-labels 的「格擋」
     *   打到「Bring」      → quest-ui 的「攜帶」
     *   打到「...」        → 「……」
     * </pre>
     *
     * 一兩幀之後整句打完，畫面上那幾個字就被換掉——玩家看到的是中文閃一下
     * 變成別的字。而這種情形是<b>問得出來</b>的：後面還有更長的候選，就代表
     * 現在定案有機會等一下要改口。
     *
     * <p>反過來說，沒有更長的候選時就沒有這個風險，可以立刻定案——
     * 「Agh!」那種短短一句的台詞不會因此被拖慢。
     */
    public boolean hasLonger(String key) {
        if (key == null) {
            return false;
        }
        String bare = key.strip();
        if (bare.isEmpty()) {
            return false;
        }
        String next = ordered.higher(bare);
        return next != null && next.startsWith(bare);
    }

    /** 每一條原文，排序後放著，只為了 {@link #hasLonger}。 */
    private final java.util.concurrent.ConcurrentSkipListSet<String> ordered =
            new java.util.concurrent.ConcurrentSkipListSet<>();

    /** 依原文排序，才能用 ceiling/higher 做前綴比對。見 {@link #lookupPrefix}。 */
    private final java.util.TreeMap<String, String> prefixIndex = new java.util.TreeMap<>();

    /**
     * 一個任務一份前綴索引。
     *
     * <p>沒有長度門檻——範圍已經窄到一個任務，短前綴不再危險。
     */
    private final Map<String, java.util.TreeMap<String, String>> byQuest =
            new java.util.HashMap<>();

    /**
     * 這個字是<b>還沒翻的裝備名</b>嗎。
     *
     * <h2>為什麼不在 lookup 裡直接擋</h2>
     * 第一版是在 {@link #lookup} 裡擋的，結果把介面標籤一起擋死了：有一把裝備
     * 叫 {@code Reflection}，於是屬性列的「遠程反傷」也翻不出來——同一份 tooltip
     * 裡其他標籤都是中文，只有那一行是英文。
     *
     * <p>{@code Guardian} 與 {@code Reflection} 在<b>鍵</b>上分不出來：兩個都是
     * 「沒有自己譯文的裝備名，而別的檔案剛好有同名條目」。分得出來的是<b>位置</b>
     * ——一個出現在物品名稱那一行，另一個出現在敘述裡的屬性標籤。
     *
     * <p>所以判斷交給呼叫端：只有畫物品名稱的地方才問這個問題。
     * 見 {@code TooltipPanel#translateLines}。
     */
    public boolean isBareGearName(String key) {
        return key != null && gearNameKeys.contains(key.strip());
    }

    public String lookup(String template) {
        if (template == null) {
            return null;
        }
        String key = template.strip();
        if (!translateNames && nameKeys.contains(key)) {
            return null;                       // 使用者選擇不翻物品名稱
        }

        String hit = entries.get(key);
        if (hit == null) {
            hit = lookupIndented(key);
        }
        if (hit == null) {
            String other = respell(key);
            if (!other.equals(key)) {
                hit = entries.get(other);
                if (hit == null) {
                    hit = lookupIndented(other);
                }
            }
        }
        if (hit == null) {
            hit = lookupMarked(key);
        }
        return hit;
    }

    /** 符合／不符合的那兩個記號。見 {@link #lookupMarked}。 */
    private static final char MARK_YES = '✔';

    private static final char MARK_NO = '✖';

    /** 記號全部換成同一個之後的索引。見 {@link #lookupMarked}。 */
    private final Map<String, String> marked = new ConcurrentHashMap<>();

    private static boolean hasMark(String text) {
        return text.indexOf(MARK_YES) >= 0 || text.indexOf(MARK_NO) >= 0;
    }

    private static String sameMarks(String text) {
        return text.replace(MARK_YES, MARK_NO);
    }

    private void noteMarked(String src, String dst) {
        if (hasMark(src) && countMarks(src) == countMarks(dst)) {
            marked.putIfAbsent(sameMarks(src), dst);
        }
    }

    private static int countMarks(String text) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == MARK_YES || c == MARK_NO) {
                n++;
            }
        }
        return n;
    }

    /**
     * {@code ✔} 與 {@code ✖} 只差<b>符合或不符合</b>，句子是同一句。
     *
     * <h2>為什麼要有這一層</h2>
     * 採集點的名牌與裝備的需求列，每一行前面都掛一個記號：
     *
     * <pre>
     *   Blossom              Blossom
     *   ✖ Ⓒ Woodcutting…     ✔ Ⓒ Woodcutting…
     *   ✖ Equipped Tool…     ✖ Equipped Tool…
     * </pre>
     *
     * 兩者在語料裡是<b>兩筆</b>，而記號取決於玩家<b>當下的等級與手上的工具</b>——
     * 那是狀態，不是內容。於是同一句話要翻兩次到四次，而實機上永遠有一半沒翻：
     * 等級夠了的那些採集點、職業對得上的那些裝備需求，整行掉回英文
     * （{@code ✖ Ability Points:} 與 {@code ✔ Class Req:} 每開一次背包就中一次）。
     *
     * <p>所以查表時把記號<b>全部換成同一個</b>再查，查到之後再依序把呼叫端
     * 自己那幾個記號填回譯文——畫面上顯示的還是遊戲當下的狀態。
     *
     * <p>只在記號數量兩邊相同時才收（見 {@link #noteMarked}），不然無從對應。
     * 實測全庫 144 條帶記號的鍵收斂成 74 組，<b>沒有任何一組</b>的譯文在
     * 換掉記號之後不一致——也就是說這一層只會多命中，不會改變任何既有結果。
     */
    private String lookupMarked(String key) {
        if (!hasMark(key)) {
            return null;
        }
        String hit = marked.get(sameMarks(key));
        if (hit == null || countMarks(hit) != countMarks(key)) {
            return null;
        }
        StringBuilder out = new StringBuilder(hit);
        int at = 0;
        for (int i = 0; i < out.length(); i++) {
            char c = out.charAt(i);
            if (c == MARK_YES || c == MARK_NO) {
                out.setCharAt(i, markAt(key, at++));
            }
        }
        return out.toString();
    }

    /** 原文裡第 {@code n} 個記號。 */
    private static char markAt(String text, int n) {
        int seen = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == MARK_YES || c == MARK_NO) && seen++ == n) {
                return c;
            }
        }
        return MARK_NO;
    }

    /**
     * 語料裡到底有沒有這一條的譯文——<b>不管</b>使用者有沒有把
     * 「翻譯物品名稱」關掉。
     *
     * <h2>為什麼不能直接用 lookup</h2>
     * 收集端拿這個判斷「這是不是還沒翻的缺口」（見
     * {@code CaptureStore#knowsTranslations}）。{@link #lookup} 在使用者關掉
     * 物品名稱時對名稱回傳 {@code null}——那是「不要顯示」，不是「沒有譯文」。
     * 拿它來判斷缺口的話，每一件裝備的名稱都會被當成沒翻，
     * {@code captured.json} 又會塞滿雜訊。
     */
    public boolean hasTranslation(String template) {
        if (template == null) {
            return false;
        }
        String key = template.strip();
        if (entries.containsKey(key) || lookupIndented(key) != null) {
            return true;
        }
        if (lookupMarked(key) != null) {
            return true;                       // 換個記號就查得到，不是缺口
        }
        // 屬性列不是整行收的，是「標籤 + 數值」拆開查的（見 LineTranslator#statRow）。
        //
        // 這裡漏掉那條路的話，畫面上明明是中文的「移動速度 +5%」還是會被記成缺口，
        // 而翻譯團隊照著 captured.json 補，補出來的就是<b>整行</b>條目——那正是
        // 先前 misc.json 裡那兩百多條、把標籤那條路整個蓋掉的東西。這一份
        // capture 的 186 條裡就有五十幾條是這種假缺口。
        //
        // 百分比與實數兩種都問：同一個標籤兩種譯名（法術傷害百分比／法術傷害值），
        // 只問一種會把另一種記成缺口。
        if (LineTranslator.lookup(key, this, false) != null
                || LineTranslator.lookup(key, this, true) != null) {
            return true;
        }
        String other = respell(key);
        return !other.equals(key)
                && (entries.containsKey(other) || lookupIndented(other) != null);
    }

    /**
     * 英式拼法換成語料裡用的美式拼法。
     *
     * <p>語料是從維基抓來的，寫的是 {@code honors}；遊戲裡顯示的是 {@code honours}。
     * 差一個字母，整句就查不到——而且是<b>整片</b>查不到：語料裡有八百多條
     * 帶著這類拼法的句子，畫面上一條都對不上，看起來就是「這幾句沒人翻」。
     *
     * <p>只在查不到時才走這條，所以就算某個詞猜錯了也不會蓋掉本來查得到的。
     */
    static String respell(String text) {
        String out = text;
        for (String[] pair : SPELLING) {
            out = out.replace(pair[0], pair[1]);
            out = out.replace(upper(pair[0]), upper(pair[1]));
        }
        return out;
    }

    private static String upper(String word) {
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    /** 英式 → 美式。只收字幹，加了字尾（-s、-ed、-ing）會自己跟著換。 */
    private static final String[][] SPELLING = {
        {"honour", "honor"}, {"colour", "color"}, {"armour", "armor"},
        {"favour", "favor"}, {"neighbour", "neighbor"}, {"rumour", "rumor"},
        {"behaviour", "behavior"}, {"harbour", "harbor"}, {"labour", "labor"},
        {"flavour", "flavor"}, {"humour", "humor"}, {"odour", "odor"},
        {"valour", "valor"}, {"vapour", "vapor"}, {"saviour", "savior"},
        {"endeavour", "endeavor"}, {"centre", "center"}, {"theatre", "theater"},
        {"fibre", "fiber"}, {"defence", "defense"}, {"offence", "offense"},
        {"travelled", "traveled"}, {"traveller", "traveler"},
        {"cancelled", "canceled"}, {"marvellous", "marvelous"},
        {"jewellery", "jewelry"}, {"realise", "realize"},
        {"apologise", "apologize"}, {"recognise", "recognize"},
        {"organise", "organize"},
    };

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
