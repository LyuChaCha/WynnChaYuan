package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Style;
import net.minecraft.server.Bootstrap;

import java.util.List;

import com.wynnchayuan.translate.ColumnAligner.Piece;

public final class AlignPointTest {

    private static int failures = 0;

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        Piece label = Piece.text("Class Type", Style.EMPTY);
        Piece value = Piece.text("Archer/Hunter", Style.EMPTY);
        Piece gap = Piece.space(80, Style.EMPTY);
        Piece margin = Piece.space(6, Style.EMPTY);

        List<Piece> labelValue = List.of(label, gap, value, margin);
        check("標籤與數值之間的空白算欄位交界",
                ColumnAligner.isAlignSpace(labelValue, 1));
        check("行尾的留白邊距不算 —— 差額灌進那裡的話數值會黏在標籤旁",
                !ColumnAligner.isAlignSpace(labelValue, 3));

        Piece grey = Piece.text("★★★", Style.EMPTY);
        Piece back = Piece.space(-23, Style.EMPTY);
        Piece lit = Piece.text("★★★", Style.EMPTY);
        List<Piece> stars = List.of(Piece.text("1 x ", Style.EMPTY),
                Piece.text("Ripe Aureate Fruit", Style.EMPTY),
                grey, back, lit);
        check("負偏移不會被當成可調整的欄距",
                ColumnAligner.alignColumns(stars, false).get(3).spacePx() == -23);
        check("正的欄距照常可以調整",
                ColumnAligner.isAlignSpace(labelValue, 1));

        check("譯文變短時把正間隔撐開", ColumnAligner.narrowed(80, 118) == 118);
        check("譯文變長時把正間隔收窄", ColumnAligner.narrowed(80, 40) == 40);
        check("正間隔不會收到把字黏在一起", ColumnAligner.narrowed(80, -30) == 6);

        check("負間隔照樣可以撐開（譯文比原文短）",
                ColumnAligner.narrowed(-25, -4) == -4);
        check("負間隔可以撐到正的", ColumnAligner.narrowed(-25, 12) == 12);
        check("負間隔不會被算得比原文更負 —— 那就是字疊在一起",
                ColumnAligner.narrowed(-25, -60) == -25);
        check("負間隔維持原值時剛好等於原文", ColumnAligner.narrowed(-25, -25) == -25);

