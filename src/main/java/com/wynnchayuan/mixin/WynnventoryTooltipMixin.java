package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynnventoryBox;
import com.wynnchayuan.translate.WynnventoryBridge;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(targets = "com.wynnventory.util.RenderUtils", remap = false)
public abstract class WynnventoryTooltipMixin {

    @ModifyVariable(method = "drawTooltip", at = @At("HEAD"), argsOnly = true, ordinal = 1,
                    remap = false, require = 0)
    private static List<Component> wynnchayuan$translate(List<Component> customLines) {
        return WynnventoryBridge.lines(customLines);
    }

    @Inject(method = "drawTooltip", at = @At("HEAD"), cancellable = true, remap = false,
            require = 0)
    private static void wynnchayuan$layout(net.minecraft.client.gui.GuiGraphics graphics,
                                           int mouseX, int mouseY, List<Component> vanilla,
                                           List<Component> custom,
                                           org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (com.wynnchayuan.render.WynnventoryLayout.draw(graphics, mouseX, mouseY, vanilla, custom)) {
            ci.cancel();
        }
    }

    @Inject(method = "calculateTooltipCoords", at = @At("RETURN"), remap = false, require = 0)
    private static void wynnchayuan$where(int mouseX, int mouseY,
                                          List<ClientTooltipComponent> vanillaComponents,
                                          List<ClientTooltipComponent> priceComponents,
                                          CallbackInfoReturnable<Vector2i> cir) {
        WynnventoryBox.note(cir.getReturnValue(), priceComponents);
    }
}
