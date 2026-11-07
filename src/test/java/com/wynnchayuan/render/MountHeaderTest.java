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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToIntFunction;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class MountHeaderTest {

    private static final String[] LANGS = {"zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr"};

    private static final Map<String, Integer> SERVER_PX = Map.of(
            "Speed", 25, "Acceleration", 54, "Toughness", 46, "Training", 35,
            "Level/Limit ", 51, "(Max)", 20);

    private static final int METER = 0xE023;

    private static final int METER_END = 0xF016;

    private static final Style WYNN = font("language/wynncraft");

    private static final Style SPACE = font("space");

    private static final Style GAUGE = font("tooltip/identification/meter");

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Field measure = Class.forName("com.wynnchayuan.translate.LineTranslator").getDeclaredField("measureForTest");
        measure.setAccessible(true);
        ToIntFunction<Component> widths = MountHeaderTest::width;
        measure.set(null, widths);
        try {
            for (int column : new int[] {75, 69}) {
                List<Component> tip = new ArrayList<>();
                tip.add(header(column - 23));
                for (String stat : new String[] {"Speed", "Acceleration", "Toughness", "Training"}) {
                    tip.add(row(stat, column));
                }
                check("原文：「(Max)」的右緣就是「(30)」的右緣（欄 " + column + "）",
                        inkEnd(tip.get(0)) == inkEnd(tip.get(1)));
                for (String lang : LANGS) {
                    TranslationStore store = new TranslationStore();
                    store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations", lang));
                    List<Component> out = TooltipTranslator.translateLines(tip, store);
                    check(lang + " 欄 " + column + " 有翻", out.size() == tip.size());
                    if (out.size() != tip.size()) {
                        continue;
                    }
                    String head = out.get(0).getString();
                    int headEnd = inkEnd(out.get(0));
                    int valueEnd = inkEnd(out.get(1));
                    check(lang + " 欄 " + column + " 標題翻了（" + head + "）", !head.contains("Level"));
                    boolean clamped = inkStart(out.get(0)) == 0 && headEnd > valueEnd;
                    check(lang + " 欄 " + column + " 標題的右緣對齊數值欄，太寬就貼齊左邊（" + headEnd + " vs " + valueEnd
                                    + (clamped ? "，貼左" : "") + "）",
                            headEnd == valueEnd || clamped);
                    check(lang + " 欄 " + column + " 標題沒有超出左邊（" + inkStart(out.get(0)) + "）",
                            inkStart(out.get(0)) >= 0);
                }
            }
        } finally {
            measure.set(null, null);
        }
        System.out.println(failures == 0 ? "MountHeader: 全部通過" : "MountHeader: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static Component header(int lead) {
        MutableComponent line = Component.empty();
        line.append(Component.literal(SpaceOffset.encode(lead)).withStyle(SPACE));
        line.append(Component.literal("Level/Limit ").withStyle(WYNN.withColor(0xAAAAAA)));
        line.append(Component.literal("(Max)").withStyle(WYNN.withColor(0x555555)));
        return line;
    }

    private static Component row(String stat, int column) {
        MutableComponent line = Component.empty();
        line.append(Component.literal(stat).withStyle(WYNN.withColor(0xFFFFFF)));
        line.append(Component.literal(SpaceOffset.encode(-SERVER_PX.get(stat))).withStyle(SPACE));
        line.append(Component.literal(SpaceOffset.encode(column)).withStyle(SPACE));
        line.append(Component.literal("1").withStyle(WYNN.withColor(TextColor.fromRgb(0xACFAC6))));
        line.append(Component.literal("/10 ").withStyle(WYNN.withColor(0xAAAAAA)));
        line.append(Component.literal("(30)").withStyle(WYNN.withColor(0x555555)));
        line.append(Component.literal(" ").withStyle(WYNN.withColor(0xAAAAAA)));
        line.append(Component.literal(new String(Character.toChars(METER)) + SpaceOffset.encode(-9))
                .withStyle(GAUGE.withColor(0x555555)));
        line.append(Component.literal(SpaceOffset.encode(9)).withStyle(GAUGE));
        line.append(Component.literal(SpaceOffset.encode(-9)).withStyle(WYNN));
        line.append(Component.literal(new String(Character.toChars(METER_END))).withStyle(GAUGE.withColor(0x55FF55)));
        return line;
    }

    private static Style font(String path) {
        return Style.EMPTY.withFont(new FontDescription.Resource(Identifier.withDefaultNamespace(path)));
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

    private static boolean inked(String s) {
        return s.codePoints().anyMatch(cp -> !Character.isWhitespace(cp) && !SpaceOffset.isOffset(cp)
                && Character.getType(cp) != Character.PRIVATE_USE);
    }

    private static int text(String s) {
        Integer known = SERVER_PX.get(s);
        if (known != null) {
            return known;
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
            return 9;
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
