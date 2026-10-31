package com.wynnchayuan.translate;

import com.wynnchayuan.render.TooltipPanel;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class BlockedByTest {

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

}
