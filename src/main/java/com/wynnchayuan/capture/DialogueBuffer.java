package com.wynnchayuan.capture;

/**
 * 把逐字打出來的對話收斂成一句完整的話。
 *
 * <h2>問題</h2>
 * Wynncraft 的對話是一個字一個字打出來的，而 Wynntils 每打一個字就發一次事件。
 * 直接記錄會得到一堆前綴碎片：
 *
 * <pre>
 *   H-hey        H-hey,       H-hey, h     H-hey, hu    ...
 *   H-hey, human- URGH! Help me out here! Some damn guards beat me bloody...
 * </pre>
 *
 * 只有最後一行有價值，前面全是垃圾。
 *
 * <h2>做法</h2>
 * 打字過程產生的是<b>嚴格遞增的前綴</b>，所以只要判斷「新內容是不是舊內容的延伸」就好：
 * 是延伸就繼續累積，不是就代表上一句已經打完、可以送出。
 *
 * <p>另外對話結束（{@code Ended}）或停止變動超過 {@link #SETTLE_MS} 時也會送出，
 * 避免最後一句卡在緩衝區裡出不來。
 *
 * <h2>比對的是原文，不是模板</h2>
 * 這裡曾經拿<b>模板</b>比前綴，結果收到的幾乎每一句都是半截的
 * （{@code "Of course, the King of Ragn"}、{@code "Hey, Green_teaT"}）。
 *
 * <p>原因是模板在打字過程中<b>不是單調遞增的</b>。參數化要比對到完整的詞才會命中：
 *
 * <pre>
 *   打到一半  "Hey, Green_teaT"    → 名字還不完整，比不到 → 模板是字面
 *   打完      "Hey, Green_teaTea"  → 比到了              → 模板變成 "Hey, {u}"
 * </pre>
 *
 * {@code "Hey, {u}"} 不是 {@code "Hey, Green_teaT"} 的延伸，於是被當成
 * 「換了新的一句」，半截的那份就這樣被送進 {@code captured.json}。
 * 地名同理：{@code Ragn} → {@code Ragni} → {@code {p}}。
 *
 * <p>這同時說明了為什麼玩家名字會外流到共享檔案：{@code {u}} 之所以沒生效，
 * 正是因為送出去的那一份還沒打完整。
 *
 * <p>所以改成比對<b>畫面上的原文</b>，並且忽略空白與圖示：原文在打字過程中
 * 是嚴格遞增的，而忽略空白讓對話框<b>重新換行</b>時也不會誤判（換行會讓排版
 * 偏移插進字串中間）。送出去的仍然是模板。
 */
public final class DialogueBuffer {

    /** 內容多久沒變就視為打完。 */
    private static final long SETTLE_MS = 1500;

    /** 拿來比前綴的形狀：畫面原文去掉空白與圖示。 */
    private String shape = "";

    /** 真正要送出去的東西：模板。 */
    private String pending = "";

    private long lastChange = 0;

    /**
     * 餵進一段新內容。
     *
     * @param raw      畫面上的原文，用來判斷「打完了沒」
     * @param template 參數化之後的模板，這才是要記錄的東西
     * @return 已經打完、可以記錄的上一句；還在打字中則回傳 {@code null}
     */
    public synchronized String offer(String raw, String template) {
        if (raw == null || template == null) {
            return null;
        }
        String next = shape(raw);
        String clean = trimCursor(template);
        if (next.isEmpty() || clean.isEmpty()) {
            return null;
        }
        if (next.equals(shape)) {
            // 形狀沒變，但模板可能剛好在這一幀完成參數化（名字終於打完）。
            // 留下比較新的那一份，它才是參數化過的。
            pending = clean;
            return null;
        }
        if (shape.isEmpty() || next.startsWith(shape)) {
            shape = next;                      // 還在打字，繼續累積
            pending = clean;
            lastChange = System.currentTimeMillis();
            return null;
        }
        // 不是延伸 → 換了新的一句，把上一句送出
        String done = pending;
        shape = next;
        pending = clean;
        lastChange = System.currentTimeMillis();
        return done;
    }

    /**
     * 前綴比對用的形狀：去掉空白與圖示。
     *
     * <h2>為什麼兩樣都要去掉</h2>
     * 圖示：打字時尾端帶一個游標，{@code "H-hey"} 與 {@code "H-hey,"} 之間
     * 夾著它，前綴關係對不上。
     *
     * <p>空白：對話框放不下就<b>重新換行</b>，換行點會往前移，字串中間因此
     * 多出或少掉空白與排版偏移。整句話明明只是多打了一個字，前綴關係卻斷了。
     */
    private static String shape(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        raw.codePoints()
           .filter(cp -> !Character.isWhitespace(cp))
           .filter(cp -> !GlyphSplitter.isGlyphCodePoint(cp))
           .forEach(out::appendCodePoint);
        return out.toString();
    }

    /**
     * 拿掉尾端的打字游標。
     *
     * <h2>為什麼要做</h2>
     * 打字時尾端每一幀都帶一個游標圖示，而呼叫端送進來的是<b>模板</b>——
     * 圖示已經被換成字面上的 {@code {#}}。不清掉的話，記進 {@code captured.json}
     * 的每一句尾端都會多一個憑空出現的 {@code {#}}，那條 key 因此永遠對不到。
     *
     * <p>前綴比對不需要這個（{@link #shape} 本來就會濾掉圖示），
     * 這裡純粹是為了讓<b>存下來的那一份</b>乾淨。
     */
    private static String trimCursor(String text) {
        String out = text;
        while (true) {
            String was = out;
            if (out.endsWith(GlyphSplitter.GLYPH_PLACEHOLDER)) {
                out = out.substring(0,
                        out.length() - GlyphSplitter.GLYPH_PLACEHOLDER.length());
            } else if (!out.isEmpty() && GlyphSplitter.isGlyphCodePoint(
                    out.codePointBefore(out.length()))) {
                out = out.substring(0, out.length()
                        - Character.charCount(out.codePointBefore(out.length())));
            }
            out = out.stripTrailing();
            if (out.equals(was)) {
                return out.strip();
            }
        }
    }

    /** 對話結束時呼叫，把緩衝區內容送出。 */
    public synchronized String flush() {
        if (pending.isEmpty()) {
            return null;
        }
        String done = pending;
        pending = "";
        shape = "";
        lastChange = 0;
        return done;
    }

    /** 內容已經穩定超過 {@link #SETTLE_MS} 就送出，否則回傳 null。 */
    public synchronized String flushIfSettled() {
        if (pending.isEmpty() || System.currentTimeMillis() - lastChange < SETTLE_MS) {
            return null;
        }
        return flush();
    }
}
