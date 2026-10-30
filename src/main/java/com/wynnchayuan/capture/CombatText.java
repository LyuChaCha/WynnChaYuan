package com.wynnchayuan.capture;

import com.wynntils.core.text.StyledText;

import java.util.Set;

public final class CombatText {

    private static final Set<String> WORDS = Set.of(
            "dodged", "blocked", "missed", "immune", "absorbed",
            "critical", "crit", "resisted", "evaded");

    private CombatText() {}

    public static boolean isIndicator(StyledText text) {
        return text != null && isIndicator(text.getStringWithoutFormatting());
    }

    public static boolean isIndicator(String raw) {
        if (raw == null) {
            return false;
        }
        String core = strip(raw);
        if (core.isEmpty()) {
            return false;
        }
        if (WORDS.contains(core.toLowerCase(java.util.Locale.ROOT))) {
            return true;
        }
        return isNumericOnly(core);
    }

    private static String strip(String raw) {
        StringBuilder sb = new StringBuilder();
        raw.codePoints().forEach(cp -> {
            if (!GlyphSplitter.isGlyphCodePoint(cp)) {
                sb.appendCodePoint(cp);
            }
        });
        String core = sb.toString().strip();
        while (core.endsWith("!")) {
            core = core.substring(0, core.length() - 1).strip();
        }
        return core;
    }

    private static boolean isNumericOnly(String core) {
        boolean hasDigit = false;
        for (int i = 0; i < core.length(); i++) {
            char c = core.charAt(i);
            if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (Character.isLetter(c) && !isUnitSuffix(core, i)) {
                return false;
            }
        }
        return hasDigit;
    }

    private static boolean isUnitSuffix(String core, int at) {
        if (at == 0 || !Character.isDigit(core.charAt(at - 1))) {
            return false;
        }
        return at + 1 >= core.length() || !Character.isLetter(core.charAt(at + 1));
    }
}
