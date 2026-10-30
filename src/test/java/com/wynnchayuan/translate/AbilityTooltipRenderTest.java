package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class AbilityTooltipRenderTest {

    private static int failures = 0;

    private static final Style COMMON = Style.EMPTY.withFont(new FontDescription.Resource(
            Identifier.withDefaultNamespace("common")));

    private static final int GREY = 0xAAAAAA;
    private static final int DARK = 0x555555;
    private static final int WHITE = 0xFFFFFF;
    private static final int GREEN = 0x55FF55;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wcy-ability-render");
        Files.writeString(dir.resolve("misc.json"), """
                {
                  "Judgement": "審判",
                  "{#} Effects: +{~} Damage Bonus (⚔) to Allies": "{#} 效: +{~} 甲 (⚔) 乙",
                  "À  +{~} Resistance Bonus (❁) to Allies": "À  +{~} 丙 (❁) 乙",
                  "À  +{~} ID Effectiveness (❈) to Allies": "À  +{~} 丁 (❈) 乙",
                  "{#} Effect: {~} Resistance Bonus (❁) to Self": "{#} 效: {~} 丙 (❁) 戊",
                  "{#} Duration: {~}s": "{#} 時: {~} 秒",
                  "{#} Total Damage: {~} (of your DPS)": "{#} 總: {~} (己)",
                  "ÀÀÀÀ({#} Damage: {~})": "ÀÀÀÀ({#} 傷: {~})",
                  "⚔ Total Damage: {~} (of your DPS, per Static)": "⚔ 總: {~} (己，庚)",
                  "(✣ Damage: {~})": "(✣ 傷: {~})",
                  "✔ Ability Points: {~}": "✔ 點: {~}"
                }
                """, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        LineTranslator.measureForTest = AbilityTooltipRenderTest::measure;
        try {
            continuationRows(store);
            subDamageRows(store);
            iconColour(store);
            notAbilityPanel(store);
        } finally {
            LineTranslator.measureForTest = null;
        }
        System.out.println(failures == 0 ? "技能樹說明的版面與顏色：全部通過"
                                         : "技能樹說明的版面與顏色：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void continuationRows(TranslationStore store) {
        MutableComponent head = line("", COMMON.withColor(0xFFFF55), " ", c(0xFFFF55),
                "Effects: ", c(GREY), "+20% ", c(WHITE), "Damage Bonus (", c(GREY),
                "⚔", c(GREEN), ") to Allies", c(GREY));
        int valueX = xOf(head, "+");
        String indent = indentTo(valueX);
        List<Component> t = panel(head,
                line(indent, c(GREY), "+20% ", c(WHITE), "Resistance Bonus (", c(GREY),
                        "❁", c(GREEN), ") to Allies", c(GREY)),
                line(indent, c(GREY), "+40% ", c(WHITE), "ID Effectiveness (", c(GREY),
                        "❈", c(0xFFAA00), ") to Allies", c(GREY)),
                line("", COMMON.withColor(0xFF55FF), " ", c(0xFF55FF),
                        "Duration: ", c(GREY), "15s", c(WHITE)));
        check("原文的續行本來就對齊在第一項的數值底下（" + xOf(t.get(3), "+") + " = " + valueX + "）",
              xOf(t.get(3), "+") == valueX);
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(t, store);
        dump(t, out);
        int headX = xOf(out.get(2), "+");
        check("第一項翻出來了，數值欄跟著短掉的標籤往左挪（" + valueX + " → " + headX + "）",
              out.get(2).getString().contains("效") && headX >= 0 && headX < valueX);
        for (int row = 3; row <= 4; row++) {
            int x = xOf(out.get(row), "+");
            check("★ 第 " + (row - 1) + " 項落在譯文第一項的數值底下（第一項 " + headX
                          + "、這一項 " + x + "）",
                  out.get(row).getString().contains("乙") && x == headX);
        }
        check("下一列（持續時間）沒有被補縮排",
              firstVisibleX(out.get(5)) == firstVisibleX(t.get(5)));
    }

    private static void subDamageRows(TranslationStore store) {
        List<Component> t = panel(
                line("⚔ ", c(0xFF5555), "Total Damage: ", c(GREY), "30% ", c(WHITE),
                        "(of your DPS, per Static)", c(DARK)),
                line("   ", c(GREY), "(", c(DARK), "✣ ", c(0xFFAA00), "Damage: 25%)", c(DARK)),
                Component.empty(),
                line("", COMMON.withColor(0xFF5555), " ", c(0xFF5555), "Total Damage: ", c(GREY),
                        "5000% ", c(WHITE), "(of your DPS)", c(DARK)),
                line("  ÀÀÀÀ", c(GREY), "(", c(DARK), "", COMMON.withColor(0xFFAA00),
                        " ", c(0xFFAA00), "Damage: ", c(DARK), "3500%", c(DARK), ")", c(DARK)));
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(t, store);
        dump(t, out);
        for (int row : new int[] {3, 6}) {
            int was = xOf(t.get(row), "(");
            int now = xOf(out.get(row), "(");
            check("★ 第 " + row + " 列的小項保住原文的縮排（原文 " + was + "、譯文 " + now + "）",
                  out.get(row).getString().contains("傷") && was > 0 && now == was);
        }
    }

    private static void iconColour(TranslationStore store) {
        List<Component> t = panel(
                line("", COMMON.withColor(0xFFFF55), " ", c(0xFFFF55), "Effect: ", c(GREY),
                        "50% ", c(WHITE), "Resistance Bonus (", c(GREY), "❁", c(GREEN),
                        ") to Self", c(GREY)));
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(t, store);
        dump(t, out);
        Component row = out.get(2);
        check("★ ❁ 保住原文的綠色（實際 " + hex(colourAt(row, "❁")) + "）",
              Integer.valueOf(GREEN).equals(colourAt(row, "❁")));
        check("說明文字不會被染成數值的白色（實際 " + hex(colourAt(row, "丙")) + "）",
              Integer.valueOf(GREY).equals(colourAt(row, "丙")));
        check("數值仍是白色", Integer.valueOf(WHITE).equals(colourAt(row, "50%")));
        List<Component> plus = panel(line("", COMMON.withColor(0xFFFF55), " ", c(0xFFFF55),
                "Effects: ", c(GREY), "+20% ", c(WHITE), "Damage Bonus (", c(GREY),
                "⚔", c(GREEN), ") to Allies", c(GREY)));
        Component other = com.wynnchayuan.render.TooltipPanel.translateLines(plus, store).get(2);
        check("對照組的 ⚔ 仍是綠色", Integer.valueOf(GREEN).equals(colourAt(other, "⚔")));
    }

    private static void notAbilityPanel(TranslationStore store) {
        List<Component> t = new ArrayList<>();
        t.add(line("Judgement", c(WHITE)));
        t.add(line("   ", c(GREY), "(", c(DARK), "✣ ", c(0xFFAA00), "Damage: 25%)", c(DARK)));
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(t, store);
        dump(t, out);
        check("不是技能樹時，空白縮排的行沒有被插入偏移",
              out.size() == 2 && !hasOffset(out.get(1)));
    }

    private static List<Component> panel(Component... rows) {
        List<Component> t = new ArrayList<>();
        t.add(line("Judgement", c(WHITE).withBold(true)));
        t.add(Component.empty());
        t.addAll(List.of(rows));
        t.add(Component.empty());
        t.add(line("✔ ", c(GREEN), "Ability Points: ", c(GREY), "2", c(WHITE)));
        return t;
    }

    private static String indentTo(int px) {
        int spaces = (px - 8 - 2) / 4;
        int accents = px - 8 - spaces * 4;
        return " ".repeat(spaces) + "À".repeat(accents) + "  ";
    }

    private static Style c(int rgb) {
        return Style.EMPTY.withColor(TextColor.fromRgb(rgb));
    }

    private static MutableComponent line(Object... parts) {
        MutableComponent m = Component.empty();
        for (int i = 0; i < parts.length; i += 2) {
            m.append(Component.literal((String) parts[i]).withStyle((Style) parts[i + 1]));
        }
        return m;
    }

    private static int xOf(Component line, String needle) {
        StringBuilder text = new StringBuilder();
        List<Style> styles = new ArrayList<>();
        line.visit((style, s) -> {
            for (int i = 0; i < s.length(); i++) {
                text.append(s.charAt(i));
                styles.add(style);
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        int at = text.indexOf(needle);
        if (at < 0) {
            return -1;
        }
        MutableComponent prefix = Component.empty();
        int from = 0;
        for (int i = 1; i <= at; i++) {
            if (i == at || !styles.get(i).equals(styles.get(from))) {
                prefix.append(Component.literal(text.substring(from, i)).withStyle(styles.get(from)));
                from = i;
            }
        }
        return measure(prefix);
    }

    private static int firstVisibleX(Component line) {
        String s = line.getString();
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            if (cp != ' ' && cp != 0xC0 && !SpaceOffset.isOffset(cp)) {
                return xOf(line, new String(Character.toChars(cp)));
            }
            i += Character.charCount(cp);
        }
        return -1;
    }

    private static boolean hasOffset(Component line) {
        return line.getString().codePoints().anyMatch(SpaceOffset::isOffset);
    }

    private static Integer colourAt(Component line, String needle) {
        Integer[] found = {null};
        line.visit((style, s) -> {
            if (found[0] == null && s.contains(needle) && style.getColor() != null) {
                found[0] = style.getColor().getValue() & 0xFFFFFF;
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static String hex(Integer rgb) {
        return rgb == null ? "（找不到）" : String.format("#%06X", rgb);
    }

    private static void dump(List<Component> in, List<Component> out) {
        for (int i = 0; i < out.size(); i++) {
            StringBuilder sb = new StringBuilder();
            out.get(i).visit((style, s) -> {
                sb.append('[').append(s.codePoints().mapToObj(cp -> SpaceOffset.isOffset(cp)
                        ? "<" + (cp - 0xD0000) + ">" : new String(Character.toChars(cp)))
                        .reduce("", String::concat));
                if (style.getColor() != null) {
                    sb.append(' ').append(String.format("#%06X", style.getColor().getValue()));
                }
                sb.append(']');
                return java.util.Optional.empty();
            }, Style.EMPTY);
            System.out.println("    " + i + " " + sb);
        }
    }

    private static int advance(int cp, Style style) {
        if (cp == 0xC0) {
            return 1;
        }
        if ((cp >= 0xE000 && cp <= 0xF8FF) || cp >= 0x2E80) {
            return 9;
        }
        int w = switch (cp) {
            case 'i', '.', ':', ',', ';', '!', '\'', '|' -> 2;
            case 'l' -> 3;
            case 't', 'I', ' ', '(', ')', '[', ']' -> 4;
            case 'f', 'k', '<', '>' -> 5;
            default -> 6;
        };
        return w + (style.isBold() && cp != ' ' ? 1 : 0);
    }

    static int measure(Component component) {
        int[] w = {0};
        component.visit((style, text) -> {
            text.codePoints().forEach(cp -> {
                if (SpaceOffset.isOffset(cp)) {
                    w[0] += SpaceOffset.decode(new String(Character.toChars(cp)));
                } else if (cp != '\n') {
                    w[0] += advance(cp, style);
                }
            });
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
