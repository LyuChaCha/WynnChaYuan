package com.wynnchayuan.mixin;

import com.wynnchayuan.translate.SearchMatch;
import com.wynntils.services.itemfilter.type.ItemSearchQuery;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 銀行與背包的搜尋框：打中文也找得到東西。
 *
 * <h2>要解決什麼</h2>
 * 倉庫裡那雙靴子顯示的是「深淵雨靴」，玩家照著打「深淵」卻一格都不亮——
 * Wynntils 拿去比對的是它自己解析出來的 {@code Abysso Galoshes}。
 * 等於畫面上寫中文、搜尋只認英文。
 *
 * <h2>為什麼打在 itemNameMatches 上</h2>
 * {@code ItemFilterService.matches} 是「篩選條件都過 <b>而且</b> 名稱也對得上」。
 * 打在它的 {@code RETURN} 上分不出是哪一半沒過，放行等於把
 * {@code level:>100} 那種條件一起放掉。{@code itemNameMatches} 只管名稱那一半，
 * 篩選條件原封不動。
 *
 * <p>它是 {@code private}——mixin 打得到，而且正因為是私有的，
 * 它的呼叫端只有 {@code matches} 一個，行為好推。
 *
 * <h2>只加不減</h2>
 * 跟 {@code WynntilsSearchMixin} 一樣從 {@code RETURN} 進來：Wynntils 說中了
 * 就不碰，只有它說沒中時才拿譯文再比一次。所以這一道<b>只會讓原本不亮的亮起來</b>，
 * 原本用英文搜得到的東西一件都不會消失。
 *
 * <p>目標類別不在時由 {@link WynntilsGate} 擋掉，遊戲照常開得起來。
 */
@Mixin(targets = "com.wynntils.services.itemfilter.ItemFilterService", remap = false)
public abstract class WynntilsItemSearchMixin {

    @Inject(
            method = "itemNameMatches",
            at = @At("RETURN"),
            cancellable = true,
            remap = false,
            require = 0)
    private void wynnchayuan$alsoMatchTranslatedName(
            ItemSearchQuery searchQuery, String itemName,
            CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            return;                            // 它自己中了，不碰
        }
        if (SearchMatch.nameAlsoMatches(itemName, searchQuery.plainTextTokens())) {
            cir.setReturnValue(true);
        }
    }
}
