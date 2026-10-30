package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.LookupScope;
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
        Cached seen;
        synchronized (TRACKER_CACHE) {
            seen = TRACKER_CACHE.get(line);
        }
        if (seen != null && seen.fresh(store)) {
            return seen.out() == null ? line : seen.out();
        }
        StyledText shown = line(line, store);
        synchronized (TRACKER_CACHE) {
            TRACKER_CACHE.put(line, new Cached(store.generation(),
                    store.namesWithOriginal(), store.translatesNames(),
                    store.peeksPlainNames(), store.pageNameMode(), false,
                    shown == line ? null : shown));
        }
        return shown;
    }

    private static final java.util.Map<StyledText, Cached> TRACKER_CACHE = lru(64);

    private static <K, V> java.util.Map<K, V> lru(int limit) {
        return new java.util.LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
                return size() > limit;
            }
        };
    }

    private static boolean firstTime(java.util.Map<String, Boolean> noted, String key) {
        synchronized (noted) {
            return noted.put(key, Boolean.TRUE) == null;
        }
    }

    private static StyledText line(StyledText line, TranslationStore store) {
        if (line == null || line.isEmpty()) {
            return line;
        }
        var translated = LineTranslator.translate(line, store);
        if (translated != null) {
            return StyledText.fromComponent(translated);
        }
        return byPart(line, store);
    }

    private static StyledText byPart(StyledText line, TranslationStore store) {
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

    private static net.minecraft.network.chat.Style styleOf(StyledText one) {
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

    public static net.minecraft.network.chat.Component bossBar(
            net.minecraft.network.chat.Component name) {
        try {
            return bossBar(name, WynnChaYuan.config(), WynnChaYuan.translations());
        } catch (Throwable t) {
            ErrorDebug.note("WynntilsText#bossBar", null, t);
            return name;
        }
    }

    static net.minecraft.network.chat.Component bossBar(
            net.minecraft.network.chat.Component name, CollectorConfig config,
            TranslationStore store) {
        if (name == null || store == null || config == null || !config.translateBossBar()) {
            return name;
        }
        if (barAge != store.generation() || BARS.size() > 256) {
            BARS.clear();
            barAge = store.generation();
        }
        net.minecraft.network.chat.Component hit = BARS.get(name);
        if (hit == null) {
            StyledText text = StyledText.fromComponent(name);
            hit = bossBarPlain(name, store);
            if (hit == null) {
                StyledText shown = line(text, store);
                hit = shown == text ? name : shown.getComponent();
            }
            net.minecraft.network.chat.Component named = bossBarName(hit, store);
            if (named != null) {
                hit = named;
            }
            net.minecraft.network.chat.Component worded = bossBarWords(hit, store);
            if (worded != null) {
                hit = worded;
            }
            BARS.put(name, hit);
            var captured = WynnChaYuan.store();
            if (captured != null) {
                captured.noteEvent(hit == name ? "bossbar.noMatch" : "bossbar.shown");
            }
            if (hit == name && config.collect()) {
                collectName(text, "label/bossbar");
            }
        }
        return hit;
    }

    static net.minecraft.network.chat.Component bossBarPlain(
            net.minecraft.network.chat.Component bar, TranslationStore store) {
        String raw = bar.getString();
        String key = raw.strip();
        if (key.isEmpty() || !key.equals(com.wynnchayuan.capture.GlyphSplitter.stripGlyphChars(key))) {
            return null;
        }
        String dst = store.lookup(key);
        if (dst == null || dst.isEmpty() || dst.contains("{")) {
            return null;
        }
        int same = 0;
        while (same < key.length() && same < dst.length()
                && key.charAt(key.length() - 1 - same) == dst.charAt(dst.length() - 1 - same)) {
            same++;
        }
        if (same == 0) {
            return null;
        }
        int start = raw.indexOf(key);
        int keep = start + key.length() - same;
        String head = raw.substring(0, start) + dst.substring(0, dst.length() - same);
        net.minecraft.network.chat.MutableComponent out =
                net.minecraft.network.chat.Component.empty();
        int[] at = {0};
        boolean[] headDone = {false};
        bar.visit((style, text) -> {
            int from = at[0];
            int to = from + text.length();
            at[0] = to;
            if (!headDone[0] && !text.isEmpty()) {
                out.append(net.minecraft.network.chat.Component.literal(head).withStyle(style));
                headDone[0] = true;
            }
            if (to > keep && !text.isEmpty()) {
                out.append(net.minecraft.network.chat.Component.literal(
                        text.substring(Math.max(0, keep - from))).withStyle(style));
            }
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return headDone[0] ? out : null;
    }

    static net.minecraft.network.chat.Component bossBarName(
            net.minecraft.network.chat.Component bar, TranslationStore store) {
        String plain = bar.getString();
        int cut = plain.indexOf(" - ");
        if (cut <= 0) {
            return null;
        }
        String head = plain.substring(0, cut).strip();
        if (head.isEmpty()) {
            return null;
        }
        String dst = store.lookup(head);
        if (dst == null) {
            String plate = store.lookup(head + " {#}{#}");
            if (plate != null && plate.endsWith("{#}{#}")) {
                dst = plate.substring(0, plate.length() - "{#}{#}".length()).strip();
            }
        }
        if (dst == null || dst.isEmpty() || dst.contains("{")) {
            return null;
        }
        String translated = dst;
        net.minecraft.network.chat.MutableComponent out =
                net.minecraft.network.chat.Component.empty();
        boolean[] done = {false};
        bar.visit((style, text) -> {
            int at = done[0] ? -1 : text.indexOf(head);
            if (at >= 0) {
                out.append(net.minecraft.network.chat.Component.literal(
                        text.substring(0, at) + translated
                                + text.substring(at + head.length())).withStyle(style));
                done[0] = true;
            } else if (!text.isEmpty()) {
                out.append(net.minecraft.network.chat.Component.literal(text).withStyle(style));
            }
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return done[0] ? out : null;
    }

    static net.minecraft.network.chat.Component bossBarWords(
            net.minecraft.network.chat.Component bar, TranslationStore store) {
        net.minecraft.network.chat.MutableComponent out =
                net.minecraft.network.chat.Component.empty();
        boolean[] changed = {false};
        bar.visit((style, text) -> {
            if (text.isEmpty()) {
                return java.util.Optional.empty();
            }
            java.util.regex.Matcher m = WORD.matcher(text);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                String dst = store.scopedLookup("bossbar", m.group());
                m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(
                        dst != null ? dst : m.group()));
                changed[0] |= dst != null;
            }
            m.appendTail(sb);
            out.append(net.minecraft.network.chat.Component.literal(sb.toString())
                    .withStyle(style));
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return changed[0] ? out : null;
    }

    private static final java.util.regex.Pattern WORD =
            java.util.regex.Pattern.compile("[A-Za-z]+");

    private static final java.util.Map<net.minecraft.network.chat.Component,
            net.minecraft.network.chat.Component> BARS = new java.util.HashMap<>();
    private static int barAge = -1;

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

    public static net.minecraft.network.chat.Component entityName(
            net.minecraft.network.chat.Component name,
            net.minecraft.world.entity.Entity entity) {
        try {
            return entityName(name, entity, WynnChaYuan.config(), WynnChaYuan.translations());
        } catch (Throwable t) {
            ErrorDebug.note("WynntilsText#entityName", null, t);
            return name;
        }
    }

    static net.minecraft.network.chat.Component entityName(
            net.minecraft.network.chat.Component name, CollectorConfig config,
            TranslationStore store) {
        return entityName(name, null, config, store);
    }

    // 盔甲座浮空字不是 TextDisplay，Wynntils 名牌事件收不到
    static net.minecraft.network.chat.Component entityName(
            net.minecraft.network.chat.Component name,
            net.minecraft.world.entity.Entity entity, CollectorConfig config,
            TranslationStore store) {
        if (name == null || store == null || config == null
                || config.nametagMode() == CollectorConfig.NametagMode.OFF) {
            return name;
        }
        if (nameAge != store.generation()) {
            NAMES.clear();
            INDICATORS.clear();
            synchronized (NAMES_NOTED) {
                NAMES_NOTED.clear();
            }
            nameAge = store.generation();
        }
        Name entry = NAMES.get(name);
        if (entry == null) {
            entry = INDICATORS.get(name);
        }
        if (entry == null) {
            net.minecraft.network.chat.Component hit;
            StyledText text = StyledText.fromComponent(name);
            net.minecraft.network.chat.Component floating =
                    LineTranslator.translateFloating(text, store);
            com.wynnchayuan.translate.NameplateProbe.check("盔甲座名稱", text, floating, store);
            StyledText shown = floating != null
                    ? StyledText.fromComponent(floating)
                    : byPart(text, store);
            hit = shown == text ? name : shown.getComponent();
            if (hit == name && config.collect()) {
                collectName(text, "label/floating");
            }
            boolean indicator = com.wynnchayuan.capture.CombatText.isIndicator(text);
            StyledText original = !name.getString().isBlank()
                    && !com.wynnchayuan.capture.GlyphSplitter.isGlyphOnly(text)
                    && !indicator ? text : null;
            entry = new Name(hit, original);
            (indicator ? INDICATORS : NAMES).put(name, entry);
        }
        if (entity != null && entry.original() != null) {
            LookAtTranslator.remember(entity, entry.original());
        }
        if (config.nametagMode() == CollectorConfig.NametagMode.LOOK_AT) {
            return name;
        }
        return entry.shown();
    }

    private static void collectName(StyledText text, String ctx) {
        if (text.isEmpty() || com.wynnchayuan.capture.GlyphSplitter.isGlyphOnly(text)
                || com.wynnchayuan.capture.CombatText.isIndicator(text)) {
            return;
        }
        if (!firstTime(NAMES_NOTED, ctx + '\0' + text.getString())) {
            return;
        }
        var captured = WynnChaYuan.store();
        com.wynnchayuan.capture.LineParts owned =
                LineTranslator.nameFramed(text, WynnChaYuan.translations());
        if (owned != null) {
            if (captured != null) {
                captured.record(owned.template(), "name", "label", ctx);
            }
            return;
        }
        String template = com.wynnchayuan.capture.GlyphSplitter.toTemplate(text);
        if (template.isBlank() || !com.wynnchayuan.capture.GlyphSplitter.hasLetter(template)
                || com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(template)
                || com.wynnchayuan.capture.PlayerDataFilter.looksPlayerNamed(template)
                || com.wynnchayuan.capture.PlayerDataFilter.mentionsOnlinePlayerLoose(template)) {
            return;
        }
        if (captured != null) {
            captured.record(template, "name", "label", ctx);
        }
    }

    private record Name(net.minecraft.network.chat.Component shown, StyledText original) {}

    // 實測 60 個名牌全部重翻一幀要 4～9ms
    private static final java.util.Map<net.minecraft.network.chat.Component, Name> NAMES = lru(512);
    private static final java.util.Map<net.minecraft.network.chat.Component, Name> INDICATORS = lru(256);
    private static final java.util.Map<String, Boolean> NAMES_NOTED = lru(1024);
    private static int nameAge = -1;

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

    public static StyledText screenText(StyledText text) {
        try {
            return screenText(text, WynnChaYuan.config(), WynnChaYuan.translations());
        } catch (Throwable t) {
            ErrorDebug.note("WynntilsText#screenText", null, t);
            return text;
        }
    }

    private static volatile boolean rawText;

    public static void holdRawText(boolean on) {
        rawText = on;
    }

    private record Board(java.util.Set<String> shown,
                         java.util.Map<String, StyledText> replaced,
                         java.util.Set<String> kept) {}

    private static volatile Board board =
            new Board(java.util.Set.of(), java.util.Map.of(), java.util.Set.of());

    public static void setScoreboard(java.util.Set<String> shown,
                                     java.util.Map<String, StyledText> replaced) {
        setScoreboard(shown, replaced, java.util.Set.of());
    }

    public static void setScoreboard(java.util.Set<String> shown,
                                     java.util.Map<String, StyledText> replaced,
                                     java.util.Set<String> kept) {
        board = new Board(java.util.Set.copyOf(shown), java.util.Map.copyOf(replaced),
                java.util.Set.copyOf(kept));
    }

    static StyledText screenText(StyledText text, CollectorConfig config,
                                 TranslationStore store) {
        if (text == null || text.isEmpty() || store == null
                || config == null || !config.wynntilsUi()) {
            return text;
        }
        if (rawText) {
            return text;
        }
        Board now = board;
        if (!now.shown().isEmpty()) {
            String plain = text.getStringWithoutFormatting();
            if (now.kept().contains(plain)) {
                return text;
            }
            if (now.shown().contains(plain)) {
                if (!config.translateScoreboard()) {
                    return text;
                }
                StyledText whole = now.replaced().get(plain);
                if (whole != null) {
                    return whole;
                }
            }
        }
        boolean content = contentScreen.getAsBoolean();
        Cached seen;
        synchronized (SCREEN_CACHE) {
            seen = SCREEN_CACHE.get(text);
        }
        if (seen != null && seen.fresh(store) && seen.content() == content) {
            return seen.out() == null ? text : seen.out();
        }
        net.minecraft.network.chat.Component hit;
        if (statusRow(text.getStringWithoutFormatting())) {
            try (LookupScope scope = LookupScope.barItemNames(true)) {
                hit = LineTranslator.translate(text, store);
            }
        } else {
            hit = LineTranslator.translate(text, store);
        }
        if (hit != null && text.getString().indexOf('\n') < 0) {
            hit = ScoreboardFlow.oneLine(hit);
        }
        if (content) {
            String title = store.pageTitleName(text.getStringWithoutFormatting());
            if (title != null && (hit == null || !hit.getString().equals(title))) {
                hit = net.minecraft.network.chat.Component.literal(title).withStyle(
                        styleOf(text).withFont(net.minecraft.network.chat.FontDescription.DEFAULT));
            }
        }
        if (hit != null && store.pageNameMode() != CollectorConfig.ItemNames.ON
                && store.isPageName(text.getStringWithoutFormatting())) {
            hit = com.wynnchayuan.translate.PageNames.bare(
                    text.getComponent(), hit, store.pageNameMode());
        }
        StyledText out = hit == null ? null : StyledText.fromComponent(hit);
        synchronized (SCREEN_CACHE) {
            SCREEN_CACHE.put(text, new Cached(store.generation(),
                    store.namesWithOriginal(), store.translatesNames(),
                    store.peeksPlainNames(), store.pageNameMode(), content, out));
        }
        return out == null ? text : out;
    }

    // 以 Wynntils 套件名比對，它搬套件時會安靜失效
    static java.util.function.BooleanSupplier contentScreen = WynntilsText::onContentScreen;

    private static boolean onContentScreen() {
        try {
            net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
            Object screen = client == null ? null : client.screen;
            if (screen == null) {
                return false;
            }
            String name = screen.getClass().getName();
            return name.startsWith("com.wynntils.screens.activities.")
                    || name.startsWith("com.wynntils.screens.maps.");
        } catch (Throwable t) {
            ErrorDebug.note("WynntilsText#onContentScreen", null, t);
            return false;
        }
    }

    static boolean statusRow(String plain) {
        return plain != null && STATUS_TIMER.matcher(plain).find();
    }

    private static final java.util.regex.Pattern STATUS_TIMER = java.util.regex.Pattern.compile(
            "\\((?:\\d{1,2}:)?(?:\\d{1,2}:\\d{2}|\\*\\*:\\*\\*)\\)\\s*$");

    private static final int SCREEN_CACHE_LIMIT = 512;

    private record Cached(int generation, boolean namesWithOriginal,
                          boolean translateNames, boolean peekPlain,
                          CollectorConfig.ItemNames pageNames, boolean content, StyledText out) {

        boolean fresh(TranslationStore store) {
            return generation == store.generation()
                    && namesWithOriginal == store.namesWithOriginal()
                    && translateNames == store.translatesNames()
                    && peekPlain == store.peeksPlainNames()
                    && pageNames == store.pageNameMode();
        }
    }

    private static final java.util.Map<StyledText, Cached> SCREEN_CACHE =
            new java.util.LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(
                        java.util.Map.Entry<StyledText, Cached> eldest) {
                    return size() > SCREEN_CACHE_LIMIT;
                }
            };

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
}
