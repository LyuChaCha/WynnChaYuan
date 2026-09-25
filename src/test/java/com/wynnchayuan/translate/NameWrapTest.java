package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * 「譯名 + 原文」太長時，附的原文要挪到下一行。
 *
 * <h2>實機回報</h2>
 * 物品名稱那一行畫出來是：
 *
 * <pre>
 *   烬咒牧杖 (Cindercurse Crosier) [51.0%]
 * </pre>
 *
 * 而 Wynncraft 的外框是<b>字型畫的</b>、照英文的寬度挑好的圖（見 {@link NameWrap}），
 * 所以後面那半截落在框外。
 *
 * <h2>這裡釘住什麼</h2>
 * 名稱那一行跟一般的長句不一樣，{@code appendedOriginalAt} 也接不住它——
 * 括號後面還跟著耐久度，整行不是以 {@code )} 結尾的。所以：
 *
 * <ul>
 *   <li>斷點落在括號的接縫上，耐久度留在<b>第一行</b>（框上替它留了位置）；</li>
 *   <li>每一小段的顏色跟著走，不能斷完變成一片白；</li>
 *   <li>放得進原文寬度的不斷，headless 量不出寬度的也不斷。</li>
 * </ul>
 */
public final class NameWrapTest {

    private static int failures = 0;

