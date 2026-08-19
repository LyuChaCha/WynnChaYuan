package com.wynnchayuan.listener;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.DialogueBuffer;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.render.DialogueOverlay;
import com.wynnchayuan.render.LookAtTranslator;
import com.wynnchayuan.translate.LineTranslator;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.ChatMessageEvent;
import com.wynntils.handlers.chat.type.RecipientType;
import com.wynntils.handlers.labels.event.TextDisplayChangedEvent;
import com.wynntils.models.dialogue.event.NpcDialogueEvent;
import com.wynntils.models.npc.label.NpcLabelInfo;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * 訂閱 Wynntils 事件，把沒見過的字串記錄下來。
 *
 * <p>只處理靜態資料庫涵蓋不到的東西——裝備與技能已經可以從官方 CDN 完整離線取得
 * （見 corpus/），沒必要靠玩家在遊戲裡碰運氣遇到。這裡負責的是任務對話、
 * 系統訊息、NPC 名牌這類只存在於連線期間的內容。
 *
 * <p>全部以 {@link EventPriority#LOWEST} 註冊且不取消任何事件：這個 mod 只觀察，
 * 不改變遊戲行為，也不與其他 mod 搶順序。
 */
public final class CaptureListener {

    // ---------------------------------------------------------------- 對話
    //
    // Wynntils 的 DialogueModel 由 ActionBarUpdatedEvent 驅動，四種事件的觸發條件
    // 差異很大，而且沒有哪一種保證一定會發：
    //
    //   Started   對話首次出現
    //   Updated   內容變了但還沒等待輸入
    //   Finished  只在 requiresShift 由 false 轉 true 時才發
    //             → 不需要按 SHIFT 的對話（自動推進、商店 NPC）永遠不會觸發
    //   Ended     對話關閉時發
    //
    // 更麻煩的是對話是「逐字打出來」的，每打一個字就發一次事件，
    // 所以任何單一事件拿到的都可能是半截句子。實測 62 次 Finished 只換來 1 句完整的話。
    //
    // 因此這裡四種都餵進 DialogueBuffer，由它判斷哪一刻才算打完；
    // 事件種類不再影響記錄與否，只用來計數。

    private final DialogueBuffer buffer = new DialogueBuffer();
    private volatile boolean lastHadChoices = false;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDialogueStarted(NpcDialogueEvent.Started event) {
        WynnChaYuan.store().noteEvent("dialogue.Started");
        feed(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDialogueUpdated(NpcDialogueEvent.Updated event) {
        WynnChaYuan.store().noteEvent("dialogue.Updated");
        feed(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDialogueFinished(NpcDialogueEvent.Finished event) {
        WynnChaYuan.store().noteEvent("dialogue.Finished");
        feed(event);
    }

    /**
     * 注意這裡<b>不能</b>呼叫 {@code buffer.flush()}。
     *
     * <p>{@code Ended} 名字看起來像「對話結束」，實際上是<b>每打一個字就發一次</b>。
     * 原因在 Wynntils 的 {@code DialogueModel#isNewDialogue}：它用
     * {@code !text.startsWith(currentText)} 判斷是不是新對話，但比對的是<b>含游標圖示</b>
     * 的原始文字——游標每幀都不一樣，所以每個字都被當成新對話，
     * 觸發 {@code endDialogue()} + {@code startDialogue()}。
     *
     * <p>實測 {@code Ended} 與 {@code Started} 各發了 152 次，而真正的對話只有幾句。
     * 在這裡 flush 等於把每個前綴都倒出來，正是碎片的來源。
     *
     * <p>結束的判定改由 {@link DialogueBuffer} 負責：換句時前綴對不上會自動送出，
     * 最後一句則由 {@link #flushSettled()} 的靜置計時器處理。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDialogueEnded(NpcDialogueEvent.Ended event) {
        WynnChaYuan.store().noteEvent("dialogue.Ended");
        feed(event);
    }

    /**
     * 把事件內容餵進緩衝區。
     *
     * <p>四種事件都餵——它們帶的都是同一句話在不同階段的樣子，
     * 由 {@link DialogueBuffer} 判斷哪一刻算打完，這裡不需要區分。
     */
    private void feed(NpcDialogueEvent event) {
        String text = event.getDialogueText();
        if (text == null || text.isBlank()) {
            return;
        }
        StyledText styled = StyledText.fromString(text);
        if (GlyphSplitter.isGlyphOnly(styled)) {
            return;      // 純符號的過場，沒有東西可翻
        }
        lastHadChoices = event.hasChoices();
        // 顯示用的譯文吃完整原文（含符號），與收集用的模板是兩條路
        DialogueOverlay.setCurrent(styled, WynnChaYuan.translations());
        record(buffer.offer(GlyphSplitter.toTemplate(styled)));
    }

    /** 供定時器呼叫：內容穩定夠久就送出，避免最後一句卡在緩衝區。 */
    public void flushSettled() {
        record(buffer.flushIfSettled());
    }

    private void record(String completed) {
        if (completed == null || !WynnChaYuan.config().collect()) {
            return;
        }
        WynnChaYuan.store().record(
                completed, "desc", "quest",
                lastHadChoices ? "dialogue/choices" : "dialogue");
    }

    // ---------------------------------------------------------------- 聊天

    /**
     * 只有伺服器發的系統訊息才會被記錄。
     *
     * <p>部分任務的敘述、提示、進度是走聊天視窗而不是對話框的，所以這裡也要收。
     * 但聊天視窗同時混著<b>其他玩家打的字</b>——那些是真人寫的內容，
     * 既不需要翻譯，記錄下來還會把別人的對話寫進檔案裡。所以採白名單：
     * 只收 {@code INFO} 與 {@code GAME_MESSAGE}，其餘（世界、公會、隊伍、私訊、
     * 寵物、喊話）一律不碰。
     */
    private static final Set<RecipientType> SERVER_MESSAGES =
            EnumSet.of(RecipientType.INFO, RecipientType.GAME_MESSAGE);

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChat(ChatMessageEvent.Match event) {
        WynnChaYuan.store().noteEvent("chat." + event.getRecipientType());
        if (!WynnChaYuan.config().collect()) {
            return;
        }
        if (!SERVER_MESSAGES.contains(event.getRecipientType())) {
            return;                  // 玩家發言，不記錄
        }
        StyledText message = event.getMessage();
        if (message == null || GlyphSplitter.isGlyphOnly(message)) {
            return;
        }
        String template = GlyphSplitter.toTemplate(message);
        if (PlayerDataFilter.carriesPlayerData(template)) {
            WynnChaYuan.store().noteEvent("chat.blocked.playerData");
            return;              // 夾帶玩家名稱／好友名單／座標，不寫進共享檔案
        }
        WynnChaYuan.store().record(
                template, "desc", "chat",
                "chat/" + event.getRecipientType());
    }

    // ---------------------------------------------------------------- 名牌

    /**
     * NPC 名牌。
     *
     * <p>Wynntils 已經把名牌拆成 icon / name / description 三段，
     * 所以這裡拿到的 {@code name} 是乾淨的純文字，符號早就被分出去了。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLabel(TextDisplayChangedEvent.Text event) {
        event.getLabelInfo()
             .filter(NpcLabelInfo.class::isInstance)
             .map(NpcLabelInfo.class::cast)
             .ifPresent(info -> {
                 String name = info.getName();
                 if (name != null && !name.isBlank()) {
                     WynnChaYuan.store().record(
                             GlyphSplitter.toTemplate(StyledText.fromString(name)),
                             "name", "npc", "npc/name");
                 }
             });

        // 同時記下「完整名牌」的模板。翻譯名牌時查的是整段文字，
        // 不是拆出來的 name——只記 name 的話字典永遠對不上實際查詢的鍵。
        StyledText full = event.getText();
        if (full != null && !GlyphSplitter.isGlyphOnly(full)) {
            String template = GlyphSplitter.toTemplate(full);
            if (!template.isBlank() && GlyphSplitter.hasLetter(template)) {
                WynnChaYuan.store().record(template, "name", "npc", "npc/nametag");
            }
        }

        translateNametag(event);
    }

    /**
     * NPC 頭頂名牌的翻譯。
     *
     * <p>名牌浮在 3D 世界裡，沒辦法像 tooltip 那樣在旁邊開一塊面板，
     * 所以這裡是<b>唯一採用就地替換</b>的地方。
     *
     * <p>風險由三件事壓住：{@link com.wynnchayuan.translate.LineTranslator} 保證符號連同
     * 原字型整段填回、地名原樣保留、佔位符對不上就整行放棄。所以最壞情況是「沒翻到」，
     * 不會變成亂碼。
     */
    private void translateNametag(TextDisplayChangedEvent.Text event) {
        CollectorConfig.NametagMode mode = WynnChaYuan.config().nametagMode();
        if (!WynnChaYuan.config().showOverlays() || mode == CollectorConfig.NametagMode.OFF) {
            return;
        }
        try {
            StyledText original = event.getText();
            if (original == null || GlyphSplitter.isGlyphOnly(original)) {
                return;
            }
            if (mode == CollectorConfig.NametagMode.LOOK_AT) {
                // 原文完全不動，只記下來等玩家看向它時才顯示譯文
                LookAtTranslator.remember(event.getTextDisplay(), original);
                return;
            }
            Component translated =
                    LineTranslator.translate(original, WynnChaYuan.translations());
            if (translated != null) {
                event.setText(StyledText.fromComponent(translated));
            }
        } catch (Throwable t) {
            WynnChaYuan.store().noteEvent("render.nametagError");
        }
    }
}
