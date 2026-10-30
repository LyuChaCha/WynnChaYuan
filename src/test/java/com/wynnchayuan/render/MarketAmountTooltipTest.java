package com.wynnchayuan.render;

import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

        priceRow(store);
        emeraldRow(store);

        if (failures > 0) {
            System.out.println("市場數量按鈕：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("市場數量按鈕：全部通過");
    }

    private static final int GOLD = 0xFFAA00;
    private static final int AQUA = 0x00AAAA;

    private static void priceRow(TranslationStore store) {
        System.out.println("=== 市場價格那一行 ===");
        String glyphs = new String(Character.toChars(0xCFFFC))
                + "" + new String(Character.toChars(0xCFFFF))
                + "" + new String(Character.toChars(0xCFFFE));
        Component line = lit(glyphs, AQUA).append(lit(" Price", GOLD));
        Component built = LineTranslator.translate(
                StyledText.fromComponent(line), store);
        System.out.println("  譯文 " + (built == null ? "（查不到）" : describe(built)));
        report("★ 標題那一行的字是金的，不是符號那一段的青色",
               built != null && describe(built).contains("價格 #FFAA00"));
    }

    private static final int WHITE = 0xFFFFFF;
    private static final int GREY = 0xAAAAAA;
    private static final int CYAN = 0x55FFFF;
    private static final int DIM = 0x555555;

    private static void emeraldRow(TranslationStore store) {
        System.out.println("=== 市場價格的綠寶石符號 ===");
        String prefix = new String(Character.toChars(0xCFFFC))
                + new String(Character.toChars(0xD0006));
        Component line = lit(prefix, WHITE)
                .append(lit(" ", WHITE))
                .append(lit("4,300", WHITE))
                .append(lit("²", GREY))
                .append(lit(" ", WHITE))
                .append(lit("✮ 4,218", CYAN))
                .append(lit("²", AQUA))
                .append(lit(" ", WHITE))
                .append(lit("(1¼² 1²½ 58²)", DIM))
                .append(lit(" each", GOLD));
        Component built = LineTranslator.translate(
                StyledText.fromComponent(line), store);
        System.out.println("  譯文 " + (built == null ? "（查不到）" : describe(built)));
        report("［綠寶石］整行查得到譯文", built != null);
        if (built == null) {
            return;
        }
        String shown = describe(built);
        report("★［綠寶石］第一顆 ² 是灰的，不是數字的白",
               shown.contains("[² " + hex(GREY) + "]"));
        report("★［綠寶石］第二顆 ² 是暗青的，不是亮青",
               shown.contains("[² " + hex(AQUA) + "]"));
        report("★［綠寶石］星號跟著右邊的數字走，是青的不是底色",
               shown.contains("[✮  " + hex(CYAN) + "]"));
        report("［綠寶石］「每個」仍是金的（語料自己指定的 {c:#FFAA00}）",
               shown.contains("每個 " + hex(GOLD)));
    }

    private static String hex(int colour) {
        return net.minecraft.network.chat.TextColor.fromRgb(colour).toString();
    }

    private static net.minecraft.network.chat.MutableComponent lit(String text, int colour) {
        return Component.literal(text).withStyle(net.minecraft.network.chat.Style.EMPTY
                .withColor(net.minecraft.network.chat.TextColor.fromRgb(colour)));
    }

    private static String describe(Component built) {
        StringBuilder out = new StringBuilder();
        built.visit((style, text) -> {
            out.append('[').append(text).append(' ')
               .append(style.getColor() == null ? "-" : style.getColor().toString())
               .append(']');
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return out.toString();
    }

    private static void report(String name, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + name);
        if (!ok) {
            failures++;
        }
    }
}
