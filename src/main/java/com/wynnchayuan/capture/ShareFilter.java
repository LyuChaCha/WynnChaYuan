package com.wynnchayuan.capture;

/**
 * 收集到的字串裡，哪些可以交給翻譯團隊。
 *
 * <h2>為什麼還留著</h2>
 * 這支原本是自動上傳（{@code CorpusUpload}）送出前的最後一道關卡。自動上傳
 * 已經拿掉了——模組不再把任何東西送出去——但玩家現在是<b>自己</b>把匯出的檔案
 * 附到公開的 Issue 上，那一樣是把字串交到別人手裡，而且一樣收不回來。
 * 所以濾網原封不動搬過來，改成套在匯出檔上（見 {@link CorpusExport}）。
 *
 * <p>濾網一共三道，各自獨立、刻意重複：這裡（模組端，{@code ShareFilterTest}）、
 * 收集站 {@code tools/collector/worker.js} 的 {@code acceptable}，以及
 * 進倉庫前 {@code tools/import-captured.py} 的 {@code NAMED}。改一道就三道一起改。
 */
public final class ShareFilter {

    private ShareFilter() {}

    /** 超過這個長度的不收——正常的一句台詞不會這麼長，那多半是黏在一起的雜訊。 */
    static final int MAX_LEN = 600;

    /**
     * 這一條可以交出去嗎。
     *
     * <h2>放行的來源</h2>
     * <ul>
     *   <li>{@code dialogue/…} 任務對話——語料的大宗，也是最需要人去跑的。</li>
     *   <li>{@code gui/…}、{@code tooltip/…} 介面與物品說明。</li>
     *   <li>{@code npc/…}、{@code label/…} 名牌。這兩類會夾到玩家名字，
     *       所以另外過 {@link PlayerDataFilter#looksPlayerNamed}。</li>
     *   <li>{@code chat/INFO} 伺服器自己的公告。</li>
     * </ul>
     *
     * <h2>擋掉的來源</h2>
     * {@code chat/} 底下除了 {@code INFO} 以外<b>全部</b>不收：公會、隊伍、喊話、
     * 私訊裡是別人打的字，裡面有名字、有閒聊、有什麼都可能。那些東西的翻譯
     * 價值趨近於零，而風險是把陌生人的話散到公開倉庫裡——完全不成比例。
     */
    public static boolean shareable(CaptureStore.Captured c) {
        if (c == null || c.src == null || c.src.isBlank() || c.src.length() > MAX_LEN) {
            return false;
        }
        if (!GlyphSplitter.hasLetter(c.src)) {
            return false;
        }
        String ctx = c.ctx == null ? "" : c.ctx;
        boolean ok = ctx.startsWith("dialogue/") || ctx.startsWith("gui/")
                || ctx.startsWith("tooltip/") || ctx.startsWith("npc/")
                || ctx.startsWith("label/") || ctx.equals("chat/INFO");
        if (!ok) {
            return false;
        }
        // 個資濾網再跑一次。收集時擋過了，但濾網一直在補，而這一條是要<b>交出去</b>的，
        // 拿現在這一版的規則重新問一次才算數。
        if (PlayerDataFilter.carriesPlayerData(c.src)) {
            return false;
        }
        if ((ctx.startsWith("npc/") || ctx.startsWith("label/"))
                && PlayerDataFilter.looksPlayerNamed(c.src)) {
            return false;
        }
        if (ctx.startsWith("gui/title") && PlayerDataFilter.looksAccountNamed(c.src)) {
            return false;
        }
        // 自己的名字（含暱稱）不管出現在哪裡都不收。
        return SelfNames.find(c.src) == null;
    }
}
