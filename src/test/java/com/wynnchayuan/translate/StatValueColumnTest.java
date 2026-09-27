package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 裝備 tooltip 的詞條列：數值欄要落回原文的位置。
 *
 * <h2>實機回報</h2>
 * 摩羯座之靴的四條詞條，只有「魔力回復」那一行的數值往左縮了 22px：
 *
 * <pre>
 *   魔力回復   +15/5s [91.6%]      ← 歪掉
 *   魔力竊取      +5/3s [100.0%]
 *   移動速度        +19% [77.7%]
 *   法術傷害百分比  +17% [80.0%]
 * </pre>
 *
 * <p>這一份 tooltip 每一行的右緣都對齊，所以英文標籤越長、欄距就越窄。
 * {@code Mana Regen} 是四個裡面最長的，欄距只剩 4px——低於
 * {@link LineTranslator#MIN_GAP_PX}，要靠 {@code tooltipGaps} 的放寬才數得到，
 * 而那道放寬原本只認「後面是字母」。數值欄一律是 {@code +15/5s} 這種以正負號
 * 開頭的字串，於是整個欄界被漏掉，中文標籤短掉的 22px 沒人補。
 */
public final class StatValueColumnTest {

    private static int failures = 0;

    private static final Style WYNN = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF))
            .withFont(new net.minecraft.network.chat.FontDescription.Resource(
                    net.minecraft.resources.Identifier.withDefaultNamespace("language/wynncraft")));
    private static final Style VALUE = WYNN.withColor(TextColor.fromRgb(0xACFAC6));
    private static final Style PCT = WYNN.withColor(TextColor.fromRgb(0x55FF71));

    /** 實機量到的整行寬度：這一份 tooltip 每一行都排到這裡。 */
    private static final int LINE_WIDTH = 140;

    /** 另一種排法：數值的右緣對齊，欄距寬鬆。 */
    private static final int COLUMN = 130;

    private static final String[][] ROWS = {
        {"Mana Regen ",   "+15/5s", " [91.6%]"},
        {"Mana Steal ",   "+5/3s",  " [100.0%]"},
        {"Walk Speed ",   "+19%",   " [77.7%]"},
        {"Spell Damage ", "+17%",   " [80.0%]"},
    };

    public static void main(String[] args) {
        Path root = Path.of(args.length > 0 ? args[0]
                                            : "src/main/resources/assets/wynnchayuan/translations");
        LineTranslator.measureForTest = StatValueColumnTest::measure;
        try {
            for (String lang : new String[] {"zh_tw", "zh_cn"}) {
                TranslationStore store = new TranslationStore();
                store.loadAll(List.of(root.resolve(lang)));
                store.setNameMode(CollectorConfig.ItemNames.OFF);
                System.out.println("== " + lang);
                run(lang + "／整行等寬", store, true);
                run(lang + "／數值靠右", store, false);
            }
        } finally {
            LineTranslator.measureForTest = null;
        }
        System.out.println(failures == 0 ? "詞條數值欄：全部通過"
                                         : "詞條數值欄：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void run(String what, TranslationStore store, boolean sameWidth) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Capricorn"));
        lines.add(Component.literal("Capricorn"));
        for (String[] r : ROWS) {
            lines.add(sameWidth ? endRow(r[0], r[1], r[2]) : valueColumnRow(r[0], r[1], r[2]));
        }
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(lines, store);
        for (int i = 2; i < lines.size(); i++) {
            String[] r = ROWS[i - 2];
            int want = valueX(lines.get(i));
            int got = valueX(out.get(i));
            System.out.println("  " + out.get(i).getString()
                               + "　數值起點 原文=" + want + " 譯文=" + got);
            check(what + "「" + r[0].strip() + "」的數值落在原文的位置"
                  + "（原文 " + want + "、譯文 " + got + "）", want == got);
        }
    }

    /** 整行排到同一個右緣：標籤越長，欄距越窄。{@code Mana Regen} 只剩 4px。 */
    private static Component endRow(String label, String value, String pct) {
        return row(label, value, pct,
                   LINE_WIDTH - measure(Component.literal(label + value + pct)));
    }

    /** 數值的右緣對齊：欄距寬鬆，本來就數得到。 */
    private static Component valueColumnRow(String label, String value, String pct) {
        return row(label, value, pct,
                   COLUMN - measure(Component.literal(label + value)));
    }

    private static Component row(String label, String value, String pct, int gap) {
        MutableComponent c = Component.empty();
        c.append(Component.literal(label).withStyle(WYNN));
        c.append(Component.literal(SpaceOffset.encode(gap))
                 .withStyle(SpaceOffset.styleFor(WYNN)));
        c.append(Component.literal(value).withStyle(VALUE));
        c.append(Component.literal(pct).withStyle(PCT));
        return c;
    }

    /** 第一個正負號畫在第幾個像素。 */
    private static int valueX(Component c) {
        int[] x = {0};
        int[] found = {Integer.MIN_VALUE};
        c.visit((style, text) -> {
            text.codePoints().forEach(cp -> {
                if (found[0] != Integer.MIN_VALUE) {
                    return;
                }
                if (cp == '+' || cp == '-') {
                    found[0] = x[0];
                    return;
                }
                x[0] += advance(cp);
            });
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static int advance(int cp) {
        if (cp >= 0xCF000 && cp <= 0xD0400) {
            return cp - 0xD0000;               // 排版偏移
        }
        if (cp >= 0x2E80) {
            return 9;
        }
        return switch (cp) {
            case 'i', '.', ':', ',', ';', '!' -> 2;
            case 'l' -> 3;
            case '[', ']', '(', ')' -> 4;
            case 't', 'I', ' ' -> 4;
            case 'f', 'k' -> 5;
            default -> 6;
        };
    }

    static int measure(Component component) {
        int[] w = {0};
        component.visit((style, text) -> {
            text.codePoints().forEach(cp -> w[0] += advance(cp));
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return w[0];
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
