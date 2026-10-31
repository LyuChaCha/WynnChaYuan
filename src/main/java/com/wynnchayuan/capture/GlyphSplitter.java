package com.wynnchayuan.capture;

import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GlyphSplitter {

    public static final String GLYPH_PLACEHOLDER = "{#}";

    public static final String NUMBER_PLACEHOLDER = "{~}";

    public static final String PLACE_PLACEHOLDER = "{p}";

    public static final String PLAYER_PLACEHOLDER = "{u}";

    private static final Pattern NUMBERS = Pattern.compile("\\d+(?:[.,]\\d+)*%?");

    private static final Identifier DEFAULT_FONT = Identifier.withDefaultNamespace("default");

    private GlyphSplitter() {}

    public static boolean isGlyphPart(StyledTextPart part) {
        String text = part.getString(null, StyleType.NONE);
        if (text.isBlank()) {
            return false;
        }
        if (isAllGlyphChars(text)) {
            return true;
        }
        PartStyle style = part.getPartStyle();
        if (style == null || !isCustomFont(style.getFont())) {
            return false;
        }
        if (isTextFont(style.getFont())) {
            return false;
        }
        // Wynncraft 數值也用自訂字型畫（+2 帶 wynntils:language）
        return !hasReadable(text);
    }

    private static boolean isTextFont(FontDescription font) {
        if (!(font instanceof FontDescription.Resource resource)) {
            return false;
        }
        Identifier id = resource.id();
        return id != null && id.getPath().startsWith("language/");
    }

    private static boolean hasReadable(String s) {
        for (int at = 0; at < s.length(); ) {
            int cp = s.codePointAt(at);
            at += Character.charCount(cp);
            if (!isGlyphCodePoint(cp) && (Character.isLetter(cp) || Character.isDigit(cp))) {
                return true;
            }
        }
        return false;
    }

    // 1.21.11 一般文字帶非 null 的 FontDescription.DEFAULT
    public static boolean isCustomFont(FontDescription font) {
        if (font == null) {
            return false;
        }
        if (font instanceof FontDescription.Resource resource) {
            Identifier id = resource.id();
            return id != null && !DEFAULT_FONT.equals(id);
        }
        return true;
    }

    public static boolean isGlyphOnly(StyledText text) {
        for (StyledTextPart part : text) {
            if (!isGlyphPart(part) && hasLetter(part.getString(null, StyleType.NONE))) {
                return false;
            }
        }
        return true;
    }

    public static String toTemplate(StyledText text) {
        return LineParts.of(text).template();
    }

    public static String parametrizeNumbers(String s) {
        return s.isEmpty() ? s : NUMBERS.matcher(s).replaceAll(NUMBER_PLACEHOLDER);
    }

    public static Matcher numberMatcher(String s) {
        return NUMBERS.matcher(s);
    }

    public static boolean hasDigit(String s) {
        for (int at = 0; at < s.length(); at++) {
            char c = s.charAt(at);
            if (c >= '0' && c <= '9') {
                return true;
            }
        }
        return false;
    }

    public static String stripGlyphChars(String s) {
        StringBuilder out = null;
        for (int at = 0; at < s.length(); ) {
            int cp = s.codePointAt(at);
            int next = at + Character.charCount(cp);
            if (isGlyphCodePoint(cp)) {
                if (out == null) {
                    out = new StringBuilder(s.length()).append(s, 0, at);
                }
            } else if (out != null) {
                out.append(s, at, next);
            }
            at = next;
        }
        return out == null ? s : out.toString();
    }

    // Wynncraft 對話游標用 U+D0000–U+D0074（第 13 平面）
    public static boolean isGlyphCodePoint(int cp) {
        int type = Character.getType(cp);
        return type == Character.PRIVATE_USE
            || type == Character.UNASSIGNED;
    }

    public static boolean isPrivateUse(int cp) {
        return Character.getType(cp) == Character.PRIVATE_USE;
    }

    private static boolean isAllGlyphChars(String s) {
        if (s.isBlank()) {
            return false;
        }
        for (int at = 0; at < s.length(); ) {
            int cp = s.codePointAt(at);
            at += Character.charCount(cp);
            if (!Character.isWhitespace(cp) && !isGlyphCodePoint(cp)) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasLetter(String s) {
        for (int at = 0; at < s.length(); ) {
            int cp = s.codePointAt(at);
            at += Character.charCount(cp);
            if (Character.isLetter(cp) && !isGlyphCodePoint(cp)) {
                return true;
            }
        }
        return false;
    }

    public static final List<String> STATUS_WORDS = List.of("Distorted");

    private static final Pattern STATUS_WORD = Pattern.compile(
            "\\b(?:" + String.join("|", STATUS_WORDS) + ")\\b");

    public static boolean hasWordBesidesStatus(String s) {
        return s != null && hasWord(STATUS_WORD.matcher(s).replaceAll(" "));
    }

    public static boolean hasWord(String s) {
        if (s == null) {
            return false;
        }
        int run = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (isHan(c)) {
                return true;
            }
            if (Character.isLetter(c) && !isGlyphCodePoint(c)) {
                if (++run >= 2) {
                    return true;
                }
            } else {
                run = 0;
            }
        }
        return false;
    }

    private static boolean isHan(char c) {
        return c >= 0x3400 && c <= 0x9FFF;
    }
}
