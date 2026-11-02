package com.wynnchayuan.client;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.LanguageLayers;
import com.wynnchayuan.PanelGap;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.Row;
import com.wynnchayuan.client.ui.Ui;
import com.wynnchayuan.render.PanelShot;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

final class DisplayTabs {

    private final SettingsScreen screen;

    DisplayTabs(SettingsScreen screen) {
        this.screen = screen;
    }

    private static ModConfig cfg() {
        return SettingsScreen.cfg();
    }

    private static Row modes(Row r, Row.Tone[] tones, String... keys) {
        for (int i = 0; i < keys.length; i++) {
            r.option(T.s(keys[i]), tones[i], T.s(keys[i] + ".hint"));
        }
        return r;
    }

    private static final Row.Tone[] BOX_REPLACE_OFF = {Row.Tone.ACCENT, Row.Tone.REPLACE, Row.Tone.OFF};
    private static final Row.Tone[] REPLACE_BOX_OFF = {Row.Tone.REPLACE, Row.Tone.ACCENT, Row.Tone.OFF};
    private static final Row.Tone[] REPLACE_BOTH_OFF = {Row.Tone.REPLACE, Row.Tone.BOTH, Row.Tone.OFF};
    private static final Row.Tone[] PLAIN = {Row.Tone.PLAIN, Row.Tone.PLAIN, Row.Tone.PLAIN};

    List<Row> master() {
        return List.of(SettingsScreen.toggle("translations.enabled", true, cfg()::translationsEnabled, () -> {
            cfg().toggleTranslations();
            LanguageLayers.reloadTranslations();
            screen.say(T.s(cfg().translationsEnabled()
                    ? "translations.enabled.on" : "translations.enabled.off"), Ui.GREEN);
        }));
    }

