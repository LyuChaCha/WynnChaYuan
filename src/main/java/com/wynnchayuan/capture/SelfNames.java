package com.wynnchayuan.capture;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 「這串字就是本機玩家」的所有寫法。
 *
 * <h2>為什麼不能照原樣比對帳號名</h2>
 * 使用者回報「有暱稱的時候翻譯就失效」，而它打到的是<b>每一句提到玩家名字的
 * 台詞</b>。實機診斷檔挖出來的原因有兩個，兩個都不是「找不到名字」：
 *
 * <ol>
 *   <li><b>伺服器把名字轉成大寫</b>。帳號名 {@code Wynnchayuan} 到了對話框是
 *       {@code Hey, WYNNCHAYUAN!}、到了角色選單是 {@code 0 QUEST WYNNCHAYUAN}。
 *       照原樣 {@code contains} 一律落空，模板留著英文名字，語料的
 *       「{@code Hey, {u}! …}」永遠對不上。見 {@link #find}。</li>
 *   <li><b>學到了假名字</b>。角色建立畫面有一行
 *       {@code - Nickname: Not Defined}，照收就等於把「Not Defined」當成玩家的
 *       名字。見 {@link #UNSET}。</li>
 * </ol>
 *
 * <p>Wynncraft 的<b>暱稱</b>另外還跟 Minecraft 帳號名毫無關係——它可以有空格、
 * 可以是數字開頭，所以名字不能只從帳號名來。
 *
 * <h2>從哪裡知道暱稱</h2>
 * 都收，哪個真的出現在字裡就用哪個，不去猜哪一個「應該」是對的：
 *
 * <ul>
 *   <li><b>語料自己</b>。鍵是「{@code Hey, {u}! Are you alright in there?…}」，
 *       畫面上是「{@code Hey, WYNNCHAYUAN! Are you…}」，前後都對得上，中間夾的
 *       就是名字。見
 *       {@link com.wynnchayuan.translate.TranslationStore#playerNameIn}。
 *       這一個最可靠：玩家一講話就認得出來，不必先去開哪個介面。</li>
 *   <li>角色選單那一行「{@code - Nickname: X}」，由 {@link #learn} 記下來。</li>
 *   <li>帳號名。<b>不等於</b>暱稱——實機診斷檔裡帳號名是 {@code Green_teaTW}，
 *       畫面上的名字卻是 {@code WYNNCHAYUAN}。</li>
 *   <li>玩家清單與實體的顯示名稱。Wynncraft 這兩個地方拿不到暱稱，留著只是
 *       因為其他伺服器可能有。可能帶著階級前綴，所以連同「去掉開頭幾個詞」的
 *       變體一起收，見 {@link #suffixes}。</li>
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

    /**
     * 「還沒設定」的那幾種寫法。
     *
     * <p>角色<b>建立</b>畫面上就有一行「{@code - Nickname: Not Defined}」——照收
     * 的話「Not Defined」會被當成玩家的名字，之後每一句出現這兩個字的英文都會被
     * 抽成 {@code {u}}。實機診斷檔裡真的收到了這一筆。
     */
    private static final Set<String> UNSET =
            Set.of("not defined", "none", "n/a", "unset", "not set", "-");

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
        remember(line.substring(at + tag.length()).strip());
    }

    /** 一個猜出來的名字要有幾條<b>不同的鍵</b>撐腰才算數。見 {@link #propose}。 */
    public static final int MIN_EVIDENCE = 2;

    /** 猜出來的名字 → 夾出它的那些鍵。 */
    private static final Map<String, Set<String>> proposals = new LinkedHashMap<>();

    /**
     * 提出一個<b>用語料猜出來的</b>名字。
     *
     * <h2>為什麼不能一票定案</h2>
     * 語料裡有兩百多條以 {@code {u}} 開頭的鍵（「{@code {u}, you take the lead.}」），
     * 於是畫面上任何一句「{@code 某某, you take the …}」都會被夾出開頭那個詞。
     * 實機掃過整份語料，這樣的誤認有 29 種——{@code Tasim}（NPC 名字）、
     * {@code Alright}、{@code Anyway}、{@code Well}⋯⋯ 認錯一個，往後每一句提到
     * 它的台詞都會被抽成 {@code {u}}，整句翻不出來。
     *
     * <p>可是誤認有個共通點：<b>只有一條鍵撐腰</b>。NPC 名字要湊到兩條不同的
     * {@code {u}} 鍵幾乎不可能，玩家的名字則是每一句叫到他的台詞都會夾出來。
     * 所以要兩條不同的鍵說同一個答案才收。
     *
     * @param name   夾出來的名字，可以是 {@code null}
     * @param source 夾出它的那一條鍵，用來數「幾條不同的鍵」
     */
    public static void propose(String name, String source) {
        if (name == null || source == null) {
            return;
        }
        Set<String> keys = proposals.computeIfAbsent(name, n -> new LinkedHashSet<>());
        keys.add(source);
        if (keys.size() >= MIN_EVIDENCE) {
            remember(name);
        }
    }

    /**
     * 直接記下一個名字。
     *
     * <p>給{@link com.wynnchayuan.translate.TranslationStore#playerNameIn 語料反推}
     * 用的入口——那一邊已經確認過是名字，這裡只擋長度與「還沒設定」。
     *
     * @param name 可以是 {@code null}，那樣什麼都不做
     */
    public static void remember(String name) {
        if (name == null) {
            return;
        }
        String bare = name.strip();
        if (bare.length() >= MIN_NAME && !UNSET.contains(bare.toLowerCase(Locale.ROOT))) {
            learned.add(bare);
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
     * <h2>為什麼不分大小寫</h2>
     * Wynncraft 的對話框與角色選單把玩家名字<b>整個轉成大寫</b>再送出來：帳號名
     * {@code Wynnchayuan} 到了台詞裡是
     *
     * <pre>
     *   Hey, WYNNCHAYUAN! Are you alright in there?
     * </pre>
     *
     * <p>照原樣比對就永遠差那一步——實機診斷檔裡側邊面板抽得出 {@code {u}}、
     * 對話框抽不出，差別只在大小寫。改成不分大小寫之後，兩邊算出來的模板才會
     * 跟語料的「{@code Hey, {u}! …}」對得上。
     *
     * <p>比對到的位置<b>前後不能再接英數</b>：不分大小寫會讓短名字（三個字母的
     * ID）掉進一般英文單字裡，例如 {@code Ash} 命中 {@code flash}。
     *
     * @return <b>照原文大小寫</b>的那一段（呼叫端還要拿它回頭切字串）；
     *         沒有就回傳 {@code null}
     */
    public static String find(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        for (String name : all()) {
            int at = indexOfWord(text, name);
            if (at >= 0) {
                return text.substring(at, at + name.length());
            }
        }
        return null;
    }

    /** 不分大小寫、且前後不接英數的位置；找不到回傳 {@code -1}。 */
    private static int indexOfWord(String text, String name) {
        int last = text.length() - name.length();
        for (int at = 0; at <= last; at++) {
            if (!text.regionMatches(true, at, name, 0, name.length())) {
                continue;
            }
            if (at > 0 && Character.isLetterOrDigit(text.charAt(at - 1))) {
                continue;
            }
            int after = at + name.length();
            if (after < text.length() && Character.isLetterOrDigit(text.charAt(after))) {
                continue;
            }
            return at;
        }
        return -1;
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
