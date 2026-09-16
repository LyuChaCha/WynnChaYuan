package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import com.wynntils.core.text.StyledText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 換掉 Wynntils 文字疊層要畫的那幾行——目前只有右上角的任務追蹤。
 *
 * <p>只在這裡轉手，判斷與翻譯都在 {@link WynntilsText}：那是一般的類別，
 * 測試進得去，出錯也查得到。
 *
 * <h2>為什麼挑 renderTemplate</h2>
 * {@code TextOverlay} 先把樣板展開成 {@code StyledText[]}，再交給
 * {@code renderTemplate} 畫。在這一步換掉，位置、字型、對齊、背景全部
 * 沿用 Wynntils 排好的，我們只換內容。
 *
 * <p>只寫方法<b>名字</b>不寫簽章：Wynntils 的方法名不會被混淆，而簽章裡有
 * Minecraft 的型別（會被重映射）。名字唯一就夠了。
 */
@Mixin(targets = "com.wynntils.core.consumers.overlays.TextOverlay", remap = false)
public abstract class TextOverlayMixin {

    @ModifyVariable(method = "renderTemplate", at = @At("HEAD"), argsOnly = true,
                    remap = false)
    private StyledText[] wynnchayuan$translate(StyledText[] lines) {
        return WynntilsText.lines(this, lines);
    }
}
