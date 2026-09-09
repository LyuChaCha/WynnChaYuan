package com.wynnchayuan.capture;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 「這串字就是本機玩家」的所有寫法。
 *
 * <h2>為什麼不能只看帳號名</h2>
 * Wynncraft 有<b>暱稱</b>，而暱稱跟 Minecraft 帳號名毫無關係——它可以有空格、
 * 可以是數字開頭。對話框送過來的是暱稱：
 *
 * <pre>
 *   Hey, 0 QUEST WYNNCHAYUAN! Are you alright in there?
 * </pre>
 *
 * <p>{@code LineParts} 只認得帳號名，於是 {@code {u}} 沒有命中，接著數字比對
 * 把開頭那個 {@code 0} 抽成 {@code {~}}，模板變成
 *
 * <pre>
 *   Hey, {~} QUEST WYNNCHAYUAN! Are you alright in there?
 * </pre>
 *
 * 而語料的鍵是「{@code Hey, {u}! …}」——永遠對不上。使用者回報「有暱稱的時候
 * 翻譯就失效」講的就是這個，而且它會打到<b>每一句提到玩家名字的台詞</b>。
 *
 * <h2>從哪裡知道暱稱</h2>
 * 三個來源都收，哪個真的出現在字裡就用哪個，不去猜哪一個「應該」是對的：
 *
 * <ul>
 *   <li>帳號名（原本就有的）。</li>
 *   <li>玩家清單與實體的<b>顯示名稱</b>——伺服器把暱稱放在這裡。可能帶著階級
 *       前綴，所以連同「去掉開頭幾個詞」的變體一起收，見 {@link #suffixes}。</li>
 *   <li>角色選單那一行「{@code - Nickname: X}」，由 {@link #learn} 記下來。
 *       這一個是<b>有實據</b>的：capture 裡就躺著這一行。</li>
 * </ul>
 *
 * <p>比對時取<b>最長</b>的那一個：暱稱整個出現時就不會只換到後半截。
 */
public final class SelfNames {

    /** 太短的不收——一兩個字元在句子裡到處都是，換錯比沒換更糟。 */
    private static final int MIN_NAME = 3;

    /** 顯示名稱最多剝掉這麼多個開頭的詞（階級前綴通常只有一個）。 */
    private static final int MAX_PREFIX_WORDS = 3;

    /** 執行時學到的名字（角色選單的「- Nickname:」那一行）。 */
    private static final Set<String> learned = new LinkedHashSet<>();

    private SelfNames() {}

    /**
     * 記下一個「就是我」的名字。
     *
     * @param line 畫面上的一整行；不是「- Nickname: X」的話什麼都不做
     */
    public static void learn(String line) {
        if (line == null) {
            return;
        }
        String tag = "Nickname:";
        int at = line.indexOf(tag);
        if (at < 0) {
            return;
        }
        String name = line.substring(at + tag.length()).strip();
        if (name.length() >= MIN_NAME) {
            learned.add(name);
        }
    }

    /** 目前認得的所有寫法，長的排前面。診斷檔會印這一份。 */
    public static List<String> all() {
        Set<String> out = new LinkedHashSet<>(learned);
        addName(out, accountName());
        for (String shown : displayNames()) {
            out.addAll(suffixes(shown));
        }
        List<String> sorted = new ArrayList<>(out);
        sorted.sort((a, b) -> b.length() - a.length());
        return sorted;
    }

    /**
     * 這段字裡出現的、最長的那個「我」。
     *
     * @return 實際出現的字串；沒有就回傳 {@code null}
     */
    public static String find(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        for (String name : all()) {
            if (text.contains(name)) {
                return name;
            }
        }
        return null;
    }

    /**
     * 顯示名稱可能帶著階級前綴（{@code VIP 0 QUEST WYNNCHAYUAN}），所以連同
     * 去掉開頭幾個詞的變體一起收。
     *
     * <p>只往後剝、不往前剝：名字的<b>結尾</b>是穩定的，前面才會被塞東西。
     * 而且比對時取最長的，所以完整那一個出現時不會挑到被剝過的。
     */
    static List<String> suffixes(String shown) {
        List<String> out = new ArrayList<>();
        if (shown == null) {
            return out;
        }
        String s = shown.strip();
        for (int i = 0; i <= MAX_PREFIX_WORDS && !s.isEmpty(); i++) {
            if (s.length() >= MIN_NAME) {
                out.add(s);
            }
            int space = s.indexOf(' ');
            if (space < 0) {
                break;
            }
            s = s.substring(space + 1).strip();
        }
        return out;
    }

    private static void addName(Set<String> out, String name) {
        if (name != null && name.length() >= MIN_NAME) {
            out.add(name);
        }
    }

    private static String accountName() {
        try {
            Minecraft mc = Minecraft.getInstance();
            String name = mc == null || mc.getUser() == null ? null : mc.getUser().getName();
            return name == null || name.isBlank() ? null : name;
        } catch (Throwable t) {
            return null;                       // 啟動早期還沒有 user
        }
    }

    /** 玩家清單與實體上的顯示名稱——伺服器把暱稱放在這裡。 */
    private static List<String> displayNames() {
        List<String> out = new ArrayList<>();
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null) {
                return out;
            }
            plain(out, mc.player.getDisplayName());
            if (mc.getConnection() != null) {
                net.minecraft.client.multiplayer.PlayerInfo info =
                        mc.getConnection().getPlayerInfo(mc.player.getUUID());
                if (info != null) {
                    plain(out, info.getTabListDisplayName());
                }
            }
        } catch (Throwable t) {
            // API 換了或還沒連上線：這只是其中一個來源，拿不到不影響其他的
        }
        return out;
    }

    private static void plain(List<String> out, Component component) {
        if (component == null) {
            return;
        }
        String text = component.getString().strip();
        if (!text.isEmpty()) {
            out.add(text);
        }
    }
}
