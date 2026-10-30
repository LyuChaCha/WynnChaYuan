package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

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

    // 整份在旁邊建好再一次換上：重載時，背景的寫檔與匯出執行緒還在查。
    private volatile TranslationIndex index = new TranslationIndex(this);

    /**
     * 讀取一個目錄下的所有 .json。
     *
     * <p>目錄不存在就<b>自動建立</b>並講清楚——先前這裡是直接 return，
     * 結果資料夾被刪掉時完全沒有任何提示，只會看到「翻譯沒反應」而查不出原因。
     *
     * <p>檔案格式錯誤只會被略過，不中斷遊戲。
     */
    public void loadAll(Path dir) {
        loadAll(List.of(dir));
    }

    /**
     * 依序載入好幾層，後面的蓋掉前面的。
     *
     * <h2>為什麼要有這個多層版本</h2>
     * 玩家在 F6 指名輔助語言時，先鋪那一層當底，再把選定的語言疊上去。
     *
     * <p>先前的寫法是<b>連呼叫兩次</b> {@link #loadAll(Path)}：
     *
     * <pre>
     *   translations.loadAll(fallback);   // 繁體
     *   translations.loadAll(trDir);      // 簡體
     * </pre>
     *
     * 但 {@code loadAll} 開頭就 {@code entries.clear()}，第二次把第一次載入的
     * 整片清掉了。實測疊完只剩 449 條（簡體自己那些），繁體那 31,137 條一條
     * 都沒留下——也就是說墊底這件事<b>從來沒有生效過</b>，而畫面上看到的是
     * 英文，正好是那段程式碼寫來要避免的結果。
     *
     * <p>改成一次把所有層交進來，照順序讀。「疊」這件事現在是這個方法的職責，
     * 呼叫端想弄錯也弄不錯。
     */
    public synchronized void loadAll(List<Path> dirs) {
        TranslationIndex next = new TranslationIndex(this);
        next.load(dirs);
        index = next;
        generation++;
    }

    /**
     * 載入的<b>第幾版</b>；每重載一次就往前一格。
     *
     * <h2>為什麼不能拿 store 物件本身當識別</h2>
     * 換語言走的是 {@code WynnChaYuan#loadLayers}，它把新語言的譯文<b>載進同一個
     * store</b>，不換物件。繪製端那幾張快取先前用「store 是不是換人了」判斷該不該
     * 清，於是切完語言，NPC 名牌與浮空字還是舊語言的——要 {@code /class} 讓實體
     * 重新生成一次才會變。
     */
    public int generation() {
        return generation;
    }

    private volatile int generation;

    /**
     * 這一刻要不要把 {@link TranslationIndex#plainNameKeys} 當成沒翻。
     *
     * <p>只有按住 Shift 畫物品 tooltip 的那一小段會是 {@code true}，畫完就還原
     * （見 {@code RenderListener#withPeek}）。跟 {@link #translateNames} 一樣做成
     * 旗標而不是直接問鍵盤：查詢表不該伸手抓遊戲狀態。
     */
    private volatile boolean peekPlainNames = false;

    public void setPeekPlainNames(boolean value) {
        this.peekPlainNames = value;
    }

    /** 見 {@link #setPeekPlainNames}：給算繪端的快取做有效性判斷用。 */
    public boolean peeksPlainNames() {
        return peekPlainNames;
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

    /** 見 {@link #setTranslateNames}：給算繪端的快取做有效性判斷用。 */
    public boolean translatesNames() {
        return translateNames;
    }

    /**
     * 物品名稱照 F6 設定顯示。{@code OFF} 保留英文、{@code ON} 只給譯名、
     * {@code BOTH} 給「譯名 (原文)」。
     */
    public void setNameMode(com.wynnchayuan.CollectorConfig.ItemNames mode) {
        this.translateNames = mode != com.wynnchayuan.CollectorConfig.ItemNames.OFF;
        this.namesWithOriginal = mode == com.wynnchayuan.CollectorConfig.ItemNames.BOTH;
    }

    /**
     * 內容名稱（任務、洞窟、探索點…）照 F6 設定顯示。見 {@link PageNames}。
     *
     * <p>跟物品名稱分開一個設定：那一個預設關著（裝備名留原文才對得上交易市場），
     * 這一個預設是譯名——內容書的標題一直都是翻的。
     */
    public void setPageNameMode(com.wynnchayuan.CollectorConfig.ItemNames mode) {
        com.wynnchayuan.CollectorConfig.ItemNames next =
                mode == null ? com.wynnchayuan.CollectorConfig.ItemNames.ON : mode;
        if (next != pageNameMode) {
            // Lootrun 使命名是在查表時改的（LootrunNames），繪製端的快取要跟著作廢
            generation++;
        }
        this.pageNameMode = next;
    }

    /** 見 {@link #setPageNameMode}。 */
    public com.wynnchayuan.CollectorConfig.ItemNames pageNameMode() {
        return pageNameMode;
    }

    private volatile com.wynnchayuan.CollectorConfig.ItemNames pageNameMode =
            com.wynnchayuan.CollectorConfig.ItemNames.ON;

    /** 見 {@link #setNameMode}：譯名後面要不要括號附原文。 */
    private volatile boolean namesWithOriginal = false;

    /** 見 {@link #setAbilityNamesWithOriginal}。 */
    private volatile boolean abilityNamesWithOriginal = false;

    /**
     * 技能名稱（技能樹的節點標題、「解鎖後將封鎖」底下那幾列）與 Major ID 名稱
     *（裝備說明的「Altruism:」）後面要不要括號附原文——F6 那一格，跟物品名稱的
     * 「譯名加原文」同一個畫法。
     *
     * <p>整格查到技能檔自己的譯名時（{@link TranslationIndex#ownAbilityName}）附在後面；敘述裡被詞表
     * 換掉的技能名（{@link #findTerm}、{@link #lookupTerm}）也一樣附，「日耀 (Sunflare)
     * 恢復的…」。附上去的原文由 {@link #abilityOriginalSpans} 擋著不再被換。
     * 譯名跟原文一樣（還沒翻、或刻意留英文）時不附。
     *
     * <p>切換時作廢繪製端的快取，跟 {@link #setPageNameMode} 一樣。
     */
    public void setAbilityNamesWithOriginal(boolean on) {
        if (on != abilityNamesWithOriginal) {
            generation++;
        }
        abilityNamesWithOriginal = on;
    }

    /** 見 {@link #setAbilityNamesWithOriginal}。 */
    public boolean abilityNamesWithOriginal() {
        return abilityNamesWithOriginal;
    }

    /** F6 選的是「譯名 + 原文」嗎。見 {@link #appendedOriginalSpan}。 */
    public boolean namesWithOriginal() {
        return namesWithOriginal;
    }

    public MarketSearch market() {
        return index.market();
    }

    public boolean isPlainName(String key) {
        return index.isPlainName(key);
    }

    public List<Path> brokenFiles() {
        return index.brokenFiles();
    }

    public String chatSpeaker(String speaker) {
        return index.chatSpeaker(speaker);
    }

    public com.wynnchayuan.capture.NameFrames nameFrames() {
        return index.nameFrames();
    }

    public String labelLookup(String template) {
        return index.labelLookup(template);
    }

    public String scopedLookup(String scope, String template) {
        return index.scopedLookup(scope, template);
    }

    public String lastResult() {
        return index.lastResult();
    }

    public boolean isUiLabel(String key) {
        return index.isUiLabel(key);
    }

    public String completeUiLabel(String prefix) {
        return index.completeUiLabel(prefix);
    }

    java.util.Set<String> keys() {
        return index.keys();
    }

    public Guess playerNameIn(String raw) {
        return index.playerNameIn(raw);
    }

    public String lookupUnwrapped(String text) {
        return index.lookupUnwrapped(text);
    }

    public String lookupFlat(String text) {
        return index.lookupFlat(text);
    }

    public Term findTerm(String text, int from) {
        return index.findTerm(text, from);
    }

    public String lookupTerm(String text) {
        return index.lookupTerm(text);
    }

    java.util.Set<String> termNames() {
        return index.termNames();
    }

    public boolean isPageName(String text) {
        return index.isPageName(text);
    }

    public String pageTitleName(String text) {
        return index.pageTitleName(text);
    }

    public String speakerOf(String template) {
        return index.speakerOf(template);
    }

    public Boolean inCast(String quest, String speaker) {
        return index.inCast(quest, speaker);
    }

    public String questOf(String template) {
        return index.questOf(template);
    }

    public String lookupPrefix(String partial) {
        return index.lookupPrefix(partial);
    }

    public String speakerOfPrefix(String partial) {
        return index.speakerOfPrefix(partial);
    }

    public String matchPrefix(String partial) {
        return index.matchPrefix(partial);
    }

    public String matchPrefix(String partial, int evidence) {
        return index.matchPrefix(partial, evidence);
    }

    public String matchPrefix(String partial, int evidence, String quest) {
        return index.matchPrefix(partial, evidence, quest);
    }

    public String nearQuestLine(String typed, String quest) {
        return index.nearQuestLine(typed, quest);
    }

    public String sequenceQuestOf(String src, String... prefer) {
        return index.sequenceQuestOf(src, prefer);
    }

    public int sequenceIndex(String quest, String src, int after) {
        return index.sequenceIndex(quest, src, after);
    }

    public String forecast(String typed, String quest, int after, int window, String speaker, int floor,
                           java.util.function.BiPredicate<String, String> fits) {
        return index.forecast(typed, quest, after, window, speaker, floor, fits);
    }

    public String forecastBySpeaker(String typed, String quest, String speaker, int floor,
                                    java.util.function.BiPredicate<String, String> fits) {
        return index.forecastBySpeaker(typed, quest, speaker, floor, fits);
    }

    public String forecastNext(String typed, String quest, int after, int window, String speaker, int floor,
                               java.util.function.BiPredicate<String, String> fits) {
        return index.forecastNext(typed, quest, after, window, speaker, floor, fits);
    }

    public String explainForecast(String typed, String quest, int after, int window, String speaker,
                                  java.util.function.BiPredicate<String, String> fits) {
        return index.explainForecast(typed, quest, after, window, speaker, fits);
    }

    public String explainQuest(String typed, String quest) {
        return index.explainQuest(typed, quest);
    }

    public boolean hasLonger(String key) {
        return index.hasLonger(key);
    }

    public boolean couldStillMatch(String typed) {
        return index.couldStillMatch(typed);
    }

    public boolean isBareGearName(String key) {
        return index.isBareGearName(key);
    }

    public boolean itemNameOnly(String key) {
        return index.itemNameOnly(key);
    }

    public boolean gearNameOnly(String key) {
        return index.gearNameOnly(key);
    }

    public boolean isItemName(String key) {
        return index.isItemName(key);
    }

    public String lookupExact(String key) {
        return index.lookupExact(key);
    }

    public String gearOwnName(String key) {
        return index.gearOwnName(key);
    }

    String lootrunName(String name) {
        return index.lootrunName(name);
    }

    public String lookup(String template) {
        return index.lookup(template);
    }

    public int appendedOriginalAt(String text) {
        return index.appendedOriginalAt(text);
    }

    public List<int[]> keptOriginals(String text) {
        return index.keptOriginals(text);
    }

    List<int[]> abilityOriginalSpans(String text) {
        return index.abilityOriginalSpans(text);
    }

    public int[] appendedOriginalSpan(String text) {
        return index.appendedOriginalSpan(text);
    }

    public java.util.Set<String> sourceKeys() {
        return index.sourceKeys();
    }

    public boolean hasTranslation(String template) {
        return index.hasTranslation(template);
    }

    public int maxBlockLines() {
        return index.maxBlockLines();
    }

    public int size() {
        return index.size();
    }

    public int loadedFiles() {
        return index.loadedFiles();
    }

    /**
     * 採集工具的名稱（{@code Dernic Pickaxe T{~}}），含名稱那一列的外框。
     *
     * <h2>為什麼不放進 {@link TranslationIndex#plainNameKeys}</h2>
     * 工具的名稱帶著等級的數字，模板裡是 {@code T{~}}。名稱列的那條路
     * （{@link #nameRowInner}）不收帶佔位符的名字，所以工具的名稱列在語料裡是
     * <b>整列</b>的條目（{@code {#}{#}{#}{#}{#}Dernic Pickaxe T{~}}），跟素材那種
     * 「一個名字、各種外框共用」不一樣，按住 Shift 也就輪不到它
     * （使用者 2026-10-10：「工具 shift 變回原文也可以裝」）。
     *
     * <p>認形狀而不是列名單：材質是一個首字大寫的詞，工具是四種之一，後面接
     * {@code T{~}}，前後只能是圖示。句子裡提到工具的（「再 {~} 級就能使用…」
     * 「已完成購買…」）不是這個形狀，按住 Shift 照舊是譯文。
     */
    private static final java.util.regex.Pattern TOOL_NAME = java.util.regex.Pattern.compile(
            "^(?:\\{#})*[A-Z][a-z]+ (?:Pickaxe|Axe|Scythe|Rod) T\\{~}(?:\\{#})*$");

    /** 見 {@link #TOOL_NAME}。 */
    static boolean isToolName(String key) {
        return key != null && TOOL_NAME.matcher(key).matches();
    }

    /** 聊天形式的台詞鍵：計數、說話者、台詞。 */
    static final java.util.regex.Pattern CHAT_LINE = java.util.regex.Pattern.compile(
            "\\[\\{~}/\\{~}] (?<speaker>[^:\\n\\[\\]]+?): (?<line>[^\\n]+)");

    /**
     * 一次猜測：夾出來的名字，以及夾出它的<b>是哪一條鍵</b>。
     *
     * <p>來源要記著，因為單獨一條鍵不足以定案——見
     * {@link com.wynnchayuan.capture.SelfNames#propose}。
     */
    public record Guess(String name, String source) {}

    /** 把所有連續空白（含換行）壓成一個空格。 */
    public static String normalise(String text) {
        return text == null ? "" : text.strip().replaceAll("\\s+", " ");
    }

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

    /** 找到的名稱在原文的哪一段，以及它的譯名。 */
    public record Term(int start, int end, String translation) {}

    /**
     * 這個字元算不算「同一個詞的一部分」。
     *
     * <h2>中日韓文字不算</h2>
     * Java 認為漢字是字母，所以「{@code 降低Meteor的魔力消耗}」裡的
     * {@code Meteor} 兩邊都被當成同一個詞的一部分：開頭那個 M 前面是「低」，
     * {@link TranslationIndex#startsWord} 判定它不是詞首；就算過得了那一關，{@link TranslationIndex#wordEnd}
     * 也會一路吃到「{@code Meteor的魔力消耗}」，跟詞表裡的 {@code Meteor}
     * 對不上。
     *
     * <p>結果是<b>詞表整個不觸發</b>——技能名在 {@code ability/*.json} 明明
     * 翻好了，敘述裡照樣顯示英文。實測語料裡有 77 條長這樣（薩滿 40、法師 26、
     * 弓手 11），差別只在譯者有沒有在名字前面留一個空格。
     *
     * <p>中文本來就不用空格分詞，緊貼著寫是<b>正常的中文</b>，不該被當成同一個
     * 詞。界線用 {@code 0x2E80}，跟 {@code LineTranslator#isLatin} 同一條。
     *
     * <p>{@link LineTranslator} 把重點段的樣式貼回譯文時用<b>同一條</b>界線。
     * 「算不算同一個詞」在一份語料裡只能有一種答案，兩邊各自判會在
     * {@code Lootrun} 那種詞上分岔：詞表不把 {@code Loot} 當詞首，
     * 重點段卻照樣貼上去，於是 {@code run} 三個字母掉色。
     */
    static boolean wordChar(char c) {
        return c < 0x2E80 && Character.isLetterOrDigit(c);
    }

    /** 測試用：第一層要不要把 quest.json 的未歸屬台詞算成競爭者。實機永遠是 true。 */
    public static volatile boolean unplacedRivals = true;

    /**
     * 畫面上的字跟 {@code src} 岔開的地方正好是一個佔位符，而岔出去的那一截
     * <b>可能正是那個佔位符打到一半</b>（見 {@link #couldBe}）。
     *
     * <p>就地取代沿用上一幀時也用這一條（{@code DialogueRewriter#within}）：先前那邊要求
     * 岔開之前至少對上 12 個字，預測在第一個字就認出「Hey, {u}! …」之後，打到名字的
     * 那幾幀反而對不上、掉回英文再跳回中文。
     */
    public static boolean slotFits(String src, String typed) {
        int room = Math.min(src.length(), typed.length());
        int agreed = 0;
        while (agreed < room && src.charAt(agreed) == typed.charAt(agreed)) {
            agreed++;
        }
        if (agreed >= src.length() || src.charAt(agreed) != '{'
                || typed.length() > agreed + FORECAST_NAME_ROOM) {
            return false;
        }
        int close = src.indexOf('}', agreed);
        String slot = close < 0 ? src.substring(agreed) : src.substring(agreed, close + 1);
        return couldBe(slot, typed.substring(agreed));
    }

    /**
     * 畫面上這一截，可能是佔位符 {@code slot} 還沒打完的樣子嗎。
     *
     * <p>不分青紅皂白都算「可能」的話，任務裡只要有一句以玩家名開頭（「{u}, why don't
     * you…」），前 24 個字就永遠分不出是哪一句。名字只會是玩家自己的名字、地名只會是
     * 地名清單裡的、數字只會是數字——不像的就不是。認不得的佔位符一律算「可能」。
     */
    private static boolean couldBe(String slot, String partial) {
        if (partial.isEmpty()) {
            return true;
        }
        if (slot.equals("{u}")) {
            // Minecraft 的名字只有英數與底線、最多 16 個字：有空白就不是名字打到一半
            if (partial.length() > 16) {
                return false;
            }
            for (int i = 0; i < partial.length(); i++) {
                char c = partial.charAt(i);
                if (!(c < 128 && (Character.isLetterOrDigit(c) || c == '_'))) {
                    return false;
                }
            }
            List<String> names = com.wynnchayuan.capture.SelfNames.all();
            if (names.isEmpty()) {
                return true;                   // 還不知道玩家叫什麼：像名字的都算可能
            }
            String low = partial.toLowerCase(Locale.ROOT);
            for (String name : names) {
                if (name.toLowerCase(Locale.ROOT).startsWith(low)) {
                    return true;
                }
            }
            return false;
        }
        if (slot.equals("{p}")) {
            return com.wynnchayuan.capture.PlaceNames.anyStartsWith(partial);
        }
        if (slot.equals("{#}")) {
            // 句中圖示：畫面上是私用區的圖示字元（收成模板時才變成 {#}），不會是字母
            return GlyphSplitter.isGlyphCodePoint(partial.codePointAt(0));
        }
        if (slot.startsWith("{~")) {
            for (int i = 0; i < partial.length(); i++) {
                if ("0123456789,.-+".indexOf(partial.charAt(i)) < 0) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    /** 一個還沒收成佔位符的名字最多多長。跟 {@code DialogueRewriter.NAME_ROOM} 一樣。 */
    private static final int FORECAST_NAME_ROOM = 24;

    /** 兩條原文是不是同一句話的兩個版本。見 {@link TranslationIndex#curatedRival}。 */
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
    static java.util.Set<String> words(String text) {
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

    /**
     * 現在畫的是物品說明的<b>名稱那兩行</b>。見 {@link #gearOwnName}。
     *
     * <p>預設是關的：漏掛只是那幾件裝備照舊跟著技能叫，掛錯才會讓技能樹上的
     * 「得寸進尺」變成一把矛。所以只有 {@code TooltipPanel} 在認出「這是物品的
     * 名稱列」之後才掛，畫完就放下。
     */
    public static void nameLine(boolean on) {
        NAME_LINE.set(on);
    }

    /** 見 {@link #nameLine}。每條執行緒各一份，理由見 {@link #BAR_ITEMS}。 */
    static final ThreadLocal<Boolean> NAME_LINE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * 現在畫的是 Lootrun 試煉卡／使命卡的<b>標題</b>。
     *
     * <h2>實機回報（使用者 2026-10-22，1.0.3_2）</h2>
     * 新版卡片的標題是裸名（{@code Hubris}，沒有開頭圖示；使命卡在 cards.json 裡也是
     * {@code "card": "High Roller"}）。語料替使命名收的是 {@code {#}Hubris} 與
     * {@code Hubris:}——裸名不能收，因為六個名字跟裝備撞名（validate：同一個原文只能
     * 一種譯法）。於是裸名只查得到那件裝備：試煉卡被名稱列的守門留成英文，
     * 使命卡則畫成「平衡之盔 (Equilibrium)」。
     *
     * <p>掛著這個旗標時，整格就是使命名的查表改答使命自己的譯名（見 {@link #lootrunName}），
     * 再照「內容名稱」的設定附原文。只有 {@code TooltipPanel} 在認出 Lootrun 卡片之後、
     * 畫第 0 列時才掛，畫完就放下；預設關著，掛漏只是回到舊行為。
     */
    public static void lootrunTitle(boolean on) {
        LOOTRUN_TITLE.set(on);
    }

    /** 見 {@link #lootrunTitle}。每條執行緒各一份，理由見 {@link #BAR_ITEMS}。 */
    static final ThreadLocal<Boolean> LOOTRUN_TITLE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    static String originalOf(String key) {
        if (key.indexOf('{') < 0) {
            return key;                        // 絕大多數的名字
        }
        if (key.contains(GlyphSplitter.PLACE_PLACEHOLDER)
                || key.contains(GlyphSplitter.PLAYER_PLACEHOLDER)) {
            return null;
        }
        String bare = bareName(key);
        if (bare.isEmpty()) {
            return null;
        }
        String number = GlyphSplitter.NUMBER_PLACEHOLDER;
        StringBuilder out = new StringBuilder();
        int from = 0;
        int nth = 0;
        for (int at = bare.indexOf(number); at >= 0; at = bare.indexOf(number, from)) {
            out.append(bare, from, at).append("{~").append(++nth).append('}');
            from = at + number.length();
        }
        return out.append(bare, from, bare.length()).toString();
    }

    /** 名稱的鍵拿掉圖示之後的樣子；圖示兩邊的空白併成一個。見 {@link TranslationIndex#bareNames}。 */
    static String bareName(String key) {
        return key.indexOf(ICON) < 0 ? key
                : key.replace(ICON, " ").strip().replaceAll(" {2,}", " ");
    }

    /**
     * 現在畫的<b>不是</b>物品名稱那一行，所以別附原文。
     *
     * <h2>實機回報</h2>
     * 公會徽章的 tooltip 長這樣：
     *
     * <pre>
     *   Guild Badge
     *
     *   Unlocks the Guild Badge
     *   Snowflake
     * </pre>
     *
     * 最後那一行是徽章的名字，而 Wynncraft 剛好有一件飾品也叫 {@code Snowflake}
     * ——只有裝備檔用到這個原文，{@link TranslationIndex#gearOnly} 說是，於是敘述裡的徽章名
     * 被當成裝備名附上原文，「雪花 (Snowflake)」還被 {@code NameWrap} 折成兩行，
     * 夾在一段英文敘述中間。
     *
     * <p>附原文是給玩家<b>對 wiki 與市場</b>用的，那個需求只在物品名稱那一行
     * 成立。敘述裡提到同名的東西時，附上去只是噪音。名稱在哪一行是呼叫端才
     * 知道的事（跟 {@link #isBareGearName} 同一個道理），所以由
     * {@code TooltipPanel#translateLines} 在畫第三行以後時掛上這個旗標。
     *
     * <p>預設是關的：漏掛只會少一對括號，掛錯才會讓整份語料的名稱都不附原文。
     */
    public static void holdAppendedOriginal(boolean on) {
        HOLD_APPENDED.set(on);
    }

    /** 見 {@link #holdAppendedOriginal}。暫時改掉的人要還原成這個值。 */
    static boolean appendedOriginalHeld() {
        return HOLD_APPENDED.get();
    }

    /** 見 {@link #nameLine}。暫時改掉的人要還原成這個值。 */
    static boolean onNameLine() {
        return NAME_LINE.get();
    }

    /** 見 {@link #holdAppendedOriginal}。每條執行緒各一份，理由見 {@link #BAR_ITEMS}。 */
    static final ThreadLocal<Boolean> HOLD_APPENDED =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * 接下來查的這一段<b>不在物品會出現的位置</b>：只有物品在用的名字查到了也不算數。
     *
     * <h2>實機回報（2026-10-10）：「漂浮字會翻譯到武器或裝備」</h2>
     * 傳送點上浮著「Return to Detlas」，畫面上是「回归之戒 (Return) 至 Detlas」——
     * {@code Return} 剛好是一枚戒指的名字。六個語言的裝備名補齊之後，四千多個名字都是
     * 查得到的鍵，其中不少是一般的字，而查表本身不知道自己被問的是哪個位置：
     *
     * <pre>
     *   Return to Detlas     名字 + 數值尾巴（to 與地名都算尾巴）
     *   Frog - 1200❤          血條：名字 + 血量
     *   Fatal {#}{#}          怪物名牌：名字 + 等級與血條的圖示
     *   Grook's Nest          某人的東西
     * </pre>
     *
     * 這幾種形狀前面的那個字是<b>標籤或怪物的名字</b>，不是物品。由認得出形狀的呼叫端
     * （{@code LineTranslator}）掛上這個旗標；物品清單（{@code - 名字}、圖示開頭的
     * {@code {#}{#}名字}）與「名字 [數量]」不掛，照舊翻。
     *
     * <p>只擋<b>只有物品在用</b>的原文（{@link TranslationIndex#getsOriginal}）：別的檔案有同名條目的
     * （{@code Thorns} 既是屬性也是裝備）照舊查得到。擋下來的結果是留原文——
     * 留英文比寫上一件不相干的裝備好，真正該翻的怪物名補進 {@code npc.json} 就會先命中。
     *
     * <p>預設是關的。呼叫端要自己還原成進來時的值（見 {@link #itemNamesBarred}），
     * 這幾條路會互相呼叫。
     */
    public static void barItemNames(boolean on) {
        BAR_ITEMS.set(on);
    }

    /** 見 {@link #barItemNames}。 */
    public static boolean itemNamesBarred() {
        return BAR_ITEMS.get();
    }

    /**
     * 見 {@link #barItemNames}。<b>每條執行緒各一份</b>。
     *
     * <h2>實機回報（2026-10-10，1.0.1）：「切換語言之後物品翻譯都會失效」</h2>
     * 這裡原本是一個靜態的 {@code volatile boolean}，註解寫「算繪只有一條執行緒，
     * 用靜態的就夠」。查表不只算繪那一條在做：收集端每 30 秒在背景執行緒整理一次
     * {@code captured.json}（{@code CaptureStore#prune}），每一條都問
     * {@link #hasTranslation}，那條路會走進 {@code LineTranslator#lookup}，
     * 同樣「掛上、查、還原成進來時的值」。兩條執行緒交錯時：
     *
     * <pre>
     *   背景   掛上（true）
     *   算繪   讀到進來時的值 = true ……
     *   背景   還原（false）
     *   算繪   …… 還原成「進來時的值」= true      ← 從此沒有人會再把它放下
     * </pre>
     *
     * 旗標一旦卡在開著，只有物品在用的名字（裝備、素材、材料）就<b>全部</b>查不到，
     * 屬性與敘述照舊翻——畫面上是「名稱列英文、底下是譯文」，重開遊戲才會好。
     * 切語言會讓所有快取作廢、整個畫面重翻一次，所以特別容易撞上；但它跟語言無關。
     *
     * <p>{@link #NAME_LINE}、{@link #HOLD_APPENDED} 只有算繪那條在寫，不會卡住，
     * 但背景那條不該讀到「現在正在畫名稱列」，一起改掉。
     */
    static final ThreadLocal<Boolean> BAR_ITEMS =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    /** 圖示的佔位符。 */
    static final String ICON = "{#}";

    /** 名稱列尾巴的鑑定度。 */
    private static final String ROLLED = " [{~}]";

    /**
     * 「物品名稱那一列」裡面的名稱；不是這種列回傳 {@code null}。
     *
     * <h2>名稱列長什麼樣</h2>
     * 同一個名字在 tooltip 上會以幾種外框出現，差別只在前後的圖示與鑑定度：
     *
     * <pre>
     *   {#}Warp{#}                    第 0 行（看不見的那一行）
     *   {#}{#}{#}Capricorn{#}
     *   {#}{#}{#}{#}{#}Warp [{~}]     看得見的那一行，已鑑定
     *   {#}{#}{#}{#}{#}Filched Purse
     *   {#}{#}{#}{#}{#}{#}{#}Hero
     * </pre>
     *
     * <h2>為什麼這種列不能有整列的譯文（使用者 2026-10-09 回報）</h2>
     * 整列模板的優先度高於逐片段。語料裡一旦有 {@code {#}Warp{#}} 這種條目，
     * 名稱就不再經過「名稱」那條路，於是那條路負責的事全部失效，而且
     * <b>只有這幾件</b>失效，看起來像隨機：
     *
     * <ul>
     *   <li>「譯名加原文」不附原文——越相杖、静星耀杖沒有 (Warp)、(Halcyon)，
     *       旁邊的橡木匕首卻有；</li>
     *   <li>F6 關掉物品名稱、按住 Shift 看原文，這幾件照樣是譯名；</li>
     *   <li>條目的譯文跟原文一樣時（收進來時還沒翻），裝備檔後來補的譯名
     *       永遠顯示不出來——繁中的 Withdrawal、Capricorn 就是這樣卡在英文。</li>
     * </ul>
     *
     * <p>這種條目是收集端把「名稱還沒翻的那一列」當成缺口記下來、再被照著補出來的，
     * 清掉一批還會再長。所以查表這一層直接不認，收集端也不再記
     * （見 {@link #hasTranslation}）。
     *
     * <h2>哪些不算</h2>
     * 只有一個圖示開頭、後面什麼都沒有的（{@code {#}Redemption}）是 Lootrun 的
     * 使命名，剛好跟裝備撞名，不是名稱列。所以外框要夠像：尾巴有圖示或鑑定度，
     * 或者開頭至少三個圖示。
     */
    public static String nameRowInner(String key) {
        if (key == null) {
            return null;
        }
        int start = 0;
        while (key.startsWith(ICON, start)) {
            start += ICON.length();
        }
        int leading = start / ICON.length();
        int end = key.length();
        boolean rolled = key.endsWith(ROLLED);
        if (rolled) {
            end -= ROLLED.length();
        }
        int trailing = 0;
        while (end - ICON.length() >= start && key.startsWith(ICON, end - ICON.length())) {
            end -= ICON.length();
            trailing++;
        }
        if (leading == 0 || end <= start) {
            return null;
        }
        if (trailing == 0 && !rolled && leading < 3) {
            return null;                       // {#}Redemption：使命名，不是名稱列
        }
        String inner = key.substring(start, end);
        return inner.indexOf('{') >= 0 || !inner.equals(inner.strip()) ? null : inner;
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
        for (int i = 0; i < SPELLING.length; i++) {
            out = out.replace(SPELLING[i][0], SPELLING[i][1]);
            out = out.replace(SPELLING_UPPER[i][0], SPELLING_UPPER[i][1]);
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
     * {@link #SPELLING} 的開頭大寫版，跟它一一對應。
     *
     * <p>先前每次查不到都在迴圈裡現組這六十個字串（{@link #upper}），名牌與 tooltip 查不到的時候
     * 這一步佔了查表時間的四到五成。內容跟現組的完全一樣，只是算一次就好。
     */
    private static final String[][] SPELLING_UPPER = upperPairs();

    private static String[][] upperPairs() {
        String[][] out = new String[SPELLING.length][];
        for (int i = 0; i < SPELLING.length; i++) {
            out[i] = new String[] {upper(SPELLING[i][0]), upper(SPELLING[i][1])};
        }
        return out;
    }

    /**
     * Wynncraft 拿來做<b>縮排</b>的字元。在它的字型裡 {@code À} 是一格固定寬度的空白，
     * 用來把「效果」那類的細項推到右邊對齊。
     */
    private static final char INDENT = 'À';

    /**
     * 開頭有幾格縮排。
     *
     * <p>開放出去是因為跨行查表與段落判斷也得認得它：Fabled 物品的 Major ID
     * 說明，續行開頭就是一串 {@code À}（讓續行對齊在「◆ 」後面）。見
     * {@code LineTranslator#rejoin} 與 {@code TooltipPanel#translateLines}。
     */
    public static int indentOf(String text) {
        int n = 0;
        while (n < text.length() && text.charAt(n) == INDENT) {
            n++;
        }
        return n;
    }
}
