package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;

/**
 * 讓 Wynntils 的搜尋框吃得下<b>譯文</b>。
 *
 * <h2>要解決什麼</h2>
 * 內容書、設定、指南那幾個畫面的搜尋是 <b>Wynntils 自己在本機比對</b>的，
 * 比的是它手上的英文資料。畫面上那張卡寫著「毀滅前奏」，玩家照著打「毀滅」
 * 卻一張都沒亮——因為它拿去比的是 {@code Prelude to Annihilation}。
 * 打「anni」才中，等於逼玩家記住英文叫什麼。
 *
 * <p>跟交易市集那一支（{@link MarketSearch}）是<b>相反</b>的解法，因為問題
 * 不一樣：市集是<b>伺服器</b>在搜，只認英文，所以要把玩家打的中文換成英文送
 * 出去。這裡是本機在搜，字都在我們手上，所以反過來——把它要比對的英文翻成
 * 中文，再比一次。
 *
 * <h2>只加不減</h2>
 * 這裡<b>只在 Wynntils 自己判定「沒中」之後才被問到</b>（見
 * {@code WynntilsSearchMixin}），回傳 true 只會讓原本不亮的亮起來，
 * 不會讓原本會亮的消失。所以最壞的情況是多亮幾張卡，不會是搜不到東西。
 *
 * <h2>比對規則照抄 Wynntils</h2>
 * Wynntils 的 {@code StringUtils.partialMatch} 不是「包含」而是
 * <b>子序列</b>：查詢字的每一個字元要依序出現在內容裡就算中
 *（所以「anni」中得了 {@code Prelude to Annihilation}）。
 * 這裡自己寫一份一樣的，有兩個理由：
 *
 * <ul>
 *   <li>這個類別不相依 Wynntils，測試才跑得動；</li>
 *   <li>兩邊的規則要一致，玩家才不會覺得「中文跟英文的搜尋方式不一樣」。</li>
 * </ul>
 *
 * 哪天 Wynntils 改了它的演算法，我們這份頂多是<b>多中或少中幾筆</b>——
 * 因為只加不減，不會把它原本的行為弄壞。
 */
public final class SearchMatch {

    private SearchMatch() {}

    /**
     * mixin 的入口：設定與語料從全域拿，出任何事都當作「沒中」。
     *
     * <p>包在 try/catch 裡的理由跟 {@code WynntilsText} 一樣——這是別人的
     * 畫面在跑，我們丟出例外等於讓別人的搜尋框壞掉。
     */
    public static boolean alsoMatches(String content, String query) {
        try {
            noteHook();
            return alsoMatches(content, query,
                    WynnChaYuan.config(), WynnChaYuan.translations());
        } catch (Throwable t) {
            return false;
        }
    }

    /** mixin 有沒有真的接上，一輩子只記一次。 */
    private static volatile boolean noted = false;

    /**
     * 在診斷檔裡留一筆「這一道真的被呼叫到了」。
     *
     * <p>mixins.json 的 {@code defaultRequire} 是 0——注入失敗時遊戲照常開，
     * 一個字都不會說。對別的 mixin 那是對的（翻譯少一塊總比進不去遊戲好），
     * 但對這一道來說，「注入失敗」與「語料查不到」在畫面上<b>一模一樣</b>：
     * 都是打中文搜不到東西。少了這一筆，下次回報又只能從截圖猜。
     */
    private static void noteHook() {
        if (noted) {
            return;
        }
        noted = true;
        try {
            WynnChaYuan.store().noteEvent("search.hook");
        } catch (Throwable ignored) {
            // 診斷記不成不該影響搜尋
        }
    }

    /**
     * 拿這一筆的譯文再比一次。
     *
     * @param content Wynntils 要比對的原文（活動名稱、設定名稱⋯⋯）
     * @param query   玩家打進搜尋框的字
     * @return 譯文中得了就 {@code true}；查不到譯文、或設定關著時一律 {@code false}
     */
    static boolean alsoMatches(String content, String query,
                               CollectorConfig config, TranslationStore store) {
        if (content == null || query == null || query.isEmpty()
                || config == null || store == null) {
            return false;
        }
        // 跟著「翻譯 Wynntils 介面」那個開關走。介面留英文的人畫面上看到的
        // 就是英文，這時候讓中文也中得了只會讓他困惑。
        if (!config.wynntilsUi()) {
            return false;
        }
        String translated = store.lookup(content.strip());
        if (translated == null || translated.equals(content)) {
            return false;
        }
        return partialMatch(translated, query);
    }

    /**
     * 子序列比對，規則同 Wynntils 的 {@code StringUtils.partialMatch}：
     * {@code query} 的每一個字元要<b>依序</b>出現在 {@code content} 裡。
     *
     * <p>大小寫用 {@link java.util.Locale#ROOT} 折平，跟它一致——用預設地區的話，
     * 土耳其語環境的 {@code I} 會折成不帶點的 {@code ı}，同一份語料在不同機器上
     * 搜出來的結果會不一樣。
     */
    static boolean partialMatch(String content, String query) {
        String needle = query.toLowerCase(java.util.Locale.ROOT);
        String haystack = content.toLowerCase(java.util.Locale.ROOT);
        int from = 0;
        for (int i = 0; i < needle.length(); i++) {
            int at = haystack.indexOf(needle.charAt(i), from);
            if (at < 0) {
                return false;
            }
            from = at + 1;
        }
        return true;
    }
}
