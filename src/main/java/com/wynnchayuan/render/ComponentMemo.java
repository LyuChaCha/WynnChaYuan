package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.WynnChaYuan;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

// Wynntils DataComponentHolderMixin 每次讀都發事件
public final class ComponentMemo {

    private ComponentMemo() {
    }

    private static final int SIZE = 4096;

    private record Entry(ItemStack stack, DataComponentType<?> type, long frame, Hit hit) { }

    public record Hit(Object value) { }

    private static final Entry[] TABLE = new Entry[SIZE];

    private static long frame;
    private static Thread renderThread;
    private static boolean filling;

    public static void forget() {
        Arrays.fill(TABLE, null);
    }

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
            return hit.hit();
        }
        Object value;
        filling = true;
        try {
            value = stack.get(type);
        } finally {
            filling = false;
        }
        Hit fresh = new Hit(value);
        TABLE[slot] = new Entry(stack, type, frame, fresh);
        return fresh;
    }

    private static boolean tracked(DataComponentType<?> type) {
        return type == DataComponents.CUSTOM_MODEL_DATA
                || type == DataComponents.DYED_COLOR
                || type == DataComponents.ENCHANTMENT_GLINT_OVERRIDE
                || type == DataComponents.POTION_CONTENTS;
    }

    private static boolean enabled() {
        ModConfig config = WynnChaYuan.config();
        return config != null && config.wynntilsFps();
    }
}
