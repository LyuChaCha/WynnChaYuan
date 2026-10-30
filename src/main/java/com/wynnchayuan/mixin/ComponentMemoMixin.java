package com.wynnchayuan.mixin;

import com.wynnchayuan.render.ComponentMemo;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DataComponentHolder.class)
public interface ComponentMemoMixin {

    @Inject(method = "get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void wynnchayuan$memo(DataComponentType<?> type, CallbackInfoReturnable<Object> cir) {
        if (!(((Object) this) instanceof ItemStack stack)) {
            return;
        }
        ComponentMemo.Hit hit = ComponentMemo.lookup(stack, type);
        if (hit != null) {
            cir.setReturnValue(hit.value());
        }
    }
}
