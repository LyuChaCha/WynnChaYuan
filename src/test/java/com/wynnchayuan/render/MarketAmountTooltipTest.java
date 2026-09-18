package com.wynnchayuan.render;

import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 市場「增加數量」按鈕的說明。
 *
 * <h2>實機回報 2026-09-18</h2>
 * 「Increase the amount of this item / you plan to buy」兩行在語料裡<b>各自都有譯文</b>，
 * 畫面上卻整段留在英文（tooltip-partial 記的是 translated=false）。
 */
public final class MarketAmountTooltipTest {

    private static int failures = 0;

    public static void main(String[] args) {
        System.out.println("=== 市場數量按鈕 ===");
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations", "zh_tw"));

        for (String verb : List.of("Increase", "Decrease", "Sets")) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(verb + " Amount"));
            tooltip.add(Component.literal(" "));
            tooltip.add(Component.literal(verb + " the amount of this item"));
            tooltip.add(Component.literal("you plan to buy"));
            tooltip.add(Component.literal(" "));
            tooltip.add(Component.literal("Current: 3"));
            for (int k = 2; k <= 3; k++) {
                Component one = LineTranslator.translate(
                        StyledText.fromComponent(tooltip.get(k)), store);
                System.out.println("  逐行 " + tooltip.get(k).getString() + " -> "
                        + (one == null ? null : one.getString()));
            }
            List<Component> out = TooltipPanel.translateLines(tooltip, store);
            StringBuilder all = new StringBuilder();
            for (Component c : out == null ? List.<Component>of() : out) {
                all.append(c.getString()).append(" / ");
            }
            System.out.println("  整份 " + all);
            report("★ " + verb + "：說明兩行都換成中文",
                   !all.toString().contains("amount of this item")
                   && !all.toString().contains("you plan to buy"));
        }

        if (failures > 0) {
            System.out.println("市場數量按鈕：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("市場數量按鈕：全部通過");
    }

    private static void report(String name, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + name);
        if (!ok) {
            failures++;
        }
    }
}
