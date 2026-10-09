package com.wynnchayuan.translate;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * 讓 WynnMarketSearch 的搜尋框吃得下譯名。
 *
 * <h2>那個模組在做什麼</h2>
 * <a href="https://github.com/a0gzy/WynnMarketSearch">WynnMarketSearch</a> 在市集問
 * 「Type the item name…」的時候，改跳出一個搜尋面板：邊打邊列出物品，點一下就把
 * <b>英文名</b>送進聊天。它的物品清單只有英文，比對是「英文名裡有沒有你打的字」。
 *
 * <h2>要解決什麼</h2>
 * 用就地取代的人，畫面上看到的是「骨弓」，但在那個面板裡打「骨弓」一筆都不會出現。
 * 跟聊天框那一路（{@code MarketListener}）是同一個問題，只是入口不同。
 *
 * <h2>怎麼接</h2>
 * 它問每一件物品「你符不符合這個搜尋字」（{@code WynnItem#matchesSearch}）。
 * 我們在那個問題前面多答一句：<b>這件物品的譯名裡有那幾個字，就算符合</b>；
 * 其餘的照它原本的規則。所以是聯集——打英文跟以前完全一樣，打譯名多找到東西，
 * 它自己的排序、最愛、上限十四筆都照舊。點下去送出的仍然是英文名，那一段我們不碰。
 *
 * <p>譯名哪裡來：跟聊天框那一路同一份索引（{@link MarketSearch}），只收可交易的物品。
 * 開關也是同一個（「市集搜尋轉英文」）。
 *
 * <h2>這個類別不碰遊戲</h2>
 * 掛進那個模組的 mixin 在 {@code mixin.WmsItemSearchMixin}，它只負責把問題轉過來。
 * 判斷全部在這裡，所以不必裝那個模組也測得到。
 */
public final class WmsBridge {

    private WmsBridge() {}

    /** 索引與開關去哪裡問。測試會換掉。 */
    static Supplier<MarketSearch> source = () ->
            com.wynnchayuan.WynnChaYuan.translations().market();
    static BooleanSupplier enabled = () ->
            com.wynnchayuan.WynnChaYuan.config().marketSearch();

    /** 打不到兩個字不查：一個字會對到幾百件，面板只列得下十四筆，等於沒查。 */
    private static final int MIN_QUERY = 2;

    /** 一次搜尋會問幾千件物品同一個字；答案算一次就好。 */
    private record Answer(String query, int indexSize, Set<String> english) {}

    private static volatile Answer last;

    /**
     * 這件物品的<b>譯名</b>符不符合搜尋字。
     *
     * @param english 物品的英文名
     * @param query   玩家打的字
     * @return {@code true} 是「譯名對得上，算它符合」；{@code false} 是「我們沒有意見」，
     *         由那個模組照它原本的規則判斷
     */
    public static boolean matches(String english, String query) {
        if (english == null || query == null) {
            return false;
        }
        try {
            Set<String> hits = resolve(query);
            // 索引裡的名字是單數（「…Boots」收成「…Boot」），拿來比的也要先收成單數
            return !hits.isEmpty()
                    && hits.contains(MarketSearch.singular(english).toLowerCase(Locale.ROOT));
        } catch (Throwable t) {
            // 這是掛在別人模組裡的程式。索引正在重新載入、設定還沒建好——任何狀況
            // 都當作沒有意見，讓它照原本的規則走；丟出去會把它的搜尋整個弄壞。
            return false;
        }
    }

    private static Set<String> resolve(String query) {
        String typed = query.strip();
        if (typed.length() < MIN_QUERY || !enabled.getAsBoolean()) {
            return Set.of();
        }
        MarketSearch market = source.get();
        if (market == null) {
            return Set.of();
        }
        Answer cached = last;
        if (cached != null && cached.query().equals(typed) && cached.indexSize() == market.size()) {
            return cached.english();
        }
        Set<String> english = new HashSet<>();
        for (MarketSearch.Suggestion s : market.suggestions(typed)) {
            english.add(s.english().toLowerCase(Locale.ROOT));
        }
        last = new Answer(typed, market.size(), english);
        return english;
    }

    /**
     * 這件物品的譯名，給那個面板在英文名旁邊多畫一行用。
     *
     * @return 沒有譯名、或功能關著時回 {@code null}
     */
    public static String translatedName(String english) {
        if (english == null || english.isEmpty()) {
            return null;
        }
        try {
            if (!enabled.getAsBoolean()) {
                return null;
            }
            MarketSearch market = source.get();
            return market == null ? null : market.shownFor(english);
        } catch (Throwable t) {
            return null;
        }
    }
}
