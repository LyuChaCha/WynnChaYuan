package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import com.wynntils.screens.base.TooltipProvider;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(targets = "com.wynntils.screens.activities.WynntilsContentBookScreen",
       remap = false)
public abstract class WynntilsBookTooltipMixin {

    @Redirect(
            method = "renderTooltips",
            at = @At(value = "INVOKE",
                    target = "Lcom/wynntils/screens/base/TooltipProvider;"
                            + "getTooltipLines()Ljava/util/List;"),
            remap = false)
    private List<Component> wynnchayuan$translate(
            TooltipProvider provider) {
        return WynntilsText.menuTooltip(provider.getTooltipLines());
    }
}
