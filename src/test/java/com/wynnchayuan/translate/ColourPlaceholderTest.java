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

public final class ColourPlaceholderTest {

    private static int failures = 0;

    private static final int GREEN = 0x55FF55;
    private static final int WHITE = 0xFFFFFF;
    private static final int PINK  = 0xFF55FF;
    private static final int GREY  = 0xAAAAAA;
    private static final int GOLD  = 0xFFAA00;

    private static StyledText cave() {
        MutableComponent all = Component.empty();
        all.append(lit("[Cave Completed]", GREEN, false));
        all.append(lit("\n", GREY, false));
        all.append(lit("Grook's Nest", WHITE, true));
        all.append(lit("\n", GREY, false));
        all.append(lit("- ", PINK, false));
        all.append(lit("Rewards:", GREY, false));
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

        check("畫面上沒有 {c 佔位符（實際：" + shown.replace("\n", " ⏎ ") + "）",
              !shown.contains("{c") && !shown.contains("{/}"));

        Integer title = colourOf(hit, "洞穴完成");
        check("標題套上原文的綠色（拿到 " + hex(title) + "）",
              title != null && title == GREEN);

        Integer name = colourOf(hit, "巢穴");
        check("洞窟名稱套上原文的白色（拿到 " + hex(name) + "）",
              name != null && name == WHITE);
        check("洞窟名稱連粗體一起搬過來", Boolean.TRUE.equals(boldOf(hit, "巢穴")));

        Integer dash = colourOf(hit, "-");
        check("行首的破折號是粉紅（拿到 " + hex(dash) + "）", dash != null && dash == PINK);
        Integer label = colourOf(hit, "獎勵");
        check("同一行的「獎勵」不被粉紅波及（拿到 " + hex(label) + "）",
              label != null && label == GREY);

        Integer own = colourOf(hit, "封印的頭盔");
        check("{c:#FFAA00} 指定的金色套得上（拿到 " + hex(own) + "）",
              own != null && own == GOLD);

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
        tierBrackets();
        report();
    }

    private static void crossesLines(String src) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-colour-cross");
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

    private static void wordPalette() {
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
        List<LineParts.Piece> none = List.of(
                new LineParts.Piece("Tier I ", grey),
                new LineParts.Piece(">>>>>>>>>>", dark),
                new LineParts.Piece(" Tier II ", rare),
                new LineParts.Piece("[0/4]", white));

        for (var runs : List.of(half, none)) {
            List<Style> words = ColourTemplate.wordPalette(runs);
            check("{w1} 是目前層級的灰（實際 " + words.size() + " 色）",
                    words.size() >= 2 && grey.equals(words.get(0)));
            check("{w2} 是下一層級的稀有度色", words.size() >= 2 && rare.equals(words.get(1)));
        }

        check("舊編號確實會挪位",
                ColourTemplate.palette(half).size() != ColourTemplate.palette(none).size());
    }

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

        List<ColourTemplate.Bar> bars = ColourTemplate.bars(half);
        check("整行只認出一條進度條（實際 " + bars.size() + " 條）", bars.size() == 1);
        if (bars.size() == 1) {
            ColourTemplate.Bar bar = bars.get(0);
            check("符號是箭頭", bar.sign() == '>');
            check("十格（實際 " + bar.styles().size() + "）", bar.styles().size() == 10);
            check("前五格是綠的",
                    bar.styles().subList(0, 5).stream().allMatch(green::equals));
            check("後五格是暗灰的",
                    bar.styles().subList(5, 10).stream().allMatch(dark::equals));
        }

        List<LineParts.Piece> none = List.of(
                new LineParts.Piece("Tier I ", grey),
                new LineParts.Piece(">>>>>>>>>>", dark),
                new LineParts.Piece(" Tier II ", rare),
                new LineParts.Piece("[0/4]", white));
        List<ColourTemplate.Bar> flat = ColourTemplate.bars(none);
        check("進度 0 也是一條十格",
                flat.size() == 1 && flat.get(0).styles().size() == 10);

        int[] span = ColourTemplate.barSpan(" >>>>>>>>>> ");
        check("找得到譯文裡的那條（實際 " + java.util.Arrays.toString(span) + "）",
                span != null && span[0] == 1 && span[1] == 11);

        check("中文不是進度條", ColourTemplate.barSpan("第 I 層") == null);
        check("兩格的重複符號不算", ColourTemplate.barSpan("看!! 這樣") == null);
        check("[2/4] 不算", ColourTemplate.barSpan("[2/4]") == null);
        check("空白不算", ColourTemplate.barSpan("第 I 層     第 II 層") == null);
    }

    private static void tierBrackets() throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        Style grey = Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA));
        Style dark = Style.EMPTY.withColor(TextColor.fromRgb(0x555555));
        Style tier = Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF));

        MutableComponent line = Component.empty();
        line.append(Component.literal(new String(Character.toChars(0xD0012)))
                .withStyle(Style.EMPTY.withFont(
                        new net.minecraft.network.chat.FontDescription.Resource(
                                net.minecraft.resources.Identifier
                                        .withDefaultNamespace("space")))));
        line.append(Component.literal("Tier I ").withStyle(grey));
        line.append(Component.literal(">>>>>>>>>>").withStyle(dark));
        line.append(Component.literal(" ").withStyle(grey));
        line.append(Component.literal("Tier II").withStyle(tier));
        line.append(Component.literal(" [2/4]").withStyle(grey));

        Component built = LineTranslator.translate(StyledText.fromComponent(line), store);
        if (built == null) {
            check("層級那一行查得到譯文", false);
            return;
        }
        String all = built.getString();
        check("層級翻出來了（實際：" + all + "）", all.contains("第 II 層"));

        Integer bracket = colourOf(built, "[");
        Integer name = colourOf(built, "第 II 層");
        check("層級是層級的顏色（拿到 " + hex(name) + "）",
                name != null && name == 0x55FFFF);
        check("中括號不是層級的顏色（拿到 " + hex(bracket) + "）",
                bracket != null && bracket != 0x55FFFF);
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
