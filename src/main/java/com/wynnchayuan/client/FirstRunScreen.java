package com.wynnchayuan.client;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.Presence;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.FirstRunView;
import com.wynnchayuan.client.ui.Icons;
import com.wynnchayuan.client.ui.Preview;
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
        boolean askNotice = !config.noticeDismissed();
        boolean askConsent = config.consent() == CollectorConfig.Consent.UNKNOWN;
        boolean askSetup = !config.setupDone();
        boolean upgrading = config.existedBefore();
        this.view = new FirstRunView(SHELL, new FirstRunView.Host() {
            @Override
            public boolean askNotice() {
                return askNotice;
            }

            @Override
            public void noticeRead() {
                if (!cfg().noticeDismissed()) {
                    cfg().setNoticeDismissed(true);
                }
            }

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
            public List<Row.Tab> custom() {
                return tabs();
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

    private static Row mode(String key, String[] labels, Row.Tone[] tones,
                            java.util.function.IntSupplier get,
                            java.util.function.IntConsumer set) {
        Row row = Row.segment(key, t(key), t(key + ".hint"));
        for (int i = 0; i < labels.length; i++) {
            row.option(T.s(labels[i]), tones[i]);
        }
        return row.selected(get).pick(set);
    }

    private static final Row.Tone[] BOX_REPLACE_OFF = {
        Row.Tone.ACCENT, Row.Tone.REPLACE, Row.Tone.OFF};

    static List<Row.Tab> tabs() {
        List<Row.Tab> tabs = new ArrayList<>();
        tabs.add(new Row.Tab("items", t("tab.items"), t("tab.items.about"), Icons.Icon.BOW,
                Preview.Scene.TOOLTIP, List.of(new Row.Group(t("group.items"), false, List.of(
                mode("items.tooltip", new String[] {"mode.panel", "mode.replace", "mode.off"},
                        BOX_REPLACE_OFF, () -> cfg().tooltipMode().ordinal(),
                        i -> cfg().setTooltipMode(CollectorConfig.TooltipMode.values()[i])),
                mode("items.names", new String[] {"mode.on", "items.names.both", "mode.off"},
                        new Row.Tone[] {Row.Tone.ACCENT, Row.Tone.BOTH, Row.Tone.OFF},
                        () -> cfg().itemNames().ordinal(), i -> {
                            cfg().setItemNames(CollectorConfig.ItemNames.values()[i]);
                            applyNameModes();
                        }))),
                new Row.Group(t("group.mods"), false, List.of(
                toggle("world.wynntils", () -> cfg().wynntilsUi(),
                        on -> cfg().setWynntilsUi(on)))))));
        tabs.add(new Row.Tab("dialogue", t("tab.dialogue"), t("tab.dialogue.about"),
                Icons.Icon.BUBBLE, Preview.Scene.DIALOGUE, List.of(
                new Row.Group(t("group.dialogue"), false, List.of(
                mode("dialogue.mode", new String[] {"mode.box", "mode.replace", "mode.off"},
                        BOX_REPLACE_OFF, () -> cfg().dialogueMode().ordinal(),
                        i -> cfg().setDialogueMode(CollectorConfig.DialogueMode.values()[i])),
                mode("dialogue.choices", new String[] {"mode.box", "mode.replace", "mode.off"},
                        BOX_REPLACE_OFF, () -> cfg().choiceMode().ordinal(),
                        i -> cfg().setChoiceMode(CollectorConfig.DialogueMode.values()[i])))))));
        CollectorConfig.DialogueMode[] order = {
            CollectorConfig.DialogueMode.REPLACE, CollectorConfig.DialogueMode.PANEL,
            CollectorConfig.DialogueMode.OFF};
        tabs.add(new Row.Tab("world", t("tab.world"), t("tab.world.about"), Icons.Icon.COMPASS,
                Preview.Scene.WORLD, List.of(new Row.Group(t("group.world"), false, List.of(
                mode("world.nametag", new String[] {"mode.lookat", "mode.replace", "mode.off"},
                        BOX_REPLACE_OFF, () -> cfg().nametagMode().ordinal(),
                        i -> cfg().setNametagMode(CollectorConfig.NametagMode.values()[i])),
                mode("world.chat", new String[] {"mode.replace", "mode.both", "mode.off"},
                        new Row.Tone[] {Row.Tone.REPLACE, Row.Tone.BOTH, Row.Tone.OFF},
                        () -> cfg().chatMode().ordinal(),
                        i -> cfg().setChatMode(CollectorConfig.ChatMode.values()[i])),
                mode("world.tracker", new String[] {"mode.replace", "mode.panel", "mode.off"},
                        new Row.Tone[] {Row.Tone.REPLACE, Row.Tone.ACCENT, Row.Tone.OFF},
                        () -> java.util.Arrays.asList(order).indexOf(cfg().trackerMode()),
                        i -> cfg().setTrackerMode(order[i])),
                toggle("world.scoreboard", () -> cfg().translateScoreboard(),
                        on -> cfg().setTranslateScoreboard(on)),
                toggle("world.titles", () -> cfg().translateTitles(),
                        on -> cfg().setTranslateTitles(on)),
                toggle("world.bossbar", () -> cfg().translateBossBar(),
                        on -> cfg().setTranslateBossBar(on)))))));
        tabs.add(new Row.Tab("privacy", t("firstrun.privacy.short"), t("firstrun.privacy.about"),
                Icons.Icon.LEAF, Preview.Scene.CAPE, List.of(
                new Row.Group(t("group.privacy"), false, List.of(
                toggle("privacy.online",
                        () -> cfg().consent() == CollectorConfig.Consent.AGREED,
                        WynnChaYuan::setConsent))))));
        return tabs;
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
