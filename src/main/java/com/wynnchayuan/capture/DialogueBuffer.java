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
 * <p>比對前必須先移除 PUA 字元——打字時尾端會帶一個游標圖示，
 * 它會讓 {@code "H-hey"} 和 {@code "H-hey,"} 的前綴關係對不上。
 */
public final class DialogueBuffer {

    /** 內容多久沒變就視為打完。 */
    private static final long SETTLE_MS = 1500;

    private String pending = "";
    private long lastChange = 0;

    /**
     * 餵進一段新內容。
     *
     * @return 已經打完、可以記錄的上一句；還在打字中則回傳 {@code null}
     */
    public synchronized String offer(String text) {
        if (text == null) {
            return null;
        }
        String clean = text.strip();
        if (clean.isEmpty()) {
            return null;
        }
        if (clean.equals(pending)) {
            return null;                       // 完全沒變，忽略
        }
        if (pending.isEmpty() || clean.startsWith(pending)) {
            pending = clean;                   // 還在打字，繼續累積
            lastChange = System.currentTimeMillis();
            return null;
        }
        // 不是延伸 → 換了新的一句，把上一句送出
        String done = pending;
        pending = clean;
        lastChange = System.currentTimeMillis();
        return done;
    }

    /** 對話結束時呼叫，把緩衝區內容送出。 */
    public synchronized String flush() {
        if (pending.isEmpty()) {
            return null;
        }
        String done = pending;
        pending = "";
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
