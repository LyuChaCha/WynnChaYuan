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
 * 裝備 tooltip 的屬性列：標籤一律靠左，從原文標籤的起點開始畫。
 *
 * <h2>實機回報</h2>
 * 簡中的 Divzer：「灵巧」「防御」「法力偷取」「攻击速度」「法术伤害」都被推到中間，
 * 「生命偷取」「普攻伤害」卻靠左。英文版每一列都靠左。
 *
 * <p>遊戲送來的一列長這樣：{@code [+2]Dexterity[-標籤寬][+148]+37 to +37[-9]}——
 * 標籤畫完先退回起點，再一口氣跳到數值欄，所以數值的位置跟標籤寬度無關。
 */
public final class StatLabelIndentTest {

    private static int failures = 0;

    private static final Style WHITE = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF));
    private static final Style GREEN = Style.EMPTY.withColor(TextColor.fromRgb(0xACFAC6));

    public static void main(String[] args) {
        Path root = Path.of(args.length > 0 ? args[0]
                                            : "src/main/resources/assets/wynnchayuan/translations");
        LineTranslator.measureForTest = StatLabelIndentTest::measure;
        try {
            for (String[] langs : new String[][] {{"zh_tw"}, {"zh_tw", "zh_cn"}}) {
                TranslationStore store = new TranslationStore();
                List<Path> dirs = new ArrayList<>();
                for (String l : langs) {
                    dirs.add(root.resolve(l));
                }
                store.loadAll(dirs);
                store.setNameMode(CollectorConfig.ItemNames.OFF);
                run(String.join("+", langs), store);
            }
        } finally {
            LineTranslator.measureForTest = null;
        }
        System.out.println(failures == 0 ? "屬性列靠左：全部通過" : "屬性列靠左：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void run(String lang, TranslationStore store) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Divzer"));
        lines.add(Component.literal("Divzer"));
        lines.add(row(2, "Dexterity", "+37 to +37"));
        lines.add(row(2, "Defence", "-73 to -73"));
        lines.add(row(2, "Agility", "-550 to -550"));
        lines.add(row(0, "Life Steal", "+274/3s to +1,186/3s"));
        lines.add(row(0, "Mana Steal", "+6/3s to +26/3s"));
        lines.add(row(0, "Attack Speed", "+1 tier to +1 tier"));
        lines.add(row(0, "Water Damage", "-715% to -385%"));
        lines.add(row(0, "Spell Damage", "+76 to +329"));
        lines.add(row(0, "Main Attack Damage", "+161 to +697"));
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(lines, store);
        System.out.println("== " + lang + "（" + out.size() + " 行）");
        for (int i = 2; i < lines.size() && i < out.size(); i++) {
            int orig = firstTextX(lines.get(i));
            int made = firstTextX(out.get(i));
            System.out.println("  " + out.get(i).getString().replaceAll("[\\x{D0000}-\\x{DFFFF}\\x{CF000}-\\x{CFFFF}]", "·")
                    + "  標籤起點 原文=" + orig + " 譯文=" + made);
            check(lang + "「" + lines.get(i).getString().replaceAll("[^A-Za-z ]", "").strip()
                          + "」標籤從原文的起點開始（原文 " + orig + "、譯文 " + made + "）",
                  made == orig);
        }
    }

    /** {@code [lead]label[-labelWidth][+150]value[-9]} */
    private static Component row(int lead, String label, String value) {
        MutableComponent c = Component.empty();
        if (lead != 0) {
            c.append(off(lead));
        }
        c.append(Component.literal(label).withStyle(WHITE));
        c.append(off(-measure(Component.literal(label))));
        c.append(off(150));
        c.append(Component.literal(value).withStyle(GREEN));
        c.append(off(-9));
        return c;
    }

    private static Component off(int px) {
        return Component.literal(SpaceOffset.encode(px)).withStyle(SpaceOffset.styleFor(WHITE));
    }

    /** 第一個實字畫在第幾個像素。 */
    private static int firstTextX(Component c) {
        int[] x = {0};
        int[] found = {Integer.MIN_VALUE};
        c.visit((style, text) -> {
            if (found[0] != Integer.MIN_VALUE) {
                return java.util.Optional.empty();
            }
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                x[0] += SpaceOffset.decode(text);
            } else if (!text.isBlank()) {
                int lead = 0;
                while (lead < text.length() && text.charAt(lead) == ' ') {
                    lead++;
                }
                found[0] = x[0] + lead * 4;
            } else {
                x[0] += text.length() * 4;
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static int advance(int cp) {
        if (cp >= 0x2E80) {
            return 9;
        }
        return switch (cp) {
            case 'i', '.', ':', ',', ';', '!', '\'' -> 2;
            case 'l' -> 3;
            case 't', 'I', ' ' -> 4;
            case 'f', 'k' -> 5;
            default -> 6;
        };
    }

    static int measure(Component component) {
        int[] w = {0};
        component.visit((style, text) -> {
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                w[0] += SpaceOffset.decode(text);
            } else {
                text.codePoints().forEach(cp -> w[0] += cp == '\n' ? 0 : advance(cp));
            }
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
