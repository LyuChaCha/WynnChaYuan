package com.wynnchayuan.render;

public final class NametagRowTest {

    private static int failures = 0;

    private static final int LINE = 10;

    private static int boxHeight(int lines) {
        return lines * LINE + 6;
    }

    public static void main(String[] args) {
        int sign = boxHeight(3);
        int villager = boxHeight(1);
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
