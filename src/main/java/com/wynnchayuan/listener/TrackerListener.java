package com.wynnchayuan.listener;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynnchayuan.render.TrackerOverlay;
import com.wynntils.core.text.StyledText;
import com.wynntils.models.activities.event.ActivityTrackerUpdatedEvent;
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

        TrackerOverlay.setCurrent(name, task, WynnChaYuan.translations());
    }
}
