package com.wynnchayuan.render;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

// Wynntils DataComponentHolderMixin 每次讀都發事件
public final class ComponentMemo {

    private ComponentMemo() {
    }

    private static final int SIZE = 4096;

    private record Entry(ItemStack stack, DataComponentType<?> type, long frame, Object value) { }

    public record Hit(Object value) { }

    private static final Entry[] TABLE = new Entry[SIZE];

    private static long frame;
    private static Thread renderThread;
    private static boolean filling;

    public static void nextFrame() {
        frame++;
        renderThread = Thread.currentThread();
    }

    public static Hit lookup(ItemStack stack, DataComponentType<?> type) {
        if (filling || renderThread != Thread.currentThread() || !tracked(type) || !enabled()) {
            return null;
        }
        int slot = (System.identityHashCode(stack) * 31 + System.identityHashCode(type)) & (SIZE - 1);
        Entry hit = TABLE[slot];
        if (hit != null && hit.frame() == frame && hit.stack() == stack && hit.type() == type) {
            return new Hit(hit.value());
        }
        Object value;
        filling = true;
        try {
            value = stack.get(type);
        } finally {
            filling = false;
        }
        TABLE[slot] = new Entry(stack, type, frame, value);
        return new Hit(value);
    }

    private static boolean tracked(DataComponentType<?> type) {
        return type == DataComponents.CUSTOM_MODEL_DATA
                || type == DataComponents.DYED_COLOR
                || type == DataComponents.ENCHANTMENT_GLINT_OVERRIDE
                || type == DataComponents.POTION_CONTENTS;
    }

    private static boolean enabled() {
        com.wynnchayuan.CollectorConfig config = com.wynnchayuan.WynnChaYuan.config();
        return config != null && config.wynntilsFps();
    }
}
