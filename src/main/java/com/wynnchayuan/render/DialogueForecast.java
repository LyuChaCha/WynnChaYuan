package com.wynnchayuan.render;

import com.wynnchayuan.translate.TranslationStore;

import java.util.function.LongSupplier;

/**
 * 記住上一句認出來的是哪個故事的第幾句，拿來預測下一句。
 *
 * <h2>為什麼</h2>
 * 使用者回報：「能不能先預測，直接讓它從譯文開始跑？目前是先跳一部分原文才會跑譯文。」
 * 逐字打字時，前綴要長到在追蹤中的任務裡唯一（至少 4 個字）、或在全庫唯一
 * （至少 12 個字）才認得出是哪一句，所以每一句開頭都先跑一段英文。
 *
 * <p>但對話是一句接一句來的。上一句認出來是任務檔的第 N 句，這一句就在後面幾句裡；
 * 名牌又寫著是誰在講。分兩層：
 * <ol>
 *   <li><b>下一句</b>（{@link TranslationStore#forecast}）：上一句後面那幾句裡、名牌那個人
 *       講的、而且全任務／那個人的全部台詞／未歸屬台詞裡都沒有別句對得上——
 *       第一個字就給。條件很嚴，有任何別句的可能就不猜（錯的中文比沒翻更糟，
 *       見 {@link DialogueRewriter#within}）。</li>
 *   <li><b>上一句所在的任務</b>：當成追蹤中的任務那樣查（至少 4 個字、任務裡唯一）。
 *       玩家沒追蹤任務、或追著別的任務時，原本要等全庫唯一的 12 個字。</li>
 * </ol>
 *
 * <p>逐字模擬全部任務對話、一句接一句（{@code DialogueLeadAudit}，zh_tw 兩萬兩千句）：
 * 沒追蹤任務時第一個中文之前的英文字元平均 12.7 → 5.2，追蹤任務時 6.4 → 5.1。
 * 第一個字就是中文的只有 0.1%：第一層要「整個任務、那個人的全部台詞、quest.json 裡
 * 還沒歸屬的台詞都沒有別句對得上」，多半要打到第 3～5 個字才分得開。放寬那一道
 * （不管 quest.json）能到一成，但那就多了「其實是那句未歸屬的台詞」這種猜錯的可能。
 *
 * <h2>兩條對話路徑各一份</h2>
 * 就地取代（{@link DialogueRewriter}）與小框（{@link DialogueOverlay}）各自決定每一幀
 * 畫什麼，各留一份自己的「上一句」，互不干擾。
 *
 * <h2>上一句要多新</h2>
 * 上一句是 {@link #FRESH_MS} 以內還在畫面上的才算。隔太久多半是另一段對話了：
 * 那時退回「整個追蹤中的任務都是候選、至少 4 個字」——就是預測之前的門檻，
 * 只是多了名牌幫忙分辨。
 */
final class DialogueForecast {

    /** 上一句之後看幾句。任務檔裡同一句話常收了 wiki 與實機兩條，所以留寬一點。 */
    static final int WINDOW = 8;

    /** 上一句畫面上消失多久之內還算「同一段對話」。 */
    static final long FRESH_MS = 45_000;

    /**
     * 認定「這一句就是它」至少要打出幾個字（或整句打完）。
     *
     * <p>預測自己猜中的那一兩個字不算數：萬一那句不在語料裡、只是開頭一樣，
     * 錯的那句不能變成下一次預測的起點。
     */
    static final int CONFIRM = 8;

    /** 沒有上一句時至少幾個字，跟 {@code TranslationStore#MIN_QUEST_PREFIX} 一樣。 */
    static final int COLD_FLOOR = 4;

    /** 第零層看上一句之後幾句。對話講到一半，下一句就是緊接著的那句（加上 wiki／實機兩個版本）。 */
    static final int NEXT_WINDOW = 4;

    /** 第零層至少打幾個字。量表會試別的值；實機用預設。 */
    static volatile int nextFloor = 2;

    /** 測試用：關掉第零層。 */
    static volatile boolean nextLine = true;

    /** 第三層的門檻（見 {@link #guess}）。量表會試別的值；實機用預設。 */
    static volatile int speakerFloor = COLD_FLOOR;

    /** 測試用：關掉第三層。 */
    static volatile boolean speakerScope = true;

    /** 測試用：關掉預測，量「沒有預測時」的樣子。實機永遠是開的。 */
    static volatile boolean enabled = true;

    private String quest;
    private int index = -1;

    /**
     * 上一句停下來時進度條的值（{@link com.wynnchayuan.capture.DialogueProgress}）；
     * 不知道是 -1。沒滿格就表示那段對話還沒講完。
     */
    private int progress = -1;

    /** 記下 {@link #progress} 時是第幾段對話；對話框收掉過就不算數。 */
    private int talk = -1;
    private String source;
    private long seen;

    private final LongSupplier clock;

    DialogueForecast() {
        this(() -> now.getAsLong());
    }

    /** 時鐘。測試換掉它來模擬「隔了很久才講下一句」。 */
    static volatile LongSupplier now = System::currentTimeMillis;

    DialogueForecast(LongSupplier clock) {
        this.clock = clock;
    }

    /** 兩條路各一份，測試要一起清掉。 */
    static void forgetAll() {
        DialogueRewriter.FORECAST.forget();
        DialogueOverlay.FORECAST.forget();
    }

