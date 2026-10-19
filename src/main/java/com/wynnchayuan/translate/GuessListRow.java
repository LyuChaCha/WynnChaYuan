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

/**
 * Wynntils「物品猜測」底下一列一列的候選物品名。
 *
 * <h2>遊戲送來的樣子</h2>
 * 未鑑定物品的說明最後，Wynntils 4.2.8 的 {@code ItemGuessFeature#getTooltipAddon}
 * 加上這一塊（javap 抄來的組法）：
 *
 * <pre>
 *   - Possibilities:                       「- 」綠、標題灰（Wynntils 自己的語言檔）
 *       - Lv. 88 [408 ²]:                   四個空白、等級列
 *           Charm of the Magma,             八個空白 + 名字（稀有度的顏色，收藏的加底線）+ 「,」
 *           Charm of the Storms,
 *           Charm of the Tides              最後一個名字後面沒有逗號
 * </pre>
 *
 * 寬度夠的時候一列可以放好幾個，用「, 」隔開。第一個名字一律另起一列，所以等級列
 * 上不會有名字（舊版把名字接在等級列後面，那一種見 {@code TooltipPanel#guessList}）。
 *
 * <h2>為什麼要另外處理（使用者 2026-10-17 的截圖）</h2>
 * <ul>
 *   <li><b>不附原文</b>：說明第三行以後一律掛著
 *       {@link TranslationStore#holdAppendedOriginal}（公會徽章那種敘述裡的同名字不附），
 *       而這一塊整塊都是物品名稱，玩家正是要拿原文去對市場與 wiki。</li>
 *   <li><b>最後一列縮排不見</b>：帶逗號的列整行查不到，逐片段那條路把行首空白留著；
 *       最後一列剛好整行就是一個名字，整行查表命中，模板剝掉的行首空白重建時沒有補回來，
 *       名字從左緣畫起。</li>
 *   <li>帶逗號的列也不經過裝備名的守門（{@code TooltipPanel#blockedGearName} 只認整行
 *       一個名字），還沒翻、又跟技能撞名的裝備會拿到技能的譯名。</li>
 * </ul>
 *
 * <h2>做法</h2>
 * 形狀認得很窄：行首至少四個空白，後面只有「名字」「, 」「名字」……可能再接一個「,」，
 * 而且<b>每一格</b>都是語料裡的物品名稱（{@link TranslationStore#isItemName}）。
 * 認得出來就逐格換字：縮排、逗號、每一格的樣式原樣留著，名字照名稱列的規矩查——
 * 附原文（F6「譯名 + 原文」）、裝備自己的名字（{@link TranslationStore#gearOwnName}）、
 * 還沒翻的裝備名留原文。
 */
public final class GuessListRow {

    private GuessListRow() {}

    /** 行首至少幾個空白才算。Wynntils 用的是八個；說明裡的置中與縮排靠的是偏移字元。 */
    private static final int MIN_INDENT = 4;

    /**
     * 認得出來的一列。
     *
     * @param translated 換好的那一列；沒有一格換得了（還沒翻、F6 關閉）時是 {@code null}
     */
    public record Row(Component translated) {}

    /**
     * @return 不是這種清單列時回傳 {@code null}
     */
    public static Row of(StyledText line, TranslationStore store) {
        if (line == null || store == null
                || !line.getStringWithoutFormatting().startsWith(" ".repeat(MIN_INDENT))) {
            return null;                       // 每一列都會問到，先用最便宜的條件擋掉
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
        // 每一格照它第一個字的樣式畫（Wynntils 一格是一個 literal）。
        // UTF-16 索引對回字元，代用對的字也算一格。
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

    /**
     * 一格名字的譯名；留原文時回傳 {@code null}。
     *
     * <p>還沒翻的裝備名一律留原文：查得到的必然是別的領域同名的那一條。
     */
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

    /** {@code text} 的第 i 個 UTF-16 索引落在第幾個字元（{@code texts} 的索引）。 */
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

    /** 名字裡沒有圖示、排版偏移（私用區與第 13 平面以後）。 */
    private static boolean plainChars(String s) {
        return s.codePoints().noneMatch(cp -> (cp >= 0xE000 && cp <= 0xF8FF) || cp >= 0xD0000
                || SpaceOffset.isOffset(cp));
    }
}
