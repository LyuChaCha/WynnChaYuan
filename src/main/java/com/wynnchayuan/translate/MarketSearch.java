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
        String en = english.strip();
        if (zh.length() < MIN_NAME || en.isEmpty() || zh.equals(en)
                || !looksLikeName(en)) {
            return;
        }
        index.computeIfAbsent(zh, k -> new LinkedHashSet<>()).add(en);
    }

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
        if (english.endsWith(".") || english.endsWith("!") || english.endsWith("?")) {
            return false;
        }
        return english.split(" ").length <= MAX_WORDS;
    }

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
            out.addAll(exact);
            return out;
        }
        Set<String> loose = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> e : index.entrySet()) {
            if (e.getKey().contains(zh)) {
                loose.addAll(e.getValue());
            }
        }
        out.addAll(loose);
        return out;
    }

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
