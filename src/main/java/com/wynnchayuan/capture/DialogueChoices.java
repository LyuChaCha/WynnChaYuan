package com.wynnchayuan.capture;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

// 要認 /choice_：框線字型 style/default/choice 沒有底線
public final class DialogueChoices {

    private static final String CHOICE = "/choice_";

    private DialogueChoices() {}

    public static List<String> of(Component message) {
        if (message == null) {
            return List.of();
        }
        Map<Integer, StringBuilder> byIndex = new TreeMap<>();
        message.visit((style, text) -> {
            String font = fontOf(style);
            int at = font.indexOf(CHOICE);
            if (at < 0 || !readable(text)) {
                return Optional.empty();
            }
            Integer index = numberAfter(font, at + CHOICE.length());
            if (index == null) {
                return Optional.empty();
            }
            byIndex.computeIfAbsent(index, k -> new StringBuilder()).append(text);
            return Optional.empty();
        }, Style.EMPTY);

        List<String> out = new ArrayList<>(byIndex.size());
        for (StringBuilder each : byIndex.values()) {
            String text = each.toString().strip();
            if (!text.isEmpty()) {
                out.add(text);
            }
        }
        return List.copyOf(out);
    }

    private static final String CHOICE_STYLE = "style/default/choice";

    private static final String BLOCK_START =
            new String(Character.toChars(0xD0023));

    private static final char CURSOR = '\uE080';

    public static int selected(Component message) {
        if (message == null) {
            return 0;
        }
        int[] state = {0, 0};
        message.visit((style, text) -> {
            if (!fontOf(style).contains(CHOICE_STYLE) || text == null) {
                return Optional.empty();
            }
            if (text.equals(BLOCK_START)) {
                state[0]++;
            } else if (text.indexOf(CURSOR) >= 0 && state[1] == 0) {
                state[1] = state[0];
            }
            return Optional.empty();
        }, Style.EMPTY);
        return state[1];
    }

    private static Integer numberAfter(String font, int at) {
        int stop = at;
        while (stop < font.length() && Character.isDigit(font.charAt(stop))) {
            stop++;
        }
        if (stop == at) {
            return null;
        }
        try {
            return Integer.parseInt(font.substring(at, stop));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean readable(String text) {
        return text != null && text.codePoints().anyMatch(cp ->
                cp >= 0x20 && cp < 0x7F && !Character.isWhitespace(cp));
    }

    private static String fontOf(Style style) {
        return style.getFont() == null ? "" : style.getFont().toString();
    }
}
