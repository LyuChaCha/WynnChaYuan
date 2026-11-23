package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class ClassTypeRowTest {

    private static final int ICON = 8;
    private static final int ENGLISH_GAP = 4;
    private static final int RIGHT_EDGE = ICON + 59 + ENGLISH_GAP + 89;

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        LineTranslator.measureForTest = ClassTypeRowTest::width;
        try {
            StyledText classType = requirementRow(" Class Type", "Mage/Dark Wizard");
            StyledText combatLevel = requirementRow(" Combat Level", "22");
            List<StyledText> rows = List.of(classType, combatLevel);

            check("原文的數值與標籤之間有 " + ENGLISH_GAP + "px，右緣在 " + RIGHT_EDGE,
                    gap(classType.getComponent()) == ENGLISH_GAP
                            && width(classType.getComponent()) == RIGHT_EDGE
                            && width(combatLevel.getComponent()) == RIGHT_EDGE);

            for (String lang : List.of("pt_br", "es_es", "ru_ru")) {
                brokenHarp(lang, rows);
            }
            brokenHarp("zh_tw", rows);
        } finally {
            LineTranslator.measureForTest = null;
        }

        System.out.println(failures == 0
                ? "ClassTypeRow: 全部通過"
                : "ClassTypeRow: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void brokenHarp(String lang, List<StyledText> rows) {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations", lang));
        boolean left = LineTranslator.columnsAreLeftAligned(rows);
        List<Component> out = new ArrayList<>();
        for (StyledText row : rows) {
            Component made = LineTranslator.translate(row, store, false, left);
            check(lang + " 需求列翻得到：" + row.getStringWithoutFormatting().strip(), made != null);
            out.add(made == null ? row.getComponent() : made);
        }
        int[] rowOf = {0, 1};
        boolean[] hit = {true, true};
        ColumnRightEdge.apply(rows, out, rowOf, hit, new boolean[2], ClassTypeRowTest::width);

        Component classType = out.get(0);
        String plain = classType.getString().replace(" ", "␠");
        int gap = gap(classType);
        check(lang + " 數值不蓋到標籤（間隔 " + gap + "px）：" + plain, gap >= ENGLISH_GAP);

        int edge = width(classType);
        int fits = labelEnd(classType) + ENGLISH_GAP + valueWidth(classType);
        check(lang + " 放得下就照原文右緣，放不下才撐寬（右緣 " + edge + "）",
                edge == Math.max(RIGHT_EDGE, fits));
        check(lang + " Combat Level 那列跟著對到同一個右緣（" + width(out.get(1)) + "）",
                width(out.get(1)) == edge);
    }

    private static StyledText requirementRow(String label, String value) {
        Style sprite = Style.EMPTY.withFont(new FontDescription.Resource(
                Identifier.withDefaultNamespace("tooltip/requirement/sprite")));
        Style wynn = Style.EMPTY.withFont(new FontDescription.Resource(
                Identifier.withDefaultNamespace("language/wynncraft")));
        int labelWidth = textWidth(label);
        int column = RIGHT_EDGE - ICON - textWidth(value);
        MutableComponent row = Component.empty()
                .append(Component.literal(new String(Character.toChars(0xE006))
                        + SpaceOffset.encode(-1)).withStyle(sprite))
                .append(Component.literal(label + SpaceOffset.encode(-labelWidth)
                        + SpaceOffset.encode(column)).withStyle(wynn))
                .append(Component.literal(value).withStyle(wynn));
        return StyledText.fromComponent(row);
    }

    private static int gap(Component row) {
        int[] spans = spans(row);
        return spans[1] - spans[0];
    }

    private static int labelEnd(Component row) {
        return spans(row)[0];
    }

    private static int valueWidth(Component row) {
        return width(row) - spans(row)[1];
    }

    private static int[] spans(Component row) {
        int[] state = {0, -1, -1, 0};
        row.visit((style, text) -> {
            int i = 0;
            while (i < text.length()) {
                int cp = text.codePointAt(i);
                i += Character.charCount(cp);
                if (SpaceOffset.isOffset(cp)) {
                    if (state[3] == 1 && state[1] < 0) {
                        state[1] = state[0];
                    }
                    state[0] += SpaceOffset.decode(new String(Character.toChars(cp)));
                    continue;
                }
                if (state[1] >= 0 && state[2] < 0) {
                    state[2] = state[0];
                }
                if (Character.isLetter(cp)) {
                    state[3] = 1;
                }
                state[0] += advance(cp);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return new int[] {state[1], state[2]};
    }

    private static int width(Component component) {
        int[] w = {0};
        component.visit((style, text) -> {
            int i = 0;
            while (i < text.length()) {
                int cp = text.codePointAt(i);
                i += Character.charCount(cp);
                w[0] += SpaceOffset.isOffset(cp)
                        ? SpaceOffset.decode(new String(Character.toChars(cp))) : advance(cp);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return w[0];
    }

    private static int textWidth(String text) {
        return text.codePoints().map(ClassTypeRowTest::advance).sum();
    }

    private static final String NARROW_2 = "i.,:;!|'";
    private static final String NARROW_3 = "l`";
    private static final String NARROW_4 = " tI[]\"*";
    private static final String NARROW_5 = "fk<>(){}";

    private static int advance(int cp) {
        if (cp >= 0xE000 && cp <= 0xF8FF) {
            return 9;
        }
        if (cp >= 0x2E80) {
            return 9;
        }
        int base = stripAccent(cp);
        return Arrays.asList(NARROW_2, NARROW_3, NARROW_4, NARROW_5).stream()
                .filter(set -> set.indexOf(base) >= 0)
                .findFirst()
                .map(set -> set == NARROW_2 ? 2 : set == NARROW_3 ? 3 : set == NARROW_4 ? 4 : 5)
                .orElse(6);
    }

    private static int stripAccent(int cp) {
        String plain = java.text.Normalizer.normalize(new String(Character.toChars(cp)),
                java.text.Normalizer.Form.NFD);
        return plain.isEmpty() ? cp : plain.codePointAt(0);
    }
}
