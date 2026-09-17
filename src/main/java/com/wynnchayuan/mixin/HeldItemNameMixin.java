package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 切換手上物品時，快捷列上方跳出來的那行名稱。
 *
 * <p>原版把物品名稱 {@code append} 進一個空的 {@code MutableComponent} 再量寬度、置中。
 * 換在 append 的參數上，寬度與置中就照譯文算，不必另外排版。
 *
 * <p>F6 的開關預設關閉，見 {@link WynntilsText#heldItemName}。
 */
@Mixin(Gui.class)
public abstract class HeldItemNameMixin {

    @ModifyArg(method = "renderSelectedItemName",
               at = @At(value = "INVOKE",
                        target = "Lnet/minecraft/network/chat/MutableComponent;append(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;"))
    private Component wynnchayuan$translate(Component name) {
        return WynntilsText.heldItemName(name);
    }
}
