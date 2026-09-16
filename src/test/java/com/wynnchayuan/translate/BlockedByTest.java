package com.wynnchayuan.translate;

import com.wynnchayuan.render.TooltipPanel;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 技能樹節點的「Blocked by:」清單。
 *
 * <h2>玩家回報</h2>
 * 同一份清單裡，有的技能名換成了中文（「刃嵐」「煙幕」），有的還是英文
 * （「Duplicity」「Beast Lore」）——而那幾個名字在語料裡<b>都翻好了</b>。
 *
 * <p>單獨一行拿去翻是會翻出來的，所以問題不在語料也不在逐行那條路，
 * 而在<b>整份 tooltip</b> 的判斷：撞名的裝備守門會把「- Duplicity」這種
 * 「整行就是一個還沒翻的裝備名」擋下來，只有認出這是技能面板時才放行
 * （見 {@code TooltipPanel#isAbilityNode}）。認不出來就一起擋掉。
 */
public final class BlockedByTest {

    private static int failures = 0;

    private static final int RED = 0xFF5555;
    private static final int GREY = 0xAAAAAA;
    private static final int WHITE = 0xFFFFFF;

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                Languages.DEFAULT));

        alone(store);
        wallOfSmoke(store);
        focus(store);

        System.out.println(failures == 0 ? "\n阻擋清單：全部通過"
                : "\n阻擋清單：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** 單獨一行——這一條本來就過，留著當對照組。 */
    private static void alone(TranslationStore store) {
        for (String[] row : new String[][] {
                {"- Lacerate", "刃嵐"}, {"- Duplicity", "疊映"},
                {"- Beast Lore", "野獸之主"}}) {
            Component out = LineTranslator.translate(
                    StyledText.fromComponent(lit(row[0], RED)), store);
            String shown = out == null ? "（沒翻）" : out.getString();
            check("單獨一行 " + row[0] + " -> " + shown, shown.contains(row[1]));
        }
    }

    /**
     * 實機那份「Wall of Smoke」節點。Duplicity 同時是刺客技能與一把還沒翻的武器，
     * 所以它是撞名守門唯一擋得到的那一行。
     */
    private static void wallOfSmoke(TranslationStore store) {
        List<Component> tip = new ArrayList<>();
        tip.add(lit("Wall of Smoke", 0xFF55FF));
        tip.add(lit("", WHITE));
        tip.add(lit("Smoke Bomb will throw +2 bombs.", GREY));
        tip.add(lit("♥ Total Damage: -10% (of your DPS)", GREY));
        tip.add(lit("(✤ Damage: -10%)", GREY));
        tip.add(lit("", WHITE));
        tip.add(lit("Blocked by:", RED));
        tip.add(lit("- Duplicity", RED));
        tip.add(lit("", WHITE));
        tip.add(lit("Blocked by another ability", RED));
        shows(store, tip, "Wall of Smoke 節點", "疊映");
    }

    /** 實機那份「Focus」節點：標題與被擋技能都留在英文。 */
    private static void focus(TranslationStore store) {
        List<Component> tip = new ArrayList<>();
        tip.add(lit("Focus", RED));
        tip.add(lit("", WHITE));
        tip.add(lit("When hitting an aggressive enemy 5+", GREY));
        tip.add(lit("blocks away, gain +1 Focus. (Max 3)", GREY));
        tip.add(lit("(Lose 50% Focus if you miss once)", GREY));
        tip.add(lit("", WHITE));
        tip.add(lit("♥ Damage Bonus: +15% (per Focus)", GREY));
        tip.add(lit("↻ Cooldown: 1s", GREY));
        tip.add(lit("", WHITE));
        tip.add(lit("Blocked by:", RED));
        tip.add(lit("- Beast Lore", RED));
        shows(store, tip, "Focus 節點", "野獸之主");
    }

    private static void shows(TranslationStore store, List<Component> tip,
                              String what, String want) {
        List<Component> out = TooltipPanel.translateLines(tip, store);
        StringBuilder all = new StringBuilder();
        for (Component line : out) {
            all.append(line.getString()).append('\n');
        }
        check(what + " 的被擋技能是中文（實際看到 "
                        + lastLine(all.toString()) + "）",
              all.toString().contains(want));
    }

    private static String lastLine(String text) {
        String[] rows = text.strip().split("\n");
        return rows.length == 0 ? "" : rows[rows.length - 1].strip();
    }

    private static Component lit(String text, int colour) {
        return Component.literal(text)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
