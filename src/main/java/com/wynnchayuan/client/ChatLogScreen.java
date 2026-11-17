package com.wynnchayuan.client;

import com.wynnchayuan.client.ui.ChatLogView;
import com.wynnchayuan.client.ui.Surface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class ChatLogScreen extends CanvasScreen {

    private final ChatLogHost host;
    private final ChatLogView view;

    public ChatLogScreen(Screen parent) {
        super(T.c("chatlog.title"), parent);
        this.host = new ChatLogHost(Minecraft.getInstance().font, this::onClose, false);
        this.view = new ChatLogView(SHELL, host);
    }

    @Override
    protected Surface view() {
        return view;
    }

    @Override
    public void tick() {
        host.tick(view);
    }
}
