package com.wynnchayuan.client;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 模組自己的介面文字。
 *
 * <h2>為什麼要有</h2>
 * F6 那幾個畫面原本整片寫死繁體中文。對只會英文的人來說，那等於整個設定面板
 * 都看不懂——而這是個<b>翻譯</b>模組，看不懂設定面板的人正是最需要它的人。
 *
 * <p>走 Minecraft 自己的語言檔（{@code assets/wynnchayuan/lang/*.json}）：
 * 介面跟著<b>遊戲</b>的語言走，而且缺哪一條就自動退回 {@code en_us}——
 * 所以新語言只要放一個檔進去就生效，沒翻到的地方是英文而不是空白。
 *
 * <p>注意這跟 F6 裡的「譯文語言」是兩件事：那個管的是<b>遊戲內容</b>要翻成
 * 哪一種語言（校稿的人會把它切到簡體，但設定面板還是想看自己的母語）。
 *
 * <h2>為什麼包一層而不直接用 Component.translatable</h2>
 * 只為了省掉每個呼叫點都要寫一次的 {@code "wynnchayuan."} 前綴——寫漏一次
 * 不會編譯失敗，畫面上會直接印出鍵名，而那種錯很難一眼看出來。
 */
public final class T {

    private T() {}

    private static final String PREFIX = "wynnchayuan.";

    /** 給要 {@code Component} 的地方。 */
    public static MutableComponent c(String key, Object... args) {
        return Component.translatable(PREFIX + key, args);
    }

    /**
     * 給只收 {@code String} 的地方（量寬度、拼字串）。
     *
     * <p>會當場解析成目前語言的字。畫面上的東西一幀畫一次，這個成本
     * 跟畫字本身比可以忽略。
     */
    public static String s(String key, Object... args) {
        return c(key, args).getString();
    }
}
