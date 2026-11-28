package com.wynnchayuan.client;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.ErrorDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

public final class Toasts {

    private Toasts() {}

    public static void show(Minecraft mc, Component message) {
        if (mc == null || message == null) {
            return;
        }
        try {
            SystemToast.addOrUpdate(mc.getToastManager(),
                    SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                    Component.literal(WynnChaYuan.MOD_NAME), message);
        } catch (Throwable t) {
            ErrorDebug.note("Toasts#show", null, t);
        }
    }
}
