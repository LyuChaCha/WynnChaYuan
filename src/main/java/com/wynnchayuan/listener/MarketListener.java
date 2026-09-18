package com.wynnchayuan.listener;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.T;
import com.wynnchayuan.translate.MarketSearch;
import com.wynnchayuan.translate.TranslationStore;
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
     * 而且英文與譯文兩種寫法都認——不管聊天翻譯開成哪一種都有效。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onChat(ChatMessageEvent.Match event) {
        StyledText message = event.getMessage();
        if (message == null) {
            return;
        }
        String plain = message.getStringWithoutFormatting();
        if (PROMPT.matcher(plain).find() || showsTranslation(plain, PROMPT_KEY)) {
            arm();
        } else if (CANCELLED.matcher(plain).find() || showsTranslation(plain, CANCEL_KEY)) {
            searching = false;
        }
    }

    /**
     * 那句提示長什麼樣。
     *
     * <p>前面有圖示與顏色碼，所以用「找得到」而不是「整行相同」。
     * 譯文那一版也收：就地取代模式下畫面上只剩譯文，而這條濾網跑在
     * 翻譯前後都可能，收兩種比較保險。
     *
     * <p>先前只寫了繁體。簡體玩家就地取代之後看到的是「输入物品名称」，
     * 一個字都對不上，市集搜尋在簡體底下<b>整個沒有啟動</b>。
     * 寫死的只是保底，其他語言靠 {@link #showsTranslation} 去問當下的譯文。
     */
    private static final java.util.regex.Pattern PROMPT =
            java.util.regex.Pattern.compile(
                    "Type the item name or type 'cancel' to cancel:|輸入物品名稱|输入物品名称");

    /** 取消或走開之後就關掉。 */
    private static final java.util.regex.Pattern CANCELLED =
            java.util.regex.Pattern.compile(
                    "chat input was canceled|聊天輸入已取消|聊天输入已取消");

    /** 語料裡那句提示的鍵。見 {@link #showsTranslation}。 */
    static final String PROMPT_KEY =
            "{#} \n{#} Type the item name or type 'cancel' to cancel:\n{#}";

    /** 語料裡「走開所以取消」那句的鍵。 */
    static final String CANCEL_KEY = "{#} You moved and your chat input was canceled.";

    /**
     * 這一行是不是那句話<b>在目前語言的譯文</b>。
     *
     * <h2>為什麼要問語料</h2>
     * 寫死在 {@link #PROMPT} 裡的只有繁簡兩種。日文、韓文哪天把這句翻了，
     * 就地取代之後畫面上就沒有英文可比——跟簡體先前壞掉的方式一模一樣。
     * 問當下載入的譯文，哪一種語言翻了都自動跟上。
     *
     * <p>模組還沒起來（測試、啟動極早期）時什麼都不認，退回寫死的那兩種。
     */
    static boolean showsTranslation(String plain, String key) {
        TranslationStore store;
        try {
            store = WynnChaYuan.translations();
        } catch (Throwable t) {
            return false;
        }
        if (store == null || plain == null) {
            return false;
        }
        String dst = store.lookup(key);
        if (dst == null) {
            return false;
        }
        String core = core(dst);
        return core.length() >= MIN_CORE && plain.contains(core);
    }

    /**
     * 譯文裡真正的字：拿掉佔位符後，最長的那一行。
     *
     * <p>提示前後各有一行只放圖示，那兩行剝完是空的，不能拿來比。
     */
    static String core(String dst) {
        String best = "";
        for (String line : dst.replaceAll("\\{[#~pu][0-9]?\\}", "").split("\n")) {
            String s = line.strip();
            if (s.length() > best.length()) {
                best = s;
            }
        }
        return best;
    }

    /** 太短的譯文不拿來比：一兩個字到處都會出現，一般聊天就會誤觸。 */
    private static final int MIN_CORE = 4;

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
        MarketSearch market = WynnChaYuan.translations().market();
        String typed = message.strip();
        if (isLatin(message)) {
            // 本來就是英文的不該被「翻譯」一次。但法文、德文、西文的譯名也可能
            // 整串都是 ASCII，一律跳過的話那幾種語言永遠用不了這個功能。
            // 所以只收<b>整個名字完全相同</b>、而且對到的不是它自己的——
            // 片段查那一路在拉丁字母上太容易撞到英文單字，不走。
            String hit = market.exact(message);
            if (hit == null || hit.equalsIgnoreCase(typed)) {
                return message;
            }
            say(T.c("market.sent", typed, hit).withStyle(ChatFormatting.GRAY));
            return hit;
        }
        List<String> hits = market.candidates(message);
        if (hits.size() == 1) {
            say(T.c("market.sent", typed, hits.get(0)).withStyle(ChatFormatting.GRAY));
            return hits.get(0);
        }
        if (hits.isEmpty()) {
            say(T.c("market.notfound", typed).withStyle(ChatFormatting.GRAY));
            return message;
        }
        // 對到好幾個就不猜。列出來讓玩家挑，並原樣送出——至少不會搜到別的東西。
        StringBuilder list = new StringBuilder();
        for (int i = 0; i < Math.min(MAX_SHOWN, hits.size()); i++) {
            list.append(i > 0 ? ", " : "").append(hits.get(i));
        }
        if (hits.size() > MAX_SHOWN) {
            list.append(T.s("market.more", hits.size()));
        }
        say(T.c("market.ambiguous", typed, list.toString()).withStyle(ChatFormatting.YELLOW));
        return message;
    }

    /**
     * 整串都是英文字母、數字與標點。
     *
     * <p>玩家自己打英文時不該被動到——而且英文名本來就是市集認得的形式，
     * 「翻譯」它只會把對的東西弄壞。見 {@link #translate} 怎麼放行拉丁字母的譯名。
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
            com.wynnchayuan.capture.OwnOutputs.note(text);
            mc.player.displayClientMessage(text, false);
        }
    }

    /**
     * 市集現在是不是在等搜尋字，而且還在時效內。
     *
     * <p>給候選清單（{@code MarketPicker}）用：跟 {@link #rewrite} 同一條件，
     * 清單出現的時候，送出去的字也一定會被這裡處理。
     */
    public static boolean active() {
        return searching && System.currentTimeMillis() - armedAt <= WINDOW_MS;
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
