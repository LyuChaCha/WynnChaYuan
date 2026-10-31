package com.wynnchayuan.translate;

import com.wynntils.core.text.PartStyle;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public final class SpaceOffset {

    private static final int ZERO = 0xD0000;

    private static final int LIMIT = 1024;

    private SpaceOffset() {}

    public static boolean isSpaceFont(PartStyle style) {
        if (style == null) {
            return false;
        }
        FontDescription font = style.getFont();
        if (!(font instanceof FontDescription.Resource resource) || resource.id() == null) {
            return false;
        }
        return "space".equals(resource.id().getPath());
    }

    public static boolean isSpaceFont(Style style) {
        if (style == null) {
            return false;
        }
        FontDescription font = style.getFont();
        if (!(font instanceof FontDescription.Resource resource) || resource.id() == null) {
            return false;
        }
        return "space".equals(resource.id().getPath());
    }

    // U+D0000+n 只在 minecraft:space 字型畫得出來
    public static Style styleFor(Style base) {
        return (base == null ? Style.EMPTY : base)
                .withFont(new FontDescription.Resource(
                        Identifier.withDefaultNamespace("space")));
    }

    public static boolean isOffsetRun(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (!isOffset(cp)) {
                return false;
            }
        }
        return true;
    }

    public static String leadingOffsets(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int end = 0;
        while (end < text.length() && isOffset(text.codePointAt(end))) {
            end += Character.charCount(text.codePointAt(end));
        }
        return text.substring(0, end);
    }

    public static boolean isOffset(int cp) {
        int px = cp - ZERO;
        return px >= -LIMIT && px <= LIMIT;
    }

    public static String trailingOffsets(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int end = text.length();
        while (end > 0) {
            int cp = text.codePointBefore(end);
            int px = cp - ZERO;
            if (px < -LIMIT || px > LIMIT) {
                break;
            }
            end -= Character.charCount(cp);
        }
        return text.substring(end);
    }

    public static int decode(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            int px = cp - ZERO;
            if (px >= -LIMIT && px <= LIMIT) {
                total += px;
            }
        }
        return total;
    }

    public static String encode(int pixels) {
        if (pixels == 0) {
            return "";
        }
        int clamped = Math.max(-LIMIT, Math.min(pixels, LIMIT));
        return new String(Character.toChars(ZERO + clamped));
    }
}
