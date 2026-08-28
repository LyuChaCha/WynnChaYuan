package com.wynnchayuan.capture;

import java.util.ArrayList;
import java.util.List;

/**
 * 最近的聊天訊息，供「複製聊天」使用。
 *
 * <h2>為什麼自己記一份</h2>
 * 想做的是「挑一則訊息、把它複製走」。直覺的做法是去問 Minecraft
 * 「滑鼠指著第幾行」，但 1.21.11 的 {@code ChatComponent} 沒有公開那個方法，
 * 真正的資料（{@code trimmedMessages}、{@code chatScrollbarPos}）都是私有的，
 * 得靠 mixin 挖進去，還要自己重算一次它的排版幾何。
 *
 * <p>這個模組到目前為止<b>一個 mixin 都沒有</b>，全靠 Wynntils 的事件與自己畫的
 * 面板。為了複製文字而開這條路，等於多了一個會隨版本壞掉的地方。
 *
 * <p>而我們本來就看得到每一則訊息——{@code ChatListener} 就掛在那裡。
 * 自己記一份，要複製什麼、怎麼排，都在自己手上。
 *
 * <h2>只留在本機</h2>
 * 這份紀錄<b>不會</b>進語料，也不會送到任何地方。它包含其他玩家講的話，
 * 那是真人寫的內容——收進共用語料是不行的（見 {@link PlayerDataFilter}），
 * 但玩家要把自己畫面上的東西複製走，本來就是他的自由。
 */
public final class ChatLog {

    /**
     * 留幾則。
     *
     * <p>聊天視窗一頁大約二十行，翻頁看得到的大概上百行。留兩百則足夠回頭找，
     * 又不會讓面板長到捲不完。
     */
    public static final int CAPACITY = 200;

    /**
     * 一則訊息的原文與譯文。
     *
     * @param original   遊戲原本要顯示的內容
     * @param translated 我們的譯文；查不到就是 {@code null}
     */
    public record Entry(String original, String translated) {

        /** 依聊天訊息的顯示模式決定複製哪一份。 */
        public String forMode(boolean replace, boolean both) {
            if (translated == null || translated.isBlank()) {
                return original;
            }
            if (both) {
                return original + "\n" + translated;
            }
            return replace ? translated : original;
        }
    }

    private static final ArrayList<Entry> entries = new ArrayList<>();

    private ChatLog() {}

    /**
     * 記一則。原文空白的直接丟掉——分隔線、純排版符號那種複製了也沒用。
     */
    public static synchronized void add(String original, String translated) {
        if (original == null || original.isBlank()) {
            return;
        }
        entries.add(new Entry(original, translated));
        while (entries.size() > CAPACITY) {
            entries.remove(0);
        }
    }

    /** @return 由新到舊。面板從最近的開始列，那才是最常要複製的。 */
    public static synchronized List<Entry> recent() {
        List<Entry> out = new ArrayList<>(entries);
        java.util.Collections.reverse(out);
        return out;
    }

    public static synchronized void clear() {
        entries.clear();
    }

    public static synchronized int size() {
        return entries.size();
    }
}
