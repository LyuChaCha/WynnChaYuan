package com.wynnchayuan.translate;

import com.wynnchayuan.capture.LineParts;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.Bootstrap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.wynnchayuan.translate.ColumnAligner.Piece;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class ColumnCentreTest {

    public static void main(String[] args) throws IOException {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        groups();
        spacing();
        glyphPrefixedAccent();
        wrapKeepsAccentsWhole();
        huggingPunctuation();
        sectionCodes();
        gluedGaps();
        rightEdgeGuard();
        overlayPill();

        System.out.println(failures == 0
                ? "ColumnCentreTest 全部通過"
                : "ColumnCentreTest 失敗 " + failures + " 項");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void gluedGaps() {
        Style s = Style.EMPTY;
        java.util.function.BiPredicate<Style, String> isGap =
                (style, text) -> SpaceOffset.isOffsetRun(text);
        String gap = SpaceOffset.encode(60);

        List<SpaceRuns.Run> glued = List.of(
                new SpaceRuns.Run(false, 0, s, " Class Type" + gap),
                new SpaceRuns.Run(false, 0, s, "Mage/Dark Wizard"));
        List<SpaceRuns.Run> split = SpaceRuns.splitGaps(glued, isGap);
        check("黏在一起的間隔拆得出來（實際 " + split.size() + " 段）",
              split.size() == 3);
        check("拆出來的中間那段是間隔", split.size() == 3 && split.get(1).space());
        check("標籤本身沒有被動到",
              split.size() == 3 && " Class Type".equals(split.get(0).text()));
        check("間隔的寬度解得對（實際 "
                      + (split.size() == 3 ? split.get(1).px() : -1) + "）",
              split.size() == 3 && split.get(1).px() == 60);

        List<SpaceRuns.Run> already = List.of(
                new SpaceRuns.Run(false, 0, s, "Class Type"),
                new SpaceRuns.Run(true, 60, s, gap));
        check("已經獨立的間隔不重複拆",
              SpaceRuns.splitGaps(already, isGap).size() == 2);

        check("判準否決時不拆",
              SpaceRuns.splitGaps(glued, (style, text) -> false).size() == 2);

        List<SpaceRuns.Run> pure = List.of(
                new SpaceRuns.Run(false, 0, s, gap));
        check("整段都是偏移時不拆",
              SpaceRuns.splitGaps(pure, isGap).size() == 1);

        String nudge = SpaceOffset.encode(3);
        List<SpaceRuns.Run> star = List.of(
                new SpaceRuns.Run(false, 0, s, "+16 Market Slots "),
                new SpaceRuns.Run(false, 0, s, gap + "★" + nudge),
                new SpaceRuns.Run(false, 0, s, "Super Priority Queue"));
        List<SpaceRuns.Run> starSplit = SpaceRuns.splitGaps(star, isGap);
        check("開頭的間隔拆得出來（實際 " + starSplit.size() + " 段）",
              starSplit.size() == 5);
        check("★ 前面那段是 60px 的間隔",
              starSplit.size() == 5 && starSplit.get(1).space()
                      && starSplit.get(1).px() == 60);
        check("★ 本身還是文字",
              starSplit.size() == 5 && "★".equals(starSplit.get(2).text()));
        check("★ 後面的微調也獨立出來，但不算欄界",
              starSplit.size() == 5 && starSplit.get(3).space()
                      && !SpaceRuns.isColumnGap(starSplit.get(3)));

        List<SpaceRuns.Run> inner = List.of(
                new SpaceRuns.Run(false, 0, s, "+10 Character Slots " + gap + "Beta Access"));
        List<SpaceRuns.Run> innerSplit = SpaceRuns.splitGaps(inner, isGap);
        check("夾在中間的間隔拆得出來（實際 " + innerSplit.size() + " 段）",
              innerSplit.size() == 3 && innerSplit.get(1).space()
                      && "+10 Character Slots ".equals(innerSplit.get(0).text())
                      && "Beta Access".equals(innerSplit.get(2).text()));
    }

    private static void groups() {
        Style s = Style.EMPTY;

        List<Piece> row = List.of(
                Piece.space(2, s),
                Piece.text("83 points", s),
                Piece.space(15, s), Piece.space(30, s), Piece.space(15, s),
                Piece.text("84 points", s));
        List<int[]> g = ColumnAligner.textGroups(row);
        check("連續的偏移算同一段間隔，兩欄就是兩組", g.size() == 2);
        check("第一欄落在文字上", g.get(0)[0] == 1 && g.get(0)[1] == 2);
        check("第二欄落在最後那段文字上", g.get(1)[0] == 5 && g.get(1)[1] == 6);

        List<Piece> single = List.of(
                Piece.space(10, s),
                Piece.text("* Modified by your gear (+23)", s));
        check("只有前導空白的置中行是一欄", ColumnAligner.textGroups(single).size() == 1);

        List<Piece> mixed = List.of(
                Piece.space(6, s),
                Piece.text("", s), Piece.text(" Earth", s),
                Piece.space(40, s),
                Piece.text("+12", s));
        check("同一欄裡的圖示與文字不會被拆成兩欄",
                ColumnAligner.textGroups(mixed).size() == 2);

        check("重排不會增減片段",
                ColumnAligner.recenterColumns(row).size() == row.size());
    }

    private static void spacing() {
        check("中文 + 空格 + 換成中文的譯名 → 空格要吃掉",
                TermSubstitution.dropsSpaceBefore("提高 ", "重擊"));
        check("英文後面的空格要留著 —— 拿掉會黏成一團",
                !TermSubstitution.dropsSpaceBefore("gains ", "重擊"));
        check("譯名還是英文時空格照留",
                !TermSubstitution.dropsSpaceBefore("提高 ", "Bash"));
        check("沒有空格就沒事", !TermSubstitution.dropsSpaceBefore("提高", "重擊"));

        check("中文譯名後面接空格再接中文 → 吃掉",
                TermSubstitution.dropsSpaceAfter("重擊", "重擊 的作用範圍", 2));
        check("後面接的是英文就留著",
                !TermSubstitution.dropsSpaceAfter("重擊", "重擊 gains", 2));
        check("行尾沒有東西可以黏，不動",
                !TermSubstitution.dropsSpaceAfter("重擊", "重擊 ", 2));

        check("德文不吃空格", !TermSubstitution.dropsSpaceBefore("mehr ", "Schaden"));
        check("西班牙文不吃空格", !TermSubstitution.dropsSpaceBefore("del ", "hechizo"));
        check("俄文不吃空格", !TermSubstitution.dropsSpaceBefore("урон ", "заклинаний"));
        check("諺文不吃空格 —— 韓文是靠空格分詞的",
                !TermSubstitution.dropsSpaceBefore("마법 ", "피해"));
        check("諺文後面的空格也要留著",
                !TermSubstitution.dropsSpaceAfter("피해", "피해 증가", 2));
    }

    private static void glyphPrefixedAccent() throws IOException {
        Path dir = Files.createTempDirectory("wynnchayuan-accent");
        write(dir, "ui-labels.json", """
            {
             "Earth": "地屬性",
             "Main Attack": "普攻"
            }""");
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        String segment = " Earth";
        check("直接查帶圖示的色段會落空（這就是原本的狀況）",
                store.lookup(segment) == null);
        check("剝掉前後的圖示與空白之後查得到",
                "地屬性".equals(TermSubstitution.lookupWordCore(segment, store)));
        check("圖示在後面也認得",
                "普攻".equals(TermSubstitution.lookupWordCore("Main Attack ", store)));
        check("沒有東西可剝就回傳 null，交給前面的查法",
                TermSubstitution.lookupWordCore("Earth", store) == null);
        check("剝完查不到就是查不到",
                TermSubstitution.lookupWordCore(" Nonsense", store) == null);
    }

    private static void wrapKeepsAccentsWhole() {
        Style green = Style.EMPTY;
        List<LineParts.Piece> accents = List.of(new LineParts.Piece("地屬性", green));

        String[] split = {"並提高你能造成的 {#} 地", "屬性傷害"};
        String[] fixed = TemplateRenderer.keepAccentsWhole(split, accents);
        check("被切開的重點詞會整個搬到下一行，圖示跟著走（實際 "
                        + fixed[0] + " ⏎ " + fixed[1] + "）",
                fixed[0].equals("並提高你能造成的 ") && fixed[1].equals("{#} 地屬性傷害"));

        String[] whole = {"並提高你能造成的 {#} 地屬性", "傷害"};
        check("沒被切到就原樣不動",
                TemplateRenderer.keepAccentsWhole(whole, accents)[0].endsWith("地屬性"));

        String[] withToken = {"造成的 {#}", "地屬性傷害"};
        List<LineParts.Piece> odd = List.of(new LineParts.Piece("{#} 地", green));
        check("含佔位符的片段不搬",
                TemplateRenderer.keepAccentsWhole(withToken, odd)[0].endsWith("{#}"));

        check("只有一行時什麼都不做",
                TemplateRenderer.keepAccentsWhole(new String[] {"地屬性傷害"}, accents).length == 1);
    }

    private static void huggingPunctuation() {
        check("中括號緊貼數值，算黏著", TemplateRenderer.hugs('['));
        check("斜線緊貼數值，算黏著", TemplateRenderer.hugs('/'));
        check("減號緊貼數值，算黏著", TemplateRenderer.hugs('-'));
        check("空白不算 —— 那是詞距不是黏著", !TemplateRenderer.hugs(' '));
        check("方塊字不算", !TemplateRenderer.hugs('傷'));
        check("英文字不算", !TemplateRenderer.hugs('A'));
        check("數字不算", !TemplateRenderer.hugs('7'));
        check("左圓括號不算 —— 那是註解的界線", !TemplateRenderer.hugs('('));
        check("右圓括號不算", !TemplateRenderer.hugs(')'));

        Style next = Style.EMPTY;
        check("整段都是標點時跟後面那個佔位符走",
                TemplateRenderer.leadTakesNext("/", 1, next));
        check("後面沒有佔位符就留給前面",
                !TemplateRenderer.leadTakesNext("/", 1, null));
        check("前面還有文字時開頭那截仍歸前一段",
                !TemplateRenderer.leadTakesNext(" 抄寫 [", 1, next));

        unitSuffix();
    }

    private static void unitSuffix() {
        int n = 8;
        check("s 緊貼數值時算單位",
              TemplateRenderer.unitLength("s [37.8%]", true, 0, n) == 1);
        check("stx 也算（綠寶石的堆疊單位）",
              TemplateRenderer.unitLength("stx", true, 0, 3) == 3);
        check("整段就是單位時也認得",
              TemplateRenderer.unitLength("s", true, 0, 1) == 1);

        check("前一個不是數值就不算",
              TemplateRenderer.unitLength("s [37.8%]", false, 0, n) == 0);
        check("中間隔著標點就不算緊貼",
              TemplateRenderer.unitLength("s [37.8%]", true, 1, n) == 0);
        check("後面還接著字母的是單字不是單位",
              TemplateRenderer.unitLength("seconds", true, 0, 7) == 0);
        check("超過三個字母不算",
              TemplateRenderer.unitLength("abcd ", true, 0, 5) == 0);
        check("大寫不算", TemplateRenderer.unitLength("S [1]", true, 0, n) == 0);
        check("中文不算——「{~} 秒」這種寫法不受影響",
              TemplateRenderer.unitLength(" 秒", true, 0, 2) == 0);
        check("空白開頭不算",
              TemplateRenderer.unitLength(" x Doom Stone", true, 0, 13) == 0);
    }

    private static void sectionCodes() {
        Style base = Style.EMPTY.withColor(net.minecraft.network.chat.TextColor
                .fromRgb(0xAAAAAA));

        check("沒有 § 時原樣輸出",
                "純文字".equals(DisplayStyles.coloured("純文字", base).getString()));

        java.util.List<Component> parts =
                flatten(DisplayStyles.coloured("警告§c危險", base));
        int grey = 0;
        int red = 0;
        for (Component leaf : parts) {
            Integer colour = leaf.getStyle().getColor() == null
                    ? null : leaf.getStyle().getColor().getValue();
            if (colour != null && colour == 0xAAAAAA) {
                grey++;
            }
            if (colour != null && colour == 0xFF5555) {
                red++;
            }
        }
        check("§ 之前沿用這一段原本的樣式", grey >= 1);
        check("§c 之後變成紅色", red >= 1);
        check("格式碼本身不會被印出來",
                !DisplayStyles.coloured("警告§c危險", base)
                        .getString().contains("§"));

        java.util.List<Component> reset =
                flatten(DisplayStyles.coloured("§c紅§r回來", base));
        Integer last = reset.get(reset.size() - 1).getStyle().getColor() == null
                ? null : reset.get(reset.size() - 1).getStyle().getColor().getValue();
        check("§r 回到這一段原本的樣式（拿到 "
                        + (last == null ? "null" : String.format("#%06X", last)) + "）",
                last != null && last == 0xAAAAAA);

        check("§ 後面不是格式碼時原樣留著",
                DisplayStyles.coloured("100§z", base).getString().contains("§z"));
    }

    private static java.util.List<Component> flatten(Component c) {
        java.util.List<Component> out = new java.util.ArrayList<>();
        c.visit((style, text) -> {
            if (!text.isEmpty()) {
                out.add(Component.literal(text).withStyle(style));
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static void write(Path dir, String name, String body) throws IOException {
        Files.write(dir.resolve(name), body.getBytes(StandardCharsets.UTF_8));
    }

    private static void rightEdgeGuard() {
        Style s = Style.EMPTY;
        SpaceRuns.Run space =
                new SpaceRuns.Run(true, 20, s, SpaceOffset.encode(20));

        List<SpaceRuns.Run> stat = List.of(
                new SpaceRuns.Run(false, 0, s, "–"),
                space,
                new SpaceRuns.Run(false, 0, s, "✤"),
                space,
                new SpaceRuns.Run(false, 0, s, "力量: -70"));
        check("圖示後面那個間隔不算標籤欄（不補右緣）",
              !SpaceRuns.labelledRun(stat, 1));
        check("破折號後面那個也不算", !SpaceRuns.labelledRun(stat, 0));

        List<SpaceRuns.Run> req = List.of(
                new SpaceRuns.Run(false, 0, s, "✔"),
                new SpaceRuns.Run(false, 0, s, " 職業類型"),
                space,
                new SpaceRuns.Run(false, 0, s, "法師/闇導士"));
        check("標籤後面那個間隔要補右緣", SpaceRuns.labelledRun(req, 0));

        List<SpaceRuns.Run> zh = List.of(
                new SpaceRuns.Run(false, 0, s, "生命回復"),
                space,
                new SpaceRuns.Run(false, 0, s, "+13%"));
        check("中文標籤也認得出來", SpaceRuns.labelledRun(zh, 0));

        List<SpaceRuns.Run> num = List.of(
                new SpaceRuns.Run(false, 0, s, "125"),
                space,
                new SpaceRuns.Run(false, 0, s, "126"));
        check("數字也算", SpaceRuns.labelledRun(num, 0));
    }
    private static void overlayPill() {
        Style s = Style.EMPTY;
        String pill = String.valueOf(new char[] {0xE060, 0xE03B, 0xE062});
        String level = String.valueOf(new char[] {0xE00B, 0xE015, 0xE029});

        List<SpaceRuns.Run> plate = List.of(
                new SpaceRuns.Run(false, 0, s, "Kandon-Beda 新兵 "),
                new SpaceRuns.Run(false, 0, s, pill),
                new SpaceRuns.Run(true, -30, s, SpaceOffset.encode(-30)),
                new SpaceRuns.Run(false, 0, s, level),
                new SpaceRuns.Run(true, 2, s, SpaceOffset.encode(2)));
        check("膠囊後面只有圖示，不算實字",
              !SpaceRuns.textAfterGap(plate, 0));
        check("往回拉去疊圖示的負偏移不能當欄位交界",
              SpaceRuns.overlayGap(plate, 0, -30));

        List<SpaceRuns.Run> column = List.of(
                new SpaceRuns.Run(false, 0, s, " 職業類型"),
                new SpaceRuns.Run(true, 76, s, SpaceOffset.encode(76)),
                new SpaceRuns.Run(false, 0, s, "法師/闇導士"));
        check("欄位交界後面有實字", SpaceRuns.textAfterGap(column, 0));
        check("欄位交界不會被當成疊字",
              !SpaceRuns.overlayGap(column, 0, 76));

        List<SpaceRuns.Run> pulled = List.of(
                new SpaceRuns.Run(false, 0, s, "防禦"),
                new SpaceRuns.Run(true, -12, s, SpaceOffset.encode(-12)),
                new SpaceRuns.Run(false, 0, s, "+1 to +2"));
        check("負偏移後面有實字就不是疊字",
              !SpaceRuns.overlayGap(pulled, 0, -12));

        check("★ 對齊圖示的 2px 微調不是欄界",
              !SpaceRuns.isColumnGap(
                      new SpaceRuns.Run(true, 2, s, SpaceOffset.encode(2))));
        check("素材 tooltip 往回拉的 12px 是欄界",
              SpaceRuns.isColumnGap(
                      new SpaceRuns.Run(true, -12, s, SpaceOffset.encode(-12))));
        check("信標選單的 69px 是欄界",
              SpaceRuns.isColumnGap(
                      new SpaceRuns.Run(true, 76, s, SpaceOffset.encode(76))));
        check("實字不是欄界",
              !SpaceRuns.isColumnGap(new SpaceRuns.Run(false, 0, s, "法師")));

        check("只看緊接在後面那一段", !SpaceRuns.textAfterGap(plate, 1));
    }

}
