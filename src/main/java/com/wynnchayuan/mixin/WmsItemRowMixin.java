package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WmsRows;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * WynnMarketSearch 的搜尋結果：英文名右邊多畫一行譯名。
 *
 * <p>掛在那一列畫完之後，所以譯名蓋在最上面，不會被它的底色蓋掉。
 * 怎麼畫在 {@link WmsRows}。
 *
 * <p>跟 {@link WmsItemSearchMixin} 一樣是 {@link Pseudo}——沒裝那個模組就整個跳過；
 * 那個模組改版、方法簽名對不上的話這一項自己失效（{@code require = 0}），
 * 搜尋那一半不受影響。
 */
@Pseudo
@Mixin(targets = "me.a0g.gui.widgets.ItemButtonWidget", remap = false)
public abstract class WmsItemRowMixin {

    @Shadow(remap = false)
    protected int x;

    @Shadow(remap = false)
    protected int y;

    @Shadow(remap = false)
    protected int width;

    @Shadow(remap = false)
    protected int height;

    @Inject(method = "draw", at = @At("TAIL"), remap = false, require = 0)
    private void wynnchayuan$translatedName(GuiGraphics graphics, int mouseX, int mouseY,
                                            float delta, CallbackInfo ci) {
        WmsRows.draw(graphics, this, x, y, width, height);
    }
}
