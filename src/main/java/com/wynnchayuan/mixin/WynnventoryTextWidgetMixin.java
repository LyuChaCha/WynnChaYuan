package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynnventoryLabels;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 正式環境 renderContents 是中介名 method_75752，兩個都列
@Pseudo
@Mixin(targets = "com.wynnventory.gui.widget.TextWidget", remap = false)
public abstract class WynnventoryTextWidgetMixin {

    @Inject(method = {"renderContents", "method_75752"}, at = @At("HEAD"), cancellable = true,
            remap = false, require = 0)
    private void wynnchayuan$translated(GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                        CallbackInfo ci) {
        if (WynnventoryLabels.draw(graphics, this)) {
            ci.cancel();
        }
    }
}
