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
            " is now offline"
    );

    /** 座標，例如 {@code [-781, 89, -5563]}。 */
    private static final Pattern COORDS =
            Pattern.compile("\\[-?\\d+,\\s*-?\\d+,\\s*-?\\d+]");

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
        if (COORDS.matcher(text).find()) {
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
