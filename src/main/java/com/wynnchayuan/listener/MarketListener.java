package com.wynnchayuan.listener;

import com.wynnchayuan.WynnChaYuan;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.ChatMessageEvent;
import com.wynntils.models.trademarket.event.TradeMarketStateEvent;
import com.wynntils.models.trademarket.type.TradeMarketState;

import net.neoforged.bus.api.EventPriority;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;

/**
 * 市集搜尋打中文，送出去之前換成英文。
 *
 * <h2>要解決什麼</h2>
 * 用「就地取代」的人，畫面上看到的是「Corkian 增幅器 III」。但市集是
 * <b>伺服器</b>在搜尋，只認得英文——照著中文打進去一筆都搜不到，
 * 得自己記住英文叫什麼。翻譯的好處到這裡就斷了。
 *
 * <h2>為什麼敢改玩家送出去的字</h2>
 * 這是整個模組唯一會<b>改寫玩家自己打的字</b>的地方，所以觸發條件不能用猜的。
 *
 * <p>Wynntils 已經在追蹤市集的狀態，並在每次變動時發 {@link TradeMarketStateEvent}。
 * 只有 {@code SEARCH_CHAT_INPUT}——也就是市集正在等你輸入搜尋字的那一刻——
 * 才會改；狀態一離開就關掉。跟朋友聊天講到「增幅器」不會被動到。
 *
 * <p>另外三道保險：
 * <ul>
 *   <li>斜線開頭的指令一律不動。</li>
 *   <li>本來就是英文的不動——玩家自己打英文時不該被「翻譯」一次。</li>
 *   <li>對到<b>兩個以上</b>英文名時不動，把候選印在聊天讓玩家自己挑。
 *       猜錯會搜到不相干的東西，玩家還會以為市集上真的沒貨。</li>
 * </ul>
 */
public final class MarketListener {

    /** 市集是不是正在等搜尋字。只有這個時候才會動玩家打的字。 */
    private static volatile boolean searching = false;

    /** 候選最多列幾個。列太多會把聊天洗掉。 */
    private static final int MAX_SHOWN = 6;

    /**
     * Wynntils 的市集狀態。<b>不能只靠它</b>——見 {@link #onChat}。
     */
    @SubscribeEvent
    public void onMarketState(TradeMarketStateEvent event) {
        if (event.getNewState() == TradeMarketState.SEARCH_CHAT_INPUT) {
            arm();
        } else if (event.getOldState() == TradeMarketState.SEARCH_CHAT_INPUT) {
            searching = false;
        }
    }

    /**
     * 自己認那句提示。
     *
     * <h2>為什麼不能只靠 Wynntils 的狀態</h2>
     * Wynntils 判斷「市集在等你輸入」靠的是比對那句提示，而且是<b>英文原文
     * 加上行尾錨點</b>：
     *
     * <pre>
     *   ^§5(圖示) Type the item name or type 'cancel' to cancel:$
     * </pre>
     *
     * 而我們自己會翻聊天。就地取代模式下那一行整個變成中文；原文加譯文模式下
     * 行尾也不再是 {@code cancel:}。兩種都會讓 Wynntils 比不到，狀態永遠不會
     * 進入搜尋輸入——<b>我們的翻譯把它自己要用的訊號弄壞了</b>。
     * 使用者回報「輸入完只顯示英文、沒有真的轉換」就是這個。
     *
     * <p>所以自己認一次。這裡拿到的是<b>還沒被我們動過</b>的原文，
     * 而且英文與中文兩種寫法都認——不管聊天翻譯開成哪一種都有效。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChat(ChatMessageEvent.Match event) {
        StyledText message = event.getMessage();
        if (message == null) {
            return;
        }
        String plain = message.getStringWithoutFormatting();
        if (PROMPT.matcher(plain).find()) {
            arm();
        } else if (CANCELLED.matcher(plain).find()) {
            searching = false;
        }
    }

    /**
     * 那句提示長什麼樣。
     *
     * <p>前面有圖示與顏色碼，所以用「找得到」而不是「整行相同」。
     * 中文那一版也收：就地取代模式下畫面上只剩中文，而這條濾網跑在
     * 翻譯前後都可能，收兩種比較保險。
     */
    private static final java.util.regex.Pattern PROMPT =
            java.util.regex.Pattern.compile(
                    "Type the item name or type 'cancel' to cancel:|輸入物品名稱");

