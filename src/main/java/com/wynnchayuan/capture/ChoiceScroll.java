package com.wynnchayuan.capture;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 把跑馬燈一格一格捲過去的選項，接回原本那一整句。
 *
 * <h2>實機回報 2026-09-30</h2>
 * 使用者翻自己的 {@code captured.json} 時發現「太長、會動的句子讀不到完整的」。
 * 那一份裡 120 條選項有 40 條是切一半的：
 *
 * <pre>
 *   Don't underestimate our stre
 *   How come one of the pedestal
 * </pre>
 *
 * <h2>為什麼收不到整句</h2>
 * 太長的選項 Wynncraft 做成跑馬燈，每個 tick 只送一個<b>固定寬度的視窗</b>
 * 過來（實機量到 27～28 個字元）：
 *
 * <pre>
 *   Don't underestimate our stre
 *   on't underestimate our stren
 *   n't underestimate our streng
 *   …
 * </pre>
 *
 * <b>整句從來不會出現在任何一幀裡。</b>所以無論收哪一格都是殘句——
 * 第一格看起來最像句子（開頭是好的），也最容易被誤收。
 *
 * <p>wiki 那邊也沒有：{@code fetch-quest-dialogue.py} 抓的是 NPC 的台詞，
 * <b>玩家的選項不在裡面</b>。所以整句只能自己從視窗接回來。
 *
 * <h2>為什麼不是「把第一格丟掉」</h2>
 * 丟掉最直覺，但那等於讓長選項<b>永遠不可能被翻</b>：算繪端看到的也只有視窗，
 * 查表用的就是第一格（見 {@code render.Marquee}——查到第一格之後，捲動中的
 * 每一格都沿用那一份譯文）。第一格不在語料裡，就沒有任何鍵查得到。
 *
 * <p>所以鍵照樣留第一格，整句接好之後放進 {@code full} 欄位<b>附在旁邊</b>，
 * 讓譯者知道那一格後面還有什麼。
 *
 * <h2>做法</h2>
 * 每一列各自累積。新的一格跟已經拼到的尾巴重疊夠多，就把多出來的那一段接上去。
 * 接到句尾的標點、或者捲回第一格，就算拼完。
 *
 * <p>重疊要夠長才算（{@value #MIN_OVERLAP}）——幾個字母的巧合接得上任何東西，
 * 而接錯的後果是語料裡多一句<b>看起來像真的</b>的假句子，比少一句糟得多。
 *
 * <p>兩個信號都沒出現就<b>不回報</b>：語料維持今天的樣子（只有第一格），
 * 不會多出一句拼到一半的假整句。少一份參考比多一份錯的好。
 *
 * <p>算繪端另外有一份 {@code render.Marquee}：那一份管的是「捲動中的每一格
 * 都要顯示同一份譯文」，跟這裡的「把原文接回來」是兩件事，所以沒有共用。
 */
public final class ChoiceScroll {

    /** 重疊少於這麼多字就不算接得上。 */
    private static final int MIN_OVERLAP = 12;

    /** 一句選項再長也不會超過這個長度；超過就是接錯了。 */
    private static final int MAX_LEN = 400;

    /**
     * 句子結束的標點。接到這裡就表示整句都看過了。
     *
     * <p>刻意不收引號與單引號：{@code Peloros'}、{@code "hello"} 這種會出現在
     * <b>句子中間</b>，收了會提早判定拼完，回報一句被切短的整句。
     */
    private static final String ENDS = ".!?…）)]」』";

    private static final Map<Integer, Row> ROWS = new ConcurrentHashMap<>();

    private ChoiceScroll() {}

    private static final class Row {
        String first;
        String full;
        String last;
        boolean done;
        /** 已經回報過的長度。再長出來才值得再回報一次。 */
        int reported;
    }

    /**
     * 餵一格進去。
     *
     * @param row    第幾列（選項的字型編號）
     * @param window 這一幀在那一列上看到的字
     * @return 這一格讓整句拼完（或者比上次回報的更長）時，回傳那一整句；
     *         其餘情況回傳 {@code null}
     */
    public static String feed(int row, String window) {
        if (window == null) {
            return null;
        }
        String w = window.strip();
        if (w.isEmpty()) {
            return null;
        }
        Row r = ROWS.computeIfAbsent(row, k -> new Row());
        if (r.first == null) {
            start(r, w);
            return null;
        }
        if (w.equals(r.last)) {
            return null;                       // 停著沒動
        }
        if (w.equals(r.first) && r.full.length() > r.first.length()) {
            r.done = true;                     // 捲回開頭，整句都看過了
            r.last = w;
            return report(r);
        }
        if (r.done && r.full.contains(w)) {
            // 已經知道整句了，而這一格就在裡面：跑馬燈捲第二輪。
            //
            // 這裡一定要<b>什麼都不動</b>。先前沒有這一條，第二輪的第二格會因為
            // 接不上整句而讓這一列從頭開始，{@link #first} 就變成第二格——
            // 收集端拿它當「第一格」，語料就會多一條從單字中間切開的鍵。
            //
            // 順序很重要：要排在「捲回開頭」後面。第一格本身也在整句裡面，
            // 排在前面就會把那個判定吃掉。
            r.last = w;
            return null;
        }
        String merged = merge(r.full, w);
        if (merged == null || merged.length() > MAX_LEN) {
            // 接不上：這一列換成別的選項了（換了一段對話、或選完進下一句）。
            // 手上那一句要在丟掉之前交出去——不然捲到一半被打斷就什麼都不剩。
            String pending = report(r);
            start(r, w);
            return pending;
        }
        boolean wasDone = r.done;
        r.full = merged;
        r.last = w;
        if (complete(merged)) {
            r.done = true;
        }
        // 只在「剛剛才拼完」那一刻回報。已經算拼完之後還繼續長出來的
        //（句子中間的句點造成的提早判定）等捲回開頭或換選項時再一起交。
        return !wasDone && r.done ? report(r) : null;
    }

    private static void start(Row r, String w) {
        r.first = w;
        r.full = w;
        r.last = w;
        r.done = complete(w);
        r.reported = 0;
    }

    /**
     * 拼完、而且比第一格長、而且比上次回報的長，才值得回報。
     *
     * <p>「比上次回報的長」是給提早判定留的退路：句子中間的句點會讓
     * {@link #complete} 提早成立，後面繼續捲進來的字照樣會讓 {@code full} 變長，
     * 捲回開頭（或換選項）時再回報一次。收集端那邊認前綴，長的會蓋掉短的
     *（見 {@code CaptureStore#record}）。
     */
    private static String report(Row r) {
        if (!r.done || r.full.length() <= r.first.length() || r.full.length() <= r.reported) {
            return null;
        }
        r.reported = r.full.length();
        return r.full;
    }

    /**
     * 這一列拼完的整句。
     *
     * @return 整句；還沒拼完、或者整句就等於第一格（本來就沒在捲）回傳 {@code null}
     */
    public static String full(int row) {
        Row r = ROWS.get(row);
        if (r == null || !r.done || r.full.length() <= r.first.length()) {
            return null;
        }
        return r.full;
    }

    /**
     * 這一列的第一格。
     *
     * <p>語料的鍵就是它，所以收集端要靠這個分辨「第一格」跟「捲到一半的視窗」
     * ——見 {@code ActionBarListener#collect}。
     */
    public static String first(int row) {
        Row r = ROWS.get(row);
        return r == null ? null : r.first;
    }

    /** 對話結束、或選項整組換掉就清空。 */
    public static void reset() {
        ROWS.clear();
    }

    /** 拼到句尾的標點就算整句都看過了。 */
    private static boolean complete(String s) {
        return !s.isEmpty() && ENDS.indexOf(s.charAt(s.length() - 1)) >= 0;
    }

    /**
     * {@code w} 接在 {@code full} 後面，重疊的部分只算一次。
     *
     * <p>從最長的重疊試起：跑馬燈一次只捲一兩個字，所以重疊幾乎是整個視窗，
     * 短的重疊反而是巧合。
     *
     * @return 接起來的字；接不上回傳 {@code null}
     */
    static String merge(String full, String w) {
        int max = Math.min(full.length(), w.length());
        for (int k = max; k >= MIN_OVERLAP; k--) {
            if (full.regionMatches(full.length() - k, w, 0, k)) {
                return full + w.substring(k);
            }
        }
        return null;
    }
}
