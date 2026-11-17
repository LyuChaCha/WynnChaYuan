package com.wynnchayuan.client;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.ChatLogView;
import com.wynnchayuan.client.ui.Icons;
import com.wynnchayuan.client.ui.Ui;
import com.wynnchayuan.translate.ErrorDebug;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.Util;

public final class ChatLogButton {

    private static final float H = 13;
    private static final float GAP = 4;

    private static boolean open;

    private ChatLogButton() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof ChatScreen chat) {
                attach(chat);
            }
        });
    }

    private static void attach(ChatScreen chat) {
        ChatLogHost host = new ChatLogHost(chat.getFont(), () -> open = false, true);
        ChatLogView view = new ChatLogView(CanvasScreen.SHELL, host);
        ScreenEvents.afterRender(chat).register(
                (s, g, mouseX, mouseY, partial) -> render(chat, view, g, mouseX, mouseY));
        ScreenEvents.afterTick(chat).register(s -> {
            if (active()) {
                host.tick(view);
            }
        });
        ScreenMouseEvents.allowMouseClick(chat).register((s, event) -> {
            if (pill(chat, event.x(), event.y())) {
                open = !open;
                CanvasScreen.SHELL.click();
                return false;
            }
            return !(active() && view.mouseDown(event.x(), event.y(), event.button()));
        });
        ScreenMouseEvents.allowMouseDrag(chat).register(
                (s, event, dx, dy) -> !(active() && view.mouseDrag(event.x(), event.y())));
        ScreenMouseEvents.allowMouseRelease(chat).register(
                (s, event) -> !(active() && view.mouseUp()));
        ScreenMouseEvents.allowMouseScroll(chat).register((s, x, y, h, v) -> {
            if (active() && view.inside(x, y)) {
                view.scroll(v);
                return false;
            }
            return true;
        });
        ScreenKeyboardEvents.allowKeyPress(chat).register((s, event) -> {
            boolean ctrl = (event.modifiers() & (2 | 8)) != 0;
            return !(active() && view.key(event.key(), ctrl));
        });
    }

    private static boolean shown() {
        return WynnChaYuan.config() != null && WynnChaYuan.config().chatCopy();
    }

    private static boolean active() {
        return open && shown();
    }

    private static float[] box(ChatScreen chat) {
        GuiCanvas measure = new GuiCanvas(null, chat.getFont(),
                Minecraft.getInstance().getWindow().getGuiScale());
        float w = measure.width(T.s("chatlog.button")) + 25;
        float[] frame = ChatLogHost.chatFrame(chat.width, chat.height);
        float right = frame[0] + frame[2];
        if (open) {
            float[] dock = ChatLogHost.dockRect(chat.width, chat.height);
            right = Math.max(right, dock[0] + dock[2]);
        }
        float x = Math.min(chat.width - 4 - w, right + GAP);
        float y = frame[1] + frame[3] - H;
        return new float[] {x, y, w, H};
    }

    private static boolean pill(ChatScreen chat, double x, double y) {
        if (!shown()) {
            return false;
        }
        float[] b = box(chat);
        return Ui.in(x, y, b[0], b[1], b[2], b[3]);
    }

    private static void render(ChatScreen chat, ChatLogView view, GuiGraphics g, int mouseX,
                               int mouseY) {
        if (!shown()) {
            return;
        }
        try {
            Minecraft mc = Minecraft.getInstance();
            GuiCanvas c = new GuiCanvas(g, chat.getFont(), mc.getWindow().getGuiScale());
            g.nextStratum();
            c.begin();
            try {
                if (open) {
                    view.render(c, chat.width, chat.height, mouseX, mouseY, Util.getMillis());
                    c.raise();
                }
                pillDraw(chat, c, mouseX, mouseY);
            } finally {
                c.end();
            }
        } catch (Throwable t) {
            ErrorDebug.note("ChatLogButton#render", null, t);
        }
    }

    private static void pillDraw(ChatScreen chat, GuiCanvas c, int mouseX, int mouseY) {
        float[] b = box(chat);
        boolean hot = Ui.in(mouseX, mouseY, b[0], b[1], b[2], b[3]);
        int accent = 0xFF000000 | (WynnChaYuan.config().themeARGB() & 0xFFFFFF);
        float r = H / 2f;
        Ui.halo(c, b[0], b[1], b[2], b[3], r, accent, hot ? 1f : 0f);
        if (open) {
            c.round(b[0], b[1], b[2], b[3], r, Ui.mix(accent, 0xFFFFFFFF, hot ? 0.12f : 0f));
        } else {
            Ui.pane(c, b[0], b[1], b[2], b[3], r, Ui.WINDOW_SOLID,
                    hot ? Ui.alpha(accent, 0xB0) : Ui.WIN_EDGE);
            c.fill(b[0] + r, b[1] + c.px(), b[0] + b[2] - r, b[1] + c.px() * 2,
                   hot ? 0x40FFFFFF : 0x22FFFFFF);
        }
        int ink = open ? Ui.ON_ACCENT : (hot ? Ui.TEXT : Ui.TEXT_2);
        c.icon(Icons.Icon.COPY, b[0] + 6, b[1] + 3, 7, open ? Ui.ON_ACCENT : (hot ? accent : Ui.TEXT_3));
        c.text(T.s("chatlog.button"), b[0] + 17, b[1] + 3, ink);
    }
}
