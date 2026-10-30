package com.wynnchayuan.translate;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Optional;

final class DisplayStyles {

    private DisplayStyles() {}

    // /class 畫面的高度烘進字型 ascent，換 default 會整段移位
    static Style forDisplay(Style style) {
        Style base = style == null ? Style.EMPTY : style;
        FontDescription paired = com.wynnchayuan.render.PairedFont.forStyle(base);
        return base.withFont(paired == null ? FontDescription.DEFAULT : paired);
    }

    private static final java.util.regex.Pattern WIDE_SCRIPT =
            java.util.regex.Pattern.compile(
                    "[\\u0400-\\u04ff\\u3040-\\u30ff\\u3400-\\u4dbf"
                            + "\\u4e00-\\u9fff\\uac00-\\ud7af\\uff00-\\uffef]");

    static Style numberDisplay(Style style) {
        Style big = displayFont(style);
        return big != null ? big : forDisplay(style);
    }

    static Style displayFont(Style style) {
        return style != null && style.getFont() instanceof FontDescription.Resource r
                && r.id() != null && r.id().getPath().startsWith("offset/") ? style : null;
    }

    static Component literal(String text, Style style) {
        text = ColourTemplate.stripColourTokens(text);
        text = deIcon(text);
        if (style != null && com.wynnchayuan.render.PairedFont.isOurs(style)
                && !WIDE_SCRIPT.matcher(text).find()) {
            style = style.withFont(FontDescription.DEFAULT);
        }
        Style base = upright(text, style);
        if (style != null && style.getFont() != null
                && !FontDescription.DEFAULT.equals(style.getFont())) {
            base = base.withItalic(false);
        }
        return text.indexOf(SECTION) < 0
                ? Component.literal(text).withStyle(base)
                : coloured(text, base);
    }

    // Wynncraft 資源包把 Á 指到全螢幕黑幕 fade.png
    static String deIcon(String text) {
        return text == null || text.indexOf(HIJACKED_LETTER) < 0
                ? text : text.replace(HIJACKED_LETTER, SAFE_LETTER);
    }

    private static final char HIJACKED_LETTER = 'Á';

    private static final char SAFE_LETTER = 'A';

    private static final char SECTION = '§';

    static Component coloured(String text, Style base) {
        MutableComponent out = Component.empty();
        Style now = base;
        int from = 0;
        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) != SECTION) {
                continue;
            }
            ChatFormatting code = ChatFormatting.getByCode(
                    Character.toLowerCase(text.charAt(i + 1)));
            if (code == null) {
                continue;
            }
            if (i > from) {
                out.append(Component.literal(text.substring(from, i)).withStyle(now));
            }
            now = code == ChatFormatting.RESET ? base : now.applyFormat(code);
            i++;
            from = i + 1;
        }
        if (from < text.length()) {
            out.append(Component.literal(text.substring(from)).withStyle(now));
        }
        return out;
    }

    private static Style upright(String text, Style style) {
        Style base = style == null ? Style.EMPTY : style;
        return base.withItalic(base.isItalic() && !hasHan(text));
    }

    // MC 對自訂名稱物品自動加斜體，Wynncraft 沒關
    static Component unslantAll(Component line) {
        if (line == null) {
            return null;
        }
        MutableComponent out = Component.empty();
        line.visit((style, text) -> {
            if (!text.isEmpty()) {
                out.append(Component.literal(text).withStyle(style.withItalic(false)));
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    static Component unslant(Component line) {
        if (line == null || !hasHan(line.getString())) {
            return line;
        }
        MutableComponent out = Component.empty();
        line.visit((style, text) -> {
            if (!text.isEmpty()) {
                out.append(Component.literal(text).withStyle(style.withItalic(false)));
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static boolean hasHan(String text) {
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            if (Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN) {
                return true;
            }
            i += Character.charCount(cp);
        }
        return false;
    }
}
