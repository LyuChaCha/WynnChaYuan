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

public final class TrackerListener {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTrackerUpdate(ActivityTrackerUpdatedEvent event) {
        WynnChaYuan.store().noteEvent("tracker.updated");

        String name = event.getName();
        CurrentQuest.set(name);
        if (name != null && !name.isBlank()) {
            String template = GlyphSplitter.parametrizeNumbers(
                    GlyphSplitter.stripGlyphChars(name).strip());
            if (PlayerDataFilter.carriesPlayerData(template)) {
                WynnChaYuan.store().noteEvent("tracker.blocked.playerData");
            } else {
                WynnChaYuan.store().record(template, "name", "quest", "tracker/name");
            }
        }

        StyledText task = event.getTask();
        if (task != null && !GlyphSplitter.isGlyphOnly(task)) {
            String template = GlyphSplitter.toTemplate(task);
            if (PlayerDataFilter.carriesPlayerData(template)) {
                WynnChaYuan.store().noteEvent("tracker.blocked.playerData");
            } else {
                WynnChaYuan.store().record(template, "desc", "quest", "tracker/task");
            }
        }

        boolean gone = (name == null || name.isBlank())
                && (task == null || GlyphSplitter.isGlyphOnly(task));
        if (gone) {
            TrackerOverlay.clear();
            return;
        }
        TrackerOverlay.setCurrent(event.getType(), name, task, WynnChaYuan.translations());
    }

    @SubscribeEvent
    public void onWorldState(WorldStateEvent event) {
        TrackerOverlay.clear();
        com.wynnchayuan.render.DialogueOverlay.clear();
        var state = event.getNewState();
        if (state == com.wynntils.models.worlds.type.WorldState.CHARACTER_SELECTION
                || state == com.wynntils.models.worlds.type.WorldState.WORLD) {
            net.minecraft.client.Minecraft.getInstance().execute(
                    com.wynnchayuan.client.NoticeScreen::maybeShowOnJoin);
        }
    }

    @SubscribeEvent
    public void onCharacterUpdate(CharacterUpdateEvent event) {
        TrackerOverlay.clear();
        com.wynnchayuan.render.DialogueOverlay.clear();
    }
}
