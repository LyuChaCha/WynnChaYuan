package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;

import java.util.Map;
import java.util.WeakHashMap;

public final class TextDisplayLabels {

    // 伺服器不會重送沒變的名牌，換設定只能自己寫回實體
    private static final Map<Display.TextDisplay, StyledText> ORIGINALS = new WeakHashMap<>();

    private TextDisplayLabels() {}

    public static void remember(Display.TextDisplay label, StyledText original) {
        ORIGINALS.put(label, original);
    }

    public static void forget(Display.TextDisplay label) {
        ORIGINALS.remove(label);
    }

    public static void clear() {
        ORIGINALS.clear();
    }

    public static boolean replaces(ModConfig config) {
        return config.showOverlays() && config.nametagMode() == ModConfig.NametagMode.REPLACE;
    }

    public static void refresh(ModConfig config, TranslationStore store) {
        boolean replaces = replaces(config);
        for (Map.Entry<Display.TextDisplay, StyledText> entry : ORIGINALS.entrySet()) {
            Display.TextDisplay label = entry.getKey();
            if (label == null || label.isRemoved()) {
                continue;
            }
            Component translated = replaces
                    ? LineTranslator.translateLabel(entry.getValue(), store) : null;
            StyledText shown = translated == null
                    ? entry.getValue() : StyledText.fromComponent(translated);
            label.setText(shown.getComponent());
        }
    }
}
