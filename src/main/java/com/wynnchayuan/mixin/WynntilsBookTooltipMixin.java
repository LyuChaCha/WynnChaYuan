package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * 綜合頁面右邊那張卡。
 *
 * <h2>為什麼要跟 {@link WynntilsMenuTooltipMixin} 分開</h2>
 * 兩邊的程式碼長得一模一樣——問被停在上面的那個元件要 tooltip、交給原版畫——
 * 但綜合頁面<b>不是</b>清單畫面的子類別，它自己有一支 {@code renderTooltips}
 * （複數）。第一版只打了清單畫面那一支，結果實機上左邊那一列任務名變成中文、
 * 右邊那張卡整張還是英文（使用者 2026-09-20 的截圖）。
 *
 * <p>兩支都打，兩支都走 {@link WynntilsText#menuTooltip}。
 */
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
            com.wynntils.screens.base.TooltipProvider provider) {
        return WynntilsText.menuTooltip(provider.getTooltipLines());
    }
}
