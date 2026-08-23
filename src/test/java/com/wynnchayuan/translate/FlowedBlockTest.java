package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 被 tooltip 寬度<b>自動斷行</b>的長敘述要查得到。
 *
 * <h2>畫面上長什麼樣</h2>
 * 裝備的背景敘述在語料裡是<b>沒有斷行的一整句</b>，遊戲卻依畫面寬度把它切成
 * 十來行。兩邊字面上永遠不相等，所以查表得先把那幾行併回一句。
 *
 * <p>這條路先前有兩個各自獨立的破口，合起來讓<b>整批</b>裝備敘述都不生效——
 * 譯文明明在檔案裡，畫面上還是整段英文：
 *
 * <ol>
 *   <li>併起來的行數上限是 8，而敘述普遍十行以上，永遠湊不齊。</li>
 *   <li>地名是<b>逐行</b>認的。「the peak of the Tower of ⏎ Ascension,」
 *       被切開之後兩行都不含完整地名，模板裡留著原樣英文；
 *       而語料裡存的是 {@code {p}}。</li>
 * </ol>
 *
 * <p>兩個都得修才會生效，所以這條測試同時盯著它們。
 */
public final class FlowedBlockTest {

    private static int failures = 0;

    private static final String NL = System.lineSeparator();

    private static final int BODY = 0xAAAAAA;

    /** Oblivion 的背景敘述，照遊戲的寬度斷行。地名剛好斷在第二、三行之間。 */
    private static final String[] LORE = {
            "This sacrificial kris, fallen from",
            "the peak of the Tower of",
            "Ascension, is completely hollow.",
            "To wield it is to invite to",
            "yourself the same hollowness. To",
            "become such an aberration, to",
            "unwrite yourself in pursuit of",
            "power...it must be fed of its",
            "wielder til only the barest",
            "existence remains.",
    };

    public static void main(String[] args) {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations"));

        check("掃描深度夠深（實際 " + store.maxBlockLines() + "）",
                store.maxBlockLines() >= LORE.length);

        String lore = joined(block(LORE), store);
        check("十行的敘述查得出來（實際："
                        + shorten(lore) + "）",
                lore != null && lore.startsWith("這把從"));
        check("被斷行切開的地名有填回去", lore != null
                && lore.contains("Tower of Ascension"));
        check("譯文裡不留 {p}", lore != null && !lore.contains("{p}"));

        // 反面：湊不齊的那幾行不能硬翻。少了最後兩行就不是完整的那一句，
        // 這時候應該讓原文照樣顯示，而不是拿半句話去撞別的條目。
        check("湊不齊就不硬翻",
                LineTranslator.translateBlock(
                        block(java.util.Arrays.copyOf(LORE, 8)),
                        store, new boolean[8]) == null);

        // 整份 tooltip 走一遍。單獨一段會過，不代表放進上下文裡也會過——
        // 呼叫端是「由長到短試、中了就往後跳」，前面某一段多吃了幾行的話，
        // 起點就會落在段落中間，那一段從此再也湊不出來。
        List<Component> tip = new ArrayList<>();
        tip.add(plain("Oblivion"));
        tip.add(plain(""));
        for (String line : LORE) {
            tip.add(plain(line));
        }
        tip.add(plain(""));
        tip.add(plain("Combat Level Min: 101"));
        List<Component> out =
                com.wynnchayuan.render.TooltipPanel.translateLines(tip, store);
        String all = out.stream().map(Component::getString)
                .reduce("", (a, b) -> a + " " + b);
        check("放進整份 tooltip 也翻得出來（實際：" + shorten(all) + "）",
                all.contains("獻祭之刃"));

        // 遊戲實際送來的樣子：置中的段落，<b>每一行開頭</b>都有一個推位置用的
        // 隱形字元。抽成模板就是行首的 {#}，攤平之後整段夾著一堆 {#}，
        // 而語料裡是乾淨的一句話。診斷檔第 20 筆就是這個樣子。
        String offset = joined(centred(LORE), store);
        check("每行開頭帶排版偏移也翻得出來（實際：" + shorten(offset) + "）",
                offset != null && offset.startsWith("這把從"));

        shaman(store);
        divider(store);
        majorId(store);

        System.out.println(failures == 0
                ? "FlowedBlock: 全部通過" : "FlowedBlock: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /**
     * Major ID 照遊戲送來的樣子：名稱那半是亮色、帶排版偏移，
     * 末尾的 {@code elements} 自成一個綠色的段。
     *
     * <p>那個綠色要跟著譯文走——使用者要的是「翻出來也保留原始格式」。
     */
    private static void majorId(TranslationStore store) {
        Style pink = Style.EMPTY.withColor(TextColor.fromRgb(0xE0B3E6));
        Style grey = Style.EMPTY.withColor(TextColor.fromRgb(BODY));
        Style green = Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55));

        MutableComponent first = Component.empty();
        first.append(Component.literal(SpaceOffset.encode(2))
                .withStyle(SpaceOffset.styleFor(pink)));
        first.append(Component.literal("Efflorescence: ").withStyle(pink));
        first.append(Component.literal("Serpent's").withStyle(grey));

        MutableComponent last = Component.empty();
        last.append(Component.literal("all ").withStyle(grey));
        last.append(Component.literal("elements").withStyle(green));
        last.append(Component.literal(".").withStyle(grey));

        List<StyledText> run = new ArrayList<>();
        run.add(StyledText.fromComponent(first));
        for (String s : new String[] {
                "Garden now centers to where", "you land with Swan Dive.",
                "Damage of Swan Dive,", "Serpent's Garden and Jasmine",
                "Bloom are distributed across" }) {
            run.add(StyledText.fromComponent(plain(s)));
        }
        run.add(StyledText.fromComponent(last));

        List<Component> out = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        check("Major ID 的敘述翻得出來",
                out != null && !out.isEmpty());
        if (out == null) {
            return;
        }
        String all = out.stream().map(Component::getString)
                .reduce("", String::concat);
        check("名稱與敘述都翻了（實際：" + shorten(all) + "）",
                all.contains("華綻") && all.contains("所有元素"));
        check("名稱保住自己的顏色", colourOf(out, "華綻") == 0xE0B3E6);
        check("末尾的元素保住原本的綠（實際："
                        + Integer.toHexString(colourOf(out, "元素")) + "）",
                colourOf(out, "元素") == 0x55FF55);
    }

