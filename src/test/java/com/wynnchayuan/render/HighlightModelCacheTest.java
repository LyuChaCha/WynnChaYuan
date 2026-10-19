package com.wynnchayuan.render;

import net.minecraft.world.item.component.CustomModelData;

import java.util.List;

/**
 * Wynntils 物品高亮的快取（{@link HighlightModelCache}）：算出來的要跟 Wynntils 原本的規則一樣。
 * 規則抄自 Wynntils 4.2.8 的 {@code ItemHighlightFeature.onGetModelData}：前綴是
 * item_tier／profession_ingredient／profession_material／store_tier 的字串換成空字串，
 * 選了「隱藏專業星等」再加上 profession_tier；位置不變，其餘欄位原樣。
 */
public final class HighlightModelCacheTest {

    private static int failures = 0;

    public static void main(String[] args) {
        HighlightModelCache.useKeys(
                List.of("item_tier", "profession_ingredient", "profession_material", "store_tier"),
                "profession_tier");
        CustomModelData gear = new CustomModelData(List.of(1f, 7f), List.of(true),
                List.of("item_tier_mythic", "frame_spear", "profession_tier_2", ""), List.of(0xFF00FF));

        CustomModelData shown = HighlightModelCache.compute(gear, false);
        check("★ 稀有度字串換成空字串、位置不變（實際 " + shown.strings() + "）",
              shown.strings().equals(List.of("", "frame_spear", "profession_tier_2", "")));
        check("其餘欄位原樣", shown.floats().equals(gear.floats()) && shown.flags().equals(gear.flags())
                && shown.colors().equals(gear.colors()));

        CustomModelData hidden = HighlightModelCache.compute(gear, true);
        check("★ 隱藏專業星等時連星等一起換（實際 " + hidden.strings() + "）",
              hidden.strings().equals(List.of("", "frame_spear", "", "")));

        CustomModelData plain = new CustomModelData(List.of(), List.of(), List.of("frame_wand"), List.of());
        check("沒有要換的回傳同一個物件（Wynntils 這時不呼叫 setValue）",
              HighlightModelCache.compute(plain, true) == plain);

        if (failures > 0) {
            System.out.println("物品高亮快取：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("物品高亮快取：全部通過");
    }

    private static void check(String what, boolean ok) {
        if (ok) {
            System.out.println("  [ok] " + what);
        } else {
            System.out.println("  [FAIL] " + what);
            failures++;
        }
    }
}
