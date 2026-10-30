package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;

// 跑在 Wynntils 算繪流程裡，丟例外會讓疊層畫不出來
public final class WynntilsText {

    static final String TRACKER = "ContentTrackerOverlay";

    private WynntilsText() {}

    public static StyledText[] lines(Object overlay, StyledText[] lines) {
        try {
            StyledText[] out = lines(overlay, lines,
                    WynnChaYuan.config(), WynnChaYuan.translations());
            if (out != lines) {
                WynnChaYuan.store().noteEvent("overlay.shown");
            } else if (tracker(overlay)) {
                WynnChaYuan.store().noteEvent("overlay.noMatch");
                for (StyledText line : lines) {
                    noteTracker(line);
                }
            }
            return out;
        } catch (Throwable t) {
            ErrorDebug.note("WynntilsText#lines", null, t);
            return lines;
        }
    }

    static StyledText[] lines(Object overlay, StyledText[] lines,
                              CollectorConfig config, TranslationStore store) {
        if (lines == null || lines.length == 0 || store == null || config == null
                || !tracker(overlay)
                || config.trackerMode() != CollectorConfig.DialogueMode.REPLACE) {
            return lines;
        }
        StyledText[] out = new StyledText[lines.length];
        boolean any = false;
        for (int i = 0; i < lines.length; i++) {
            out[i] = trackerLine(lines[i], store);
            any |= out[i] != lines[i];
        }
        return any ? out : lines;
    }

    private static StyledText trackerLine(StyledText line, TranslationStore store) {
        if (line == null || line.isEmpty()) {
            return line;
        }
        WynntilsScreenText.Cached seen;
        synchronized (TRACKER_CACHE) {
            seen = TRACKER_CACHE.get(line);
        }
        if (seen != null && seen.fresh(store)) {
            return seen.out() == null ? line : seen.out();
        }
        StyledText shown = line(line, store);
        synchronized (TRACKER_CACHE) {
            TRACKER_CACHE.put(line, new WynntilsScreenText.Cached(store.generation(),
                    store.namesWithOriginal(), store.translatesNames(),
                    store.peeksPlainNames(), store.pageNameMode(), false,
                    shown == line ? null : shown));
        }
        return shown;
    }

    private static final java.util.Map<StyledText, WynntilsScreenText.Cached> TRACKER_CACHE = lru(64);

