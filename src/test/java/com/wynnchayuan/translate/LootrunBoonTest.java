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
    private static final Style WHITE = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF));
    private static final String CORPUS = "src/main/resources/assets/wynnchayuan/translations";
    private static final Style SPRITE = Style.EMPTY.withFont(
            new net.minecraft.network.chat.FontDescription.Resource(
                    net.minecraft.resources.Identifier.withDefaultNamespace(
                            "tooltip/attribute/sprite")));
    private static final String ICON = "󯿿󰀁󰀂";

    public static void main(String[] args) {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of(CORPUS, Languages.DEFAULT));

        persnickety(store);
        statRows(store);
        accentMove();
        heavensent(store);
        rewrapColour(store);
        heavensentElemental(store);
        corpusBoons();

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
        // 這一行故意改成遊戲裡沒有的顏色組合。
        //
        // 原本用的是實機的「Blue or Purple Beacon more」，但語料後來補上了 Persnickety 的整段譯文，
        // 整段一翻就收成較少行，這個測試要驗的「整段沒翻時一個字都不翻」就測不到了。
        // 換成語料永遠不會有的句子，測的才一直是「查不到整段」這條路。
        tip.add(line(GREY, "Grey or Silver Beacon more"));
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
        // 期望的字從語料取，不寫死措辭：翻譯團隊改譯法時這條不該跟著紅。
        String dst = store.lookup("For the rest of your Lootrun,\ngain +{~} {#}Defence (Max x{~}) for"
                + "\neach Beacon offered");
        check("語料收著這一段", dst != null);
        if (dst == null) {
            return;
        }
        String want = dst.replaceAll("\\{~1\\}|\\{~\\}(?=[^~]*x\\{)", "2")
                         .replaceAll("\\{~2\\}|\\{~\\}", "15")
                         .replace("{#}", ICON);
        String all = String.join("", out.stream().map(Component::getString).toList());
        check("字沒有變少（實際 " + all + "，語料 " + want + "）",
              all.replace(" ", "").equals(want.replace("\n", "").replace(" ", "")));
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
        // 找括號本身，不找「上限／最多」——那是措辭，翻譯團隊會改
        int paren = colourOf(out, "(");
        check("括號註解不會拿到原文最後一行的深灰（實際 #" + Integer.toHexString(paren) + "）",
              paren != -1 && paren != 0x555555);
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

    /**
     * 玩家回報的那一塊：Heavensent 的元素傷害版。實機畫成
     *
     * <pre>
     *   本次 Lootrun 剩餘期間，
     *   每提供一個信標就 +4%
     *   元素傷害 (最多 x15)
     * </pre>
     *
     * <p>「+4%」跟「元素傷害」分家，「(最多 x15)」也跟著離開了它的數值。
     * 寬度用 {@link #mc}——拿它量，舊的折法斷出來的正是實機那三行。
     *
     * <p>期望的字從語料取，不寫死措辭：譯法會改（「每提供一個信標就」後來改成
     * 「每出現一個信標，獲得」），這條要盯的是折法，不是譯法。
     */
    private static void heavensentElemental(TranslationStore store) {
        System.out.println("=== Heavensent（元素傷害）===");
        String key = "For the rest of your Lootrun,\ngain +{~} Elemental Damage"
                + "\n(Max x{~}) for each Beacon\noffered";
        String dst = store.lookup(key);
        check("語料收著這一段（實際 " + dst + "）", dst != null);
        if (dst == null) {
            return;
        }
        List<StyledText> run = List.of(
                st(line(GREY, "For the rest of your Lootrun,")),
                st(join(part(GREY, "gain "), part(WHITE, "+4%"), part(GREY, " Elemental Damage"))),
                st(line(GREY, "(Max x15) for each Beacon")),
                st(line(GREY, "offered")));
        List<Component> out = block(run, store);
        checkBoon("Heavensent 元素傷害", run, out, "+4%", "元素傷害", true);
        if (out == null) {
            return;
        }
        check("★ 「(最多 x15)」跟它的數值在同一行",
              rowOf(out, "(最多 x15)") >= 0 && rowOf(out, "(最多 x15)") == rowOf(out, "+4%"));
        check("第一行收在逗號上（實際 " + out.get(0).getString() + "）",
              out.get(0).getString().strip().endsWith("，"));
        check("數值用原文數值的顏色", colourOf(out, "4%") == 0xFFFFFF);
        check("屬性名用正文的顏色", colourOf(out, "元素傷害") == 0xAAAAAA);
        String want = dst.replaceAll("\\{~1\\}|\\{~\\}(?=[^~]*x\\{)", "4%")
                         .replaceAll("\\{~2\\}|\\{~\\}", "15");
        String all = String.join("", out.stream().map(Component::getString).toList());
        check("字沒有變少（實際 " + all + "，語料 " + want + "）",
              all.replace(" ", "").equals(want.replace("\n", "").replace(" ", "")));
    }

    /**
     * 語料裡現有的幾個賜福，四個語言各跑一次。
     *
     * <p>中文是一句話、要我們自己折回原文的行數；日文與俄文是譯者自己排好行的，
     * 不會重折——一起跑是為了確定新規則沒有去動它們。
     */
    private static void corpusBoons() {
        String[][] labels = {
            // 語言, Damage, Spell Damage, Defence, Elemental（藍紫信標次數那條）
            {"zh_tw", "傷害", "法術傷害", "防禦", "元素傷害"},
            {"zh_cn", "伤害", "法术伤害", "防御", "元素伤害"},
            {"ja_jp", "ダメージ", "呪文ダメージ", "防御", "属性ダメージ"},
            {"ru_ru", "урону", "урону заклинаний", "Защите", "стихийному урону"},
        };
        for (String[] lang : labels) {
            System.out.println("=== 語料裡的賜福：" + lang[0] + " ===");
            TranslationStore store = new TranslationStore();
            store.loadAll(Path.of(CORPUS, lang[0]));
            boolean ours = lang[0].startsWith("zh");
            List<StyledText> damage = List.of(
                    st(line(GREY, "For the rest of your Lootrun,")),
                    st(join(part(GREY, "gain "), part(WHITE, "+3%"),
                            part(GREY, " Damage (Max x15) for"))),
                    st(line(GREY, "each Beacon offered")));
            checkBoon(lang[0] + " Damage", damage, block(damage, store), "+3%", lang[1], ours);
            List<StyledText> spell = List.of(
                    st(line(GREY, "For the rest of your Lootrun,")),
                    st(join(part(GREY, "gain "), part(WHITE, "+4%"),
                            part(GREY, " Spell Damage (Max"))),
                    st(line(GREY, "x15) for each Beacon offered")));
            checkBoon(lang[0] + " Spell Damage", spell, block(spell, store), "+4%", lang[2], ours);
            List<StyledText> defence = List.of(
                    st(line(GREY, "For the rest of your Lootrun,")),
                    st(join(part(GREY, "gain "), part(AQUA, "+2"), part(GREY, " "),
                            part(SPRITE, ICON), part(RED, "Defence"),
                            part(GREY, " (Max x15) for"))),
                    st(line(GREY, "each Beacon offered")));
            checkBoon(lang[0] + " Defence", defence, block(defence, store), "+2", lang[3], ours);
            List<StyledText> once = List.of(
                    st(line(GREY, "Once you have been offered a")),
                    st(line(GREY, "Blue or Purple Beacon more")),
                    st(join(part(GREY, "than "), part(AQUA, "20"),
                            part(GREY, " times this Lootrun,"))),
                    st(join(part(GREY, "gain "), part(AQUA, "+5%"),
                            part(GREY, " Elemental Damage"))));
            checkBoon(lang[0] + " Elemental", once, block(once, store), "+5%", lang[4], ours);
            if (lang[0].equals("zh_tw")) {
                List<StyledText> health = List.of(
                        st(line(GREY, "For the rest of this Lootrun,")),
                        st(join(part(GREY, "gain "), part(WHITE, "+100"),
                                part(GREY, " Health (Max x15)"))),
                        st(join(part(GREY, "for every "), part(WHITE, "3"),
                                part(GREY, " items offered to"))),
                        st(line(GREY, "you from a Chest")));
                checkBoon("zh_tw Health", health, block(health, store), "+100", "生命", true);
            }
        }
    }

    /**
     * 一塊賜福折完之後都該成立的事。
     *
     * @param ours 這一段是不是我們自己折的（中文）。譯者自己排好行的不量寬度——
     *             那是譯者的決定，不是折行規則的。
     */
    private static void checkBoon(String name, List<StyledText> run, List<Component> out,
                                  String value, String label, boolean ours) {
        check(name + "：整段查得到", out != null);
        if (out == null) {
            return;
        }
        dump(out);
        check(name + "：行數沒有超過原文（" + out.size() + " / " + run.size() + "）",
              out.size() <= run.size());
        int valueRow = rowOf(out, value);
        int labelRow = rowOf(out, label);
        check("★ " + name + "：「" + value + "」跟「" + label + "」在同一行（第 " + valueRow
                        + " 行、第 " + labelRow + " 行）",
              valueRow >= 0 && valueRow == labelRow);
        int widest = 0;
        for (StyledText row : run) {
            widest = Math.max(widest, mc(row.getComponent()));
        }
        for (int r = 0; r < out.size(); r++) {
            String s = out.get(r).getString();
            String core = s.strip();
            check(name + "：第 " + r + " 行不是從收尾的標點開頭（" + s + "）",
                  core.isEmpty() || "，。、；：）)%".indexOf(core.charAt(0)) < 0);
            if (ours) {
                check(name + "：第 " + r + " 行沒有比原文最寬的一行寬（" + mc(out.get(r))
                                + " / " + widest + "）",
                      mc(out.get(r)) <= widest);
            }
        }
    }

    private static List<Component> block(List<StyledText> run, TranslationStore store) {
        LineTranslator.measureForTest = LootrunBoonTest::mc;
        try {
            return LineTranslator.translateBlock(run, store, new boolean[run.size()]);
        } finally {
            LineTranslator.measureForTest = null;
        }
    }

    /** 第一個包含 {@code needle} 的行；找不到是 -1。 */
    private static int rowOf(List<Component> rows, String needle) {
        for (int r = 0; r < rows.size(); r++) {
            if (rows.get(r).getString().contains(needle)) {
                return r;
            }
        }
        return -1;
    }

    private static StyledText st(Component c) {
        return StyledText.fromComponent(c);
    }

    /**
     * 照 Minecraft 預設字型逐字的前進寬度（含 1px 字距）量；中日韓 9、屬性圖示 3。
     *
     * <p>{@link #width} 把拉丁字母一律算 6，原文量得偏寬，實機那種「剛好放不下」的斷法
     * 就重現不出來。這一份把 i、l、t、括號這些窄字算對，舊的折法才會斷出實機的三行。
     */
    private static int mc(Component c) {
        int px = 0;
        String s = c.getString();
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (com.wynnchayuan.capture.GlyphSplitter.isGlyphCodePoint(cp)) {
                px += 3;
            } else if (cp >= 0x2E80) {
                px += 9;
            } else if (cp == 'l') {
                px += 3;
            } else if ("i!|,.:;'".indexOf(cp) >= 0) {
                px += 2;
            } else if (cp == ' ' || "tI[]".indexOf(cp) >= 0) {
                px += 4;
            } else if ("fk(){}<>*\"".indexOf(cp) >= 0) {
                px += 5;
            } else if (cp == '~' || cp == '@') {
                px += 7;
            } else {
                px += 6;
            }
        }
        return px;
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
