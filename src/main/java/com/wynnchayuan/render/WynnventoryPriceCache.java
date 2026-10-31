package com.wynnchayuan.render;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class WynnventoryPriceCache {

    private WynnventoryPriceCache() {
    }

    private static final long TTL_NANOS = 1_000_000_000L;

    private record Entry(ItemStack stack, long at, List<Component> lines) { }

    private static volatile Entry last;

    public static void forget() {
        last = null;
    }

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