    List<Row> items() {
        List<Row> rows = new ArrayList<>();
        rows.add(modes(Row.segment("items.tooltip", SettingsScreen.t("items.tooltip"), SettingsScreen.t("items.tooltip.hint")),
                        BOX_REPLACE_OFF, "mode.panel", "mode.replace", "mode.off")
                .selected(() -> cfg().tooltipMode().ordinal())
                .pick(i -> SettingsScreen.cycleTo(cfg()::tooltipMode, cfg()::cycleTooltipMode,
                        ModConfig.TooltipMode.values()[i]))
                .reset(() -> cfg().tooltipMode() == ModConfig.TooltipMode.PANEL,
                        () -> SettingsScreen.cycleTo(cfg()::tooltipMode, cfg()::cycleTooltipMode,
                                ModConfig.TooltipMode.PANEL))
                .details(List.of(SettingsScreen.toggle("items.shiftpeek", true, cfg()::shiftPeekNames,
                        cfg()::toggleShiftPeekNames))));
        rows.add(SettingsScreen.toggle("items.market", true, cfg()::marketSearch, cfg()::toggleMarketSearch));
        rows.add(Row.segment("items.shot", SettingsScreen.t("items.shot"), () -> {
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
                .pick(i -> SettingsScreen.cycleTo(cfg()::shotMode, cfg()::cycleShotMode,
                        ModConfig.ShotMode.values()[i]))
                .extra(SettingsScreen.t("button.keybinds"), () -> screen.client().setScreen(
                        new KeyBindsScreen(screen, screen.client().options)))
                .reset(() -> cfg().shotMode() == ModConfig.ShotMode.KEY,
                        () -> SettingsScreen.cycleTo(cfg()::shotMode, cfg()::cycleShotMode,
                                ModConfig.ShotMode.KEY)));
        return rows;
    }

    List<Row> names() {
        List<Row> rows = new ArrayList<>();
        Runnable nextName = () ->
                WynnChaYuan.translations().setNameMode(cfg().cycleItemNames(1));
        rows.add(modes(Row.segment("items.names", SettingsScreen.t("items.names"), SettingsScreen.t("items.names.hint")),
                        REPLACE_BOTH_OFF, "mode.on", "items.names.both", "mode.off")
                .selected(() -> cfg().itemNames().ordinal())
                .pick(i -> SettingsScreen.cycleTo(cfg()::itemNames, nextName,
                        ModConfig.ItemNames.values()[i]))
                .reset(() -> cfg().itemNames() == ModConfig.ItemNames.OFF,
                        () -> SettingsScreen.cycleTo(cfg()::itemNames, nextName, ModConfig.ItemNames.OFF)));
        Runnable nextPage = () ->
                WynnChaYuan.translations().setPageNameMode(cfg().cyclePageNames(1));
        rows.add(modes(Row.segment("items.pages", SettingsScreen.t("items.pages"), SettingsScreen.t("items.pages.hint")),
                        REPLACE_BOTH_OFF, "mode.on", "items.names.both", "mode.off")
                .selected(() -> cfg().pageNames().ordinal())
                .pick(i -> SettingsScreen.cycleTo(cfg()::pageNames, nextPage,
                        ModConfig.ItemNames.values()[i]))
                .reset(() -> cfg().pageNames() == ModConfig.ItemNames.ON,
                        () -> SettingsScreen.cycleTo(cfg()::pageNames, nextPage, ModConfig.ItemNames.ON)));
        rows.add(SettingsScreen.toggle("items.abilitynames", false, cfg()::abilityNamesOriginal, () ->
                WynnChaYuan.translations().setAbilityNamesWithOriginal(
                        cfg().toggleAbilityNamesOriginal())).tone(Row.Tone.BOTH));
        return rows;
    }

    List<Row> place() {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.segment("panel.anchor", SettingsScreen.t("panel.anchor"), SettingsScreen.t("panel.anchor.hint"))
                .option(T.s("mode.follow"), Row.Tone.PLAIN)
                .option(T.s("mode.pinned"), Row.Tone.PLAIN)
                .selected(() -> cfg().panelAnchor().ordinal())
                .pick(i -> SettingsScreen.cycleTo(cfg()::panelAnchor, cfg()::togglePanelAnchor,
                        ModConfig.PanelAnchor.values()[i]))
                .reset(() -> cfg().panelAnchor() == ModConfig.PanelAnchor.FOLLOW,
                        () -> SettingsScreen.cycleTo(cfg()::panelAnchor, cfg()::togglePanelAnchor,
                                ModConfig.PanelAnchor.FOLLOW)));
        rows.add(Row.action("panel.place", SettingsScreen.t("panel.place"), SettingsScreen.t("panel.place.hint"))
                .label(SettingsScreen.t("button.adjust"))
                .run(() -> screen.client().setScreen(new PositionScreen(screen))));
        rows.add(modes(Row.segment("panel.side", SettingsScreen.t("panel.side"), SettingsScreen.t("panel.side.hint")),
                        PLAIN, "mode.auto", "mode.right", "mode.left")
                .selected(() -> cfg().panelSide().ordinal())
                .pick(i -> SettingsScreen.cycleTo(cfg()::panelSide, cfg()::cyclePanelSide,
                        ModConfig.PanelSide.values()[i]))
                .reset(() -> cfg().panelSide() == ModConfig.PanelSide.AUTO,
                        () -> SettingsScreen.cycleTo(cfg()::panelSide, cfg()::cyclePanelSide,
                                ModConfig.PanelSide.AUTO)));
        rows.add(Row.slider("panel.gap", SettingsScreen.t("panel.gap"), SettingsScreen.t("panel.gap.hint"))
                .range(PanelGap.MIN, PanelGap.MAX).value(() -> cfg().panelGap())
                .slide(v -> cfg().setPanelGapLive(v))
                .format(v -> v + " px")
                .commit(cfg()::saveIfDirty)
                .reset(() -> cfg().panelGap() == ModConfig.DEFAULT_PANEL_GAP, () -> {
                    cfg().setPanelGapLive(ModConfig.DEFAULT_PANEL_GAP);
                    cfg().saveIfDirty();
                }));
        rows.add(SettingsScreen.toggle("dialogue.overlays", true, cfg()::showOverlays, cfg()::toggleOverlays));
        return rows;
    }

    private static final String DEFAULT_COLOUR = ModConfig.DEFAULT_COLOUR;

    List<Row> look() {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.colour("panel.colour", SettingsScreen.t("panel.colour"), SettingsScreen.t("panel.colour.hint"))
                .hex(() -> cfg().accentColor())
                .setHex(hex -> cfg().setAccentColorLive(hex))
                .commit(cfg()::saveIfDirty)
                .reset(() -> DEFAULT_COLOUR.equalsIgnoreCase(cfg().accentColor()),
                        () -> cfg().setAccentColor(DEFAULT_COLOUR)));
        rows.add(Row.colour("panel.theme", SettingsScreen.t("panel.theme"), SettingsScreen.t("panel.theme.hint"))
                .hex(() -> cfg().themeColor())
                .setHex(hex -> cfg().setThemeColorLive(hex))
                .commit(cfg()::saveIfDirty)
                .reset(() -> DEFAULT_COLOUR.equalsIgnoreCase(cfg().themeColor()),
                        () -> cfg().setThemeColor(DEFAULT_COLOUR)));
        return rows;
    }

