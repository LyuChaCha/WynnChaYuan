package com.wynnchayuan.translate;

import com.wynnchayuan.render.TooltipPanel;
import com.wynnchayuan.render.WynnModTooltip;
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

/**
 * WynnMod 重畫的裝備說明：標籤被拆成兩個片段、或被砍掉尾巴的那幾列。
 *
 * <h2>實機回報（2026-10-10）</h2>
 * 整份說明都翻好了，只剩這幾列是英文：
 *
 * <pre>
 *   Main Scale            [90.7%]
 *   Elemental Spell Da..  *+366 [92.0%]
 * </pre>
 *
 * 兩列是同一個原因。WynnMod 的說明是它自己一段一段接起來的，標籤拆成
 * 「Main」＋「 Scale」、「Elemental Spell Da」＋「..」兩個樣式相同的片段，而屬性列
 * 是逐片段查的——拆開的每一半都不是標籤。「Damage Scale」翻得出來只是因為語料
 * 剛好另外收了整列的條目。
 *
 * <p>下面的列照診斷檔 {@code wynnmod-tooltip-2.json} 的片段重建（字型、顏色、偏移
 * 都照抄），整份丟進跟實機同一個入口。不寫死譯文：標籤該翻成什麼，問沒被拆開的
 * 那一種寫法。
 */
public final class WynnModRowsTest {

    private static int failures = 0;

