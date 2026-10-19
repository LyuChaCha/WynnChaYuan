package com.wynnchayuan.render;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

/**
 * 同一幀裡，同一件物品讀同一種資料，只算一次。
 *
 * <h2>為什麼</h2>
 * Wynntils 在 ItemStack 每一次讀 {@code custom_model_data}、染色、附魔光澤、藥水內容時
 * 都發一個事件（它的 {@code DataComponentHolderMixin}），讓物品高亮、耐久度條這些功能改值。
 * 畫一格物品，模型解析、裝飾、數量都會各讀一次，於是同一件物品一幀發好幾次同樣的事件。
 * 2026-10-19 在公會銀行錄的 JFR：這一段佔容器畫面渲染取樣的 15%，大半是事件匯流排本身。
 *
 * <h2>做法</h2>
 * 只在算繪執行緒、只管 Wynntils 會改的那四種資料。第一次讀照常走完（含 Wynntils 的事件），
 * 結果記下來；同一幀再讀直接回答案。每一幀開頭（{@code GameRenderer#render}）換一個幀號，
 * 上一幀的全部作廢——Wynntils 那些功能看的設定、畫面、物品內容在一幀之內不會變。
 *
 * <p>表是固定大小的直接對應：一格一筆不可變的紀錄，撞到就覆蓋。
 *
 * <p>F6「功能」的效能開關關掉時整段不做。
 */
public final class ComponentMemo {

    private ComponentMemo() {
    }

    private static final int SIZE = 4096;

    private record Entry(ItemStack stack, DataComponentType<?> type, long frame, Object value) { }

    /** 命中時的答案（值本身可能是 null，所以包一層）。 */
    public record Hit(Object value) { }

    private static final Entry[] TABLE = new Entry[SIZE];

    private static long frame;
    private static Thread renderThread;
    private static boolean filling;

    /** 每一幀開頭叫一次（算繪執行緒）。 */
    public static void nextFrame() {
        frame++;
        renderThread = Thread.currentThread();
    }

    /**
     * {@code DataComponentHolder#get} 開頭問這裡。
     *
     * @return 這一幀已經算過的答案；不歸這裡管或第一次讀時回 {@code null}，照常往下走
     */
    public static Hit lookup(ItemStack stack, DataComponentType<?> type) {
        if (filling || renderThread != Thread.currentThread() || !tracked(type) || !enabled()) {
            return null;
        }
        int slot = (System.identityHashCode(stack) * 31 + System.identityHashCode(type)) & (SIZE - 1);
        Entry hit = TABLE[slot];
        if (hit != null && hit.frame() == frame && hit.stack() == stack && hit.type() == type) {
            return new Hit(hit.value());
        }
        // 第一次讀：照常走完一次（含 Wynntils 的事件），記下結果。
        // 這中間別的物品的讀取不進表（filling 擋著），免得巢狀的讀取互相覆蓋。
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
