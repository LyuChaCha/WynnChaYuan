package com.wynnchayuan.translate;

/**
 * WynnMarketSearch 的搜尋框打譯名找不找得到。
 *
 * <p>真正掛進那個模組的是 mixin，要實機才跑得到；這裡測的是它轉過來問的那一句
 * （{@link WmsBridge#matches}）答得對不對，以及幾個不能出事的地方：功能關著時
 * 不插手、索引壞掉時不丟例外——那段程式跑在別人的搜尋迴圈裡，丟出去會把它弄壞。
 */
public final class WmsBridgeTest {

    private static int failures = 0;

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    public static void main(String[] args) {
        MarketSearch market = new MarketSearch();
        market.add("Bony Bow", "骨弓");
        market.add("Corkian Amplifier III", "Corkian 增幅器 III");
        market.add("Heart of the Forest", "森林之心");
        market.add("Heart of Ice", "寒冰之心");
        market.add("Leather Boots", "皮靴");
        boolean[] on = {true};
        WmsBridge.source = () -> market;
        WmsBridge.enabled = () -> on[0];

        System.out.println("打譯名");
        check("整個譯名：找得到那一件", WmsBridge.matches("Bony Bow", "骨弓"));
        check("別的物品不會被拉進來", !WmsBridge.matches("Heart of Ice", "骨弓"));
        check("打一半（之心）：兩件都算符合",
                WmsBridge.matches("Heart of the Forest", "之心")
                        && WmsBridge.matches("Heart of Ice", "之心"));
        check("打一半時不相干的不算", !WmsBridge.matches("Bony Bow", "之心"));
        check("那個模組的名字大小寫不同也認得", WmsBridge.matches("bony bow", "骨弓"));
        check("譯名夾著英文與空白（Corkian 增幅器）", WmsBridge.matches(
                "Corkian Amplifier III", "corkian增幅器"));
        check("★ 複數的名字（Boots）：索引收成單數，比的時候也要收",
                WmsBridge.matches("Leather Boots", "皮靴"));

        System.out.println("不該插手的時候");
        check("打英文：我們沒有意見，照它原本的規則", !WmsBridge.matches("Bony Bow", "bony"));
        check("只打一個字不查", !WmsBridge.matches("Bony Bow", "骨"));
        check("空的搜尋字", !WmsBridge.matches("Bony Bow", "") && !WmsBridge.matches("Bony Bow", null));
        check("物品沒有名字", !WmsBridge.matches(null, "骨弓"));
        on[0] = false;
        check("★「市集搜尋轉英文」關著：完全不插手", !WmsBridge.matches("Bony Bow", "骨弓"));
        check("關著的時候也不畫譯名", WmsBridge.translatedName("Bony Bow") == null);
        on[0] = true;

        System.out.println("譯名那一行");
        check("英文名查得到譯名", "骨弓".equals(WmsBridge.translatedName("Bony Bow")));
        check("複數的名字也查得到", "皮靴".equals(WmsBridge.translatedName("Leather Boots")));
        check("索引裡沒有的回 null", WmsBridge.translatedName("Morph-Stardust") == null);

        System.out.println("索引變了");
        check("加東西之前找不到", !WmsBridge.matches("Morph-Stardust", "星塵"));
        market.add("Morph-Stardust", "變形-星塵");
        check("★ 加了之後同一個搜尋字要重新算，不能拿舊答案", WmsBridge.matches("Morph-Stardust", "星塵"));
        check("新加的也有譯名那一行", "變形-星塵".equals(WmsBridge.translatedName("Morph-Stardust")));

        System.out.println("壞掉的時候");
        WmsBridge.source = () -> {
            throw new IllegalStateException("索引正在重新載入");
        };
        check("★ 問索引時丟例外：答「沒有意見」，不往外丟", !WmsBridge.matches("Bony Bow", "骨弓"));
        check("譯名那一行也不往外丟", WmsBridge.translatedName("Bony Bow") == null);
        WmsBridge.source = () -> null;
        check("索引還沒建好（null）", !WmsBridge.matches("Bony Bow", "骨弓"));

        if (failures > 0) {
            System.out.println("WmsBridge: " + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("WmsBridge: 全部通過");
    }
}
