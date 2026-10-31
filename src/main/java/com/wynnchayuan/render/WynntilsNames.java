package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;

final class WynntilsNames {

    private WynntilsNames() {}

    static net.minecraft.network.chat.Component entityName(
            net.minecraft.network.chat.Component name,
            net.minecraft.world.entity.Entity entity) {
        try {
            return entityName(name, entity, WynnChaYuan.config(), WynnChaYuan.translations());
        } catch (Throwable t) {
            ErrorDebug.note("WynntilsNames#entityName", null, t);
            return name;
        }
    }

    static net.minecraft.network.chat.Component entityName(
            net.minecraft.network.chat.Component name, CollectorConfig config,
            TranslationStore store) {
        return entityName(name, null, config, store);
    }

    // 盔甲座浮空字不是 TextDisplay，Wynntils 名牌事件收不到
    static net.minecraft.network.chat.Component entityName(
            net.minecraft.network.chat.Component name,
            net.minecraft.world.entity.Entity entity, CollectorConfig config,
            TranslationStore store) {
        if (name == null || store == null || config == null
                || config.nametagMode() == CollectorConfig.NametagMode.OFF) {
            return name;
        }
        if (nameAge != store.cacheAge()) {
            NAMES.clear();
            INDICATORS.clear();
            synchronized (NAMES_NOTED) {
                NAMES_NOTED.clear();
            }
            nameAge = store.cacheAge();
        }
        Name entry = NAMES.get(name);
        if (entry == null) {
            entry = INDICATORS.get(name);
        }
        if (entry == null) {
            net.minecraft.network.chat.Component hit;
            StyledText text = StyledText.fromComponent(name);
            net.minecraft.network.chat.Component floating =
                    LineTranslator.translateFloating(text, store);
            com.wynnchayuan.translate.NameplateProbe.check("盔甲座名稱", text, floating, store);
            StyledText shown = floating != null
                    ? StyledText.fromComponent(floating)
                    : WynntilsText.byPart(text, store);
            hit = shown == text ? name : shown.getComponent();
            if (hit == name && config.collect()) {
                collectName(text, "label/floating");
            }
            boolean indicator = com.wynnchayuan.capture.CombatText.isIndicator(text);
            StyledText original = !name.getString().isBlank()
                    && !com.wynnchayuan.capture.GlyphSplitter.isGlyphOnly(text)
                    && !indicator ? text : null;
            entry = new Name(hit, original);
            (indicator ? INDICATORS : NAMES).put(name, entry);
        }
        if (entity != null && entry.original() != null) {
            LookAtTranslator.remember(entity, entry.original());
        }
        if (config.nametagMode() == CollectorConfig.NametagMode.LOOK_AT) {
            return name;
        }
        return entry.shown();
    }

    static void collectName(StyledText text, String ctx) {
        if (text.isEmpty() || com.wynnchayuan.capture.GlyphSplitter.isGlyphOnly(text)
                || com.wynnchayuan.capture.CombatText.isIndicator(text)) {
            return;
        }
        if (!WynntilsText.firstTime(NAMES_NOTED, ctx + '\0' + text.getString())) {
            return;
        }
        var captured = WynnChaYuan.store();
        com.wynnchayuan.capture.LineParts owned =
                LineTranslator.nameFramed(text, WynnChaYuan.translations());
        if (owned != null) {
            if (captured != null) {
                captured.record(owned.template(), "name", "label", ctx);
            }
            return;
        }
        String template = com.wynnchayuan.capture.GlyphSplitter.toTemplate(text);
        if (template.isBlank() || !com.wynnchayuan.capture.GlyphSplitter.hasLetter(template)
                || com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(template)
                || com.wynnchayuan.capture.PlayerDataFilter.looksPlayerNamed(template)
                || com.wynnchayuan.capture.PlayerDataFilter.mentionsOnlinePlayerLoose(template)) {
            return;
        }
        if (captured != null) {
            captured.record(template, "name", "label", ctx);
        }
    }

    private record Name(net.minecraft.network.chat.Component shown, StyledText original) {}

    // 實測 60 個名牌全部重翻一幀要 4～9ms
    private static final java.util.Map<net.minecraft.network.chat.Component, Name> NAMES = WynntilsText.lru(512);

    private static final java.util.Map<net.minecraft.network.chat.Component, Name> INDICATORS = WynntilsText.lru(256);

    private static final java.util.Map<String, Boolean> NAMES_NOTED = WynntilsText.lru(1024);

    private static long nameAge = -1;
}
