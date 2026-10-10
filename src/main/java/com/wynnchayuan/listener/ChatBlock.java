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

    /**
     * @param keep 這一列<b>不翻</b>，照原文送回去——夾著別人名字的伺服器訊息、
     *             別的模組要讀的那幾條。只有被攔下來的列會帶這個旗標：沒被攔的
     *             那一種，原文本來就自己顯示了。
     */
    private record Row(StyledText original, Component translated, boolean keep) {}

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
        pending.add(new Row(original, translated, false));
        last = System.currentTimeMillis();
    }

    /**
     * 攢著的這幾行，原文<b>已經被攔下來、沒有顯示</b>。
     *
     * <h2>就地取代模式的多欄面板</h2>
     * 就地取代是一則訊息當場換一則，而 Lootrun 的信標面板是<b>一列一則</b>送來的：
     * 每一列只看得到自己，於是多欄的列照欄置中、單欄的接續列原地不動（兩者錯開），
     * 伺服器折好行的敘述也只能一列一列查到半句（「+6 場挑戰以／本次 Lootrun。獲得／
     * 完成它們不會／獲得時間獎勵。」）。原文加譯文那個模式沒有這個問題，因為它本來
     * 就等整塊跳完才一起算（{@link #stacked}）。使用者 2026-10-10：「又遇到老問題，
     * 字不會置中對齊 (lootrun)」——他用的正是就地取代。
     *
     * <p>所以就地取代遇到多欄的列時，從那一列起把伺服器訊息<b>攔下來</b>
     * （{@code ChatMessageEvent.Match#cancelChat}，見 {@code ChatListener#onMatch}），
     * 攢到安靜下來再整塊翻、整塊送——走的是跟原文加譯文同一支 {@link #stacked}，
     * 差別只在原文那一份不顯示。
     *
     * <p>攔下來的東西<b>一定要送出去</b>：一行都沒翻到也要把原文原樣補回畫面，
     * 不然伺服器送來的內容就憑空消失了。見 {@link #compose}。
     */
    private static boolean held = false;

    /**
     * 攔下一列。見 {@link #held}。
     *
     * @param keep 這一列照原文送回去、不要翻（見 {@link Row}）
     */
    public static synchronized void hold(StyledText original, boolean keep) {
        long now = System.currentTimeMillis();
        // 上一塊早就該送了卻還攢著（HUD 沒在畫的時候 tick 不會來）：先放它走，
        // 別讓新來的這一列接在一塊過期的面板後面。
        if (pending.size() >= MAX_ROWS || ready(now)) {
            flush();
        }
        if (!held || pending.isEmpty()) {
            heldSince = now;
        }
        pending.add(new Row(original, null, keep));
        held = true;
        last = now;
    }

    /**
     * 最多攔多久。
     *
     * <p>「安靜下來」是等不到的時候：面板後面緊跟著一連串伺服器訊息（戰鬥中每幾十毫秒
     * 一則），每來一則就重新計時，面板會一直出不來。信標面板整塊是同一個 tick 送完的，
     * 六百毫秒遠遠夠。
     */
    private static final long HOLD_MAX_MS = 600;

    private static long heldSince;

    /** 假裝是那個時間點開始攔的，測試用。 */
    static synchronized void heldAt(long when) {
        heldSince = when;
    }

    /** 現在是不是正在攔一塊面板——是的話後面跟著來的列也要一起攔，順序才不會亂。 */
    public static synchronized boolean holding() {
        return held && !pending.isEmpty();
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
        if (pending.isEmpty()) {
            return false;
        }
        return now - last >= IDLE_MS || (held && now - heldSince >= HOLD_MAX_MS);
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
        held = false;
        mine.clear();
        sentAt.clear();
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

    /**
     * 現在正在把攢著的那一則送出去。
     *
     * <h2>實機回報（2026-10-10）：「信標直接不出來了」</h2>
     * 就地取代模式攔下信標面板、整塊翻好送出去之後，那一則<b>又被自己攔回來</b>：
     * 它一樣是多欄的列（{@code LineTranslator#panelRow}），而「是不是自己送的」
     * 沒認出來。攔下來的東西一定會再送，於是每兩百毫秒重送一次、永遠到不了畫面
     * ——那一場的 log 裡同一塊面板重送了一千三百多次，期間別的伺服器訊息也被捲進去。
     *
     * <p>沒認出來是因為兩邊拿來比的字不是同一種：記下來的是
     * {@code Component#getString}（純文字），事件那邊拿的是
     * {@code StyledText#getString}——<b>帶顏色碼</b>的。沒有顏色的訊息兩者剛好
     * 相同，所以原文加譯文那個模式一直沒事（認不出來也只是白查一次表）；面板的
     * 信標名稱是粗體加顏色，就對不上了。
     *
     * <p>所以改成兩道：
     * <ol>
     *   <li>送的那一刻掛這個旗標。{@code displayClientMessage} 是<b>同步</b>觸發
     *       聊天事件的（Wynntils 的 {@code ChatHandler} 當場發 Match 與 Edit），
     *       旗標掛著的時候進來的就是我們自己那一則，跟內容長什麼樣無關；</li>
     *   <li>內容那一道照舊留著（萬一哪個模組把聊天延後處理），但兩邊都比
     *       <b>去掉樣式的字</b>，而且只記幾秒。</li>
     * </ol>
     */
    private static boolean sending = false;

    /** 內容那一道記多久。事件是當場回來的，幾秒只是給延後處理的模組留餘地。 */
    private static final long MINE_TTL_MS = 5_000;

    /** {@link #mine} 裡每一則是什麼時候送的。 */
    private static final java.util.Map<String, Long> sentAt = new java.util.HashMap<>();

    /** 這一則是不是我們自己剛送出去的譯文。見 {@link #sending}。 */
    public static synchronized boolean isOurs(StyledText message) {
        if (message == null) {
            return false;
        }
        return sending || isOurs(message.getStringWithoutFormatting());
    }

    /** 內容那一道：比的是去掉樣式的字。見 {@link #sending}。 */
    static synchronized boolean isOurs(String plain) {
        if (plain == null) {
            return false;
        }
        String key = bare(plain);
        if (!mine.contains(key)) {
            return false;
        }
        Long when = sentAt.get(key);
        return when != null && System.currentTimeMillis() - when <= MINE_TTL_MS;
    }

    /** 去掉樣式碼之後的字。兩邊都過這一道，比的才是同一種東西。 */
    private static String bare(String text) {
        return text.indexOf('\u00a7') < 0 ? text : STYLE_CODE.matcher(text).replaceAll("");
    }

    private static final java.util.regex.Pattern STYLE_CODE =
            java.util.regex.Pattern.compile("\u00a7(?:#[0-9a-fA-F]{8}|\\{[^}]*}|\\[[^\\]]*]|.)");

    /** 記下「這一則是我們送的」。測試也從這裡進來（{@link #flush} headless 跑不了）。 */
    static synchronized void remember(Component sent) {
        String key = bare(sent.getString());
        mine.add(key);
        sentAt.put(key, System.currentTimeMillis());
        sentAt.keySet().retainAll(mine);
    }

    private static void flush() {
        List<Row> rows = new ArrayList<>(pending);
        boolean wasHeld = held;
        pending.clear();
        held = false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        Component whole = compose(rows, wasHeld, WynnChaYuan.translations());
        if (whole != null) {
            // 先記下來再送：displayClientMessage 會同步再觸發一次聊天事件，
            // 記晚了就來不及擋。見 #mine 與 #sending。
            remember(whole);
            sending = true;
            try {
                mc.player.displayClientMessage(whole, false);
            } finally {
                sending = false;
            }
        }
    }

    /**
     * 這一塊要送出去的樣子。
     *
     * @param wasHeld 原文被攔下來了（見 {@link #held}）：就算一行都沒翻到，也要把原文
     *                原樣送回去
     * @return 不必送的時候回 {@code null}（原文自己會顯示，而且沒有東西可翻）
     */
    static Component compose(List<Row> rows, boolean wasHeld,
                             com.wynnchayuan.translate.TranslationStore store) {
        Component whole = rows.size() > 1 ? asBlock(rows, store) : null;
        if (whole == null) {
            whole = stacked(rows, wasHeld, store);
        }
        return whole;
    }

    /** 測試用：現在攢著的這幾行會送出什麼。不清掉攢著的東西。 */
    static synchronized Component preview(com.wynnchayuan.translate.TranslationStore store) {
        return compose(new ArrayList<>(pending), held, store);
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
    private static Component asBlock(List<Row> rows,
                                     com.wynnchayuan.translate.TranslationStore store) {
        try {
            net.minecraft.network.chat.MutableComponent joined = Component.empty();
            for (int i = 0; i < rows.size(); i++) {
                if (i > 0) {
                    joined.append(Component.literal("\n"));
                }
                joined.append(rows.get(i).original().getComponent());
            }
            return LineTranslator.translateChat(StyledText.fromComponent(joined),
                                                store);
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
    private static Component stacked(List<Row> rows, boolean wasHeld,
                                     com.wynnchayuan.translate.TranslationStore store) {
        // 只有一則的時候<b>不能</b>先算對齊。
        //
        // 「洞穴完成」那一塊是六行擠在一則訊息裡，這裡拿到的是<b>一個</b>
        // 含換行的 StyledText。把它交給 chatCentred 等於問「這六行合起來
        // 算不算置中」，然後拿那一個答案去套六行。
        //
        // 交給 translateChat 自己拆行判斷才對——它看得到裡面有幾行。
        boolean[] centred = null;
        // 這一塊是不是「兩欄併排的面板」。跟置中一樣要<b>整塊一起看</b>：
        // 信標面板是一行一則訊息送來的，逐行重譯時每一行只看得到自己，
        // 於是多欄的行照欄置中、單欄的接續行卻靠左不動，兩者就錯開。
        // 見 LineTranslator#chatPanel。
        boolean panel = false;
        if (rows.size() > 1) {
            List<StyledText> originals = new ArrayList<>(rows.size());
            for (Row row : rows) {
                originals.add(row.original());
            }
            try {
                centred = LineTranslator.chatCentred(originals);
                panel = LineTranslator.chatPanel(originals);
            } catch (Throwable t) {
                centred = null;
                panel = false;
            }
        }
        // 面板的每一欄先試「折成好幾列的一句話併起來翻」；併不起來的列是 null，
        // 底下照舊逐列翻。見 LineTranslator#flowPanel。
        Component[] flowed = null;
        if (panel) {
            try {
                List<StyledText> originals = new ArrayList<>(rows.size());
                for (Row row : rows) {
                    originals.add(row.original());
                }
                flowed = LineTranslator.flowPanel(originals, store, centred);
            } catch (Throwable t) {
                flowed = null;                 // 併句出事不能拖累逐列那條路
            }
        }
        net.minecraft.network.chat.MutableComponent out = Component.empty();
        boolean any = false;
        // 「有沒有東西可送」與「有沒有真的翻到」是兩件事。
        //
        // 查不到譯文的行改成原樣帶上之後，any 就永遠是 true——於是<b>每一塊</b>
        // 聊天訊息都會被原封不動再送一次。原文那一份並沒有被取消，畫面上就是
        // 每則訊息都出現兩遍（實機回報：聊天欄一直洗頻）。
        //
        // 所以要另外記「有沒有任何一行真的查到譯文」。一行都沒有的話這一塊
        // 根本不必送，原文自己會顯示。
        boolean translated = false;
        for (int i = 0; i < rows.size(); i++) {
            // 只有一則時收進來那份就是對的（ChatListener 走的是同一支），
            // 不必再翻一次——翻兩次連診斷檔都會記兩份。
            Component line = centred == null ? rows.get(i).translated() : null;
            boolean keep = rows.get(i).keep();
            if (!keep && flowed != null && flowed[i] != null) {
                line = flowed[i];
            }
            if (line == null && !keep) {
                try {
                    line = LineTranslator.translateChat(rows.get(i).original(),
                                                        store,
                                                        centred == null ? null : centred[i],
                                                        panel);
                } catch (Throwable t) {
                    line = null;
                }
            }
            if (line == null) {
                line = rows.get(i).translated();
            }
            if (line == null && wasHeld && !keep) {
                // 被攔下來的列沒有經過 ChatListener 的就地取代，那邊查不到整則的鍵
                // 還會退回通用的那一支；這裡補上同一步，免得被攔的單則訊息反而少翻。
                try {
                    line = LineTranslator.translate(rows.get(i).original(), store);
                } catch (Throwable t) {
                    line = null;
                }
            }
            if (line == null) {
                // 查不到譯文就<b>原樣送出</b>，絕對不能跳過。
                //
                // 先前是 continue，那一行就這樣從畫面上消失了。玩家回報的
                // 「獵殺重抽之後信標不見了」就是這個：重抽會換出語料裡沒有的
                // 組合（單獨一個「{#}Blue Beacon」、「{#}Empower next Beacon…」），
                // 於是那幾行整個被刪掉，畫面上少了一整個信標。
                //
                // 少一句沒翻的英文，比憑空吃掉伺服器送來的內容好得多——
                // 而且「沒翻到」看得出來，「被吃掉」看不出來。
                line = rows.get(i).original().getComponent();
            } else {
                translated = true;         // 這一塊裡真的有翻到東西
            }
            if (any) {
                out.append(Component.literal("\n"));
            }
            out.append(line);
            any = true;
        }
        // 攔下來的那一種沒有「原文自己會顯示」這回事，有東西就得送
        return (wasHeld ? any : worthSending(any, translated)) ? out : null;
    }

    /**
     * 這一塊該不該送出去。
     *
     * <h2>為什麼是兩個條件</h2>
     * 「有沒有東西可送」與「有沒有真的翻到」是兩件事，而混為一談會出大事。
     *
     * <p>查不到譯文的行原本是直接跳過的（信標因此消失）。改成原樣帶上之後，
     * {@code any} 就永遠是 true——於是<b>每一塊</b>聊天訊息都被原封不動再送
     * 一次。原文那一份並沒有被取消，畫面上就是每則訊息出現兩遍。實機回報是
     * 「聊天欄一直洗頻」。
     *
     * <p>抽成獨立的方法是為了測得到：{@link #stacked} 會碰到 Minecraft 的
     * 實例，headless 跑不起來，而真正出錯的就是這個判斷。
     *
     * @param any        有沒有任何一行可以送（含原樣帶上的原文）
     * @param translated 有沒有任何一行<b>真的</b>查到譯文
     */
    static boolean worthSending(boolean any, boolean translated) {
        return any && translated;
    }
}
