package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynnModTooltip;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

/**
 * WynnMod 自己重畫的物品說明：它把內容交出去的那一步，換成譯文。
 *
 * <h2>實機回報（2026-10-10）：「裝備整份都是英文」</h2>
 * WynnMod 的「Decorate Item Tooltip」會照它自己解析出來的物品資料<b>整份重畫</b>
 * 裝備的 tooltip，而且它那個 mixin 的優先度是 -10000——刻意排在所有模組的最後一步。
 * 我們在 Wynntils 的事件裡翻好的那一份，到這裡被它整個換掉，畫面上就是全英文；
 * 素材與介面不經過它，所以看起來是「部分有翻、部分沒翻」。
 *
 * <p>它的流程是：發一個 {@code DrawItemTooltip} 事件，功能那一邊呼叫
 * {@code setText(重畫好的那幾行)}，最後拿 {@code getText()} 去畫。這裡接的是
 * {@code setText}——只有它換過內容的那些物品才會經過，其餘的照舊走 Wynntils 那條路。
 *
 * <p>{@link Pseudo}：沒裝 WynnMod 就整個跳過；它改版、方法對不上的話這一項自己失效
 * （{@code require = 0}），其餘功能不受影響。
 */
@Pseudo
@Mixin(targets = "com.wynnmod.mixin.events.ContainerEvents$DrawItemTooltip", remap = false)
public abstract class WynnModTooltipMixin {

    @ModifyVariable(method = "setText", at = @At("HEAD"), argsOnly = true,
                    remap = false, require = 0)
    private List<Component> wynnchayuan$translate(List<Component> text) {
        return WynnModTooltip.lines(text);
    }
}
