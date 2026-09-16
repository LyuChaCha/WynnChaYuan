package com.wynnchayuan;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
    private DialogueMode dialogueMode = DialogueMode.REPLACE;

    /** PANEL 另開小框；REPLACE 藏掉原文、譯文就地畫；OFF 不翻譯對話。 */
    public enum DialogueMode { PANEL, REPLACE, OFF }

    /**
     * 對話<b>選項</b>要怎麼顯示，跟上面的內文分開管。
     *
     * <h2>為什麼要另外一個開關</h2>
     * 選項不在 NPC 那句話裡——它是另一條訊息、另一個框、另一組字型
     * （見 {@link com.wynnchayuan.render.DialogueRewriter}）。兩邊分開之後
     * 才有「內文就地取代、選項留面板」這種組合；先前選項是跟著內文走的，
     * 於是就地取代模式下同一組選項會<b>出現兩次</b>：遊戲框裡一次、
     * 我們的面板裡再一次。
     *
     * <p>預設 {@code REPLACE}，跟內文一致。
     */
    private DialogueMode choiceMode = DialogueMode.REPLACE;

    /**
     * 聊天視窗裡的<b>伺服器訊息</b>要不要翻。
     *
     * <p>任務完成的獎勵清單、進出區域的提示這些都走聊天而不是對話框。
     * 只碰伺服器發的（見 {@code ChatListener} 的白名單）——別的玩家打的字
     * 是真人寫的，不翻也不動。
     *
     * <p>預設 {@code OFF}。這個模組的前提是不取代原文，新的取代面一律要
     * 玩家自己打開。{@code BOTH} 是折衷：原文留著，下面補一行譯文。
     */
    private ChatMode chatMode = ChatMode.BOTH;

    /** OFF 不翻；REPLACE 就地取代；BOTH 原文下面再補一行譯文。 */
    public enum ChatMode { OFF, REPLACE, BOTH }

    /**
     * 螢幕正中央那行大字要不要翻（見 {@code TitleListener}）。
     *
     * <p>只有開與關——那是畫面中央的大字，兩三秒就消失，旁邊沒有空間再開小框，
     * 所以要翻就只能就地取代。預設開著：這種提示（「你正在掛機」「移動以繼續」）
     * 純粹是系統訊息，留著英文對誰都沒有好處。
     */
    private boolean translateTitles = true;

    /**
     * 要不要記下最近的聊天訊息，供「複製聊天」使用。
     *
     * <p>跟聊天<b>翻譯</b>是兩回事：翻譯關掉的人一樣可能想複製原文，
     * 而別人講的話我們從來不翻、卻常常是最想複製的那幾行。
     *
     * <p>預設開著。這份紀錄只留在記憶體、不寫檔、不進語料，
     * 關掉就只是不再記（見 {@code ChatLog}）。
     */
    private boolean chatCopy = true;

    /**
     * 市集搜尋打中文自動換成英文。
     *
     * <p>預設<b>開啟</b>：用就地取代的人看到的是中文，照著打卻搜不到東西——
     * 那是翻譯造成的問題，預設就該補起來。不想要的人可以關掉。
     */
    private boolean marketSearch = true;

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
        return cycleShotMode(1);
    }

    /**
     * @param step 往前幾格。{@code -1} 就是回到上一個——設定畫面的右鍵走這條。
     */
    public ShotMode cycleShotMode(int step) {
        ShotMode[] all = ShotMode.values();
        shotMode = all[Math.floorMod(shotMode.ordinal() + step, all.length)];
        save();
        return shotMode;
    }

    /** 對話框與任務追蹤小框是否顯示。與 tooltip 無關，各自獨立。 */
    private boolean showOverlays = true;

    /** 是否收集未翻譯字串。 */
    private boolean collect = true;

    // 先前這裡有 shareCaptures（自動分享語料的開關）。自動上傳整個拿掉了，
    // 開關也跟著拿掉；舊設定檔裡留著那個欄位不影響讀取，下次存檔就消失。

    /**
     * 已經提示過「有新版」的那個版本號。
     *
     * <p>每次進遊戲都跳一次的更新提示，第三次之後就沒有人在看了。記住提示過
     * 哪一版，同一版只講一次；下一版出來時因為版本號不同，會再講一次。
     */
    private String notifiedVersion = "";

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
    private NametagMode nametagMode = NametagMode.REPLACE;

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
     *
     * <p>寫死一個語言的用途是<b>校稿</b>：譯者的 Minecraft 是繁中，但要看
     * 簡中翻得對不對，總不能為了看一眼就把整個遊戲切成簡體再切回來。
     * 指名之後 {@code Languages#pick} 不再看遊戲設定，也不再看 jar 裡打包了
     * 哪幾種——譯文本來就是啟動時從 GitHub 抓的，jar 裡有沒有不影響。
     */
    private String language = "";

    /**
     * F6 這幾個畫面自己用哪一種語言。
     *
     * <h2>為什麼跟譯文語言分開</h2>
     * 介面原本跟著譯文語言走。但譯文語言的清單只列<b>有語料</b>的語言，
     * 而介面語言檔可以先做好——日文與韓文的介面早就翻完了，卻永遠選不到，
     * 也就沒辦法測。
     *
     * <p>空字串＝跟著譯文語言（原本的行為）。
     */
    private String uiLanguage = "";

    /**
     * 沒翻到的地方要退回哪一種語言。
     *
     * <p>三種值：
     *
     * <ul>
     *   <li>空字串＝<b>自動</b>。同語族的墊在底下（簡中沒翻到就看繁中），
     *       不同語族的不墊——見 {@code Languages#fallbackFor}。</li>
     *   <li>{@code "off"}＝<b>不墊</b>。沒翻到就顯示英文原文。</li>
     *   <li>語言代碼＝指名拿那一種墊。</li>
     * </ul>
     *
     * <p>為什麼要能關：簡中剛開始翻的時候，滿畫面繁中對某些人是幫助、對
     * 某些人是干擾——他寧可看原文，至少那是他本來就在讀的東西。這件事
     * 沒有一個對所有人都對的答案，所以交給玩家。
     */
    private String fallbackLanguage = "";

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

    /**
     * 玩家自己拉的框大小，{@code {寬, 高}}。沒設過就不在這張表裡，呼叫端沿用
     * 自己算出來的尺寸。
     *
     * <p><b>寬是實際寬度，高是最小高度。</b>兩者不對稱是刻意的：寬度決定
     * 換行位置，是玩家真正想控制的東西；高度則由折了幾行決定，硬鎖住只會
     * 把長句切掉。設成下限的話，短句也撐得住一個固定大小的框——NPC 是一個字
     * 一個字打出來的，框跟著行數長高會看起來像在抽搐。
     */
    private final java.util.EnumMap<Overlay, int[]> overlaySize = new java.util.EnumMap<>(Overlay.class);

    /** 可以自由擺位的四個框。 */
    public enum Overlay { TOOLTIP, DIALOGUE, TRACKER, NAMETAG, CHOICES }

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

    /** 見 {@link #choiceMode}。 */
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

    public boolean marketSearch() {
        return marketSearch;
    }

    public boolean toggleMarketSearch() {
        marketSearch = !marketSearch;
        save();
        return marketSearch;
    }

    public boolean chatCopy() {
        return chatCopy;
    }

    public boolean toggleChatCopy() {
        chatCopy = !chatCopy;
        if (!chatCopy) {
            com.wynnchayuan.capture.ChatLog.clear();   // 關掉就別留著
        }
        save();
        return chatCopy;
    }

    /** 在 關閉 → 就地取代 → 原文加譯文 之間輪替。 */
    public ChatMode cycleChatMode() {
        return cycleChatMode(1);
    }

    /**
     * @param step 往前幾格。{@code -1} 就是回到上一個——設定畫面的右鍵走這條。
     */
    public ChatMode cycleChatMode(int step) {
        ChatMode[] all = ChatMode.values();
        chatMode = all[Math.floorMod(chatMode.ordinal() + step, all.length)];
        save();
        return chatMode;
    }

    /** 在 小框 → 就地取代 → 關閉 之間輪替。 */
    public DialogueMode cycleDialogueMode() {
        return cycleDialogueMode(1);
    }

    /**
     * @param step 往前幾格。{@code -1} 就是回到上一個——設定畫面的右鍵走這條。
     */
    public DialogueMode cycleDialogueMode(int step) {
        DialogueMode[] all = DialogueMode.values();
        dialogueMode = all[Math.floorMod(dialogueMode.ordinal() + step, all.length)];
        save();
        return dialogueMode;
    }

    /** 在 面板 → 就地取代 → 關閉 之間輪替。見 {@link #choiceMode}。 */
    public DialogueMode cycleChoiceMode() {
        return cycleChoiceMode(1);
    }

    /**
     * @param step 往前幾格。{@code -1} 就是回到上一個——設定畫面的右鍵走這條。
     */
    public DialogueMode cycleChoiceMode(int step) {
        DialogueMode[] all = DialogueMode.values();
        choiceMode = all[Math.floorMod(choiceMode.ordinal() + step, all.length)];
        save();
        return choiceMode;
    }

    /** 在 面板 → 就地取代 → 關閉 之間輪替。 */
    public TooltipMode cycleTooltipMode() {
        return cycleTooltipMode(1);
    }

    /**
     * @param step 往前幾格。{@code -1} 就是回到上一個——設定畫面的右鍵走這條。
     */
    public TooltipMode cycleTooltipMode(int step) {
        TooltipMode[] all = TooltipMode.values();
        tooltipMode = all[Math.floorMod(tooltipMode.ordinal() + step, all.length)];
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

    public String notifiedVersion() {
        return notifiedVersion;
    }

    public void notifiedVersion(String version) {
        notifiedVersion = version == null ? "" : version;
        save();
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

    /** 見 {@link #uiLanguage}：空字串＝跟著譯文語言。 */
    public String uiLanguage() {
        return uiLanguage;
    }

    public void setUiLanguage(String lang) {
        uiLanguage = lang == null ? "" : lang.trim();
        save();
    }

    /** 見 {@link #fallbackLanguage}：空字串＝自動、{@code "off"}＝不墊。 */
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

    /** 這個框的大小有沒有被拉過。沒有的話呼叫端該用自己算出來的尺寸。 */
    public boolean hasOverlaySize(Overlay which) {
        return overlaySize.containsKey(which);
    }

    /** 玩家拉的寬度；沒設過回 0。 */
    public int overlayW(Overlay which) {
        int[] s = overlaySize.get(which);
        return s == null ? 0 : s[0];
    }

    /** 玩家拉的高度，意義是<b>最小</b>高度；沒設過回 0。 */
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

    /** 回到自動算的大小。 */
    public void clearOverlaySize(Overlay which) {
        if (overlaySize.remove(which) != null) {
            save();
        }
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
        return cycleNametagMode(1);
    }

    /**
     * @param step 往前幾格。{@code -1} 就是回到上一個——設定畫面的右鍵走這條。
     */
    public NametagMode cycleNametagMode(int step) {
        NametagMode[] all = NametagMode.values();
        nametagMode = all[Math.floorMod(nametagMode.ordinal() + step, all.length)];
        save();
        return nametagMode;
    }

    public PanelSide panelSide() {
        return panelSide;
    }

    /** 依序在 AUTO → RIGHT → LEFT 之間輪替。 */
    public PanelSide cyclePanelSide() {
        return cyclePanelSide(1);
    }

    /**
     * @param step 往前幾格。{@code -1} 就是回到上一個——設定畫面的右鍵走這條。
     */
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



    /**
     * 舊版寫過、這一版已經不用的欄位。
     *
     * <p>讀到就在載入後立刻重寫一次設定檔把它們清掉，不必等玩家下一次改設定。
     * {@code showPanel} 是 tooltipMode／showOverlays 的前身，載入時先換算過來；
     * {@code shareCaptures} 是 0.1.9_6 拿掉的自動分享開關。
     */
    static final java.util.Set<String> RETIRED = java.util.Set.of("shareCaptures", "showPanel");

    /** 設定檔正常只有 1 KB 上下。大到這個程度一定不是這個模組寫的。 */
    private static final long MAX_BYTES = 1L << 20;

    /**
     * 讀設定。
     *
     * <h2>為什麼每一欄各自讀</h2>
     * 先前整份包在一個 try 裡，用 {@code getAsBoolean}／{@code valueOf} 直接讀。
     * 任何一欄型別不對（手改成字串、寫成 null、較新版本才有的列舉值）就丟例外，
     * <b>那一欄之後的全部</b>都退回預設——玩家的設定看起來像被隨機重設了一半。
     * 整份讀不懂時則是靜靜用預設值，而壞掉的檔留在原地，下一次存檔就把它蓋掉。
     *
     * <p>現在每一欄讀不懂就只有那一欄用預設；整份讀不懂就改名放旁邊
     * （見 {@link SafeFiles#readObject}）。
     */
    private void load() {
        JsonObject o = SafeFiles.readObject(file, MAX_BYTES);
        if (o == null) {
            return;                            // 沒有檔、或讀不懂（已經移開）：全部用預設值
        }
        Boolean showPanel = boolOrNull(o, "showPanel");
        TooltipMode tooltip = enumOf(o, "tooltipMode", TooltipMode.class);
        if (tooltip != null) {
            tooltipMode = tooltip;
        } else if (showPanel != null) {
            // 舊設定檔：showPanel 為 false 代表不翻 tooltip
            tooltipMode = showPanel ? TooltipMode.PANEL : TooltipMode.OFF;
        }
        shotMode = enumOr(o, "shotMode", ShotMode.class, shotMode);
        // 舊設定檔沒有這個欄位時維持預設——升上來的人畫面不會突然變樣
        DialogueMode dialogue = enumOf(o, "dialogueMode", DialogueMode.class);
        if (dialogue != null) {
            dialogueMode = dialogue;
        }
        DialogueMode choice = enumOf(o, "choiceMode", DialogueMode.class);
        if (choice != null) {
            choiceMode = choice;
        } else if (dialogue != null) {
            // 舊設定檔只有一個開關，選項是跟著內文走的。照那個值帶過來，
            // 升上來的人畫面不會突然變樣。
            choiceMode = dialogue;
        }
        chatCopy = bool(o, "chatCopy", chatCopy);
        translateTitles = bool(o, "translateTitles", translateTitles);
        marketSearch = bool(o, "marketSearch", marketSearch);
        chatMode = enumOr(o, "chatMode", ChatMode.class, chatMode);
        Boolean overlays = boolOrNull(o, "showOverlays");
        if (overlays != null) {
            showOverlays = overlays;
        } else if (showPanel != null) {
            showOverlays = showPanel;
        }
        collect = bool(o, "collect", collect);
        notifiedVersion = str(o, "notifiedVersion", notifiedVersion);
        language = str(o, "language", language).trim();
        fallbackLanguage = str(o, "fallbackLanguage", fallbackLanguage).trim();
        uiLanguage = str(o, "uiLanguage", uiLanguage).trim();
        debugDumps = bool(o, "debugDumps", debugDumps);
        collectGuiText = bool(o, "collectGuiText", collectGuiText);
        source = enumOr(o, "source", Source.class, source);
        translateNametags = bool(o, "translateNametags", translateNametags);
        nametagMode = enumOr(o, "nametagMode", NametagMode.class, nametagMode);
        panelSide = enumOr(o, "panelSide", PanelSide.class, panelSide);
        translateItemNames = bool(o, "translateItemNames", translateItemNames);
        // 數字一律夾回設定畫面允許的範圍。手改或別的版本寫出的極端值會讓小框
        // 跑到畫面外、或一出現就消失，看起來就像翻譯壞了。
        panelGap = clamp(integer(o, "panelGap", panelGap), 0, 200);
        String accent = str(o, "accentColor", accentColor);
        if (accent.matches("#[0-9a-fA-F]{6}")) {
            accentColor = accent;
        }
        int hold = integer(o, "dialogueHoldMs", dialogueHoldMs);
        // 跟 setDialogueHoldSeconds 一致：0 以下是「持續顯示」
        dialogueHoldMs = hold <= 0 ? Integer.MAX_VALUE : hold;
        nametagHoldMs = clamp(integer(o, "nametagHoldMs", nametagHoldMs), 0, 60_000);
        panelAnchor = enumOr(o, "panelAnchor", PanelAnchor.class, panelAnchor);
        readPairs(o, "overlayPos", overlayPos);
        readPairs(o, "overlaySize", overlaySize);
        showBadges = bool(o, "showBadges", showBadges);
        badgeStyle = enumOr(o, "badgeStyle", BadgeStyle.class, badgeStyle);
        nametagRange = clamp(number(o, "nametagRange", nametagRange), 2.0, 64.0);
        nametagAngle = clamp(number(o, "nametagAngle", nametagAngle), 1.0, 45.0);
        fixedX = integer(o, "fixedX", fixedX);
        fixedY = integer(o, "fixedY", fixedY);

        for (String old : RETIRED) {
            if (o.has(old)) {
                System.out.println("[WynnChaYuan] 設定檔帶著舊版的欄位 " + old + "，重寫一次把它清掉");
                save();
                break;
            }
        }
    }

    /** 不存在、是 JSON null、或不是單一值（陣列、物件）都當成沒寫。 */
    private static JsonElement primitive(JsonObject o, String name) {
        JsonElement el = o.get(name);
        return el == null || !el.isJsonPrimitive() ? null : el;
    }

    /** 布林值；手改成 {@code "true"} 字串的也認。其他一律當成沒寫。 */
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

    /**
     * 字串欄位。<b>只收真正的字串</b>。
     *
     * <p>Gson 的 {@code getAsString} 對數字與布林值也會給答案（{@code 5} 變成
     * {@code "5"}）。但這幾欄是語言代碼、版本號、色碼——寫成數字一定是壞的，
     * 拿著假值去查語言檔只會永遠落空，退回預設反而是對的。
     */
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
            return fallback;                   // "wide"、true 之類
        }
    }

    private static int integer(JsonObject o, String name, int fallback) {
        double v = number(o, name, Double.NaN);
        if (Double.isNaN(v)) {
            return fallback;
        }
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, Math.round(v)));
    }

    /**
     * 列舉值；不認得就回傳 {@code null}。
     *
     * <p>「不認得」多半是<b>較新版本</b>加的選項——玩家退回舊版測試時就會遇到。
     * 以前這裡丟例外，連帶後面每一欄都退回預設。
     */
    private static <E extends Enum<E>> E enumOf(JsonObject o, String name, Class<E> type) {
        JsonElement el = primitive(o, name);
        if (el == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, el.getAsString().trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            System.err.println("[WynnChaYuan] 設定 " + name + " 的值 " + el + " 這一版不認得，用預設值");
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

    /** 小框的位置／大小。哪一個框讀不懂，就只有那一個框回到預設。 */
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
            } catch (RuntimeException ignored) {
                // 這一個框用預設位置
            }
        }
    }

    /**
     * 寫設定。
     *
     * <p>先寫暫存檔再換上去：先前是直接開檔覆寫，存到一半遊戲被關掉，下一次啟動
     * 讀到的就是半截的 JSON。同步鎖是因為設定畫面（主執行緒）與更新提示可能同時存。
     */
    private synchronized void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("tooltipMode", tooltipMode.name());
            o.addProperty("showOverlays", showOverlays);
            o.addProperty("collect", collect);
            o.addProperty("notifiedVersion", notifiedVersion);
            o.addProperty("language", language);
            o.addProperty("fallbackLanguage", fallbackLanguage);
            // uiLanguage 與 marketSearch 先前<b>只活在記憶體裡</b>：setUiLanguage 與
            // toggleMarketSearch 都有呼叫 save()，但 save 沒寫這兩欄、load 也沒讀，
            // 於是 F6 選的介面語言與市集搜尋開關每次重開遊戲都回到預設——
            // 玩家看到的是「設定按了沒反應」。
            o.addProperty("uiLanguage", uiLanguage);
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
            o.addProperty("choiceMode", choiceMode.name());
            o.addProperty("chatMode", chatMode.name());
            o.addProperty("translateTitles", translateTitles);
            o.addProperty("chatCopy", chatCopy);
            o.addProperty("marketSearch", marketSearch);
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
            o.addProperty("showBadges", showBadges);
            o.addProperty("badgeStyle", badgeStyle.name());
            o.addProperty("nametagRange", nametagRange);
            o.addProperty("nametagAngle", nametagAngle);
            o.addProperty("fixedX", fixedX);
            o.addProperty("fixedY", fixedY);
            SafeFiles.writeAtomically(file, GSON.toJson(o));
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 設定寫入失敗: " + e.getMessage());
        }
    }
}
