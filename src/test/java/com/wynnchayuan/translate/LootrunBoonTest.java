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
        serendipity(store);
        missionReward(store);
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
        int paren = colourOf(out, "(");
        check("括號註解不會拿到原文最後一行的深灰（實際 #" + Integer.toHexString(paren) + "）",
              paren != -1 && paren != 0x555555);
    }

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

        String[] word = LineTranslator.keepAccentsWhole(
                new String[] {"可透過 {#}物品升", "級師 將物品提升"},
                List.of(new com.wynnchayuan.capture.LineParts.Piece("物品升級師", RED)));
        check("圖示前面是字時只搬圖示（實際 " + String.join(" ⏎ ", word) + "）",
              word[0].equals("可透過 ") && word[1].startsWith("{#}物品升級師"));
    }

    private static void serendipity(TranslationStore store) {
        System.out.println("=== Serendipity（圖示後面的縫）===");
        String key = "For the rest of this Lootrun, gain +{~} {#}Dexterity (Max x{~})"
                + " everytime you open a Chest";
        String dst = store.lookup(key);
        check("語料收著這一句（實際 " + dst + "）", dst != null && dst.contains("{#}"));
        if (dst == null || !dst.contains("{#}")) {
            return;
        }
        String sprite = "\uE003";
        MutableComponent icon = Component.literal(sprite + SpaceOffset.encode(2))
                .setStyle(SpaceOffset.styleFor(Style.EMPTY));
        List<StyledText> run = List.of(
                st(line(GREY, "For the rest of this Lootrun,")),
                st(join(part(GREY, "gain "), part(WHITE, "+2"), part(GREY, " "),
                        icon, part(RED, "Dexterity"),
                        part(GREY, " (Max x15)"))),
                st(line(GREY, "everytime you open a Chest")));
        LineTranslator.measureForTest = LootrunBoonTest::width;
        List<Component> out;
        try {
            out = LineTranslator.translateBlock(run, store, new boolean[3]);
        } finally {
            LineTranslator.measureForTest = null;
        }
        check("整句查得到", out != null);
        if (out == null) {
            return;
        }
        dump(out);
        int at = dst.indexOf("{#}") + 3;
        int end = dst.indexOf(' ', at);
        String name = dst.substring(at, end < 0 ? dst.length() : end);
        String want = sprite + SpaceOffset.encode(2) + name;
        boolean tight = false;
        for (Component row : out) {
            tight |= row.getString().contains(want);
        }
        check("★ 圖示、原本那兩像素、屬性名「" + name + "」緊緊相連，縫沒有被撐開", tight);
    }

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

    private static void corpusBoons() {
        String[][] labels = {
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

    private static void missionReward(TranslationStore store) {
        System.out.println("=== 使命獎勵（白灰混排）===");
        String key = "Purple and Blue Beacons are\nobscured, but will always be"
                + "\nGreatly Empowered.";
        String dst = store.lookup(key);
        check("語料收著這一段（實際 " + dst + "）", dst != null);
        if (dst == null) {
            return;
        }
        List<StyledText> run = List.of(
                st(join(part(WHITE, "Purple and Blue Beacons "), part(GREY, "are"))),
                st(line(GREY, "obscured, but will always be")),
                st(line(WHITE, "Greatly Empowered.")));
        List<Component> out = block(run, store);
        check("整段查得到", out != null);
        if (out == null) {
            return;
        }
        dump(out);
        StringBuilder shown = new StringBuilder();
        List<Integer> painted = new ArrayList<>();
        for (Component row : out) {
            for (StyledTextPart p : StyledText.fromComponent(row)) {
                String raw = p.getString(null, StyleType.NONE);
                TextColor col = p.getPartStyle().getStyle().getColor();
                for (int i = 0; i < raw.length(); i++) {
                    shown.append(raw.charAt(i));
                    painted.add(col == null ? -1 : col.getValue());
                }
            }
        }
        StringBuilder want = new StringBuilder();
        List<Integer> expect = new ArrayList<>();
        int slot = 0;
        for (int i = 0; i < dst.length(); i++) {
            if (dst.charAt(i) == '{') {
                int close = dst.indexOf('}', i);
                if (close < 0) {
                    break;
                }
                String body = dst.substring(i + 1, close);
                slot = body.equals("c1") ? 1 : body.equals("c2") ? 2 : 0;
                i = close;
                continue;
            }
            want.append(dst.charAt(i));
            expect.add(slot == 1 ? 0xFFFFFF : slot == 2 ? 0xAAAAAA : -1);
        }
        check("字沒有變少（實際 " + shown + "，語料 " + want + "）",
              shown.toString().equals(want.toString()));
        if (!shown.toString().equals(want.toString())) {
            return;
        }
        check("語料這一條有標 {c1} 與 {c2}（少了底色投票會把敘述染白）",
              dst.contains("{c1}") && dst.contains("{c2}"));
        java.util.Set<Integer> seen = new java.util.LinkedHashSet<>(painted);
        check("★ 畫出來不只一個顏色（實際 " + seen.size() + " 種："
                        + seen.stream().map(c -> String.format("#%06X", c)).toList() + "）",
              seen.size() >= 2);
        for (int i = 0; i < expect.size(); i++) {
            if (expect.get(i) < 0) {
                continue;
            }
            if (painted.get(i).equals(expect.get(i))) {
                continue;
            }
            check("★「" + want.charAt(i) + "」（第 " + i + " 個字）該是 #"
                            + String.format("%06X", expect.get(i)) + "（實際 #"
                            + String.format("%06X", painted.get(i)) + "）", false);
            return;
        }
        check("★ 白的信標名與「大幅強化」、灰的敘述都貼對了（" + expect.size() + " 個字）",
              true);
    }

    private static List<Component> block(List<StyledText> run, TranslationStore store) {
        LineTranslator.measureForTest = LootrunBoonTest::mc;
        try {
            return LineTranslator.translateBlock(run, store, new boolean[run.size()]);
        } finally {
            LineTranslator.measureForTest = null;
        }
    }

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
