package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class HotPathFrames {

    private HotPathFrames() {}

    public static List<Component> tooltip(List<Component> lines, TranslationStore store) {
        return TooltipTranslator.translateLines(lines, store);
    }

    public static List<Component> tooltipCached(List<Component> lines, TranslationStore store) {
        return TooltipTranslator.translateLinesCached(lines, store);
    }

    public static StyledText screenText(StyledText text, ModConfig config,
                                        TranslationStore store) {
        return WynntilsScreenText.screenText(text, config, store);
    }

    public static void scoreboardShows(List<StyledText> rows) {
        Set<String> plain = new HashSet<>();
        for (StyledText row : rows) {
            plain.add(row.getStringWithoutFormatting());
        }
        WynntilsScreenText.setScoreboard(plain, Map.of());
    }

    public static Component nameplate(Component name, ModConfig config,
                                      TranslationStore store) {
        return WynntilsNames.entityName(name, config, store);
    }
}
