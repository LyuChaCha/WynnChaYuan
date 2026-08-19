package com.wynncollect.capture;

import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;

import java.util.ArrayList;
import java.util.List;

/**
 * 把譯文套回原始文字，並保留材質包符號。
 *
 * <p>這是 {@link GlyphSplitter} 的反向操作。抽取時符號被換成
 * {@code {#}}，套用譯文時要把它們填回去——關鍵在於<b>符號連同原本的樣式一起填回</b>，
 * 而不是只填字元。符號的外觀來自它的 {@code font}，只還原字元會變成豆腐格。
 *
 * <p>因此這裡不是做字串拼接，而是重建一串 {@link StyledTextPart}：
 * 譯文的文字段落沿用原文第一個文字片段的樣式（顏色、粗體都跟著走），
 * {@code {#}} 的位置則整個塞回原本那個帶 font 的片段。
 *
 * <p>譯者可以自由調整 {@code {#}} 在句中的位置以配合中文語序；只要個數對得上，
 * 符號就會落在譯者指定的地方。個數不符時視為譯文有問題，回傳 null 讓呼叫端沿用原文，
 * 寧可不翻也不要破圖。
 */
public final class GlyphRestorer {

    private GlyphRestorer() {}

    /**
     * @param original   遊戲送來的原始文字
     * @param translated 譯文模板，{@code {#}} 標示符號位置
     * @return 套用後的文字；譯文為空或佔位符數量不符時回傳 {@code null}
     */
    public static StyledText apply(StyledText original, String translated) {
        if (translated == null || translated.isBlank()) {
            return null;
        }

        List<String> glyphs = GlyphSplitter.extractGlyphs(original);
        List<String> chunks = splitOnPlaceholder(translated);
        // chunks 之間的縫隙數就是譯文用到的佔位符數
        if (chunks.size() - 1 != glyphs.size()) {
            return null;                       // 譯者刪了或多加了符號，放棄套用
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
                // 符號連同原樣式一起填回，字型才不會掉
                parts.add(makePart(glyphs.get(i), glyphStyles.get(i)));
            }
        }
        return parts.isEmpty() ? null : StyledText.fromParts(parts);
    }

    // ------------------------------------------------------------ 內部

    /** 以 {@code {#}} 切開譯文，保留空字串以維持縫隙數正確。 */
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
                ? new StyledTextPart(text, net.minecraft.network.chat.Style.EMPTY, null, null)
                : new StyledTextPart(text, style.getStyle(), null, null);
    }

    /** 譯文文字要沿用的樣式：原文第一個非符號片段的樣式。 */
    private static PartStyle firstTextStyle(StyledText original) {
        for (StyledTextPart part : original) {
            if (!GlyphSplitter.isGlyphPart(part)
                    && !part.getString(null, StyleType.NONE).isBlank()) {
                return part.getPartStyle();
            }
        }
        return null;
    }

    /** 依序收集每一段連續符號的樣式，與 extractGlyphs 的順序一一對應。 */
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
