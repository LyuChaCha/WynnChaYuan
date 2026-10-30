package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import com.wynntils.core.text.StyledText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// FontRenderer 先折行才進 renderText，整句的鍵要在折行之前換
@Mixin(targets = "com.wynntils.screens.activities.WynntilsContentBookScreen",
       remap = false)
public abstract class WynntilsBookTextMixin {

    @ModifyArg(
            method = "doRender",
            at = @At(value = "INVOKE",
                    target = "Lcom/wynntils/utils/render/FontRenderer;"
                            + "renderAlignedTextInBox(Lnet/minecraft/client/gui/GuiGraphics;"
                            + "Lcom/wynntils/core/text/StyledText;FFFFF"
                            + "Lcom/wynntils/utils/colors/CustomColor;"
                            + "Lcom/wynntils/utils/render/type/HorizontalAlignment;"
                            + "Lcom/wynntils/utils/render/type/VerticalAlignment;"
                            + "Lcom/wynntils/utils/render/type/TextShadow;)V",
                    remap = false),
            index = 1, remap = false)
    private StyledText wynnchayuan$translate(StyledText text) {
        return WynntilsText.screenText(text);
    }
}
