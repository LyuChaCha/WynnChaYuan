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
 * Major ID 的<b>名稱</b>要保住它自己的顏色。
 *
 * <h2>畫面上長什麼樣</h2>
 * 原文的 {@code ✦ Altruism: Allies within 16 blocks...} 裡，名稱那半是亮色的，
 * 說明那半是暗一階的。譯文如果整段用同一個顏色，「利他主義」就跟說明糊在一起，
 * 一眼看不出哪裡是名字。
 *
 * <p>難處在於譯文是中文，跟原文的字面對不起來，一般的樣式沿用比對不到。
 * 程式的做法是把<b>譯出來的名稱</b>連同原文名稱的樣式一起交給重建那一步
 * （見 {@code labelAccent}）。這條測試就是釘住那一步。
 */
public final class MajorIdColourTest {

    private static int failures = 0;

    private static final int NAME_COLOUR = 0xFF55FF;
    private static final int BODY_COLOUR = 0xAAAAAA;
    private static final int MARK_COLOUR = 0xFFFF55;

    /** 遊戲送來的樣子：名稱與說明各有顏色，並依 tooltip 寬度斷成四行。 */
    private static List<StyledText> block() {
        return block(false);
    }

    /**
     * @param markSeparate {@code ✦} 自成一個色段。遊戲實際上就是這樣送的，
     *                     而「純符號的段也算重點段」是後來為了保住 {@code ✖}
     *                     的紅色才加的——兩件事湊在一起，名稱的樣式就被
     *                     {@code ✦} 擠掉了。
     */
    private static List<StyledText> block(boolean markSeparate) {
        String[][] rows = {
            {"✦ Altruism: ", "Allies within 16"},
            {null, "blocks gain 100% of the"},
            {null, "health you gain from Health"},
            {null, "Regen and Life Steal."},
        };
        List<StyledText> out = new ArrayList<>();
        for (String[] row : rows) {
            MutableComponent line = Component.empty();
            if (row[0] != null) {
                String name = row[0];
                if (markSeparate) {
                    line.append(Component.literal("✦ ").withStyle(
                            Style.EMPTY.withColor(TextColor.fromRgb(MARK_COLOUR))));
                    name = name.substring("✦ ".length());
                }
                line.append(Component.literal(name).withStyle(
                        Style.EMPTY.withColor(TextColor.fromRgb(NAME_COLOUR))));
            }
            line.append(Component.literal(row[1]).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR))));
            out.add(StyledText.fromComponent(line));
        }
        return out;
    }

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations"));

        // 診斷檔要真的寫得出來。使用者回報「layout-debug.txt 根本沒生成」，
        // 而那個檔正是查 Major ID 顏色唯一的依據——它不寫出來，就等於沒有診斷。
        Path dir = java.nio.file.Files.createTempDirectory("wynnchayuan");
        FlowedDebug.init(dir);
        Path debug = dir.resolve("majorid-debug.txt");
        check("診斷檔一開場就存在", java.nio.file.Files.isRegularFile(debug));

        List<StyledText> run = block();
        List<Component> built = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);

        check("整段查得到譯文", built != null && !built.isEmpty());
        if (built == null || built.isEmpty()) {
            report();
            return;
        }
        String all = built.stream().map(Component::getString)
                .reduce("", (a, b) -> a + b);
        check("名稱有翻出來（實際：" + all.substring(0, Math.min(24, all.length())) + "）",
                all.contains("利他主義"));
        Integer colour = colourOf(built, "利他主義");
        check("名稱查得到顏色", colour != null);
        if (colour != null) {
            check("名稱不是說明的顏色（拿到 #" + String.format("%06X", colour) + "）",
                    colour != BODY_COLOUR);
            check("名稱沿用原文名稱的顏色", colour == NAME_COLOUR);
        }
        // ✦ 自成一個色段——遊戲實際上就是這樣送的
        List<StyledText> split = block(true);
        List<Component> two = LineTranslator.translateBlock(
                split, store, new boolean[split.size()]);
        check("✦ 獨立成段時整段仍查得到", two != null && !two.isEmpty());
        if (two != null && !two.isEmpty()) {
            Integer c = colourOf(two, "利他主義");
            check("✦ 獨立成段時名稱仍保住自己的顏色（拿到 "
                            + (c == null ? "null" : "#" + String.format("%06X", c)) + "）",
                    c != null && c == NAME_COLOUR);
        }
        // 名稱比同一行露出的說明<b>還長</b>——這一行的「主要樣式」於是變成名稱，
        // 被判為「不同」的反而是說明。先前程式就是在這裡挑錯，兩邊顏色對調。
        List<StyledText> longName = new ArrayList<>();
        MutableComponent head = Component.empty();
        head.append(Component.literal("✦ Altruism: ").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(NAME_COLOUR))));
        head.append(Component.literal("Allies").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR))));
        longName.add(StyledText.fromComponent(head));
        for (String row : new String[] {"within 16 blocks gain 100% of the",
                                        "health you gain from Health",
                                        "Regen and Life Steal."}) {
            longName.add(StyledText.fromComponent(Component.literal(row).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR)))));
        }
        List<Component> third = LineTranslator.translateBlock(
                longName, store, new boolean[longName.size()]);
        check("名稱比說明長時整段仍查得到", third != null && !third.isEmpty());
        if (third != null && !third.isEmpty()) {
            Integer c = colourOf(third, "利他主義");
            check("名稱比說明長時顏色不會跟說明對調（拿到 "
                            + (c == null ? "null" : "#" + String.format("%06X", c)) + "）",
                    c != null && c == NAME_COLOUR);
        }
        String written = java.nio.file.Files.readString(debug);
        check("跨行翻譯之後診斷檔有內容（" + written.length() + " 字）",
                written.contains("原文第一行的色段"));
        check("診斷檔記下了原文的色段", written.contains("#FF55FF"));
        check("診斷檔記下了每一次呼叫的流水號", written.contains("=== 1 ==="));
        // 遊戲實際送來的結構（取自使用者回報的 majorid-debug.txt）：
        //   [0] #E0B3E6 「」                        ← 空字串
        //   [1] #E0B3E6 「(位移字元)Transcendence: 」 ← 前綴是寬度位移字元，不是 ✦
        //   [2] #AAAAAA 「25% chance for」
        // 名稱那一段比同一行的說明長，前面還掛著一個空的片段——
        // 這兩件事都是我自己捏測試時沒有的，所以先前三種情境全過卻修不好。
        String offset = new StringBuilder().appendCodePoint(0xD0002).toString();
        List<StyledText> real = new ArrayList<>();
        MutableComponent head2 = Component.empty();
        head2.append(Component.literal("").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(0xE0B3E6))));
        head2.append(Component.literal(offset + "Transcendence: ").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(0xE0B3E6))));
        head2.append(Component.literal("25% chance for").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA))));
        real.add(StyledText.fromComponent(head2));
        for (String row : new String[] {"spells to cost no mana when", "casted."}) {
            real.add(StyledText.fromComponent(Component.literal(row).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA)))));
        }
        List<Component> got = LineTranslator.translateBlock(
                real, store, new boolean[real.size()]);
        check("遊戲實際結構查得到（"
                        + (got == null || got.isEmpty() ? "null" : got.get(0).getString())
                        + "）", got != null && !got.isEmpty());
        if (got != null && !got.isEmpty()) {
            Integer c = colourOf(got, "超凡境界");
            check("遊戲實際結構下名稱保住自己的顏色（拿到 "
                            + (c == null ? "null" : "#" + String.format("%06X", c)) + "）",
                    c != null && c == 0xE0B3E6);
            // 「顏色相反」的意思是說明那半拿到了名稱的顏色。分開驗一次。
            Integer body = colourOf(got, "機率");
            check("說明那半沒有拿到名稱的顏色（拿到 "
                            + (body == null ? "null" : "#" + String.format("%06X", body))
                            + "）", body != null && body == 0xAAAAAA);
            for (Component one : got) {
                System.out.println("      輸出：" + one.getString());
                for (Component leaf : flatten(one)) {
                    System.out.println("        「" + leaf.getString() + "」 "
                            + (leaf.getStyle().getColor() == null ? "繼承"
                               : leaf.getStyle().getColor().toString()));
                }
            }
        }

        // 名稱在原文裡是一個字一個顏色拼出來的（彩虹字）。
        // 譯文<b>不</b>重現那道漸層——照專案的原則保留原始樣式，不特地改顏色。
        // 這裡釘住的是：名稱仍然是名稱那個顏色，說明仍然是說明那個顏色。
        int[] rainbow = {0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF,
                         0x5555FF, 0xFF55FF, 0xFF5555, 0xFFAA00, 0xFFFF55,
                         0x55FF55, 0x55FFFF, 0x5555FF};
        List<StyledText> shiny = new ArrayList<>();
        MutableComponent head3 = Component.empty();
        String label = "Efflorescence";
        for (int i = 0; i < label.length(); i++) {
            head3.append(Component.literal(String.valueOf(label.charAt(i))).withStyle(
                    Style.EMPTY.withColor(
                            TextColor.fromRgb(rainbow[i % rainbow.length]))));
        }
        head3.append(Component.literal(": Serpent's Garden now centers to").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR))));
        shiny.add(StyledText.fromComponent(head3));
        for (String row : new String[] {
                "where you land with Swan Dive. Damage of Swan Dive,",
                "Serpent's Garden and Jasmine Bloom are distributed",
                "across all elements."}) {
            shiny.add(StyledText.fromComponent(Component.literal(row).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR)))));
        }
        List<Component> painted = LineTranslator.translateBlock(
                shiny, store, new boolean[shiny.size()]);
        check("彩虹名稱的整段查得到", painted != null && !painted.isEmpty());
        if (painted != null && !painted.isEmpty()) {
            // 名稱那一段只能有<b>一個</b>顏色。先前這裡會被拆成一個字一段、
            // 各自套上漸層裡的一個顏色；現在保留原始樣式，整段是同一色。
            int leaves = 0;
            java.util.LinkedHashSet<Integer> used = new java.util.LinkedHashSet<>();
            for (Component line : painted) {
                for (Component leaf : flatten(line)) {
                    if (!leaf.getString().contains("華綻")) {
                        continue;
                    }
                    leaves++;
                    if (leaf.getStyle().getColor() != null) {
                        used.add(leaf.getStyle().getColor().getValue());
                    }
                }
            }
            check("譯名沒有被拆成一個字一段（拆成 " + leaves + " 段）", leaves == 1);
            check("譯名維持單一顏色，沒有被重新上色（用到 " + used.size() + " 個）",
                    used.size() == 1);
            check("用的是原文名稱本來的顏色", rainbowContains(rainbow, used));
            Integer bodyColour = colourOf(painted, "分散");
            check("說明那半沒有被染成彩虹（拿到 "
                            + (bodyColour == null ? "null"
                               : "#" + String.format("%06X", bodyColour)) + "）",
                    bodyColour != null && bodyColour == BODY_COLOUR);
        }
        // 普通的兩段上色照舊
        List<StyledText> ordinary = block();
        List<Component> plain = LineTranslator.translateBlock(
                ordinary, store, new boolean[ordinary.size()]);
        check("普通的兩段上色仍然正確",
                plain != null && colourOf(plain, "利他主義") != null
                        && colourOf(plain, "利他主義") == NAME_COLOUR);

        keepWordColour(store);
        report();
    }

    /**
     * 一段被切成好幾行之後，行內那個<b>最長</b>的彩色詞不能掉色。
     *
     * <h2>畫面上長什麼樣</h2>
     * 技能樹的 {@code Larger Mana Bank II} 寫著：
     *
     * <pre>
     *   Increase your maximum          ← 整行灰色
     *   Mana Bank ✺ by +30.            ← 「Mana Bank」是藍的
     * </pre>
     *
     * <p>重點段是拿<b>那一行自己的</b>主樣式篩出來的。第二行裡「Mana Bank」比
     * 「 by +30.」長，於是藍色成了那一行的主樣式，這一段就被當成「不是重點」丟掉——
     * 譯文重建時沒有東西可以把藍色貼回去，畫面上 Mana Bank 變成白的。
     *
     * <p>Major ID 的敘述整段套上標題顏色，也是同一個機制的另一面：名稱比同一行
     * 露出的說明長時，說明那半的灰色被丟掉。
     */
    private static void keepWordColour(TranslationStore store) {
        final int WORD = 0x55FFFF;
        MutableComponent second = Component.empty();
        second.append(Component.literal("Mana Bank ").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(WORD))));
        second.append(Component.literal("✺").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(0xFF55FF))));
        second.append(Component.literal(" by +30.").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR))));

        List<StyledText> run = List.of(
                StyledText.fromComponent(Component.literal("Increase your maximum")
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR)))),
                StyledText.fromComponent(second));
        List<Component> out = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        check("技能敘述整段查得到", out != null && !out.isEmpty());
        if (out == null || out.isEmpty()) {
            return;
        }
        Integer word = colourOf(out, "Mana Bank");
        check("行內最長的那個彩色詞沒有掉色（拿到 "
                        + (word == null ? "null" : "#" + String.format("%06X", word))
                        + "）", word != null && word == WORD);
        Integer body = colourOf(out, "上限提高");
        check("其餘敘述仍然是敘述的顏色（拿到 "
                        + (body == null ? "null" : "#" + String.format("%06X", body))
                        + "）", body != null && body == BODY_COLOUR);
    }

    /** 譯名用到的顏色，得是原文名稱那幾段裡本來就有的。 */
    private static boolean rainbowContains(int[] ramp, java.util.Set<Integer> used) {
        for (int colour : used) {
            boolean found = false;
            for (int candidate : ramp) {
                found |= candidate == colour;
            }
            if (!found) {
                return false;
            }
        }
        return !used.isEmpty();
    }

    /** 找出含有 needle 的那一段是什麼顏色。 */
    private static Integer colourOf(List<Component> lines, String needle) {
        for (Component line : lines) {
            for (Component part : flatten(line)) {
                if (part.getString().contains(needle)) {
                    TextColor colour = part.getStyle().getColor();
                    return colour == null ? null : colour.getValue();
                }
            }
        }
        return null;
    }

    /** 只收<b>葉子</b>。最外層那個容器的 getString() 含有整行，會先被撈到，
     *  而它自己沒有顏色——那樣量到的永遠是 null。 */
    private static List<Component> flatten(Component component) {
        List<Component> out = new ArrayList<>();
        if (component.getSiblings().isEmpty()) {
            out.add(component);
        }
        for (Component child : component.getSiblings()) {
            out.addAll(flatten(child));
        }
        return out;
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        System.out.println(failures == 0
                ? "MajorIdColour: 全部通過" : "MajorIdColour: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
