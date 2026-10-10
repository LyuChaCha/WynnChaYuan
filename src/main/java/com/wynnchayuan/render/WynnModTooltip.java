package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.listener.RenderListener;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * WynnMod 重畫的物品說明：再翻一次。
 *
 * <p>為什麼需要、接在哪裡，見 {@code WynnModTooltipMixin}。這裡只管「要不要翻」
 * 與「出事時退回它原本那一份」。
 *
 * <h2>只在就地取代模式做</h2>
 * 另開面板的模式本來就不改原本的 tooltip，譯文面板照 Wynntils 事件拿到的那一份畫，
 * WynnMod 重畫什麼都不影響它；關閉模式更不用說。
 */
public final class WynnModTooltip {

    private WynnModTooltip() {}

    private static volatile Boolean installed;

    /** 有沒有裝 WynnMod。 */
    public static boolean installed() {
        Boolean known = installed;
        if (known == null) {
            boolean found;
            try {
                found = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("wynnmod");
            } catch (Throwable t) {
                found = false;                 // 測試環境沒有 loader
            }
            installed = known = found;
        }
        return known;
    }

    /**
     * WynnMod 要畫的那幾行 → 我們要它畫的那幾行。
     *
     * <p>翻不到、開關關著、或中途出任何事，都<b>原樣</b>交回去：這是別人模組的
     * 算繪路徑，在這裡丟例外壞掉的是它的 tooltip。
     */
    public static List<Component> lines(List<Component> text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        try {
            CollectorConfig config = WynnChaYuan.config();
            if (config == null || !config.wynnmodTooltip()
                    || config.tooltipMode() != CollectorConfig.TooltipMode.REPLACE) {
                return text;
            }
            List<Component> out = RenderListener.translateForeign(text);
            if (out.isEmpty()) {
                TooltipDebug.dumpForeign("wynnmod", text, null);
                return text;
            }
            TooltipDebug.dumpForeign("wynnmod", text, out);
            WynnChaYuan.store().noteEvent("tooltip.wynnmod");
            return out;
        } catch (Throwable t) {
            try {
                WynnChaYuan.store().noteEvent("render.wynnmodError");
                com.wynnchayuan.translate.ErrorDebug.note("tooltip.wynnmod",
                        text.get(0).getString(), t);
            } catch (Throwable ignored) {
                // 連記錄都失敗就算了，重點是把原本那一份交回去
            }
            return text;
        }
    }
}
