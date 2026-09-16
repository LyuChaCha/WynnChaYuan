package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 每日／公會目標那幾條字（「Finish Quests: 2/3」）。
 *
 * <p>Wynntils 把目標畫成進度條，條上的字是 {@code asObjectiveString()} 給的。
 * 換在這裡，進度條與版面都不必碰。
 *
 * <p>判斷與翻譯在 {@link WynntilsText#objective}；關掉開關時原樣回傳。
 */
@Mixin(targets = "com.wynntils.models.objectives.WynnObjective", remap = false)
public abstract class WynnObjectiveMixin {

    @Inject(method = "asObjectiveString", at = @At("RETURN"), cancellable = true,
            remap = false)
    private void wynnchayuan$translate(CallbackInfoReturnable<String> hit) {
        hit.setReturnValue(WynntilsText.objective(hit.getReturnValue()));
    }
}
