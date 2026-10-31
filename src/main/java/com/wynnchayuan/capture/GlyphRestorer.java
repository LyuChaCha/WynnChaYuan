package com.wynnchayuan.capture;

import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

public final class GlyphRestorer {

    private GlyphRestorer() {}

    public static StyledText apply(StyledText original, String translated) {
        if (translated == null || translated.isBlank()) {
            return null;
        }

        List<String> glyphs = GlyphSplitter.extractGlyphs(original);
        List<String> chunks = splitOnPlaceholder(translated);
        if (chunks.size() - 1 != glyphs.size()) {
            return null;
        }

        PartStyle textStyle = firstTextStyle(original);
        List<PartStyle> glyphStyles = glyphStyles(original);

        List<StyledTextPart> parts = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            if (!chunk.isEmpty()) {
                parts.add(makePart(chunk, textStyle));
            }
            if (i < glyphs.size()) {
                parts.add(makePart(glyphs.get(i), glyphStyles.get(i)));
            }
        }
        return parts.isEmpty() ? null : StyledText.fromParts(parts);
    }

    private static List<String> splitOnPlaceholder(String translated) {
        List<String> out = new ArrayList<>();
        String ph = GlyphSplitter.GLYPH_PLACEHOLDER;
        int from = 0;
        while (true) {
            int at = translated.indexOf(ph, from);
            if (at < 0) {
                out.add(translated.substring(from));
                return out;
            }
            out.add(translated.substring(from, at));
            from = at + ph.length();
        }
    }

    private static StyledTextPart makePart(String text, PartStyle style) {
        return style == null
                ? new StyledTextPart(text, Style.EMPTY, null, null)
                : new StyledTextPart(text, style.getStyle(), null, null);
    }

    private static PartStyle firstTextStyle(StyledText original) {
        for (StyledTextPart part : original) {
            if (!GlyphSplitter.isGlyphPart(part)
                    && !part.getString(null, StyleType.NONE).isBlank()) {
                return part.getPartStyle();
            }
        }
        return null;
    }

    private static List<PartStyle> glyphStyles(StyledText original) {
        List<PartStyle> styles = new ArrayList<>();
        boolean inRun = false;
        for (StyledTextPart part : original) {
            if (part.getString(null, StyleType.NONE).isEmpty()) {
                continue;
            }
            if (GlyphSplitter.isGlyphPart(part)) {
                if (!inRun) {
                    styles.add(part.getPartStyle());
                    inRun = true;
                }
            } else {
                inRun = false;
            }
        }
        return styles;
    }
}
