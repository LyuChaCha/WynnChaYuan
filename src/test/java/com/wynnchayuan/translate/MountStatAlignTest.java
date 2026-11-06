package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class MountStatAlignTest {

    private static final String[] STATS = {
        "Speed", "Acceleration", "Altitude", "Jump Height", "Energy", "Handling", "Toughness", "Boost", "Training"
    };

    private static final int[] STAT_PX = {25, 54, 33, 58, 30, 35, 46, 24, 35};

    private static final String[] LANGS = {"zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr"};

    private static final int[] COLUMNS = {75, 69};

    private static final int METER = 0xE023;

    private static final int METER_END = 0xF016;

    private static final int METER_PX = 9;

    private static final Style WYNN = font("language/wynncraft");

    private static final Style SPACE = font("space");

    private static final Style GAUGE = font("tooltip/identification/meter");

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        LineTranslator.measureForTest = MountStatAlignTest::width;
        try {
            List<StyledText> rows = new ArrayList<>();
            List<String> names = new ArrayList<>();
            List<Integer> rights = new ArrayList<>();
            for (int column : COLUMNS) {
                for (int k = 0; k < STATS.length; k++) {
                    rows.add(row(STATS[k], STAT_PX[k], column, "1", "10", "30"));
                    names.add(STATS[k]);
                    rights.add(column + text("1/10 (30) ") + METER_PX);
                }
            }
            for (int k = 0; k < rows.size(); k++) {
                check("原文每一列都排到同一個右緣（" + visible(rows.get(k).getString()) + "）",
                        width(rows.get(k).getComponent()) == rights.get(k));
            }
            for (String lang : LANGS) {
                TranslationStore store = new TranslationStore();
                store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations", lang));
                store.setAbilityNamesWithOriginal(true);
                for (boolean left : new boolean[] {false, true}) {
                    for (int k = 0; k < rows.size(); k++) {
                        String stat = names.get(k);
                        int right = rights.get(k);
                        Component out = LineTranslator.translate(rows.get(k), store, false, left);
                        String text = out == null ? "" : out.getString();
                        check(lang + (left ? " 靠左" : " 靠右") + " " + stat + " 數值欄對回同一個右緣："
                                        + visible(text) + "  " + (out == null ? -1 : width(out)) + " 應為 " + right,
                                out != null && width(out) == right);
                        int gap = out == null ? -1 : labelGap(out);
                        check(lang + " " + stat + " 標籤跟數值沒疊在一起（間距 " + gap + " px）：" + visible(text),
                                gap >= MIN_GAP_PX);
                        check(lang + " " + stat + " 標籤翻了、沒附原文：" + visible(text),
                                !text.isEmpty() && !text.startsWith(stat) && !text.contains("(" + stat + ")"));
                    }
                }
            }
        } finally {
            LineTranslator.measureForTest = null;
        }
        System.out.println(failures == 0 ? "MountStatAlign: 全部通過" : "MountStatAlign: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static final int MIN_GAP_PX = 4;

    private static int labelGap(Component component) {
        int[] x = {0};
        int[] labelEnd = {-1};
        int[] valueStart = {-1};
        component.visit((style, s) -> {
            int i = 0;
            while (i < s.length()) {
                int cp = s.codePointAt(i);
                i += Character.charCount(cp);
                if (valueStart[0] < 0 && !SpaceOffset.isOffset(cp)) {
                    if (Character.isDigit(cp) && labelEnd[0] >= 0) {
                        valueStart[0] = x[0];
                    } else if (Character.isLetter(cp)) {
                        labelEnd[0] = x[0] + widthOf(cp);
                    }
                }
                x[0] += widthOf(cp);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return labelEnd[0] < 0 || valueStart[0] < 0 ? -1 : valueStart[0] - labelEnd[0];
    }

    private static StyledText row(String stat, int label, int column, String current, String cap, String max) {
        MutableComponent line = Component.empty();
        line.append(Component.literal(stat).withStyle(WYNN.withColor(0xFFFFFF)));
        line.append(Component.literal(SpaceOffset.encode(-label)).withStyle(SPACE));
        line.append(Component.literal(SpaceOffset.encode(column)).withStyle(SPACE));
        line.append(Component.literal(current).withStyle(WYNN.withColor(TextColor.fromRgb(0xACFAC6))));
        line.append(Component.literal("/" + cap + " ").withStyle(WYNN.withColor(0xAAAAAA)));
        if (max != null) {
            line.append(Component.literal("(" + max + ")").withStyle(WYNN.withColor(0x555555)));
        }
        line.append(Component.literal(" ").withStyle(WYNN.withColor(0xAAAAAA)));
        line.append(Component.literal(new String(Character.toChars(METER)) + SpaceOffset.encode(-METER_PX))
                .withStyle(GAUGE.withColor(0x555555)));
        line.append(Component.literal(SpaceOffset.encode(METER_PX)).withStyle(GAUGE));
        line.append(Component.literal(SpaceOffset.encode(-METER_PX)).withStyle(WYNN));
        line.append(Component.literal(new String(Character.toChars(METER_END))).withStyle(GAUGE.withColor(0x55FF55)));
        return StyledText.fromComponent(line);
    }

    private static Style font(String path) {
        return Style.EMPTY.withFont(new FontDescription.Resource(Identifier.withDefaultNamespace(path)));
    }

    private static String visible(String s) {
        StringBuilder out = new StringBuilder();
        s.codePoints().forEach(cp -> {
            if (SpaceOffset.isOffset(cp)) {
                out.append('[').append(SpaceOffset.decode(new String(Character.toChars(cp)))).append(']');
            } else if (cp == METER || cp == METER_END) {
                out.append('●');
            } else {
                out.appendCodePoint(cp);
            }
        });
        return out.toString();
    }

    private static int text(String s) {
        for (int k = 0; k < STATS.length; k++) {
            if (STATS[k].equals(s)) {
                return STAT_PX[k];
            }
        }
        int w = 0;
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            w += widthOf(cp);
        }
        return w;
    }

    private static int widthOf(int cp) {
        if (SpaceOffset.isOffset(cp)) {
            return SpaceOffset.decode(new String(Character.toChars(cp)));
        }
        if (cp == METER || cp == METER_END) {
            return METER_PX;
        }
        if (cp >= 0x2E80) {
            return 9;
        }
        return switch (cp) {
            case ' ', 't', 'I', '(', ')' -> 4;
            case 'i', 'ı', '.', ',', '!', ':' -> 2;
            case 'l' -> 3;
            case 'f', 'k', '<', '>' -> 5;
            default -> 6;
        };
    }

    private static int width(Component component) {
        int[] w = {0};
        component.visit((style, s) -> {
            w[0] += text(s);
            return Optional.empty();
        }, Style.EMPTY);
        return w[0];
    }
}
