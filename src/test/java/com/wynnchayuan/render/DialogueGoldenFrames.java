package com.wynnchayuan.render;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class DialogueGoldenFrames {

    private static final int WIDTH = 232;
    private static final int STILL_FRAMES = 12;
    private static final int STEADY_AFTER = 6;
    private static final int MAX_ROWS = 5;

    private DialogueGoldenFrames() {}

    public static List<String> inPlace(String src, TranslationStore store) {
        DialogueTyping.forget();
        List<String> frames = new ArrayList<>(src.length() + STILL_FRAMES);
        for (int at = 1; at <= src.length(); at++) {
            frames.add(inPlaceFrame(src.substring(0, at), store));
        }
        for (int again = 0; again < STILL_FRAMES; again++) {
            frames.add(inPlaceFrame(src, store));
        }
        return frames;
    }

    public static List<String> panel(String src, TranslationStore store,
                                     Function<Component, String> flatten) {
        List<String> frames = new ArrayList<>(src.length() + STILL_FRAMES);
        String[] cache = new String[3];
        for (int at = 1; at <= src.length(); at++) {
            frames.add(panelFrame(src.substring(0, at), store, cache, false, flatten));
        }
        for (int again = 0; again < STILL_FRAMES; again++) {
            frames.add(panelFrame(src, store, cache, again >= STEADY_AFTER, flatten));
        }
        return frames;
    }

    private static String inPlaceFrame(String raw, TranslationStore store) {
        try {
            return DialogueTyping.line(raw, store, rowsFor(raw), null, WIDTH);
        } catch (RuntimeException e) {
            return "!" + e.getClass().getSimpleName();
        }
    }

    private static String panelFrame(String raw, TranslationStore store, String[] cache,
                                     boolean steady, Function<Component, String> flatten) {
        try {
            StyledText line = DialogueIcons.line(raw, store);
            String template = GlyphSplitter.toTemplate(line);
            String before = cache[2];
            cache[2] = raw;
            if (DialogueOverlay.canReuse(cache[0], template, before, raw)) {
                return cache[1];
            }
            DialogueOverlay.LineResult result =
                    DialogueOverlay.translateLine(line, template, store, steady);
            if (result == null) {
                return null;
            }
            cache[0] = result.source();
            cache[1] = flatten.apply(result.translated());
            return cache[1];
        } catch (RuntimeException e) {
            return "!" + e.getClass().getSimpleName();
        }
    }

    private static int rowsFor(String raw) {
        for (int rows = 1; rows <= MAX_ROWS; rows++) {
            if (DialogueLayout.wrap(raw, rows, null, WIDTH) != null) {
                return rows;
            }
        }
        return MAX_ROWS;
    }
}
