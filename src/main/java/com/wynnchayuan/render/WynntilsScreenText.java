package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.LookupScope;
import com.wynnchayuan.translate.PageNames;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

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

    private record Board(Set<String> shown, Map<String, StyledText> replaced, Set<String> kept) {}

    private static volatile Board board =
            new Board(Set.of(), Map.of(), Set.of());

    static void setScoreboard(Set<String> shown, Map<String, StyledText> replaced) {
        setScoreboard(shown, replaced, Set.of());
    }

    static void setScoreboard(Set<String> shown, Map<String, StyledText> replaced,
                              Set<String> kept) {
        board = new Board(Set.copyOf(shown), Map.copyOf(replaced), Set.copyOf(kept));
    }

    static StyledText screenText(StyledText text, ModConfig config,
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
        Component hit;
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
                hit = Component.literal(title).withStyle(
                        WynntilsText.styleOf(text).withFont(FontDescription.DEFAULT));
            }
        }
        if (hit != null && store.pageNameMode() != ModConfig.ItemNames.ON
                && store.isPageName(text.getStringWithoutFormatting())) {
            hit = PageNames.bare(text.getComponent(), hit, store.pageNameMode());
        }
        StyledText out = hit == null ? null : StyledText.fromComponent(hit);
        synchronized (SCREEN_CACHE) {
            SCREEN_CACHE.put(text, new Cached(store.cacheAge(), store.pageNameMode(), content, out));
        }
        return out == null ? text : out;
    }

    // 以 Wynntils 套件名比對，它搬套件時會安靜失效
    static BooleanSupplier contentScreen = WynntilsScreenText::onContentScreen;

    private static boolean onContentScreen() {
        Minecraft client = Minecraft.getInstance();
        Object screen = client == null ? null : client.screen;
        if (screen == null) {
            return false;
        }
        String name = screen.getClass().getName();
        return name.startsWith("com.wynntils.screens.activities.")
                || name.startsWith("com.wynntils.screens.maps.");
    }

    static boolean statusRow(String plain) {
        return plain != null && STATUS_TIMER.matcher(plain).find();
    }

    private static final Pattern STATUS_TIMER = Pattern.compile(
            "\\((?:\\d{1,2}:)?(?:\\d{1,2}:\\d{2}|\\*\\*:\\*\\*)\\)\\s*$");

    private static final int SCREEN_CACHE_LIMIT = 512;

    record Cached(long age, ModConfig.ItemNames pageNames, boolean content, StyledText out) {

        boolean fresh(TranslationStore store) {
            return age == store.cacheAge() && pageNames == store.pageNameMode();
        }
    }

    static final Map<StyledText, Cached> SCREEN_CACHE = new LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<StyledText, Cached> eldest) {
                    return size() > SCREEN_CACHE_LIMIT;
                }
            };
}
