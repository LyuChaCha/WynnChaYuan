package com.wynnchayuan;


import com.wynnchayuan.capture.ChatLog;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.TranslationCache;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class CollectorConfig {

    public static final int DEFAULT_PANEL_GAP = PanelGap.DEFAULT;

    final Path file;

    final ConfigFile configFile = new ConfigFile(this);

    TooltipMode tooltipMode = TooltipMode.OFF;

    public enum TooltipMode { PANEL, REPLACE, OFF }

    DialogueMode dialogueMode = DialogueMode.REPLACE;

    public enum DialogueMode { PANEL, REPLACE, OFF }

    DialogueMode choiceMode = DialogueMode.REPLACE;

    ChatMode chatMode = ChatMode.BOTH;

    public enum ChatMode { REPLACE, BOTH, OFF }

    boolean translateTitles = true;

    boolean translationsEnabled = true;

    DialogueMode trackerMode = DialogueMode.REPLACE;

    boolean translateObjectives = true;

    boolean translateScoreboard = true;

    boolean translateHeldItem = true;

    boolean chatCopy = true;

    boolean capes = true;

    boolean ownCape = true;

    boolean wynntilsUi = true;

    boolean wynntilsFps = true;

    boolean dialogueCover = true;

    boolean marketSearch = true;

    boolean wynnMarketSearch = true;

    boolean wynnventory = true;

    boolean wynnmodTooltip = true;

    boolean shiftPeekNames = true;

    boolean abilityNamesOriginal = false;

    ShotMode shotMode = ShotMode.KEY;

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

    boolean showOverlays = true;

    volatile boolean collect = true;

    String notifiedVersion = "";

    volatile String syncedTranslations = "";

    volatile String syncedTranslationsDate = "";

    volatile boolean autoUpdateTranslations = false;

    boolean debugDumps = false;

    boolean collectGuiText = false;

    Source source = Source.GITHUB;

    public enum Source { GITHUB, LOCAL }

    boolean translateBossBar = true;

    NametagMode nametagMode = NametagMode.REPLACE;

    public enum NametagMode { LOOK_AT, REPLACE, OFF }

    PanelSide panelSide = PanelSide.AUTO;

    public enum PanelSide { AUTO, RIGHT, LEFT }

    public enum ItemNames { ON, BOTH, OFF }

    ItemNames itemNames = ItemNames.OFF;

    boolean noticeDismissed = false;

    public boolean noticeDismissed() {
        return noticeDismissed;
    }

    boolean oldKeysReleased = false;

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

    volatile String language = "";

    volatile String uiLanguage = "";

    volatile String fallbackLanguage = "";

    int panelGap = DEFAULT_PANEL_GAP;

    String accentColor = "#6FA8D8";

    String themeColor = "#6FA8D8";

    int dialogueHoldMs = 6000;

    int nametagHoldMs = 1500;

    PanelAnchor panelAnchor = PanelAnchor.FOLLOW;

    public enum PanelAnchor { FOLLOW, FIXED }

    int fixedX = 20;
    int fixedY = 20;

    final EnumMap<Overlay, int[]> overlayPos = new EnumMap<>(Overlay.class);

    final EnumMap<Overlay, int[]> overlaySize = new EnumMap<>(Overlay.class);

    public enum Overlay { TOOLTIP, DIALOGUE, TRACKER, NAMETAG, CHOICES }

    private final boolean existed;

    public CollectorConfig(Path file) {
        this.file = file;
        this.existed = Files.exists(file);
        load();
    }

    public boolean existedBefore() {
        return existed;
    }

    public enum Consent { UNKNOWN, AGREED, DECLINED }

    Consent consent = Consent.UNKNOWN;
    boolean setupDone = false;
    boolean dataOptOut = false;
    boolean dataDeletePending = false;

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
            ChatLog.clear();
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

    ItemNames pageNames = ItemNames.ON;

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
        accentColor = v.toUpperCase(Locale.ROOT);
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

    static String normalHex(String hex) {
        if (hex == null) {
            return null;
        }
        String v = hex.strip();
        if (!v.startsWith("#")) {
            v = "#" + v;
        }
        return v.matches("#[0-9a-fA-F]{6}") ? v.toUpperCase(Locale.ROOT) : null;
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

    double nametagRange = 6.0;

    double nametagAngle = 6.0;

    final Map<String, Boolean> badgeRoles = new LinkedHashMap<>();

    Boolean badgeRainbow;

    final Map<String, String> badgeColours = new LinkedHashMap<>();

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
        TranslationCache.syncsFromGitHub = source == Source.GITHUB;
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

    static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void load() {
        configFile.load();
    }

    private synchronized void save() {
        configFile.save();
    }
}
