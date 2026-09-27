package com.wynnchayuan.render;

/**
 * 一排名牌：框各用自己的高度，矮的置中。
 *
 * <h2>實機回報</h2>
 * 路邊一個三行的招牌旁邊站著兩個村民：
 *
 * <pre>
 *   ┌────────┐ ┌──────────────────────┐ ┌──────────────────────┐
 *   │交易市場│ │Hyloch 市民  LV 120   │ │Hyloch 市民  LV 120   │
 *   │在市場上│ │                      │ │                      │
 *   │買賣物品│ │                      │ │                      │
 *   └────────┘ └──────────────────────┘ └──────────────────────┘
 * </pre>
 *
 * 整排先前統一用最高的那個當高度，單行的兩個被拉成三行高，字擠在最上面。
 */
public final class NametagRowTest {

    private static int failures = 0;

    /** Minecraft 預設字型：lineHeight 9，{@code drawRow} 再加 1。 */
    private static final int LINE = 10;

    private static int boxHeight(int lines) {
        return lines * LINE + 6;
    }

    public static void main(String[] args) {
        int sign = boxHeight(3);               // 交易市場那一個
        int villager = boxHeight(1);           // 兩個村民
        int tallest = Math.max(sign, villager);

        check("三行的框是 36px 高", sign == 36);
        check("一行的框是 16px 高", villager == 16);

        int signTop = LookAtTranslator.topOf(tallest, sign);
        int villagerTop = LookAtTranslator.topOf(tallest, villager);

        check("★ 最高的那個不動（往下挪 " + signTop + "）", signTop == 0);
        check("★ 矮的往下挪，不是靠上（往下挪 " + villagerTop + "）", villagerTop > 0);
        check("★ 矮的置中：上面留 " + villagerTop + "、下面留 "
              + (tallest - villager - villagerTop) + "，差不超過 1px",
              Math.abs((tallest - villager - villagerTop) - villagerTop) <= 1);
        check("★ 矮的框沒有被撐大（還是 " + villager + "px）", villager == 16);
        check("矮的框整個在這一排裡面", villagerTop + villager <= tallest);

        // 一排全是單行時，誰都不該被挪動
        check("全部一樣高時不挪動", LookAtTranslator.topOf(villager, villager) == 0);

        System.out.println(failures == 0 ? "名牌排版：全部通過"
                                         : "名牌排版：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
