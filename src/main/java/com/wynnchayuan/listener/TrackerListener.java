package com.wynnchayuan.listener;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.CurrentQuest;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynnchayuan.render.TrackerOverlay;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.activities.event.ActivityTrackerUpdatedEvent;
import com.wynntils.models.character.event.CharacterUpdateEvent;
import com.wynntils.models.worlds.event.WorldStateEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * 追蹤中的任務／活動。
 *
 * <p>Wynntils 有正式事件 {@link ActivityTrackerUpdatedEvent}，帶著追蹤項目的
 * 類型、名稱與當前目標，所以既不必用 reflection 硬戳它的內部，也不用每幀輪詢——
 * 內容變了才會收到一次。
 *
 * <p>任務名稱與目標敘述分開處理：名稱是短詞（{@code role=name}），
 * 目標是完整句子（{@code role=desc}），兩者的翻譯風格與優先度都不一樣。
 */
public final class TrackerListener {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTrackerUpdate(ActivityTrackerUpdatedEvent event) {
        WynnChaYuan.store().noteEvent("tracker.updated");

        String name = event.getName();
        // 記下來給對話收集用：同一個任務的台詞才排得在一起
        CurrentQuest.set(name);
        if (name != null && !name.isBlank()) {
            WynnChaYuan.store().record(
                    GlyphSplitter.stripGlyphChars(name).strip(),
                    "name", "quest", "tracker/name");
        }

        StyledText task = event.getTask();
        if (task != null && !GlyphSplitter.isGlyphOnly(task)) {
            // 追蹤目標常帶座標（「Explore the pit at [356, 61, -1914]」）。
            // 那是固定的世界座標、對所有玩家都一樣，不是個資，所以不該擋掉——
            // 數值參數化會自動把它變成 [{~}, {~}, {~}]，一併解決了去重與安全。
            String template = GlyphSplitter.toTemplate(task);
            if (PlayerDataFilter.carriesPlayerData(template)) {
                WynnChaYuan.store().noteEvent("tracker.blocked.playerData");
            } else {
                WynnChaYuan.store().record(template, "desc", "quest", "tracker/task");
            }
        }

        // 追蹤欄空掉了（切換任務、換角色、取消追蹤），譯文也要跟著收掉。
        // 先前只在有內容時更新，於是原文那一欄已經消失、翻譯還孤零零地留在畫面上。
        boolean gone = (name == null || name.isBlank())
                && (task == null || GlyphSplitter.isGlyphOnly(task));
        if (gone) {
            TrackerOverlay.clear();
            return;
        }
        TrackerOverlay.setCurrent(name, task, WynnChaYuan.translations());
    }

    /**
     * 換角色或換世界時把疊層收乾淨。
     *
     * <h2>為什麼不能只靠追蹤更新事件</h2>
     * 那個事件只在<b>有東西可追蹤</b>時才來。玩家切角色的瞬間追蹤欄整個消失，
     * 事件卻不一定會補一則空的——結果原文那一欄沒了，我們的譯文還留在畫面上，
     * 而且停在上一個角色的任務。
     */
    @SubscribeEvent
    public void onWorldState(WorldStateEvent event) {
        TrackerOverlay.clear();
        com.wynnchayuan.render.DialogueOverlay.clear();
    }

    @SubscribeEvent
    public void onCharacterUpdate(CharacterUpdateEvent event) {
        TrackerOverlay.clear();
        com.wynnchayuan.render.DialogueOverlay.clear();
    }
}
