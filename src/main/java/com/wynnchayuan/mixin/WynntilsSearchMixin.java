package com.wynnchayuan.mixin;

import com.wynnchayuan.translate.SearchMatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Wynntils 的搜尋框：英文沒中的時候，拿譯文再比一次。
 *
 * <h2>為什麼打在工具類別上</h2>
 * {@code StringUtils.partialMatch} 是 Wynntils <b>每一個</b>搜尋框共用的那一支：
 * 內容書、設定、統計、獵寶路線、路徑點、疊層選擇、套裝指南⋯⋯全都呼叫它。
 * 一個一個畫面去打，要寫十幾份長得一樣的 mixin，而且它哪天多一個畫面我們就
 * 漏一個。打在共用的那一支上，一次全有。
 *
 * <p>內容書那一支長這樣（反編譯 {@code reloadContentBookWidgets}）：
 *
 * <pre>
 *   StringUtils.partialMatch(activityInfo.name(), searchWidget.getTextBoxInput())
 * </pre>
 *
 * 第一個參數是<b>被搜的內容</b>、第二個是<b>玩家打的字</b>。
 *
 * <h2>為什麼挑 RETURN 而不是 HEAD</h2>
 * 從 {@code RETURN} 進來時，Wynntils 自己的判斷<b>已經跑完了</b>：它說中了
 * 就直接放行，我們一個字都不碰；只有它說沒中的時候才輪到我們。所以這一道
 * <b>只會讓原本不亮的亮起來</b>，不可能讓原本搜得到的東西消失。
 *
 * <p>打在 {@code HEAD} 並取消的話就是我們說了算，那風險完全不同——
 * 搜尋是玩家找東西的最後手段，弄壞它比沒翻譯還糟。
 *
 * <p>目標類別不在時由 {@link WynntilsGate} 擋掉，遊戲照常開得起來。
 */
@Mixin(targets = "com.wynntils.utils.StringUtils", remap = false)
public abstract class WynntilsSearchMixin {

    @Inject(
            method = "partialMatch(Ljava/lang/String;Ljava/lang/String;)Z",
            at = @At("RETURN"),
            cancellable = true,
            remap = false)
    private static void wynnchayuan$alsoMatchTranslated(
            String content, String query, CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            return;                            // 它自己中了，不碰
        }
        if (SearchMatch.alsoMatches(content, query)) {
            cir.setReturnValue(true);
        }
    }
}