    /** 假字型：中日文一個字兩格，其他一格。 */
    private static final ToIntFunction<Component> WIDTH = c -> {
        int[] w = {0};
        c.visit((style, text) -> {
            for (int i = 0; i < text.length(); i++) {
                w[0] += text.charAt(i) > 0x2E80 ? 2 : 1;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return w[0];
    };

    private static final int NAME = 0x55FFFF;
    private static final int WEAR = 0xFFDD33;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-name-wrap");
        // gearOnly 才會附原文：只有裝備檔有這個原文。
        Files.writeString(dir.resolve("gear-weapon.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "a1": {"src": "Cindercurse Crosier", "dst": "燼咒牧杖", "role": "name"},
                   "a2": {"src": "Idol", "dst": "神像", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        store.setNameMode(com.wynnchayuan.CollectorConfig.ItemNames.BOTH);

        tooLong(store);
        shortEnough(store);
        noOriginal(store);
        noFont(store);
        lineUp(store);
        inStep(store);
        invisibleRow(store);

        System.out.println(failures == 0
                ? "NameWrapTest 全部通過" : failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /** ★ 回報的那一行：拆成兩行，耐久度留在上面。 */
    private static void tooLong(TranslationStore store) {
        List<Component> original = List.of(
                row("Cindercurse Crosier", NAME, " [51.0%]", WEAR));
        List<Component> translated = List.of(
                row("燼咒牧杖 (Cindercurse Crosier)", NAME, " [51.0%]", WEAR));

        List<Component> out = rows(original, translated, store, WIDTH);

        check("多出一行（實際 " + out.size() + " 行）", out.size() == 2);
        if (out.size() != 2) {
            return;
        }
        check("名稱與耐久度留在第一行（實際 " + out.get(0).getString() + "）",
              "燼咒牧杖 [51.0%]".equals(out.get(0).getString()));
        check("原文自己一行（實際 " + out.get(1).getString() + "）",
              "(Cindercurse Crosier)".equals(out.get(1).getString()));
        check("第一行收得進原文的寬度",
              WIDTH.applyAsInt(out.get(0)) <= WIDTH.applyAsInt(original.get(0)));
        check("譯名的顏色沒掉", NAME == colourAt(out.get(0), 0));
        check("耐久度的顏色沒掉", WEAR == colourAt(out.get(0), 5));
        check("原文那一行是名稱的顏色", NAME == colourAt(out.get(1), 0));
    }

    /**
     * 收得進原文寬度的就不拆。
     *
     * <p>開了「譯名 + 原文」之後這種情形不多——附的原文是<b>純多出來</b>的寬度，
     * 譯名再短也只是把整行加長。真正收得進去的是原文本來就很長的那種
     * （前綴、後綴一大串），譯名短到連同括號裡的原文都還比它窄。
     */
    private static void shortEnough(TranslationStore store) {
        List<Component> original = List.of(
                row("Idol of the Everlasting Flame", NAME, " [98.0%]", WEAR));
        List<Component> translated = List.of(row("神像 (Idol)", NAME, " [98.0%]", WEAR));
        List<Component> out = rows(original, translated, store, WIDTH);
        check("放得下就維持一行", out == translated);
    }

    /** F6 選「只要譯名」時沒有括號那一段，再寬也不能拆。 */
    private static void noOriginal(TranslationStore store) {
        List<Component> original = List.of(row("Crosier", NAME, " [51.0%]", WEAR));
        List<Component> translated = List.of(row("燼咒牧杖啊啊啊", NAME, " [51.0%]", WEAR));
        check("沒有附原文就不拆",
              rows(original, translated, store, WIDTH) == translated);
    }

    /** headless 量出來全是 0，寬度那條規則不該被當成「超寬」。 */
    private static void noFont(TranslationStore store) {
        List<Component> original = List.of(
                row("Cindercurse Crosier", NAME, " [51.0%]", WEAR));
        List<Component> translated = List.of(
                row("燼咒牧杖 (Cindercurse Crosier)", NAME, " [51.0%]", WEAR));
        check("量不出寬度就不動",
              rows(original, translated, store, c -> 0) == translated);
    }

    /**
     * 第二行要對齊上一行的名字。
     *
     * <h2>實機回報</h2>
     * 第一版把開頭那截徽記留在第一行、第二行從零開始，畫出來是：
     *
     * <pre>
     *       橡木法杖
     *   (Oak Wood Wand)      ← 貼著最左邊，比名字凸出去一截
     * </pre>
     *
     * <p>徽記不能複製到第二行（會畫兩次），所以墊的是 {@code minecraft:space}
     * 的位移字元，寬度跟那一截一樣。
     */
    private static void lineUp(TranslationStore store) {
        // 實機那一截：位移字元 + 徽記的圖，全都在私用區
        String badge = new StringBuilder()
                .appendCodePoint(0xCFFF0).appendCodePoint(0xD0005).toString();
        List<Component> original = List.of(
                row(badge, NAME, "Cindercurse Crosier", NAME, " [51.0%]", WEAR));
        List<Component> translated = List.of(
                row(badge, NAME, "燼咒牧杖 (Cindercurse Crosier)", NAME,
                    " [51.0%]", WEAR));

        List<Component> out = rows(original, translated, store, WIDTH);
        check("拆成兩行（實際 " + out.size() + " 行）", out.size() == 2);
        if (out.size() != 2) {
            return;
        }
        check("徽記留在第一行", out.get(0).getString().startsWith(badge));
        check("徽記沒有被畫第二次", !out.get(1).getString().contains(badge));
        check("第二行有墊寬（實際 " + WIDTH.applyAsInt(out.get(1)) + " px）",
              WIDTH.applyAsInt(out.get(1))
                      > WIDTH.applyAsInt(Component.literal("(Cindercurse Crosier)")));
    }

    /** 看不見的第 0 行寬度是 0，不能被拆出一行來。 */
    private static void invisibleRow(TranslationStore store) {
        List<Component> original = List.of(
                Component.literal("󟌀Cindercurse Crosier󟌀"),
                row("Cindercurse Crosier", NAME, " [51.0%]", WEAR));
        List<Component> translated = List.of(
                Component.literal("󟌀燼咒牧杖 (Cindercurse Crosier)󟌀"),
                row("燼咒牧杖 (Cindercurse Crosier)", NAME, " [51.0%]", WEAR));
        // 第 0 行的偏移字元把寬度抵成 0，看得見的名字在第 1 行
        ToIntFunction<Component> width =
                c -> c.getString().startsWith("󟌀") ? 0 : WIDTH.applyAsInt(c);

        List<Component> out = rows(original, translated, store, width);
        check("拆的是看得見的那一行（實際 " + out.size() + " 行）", out.size() == 3);
        if (out.size() == 3) {
            check("第 0 行原封不動", out.get(0) == translated.get(0));
            check("原文接在名稱下面（實際 " + out.get(2).getString() + "）",
                  out.get(2).getString().endsWith("(Cindercurse Crosier)"));
        }
    }

    /**
     * 拆完的譯文。
     *
     * <p>置中旗標這裡一律給「不置中」——{@link NameWrap#split} 只是把它跟著補一格，
     * 內容不影響判斷。行數有沒有跟著補，由 {@link #inStep} 單獨釘。
     */
    private static List<Component> rows(List<Component> original,
                                        List<Component> translated,
                                        TranslationStore store,
                                        ToIntFunction<Component> width) {
        return NameWrap.split(original, translated,
                              new boolean[translated.size()], store, width).translated();
    }

    /**
     * 拆出一行之後，原文與置中旗標要跟著補一格。
     *
     * <h2>為什麼</h2>
     * 後面的 {@link TooltipWiden#fit} 三份行數對不起來就整個不做事，而那一步正是
     * 把靠右的欄位收回同一條右緣的地方。對不起來的話，實機會看到
     * 「職業類型停在原本的右緣、戰鬥等級被推到新的右緣」。
     */
    private static void inStep(TranslationStore store) {
        List<Component> original = List.of(
                row("Cindercurse Crosier", NAME, " [51.0%]", WEAR),
                row("Class Type", NAME));
        List<Component> translated = List.of(
                row("燼咒牧杖 (Cindercurse Crosier)", NAME, " [51.0%]", WEAR),
                row("職業類型", NAME));

        NameWrap.Split split = NameWrap.split(original, translated,
                                              new boolean[] {false, true}, store, WIDTH);
        check("譯文多一行（實際 " + split.translated().size() + "）",
              split.translated().size() == 3);
        check("原文跟著補一格（實際 " + split.original().size() + "）",
              split.original().size() == 3);
        check("補的是名稱那一行，當作拆出來那一行的對照",
              split.original().get(1) == original.get(0));
        check("置中旗標也補一格，而且後面那些沒有錯位",
              split.centered().length == 3 && !split.centered()[0]
                      && !split.centered()[1] && split.centered()[2]);
    }

    /** 照遊戲送來的形狀組一行：一段一個顏色。 */
    private static Component row(Object... pairs) {
        net.minecraft.network.chat.MutableComponent out = Component.empty();
        for (int i = 0; i < pairs.length; i += 2) {
            out.append(Component.literal((String) pairs[i]).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb((Integer) pairs[i + 1]))));
        }
        return out;
    }

    /** 第 {@code index} 個字畫出來是什麼顏色。 */
    private static int colourAt(Component line, int index) {
        int[] at = {0};
        int[] found = {-1};
        line.visit((style, text) -> {
            if (found[0] < 0 && at[0] + text.length() > index && style.getColor() != null) {
                found[0] = style.getColor().getValue();
            }
            at[0] += text.length();
            return Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) {
            failures++;
        }
    }
}