    private static final Style WYNN = font("language/wynncraft").withColor(TextColor.fromRgb(0xFFFFFF));
    private static final Style PLAIN = font("default").withColor(TextColor.fromRgb(0xFFFFFF));
    private static final Style SPACE = font("space").withColor(TextColor.fromRgb(0xFFFFFF));
    private static final Style ICON = font("tooltip/identification/major")
            .withColor(TextColor.fromRgb(0xFFFFFF));
    private static final Style STAR = font("language/wynncraft").withColor(TextColor.fromRgb(0x567D63));
    private static final Style VALUE = font("language/wynncraft").withColor(TextColor.fromRgb(0xACFAC6));
    private static final Style ROLL = font("language/wynncraft").withColor(TextColor.fromRgb(0x55FF85));

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                              Languages.DEFAULT));

        LineTranslator.measureForTest = WynnModRowsTest::width;
        try {
            split(store);
        } finally {
            LineTranslator.measureForTest = null;
        }

        System.out.println(failures == 0
                ? "WynnModRows: 全部通過" : "WynnModRows: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void split(TranslationStore store) {
        // 同一份說明裡沒被拆、沒被砍的寫法：標籤一段。這一份只拿來問標籤該翻成什麼。
        List<Component> whole = List.of(
                row("Halcyon"),
                scale(73, "Main Scale"),
                scale(57, "Damage Scale"),
                stat(14, true, "+34%", "Healing Efficiency"),
                stat(9, true, "+366", "Elemental Spell Damage"),
                stat(9, true, "+12%", "Elemental Spell Damage"),
                stat(44, false, "-5", "Ice Snake Cost"),
                stat(9, false, "+405", "Fire Main Attack Damage"),
                stat(9, false, "+32%", "Fire Main Attack Damage"),
                stat(9, false, "+29%", "Elemental Spell Damage"));
        // 實機的寫法：「Main」＋「 Scale」，「Elemental Spell Da」＋「..」
        List<Component> split = List.of(
                row("Halcyon"),
                scale(73, "Main", " Scale"),
                scale(57, "Damage", " Scale"),
                stat(14, true, "+34%", "Healing Efficiency"),
                stat(9, true, "+366", "Elemental Spell Da", ".."),
                stat(9, true, "+12%", "Elemental Spell Da", ".."),
                stat(44, false, "-5", "Ice Snake Cost"),
                // 第二版測試還是沒翻到的那一種：兩個點是 WynnMod 另外做好接上去的，
                // 畫出來一樣，樣式物件卻不相等（斜體「沒設」對「否」、顏色「沒設」對白色）。
                truncated(9, "+405", "Fire Main Attack Da", WYNN.withItalic(false)),
                truncated(9, "+32%", "Fire Main Attack D", font("language/wynncraft")),
                // 連懸停事件都不一樣的兩個點：照樣是那個標籤的一部分
                truncated(9, "+29%", "Elemental Spell Da", WYNN.withInsertion("x")));
        check("前提：那兩種兩個點的樣式跟標籤不相等，但畫出來一樣",
                !WYNN.equals(WYNN.withItalic(false)) && !WYNN.equals(font("language/wynncraft")));

        List<String> want = labels(TooltipPanel.translateInPlace(whole, store));
        List<Component> shown = TooltipPanel.translateInPlace(
                WynnModTooltip.joinSameStyle(split), store);
        List<String> got = labels(shown);

        check("前提：沒被拆開的寫法每一列都翻得出來（實際 " + want + "）",
                want.size() == whole.size()
                        && want.subList(1, want.size()).stream().noneMatch(WynnModRowsTest::english));
        check("前提：實數與百分比的元素法術傷害是兩種標籤（實際 " + want.get(4) + "／" + want.get(5) + "）",
                want.size() > 5 && !want.get(4).equals(want.get(5)));
        check("拆成兩段的說明有翻出東西", got.size() == split.size());
        if (got.size() != split.size() || want.size() != whole.size()) {
            return;
        }
        check("★ 「Main」＋「 Scale」翻成跟沒拆開一樣的標籤（實際 " + got.get(1) + "）",
                got.get(1).equals(want.get(1)));
        check("「Damage」＋「 Scale」照舊（實際 " + got.get(2) + "）", got.get(2).equals(want.get(2)));
        check("★ 「Elemental Spell Da..」換回完整標籤的譯名（實際 " + got.get(4) + "）",
                got.get(4).equals(want.get(4)));
        check("★ 百分比的那一列挑百分比的標籤（實際 " + got.get(5) + "）",
                got.get(5).equals(want.get(5)));
        for (int i = 1; i < got.size(); i++) {
            String all = shown.get(i).getString();
            check("第 " + i + " 列沒有留下兩個點（實際 " + got.get(i) + "）", !all.contains(".."));
        }
        check("★ 兩個點的樣式物件不相等也併得起來（實際 " + got.get(7) + "、" + got.get(8) + "）",
                got.get(7).equals(want.get(7)) && got.get(8).equals(want.get(8)));
        check("★ 兩個點帶著別的欄位也併（實際 " + got.get(9) + "）", got.get(9).equals(want.get(9)));
        check("前提：火屬性普攻的實數與百分比是兩種標籤（實際 " + want.get(7) + "／" + want.get(8) + "）",
                !want.get(7).equals(want.get(8)));
        // 顏色不同的不併：那是兩個欄位，不是一個標籤
        Component twoColours = Component.empty()
                .append(Component.literal("Main").withStyle(WYNN))
                .append(Component.literal(" Scale").withStyle(VALUE));
        check("顏色不同的相鄰片段不併",
                WynnModTooltip.joinSameStyle(List.of(twoColours)).get(0) == twoColours);
        check("沒被拆的列不受影響（實際 " + got.get(3) + "、" + got.get(6) + "）",
                got.get(3).equals(want.get(3)) && got.get(6).equals(want.get(6)));

        // 數值那一欄要留在原位：每一列「標籤寬＋欄距」跟原文一樣。
        for (int i = 1; i < split.size(); i++) {
            int before = valueStart(split.get(i));
            int after = valueStart(shown.get(i));
            check("★ 第 " + i + " 列的數值欄沒有移位（原文 " + before + "，譯文 " + after + "）",
                    before == after);
        }

        // 反面：沒有任何可併的片段時，原本那一行原樣交回去（同一個物件）。
        Component untouched = stat(14, true, "+34%", "Healing Efficiency");
        check("沒有可併的片段時不重建那一行",
                WynnModTooltip.joinSameStyle(List.of(untouched)).get(0) == untouched);
        // 圖示與排版偏移不併：個數與順序是版面的一部分。
        MutableComponent offsets = Component.empty()
                .append(Component.literal(SpaceOffset.encode(-1)).withStyle(SPACE))
                .append(Component.literal(SpaceOffset.encode(-1)).withStyle(SPACE));
        check("相鄰的排版偏移不併",
                WynnModTooltip.joinSameStyle(List.of((Component) offsets)).get(0) == offsets);
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    /** 每一列在第一個排版偏移之前的字（去掉圖示）——也就是標籤。 */
    private static List<String> labels(List<Component> lines) {
        List<String> out = new ArrayList<>();
        for (Component line : lines) {
            StringBuilder label = new StringBuilder();
            boolean[] done = {false};
            line.visit((style, text) -> {
                text.codePoints().forEach(cp -> {
                    if (SpaceOffset.isOffset(cp)) {
                        done[0] = true;
                    } else if (!done[0] && !(cp >= 0xE000 && cp <= 0xF8FF)) {
                        label.appendCodePoint(cp);
                    }
                });
                return java.util.Optional.empty();
            }, Style.EMPTY);
            out.add(label.toString().strip());
        }
        return out;
    }

    /** 最後一個排版偏移結束的位置：數值那一欄從這裡開始。 */
    private static int valueStart(Component line) {
        int[] at = {0};
        int[] start = {0};
        line.visit((style, text) -> {
            text.codePoints().forEach(cp -> {
                at[0] += width(cp);
                if (SpaceOffset.isOffset(cp)) {
                    start[0] = at[0];
                }
            });
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return start[0];
    }

    private static boolean english(String label) {
        return label.chars().anyMatch(c -> (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'));
    }

    private static Component row(String text) {
        return Component.literal(text).withStyle(WYNN);
    }

    /** 「圖示＋空白＋標籤＋欄距＋[適性]」。 */
    private static Component scale(int gap, String... label) {
        MutableComponent row = Component.empty()
                .append(Component.literal(new String(Character.toChars(0xE000))).withStyle(ICON))
                .append(Component.literal(" ").withStyle(PLAIN));
        for (String part : label) {
            row.append(Component.literal(part).withStyle(WYNN));
        }
        return row.append(Component.literal(SpaceOffset.encode(gap)).withStyle(SPACE))
                .append(Component.literal("[90.7%]").withStyle(ROLL));
    }

    /** 「標籤＋欄距＋(星號)＋數值＋空白＋[品質]」。 */
    private static Component stat(int gap, boolean star, String value, String... label) {
        MutableComponent row = Component.empty();
        for (String part : label) {
            row.append(Component.literal(part).withStyle(WYNN));
        }
        row.append(Component.literal(SpaceOffset.encode(gap)).withStyle(SPACE));
        if (star) {
            row.append(Component.literal("*").withStyle(STAR));
        }
        return row.append(Component.literal(value).withStyle(VALUE))
                .append(Component.literal(" ").withStyle(PLAIN))
                .append(Component.literal("[92.0%]").withStyle(ROLL));
    }

    /** 砍過的標籤：兩個點用另一個「畫出來一樣」的樣式接上去。 */
    private static Component truncated(int gap, String value, String label, Style dots) {
        return Component.empty()
                .append(Component.literal(label).withStyle(WYNN))
                .append(Component.literal("..").withStyle(dots))
                .append(Component.literal(SpaceOffset.encode(gap)).withStyle(SPACE))
                .append(Component.literal(value).withStyle(VALUE))
                .append(Component.literal(" ").withStyle(PLAIN))
                .append(Component.literal("[92.0%]").withStyle(ROLL));
    }

    private static Style font(String id) {
        return Style.EMPTY.withFont(new FontDescription.Resource(
                Identifier.withDefaultNamespace(id)));
    }

    /** 量尺：半形 6、全形 9、圖示 0、偏移碼位照面值。 */
    private static int width(Component component) {
        int[] w = {0};
        component.visit((style, text) -> {
            text.codePoints().forEach(cp -> w[0] += width(cp));
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return w[0];
    }

    private static int width(int cp) {
        if (SpaceOffset.isOffset(cp)) {
            return SpaceOffset.decode(new String(Character.toChars(cp)));
        }
        if (cp >= 0xE000 && cp <= 0xF8FF) {
            return 0;
        }
        return cp >= 0x2E80 ? 9 : 6;
    }
}
