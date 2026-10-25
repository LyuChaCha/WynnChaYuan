package com.wynnchayuan.capture;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.Optional;

/**
 * 對話框底下那條進度條：這段對話講到第幾句。
 *
 * <h2>實機看到的（test2 的 dialogue-start.txt，King's Recruit 開場 17 句）</h2>
 * 字型 {@code hud/dialogue/effect/progress}，U+E000…U+E0DF 共 224 格，值是
 * 「223 × 第 n 句 ÷ 共 m 句」：三句的對話是 E04B／E095／E0DF（1/3、2/3、3/3），
 * 六句的是 E025、E04B、E06F、E095、E0BB、E0DF。
 *
 * <p>它是<b>動畫</b>：換句的第一幀還是上一句的值，接著幾幀滑向新的值
 * （新的一段對話從上一段的滿格滑回來，所以會看到 E071→E04B、E054→E025 這種往下走的）。
 * 所以第一幀拿不到這一句的 n/m；拿得到的是<b>上一句停下來時</b>的值——
 * 沒滿格就表示那段對話還沒講完，這一句一定是它的下一句。見 {@code DialogueForecast}。
 */
public final class DialogueProgress {

    /** 滿格。 */
    public static final int FULL = 0xDF;

    private static final String FONT = "hud/dialogue/effect/progress";

    /** 連續幾則訊息同一個值才算停下來（動畫走完）。 */
    private static final int STEADY = 3;

    private static volatile int value = -1;
    private static volatile int same = 0;

    private DialogueProgress() {}

    /** 每一則原始 action bar 都餵（跟 {@link DialogueSpeaker#noteRaw} 同一個地方）。 */
    public static void noteRaw(Component message) {
        if (message == null) {
            return;
        }
        int[] found = {-1};
        message.visit((style, part) -> {
            if (style != null && style.getFont() != null
                    && style.getFont().toString().contains(FONT)) {
                part.codePoints().filter(cp -> cp >= 0xE000 && cp <= 0xE000 + FULL)
                        .forEach(cp -> found[0] = cp - 0xE000);
            }
            return Optional.empty();
        }, Style.EMPTY);
        set(found[0]);
    }

    /** 測試用，也給 {@link #noteRaw} 用。 */
    public static void set(int now) {
        if (now == value) {
            same++;
        } else {
            value = now;
            same = 0;
        }
    }

    /** 測試用：直接指定一個已經停下來的值。 */
    public static void settle(int now) {
        value = now;
        same = STEADY;
    }

    /** 第幾段對話。對話框收掉一次就加一。 */
    private static volatile int conversation;

    /**
     * 對話框收掉了（講完、被打斷、走開）。之後的「上一句還沒講完」都不算數：
     * 玩家半途走開去跟別人講話時，不能把新的那段當成舊的那段的下一句。
     */
    public static void endConversation() {
        conversation++;
        value = -1;
        same = 0;
    }

    /** @return 現在是第幾段對話（見 {@link #endConversation}） */
    public static int conversation() {
        return conversation;
    }

    /** @return 進度條停下來時的值；還在動或沒有進度條時回傳 -1 */
    public static int settled() {
        return same >= STEADY ? value : -1;
    }
}