        String offsets = new StringBuilder()
                .appendCodePoint(0xCFFD2).appendCodePoint(0xD0064).toString();
        int px = SpaceOffset.decode(offsets);
        Style wynn = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(
                net.minecraft.resources.Identifier.withDefaultNamespace("language/wynncraft")));
        Style spaceFont = SpaceOffset.styleFor(Style.EMPTY);

        check("space 字型的偏移照舊認得（不必量寬度）",
                SpaceRuns.isAdjustableSpace(spaceFont, offsets, 0));
        check("別的字型：量出來的寬度等於偏移值，就是偏移（" + px + " px）",
                SpaceRuns.isAdjustableSpace(wynn, offsets, px));
        check("別的字型：寬度對不上就不當成偏移 —— 那可能是材質包的圖示",
                !SpaceRuns.isAdjustableSpace(wynn, offsets, px + 7));
        check("範圍外的碼位一律不算",
                !SpaceRuns.isAdjustableSpace(spaceFont, "Defence", 41));

        List<Piece> twoColumns = List.of(
                Piece.text("Spell Damage", Style.EMPTY),
                Piece.space(60, Style.EMPTY),
                Piece.text("+60 to", Style.EMPTY),
                Piece.space(24, Style.EMPTY),
                Piece.text("+75", Style.EMPTY));
        check("兩欄的行兩個空白都算交界（第一欄）",
                ColumnAligner.isAlignSpace(twoColumns, 1));
        check("兩欄的行兩個空白都算交界（第二欄）",
                ColumnAligner.isAlignSpace(twoColumns, 3));

        List<Piece> centered = List.of(gap, value, margin);
        check("前導空白不算欄位交界（動它會讓整行位移）",
                !ColumnAligner.isAlignSpace(centered, 0));
        check("前導空白被認出是置中（差額要減半）",
                ColumnAligner.isLeading(centered, 0));
        check("標籤在前時不算置中",
                !ColumnAligner.isLeading(labelValue, 1));

        check("空的一份：沒有欄，不補右緣",
                LineTranslator.columnsAreLeftAligned(List.of()));
        check("null 不會炸", LineTranslator.columnsAreLeftAligned(null));
        check("行數不足三行：沒有欄，不補右緣",
                LineTranslator.columnsAreLeftAligned(List.of(
                        com.wynntils.core.text.StyledText.fromString("Theatre Royal [Cave]"),
                        com.wynntils.core.text.StyledText.fromString("Can be explored"))));
        check("行數夠但沒有一行有欄距：一樣不補",
                LineTranslator.columnsAreLeftAligned(List.of(
                        com.wynntils.core.text.StyledText.fromString("Rewards:"),
                        com.wynntils.core.text.StyledText.fromString("- +7500000 XP"),
                        com.wynntils.core.text.StyledText.fromString("- +Various Items"))));

        check("有現成對齊空白時交界落在空白上",
                ColumnAligner.findAlignPoint(labelValue) == 1
                        && labelValue.get(1).isSpace());

        List<Piece> gapOnlyLater = List.of(
                Piece.text("Combat Experience   ", Style.EMPTY),
                Piece.text("+2% to", Style.EMPTY),
                Piece.space(20, Style.EMPTY),
                Piece.text("+7%", Style.EMPTY));
        int boundary = ColumnAligner.findAlignPoint(gapOnlyLater);
        check("標籤後沒有空白時交界落在第一個數值（不是後面那個空白）",
                boundary == 1 && !gapOnlyLater.get(1).isSpace());

        check("沒有對齊空白時找數值區起點",
                ColumnAligner.findAlignPoint(List.of(
                        Piece.text("Combat Level", Style.EMPTY),
                        Piece.text("114", Style.EMPTY))) == 1);

        check("數值區沒有數字就不算欄位結構",
                ColumnAligner.findAlignPoint(List.of(
                        Piece.text("Doom Stone", Style.EMPTY),
                        Piece.text("◆◆◆", Style.EMPTY))) == -1);

        List<Piece> literalGap = List.of(
                Piece.text(" Class Type", Style.EMPTY),
                Piece.text("  ", Style.EMPTY),
                Piece.text("Mage/Dark Wizard", Style.EMPTY));
        check("字面空格當間隔、數值是文字，也要認得出交界",
                ColumnAligner.findAlignPoint(literalGap) == 2);

        check("字面空格不足兩格時不算欄位（由 hasColumnGap 擋）",
                !ColumnAligner.hasColumnGap(List.of(
                        Piece.text("Emerald Pouch", Style.EMPTY),
                        Piece.text(" ", Style.EMPTY),
                        Piece.text("[Tier 8]", Style.EMPTY)), 1));
        check("兩格以上就算欄位",
                ColumnAligner.hasColumnGap(literalGap, 2));

        List<Piece> range = List.of(
                Piece.text("Earth Main Attack Damage", Style.EMPTY),
                Piece.space(20, Style.EMPTY),
                Piece.text("-2414", Style.EMPTY),
                Piece.text(" to ", Style.EMPTY),
                Piece.text("-1300", Style.EMPTY));
        check("範圍值不會被從中間拆開",
                ColumnAligner.findAlignPoint(range) == 1);

        check("空白全在行尾時不當成對齊點",
                ColumnAligner.findAlignPoint(List.of(
                        Piece.text("Weekly Objectives", Style.EMPTY), margin)) == -1);

        java.util.List<ColumnAligner.Piece> title = java.util.List.of(
                ColumnAligner.Piece.text("", Style.EMPTY),
                ColumnAligner.Piece.space(5, Style.EMPTY),
                ColumnAligner.Piece.text("Dragon's Tome", Style.EMPTY));
        check("前面只有圖示，不算欄位交界",
                !ColumnAligner.labelled(title, 1));
        java.util.List<ColumnAligner.Piece> stat = java.util.List.of(
                ColumnAligner.Piece.text("Health", Style.EMPTY),
                ColumnAligner.Piece.space(40, Style.EMPTY),
                ColumnAligner.Piece.text("+390", Style.EMPTY));
        check("前面有標籤，是欄位交界",
                ColumnAligner.labelled(stat, 1));

        check("純字母的 ID", TemplateLookup.isName("Xikys"));
        check("帶底線的 ID", TemplateLookup.isName("Green_teaTW"));
        check("數字被收成佔位符的 ID", TemplateLookup.isName("{~}N{~}K{~}"));
        check("整串都是數字的 ID", TemplateLookup.isName("{~}"));
        check("一句話不是 ID", !TemplateLookup.isName("the legendary ice mage"));
        check("空的不是 ID", !TemplateLookup.isName(""));

        gearRequirements();

        System.out.println(failures == 0
                ? "AlignPoint: 全部通過"
                : "AlignPoint: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + name);
        if (!ok) {
            failures++;
        }
    }

    private static void gearRequirements() {
        LineTranslator.measureForTest = AlignPointTest::gearWidth;
        try {
            StyledText classType = requirementRow(" Class Type", -66, 72, "Archer/Hunter");
            StyledText combatLevel = requirementRow(" Combat Level", -78, 144, "1");
            List<StyledText> rows = List.of(classType, combatLevel);

            check("原文兩行的右緣本來就對齊（量尺沒歪）",
                    gearWidth(classType.getComponent()) == 149
                            && gearWidth(combatLevel.getComponent()) == 149);
            check("兩行有欄距的需求列＝靠右，不是靠左",
                    !LineTranslator.columnsAreLeftAligned(rows));

            TranslationStore store = new TranslationStore();
            store.loadAll(java.nio.file.Path.of(
                    "src/main/resources/assets/wynnchayuan/translations", "zh_tw"));
            boolean left = LineTranslator.columnsAreLeftAligned(rows);
            for (StyledText row : rows) {
                net.minecraft.network.chat.Component out =
                        LineTranslator.translate(row, store, false, left);
                String plain = out == null ? "(沒翻到)" : out.getString();
                int was = gearWidth(row.getComponent());
                int now = out == null ? -1 : gearWidth(out);
                check("需求列的右緣對回原文：" + plain.replace(" ", "␠")
                                + "  " + was + " -> " + now,
                        now == was);
            }
        } finally {
            LineTranslator.measureForTest = null;
        }
    }

    private static StyledText requirementRow(String label, int rewind, int column,
                                             String value) {
        Style sprite = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(
                net.minecraft.resources.Identifier.withDefaultNamespace(
                        "tooltip/requirement/sprite")));
        Style wynn = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(
                net.minecraft.resources.Identifier.withDefaultNamespace("language/wynncraft")));
        net.minecraft.network.chat.MutableComponent row =
                net.minecraft.network.chat.Component.empty()
                .append(net.minecraft.network.chat.Component.literal(
                        new String(Character.toChars(0xE006)) + SpaceOffset.encode(-1))
                        .withStyle(sprite))
                .append(net.minecraft.network.chat.Component.literal(
                        label + SpaceOffset.encode(rewind) + SpaceOffset.encode(column))
                        .withStyle(wynn))
                .append(net.minecraft.network.chat.Component.literal(value).withStyle(wynn));
        return StyledText.fromComponent(row);
    }

    private static int gearWidth(net.minecraft.network.chat.Component component) {
        int[] w = {0};
        component.visit((style, text) -> {
            int i = 0;
            while (i < text.length()) {
                int cp = text.codePointAt(i);
                i += Character.charCount(cp);
                if (SpaceOffset.isOffset(cp)) {
                    w[0] += SpaceOffset.decode(new String(Character.toChars(cp)));
                } else if (cp >= 0xE000 && cp <= 0xF8FF) {
                    w[0] += 0;
                } else if (cp >= 0x2E80) {
                    w[0] += 9;
                } else {
                    w[0] += 6;
                }
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return w[0];
    }
}
