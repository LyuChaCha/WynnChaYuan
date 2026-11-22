package com.wynnchayuan.client;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.ChatLogView;
import com.wynnchayuan.client.ui.Icons;
import com.wynnchayuan.client.ui.Ui;
import com.wynnchayuan.translate.ErrorDebug;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
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
    private static final float EDGE = 2;
    private static final float HOTBAR_HALF = 91;

    private static boolean open;
    private static float[] press;
    private static float[] dragged;

    private ChatLogButton() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (screen instanceof ChatScreen chat) {
                attach(chat);
            }
        });
        HudElementRegistry.replaceElement(VanillaHudElements.CHAT, chat -> (g, tick) -> {
            if (!coversChat()) {
                chat.render(g, tick);
            }
        });
    }

    private static boolean coversChat() {
        return active() && Minecraft.getInstance().screen instanceof ChatScreen;
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
            float[] b = pill(chat, event.x(), event.y());
            if (b != null) {
                if (event.button() == 1) {
                    WynnChaYuan.config().setChatLogButtonPos(-1, -1);
                    CanvasScreen.SHELL.click();
                } else if (event.button() == 0) {
                    press = new float[] {(float) event.x(), (float) event.y(),
                            (float) event.x() - b[0], (float) event.y() - b[1]};
                    dragged = null;
                }
                return false;
            }
            return !(active() && view.mouseDown(event.x(), event.y(), event.button()));
        });
        ScreenMouseEvents.allowMouseDrag(chat).register((s, event, dx, dy) -> {
            if (press != null) {
                if (dragged != null || Math.abs(event.x() - press[0]) + Math.abs(event.y() - press[1]) > 3) {
                    dragged = new float[] {(float) event.x() - press[2], (float) event.y() - press[3]};
                }
                return false;
            }
            return !(active() && view.mouseDrag(event.x(), event.y()));
        });
        ScreenMouseEvents.allowMouseRelease(chat).register((s, event) -> {
            if (press != null) {
                release(chat);
                return false;
            }
            return !(active() && view.mouseUp());
        });
        ScreenEvents.remove(chat).register(s -> {
            press = null;
            dragged = null;
        });
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

    private static void release(ChatScreen chat) {
        if (dragged == null) {
            open = !open;
            CanvasScreen.SHELL.click();
        } else {
            float[] b = box(chat);
            if (b != null) {
                WynnChaYuan.config().setChatLogButtonPos(b[0] / chat.width, b[1] / chat.height);
            }
        }
        press = null;
        dragged = null;
    }

    private static boolean shown() {
        return WynnChaYuan.config() != null && WynnChaYuan.config().chatCopy()
                && WynnChaYuan.config().chatLogButton();
    }

    private static boolean active() {
        return open && shown();
    }

    private static float[] box(ChatScreen chat) {
        GuiCanvas measure = new GuiCanvas(null, chat.getFont(),
                Minecraft.getInstance().getWindow().getGuiScale());
        float w = measure.width(T.s("chatlog.button")) + 25;
        if (dragged != null) {
            return clampTo(dragged[0], dragged[1], w, chat.width, chat.height);
        }
        double[] saved = WynnChaYuan.config().chatLogButtonPos();
        if (saved != null) {
            return clampTo((float) (saved[0] * chat.width), (float) (saved[1] * chat.height), w,
                           chat.width, chat.height);
        }
        float[] frame = ChatLogHost.chatFrame(chat.width, chat.height);
        float[] dock = open ? ChatLogHost.dockRect(chat.width, chat.height) : null;
        return place(frame, dock, w, chat.width);
    }

    static float[] place(float[] frame, float[] dock, float w, float screenW) {
        float right = frame[0] + frame[2];
        float top = frame[1];
        if (dock != null) {
            right = Math.max(right, dock[0] + dock[2]);
            top = Math.min(top, dock[1]);
        }
        float side = right + GAP;
        if (side + w <= Math.min(screenW - EDGE, screenW / 2f - HOTBAR_HALF - GAP)) {
            return new float[] {side, frame[1] + frame[3] - H, w, H};
        }
        float x = Math.max(EDGE, Math.min(screenW - EDGE - w, right - w));
        float above = top - GAP - H;
        if (above >= EDGE) {
            return new float[] {x, above, w, H};
        }
        if (dock != null) {
            return null;
        }
        return new float[] {x, top + EDGE, w, H};
    }

    static float[] clampTo(float x, float y, float w, float screenW, float screenH) {
        float cx = Math.max(EDGE, Math.min(screenW - EDGE - w, x));
        float cy = Math.max(EDGE, Math.min(screenH - EDGE - H, y));
        return new float[] {cx, cy, w, H};
    }

    private static float[] pill(ChatScreen chat, double x, double y) {
        if (!shown()) {
            return null;
        }
        float[] b = box(chat);
        return b != null && Ui.in(x, y, b[0], b[1], b[2], b[3]) ? b : null;
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
        if (b == null) {
            return;
        }
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
