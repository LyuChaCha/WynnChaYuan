package com.wynnchayuan.listener;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynnchayuan.translate.LineTranslator;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.ChatMessageEvent;
import com.wynntils.handlers.chat.type.RecipientType;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * 聊天視窗裡的伺服器訊息。
 *
 * <h2>為什麼聊天要另外接一支</h2>
 * 任務完成的獎勵清單、進出區域的提示、商店與活動公告，這些都走<b>聊天</b>
 * 而不是對話框。先前 {@link CaptureListener} 只把它們<b>收錄</b>進語料，
 * 沒有任何地方把譯文畫回去——語料裡明明有「[任務完成]」「獎勵:」
 * 「- +{~} 經驗值」，玩家看到的還是英文。
 *
 * <h2>只碰伺服器發的</h2>
 * 聊天視窗同時混著<b>其他玩家打的字</b>。那是真人寫的內容，翻它既沒意義也不禮貌，
 * 而且夾帶人名。白名單跟收錄那一關用同一份：只有 {@code INFO} 與
 * {@code GAME_MESSAGE}，其餘（世界、公會、隊伍、私訊、寵物、喊話）一律不動。
 *
 * <p>再過一次 {@link PlayerDataFilter}：伺服器訊息裡也有夾帶玩家名的
 * （好友上線、交易邀請），那些原樣放過。
 */
public final class ChatListener {

    /** 跟收錄那一關同一份白名單。見 {@link CaptureListener#onChat}。 */
    private static final Set<RecipientType> SERVER_MESSAGES =
            EnumSet.of(RecipientType.INFO, RecipientType.GAME_MESSAGE);

    /**
     * 用 {@code Edit} 而不是 {@code Match}——只有 {@code Edit} 收得到
     * {@code setMessage}，那是 Wynntils 專門留給「改寫聊天內容」的那一支。
     *
     * <p>優先權放最低：讓其他模組先改完，我們拿到的才是最後真正要畫的樣子。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChat(ChatMessageEvent.Edit event) {
        CollectorConfig.ChatMode mode = WynnChaYuan.config().chatMode();
        if (mode == CollectorConfig.ChatMode.OFF) {
            return;
        }
        if (!SERVER_MESSAGES.contains(event.getRecipientType())) {
            return;                      // 玩家發言，不碰
        }
        StyledText message = event.getMessage();
        if (message == null || GlyphSplitter.isGlyphOnly(message)) {
            return;
        }
        if (PlayerDataFilter.carriesPlayerData(GlyphSplitter.toTemplate(message))) {
            return;                      // 夾帶玩家名的伺服器訊息，原樣放過
        }
        Component hit = LineTranslator.translate(message, WynnChaYuan.translations());
        if (hit == null) {
            return;                      // 查不到就別動，原文比半吊子好
        }
        event.setMessage(mode == CollectorConfig.ChatMode.BOTH
                ? StyledText.fromComponent(
                        Component.empty().append(message.getComponent()).append("\n").append(hit))
                : StyledText.fromComponent(hit));
    }
}
