package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;

public record PanelPlace(Integer x, Integer y, Integer w, Integer h) {

    public static final PanelPlace AUTO = new PanelPlace(null, null, null, null);

    public boolean placed() {
        return x != null && y != null;
    }

    public boolean sized() {
        return w != null && h != null;
    }

    public static PanelPlace saved(ModConfig cfg, ModConfig.Overlay which) {
        if (cfg == null) {
            return AUTO;
        }
        boolean pos = cfg.hasOverlayPos(which);
        boolean size = cfg.hasOverlaySize(which);
        return new PanelPlace(pos ? cfg.overlayX(which) : null, pos ? cfg.overlayY(which) : null,
                size ? cfg.overlayW(which) : null, size ? cfg.overlayH(which) : null);
    }
}
