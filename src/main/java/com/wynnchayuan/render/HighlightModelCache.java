package com.wynnchayuan.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.component.CustomModelData;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Wynntils「物品高亮」每次讀物品模型資料時做的那一步，結果記起來。
 *
 * <h2>為什麼</h2>
 * Wynntils 在每一次讀 ItemStack 的 {@code custom_model_data} 時都發一個事件
 * （它的 {@code DataComponentHolderMixin}），物品高亮的 {@code onGetModelData} 收到之後
 * 把稀有度那幾個字串換成空字串：開一條 stream、逐字比前綴、組一份新清單再比一次。
 * 一格物品一幀要讀好幾次，容器開著時每一幀上百次。2026-10-15 在市集錄的 JFR 裡，
 * 這一步佔渲染執行緒取樣的 8%（Wynntils 全部是 58%）。
 *
 * <h2>做法</h2>
 * 結果只取決於原本那份模型資料與「隱藏專業星等」這個設定。模型資料是不可變的 record，
 * 同一件物品每次讀到的是<b>同一個物件</b>，所以照物件身分快取：命中就直接回答案，
 * 沒命中才照 Wynntils 原本的規則算一次。算法照抄它的（換掉的前綴從它的常數讀），
 * 畫面跟原本一模一樣。
 *
 * <p>快取是固定大小的直接對應表：一格放一筆不可變的紀錄，寫入只是換一個參考，
 * 別的執行緒同時讀也只會讀到舊的或新的那一筆，不會讀到一半。
 *
 * <p>讀不到 Wynntils 的欄位（它改版了）就整個停用，交回它原本的方法。
 */
public final class HighlightModelCache {

    private HighlightModelCache() {
    }

    private static final int SIZE = 1024;

    /** 一筆：原本那份、當時「隱藏專業星等」的值、換好的那份（沒有要換的時候是原本那份）。 */
    private record Entry(CustomModelData key, boolean hideStar, CustomModelData value) { }

    private static final Entry[] TABLE = new Entry[SIZE];

    private static volatile boolean broken;
    private static List<String> highlightKeys;
    private static String starKey;
    private static Field hideStarField;
    private static java.lang.reflect.Method configGet;
    private static Class<?> guideScreen;

    /**
     * 物品高亮的 {@code onGetModelData} 開頭問這裡。
     *
     * @param feature  那個 Wynntils 功能本身（讀它的設定）
     * @param original 事件帶來的原本那份模型資料
     * @return 要換成的那份（跟原本相同就回原本那個物件）；回 {@code null} 代表不處理，
     *         交給 Wynntils 原本的方法
     */
    public static CustomModelData lookup(Object feature, CustomModelData original) {
        if (broken || original == null || !enabled() || !ready(feature)) {
            return null;
        }
        try {
            // 跟 Wynntils 一樣：導覽畫面裡不動
            if (guideScreen != null && guideScreen.isInstance(Minecraft.getInstance().screen)) {
                return null;
            }
            boolean hide = hideStar(feature);
            int slot = System.identityHashCode(original) & (SIZE - 1);
            Entry hit = TABLE[slot];
            if (hit != null && hit.key() == original && hit.hideStar() == hide) {
                return hit.value();
            }
            CustomModelData value = compute(original, hide);
            TABLE[slot] = new Entry(original, hide, value);
            return value;
        } catch (Throwable t) {
            broken = true;
            return null;
        }
    }

    /** 照 Wynntils 的規則：稀有度那幾個前綴（與選擇隱藏時的專業星等）換成空字串，位置不變。 */
    static CustomModelData compute(CustomModelData original, boolean hideStar) {
        List<String> strings = original.strings();
        List<String> out = null;
        for (int i = 0; i < strings.size(); i++) {
            String s = strings.get(i);
            if (!s.isEmpty() && hides(s, hideStar)) {
                if (out == null) {
                    out = new ArrayList<>(strings);
                }
                out.set(i, "");
            }
        }
        if (out == null) {
            return original;
        }
        return new CustomModelData(original.floats(), original.flags(), List.copyOf(out),
                                   original.colors());
    }

    private static boolean hides(String s, boolean hideStar) {
        for (String key : highlightKeys) {
            if (s.startsWith(key)) {
                return true;
            }
        }
        return hideStar && starKey != null && s.startsWith(starKey);
    }

    /** 測試用：直接給前綴。 */
    static void useKeys(List<String> keys, String star) {
        highlightKeys = List.copyOf(keys);
        starKey = star;
    }

    private static boolean enabled() {
        com.wynnchayuan.CollectorConfig config = com.wynnchayuan.WynnChaYuan.config();
        return config == null || config.wynntilsFps();
    }

    /** 「隱藏專業星等」讀一次就記半秒：每次讀模型資料都走反射，反射本身成了熱點（JFR 1%）。 */
    private static boolean hideCached;
    private static long hideReadAt;

    private static boolean hideStar(Object feature) throws ReflectiveOperationException {
        long now = System.nanoTime();
        if (hideReadAt == 0 || now - hideReadAt > 500_000_000L) {
            hideCached = Boolean.TRUE.equals(configGet.invoke(hideStarField.get(feature)));
            hideReadAt = now;
        }
        return hideCached;
    }

    @SuppressWarnings("unchecked")
    private static synchronized boolean ready(Object feature) {
        if (hideStarField != null) {
            return true;
        }
        if (broken || feature == null) {
            return false;
        }
        try {
            Class<?> type = feature.getClass();
            Field keys = type.getDeclaredField("DEFAULT_HIGHLIGHT_KEYS");
            Field star = type.getDeclaredField("PROFESSION_STAR_KEY");
            Field hide = type.getDeclaredField("hideProfessionStar");
            keys.setAccessible(true);
            star.setAccessible(true);
            hide.setAccessible(true);
            highlightKeys = List.copyOf((List<String>) keys.get(null));
            starKey = (String) star.get(null);
            try {
                guideScreen = Class.forName("com.wynntils.screens.guides.WynntilsGuideScreen");
            } catch (ClassNotFoundException e) {
                guideScreen = null;
            }
            configGet = hide.getType().getMethod("get");
            hideStarField = hide;
            return true;
        } catch (Throwable t) {
            System.out.println("[WynnChaYuan] Wynntils 物品高亮的欄位對不上，加速停用：" + t);
            broken = true;
            return false;
        }
    }
}
