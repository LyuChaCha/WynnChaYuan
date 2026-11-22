package com.wynnchayuan.client;

import com.wynntils.core.components.Managers;
import com.wynntils.features.chat.ChatTimestampFeature;

import java.lang.reflect.Field;

final class WynntilsChatShift {

    private static final float GAP = 4;

    private static Field width;
    private static boolean broken;

    private WynntilsChatShift() {
    }

    static float pixels() {
        if (broken) {
            return 0;
        }
        try {
            ChatTimestampFeature feature = Managers.Feature.getFeatureInstance(ChatTimestampFeature.class);
            if (feature == null || !feature.isEnabled()) {
                return 0;
            }
            if (width == null) {
                Field f = ChatTimestampFeature.class.getDeclaredField("timestampWidth");
                f.setAccessible(true);
                width = f;
            }
            int w = width.getInt(feature);
            return w == 0 ? 0 : w + GAP;
        } catch (Throwable t) {
            broken = true;
            return 0;
        }
    }
}
