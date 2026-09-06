package com.wynnchayuan.translate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 中文物品名 → 英文原文，給交易市集的搜尋用。
 *
 * <h2>要解決什麼</h2>
 * 用「就地取代」的人，畫面上看到的是「Corkian 增幅器 III」。可是市集是
 * <b>伺服器</b>在搜尋，只認得英文——照著中文打進去一筆都搜不到，得自己記住
 * 英文叫什麼，那等於把翻譯的好處抵銷掉一半。
 *
 * <p>所以反過來查一次：玩家打中文，送出去之前換成英文。
 *
 * <h2>為什麼敢這樣改玩家送出去的字</h2>
 * 只在<b>市集正在等你輸入搜尋字</b>的那一刻改，其他時候一個字都不動——
 * 那個狀態是 Wynntils 的 {@code TradeMarketStateEvent} 給的，不是我們猜的。
 * 見 {@code MarketListener}。
 *
 * <h2>對不準的時候寧可不動</h2>
 * 一個中文名對到兩個英文名時<b>不換</b>，把候選列出來讓玩家自己挑。
 * 猜錯會搜到完全不相干的東西，那比不翻更糟——玩家還會以為市集上真的沒貨。
 *
 * <p>整份語料量過：2789 條物品名裡，會撞名的只有 41 個（1.5%）。
 */
public final class MarketSearch {

    /**
     * 正規化後的中文名 → 英文原文。
     *
     * <p>用 {@link TreeMap} 是為了「前綴查」那條路：玩家常常只打一半
     * （「增幅器」），要能從中間比對。
     */
    private final Map<String, Set<String>> index = new TreeMap<>();

    /** 太短的不收：一兩個字幾乎一定撞名，換出來的東西不會是玩家要的。 */
    private static final int MIN_NAME = 2;

    /**
     * 建索引。只收<b>物品名稱</b>，句子不收。
     *
     * <p>句子收進來會讓「前綴查」冒出一堆敘述文——玩家打「增幅器」，跳出
     * 「Corkian 強化道具可在物品鑑定師鑑定物品時使用」當候選，沒有意義。
     */
    public void add(String english, String chinese) {
        if (english == null || chinese == null) {
            return;
        }
        String zh = key(chinese);
        String en = bare(english);
        if (zh.length() < MIN_NAME || en.isEmpty() || zh.equals(en)
                || !looksLikeName(en)) {
            return;
        }
        index.computeIfAbsent(zh, k -> new LinkedHashSet<>()).add(en);
    }

    /**
     * 剝掉名字前後的裝飾。
     *
     * <h2>為什麼</h2>
     * 語料裡同一個東西常常收了兩次，差別只在面板上的項目符號：
     *
     * <pre>
     *   "Silverbull Share"    -> "Silverbull 股份"
     *   "✮ Silverbull Share"  -> "✮ Silverbull 股份"
     * </pre>
     *
     * 兩條都進索引的話，玩家打「股份」會看到<b>同一個物品出現兩次</b>，
     * 而且那個 {@code ✮} 打進市集只會搜不到。使用者回報的就是這個。
     *
     * <p>行尾的冒號同理（{@code "Tradable Shares:"}）。
     */
    private static String bare(String english) {
        int at = 0;
        while (at < english.length()) {
            int cp = english.codePointAt(at);
            if (Character.isWhitespace(cp) || BULLETS.indexOf(cp) >= 0
                    || Character.getType(cp) == Character.OTHER_SYMBOL) {
                at += Character.charCount(cp);
            } else {
                break;
            }
        }
        int end = english.length();
        while (end > at && (english.charAt(end - 1) == ':'
                || Character.isWhitespace(english.charAt(end - 1)))) {
            end--;
        }
        return english.substring(at, end);
    }

    /** 見 {@link #bare}：行首的項目符號。{@code ✮ ✦ ➤} 那些走 OTHER_SYMBOL。 */
    private static final String BULLETS = "-–—•*";

    /** 重新載入譯文時要先清掉，不然舊語料的名字會留著。 */
    public void clear() {
        index.clear();
    }

    public int size() {
        return index.size();
    }