    static <K, V> java.util.Map<K, V> lru(int limit) {
        return new java.util.LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
                return size() > limit;
            }
        };
    }

    static boolean firstTime(java.util.Map<String, Boolean> noted, String key) {
        synchronized (noted) {
            return noted.put(key, Boolean.TRUE) == null;
        }
    }

    static StyledText line(StyledText line, TranslationStore store) {
        if (line == null || line.isEmpty()) {
            return line;
        }
        var translated = LineTranslator.translate(line, store);
        if (translated != null) {
            return StyledText.fromComponent(translated);
        }
        return byPart(line, store);
    }

    static StyledText byPart(StyledText line, TranslationStore store) {
        net.minecraft.network.chat.MutableComponent out =
                net.minecraft.network.chat.Component.empty();
        boolean any = false;
        int parts = 0;
        for (com.wynntils.core.text.StyledTextPart part : line) {
            StyledText one = StyledText.fromPart(part);
            parts++;
            if (one.isBlank()) {
                out.append(one.getComponent());
                continue;
            }
            net.minecraft.network.chat.Component done = LineTranslator.translate(one, store);
            if (done == null) {
                String split = splitHeader(one.getStringWithoutFormatting(), store);
                if (split != null) {
                    // Wynntils 標題樣板包成 language/wynncraft，沒有中文字
                    done = net.minecraft.network.chat.Component.literal(split)
                            .withStyle(styleOf(one).withFont(
                                    net.minecraft.network.chat.FontDescription.DEFAULT));
                }
            }
            any |= done != null;
            out.append(done != null ? done : one.getComponent());
        }
        return any ? StyledText.fromComponent(out) : line;
    }

    static String splitHeader(String text, TranslationStore store) {
        for (String dash : DASHES) {
            int at = text.indexOf(dash);
            if (at <= 0) {
                continue;
            }
            String left = text.substring(0, at);
            String right = text.substring(at + dash.length());
            String l = half(left, store);
            String r = half(right, store);
            if (l == null && r == null) {
                return null;
            }
            return (l != null ? l : left) + dash + (r != null ? r : right);
        }
        return null;
    }

    private static final String[] DASHES = {" - ", " – ", " — "};

    private static String half(String text, TranslationStore store) {
        int start = 0;
        while (start < text.length()
                && (Character.isWhitespace(text.charAt(start))
                    || com.wynnchayuan.capture.GlyphSplitter.isGlyphCodePoint(
                            text.codePointAt(start)))) {
            start += Character.charCount(text.codePointAt(start));
        }
        String core = text.substring(start).strip();
        if (core.isEmpty()) {
            return null;
        }
        String hit = store.pageTitleName(core);
        if (hit == null) {
            hit = store.lookup(core);
        }
        return hit == null || hit.isBlank() ? null : text.substring(0, start) + hit;
    }

    static net.minecraft.network.chat.Style styleOf(StyledText one) {
        net.minecraft.network.chat.Style[] found = {net.minecraft.network.chat.Style.EMPTY};
        one.getComponent().visit((style, text) -> {
            if (!text.isEmpty()) {
                found[0] = style;
                return java.util.Optional.of(Boolean.TRUE);
            }
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return found[0];
    }

    public static String objective(String text) {
        try {
            String out = objective(text, WynnChaYuan.config(), WynnChaYuan.translations());
            WynnChaYuan.store().noteEvent(
                    out.equals(text) ? "objective.noMatch" : "objective.shown");
            return out;
        } catch (Throwable t) {
            ErrorDebug.note("WynntilsText#objective", null, t);
            return text;
        }
    }

    static String objective(String text, CollectorConfig config, TranslationStore store) {
        if (text == null || text.isBlank() || store == null || config == null
                || !config.translateObjectives()) {
            return text;
        }
        synchronized (OBJECTIVES) {
            if (objectiveAge != store.generation()) {
                OBJECTIVES.clear();
                objectiveAge = store.generation();
            }
            String hit = OBJECTIVES.get(text);
            if (hit == null) {
                var translated = LineTranslator.translate(StyledText.fromString(text), store);
                hit = translated == null ? text : translated.getString();
                if (OBJECTIVES.size() > 256) {
                    OBJECTIVES.clear();
                }
                OBJECTIVES.put(text, hit);
            }
            return hit;
        }
    }

    private static final java.util.Map<String, String> OBJECTIVES = new java.util.HashMap<>();
    private static int objectiveAge = -1;

    public static String marker(String text) {
        try {
            return marker(text, WynnChaYuan.config(), WynnChaYuan.translations());
        } catch (Throwable t) {
            ErrorDebug.note("WynntilsText#marker", null, t);
            return text;
        }
    }

    static String marker(String text, CollectorConfig config, TranslationStore store) {
        if (text == null || text.isBlank() || store == null || config == null
                || config.trackerMode() == CollectorConfig.DialogueMode.OFF) {
            return text;
        }
        synchronized (MARKERS) {
            if (markerAge != store.generation()) {
                MARKERS.clear();
                markerAge = store.generation();
            }
            String hit = MARKERS.get(text);
            if (hit == null) {
                var translated = LineTranslator.translate(StyledText.fromString(text), store);
                hit = translated == null ? text : StyledText.fromComponent(translated).getString();
                if (MARKERS.size() > 256) {
                    MARKERS.clear();
                }
                MARKERS.put(text, hit);
            }
            return hit;
        }
    }

    private static final java.util.Map<String, String> MARKERS = new java.util.HashMap<>();
    private static int markerAge = -1;

    public static net.minecraft.network.chat.Component heldItemName(
            net.minecraft.network.chat.Component name) {
        try {
            return heldItemName(name, WynnChaYuan.config(), WynnChaYuan.translations());
        } catch (Throwable t) {
            ErrorDebug.note("WynntilsText#heldItemName", null, t);
            return name;
        }
    }

    static net.minecraft.network.chat.Component heldItemName(
            net.minecraft.network.chat.Component name, CollectorConfig config,
            TranslationStore store) {
        if (name == null || store == null || config == null || !config.translateHeldItem()) {
            return name;
        }
        var translated = LineTranslator.translate(StyledText.fromComponent(name), store);
        return translated == null ? name : translated;
    }

    public static StyledText notification(StyledText text) {
        try {
            CollectorConfig config = WynnChaYuan.config();
            TranslationStore store = WynnChaYuan.translations();
            if (text == null || text.isEmpty() || config == null || store == null
                    || config.chatMode() == CollectorConfig.ChatMode.OFF) {
                return text;
            }
            StyledText shown = line(text, store);
            if (shown == text && config.collect()
                    && !com.wynnchayuan.capture.GlyphSplitter.isGlyphOnly(text)) {
                String template = com.wynnchayuan.capture.GlyphSplitter.toTemplate(text);
                var captured = WynnChaYuan.store();
                if (captured != null && !template.isBlank()
                        && !com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(template)) {
                    captured.record(template, "desc", "chat", "chat/notification");
                }
            }
            return shown;
        } catch (Throwable t) {
            ErrorDebug.note("WynntilsText#notification", null, t);
            return text;
        }
    }

    public static StyledText heldItemText(StyledText text) {
        try {
            if (text == null || text.isEmpty()) {
                return text;
            }
            net.minecraft.network.chat.Component shown = heldItemName(text.getComponent(),
                    WynnChaYuan.config(), WynnChaYuan.translations());
            return shown == text.getComponent() ? text : StyledText.fromComponent(shown);
        } catch (Throwable t) {
            ErrorDebug.note("WynntilsText#heldItemText", null, t);
            return text;
        }
    }

    private static void noteTracker(StyledText line) {
        if (line == null || line.isEmpty()
                || !WynnChaYuan.config().collect()) {
            return;
        }
        if (!firstTime(TRACKER_NOTED, line.getString())) {
            return;
        }
        String template = com.wynnchayuan.capture.GlyphSplitter.toTemplate(line);
        if (template.isBlank()
                || !com.wynnchayuan.capture.GlyphSplitter.hasLetter(template)
                || com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(template)
                || com.wynnchayuan.capture.PlayerDataFilter
                        .mentionsOnlinePlayerLoose(template)) {
            return;
        }
        var captured = WynnChaYuan.store();
        if (captured != null) {
            captured.record(template, "desc", "quest", "tracker/line");
        }
    }

    private static final java.util.Map<String, Boolean> TRACKER_NOTED = lru(256);

    private static boolean tracker(Object overlay) {
        return overlay != null && TRACKER.equals(overlay.getClass().getSimpleName());
    }

    public static java.util.List<net.minecraft.network.chat.Component> menuTooltip(
            java.util.List<net.minecraft.network.chat.Component> lines) {
        try {
            CollectorConfig config = WynnChaYuan.config();
            if (lines == null || lines.isEmpty() || config == null || !config.wynntilsUi()) {
                return lines;
            }
            com.wynnchayuan.listener.GuiTextCapture.record(lines);
            java.util.List<net.minecraft.network.chat.Component> out =
                    TooltipPanel.translateInPlace(lines, WynnChaYuan.translations());
            if (out == null || out.isEmpty()) {
                return lines;
            }
            return out;
        } catch (Throwable t) {
            ErrorDebug.note("WynntilsText#menuTooltip", null, t);
            return lines;
        }
    }

    public static net.minecraft.network.chat.Component bossBar(net.minecraft.network.chat.Component name) {
        return WynntilsBossBar.bossBar(name);
    }

    public static net.minecraft.network.chat.Component entityName(net.minecraft.network.chat.Component name,
                                                                  net.minecraft.world.entity.Entity entity) {
        return WynntilsNames.entityName(name, entity);
    }

    public static StyledText screenText(StyledText text) {
        return WynntilsScreenText.screenText(text);
    }

    public static void holdRawText(boolean on) {
        WynntilsScreenText.holdRawText(on);
    }

    public static void setScoreboard(java.util.Set<String> shown,
                                     java.util.Map<String, StyledText> replaced) {
        WynntilsScreenText.setScoreboard(shown, replaced);
    }

    public static void setScoreboard(java.util.Set<String> shown,
                                     java.util.Map<String, StyledText> replaced,
                                     java.util.Set<String> kept) {
        WynntilsScreenText.setScoreboard(shown, replaced, kept);
    }
}
