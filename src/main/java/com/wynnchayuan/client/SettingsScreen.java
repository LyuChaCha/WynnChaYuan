package com.wynnchayuan.client;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.CaptureStore;
import com.wynnchayuan.capture.CorpusExport;
import com.wynnchayuan.client.ui.Icons;
import com.wynnchayuan.client.ui.Preview;
import com.wynnchayuan.client.ui.Row;
import com.wynnchayuan.client.ui.SettingsView;
import com.wynnchayuan.client.ui.Ui;
import com.wynnchayuan.render.PanelShot;
import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.RemoteSync;
import com.wynnchayuan.translate.TranslationUpdate;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class SettingsScreen extends Screen {

    private static int lastTab = 0;

    private final Screen parent;
    private SettingsView view;

    private String status = "";
    private int statusColour = Ui.TEXT_3;
    private long statusAt = 0;
    private static final long STATUS_MS = 4000;
    private boolean fetching = false;
    private boolean checkingVersion;

    private String pendingLanguage;
    private String pendingFallback;
    private String languageBusy;
    private String fallbackBusy;

    public SettingsScreen() {
        this(null);
    }

    public SettingsScreen(Screen parent) {
        super(Component.literal("WynnChaYuan"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (view == null) {
            view = new SettingsView(buildTabs(), new Host(), lastTab);
        }
    }

    private static CollectorConfig cfg() {
        return WynnChaYuan.config();
    }

    private void say(String text, int colour) {
        status = text;
        statusColour = colour;
        statusAt = System.currentTimeMillis();
    }

    private static Supplier<String> t(String key) {
        return () -> T.s(key);
    }

    private static Row toggle(String key, boolean byDefault,
                              java.util.function.BooleanSupplier get, Runnable flip) {
        return Row.toggle(key, t(key), t(key + ".hint")).on(get).flip(flip)
                .reset(() -> get.getAsBoolean() == byDefault, () -> {
                    if (get.getAsBoolean() != byDefault) {
                        flip.run();
                    }
                });
    }

    private static <E extends Enum<E>> void cycleTo(Supplier<E> get, Runnable next, E want) {
        for (int i = 0; i < want.getDeclaringClass().getEnumConstants().length
                && get.get() != want; i++) {
            next.run();
        }
    }

    private List<Row.Tab> buildTabs() {
        List<Row.Tab> tabs = new ArrayList<>();
        tabs.add(new Row.Tab("items", t("tab.items"), t("tab.items.about"), Icons.Icon.BOW,
                Preview.Scene.TOOLTIP, List.of(
                        new Row.Group(t("group.items"), false, items()),
                        new Row.Group(t("group.mods"), false, mods()))));
        tabs.add(new Row.Tab("panel", t("tab.panel"), t("tab.panel.about"), Icons.Icon.SCROLL,
                Preview.Scene.PANEL, List.of(
                        new Row.Group(t("group.place"), false, place()),
                        new Row.Group(t("group.look"), false, look()))));
        tabs.add(new Row.Tab("dialogue", t("tab.dialogue"), t("tab.dialogue.about"),
                Icons.Icon.BUBBLE, Preview.Scene.DIALOGUE,
                List.of(new Row.Group(t("group.dialogue"), false, dialogue()))));
        tabs.add(new Row.Tab("world", t("tab.world"), t("tab.world.about"), Icons.Icon.COMPASS,
                Preview.Scene.WORLD, List.of(new Row.Group(t("group.world"), false, world()))));
        tabs.add(new Row.Tab("data", t("tab.data"), t("tab.data.about"), Icons.Icon.BOOK,
                Preview.Scene.DATA, List.of(
                        new Row.Group(t("group.lang"), false, languages()),
                        new Row.Group(t("group.tools"), true, tools()))));
        tabs.add(new Row.Tab("badge", t("tab.badge"), t("tab.badge.about"), Icons.Icon.LEAF,
                Preview.Scene.CAPE, List.of(
                        new Row.Group(t("group.badge"), false, badge()),
                        new Row.Group(t("group.cape"), false, fun()),
                        new Row.Group(t("group.privacy"), false, privacy()),
                        new Row.Group(t("group.perf"), false, List.of(
                                toggle("perf.highlight", true, cfg()::wynntilsFps,
                                       cfg()::toggleWynntilsFps))))));
        return tabs;
    }

    private List<Row> mods() {
        List<Row> rows = new ArrayList<>();
        rows.add(toggle("world.wynntils", true, cfg()::wynntilsUi, cfg()::toggleWynntilsUi));
        if (com.wynnchayuan.translate.WmsBridge.installed()) {
            rows.add(toggle("items.wms", true, cfg()::wynnMarketSearch,
                    cfg()::toggleWynnMarketSearch));
        }
        if (com.wynnchayuan.translate.WynnventoryBridge.installed()) {
            rows.add(toggle("items.wynnventory", true, cfg()::wynnventory, cfg()::toggleWynnventory));
        }
        if (com.wynnchayuan.render.WynnModTooltip.installed()) {
            rows.add(toggle("items.wynnmod", true, cfg()::wynnmodTooltip,
                    cfg()::toggleWynnmodTooltip));
        }
        return rows;
    }

    private static final String CONTRIBUTE_URL =
            "https://github.com/LyuChaCha/WynnChaYuan/blob/main/CONTRIBUTING.md";

    private static final String SPONSOR_URL =
            "https://github.com/LyuChaCha/WynnChaYuan#%E8%B4%8A%E5%8A%A9";

    private List<Row> fun() {
        List<Row> rows = new ArrayList<>();
        rows.add(toggle("fun.capes", true, cfg()::capes, cfg()::toggleCapes));
        String self = com.wynnchayuan.render.Badges.selfName();
        java.util.UUID me = this.minecraft == null ? null : this.minecraft.getUser().getProfileId();
        if (!cfg().ownCape() || com.wynnchayuan.cape.Capes.hasCape(self, me)) {
            rows.add(toggle("fun.owncape", true, cfg()::ownCape, cfg()::toggleOwnCape));
        } else {
            rows.add(Row.status("fun.nocape", t("fun.nocape"), t("fun.nocape.hint"))
                    .label(t("fun.nocape.label")));
        }
        if (!com.wynnchayuan.NetworkGate.registry()) {
            rows.add(Row.status("fun.offline", t("fun.offline"), t("fun.offline.hint"))
                    .label(t("fun.offline.label")));
            rows.add(Row.action("fun.gallery", t("fun.gallery"), t("fun.gallery.hint"))
                    .label(t("fun.gallery.button"))
                    .run(() -> ConfirmLinkScreen.confirmLinkNow(this,
                            com.wynnchayuan.Presence.GALLERY_URL)));
            return rows;
        }
        rows.add(Row.action("fun.upload", t("fun.upload"), t("fun.upload.hint"))
                .label(t("fun.upload.button"))
                .run(() -> com.wynnchayuan.Presence.site("/account", true,
                        url -> net.minecraft.client.Minecraft.getInstance().execute(
                                () -> ConfirmLinkScreen.confirmLinkNow(this, url)))));
        rows.add(Row.action("fun.remove", t("fun.remove"), t("fun.remove.hint"))
                .label(t("fun.remove.button"))
                .run(() -> com.wynnchayuan.Presence.remove(this::removed)));
        rows.add(Row.action("fun.gallery", t("fun.gallery"), t("fun.gallery.hint"))
                .label(t("fun.gallery.button"))
                .run(() -> com.wynnchayuan.Presence.gallery(true,
                        url -> net.minecraft.client.Minecraft.getInstance().execute(
                                () -> ConfirmLinkScreen.confirmLinkNow(this, url)))));
        return rows;
    }

    private static final long ARM_MS = 6000;

    private long armedAt;
    private boolean deleting;

    private boolean armed() {
        return System.currentTimeMillis() - armedAt < ARM_MS;
    }

    private void rebuild() {
        float at = view.scrollPosition();
        lastTab = view.tab();
        view = new SettingsView(buildTabs(), new Host(), lastTab);
        view.restoreScroll(at);
    }

    private void requestDelete() {
        deleting = true;
        WynnChaYuan.setDataOptOut(true, this::deleted);
        say(T.s("privacy.deleting"), Ui.TEXT_3);
        rebuild();
    }

    private List<Row> privacy() {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.toggle("privacy.online", t("privacy.online"), t("privacy.online.hint"))
                .on(() -> cfg().consent() == CollectorConfig.Consent.AGREED)
                .flip(() -> {
                    boolean agree = cfg().consent() != CollectorConfig.Consent.AGREED;
                    WynnChaYuan.setConsent(agree);
                    say(T.s(agree ? "privacy.online.on" : "privacy.online.off"), Ui.GREEN);
                    rebuild();
                }));
        rows.add(Row.action("privacy.policy", t("privacy.policy"), t("privacy.policy.hint"))
                .label(t("fun.gallery.button"))
                .run(() -> ConfirmLinkScreen.confirmLinkNow(this,
                        com.wynnchayuan.Presence.PRIVACY_URL)));
        if (!cfg().dataOptOut()) {
            rows.add(Row.action("privacy.delete", t("privacy.delete"), t("privacy.delete.hint"))
                    .label(() -> T.s(armed() ? "privacy.delete.confirm" : "privacy.delete.button"))
                    .danger(this::armed)
                    .run(() -> {
                        if (armed()) {
                            armedAt = 0;
                            requestDelete();
                        } else {
                            armedAt = System.currentTimeMillis();
                            say(T.s("privacy.delete.armed"), Ui.RED);
                        }
                    }));
            return rows;
        }
        rows.add(Row.status("privacy.delete", t("privacy.delete"), t("privacy.delete.hint"))
                .label(() -> T.s(deleting ? "privacy.delete.state.working"
                        : cfg().dataDeletePending() ? "privacy.delete.state.retry"
                        : "privacy.delete.state.done"))
                .labelColour(() -> deleting ? Ui.TEXT_3
                        : cfg().dataDeletePending() ? Ui.AMBER : Ui.GREEN)
                .run(() -> {
                    if (!deleting && cfg().dataDeletePending()) {
                        requestDelete();
                    }
                }));
        rows.add(Row.action("privacy.resume", t("privacy.resume"), t("privacy.resume.hint"))
                .label(t("privacy.resume.button"))
                .enabled(() -> !deleting)
                .run(() -> {
                    WynnChaYuan.setDataOptOut(false, null);
                    say(T.s("privacy.resumed"), Ui.GREEN);
                    rebuild();
                }));
        return rows;
    }

    private void deleted(com.wynnchayuan.Presence.DeleteResult result) {
        deleting = false;
        if (result == com.wynnchayuan.Presence.DeleteResult.DONE) {
            say(T.s("privacy.deleted"), Ui.GREEN);
        } else {
            say(T.s("privacy.delete.later"), Ui.AMBER);
        }
    }

    private void removed(com.wynnchayuan.Presence.Result result) {
        net.minecraft.client.Minecraft.getInstance().execute(() -> {
            if (result == com.wynnchayuan.Presence.Result.DONE) {
                say(T.s("fun.remove.done"), Ui.GREEN);
            } else {
                say(T.s("fun.upload." + result.name().toLowerCase(java.util.Locale.ROOT)), Ui.RED);
            }
        });
    }

    private List<Row> badge() {
        List<Row> rows = new ArrayList<>();
        String self = com.wynnchayuan.render.Badges.selfName();
        List<com.wynnchayuan.render.Badges.Role> mine =
                com.wynnchayuan.render.Badges.registered(self);
        if (mine.isEmpty()) {
            rows.add(Row.status("badge.none", t("badge.none"), t("badge.none.hint"))
                    .label(t("badge.none.label"))
                    .labelColour(() -> com.wynnchayuan.render.Colors.opaque(
                            com.wynnchayuan.render.Badges.INSTALLED)));
            rows.add(Row.action("badge.contribute", t("badge.contribute"), t("badge.contribute.hint"))
                    .label(t("badge.contribute.button"))
                    .run(() -> ConfirmLinkScreen.confirmLinkNow(this, CONTRIBUTE_URL)));
            rows.add(Row.action("badge.sponsor", t("badge.sponsor"), t("badge.sponsor.hint"))
                    .label(t("badge.sponsor.button"))
                    .run(() -> ConfirmLinkScreen.confirmLinkNow(this, SPONSOR_URL)));
            return rows;
        }
        for (com.wynnchayuan.render.Badges.Role role : mine) {
            if (mine.size() > 1) {
                java.util.function.BooleanSupplier shown =
                        () -> cfg().badgeRoleShown(role.id(), !role.hidden());
                rows.add(Row.toggle("badge.role." + role.id(),
                                () -> T.s("badge.role", role.name()), t("badge.role.hint"))
                        .on(shown)
                        .flip(() -> {
                            if (!shown.getAsBoolean() || badgesShown(mine) > 1) {
                                cfg().setBadgeRole(role.id(), !shown.getAsBoolean());
                            }
                        })
                        .reset(() -> shown.getAsBoolean() == !role.hidden(),
                                () -> cfg().setBadgeRole(role.id(), !role.hidden())));
            }
            String registered = String.format("#%06X", role.color() & 0xFFFFFF);
            rows.add(Row.colour("badge.colour." + role.id(),
                            () -> T.s("badge.colour", role.name()), t("badge.colour.hint"))
                    .hex(() -> {
                        String own = cfg().badgeColour(role.id());
                        return own == null ? registered : own;
                    })
                    .setHex(hex -> cfg().setBadgeColourLive(role.id(), hex))
                    .commit(cfg()::saveIfDirty)
                    .reset(() -> cfg().badgeColour(role.id()) == null,
                            () -> cfg().clearBadgeColour(role.id())));
            if (com.wynnchayuan.render.Badges.rainbowAllowed(role)) {
                java.util.function.BooleanSupplier rainbow =
                        () -> cfg().badgeRainbow(role.rainbow());
                rows.add(Row.toggle("badge.rainbow", t("badge.rainbow"), t("badge.rainbow.hint"))
                        .on(rainbow)
                        .flip(() -> cfg().setBadgeRainbow(!rainbow.getAsBoolean()))
                        .reset(() -> rainbow.getAsBoolean() == role.rainbow(),
                                () -> cfg().setBadgeRainbow(role.rainbow())));
            }
        }
        return rows;
    }

    private int badgesShown(List<com.wynnchayuan.render.Badges.Role> mine) {
        int n = 0;
        for (com.wynnchayuan.render.Badges.Role role : mine) {
            if (cfg().badgeRoleShown(role.id(), !role.hidden())) {
                n++;
            }
        }
        return n;
    }

    private List<Row> items() {
        List<Row> rows = new ArrayList<>();
        rows.add(toggle("translations.enabled", true, cfg()::translationsEnabled, () -> {
            cfg().toggleTranslations();
            WynnChaYuan.reloadTranslations();
            say(T.s(cfg().translationsEnabled()
                    ? "translations.enabled.on" : "translations.enabled.off"), Ui.GREEN);
        }));
        rows.add(Row.segment("items.tooltip", t("items.tooltip"), t("items.tooltip.hint"))
                .option(T.s("mode.panel"), Row.Tone.ACCENT)
                .option(T.s("mode.replace"), Row.Tone.REPLACE)
                .option(T.s("mode.off"), Row.Tone.OFF)
                .selected(() -> cfg().tooltipMode().ordinal())
                .pick(i -> cycleTo(cfg()::tooltipMode, cfg()::cycleTooltipMode,
                        CollectorConfig.TooltipMode.values()[i]))
                .reset(() -> cfg().tooltipMode() == CollectorConfig.TooltipMode.PANEL,
                        () -> cycleTo(cfg()::tooltipMode, cfg()::cycleTooltipMode,
                                CollectorConfig.TooltipMode.PANEL)));
        Runnable nextName = () ->
                WynnChaYuan.translations().setNameMode(cfg().cycleItemNames(1));
        rows.add(Row.segment("items.names", t("items.names"), t("items.names.hint"))
                .option(T.s("mode.on"), Row.Tone.ACCENT)
                .option(T.s("items.names.both"), Row.Tone.BOTH)
                .option(T.s("mode.off"), Row.Tone.OFF)
                .selected(() -> cfg().itemNames().ordinal())
                .pick(i -> cycleTo(cfg()::itemNames, nextName,
                        CollectorConfig.ItemNames.values()[i]))
                .reset(() -> cfg().itemNames() == CollectorConfig.ItemNames.OFF,
                        () -> cycleTo(cfg()::itemNames, nextName, CollectorConfig.ItemNames.OFF)));
        rows.add(toggle("items.abilitynames", false, cfg()::abilityNamesOriginal, () ->
                WynnChaYuan.translations().setAbilityNamesWithOriginal(
                        cfg().toggleAbilityNamesOriginal())));
        rows.add(toggle("items.shiftpeek", true, cfg()::shiftPeekNames,
                cfg()::toggleShiftPeekNames));
        Runnable nextPage = () ->
                WynnChaYuan.translations().setPageNameMode(cfg().cyclePageNames(1));
        rows.add(Row.segment("items.pages", t("items.pages"), t("items.pages.hint"))
                .option(T.s("mode.on"), Row.Tone.ACCENT)
                .option(T.s("items.names.both"), Row.Tone.BOTH)
                .option(T.s("items.pages.off"), Row.Tone.OFF)
                .selected(() -> cfg().pageNames().ordinal())
                .pick(i -> cycleTo(cfg()::pageNames, nextPage,
                        CollectorConfig.ItemNames.values()[i]))
                .reset(() -> cfg().pageNames() == CollectorConfig.ItemNames.ON,
                        () -> cycleTo(cfg()::pageNames, nextPage, CollectorConfig.ItemNames.ON)));
        rows.add(toggle("items.market", true, cfg()::marketSearch, cfg()::toggleMarketSearch));
        rows.add(Row.segment("items.shot", t("items.shot"), () -> {
                    String clash = PanelShot.conflict();
                    if (clash != null) {
                        return T.s("items.shot.clash", clash);
                    }
                    return PanelShot.hasKey() ? T.s("items.shot.bound", PanelShot.keyName())
                                              : T.s("items.shot.unbound");
                })
                .option(T.s("mode.hotkey"), Row.Tone.ACCENT)
                .option(T.s("mode.off"), Row.Tone.OFF)
                .selected(() -> cfg().shotMode().ordinal())
                .pick(i -> cycleTo(cfg()::shotMode, cfg()::cycleShotMode,
                        CollectorConfig.ShotMode.values()[i]))
                .extra(t("button.keybinds"), () -> this.minecraft.setScreen(
                        new net.minecraft.client.gui.screens.options.controls.KeyBindsScreen(
                                this, this.minecraft.options)))
                .reset(() -> cfg().shotMode() == CollectorConfig.ShotMode.KEY,
                        () -> cycleTo(cfg()::shotMode, cfg()::cycleShotMode,
                                CollectorConfig.ShotMode.KEY)));
        return rows;
    }

    private List<Row> place() {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.segment("panel.anchor", t("panel.anchor"), t("panel.anchor.hint"))
                .option(T.s("mode.follow"), Row.Tone.PLAIN)
                .option(T.s("mode.pinned"), Row.Tone.PLAIN)
                .selected(() -> cfg().panelAnchor().ordinal())
                .pick(i -> cycleTo(cfg()::panelAnchor, cfg()::togglePanelAnchor,
                        CollectorConfig.PanelAnchor.values()[i]))
                .reset(() -> cfg().panelAnchor() == CollectorConfig.PanelAnchor.FOLLOW,
                        () -> cycleTo(cfg()::panelAnchor, cfg()::togglePanelAnchor,
                                CollectorConfig.PanelAnchor.FOLLOW)));
        rows.add(Row.action("panel.place", t("panel.place"), t("panel.place.hint"))
                .label(t("button.adjust"))
                .run(() -> this.minecraft.setScreen(new PositionScreen(this))));
        rows.add(Row.segment("panel.side", t("panel.side"), t("panel.side.hint"))
                .option(T.s("mode.auto"), Row.Tone.PLAIN)
                .option(T.s("mode.right"), Row.Tone.PLAIN)
                .option(T.s("mode.left"), Row.Tone.PLAIN)
                .selected(() -> cfg().panelSide().ordinal())
                .pick(i -> cycleTo(cfg()::panelSide, cfg()::cyclePanelSide,
                        CollectorConfig.PanelSide.values()[i]))
                .reset(() -> cfg().panelSide() == CollectorConfig.PanelSide.AUTO,
                        () -> cycleTo(cfg()::panelSide, cfg()::cyclePanelSide,
                                CollectorConfig.PanelSide.AUTO)));
        rows.add(Row.slider("panel.gap", t("panel.gap"), t("panel.gap.hint"))
                .range(com.wynnchayuan.PanelGap.MIN, com.wynnchayuan.PanelGap.MAX)
                .value(() -> cfg().panelGap())
                .slide(v -> cfg().setPanelGapLive(v))
                .format(v -> v + " px")
                .commit(cfg()::saveIfDirty)
                .reset(() -> cfg().panelGap() == CollectorConfig.DEFAULT_PANEL_GAP, () -> {
                    cfg().setPanelGapLive(CollectorConfig.DEFAULT_PANEL_GAP);
                    cfg().saveIfDirty();
                }));
        return rows;
    }

    private static final String DEFAULT_COLOUR = "#6FA8D8";

    private List<Row> look() {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.colour("panel.theme", t("panel.theme"), t("panel.theme.hint"))
                .hex(() -> cfg().themeColor())
                .setHex(hex -> cfg().setThemeColorLive(hex))
                .commit(cfg()::saveIfDirty)
                .reset(() -> DEFAULT_COLOUR.equalsIgnoreCase(cfg().themeColor()),
                        () -> cfg().setThemeColor(DEFAULT_COLOUR)));
        rows.add(Row.colour("panel.colour", t("panel.colour"), t("panel.colour.hint"))
                .hex(() -> cfg().accentColor())
                .setHex(hex -> cfg().setAccentColorLive(hex))
                .commit(cfg()::saveIfDirty)
                .reset(() -> DEFAULT_COLOUR.equalsIgnoreCase(cfg().accentColor()),
                        () -> cfg().setAccentColor(DEFAULT_COLOUR)));
        return rows;
    }

    private Row dialogueMode(String key, Supplier<CollectorConfig.DialogueMode> get,
                             Runnable next) {
        return Row.segment(key, t(key), t(key + ".hint"))
                .option(T.s("mode.box"), Row.Tone.ACCENT)
                .option(T.s("mode.replace"), Row.Tone.REPLACE)
                .option(T.s("mode.off"), Row.Tone.OFF)
                .selected(() -> get.get().ordinal())
                .pick(i -> {
                    cycleTo(get, next, CollectorConfig.DialogueMode.values()[i]);
                    needBoxes("dialogue.mode".equals(key)
                            && get.get() == CollectorConfig.DialogueMode.PANEL);
                })
                .reset(() -> get.get() == CollectorConfig.DialogueMode.REPLACE,
                        () -> cycleTo(get, next, CollectorConfig.DialogueMode.REPLACE));
    }

    private static void needBoxes(boolean box) {
        if (box && !cfg().showOverlays()) {
            cfg().toggleOverlays();
        }
    }

    private static int holdSeconds() {
        int ms = cfg().dialogueHoldMs();
        return ms == Integer.MAX_VALUE ? 0 : ms / 1000;
    }

    private List<Row> dialogue() {
        List<Row> rows = new ArrayList<>();
        rows.add(dialogueMode("dialogue.mode", cfg()::dialogueMode, cfg()::cycleDialogueMode));
        rows.add(dialogueMode("dialogue.choices", cfg()::choiceMode, cfg()::cycleChoiceMode));
        rows.add(toggle("dialogue.cover", true, cfg()::dialogueCover, cfg()::toggleDialogueCover));
        rows.add(Row.slider("dialogue.hold", t("dialogue.hold"), t("dialogue.hold.hint"))
                .range(0, 30)
                .value(() -> Math.min(30, holdSeconds()))
                .slide(v -> cfg().setDialogueHoldSecondsLive(v))
                .format(v -> v == 0 ? T.s("unit.forever") : T.s("unit.seconds", v))
                .commit(cfg()::saveIfDirty)
                .reset(() -> holdSeconds() == 6, () -> {
                    cfg().setDialogueHoldSecondsLive(6);
                    cfg().saveIfDirty();
                }));
        rows.add(toggle("dialogue.overlays", true, cfg()::showOverlays, cfg()::toggleOverlays));
        return rows;
    }

    private List<Row> world() {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.segment("world.nametag", t("world.nametag"), t("world.nametag.hint"))
                .option(T.s("mode.lookat"), Row.Tone.ACCENT)
                .option(T.s("mode.replace"), Row.Tone.REPLACE)
                .option(T.s("mode.off"), Row.Tone.OFF)
                .selected(() -> cfg().nametagMode().ordinal())
                .pick(i -> {
                    cycleTo(cfg()::nametagMode, cfg()::cycleNametagMode,
                            CollectorConfig.NametagMode.values()[i]);
                    needBoxes(cfg().nametagMode() == CollectorConfig.NametagMode.LOOK_AT);
                })
                .extra(t("button.advanced"), () -> this.minecraft.setScreen(new NametagScreen(this)))
                .reset(() -> cfg().nametagMode() == CollectorConfig.NametagMode.REPLACE,
                        () -> cycleTo(cfg()::nametagMode, cfg()::cycleNametagMode,
                                CollectorConfig.NametagMode.REPLACE)));
        rows.add(Row.segment("world.chat", t("world.chat"), t("world.chat.hint"))
                .option(T.s("mode.replace"), Row.Tone.REPLACE)
                .option(T.s("mode.both"), Row.Tone.BOTH)
                .option(T.s("mode.off"), Row.Tone.OFF)
                .selected(() -> cfg().chatMode().ordinal())
                .pick(i -> cycleTo(cfg()::chatMode, cfg()::cycleChatMode,
                        CollectorConfig.ChatMode.values()[i]))
                .reset(() -> cfg().chatMode() == CollectorConfig.ChatMode.BOTH,
                        () -> cycleTo(cfg()::chatMode, cfg()::cycleChatMode,
                                CollectorConfig.ChatMode.BOTH)));
        rows.add(toggle("world.titles", true, cfg()::translateTitles, cfg()::toggleTitles)
                .onText(t("mode.replace")));
        rows.add(toggle("world.bossbar", true, cfg()::translateBossBar, cfg()::toggleBossBar)
                .onText(t("mode.replace")));
        CollectorConfig.DialogueMode[] order = {
            CollectorConfig.DialogueMode.REPLACE, CollectorConfig.DialogueMode.PANEL,
            CollectorConfig.DialogueMode.OFF};
        rows.add(Row.segment("world.tracker", t("world.tracker"), t("world.tracker.hint"))
                .option(T.s("mode.replace"), Row.Tone.REPLACE)
                .option(T.s("mode.panel"), Row.Tone.ACCENT)
                .option(T.s("mode.off"), Row.Tone.OFF)
                .selected(() -> java.util.Arrays.asList(order).indexOf(cfg().trackerMode()))
                .pick(i -> {
                    cycleTo(cfg()::trackerMode, cfg()::cycleTrackerMode, order[i]);
                    needBoxes(cfg().trackerMode() == CollectorConfig.DialogueMode.PANEL);
                })
                .reset(() -> cfg().trackerMode() == CollectorConfig.DialogueMode.REPLACE,
                        () -> cycleTo(cfg()::trackerMode, cfg()::cycleTrackerMode,
                                CollectorConfig.DialogueMode.REPLACE)));
        rows.add(toggle("world.objectives", true, cfg()::translateObjectives,
                cfg()::toggleObjectives).onText(t("mode.replace")));
        rows.add(toggle("world.scoreboard", true, cfg()::translateScoreboard,
                cfg()::toggleScoreboard));
        rows.add(toggle("world.helditem", true, cfg()::translateHeldItem, cfg()::toggleHeldItem));
        rows.add(toggle("world.chatcopy", true, cfg()::chatCopy, cfg()::toggleChatCopy));
        return rows;
    }

    private static String badge(String lang) {
        return switch (lang) {
            case "zh_tw" -> "繁";
            case "zh_cn" -> "简";
            case "ja_jp" -> "日";
            case "ko_kr" -> "한";
            default -> lang.length() >= 2 ? lang.substring(0, 2).toUpperCase(java.util.Locale.ROOT) : lang;
        };
    }

    private static Row.Choice language(String lang) {
        return new Row.Choice(lang, Languages.nativeName(lang), badge(lang), false);
    }

    private List<Row> languages() {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.select("data.language", t("data.language"), t("data.language.hint"))
                .choices(() -> {
                    List<Row.Choice> out = new ArrayList<>();
                    out.add(new Row.Choice("", T.s("data.language.auto",
                            Languages.nativeName(WynnChaYuan.autoLanguage())), "A", true));
                    for (String lang : Languages.bundled()) {
                        out.add(language(lang));
                    }
                    return out;
                })
                .current(() -> pendingLanguage != null ? pendingLanguage : cfg().language())
                .choose(id -> pendingLanguage = id.equals(cfg().language()) ? null : id)
                .apply(() -> pendingLanguage != null, this::applyLanguage)
                .locked(() -> fetching)
                .busy(() -> languageBusy));
        rows.add(Row.select("data.fallback", t("data.fallback"), t("data.fallback.hint"))
                .choices(() -> {
                    List<Row.Choice> out = new ArrayList<>();
                    String under = WynnChaYuan.fallbackLanguage();
                    out.add(new Row.Choice("", under == null
                            ? T.s("data.fallback.auto.none")
                            : T.s("data.fallback.auto", Languages.nativeName(under)), "A", true));
                    out.add(new Row.Choice(WynnChaYuan.OFF, T.s("data.fallback.off"), "EN", false));
                    for (String lang : Languages.bundled()) {
                        if (!lang.equals(WynnChaYuan.language())) {
                            out.add(language(lang));
                        }
                    }
                    return out;
                })
                .current(() -> pendingFallback != null ? pendingFallback : cfg().fallbackLanguage())
                .choose(id -> pendingFallback = id.equals(cfg().fallbackLanguage()) ? null : id)
                .apply(() -> pendingFallback != null, this::applyFallback)
                .locked(() -> fetching)
                .busy(() -> fallbackBusy));
        rows.add(Row.select("data.ui", t("data.ui"), t("data.ui.hint"))
                .choices(() -> {
                    List<Row.Choice> out = new ArrayList<>();
                    String following = cfg().language();
                    out.add(new Row.Choice("", T.s("data.ui.auto", Languages.nativeName(
                            following.isEmpty() ? WynnChaYuan.autoLanguage() : following)),
                            "A", true));
                    for (String lang : T.available()) {
                        out.add(language(lang));
                    }
                    return out;
                })
                .current(() -> cfg().uiLanguage())
                .choose(id -> {
                    cfg().setUiLanguage(id);
                    lastTab = view.tab();
                    view = new SettingsView(buildTabs(), new Host(), lastTab);
                }));
        rows.add(toggle("data.collect", true, cfg()::collect, cfg()::toggleCollect));
        rows.add(toggle("data.collectgui", false, cfg()::collectGuiText,
                cfg()::toggleCollectGuiText));
        rows.add(toggle("data.debug", false, cfg()::debugDumps, () -> {
            cfg().toggleDebugDumps();
            say(T.s(cfg().debugDumps() ? "data.debug.on" : "data.debug.off"), Ui.GREEN);
        }));
        return rows;
    }

    private void applyLanguage() {
        String next = pendingLanguage;
        if (next == null) {
            return;
        }
        fetching = true;
        say(T.s("data.language.switching"), Ui.TEXT_3);
        WynnChaYuan.switchLanguage(next, result -> {
            pendingLanguage = null;
            if (next.equals(pendingFallback)) {
                pendingFallback = null;
            }
            fetching = false;
            languageBusy = null;
            say("✔ " + result, Ui.GREEN);
        }, (done, total) -> Minecraft.getInstance().execute(() -> {
            languageBusy = T.s("data.language.progress", done, total);
            say(languageBusy, Ui.TEXT_3);
        }));
    }

    private void applyFallback() {
        String next = pendingFallback;
        if (next == null) {
            return;
        }
        fetching = true;
        say(T.s("data.language.switching"), Ui.TEXT_3);
        WynnChaYuan.switchFallback(next, result -> {
            pendingFallback = null;
            fetching = false;
            fallbackBusy = null;
            say("✔ " + result, Ui.GREEN);
        }, (done, total) -> Minecraft.getInstance().execute(() -> {
            fallbackBusy = T.s("data.language.progress", done, total);
            say(fallbackBusy, Ui.TEXT_3);
        }));
    }

    private static boolean github() {
        return cfg().source() == CollectorConfig.Source.GITHUB;
    }

    private List<Row> tools() {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.segment("data.source", t("data.source"), t("data.source.hint"))
                .option(T.s("data.source.github"), Row.Tone.PLAIN)
                .option(T.s("data.source.local"), Row.Tone.PLAIN)
                .selected(() -> cfg().source().ordinal())
                .pick(i -> cycleTo(cfg()::source, cfg()::toggleSource,
                        CollectorConfig.Source.values()[i]))
                .reset(SettingsScreen::github, () -> cycleTo(cfg()::source, cfg()::toggleSource,
                        CollectorConfig.Source.GITHUB)));
        rows.add(toggle("data.autoupdate", false, cfg()::autoUpdateTranslations,
                cfg()::toggleAutoUpdateTranslations));
        rows.add(Row.action("data.reload", t("data.reload"),
                        () -> T.s(github() ? "data.reload.github" : "data.reload.local"))
                .label(() -> T.s(github() ? "data.reload.fetch" : "data.reload.reread"))
                .enabled(() -> !fetching)
                .run(this::reload));
        rows.add(Row.status("data.version", t("data.version"), this::versionHint)
                .label(() -> T.s(VERSION_KEY.get(versionState())))
                .labelColour(() -> switch (versionState()) {
                    case LATEST -> Ui.GREEN;
                    case BEHIND -> Ui.AMBER;
                    case UNKNOWN -> Ui.RED;
                    default -> Ui.TEXT_2;
                })
                .run(this::recheckVersion));
        rows.add(Row.action("data.export", t("data.export"), t("data.export.hint"))
                .label(t("data.export.button"))
                .run(this::exportCorpus));
        rows.add(Row.action("data.submit", t("data.submit"), t("data.submit.hint"))
                .label(t("data.submit.button"))
                .run(() -> ConfirmLinkScreen.confirmLinkNow(this, CorpusExport.ISSUE_URL)));
        return rows;
    }

    private TranslationUpdate.State versionState() {
        return TranslationUpdate.verdict(github(), checkingVersion, TranslationUpdate.asked(),
                cfg().syncedTranslations(), TranslationUpdate.seen());
    }

    private static final java.util.Map<TranslationUpdate.State, String> VERSION_KEY =
            java.util.Map.of(
                    TranslationUpdate.State.LOCAL, "data.version.local",
                    TranslationUpdate.State.CHECKING, "data.version.checking",
                    TranslationUpdate.State.LATEST, "data.version.latest",
                    TranslationUpdate.State.BEHIND, "data.version.behind",
                    TranslationUpdate.State.UNKNOWN, "data.version.unknown");

    private static String shortVersion(String sha, String date) {
        return TranslationUpdate.label(sha, date, T.s("data.version.bundled"));
    }

    private String versionHint() {
        String local = shortVersion(cfg().syncedTranslations(), cfg().syncedTranslationsDate());
        return switch (versionState()) {
            case LOCAL -> T.s("data.version.local.hint");
            case CHECKING -> T.s("data.version.checking");
            case LATEST -> T.s("data.version.latest.hint", local);
            case BEHIND -> T.s("data.version.behind.hint", local,
                    shortVersion(TranslationUpdate.seen(), RemoteSync.lastRemoteDate()));
            case UNKNOWN -> T.s("data.version.unknown.hint", local);
        };
    }

    private void recheckVersion() {
        if (checkingVersion) {
            return;
        }
        if (!github()) {
            say(T.s("data.version.local.hint"), Ui.TEXT_3);
            return;
        }
        checkingVersion = true;
        WynnChaYuan.recheckTranslationVersion(() -> {
            checkingVersion = false;
            say(versionHint(),
                    versionState() == TranslationUpdate.State.LATEST ? Ui.GREEN : Ui.AMBER);
        });
    }

    private void reload() {
        if (github()) {
            say(T.s("status.fetching"), Ui.TEXT_3);
            fetching = true;
            WynnChaYuan.resyncTranslations(result -> {
                fetching = false;
                report(result, WynnChaYuan.translations().size() > 0);
            });
            return;
        }
        WynnChaYuan.reloadTranslations();
        report(WynnChaYuan.translations().lastResult(), WynnChaYuan.translations().size() > 0);
    }

    private void report(String result, boolean ok) {
        say((ok ? "✔ " : "✘ ") + result, ok ? Ui.GREEN : Ui.RED);
        if (this.minecraft != null && this.minecraft.player != null) {
            Component line = Component.literal("[WynnChaYuan] " + result)
                    .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED);
            com.wynnchayuan.capture.OwnOutputs.note(line);
            this.minecraft.player.displayClientMessage(line, false);
        }
    }

    private void exportCorpus() {
        CaptureStore store = WynnChaYuan.store();
        java.nio.file.Path dir = WynnChaYuan.configDir();
        if (store == null || dir == null) {
            return;
        }
        say(T.s("data.export.working"), Ui.TEXT_3);
        Minecraft client = Minecraft.getInstance();
        Thread worker = new Thread(() -> {
            try {
                CorpusExport.Result result = CorpusExport.write(
                        dir, store, WynnChaYuan.version(), WynnChaYuan.language());
                client.execute(() -> {
                    if (result.count() == 0) {
                        say(T.s("data.export.empty"), Ui.TEXT_3);
                    } else {
                        say(T.s("data.export.done", result.count()), Ui.GREEN);
                    }
                    Util.getPlatform().openPath(result.file().getParent());
                });
            } catch (Exception e) {
                client.execute(() -> say(T.s("data.export.failed", String.valueOf(e.getMessage())),
                        Ui.RED));
            }
        }, WynnChaYuan.MOD_ID + "-export");
        worker.setDaemon(true);
        worker.start();
    }

    private final class Host implements SettingsView.Host {

        @Override
        public int accent() {
            return cfg().themeARGB();
        }

        @Override
        public int frame() {
            return cfg().accentARGB();
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
            return "WynnChaYuan";
        }

        @Override
        public String tagline() {
            return T.s("header.tagline");
        }

        @Override
        public String version() {
            return "v" + WynnChaYuan.version();
        }

        @Override
        public SettingsView.Status status() {
            if (!status.isEmpty()
                    && (fetching || System.currentTimeMillis() - statusAt < STATUS_MS)) {
                return new SettingsView.Status(status, statusColour);
            }
            int size = WynnChaYuan.translations().size();
            return new SettingsView.Status(T.s("footer.hint", String.format("%,d", size)),
                    size > 0 ? Ui.TEXT_3 : Ui.RED);
        }

        @Override
        public boolean hasUpdate() {
            return com.wynnchayuan.Releases.newer() != null;
        }

        @Override
        public boolean blurred() {
            Minecraft mc = Minecraft.getInstance();
            return mc.options.getMenuBackgroundBlurriness() > 0;
        }

        @Override
        public void openNotice() {
            SettingsScreen.this.minecraft.setScreen(new NoticeScreen(SettingsScreen.this));
        }

        @Override
        public void openUpdates() {
            SettingsScreen.this.minecraft.setScreen(new ReleaseNotesScreen(SettingsScreen.this));
        }

        @Override
        public void openCredits() {
            SettingsScreen.this.minecraft.setScreen(new CreditsScreen(SettingsScreen.this));
        }

        @Override
        public void done() {
            if (pendingLanguage != null) {
                applyLanguage();
            } else if (pendingFallback != null) {
                applyFallback();
            }
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
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        }

        @Override
        public Preview.State preview() {
            return previewState();
        }

        @Override
        public void resetDone(String what) {
            say(T.s("reset.done", what), cfg().themeARGB());
        }
    }

    private final Preview.State previewState = new Preview.State();

    private Preview.State previewState() {
        Preview.State s = previewState;
        CollectorConfig c = cfg();
        s.tooltipMode = c.tooltipMode().ordinal();
        s.names = c.itemNames().ordinal();
        s.anchorFixed = c.panelAnchor() == CollectorConfig.PanelAnchor.FIXED;
        s.side = c.panelSide().ordinal();
        s.gap = c.panelGap();
        s.dialogueMode = c.dialogueMode().ordinal();
        s.choiceMode = c.choiceMode().ordinal();
        s.holdSeconds = holdSeconds();
        s.overlays = c.showOverlays();
        s.nametag = c.nametagMode().ordinal();
        s.chat = c.chatMode().ordinal();
        s.titles = c.translateTitles();
        s.bossbar = c.translateBossBar();
        s.tracker = switch (c.trackerMode()) {
            case REPLACE -> 0;
            case PANEL -> 1;
            case OFF -> 2;
        };
        s.objectives = c.translateObjectives();
        s.scoreboard = c.translateScoreboard();
        s.heldItem = c.translateHeldItem();
        s.loaded = WynnChaYuan.translations().size();
        s.loadedLabel = T.s("preview.loaded");
        s.panelOffNote = T.s("preview.panel.off");
        String lang = c.language();
        String under = WynnChaYuan.fallbackLanguage();
        s.facts = new String[][] {
            {T.s("data.language"), Languages.nativeName(
                    lang.isEmpty() ? WynnChaYuan.autoLanguage() : lang)},
            {T.s("data.fallback"), under == null ? T.s("data.fallback.off")
                    : Languages.nativeName(under)},
            {T.s("data.source"), T.s(github() ? "data.source.github" : "data.source.local")},
            {T.s("data.version"), T.s(VERSION_KEY.get(versionState())), "version"},
        };
        s.versionColour = switch (versionState()) {
            case LATEST -> Ui.GREEN;
            case BEHIND -> Ui.AMBER;
            case UNKNOWN -> Ui.RED;
            default -> Ui.TEXT_2;
        };
        s.translate = SettingsScreen::previewLookup;
        return s;
    }

    static String previewLookup(String text) {
        var store = WynnChaYuan.translations();
        String hit = store.lookup(text);
        if (hit == null || hit.isBlank()) {
            return text;
        }
        return Preview.bareName(hit, text, store.appendedOriginalAt(hit));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        Minecraft mc = Minecraft.getInstance();
        int guiScale = (int) mc.getWindow().getGuiScale();
        GuiCanvas canvas = new GuiCanvas(g, this.font, guiScale);
        view.beneathOverlays(() -> {
            float[] model = view.spot("preview.model");
            if (model != null && mc.player != null) {
                canvas.outside(() -> {
                    try {
                        drawPlayer(g, mc, model, view.modelYaw(), view.modelPitch());
                    } catch (Throwable t) {
                        com.wynnchayuan.translate.ErrorDebug.note("settings.model", null, t);
                    }
                });
            }
        });
        canvas.begin();
        try {
            double mx = mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth()
                    / Math.max(1, mc.getWindow().getScreenWidth());
            double my = mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight()
                    / Math.max(1, mc.getWindow().getScreenHeight());
            view.render(canvas, this.width, this.height, mx, my, Util.getMillis());
        } finally {
            canvas.end();
        }
    }

    private static void drawPlayer(GuiGraphics g, Minecraft mc, float[] box, float yaw,
                                   float pitch) {
        net.minecraft.world.entity.LivingEntity player = mc.player;
        int x1 = Math.round(box[0]);
        int y1 = Math.round(box[1]);
        int x2 = Math.round(box[0] + box[2]);
        int y2 = Math.round(box[1] + box[3]) - 14;
        if (x2 - x1 < 20 || y2 - y1 < 30) {
            return;
        }
        net.minecraft.client.renderer.entity.state.EntityRenderState state =
                mc.getEntityRenderDispatcher().getRenderer(player).createRenderState(player, 1.0f);
        state.lightCoords = 15728880;
        state.shadowPieces.clear();
        state.outlineColor = 0;
        state.nameTag = null;
        if (state instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState
                living) {
            living.bodyRot = 180.0f + yaw;
            living.yRot = 0;
            living.xRot = 0;
            living.boundingBoxWidth /= living.scale;
            living.boundingBoxHeight /= living.scale;
            living.scale = 1.0f;
        }
        if (state instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState avatar) {
            avatar.capeFlap = 0;
            avatar.capeLean = 6;
            avatar.capeLean2 = 0;
        }
        org.joml.Quaternionf pose = new org.joml.Quaternionf().rotateZ((float) Math.PI);
        org.joml.Quaternionf camera = new org.joml.Quaternionf()
                .rotateX(pitch * (float) (Math.PI / 180.0));
        pose.mul(camera);
        float size = Math.min((y2 - y1) / 2.3f, (x2 - x1) / 1.5f);
        org.joml.Vector3f lift = new org.joml.Vector3f(0, state.boundingBoxHeight / 2.0f, 0);
        g.submitEntityRenderState(state, size, lift, pose, camera, x1, y1, x2, y2);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return view.mouseDown(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        return view.mouseDrag(event.x(), event.y());
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return view.mouseUp();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        return view.scroll(dy);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // GLFW 修飾鍵位元：2 是 Ctrl、8 是 Super（mac 的 Cmd）
        boolean ctrl = (event.modifiers() & (2 | 8)) != 0;
        if (view.key(event.key(), ctrl)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return view.typed(event.codepointAsString());
    }

    @Override
    public void onClose() {
        cfg().saveIfDirty();
        lastTab = view == null ? lastTab : view.tab();
        this.minecraft.setScreen(parent);
    }

    @Override
    public void removed() {
        cfg().saveIfDirty();
        if (view != null) {
            lastTab = view.tab();
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
