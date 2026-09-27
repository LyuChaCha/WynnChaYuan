package com.wynnchayuan.mixin;

import com.wynntils.screens.base.widgets.TextInputBoxWidget;

/**
 * 這個輸入框裡現在有沒有玩家打的字。
 *
 * <p>{@link WynntilsTextBoxMixin} 與 {@link WynntilsSearchBoxMixin} 共用。
 * 拆成獨立的類別是因為那兩個 mixin 打的是<b>同一條繼承鏈</b>上的父子類別——
 * 同一個 mixin 類別套到父子兩邊，注入進去的私有方法會互相覆蓋，
 * 分成兩個 mixin 就各自乾淨，共用的邏輯放在這裡（這個類別自己不是 mixin）。
 */
final class TypedText {

    private TypedText() {}

    /**
     * @return 框裡有字就 {@code true}；空的時候畫的是 Wynntils 自己的提示字
     *         （「Search...」），那該翻
     */
    static boolean present(Object widget) {
        if (!(widget instanceof TextInputBoxWidget box)) {
            return true;                       // 認不出來就一律當成玩家打的，寧可不翻
        }
        String typed = box.getTextBoxInput();
        return typed != null && !typed.isEmpty();
    }
}
