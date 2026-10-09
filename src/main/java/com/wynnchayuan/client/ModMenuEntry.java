package com.wynnchayuan.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * 讓 Mod Menu 的模組清單上出現「設定」那顆按鈕。
 *
 * <p>Mod Menu 是選裝的：沒有裝的話這個類別<b>永遠不會被載入</b>——它只登記在
 * {@code fabric.mod.json} 的 {@code modmenu} 進入點底下，而那個進入點只有 Mod Menu
 * 自己會去讀。所以這裡可以直接用它的介面，不必擔心沒裝的人找不到類別。
 */
public final class ModMenuEntry implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return SettingsScreen::new;
    }
}
