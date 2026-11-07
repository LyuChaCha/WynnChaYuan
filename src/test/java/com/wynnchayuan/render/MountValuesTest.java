package com.wynnchayuan.render;

import com.wynnchayuan.translate.SpaceOffset;
import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.ToIntFunction;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class MountValuesTest {

    private static final String[] LANGS = {"zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr"};

    private static final String[][] ROWS = {
        {"Speed", "1", "67", "76"},
        {"Acceleration", "4", "76", "111"},
        {"Jump Height", "1", "65", "79"},
        {"Energy", "4", "79", "109"},
        {"Handling", "9", "70", "121"},
        {"Toughness", "11", "71", "83"},
        {"Boost", "1", "72", "103"},
        {"Training", "1", "71", "104"},
    };

    private static final Map<String, Integer> LABEL_PX = Map.of(
            "Speed", 25, "Acceleration", 54, "Jump Height", 52, "Energy", 30, "Handling", 35,
            "Toughness", 46, "Boost", 24, "Training", 35, "Level/Limit ", 51, "(Max)", 20);

    private static final int RIGHT = 140;

    private static final int METER = 0xE023;

    private static final int METER_END = 0xF016;

    private static final int METER_PX = 9;

    private static final int MIN_GAP_PX = 4;

    private static final Style WYNN = font("language/wynncraft");

    private static final Style SPACE = font("space");

    private static final Style GAUGE = font("tooltip/identification/meter");

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Field measure = Class.forName("com.wynnchayuan.translate.LineTranslator").getDeclaredField("measureForTest");
        measure.setAccessible(true);
        ToIntFunction<Component> widths = MountValuesTest::width;
        measure.set(null, widths);
        try {
            List<Component> tip = new ArrayList<>();
            tip.add(header());
            for (String[] r : ROWS) {
                tip.add(row(r[0], r[1], r[2], r[3]));
            }
            Set<Integer> before = new HashSet<>();
            for (int k = 1; k < tip.size(); k++) {
                before.add(width(tip.get(k)));
            }
            check("前提：伺服器的字寬表把 1 算窄，原文的右緣本來就參差（" + before + "）", before.size() > 1);
            for (String lang : LANGS) {
                TranslationStore store = new TranslationStore();
                store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations", lang));
                List<Component> out = TooltipTranslator.translateLines(tip, store);
                check(lang + " 有翻", out.size() == tip.size());
                if (out.size() != tip.size()) {
                    continue;
                }
                Set<Integer> ends = new HashSet<>();
                Set<Integer> inks = new HashSet<>();
                for (int k = 1; k < out.size(); k++) {
                    Component c = out.get(k);
                    ends.add(width(c));
                    inks.add(inkEnd(c));
                    int gap = labelGap(c);
                    check(lang + " " + ROWS[k - 1][0] + " 標籤與數值沒疊在一起（" + gap + " px）",
                            gap >= MIN_GAP_PX);
                }
                check(lang + " 每列的量表圖示對齊同一個右緣（" + ends + "）", ends.size() == 1);
                check(lang + " 每列的數值右緣對齊（" + inks + "）", inks.size() == 1);
                check(lang + " 標題右緣跟著數值欄（" + inkEnd(out.get(0)) + " vs " + inkEnd(out.get(1)) + "）",
                        inkEnd(out.get(0)) == inkEnd(out.get(1)) || inkStart(out.get(0)) == 0);
            }
        } finally {
            measure.set(null, null);
        }
        System.out.println(failures == 0 ? "MountValues: 全部通過" : "MountValues: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static Component header() {
        MutableComponent line = Component.empty();
        int column = RIGHT - server("1/67 (76) ") - METER_PX;
        line.append(Component.literal(SpaceOffset.encode(column - 23)).withStyle(SPACE));
        line.append(Component.literal("Level/Limit ").withStyle(WYNN.withColor(0xAAAAAA)));
        line.append(Component.literal("(Max)").withStyle(WYNN.withColor(0x555555)));
        return line;
    }

    private static Component row(String stat, String current, String cap, String max) {
        String values = current + "/" + cap + " (" + max + ") ";
        MutableComponent line = Component.empty();
        line.append(Component.literal(stat).withStyle(WYNN.withColor(0xFFFFFF)));
        line.append(Component.literal(SpaceOffset.encode(-LABEL_PX.get(stat))).withStyle(SPACE));
        line.append(Component.literal(SpaceOffset.encode(RIGHT - server(values) - METER_PX)).withStyle(SPACE));
        line.append(Component.literal(current).withStyle(WYNN.withColor(TextColor.fromRgb(0xACFAC6))));
        line.append(Component.literal("/" + cap + " ").withStyle(WYNN.withColor(0xAAAAAA)));
        line.append(Component.literal("(" + max + ")").withStyle(WYNN.withColor(0x555555)));
        line.append(Component.literal(" ").withStyle(WYNN.withColor(0xAAAAAA)));
        line.append(Component.literal(new String(Character.toChars(METER)) + SpaceOffset.encode(-METER_PX))
                .withStyle(GAUGE.withColor(0x555555)));
        line.append(Component.literal(SpaceOffset.encode(METER_PX)).withStyle(GAUGE));
        line.append(Component.literal(SpaceOffset.encode(-METER_PX)).withStyle(WYNN));
        line.append(Component.literal(new String(Character.toChars(METER_END))).withStyle(GAUGE.withColor(0x55FF55)));
        return line;
    }

    private static int server(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            w += s.charAt(i) == '1' ? 4 : widthOf(s.charAt(i));
        }
        return w;
    }

    private static Style font(String path) {
        return Style.EMPTY.withFont(new FontDescription.Resource(Identifier.withDefaultNamespace(path)));
    }

    private static boolean inked(String s) {
        return s.codePoints().anyMatch(cp -> !Character.isWhitespace(cp) && !SpaceOffset.isOffset(cp)
                && Character.getType(cp) != Character.PRIVATE_USE);
    }

    private static int inkEnd(Component line) {
        int[] x = {0};
        int[] end = {-1};
        line.visit((style, s) -> {
            x[0] += text(s);
            if (inked(s)) {
                end[0] = x[0];
            }
            return Optional.empty();
        }, Style.EMPTY);
        return end[0];
    }

    private static int inkStart(Component line) {
        int[] x = {0};
        int[] start = {Integer.MIN_VALUE};
        line.visit((style, s) -> {
            if (start[0] == Integer.MIN_VALUE && inked(s)) {
                start[0] = x[0];
            }
            x[0] += text(s);
            return Optional.empty();
        }, Style.EMPTY);
        return start[0];
    }

    private static int labelGap(Component component) {
        int[] x = {0};
        int[] labelEnd = {-1};
        int[] valueStart = {-1};
        component.visit((style, s) -> {
            int i = 0;
            while (i < s.length()) {
                int cp = s.codePointAt(i);
                i += Character.charCount(cp);
                int w = SpaceOffset.isOffset(cp) ? SpaceOffset.decode(new String(Character.toChars(cp))) : widthOf(cp);
                if (valueStart[0] < 0 && !SpaceOffset.isOffset(cp)) {
                    if (Character.isDigit(cp) && labelEnd[0] >= 0) {
                        valueStart[0] = x[0];
                    } else if (Character.isLetter(cp)) {
                        labelEnd[0] = x[0] + w;
                    }
                }
                x[0] += w;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return labelEnd[0] < 0 || valueStart[0] < 0 ? -1 : valueStart[0] - labelEnd[0];
    }

    private static int text(String s) {
        Integer known = LABEL_PX.get(s);
        if (known != null) {
            return known;
        }
        int w = 0;
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            w += SpaceOffset.isOffset(cp) ? SpaceOffset.decode(new String(Character.toChars(cp))) : widthOf(cp);
        }
        return w;
    }

    private static int widthOf(int cp) {
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
