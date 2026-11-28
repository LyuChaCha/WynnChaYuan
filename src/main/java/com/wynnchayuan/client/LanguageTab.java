package com.wynnchayuan.client;

import com.wynnchayuan.LanguageLayers;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.Row;
import com.wynnchayuan.client.ui.SettingsView;
import com.wynnchayuan.client.ui.Ui;
import com.wynnchayuan.translate.LanguagePack;
import com.wynnchayuan.translate.Languages;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

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
            default -> lang.length() >= 2 ? lang.substring(0, 2).toUpperCase(Locale.ROOT) : lang;
        };
    }

    private static Row.Choice language(String lang) {
        return new Row.Choice(lang, Languages.nativeName(lang), badge(lang), false);
    }

    private static Row.Choice corpus(String lang) {
        String name = Languages.nativeName(lang);
        if (LanguagePack.needsDownload(WynnChaYuan.configDir(), lang)) {
            name = T.s("data.language.needs_download", name);
        }
        return new Row.Choice(lang, name, badge(lang), false);
    }

    private static String downloading(String lang) {
        LanguagePack.Status at = LanguagePack.status(lang);
        if (lang == null || at == null || !LanguagePack.running(lang)) {
            return null;
        }
        return T.s("data.language.downloading", at.percent() + "%");
    }

    private static String note(String lang) {
        if (lang == null || !LanguagePack.needsDownload(WynnChaYuan.configDir(), lang)
                || LanguagePack.running(lang)) {
            return null;
        }
        String name = Languages.nativeName(lang);
        String blocked = LanguageLayers.downloadBlocked();
        if (blocked != null) {
            return T.s(blocked, name);
        }
        LanguagePack.Status at = LanguagePack.status(lang);
        if (at != null && at.phase() == LanguagePack.Phase.FAILED) {
            return T.s("data.language.download_failed", name);
        }
        return null;
    }

    private static Supplier<String> hint(String key, Supplier<String> lang) {
        Supplier<String> base = SettingsScreen.t(key);
        return () -> {
            String said = note(lang.get());
            return said != null ? said : base.get();
        };
    }

    private static String percent(int done, int total) {
        return T.s("data.language.downloading",
                new LanguagePack.Status(LanguagePack.Phase.DOWNLOADING, done, total).percent() + "%");
    }

    List<Row> languages() {
        List<Row> rows = new ArrayList<>();
        rows.add(Row.select("data.language", SettingsScreen.t("data.language"),
                        hint("data.language.hint", WynnChaYuan::language))
                .choices(() -> {
                    List<Row.Choice> out = new ArrayList<>();
                    out.add(new Row.Choice("", T.s("data.language.auto",
                            Languages.nativeName(LanguageLayers.autoLanguage())), "A", true));
                    for (String lang : Languages.bundled()) {
                        out.add(corpus(lang));
                    }
                    return out;
                })
                .current(() -> pendingLanguage != null ? pendingLanguage : SettingsScreen.cfg().language())
                .choose(id -> pendingLanguage = id.equals(SettingsScreen.cfg().language()) ? null : id)
                .apply(() -> pendingLanguage != null, this::applyLanguage)
                .locked(() -> screen.toolsTab.fetching)
                .busy(() -> languageBusy != null ? languageBusy : downloading(WynnChaYuan.language())));
        rows.add(Row.select("data.fallback", SettingsScreen.t("data.fallback"),
                        hint("data.fallback.hint", LanguageLayers::fallbackLanguage))
                .choices(() -> {
                    List<Row.Choice> out = new ArrayList<>();
                    String under = LanguageLayers.fallbackLanguage();
                    out.add(new Row.Choice("", under == null
                            ? T.s("data.fallback.auto.none")
                            : T.s("data.fallback.auto", Languages.nativeName(under)), "A", true));
                    out.add(new Row.Choice(LanguageLayers.OFF, T.s("data.fallback.off"), "EN", false));
                    for (String lang : Languages.bundled()) {
                        if (!lang.equals(WynnChaYuan.language())) {
                            out.add(corpus(lang));
                        }
                    }
                    return out;
                })
                .current(() -> pendingFallback != null ? pendingFallback : SettingsScreen.cfg().fallbackLanguage())
                .choose(id -> pendingFallback = id.equals(SettingsScreen.cfg().fallbackLanguage()) ? null : id)
                .apply(() -> pendingFallback != null, this::applyFallback)
                .locked(() -> screen.toolsTab.fetching)
                .busy(() -> fallbackBusy != null ? fallbackBusy
                        : downloading(LanguageLayers.fallbackLanguage())));
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
        return rows;
    }

    List<Row> collection() {
        List<Row> rows = new ArrayList<>();
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
            report(result);
        }, (done, total) -> Minecraft.getInstance().execute(() -> {
            languageBusy = percent(done, total);
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
            report(result);
        }, (done, total) -> Minecraft.getInstance().execute(() -> {
            fallbackBusy = percent(done, total);
            screen.say(fallbackBusy, Ui.TEXT_3);
        }));
    }

    private void report(LanguageLayers.Outcome result) {
        screen.say((result.ok() ? "✔ " : "✘ ") + result.text(), result.ok() ? Ui.GREEN : Ui.RED);
    }
}
