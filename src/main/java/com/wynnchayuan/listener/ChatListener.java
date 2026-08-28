package com.wynnchayuan.listener;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.ChatLog;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynnchayuan.translate.LineTranslator;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.type.StyleType;
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
        StyledText message = event.getMessage();
        if (message == null || GlyphSplitter.isGlyphOnly(message)) {
            return;
        }
        CollectorConfig.ChatMode mode = WynnChaYuan.config().chatMode();
        boolean serverSide = SERVER_MESSAGES.contains(event.getRecipientType());
        // 查譯文。玩家發言、夾帶玩家名的伺服器訊息不查——
        // 那是真人寫的內容，翻它既沒意義也不禮貌。
        Component hit = serverSide
                && !PlayerDataFilter.carriesPlayerData(GlyphSplitter.toTemplate(message))
                ? LineTranslator.translate(message, WynnChaYuan.translations())
                : null;

        // 先記進複製用的緩衝區，再考慮要不要改畫面。
        //
        // 兩件事是分開的：就算使用者把聊天翻譯關掉，「複製聊天」也要能用；
        // 而且他可能想複製別人講的話，那些我們從來不翻。
        // 緩衝區只留在本機，不進語料。見 {@link ChatLog}。
        if (WynnChaYuan.config().chatCopy()) {
            ChatLog.add(message.getString(StyleType.NONE),
                        hit == null ? null : hit.getString());
        }

        if (mode == CollectorConfig.ChatMode.OFF || !serverSide || hit == null) {
            return;                      // 查不到就別動，原文比半吊子好
        }
        event.setMessage(mode == CollectorConfig.ChatMode.BOTH
                ? StyledText.fromComponent(
                        Component.empty().append(message.getComponent()).append("\n").append(hit))
                : StyledText.fromComponent(hit));
    }
}
