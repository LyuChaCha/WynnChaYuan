package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import com.wynntils.core.text.StyledText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// Wynntils 方法名不混淆，但簽章有會重映射的 MC 型別
@Mixin(targets = "com.wynntils.core.consumers.overlays.TextOverlay", remap = false)
public abstract class TextOverlayMixin {

    @ModifyVariable(method = "renderTemplate", at = @At("HEAD"), argsOnly = true,
                    remap = false)
    private StyledText[] wynnchayuan$translate(StyledText[] lines) {
        return WynntilsText.lines(this, lines);
    }
}
