package com.wynnchayuan.listener;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.ChatLog;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.LineTranslator;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.ChatMessageEvent;
import com.wynntils.handlers.chat.type.RecipientType;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.EnumSet;
import java.util.Set;

public final class ChatListener {

    private static final Set<RecipientType> SERVER_MESSAGES =
            EnumSet.of(RecipientType.INFO, RecipientType.GAME_MESSAGE);

    // 只有 Match 能 cancelChat；最低優先權讓 Wynntils 先讀原文
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onMatch(ChatMessageEvent.Match event) {
        if (event.isChatCanceled()
                || WynnChaYuan.config().chatMode() != CollectorConfig.ChatMode.REPLACE
                || !SERVER_MESSAGES.contains(event.getRecipientType())) {
            return;
        }
        StyledText message = event.getMessage();
        if (message == null || ChatBlock.isOurs(message)) {
            return;
        }
        boolean panel;
        try {
            panel = ChatBlock.holding() || LineTranslator.panelRow(message);
        } catch (Throwable t) {
            ErrorDebug.note("ChatListener#onMatch", null, t);
            panel = false;
        }
        if (panel) {
            boolean keep;
            try {
                keep = com.wynnchayuan.render.ThirdPartyLiterals.reserved(
                               message.getStringWithoutFormatting())
                        || (com.wynnchayuan.capture.Broadcasts.find(message) == null
                            && PlayerDataFilter.carriesPlayerData(
                                    GlyphSplitter.toTemplate(message))
                            && (LineTranslator.nameFramed(message, WynnChaYuan.translations()) == null
                                || LineTranslator.translateChat(
                                        message, WynnChaYuan.translations()) == null));
            } catch (Throwable t) {
                ErrorDebug.note("ChatListener#onMatch", null, t);
                keep = true;
            }
            event.cancelChat();
            ChatBlock.hold(message, keep);
            if (WynnChaYuan.config().chatCopy()) {
                ChatLog.add(message.getComponent(), null);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChat(ChatMessageEvent.Edit event) {
        StyledText message = event.getMessage();
        if (message == null || GlyphSplitter.isGlyphOnly(message)) {
            return;
        }
        if (ChatBlock.isOurs(message)) {
            Component ours = ChatBlock.sendingNow();
            if (ours != null) {
                StyledText original = StyledText.fromComponent(ours);
                if (!original.equals(message)) {
                    event.setMessage(original);
                }
            }
            return;
        }
        CollectorConfig.ChatMode mode = WynnChaYuan.config().chatMode();
        boolean serverSide = SERVER_MESSAGES.contains(event.getRecipientType());
        boolean reserved = com.wynnchayuan.render.ThirdPartyLiterals.reserved(
                message.getStringWithoutFormatting());
        boolean carries = serverSide
                && PlayerDataFilter.carriesPlayerData(GlyphSplitter.toTemplate(message));
        Component hit;
        if (serverSide && !reserved
                && (com.wynnchayuan.capture.Broadcasts.find(message) != null
                    || (carries && LineTranslator.nameFramed(
                            message, WynnChaYuan.translations()) != null))) {
            hit = LineTranslator.translateChat(message, WynnChaYuan.translations());
        } else {
            hit = serverSide
                    && !carries
                    && !reserved
                    ? (mode == CollectorConfig.ChatMode.BOTH
                            ? LineTranslator.translateChat(message, WynnChaYuan.translations())
                            : replaceInPlace(message))
                    : null;
        }

        if (WynnChaYuan.config().chatCopy()) {
            ChatLog.add(message.getComponent(), hit);
        }

        if (mode == CollectorConfig.ChatMode.OFF || !serverSide) {
            return;
        }
        if (mode == CollectorConfig.ChatMode.BOTH) {
            ChatBlock.queue(message, hit);
            return;
        }
        if (hit == null) {
            return;
        }
        event.setMessage(StyledText.fromComponent(hit));
    }

    private static Component replaceInPlace(StyledText message) {
        Component chat = LineTranslator.translateChat(message, WynnChaYuan.translations());
        return chat != null ? chat : LineTranslator.translate(message, WynnChaYuan.translations());
    }
}
