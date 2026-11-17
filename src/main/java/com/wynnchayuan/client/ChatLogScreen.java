package com.wynnchayuan.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.wynnchayuan.ModConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.ChatLog;
import com.wynnchayuan.client.ui.ChatLogView;
import com.wynnchayuan.client.ui.Styled;
import com.wynnchayuan.client.ui.Surface;
import com.wynnchayuan.render.PanelShot;
import com.wynnchayuan.translate.ErrorDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ChatLogScreen extends CanvasScreen {

    private static final DateTimeFormatter CLOCK =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT).withZone(ZoneId.systemDefault());

    private final ChatLogView view;

    private long builtVersion = -1;
    private int builtWidth = -1;
    private ModConfig.ChatMode builtMode;
    private List<ChatLogView.Msg> built = List.of();

    public ChatLogScreen(Screen parent) {
        super(T.c("chatlog.title"), parent);
        this.view = new ChatLogView(SHELL, new ChatLogView.Host() {
            @Override
            public List<ChatLogView.Msg> messages(int wrapWidth) {
                return build(wrapWidth);
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
                return ChatLogScreen.this.copyText(keys);
            }

            @Override
            public void close() {
                onClose();
            }
        });
    }

    @Override
    protected Surface view() {
        return view;
    }

    private List<ChatLogView.Msg> build(int wrap) {
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
                for (FormattedCharSequence seq : this.font.split(part, wrap)) {
                    lines.add(GameLine.of(seq, this.font));
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

    private boolean copyText(List<Object> keys) {
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
            this.minecraft.keyboardHandler.setClipboard(out.toString());
            return true;
        } catch (Throwable t) {
            ErrorDebug.note("ChatLogScreen#copyText", null, t);
            return false;
        }
    }

    @Override
    public void tick() {
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
                    ok = ImageClipboard.copy(card, "ChatLogScreen#image");
                } catch (Throwable t) {
                    ErrorDebug.note("ChatLogScreen#image", null, t);
                    ok = false;
                }
                boolean done = ok;
                mc.execute(() -> view.report(done ? "chatlog.copied.image" : "chatlog.image.failed",
                        done));
            });
        } catch (Throwable t) {
            ErrorDebug.note("ChatLogScreen#tick", null, t);
            view.report("chatlog.image.failed", false);
        }
    }
}
