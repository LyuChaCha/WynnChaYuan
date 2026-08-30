package com.wynnchayuan.capture;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * 從原始 action bar 抽對話選項。
 *
 * <h2>釘住的是實機資料，不是猜測</h2>
 * 下面那組片段照抄自 {@code dialogue-choice-probe-1.txt}（Mont，{p} 村口）：
 * <pre>
 *   font=…/text/wynncraft/choice_1  text=Are you headed someplace?
 *   font=…/text/wynncraft/choice_3  text=Just saying hello.
 *   font=…/text/wynncraft/choice_2  text=What's life like here?
 * </pre>
 *
 * <p>兩個關鍵性質都來自這份資料：
 * <ol>
 *   <li><b>送過來的順序不是編號順序</b>（1、3、2）——照出現先後收就會排錯</li>
 *   <li><b>選項一個號碼都沒有</b>——舊的 looksLikeChoice 靠「1.」認選項，
 *       所以它從來沒有成立過</li>
 * </ol>
 */
public final class DialogueChoicesTest {

    private static int failures = 0;

    /** 對話文字用的字型，一個選項一個。 */
    private static Style choiceFont(int index) {
        return font("hud/dialogue/text/wynncraft/choice_" + index);
    }

    private static Style font(String path) {
        return Style.EMPTY.withFont(new FontDescription.Resource(
                Identifier.fromNamespaceAndPath("minecraft", path)));
    }

    public static void main(String[] args) {
        // 1. 實機那一段：三個選項，送來的順序是 1、3、2
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

        // 2. 畫框線的 style/default/choice 不能收——它沒有底線，而且全是排版字元
        MutableComponent styled = Component.empty();
        styled.append(Component.literal("퀂A")
                              .withStyle(font("hud/dialogue/style/default/choice")));
        styled.append(Component.literal("Just saying hello.").withStyle(choiceFont(1)));
        List<String> only = DialogueChoices.of(styled);
        show("含框線字型", only);
        check("框線那個字型不會混進來", only.equals(List.of("Just saying hello.")));

        // 3. 沒有選項的對話
        MutableComponent plain = Component.empty();
        plain.append(Component.literal("Alright fella")
                             .withStyle(font("hud/dialogue/text/wynncraft/body_0")));
        check("沒有選項時回傳空的", DialogueChoices.of(plain).isEmpty());
        check("null 不會炸", DialogueChoices.of(null).isEmpty());

        // 4. 一個選項被切成好幾段（句中換色）要接回去，不是只留最後一段
        MutableComponent split = Component.empty();
        split.append(Component.literal("Are you ").withStyle(choiceFont(1)));
        split.append(Component.literal("headed").withStyle(choiceFont(1)));
        split.append(Component.literal(" someplace?").withStyle(choiceFont(1)));
        List<String> joined = DialogueChoices.of(split);
        show("切成三段的選項", joined);
        check("同一個選項的幾段會接回去",
              joined.equals(List.of("Are you headed someplace?")));

        report();
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