    /** 譯文裡某一段文字實際被塗上的顏色。 */
    private static int colourOf(List<Component> out, String needle) {
        for (Component line : out) {
            for (Component part : flatten(line)) {
                if (part.getString().contains(needle)
                        && part.getStyle().getColor() != null) {
                    return part.getStyle().getColor().getValue();
                }
            }
        }
        return -1;
    }

    private static List<Component> flatten(Component c) {
        List<Component> out = new ArrayList<>();
        out.add(c);
        for (Component child : c.getSiblings()) {
            out.addAll(flatten(child));
        }
        return out;
    }

    /**
     * 譯文<b>比原文少行</b>時要自己折回去。
     *
     * <p>譯者常把三行的英文寫成一句中文——那在語料裡就是沒有換行的一整句。
     * 先前只有「攤平查表」那條路會折，精準命中的不會，
     * 於是畫面上就是一條長到衝出面板的行（issue #90 第 4 點）。
     */
    private static void shaman(TranslationStore store) {
        String[] src = {
                "When casting Uproot, instead",
                "wear one of the Mystic Masks.",
                "(Shift + Uproot to remove it)",
        };
        List<StyledText> run = block(src);
        List<Component> out = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        check("薩滿的三行敘述翻得出來", out != null && !out.isEmpty());
        if (out == null) {
            return;
        }
        String all = out.stream().map(Component::getString)
                .reduce("", String::concat);
        check("內容是譯文（實際：" + shorten(all) + "）", all.contains("神祕面具"));

        // 折行本身量的是<b>像素寬度</b>，而測試環境沒有字型（Minecraft.getInstance()
        // 是 null），量出來一律是 0，折不出東西。所以這裡釘的是<b>判斷</b>：
        // 「要不要折」才是這次改的地方，折的機制本來就在，只是沒被叫到。
        check("譯文比原文少行 -> 要折",
                LineTranslator.needsWrap("一句話", 3, false));
        check("攤平查到的一律要折",
                LineTranslator.needsWrap("一句話", 1, true));
        check("行數一樣就不折——那是譯者排好的形狀",
                !LineTranslator.needsWrap("一" + NL + "二" + NL + "三", 3, false));
        check("比原文多行也不折——折了只會更長",
                !LineTranslator.needsWrap("一" + NL + "二" + NL + "三" + NL + "四",
                        3, false));
    }

    /**
     * lore 上下那條分隔線（◆—◆）不能被吃掉。
     *
     * <p>它整行都是符號，抽成模板就是一串 {@code {#}}——跟行首的排版偏移
     * 長得一模一樣。把它當成偏移剝掉的話，這一行會變成空的，
     * 於是整段連分隔線一起被當成同一句話併進去，譯文出來就少了那條線。
     */
    private static void divider(TranslationStore store) {
        List<Component> tip = new ArrayList<>();
        tip.add(plain("Halcyon"));
        tip.add(rule());
        for (StyledText line : centred(LORE)) {
            tip.add(line.getComponent().copy());
        }
        tip.add(rule());
        List<Component> out =
                com.wynnchayuan.render.TooltipPanel.translateLines(tip, store);
        String all = out.stream().map(Component::getString)
                .reduce("", (a, b) -> a + b);
        check("敘述有翻（實際：" + shorten(all) + "）", all.contains("獻祭之刃"));
        int rules = 0;
        for (Component line : out) {
            if (line.getString().contains(RULE)) {
                rules++;
            }
        }
        check("上下兩條分隔線都還在（實際 " + rules + " 條）", rules == 2);
    }

    /** 整行都是符號的分隔線。 */
    private static final String RULE = "";

    private static Component rule() {
        return Component.literal(RULE).withStyle(
                Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(
                        net.minecraft.resources.Identifier.withDefaultNamespace("common"))));
    }

    /** 置中的段落：每一行前面都掛一個推位置用的隱形字元。 */
    private static List<StyledText> centred(String[] lines) {
        List<StyledText> run = new ArrayList<>(lines.length);
        for (String line : lines) {
            MutableComponent c = Component.empty();
            c.append(Component.literal(SpaceOffset.encode(12))
                    .withStyle(SpaceOffset.styleFor(Style.EMPTY)));
            c.append(plain(line));
            run.add(StyledText.fromComponent(c));
        }
        return run;
    }

    private static String joined(List<StyledText> run, TranslationStore store) {
        List<Component> out = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        return out == null ? null
                : out.stream().map(Component::getString).reduce("", String::concat);
    }

    private static List<StyledText> block(String[] lines) {
        List<StyledText> run = new ArrayList<>(lines.length);
        for (String line : lines) {
            run.add(StyledText.fromComponent(plain(line)));
        }
        return run;
    }

    private static MutableComponent plain(String text) {
        return Component.literal(text)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(BODY)));
    }

    private static String shorten(String text) {
        if (text == null) {
            return "null";
        }
        String flat = text.replace("\n", " ").strip();
        return flat.length() <= 34 ? flat : flat.substring(0, 34) + "…";
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
