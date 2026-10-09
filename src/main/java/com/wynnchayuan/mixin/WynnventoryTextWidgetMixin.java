package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynnventoryLabels;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Wynnventory 獎勵畫面上寫死的標籤（Aspects、Tomes、Mythic、Filters…）。
 *
 * <p>那個畫面的字都是一個個 {@code TextWidget}。有譯文的那幾個改由我們畫
 * （{@link WynnventoryLabels}），其餘照它原本的畫——獎勵池的名字是縮寫與地區名，不翻。
 *
 * <p>方法名寫兩個：{@code renderContents} 是它覆寫的<b>原版</b>方法
 * （{@code AbstractButton#renderContents}），正式環境裡會跟著原版改成中介名
 * {@code method_75752}；它不在編譯時的類別路徑上，改名工具認不出這層關係，只好自己列。
 *
 * <p>這裡刻意不用 {@code @Shadow} 接它的欄位，原因見 {@link WynnventoryLabels}。
 */
@Pseudo
@Mixin(targets = "com.wynnventory.gui.widget.TextWidget", remap = false)
public abstract class WynnventoryTextWidgetMixin {

    @Inject(method = {"renderContents", "method_75752"}, at = @At("HEAD"), cancellable = true,
            remap = false, require = 0)
    private void wynnchayuan$translated(GuiGraphics graphics, int mouseX, int mouseY, float delta,
                                        CallbackInfo ci) {
        if (WynnventoryLabels.draw(graphics, this)) {
            ci.cancel();
        }
    }
}
