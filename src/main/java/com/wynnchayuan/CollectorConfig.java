package com.wynnchayuan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import com.wynnchayuan.translate.ErrorDebug;
import java.nio.file.Path;

public final class CollectorConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final int DEFAULT_PANEL_GAP = PanelGap.DEFAULT;

    private final Path file;

    private TooltipMode tooltipMode = TooltipMode.OFF;

    public enum TooltipMode { PANEL, REPLACE, OFF }

    private DialogueMode dialogueMode = DialogueMode.REPLACE;

    public enum DialogueMode { PANEL, REPLACE, OFF }

    private DialogueMode choiceMode = DialogueMode.REPLACE;

    private ChatMode chatMode = ChatMode.BOTH;

    public enum ChatMode { REPLACE, BOTH, OFF }

    private boolean translateTitles = true;

    private boolean translationsEnabled = true;

    private DialogueMode trackerMode = DialogueMode.REPLACE;

    private boolean translateObjectives = true;

    private boolean translateScoreboard = true;

    private boolean translateHeldItem = true;

    private boolean chatCopy = true;

    private boolean capes = true;

    private boolean ownCape = true;

    private boolean wynntilsUi = true;

    private boolean wynntilsFps = true;

    private boolean dialogueCover = true;

    private boolean marketSearch = true;

    private boolean wynnMarketSearch = true;

    private boolean wynnventory = true;

    private boolean wynnmodTooltip = true;

    private boolean shiftPeekNames = true;

    private boolean abilityNamesOriginal = false;

    private ShotMode shotMode = ShotMode.KEY;

    public enum ShotMode { KEY, OFF }

    public ShotMode shotMode() {
        return shotMode;
    }

    public ShotMode cycleShotMode() {
        return cycleShotMode(1);
    }

    public ShotMode cycleShotMode(int step) {
        ShotMode[] all = ShotMode.values();
        shotMode = all[Math.floorMod(shotMode.ordinal() + step, all.length)];
        save();
        return shotMode;
    }

    private boolean showOverlays = true;

    private boolean collect = true;

    private String notifiedVersion = "";

    private String syncedTranslations = "";

    private String syncedTranslationsDate = "";

    private boolean autoUpdateTranslations = false;

    private boolean debugDumps = false;

    private boolean collectGuiText = false;

    private Source source = Source.GITHUB;

    public enum Source { GITHUB, LOCAL }

    private boolean translateBossBar = true;

    private NametagMode nametagMode = NametagMode.REPLACE;

    public enum NametagMode { LOOK_AT, REPLACE, OFF }

    private PanelSide panelSide = PanelSide.AUTO;

    public enum PanelSide { AUTO, RIGHT, LEFT }

    public enum ItemNames { ON, BOTH, OFF }

    private ItemNames itemNames = ItemNames.OFF;

    private boolean noticeDismissed = false;

    public boolean noticeDismissed() {
        return noticeDismissed;
    }

    private boolean oldKeysReleased = false;

    public boolean oldKeysReleased() {
        return oldKeysReleased;
    }

    public void markOldKeysReleased() {
        oldKeysReleased = true;
        save();
    }

    public void setNoticeDismissed(boolean value) {
        noticeDismissed = value;
        save();
    }

    private String language = "";

    private String uiLanguage = "";

    private String fallbackLanguage = "";

    private int panelGap = DEFAULT_PANEL_GAP;

    private String accentColor = "#6FA8D8";

    private String themeColor = "#6FA8D8";

    private int dialogueHoldMs = 6000;

    private int nametagHoldMs = 1500;

    private PanelAnchor panelAnchor = PanelAnchor.FOLLOW;

    public enum PanelAnchor { FOLLOW, FIXED }

    private int fixedX = 20;
    private int fixedY = 20;

    private final java.util.EnumMap<Overlay, int[]> overlayPos = new java.util.EnumMap<>(Overlay.class);

    private final java.util.EnumMap<Overlay, int[]> overlaySize = new java.util.EnumMap<>(Overlay.class);

    public enum Overlay { TOOLTIP, DIALOGUE, TRACKER, NAMETAG, CHOICES }

    private final boolean existed;

    public CollectorConfig(Path file) {
        this.file = file;
        this.existed = java.nio.file.Files.exists(file);
        load();
    }

    public boolean existedBefore() {
        return existed;
    }

    public enum Consent { UNKNOWN, AGREED, DECLINED }

    private Consent consent = Consent.UNKNOWN;
    private boolean setupDone = false;
    private boolean dataOptOut = false;
    private boolean dataDeletePending = false;

    public Consent consent() {
        return consent;
    }

    public void setConsent(Consent value) {
        consent = value == null ? Consent.UNKNOWN : value;
        save();
    }

    public boolean setupDone() {
        return setupDone;
    }

    public void markSetupDone() {
        setupDone = true;
        save();
    }

    public boolean needsFirstRun() {
        return consent == Consent.UNKNOWN || !setupDone;
    }

    public boolean dataOptOut() {
        return dataOptOut;
    }

    public boolean dataDeletePending() {
        return dataDeletePending;
    }

    public void setDataOptOut(boolean on) {
        dataOptOut = on;
        dataDeletePending = on;
        save();
    }

    public void dataDeleted() {
        dataDeletePending = false;
        save();
    }

    public void applyRecommended() {
        translationsEnabled = true;
        dialogueMode = DialogueMode.REPLACE;
        choiceMode = DialogueMode.REPLACE;
        chatMode = ChatMode.BOTH;
        nametagMode = NametagMode.REPLACE;
        tooltipMode = TooltipMode.OFF;
        itemNames = ItemNames.OFF;
        pageNames = ItemNames.ON;
        trackerMode = DialogueMode.REPLACE;
        translateScoreboard = true;
        translateObjectives = true;
        translateTitles = true;
        translateBossBar = true;
        translateHeldItem = true;
        wynntilsUi = true;
        showOverlays = true;
        save();
    }

    public void setDialogueModes(DialogueMode mode) {
        dialogueMode = mode;
        choiceMode = mode;
        if (mode == DialogueMode.PANEL) {
            showOverlays = true;
        }
        save();
    }

    public void setDialogueMode(DialogueMode mode) {
        dialogueMode = mode;
        if (mode == DialogueMode.PANEL) {
            showOverlays = true;
        }
        save();
    }

    public void setChoiceMode(DialogueMode mode) {
        choiceMode = mode;
        save();
    }

    public void setChatMode(ChatMode mode) {
        chatMode = mode;
        save();
    }

    public void setNametagMode(NametagMode mode) {
        nametagMode = mode;
        if (mode == NametagMode.LOOK_AT) {
            showOverlays = true;
        }
        save();
    }

    public void setTooltipMode(TooltipMode mode) {
        tooltipMode = mode;
        save();
    }

    public void setItemNames(ItemNames mode) {
        itemNames = mode;
        save();
    }

    public void setTrackerMode(DialogueMode mode) {
        trackerMode = mode;
        if (mode == DialogueMode.PANEL) {
            showOverlays = true;
        }
        save();
    }

    public void setTranslateScoreboard(boolean on) {
        translateScoreboard = on;
        save();
    }

    public void setTranslateTitles(boolean on) {
        translateTitles = on;
        save();
    }

    public void setTranslateBossBar(boolean on) {
        translateBossBar = on;
        save();
    }

    public void setWynntilsUi(boolean on) {
        wynntilsUi = on;
        save();
    }

    public TooltipMode tooltipMode() {
        return tooltipMode;
    }

    public DialogueMode dialogueMode() {
        return dialogueMode;
    }

    public DialogueMode choiceMode() {
        return choiceMode;
    }

    public ChatMode chatMode() {
        return chatMode;
    }

    public boolean translateTitles() {
        return translateTitles;
    }

    public boolean toggleTitles() {
        translateTitles = !translateTitles;
        save();
        return translateTitles;
    }

    public boolean translationsEnabled() {
        return translationsEnabled;
    }

    public boolean toggleTranslations() {
        translationsEnabled = !translationsEnabled;
        save();
        return translationsEnabled;
    }

    public DialogueMode trackerMode() {
        return trackerMode;
    }

    public DialogueMode cycleTrackerMode() {
        return cycleTrackerMode(1);
    }

    public DialogueMode cycleTrackerMode(int step) {
        DialogueMode[] all = DialogueMode.values();
        trackerMode = all[Math.floorMod(trackerMode.ordinal() + step, all.length)];
        save();
        return trackerMode;
    }

    public boolean translateObjectives() {
        return translateObjectives;
    }

    public boolean toggleObjectives() {
        translateObjectives = !translateObjectives;
        save();
        return translateObjectives;
    }

    public boolean translateScoreboard() {
        return translateScoreboard;
    }

    public boolean toggleScoreboard() {
        translateScoreboard = !translateScoreboard;
        save();
        return translateScoreboard;
    }

    public boolean translateHeldItem() {
        return translateHeldItem;
    }

    public boolean toggleHeldItem() {
        translateHeldItem = !translateHeldItem;
        save();
        return translateHeldItem;
    }

    public boolean wynntilsUi() {
        return wynntilsUi;
    }

    public boolean toggleWynntilsUi() {
        wynntilsUi = !wynntilsUi;
        save();
        return wynntilsUi;
    }

    public boolean wynntilsFps() {
        return wynntilsFps;
    }

    public boolean dialogueCover() {
        return dialogueCover;
    }

    public boolean toggleDialogueCover() {
        dialogueCover = !dialogueCover;
        save();
        return dialogueCover;
    }

    public boolean toggleWynntilsFps() {
        wynntilsFps = !wynntilsFps;
        save();
        return wynntilsFps;
    }

    public boolean marketSearch() {
        return marketSearch;
    }

    public boolean toggleMarketSearch() {
        marketSearch = !marketSearch;
        save();
        return marketSearch;
    }

    public boolean wynnMarketSearch() {
        return wynnMarketSearch;
    }

    public boolean toggleWynnMarketSearch() {
        wynnMarketSearch = !wynnMarketSearch;
        save();
        return wynnMarketSearch;
    }

    public boolean wynnventory() {
        return wynnventory;
    }

    public boolean toggleWynnventory() {
        wynnventory = !wynnventory;
        save();
        return wynnventory;
    }

    public boolean wynnmodTooltip() {
        return wynnmodTooltip;
    }

    public boolean toggleWynnmodTooltip() {
        wynnmodTooltip = !wynnmodTooltip;
        save();
        return wynnmodTooltip;
    }

    public boolean shiftPeekNames() {
        return shiftPeekNames;
    }

    public boolean toggleShiftPeekNames() {
        shiftPeekNames = !shiftPeekNames;
        save();
        return shiftPeekNames;
    }

    public boolean abilityNamesOriginal() {
        return abilityNamesOriginal;
    }

    public boolean toggleAbilityNamesOriginal() {
        abilityNamesOriginal = !abilityNamesOriginal;
        save();
        return abilityNamesOriginal;
    }

    public boolean chatCopy() {
        return chatCopy;
    }

    public boolean capes() {
        return capes;
    }

    public boolean toggleCapes() {
        capes = !capes;
        save();
        return capes;
    }

    public boolean ownCape() {
        return ownCape;
    }

    public boolean toggleOwnCape() {
        ownCape = !ownCape;
        save();
        return ownCape;
    }

    public boolean toggleChatCopy() {
        chatCopy = !chatCopy;
        if (!chatCopy) {
            com.wynnchayuan.capture.ChatLog.clear();
        }
        save();
        return chatCopy;
    }

    public ChatMode cycleChatMode() {
        return cycleChatMode(1);
    }

    public ChatMode cycleChatMode(int step) {
        ChatMode[] all = ChatMode.values();
        chatMode = all[Math.floorMod(chatMode.ordinal() + step, all.length)];
        save();
        return chatMode;
    }

    public DialogueMode cycleDialogueMode() {
        return cycleDialogueMode(1);
    }

    public DialogueMode cycleDialogueMode(int step) {
        DialogueMode[] all = DialogueMode.values();
        dialogueMode = all[Math.floorMod(dialogueMode.ordinal() + step, all.length)];
        save();
        return dialogueMode;
    }

    public DialogueMode cycleChoiceMode() {
        return cycleChoiceMode(1);
    }

    public DialogueMode cycleChoiceMode(int step) {
        DialogueMode[] all = DialogueMode.values();
        choiceMode = all[Math.floorMod(choiceMode.ordinal() + step, all.length)];
        save();
        return choiceMode;
    }

    public TooltipMode cycleTooltipMode() {
        return cycleTooltipMode(1);
    }

    public TooltipMode cycleTooltipMode(int step) {
        TooltipMode[] all = TooltipMode.values();
        tooltipMode = all[Math.floorMod(tooltipMode.ordinal() + step, all.length)];
        save();
        return tooltipMode;
    }

    public boolean showOverlays() {
        return showOverlays;
    }

    public boolean toggleOverlays() {
        showOverlays = !showOverlays;
        save();
        return showOverlays;
    }

    public boolean collect() {
        return collect;
    }

    public String notifiedVersion() {
        return notifiedVersion;
    }

    public void notifiedVersion(String version) {
        notifiedVersion = version == null ? "" : version;
        save();
    }

    public String syncedTranslations() {
        return syncedTranslations;
    }

    public void syncedTranslations(String version) {
        syncedTranslations = version == null ? "" : version;
        save();
    }

    public String syncedTranslationsDate() {
        return syncedTranslationsDate;
    }

    public void syncedTranslationsDate(String date) {
        syncedTranslationsDate = date == null ? "" : date;
        save();
    }

    public boolean autoUpdateTranslations() {
        return autoUpdateTranslations;
    }

    public boolean toggleAutoUpdateTranslations() {
        autoUpdateTranslations = !autoUpdateTranslations;
        save();
        return autoUpdateTranslations;
    }

    public boolean debugDumps() {
        return debugDumps;
    }

    public boolean translateBossBar() {
        return translateBossBar;
    }

    public ItemNames itemNames() {
        return itemNames;
    }

    public String language() {
        return language;
    }

    public String uiLanguage() {
        return uiLanguage;
    }

    public void setUiLanguage(String lang) {
        uiLanguage = lang == null ? "" : lang.trim();
        save();
    }

    public String fallbackLanguage() {
        return fallbackLanguage;
    }

    public void setFallbackLanguage(String lang) {
        fallbackLanguage = lang == null ? "" : lang.trim();
        save();
    }

    public void setLanguage(String lang) {
        language = lang == null ? "" : lang.trim();
        save();
    }

    private ItemNames pageNames = ItemNames.ON;

    public ItemNames pageNames() {
        return pageNames;
    }

    public ItemNames cyclePageNames(int step) {
        ItemNames[] all = ItemNames.values();
        pageNames = all[Math.floorMod(pageNames.ordinal() + step, all.length)];
        save();
        return pageNames;
    }

    public ItemNames cycleItemNames(int step) {
        ItemNames[] all = ItemNames.values();
        itemNames = all[Math.floorMod(itemNames.ordinal() + step, all.length)];
        save();
        return itemNames;
    }

    public int panelGap() {
        return panelGap;
    }

    public int accentARGB() {
        return 0xFF000000 | (parseHex(accentColor) & 0xFFFFFF);
    }

    public int backgroundARGB() {
        int c = parseHex(accentColor);
        int r = ((c >> 16) & 0xFF) / 6;
        int g = ((c >> 8) & 0xFF) / 6;
        int b = (c & 0xFF) / 6;
        return 0xE0000000 | (r << 16) | (g << 8) | b;
    }

    public String accentColor() {
        return accentColor;
    }

    public boolean setAccentColor(String hex) {
        String v = hex.strip();
        if (!v.startsWith("#")) {
            v = "#" + v;
        }
        if (!v.matches("#[0-9a-fA-F]{6}")) {
            return false;
        }
        accentColor = v.toUpperCase(java.util.Locale.ROOT);
        save();
        return true;
    }

    public int themeARGB() {
        return 0xFF000000 | (parseHex(themeColor) & 0xFFFFFF);
    }

    public String themeColor() {
        return themeColor;
    }

    public boolean setThemeColor(String hex) {
        if (!setThemeColorLive(hex)) {
            return false;
        }
        saveIfDirty();
        return true;
    }

    public boolean setThemeColorLive(String hex) {
        String v = normalHex(hex);
        if (v == null) {
            return false;
        }
        if (!v.equals(themeColor)) {
            themeColor = v;
            dirty = true;
        }
        return true;
    }

    public boolean setAccentColorLive(String hex) {
        String v = normalHex(hex);
        if (v == null) {
            return false;
        }
        if (!v.equals(accentColor)) {
            accentColor = v;
            dirty = true;
        }
        return true;
    }

    public void setPanelGapLive(int px) {
        int v = PanelGap.clamp(px);
        if (v != panelGap) {
            panelGap = v;
            dirty = true;
        }
    }

    public void setDialogueHoldSecondsLive(int sec) {
        int v = sec <= 0 ? Integer.MAX_VALUE : Math.min(sec, 600) * 1000;
        if (v != dialogueHoldMs) {
            dialogueHoldMs = v;
            dirty = true;
        }
    }

    private static String normalHex(String hex) {
        if (hex == null) {
            return null;
        }
        String v = hex.strip();
        if (!v.startsWith("#")) {
            v = "#" + v;
        }
        return v.matches("#[0-9a-fA-F]{6}") ? v.toUpperCase(java.util.Locale.ROOT) : null;
    }

    private static int parseHex(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return 0x6FA8D8;
        }
    }

    public int dialogueHoldMs() {
        return dialogueHoldMs;
    }

    public boolean setDialogueHoldSeconds(String value) {
        Integer sec = parseSeconds(value);
        if (sec == null) {
            return false;
        }
        dialogueHoldMs = sec <= 0 ? Integer.MAX_VALUE : Math.min(sec, 600) * 1000;
        save();
        return true;
    }

    private double nametagRange = 6.0;

    private double nametagAngle = 6.0;

    private final java.util.Map<String, Boolean> badgeRoles = new java.util.LinkedHashMap<>();

    private Boolean badgeRainbow;

    private final java.util.Map<String, String> badgeColours = new java.util.LinkedHashMap<>();

    public String badgeColour(String role) {
        return badgeColours.get(role);
    }

    public boolean setBadgeColourLive(String role, String hex) {
        String v = normalHex(hex);
        if (v == null) {
            return false;
        }
        if (!v.equals(badgeColours.get(role))) {
            badgeColours.put(role, v);
            dirty = true;
        }
        return true;
    }

    public void clearBadgeColour(String role) {
        if (badgeColours.remove(role) != null) {
            save();
        }
    }

    public boolean badgeRoleShown(String role, boolean registered) {
        Boolean mine = badgeRoles.get(role);
        return mine == null ? registered : mine;
    }

    public void setBadgeRole(String role, boolean shown) {
        badgeRoles.put(role, shown);
        save();
    }

    public boolean badgeRainbow(boolean registered) {
        return badgeRainbow == null ? registered : badgeRainbow;
    }

    public void setBadgeRainbow(boolean on) {
        badgeRainbow = on;
        save();
    }

    public double nametagRange() {
        return nametagRange;
    }

    public double nametagAngle() {
        return nametagAngle;
    }

    public boolean setNametagRange(String value) {
        Double v = parseNumber(value);
        if (v == null) {
            return false;
        }
        nametagRange = Math.max(2.0, Math.min(v, 64.0));
        save();
        return true;
    }

    public boolean setNametagAngle(String value) {
        Double v = parseNumber(value);
        if (v == null) {
            return false;
        }
        nametagAngle = Math.max(1.0, Math.min(v, 45.0));
        save();
        return true;
    }

    public void setNametagRangeLive(double value) {
        double v = Math.max(2.0, Math.min(value, 64.0));
        if (v != nametagRange) {
            nametagRange = v;
            dirty = true;
        }
    }

    public void setNametagAngleLive(double value) {
        double v = Math.max(1.0, Math.min(value, 45.0));
        if (v != nametagAngle) {
            nametagAngle = v;
            dirty = true;
        }
    }

    public void setNametagHoldSecondsLive(int sec) {
        int v = Math.max(0, Math.min(sec, 60)) * 1000;
        if (v != nametagHoldMs) {
            nametagHoldMs = v;
            dirty = true;
        }
    }

    private static Double parseNumber(String value) {
        try {
            return Double.parseDouble(value.strip());
        } catch (Exception e) {
            ErrorDebug.note("CollectorConfig#parseNumber", null, e);
            return null;
        }
    }

    public boolean setNametagHoldSeconds(String value) {
        Integer sec = parseSeconds(value);
        if (sec == null) {
            return false;
        }
        nametagHoldMs = Math.max(0, Math.min(sec, 60)) * 1000;
        save();
        return true;
    }

    private static Integer parseSeconds(String value) {
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public int cycleDialogueHold() {
        int[] steps = {3000, 6000, 10000, 15000, Integer.MAX_VALUE};
        dialogueHoldMs = next(steps, dialogueHoldMs);
        save();
        return dialogueHoldMs;
    }

    public int nametagHoldMs() {
        return nametagHoldMs;
    }

    public int cycleNametagHold() {
        int[] steps = {0, 1500, 3000, 5000};
        nametagHoldMs = next(steps, nametagHoldMs);
        save();
        return nametagHoldMs;
    }

    private static int next(int[] steps, int current) {
        for (int i = 0; i < steps.length; i++) {
            if (steps[i] == current) {
                return steps[(i + 1) % steps.length];
            }
        }
        return steps[0];
    }

    public boolean setPanelGap(String value) {
        Integer px = parseSeconds(value);
        if (px == null) {
            return false;
        }
        panelGap = PanelGap.clamp(px);
        save();
        return true;
    }

    public PanelAnchor panelAnchor() {
        return panelAnchor;
    }

    public PanelAnchor togglePanelAnchor() {
        panelAnchor = panelAnchor == PanelAnchor.FOLLOW
                ? PanelAnchor.FIXED : PanelAnchor.FOLLOW;
        save();
        return panelAnchor;
    }

    public boolean hasOverlayPos(Overlay which) {
        return which == Overlay.TOOLTIP || overlayPos.containsKey(which);
    }

    public int overlayX(Overlay which) {
        if (which == Overlay.TOOLTIP) {
            return fixedX;
        }
        int[] p = overlayPos.get(which);
        return p == null ? 0 : p[0];
    }

    public int overlayY(Overlay which) {
        if (which == Overlay.TOOLTIP) {
            return fixedY;
        }
        int[] p = overlayPos.get(which);
        return p == null ? 0 : p[1];
    }

    public void setOverlayPos(Overlay which, int x, int y) {
        if (which == Overlay.TOOLTIP) {
            setFixedPos(x, y);
            return;
        }
        int[] old = overlayPos.get(which);
        if (old != null && old[0] == x && old[1] == y) {
            return;
        }
        overlayPos.put(which, new int[] {x, y});
        save();
    }

    public boolean hasOverlaySize(Overlay which) {
        return overlaySize.containsKey(which);
    }

    public int overlayW(Overlay which) {
        int[] s = overlaySize.get(which);
        return s == null ? 0 : s[0];
    }

    public int overlayH(Overlay which) {
        int[] s = overlaySize.get(which);
        return s == null ? 0 : s[1];
    }

    public void setOverlaySize(Overlay which, int w, int h) {
        int[] old = overlaySize.get(which);
        if (old != null && old[0] == w && old[1] == h) {
            return;
        }
        overlaySize.put(which, new int[] {w, h});
        save();
    }

    public void clearOverlaySize(Overlay which) {
        if (overlaySize.remove(which) != null) {
            save();
        }
    }

    public void clearOverlayPos(Overlay which) {
        if (which == Overlay.TOOLTIP) {
            setFixedPos(20, 20);
            return;
        }
        if (overlayPos.remove(which) != null) {
            save();
        }
    }

    public int fixedX() {
        return fixedX;
    }

    public int fixedY() {
        return fixedY;
    }

    public void setFixedPos(int x, int y) {
        if (fixedX == x && fixedY == y) {
            return;
        }
        fixedX = x;
        fixedY = y;
        dirty = true;
    }

    public void saveIfDirty() {
        if (dirty) {
            dirty = false;
            save();
        }
    }

    private boolean dirty = false;

    public boolean toggleBossBar() {
        translateBossBar = !translateBossBar;
        save();
        return translateBossBar;
    }

    public NametagMode nametagMode() {
        return nametagMode;
    }

    public NametagMode cycleNametagMode() {
        return cycleNametagMode(1);
    }

    public NametagMode cycleNametagMode(int step) {
        NametagMode[] all = NametagMode.values();
        nametagMode = all[Math.floorMod(nametagMode.ordinal() + step, all.length)];
        save();
        return nametagMode;
    }

    public PanelSide panelSide() {
        return panelSide;
    }

    public PanelSide cyclePanelSide() {
        return cyclePanelSide(1);
    }

    public PanelSide cyclePanelSide(int step) {
        PanelSide[] all = PanelSide.values();
        panelSide = all[Math.floorMod(panelSide.ordinal() + step, all.length)];
        save();
        return panelSide;
    }

    public Source source() {
        return source;
    }

    public Source toggleSource() {
        source = source == Source.GITHUB ? Source.LOCAL : Source.GITHUB;
        com.wynnchayuan.translate.TranslationCache.syncsFromGitHub = source == Source.GITHUB;
        save();
        return source;
    }

    public boolean collectGuiText() {
        return collectGuiText;
    }

    public boolean toggleCollectGuiText() {
        collectGuiText = !collectGuiText;
        save();
        return collectGuiText;
    }

    public boolean toggleCollect() {
        collect = !collect;
        save();
        return collect;
    }

    public boolean toggleDebugDumps() {
        debugDumps = !debugDumps;
        save();
        return debugDumps;
    }



    static final java.util.Set<String> RETIRED = java.util.Set.of("shareCaptures", "showPanel");

    private static final long MAX_BYTES = 1L << 20;

    private void load() {
        JsonObject o = SafeFiles.readObject(file, MAX_BYTES);
        if (o == null) {
            return;
        }
        Boolean showPanel = boolOrNull(o, "showPanel");
        TooltipMode tooltip = enumOf(o, "tooltipMode", TooltipMode.class);
        if (tooltip != null) {
            tooltipMode = tooltip;
        } else if (showPanel != null) {
            tooltipMode = showPanel ? TooltipMode.PANEL : TooltipMode.OFF;
        }
        shotMode = enumOr(o, "shotMode", ShotMode.class, shotMode);
        DialogueMode dialogue = enumOf(o, "dialogueMode", DialogueMode.class);
        if (dialogue != null) {
            dialogueMode = dialogue;
        }
        DialogueMode choice = enumOf(o, "choiceMode", DialogueMode.class);
        if (choice != null) {
            choiceMode = choice;
        } else if (dialogue != null) {
            choiceMode = dialogue;
        }
        chatCopy = bool(o, "chatCopy", chatCopy);
        capes = bool(o, "capes", capes);
        ownCape = bool(o, "ownCape", ownCape);
        wynntilsUi = bool(o, "wynntilsUi", wynntilsUi);
        wynntilsFps = bool(o, "wynntilsFps", wynntilsFps);
        dialogueCover = bool(o, "dialogueCover", dialogueCover);
        translateTitles = bool(o, "translateTitles", translateTitles);
        translationsEnabled = bool(o, "translationsEnabled", translationsEnabled);
        DialogueMode tracker = enumOf(o, "trackerMode", DialogueMode.class);
        if (tracker != null) {
            trackerMode = tracker;
        } else if (Boolean.FALSE.equals(boolOrNull(o, "showScoreboard"))) {
            trackerMode = DialogueMode.OFF;
        }
        translateObjectives = bool(o, "translateObjectives", translateObjectives);
        translateScoreboard = bool(o, "translateScoreboard", translateScoreboard);
        translateHeldItem = bool(o, "translateHeldItem", translateHeldItem);
        marketSearch = bool(o, "marketSearch", marketSearch);
        wynnventory = bool(o, "wynnventory", wynnventory);
        wynnmodTooltip = bool(o, "wynnmodTooltip", wynnmodTooltip);
        wynnMarketSearch = bool(o, "wynnMarketSearch", wynnMarketSearch);
        shiftPeekNames = bool(o, "shiftPeekNames", shiftPeekNames);
        abilityNamesOriginal = bool(o, "abilityNamesOriginal", abilityNamesOriginal);
        chatMode = enumOr(o, "chatMode", ChatMode.class, chatMode);
        Boolean overlays = boolOrNull(o, "showOverlays");
        if (overlays != null) {
            showOverlays = overlays;
        } else if (showPanel != null) {
            showOverlays = showPanel;
        }
        collect = bool(o, "collect", collect);
        notifiedVersion = str(o, "notifiedVersion", notifiedVersion);
        syncedTranslations = str(o, "syncedTranslations", syncedTranslations);
        syncedTranslationsDate = str(o, "syncedTranslationsDate", syncedTranslationsDate);
        autoUpdateTranslations =
                bool(o, "autoUpdateTranslations", autoUpdateTranslations);
        language = str(o, "language", language).trim();
        fallbackLanguage = str(o, "fallbackLanguage", fallbackLanguage).trim();
        uiLanguage = str(o, "uiLanguage", uiLanguage).trim();
        debugDumps = bool(o, "debugDumps", debugDumps);
        collectGuiText = bool(o, "collectGuiText", collectGuiText);
        source = enumOr(o, "source", Source.class, source);
        translateBossBar = bool(o, "translateBossBar", translateBossBar);
        nametagMode = enumOr(o, "nametagMode", NametagMode.class, nametagMode);
        panelSide = enumOr(o, "panelSide", PanelSide.class, panelSide);
        noticeDismissed = bool(o, "noticeDismissed", noticeDismissed);
        oldKeysReleased = bool(o, "oldKeysReleased", oldKeysReleased);
        consent = enumOr(o, "privacyConsent", Consent.class, consent);
        setupDone = bool(o, "setupDone", setupDone);
        dataOptOut = bool(o, "dataOptOut", dataOptOut);
        dataDeletePending = dataOptOut && bool(o, "dataDeletePending", dataDeletePending);
        pageNames = enumOr(o, "pageNames", ItemNames.class, pageNames);
        if (o.has("itemNames")) {
            itemNames = enumOr(o, "itemNames", ItemNames.class, itemNames);
        } else if (bool(o, "translateItemNames", false)) {
            itemNames = ItemNames.ON;
        }
        panelGap = PanelGap.clamp(integer(o, "panelGap", panelGap));
        String accent = str(o, "accentColor", accentColor);
        if (accent.matches("#[0-9a-fA-F]{6}")) {
            accentColor = accent;
        }
        String theme = str(o, "themeColor", accentColor);
        if (theme.matches("#[0-9a-fA-F]{6}")) {
            themeColor = theme;
        }
        int hold = integer(o, "dialogueHoldMs", dialogueHoldMs);
        dialogueHoldMs = hold <= 0 ? Integer.MAX_VALUE : hold;
        nametagHoldMs = clamp(integer(o, "nametagHoldMs", nametagHoldMs), 0, 60_000);
        panelAnchor = enumOr(o, "panelAnchor", PanelAnchor.class, panelAnchor);
        readPairs(o, "overlayPos", overlayPos);
        readPairs(o, "overlaySize", overlaySize);
        badgeRoles.clear();
        if (o.has("badgeRoles") && o.get("badgeRoles").isJsonObject()) {
            for (java.util.Map.Entry<String, com.google.gson.JsonElement> e
                    : o.getAsJsonObject("badgeRoles").entrySet()) {
                try {
                    badgeRoles.put(e.getKey(), e.getValue().getAsBoolean());
                } catch (Exception failure) {
                    ErrorDebug.note("CollectorConfig#load", null, failure);
                }
            }
        }
        badgeRainbow = o.has("badgeRainbow") ? (Boolean) bool(o, "badgeRainbow", false) : null;
        badgeColours.clear();
        if (o.has("badgeColours") && o.get("badgeColours").isJsonObject()) {
            for (java.util.Map.Entry<String, com.google.gson.JsonElement> e
                    : o.getAsJsonObject("badgeColours").entrySet()) {
                try {
                    String hex = normalHex(e.getValue().getAsString());
                    if (hex != null) {
                        badgeColours.put(e.getKey(), hex);
                    }
                } catch (Exception failure) {
                    ErrorDebug.note("CollectorConfig#load", null, failure);
                }
            }
        }
        nametagRange = clamp(number(o, "nametagRange", nametagRange), 2.0, 64.0);
        nametagAngle = clamp(number(o, "nametagAngle", nametagAngle), 1.0, 45.0);
        fixedX = integer(o, "fixedX", fixedX);
        fixedY = integer(o, "fixedY", fixedY);

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
            ErrorDebug.note("CollectorConfig#number", null, e);
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
            return Enum.valueOf(type, el.getAsString().trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            ModLog.warn("設定 " + name + " 的值 " + el + " 這一版不認得，用預設值");
            return null;
        }
    }

    private static <E extends Enum<E>> E enumOr(JsonObject o, String name, Class<E> type, E fallback) {
        E v = enumOf(o, name, type);
        return v == null ? fallback : v;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static void readPairs(JsonObject o, String name, java.util.EnumMap<Overlay, int[]> into) {
        JsonElement el = o.get(name);
        if (el == null || !el.isJsonObject()) {
            return;
        }
        JsonObject all = el.getAsJsonObject();
        for (Overlay which : Overlay.values()) {
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
                ErrorDebug.note("CollectorConfig#readPairs", null, failure);
            }
        }
    }

    private synchronized void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("tooltipMode", tooltipMode.name());
            o.addProperty("showOverlays", showOverlays);
            o.addProperty("collect", collect);
            o.addProperty("notifiedVersion", notifiedVersion);
            o.addProperty("syncedTranslations", syncedTranslations);
            o.addProperty("syncedTranslationsDate", syncedTranslationsDate);
            o.addProperty("autoUpdateTranslations", autoUpdateTranslations);
            o.addProperty("language", language);
            o.addProperty("fallbackLanguage", fallbackLanguage);
            o.addProperty("uiLanguage", uiLanguage);
            o.addProperty("source", source.name());
            o.addProperty("debugDumps", debugDumps);
            o.addProperty("collectGuiText", collectGuiText);
            o.addProperty("translateBossBar", translateBossBar);
            o.addProperty("nametagMode", nametagMode.name());
            o.addProperty("panelSide", panelSide.name());
            o.addProperty("itemNames", itemNames.name());
            o.addProperty("pageNames", pageNames.name());
            o.addProperty("noticeDismissed", noticeDismissed);
            o.addProperty("oldKeysReleased", oldKeysReleased);
            o.addProperty("privacyConsent", consent.name());
            o.addProperty("setupDone", setupDone);
            o.addProperty("dataOptOut", dataOptOut);
            o.addProperty("dataDeletePending", dataDeletePending);
            o.addProperty("panelGap", panelGap);
            o.addProperty("accentColor", accentColor);
            o.addProperty("themeColor", themeColor);
            o.addProperty("dialogueHoldMs", dialogueHoldMs);
            o.addProperty("dialogueMode", dialogueMode.name());
            o.addProperty("choiceMode", choiceMode.name());
            o.addProperty("chatMode", chatMode.name());
            o.addProperty("translateTitles", translateTitles);
            o.addProperty("translationsEnabled", translationsEnabled);
            o.addProperty("trackerMode", trackerMode.name());
            o.addProperty("translateObjectives", translateObjectives);
            o.addProperty("translateScoreboard", translateScoreboard);
            o.addProperty("translateHeldItem", translateHeldItem);
            o.addProperty("wynnventory", wynnventory);
            o.addProperty("wynnmodTooltip", wynnmodTooltip);
            o.addProperty("wynnMarketSearch", wynnMarketSearch);
            o.addProperty("chatCopy", chatCopy);
            o.addProperty("capes", capes);
            o.addProperty("ownCape", ownCape);
            o.addProperty("wynntilsUi", wynntilsUi);
            o.addProperty("wynntilsFps", wynntilsFps);
            o.addProperty("dialogueCover", dialogueCover);
            o.addProperty("marketSearch", marketSearch);
            o.addProperty("shiftPeekNames", shiftPeekNames);
            o.addProperty("abilityNamesOriginal", abilityNamesOriginal);
            o.addProperty("shotMode", shotMode.name());
            o.addProperty("nametagHoldMs", nametagHoldMs);
            o.addProperty("panelAnchor", panelAnchor.name());
            com.google.gson.JsonObject positions = new com.google.gson.JsonObject();
            overlayPos.forEach((which, xy) -> {
                com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                arr.add(xy[0]);
                arr.add(xy[1]);
                positions.add(which.name(), arr);
            });
            o.add("overlayPos", positions);
            com.google.gson.JsonObject sizes = new com.google.gson.JsonObject();
            overlaySize.forEach((which, wh) -> {
                com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                arr.add(wh[0]);
                arr.add(wh[1]);
                sizes.add(which.name(), arr);
            });
            o.add("overlaySize", sizes);
            if (!badgeRoles.isEmpty()) {
                JsonObject roles = new JsonObject();
                badgeRoles.forEach(roles::addProperty);
                o.add("badgeRoles", roles);
            }
            if (badgeRainbow != null) {
                o.addProperty("badgeRainbow", badgeRainbow);
            }
            if (!badgeColours.isEmpty()) {
                JsonObject colours = new JsonObject();
                badgeColours.forEach(colours::addProperty);
                o.add("badgeColours", colours);
            }
            o.addProperty("nametagRange", nametagRange);
            o.addProperty("nametagAngle", nametagAngle);
            o.addProperty("fixedX", fixedX);
            o.addProperty("fixedY", fixedY);
            SafeFiles.writeAtomically(file, GSON.toJson(o));
        } catch (Exception e) {
            ModLog.warn("設定寫入失敗: " + e.getMessage());
        }
    }
}