    /** 取消或走開之後就關掉。 */
    private static final java.util.regex.Pattern CANCELLED =
            java.util.regex.Pattern.compile(
                    "chat input was canceled|聊天輸入已取消");

    /**
     * 開啟轉換，並記下時間。
     *
     * <p>有時效：提示出現之後如果玩家沒打字就跑掉了，狀態要自己失效，
     * 不能一直開著等下一句聊天被改掉。
     */
    private static void arm() {
        searching = true;
        armedAt = System.currentTimeMillis();
    }

    private static volatile long armedAt = 0;

    /** 提示出現後多久內打的字才算搜尋。 */
    private static final long WINDOW_MS = 120_000;

    /**
     * 送出去的聊天訊息。
     *
     * <p>回傳原字串代表不動。這個方法是<b>純函式</b>——狀態判斷在外面做完了，
     * 這樣才測得到。
     */
    public static String rewrite(String message) {
        if (!searching || message == null || message.isBlank()
                || message.startsWith("/")
                || System.currentTimeMillis() - armedAt > WINDOW_MS) {
            return message;
        }
        searching = false;                     // 一次性：搜尋字送出去就關掉
        try {
            return translate(message);
        } catch (Throwable t) {
            // 這個鉤子掛在<b>送出聊天</b>上。丟例外可能讓玩家的訊息整個消失，
            // 那比翻不出來嚴重得多——出任何狀況都原樣送出。
            System.err.println("[WynnChaYuan] 市集搜尋轉換失敗，原樣送出: " + t);
            return message;
        }
    }

    private static String translate(String message) {
        if (!WynnChaYuan.config().marketSearch()) {
            return message;
        }
        if (isLatin(message)) {
            return message;                    // 本來就是英文，不用翻
        }
        List<String> hits = WynnChaYuan.translations().market().candidates(message);
        if (hits.size() == 1) {
            say(Component.literal("[WynnChaYuan] 搜尋「" + message.strip() + "」→ "
                            + hits.get(0)).withStyle(ChatFormatting.GRAY));
            return hits.get(0);
        }
        if (hits.isEmpty()) {
            say(Component.literal("[WynnChaYuan] 查不到「" + message.strip()
                            + "」的英文名，原樣送出").withStyle(ChatFormatting.GRAY));
            return message;
        }
        // 對到好幾個就不猜。列出來讓玩家挑，並原樣送出——至少不會搜到別的東西。
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < Math.min(MAX_SHOWN, hits.size()); i++) {
            list.append(i > 0 ? "、" : "").append(hits.get(i));
        }
        if (hits.size() > MAX_SHOWN) {
            list.append("…（共 ").append(hits.size()).append(" 個）");
        }
        say(Component.literal("[WynnChaYuan]「" + message.strip() + "」對到好幾個，"
                        + "請改打其中一個：" + list).withStyle(ChatFormatting.YELLOW));
        return message;
    }

    /**
     * 整串都是英文字母、數字與標點。
     *
     * <p>玩家自己打英文時不該被動到——而且英文名本來就是市集認得的形式，
     * 「翻譯」它只會把對的東西弄壞。
     */
    private static boolean isLatin(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) > 0x7F) {
                return false;
            }
        }
        return true;
    }

    private static void say(Component text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            mc.player.displayClientMessage(text, false);
        }
    }

    /** 給測試用：現在會不會轉換。 */
    static boolean armed() {
        return searching;
    }

    /** 給測試用：市集狀態是外面給的，測的時候要能自己設。 */
    static void searching(boolean on) {
        searching = on;
        armedAt = on ? System.currentTimeMillis() : 0;
    }
}
