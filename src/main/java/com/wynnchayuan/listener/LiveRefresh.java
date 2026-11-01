package com.wynnchayuan.listener;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.render.TextDisplayLabels;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.TranslationStore;

public final class LiveRefresh {

    private static long shown = Long.MIN_VALUE;

    private LiveRefresh() {}

    public static void tick() {
        ModConfig config = WynnChaYuan.config();
        TranslationStore store = WynnChaYuan.translations();
        if (config == null || store == null) {
            return;
        }
        long stamp = store.cacheAge() << 3 | config.nametagMode().ordinal() << 1
                | (config.showOverlays() ? 1 : 0);
        if (stamp == shown) {
            return;
        }
        shown = stamp;
        try {
            TextDisplayLabels.refresh(config, store);
            ScoreboardListener.retranslate();
        } catch (Throwable t) {
            ErrorDebug.note("LiveRefresh#tick", null, t);
        }
    }
}
