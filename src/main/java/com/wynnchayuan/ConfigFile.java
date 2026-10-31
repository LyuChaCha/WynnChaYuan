package com.wynnchayuan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynnchayuan.translate.ErrorDebug;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class ConfigFile {

    private final CollectorConfig config;

    ConfigFile(CollectorConfig config) {
        this.config = config;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    static final Set<String> RETIRED = Set.of("shareCaptures", "showPanel");

    private static final long MAX_BYTES = 1L << 20;

    void load() {
        JsonObject o = SafeFiles.readObject(config.file, MAX_BYTES);
        if (o == null) {
            return;
        }
        Boolean showPanel = boolOrNull(o, "showPanel");
        CollectorConfig.TooltipMode tooltip = enumOf(o, "tooltipMode", CollectorConfig.TooltipMode.class);
        if (tooltip != null) {
            config.tooltipMode = tooltip;
        } else if (showPanel != null) {
            config.tooltipMode = showPanel ? CollectorConfig.TooltipMode.PANEL : CollectorConfig.TooltipMode.OFF;
        }
        config.shotMode = enumOr(o, "shotMode", CollectorConfig.ShotMode.class, config.shotMode);
        CollectorConfig.DialogueMode dialogue = enumOf(o, "dialogueMode", CollectorConfig.DialogueMode.class);
        if (dialogue != null) {
            config.dialogueMode = dialogue;
        }
        CollectorConfig.DialogueMode choice = enumOf(o, "choiceMode", CollectorConfig.DialogueMode.class);
        if (choice != null) {
            config.choiceMode = choice;
        } else if (dialogue != null) {
            config.choiceMode = dialogue;
        }
        config.chatCopy = bool(o, "chatCopy", config.chatCopy);
        config.capes = bool(o, "capes", config.capes);
        config.ownCape = bool(o, "ownCape", config.ownCape);
        config.wynntilsUi = bool(o, "wynntilsUi", config.wynntilsUi);
        config.wynntilsFps = bool(o, "wynntilsFps", config.wynntilsFps);
        config.dialogueCover = bool(o, "dialogueCover", config.dialogueCover);
        config.translateTitles = bool(o, "translateTitles", config.translateTitles);
        config.translationsEnabled = bool(o, "translationsEnabled", config.translationsEnabled);
        CollectorConfig.DialogueMode tracker = enumOf(o, "trackerMode", CollectorConfig.DialogueMode.class);
        if (tracker != null) {
            config.trackerMode = tracker;
        } else if (Boolean.FALSE.equals(boolOrNull(o, "showScoreboard"))) {
            config.trackerMode = CollectorConfig.DialogueMode.OFF;
        }
        config.translateObjectives = bool(o, "translateObjectives", config.translateObjectives);
        config.translateScoreboard = bool(o, "translateScoreboard", config.translateScoreboard);
        config.translateHeldItem = bool(o, "translateHeldItem", config.translateHeldItem);
        config.marketSearch = bool(o, "marketSearch", config.marketSearch);
        config.wynnventory = bool(o, "wynnventory", config.wynnventory);
        config.wynnmodTooltip = bool(o, "wynnmodTooltip", config.wynnmodTooltip);
        config.wynnMarketSearch = bool(o, "wynnMarketSearch", config.wynnMarketSearch);
        config.shiftPeekNames = bool(o, "shiftPeekNames", config.shiftPeekNames);
        config.abilityNamesOriginal = bool(o, "abilityNamesOriginal", config.abilityNamesOriginal);
        config.chatMode = enumOr(o, "chatMode", CollectorConfig.ChatMode.class, config.chatMode);
        Boolean overlays = boolOrNull(o, "showOverlays");
        if (overlays != null) {
            config.showOverlays = overlays;
        } else if (showPanel != null) {
            config.showOverlays = showPanel;
        }
        config.collect = bool(o, "collect", config.collect);
        config.notifiedVersion = str(o, "notifiedVersion", config.notifiedVersion);
        config.syncedTranslations = str(o, "syncedTranslations", config.syncedTranslations);
        config.syncedTranslationsDate = str(o, "syncedTranslationsDate", config.syncedTranslationsDate);
        config.autoUpdateTranslations =
                bool(o, "autoUpdateTranslations", config.autoUpdateTranslations);
        config.language = str(o, "language", config.language).trim();
        config.fallbackLanguage = str(o, "fallbackLanguage", config.fallbackLanguage).trim();
        config.uiLanguage = str(o, "uiLanguage", config.uiLanguage).trim();
        config.debugDumps = bool(o, "debugDumps", config.debugDumps);
        config.collectGuiText = bool(o, "collectGuiText", config.collectGuiText);
        config.source = enumOr(o, "source", CollectorConfig.Source.class, config.source);
        config.translateBossBar = bool(o, "translateBossBar", config.translateBossBar);
        config.nametagMode = enumOr(o, "nametagMode", CollectorConfig.NametagMode.class, config.nametagMode);
        config.panelSide = enumOr(o, "panelSide", CollectorConfig.PanelSide.class, config.panelSide);
        config.noticeDismissed = bool(o, "noticeDismissed", config.noticeDismissed);
        config.oldKeysReleased = bool(o, "oldKeysReleased", config.oldKeysReleased);
        config.consent = enumOr(o, "privacyConsent", CollectorConfig.Consent.class, config.consent);
        config.setupDone = bool(o, "setupDone", config.setupDone);
        config.dataOptOut = bool(o, "dataOptOut", config.dataOptOut);
        config.dataDeletePending = config.dataOptOut && bool(o, "dataDeletePending", config.dataDeletePending);
        config.pageNames = enumOr(o, "pageNames", CollectorConfig.ItemNames.class, config.pageNames);
        if (o.has("itemNames")) {
            config.itemNames = enumOr(o, "itemNames", CollectorConfig.ItemNames.class, config.itemNames);
        } else if (bool(o, "translateItemNames", false)) {
            config.itemNames = CollectorConfig.ItemNames.ON;
        }
        config.panelGap = PanelGap.clamp(integer(o, "panelGap", config.panelGap));
        String accent = str(o, "accentColor", config.accentColor);
        if (accent.matches("#[0-9a-fA-F]{6}")) {
            config.accentColor = accent;
        }
        String theme = str(o, "themeColor", config.accentColor);
        if (theme.matches("#[0-9a-fA-F]{6}")) {
            config.themeColor = theme;
        }
        int hold = integer(o, "dialogueHoldMs", config.dialogueHoldMs);
        config.dialogueHoldMs = hold <= 0 ? Integer.MAX_VALUE : hold;
        config.nametagHoldMs = CollectorConfig.clamp(integer(o, "nametagHoldMs", config.nametagHoldMs), 0, 60_000);
        config.panelAnchor = enumOr(o, "panelAnchor", CollectorConfig.PanelAnchor.class, config.panelAnchor);
        readPairs(o, "overlayPos", config.overlayPos);
        readPairs(o, "overlaySize", config.overlaySize);
        config.badgeRoles.clear();
        if (o.has("badgeRoles") && o.get("badgeRoles").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e
                    : o.getAsJsonObject("badgeRoles").entrySet()) {
                try {
                    config.badgeRoles.put(e.getKey(), e.getValue().getAsBoolean());
                } catch (Exception failure) {
                    ErrorDebug.note("ConfigFile#load", null, failure);
                }
            }
        }
        config.badgeRainbow = o.has("badgeRainbow") ? (Boolean) bool(o, "badgeRainbow", false) : null;
        config.badgeColours.clear();
        if (o.has("badgeColours") && o.get("badgeColours").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e
                    : o.getAsJsonObject("badgeColours").entrySet()) {
                try {
                    String hex = CollectorConfig.normalHex(e.getValue().getAsString());
                    if (hex != null) {
                        config.badgeColours.put(e.getKey(), hex);
                    }
                } catch (Exception failure) {
                    ErrorDebug.note("ConfigFile#load", null, failure);
                }
            }
        }
        config.nametagRange = CollectorConfig.clamp(number(o, "nametagRange", config.nametagRange), 2.0, 64.0);
        config.nametagAngle = CollectorConfig.clamp(number(o, "nametagAngle", config.nametagAngle), 1.0, 45.0);
        config.fixedX = integer(o, "fixedX", config.fixedX);
        config.fixedY = integer(o, "fixedY", config.fixedY);

        for (String old : RETIRED) {
            if (o.has(old)) {
                ModLog.info("設定檔帶著舊版的欄位 " + old + "，重寫一次把它清掉");
                save();
                break;
            }
        }
    }

    private static JsonElement primitive(JsonObject o, String name) {
        JsonElement el = o.get(name);
        return el == null || !el.isJsonPrimitive() ? null : el;
    }

    private static Boolean boolOrNull(JsonObject o, String name) {
        JsonElement el = primitive(o, name);
        if (el == null) {
            return null;
        }
        if (el.getAsJsonPrimitive().isBoolean()) {
            return el.getAsBoolean();
        }
        String s = el.getAsString().trim();
        if (s.equalsIgnoreCase("true")) {
            return true;
        }
        return s.equalsIgnoreCase("false") ? Boolean.FALSE : null;
    }

    private static boolean bool(JsonObject o, String name, boolean fallback) {
        Boolean v = boolOrNull(o, name);
        return v == null ? fallback : v;
    }

    private static String str(JsonObject o, String name, String fallback) {
        JsonElement el = primitive(o, name);
        return el == null || !el.getAsJsonPrimitive().isString() ? fallback : el.getAsString();
    }

    private static double number(JsonObject o, String name, double fallback) {
        JsonElement el = primitive(o, name);
        if (el == null) {
            return fallback;
        }
        try {
            double v = el.getAsDouble();
            return Double.isFinite(v) ? v : fallback;
        } catch (RuntimeException e) {
            ErrorDebug.note("ConfigFile#number", null, e);
            return fallback;
        }
    }

    private static int integer(JsonObject o, String name, int fallback) {
        double v = number(o, name, Double.NaN);
        if (Double.isNaN(v)) {
            return fallback;
        }
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, Math.round(v)));
    }

    private static <E extends Enum<E>> E enumOf(JsonObject o, String name, Class<E> type) {
        JsonElement el = primitive(o, name);
        if (el == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, el.getAsString().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            ModLog.warn("設定 " + name + " 的值 " + el + " 這一版不認得，用預設值");
            return null;
        }
    }

    private static <E extends Enum<E>> E enumOr(JsonObject o, String name, Class<E> type, E fallback) {
        E v = enumOf(o, name, type);
        return v == null ? fallback : v;
    }

    private static void readPairs(JsonObject o, String name, EnumMap<CollectorConfig.Overlay, int[]> into) {
        JsonElement el = o.get(name);
        if (el == null || !el.isJsonObject()) {
            return;
        }
        JsonObject all = el.getAsJsonObject();
        for (CollectorConfig.Overlay which : CollectorConfig.Overlay.values()) {
            JsonElement pair = all.get(which.name());
            if (pair == null || !pair.isJsonArray() || pair.getAsJsonArray().size() != 2) {
                continue;
            }
            try {
                double a = pair.getAsJsonArray().get(0).getAsDouble();
                double b = pair.getAsJsonArray().get(1).getAsDouble();
                if (Double.isFinite(a) && Double.isFinite(b)) {
                    into.put(which, new int[] {(int) a, (int) b});
                }
            } catch (RuntimeException failure) {
                ErrorDebug.note("ConfigFile#readPairs", null, failure);
            }
        }
    }

    synchronized void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("tooltipMode", config.tooltipMode.name());
            o.addProperty("showOverlays", config.showOverlays);
            o.addProperty("collect", config.collect);
            o.addProperty("notifiedVersion", config.notifiedVersion);
            o.addProperty("syncedTranslations", config.syncedTranslations);
            o.addProperty("syncedTranslationsDate", config.syncedTranslationsDate);
            o.addProperty("autoUpdateTranslations", config.autoUpdateTranslations);
            o.addProperty("language", config.language);
            o.addProperty("fallbackLanguage", config.fallbackLanguage);
            o.addProperty("uiLanguage", config.uiLanguage);
            o.addProperty("source", config.source.name());
            o.addProperty("debugDumps", config.debugDumps);
            o.addProperty("collectGuiText", config.collectGuiText);
            o.addProperty("translateBossBar", config.translateBossBar);
            o.addProperty("nametagMode", config.nametagMode.name());
            o.addProperty("panelSide", config.panelSide.name());
            o.addProperty("itemNames", config.itemNames.name());
            o.addProperty("pageNames", config.pageNames.name());
            o.addProperty("noticeDismissed", config.noticeDismissed);
            o.addProperty("oldKeysReleased", config.oldKeysReleased);
            o.addProperty("privacyConsent", config.consent.name());
            o.addProperty("setupDone", config.setupDone);
            o.addProperty("dataOptOut", config.dataOptOut);
            o.addProperty("dataDeletePending", config.dataDeletePending);
            o.addProperty("panelGap", config.panelGap);
            o.addProperty("accentColor", config.accentColor);
            o.addProperty("themeColor", config.themeColor);
            o.addProperty("dialogueHoldMs", config.dialogueHoldMs);
            o.addProperty("dialogueMode", config.dialogueMode.name());
            o.addProperty("choiceMode", config.choiceMode.name());
            o.addProperty("chatMode", config.chatMode.name());
            o.addProperty("translateTitles", config.translateTitles);
            o.addProperty("translationsEnabled", config.translationsEnabled);
            o.addProperty("trackerMode", config.trackerMode.name());
            o.addProperty("translateObjectives", config.translateObjectives);
            o.addProperty("translateScoreboard", config.translateScoreboard);
            o.addProperty("translateHeldItem", config.translateHeldItem);
            o.addProperty("wynnventory", config.wynnventory);
            o.addProperty("wynnmodTooltip", config.wynnmodTooltip);
            o.addProperty("wynnMarketSearch", config.wynnMarketSearch);
            o.addProperty("chatCopy", config.chatCopy);
            o.addProperty("capes", config.capes);
            o.addProperty("ownCape", config.ownCape);
            o.addProperty("wynntilsUi", config.wynntilsUi);
            o.addProperty("wynntilsFps", config.wynntilsFps);
            o.addProperty("dialogueCover", config.dialogueCover);
            o.addProperty("marketSearch", config.marketSearch);
            o.addProperty("shiftPeekNames", config.shiftPeekNames);
            o.addProperty("abilityNamesOriginal", config.abilityNamesOriginal);
            o.addProperty("shotMode", config.shotMode.name());
            o.addProperty("nametagHoldMs", config.nametagHoldMs);
            o.addProperty("panelAnchor", config.panelAnchor.name());
            JsonObject positions = new JsonObject();
            config.overlayPos.forEach((which, xy) -> {
                JsonArray arr = new JsonArray();
                arr.add(xy[0]);
                arr.add(xy[1]);
                positions.add(which.name(), arr);
            });
            o.add("overlayPos", positions);
            JsonObject sizes = new JsonObject();
            config.overlaySize.forEach((which, wh) -> {
                JsonArray arr = new JsonArray();
                arr.add(wh[0]);
                arr.add(wh[1]);
                sizes.add(which.name(), arr);
            });
            o.add("overlaySize", sizes);
            if (!config.badgeRoles.isEmpty()) {
                JsonObject roles = new JsonObject();
                config.badgeRoles.forEach(roles::addProperty);
                o.add("badgeRoles", roles);
            }
            if (config.badgeRainbow != null) {
                o.addProperty("badgeRainbow", config.badgeRainbow);
            }
            if (!config.badgeColours.isEmpty()) {
                JsonObject colours = new JsonObject();
                config.badgeColours.forEach(colours::addProperty);
                o.add("badgeColours", colours);
            }
            o.addProperty("nametagRange", config.nametagRange);
            o.addProperty("nametagAngle", config.nametagAngle);
            o.addProperty("fixedX", config.fixedX);
            o.addProperty("fixedY", config.fixedY);
            SafeFiles.writeAtomically(config.file, GSON.toJson(o));
        } catch (Exception e) {
            ModLog.warn("設定寫入失敗: " + e.getMessage());
        }
    }
}