    private Row dialogueMode(String key, Supplier<ModConfig.DialogueMode> get,
                             Runnable next) {
        return modes(Row.segment(key, SettingsScreen.t(key), SettingsScreen.t(key + ".hint")),
                        BOX_REPLACE_OFF, "mode.box", "mode.replace", "mode.off")
                .selected(() -> get.get().ordinal())
                .pick(i -> {
                    SettingsScreen.cycleTo(get, next, ModConfig.DialogueMode.values()[i]);
                    needBoxes("dialogue.mode".equals(key)
                            && get.get() == ModConfig.DialogueMode.PANEL);
                })
                .reset(() -> get.get() == ModConfig.DialogueMode.REPLACE,
                        () -> SettingsScreen.cycleTo(get, next, ModConfig.DialogueMode.REPLACE));
    }

    private static void needBoxes(boolean box) {
        if (box && !cfg().showOverlays()) {
            cfg().toggleOverlays();
        }
    }

    static int holdSeconds() {
        int ms = cfg().dialogueHoldMs();
        return ms == Integer.MAX_VALUE ? 0 : ms / 1000;
    }

    List<Row> dialogue() {
        List<Row> rows = new ArrayList<>();
        rows.add(dialogueMode("dialogue.mode", cfg()::dialogueMode, cfg()::cycleDialogueMode)
                .details(List.of(
                        SettingsScreen.toggle("dialogue.cover", true, cfg()::dialogueCover, cfg()::toggleDialogueCover),
                        Row.slider("dialogue.hold", SettingsScreen.t("dialogue.hold"), SettingsScreen.t("dialogue.hold.hint"))
                                .range(0, 30)
                                .value(() -> Math.min(30, holdSeconds()))
                                .slide(v -> cfg().setDialogueHoldSecondsLive(v))
                                .format(v -> v == 0 ? T.s("unit.forever") : T.s("unit.seconds", v))
                                .commit(cfg()::saveIfDirty)
                                .reset(() -> holdSeconds() == 6, () -> {
                                    cfg().setDialogueHoldSecondsLive(6);
                                    cfg().saveIfDirty();
                                }))));
        rows.add(dialogueMode("dialogue.choices", cfg()::choiceMode, cfg()::cycleChoiceMode));
        return rows;
    }