    void forget() {
        asked = null;
        answer = null;
        quest = null;
        index = -1;
        source = null;
        seen = 0;
        progress = -1;
    }

    /**
     * 這一幀畫面上的這幾個字，預測是哪一句。
     *
     * @param typed 模板（玩家名已經收成 {@code {u}} 的那一份）
     * @return 預測的原文；不夠確定就回傳 {@code null}
     */
    String guess(TranslationStore store, String typed) {
        String speaker = com.wynnchayuan.capture.DialogueSpeaker.current();
        if (!enabled || store == null || typed == null) {
            return null;
        }
        if (source != null && typed.length() > source.length() && typed.startsWith(source)) {
            // 「上一句」其實是<b>這一句</b>打到一半時被當成的那條短句（「Alright!」打完
            // 先貼上自己的譯文，接著又打出「Alright! Let's…」）。它不是上一句，
            // 拿它當起點等於拿一個沒把握的猜測再去猜，乾脆不猜。
            return null;
        }
        boolean fresh = quest != null && clock.getAsLong() - seen <= FRESH_MS;
        String tracked = com.wynnchayuan.capture.CurrentQuest.get();
        String where = fresh ? quest : tracked;
        if (where == null) {
            return null;
        }
        // 同一個問題問第二次直接回上一次的答案。認不出來的句子打完之後每一幀都會再問，
        // 每次都掃一遍整個任務與那個人的全部台詞，會在畫面上卡（#1444 的教訓）。
        // 快取放在這裡、一條路一份：兩條路同時在問時才不會互相把對方的答案擠掉。
        boolean midway = fresh && progress > 0
                && progress < com.wynnchayuan.capture.DialogueProgress.FULL - 2
                && talk == com.wynnchayuan.capture.DialogueProgress.conversation();
        Query ask = new Query(typed, where, fresh ? index : -1, speaker, tracked,
                store.generation(), midway);
        if (ask.equals(asked)) {
            return answer;
        }
        String guess = null;
        if (midway && nextLine) {
            // 第零層：上一句停下來時進度條沒滿格——那段對話還沒講完，這一句就是它的
            // 下一句。玩家回頭找 NPC 重講、順路跟別人聊天，進度條都會從頭來，
            // 不會落在這裡；所以只要在上一句後面幾句裡唯一就夠了，不必再跟整個任務、
            // 這個人全部的台詞比。見 TranslationStore#forecastNext。
            guess = store.forecastNext(typed, where, index, NEXT_WINDOW, speaker, nextFloor,
                    DialogueRewriter::within);
        }
        if (guess == null && speaker != null) {
            guess = store.forecast(typed, where, fresh ? index : -1, WINDOW, speaker,
                    fresh ? 1 : COLD_FLOOR, DialogueRewriter::within);
        }
        if (guess == null && fresh && !quest.equals(tracked)) {
            // 第二層：上一句所在的任務，當成「追蹤中的任務」那樣查（至少 4 個字、
            // 在這個任務裡唯一）。條件跟追蹤欄那條一模一樣，差別只在任務是從上一句
            // 認出來的——玩家沒追蹤任務、或追著別的任務時，這段對話照樣認得出來，
            // 不必等到全庫唯一的 12 個字。
            guess = store.matchPrefix(typed, 0, quest);
        }
        if (guess == null && speaker != null && speakerScope) {
            // 第三層：同一個任務裡、名牌那個人（或不知道誰）講的句子裡唯一。
            // 見 TranslationStore#forecastBySpeaker。
            guess = store.forecastBySpeaker(typed, where, speaker, speakerFloor,
                    DialogueRewriter::within);
        }
        asked = ask;
        answer = guess;
        return guess;
    }

    /** 見 {@link #guess}：上一次問了什麼、答了什麼。 */
    private record Query(String typed, String where, int after, String speaker, String tracked,
                         int generation, boolean midway) {}

    private Query asked;
    private String answer;

    /**
     * 這一幀確定畫面上是 {@code heard} 這一句（打出夠多字、或整句打完）。
     *
     * <p>每一幀都叫：玩家讀得久，「上一句多新」要從它最後還在畫面上的那一刻算。
     */
    void heard(TranslationStore store, String heard) {
        if (store == null || heard == null) {
            return;
        }
        long now = clock.getAsLong();
        if (heard.equals(source)) {
            seen = now;
            int bar = com.wynnchayuan.capture.DialogueProgress.settled();
            if (bar >= 0) {
                progress = bar;                // 這一句停下來時，對話講到哪
                talk = com.wynnchayuan.capture.DialogueProgress.conversation();
            }
            return;
        }
        String q = store.sequenceQuestOf(heard, quest,
                com.wynnchayuan.capture.CurrentQuest.get());
        int at = q == null ? -1 : store.sequenceIndex(q, heard, q.equals(quest) ? index : -1);
        if (at < 0) {
            return;                            // 不是任務台詞（或是拼出來的），不當起點
        }
        quest = q;
        index = at;
        source = heard;
        seen = now;
        progress = -1;                         // 換了一句：等進度條停下來再記
    }

    /** 給測試看的：上一句排在第幾。 */
    int index() {
        return index;
    }

    /** 給量表看的：上一句的故事。 */
    String anchorQuest() {
        return quest;
    }

    /** 給量表看的：上一句還算不算同一段對話。 */
    boolean fresh() {
        return quest != null && clock.getAsLong() - seen <= FRESH_MS;
    }
}
