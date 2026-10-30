package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import com.wynntils.models.marker.type.MarkerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// MarkerInfo 被 Wynntils 拿去比對與存檔，只換讀取
@Mixin(targets = "com.wynntils.features.map.WorldWaypointDistanceFeature", remap = false)
public abstract class WaypointTextMixin {

    @Redirect(method = "onRenderLevelPost",
              at = @At(value = "INVOKE",
                       target = "Lcom/wynntils/models/marker/type/MarkerInfo;additionalText()Ljava/lang/String;"),
              remap = false)
    private String wynnchayuan$translate(MarkerInfo marker) {
        return WynntilsText.marker(marker.additionalText());
    }
}