    /**
     * 這個英文像不像<b>物品名稱</b>。
     *
     * <p>語料裡的扁平檔（{@code misc.json}、{@code gui.json}）沒有 role 可以看，
     * 名稱與整句敘述混在一起。敘述收進來的話，玩家打「增幅器」會跳出
     * 「Corkian Augments can be applied while identifying an item…」當候選，
     * 沒有意義而且會把真正的答案擠掉。
     *
     * <p>用形狀判斷：物品名不會很長、不會有句號、也不會有換行。
     */
    private static boolean looksLikeName(String english) {
        if (english.length() > MAX_NAME || english.indexOf('\n') >= 0) {
            return false;
        }
        // 佔位符是<b>模板</b>不是名字。「{~} emeralds」被當成候選丟給玩家，
        // 打進市集只會搜到空的——使用者回報的「會有佔位符號」就是這個。
        if (english.indexOf('{') >= 0 || english.indexOf('}') >= 0) {
            return false;
        }
        if (english.endsWith(".") || english.endsWith("!") || english.endsWith("?")) {
            return false;
        }
        String[] words = english.split(" ");
        return words.length <= MAX_WORDS && titleCase(words);
    }

    /**
     * 這幾個字看起來像<b>名字</b>而不是句子被折斷的一截。
     *
     * <h2>為什麼光看長度不夠</h2>
     * 面板上的句子會照寬度折行，每一截都短得像個名字，句號也留在最後一行：
     *
     * <pre>
     *   "Only tradable shares can be"    -> "只有可交易的股份才能"
     *   "Shares can be used on"          -> "股份可用於兌換"
     *   "Not enough tradable shares"     -> "可交易的股份不足"
     * </pre>
     *
     * 五個字以內、沒有句號，長度那一關全部放行，於是玩家打「股份」會拿到
     * 一串句子碎片當候選。
     *
     * <h2>怎麼分</h2>
     * 物品名是 Title Case，句子不是。但不能要求<b>每個</b>字都大寫開頭——
     * 真的有 {@code Ring of the Wild} 這種名字。所以放寬成兩條：
     * 小寫開頭的字不能超過一半，而且不能有<b>長的</b>小寫字。
     * 名字裡的小寫只會是 {@code of}、{@code the} 這種虛詞，
     * 句子裡的是 {@code tradable}、{@code enough} 這種實詞。
     */
    private static boolean titleCase(String[] words) {
        int lower = 0;
        for (String word : words) {
            if (word.isEmpty() || !Character.isLowerCase(word.codePointAt(0))) {
                continue;
            }
            if (word.length() > MAX_PARTICLE) {
                return false;
            }
            lower++;
        }
        return lower * 2 <= words.length;
    }

    /** 見 {@link #titleCase}：名字裡容得下的小寫虛詞有多長（{@code of}、{@code the}）。 */
    private static final int MAX_PARTICLE = 4;

    /** 見 {@link #looksLikeName}。「Legendary Corkian Augment」是三個字、25 個字元。 */
    private static final int MAX_NAME = 40;

    private static final int MAX_WORDS = 5;

    /**
     * 把中文查成英文。
     *
     * @return 找到剛好一個就回傳它；找不到或不只一個都回傳 {@code null}
     */
    public String lookup(String chinese) {
        List<String> hits = candidates(chinese);
        return hits.size() == 1 ? hits.get(0) : null;
    }

