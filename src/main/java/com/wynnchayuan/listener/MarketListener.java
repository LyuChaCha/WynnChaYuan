package com.wynnchayuan.listener;

import com.wynnchayuan.ModLog;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.T;
import com.wynnchayuan.translate.ErrorDebug;
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

public final class MarketListener {

    private static volatile boolean searching = false;

    private static final int MAX_SHOWN = 6;

    @SubscribeEvent
    public void onMarketState(TradeMarketStateEvent event) {
        if (event.getNewState() == TradeMarketState.SEARCH_CHAT_INPUT) {
            arm();
        } else if (event.getOldState() == TradeMarketState.SEARCH_CHAT_INPUT) {
            searching = false;
        }
    }

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

    private static final java.util.regex.Pattern PROMPT =
            java.util.regex.Pattern.compile(
                    "Type the item name or type 'cancel' to cancel:|輸入物品名稱|输入物品名称");

    private static final java.util.regex.Pattern CANCELLED =
            java.util.regex.Pattern.compile(
                    "chat input was canceled|聊天輸入已取消|聊天输入已取消");

    static final String PROMPT_KEY =
            "{#} \n{#} Type the item name or type 'cancel' to cancel:\n{#}";

    static final String CANCEL_KEY = "{#} You moved and your chat input was canceled.";

    static boolean showsTranslation(String plain, String key) {
        TranslationStore store;
        try {
            store = WynnChaYuan.translations();
        } catch (Throwable t) {
            ErrorDebug.note("MarketListener#showsTranslation", null, t);
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

    private static final int MIN_CORE = 4;

    private static void arm() {
        searching = true;
        armedAt = System.currentTimeMillis();
    }

    private static volatile long armedAt = 0;

    private static final long WINDOW_MS = 120_000;

    public static String rewrite(String message) {
        if (!searching || message == null || message.isBlank()
                || message.startsWith("/")
                || System.currentTimeMillis() - armedAt > WINDOW_MS) {
            return message;
        }
        searching = false;
        try {
            return translate(message);
        } catch (Throwable t) {
            ModLog.warn("市集搜尋轉換失敗，原樣送出: " + t);
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

    public static boolean active() {
        return searching && System.currentTimeMillis() - armedAt <= WINDOW_MS;
    }

    static boolean armed() {
        return searching;
    }

    static void searching(boolean on) {
        searching = on;
        armedAt = on ? System.currentTimeMillis() : 0;
    }
}
