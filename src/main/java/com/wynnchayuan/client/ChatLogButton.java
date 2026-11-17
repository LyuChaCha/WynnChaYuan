package com.wynnchayuan.client;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.Icons;
import com.wynnchayuan.client.ui.Ui;
import com.wynnchayuan.translate.ErrorDebug;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;

public final class ChatLogButton {

    private static final float H = 13;
    private static final float ABOVE_INPUT = 17;

    private ChatLogButton() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof ChatScreen chat) {
                ScreenEvents.afterRender(chat).register(
                        (s, g, mouseX, mouseY, partial) -> render(chat, g, mouseX, mouseY));
                ScreenMouseEvents.allowMouseClick(chat).register(
                        (s, event) -> !click(chat, event.x(), event.y()));
            }
        });
    }

    private static boolean shown() {
        return WynnChaYuan.config() != null && WynnChaYuan.config().chatCopy();
    }

    private static float[] box(ChatScreen chat, GuiCanvas c) {
        String label = T.s("chatlog.button");
        float w = c.width(label) + 25;
        return new float[] {chat.width - 4 - w, chat.height - ABOVE_INPUT - H, w, H};
    }

    private static void render(ChatScreen chat, GuiGraphics g, int mouseX, int mouseY) {
        if (!shown()) {
            return;
        }
        try {
            Minecraft mc = Minecraft.getInstance();
            GuiCanvas c = new GuiCanvas(g, chat.getFont(), mc.getWindow().getGuiScale());
            g.nextStratum();
            c.begin();
            try {
                float[] b = box(chat, c);
                boolean hot = Ui.in(mouseX, mouseY, b[0], b[1], b[2], b[3]);
                int accent = 0xFF000000 | (WynnChaYuan.config().themeARGB() & 0xFFFFFF);
                Ui.halo(c, b[0], b[1], b[2], b[3], H / 2f, accent, hot ? 1f : 0f);
                Ui.pane(c, b[0], b[1], b[2], b[3], H / 2f, Ui.WINDOW_SOLID,
                        hot ? Ui.alpha(accent, 0xB0) : Ui.WIN_EDGE);
                c.fill(b[0] + H / 2f, b[1] + c.px(), b[0] + b[2] - H / 2f, b[1] + c.px() * 2,
                       hot ? 0x40FFFFFF : 0x22FFFFFF);
                c.icon(Icons.Icon.COPY, b[0] + 6, b[1] + 3, 7, hot ? accent : Ui.TEXT_3);
                c.text(T.s("chatlog.button"), b[0] + 17, b[1] + 3, hot ? Ui.TEXT : Ui.TEXT_2);
            } finally {
                c.end();
            }
        } catch (Throwable t) {
            ErrorDebug.note("ChatLogButton#render", null, t);
        }
    }

    private static boolean click(ChatScreen chat, double x, double y) {
        if (!shown()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        GuiCanvas c = new GuiCanvas(null, chat.getFont(), mc.getWindow().getGuiScale());
        float[] b = box(chat, c);
        if (!Ui.in(x, y, b[0], b[1], b[2], b[3])) {
            return false;
        }
        mc.setScreen(new ChatLogScreen(chat));
        return true;
    }
}
