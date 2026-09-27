package com.wynnchayuan.render;

import com.wynntils.screens.base.widgets.TextInputBoxWidget;

/**
 * 這個輸入框裡現在有沒有玩家打的字。
 *
 * <p>{@code WynntilsTextBoxMixin} 與 {@code WynntilsSearchBoxMixin} 共用。
 * 拆成獨立的類別是因為那兩個 mixin 打的是<b>同一條繼承鏈</b>上的父子類別——
 * 同一個 mixin 類別套到父子兩邊，注入進去的私有方法會互相覆蓋，
 * 分成兩個 mixin 就各自乾淨，共用的邏輯放在這裡。
 *
 * <h2>為什麼不放在 mixin 套件底下</h2>
 * 放過。實機一開背包就炸：
 *
 * <pre>
 *   IllegalClassLoadError: com.wynnchayuan.mixin.TypedText is in a defined
 *   mixin package com.wynnchayuan.mixin.* owned by wynnchayuan.mixins.json
 *   and cannot be referenced directly
 * </pre>
 *
 * mixins.json 宣告的那個套件是 Mixin 自己的地盤，裡面的類別只能由 Mixin 載入，
 * 不能被注入進去的程式碼直接引用。編譯與 {@code gradle build} 都不會有話說——
 * 只有實機載入那一刻才會炸，而且炸的是<b>畫面正在畫的那一幀</b>。
 */
public final class TypedText {

    private TypedText() {}

    /**
     * @return 框裡有字就 {@code true}；空的時候畫的是 Wynntils 自己的提示字
     *         （「Search...」），那該翻
     */
    public static boolean present(Object widget) {
        if (!(widget instanceof TextInputBoxWidget box)) {
            return true;                       // 認不出來就一律當成玩家打的，寧可不翻
        }
        String typed = box.getTextBoxInput();
        return typed != null && !typed.isEmpty();
    }
}
