package com.wynnchayuan.client;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.NoticeView;
import com.wynnchayuan.client.ui.Surface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class NoticeScreen extends CanvasScreen {

    private static boolean shownThisSession = false;

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

    public static void maybeShowOnJoin() {
        var config = WynnChaYuan.config();
        if (config == null || shownThisSession || config.noticeDismissed()) {
            return;
        }
        pending = true;
    }

    public static void clientTick() {
        if (!pending) {
            return;
        }
        var mc = Minecraft.getInstance();
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