    List<Row> world() {
        List<Row> rows = new ArrayList<>();
        rows.add(modes(Row.segment("world.nametag", SettingsScreen.t("world.nametag"), SettingsScreen.t("world.nametag.hint")),
                        BOX_REPLACE_OFF, "mode.lookat", "mode.replace", "mode.off")
                .selected(() -> cfg().nametagMode().ordinal())
                .pick(i -> {
                    SettingsScreen.cycleTo(cfg()::nametagMode, cfg()::cycleNametagMode,
                            ModConfig.NametagMode.values()[i]);
                    needBoxes(cfg().nametagMode() == ModConfig.NametagMode.LOOK_AT);
                })
                .reset(() -> cfg().nametagMode() == ModConfig.NametagMode.REPLACE,
                        () -> SettingsScreen.cycleTo(cfg()::nametagMode, cfg()::cycleNametagMode,
                                ModConfig.NametagMode.REPLACE))
                .details(List.of(Row.action("world.nametag.advanced",
                                SettingsScreen.t("world.nametag.advanced"),
                                SettingsScreen.t("world.nametag.advanced.hint"))
                        .label(SettingsScreen.t("button.adjust"))
                        .run(() -> screen.client().setScreen(new NametagScreen(screen))))));
        rows.add(SettingsScreen.toggle("world.helditem", true, cfg()::translateHeldItem, cfg()::toggleHeldItem)
                .tone(Row.Tone.REPLACE));
        rows.add(SettingsScreen.toggle("world.titles", true, cfg()::translateTitles, cfg()::toggleTitles)
                .onText(SettingsScreen.t("mode.replace")).tone(Row.Tone.REPLACE));
        return rows;
    }

    List<Row> hud() {
        List<Row> rows = new ArrayList<>();
        rows.add(SettingsScreen.toggle("world.bossbar", true, cfg()::translateBossBar, cfg()::toggleBossBar)
                .onText(SettingsScreen.t("mode.replace")).tone(Row.Tone.REPLACE));
        ModConfig.DialogueMode[] order = {
            ModConfig.DialogueMode.REPLACE, ModConfig.DialogueMode.PANEL,
            ModConfig.DialogueMode.OFF};
        rows.add(modes(Row.segment("world.tracker", SettingsScreen.t("world.tracker"), SettingsScreen.t("world.tracker.hint")),
                        REPLACE_BOX_OFF, "mode.replace", "mode.panel", "mode.off")
                .selected(() -> Arrays.asList(order).indexOf(cfg().trackerMode()))
                .pick(i -> {
                    SettingsScreen.cycleTo(cfg()::trackerMode, cfg()::cycleTrackerMode, order[i]);
                    needBoxes(cfg().trackerMode() == ModConfig.DialogueMode.PANEL);
                })
                .reset(() -> cfg().trackerMode() == ModConfig.DialogueMode.REPLACE,
                        () -> SettingsScreen.cycleTo(cfg()::trackerMode, cfg()::cycleTrackerMode,
                                ModConfig.DialogueMode.REPLACE)));
        rows.add(SettingsScreen.toggle("world.objectives", true, cfg()::translateObjectives,
                cfg()::toggleObjectives).onText(SettingsScreen.t("mode.replace")).tone(Row.Tone.REPLACE));
        rows.add(SettingsScreen.toggle("world.scoreboard", true, cfg()::translateScoreboard,
                cfg()::toggleScoreboard).tone(Row.Tone.REPLACE));
        return rows;
    }

    List<Row> chat() {
        return List.of(modes(Row.segment("world.chat", SettingsScreen.t("world.chat"), SettingsScreen.t("world.chat.hint")),
                        REPLACE_BOTH_OFF, "mode.replace", "mode.both", "mode.off")
                .selected(() -> cfg().chatMode().ordinal())
                .pick(i -> SettingsScreen.cycleTo(cfg()::chatMode, cfg()::cycleChatMode,
                        ModConfig.ChatMode.values()[i]))
                .reset(() -> cfg().chatMode() == ModConfig.ChatMode.BOTH,
                        () -> SettingsScreen.cycleTo(cfg()::chatMode, cfg()::cycleChatMode,
                                ModConfig.ChatMode.BOTH))
                .details(List.of(SettingsScreen.toggle("world.chatcopy", true, cfg()::chatCopy,
                        cfg()::toggleChatCopy))));
    }
}
