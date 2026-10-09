package com.wynnchayuan.client;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.NoticeView;
import com.wynnchayuan.client.ui.Surface;
import net.minecraft.client.gui.screens.Screen;

/**
 * 使用須知：跟別的玩家講話請用原文。
 *
 * <p>第一次進到 Wynncraft 的角色選擇時自動跳出一次；之後從設定畫面側欄最底下的
 * 「使用須知」打開。勾「不再自動顯示」只影響自動跳出，從設定永遠打得開。
 *
 * <p>畫面本身在 {@link NoticeView}。
 */
public final class NoticeScreen extends CanvasScreen {

    /** 這一次開遊戲跳過了沒。一場只跳一次，不然每換一次世界就再跳一次。 */
    private static boolean shownThisSession = false;

    /** 要跳，但還在等載入畫面收掉。 */
    private static volatile boolean pending = false;

    private final NoticeView view;
    private boolean dismissed;

    public NoticeScreen(Screen parent) {
        super(T.c("notice.title"), parent);
        this.dismissed = WynnChaYuan.config().noticeDismissed();
        this.view = new NoticeView(SHELL, () -> dismissed, () -> dismissed = !dismissed,
                this::onClose);
    }

    @Override
    protected Surface view() {
        return view;
    }

    /** 進到伺服器時叫一次：該跳就記下來，等 {@link #clientTick} 找到空檔再跳。 */
    public static void maybeShowOnJoin() {
        var config = WynnChaYuan.config();
        if (config == null || shownThisSession || config.noticeDismissed()) {
            return;
        }
        pending = true;
    }

    /**
     * 每個 tick 看一下能不能跳了。
     *
     * <p>不能在收到「進伺服器」的當下就開畫面：那時候載入畫面還在，開了會被它蓋掉，
     * 或是把它擠掉讓遊戲卡在奇怪的狀態。等到人已經站在世界裡、畫面上沒有別的東西才跳。
     */
    public static void clientTick() {
        if (!pending) {
            return;
        }
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        pending = false;
        shownThisSession = true;
        mc.setScreen(new NoticeScreen(null));
    }

    @Override
    public void onClose() {
        if (dismissed != WynnChaYuan.config().noticeDismissed()) {
            WynnChaYuan.config().setNoticeDismissed(dismissed);
        }
        super.onClose();
    }
}
