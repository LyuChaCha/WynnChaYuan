package com.wynnchayuan.client;

import com.wynnchayuan.LanguageLayers;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.Row;
import com.wynnchayuan.client.ui.SettingsView;
import com.wynnchayuan.client.ui.Ui;
import com.wynnchayuan.translate.Languages;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

final class LanguageTab {

    private final SettingsScreen screen;

    LanguageTab(SettingsScreen screen) {
        this.screen = screen;
    }

    String pendingLanguage;

    String pendingFallback;

    private String languageBusy;

    private String fallbackBusy;

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

    List<Row> languages() {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.select("data.language", SettingsScreen.t("data.language"), SettingsScreen.t("data.language.hint"))
                .choices(() -> {
                    List<Row.Choice> out = new ArrayList<>();
                    out.add(new Row.Choice("", T.s("data.language.auto",
                            Languages.nativeName(LanguageLayers.autoLanguage())), "A", true));
                    for (String lang : Languages.bundled()) {
                        out.add(language(lang));
                    }
                    return out;
                })
                .current(() -> pendingLanguage != null ? pendingLanguage : SettingsScreen.cfg().language())
                .choose(id -> pendingLanguage = id.equals(SettingsScreen.cfg().language()) ? null : id)
                .apply(() -> pendingLanguage != null, this::applyLanguage)
                .locked(() -> screen.toolsTab.fetching)
                .busy(() -> languageBusy));
        rows.add(Row.select("data.fallback", SettingsScreen.t("data.fallback"), SettingsScreen.t("data.fallback.hint"))
                .choices(() -> {
                    List<Row.Choice> out = new ArrayList<>();
                    String under = LanguageLayers.fallbackLanguage();
                    out.add(new Row.Choice("", under == null
                            ? T.s("data.fallback.auto.none")
                            : T.s("data.fallback.auto", Languages.nativeName(under)), "A", true));
                    out.add(new Row.Choice(LanguageLayers.OFF, T.s("data.fallback.off"), "EN", false));
                    for (String lang : Languages.bundled()) {
                        if (!lang.equals(WynnChaYuan.language())) {
                            out.add(language(lang));
                        }
                    }
                    return out;
                })
                .current(() -> pendingFallback != null ? pendingFallback : SettingsScreen.cfg().fallbackLanguage())
                .choose(id -> pendingFallback = id.equals(SettingsScreen.cfg().fallbackLanguage()) ? null : id)
                .apply(() -> pendingFallback != null, this::applyFallback)
                .locked(() -> screen.toolsTab.fetching)
                .busy(() -> fallbackBusy));
        rows.add(Row.select("data.ui", SettingsScreen.t("data.ui"), SettingsScreen.t("data.ui.hint"))
                .choices(() -> {
                    List<Row.Choice> out = new ArrayList<>();
                    String following = SettingsScreen.cfg().language();
                    out.add(new Row.Choice("", T.s("data.ui.auto", Languages.nativeName(
                            following.isEmpty() ? LanguageLayers.autoLanguage() : following)),
                            "A", true));
                    for (String lang : T.available()) {
                        out.add(language(lang));
                    }
                    return out;
                })
                .current(() -> SettingsScreen.cfg().uiLanguage())
                .choose(id -> {
                    SettingsScreen.cfg().setUiLanguage(id);
                    SettingsScreen.lastTab = screen.view.tab();
                    screen.view = new SettingsView(screen.buildTabs(), screen.new Host(), SettingsScreen.lastTab);
                }));
        rows.add(SettingsScreen.toggle("data.collect", true, SettingsScreen.cfg()::collect, SettingsScreen.cfg()::toggleCollect));
        rows.add(SettingsScreen.toggle("data.collectgui", false, SettingsScreen.cfg()::collectGuiText,
                SettingsScreen.cfg()::toggleCollectGuiText));
        rows.add(SettingsScreen.toggle("data.debug", false, SettingsScreen.cfg()::debugDumps, () -> {
            SettingsScreen.cfg().toggleDebugDumps();
            screen.say(T.s(SettingsScreen.cfg().debugDumps() ? "data.debug.on" : "data.debug.off"), Ui.GREEN);
        }));
        return rows;
    }

    void applyLanguage() {
        String next = pendingLanguage;
        if (next == null) {
            return;
        }
        screen.toolsTab.fetching = true;
        screen.say(T.s("data.language.switching"), Ui.TEXT_3);
        LanguageLayers.switchLanguage(next, result -> {
            pendingLanguage = null;
            if (next.equals(pendingFallback)) {
                pendingFallback = null;
            }
            screen.toolsTab.fetching = false;
            languageBusy = null;
            screen.say("✔ " + result, Ui.GREEN);
        }, (done, total) -> Minecraft.getInstance().execute(() -> {
            languageBusy = T.s("data.language.progress", done, total);
            screen.say(languageBusy, Ui.TEXT_3);
        }));
    }

    void applyFallback() {
        String next = pendingFallback;
        if (next == null) {
            return;
        }
        screen.toolsTab.fetching = true;
        screen.say(T.s("data.language.switching"), Ui.TEXT_3);
        LanguageLayers.switchFallback(next, result -> {
            pendingFallback = null;
            screen.toolsTab.fetching = false;
            fallbackBusy = null;
            screen.say("✔ " + result, Ui.GREEN);
        }, (done, total) -> Minecraft.getInstance().execute(() -> {
            fallbackBusy = T.s("data.language.progress", done, total);
            screen.say(fallbackBusy, Ui.TEXT_3);
        }));
    }
}
