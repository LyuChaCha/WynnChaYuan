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

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class NameWrapTest {

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
        Files.writeString(dir.resolve("gear-weapon.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "a1": {"src": "Cindercurse Crosier", "dst": "燼咒牧杖", "role": "name"},
                   "a2": {"src": "Idol", "dst": "神像", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        store.setNameMode(com.wynnchayuan.ModConfig.ItemNames.BOTH);

        tooLong(store);
        shortEnough(store);
        noOriginal(store);
        noFont(store);
        lineUp(store);
        inStep(store);
        invisibleRow(store);
        fitsTheCard(store);
        fewerRows(store);

        System.out.println(failures == 0
                ? "NameWrapTest 全部通過" : failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

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

    private static void shortEnough(TranslationStore store) {
        List<Component> original = List.of(
                row("Idol of the Everlasting Flame", NAME, " [98.0%]", WEAR));
        List<Component> translated = List.of(row("神像 (Idol)", NAME, " [98.0%]", WEAR));
        List<Component> out = rows(original, translated, store, WIDTH);
        check("放得下就維持一行", out == translated);
    }

    private static void noOriginal(TranslationStore store) {
        List<Component> original = List.of(row("Crosier", NAME, " [51.0%]", WEAR));
        List<Component> translated = List.of(row("燼咒牧杖啊啊啊", NAME, " [51.0%]", WEAR));
        check("沒有附原文就不拆",
              rows(original, translated, store, WIDTH) == translated);
    }

    private static void noFont(TranslationStore store) {
        List<Component> original = List.of(
                row("Cindercurse Crosier", NAME, " [51.0%]", WEAR));
        List<Component> translated = List.of(
                row("燼咒牧杖 (Cindercurse Crosier)", NAME, " [51.0%]", WEAR));
        check("量不出寬度就不動",
              rows(original, translated, store, c -> 0) == translated);
    }

    private static void lineUp(TranslationStore store) {
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

    private static void invisibleRow(TranslationStore store) {
        List<Component> original = List.of(
                Component.literal("󟌀Cindercurse Crosier󟌀"),
                row("Cindercurse Crosier", NAME, " [51.0%]", WEAR));
        List<Component> translated = List.of(
                Component.literal("󟌀燼咒牧杖 (Cindercurse Crosier)󟌀"),
                row("燼咒牧杖 (Cindercurse Crosier)", NAME, " [51.0%]", WEAR));
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

    private static List<Component> rows(List<Component> original,
                                        List<Component> translated,
                                        TranslationStore store,
                                        ToIntFunction<Component> width) {
        return NameWrap.split(original, translated,
                              new boolean[translated.size()], store, width).translated();
    }

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

    private static Component row(Object... pairs) {
        net.minecraft.network.chat.MutableComponent out = Component.empty();
        for (int i = 0; i < pairs.length; i += 2) {
            out.append(Component.literal((String) pairs[i]).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb((Integer) pairs[i + 1]))));
        }
        return out;
    }

    private static void fitsTheCard(TranslationStore store) {
        List<Component> original = List.of(
                row("Idol", NAME),
                row("Class Type    Archer/Hunter", NAME));
        List<Component> translated = List.of(
                row("神像 (Idol)", NAME),
                row("職業類型  弓箭手/獵人", NAME));
        List<Component> out = NameWrap.split(original, translated,
                new boolean[] {false, false}, store, WIDTH).translated();
        check("★ 名字比同一行的原文寬，但收得進整份 -> 不拆（實際 "
                        + out.size() + " 行）",
              out == translated);

        List<Component> wide = List.of(
                row("燼咒牧杖 (Cindercurse Crosier) 還要更長一點才撐得破", NAME),
                row("職業類型  弓箭手/獵人", NAME));
        List<Component> split = NameWrap.split(original, wide,
                new boolean[] {false, false}, store, WIDTH).translated();
        check("連整份都放不下的照拆（實際 " + split.size() + " 行）",
              split.size() == 3);
    }

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

    private static void fewerRows(TranslationStore store) {
        List<Component> original = List.of(
                row("", NAME),
                row("Cindercurse Crosier", NAME),
                row("This item's power has", NAME),
                row("been sealed, an Item", NAME),
                row("Identifier can unlock it.", NAME),
                row("Combat Level        105", NAME));
        List<Component> translated = List.of(
                row("", NAME),
                row("燼咒牧杖 (Cindercurse Crosier)", NAME),
                row("這件物品的力量已被封印，", NAME),
                row("物品鑑定師可以解放它。", NAME),
                row("戰鬥等級            105", NAME));
        boolean[] centered = {false, false, true, true, true, false};

        NameWrap.Split split = NameWrap.split(original, translated, centered, store, WIDTH);
        List<Component> out = split.translated();
        check("★ 行數對不上也拆了（實際 " + out.size() + " 行）", out.size() == 6);
        if (out.size() != 6) {
            return;
        }
        check("名稱留在原本那一行（實際 " + out.get(1).getString() + "）",
              "燼咒牧杖".equals(out.get(1).getString()));
        check("原文自己一行（實際 " + out.get(2).getString() + "）",
              out.get(2).getString().strip().equals("(Cindercurse Crosier)"));
        check("後面的敘述沒有被動到", out.get(3) == translated.get(2)
                && out.get(5) == translated.get(4));
        check("原文那一份也補了一行", split.original().size() == 7);
        check("置中旗標照自己的長度補一格、後面沒有錯位",
              split.centered().length == 7 && !split.centered()[1] && !split.centered()[2]
                      && split.centered()[3] && !split.centered()[6]);

        List<Component> later = List.of(
                row("", NAME),
                row("燼咒牧杖", NAME),
                row("這件物品的力量已被封印。", NAME),
                row("神像 (Idol) 這個名字在敘述裡出現，而且這一行特別特別特別長", NAME),
                row("戰鬥等級            105", NAME));
        check("名稱以外的行不動", NameWrap.split(original, later, centered, store, WIDTH)
                .translated() == later);
    }

}
