package com.wynnchayuan.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.wynnchayuan.ModConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.ChatLog;
import com.wynnchayuan.client.ui.ChatLogView;
import com.wynnchayuan.client.ui.Styled;
import com.wynnchayuan.render.PanelShot;
import com.wynnchayuan.translate.ErrorDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ChatLogHost implements ChatLogView.Host {

    private static final DateTimeFormatter CLOCK =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT).withZone(ZoneId.systemDefault());

    private final Font font;
    private final Runnable close;
    private final boolean docked;

    private long builtVersion = -1;
    private int builtWidth = -1;
    private ModConfig.ChatMode builtMode;
    private List<ChatLogView.Msg> built = List.of();

    ChatLogHost(Font font, Runnable close, boolean docked) {
        this.font = font;
        this.close = close;
        this.docked = docked;
    }

    @Override
    public List<ChatLogView.Msg> messages(int wrap) {
        ModConfig.ChatMode mode = WynnChaYuan.config().chatMode();
        long version = ChatLog.version();
        if (version == builtVersion && wrap == builtWidth && mode == builtMode) {
            return built;
        }
        boolean replace = mode == ModConfig.ChatMode.REPLACE;
        boolean both = mode == ModConfig.ChatMode.BOTH;
        List<ChatLogView.Msg> out = new ArrayList<>();
        for (ChatLog.Entry entry : ChatLog.oldestFirst()) {
            List<Styled> lines = new ArrayList<>();
            for (Component part : entry.shown(replace, both)) {
                for (FormattedCharSequence seq : font.split(part, wrap)) {
                    lines.add(GameLine.of(seq, font));
                }
            }
            if (lines.isEmpty()) {
                continue;
            }
            String time = entry.at() > 0 ? CLOCK.format(Instant.ofEpochMilli(entry.at())) : "";
            out.add(new ChatLogView.Msg(entry, List.copyOf(lines), time));
        }
        built = List.copyOf(out);
        builtVersion = version;
        builtWidth = wrap;
        builtMode = mode;
        return built;
    }

    @Override
    public boolean recording() {
        return WynnChaYuan.config().chatCopy();
    }

    @Override
    public void startRecording() {
        if (!WynnChaYuan.config().chatCopy()) {
            WynnChaYuan.config().toggleChatCopy();
        }
    }

    @Override
    public boolean multi() {
        return WynnChaYuan.config().chatCopyMulti();
    }

    @Override
    public void setMulti(boolean multi) {
        WynnChaYuan.config().setChatCopyMulti(multi);
    }

    @Override
    public boolean copyText(List<Object> keys) {
        ModConfig.ChatMode mode = WynnChaYuan.config().chatMode();
        StringBuilder out = new StringBuilder();
        for (Object key : keys) {
            if (!(key instanceof ChatLog.Entry entry)) {
                continue;
            }
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(entry.forMode(mode == ModConfig.ChatMode.REPLACE,
                    mode == ModConfig.ChatMode.BOTH));
        }
        if (out.length() == 0) {
            return false;
        }
        try {
            Minecraft.getInstance().keyboardHandler.setClipboard(out.toString());
            return true;
        } catch (Throwable t) {
            ErrorDebug.note("ChatLogHost#copyText", null, t);
            return false;
        }
    }

    @Override
    public void close() {
        close.run();
    }

    @Override
    public float[] dock(int screenW, int screenH) {
        if (!docked) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        double scale = mc.options.chatScale().get();
        float w = (float) (ChatComponent.getWidth(mc.options.chatWidth().get()) * scale) + 16;
        float h = (float) (ChatComponent.getHeight(mc.options.chatHeightFocused().get()) * scale) + 52;
        w = Math.max(Math.min(280, screenW - 8), Math.min(screenW - 8, w));
        h = Math.max(Math.min(120, screenH - 40), Math.min(screenH - 40, h));
        return new float[] {2, screenH - 16 - h, w, h};
    }

    void tick(ChatLogView view) {
        float[] rect = view.pendingShot();
        if (rect == null) {
            return;
        }
        view.shotTaken();
        Minecraft mc = Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();
        int px = (int) Math.floor(rect[0] * scale);
        int py = (int) Math.floor(rect[1] * scale);
        int pw = (int) Math.ceil(rect[2] * scale);
        int ph = (int) Math.ceil(rect[3] * scale);
        try {
            Screenshot.takeScreenshot(mc.getMainRenderTarget(), full -> {
                boolean ok;
                try (NativeImage image = full; NativeImage card = PanelShot.crop(image, px, py, pw, ph)) {
                    ok = ImageClipboard.copy(card, "ChatLogHost#image");
                } catch (Throwable t) {
                    ErrorDebug.note("ChatLogHost#image", null, t);
                    ok = false;
                }
                boolean done = ok;
                mc.execute(() -> view.report(done ? "chatlog.copied.image" : "chatlog.image.failed",
                        done));
            });
        } catch (Throwable t) {
            ErrorDebug.note("ChatLogHost#tick", null, t);
            view.report("chatlog.image.failed", false);
        }
    }
}
