package com.wynnchayuan.render;

import com.wynnchayuan.capture.LineParts;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public final class DialogueColourTest {

    private static int failures = 0;

    private static final TextColor BODY = TextColor.fromRgb(0xe0d4c0);

    private static final TextColor ITEM = TextColor.fromRgb(0x55ffff);

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        dominant();
        leadingAccent();
        acrossRows();
        onlyColour();
        symbols();
        splitName();
        growing();
        noRetreat();
        twice();
        typing();
        runaway();
        remembered();
        bundled();
        keepsWhole();

        System.out.println(failures == 0 ? "\n對話顏色：全部通過"
                : "\n對話顏色：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void dominant() {
        Row row = new Row()
                .add("Do you know about the ", BODY)
                .add("[Abysso Galoshes]", ITEM)
                .add("?", BODY);
        check("底色取字數最多的", BODY, row.tone());
    }

    private static void leadingAccent() {
        Row row = new Row()
                .add("[Abysso Galoshes]", ITEM)
                .add(" are what I have been looking for all this time.", BODY);
        check("開頭是強調詞時底色仍取散文", BODY, row.tone());

        List<LineParts.Piece> out = row.paint("[Abysso Galoshes] 正是我找了這麼久的東西。");
        check("強調詞在開頭也貼得到", ITEM, colourAt(out, "[Abysso Galoshes]"));
        check("後面的散文是底色", BODY, colourAt(out, " 正是我找了這麼久的東西。"));
    }

    private static void acrossRows() {
        Row row = new Row()
                .add("I wonder where those boots have gone... It's been", BODY)
                .add("such a long time. Do you know about the ", BODY)
                .add("[Abysso Galoshes]", ITEM)
                .add("?", BODY);

        boolean[] used = new boolean[row.accents().size()];
        List<LineParts.Piece> first = row.paint(
                "不知道那雙靴子跑到哪去了……都過了這麼久。你聽說過 [Abysso Galoshes]", used);
        check("跨行也貼得到", ITEM, colourAt(first, "[Abysso Galoshes]"));

        List<LineParts.Piece> second = row.paint("嗎？", used);
        check("第二行沒有強調詞就只有一截", 1, second.size());
        check("第二行是底色", BODY, second.get(0).style().getColor());
    }

    private static void growing() {
        List<LineParts.Piece> tint = List.of(new LineParts.Piece(
                "[Abysso Galoshes]", Style.EMPTY.withColor(ITEM)));
        String full = "你聽說過 [Abysso Galoshes] 嗎？";
        int start = full.indexOf('[');
        int stop = full.indexOf(']') + 1;
        String white = null;
        String wrong = null;
        for (int n = start + 1; n <= stop; n++) {
            String typed = full.substring(0, n);
            List<LineParts.Piece> out = DialogueRewriter.paint(typed,
                    Style.EMPTY.withColor(BODY), tint, new boolean[tint.size()]);
            LineParts.Piece last = out.get(out.size() - 1);
            if (!ITEM.equals(last.style().getColor()) && white == null) {
                white = typed;
            }
            if (!full.substring(start, n).equals(last.text()) && wrong == null) {
                wrong = typed + " → 「" + last.text() + "」";
            }
        }
        check("打到名字的第一個字母就有顏色", null, white);
        check("上色的正好是名字打出來的那一截", null, wrong);

        List<LineParts.Piece> before = DialogueRewriter.paint("你聽說過 ",
                Style.EMPTY.withColor(BODY), tint, new boolean[tint.size()]);
        check("名字還沒開始就整句都是底色", 1, before.size());
        check("而且是底色", BODY, before.get(0).style().getColor());
    }

    private static void noRetreat() {
        String full = "多年前被一個海盜從我們這裡偷走了！你要是能把 [Abysso Galoshes] 帶來幫我們";
        String name = "[Abysso Galoshes]";
        int start = full.indexOf('[');
        int last = 0;
        String shrank = null;
        String gap = null;
        for (int n = start + 1; n <= start + name.length(); n++) {
            String typed = full.substring(0, n);
            List<LineParts.Piece> tint = new ArrayList<>();
            int lag = n - start - 6;
            if (lag >= 2) {
                tint.add(new LineParts.Piece(name.substring(0, lag),
                        Style.EMPTY.withColor(ITEM)));
            }
            tint.add(new LineParts.Piece(name, Style.EMPTY.withColor(ITEM)));

            int inked = 0;
            for (LineParts.Piece piece : DialogueRewriter.paint(typed,
                    Style.EMPTY.withColor(BODY), tint, new boolean[tint.size()])) {
                if (ITEM.equals(piece.style().getColor())) {
                    inked += piece.text().length();
                }
            }
            if (inked < last && shrank == null) {
                shrank = typed + "：這一幀 " + inked + " 個字，上一幀 " + last;
            }
            if (inked != n - start && gap == null) {
                gap = typed + "：上色 " + inked + " 個字，打出來的是 " + (n - start);
            }
            last = inked;
        }
        check("上色的長度不會縮回去", null, shrank);
        check("打到哪就上色到哪", null, gap);
    }

    private static void onlyColour() {
        Style base = Style.EMPTY.withColor(BODY);
        Row row = new Row()
                .add("Talk to ", BODY)
                .add("Ferndor", Style.EMPTY.withColor(ITEM).withBold(true));

        for (LineParts.Piece piece : row.paint("去找 Ferndor 談談", base)) {
            if (piece.style().isBold()) {
                fail("粗體被一起貼回去了：" + piece.text());
            }
            if (!piece.style().equals(base.withColor(piece.style().getColor()))) {
                fail("除了顏色以外還改了別的：" + piece.text());
            }
        }
        check("強調詞仍然上了色", ITEM,
                colourAt(row.paint("去找 Ferndor 談談", base), "Ferndor"));
    }

    private static void symbols() {
        Row row = new Row()
                .add("Bring me the ", BODY)
                .add("[", ITEM)
                .add("Mummy's Rag", ITEM)
                .add("]", ITEM);
        List<String> got = new ArrayList<>();
        for (LineParts.Piece piece : row.accents()) {
            got.add(piece.text());
        }
        check("同色的連在一起併成整個名字",
                List.of("[Mummy's Rag]", "Mummy's Rag"), got);
    }

    private static void splitName() {
        List<String> texts = new ArrayList<>(List.of(
                "A pirate stole them from us years ago! I could reward you if you could bring me the ",
                "[Abysso",
                "Galoshes]",
                " and help us!"));
        List<Style> styles = new ArrayList<>(List.of(
                Style.EMPTY.withColor(BODY), Style.EMPTY.withColor(ITEM),
                Style.EMPTY.withColor(ITEM), Style.EMPTY.withColor(BODY)));
        List<Integer> body = List.of(0, 2);
        List<Integer> ends = List.of(1, 3);

        TextColor tone = DialogueRewriter.tone(texts, styles, body, ends);
        List<LineParts.Piece> tint =
                DialogueRewriter.accents(texts, styles, body, ends, tone);
        check("斷行吃掉的空白補回來了", "[Abysso Galoshes]", tint.get(0).text());

        List<String> rows = List.of(
                "多年前被一個海盜從我們這裡偷走了！你要是能把 [",
                "Abysso Galoshes] 帶來幫我們，我可以給你報酬！");
        int[] starts = new int[rows.size()];
        List<LineParts.Piece> whole = DialogueRewriter.paint(
                DialogueRewriter.flatten(rows, starts),
                Style.EMPTY.withColor(tone), tint, new boolean[tint.size()]);

        List<LineParts.Piece> second = DialogueRewriter.cut(
                whole, starts[1], starts[1] + rows.get(1).length());
        check("名字的後半沒有自己變成一截", ITEM, colourAt(second, "Abysso Galoshes]"));
        check("後面的散文是底色", BODY, colourAt(second, " 帶來幫我們，我可以給你報酬！"));

        List<LineParts.Piece> first = DialogueRewriter.cut(
                whole, starts[0], starts[0] + rows.get(0).length());
        check("第一行的中括號跟著名字上色", ITEM,
                first.get(first.size() - 1).style().getColor());
        check("兩行加起來還是原本的字",
                rows.get(0) + rows.get(1), text(first) + text(second));
    }

    private static void twice() {
        Row row = new Row()
                .add("", BODY)
                .add("Ferndor", ITEM)
                .add(" told me to find ", BODY)
                .add("Ferndor", ITEM)
                .add(".", BODY);
        List<LineParts.Piece> out = row.paint("Ferndor 叫我去找 Ferndor。");
        int painted = 0;
        for (LineParts.Piece piece : out) {
            if ("Ferndor".equals(piece.text())
                    && ITEM.equals(piece.style().getColor())) {
                painted++;
            }
        }
        check("兩處都上了色", 2, painted);
    }

    private static void typing() {
        Row row = new Row()
                .add("Do you know about the ", BODY)
                .add("[Abysso", ITEM);

        List<LineParts.Piece> out = row.paint("你聽說過 [Abysso Galoshes] 嗎？");
        check("整組方括號一起上色", ITEM, colourAt(out, "[Abysso Galoshes]"));
        check("後面的散文還是底色", BODY, colourAt(out, " 嗎？"));
    }

    private static void runaway() {
        Row row = new Row()
                .add("Bring me ", BODY)
                .add("[Abysso", ITEM);

        List<LineParts.Piece> out = row.paint("把 [Abysso 拿來給我");
        check("找不到收尾就只貼比對到的那一截", ITEM, colourAt(out, "[Abysso"));
        check("後面不受影響", BODY, colourAt(out, " 拿來給我"));
    }

    private static void remembered() throws Exception {
        java.nio.file.Path save = Files.createTempDirectory("wcy-tint")
                .resolve("dialogue-colours.json");
        DialogueTint.forTest();
        DialogueTint.init(save, "zh_tw");

        String whole = "你聽說過 [Abysso Galoshes] 嗎？";
        DialogueTint.learn(whole, List.of(
                new LineParts.Piece("[Abysso Galoshes]",
                        Style.EMPTY.withColor(ITEM))));
        DialogueTint.flush();

        DialogueTint.forTest();
        DialogueTint.init(save, "zh_tw");

        check("重開之後還記得", ITEM,
                colourAt(DialogueTint.of(whole), "[Abysso Galoshes]"));
        check("打字中拿前綴也查得到", ITEM,
                colourAt(DialogueTint.of("你聽說過 [Aby"), "[Abysso Galoshes]"));
        check("沒看過的句子就是空的", 0, DialogueTint.of("從來沒講過這句").size());
    }

    private static void bundled() throws Exception {
        DialogueTint.forTest();
        DialogueTint.init(Files.createTempDirectory("tint-bundled")
                .resolve(DialogueTint.FILE), "zh_tw");
        List<LineParts.Piece> got = DialogueTint.of(
                "多年前被一個海盜從我們這裡偷走了！你要是能把 [Abysso Galoshes] 帶來幫我們，我可以給你報酬！");
        check("jar 裡附的顏色讀得到", 1, got.size());
        check("附的是整個名字，不是半截", "[Abysso Galoshes]", got.get(0).text());
        check("附的那一截有顏色",
                TextColor.parseColor("dark_aqua").result().orElse(null),
                got.get(0).style().getColor());
    }

    private static void keepsWhole() throws Exception {
        DialogueTint.forTest();
        DialogueTint.init(Files.createTempDirectory("tint-whole")
                .resolve(DialogueTint.FILE), "zh_tw");
        String full = "多年前被一個海盜從我們這裡偷走了！你要是能把 "
                + "[Abysso Galoshes] 帶來幫我們，我可以給你報酬！";
        for (int n = 4; n < full.length(); n += 7) {
            DialogueTint.learn(full.substring(0, n), List.of(
                    new LineParts.Piece("半截", Style.EMPTY.withColor(BODY))));
        }
        List<LineParts.Piece> got = DialogueTint.of(full.substring(0, 24));
        check("打字途中還查得到整句那一筆", "[Abysso Galoshes]",
                got.isEmpty() ? "（被蓋掉了）" : got.get(0).text());
        check("整句本身也還在", "[Abysso Galoshes]",
                DialogueTint.of(full).isEmpty()
                        ? "（被蓋掉了）" : DialogueTint.of(full).get(0).text());
        check("開頭兩三個字不做前綴比對", 0, DialogueTint.of("多年").size());
    }

    private static final class Row {
        private final List<String> texts = new ArrayList<>();
        private final List<Style> styles = new ArrayList<>();

        Row add(String text, TextColor colour) {
            return add(text, Style.EMPTY.withColor(colour));
        }

        Row add(String text, Style style) {
            texts.add(text);
            styles.add(style);
            return this;
        }

        private List<Integer> from() {
            return List.of(0);
        }

        private List<Integer> to() {
            return List.of(texts.size() - 1);
        }

        TextColor tone() {
            return DialogueRewriter.tone(texts, styles, from(), to());
        }

        List<LineParts.Piece> accents() {
            return DialogueRewriter.accents(texts, styles, from(), to(), tone());
        }

        List<LineParts.Piece> paint(String translated) {
            List<LineParts.Piece> tint = accents();
            return paint(translated, new boolean[tint.size()]);
        }

        List<LineParts.Piece> paint(String translated, boolean[] used) {
            return DialogueRewriter.paint(translated,
                    Style.EMPTY.withColor(tone()), accents(), used);
        }

        List<LineParts.Piece> paint(String translated, Style base) {
            List<LineParts.Piece> tint = accents();
            return DialogueRewriter.paint(translated, base, tint,
                    new boolean[tint.size()]);
        }
    }

    private static String text(List<LineParts.Piece> pieces) {
        StringBuilder out = new StringBuilder();
        for (LineParts.Piece piece : pieces) {
            out.append(piece.text());
        }
        return out.toString();
    }

    private static TextColor colourAt(List<LineParts.Piece> pieces, String text) {
        for (LineParts.Piece piece : pieces) {
            if (piece.text().equals(text)) {
                return piece.style().getColor();
            }
        }
        fail("譯文裡沒有這一截：" + text + "（收到 " + pieces + "）");
        return null;
    }

    private static void check(String what, Object want, Object got) {
        if (want == null ? got == null : want.equals(got)) {
            System.out.println("  通過  " + what);
            return;
        }
        fail(what + "：預期 " + want + "，實際 " + got);
    }

    private static void fail(String why) {
        System.out.println("  失敗  " + why);
        failures++;
    }
}
