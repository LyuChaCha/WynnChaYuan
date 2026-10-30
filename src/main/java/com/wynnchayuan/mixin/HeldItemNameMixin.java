package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Gui.class)
public abstract class HeldItemNameMixin {

    @ModifyArg(method = "renderSelectedItemName",
               at = @At(value = "INVOKE",
                        target = "Lnet/minecraft/network/chat/MutableComponent;append(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;"))
    private Component wynnchayuan$translate(Component name) {
        return WynntilsText.heldItemName(name);
    }
}
