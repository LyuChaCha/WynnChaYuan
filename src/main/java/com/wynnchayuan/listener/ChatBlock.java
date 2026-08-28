package com.wynnchayuan.listener;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.LineTranslator;
import com.wynntils.core.text.StyledText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次跳好幾行的伺服器訊息，譯文等它跳完再一起出來。
 *
 * <h2>畫面上長什麼樣</h2>
 * 任務完成的獎勵清單是<b>一行一則</b>聊天訊息送過來的。每一則各自接一句譯文，
 * 玩家看到的就是中英交錯：
 *
 * <pre>
 *   [Quest Completed]
 *   [任務完成]
 *   King's Recruit
 *   國王的新兵
 *   Rewards:
 *   獎勵:
 * </pre>
 *
 * 原文那一塊是照英文寬度排版的（置中、縮排都算好了），中間插進中文之後
 * 整塊就散了。使用者的要求是「等它全部跳完，再跳翻譯」。
 *
 * <h2>怎麼做</h2>
 * 譯文一律<b>先攢著</b>，等聊天安靜下來（見 {@link #IDLE_MS}）再一起送。
 * 單獨一則訊息看起來跟以前一樣，只是晚了十分之二秒；一整塊的則會是
 * 六行原文跳完、六行譯文再跟上。
 *
 * <p>攢起來還有第二個好處：<b>整塊一起查表</b>。
 * 「{@code - Rewards:}」單獨一行查不到，接成一整塊就對得上語料裡那一條
 * ——顏色與縮排也就跟著對了。查不到才退回逐行的那幾句。
 *
 * <h2>為什麼不在事件裡直接等</h2>
 * {@code ChatMessageEvent.Edit} 是同步的：那一則訊息當下就要決定改成什麼。
 * 要等後面還有沒有，只能讓原文照常跳出去、譯文另外補一則。
 */
public final class ChatBlock {

    private ChatBlock() {}

    /**
     * 安靜多久算「跳完了」。
     *
     * <p>整塊訊息是同一個封包送來的，彼此相隔不到一個 tick。兩百毫秒比那長得多，
     * 又短到玩家感覺不出來——單獨一則訊息的譯文晚這麼一下看不出差別。
     */
    private static final long IDLE_MS = 200;

    /** 一塊最多攢幾行。再多就不是「一塊訊息」，是聊天在洗版。 */
    private static final int MAX_ROWS = 24;

    private record Row(StyledText original, Component translated) {}

    private static final List<Row> pending = new ArrayList<>();
    private static long last;

    /**
     * 收一行。
     *
     * @param original   伺服器原本送來的那一行（用來整塊重查一次）
     * @param translated 逐行查到的譯文；查不到就是 {@code null}
     */
    public static synchronized void queue(StyledText original, Component translated) {
        if (pending.size() >= MAX_ROWS) {
            flush();
        }
        pending.add(new Row(original, translated));
        last = System.currentTimeMillis();
    }

    /** 每一幀問一次：安靜夠久了就把攢著的送出去。由 HUD 的算繪路徑呼叫。 */
    public static synchronized void tick() {
        if (ready(System.currentTimeMillis())) {
            flush();
        }
    }

    /**
     * 該送出去了嗎。抽出來是為了讓「等多久」這件事測得到——
     * {@link #flush} 會碰到 Minecraft 的實例，headless 測不了。
     */
    static synchronized boolean ready(long now) {
        return !pending.isEmpty() && now - last >= IDLE_MS;
    }

    /** 目前攢了幾行，測試用。 */
    static synchronized int size() {
        return pending.size();
    }

    /** 假裝最後一行是那個時間點收到的，測試用。 */
    static synchronized void arrivedAt(long when) {
        last = when;
    }

    /** 換伺服器、關掉功能時清掉，免得下一塊沾到上一塊的尾巴。 */
    public static synchronized void clear() {
        pending.clear();
    }

    private static void flush() {
        List<Row> rows = new ArrayList<>(pending);
        pending.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        Component whole = rows.size() > 1 ? asBlock(rows) : null;
        if (whole != null) {
            mc.player.displayClientMessage(whole, false);
            return;
        }
        for (Row row : rows) {
            if (row.translated() != null) {
                mc.player.displayClientMessage(row.translated(), false);
            }
        }
    }

    /**
     * 把攢著的幾行接成一整塊，重查一次。
     *
     * <p>語料裡那一條本來就是整塊的（「{@code [Cave Completed]\n…\n- Rewards:\n…}」），
     * 逐行查當然查不到。接起來查得到的話，顏色、縮排、置中都是照整塊算的，
     * 比六句各自為政準得多。
     *
     * @return 整塊的譯文；查不到就回傳 {@code null}，讓呼叫端退回逐行那幾句
     */
    private static Component asBlock(List<Row> rows) {
        try {
            net.minecraft.network.chat.MutableComponent joined = Component.empty();
            for (int i = 0; i < rows.size(); i++) {
                if (i > 0) {
                    joined.append(Component.literal("\n"));
                }
                joined.append(rows.get(i).original().getComponent());
            }
            return LineTranslator.translate(StyledText.fromComponent(joined),
                                            WynnChaYuan.translations());
        } catch (Throwable t) {
            return null;              // 整塊查表出事也不能讓逐行那幾句跟著不見
        }
    }
}
