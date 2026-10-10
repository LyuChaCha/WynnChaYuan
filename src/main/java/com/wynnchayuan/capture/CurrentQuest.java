package com.wynnchayuan.capture;

/**
 * 現在追蹤的是哪個任務。
 *
 * <h2>為什麼要記這個</h2>
 * 對話是一句一句進來的，收集下來就是一大堆看不出關聯的句子。譯者拿到那份檔案，
 * 面對的是幾百句斷了脈絡的台詞——不知道誰在跟誰講話、不知道前後文，
 * 光是「這句是問句還是回答」都得用猜的，翻出來的語氣自然接不起來。
 *
 * <p>但玩家在跑對話的當下，追蹤器上就寫著任務名稱。把那個名稱一起記進 {@code ctx}，
 * 合併時就能照任務分堆，同一段劇情的台詞會排在一起。
 *
 * <h2>限制</h2>
 * 這是<b>當下</b>追蹤的任務，不保證就是這段對話所屬的任務——玩家可能追著 A
 * 卻順手跟 B 的 NPC 講話。所以它的定位是「幫譯者分堆的線索」，不是權威資料，
 * 分錯了頂多是排序不理想，不影響譯文本身。
 */
public final class CurrentQuest {

    /** 任務名稱與 NPC 名稱裡不能出現的字，會把 ctx 的分隔弄亂。 */
    private static final String SEPARATORS = "/#";

    private static volatile String name;

    private CurrentQuest() {}

    public static void set(String value) {
        name = value == null || value.isBlank() ? null : sanitise(value);
    }

    /** @return 現在追蹤的任務名稱；沒有就回傳 {@code null} */
    public static String get() {
        return name;
    }

    /**
     * 把任務名稱接在 {@code ctx} 後面。
     *
     * @return 例如 {@code dialogue/Cook Assistant}；不知道任務時原樣回傳 {@code base}
     */
    public static String tag(String base) {
        String current = name;
        return current == null ? base : base + "/" + current;
    }

    /**
     * 再接上說話的是誰。
     *
     * @return 例如 {@code dialogue/Cook Assistant#Aledar}
     */
    public static String tag(String base, String speaker) {
        String tagged = tag(base);
        String who = speakerName(speaker);
        return who == null ? tagged : tagged + "#" + sanitise(who);
    }

    /**
     * 說話者只留<b>名字</b>。
     *
     * <h2>實機回報（issue #1124）</h2>
     * 對話框沒給名牌時，說話者退回「玩家正前方那塊名牌」
     * （{@code LookAtTranslator#nearestLabel}）——而那是<b>整塊</b>名牌：名字底下
     * 還有一列等級牌，全是自訂字型的圖示與排版偏移（U+E060、U+CFFFF…U+D0002）。
     * 於是 {@code ctx} 與匯出的 {@code speaker} 變成「Old Drunk、換行、一串圖示」，
     * 一份 75 句對話的檔案裡有 33 句是這樣，匯入時得一句一句手改。
     *
     * <p>名字到第一個換行、或第一個圖示／排版偏移碼位為止（「Espren Citizen」
     * 後面直接跟著圖示的那一種沒有換行）。開頭的圖示與空白先跳過，免得名字上面
     * 還有一列圖示時整個變成空的。
     *
     * @return 乾淨的名字；什麼都不剩就回傳 {@code null}
     */
    public static String speakerName(String raw) {
        if (raw == null) {
            return null;
        }
        int at = 0;
        while (at < raw.length()) {
            int cp = raw.codePointAt(at);
            if (!isPlateJunk(cp) && !Character.isWhitespace(cp)) {
                break;
            }
            at += Character.charCount(cp);
        }
        int end = at;
        while (end < raw.length()) {
            int cp = raw.codePointAt(end);
            if (cp == '\n' || cp == '\r' || isPlateJunk(cp)) {
                break;
            }
            end += Character.charCount(cp);
        }
        String name = raw.substring(at, end).strip();
        return name.isEmpty() ? null : name;
    }

    /** 私用區的圖示、未分配碼位、排版偏移——名牌上不屬於名字的東西。 */
    private static boolean isPlateJunk(int cp) {
        return GlyphSplitter.isGlyphCodePoint(cp)
                || com.wynnchayuan.translate.SpaceOffset.isOffset(cp);
    }

    private static String sanitise(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            sb.append(SEPARATORS.indexOf(c) >= 0 ? ' ' : c);
        }
        return sb.toString().strip();
    }
}
