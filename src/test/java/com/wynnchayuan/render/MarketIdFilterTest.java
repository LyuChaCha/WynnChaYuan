package com.wynnchayuan.render;

import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class MarketIdFilterTest {

    private static final int GREEN = 0x55FF55;
    private static final int GREY = 0xAAAAAA;

    private static final String[][] ROWS = {
            {"Sprint Regen Identification Only", "Must have the Sprint Regen", "identification"},
            {"Critical Damage Identification Only", "Must have the Critical Damage", "identification"},
            {"Health Identification Only", "Must have the Health identification"},
            {"Health Identification Only", "Must have the Health", "identification"},
            {"Elemental Main Attack Damage Identification Only",
                    "Must have the Elemental Main Attack", "Damage identification"},
            {"Elemental Main Attack Damage Identification Only",
                    "Must have the Elemental Main Attack Damage", "identification"},
            {"\uE001Strength Identification Only", "Must have the \uE001Strength", "identification"},
            {"3rd Spell Cost Identification Only", "Must have the {sp3} Cost", "identification"},
    };

    public static void main(String[] args) {
        for (String lang : List.of("zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr")) {
            System.out.println("=== 市場詞條篩選 " + lang + " ===");
            TranslationStore store = new TranslationStore();
            store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations", lang));
            for (String[] row : ROWS) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(row(row[0], GREEN));
                tooltip.add(Component.literal(" "));
                for (int i = 1; i < row.length; i++) {
                    tooltip.add(row(row[i], GREY));
                }
                List<Component> out = TooltipPanel.translateLines(tooltip, store);
                StringBuilder all = new StringBuilder();
                for (Component c : out == null ? List.<Component>of() : out) {
                    all.append(c.getString()).append(" / ");
                }
                String shown = all.toString();
                System.out.println("  " + shown);
                check(lang + "：" + row[1] + " 有譯文", out != null);
                check(lang + "：" + String.join(" / ", row).substring(row[0].length() + 3) + " 沒有留英文",
                        !shown.contains("Must have") && !shown.contains("identification")
                        && !shown.contains("Identification"));
                check(lang + "：數字照原文填回", !row[1].contains("{sp3}") || shown.contains("3"));
            }
        }
        if (failures > 0) {
            System.out.println("市場詞條篩選：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("市場詞條篩選：全部通過");
    }

    private static MutableComponent row(String text, int colour) {
        int icon = text.indexOf('\uE001');
        if (icon < 0) {
            return lit(text, colour);
        }
        return lit(text.substring(0, icon), colour)
                .append(lit(text.substring(icon, icon + 1), 0xFFAA00))
                .append(lit(text.substring(icon + 1), colour));
    }

    private static MutableComponent lit(String text, int colour) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }
}
