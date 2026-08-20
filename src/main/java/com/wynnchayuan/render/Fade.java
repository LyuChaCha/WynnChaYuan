package com.wynnchayuan.render;

/**
 * 停留時間結束後的淡出。
 *
 * <h2>為什麼不直接消失</h2>
 * 對話框本來就是「看完就走」的東西，時間一到啪一聲不見，眼角餘光會以為
 * 畫面閃了一下——尤其它就在視線中央下方。淡出幾百毫秒就不會有那種突兀感。
 *
 * <h2>換內容時不淡</h2>
 * NPC 講下一句時，計時會重新開始，透明度直接回到全滿。那是「換內容」不是
 * 「結束」，中間插一段淡出反而像卡頓。
 *
 * <p>這件事不需要額外的狀態——透明度是從「距離上次更新多久」算出來的，
 * 所以更新時間一被推後，透明度自然就回到 1。
 */
public final class Fade {

    /**
     * 淡出要花多久。
     *
     * <p>350 毫秒：短到不會讓人等，長到看得出是「淡掉」而不是「閃掉」。
     * 做成常數而不是設定，是因為這個值沒有人會想調——會想調的是停留時間，
     * 那個本來就在設定裡。
     */
    private static final long DURATION_MS = 350;

    private Fade() {}

    /**
     * 現在該用多少透明度畫。
     *
     * @param lastUpdate 內容最後一次更新的時間
     * @param holdMs     停留多久才開始淡出；{@link Integer#MAX_VALUE} 代表不消失
     * @return 1 表示全不透明，0 表示已經完全消失（呼叫端可以順手清掉內容）
     */
    public static float alphaFor(long lastUpdate, int holdMs) {
        if (holdMs == Integer.MAX_VALUE) {
            return 1.0f;
        }
        long elapsed = System.currentTimeMillis() - lastUpdate;
        if (elapsed <= holdMs) {
            return 1.0f;
        }
        long into = elapsed - holdMs;
        if (into >= DURATION_MS) {
            return 0.0f;
        }
        return 1.0f - (float) into / DURATION_MS;
    }
}
