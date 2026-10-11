package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 內容書與 Wynntils 頁面上的<b>內容名稱</b>：任務、洞窟、探索點、地城這些。
 *
 * <h2>為什麼要有（使用者 2026-10-11）</h2>
 * 「Wynntils 頁面的名稱希望也可以像物品名稱一樣可以開啟完全取代、中英對照、關閉」。
 * 物品名稱那個三段式設定只管裝備、素材與材料；內容書卡片的標題是另一種東西：
 *
 * <pre>
 *   Visceral Remains [Cave]   ->   臟腑殘骸 [洞窟]
 * </pre>
 *
 * 語料裡這是<b>一整列的條目</b>，不經過物品名稱那條路——所以不附原文、關掉物品名稱也
 * 還是譯名。想照著名字去查 wiki 或問人的玩家，手上只有一個別人不認得的中文名。
 *
 * <h2>做法</h2>
 * 不動查表。照常翻完之後，在畫出去之前改這一列：
 *
 * <pre>
 *   ON     臟腑殘骸 [洞窟]                       （照舊）
 *   BOTH   臟腑殘骸 [洞窟]  ＋下一列 Visceral Remains
 *   OFF    Visceral Remains [洞窟]
 * </pre>
 *
 * 改的是<b>畫出來的字</b>而不是譯文模板：原文的名字是橘色、類型標籤是灰色，照字元位置
 * 插進去或換掉，兩段顏色原樣留著；改模板的話顏色要重猜一次。
 *
 * <p>清單那一列送來的是裸名（{@code Visceral Remains}，沒有方括號），靠
 * {@link TranslationStore#isPageName} 認——語料裡有「這個名字 [類型]」的條目才算。
 */
public final class PageNames {

    private PageNames() {}

    /**
     * 卡片標題：「名字 [類型]」。
     *
     * <p>類型寫死成清單，不收任意的方括號——{@code + New Quest [Cowfusion]}、
     * {@code Builder's Toolbelt [Region Tool]} 也是「字 [字]」，那些不是內容的名稱。
     */
    private static final Pattern TITLE = Pattern.compile(
            "^(.+) \\[(Quest|Mini-Quest|Cave|Secret Discovery|World Discovery"
                    + "|Territorial Discovery|Boss Altar|Lootrun Camp|Dungeon|Raid|World Event)]$");

    /** 這個鍵是不是卡片標題；是的話回傳名字那一段，不是回傳 {@code null}。 */
    public static String nameOfTitle(String text) {
        if (text == null || text.indexOf('\n') >= 0) {
            return null;
        }
        Matcher m = TITLE.matcher(text);
        return m.matches() ? m.group(1) : null;
    }

    /**
     * 一份說明翻完之後，把標題那一列照設定改好。
     *
     * <h2>三種設定</h2>
     * <pre>
     *   ON     Aelumia 冶金廠 [洞窟]
     *   BOTH   Aelumia 冶金廠 [洞窟]
     *          Aelumia Metalmill            ← 原文自己一列，排在標題下面
     *   OFF    Aelumia Metalmill [洞窟]
     * </pre>
     *
     * <h2>原文為什麼不接在同一列（使用者 2026-10-11 實測第一版）</h2>
     * 第一版是「Aelumia 冶金廠 (Aelumia Metalmill) [洞窟]」。太長，而且名字裡留著英文的
     * 那幾張卡顏色是花的——見 {@link #title}。使用者：「似乎可以讓原文在下面一列」。
     * 物品名稱放不下時也是這樣挪（{@link NameWrap}）。
     *
     * @param original   原文的每一行
     * @param translated 翻好的每一行
     * @return 改好的那一份；不必改時回傳 {@code translated} 本身
     */
    public static List<Component> titles(List<Component> original, List<Component> translated,
                                         CollectorConfig.ItemNames mode) {
        if (original == null || original.isEmpty()
                || translated == null || translated.isEmpty()) {
            return translated;
        }
        Component first = original.get(0);
        String name = first == null ? null : nameOfTitle(first.getString());
        if (name == null) {
            return translated;                 // 不是內容的卡片（物品、技能…），整份不碰
        }
        if (mode == null) {
            mode = CollectorConfig.ItemNames.ON;
        }
        // 標題只會在第一列。譯文的行數可能比原文少（整段條目收短），但第一列對第一列。
        Component changed = title(first, translated.get(0), name, mode);
        boolean below = mode == CollectorConfig.ItemNames.BOTH && changed != null
                && !titleName(changed.getString()).equals(name);
        if (changed == translated.get(0) && !below) {
            return translated;
        }
        List<Component> out = new ArrayList<>(translated);
        out.set(0, changed);
        if (below) {
            // 原文那一段照遊戲原本的樣子（字型、顏色）抄下來，自己一列
            MutableComponent row = Component.empty();
            copy(row, first, 0, name.length());
            out.add(1, row);
        }
        return out;
    }

    /** 「名字 [類型]」的名字那一段；不是這個形狀就整串回傳。 */
    private static String titleName(String shown) {
        int cut = shown.lastIndexOf(" [");
        return cut > 0 && shown.endsWith("]") ? shown.substring(0, cut) : shown;
    }

    /**
     * 一列標題。不是「名字 [類型]」的形狀、或譯名跟原文一樣時，回傳 {@code translated} 本身。
     *
     * <h2>名字整段一個顏色</h2>
     * 原文的名字是一整段橘色。譯名裡留著英文的（「Aelumia 冶金廠」）重建時顏色是照字面
     * 對回去的：「Aelumia」對得到原文那一段、拿到橘色，「冶金廠」對不到、掉成預設的白色。
     * 所以不管哪一種設定，名字那一段都重新塗成原文名字的顏色——這一列的結構是固定的，
     * 用不著猜。
     */
    static Component title(Component original, Component translated, String name,
                           CollectorConfig.ItemNames mode) {
        if (translated == null) {
            return null;
        }
        String shown = translated.getString();
        int cut = shown.lastIndexOf(" [");
        if (cut <= 0 || !shown.endsWith("]")) {
            return translated;                 // 譯文不是「名字 [類型]」的形狀，不猜
        }
        String local = shown.substring(0, cut);
        if (local.equals(name)) {
            return translated;                 // 名字本來就留原文
        }
        MutableComponent out = Component.empty();
        if (mode == CollectorConfig.ItemNames.OFF) {
            // 保留原文：名字那一段<b>整段拿原文的</b>（字型、顏色都是遊戲原本的）
            copy(out, original, 0, name.length());
        } else {
            net.minecraft.network.chat.TextColor colour = nameColour(original, name.length());
            int[] at = {0};
            boolean[] same = {true};
            translated.visit((style, part) -> {
                int end = Math.min(at[0] + part.length(), cut);
                if (end > at[0]) {
                    Style painted = colour == null ? style : style.withColor(colour);
                    same[0] &= painted.equals(style);
                    out.append(Component.literal(part.substring(0, end - at[0])).withStyle(painted));
                }
                at[0] += part.length();
                return Optional.empty();
            }, Style.EMPTY);
            if (same[0]) {
                return translated;             // 顏色本來就對，原樣交回去
            }
        }
        copy(out, translated, cut, shown.length());
        return out;
    }

    /**
     * 原文名字那一段是什麼顏色：<b>佔最多字的那一個</b>。
     *
     * <h2>為什麼不看第一個字元（使用者 2026-10-11 實測第二版）</h2>
     * 「原本的 title 顏色消失了」——任務卡的標題從綠色變成白的。第二版拿原文第一個字元的
     * 顏色去塗，而那張卡的標題開頭有一個看不見的字元，顏色跟名字不一樣；下面那一列原文
     * 是整段照抄的，所以還是綠的，只有標題被塗錯。
     *
     * <p>只數字母與數字（空白、圖示、排版用的字元都不算），哪個顏色的字最多就是名字的顏色。
     *
     * @return 名字的顏色；整段都沒設顏色時回傳 {@code null}（那就不重塗）
     */
    private static net.minecraft.network.chat.TextColor nameColour(Component line, int length) {
        java.util.Map<net.minecraft.network.chat.TextColor, Integer> votes = new java.util.LinkedHashMap<>();
        int[] at = {0};
        line.visit((style, part) -> {
            for (int i = 0; i < part.length() && at[0] + i < length; i++) {
                char c = part.charAt(i);
                if (Character.isLetterOrDigit(c) && !(c >= 0xE000 && c <= 0xF8FF)
                        && style.getColor() != null) {
                    votes.merge(style.getColor(), 1, Integer::sum);
                }
            }
            at[0] += part.length();
            return Optional.empty();
        }, Style.EMPTY);
        net.minecraft.network.chat.TextColor best = null;
        int most = 0;
        for (java.util.Map.Entry<net.minecraft.network.chat.TextColor, Integer> e : votes.entrySet()) {
            if (e.getValue() > most) {
                most = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    /** 把 {@code line} 畫出來的 {@code [from, to)} 接到 {@code out} 後面，樣式原樣。 */
    private static void copy(MutableComponent out, Component line, int from, int to) {
        int[] at = {0};
        line.visit((style, part) -> {
            int end = at[0] + part.length();
            int a = Math.max(at[0], from);
            int b = Math.min(end, to);
            if (a < b) {
                out.append(Component.literal(part.substring(a - at[0], b - at[0])).withStyle(style));
            }
            at[0] = end;
            return Optional.empty();
        }, Style.EMPTY);
    }

    /**
     * 裸名那一列（清單）。
     *
     * @param original   原文那一段
     * @param translated 翻好的那一段；{@code null} 代表沒翻到
     * @return 照設定該畫的那一段；{@code null} 代表「畫原文」
     */
    public static Component bare(Component original, Component translated,
                                 CollectorConfig.ItemNames mode) {
        if (translated == null || mode == null || mode == CollectorConfig.ItemNames.ON) {
            return translated;
        }
        if (mode == CollectorConfig.ItemNames.OFF) {
            return null;
        }
        String name = original.getString();
        String shown = translated.getString();
        if (shown.equals(name) || shown.endsWith("(" + name + ")")) {
            return translated;
        }
        return splice(translated, shown.length(), shown.length(), " (" + name + ")");
    }

    /**
     * 把畫出來的字的 {@code [from, to)} 換成 {@code text}，其餘每一段的樣式原樣留著。
     *
     * <p>換進去的字跟著<b>它前面那個字</b>的樣式（{@code from} 是 0 時跟著被換掉的第一個字）
     * ——名字後面附的原文要跟名字同色，不是跟後面的類型標籤同色。
     */
    static Component splice(Component line, int from, int to, String text) {
        List<Style> styles = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        line.visit((style, part) -> {
            if (!part.isEmpty()) {
                styles.add(style);
                texts.add(part);
            }
            return Optional.empty();
        }, Style.EMPTY);

        // 換進去的字用哪一個樣式：前面那個字的；從頭換的話用被換掉的第一個字的
        int probe = from > 0 ? from - 1 : 0;
        Style inserted = styles.isEmpty() ? Style.EMPTY : styles.get(styles.size() - 1);
        int at = 0;
        for (int i = 0; i < texts.size(); i++) {
            if (probe < at + texts.get(i).length()) {
                inserted = styles.get(i);
                break;
            }
            at += texts.get(i).length();
        }

        MutableComponent out = Component.empty();
        at = 0;
        for (int i = 0; i < texts.size(); i++) {           // from 之前的原樣抄
            String part = texts.get(i);
            int end = at + part.length();
            int headEnd = Math.min(end, from);
            if (headEnd > at) {
                out.append(Component.literal(part.substring(0, headEnd - at))
                        .withStyle(styles.get(i)));
            }
            at = end;
        }
        out.append(Component.literal(text).withStyle(inserted));
        at = 0;
        for (int i = 0; i < texts.size(); i++) {           // to 之後的原樣抄
            String part = texts.get(i);
            int end = at + part.length();
            int tailStart = Math.max(at, to);
            if (tailStart < end) {
                out.append(Component.literal(part.substring(tailStart - at))
                        .withStyle(styles.get(i)));
            }
            at = end;
        }
        return out;
    }
}
