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
        iconSpace();
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
        // 實機記下的偏移（layout-debug）：標籤也是裝備名的兩列
        lines.add(rowAt(2, "Agility", -37, 144, "+25 to +25"));
        lines.add(iconRow("Agility", -37, 144, "+25 to +25"));
        lines.add(wynnRow("", "Agility ", 151, "+35"));
        lines.add(wynnRow(null, "Reflection ", 101, "+12%"));
        lines.add(spriteOffsetRow("Agility ", 151, "+35"));
        lines.add(rowAt(0, "Reflection", -44, 129, "+27% to +117%"));
        lines.add(rowAt(0, "Exploding", -44, 150, "+15% to +65%"));
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(lines, store);
        System.out.println("== " + lang + "（" + out.size() + " 行）");
        for (int i = 2; i < lines.size() && i < out.size(); i++) {
            int orig = firstTextX(lines.get(i));
            int made = firstTextX(out.get(i));
            StringBuilder fonts = new StringBuilder();
            out.get(i).visit((st, tx) -> {
                fonts.append("[").append(tx.replaceAll("[\\x{D0000}-\\x{DFFFF}\\x{CF000}-\\x{CFFFF}]", "·"))
                     .append("@").append(st.getFont()).append("]");
                return java.util.Optional.empty();
            }, Style.EMPTY);
            System.out.println("    片段 " + fonts);
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

    private static Component rowAt(int lead, String label, int back, int jump, String value) {
        MutableComponent c = Component.empty();
        if (lead != 0) {
            c.append(off(lead));
        }
        c.append(Component.literal(label).withStyle(WHITE));
        c.append(off(back));
        c.append(off(jump));
        c.append(Component.literal(value).withStyle(GREEN));
        c.append(off(-9));
        return c;
    }

    /** 實機的屬性列：屬性圖示（另一個字型）、+2、標籤、往回退、跳到數值欄。 */
    private static Component iconRow(String label, int back, int jump, String value) {
        MutableComponent c = Component.empty();
        c.append(Component.literal("").withStyle(Style.EMPTY.withFont(
                new net.minecraft.network.chat.FontDescription.Resource(
                        net.minecraft.resources.Identifier.withDefaultNamespace(
                                "tooltip/attribute/sprite")))));
        c.append(off(2));
        c.append(Component.literal(label).withStyle(WHITE));
        c.append(off(back));
        c.append(off(jump));
        c.append(Component.literal(value).withStyle(GREEN));
        c.append(off(-9));
        return c;
    }

    private static final Style WYNN = WHITE.withFont(
            new net.minecraft.network.chat.FontDescription.Resource(
                    net.minecraft.resources.Identifier.withDefaultNamespace("language/wynncraft")));

    /**
     * 照 tooltip-partial 記下的實機結構：偏移字元與文字都是 language/wynncraft 字型，
     * 標籤後面帶一個空白——{@code <圖示> [+2] 'Agility ' [+151] '+35'}。
     */
    private static Component wynnRow(String icon, String label, int jump, String value) {
        MutableComponent c = Component.empty();
        if (icon != null) {
            c.append(Component.literal(icon).withStyle(Style.EMPTY.withFont(
                    new net.minecraft.network.chat.FontDescription.Resource(
                            net.minecraft.resources.Identifier.withDefaultNamespace(
                                    "tooltip/attribute/sprite")))));
            c.append(Component.literal(SpaceOffset.encode(2)).withStyle(WYNN));
        }
        c.append(Component.literal(label).withStyle(WYNN));
        c.append(Component.literal(SpaceOffset.encode(jump)).withStyle(WYNN));
        c.append(Component.literal(value).withStyle(WYNN.withColor(0xACFAC6)));
        return c;
    }

    /** 同上，但 +2 那個偏移跟圖示同一個字型。 */
    private static Component spriteOffsetRow(String label, int jump, String value) {
        Style sprite = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(
                net.minecraft.resources.Identifier.withDefaultNamespace("tooltip/attribute/sprite")));
        MutableComponent c = Component.empty();
        c.append(Component.literal("" + SpaceOffset.encode(2)).withStyle(sprite));
        c.append(Component.literal(label).withStyle(WYNN));
        c.append(Component.literal(SpaceOffset.encode(jump)).withStyle(WYNN));
        c.append(Component.literal(value).withStyle(WYNN.withColor(0xACFAC6)));
        return c;
    }

    /**
     * 實機畫出去的那一列（tooltip-partial）：圖示、+2、<b>圖示字型的空白</b>、敏捷。
     * 那個空白把標籤推開十幾像素，要拿掉；圖示與偏移留著。
     */
    private static void iconSpace() {
        Style sprite = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(
                net.minecraft.resources.Identifier.withDefaultNamespace("tooltip/attribute/sprite")));
        MutableComponent drawn = Component.empty();
        drawn.append(Component.literal(String.valueOf((char) 0xE014)).withStyle(sprite));
        drawn.append(Component.literal(SpaceOffset.encode(2)).withStyle(WYNN));
        drawn.append(Component.literal(" ").withStyle(sprite));
        drawn.append(Component.literal("敏捷").withStyle(WHITE));
        drawn.append(Component.literal(SpaceOffset.encode(166)).withStyle(WYNN));
        drawn.append(Component.literal("+35").withStyle(GREEN));
        Component fixed = LineTranslator.dropIconSpaces(drawn);
        String plain = fixed.getString();
        check("★ 圖示字型的空白拿掉了", !plain.contains(" 敏捷"));
        check("圖示、偏移與數值都還在",
              plain.startsWith(String.valueOf((char) 0xE014) + SpaceOffset.encode(2) + "敏捷")
                      && plain.endsWith("+35"));
        // 一般字型的空白是真的排版，不動
        MutableComponent normal = Component.empty();
        normal.append(Component.literal(" 职业类型").withStyle(WHITE));
        check("預設字型的前導空白不動",
              LineTranslator.dropIconSpaces(normal).getString().equals(" 职业类型"));
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
            } else if (!text.isBlank() && text.codePoints().allMatch(
                    cp -> cp >= 0xE000 && cp <= 0xF8FF)) {
                x[0] += measure(Component.literal(text));   // 圖示
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
