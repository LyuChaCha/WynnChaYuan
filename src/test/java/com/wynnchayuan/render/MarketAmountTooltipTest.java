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
        emeraldRow(store);

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

    private static final int WHITE = 0xFFFFFF;
    private static final int GREY = 0xAAAAAA;
    private static final int CYAN = 0x55FFFF;
    private static final int DIM = 0x555555;

    /**
     * 價格那一行的綠寶石符號該保留<b>自己的</b>顏色。
     *
     * <h2>實機回報</h2>
     * 原文 {@code 4,300² ✮ 4,218² (1¼² 1²½ 58²) each} 切成六段，而兩顆
     * {@code ²} 各自比前面的數字暗一階：白配灰、亮青配暗青。譯文兩顆都跟著
     * 數字走，亮了一階。診斷檔 {@code majorid-debug.txt} 記的是
     * 「{@code ²} ★在譯文裡卻沒貼上」兩次。
     *
     * <h2>怎麼壞的</h2>
     * 語料寫的是 {@code {~}²}——{@code ²} 緊貼著數值佔位符，於是
     * {@code appendHugging} 把它當成「跟數值同屬一段的標點」，拿數值的樣式畫。
     * 但它在原文裡<b>本來就是自己一段</b>，而那一段已經登記成重點段了。
     * 見 {@code LineTranslator#hasOwnColour}。
     */
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
        // 原文 `✮ 4,218` 是一整段青色——星號跟它右邊的數字同屬一段。
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
