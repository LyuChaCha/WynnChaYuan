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

    List<Row> items() {
        List<Row> rows = new ArrayList<>();
        rows.add(SettingsScreen.toggle("translations.enabled", true, SettingsScreen.cfg()::translationsEnabled, () -> {
            SettingsScreen.cfg().toggleTranslations();
            LanguageLayers.reloadTranslations();
            screen.say(T.s(SettingsScreen.cfg().translationsEnabled()
                    ? "translations.enabled.on" : "translations.enabled.off"), Ui.GREEN);
        }));
        rows.add(Row.segment("items.tooltip", SettingsScreen.t("items.tooltip"), SettingsScreen.t("items.tooltip.hint"))
                .option(T.s("mode.panel"), Row.Tone.ACCENT)
                .option(T.s("mode.replace"), Row.Tone.REPLACE)
                .option(T.s("mode.off"), Row.Tone.OFF)
                .selected(() -> SettingsScreen.cfg().tooltipMode().ordinal())
                .pick(i -> SettingsScreen.cycleTo(SettingsScreen.cfg()::tooltipMode, SettingsScreen.cfg()::cycleTooltipMode,
                        ModConfig.TooltipMode.values()[i]))
                .reset(() -> SettingsScreen.cfg().tooltipMode() == ModConfig.TooltipMode.PANEL,
                        () -> SettingsScreen.cycleTo(SettingsScreen.cfg()::tooltipMode, SettingsScreen.cfg()::cycleTooltipMode,
                                ModConfig.TooltipMode.PANEL)));
        Runnable nextName = () ->
                WynnChaYuan.translations().setNameMode(SettingsScreen.cfg().cycleItemNames(1));
        rows.add(Row.segment("items.names", SettingsScreen.t("items.names"), SettingsScreen.t("items.names.hint"))
                .option(T.s("mode.on"), Row.Tone.ACCENT)
                .option(T.s("items.names.both"), Row.Tone.BOTH)
                .option(T.s("mode.off"), Row.Tone.OFF)
                .selected(() -> SettingsScreen.cfg().itemNames().ordinal())
                .pick(i -> SettingsScreen.cycleTo(SettingsScreen.cfg()::itemNames, nextName,
                        ModConfig.ItemNames.values()[i]))
                .reset(() -> SettingsScreen.cfg().itemNames() == ModConfig.ItemNames.OFF,
                        () -> SettingsScreen.cycleTo(SettingsScreen.cfg()::itemNames, nextName, ModConfig.ItemNames.OFF)));
        rows.add(SettingsScreen.toggle("items.abilitynames", false, SettingsScreen.cfg()::abilityNamesOriginal, () ->
                WynnChaYuan.translations().setAbilityNamesWithOriginal(
                        SettingsScreen.cfg().toggleAbilityNamesOriginal())));
        rows.add(SettingsScreen.toggle("items.shiftpeek", true, SettingsScreen.cfg()::shiftPeekNames,
                SettingsScreen.cfg()::toggleShiftPeekNames));
        Runnable nextPage = () ->
                WynnChaYuan.translations().setPageNameMode(SettingsScreen.cfg().cyclePageNames(1));
        rows.add(Row.segment("items.pages", SettingsScreen.t("items.pages"), SettingsScreen.t("items.pages.hint"))
                .option(T.s("mode.on"), Row.Tone.ACCENT)
                .option(T.s("items.names.both"), Row.Tone.BOTH)
                .option(T.s("items.pages.off"), Row.Tone.OFF)
                .selected(() -> SettingsScreen.cfg().pageNames().ordinal())
                .pick(i -> SettingsScreen.cycleTo(SettingsScreen.cfg()::pageNames, nextPage,
                        ModConfig.ItemNames.values()[i]))
                .reset(() -> SettingsScreen.cfg().pageNames() == ModConfig.ItemNames.ON,
                        () -> SettingsScreen.cycleTo(SettingsScreen.cfg()::pageNames, nextPage, ModConfig.ItemNames.ON)));
        rows.add(SettingsScreen.toggle("items.market", true, SettingsScreen.cfg()::marketSearch, SettingsScreen.cfg()::toggleMarketSearch));
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
                .selected(() -> SettingsScreen.cfg().shotMode().ordinal())
                .pick(i -> SettingsScreen.cycleTo(SettingsScreen.cfg()::shotMode, SettingsScreen.cfg()::cycleShotMode,
                        ModConfig.ShotMode.values()[i]))
                .extra(SettingsScreen.t("button.keybinds"), () -> screen.client().setScreen(
                        new KeyBindsScreen(screen, screen.client().options)))
                .reset(() -> SettingsScreen.cfg().shotMode() == ModConfig.ShotMode.KEY,
                        () -> SettingsScreen.cycleTo(SettingsScreen.cfg()::shotMode, SettingsScreen.cfg()::cycleShotMode,
                                ModConfig.ShotMode.KEY)));
        return rows;
    }

    List<Row> place() {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.segment("panel.anchor", SettingsScreen.t("panel.anchor"), SettingsScreen.t("panel.anchor.hint"))
                .option(T.s("mode.follow"), Row.Tone.PLAIN)
                .option(T.s("mode.pinned"), Row.Tone.PLAIN)
                .selected(() -> SettingsScreen.cfg().panelAnchor().ordinal())
                .pick(i -> SettingsScreen.cycleTo(SettingsScreen.cfg()::panelAnchor, SettingsScreen.cfg()::togglePanelAnchor,
                        ModConfig.PanelAnchor.values()[i]))
                .reset(() -> SettingsScreen.cfg().panelAnchor() == ModConfig.PanelAnchor.FOLLOW,
                        () -> SettingsScreen.cycleTo(SettingsScreen.cfg()::panelAnchor, SettingsScreen.cfg()::togglePanelAnchor,
                                ModConfig.PanelAnchor.FOLLOW)));
        rows.add(Row.action("panel.place", SettingsScreen.t("panel.place"), SettingsScreen.t("panel.place.hint"))
                .label(SettingsScreen.t("button.adjust"))
                .run(() -> screen.client().setScreen(new PositionScreen(screen))));
        rows.add(Row.segment("panel.side", SettingsScreen.t("panel.side"), SettingsScreen.t("panel.side.hint"))
                .option(T.s("mode.auto"), Row.Tone.PLAIN)
                .option(T.s("mode.right"), Row.Tone.PLAIN)
                .option(T.s("mode.left"), Row.Tone.PLAIN)
                .selected(() -> SettingsScreen.cfg().panelSide().ordinal())
                .pick(i -> SettingsScreen.cycleTo(SettingsScreen.cfg()::panelSide, SettingsScreen.cfg()::cyclePanelSide,
                        ModConfig.PanelSide.values()[i]))
                .reset(() -> SettingsScreen.cfg().panelSide() == ModConfig.PanelSide.AUTO,
                        () -> SettingsScreen.cycleTo(SettingsScreen.cfg()::panelSide, SettingsScreen.cfg()::cyclePanelSide,
                                ModConfig.PanelSide.AUTO)));
        rows.add(Row.slider("panel.gap", SettingsScreen.t("panel.gap"), SettingsScreen.t("panel.gap.hint"))
                .range(PanelGap.MIN, PanelGap.MAX).value(() -> SettingsScreen.cfg().panelGap())
                .slide(v -> SettingsScreen.cfg().setPanelGapLive(v))
                .format(v -> v + " px")
                .commit(SettingsScreen.cfg()::saveIfDirty)
                .reset(() -> SettingsScreen.cfg().panelGap() == ModConfig.DEFAULT_PANEL_GAP, () -> {
                    SettingsScreen.cfg().setPanelGapLive(ModConfig.DEFAULT_PANEL_GAP);
                    SettingsScreen.cfg().saveIfDirty();
                }));
        return rows;
    }

    private static final String DEFAULT_COLOUR = "#6FA8D8";

    List<Row> look() {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.colour("panel.theme", SettingsScreen.t("panel.theme"), SettingsScreen.t("panel.theme.hint"))
                .hex(() -> SettingsScreen.cfg().themeColor())
                .setHex(hex -> SettingsScreen.cfg().setThemeColorLive(hex))
                .commit(SettingsScreen.cfg()::saveIfDirty)
                .reset(() -> DEFAULT_COLOUR.equalsIgnoreCase(SettingsScreen.cfg().themeColor()),
                        () -> SettingsScreen.cfg().setThemeColor(DEFAULT_COLOUR)));
        rows.add(Row.colour("panel.colour", SettingsScreen.t("panel.colour"), SettingsScreen.t("panel.colour.hint"))
                .hex(() -> SettingsScreen.cfg().accentColor())
                .setHex(hex -> SettingsScreen.cfg().setAccentColorLive(hex))
                .commit(SettingsScreen.cfg()::saveIfDirty)
                .reset(() -> DEFAULT_COLOUR.equalsIgnoreCase(SettingsScreen.cfg().accentColor()),
                        () -> SettingsScreen.cfg().setAccentColor(DEFAULT_COLOUR)));
        return rows;
    }

    private Row dialogueMode(String key, Supplier<ModConfig.DialogueMode> get,
                             Runnable next) {
        return Row.segment(key, SettingsScreen.t(key), SettingsScreen.t(key + ".hint"))
                .option(T.s("mode.box"), Row.Tone.ACCENT)
                .option(T.s("mode.replace"), Row.Tone.REPLACE)
                .option(T.s("mode.off"), Row.Tone.OFF)
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
        if (box && !SettingsScreen.cfg().showOverlays()) {
            SettingsScreen.cfg().toggleOverlays();
        }
    }

    static int holdSeconds() {
        int ms = SettingsScreen.cfg().dialogueHoldMs();
        return ms == Integer.MAX_VALUE ? 0 : ms / 1000;
    }

    List<Row> dialogue() {
        List<Row> rows = new ArrayList<>();
        rows.add(dialogueMode("dialogue.mode", SettingsScreen.cfg()::dialogueMode, SettingsScreen.cfg()::cycleDialogueMode));
        rows.add(dialogueMode("dialogue.choices", SettingsScreen.cfg()::choiceMode, SettingsScreen.cfg()::cycleChoiceMode));
        rows.add(SettingsScreen.toggle("dialogue.cover", true, SettingsScreen.cfg()::dialogueCover, SettingsScreen.cfg()::toggleDialogueCover));
        rows.add(Row.slider("dialogue.hold", SettingsScreen.t("dialogue.hold"), SettingsScreen.t("dialogue.hold.hint"))
                .range(0, 30)
                .value(() -> Math.min(30, holdSeconds()))
                .slide(v -> SettingsScreen.cfg().setDialogueHoldSecondsLive(v))
                .format(v -> v == 0 ? T.s("unit.forever") : T.s("unit.seconds", v))
                .commit(SettingsScreen.cfg()::saveIfDirty)
                .reset(() -> holdSeconds() == 6, () -> {
                    SettingsScreen.cfg().setDialogueHoldSecondsLive(6);
                    SettingsScreen.cfg().saveIfDirty();
                }));
        rows.add(SettingsScreen.toggle("dialogue.overlays", true, SettingsScreen.cfg()::showOverlays, SettingsScreen.cfg()::toggleOverlays));
        return rows;
    }

    List<Row> world() {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.segment("world.nametag", SettingsScreen.t("world.nametag"), SettingsScreen.t("world.nametag.hint"))
                .option(T.s("mode.lookat"), Row.Tone.ACCENT)
                .option(T.s("mode.replace"), Row.Tone.REPLACE)
                .option(T.s("mode.off"), Row.Tone.OFF)
                .selected(() -> SettingsScreen.cfg().nametagMode().ordinal())
                .pick(i -> {
                    SettingsScreen.cycleTo(SettingsScreen.cfg()::nametagMode, SettingsScreen.cfg()::cycleNametagMode,
                            ModConfig.NametagMode.values()[i]);
                    needBoxes(SettingsScreen.cfg().nametagMode() == ModConfig.NametagMode.LOOK_AT);
                })
                .extra(SettingsScreen.t("button.advanced"), () -> screen.client().setScreen(new NametagScreen(screen)))
                .reset(() -> SettingsScreen.cfg().nametagMode() == ModConfig.NametagMode.REPLACE,
                        () -> SettingsScreen.cycleTo(SettingsScreen.cfg()::nametagMode, SettingsScreen.cfg()::cycleNametagMode,
                                ModConfig.NametagMode.REPLACE)));
        rows.add(Row.segment("world.chat", SettingsScreen.t("world.chat"), SettingsScreen.t("world.chat.hint"))
                .option(T.s("mode.replace"), Row.Tone.REPLACE)
                .option(T.s("mode.both"), Row.Tone.BOTH)
                .option(T.s("mode.off"), Row.Tone.OFF)
                .selected(() -> SettingsScreen.cfg().chatMode().ordinal())
                .pick(i -> SettingsScreen.cycleTo(SettingsScreen.cfg()::chatMode, SettingsScreen.cfg()::cycleChatMode,
                        ModConfig.ChatMode.values()[i]))
                .reset(() -> SettingsScreen.cfg().chatMode() == ModConfig.ChatMode.BOTH,
                        () -> SettingsScreen.cycleTo(SettingsScreen.cfg()::chatMode, SettingsScreen.cfg()::cycleChatMode,
                                ModConfig.ChatMode.BOTH)));
        rows.add(SettingsScreen.toggle("world.titles", true, SettingsScreen.cfg()::translateTitles, SettingsScreen.cfg()::toggleTitles)
                .onText(SettingsScreen.t("mode.replace")));
        rows.add(SettingsScreen.toggle("world.bossbar", true, SettingsScreen.cfg()::translateBossBar, SettingsScreen.cfg()::toggleBossBar)
                .onText(SettingsScreen.t("mode.replace")));
        ModConfig.DialogueMode[] order = {
            ModConfig.DialogueMode.REPLACE, ModConfig.DialogueMode.PANEL,
            ModConfig.DialogueMode.OFF};
        rows.add(Row.segment("world.tracker", SettingsScreen.t("world.tracker"), SettingsScreen.t("world.tracker.hint"))
                .option(T.s("mode.replace"), Row.Tone.REPLACE)
                .option(T.s("mode.panel"), Row.Tone.ACCENT)
                .option(T.s("mode.off"), Row.Tone.OFF)
                .selected(() -> Arrays.asList(order).indexOf(SettingsScreen.cfg().trackerMode()))
                .pick(i -> {
                    SettingsScreen.cycleTo(SettingsScreen.cfg()::trackerMode, SettingsScreen.cfg()::cycleTrackerMode, order[i]);
                    needBoxes(SettingsScreen.cfg().trackerMode() == ModConfig.DialogueMode.PANEL);
                })
                .reset(() -> SettingsScreen.cfg().trackerMode() == ModConfig.DialogueMode.REPLACE,
                        () -> SettingsScreen.cycleTo(SettingsScreen.cfg()::trackerMode, SettingsScreen.cfg()::cycleTrackerMode,
                                ModConfig.DialogueMode.REPLACE)));
        rows.add(SettingsScreen.toggle("world.objectives", true, SettingsScreen.cfg()::translateObjectives,
                SettingsScreen.cfg()::toggleObjectives).onText(SettingsScreen.t("mode.replace")));
        rows.add(SettingsScreen.toggle("world.scoreboard", true, SettingsScreen.cfg()::translateScoreboard,
                SettingsScreen.cfg()::toggleScoreboard));
        rows.add(SettingsScreen.toggle("world.helditem", true, SettingsScreen.cfg()::translateHeldItem, SettingsScreen.cfg()::toggleHeldItem));
        rows.add(SettingsScreen.toggle("world.chatcopy", true, SettingsScreen.cfg()::chatCopy, SettingsScreen.cfg()::toggleChatCopy));
        return rows;
    }
}
