package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 帶樣式的一句話：併掉換行、切成指定的列數，樣式跟著字走。
 *
 * <h2>誰在用</h2>
 * 原文是<b>伺服器折好行</b>才送來的那幾個地方——記分板的目標敘述、Lootrun 信標
 * 面板的兩欄。那裡一列只有二十個字上下，一句話被拆成三四列，一列一列翻只翻得出
 * 半句。做法都是「併成整句翻，再照原本的列數切回去」（列數不能變：畫面是照原本
 * 的列一對一換的）。切回去這一步就在這裡。
 *
 * <p>這個類別只算字，不碰畫面，也不問字型——寬度用「中日韓的字兩格、其他一格」
 * 估。要的是每一列差不多長，不是像素級的準。
 */
public final class TextSplit {

    private TextSplit() {}

    /** 一個字與它的樣式。 */
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

    /** 好幾行接成一句（同 {@link #oneLine} 的接法）。 */
    public static Component join(List<Component> lines) {
        MutableComponent all = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                all.append(Component.literal("\n"));
            }
            all.append(lines.get(i));
        }
        return oneLine(all);
    }

    /**
     * 把一句切成剛好 {@code rows} 列，每一列盡量一樣寬。
     *
     * <p>只在看起來可以斷的地方斷：中日韓的字之間、空白。英文單字與數字不拆，
     * 句號逗號不放在一列的開頭。字不夠分的時候後面幾列是空的——列數不能少。
     */
    public static List<Component> split(Component whole, int rows) {
        List<Glyph> all = glyphs(whole);
        // 先切成「不能拆開」的小塊
        List<List<Glyph>> atoms = new ArrayList<>();
        List<Glyph> current = new ArrayList<>();
        for (Glyph g : all) {
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
    public static boolean wide(int cp) {
        return (cp >= 0x2E80 && cp <= 0x9FFF)      // 部首、假名、注音、漢字
                || (cp >= 0xAC00 && cp <= 0xD7AF)  // 韓文音節
                || (cp >= 0xF900 && cp <= 0xFAFF)  // 相容漢字
                || (cp >= 0xFF00 && cp <= 0xFF60)  // 全形英數與標點
                || (cp >= 0xFFE0 && cp <= 0xFFE6)
                || (cp >= 0x20000 && cp <= 0x3FFFF);
    }
}
