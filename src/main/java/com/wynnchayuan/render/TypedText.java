package com.wynnchayuan.render;

import com.wynntils.screens.base.widgets.TextInputBoxWidget;

// 不可放 mixin 套件：實機載入會 IllegalClassLoadError
public final class TypedText {

    private TypedText() {}

    public static boolean present(Object widget) {
        if (!(widget instanceof TextInputBoxWidget box)) {
            return true;
        }
        String typed = box.getTextBoxInput();
        return typed != null && !typed.isEmpty();
    }
}
