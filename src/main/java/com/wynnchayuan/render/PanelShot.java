package com.wynnchayuan.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.ErrorDebug;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class PanelShot {

    private static final String FOLDER = "wynnchayuan";

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss", Locale.ROOT);

    private static volatile int lastX;
    private static volatile int lastY;
    private static volatile int lastW;
    private static volatile int lastH;
    private static volatile String lastName = "panel";

    private static volatile long lastFrame = 0;

    private static volatile boolean pending = false;

    private static KeyMapping bound;

    private PanelShot() {}

    private static void log(String stage) {
        try {
            java.nio.file.Path dir = com.wynnchayuan.ConfigFolder.debugDir(
                    net.fabricmc.loader.api.FabricLoader.getInstance()
                            .getConfigDir().resolve(WynnChaYuan.MOD_ID));
            java.nio.file.Files.writeString(dir.resolve("shot-debug.txt"),
                    java.time.LocalTime.now().withNano(0) + "  " + stage
                            + System.lineSeparator(),
                    java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Throwable t) {
            ErrorDebug.note("PanelShot#log", null, t);
        }
    }

    public static void bind(KeyMapping mapping) {
        bound = mapping;
        String where = "?";
        try {
            var key = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
                    .getBoundKeyOf(mapping);
            if (key != null) {
                where = key.getName();
            }
        } catch (Throwable t) {
            where = "讀不到（" + t.getClass().getSimpleName() + "）";
        }
        log("bind：截圖鍵已交給 PanelShot（目前綁定 " + where + "）");
    }

    public static boolean hasKey() {
        if (bound == null) {
            return false;
        }
        try {
            var key = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
                    .getBoundKeyOf(bound);
            return key != null
                    && key.getValue() != com.mojang.blaze3d.platform.InputConstants.UNKNOWN.getValue();
        } catch (Throwable t) {
            ErrorDebug.note("PanelShot#hasKey", null, t);
            return false;
        }
    }

    public static String keyName() {
        if (bound == null) {
            return "未設定";
        }
        try {
            var key = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
                    .getBoundKeyOf(bound);
            if (key == null || key.getValue() == com.mojang.blaze3d.platform.InputConstants.UNKNOWN.getValue()) {
                return "未設定";
            }
            return bound.getTranslatedKeyMessage().getString();
        } catch (Throwable t) {
            ErrorDebug.note("PanelShot#keyName", null, t);
            return "未設定";
        }
    }

    public static String conflict() {
        if (bound == null) {
            return null;
        }
        try {
            var mine = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
                    .getBoundKeyOf(bound);
            if (mine == null
                    || mine.getValue() == com.mojang.blaze3d.platform.InputConstants.UNKNOWN.getValue()) {
                return null;
            }
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc == null || mc.options == null) {
                return null;
            }
            for (KeyMapping other : mc.options.keyMappings) {
                if (other == bound) {
                    continue;
                }
                var theirs = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
                        .getBoundKeyOf(other);
                if (theirs != null && theirs.getValue() == mine.getValue()) {
                    return net.minecraft.network.chat.Component
                            .translatable(other.getName()).getString();
                }
            }
        } catch (Throwable t) {
            ErrorDebug.note("PanelShot#conflict", null, t);
        }
        return null;
    }

    public static void request() {
        if (WynnChaYuan.config().shotMode() == com.wynnchayuan.CollectorConfig
                .ShotMode.OFF) {
            return;
        }
        pending = true;
        log("request：收到，準備拍");
        tell(Component.literal(com.wynnchayuan.client.T.s("shot.taking")).withStyle(ChatFormatting.GRAY));
    }

    // MC 1.21.5 起 GUI 延後繪製，改在 tick 讀 framebuffer
    private static boolean wasDown = false;

    public static void listen() {
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register(
                (client, screen, width, height) -> {
                    // afterKeyPress 只在畫面沒吃鍵時跑，背包與 Wynntils 會吃鍵
                    net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
                            .beforeKeyPress(screen).register((s, keyEvent) -> {
                                if (bound != null && bound.matches(keyEvent)) {
                                    log("畫面鍵盤事件（前置）：收到截圖鍵（"
                                            + screen.getClass().getSimpleName() + "）");
                                    request();
                                } else {
                                    noteOtherKey(screen, keyEvent);
                                }
                            });
                    net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
                            .afterKeyPress(screen).register((s, keyEvent) -> {
                                if (bound != null && bound.matches(keyEvent)) {
                                    log("畫面鍵盤事件（後置）：收到截圖鍵（"
                                            + screen.getClass().getSimpleName() + "）");
                                    request();
                                }
                            });
                });
        log("listen：已掛上畫面鍵盤事件（前置與後置）");
    }

    private static void noteOtherKey(net.minecraft.client.gui.screens.Screen screen,
                                     Object keyEvent) {
        if (keyLogged >= KEY_LOG_LIMIT) {
            return;
        }
        keyLogged++;
        log("畫面鍵盤事件：收到別的鍵（" + keyEvent + "）於 "
                + screen.getClass().getSimpleName() + "，截圖鍵是 " + keyName());
    }

    private static final int KEY_LOG_LIMIT = 20;

    private static int keyLogged = 0;

    public static void tick() {
        if (!pending) {
            return;
        }
        pending = false;
        if (ready()) {
            log("tick：面板還在（" + lastW + "x" + lastH + "），開始拍");
            capture();
            return;
        }
        if (!com.wynnchayuan.render.DialogueOverlay.hasContent()) {
            log("tick：畫面上沒有任何譯文（w=" + lastW + " h=" + lastH
                    + " 距上次繪製=" + (System.currentTimeMillis() - lastFrame)
                    + "ms），不拍");
            tell(com.wynnchayuan.client.T.c("shot.nothing")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        log("tick：沒有可裁的框但對話框有譯文（距上次繪製="
                + (System.currentTimeMillis() - lastFrame) + "ms），改拍整個畫面");
        captureScreen();
    }

    public static void note(int x, int y, int w, int h, String name) {
        lastX = x;
        lastY = y;
        lastW = w;
        lastH = h;
        if (name != null && !name.isBlank()) {
            lastName = name;
        }
        lastFrame = System.currentTimeMillis();
    }

    public static boolean ready() {
        return lastW > 0 && lastH > 0
                && System.currentTimeMillis() - lastFrame < 500;
    }

    public static void capture() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || !ready()) {
            return;
        }
        double scale = mc.getWindow().getGuiScale();
        shoot(mc, (int) Math.floor(lastX * scale), (int) Math.floor(lastY * scale),
              (int) Math.ceil(lastW * scale), (int) Math.ceil(lastH * scale),
              safe(lastName));
    }

    public static void captureScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return;
        }
        shoot(mc, 0, 0, mc.getWindow().getWidth(), mc.getWindow().getHeight(),
              "screen");
    }

    private static void shoot(Minecraft mc, int px, int py, int pw, int ph,
                              String name) {
        Screenshot.takeScreenshot(mc.getMainRenderTarget(), full -> {
            NativeImage cropped;
            try (NativeImage image = full) {
                cropped = crop(image, px, py, pw, ph);
            } catch (Exception e) {
                tell(Component.literal(com.wynnchayuan.client.T.s("shot.capture_failed", e.getMessage()))
                        .withStyle(ChatFormatting.RED));
                return;
            }
            mc.execute(() -> {
                try {
                    mc.setScreen(new com.wynnchayuan.client.ShotScreen(
                            cropped, name, mc.screen));
                } catch (Throwable t) {
                    com.wynnchayuan.translate.ErrorDebug.note(
                            "shot.screen", name, t);
                    tell(Component.literal(com.wynnchayuan.client.T.s("shot.panel_failed",
                            t.getClass().getSimpleName()))
                            .withStyle(ChatFormatting.YELLOW));
                    save(cropped, name);
                }
            });
        });
    }

    private static NativeImage crop(NativeImage full, int x, int y, int w, int h) {
        int left = Math.max(0, Math.min(x, full.getWidth() - 1));
        int top = Math.max(0, Math.min(y, full.getHeight() - 1));
        int right = Math.max(left + 1, Math.min(x + w, full.getWidth()));
        int bottom = Math.max(top + 1, Math.min(y + h, full.getHeight()));

        NativeImage out = new NativeImage(right - left, bottom - top, false);
        for (int row = top; row < bottom; row++) {
            for (int col = left; col < right; col++) {
                out.setPixel(col - left, row - top, full.getPixel(col, row));
            }
        }
        return out;
    }

    private static void save(NativeImage image, String name) {
        Minecraft mc = Minecraft.getInstance();
        try (NativeImage shot = image) {
            Path dir = mc.gameDirectory.toPath()
                    .resolve(Screenshot.SCREENSHOT_DIR).resolve(FOLDER);
            Files.createDirectories(dir);
            Path file = dir.resolve(safe(name) + "_"
                    + LocalDateTime.now().format(STAMP) + ".png");
            shot.writeToFile(file);
            tell(saved(file));
        } catch (Exception e) {
            tell(Component.literal(com.wynnchayuan.client.T.s("shot.save_failed", e.getMessage()))
                    .withStyle(ChatFormatting.RED));
        }
    }

    public static Component saved(Path file) {
        Path abs = file.toAbsolutePath();
        Path dir = abs.getParent();
        return Component.literal(com.wynnchayuan.client.T.s("shot.saved"))
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(shown(abs))
                        .withStyle(Style.EMPTY
                                .withColor(ChatFormatting.WHITE)
                                .withUnderlined(true)
                                .withClickEvent(new ClickEvent.OpenFile(abs.toString()))
                                .withHoverEvent(new net.minecraft.network.chat.HoverEvent
                                        .ShowText(Component.literal(
                                                com.wynnchayuan.client.T.s("shot.click_open") + System.lineSeparator()
                                                + abs)))))
                .append(Component.literal("  " + com.wynnchayuan.client.T.s("shot.open_folder"))
                        .withStyle(Style.EMPTY
                                .withColor(ChatFormatting.GOLD)
                                .withClickEvent(new ClickEvent.OpenFile(
                                        dir == null ? abs.toString() : dir.toString()))
                                .withHoverEvent(new net.minecraft.network.chat.HoverEvent
                                        .ShowText(Component.literal(
                                                dir == null ? abs.toString()
                                                            : dir.toString())))));
    }

    private static String shown(Path abs) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.gameDirectory != null) {
                return mc.gameDirectory.toPath().toAbsolutePath()
                        .relativize(abs).toString().replace('\\', '/');
            }
        } catch (Throwable t) {
            ErrorDebug.note("PanelShot#shown", null, t);
        }
        return abs.toString();
    }

    public static void announce(Component message) {
        tell(message);
    }

    private static String safe(String name) {
        StringBuilder out = new StringBuilder();
        name.codePoints().forEach(cp -> {
            if (cp == '/' || cp == '\\' || cp == ':' || cp == '*' || cp == '?'
                    || cp == '"' || cp == '<' || cp == '>' || cp == '|'
                    || Character.isISOControl(cp)) {
                out.append('_');
            } else {
                out.appendCodePoint(cp);
            }
        });
        String clean = out.toString().strip();
        if (clean.isEmpty()) {
            clean = "panel";
        }
        return clean.length() > 60 ? clean.substring(0, 60) : clean;
    }

    // 截圖回呼在算繪執行緒，displayClientMessage 不會顯示
    private static void tell(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            mc.execute(() -> say(mc, message));
        }
    }

    private static void toast(Minecraft mc, Component message) {
        try {
            net.minecraft.client.gui.components.toasts.SystemToast.addOrUpdate(
                    mc.getToastManager(),
                    net.minecraft.client.gui.components.toasts.SystemToast
                            .SystemToastId.PERIODIC_NOTIFICATION,
                    Component.literal(WynnChaYuan.MOD_NAME),
                    message);
        } catch (Throwable t) {
            ErrorDebug.note("PanelShot#toast", null, t);
        }
    }

    private static void say(Minecraft mc, Component message) {
        toast(mc, message);
        if (mc.player != null) {
            Component line = Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(WynnChaYuan.MOD_NAME)
                            .withStyle(ChatFormatting.AQUA))
                    .append(Component.literal("] ")
                            .withStyle(ChatFormatting.DARK_GRAY))
                    .append(message);
            com.wynnchayuan.capture.OwnOutputs.note(line);
            mc.player.displayClientMessage(line, false);
            mc.player.playSound(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1f, 1f);
        }
    }
}
