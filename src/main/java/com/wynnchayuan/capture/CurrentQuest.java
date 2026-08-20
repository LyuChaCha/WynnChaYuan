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

    /** 任務名稱裡不能出現的字，會把 ctx 的分隔弄亂。 */
    private static final String SEPARATORS = "/";

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

    private static String sanitise(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            sb.append(SEPARATORS.indexOf(c) >= 0 ? ' ' : c);
        }
        return sb.toString().strip();
    }
}
