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

        priceRow(store);

        if (failures > 0) {
            System.out.println("市場數量按鈕：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("市場數量按鈕：全部通過");
    }

    private static final int GOLD = 0xFFAA00;
    private static final int AQUA = 0x00AAAA;

    /**
     * 市場那張卡的「Price」那一行是什麼顏色。
     *
     * <h2>實機回報</h2>
     * 原文那一行的 {@code Price} 是金色的，譯文的「價格」卻是青的。
     *
     * <p>那一行在語料裡是<b>自己一條</b>（{@code "{#} Price"}），而且
     * {@code tooltip-partial-3.json} 記著 {@code translated=true}——
     * 也就是走的是逐行那條路。字面取自同一份診斷檔：前面是 chat/prefix
     * 字型的位移符號（符號那一段自己也有顏色），後面才是 {@code " Price"}。
     *
     * <p>符號不參與底色的統計（{@code solidCount} 不數），所以這一行
     * 只有金色有票，畫出來就該是金色。
     */
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
