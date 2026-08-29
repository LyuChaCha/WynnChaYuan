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
        mine.clear();
    }

    /**
     * 我們自己剛送出去的那幾則。
     *
     * <h2>為什麼要記</h2>
     * {@code displayClientMessage} 會<b>再觸發一次聊天事件</b>，於是我們送出去的
     * 譯文又被當成新訊息收回來、再翻一次。診斷檔裡因此出現整段中文被拿去查表：
     *
     * <pre>
     *   === 聊天對齊 5 ===（沒翻到：語料裡查不到這一塊）
     *     原文：󐁙§6§l歡迎來到 Wynncraft！…
     * </pre>
     *
     * <p>白費力氣還算小事，真正的問題是它會跟後面真正的新訊息<b>攢成同一塊</b>
     * ——診斷檔的「聊天對齊 4」就是三則不相干的訊息被接在一起。
     */
    private static final java.util.Set<String> mine =
            java.util.Collections.newSetFromMap(
                    new java.util.LinkedHashMap<>() {
                        @Override
                        protected boolean removeEldestEntry(
                                java.util.Map.Entry<String, Boolean> eldest) {
                            return size() > MINE_MEMORY;
                        }
                    });

    /** 記得自己送過的最後幾則就夠了——事件是同一個 tick 回來的。 */
    private static final int MINE_MEMORY = 32;

    /** 這一則是不是我們自己剛送出去的譯文。 */
    public static synchronized boolean isOurs(String message) {
        return message != null && mine.contains(message);
    }

    private static synchronized void remember(Component sent) {
        mine.add(sent.getString());
    }

    private static void flush() {
        List<Row> rows = new ArrayList<>(pending);
        pending.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        Component whole = rows.size() > 1 ? asBlock(rows) : null;
        if (whole == null) {
            whole = stacked(rows);
        }
        if (whole != null) {
            // 先記下來再送：displayClientMessage 會同步再觸發一次聊天事件，
            // 記晚了就來不及擋。見 #mine。
            remember(whole);
            mc.player.displayClientMessage(whole, false);
        }
    }

    /**
     * 把攢著的幾行接成一整塊，重查一次。
     *
     * <p>語料裡那一條本來就是整塊的（「{@code [Cave Completed]\n…\n- Rewards:\n…}」），
     * 逐行查當然查不到。接起來查得到的話，顏色、縮排、置中都是照整塊算的，
     * 比六句各自為政準得多。
     *
     * <h2>只認整塊那一條</h2>
     * 走的是 {@link LineTranslator#translateChat}，它<b>只</b>查整塊的鍵。
     * 先前用的是通用的 {@code translate}，查不到整塊時它會退到逐片段替換——
     * 那條路幾乎一定回傳「有翻到一點點」的結果，於是整塊就被那份半吊子佔住，
     * 逐行查到的好譯文反而全部被丟掉。實機那張「任務完成」的圖裡，
     * 四行獎勵有三行是英文、一行是中文，就是這樣來的。
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
            return LineTranslator.translateChat(StyledText.fromComponent(joined),
                                                WynnChaYuan.translations());
        } catch (Throwable t) {
            return null;              // 整塊查表出事也不能讓逐行那幾句跟著不見
        }
    }

    /**
     * 整塊查不到時：逐行的譯文疊成<b>一則</b>訊息。
     *
     * <p>疊成一則而不是各發各的，是為了讓它們在聊天視窗裡連在一起——
     * 中間插不進別的訊息，看起來就還是一塊。
     *
     * <p>對齊要<b>整塊一起算</b>：一行一行單獨看，分不出置中與靠左
     * （見 {@link LineTranslator#chatCentred}）。所以先問過整塊，再把答案
     * 一行一行傳下去重譯一次；重譯不到的才用收進來時那份。
     */
    private static Component stacked(List<Row> rows) {
        // 只有一則的時候<b>不能</b>先算對齊。
        //
        // 「洞穴完成」那一塊是六行擠在一則訊息裡，這裡拿到的是<b>一個</b>
        // 含換行的 StyledText。把它交給 chatCentred 等於問「這六行合起來
        // 算不算置中」，然後拿那一個答案去套六行。
        //
        // 交給 translateChat 自己拆行判斷才對——它看得到裡面有幾行。
        boolean[] centred = null;
        if (rows.size() > 1) {
            List<StyledText> originals = new ArrayList<>(rows.size());
            for (Row row : rows) {
                originals.add(row.original());
            }
            try {
                centred = LineTranslator.chatCentred(originals);
            } catch (Throwable t) {
                centred = null;
            }
        }
        net.minecraft.network.chat.MutableComponent out = Component.empty();
        boolean any = false;
        for (int i = 0; i < rows.size(); i++) {
            // 只有一則時收進來那份就是對的（ChatListener 走的是同一支），
            // 不必再翻一次——翻兩次連診斷檔都會記兩份。
            Component line = centred == null ? rows.get(i).translated() : null;
            if (line == null) {
                try {
                    line = LineTranslator.translateChat(rows.get(i).original(),
                                                        WynnChaYuan.translations(),
                                                        centred == null ? null : centred[i]);
                } catch (Throwable t) {
                    line = null;
                }
            }
            if (line == null) {
                line = rows.get(i).translated();
            }
            if (line == null) {
                continue;                 // 這一行沒有譯文，跳過
            }
            if (any) {
                out.append(Component.literal("\n"));
            }
            out.append(line);
            any = true;
        }
        return any ? out : null;
    }
}
