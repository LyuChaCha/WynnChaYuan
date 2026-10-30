package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WmsRows;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.a0g.gui.widgets.ItemButtonWidget", remap = false)
public abstract class WmsItemRowMixin {

    @Shadow(remap = false)
    protected int x;

    @Shadow(remap = false)
    protected int y;

    @Shadow(remap = false)
    protected int width;

    @Shadow(remap = false)
    protected int height;

    @Inject(method = "draw", at = @At("TAIL"), remap = false, require = 0)
    private void wynnchayuan$translatedName(GuiGraphics graphics, int mouseX, int mouseY,
                                            float delta, CallbackInfo ci) {
        WmsRows.draw(graphics, this, x, y, width, height);
    }
}
