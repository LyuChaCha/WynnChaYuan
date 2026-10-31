package com.wynnchayuan.capture;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.Optional;
import java.util.regex.Pattern;

public final class DialogueSpeaker {

    private static final String NAMEPLATE_FONT = "hud/dialogue/text/nameplate";

    private DialogueSpeaker() {}

    public static String of(StyledText text) {
        return text == null ? null : of(text.getComponent());
    }

    // Wynntils getDialogueText() 沒有名牌，只在原始 action bar
    public static void noteRaw(Component message) {
        if (message == null) {
            current = null;
            currentText = "";
            return;
        }
        current = of(message);
        currentText = letters(message.getString());
    }

    public static String current() {
        return current;
    }

    public static void noteName(String name) {
        current = named(name == null ? null : name.strip());
        currentText = "";
    }

    public static String currentFor(String dialogueText) {
        String name = current;
        if (name == null || dialogueText == null) {
            return null;
        }
        String said = letters(dialogueText);
        return !said.isEmpty() && currentText.contains(said) ? name : null;
    }

    public static String cleanName(String raw) {
        if (raw == null) {
            return null;
        }
        String first = raw.strip().split("\n", 2)[0];
        return named(GlyphSplitter.stripGlyphChars(first).strip());
    }

    private static String letters(String s) {
        StringBuilder out = new StringBuilder(s.length());
        s.codePoints().filter(Character::isLetterOrDigit)
                .filter(cp -> cp < 0xE000 || (cp > 0xF8FF && cp < 0xF0000))
                .forEach(out::appendCodePoint);
        return out.toString();
    }

    private static volatile String current;
    private static volatile String currentText = "";

    private static String of(Component component) {
        StringBuilder name = new StringBuilder();
        component.visit((style, part) -> {
            if (isNameplate(style)) {
                name.append(part);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return named(GlyphSplitter.stripGlyphChars(name.toString()).strip());
    }

    private static final Pattern MYSTERY = Pattern.compile("[?\\p{M}\\s]*\\?[?\\p{M}\\s]*");

    private static String named(String text) {
        if (text == null) {
            return null;
        }
        long letters = text.chars().filter(c -> (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')).count();
        if (letters >= 2) {
            return text;
        }
        return MYSTERY.matcher(text).matches() ? text : null;
    }

    private static boolean isNameplate(Style style) {
        return style != null && style.getFont() != null
                && style.getFont().toString().contains(NAMEPLATE_FONT);
    }
}
