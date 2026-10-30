package com.wynnchayuan.client;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.Presence;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.FirstRunView;
import com.wynnchayuan.client.ui.Row;
import com.wynnchayuan.client.ui.Surface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class FirstRunScreen extends CanvasScreen {

    private static boolean shownThisSession = false;

    private final FirstRunView view;

    public FirstRunScreen(Screen parent) {
        super(T.c("firstrun.title"), parent);
        CollectorConfig config = WynnChaYuan.config();
        boolean askConsent = config.consent() == CollectorConfig.Consent.UNKNOWN;
        boolean askSetup = !config.setupDone();
        boolean upgrading = config.existedBefore();
        this.view = new FirstRunView(SHELL, new FirstRunView.Host() {
            @Override
            public boolean askConsent() {
                return askConsent;
            }

            @Override
            public boolean askSetup() {
                return askSetup;
            }

            @Override
            public boolean upgrading() {
                return upgrading;
            }

            @Override
            public void agree() {
                WynnChaYuan.setConsent(true);
            }

            @Override
            public void disagree() {
                WynnChaYuan.setConsent(false);
            }

            @Override
            public void openPolicy() {
                ConfirmLinkScreen.confirmLinkNow(FirstRunScreen.this, Presence.PRIVACY_URL);
            }

            @Override
            public void recommended() {
                boolean wasOn = cfg().translationsEnabled();
                cfg().applyRecommended();
                applyNameModes();
                if (!wasOn) {
                    WynnChaYuan.reloadTranslations();
                }
            }

            @Override
            public void settled() {
                if (!cfg().setupDone()) {
                    cfg().markSetupDone();
                }
            }

            @Override
            public List<Row> custom() {
                return rows();
            }

            @Override
            public void finish() {
                onClose();
            }
        });
    }

    @Override
    protected Surface view() {
        return view;
    }

    private static CollectorConfig cfg() {
        return WynnChaYuan.config();
    }

    private static Supplier<String> t(String key) {
        return () -> T.s(key);
    }

    private static void applyNameModes() {
        if (WynnChaYuan.translations() != null) {
            WynnChaYuan.translations().setNameMode(cfg().itemNames());
            WynnChaYuan.translations().setPageNameMode(cfg().pageNames());
        }
    }

    private static Row toggle(String key, java.util.function.BooleanSupplier get,
                              java.util.function.Consumer<Boolean> set) {
        return Row.toggle(key, t(key), t(key + ".hint")).on(get)
                .flip(() -> set.accept(!get.getAsBoolean()));
    }

    static List<Row> rows() {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.segment("dialogue.mode", t("dialogue.mode"), t("dialogue.mode.hint"))
                .option(T.s("mode.box"), Row.Tone.ACCENT)
                .option(T.s("mode.replace"), Row.Tone.REPLACE)
                .option(T.s("mode.off"), Row.Tone.OFF)
                .selected(() -> cfg().dialogueMode().ordinal())
                .pick(i -> cfg().setDialogueModes(CollectorConfig.DialogueMode.values()[i])));
        rows.add(Row.segment("world.chat", t("world.chat"), t("world.chat.hint"))
                .option(T.s("mode.replace"), Row.Tone.REPLACE)
                .option(T.s("mode.both"), Row.Tone.BOTH)
                .option(T.s("mode.off"), Row.Tone.OFF)
                .selected(() -> cfg().chatMode().ordinal())
                .pick(i -> cfg().setChatMode(CollectorConfig.ChatMode.values()[i])));
        rows.add(Row.segment("world.nametag", t("world.nametag"), t("world.nametag.hint"))
                .option(T.s("mode.lookat"), Row.Tone.ACCENT)
                .option(T.s("mode.replace"), Row.Tone.REPLACE)
                .option(T.s("mode.off"), Row.Tone.OFF)
                .selected(() -> cfg().nametagMode().ordinal())
                .pick(i -> cfg().setNametagMode(CollectorConfig.NametagMode.values()[i])));
        rows.add(Row.segment("items.tooltip", t("items.tooltip"), t("items.tooltip.hint"))
                .option(T.s("mode.panel"), Row.Tone.ACCENT)
                .option(T.s("mode.replace"), Row.Tone.REPLACE)
                .option(T.s("mode.off"), Row.Tone.OFF)
                .selected(() -> cfg().tooltipMode().ordinal())
                .pick(i -> cfg().setTooltipMode(CollectorConfig.TooltipMode.values()[i])));
        rows.add(Row.segment("items.names", t("items.names"), t("items.names.hint"))
                .option(T.s("mode.on"), Row.Tone.ACCENT)
                .option(T.s("items.names.both"), Row.Tone.BOTH)
                .option(T.s("mode.off"), Row.Tone.OFF)
                .selected(() -> cfg().itemNames().ordinal())
                .pick(i -> {
                    cfg().setItemNames(CollectorConfig.ItemNames.values()[i]);
                    applyNameModes();
                }));
        CollectorConfig.DialogueMode[] order = {
            CollectorConfig.DialogueMode.REPLACE, CollectorConfig.DialogueMode.PANEL,
            CollectorConfig.DialogueMode.OFF};
        rows.add(Row.segment("world.tracker", t("world.tracker"), t("world.tracker.hint"))
                .option(T.s("mode.replace"), Row.Tone.REPLACE)
                .option(T.s("mode.panel"), Row.Tone.ACCENT)
                .option(T.s("mode.off"), Row.Tone.OFF)
                .selected(() -> java.util.Arrays.asList(order).indexOf(cfg().trackerMode()))
                .pick(i -> cfg().setTrackerMode(order[i])));
        rows.add(toggle("world.scoreboard", () -> cfg().translateScoreboard(),
                on -> cfg().setTranslateScoreboard(on)));
        rows.add(toggle("world.titles", () -> cfg().translateTitles(),
                on -> cfg().setTranslateTitles(on)));
        rows.add(toggle("world.bossbar", () -> cfg().translateBossBar(),
                on -> cfg().setTranslateBossBar(on)));
        rows.add(toggle("world.wynntils", () -> cfg().wynntilsUi(),
                on -> cfg().setWynntilsUi(on)));
        return rows;
    }

    public static void clientTick() {
        CollectorConfig config = WynnChaYuan.config();
        if (shownThisSession || config == null || !config.needsFirstRun()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getOverlay() != null) {
            return;
        }
        Screen screen = mc.screen;
        boolean title = screen instanceof TitleScreen;
        boolean world = screen == null && mc.level != null && mc.player != null;
        if (!title && !world) {
            return;
        }
        shownThisSession = true;
        mc.setScreen(new FirstRunScreen(screen));
    }

    @Override
    public void onClose() {
        if (view.step() == FirstRunView.Step.CUSTOM) {
            cfg().markSetupDone();
        }
        super.onClose();
    }
}
