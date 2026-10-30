package com.wynnchayuan.listener;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynnchayuan.render.ScoreboardFlow;
import com.wynnchayuan.render.TrackerOverlay;
import com.wynnchayuan.render.WynntilsText;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.scoreboard.ScoreboardSegment;
import com.wynntils.handlers.scoreboard.event.ScoreboardUpdatedEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ScoreboardListener {

    private static final String ACTIVITY_PART = "ActivityTrackerScoreboardPart";

    private static final String LOOTRUN_PART = "LootrunScoreboardPart";

    static boolean mirrors(String part) {
        if (part == null) {
            return true;
        }
        return !part.contains(ACTIVITY_PART) && !part.contains(LOOTRUN_PART);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onScoreboardUpdated(ScoreboardUpdatedEvent event) {
        try {
            update(event);
        } catch (Throwable t) {
            ErrorDebug.note("ScoreboardListener#onScoreboardUpdated", null, t);
            WynnChaYuan.store().noteEvent("scoreboard.error");
        }
    }

    private void update(ScoreboardUpdatedEvent event) {
        TranslationStore store = WynnChaYuan.translations();
        List<Component> lines = new ArrayList<>();
        Set<String> shown = new HashSet<>();
        Map<String, StyledText> replaced = new HashMap<>();
        Set<String> kept = new HashSet<>();
        Set<String> names = memberNames(event);
        boolean any = false;
        for (var pair : event.getScoreboardSegments()) {
            ScoreboardSegment segment = pair.b();
            if (segment == null || !segment.isVisible()) {
                continue;
            }
            String part = pair.a() == null ? null : pair.a().getClass().getSimpleName();
            if (part != null && part.contains(ACTIVITY_PART)) {
                continue;
            }
            List<StyledText> content = segment.getContent();
            shown.add(ScoreboardFlow.key(segment.getHeader()));
            Set<String> players = ScoreboardFlow.keptRows(part, segment.getHeader(), content, names);
            kept.addAll(players);
            for (StyledText row : content) {
                shown.add(ScoreboardFlow.key(row));
                if (!players.contains(ScoreboardFlow.key(row))) {
                    capture(row, "desc");
                }
            }
            ScoreboardFlow.Plan plan = ScoreboardFlow.plan(content, store, players);
            if (plan.missing() > 0 && players.isEmpty()) {
                captureBlock(content);
            }
            replaced.putAll(plan.rows());
            if (!mirrors(part)) {
                continue;
            }
            any |= header(lines, segment.getHeader(), store);
            lines.addAll(plan.panel());
            any |= plan.any();
        }
        WynnChaYuan.store().noteEvent(any ? "scoreboard.shown" : "scoreboard.noMatch");
        boolean on = WynnChaYuan.config() == null || WynnChaYuan.config().translateScoreboard();
        WynntilsText.setScoreboard(shown, replaced, kept);
        TrackerOverlay.setExtras(any && on ? lines : List.of());
    }

    private static Set<String> memberNames(ScoreboardUpdatedEvent event) {
        Set<String> names = new HashSet<>();
        for (var pair : event.getScoreboardSegments()) {
            ScoreboardSegment segment = pair.b();
            String part = pair.a() == null ? null : pair.a().getClass().getSimpleName();
            if (segment == null || !ScoreboardFlow.isParty(part, segment.getHeader())) {
                continue;
            }
            for (StyledText row : segment.getContent()) {
                String name = ScoreboardFlow.partyName(row);
                if (name != null) {
                    names.add(name);
                }
            }
        }
        try {
            names.addAll(com.wynntils.core.components.Models.Party.getPartyMembers());
        } catch (Throwable t) {
            ErrorDebug.note("ScoreboardListener#memberNames", null, t);
        }
        return names;
    }

    private boolean header(List<Component> lines, StyledText row, TranslationStore store) {
        if (!capture(row, "name")) {
            return false;
        }
        Component translated = LineTranslator.translate(row, store);
        if (translated != null) {
            translated = ScoreboardFlow.oneLine(translated);
        }
        lines.add(translated != null ? translated : LineTranslator.untranslated(row));
        return translated != null;
    }

    private boolean capture(StyledText row, String role) {
        if (row == null || GlyphSplitter.isGlyphOnly(row)) {
            return false;
        }
        String template = GlyphSplitter.toTemplate(row);
        if (template.isBlank()) {
            return false;
        }
        if (PlayerDataFilter.carriesPlayerData(template)
                || PlayerDataFilter.mentionsOnlinePlayerLoose(template)) {
            WynnChaYuan.store().noteEvent("scoreboard.blocked.playerData");
        } else {
            WynnChaYuan.store().record(template, role, "quest", "scoreboard");
        }
        return true;
    }

    private void captureBlock(List<StyledText> content) {
        List<String> rows = new ArrayList<>();
        for (StyledText row : content) {
            if (row == null || GlyphSplitter.isGlyphOnly(row)) {
                continue;
            }
            String template = GlyphSplitter.toTemplate(row);
            if (template.isBlank()) {
                continue;
            }
            if (PlayerDataFilter.carriesPlayerData(template)
                    || PlayerDataFilter.mentionsOnlinePlayerLoose(template)) {
                return;
            }
            rows.add(template);
        }
        if (rows.size() >= 2) {
            WynnChaYuan.store().record(String.join("\n", rows), "desc", "quest",
                    "scoreboard/block");
        }
    }
}
