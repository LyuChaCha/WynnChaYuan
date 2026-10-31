package com.wynnchayuan.translate;

import com.wynnchayuan.ModConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class WeightColumnTest {

    private static final Style WYNN = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF))
            .withFont(new net.minecraft.network.chat.FontDescription.Resource(
                    net.minecraft.resources.Identifier.withDefaultNamespace("language/wynncraft")));
    private static final Style PCT = WYNN.withColor(TextColor.fromRgb(0xFFCB21));

    private static final int WIDEST = 150;

    private static final String[][] ROWS = {
        {"Main Scale",             "57.0%"},
        {"Spellsteal Scale",       "48.2%"},
        {"Water spellsteal Scale", "47.4%"},
        {"Obli Scale",             "46.4%"},
    };

    public static void main(String[] args) {
        Path root = Path.of(args.length > 0 ? args[0]
                                            : "src/main/resources/assets/wynnchayuan/translations");
        LineTranslator.measureForTest = WeightColumnTest::measure;
        try {
            for (String lang : new String[] {"zh_tw", "zh_cn"}) {
                TranslationStore store = new TranslationStore();
                store.loadAll(List.of(root.resolve(lang)));
                store.setNameMode(ModConfig.ItemNames.OFF);
                System.out.println("== " + lang);
                run(lang + "／[xx%] 一段", store, "", false);
                run(lang + "／ [xx%] 一段", store, " ", false);
                run(lang + "／[ xx% ] 分三段", store, "", true);
            }
            guards();
        } finally {
            LineTranslator.measureForTest = null;
        }
        System.out.println(failures == 0 ? "權重百分比欄：全部通過"
                                         : "權重百分比欄：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void run(String what, TranslationStore store, String lead, boolean split) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Stardew"));
        lines.add(Component.literal("Stardew"));
        int tightest = Integer.MAX_VALUE;
        for (String[] r : ROWS) {
            int gap = gapFor(r[0], "[" + r[1] + "]");
            tightest = Math.min(tightest, gap);
            lines.add(row(r[0], gap, lead, r[1], split));
        }
        check(what + "：測資裡最窄的欄距低於門檻（" + tightest + "px）",
              tightest > 0 && tightest < SpaceRuns.MIN_GAP_PX);
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(lines, store);
        for (int i = 2; i < lines.size(); i++) {
            String[] r = ROWS[i - 2];
            if (out.get(i).getString().equals(lines.get(i).getString())) {
                check(what + "「" + r[0] + "」有翻到", false);
                continue;
            }
            int want = valueX(lines.get(i));
            int got = valueX(out.get(i));
            System.out.println("  " + out.get(i).getString()
                               + "　數值起點 原文=" + want + " 譯文=" + got);
            check(what + "「" + r[0] + "」的百分比落在原文的位置"
                  + "（原文 " + want + "、譯文 " + got + "）", want == got);
        }
    }

    private static void guards() {
        System.out.println("== 不該認成欄界的");
        check("「標籤 <+3>[47.4%]」認得欄界", gaps(run("Water spellsteal Scale"), space(3),
                                                  run("[47.4%]")) == 1);
        check("「標籤 <+3> [47.4%]」認得欄界", gaps(run("Water spellsteal Scale"), space(3),
                                                   run(" [47.4%]")) == 1);
        check("「標籤 <+3>[」「47.4%」「]」跨片段也認得", gaps(run("Water spellsteal Scale"),
                space(3), run("["), run("47.4%"), run("]")) == 1);
        check("「字 <+3>[Cave]」不是欄界", gaps(run("Ragni"), space(3), run("[Cave]")) == 0);
        check("「字 <+3>[」後面沒東西不是欄界", gaps(run("Ragni"), space(3), run("[")) == 0);
        check("「- +1 <+2>[5%]」前面沒有字母不是欄界", gaps(run("- +1 "), space(2),
                                                       run("[5%]")) == 0);
    }

    private static int gaps(SpaceRuns.Run... runs) {
        int n = 0;
        for (boolean b : SpaceRuns.tooltipGaps(List.of(runs))) {
            if (b) {
                n++;
            }
        }
        return n;
    }

    private static SpaceRuns.Run run(String text) {
        return new SpaceRuns.Run(false, 0, WYNN, text);
    }

    private static SpaceRuns.Run space(int px) {
        return new SpaceRuns.Run(true, px, SpaceOffset.styleFor(WYNN), SpaceOffset.encode(px));
    }

    private static int gapFor(String label, String value) {
        return Math.max(0, WIDEST - measure(Component.literal(label + value)));
    }

    private static Component row(String label, int gap, String lead, String pct, boolean split) {
        MutableComponent c = Component.empty();
        c.append(Component.literal(label).withStyle(WYNN));
        c.append(Component.literal(SpaceOffset.encode(gap))
                 .withStyle(SpaceOffset.styleFor(WYNN)));
        if (split) {
            c.append(Component.literal(lead + "[").withStyle(PCT));
            c.append(Component.literal(pct).withStyle(PCT));
            c.append(Component.literal("]").withStyle(PCT));
        } else {
            c.append(Component.literal(lead + "[" + pct + "]").withStyle(PCT));
        }
        return c;
    }

    private static int valueX(Component c) {
        int[] x = {0};
        int[] found = {Integer.MIN_VALUE};
        c.visit((style, text) -> {
            text.codePoints().forEach(cp -> {
                if (found[0] != Integer.MIN_VALUE) {
                    return;
                }
                if (cp == '[') {
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
            return cp - 0xD0000;
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

}