    /**
     * 所有對得上的英文名。
     *
     * <p>兩段：先要求<b>完全相同</b>，對不上才退而求其次找<b>包含</b>它的。
     * 兩段分開是因為「增幅器」完全相同時只對到一個（Amplifiers），
     * 但當成片段查會撈出八條——先問精確的，答案好得多。
     */
    public List<String> candidates(String chinese) {
        List<String> out = new ArrayList<>();
        if (chinese == null) {
            return out;
        }
        String zh = key(chinese);
        if (zh.length() < MIN_NAME) {
            return out;
        }
        Set<String> exact = index.get(zh);
        if (exact != null) {
            return singulars(exact);
        }
        Set<String> loose = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> e : index.entrySet()) {
            if (e.getKey().contains(zh)) {
                loose.addAll(e.getValue());
            }
        }
        return singulars(loose);
    }

    /**
     * 候選一律收斂成<b>單數</b>，一樣的就併成一個。
     *
     * <h2>為什麼</h2>
     * 語料裡的分類標題常常是複數：「增幅器」對到的是 {@code Amplifiers}、
     * 「絕緣器」對到 {@code Insulators}。而市集上的物品叫
     * {@code Corkian Amplifier I}——用複數搜不到東西。
     *
     * <p>市集是<b>包含</b>比對，所以單數一定不比複數差：搜 {@code Amplifier}
     * 找得到所有 Amplifier，搜 {@code Boot} 也照樣找得到 {@code Boots}。
     *
     * <p>順帶把「綠寶石」那種收乾淨：語料裡同時有 {@code Emeralds} 與
     * {@code Emerald}，收斂之後剩一個，就不必再問玩家要哪一個。
     */
    private static List<String> singulars(Set<String> names) {
        Set<String> out = new LinkedHashSet<>();
        for (String name : names) {
            out.add(singular(name));
        }
        return covered(new ArrayList<>(out));
    }

    /**
     * 別的候選<b>包含</b>得到的就不必再問。
     *
     * <h2>為什麼</h2>
     * 同一個物品在語料裡被不同面板收了好幾次，長短不一：
     *
     * <pre>
     *   Silverbull Share
     *   Trade Silverbull Share
     *   Get Tradable Silverbull Share
     * </pre>
     *
     * 玩家打「股份」會看到三個候選，可是它們<b>是同一個東西</b>——使用者回報的
     * 「只有關鍵字股份，但會找到多個同個物品」就是這個。
     *
     * <h2>為什麼留短的一定不吃虧</h2>
     * 市集是<b>包含</b>比對（{@link #singulars} 那條複數規則靠的也是這件事）。
     * 搜 {@code Silverbull Share} 找得到的東西，是搜
     * {@code Trade Silverbull Share} 的超集合——短的嚴格比較好，
     * 沒有任何一筆會因此漏掉。
     *
     * <p>只在<b>本來就要問玩家</b>的時候才會動到：候選剩一個時這個迴圈什麼都不做。
     * 所以最壞的情況是「本來要問三個，現在問一個」，不會把單一答案弄丟。
     */
    private static List<String> covered(List<String> names) {
        List<String> out = new ArrayList<>(names.size());
        for (String name : names) {
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            boolean redundant = false;
            for (String other : names) {
                if (other.length() < name.length()
                        && lower.contains(other.toLowerCase(java.util.Locale.ROOT))) {
                    redundant = true;
                    break;
                }
            }
            if (!redundant) {
                out.add(name);
            }
        }
        return out;
    }

    /**
     * 去掉結尾那個複數的 s。
     *
     * <p>只動<b>最後一個字</b>，而且謹慎：{@code ss} 結尾不動（Glass 不能變 Glas）、
     * 所有格的 {@code 's} 不動、太短的不動。
     */
    public static String singular(String name) {
        int at = name.lastIndexOf(' ');
        String head = at < 0 ? "" : name.substring(0, at + 1);
        String last = name.substring(at + 1);
        if (last.length() < MIN_PLURAL || !last.endsWith("s")) {
            return name;
        }
        char before = last.charAt(last.length() - 2);
        if (before == 's' || before == '\'' || before == '’') {
            return name;
        }
        return head + last.substring(0, last.length() - 1);
    }

    /** 見 {@link #singular}：短於這個長度的字不去 s。 */
    private static final int MIN_PLURAL = 4;

    /**
     * 比對用的形狀。
     *
     * <h2>為什麼要正規化</h2>
     * 玩家打的字跟語料裡的長得不會完全一樣：
     *
     * <pre>
     *   玩家打  Corkian增幅器 III      ← 中英之間沒有空格
     *   語料是  Corkian 增幅器 III
     * </pre>
     *
     * 差一個空格就整條對不到，而中文使用者<b>不會</b>自己在中英之間補空格。
     * 所以兩邊都把空白拿掉再比。英文字母一律小寫，大小寫不該影響搜尋。
     *
     * <p>佔位符也拿掉：語料裡的「{@code {~} x 熟成金果 {#}{#}{#}}」那種，
     * 玩家看到的畫面上是數量與圖示，打字時不會打進去。
     */
    static String key(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {                    // {#} {~} {p} {u} 與 {~1}
                int close = text.indexOf('}', i);
                if (close > 0 && close - i <= 4) {
                    i = close;
                    continue;
                }
            }
            if (Character.isWhitespace(c)) {
                continue;
            }
            out.append(Character.toLowerCase(c));
        }
        return out.toString();
    }
}
