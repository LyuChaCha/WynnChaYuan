package com.wynnchayuan.translate;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class UpdateButtonTest {

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
            check("指令帶著開頭的斜線（實際 " + run.command() + "）",
                    run.command().startsWith("/"));
            check("指令名字跟註冊的那一個一致",
                    run.command().equals("/" + TranslationUpdate.COMMAND));
        }

        check("有底線 —— 聊天室裡那是唯一看得出「可以按」的提示",
                button.getStyle().isUnderlined());
        check("滑過去有說明", button.getStyle().getHoverEvent() != null);

        // Fabric 客戶端指令名不能有空白
        check("指令名字沒有空白", !TranslationUpdate.COMMAND.contains(" "));

        System.out.println(failures == 0 ? "\n更新按鈕：全部通過"
                : "\n更新按鈕：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

}
