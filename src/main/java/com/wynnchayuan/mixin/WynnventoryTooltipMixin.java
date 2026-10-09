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

/**
 * Wynnventory 的價格框與意象清單：畫之前把寫死的字（物品名、數值名…）換成譯文。
 *
 * <p>它所有的側邊框都走 {@code RenderUtils.drawTooltip(graphics, x, y, 原本的 tooltip, 它的那幾行)}，
 * 這裡換的是最後那個參數——寬度、位置它自己照換過的字算。
 *
 * <p>{@link Pseudo}：沒裝那個模組就整個跳過；它改版、方法對不上的話這一項自己失效
 * （{@code require = 0}），標籤那一半（語言那一層）不受影響。
 */
@Pseudo
@Mixin(targets = "com.wynnventory.util.RenderUtils", remap = false)
public abstract class WynnventoryTooltipMixin {

    @ModifyVariable(method = "drawTooltip", at = @At("HEAD"), argsOnly = true, ordinal = 1,
                    remap = false, require = 0)
    private static List<Component> wynnchayuan$translate(List<Component> customLines) {
        return WynnventoryBridge.lines(customLines);
    }

    /**
     * 它算好價格框放哪裡了：記下來，我們「另開面板」的譯文面板才讓得開。
     * 見 {@link WynnventoryBox}。
     */
    @Inject(method = "calculateTooltipCoords", at = @At("RETURN"), remap = false, require = 0)
    private static void wynnchayuan$where(int mouseX, int mouseY,
                                          List<ClientTooltipComponent> vanillaComponents,
                                          List<ClientTooltipComponent> priceComponents,
                                          CallbackInfoReturnable<Vector2i> cir) {
        WynnventoryBox.note(cir.getReturnValue(), priceComponents);
    }
}
