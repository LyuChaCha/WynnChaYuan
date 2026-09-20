package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import com.wynntils.core.text.StyledText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Wynntils 自己那幾個畫面上的字（綜合頁面、地圖、疊層管理⋯⋯）。
 *
 * <h2>為什麼打在這裡</h2>
 * Wynntils 不用原版的 {@code Font} 畫自己的畫面，而是全部經過它自己的
 * {@code FontRenderer}。那個類別有十幾個多載，但它們最後都轉進<b>同一支</b>
 * 不帶 {@code maxWidth} 的 {@code renderText}——在那一支的入口把字換掉，
 * 排版、對齊、捲動全部沿用它原本的算法，我們只換內容。
 *
 * <p>選「不帶 maxWidth 的那一支」是刻意的：帶 {@code maxWidth} 的那支負責
 * 折行，折完再一行一行送進這一支。打在折行<b>之後</b>，我們拿到的就是
 * 實際要畫的那一行，不必自己重算寬度。
 *
 * <h2>只換查得到的</h2>
 * 判斷與翻譯都在 {@link WynntilsText#screenText}：查得到才換，查不到原樣回傳。
 * 所以 Wynntils 自己已經翻好的中文、以及我們上一幀換過的字，走到這裡都不動。
 * F6 關掉「Wynntils 介面」時同樣原樣回傳。
 *
 * <p>目標類別不在時整個 mixin 會被 {@link WynntilsGate} 跳過——Wynntils 改版
 * 搬家或玩家沒裝，遊戲照常開得起來。
 */
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
}
