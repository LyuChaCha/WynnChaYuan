package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.ModLog;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.ErrorDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.component.CustomModelData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

// Wynntils 每次讀 custom_model_data 都發事件，照物件身分快取
public final class HighlightModelCache {

    private HighlightModelCache() {
    }

    private static final int SIZE = 1024;

    private record Entry(CustomModelData key, boolean hideStar, CustomModelData value) { }

    private static final Entry[] TABLE = new Entry[SIZE];

    private static volatile boolean broken;
    private static List<String> highlightKeys;
    private static String starKey;
    private static Field hideStarField;
    private static Method configGet;
    private static Class<?> guideScreen;

    public static CustomModelData lookup(Object feature, CustomModelData original) {
        if (broken || original == null || !enabled() || !ready(feature)) {
            return null;
        }
        try {
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
            ErrorDebug.note("HighlightModelCache#lookup", null, t);
            broken = true;
            return null;
        }
    }

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

    static void useKeys(List<String> keys, String star) {
        highlightKeys = List.copyOf(keys);
        starKey = star;
    }

    private static boolean enabled() {
        ModConfig config = WynnChaYuan.config();
        return config == null || config.wynntilsFps();
    }

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
            ModLog.info("Wynntils 物品高亮的欄位對不上，加速停用：" + t);
            broken = true;
            return false;
        }
    }
}
