package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lootrun 使命與試煉的名字照 F6「任務與洞窟名稱」的設定顯示。
 *
 * <h2>為什麼要有（使用者 2026-10-14）</h2>
 * 「mission 希望也可以後面 (原文)」。使命名出現在記分板（{@code 獻祭儀式:}）、
 * 使命開始與信標的聊天面板（{@code 󐀵使命開始󐁰獻祭儀式}），語料裡都是一整格的條目，
 * 不經過物品名稱那條路，所以「譯名加原文」從來不會附上去。
 *
 * <h2>做法</h2>
 * 查完表之後改譯文：原文按 {@code {#}} 與換行切成格，哪一格<b>整格</b>是使命名
 * （可以帶結尾冒號），譯文的同一格就照設定改。
 *
 * <pre>
 *   ON     獻祭儀式:                      （照舊）
 *   BOTH   獻祭儀式 (Sacrificial Ritual):
 *   OFF    Sacrificial Ritual:
 * </pre>
 *
 * 只認整格：敘述裡提到使命名的句子不動。兩邊切出來的格數或分隔符對不上就整條不碰
 * ——那是譯文重排過的整段，猜哪一格是名字只會猜錯。
 *
 * <p>名單照 Wynntils 4.2.8 的 {@code MissionType} 與 {@code TrialType}。
 */
public final class LootrunNames {

    private LootrunNames() {}

    static final Set<String> NAMES = Set.of(
            // 使命
            "Cleansing Greed", "Materialism", "Hoarder", "Jester's Trick", "Interest Scheme",
            "Orphion's Grace", "Opal Offering", "Gourmand", "Route Indigo", "Ostinato",
            "Porphyrophobia", "Radiant Hunter", "Equilibrium", "Inner Peace", "Optimism",
            "High Spirits", "Beleza Pura", "Backup Beat", "Requiem", "Stasis", "Chronokinesis",
            "Thrill Seeker", "Knife Edge", "King's Court", "High Roller", "Redemption",
            "Sacrificial Ritual", "Complete Chaos",
            // Wynntils 4.2.8 的名單沒有、實機出現的使命（2026-10-17 的記分板與「使命開始」面板）
            "Cleansing Ritual",
            // 2026-10-19 capture：Wynntils 4.2.8 沒有
            "Balancing Act",
            // 試煉
            "Adrenaline Junkie", "Dying Light", "Monochromokopia", "Chronotrigger", "All In",
            "Gambling Beast", "Hubris", "Lights Out", "Side Hustle", "Treasury Bill",
            "Ultimate Sacrifice", "Warmth Devourer", "Imperitia");

    /** 全部的使命與試煉名（英文原名）。 */
    public static Set<String> names() {
        return NAMES;
    }

    /** 這個字串（去掉前後空白）整個就是一個使命或試煉名嗎。 */
    public static boolean isName(String text) {
        return text != null && NAMES.contains(text.strip());
    }

    private static final String ICON = "{#}";
    private static final char NEWLINE = (char) 10;
    private static final String COLOUR_END = "{/}";

    /**
     * 照設定改好的譯文；用不上時回傳 {@code hit} 本身。
     *
     * @param template 查表用的原文
     * @param hit      查到的譯文
     */
    public static String apply(String template, String hit, CollectorConfig.ItemNames mode) {
        if (template == null || hit == null || mode == null
                || mode == CollectorConfig.ItemNames.ON || !mentionsName(template)) {
            return hit;
        }
        List<String> src = split(template);
        List<String> dst = split(hit);
        if (src.size() != dst.size()) {
            return hit;
        }
        StringBuilder out = new StringBuilder(hit.length() + 24);
        boolean changed = false;
        for (int i = 0; i < src.size(); i++) {
            String s = src.get(i);
            String d = dst.get(i);
            if (i % 2 == 1) {                  // 分隔符：兩邊要一模一樣
                if (!s.equals(d)) {
                    return hit;
                }
                out.append(d);
                continue;
            }
            String name = nameOf(s);
            String fixed = name == null ? null : rewrite(d, name, mode);
            if (fixed != null) {
                out.append(fixed);
                changed = true;
            } else {
                out.append(d);
            }
        }
        return changed ? out.toString() : hit;
    }

    /**
     * 這段文字裡的使命／試煉名（英文原名）落在哪裡。
     *
     * <h2>為什麼需要（實機回報 2026-10-17）</h2>
     * 「譯名加原文」附上去的那一段是刻意留的英文，但畫之前還會對整句掃一次詞表：
     * 記分板成了「小丑的把戲 (Jester's 詭計)」、卡片標題成了「逐險者 (獵殺悸動 Seeker)」——
     * {@code Trick}、{@code Thrill} 剛好是 {@code ability-terms.json} 裡的詞。
     * 物品名稱那條路早就擋了（{@link TranslationStore#appendedOriginalAt}），這條路沒有。
     *
     * <p>不只認括號裡的：設成只顯示原文（OFF）時整格就是英文名，一樣不能被換掉。
     * 名字前後必須不是字母或數字，免得長名字裡的一截被當成另一個名字。
     *
     * @return 每一段 {@code {起, 迄}}；沒有就是空清單
     */
    static List<int[]> spans(String text) {
        List<int[]> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }
        for (String name : NAMES) {
            int at = text.indexOf(name);
            while (at >= 0) {
                int end = at + name.length();
                boolean left = at == 0 || !Character.isLetterOrDigit(text.charAt(at - 1));
                boolean right = end >= text.length() || !Character.isLetterOrDigit(text.charAt(end));
                if (left && right) {
                    out.add(new int[] {at, end});
                }
                at = text.indexOf(name, at + 1);
            }
        }
        return out;
    }

    /** 粗篩：原文裡有沒有任何一個名字。大部分查表都在這裡就回去了。 */
    private static boolean mentionsName(String template) {
        for (String name : NAMES) {
            if (template.contains(name)) {
                return true;
            }
        }
        return false;
    }

    /** 這一格整格是使命名嗎；是的話回傳名字。 */
    private static String nameOf(String cell) {
        String core = cell.strip();
        if (core.endsWith(":")) {
            core = core.substring(0, core.length() - 1).strip();
        }
        return NAMES.contains(core) ? core : null;
    }

    /** 譯文的這一格改成設定要的樣子；不必改時回傳 {@code null}。 */
    private static String rewrite(String cell, String name, CollectorConfig.ItemNames mode) {
        String core = cell.strip();
        if (core.isEmpty() || core.contains("(" + name + ")")) {
            return null;                       // 物品名稱那條路已經附過了
        }
        int lead = cell.indexOf(core);
        String before = cell.substring(0, lead);
        String after = cell.substring(lead + core.length());
        String colon = "";
        if (core.endsWith(":") || core.endsWith("：")) {
            colon = core.substring(core.length() - 1);
            core = core.substring(0, core.length() - 1).stripTrailing();
        }
        // 「{c1}獻祭儀式{/}」：原文接在收尾標記前面，顏色跟著譯名
        String close = "";
        if (core.endsWith(COLOUR_END)) {
            close = COLOUR_END;
            core = core.substring(0, core.length() - COLOUR_END.length());
        }
        if (core.isEmpty() || core.equals(name)) {
            return null;                       // 本來就是原文
        }
        String shown;
        if (mode == CollectorConfig.ItemNames.OFF) {
            // 譯文前面的顏色標記留著，只換字
            int text = 0;
            while (core.startsWith("{c", text)) {
                int end = core.indexOf('}', text);
                if (end < 0) {
                    break;
                }
                text = end + 1;
            }
            shown = core.substring(0, text) + name;
        } else {
            shown = core + " (" + name + ")";
        }
        return before + shown + close + colon + after;
    }

    /**
     * 按 {@code {#}} 與換行切開，分隔符自己一格：偶數位置是字，奇數位置是分隔符。
     */
    static List<String> split(String text) {
        List<String> out = new ArrayList<>();
        int from = 0;
        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) == NEWLINE) {
                out.add(text.substring(from, i));
                out.add(String.valueOf(NEWLINE));
                from = ++i;
            } else if (text.startsWith(ICON, i)) {
                out.add(text.substring(from, i));
                out.add(ICON);
                i += ICON.length();
                from = i;
            } else {
                i++;
            }
        }
        out.add(text.substring(from));
        return out;
    }
}
