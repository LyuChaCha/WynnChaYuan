package com.wynnchayuan.render;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 記分板上<b>被折成好幾列的一句話</b>，併起來當一句翻。
 *
 * <h2>為什麼要有</h2>
 * 記分板一列只有二十個字上下，伺服器把目標敘述自己折好再送：
 *
 * <pre>
 *   Slay the Knightmare
 *   by destroying its
 *   shields atop the
 *   statue.
 * </pre>
 *
 * 一列一列查，查到的是四個半句——「the」這種列根本沒得翻，而中文的語序跟英文
 * 不一樣，半句照原順序排出來也不成句。玩家送來的 capture 裡這種半句一次就是
 * 六十幾條（2026-10-08）。
 *
 * <p>所以先試「連續幾列併成一句」查不查得到：查得到就整句翻，再照原本的列數
 * 切回去（就地取代那條路一次只畫一列，列數不能變）；查不到才退回一列一列查，
 * 跟原本一樣。
 *
 * <h2>順便擋掉的一個方框</h2>
 * 一列一列查的時候，「Hold the platform」會對到漂浮字那一條
 * 「Hold\nthe platform」（查表不分換行與空白），拿回來的譯文帶著換行。
 * 記分板一列只畫一行，換行字元就被畫成一個方框——使用者 2026-10-08 回報的
 * 「守住□這座平台」。原文沒有換行的地方，譯文裡的換行一律併掉，見 {@link #oneLine}。
 *
 * <p>這個類別不碰遊戲的畫面，只算；設定與面板由呼叫的那一邊決定。
 */
public final class ScoreboardFlow {

    private ScoreboardFlow() {}

    /** 一句話最多折成幾列。再長的敘述記分板放不下，試下去只是白查。 */
    static final int MAX_RUN = 6;

    /**
     * 一段內容排好的結果。
     *
     * @param panel 給我們自己那個面板的：一句一行，面板會自己折
     * @param rows  給就地取代的：原本那一列的字（去掉樣式）→ 換上去的那一列
     * @param any   有沒有任何一列真的翻出來
     */
    public record Plan(List<Component> panel, Map<String, StyledText> rows, boolean any) {}

    /**
     * 排一段內容。
     *
     * @param content 那一段標題底下的每一列，照畫面上的順序
     */
    public static Plan plan(List<StyledText> content, TranslationStore store) {
        List<StyledText> live = new ArrayList<>();
        if (content != null) {
            for (StyledText row : content) {
                if (row != null && !GlyphSplitter.isGlyphOnly(row)
                        && !GlyphSplitter.toTemplate(row).isBlank()) {
                    live.add(row);
                }
            }
        }
        List<Component> panel = new ArrayList<>();
        Map<String, StyledText> rows = new LinkedHashMap<>();
        boolean any = false;
        int i = 0;
        while (i < live.size()) {
            int end = -1;
            Component whole = null;
            // 從最長的開始試：四列是一句的時候，前兩列湊巧也是一句的話要整句優先
            for (int j = Math.min(live.size(), i + MAX_RUN) - 1; j > i; j--) {
                whole = sentence(live.subList(i, j + 1), store);
                if (whole != null) {
                    end = j;
                    break;
                }
            }
            if (whole != null) {
                panel.add(whole);
                List<Component> parts = split(whole, end - i + 1);
                for (int k = 0; k <= end - i; k++) {
                    rows.put(key(live.get(i + k)), styled(parts.get(k)));
                }
                any = true;
                i = end + 1;
                continue;
            }
            StyledText row = live.get(i);
            Component one = LineTranslator.translate(row, store);
            if (one != null) {
                one = oneLine(one);
                rows.put(key(row), styled(one));
                any = true;
            }
            panel.add(one != null ? one : LineTranslator.untranslated(row));
            i++;
        }
        return new Plan(panel, rows, any);
    }

    /** 那一列在對照表裡的鍵：畫面上的字，去掉樣式。 */
    public static String key(StyledText row) {
        return row == null ? "" : row.getStringWithoutFormatting();
    }

    private static StyledText styled(Component part) {
        return part == null || part.getString().isEmpty()
                ? StyledText.EMPTY : StyledText.fromComponent(part);
    }

    /**
     * 這幾列併成一句查不查得到；查得到就回整句的譯文。
     *
     * <p>先問語料「有沒有這一整句」才翻：{@link LineTranslator#translate} 查不到整句
     * 時會退去換句子裡的詞，那樣隨便哪幾列併起來都「翻得出一點東西」，分不出
     * 哪幾列真的是一句。
     */
    static Component sentence(List<StyledText> run, TranslationStore store) {
        StyledText joined = StyledText.join(" ", run);
        String template = GlyphSplitter.toTemplate(joined);
        if (template.isBlank() || store.lookup(template) == null) {
            return null;
        }
        Component hit = LineTranslator.translate(joined, store);
        return hit == null ? null : oneLine(hit);
    }

    // ------------------------------------------------------------ 帶樣式的字

    /** 一個字與它的樣式。整個類別都是在這個清單上算的。 */
    private record Glyph(int cp, Style style) {}

    private static List<Glyph> glyphs(Component text) {
        List<Glyph> out = new ArrayList<>();
        text.visit((style, piece) -> {
            piece.codePoints().forEach(cp -> out.add(new Glyph(cp, style)));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static Component build(List<Glyph> glyphs) {
        MutableComponent out = Component.empty();
        StringBuilder run = new StringBuilder();
        Style style = null;
        for (Glyph g : glyphs) {
            if (style != null && !style.equals(g.style())) {
                out.append(Component.literal(run.toString()).withStyle(style));
                run.setLength(0);
            }
            style = g.style();
            run.appendCodePoint(g.cp());
        }
        if (run.length() > 0) {
            out.append(Component.literal(run.toString()).withStyle(style));
        }
        return out;
    }

    /**
     * 譯文裡的換行併掉：只畫一行的地方用。
     *
     * <p>換行的兩邊只要有一邊是中日韓的字就直接接起來（「守住」「這座平台」），
     * 兩邊都是拉丁字才補一個空白（俄文、西班牙文）。沒有換行的原樣回傳同一個物件。
     */
    public static Component oneLine(Component text) {
        if (text == null || text.getString().indexOf('\n') < 0) {
            return text;
        }
        List<Glyph> in = glyphs(text);
        List<Glyph> out = new ArrayList<>(in.size());
        for (int i = 0; i < in.size(); i++) {
            Glyph g = in.get(i);
            if (g.cp() != '\n') {
                out.add(g);
                continue;
            }
            int next = i + 1;
            while (next < in.size() && Character.isWhitespace(in.get(next).cp())) {
                next++;
            }
            while (!out.isEmpty() && out.get(out.size() - 1).cp() == ' ') {
                out.remove(out.size() - 1);
            }
            boolean glue = out.isEmpty() || next >= in.size()
                    || wide(out.get(out.size() - 1).cp()) || wide(in.get(next).cp());
            if (!glue) {
                out.add(new Glyph(' ', g.style()));
            }
            i = next - 1;
        }
        return build(out);
    }

    /**
     * 把一句切成剛好 {@code rows} 列，每一列盡量一樣寬。
     *
     * <p>只在看起來可以斷的地方斷：中日韓的字之間、空白。英文單字與數字不拆，
     * 句號逗號不放在一列的開頭。字不夠分的時候後面幾列是空的——列數不能少，
     * 就地取代那一邊是照原本的列一對一換的。
     */
    static List<Component> split(Component whole, int rows) {
        List<Glyph> all = glyphs(whole);
        // 先切成「不能拆開」的小塊
        List<List<Glyph>> atoms = new ArrayList<>();
        List<Glyph> current = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            Glyph g = all.get(i);
            if (!current.isEmpty() && breakable(current.get(current.size() - 1).cp(), g.cp())) {
                atoms.add(current);
                current = new ArrayList<>();
            }
            current.add(g);
        }
        if (!current.isEmpty()) {
            atoms.add(current);
        }
        int left = 0;
        for (List<Glyph> atom : atoms) {
            left += width(atom);
        }
        List<Component> out = new ArrayList<>(rows);
        int at = 0;
        for (int row = 0; row < rows; row++) {
            List<Glyph> line = new ArrayList<>();
            if (row == rows - 1) {
                while (at < atoms.size()) {
                    line.addAll(atoms.get(at++));
                }
            } else {
                int target = (int) Math.ceil(left / (double) (rows - row));
                int used = 0;
                while (at < atoms.size()) {
                    int w = width(atoms.get(at));
                    if (used > 0 && used + w > target) {
                        break;
                    }
                    line.addAll(atoms.get(at++));
                    used += w;
                }
                left -= used;
            }
            out.add(build(trim(line)));
        }
        return out;
    }

    private static List<Glyph> trim(List<Glyph> line) {
        int from = 0;
        int to = line.size();
        while (from < to && line.get(from).cp() == ' ') {
            from++;
        }
        while (to > from && line.get(to - 1).cp() == ' ') {
            to--;
        }
        return line.subList(from, to);
    }

    private static int width(List<Glyph> atom) {
        int w = 0;
        for (Glyph g : atom) {
            w += wide(g.cp()) ? 2 : 1;
        }
        return w;
    }

    /** {@code a} 與 {@code b} 之間可不可以換列。 */
    private static boolean breakable(int a, int b) {
        if (CLOSERS.indexOf(b) >= 0 || OPENERS.indexOf(a) >= 0) {
            return false;                      // 標點不落單在列首，開括號不落單在列尾
        }
        if (a == ' ' || b == ' ') {
            return true;
        }
        return wide(a) || wide(b);
    }

    private static final String CLOSERS = "，。！？、；：）」』】〉》］｝,.!?;:)]}%％…‥ー";
    private static final String OPENERS = "（「『【〈《［｛([{";

    /** 中日韓的字與全形符號：一個字佔兩格，而且前後都可以斷。 */
    static boolean wide(int cp) {
        return (cp >= 0x2E80 && cp <= 0x9FFF)      // 部首、假名、注音、漢字
                || (cp >= 0xAC00 && cp <= 0xD7AF)  // 韓文音節
                || (cp >= 0xF900 && cp <= 0xFAFF)  // 相容漢字
                || (cp >= 0xFF00 && cp <= 0xFF60)  // 全形英數與標點
                || (cp >= 0xFFE0 && cp <= 0xFFE6)
                || (cp >= 0x20000 && cp <= 0x3FFFF);
    }
}
