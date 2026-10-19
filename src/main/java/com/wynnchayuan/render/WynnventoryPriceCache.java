package com.wynnchayuan.render;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Wynnventory 價格框的內容，同一件物品一秒內只算一次。
 *
 * <h2>為什麼</h2>
 * 滑鼠指著物品時，它每一幀都把那件物品重新解析（Wynntils 的裝備解析、它自己的交易市場快照、
 * 價格預測），2026-10-19 在背包錄的 JFR 裡，指著物品的那幾秒它佔渲染取樣的兩成。
 * 內容只會在伺服器的價格資料回來時變，一秒更新一次就夠。
 *
 * <p>照物品物件身分記最近的一件（指著的那一件），超過一秒或換了一件就重算。
 * F6「功能」的效能開關關掉時不做。
 */
public final class WynnventoryPriceCache {

    private WynnventoryPriceCache() {
    }

    private static final long TTL_NANOS = 1_000_000_000L;

    private record Entry(ItemStack stack, long at, List<Component> lines) { }

    private static volatile Entry last;

    /** 這一件一秒內算過的結果；沒有就回 {@code null}。 */
    public static List<Component> get(ItemStack stack) {
        Entry hit = last;
        if (hit == null || hit.stack() != stack || !enabled()
                || System.nanoTime() - hit.at() > TTL_NANOS) {
            return null;
        }
        return hit.lines();
    }

    public static void put(ItemStack stack, List<Component> lines) {
        if (stack == null || lines == null || !enabled()) {
            return;
        }
        last = new Entry(stack, System.nanoTime(), List.copyOf(lines));
    }

    private static boolean enabled() {
        com.wynnchayuan.CollectorConfig config = com.wynnchayuan.WynnChaYuan.config();
        return config != null && config.wynntilsFps();
    }
}
