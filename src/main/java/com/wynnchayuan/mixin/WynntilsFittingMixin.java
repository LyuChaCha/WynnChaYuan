package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynnventoryLayout;
import com.wynntils.mc.event.TooltipRenderEvent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wynntils 的「tooltip 縮放」決定位置的那一步：
 * <ul>
 *   <li>我們在畫 Wynnventory 的價格框時跳過——位置與縮放已經算好了；</li>
 *   <li>它定好之後，物品說明蓋到價格框的話往右讓開。</li>
 * </ul>
 * 見 {@link WynnventoryLayout}。
 */
@Mixin(targets = "com.wynntils.features.tooltips.TooltipFittingFeature", remap = false)
public abstract class WynntilsFittingMixin {

    @Inject(method = "onTooltipRenderPosition", at = @At("HEAD"), cancellable = true,
            remap = false, require = 0)
    private void wynnchayuan$skipOurs(TooltipRenderEvent.Position event, CallbackInfo ci) {
        if (WynnventoryLayout.drawing()) {
            ci.cancel();
        }
    }

    @Inject(method = "onTooltipRenderPosition", at = @At("RETURN"), remap = false, require = 0)
    private void wynnchayuan$makeRoom(TooltipRenderEvent.Position event, CallbackInfo ci) {
        ClientTooltipPositioner current = event.getPositioner();
        ClientTooltipPositioner moved = WynnventoryLayout.shift(current,
                com.wynnchayuan.render.TooltipScaleProbe.scaleOf(current));
        if (moved != null) {
            event.setPositioner(moved);
        }
    }
}
