package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynnModTooltip;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

// WynnMod Decorate Item Tooltip 整份重畫，mixin 優先度 -10000
@Pseudo
@Mixin(targets = "com.wynnmod.mixin.events.ContainerEvents$DrawItemTooltip", remap = false)
public abstract class WynnModTooltipMixin {

    @ModifyVariable(method = "setText", at = @At("HEAD"), argsOnly = true,
                    remap = false, require = 0)
    private List<Component> wynnchayuan$translate(List<Component> text) {
        return WynnModTooltip.lines(text);
    }
}
