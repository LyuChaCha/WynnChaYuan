package com.wynnchayuan.client;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.Icons;
import com.wynnchayuan.client.ui.Preview;
import com.wynnchayuan.client.ui.Row;
import com.wynnchayuan.client.ui.SettingsView;
import com.wynnchayuan.client.ui.Surface;
import com.wynnchayuan.client.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

/**
 * 名牌的進階設定：怎麼顯示，以及什麼情況下才算「你在看它」。
 *
 * <p>從設定畫面「世界與聊天 → 名牌與漂浮字」旁邊的「進階…」進來。內容只有四列，
 * 用的是跟設定畫面同一套列（{@link SettingsView} 的小視窗模式）——分段、滑桿、
 * 逐項重置、滑過的說明都一樣，不另外做一套。
 */
public final class NametagScreen extends CanvasScreen {

    private final SettingsView view;
    private final Preview.State preview = new Preview.State();

    public NametagScreen(Screen parent) {
        super(T.c("nametag.title"), parent);
        this.view = new SettingsView(List.of(new Row.Tab("nametag", () -> T.s("nametag.title"),
                () -> T.s("nametag.about1"), Icons.Icon.COMPASS, Preview.Scene.WORLD,
                List.of(new Row.Group(() -> T.s("world.nametag"), false, rows())))),
                new Host(), 0, true);
    }

    @Override
    protected Surface view() {
        return view;
    }

    private static CollectorConfig cfg() {
        return WynnChaYuan.config();
    }

    private static int holdSeconds() {
        int ms = cfg().nametagHoldMs();
        return ms == Integer.MAX_VALUE ? 0 : ms / 1000;
    }

    private static List<Row> rows() {
        Runnable next = () -> cfg().cycleNametagMode();
        return List.of(
                Row.segment("nametag.mode", () -> T.s("world.nametag"),
                                () -> T.s(switch (cfg().nametagMode()) {
                                    case OFF -> "nametag.desc.off";
                                    case LOOK_AT -> "nametag.desc.lookat";
                                    case REPLACE -> "nametag.desc.replace";
                                }))
                        .option(T.s("mode.lookat"), Row.Tone.ACCENT)
                        .option(T.s("mode.replace"), Row.Tone.REPLACE)
                        .option(T.s("mode.off"), Row.Tone.OFF)
                        .selected(() -> cfg().nametagMode().ordinal())
                        .pick(i -> {
                            for (int n = 0; n < 3 && cfg().nametagMode().ordinal() != i; n++) {
                                next.run();
                            }
                        })
                        .reset(() -> cfg().nametagMode() == CollectorConfig.NametagMode.REPLACE,
                                () -> {
                                    for (int n = 0; n < 3 && cfg().nametagMode()
                                            != CollectorConfig.NametagMode.REPLACE; n++) {
                                        next.run();
                                    }
                                }),
                Row.slider("nametag.hold", () -> T.s("nametag.hold"), () -> T.s("nametag.about1"))
                        .range(0, 15)
                        .value(() -> Math.min(15, holdSeconds()))
                        .slide(v -> cfg().setNametagHoldSecondsLive(v))
                        .format(v -> v == 0 ? T.s("unit.forever") : T.s("unit.seconds", v))
                        .commit(cfg()::saveIfDirty)
                        .reset(() -> holdSeconds() == 1, () -> {
                            cfg().setNametagHoldSecondsLive(1);
                            cfg().saveIfDirty();
                        }),
                Row.slider("nametag.range", () -> T.s("nametag.range"), () -> T.s("nametag.about2"))
                        .range(2, 64)
                        .value(() -> (int) Math.round(cfg().nametagRange()))
                        .slide(v -> cfg().setNametagRangeLive(v))
                        .format(String::valueOf)
                        .commit(cfg()::saveIfDirty)
                        .reset(() -> Math.round(cfg().nametagRange()) == 6, () -> {
                            cfg().setNametagRangeLive(6);
                            cfg().saveIfDirty();
                        }),
                Row.slider("nametag.angle", () -> T.s("nametag.angle"), () -> T.s("nametag.about2"))
                        .range(1, 45)
                        .value(() -> (int) Math.round(cfg().nametagAngle()))
                        .slide(v -> cfg().setNametagAngleLive(v))
                        .format(v -> v + "°")
                        .commit(cfg()::saveIfDirty)
                        .reset(() -> Math.round(cfg().nametagAngle()) == 6, () -> {
                            cfg().setNametagAngleLive(6);
                            cfg().saveIfDirty();
                        }));
    }

    @Override
    public void removed() {
        cfg().saveIfDirty();
        super.removed();
    }

    private final class Host implements SettingsView.Host {
        private String status = "";
        private long statusAt;

        @Override
        public int accent() {
            return SHELL.accent();
        }

        @Override
        public int frame() {
            return SHELL.frame();
        }

        @Override
        public int tone(Row.Tone tone) {
            int accent = cfg().themeARGB();
            return switch (tone) {
                case REPLACE -> ModeColours.replace(accent);
                case BOTH -> ModeColours.both(accent);
                case OFF -> 0x40FFFFFF;
                default -> accent;
            };
        }

        @Override
        public String tr(String key, Object... args) {
            return T.s(key, args);
        }

        @Override
        public String title() {
            return WynnChaYuan.MOD_NAME;
        }

        @Override
        public String tagline() {
            return "";
        }

        @Override
        public String version() {
            return "";
        }

        @Override
        public SettingsView.Status status() {
            if (!status.isEmpty() && System.currentTimeMillis() - statusAt < 4000) {
                return new SettingsView.Status(status, cfg().themeARGB());
            }
            return new SettingsView.Status(T.s("nametag.about2"), Ui.TEXT_3);
        }

        @Override
        public boolean hasUpdate() {
            return false;
        }

        @Override
        public boolean blurred() {
            return SHELL.blurred();
        }

        @Override
        public void openNotice() { }

        @Override
        public void openUpdates() { }

        @Override
        public void openCredits() { }

        @Override
        public void done() {
            onClose();
        }

        @Override
        public String clipboard() {
            return Minecraft.getInstance().keyboardHandler.getClipboard();
        }

        @Override
        public void setClipboard(String text) {
            Minecraft.getInstance().keyboardHandler.setClipboard(text);
        }

        @Override
        public void click() {
            SHELL.click();
        }

        @Override
        public Preview.State preview() {
            CollectorConfig c = cfg();
            preview.nametag = c.nametagMode().ordinal();
            preview.chat = c.chatMode().ordinal();
            preview.titles = c.translateTitles();
            preview.bossbar = c.translateBossBar();
            preview.tracker = switch (c.trackerMode()) {
                case REPLACE -> 0;
                case PANEL -> 1;
                case OFF -> 2;
            };
            preview.objectives = c.translateObjectives();
            preview.scoreboard = c.translateScoreboard();
            preview.heldItem = c.translateHeldItem();
            preview.translate = SettingsScreen::previewLookup;
            return preview;
        }

        @Override
        public void resetDone(String what) {
            status = T.s("reset.done", what);
            statusAt = System.currentTimeMillis();
        }
    }
}
