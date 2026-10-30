package com.wynnchayuan.translate;

import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

public final class GuessListRow {

    private GuessListRow() {}

    private static final int MIN_INDENT = 4;

    public record Row(Component translated) {}

    public static Row of(StyledText line, TranslationStore store) {
        if (line == null || store == null
                || !line.getStringWithoutFormatting().startsWith(" ".repeat(MIN_INDENT))) {
            return null;
        }
        List<String> texts = new ArrayList<>();
        List<Style> styles = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        for (StyledTextPart part : line) {
            String raw = part.getString(null, StyleType.NONE);
            if (raw.isEmpty()) {
                continue;
            }
            PartStyle ps = part.getPartStyle();
            Style style = ps == null ? Style.EMPTY : ps.getStyle();
            for (int i = 0; i < raw.length(); ) {
                int cp = raw.codePointAt(i);
                int len = Character.charCount(cp);
                texts.add(raw.substring(i, i + len));
                styles.add(style);
                i += len;
            }
            plain.append(raw);
        }
        String text = plain.toString();
        if (text.indexOf('\n') >= 0) {
            return null;
        }
        int indent = 0;
        while (indent < text.length() && text.charAt(indent) == ' ') {
            indent++;
        }
        if (indent < MIN_INDENT || indent == text.length()) {
            return null;
        }
        int end = text.length();
        boolean trailingComma = text.charAt(end - 1) == ',';
        if (trailingComma) {
            end--;
        }
        String body = text.substring(indent, end);
        String[] names = body.split(", ", -1);
        int[] starts = new int[names.length];
        int at = indent;
        for (int k = 0; k < names.length; k++) {
            String name = names[k];
            if (name.isEmpty() || !name.equals(name.strip()) || !hasLetter(name)
                    || !store.isItemName(name) || !plainChars(name)) {
                return null;
            }
            starts[k] = at;
            at += name.length() + 2;
        }
        List<Integer> charAt = charIndex(texts);
        String[] shown = new String[names.length];
        boolean any = false;
        boolean held = TranslationStore.appendedOriginalHeld();
        boolean onName = TranslationStore.onNameLine();
        try {
            TranslationStore.holdAppendedOriginal(false);
            TranslationStore.nameLine(true);
            for (int k = 0; k < names.length; k++) {
                shown[k] = name(names[k], store);
                any |= shown[k] != null;
            }
        } finally {
            TranslationStore.holdAppendedOriginal(held);
            TranslationStore.nameLine(onName);
        }
        if (!any) {
            return new Row(null);
        }
        MutableComponent out = Component.empty();
        out.append(Component.literal(text.substring(0, indent))
                .withStyle(styles.get(charAt.get(0))));
        for (int k = 0; k < names.length; k++) {
            Style style = styles.get(charAt.get(starts[k]));
            if (k > 0) {
                Style comma = styles.get(charAt.get(starts[k] - 2));
                out.append(Component.literal(", ").withStyle(comma));
            }
            out.append(shown[k] == null
                    ? Component.literal(names[k]).withStyle(style)
                    : LineTranslator.literal(shown[k], style));
        }
        if (trailingComma) {
            out.append(Component.literal(",").withStyle(styles.get(styles.size() - 1)));
        }
        return new Row(out);
    }

    private static String name(String name, TranslationStore store) {
        if (store.isBareGearName(name)) {
            return null;
        }
        String hit = store.lookup(name);
        if (hit == null || hit.isBlank() || hit.equals(name) || hit.indexOf('{') >= 0
                || hit.indexOf('\n') >= 0) {
            return null;
        }
        return hit;
    }

    private static List<Integer> charIndex(List<String> texts) {
        List<Integer> out = new ArrayList<>();
        for (int k = 0; k < texts.size(); k++) {
            for (int j = 0; j < texts.get(k).length(); j++) {
                out.add(k);
            }
        }
        return out;
    }

    private static boolean hasLetter(String s) {
        return s.codePoints().anyMatch(Character::isLetter);
    }

    private static boolean plainChars(String s) {
        return s.codePoints().noneMatch(cp -> (cp >= 0xE000 && cp <= 0xF8FF) || cp >= 0xD0000
                || SpaceOffset.isOffset(cp));
    }
}
