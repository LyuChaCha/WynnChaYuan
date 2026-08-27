package com.wynnchayuan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 使用者設定，開關狀態會記住。
 *
 * <p>刻意做得極簡：只有幾個布林值，直接讀寫一個小 JSON，
 * 不引入設定框架，也不依賴 ModMenu／YACL——少一個依賴就少一個壞掉的理由。
 */
public final class CollectorConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;

    /**
     * 物品 tooltip 的翻譯呈現方式。
     *
     * <p>{@code PANEL} 在旁邊另開一塊，原文完全不動——預設值，最保險。
     * {@code REPLACE} 直接把原文換成譯文，畫面乾淨但看不到英文原名。
     * 兩者用的是同一套逐片段替換，格式保真程度一樣。
     */
    private TooltipMode tooltipMode = TooltipMode.PANEL;

    /** PANEL 另開面板；REPLACE 就地取代；OFF 不翻譯物品。 */
    public enum TooltipMode { PANEL, REPLACE, OFF }

    /**
     * 任務對話要怎麼呈現。
     *
     * <p>{@code PANEL} 是原本的做法：譯文另開一小塊畫在對話上方，原文原封不動。
     * {@code REPLACE} 則把遊戲自己那段對話<b>藏起來</b>，譯文畫在它原本的位置上
     * ——畫面上只剩中文，像是遊戲本來就是中文的。{@code OFF} 完全不管對話。
     *
     * <p>藏原文靠的是 Wynntils 的 {@code ActionBarRenderEvent}：Wynncraft 的對話
     * 其實是走 action bar 的一段（{@code DialogueSegment}），把那一段停用掉，
     * Wynntils 就會在送去繪製前把它從字串裡剪掉。不需要 mixin，也不會動到
     * 血量、魔力那些同樣在 action bar 上的東西。
     *
     * <p>預設仍是 {@code PANEL}。這個模組的前提是不取代原文——多人遊戲裡
     * 跟別人講「我卡在 Lava Springs」需要看得到英文。就地取代是選項，不是預設。
     */
    private DialogueMode dialogueMode = DialogueMode.PANEL;

    /** PANEL 另開小框；REPLACE 藏掉原文、譯文就地畫；OFF 不翻譯對話。 */
    public enum DialogueMode { PANEL, REPLACE, OFF }

    /**
     * 譯文截圖：什麼時候拍。
     *
     * <p>{@code OFF} 不拍。{@code KEY} 只在按下快捷鍵時拍一張。
     * {@code AUTO} 每看到一份<b>沒拍過的</b>譯文就自動拍一張，
     * 一場遊戲上限 200 張——這是給校稿用的，不是備份整個遊戲。
     */
    private ShotMode shotMode = ShotMode.KEY;

    /** OFF 不拍；KEY 按鍵才拍；AUTO 看到沒拍過的譯文就拍。 */
    public enum ShotMode { OFF, KEY, AUTO }

    public ShotMode shotMode() {
        return shotMode;
    }

    /** 在 關閉 → 快捷鍵 → 自動 之間輪替。 */
    public ShotMode cycleShotMode() {
        ShotMode[] all = ShotMode.values();
        shotMode = all[(shotMode.ordinal() + 1) % all.length];
        save();
        return shotMode;
    }

    /** 對話框與任務追蹤小框是否顯示。與 tooltip 無關，各自獨立。 */
    private boolean showOverlays = true;

    /** 是否收集未翻譯字串。 */
    private boolean collect = true;

    /**
     * 是否寫出診斷檔。
     *
     * <p><b>預設關閉。</b>那些檔案是拿來回報問題用的——對排查很有用，
     * 但一般玩家的 config 資料夾不該被十幾個 txt 洗版。要回報問題時打開，
     * 重現一次，把 config 資料夾裡的檔案附上就好。
     *
     * <p>跟「收集未翻譯字串」是兩件事：那個是<b>語料</b>（缺哪些句子要翻），
     * 這個是<b>診斷</b>（已經翻了但畫面上不對）。先前兩者共用同一個開關，
     * 於是想幫忙收集語料的人會一併收到一整包診斷檔。
     */
    private boolean debugDumps = false;

    /**
     * 是否連介面 tooltip 也收集（公會選單、任務書、製作台…）。
     *
     * <p>預設關閉：裝備與技能已有官方資料，一直掃背包只會把真正缺的
     * GUI 字串淹掉。想補 GUI 翻譯時再打開，逛一輪選單就夠。
     */
    private boolean collectGuiText = false;

    /**
     * 譯文從哪裡來。
     *
     * <p>{@code GITHUB}：進遊戲時從 repo 同步，所有人看到同一份最新翻譯。
     * {@code LOCAL}：只用 config 目錄下的檔案，給自己試譯或離線使用。
     */
    private Source source = Source.GITHUB;

    /** GITHUB 以遠端為準（本機仍可覆蓋）；LOCAL 完全只看本機檔案。 */
    public enum Source { GITHUB, LOCAL }

    /**
     * 是否翻譯 NPC 頭頂名牌。
     *
     * <p>名牌是唯一就地替換原文的地方（浮在 3D 世界裡，沒辦法開側欄），
     * 所以獨立一個開關，不想動原文的人可以只關這項。
     */
    private boolean translateNametags = true;

    /**
     * 名牌翻譯的呈現方式。
     *
     * <p>預設 {@code LOOK_AT}：原文名牌不動，注視時另外跳一個小框。
     * 就地取代會讓畫面上再也看不到原文，跟只認得英文名的老玩家就對不上話。
     */
    private NametagMode nametagMode = NametagMode.LOOK_AT;

    /** OFF 不翻；LOOK_AT 注視時顯示小框；REPLACE 直接取代名牌文字。 */
    public enum NametagMode { OFF, LOOK_AT, REPLACE }

    /** tooltip 面板放在原本 tooltip 的哪一邊。 */
    private PanelSide panelSide = PanelSide.AUTO;

    /** 面板位置選項。AUTO 會依畫面空間自動決定。 */
    public enum PanelSide { AUTO, RIGHT, LEFT }

    /**
     * 是否翻譯物品名稱。
     *
     * <p>裝備名稱多半是專有名詞，翻了反而對不上社群討論與 wiki，
     * 所以獨立一個開關，預設不翻。
     */
    private boolean translateItemNames = false;

    /**
     * 要用哪一種語言的譯文。
     *
     * <p>空字串表示「跟著遊戲語言走」——日文玩家裝好就是日文，不必先來翻設定。
     * jar 裡沒有對應的語言就退回繁體中文。見 {@code Languages#pick}。
     */
    private String language = "";

    /** 面板與原 tooltip 之間的間距（像素）。 */
    private int panelGap = 12;

    /**
     * 所有小框的主題色（框線）。以 {@code #RRGGBB} 存，方便手改設定檔。
     */
    private String accentColor = "#6FA8D8";

    /** 對話框在最後一次更新後還顯示多久（毫秒）。 */
    private int dialogueHoldMs = 6000;

    /** 名牌譯文在移開視線後還顯示多久（毫秒）。 */
    private int nametagHoldMs = 1500;

    /** 面板要跟著滑鼠，還是固定在畫面上某處。 */
    private PanelAnchor panelAnchor = PanelAnchor.FOLLOW;

    /** FOLLOW 跟著 tooltip 走；FIXED 固定在 fixedX/fixedY，可按住 ALT 拖曳。 */
    public enum PanelAnchor { FOLLOW, FIXED }

    private int fixedX = 20;
    private int fixedY = 20;

    /**
     * 三個小框各自的位置。
     *
     * <p>沒有設過就用各自的預設錨點（對話在下方置中、追蹤在左上、名牌在準心下方），
     * 所以 {@code null} 有意義，不能用 0 或 -1 當「未設定」——那些是合法座標。
     *
     * <p>存絕對像素而不是螢幕比例，是因為玩家實際在排的是「這個框不要壓到那個框」，
     * 那是像素關係。換解析度會跑掉，但重設一次就好，比每次都要重新理解比例直觀。
     *
     * <p>{@code DIALOGUE} 與 {@code NAMETAG} 存的是<b>水平中心</b>，其餘存左緣。
     * 那兩個框的寬度隨內容長短變動：存左緣的話，玩家把示意框拖到正中央，
     * 實際跳出來的短句卻會偏左——因為對齊的是左緣不是中心。
     */
    private final java.util.EnumMap<Overlay, int[]> overlayPos = new java.util.EnumMap<>(Overlay.class);

    /** 可以自由擺位的四個框。 */
    public enum Overlay { TOOLTIP, DIALOGUE, TRACKER, NAMETAG }

    public CollectorConfig(Path file) {
        this.file = file;
        load();
    }

    public TooltipMode tooltipMode() {
        return tooltipMode;
    }

    public DialogueMode dialogueMode() {
        return dialogueMode;
    }

    /** 在 小框 → 就地取代 → 關閉 之間輪替。 */
    public DialogueMode cycleDialogueMode() {
        DialogueMode[] all = DialogueMode.values();
        dialogueMode = all[(dialogueMode.ordinal() + 1) % all.length];
        save();
        return dialogueMode;
    }

    /** 在 面板 → 就地取代 → 關閉 之間輪替。 */
    public TooltipMode cycleTooltipMode() {
        TooltipMode[] all = TooltipMode.values();
        tooltipMode = all[(tooltipMode.ordinal() + 1) % all.length];
        save();
        return tooltipMode;
    }

    /** 小框（對話、追蹤、名牌）的總開關。 */
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

    public boolean debugDumps() {
        return debugDumps;
    }

    public boolean translateNametags() {
        return translateNametags;
    }

    public boolean translateItemNames() {
        return translateItemNames;
    }

    /** 設定檔裡寫的語言；空字串表示跟著遊戲走。 */
    public String language() {
        return language;
    }

    public void setLanguage(String lang) {
        language = lang == null ? "" : lang.trim();
        save();
    }

    public boolean toggleItemNames() {
        translateItemNames = !translateItemNames;
        save();
        return translateItemNames;
    }

    public int panelGap() {
        return panelGap;
    }

    /** 框線顏色，已含不透明度。 */
    public int accentARGB() {
        return 0xFF000000 | (parseHex(accentColor) & 0xFFFFFF);
    }

    /** 背景色：主題色壓暗，維持可讀性又看得出關聯。 */
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

    /** @return 是否為合法的 #RRGGBB 並已套用 */
    public boolean setAccentColor(String hex) {
        String v = hex.strip();
        if (!v.startsWith("#")) {
            v = "#" + v;
        }
        if (!v.matches("#[0-9a-fA-F]{6}")) {
            return false;
        }
        accentColor = v.toUpperCase();
        save();
        return true;
    }

    private static int parseHex(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (NumberFormatException e) {
            return 0x6FA8D8;               // 設定檔被改壞時退回預設，不要讓畫面消失
        }
    }

    public int dialogueHoldMs() {
        return dialogueHoldMs;
    }

    /**
     * 直接設定秒數。0 或負數代表「持續顯示」。
     *
     * @return 是否為合法數字
     */
    public boolean setDialogueHoldSeconds(String value) {
        Integer sec = parseSeconds(value);
        if (sec == null) {
            return false;
        }
        dialogueHoldMs = sec <= 0 ? Integer.MAX_VALUE : Math.min(sec, 600) * 1000;
        save();
        return true;
    }

    /**
     * 名牌翻譯的偵測距離（格）。
     *
     * <p>城裡 NPC 站得很密，距離放太遠會一直抓到後排的；曠野找 NPC 又希望
     * 早一點看到。所以做成可調，而不是挑一個折衷值讓兩邊都不好用。
     */
    // 預設 6 格。24 格在城裡會把整條街的攤位都掃進來——名牌是「我面前這位是誰」，
    // 不是「附近有誰」。想掃遠一點的人自己調大就好。
    private double nametagRange = 6.0;

    /**
     * 準心與名牌的夾角上限（度）。
     *
     * <p>角度小＝要對得很準才顯示，適合 NPC 密集的地方；角度大＝掃過去就會跳，
     * 適合找人。存成角度而不是餘弦值，因為設定畫面要給人看。
     */
    private double nametagAngle = 6.0;

    /**
     * 要不要顯示貢獻者標記。
     *
     * <p>預設開：這是給模組使用者之間互相認得出來用的，關掉就沒意義了。
     * 但覺得畫面吵的人應該能關。
     */
    private boolean showBadges = true;

    /** GRADIENT 多重身分逐字漸層；PRIMARY 只顯示第一個身分。 */
    private BadgeStyle badgeStyle = BadgeStyle.GRADIENT;

    public enum BadgeStyle { GRADIENT, PRIMARY }

    public boolean showBadges() {
        return showBadges;
    }

    public boolean toggleBadges() {
        showBadges = !showBadges;
        save();
        return showBadges;
    }

    public BadgeStyle badgeStyle() {
        return badgeStyle;
    }

    public BadgeStyle cycleBadgeStyle() {
        badgeStyle = badgeStyle == BadgeStyle.GRADIENT
                ? BadgeStyle.PRIMARY : BadgeStyle.GRADIENT;
        save();
        return badgeStyle;
    }

    public double nametagRange() {
        return nametagRange;
    }

    public double nametagAngle() {
        return nametagAngle;
    }

    /** @return 格式不對就回 false，讓呼叫端提示而不是靜靜吃掉 */
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
        // 下限 1 度：再小幾乎對不準；上限 45 度：再大等於整個視野都算數
        nametagAngle = Math.max(1.0, Math.min(v, 45.0));
        save();
        return true;
    }

    private static Double parseNumber(String value) {
        try {
            return Double.parseDouble(value.strip());
        } catch (Exception e) {
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

    /** 接受整數秒；空白或非數字回傳 null 讓呼叫端提示。 */
    private static Integer parseSeconds(String value) {
        try {
            return Integer.parseInt(value.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 在 3／6／10／15 秒與「持續顯示」之間輪替。 */
    public int cycleDialogueHold() {
        int[] steps = {3000, 6000, 10000, 15000, Integer.MAX_VALUE};
        dialogueHoldMs = next(steps, dialogueHoldMs);
        save();
        return dialogueHoldMs;
    }

    public int nametagHoldMs() {
        return nametagHoldMs;
    }

    /** 在 0／1.5／3／5 秒之間輪替。 */
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

    /**
     * 面板與原本 tooltip 之間留多寬。
     *
     * <p>原本是在 4/8/12/16/24 之間輪替，但「剛好合適」的值跟畫面大小、
     * GUI 縮放、個人習慣都有關，給幾個固定值總有人差那麼幾像素。
     *
     * @return 格式不對就回 false，讓呼叫端提示而不是靜靜吃掉
     */
    public boolean setPanelGap(String value) {
        Integer px = parseSeconds(value);      // 同樣是「整數就好」的解析
        if (px == null) {
            return false;
        }
        panelGap = Math.max(0, Math.min(px, 200));
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

    /** 這個框有沒有被擺過位置。沒有的話呼叫端該用自己的預設錨點。 */
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

    /** 回到預設錨點。 */
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

    /** 拖曳時呼叫。存檔有節流，不會每幀寫磁碟。 */
    public void setFixedPos(int x, int y) {
        if (fixedX == x && fixedY == y) {
            return;
        }
        fixedX = x;
        fixedY = y;
        dirty = true;
    }

    /** 把拖曳期間累積的變更寫入磁碟。 */
    public void saveIfDirty() {
        if (dirty) {
            dirty = false;
            save();
        }
    }

    private boolean dirty = false;

    public boolean toggleNametags() {
        translateNametags = !translateNametags;
        save();
        return translateNametags;
    }

    public NametagMode nametagMode() {
        return nametagMode;
    }

    /** 在 關閉 → 注視顯示 → 就地取代 之間輪替。 */
    public NametagMode cycleNametagMode() {
        NametagMode[] all = NametagMode.values();
        nametagMode = all[(nametagMode.ordinal() + 1) % all.length];
        save();
        return nametagMode;
    }

    public PanelSide panelSide() {
        return panelSide;
    }

    /** 依序在 AUTO → RIGHT → LEFT 之間輪替。 */
    public PanelSide cyclePanelSide() {
        PanelSide[] all = PanelSide.values();
        panelSide = all[(panelSide.ordinal() + 1) % all.length];
        save();
        return panelSide;
    }

    public Source source() {
        return source;
    }

    public Source toggleSource() {
        source = source == Source.GITHUB ? Source.LOCAL : Source.GITHUB;
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



    private void load() {
        if (!Files.exists(file)) {
            return;
        }
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            if (o.has("tooltipMode")) {
                tooltipMode = TooltipMode.valueOf(o.get("tooltipMode").getAsString());
            } else if (o.has("showPanel")) {
                // 舊設定檔：showPanel 為 false 代表不翻 tooltip
                tooltipMode = o.get("showPanel").getAsBoolean()
                        ? TooltipMode.PANEL : TooltipMode.OFF;
            }
            if (o.has("shotMode")) {
                shotMode = ShotMode.valueOf(o.get("shotMode").getAsString());
            }
            if (o.has("dialogueMode")) {
                // 舊設定檔沒有這個欄位，維持 PANEL——升上來的人畫面不會突然變樣
                dialogueMode = DialogueMode.valueOf(o.get("dialogueMode").getAsString());
            }
            if (o.has("showOverlays")) {
                showOverlays = o.get("showOverlays").getAsBoolean();
            } else if (o.has("showPanel")) {
                showOverlays = o.get("showPanel").getAsBoolean();
            }
            if (o.has("collect")) {
                collect = o.get("collect").getAsBoolean();
            }
            if (o.has("debugDumps")) {
                debugDumps = o.get("debugDumps").getAsBoolean();
            }
            if (o.has("collectGuiText")) {
                collectGuiText = o.get("collectGuiText").getAsBoolean();
            }
            if (o.has("source")) {
                source = Source.valueOf(o.get("source").getAsString());
            }
            if (o.has("translateNametags")) {
                translateNametags = o.get("translateNametags").getAsBoolean();
            }
            if (o.has("nametagMode")) {
                nametagMode = NametagMode.valueOf(o.get("nametagMode").getAsString());
            }
            if (o.has("panelSide")) {
                panelSide = PanelSide.valueOf(o.get("panelSide").getAsString());
            }
            if (o.has("translateItemNames")) {
                translateItemNames = o.get("translateItemNames").getAsBoolean();
            }
            if (o.has("panelGap")) {
                panelGap = o.get("panelGap").getAsInt();
            }
            if (o.has("accentColor")) {
                accentColor = o.get("accentColor").getAsString();
            }
            if (o.has("dialogueHoldMs")) {
                dialogueHoldMs = o.get("dialogueHoldMs").getAsInt();
            }
            if (o.has("nametagHoldMs")) {
                nametagHoldMs = o.get("nametagHoldMs").getAsInt();
            }
            if (o.has("panelAnchor")) {
                panelAnchor = PanelAnchor.valueOf(o.get("panelAnchor").getAsString());
            }
            if (o.has("overlayPos")) {
                com.google.gson.JsonObject positions = o.getAsJsonObject("overlayPos");
                for (Overlay which : Overlay.values()) {
                    if (!positions.has(which.name())) {
                        continue;
                    }
                    com.google.gson.JsonArray xy = positions.getAsJsonArray(which.name());
                    if (xy != null && xy.size() == 2) {
                        overlayPos.put(which,
                                new int[] {xy.get(0).getAsInt(), xy.get(1).getAsInt()});
                    }
                }
            }
            if (o.has("showBadges")) {
                showBadges = o.get("showBadges").getAsBoolean();
            }
            if (o.has("badgeStyle")) {
                badgeStyle = BadgeStyle.valueOf(o.get("badgeStyle").getAsString());
            }
            if (o.has("nametagRange")) {
                nametagRange = o.get("nametagRange").getAsDouble();
            }
            if (o.has("nametagAngle")) {
                nametagAngle = o.get("nametagAngle").getAsDouble();
            }
            if (o.has("fixedX")) {
                fixedX = o.get("fixedX").getAsInt();
            }
            if (o.has("fixedY")) {
                fixedY = o.get("fixedY").getAsInt();
            }
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 設定讀取失敗，使用預設值: " + e.getMessage());
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            JsonObject o = new JsonObject();
            o.addProperty("tooltipMode", tooltipMode.name());
            o.addProperty("showOverlays", showOverlays);
            o.addProperty("collect", collect);
            o.addProperty("source", source.name());
            o.addProperty("debugDumps", debugDumps);
            o.addProperty("collectGuiText", collectGuiText);
            o.addProperty("translateNametags", translateNametags);
            o.addProperty("nametagMode", nametagMode.name());
            o.addProperty("panelSide", panelSide.name());
            o.addProperty("translateItemNames", translateItemNames);
            o.addProperty("panelGap", panelGap);
            o.addProperty("accentColor", accentColor);
            o.addProperty("dialogueHoldMs", dialogueHoldMs);
            o.addProperty("dialogueMode", dialogueMode.name());
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
            o.addProperty("showBadges", showBadges);
            o.addProperty("badgeStyle", badgeStyle.name());
            o.addProperty("nametagRange", nametagRange);
            o.addProperty("nametagAngle", nametagAngle);
            o.addProperty("fixedX", fixedX);
            o.addProperty("fixedY", fixedY);
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(o, w);
            }
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 設定寫入失敗: " + e.getMessage());
        }
    }
}
