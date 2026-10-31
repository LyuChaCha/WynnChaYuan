package com.wynnchayuan.translate;

import com.wynnchayuan.ThirdPartyField;
import com.wynnchayuan.WynnChaYuan;
import net.fabricmc.loader.api.FabricLoader;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class WmsBridge {

    private WmsBridge() {}

    static Supplier<MarketSearch> source = () ->
            WynnChaYuan.translations().market();
    static BooleanSupplier enabled = () ->
            WynnChaYuan.config().wynnMarketSearch();

    private static volatile Boolean installed;

    public static boolean installed() {
        Boolean known = installed;
        if (known == null) {
            boolean found;
            try {
                found = FabricLoader.getInstance().isModLoaded("wms");
            } catch (Throwable t) {
                ErrorDebug.note("WmsBridge#installed", null, t);
                found = false;
            }
            installed = known = found;
        }
        return known;
    }

    private record Answer(String query, int indexSize, Set<String> english) {}

    private static volatile Answer last;

    private static final ThirdPartyField ITEM_NAME = new ThirdPartyField("name");

    public static boolean matchesItem(Object item, String query) {
        return ITEM_NAME.get(item) instanceof String english && matches(english, query);
    }

    public static boolean matches(String english, String query) {
        if (english == null || query == null) {
            return false;
        }
        try {
            Set<String> hits = resolve(query);
            return !hits.isEmpty()
                    && hits.contains(MarketSearch.singular(english).toLowerCase(Locale.ROOT));
        } catch (Throwable t) {
            ErrorDebug.note("WmsBridge#matches", null, t);
            return false;
        }
    }

    private static Set<String> resolve(String query) {
        String typed = query.strip();
        if (tooShort(typed) || !enabled.getAsBoolean()) {
            return Set.of();
        }
        MarketSearch market = source.get();
        if (market == null) {
            return Set.of();
        }
        Answer cached = last;
        if (cached != null && cached.query().equals(typed) && cached.indexSize() == market.size()) {
            return cached.english();
        }
        Set<String> english = new HashSet<>();
        for (MarketSearch.Suggestion s : market.suggestions(typed)) {
            english.add(s.english().toLowerCase(Locale.ROOT));
        }
        last = new Answer(typed, market.size(), english);
        return english;
    }

    private static boolean tooShort(String typed) {
        return typed.isEmpty() || (typed.codePointCount(0, typed.length()) < 2 && typed.codePointAt(0) < 0x80);
    }

    public static String translatedName(String english) {
        if (english == null || english.isEmpty()) {
            return null;
        }
        try {
            if (!enabled.getAsBoolean()) {
                return null;
            }
            MarketSearch market = source.get();
            return market == null ? null : market.shownFor(english);
        } catch (Throwable t) {
            ErrorDebug.note("WmsBridge#translatedName", null, t);
            return null;
        }
    }
}
