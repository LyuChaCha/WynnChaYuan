package com.wynnchayuan.translate;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;

/**
 * 聊天裡那顆「按這裡更新」。
 *
 * <h2>為什麼值得釘</h2>
 * 這顆按鈕壞掉的方式是<b>安靜的</b>：字照樣印出來，只是按下去沒反應，或是
 * 指令被送到 Wynncraft 去換來一句「未知的指令」。兩種都要玩家回報才會發現。
 *
 * <p>所以這裡釘三件事：點下去是<b>指令</b>而不是別的動作、指令名字跟
 * {@link TranslationUpdate#COMMAND} 註冊的那一個一致（差一個字就送到伺服器去了）、
 * 以及<b>有底線</b>——聊天室裡沒有按鈕長相，底線是唯一會讓人想到「這可以按」
 * 的提示，掉了等於沒有按鈕。
 */
public final class UpdateButtonTest {

    private static int failures = 0;

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        Component button = TranslationUpdate.button();

        check("按鈕有字（" + button.getString().strip() + "）",
                !button.getString().isBlank());

        ClickEvent click = button.getStyle().getClickEvent();
        check("按下去有事情發生", click != null);
        check("是「送出指令」而不是開網址或填字",
                click instanceof ClickEvent.RunCommand);
        if (click instanceof ClickEvent.RunCommand run) {
            // 前面那個斜線不能少：聊天框是照字面送的，少了就變成講話，
            // 整句「wynnchayuan-update」會出現在公頻上。
            check("指令帶著開頭的斜線（實際 " + run.command() + "）",
                    run.command().startsWith("/"));
            check("指令名字跟註冊的那一個一致",
                    run.command().equals("/" + TranslationUpdate.COMMAND));
        }

        check("有底線 —— 聊天室裡那是唯一看得出「可以按」的提示",
                button.getStyle().isUnderlined());
        check("滑過去有說明", button.getStyle().getHoverEvent() != null);

        // 客戶端指令不能有空白：Fabric 是照第一個 literal 去接的，
        // 而帶空白的名字在聊天框會被拆成指令＋參數，接不到。
        check("指令名字沒有空白", !TranslationUpdate.COMMAND.contains(" "));

        System.out.println(failures == 0 ? "\n更新按鈕：全部通過"
                : "\n更新按鈕：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + name);
        if (!ok) {
            failures++;
        }
    }
}
