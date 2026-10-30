package com.wynnchayuan.capture;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class DialogueChoicesTest {

    private static int failures = 0;

    private static Style choiceFont(int index) {
        return font("hud/dialogue/text/wynncraft/choice_" + index);
    }

    private static Style font(String path) {
        return Style.EMPTY.withFont(new FontDescription.Resource(
                Identifier.fromNamespaceAndPath("minecraft", path)));
    }

    public static void main(String[] args) {
        MutableComponent real = Component.empty();
        real.append(Component.literal("Well, this is a sight, for sure! What can we do for")
                            .withStyle(font("hud/dialogue/text/wynncraft/body_0")));
        real.append(Component.literal("you, fella?")
                            .withStyle(font("hud/dialogue/text/wynncraft/body_1")));
        real.append(Component.literal("Are you headed someplace?").withStyle(choiceFont(1)));
        real.append(Component.literal("Just saying hello.").withStyle(choiceFont(3)));
        real.append(Component.literal("What's life like here?").withStyle(choiceFont(2)));
        real.append(Component.literal("Mont")
                            .withStyle(font("hud/dialogue/text/nameplate")));

        List<String> got = DialogueChoices.of(real);
        show("實機那一段", got);
        check("抽到三個選項（實際 " + got.size() + "）", got.size() == 3);
        check("照編號排，不是照送來的先後",
              got.equals(List.of("Are you headed someplace?",
                                 "What's life like here?",
                                 "Just saying hello.")));
        check("NPC 的台詞不會被收進來",
              got.stream().noneMatch(s -> s.contains("sight, for sure")));
        check("名牌不會被收進來",
              got.stream().noneMatch(s -> s.contains("Mont")));

        MutableComponent styled = Component.empty();
        styled.append(Component.literal("퀂A")
                              .withStyle(font("hud/dialogue/style/default/choice")));
        styled.append(Component.literal("Just saying hello.").withStyle(choiceFont(1)));
        List<String> only = DialogueChoices.of(styled);
        show("含框線字型", only);
        check("框線那個字型不會混進來", only.equals(List.of("Just saying hello.")));

        MutableComponent plain = Component.empty();
        plain.append(Component.literal("Alright fella")
                             .withStyle(font("hud/dialogue/text/wynncraft/body_0")));
        check("沒有選項時回傳空的", DialogueChoices.of(plain).isEmpty());
        check("null 不會炸", DialogueChoices.of(null).isEmpty());

        MutableComponent split = Component.empty();
        split.append(Component.literal("Are you ").withStyle(choiceFont(1)));
        split.append(Component.literal("headed").withStyle(choiceFont(1)));
        split.append(Component.literal(" someplace?").withStyle(choiceFont(1)));
        List<String> joined = DialogueChoices.of(split);
        show("切成三段的選項", joined);
        check("同一個選項的幾段會接回去",
              joined.equals(List.of("Are you headed someplace?")));

        check("E080 在第一塊 → 選到第 1 列", DialogueChoices.selected(styleBlocks(1)) == 1);
        check("E080 在第三塊 → 選到第 3 列", DialogueChoices.selected(styleBlocks(3)) == 3);
        check("沒有 E080 就回 0（不標，寧可沒有反白也不要標錯）",
              DialogueChoices.selected(styleBlocks(0)) == 0);
        check("null 不會炸", DialogueChoices.selected(null) == 0);
        check("選項文字那個字型不會被誤認成樣式塊",
              DialogueChoices.selected(real) == 0);

        report();
    }

    private static MutableComponent styleBlocks(int cursorAt) {
        Style style = font("hud/dialogue/style/default/choice");
        MutableComponent out = Component.empty();
        for (int i = 1; i <= 3; i++) {
            out.append(Component.literal(cp(0xD0023)).withStyle(style));
            out.append(Component.literal(i == cursorAt
                    ? cp(0xCFFF5) + " " + cp(0xD0002) + "" + cp(0xD0002)
                    : cp(0xD0002) + "" + cp(0xD0002)).withStyle(style));
            out.append(Component.literal(cp(0xCFF48)).withStyle(style));
        }
        return out;
    }

    private static String cp(int codePoint) {
        return new String(Character.toChars(codePoint));
    }

    private static void show(String what, List<String> got) {
        System.out.println("      " + what + "：" + got);
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures == 0) {
            System.out.println("對話選項：全部通過");
        } else {
            System.out.println("對話選項：" + failures + " 項失敗");
            System.exit(1);
        }
    }
}
