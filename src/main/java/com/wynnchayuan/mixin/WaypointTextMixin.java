package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import com.wynntils.models.marker.type.MarkerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Wynntils 任務指引：畫面上那個卷軸圖示底下的任務名（「Cook Assistant 30m」）。
 *
 * <p>字是 {@code MarkerInfo#additionalText()} 給的，每一幀算一次畫面上的標記時讀。
 * 只換這一處讀取、不動 {@code MarkerInfo} 本身——那份資料 Wynntils 還拿去比對與存檔，
 * 改掉記錄本身會讓它認不出同一個標記。
 *
 * <p>判斷與翻譯在 {@link WynntilsText#marker}；翻不出來原樣回傳。
 */
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
