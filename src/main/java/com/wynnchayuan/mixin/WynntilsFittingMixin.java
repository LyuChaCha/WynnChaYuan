package com.wynnchayuan.mixin;

import com.wynnchayuan.render.TooltipScaleProbe;
import com.wynnchayuan.render.WynnventoryLayout;
import com.wynntils.mc.event.TooltipRenderEvent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
                TooltipScaleProbe.scaleOf(current));
        if (moved != null) {
            event.setPositioner(moved);
        }
    }
}
