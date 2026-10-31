package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.CombatText;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.NameplateProbe;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.Map;

final class WynntilsNames {

    private WynntilsNames() {}

    static Component entityName(Component name, Entity entity) {
        try {
            return entityName(name, entity, WynnChaYuan.config(), WynnChaYuan.translations());
        } catch (Throwable t) {
            ErrorDebug.note("WynntilsNames#entityName", null, t);
            return name;
        }
    }

    static Component entityName(Component name, ModConfig config, TranslationStore store) {
        return entityName(name, null, config, store);
    }

    // 盔甲座浮空字不是 TextDisplay，Wynntils 名牌事件收不到
    static Component entityName(Component name, Entity entity, ModConfig config,
            TranslationStore store) {
        if (name == null || store == null || config == null
                || config.nametagMode() == ModConfig.NametagMode.OFF) {
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
            Component hit;
            StyledText text = StyledText.fromComponent(name);
            Component floating = LineTranslator.translateFloating(text, store);
            NameplateProbe.check("盔甲座名稱", text, floating, store);
            StyledText shown = floating != null
                    ? StyledText.fromComponent(floating)
                    : WynntilsText.byPart(text, store);
            hit = shown == text ? name : shown.getComponent();
            if (hit == name && config.collect()) {
                collectName(text, "label/floating");
            }
            boolean indicator = CombatText.isIndicator(text);
            StyledText original = !name.getString().isBlank()
                    && !GlyphSplitter.isGlyphOnly(text)
                    && !indicator ? text : null;
            entry = new Name(hit, original);
            (indicator ? INDICATORS : NAMES).put(name, entry);
        }
        if (entity != null && entry.original() != null) {
            LookAtTranslator.remember(entity, entry.original());
        }
        if (config.nametagMode() == ModConfig.NametagMode.LOOK_AT) {
            return name;
        }
        return entry.shown();
    }

    static void collectName(StyledText text, String ctx) {
        if (text.isEmpty() || GlyphSplitter.isGlyphOnly(text)
                || CombatText.isIndicator(text)) {
            return;
        }
        if (!WynntilsText.firstTime(NAMES_NOTED, ctx + '\0' + text.getString())) {
            return;
        }
        var captured = WynnChaYuan.store();
        LineParts owned = LineTranslator.nameFramed(text, WynnChaYuan.translations());
        if (owned != null) {
            if (captured != null) {
                captured.record(owned.template(), "name", "label", ctx);
            }
            return;
        }
        String template = GlyphSplitter.toTemplate(text);
        if (template.isBlank() || !GlyphSplitter.hasLetter(template)
                || PlayerDataFilter.carriesPlayerData(template)
                || PlayerDataFilter.looksPlayerNamed(template)
                || PlayerDataFilter.mentionsOnlinePlayerLoose(template)) {
            return;
        }
        if (captured != null) {
            captured.record(template, "name", "label", ctx);
        }
    }

    private record Name(Component shown, StyledText original) {}

    // 實測 60 個名牌全部重翻一幀要 4～9ms
    private static final Map<Component, Name> NAMES = WynntilsText.lru(512);

    private static final Map<Component, Name> INDICATORS = WynntilsText.lru(256);

    private static final Map<String, Boolean> NAMES_NOTED = WynntilsText.lru(1024);

    private static long nameAge = -1;
}
