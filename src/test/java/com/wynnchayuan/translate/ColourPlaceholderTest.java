package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import com.wynnchayuan.capture.LineParts;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 譯文可以自己指定顏色：{@code {c1}}、{@code {c:#FF55FF}}、{@code {/}}。
 *
 * <h2>為什麼要有這個功能</h2>
 * 顏色本來是<b>猜</b>出來的：拿原文帶特殊樣式的片段，到譯文裡找同樣的字面。
 * 英文原樣留著的詞找得到，翻成中文的就找不到，那一段只好掉回底色。
 * 整行同色的還能靠位置補救，一行裡混了兩三種顏色的就沒辦法——
 * 實機那張「[Cave Completed] … - Rewards: … +1 Unidentified Helmet」
 * 每一行的顏色都不一樣，譯文出來卻是一片灰。
 *
 * <p>猜不到的時候，唯一知道答案的是譯者。所以讓他直接寫出來。
 *
 * <h2>這裡釘住什麼</h2>
 * <ol>
 *   <li>{@code {cN}} 真的把原文第 N 個顏色搬過去，粗體之類的裝飾一起搬</li>
 *   <li>同一行裡兩個佔位符各管各的，不會互相污染</li>
 *   <li>{@code {/}} 之後回到底色</li>
 *   <li>編號超出範圍時<b>當作沒寫</b>——顏色不對可以忍，整句變回英文不行</li>
 *   <li>佔位符本身<b>絕對不能</b>漏到畫面上</li>
 * </ol>
 */
public final class ColourPlaceholderTest {

    private static int failures = 0;

    private static final int GREEN = 0x55FF55;    // [Cave Completed]
    private static final int WHITE = 0xFFFFFF;    // 洞窟名稱
    private static final int PINK  = 0xFF55FF;    // - Rewards:
    private static final int GREY  = 0xAAAAAA;    // 內文
    private static final int GOLD  = 0xFFAA00;    // 譯者自己指定的顏色

    /**
     * 實機「洞穴完成」那一塊的骨架：一則訊息、四行、每一行各有自己的顏色。
     *
     * <p>顏色出現的順序就是 {@code {c1}} {@code {c2}} {@code {c3}} 的編號順序，
     * 見 {@code LineTranslator#palette}。
     */
    private static StyledText cave() {
        MutableComponent all = Component.empty();
        all.append(lit("[Cave Completed]", GREEN, false));    // {c1}
        all.append(lit("\n", GREY, false));
        all.append(lit("Grook's Nest", WHITE, true));         // {c2}
        all.append(lit("\n", GREY, false));
        all.append(lit("- ", PINK, false));                   // {c3}
        all.append(lit("Rewards:", GREY, false));             // {c4}
        all.append(lit("\n", GREY, false));
        all.append(lit("- ", PINK, false));
        all.append(lit("Sealed Helmet", GREY, false));
        return StyledText.fromComponent(all);
    }

    private static MutableComponent lit(String text, int colour, boolean bold) {
        return Component.literal(text).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(colour)).withBold(bold));
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-colour");
        FlowedDebug.init(dir);

        // 模板就是原文本身：這一塊沒有數值也沒有地名，所以鍵跟畫面上一樣。
        String src = "[Cave Completed]\nGrook's Nest\n- Rewards:\n- Sealed Helmet";
        String dst = "{c1}[洞穴完成]{/}\n{c2}Grook 巢穴{/}\n{c3}- {c4}獎勵：{/}\n"
                   + "{c3}- {c:#FFAA00}封印的頭盔{/}";
        write(dir.resolve("cave.json"), src, dst);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        check("暫用語料讀得進來", store.lookup(src) != null);

        Component hit = LineTranslator.translate(cave(), store);
        check("查得到譯文", hit != null);
        if (hit == null) {
            report();
            return;
        }
        String shown = hit.getString();
        System.out.println("      輸出：" + shown.replace("\n", " ⏎ "));

        // 1. 佔位符不能漏到畫面上。這是最要命的一種壞法：玩家會直接看到 {c1}。
        check("畫面上沒有 {c 佔位符（實際：" + shown.replace("\n", " ⏎ ") + "）",
              !shown.contains("{c") && !shown.contains("{/}"));

        // 2. 每一行各自拿到原文那一行的顏色
        Integer title = colourOf(hit, "洞穴完成");
        check("標題套上原文的綠色（拿到 " + hex(title) + "）",
              title != null && title == GREEN);

        Integer name = colourOf(hit, "巢穴");
        check("洞窟名稱套上原文的白色（拿到 " + hex(name) + "）",
              name != null && name == WHITE);
        check("洞窟名稱連粗體一起搬過來", Boolean.TRUE.equals(boldOf(hit, "巢穴")));

        // 3. 同一行裡兩個佔位符各管各的
        Integer dash = colourOf(hit, "-");
        check("行首的破折號是粉紅（拿到 " + hex(dash) + "）", dash != null && dash == PINK);
        Integer label = colourOf(hit, "獎勵");
        check("同一行的「獎勵」不被粉紅波及（拿到 " + hex(label) + "）",
              label != null && label == GREY);

        // 4. {c:#RRGGBB} 自己指定的顏色
        Integer own = colourOf(hit, "封印的頭盔");
        check("{c:#FFAA00} 指定的金色套得上（拿到 " + hex(own) + "）",
              own != null && own == GOLD);

        // 5. 編號超出範圍時當作沒寫，整句仍然翻得出來
        Path spare = Files.createTempDirectory("wynnchayuan-colour-bad");
        write(spare.resolve("cave.json"), src,
              "{c9}[洞穴完成]{/}\nGrook 巢穴\n- 獎勵：\n- 封印的頭盔");
        TranslationStore lenient = new TranslationStore();
        lenient.loadAll(spare);
        Component odd = LineTranslator.translate(cave(), lenient);
        check("編號超出範圍時整句仍然翻得出來", odd != null);
        check("編號超出範圍時佔位符也不會漏出去",
              odd == null || (!odd.getString().contains("{c9}")
                              && !odd.getString().contains("{/}")));

        crossesLines(src);
        wordPalette();
        progressBar();
        report();
    }

    /**
     * 色段可以跨行，直到 {@code {/}} 或下一個 {@code {cN}} 為止。
     *
     * <h2>先前壞在哪</h2>
     * 色段原本<b>每一行都會收掉</b>，理由是「忘了寫 {@code {/}} 只影響那一行」。
     * 但翻譯團隊第一次用就踩到：一句話被 tooltip 切成兩行，在第一行開色段、
     * 想一路染到第二行，結果第二行整個掉回底色，而寫在第二行的 {@code {/}}
     * 也變成空操作——warrior.json 有三條譯文都是這樣壞的。
     *
     * <p>跨行才是譯者預期的行為，也跟註解（{@code inNote}）的處理一致：
     * 註解同樣會被 tooltip 切成兩行，那個狀態本來就是跨行帶著走的。
     *
     * <h2>這裡釘住什麼</h2>
     * <ol>
     *   <li>第一行開的色段，會延伸到第二行</li>
     *   <li>寫在第二行的 {@code {/}} 真的會把它收掉，不是空操作</li>
     * </ol>
     */
    private static void crossesLines(String src) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-colour-cross");
        // 第 1 行開 {c1}（原文的綠色），一路染到第 2 行中間才用 {/} 收掉。
        write(dir.resolve("cave.json"), src,
              "{c1}[洞穴完成]\n延續的字{/}沒染色\n- 獎勵：\n- 封印的頭盔");
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        Component hit = LineTranslator.translate(cave(), store);
        check("跨行的色段：整句仍然翻得出來", hit != null);
        if (hit == null) {
            return;
        }
        check("跨行的色段：佔位符不會漏到畫面上",
              !hit.getString().contains("{c") && !hit.getString().contains("{/}"));

        Integer first = colourOf(hit, "洞穴完成");
        check("第 1 行拿到 {c1} 的綠色（拿到 " + hex(first) + "）",
              first != null && first == GREEN);

        Integer carried = colourOf(hit, "延續的字");
        check("色段延伸到第 2 行（拿到 " + hex(carried) + "，應為綠色）",
              carried != null && carried == GREEN);

        Integer closed = colourOf(hit, "沒染色");
        check("第 2 行的 {/} 真的收掉色段（拿到 " + hex(closed) + "，不該是綠色）",
              closed == null || closed != GREEN);
    }

    private static void write(Path file, String src, String dst) throws Exception {
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty(src, dst);
        Files.writeString(file, root.toString(), StandardCharsets.UTF_8);
    }

    private static String hex(Integer colour) {
        return colour == null ? "null" : String.format("#%06X", colour);
    }

    /**
     * 只看<b>葉節點</b>：上層節點的 getString() 含了所有子節點的字，
     * 拿它去比對一定第一個就中，量到的卻是空的根樣式。
     */
    private static Integer colourOf(Component line, String needle) {
        for (Component part : flatten(line)) {
            if (part.getSiblings().isEmpty() && part.getString().contains(needle)) {
                TextColor colour = part.getStyle().getColor();
                return colour == null ? null : colour.getValue();
            }
        }
        return null;
    }

    private static Boolean boldOf(Component line, String needle) {
        for (Component part : flatten(line)) {
            if (part.getSiblings().isEmpty() && part.getString().contains(needle)) {
                return part.getStyle().isBold();
            }
        }
        return null;
    }

    private static List<Component> flatten(Component root) {
        List<Component> out = new ArrayList<>();
        out.add(root);
        for (Component child : root.getSiblings()) {
            out.addAll(flatten(child));
        }
        return out;
    }

    /**
     * {@code {wN}} 只數<b>有字母</b>的片段，所以進度變了編號也不會挪位。
     *
     * <h2>為什麼需要它</h2>
     * 意象的層級進度長這樣，綠箭頭的數量隨進度變動：
     *
     * <pre>
     *   Tier I  &gt;&gt;&gt;&gt;&gt;  &gt;&gt;&gt;&gt;&gt;  Tier II  [2/4]
     *   灰      綠        暗灰     洋紅     白
     * </pre>
     *
     * 進度 0 或滿的時候箭頭只有一段，{@code {cN}} 的編號就整個往前挪一格，
     * 寫死的號碼有一半的時候會落空——落空當作沒寫，畫面上整行變灰。
     */
    private static void wordPalette() {
        Style grey = Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA));
        Style green = Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55));
        Style dark = Style.EMPTY.withColor(TextColor.fromRgb(0x555555));
        Style rare = Style.EMPTY.withColor(TextColor.fromRgb(0xFF55FF));
        Style white = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF));

        // 進度做到一半：綠箭頭與暗箭頭各一段
        List<LineParts.Piece> half = List.of(
                new LineParts.Piece("Tier I ", grey),
                new LineParts.Piece(">>>>>", green),
                new LineParts.Piece(">>>>>", dark),
                new LineParts.Piece(" Tier II ", rare),
                new LineParts.Piece("[2/4]", white));
        // 進度是 0：只有一段暗箭頭
        List<LineParts.Piece> none = List.of(
                new LineParts.Piece("Tier I ", grey),
                new LineParts.Piece(">>>>>>>>>>", dark),
                new LineParts.Piece(" Tier II ", rare),
                new LineParts.Piece("[0/4]", white));

        for (var runs : List.of(half, none)) {
            List<Style> words = LineTranslator.wordPalette(runs);
            check("{w1} 是目前層級的灰（實際 " + words.size() + " 色）",
                    words.size() >= 2 && grey.equals(words.get(0)));
            check("{w2} 是下一層級的稀有度色", words.size() >= 2 && rare.equals(words.get(1)));
        }

        // 對照：舊的 palette 會因為箭頭段數不同而挪位，這正是要避開的
        check("舊編號確實會挪位",
                LineTranslator.palette(half).size() != LineTranslator.palette(none).size());
    }

    /**
     * 進度條連同<b>每一格的顏色</b>搬回來。
     *
     * <h2>為什麼 {@code {wN}} 不夠</h2>
     * {@code {wN}} 解決的是「下一層級那一段是第幾個顏色」，那是一段一個顏色。
     * 但箭頭本身是<b>一格一個顏色</b>——九綠五灰就是進度 9/14。譯文裡那條箭頭
     * 是一整段文字，怎麼標顏色都只會是同一個色，進度就沒了。使用者回報畫面上
     * 那條該是灰的卻整條變亮。
     *
     * <p>所以改成照抄：譯文那條跟原文同一個符號、同樣長，就一格一格把顏色搬回來。
     */
    private static void progressBar() {
        Style grey = Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA));
        Style green = Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55));
        Style dark = Style.EMPTY.withColor(TextColor.fromRgb(0x555555));
        Style rare = Style.EMPTY.withColor(TextColor.fromRgb(0xFF55FF));
        Style white = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF));

        List<LineParts.Piece> half = List.of(
                new LineParts.Piece("Tier I ", grey),
                new LineParts.Piece(">>>>>", green),
                new LineParts.Piece(">>>>>", dark),
                new LineParts.Piece(" Tier II ", rare),
                new LineParts.Piece("[2/4]", white));

        List<LineTranslator.Bar> bars = LineTranslator.bars(half);
        check("整行只認出一條進度條（實際 " + bars.size() + " 條）", bars.size() == 1);
        if (bars.size() == 1) {
            LineTranslator.Bar bar = bars.get(0);
            check("符號是箭頭", bar.sign() == '>');
            check("十格（實際 " + bar.styles().size() + "）", bar.styles().size() == 10);
            check("前五格是綠的",
                    bar.styles().subList(0, 5).stream().allMatch(green::equals));
            check("後五格是暗灰的",
                    bar.styles().subList(5, 10).stream().allMatch(dark::equals));
        }

        // 進度 0：整條都是暗灰，格數不變——譯文那條照樣對得上
        List<LineParts.Piece> none = List.of(
                new LineParts.Piece("Tier I ", grey),
                new LineParts.Piece(">>>>>>>>>>", dark),
                new LineParts.Piece(" Tier II ", rare),
                new LineParts.Piece("[0/4]", white));
        List<LineTranslator.Bar> flat = LineTranslator.bars(none);
        check("進度 0 也是一條十格",
                flat.size() == 1 && flat.get(0).styles().size() == 10);

        // 譯文那一段裡找得到那條，兩頭的空白不算進去
        int[] span = LineTranslator.barSpan(" >>>>>>>>>> ");
        check("找得到譯文裡的那條（實際 " + java.util.Arrays.toString(span) + "）",
                span != null && span[0] == 1 && span[1] == 11);

        // 反面：這些都不是進度條
        check("中文不是進度條", LineTranslator.barSpan("第 I 層") == null);
        check("兩格的重複符號不算", LineTranslator.barSpan("看!! 這樣") == null);
        check("[2/4] 不算", LineTranslator.barSpan("[2/4]") == null);
        check("空白不算", LineTranslator.barSpan("第 I 層     第 II 層") == null);
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            System.out.println("顏色佔位符：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("顏色佔位符：全部通過");
    }
}
