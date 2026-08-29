package com.wynnchayuan.capture;

import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 擋掉夾帶玩家個資的系統訊息。
 *
 * <h2>為什麼需要</h2>
 * 聊天的 {@code INFO} 類型不只有介面字串，也混著一堆<b>執行期才產生、內含玩家資料</b>
 * 的通知。實測收集到的內容包括：
 *
 * <pre>
 *   Green_teaTW's friends (43): Haagen_Dazs69, WD69, GreenTEA6666, ...   ← 完整好友名單
 *   PoorChaCha {#} shouts: 晚安                                           ← 別人打的字
 *   eric18960 has logged into server AS12 as a Knight
 *   [!] Congratulations to I Tonk you Bonk for reaching level 110
 *   你死在了 [-781, 89, -5563]                                            ← 座標
 * </pre>
 *
 * 這些東西有兩個問題：<b>不是可翻譯的內容</b>（玩家名字是變數，不是文案），
 * 而且 {@code captured.json} 是要拿給大家一起翻譯用的，
 * 把別人的名字和自己的好友名單寫進去再分享出去並不妥當。
 *
 * <h2>做法與限制</h2>
 * 用結構性特徵比對，再加上本機玩家名稱。這是<b>啟發式的，不保證滴水不漏</b>——
 * 遊戲隨時可能新增別種通知格式。所以寧可誤擋也不要漏放：
 * 漏掉幾句可翻譯的系統訊息，代價遠小於把別人的個資寫進共享檔案。
 */
public final class PlayerDataFilter {

    /**
     * 一看就知道帶玩家資料的訊息特徵。
     *
     * <p>刻意用「片語」而不是完整比對，因為前後常常還接著別的東西。
     */
    private static final List<String> MARKERS = List.of(
            " shouts:",                 // 玩家喊話
            "has logged into server",   // 上線廣播
            "has left the game",
            " left the game",       // 沒有 has 的版本，一樣夾帶玩家名
            "'s friends (",             // 好友名單標題
            "[Server: ",                // 好友名單條目
            "Congratulations to",       // 別人的成就
            " has joined your party",
            " has invited you",
            " sent you a friend request",
            " has thrown a ",           // 「某某人丟了經驗炸彈」，夾帶玩家名
            " of your XP to ",          // 「將 X% 經驗貢獻給某公會」，夾帶公會名
            " has just logged in",      // 上線通知，夾帶玩家名
            " has just logged out",
            " is now online",
            " is now offline",
            // 領地名牌：「Kandon Ridge / Controlled by Paladins United [Lv. 32]」。
            // 公會名稱跟玩家名稱一樣是別人的資料，不該進共享語料；而且那一行
            // 每塊領地、每次易主都不一樣，收進來也永遠不會有人翻。
            //
            // 先前會漏掉，是因為這條走<b>名牌</b>那條路，而名牌只擋得住
            // 「長得像帳號名」的東西——公會名沒有底線，整條穿了過去。
            // 一次 Lootrun 就收進 83 塊別人的領地。
            "Controlled by "
    );

    /** 座標，例如 {@code [-781, 89, -5563]}。 */
    private static final Pattern COORDS =
            Pattern.compile("\\[-?\\d+,\\s*-?\\d+,\\s*-?\\d+]");

    /**
     * 玩家攤位的名牌，例如 {@code PoorChaCha's Shop}。
     *
     * <p>攤位名牌<b>整段都是玩家內容</b>：名稱是玩家 ID，下面那行是玩家自己
     * 打的招牌字（實際收到的有中文、有梗圖字串）。這種東西既不該進語料，
     * 也不該出現在別人的 captured.json 裡。
     *
     * <p>比對整段而不是只看第一行，因為模組拿到的是<b>整塊名牌</b>——
     * 招牌字跟名稱在同一筆。
     */
    private static final Pattern PLAYER_SHOP =
            Pattern.compile("['’]s Shop(\\n|$)");

    /**
     * 中日韓文字。
     *
     * <h2>為什麼「出現中文」就該擋</h2>
     * Wynncraft 的原文<b>全部是英文</b>。收集到的字串裡出現中文，只有兩種可能，
     * 兩種都不該進語料：
     *
     * <ul>
     *   <li><b>玩家自己打的字</b>——攤位招牌、名牌上的留言。那是別人的內容，
     *       不是遊戲文案。</li>
     *   <li><b>我們自己的譯文繞回來了</b>——就地取代模式下畫面上的 tooltip
     *       已經是中文，再收集一次就會把譯文當成原文記下來。實際收到的
     *       {@code captured.json} 裡就有「- 等級 {~} [...]」這種條目。</li>
     * </ul>
     */
    /**
     * 玩家自製物品的製作者署名，例如整整一行只有 {@code by eric18960}。
     *
     * <p>玩家做出來的裝備會在 lore 最後掛上做的人是誰。那一行永遠是別人的 ID，
     * 翻不了也不該進共享語料——實際收到的 {@code npc.json} 裡就混進了兩筆。
     */
    private static final Pattern CRAFTED_BY =
            Pattern.compile("(?m)^\\s*by \\S+\\s*$");

    /**
     * 經驗共享通知後面掛的那個人是誰，例如：
     *
     * <pre>
     *   [+120 Combat XP]
     *   [eric18960]
     * </pre>
     *
     * <p>只擋「後面緊跟著一行括號名字」的情況——單獨的經驗提示是漂浮字，
     * 那個要翻譯，不能一起擋掉。
     */
    private static final Pattern XP_SHARE_TARGET =
            Pattern.compile("Combat XP]\\s*\\n\\s*\\[");

    private static final Pattern CJK = Pattern.compile(
            "[\\u3040-\\u30ff\\u3400-\\u4dbf\\u4e00-\\u9fff\\uf900-\\ufaff]");

    private PlayerDataFilter() {}

    /** 這段訊息是否夾帶玩家資料、不該被記錄。 */
    public static boolean carriesPlayerData(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String marker : MARKERS) {
            if (text.contains(marker)) {
                return true;
            }
        }
        if (COORDS.matcher(text).find() || PLAYER_SHOP.matcher(text).find()
                || CRAFTED_BY.matcher(text).find()
                || XP_SHARE_TARGET.matcher(text).find()
                || CJK.matcher(text).find()) {
            return true;
        }
        String self = localPlayerName();
        return self != null && !self.isBlank() && text.contains(self);
    }

    /**
     * 本機玩家名稱。取不到就回傳 null——這只是額外的一層防護，
     * 拿不到不影響上面的特徵比對。
     */
    private static String localPlayerName() {
        try {
            Minecraft mc = Minecraft.getInstance();
            return mc == null || mc.getUser() == null ? null : mc.getUser().getName();
        } catch (Throwable t) {
            return null;   // 初始化階段可能還沒有 user，不值得為此中斷收集
        }
    }
}
