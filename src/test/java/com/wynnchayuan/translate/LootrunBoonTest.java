package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lootrun 賜福（boon）的敘述：整段沒翻時一個字都不翻，整段翻了要折得好看、顏色跟著來源走。
 */
public final class LootrunBoonTest {

    private static int failures = 0;

    private static final Style GREY = Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA));
    private static final Style AQUA = Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF));
    private static final Style RED = Style.EMPTY.withColor(TextColor.fromRgb(0xFF5555));
    private static final Style SPRITE = Style.EMPTY.withFont(
            new net.minecraft.network.chat.FontDescription.Resource(
                    net.minecraft.resources.Identifier.withDefaultNamespace(
                            "tooltip/attribute/sprite")));
    private static final String ICON = "󯿿󰀁󰀂";

    public static void main(String[] args) {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        persnickety(store);
        statRows(store);
        accentMove();
        heavensent(store);
        rewrapColour(store);

        System.out.println(failures == 0
                ? "Lootrun 賜福：全部通過" : "Lootrun 賜福：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void persnickety(TranslationStore store) {
        System.out.println("=== Persnickety ===");
        List<Component> tip = new ArrayList<>();
        tip.add(line(GREY, "Persnickety"));
        tip.add(line(GREY, " "));
        tip.add(line(GREY, "Once you have been offered a"));
        tip.add(line(GREY, "Blue or Purple Beacon more"));
        tip.add(join(part(GREY, "than "), part(AQUA, "20"), part(GREY, " times this Lootrun,")));
        tip.add(join(part(GREY, "gain "), part(AQUA, "+50%"), part(GREY, " Walk Speed")));
        tip.add(line(GREY, " "));
        tip.add(join(part(GREY, "✔ "), part(GREY, "Blue Beacon Offered: "), part(AQUA, "1")));
        tip.add(line(GREY, " "));
        tip.add(line(GREY, "Click to choose!"));
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(tip, store);
        dump(out);
        check("整份還是畫得出來", out.size() == tip.size());
        if (out.size() != tip.size()) {
            return;
        }
        check("★ 句子裡的屬性名跟著整句留英文（實際 " + out.get(5).getString() + "）",
              out.get(5).getString().equals("gain +50% Walk Speed"));
        check("獨立的一行照翻（實際 " + out.get(7).getString() + "）",
              !out.get(7).getString().contains("Offered"));
    }

    private static void heavensent(TranslationStore store) {
        System.out.println("=== Heavensent ===");
        List<StyledText> run = List.of(
                StyledText.fromComponent(line(GREY, "For the rest of your Lootrun,")),
                StyledText.fromComponent(join(part(GREY, "gain "), part(AQUA, "+2"),
                        part(GREY, " "), part(SPRITE, ICON), part(RED, "Defence"),
                        part(GREY, " (Max x15) for"))),
                StyledText.fromComponent(line(GREY, "each Beacon offered")));
        LineTranslator.measureForTest = LootrunBoonTest::width;
        List<Component> out;
        try {
            out = LineTranslator.translateBlock(run, store, new boolean[3]);
        } finally {
            LineTranslator.measureForTest = null;
        }
        check("整段查得到", out != null);
        if (out == null) {
            return;
        }
        dump(out);
        int valueRow = -1;
        int iconRow = -1;
        for (int r = 0; r < out.size(); r++) {
            String s = out.get(r).getString();
            check("第 " + r + " 行不是從圖示開始（實際 " + s + "）", !s.startsWith(ICON));
            if (s.contains("+2")) {
                valueRow = r;
            }
            if (s.contains(ICON)) {
                iconRow = r;
            }
        }
        check("★ +2 跟屬性圖示在同一行（+2 在第 " + valueRow + " 行，圖示在第 " + iconRow + " 行）",
              valueRow >= 0 && valueRow == iconRow);
        check("數值用原文數值的顏色", colourOf(out, "2") == 0x55FFFF);
        check("屬性名用原文屬性名的顏色", colourOf(out, "防禦") == 0xFF5555);
        String all = String.join("", out.stream().map(Component::getString).toList());
        check("字沒有變少（實際 " + all + "）",
              all.replace(" ", "").equals(
                      ("本次 Lootrun 剩餘期間，每提供一個信標就 +2 " + ICON + "防禦 (上限 x15)")
                              .replace(" ", "")));
    }

    /**
     * 真正的屬性列不能被「句子不翻片段」那條規則拖下水：它們前面是空行或別的欄位，
     * 不是一句沒收尾的話。
     */
    private static void statRows(TranslationStore store) {
        System.out.println("=== 屬性列 ===");
        List<Component> tip = new ArrayList<>();
        tip.add(line(GREY, "Persnickety"));
        tip.add(line(GREY, " "));
        tip.add(join(part(AQUA, "+13%"), part(GREY, " Walk Speed")));
        tip.add(join(part(AQUA, "+450"), part(GREY, " Health Regen")));
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(tip, store);
        dump(out);
        check("整份畫得出來", out.size() == tip.size());
        if (out.size() != tip.size()) {
            return;
        }
        check("「+13% Walk Speed」照翻（實際 " + out.get(2).getString() + "）",
              !out.get(2).getString().contains("Walk"));
        check("「+450 Health Regen」照翻（實際 " + out.get(3).getString() + "）",
              !out.get(3).getString().contains("Health"));
    }

    /**
     * 整段譯文是<b>我們自己折</b>回原文行數的時候，「第 i 行」兩邊指的不是同一段字。
     * 原文最後一行整行是深灰，但折出來的最後一行裝的是第二行的「防禦 (上限 x15)」——
     * 那幾個字不能被染成深灰。
     */
    private static void rewrapColour(TranslationStore store) {
        System.out.println("=== 折行後的顏色 ===");
        Style dark = Style.EMPTY.withColor(TextColor.fromRgb(0x555555));
        List<StyledText> run = List.of(
                StyledText.fromComponent(line(GREY, "For the rest of your Lootrun,")),
                StyledText.fromComponent(join(part(GREY, "gain "), part(AQUA, "+2"),
                        part(GREY, " "), part(SPRITE, ICON), part(RED, "Defence"),
                        part(GREY, " (Max x15) for"))),
                StyledText.fromComponent(line(dark, "each Beacon offered")));
        LineTranslator.measureForTest = LootrunBoonTest::width;
        List<Component> out;
        try {
            out = LineTranslator.translateBlock(run, store, new boolean[3]);
        } finally {
            LineTranslator.measureForTest = null;
        }
        check("整段查得到", out != null);
        if (out == null) {
            return;
        }
        dump(out);
        check("「(上限 x」不會拿到原文最後一行的深灰（實際 #"
                        + Integer.toHexString(colourOf(out, "(上限")) + "）",
              colourOf(out, "(上限") != 0x555555);
    }

    /**
     * 實機的斷法：折行斷在「+{~1} {#}防／禦」，重點詞「防禦」整個搬下去時，
     * 圖示與它前面的數值都要跟著走。只看折行測不到——兇手是折完之後的搬詞。
     */
    private static void accentMove() {
        System.out.println("=== 搬詞時數值跟著圖示 ===");
        String[] split = {"本次 Lootrun 剩餘期間，", "每提供一個信標就 +{~1} {#}防",
                          "禦 (上限 x{~2})"};
        String[] kept = LineTranslator.keepAccentsWhole(split, List.of(
                new com.wynnchayuan.capture.LineParts.Piece("防禦", RED)));
        System.out.println("    搬完：" + String.join(" ⏎ ", kept));
        check("★ 數值、圖示、屬性名在同一行", kept[2].startsWith("+{~1} {#}防禦"));
        check("上一行留下的是句子", kept[1].strip().equals("每提供一個信標就"));
        check("字沒有變少", String.join("", kept).equals(String.join("", split)));

        // 反面：圖示前面是字不是數值，照舊只搬圖示
        String[] word = LineTranslator.keepAccentsWhole(
                new String[] {"可透過 {#}物品升", "級師 將物品提升"},
                List.of(new com.wynnchayuan.capture.LineParts.Piece("物品升級師", RED)));
        check("圖示前面是字時只搬圖示（實際 " + String.join(" ⏎ ", word) + "）",
              word[0].equals("可透過 ") && word[1].startsWith("{#}物品升級師"));
    }

    /** 包含 {@code needle} 的第一個片段的顏色；找不到是 -1。 */
    private static int colourOf(List<Component> rows, String needle) {
        for (Component row : rows) {
            for (StyledTextPart p : StyledText.fromComponent(row)) {
                if (p.getString(null, StyleType.NONE).contains(needle)) {
                    TextColor col = p.getPartStyle().getStyle().getColor();
                    return col == null ? 0 : col.getValue();
                }
            }
        }
        return -1;
    }

    /** 貼近實機字型：拉丁 6、中日韓 9、空白 4、圖示 9。 */
    private static int width(Component c) {
        int px = 0;
        String s = c.getString();
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == ' ') {
                px += 4;
            } else if (com.wynnchayuan.capture.GlyphSplitter.isGlyphCodePoint(cp)) {
                px += 3;
            } else if (cp >= 0x2E80) {
                px += 9;
            } else {
                px += 6;
            }
        }
        return px;
    }

    private static void dump(List<Component> rows) {
        for (int r = 0; r < rows.size(); r++) {
            StringBuilder sb = new StringBuilder("    [" + r + "] ");
            for (StyledTextPart p : StyledText.fromComponent(rows.get(r))) {
                String raw = p.getString(null, StyleType.NONE);
                TextColor col = p.getPartStyle().getStyle().getColor();
                sb.append('«').append(raw).append('»')
                  .append(col == null ? "" : String.format("#%06X", col.getValue()));
            }
            System.out.println(sb);
        }
    }

    private static MutableComponent part(Style style, String text) {
        return Component.literal(text).withStyle(style);
    }

    private static Component line(Style style, String text) {
        return part(style, text);
    }

    private static Component join(MutableComponent... parts) {
        MutableComponent out = Component.empty();
        for (MutableComponent p : parts) {
            out.append(p);
        }
        return out;
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
