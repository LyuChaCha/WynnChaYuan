package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class DividerBlockTest {

    private static int failures = 0;

    private static final String SPACE = "space";
    private static final String DIVIDER_FONT = "tooltip/divider";
    private static final String ICON = "common";
    private static final String TEXT = "language/wynncraft";

    private static Style font(String id) {
        return Style.EMPTY.withFont(new FontDescription.Resource(
                Identifier.withDefaultNamespace(id)));
    }

    private static MutableComponent part(String text, String fontId) {
        return Component.literal(text).withStyle(font(fontId));
    }

    private static MutableComponent divider() {
        MutableComponent c = Component.empty();
        c.append(part("", SPACE));
        c.append(part("", DIVIDER_FONT));
        return c;
    }

    private static MutableComponent statRow() {
        MutableComponent c = Component.empty();
        c.append(part("", ICON));
        c.append(part(" Boss Altars Won", TEXT));
        c.append(part("", SPACE));
        c.append(part("0", TEXT));
        c.append(part(" ", TEXT));
        c.append(part("", ICON));
        c.append(part("", DIVIDER_FONT));
        c.append(part("", ICON));
        return c;
    }

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        check("★ 攤平過的舊鍵還在語料裡（不然下面測的是空氣）",
              store.lookup("{#}{#} Boss Altars Won{#}{~} {#}{#}{#}") != null);

        String dividerTemplate = com.wynnchayuan.capture.LineParts
                .of(StyledText.fromComponent(divider())).template();
        String rowTemplate = com.wynnchayuan.capture.LineParts
                .of(StyledText.fromComponent(statRow())).template();
        check("★ 分隔線的模板是 {#}{#}（實際 " + dividerTemplate + "）",
              "{#}{#}".equals(dividerTemplate));
        check("★ 統計列的模板跟實機一樣（實際 " + rowTemplate + "）",
              "{#} Boss Altars Won{#}{~} {#}{#}{#}".equals(rowTemplate));

        List<Component> in = new ArrayList<>();
        in.add(divider());
        in.add(statRow());
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(in, store);

        check("★ 分隔線那一行還在（實際 " + out.size() + " 行）", out.size() == 2);
        if (out.size() == 2) {
            check("★ 分隔線沒被吃掉（實際「" + out.get(0).getString() + "」）",
                  !out.get(0).getString().contains("首領祭壇"));
            check("統計列翻出來了（實際「" + out.get(1).getString() + "」）",
                  out.get(1).getString().contains("首領祭壇通關數"));
        }

        String key = "{#}{#}\nThe Purifying Light will continually move around the area.\n"
                + "Guide the Holder of the Decaying Crystal towards it\n"
                + "in order to progress through each of the {~} floors.";
        check("★ 精確查表那條路沒被擋掉（raid.json 第一行就是分隔線）",
              store.lookup(key) != null);
        List<Component> raid = new ArrayList<>();
        raid.add(divider());
        raid.add(part("The Purifying Light will continually move around the area.", TEXT));
        raid.add(part("Guide the Holder of the Decaying Crystal towards it", TEXT));
        raid.add(part("in order to progress through each of the 3 floors.", TEXT));
        StringBuilder zh = new StringBuilder();
        for (Component c : com.wynnchayuan.render.TooltipPanel.translateLines(raid, store)) {
            zh.append(c.getString()).append('\n');
        }
        check("整段命中的那種照舊翻得出來（實際「"
                        + zh.toString().replace('\n', '/').stripTrailing() + "」）",
              zh.toString().contains("淨化之光"));

        weeklyObjectives(store);

        System.out.println(failures == 0
                ? "分隔線：全部通過" : "分隔線：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void weeklyObjectives(TranslationStore store) {
        List<Component> in = new ArrayList<>();
        for (String line : new String[] {
                "Weekly Objectives",
                "Complete weekly objectives", "to win prizes for your guild", "and for yourself.",
                " ",
                "Current Guild Goal: 5/15",
                " ",
                "Goal Rewards:", "- 2 Banner Points", "- 4 Tomes", "- 2048 Emeralds",
                "- 151184633 Guild XP",
                " ",
                "Your Objective:", "- Gather Fish: 0/200", "- Streak: 0",
                "- Reward Multiplier: 1x",
                " ",
                "Complete personal objectives", "in a streak to receive", "better rewards!"}) {
            in.add(Component.literal(line));
        }
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(in, store);
        StringBuilder shown = new StringBuilder();
        for (Component c : out) {
            shown.append('[').append(c.getString()).append(']');
        }
        check("★ 每週目標畫出 17 行（實際 " + out.size() + "：" + shown + "）",
              out.size() == 17);
        if (out.size() != 17) {
            return;
        }
        for (int row : new int[] {2, 4, 10, 15}) {
            check("★ 第 " + row + " 行是空行（實際「" + out.get(row).getString() + "」）",
                  out.get(row).getString().isBlank());
        }
        check("併起來的說明在第 1 行（實際「" + out.get(1).getString() + "」）",
              out.get(1).getString().contains("每週目標"));
        check("★ 目前公會目標後面隔一個空行才是目標獎勵（實際「"
                        + out.get(3).getString() + "」「" + out.get(5).getString() + "」）",
              out.get(3).getString().contains("目前公會目標")
                      && out.get(5).getString().contains("目標獎勵"));
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
