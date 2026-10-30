package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import com.wynntils.core.text.StyledText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// Wynntils FontRenderer 的多載最後都轉進不帶 maxWidth 的 renderText
@Mixin(targets = "com.wynntils.utils.render.FontRenderer", remap = false)
public abstract class WynntilsFontMixin {

    @ModifyVariable(
            method = "renderText(Lnet/minecraft/client/gui/GuiGraphics;"
                    + "Lcom/wynntils/core/text/StyledText;FF"
                    + "Lcom/wynntils/utils/colors/CustomColor;"
                    + "Lcom/wynntils/utils/render/type/HorizontalAlignment;"
                    + "Lcom/wynntils/utils/render/type/VerticalAlignment;"
                    + "Lcom/wynntils/utils/render/type/TextShadow;F)V",
            at = @At("HEAD"), argsOnly = true, remap = false)
    private StyledText wynnchayuan$translate(StyledText text) {
        return WynntilsText.screenText(text);
    }

    // renderScrollingText 先量寬度決定捲不捲，要在入口就換
    @ModifyVariable(
            method = "renderScrollingText(Lnet/minecraft/client/gui/GuiGraphics;"
                    + "Lcom/wynntils/core/text/StyledText;FFF"
                    + "Lcom/wynntils/utils/colors/CustomColor;"
                    + "Lcom/wynntils/utils/render/type/HorizontalAlignment;"
                    + "Lcom/wynntils/utils/render/type/VerticalAlignment;"
                    + "Lcom/wynntils/utils/render/type/TextShadow;F)V",
            at = @At("HEAD"), argsOnly = true, remap = false)
    private StyledText wynnchayuan$translateScrolling(StyledText text) {
        return WynntilsText.screenText(text);
    }
}
