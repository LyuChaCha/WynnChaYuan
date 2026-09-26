package com.wynnchayuan.render;

import com.wynnchayuan.capture.LineParts;

import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 句中的強調色有沒有貼回譯文。
 *
 * <h2>為什麼要有</h2>
 * Wynncraft 靠顏色標物品名、NPC 名、地名——{@code [Abysso Galoshes]} 是青色，
 * 周圍的散文是米色。那幾個詞是<b>另外一段</b>送過來的，而就地取代會把一行
 * 併成一段（不併的話中文攤不回原本的行數）。併完只剩一個顏色，玩家看到的是
 * 整句同色的中文——名字混在句子裡認不出來，正是實機回報的毛病。
 *
 * <p>所以顏色是事後貼回去的（{@link DialogueRewriter#paint}）。這支測試釘住
 * 那個貼法：貼對位置、貼對顏色、<b>而且只貼顏色</b>。
 *
 * <h2>為什麼「只貼顏色」是條紅線</h2>
 * 這一行要補回去的尾隨偏移是<b>先算好的</b>——照整行譯文的寬度算。粗體會讓
 * 每個字寬 +1px，貼回去就對不上，框與頭像會被往右推。顏色不影響字寬，
 * 所以帶顏色是唯一不會動到版面的做法。
 */
public final class DialogueColourTest {

    private static int failures = 0;

    /** 散文的米色。 */
    private static final TextColor BODY = TextColor.fromRgb(0xe0d4c0);

    /** 物品名的青色。 */
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
        twice();
        typing();
        runaway();
        remembered();
        bundled();

        System.out.println(failures == 0 ? "\n對話顏色：全部通過"
                : "\n對話顏色：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ------------------------------------------------------------------

    /** 底色是照字數選的，不是取第一段。 */
    private static void dominant() {
        Row row = new Row()
                .add("Do you know about the ", BODY)
                .add("[Abysso Galoshes]", ITEM)
                .add("?", BODY);
        check("底色取字數最多的", BODY, row.tone());
    }

    /**
     * 開頭就是強調詞的行。
     *
     * <p>取第一段當底色的話，整行會被染成青色、散文反而變成例外——顏色接反。
     */
    private static void leadingAccent() {
        Row row = new Row()
                .add("[Abysso Galoshes]", ITEM)
                .add(" are what I have been looking for all this time.", BODY);
        check("開頭是強調詞時底色仍取散文", BODY, row.tone());

        List<LineParts.Piece> out = row.paint("[Abysso Galoshes] 正是我找了這麼久的東西。");
        check("強調詞在開頭也貼得到", ITEM, colourAt(out, "[Abysso Galoshes]"));
        check("後面的散文是底色", BODY, colourAt(out, " 正是我找了這麼久的東西。"));
    }

    /**
     * 原文在第二行的詞，譯文重排之後落到第一行。
     *
     * <p>中文比英文短，斷行位置本來就不會一樣。逐行收強調色的話，這個詞
     * 在第一行找不到對應，顏色就掉了——所以強調色是整段一起收的。
     */
    private static void acrossRows() {
        Row row = new Row()
                .add("I wonder where those boots have gone... It's been", BODY)
                .add("such a long time. Do you know about the ", BODY)
                .add("[Abysso Galoshes]", ITEM)
                .add("?", BODY);

        // 譯文短，那個詞被排到了第一行
        boolean[] used = new boolean[row.accents().size()];
        List<LineParts.Piece> first = row.paint(
                "不知道那雙靴子跑到哪去了……都過了這麼久。你聽說過 [Abysso Galoshes]", used);
        check("跨行也貼得到", ITEM, colourAt(first, "[Abysso Galoshes]"));

        List<LineParts.Piece> second = row.paint("嗎？", used);
        check("第二行沒有強調詞就只有一截", 1, second.size());
        check("第二行是底色", BODY, second.get(0).style().getColor());
    }

    /** 只換顏色：字型、粗體、底線都照底色那一段走。見類別說明。 */
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

    /**
     * 純符號不會自己成為一段。
     *
     * <p>一個 {@code [} 在譯文裡到處都對得上，單獨拿去貼只會把別的地方染色。
     * 但它跟後面同色的段是連著的，併起來就是完整的名字——併完的那個才是
     * 要貼的東西，拆開的幾段只是備胎。
     */
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

    /**
     * 名字剛好跨在原文的兩行之間——實機回報「Abysso 白、Galoshes] 青」。
     *
     * <h2>現場長什麼樣</h2>
     * 原文斷在名字中間，所以遊戲送的是兩段青色：{@code [Abysso} 在第一行結尾、
     * {@code Galoshes]} 在第二行開頭。中文斷在別的地方——第一行結尾只剩一個
     * {@code [}，名字整個落在第二行。
     *
     * <p>於是逐行貼的時候，第一行找不到 {@code [Abysso}（只有一個中括號），
     * 第二行找得到 {@code Galoshes]}，畫面上就是半白半青。兩件事要一起做：
     * 同色相鄰的段先併回一個名字，而且整段一起貼、不逐行貼。
     */
    private static void splitName() {
        List<String> texts = new ArrayList<>(List.of(
                "A pirate stole them from us years ago! I could reward you if you could bring me the ",
                "[Abysso",
                "Galoshes]",
                " and help us!"));
        List<Style> styles = new ArrayList<>(List.of(
                Style.EMPTY.withColor(BODY), Style.EMPTY.withColor(ITEM),
                Style.EMPTY.withColor(ITEM), Style.EMPTY.withColor(BODY)));
        List<Integer> body = List.of(0, 2);      // 兩行：各自的第一段
        List<Integer> ends = List.of(1, 3);      // 兩行：各自的最後一段

        TextColor tone = DialogueRewriter.tone(texts, styles, body, ends);
        List<LineParts.Piece> tint =
                DialogueRewriter.accents(texts, styles, body, ends, tone);
        check("斷行吃掉的空白補回來了", "[Abysso Galoshes]", tint.get(0).text());

        // 中文的斷行位置不一樣：第一行結尾只有一個中括號
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

    /** 同一個詞出現兩次就是兩處，不會第二段又貼回第一處。 */
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

    /**
     * 打字打到一半的物品名。
     *
     * <p>Wynncraft 的顏色跟著打字長出來：先送 {@code [Abysso} 的顏色，
     * 再送整組的。中文比英文短，整個名字早就在畫面上了——只照字面貼的話
     * 玩家會看到「{@code [Abysso} 有色、{@code Galoshes]} 沒色」。
     */
    private static void typing() {
        Row row = new Row()
                .add("Do you know about the ", BODY)
                .add("[Abysso", ITEM);          // 還在打，只送到這裡

        List<LineParts.Piece> out = row.paint("你聽說過 [Abysso Galoshes] 嗎？");
        check("整組方括號一起上色", ITEM, colourAt(out, "[Abysso Galoshes]"));
        check("後面的散文還是底色", BODY, colourAt(out, " 嗎？"));
    }

    /**
     * 少一個右括號時不准把整行吃掉。
     *
     * <p>沒有上限的話，一個沒收尾的 {@code [} 會讓後面整句都變成物品的顏色。
     */
    private static void runaway() {
        Row row = new Row()
                .add("Bring me ", BODY)
                .add("[Abysso", ITEM);

        List<LineParts.Piece> out = row.paint("把 [Abysso 拿來給我");
        check("找不到收尾就只貼比對到的那一截", ITEM, colourAt(out, "[Abysso"));
        check("後面不受影響", BODY, colourAt(out, " 拿來給我"));
    }

    /**
     * 記住的顏色要能撐過重開遊戲。
     *
     * <h2>為什麼一定要落地</h2>
     * 顏色是跟著打字長出來的，中文又比英文早幾百毫秒出現那個詞——第一次讀
     * 一定會白一下。只記在記憶體裡的話，每次重開遊戲又要重白一次，
     * 等於沒修。存成檔案之後，一句話這輩子只白那一次。
     *
     * <p>還要釘住「拿前綴查得到整句」：打字中手上只有譯文的前半段。
     */
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

        // 重開遊戲：整個清掉，只從檔案讀回來
        DialogueTint.forTest();
        DialogueTint.init(save, "zh_tw");

        check("重開之後還記得", ITEM,
                colourAt(DialogueTint.of(whole), "[Abysso Galoshes]"));
        check("打字中拿前綴也查得到", ITEM,
                colourAt(DialogueTint.of("你聽說過 [Aby"), "[Abysso Galoshes]"));
        check("沒看過的句子就是空的", 0, DialogueTint.of("從來沒講過這句").size());
    }

    /**
     * 隨 jar 附的那一份要真的讀得到。
     *
     * <p>這是產生物：路徑打錯、JSON 壞掉，模組都照常跑，只是每一句第一次讀
     * 又變回「先白再染」——完全沒有訊號。所以要自己驗。
     */
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

    // ------------------------------------------------------------------

    /** 一行對話：幾段文字，各自帶自己的樣式。 */
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

    /** 把幾截接回一整條字串：切開之後不能多一個字也不能少一個字。 */
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
