package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.LookupScope;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;

final class WynntilsScreenText {

    private WynntilsScreenText() {}

    static StyledText screenText(StyledText text) {
        try {
            return screenText(text, WynnChaYuan.config(), WynnChaYuan.translations());
        } catch (Throwable t) {
            ErrorDebug.note("WynntilsScreenText#screenText", null, t);
            return text;
        }
    }

    private static volatile boolean rawText;

    static void holdRawText(boolean on) {
        rawText = on;
    }

    private record Board(java.util.Set<String> shown,
                         java.util.Map<String, StyledText> replaced,
                         java.util.Set<String> kept) {}

    private static volatile Board board =
            new Board(java.util.Set.of(), java.util.Map.of(), java.util.Set.of());

    static void setScoreboard(java.util.Set<String> shown,
                              java.util.Map<String, StyledText> replaced) {
        setScoreboard(shown, replaced, java.util.Set.of());
    }

    static void setScoreboard(java.util.Set<String> shown,
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
                        WynntilsText.styleOf(text).withFont(net.minecraft.network.chat.FontDescription.DEFAULT));
            }
        }
        if (hit != null && store.pageNameMode() != CollectorConfig.ItemNames.ON
                && store.isPageName(text.getStringWithoutFormatting())) {
            hit = com.wynnchayuan.translate.PageNames.bare(
                    text.getComponent(), hit, store.pageNameMode());
        }
        StyledText out = hit == null ? null : StyledText.fromComponent(hit);
        synchronized (SCREEN_CACHE) {
            SCREEN_CACHE.put(text, new Cached(store.cacheAge(), store.pageNameMode(), content, out));
        }
        return out == null ? text : out;
    }

    // 以 Wynntils 套件名比對，它搬套件時會安靜失效
    static java.util.function.BooleanSupplier contentScreen = WynntilsScreenText::onContentScreen;

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
            ErrorDebug.note("WynntilsScreenText#onContentScreen", null, t);
            return false;
        }
    }

    static boolean statusRow(String plain) {
        return plain != null && STATUS_TIMER.matcher(plain).find();
    }

    private static final java.util.regex.Pattern STATUS_TIMER = java.util.regex.Pattern.compile(
            "\\((?:\\d{1,2}:)?(?:\\d{1,2}:\\d{2}|\\*\\*:\\*\\*)\\)\\s*$");

    private static final int SCREEN_CACHE_LIMIT = 512;

    record Cached(long age, CollectorConfig.ItemNames pageNames, boolean content, StyledText out) {

        boolean fresh(TranslationStore store) {
            return age == store.cacheAge() && pageNames == store.pageNameMode();
        }
    }

    static final java.util.Map<StyledText, Cached> SCREEN_CACHE =
            new java.util.LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(
                        java.util.Map.Entry<StyledText, Cached> eldest) {
                    return size() > SCREEN_CACHE_LIMIT;
                }
            };
}
