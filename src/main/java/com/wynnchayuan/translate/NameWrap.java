package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * 「譯名 + 原文」太長時，把原文挪到<b>下一行</b>。
 *
 * <h2>凸出去的是什麼</h2>
 * Wynncraft 2.1 的物品 tooltip 外框不是遊戲畫的方塊，是<b>字型畫的</b>：名稱那一行
 * 開頭那幾個私有區字元（{@code 󏿰󏿏󐀅}）一邊位移一邊蓋上外框與徽記的圖，
 * 而那些圖的大小是伺服器照<b>英文</b>的寬度挑好的。
 *
 * <p>所以名稱一長就凸出去——遊戲的黑底會跟著變寬，字型畫的那個框不會。實機回報：
 *
 * <pre>
 *   烬咒牧杖 (Cindercurse Crosier) [51.0%]
 *   └────────── 框在這裡就結束了 ──────────┘  後面半截在框外
 * </pre>
 *
 * <h2>怎麼判斷「太長」</h2>
 * 拿<b>同一行的原文</b>量。那一行的寬度正是伺服器畫框時用的寬度，超過就是凸出去，
 * 不必猜框有多寬。中文比英文短，所以真正會超過的就是附在後面的那段原文。
 *
 * <p>拆完第一行變成「{@code 烬咒牧杖 [51.0%]}」，比英文還窄，一定收得進框裡；
 * 原文自己一行落在框下面，看得到也複製得到。耐久度留在第一行——它跟名稱是一組，
 * 而且框上本來就替它留了位置。
 *
 * <h2>為什麼不在 {@link TooltipWiden} 裡做</h2>
 * 撐寬那一步的前提是「譯文與原文一行對一行」（見 {@link TooltipWiden#fit}），
 * 而這裡會多生一行。所以排在撐寬<b>之後</b>，整份 tooltip 的最後一步。
 *
 * <h2>看不見的第 0 行不會被動到</h2>
 * 物品名稱其實是兩行，第 0 行寬度是 0（見 {@code TooltipPanel}）。量出來是 0，
 * 「有沒有超過」永遠不成立，所以那一行照舊。
 */
public final class NameWrap {

    private NameWrap() {}

    /**
     * 名稱那一行超過原文寬度時，把「{@code  (原文)}」拆成下一行。
     *
     * @param original   遊戲送來的原文，逐行
     * @param translated 翻好的每一行
     * @param store      查「括號裡是不是我們附上去的原文」用的
     * @param width      量寬度；正式路徑是 {@code mc.font.width}，測試注入假字型
     * @return 不需要拆時就是 {@code translated} 本身；否則是多一行的新清單
     */
    public static List<Component> split(List<Component> original, List<Component> translated,
                                        TranslationStore store, ToIntFunction<Component> width) {
        if (original == null || translated == null || store == null
                || !store.namesWithOriginal()) {
            return translated;
        }
        int n = Math.min(original.size(), translated.size());
        for (int i = 0; i < n; i++) {
            Component line = translated.get(i);
            int budget = width.applyAsInt(original.get(i));
            // headless（測試、沒有字型）量出來是 0，跟第 0 行一樣不動
            if (budget <= 0 || width.applyAsInt(line) <= budget) {
                continue;
            }
            List<Piece> pieces = pieces(line);
            int[] span = store.appendedOriginalSpan(text(pieces));
            if (span == null) {
                continue;
            }
            List<Component> out = new ArrayList<>(translated);
            // 開頭那一個空白屬於「接在名字後面」，跟著括號一起走
            out.set(i, join(slice(pieces, 0, span[0]),
                            slice(pieces, span[1], text(pieces).length())));
            out.add(i + 1, indented(indent(pieces, width),
                                    slice(pieces, span[0] + 1, span[1])));
            return out;
        }
        return translated;
    }

    /** 一段字與它的樣式。整行拆成這樣才切得開。 */
    private record Piece(String text, Style style) {}

    private static List<Piece> pieces(Component line) {
        List<Piece> out = new ArrayList<>();
        line.visit((style, text) -> {
            if (!text.isEmpty()) {
                out.add(new Piece(text, style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static String text(List<Piece> pieces) {
        StringBuilder sb = new StringBuilder();
        for (Piece p : pieces) {
            sb.append(p.text());
        }
        return sb.toString();
    }

    /** 取 {@code [from, to)} 這一段，每一小段各自留著自己的樣式。 */
    private static List<Piece> slice(List<Piece> pieces, int from, int to) {
        List<Piece> out = new ArrayList<>();
        int at = 0;
        for (Piece p : pieces) {
            int end = at + p.text().length();
            int a = Math.max(from, at);
            int b = Math.min(to, end);
            if (a < b) {
                out.add(new Piece(p.text().substring(a - at, b - at), p.style()));
            }
            at = end;
        }
        return out;
    }

    /**
     * 名字前面那一截<b>圖</b>有多寬。
     *
     * <h2>為什麼第二行需要它</h2>
     * 名稱那一行開頭是位移字元與徽記的圖（{@code 󏿰󏿏󐀅}），名字是從那之後
     * 才開始畫的。拆出來的第二行沒有那一截，於是貼著面板的左緣起頭，
     * 比名字凸出去一大塊——使用者回報的「名稱跑掉」：
     *
     * <pre>
     *       橡木法杖          ← 名字從徽記後面開始
     *   (Oak Wood Wand)      ← 第二行卻從最左邊開始
     * </pre>
     *
     * <p>所以量出那一截的寬度，第二行用同樣的寬度墊開，兩行就對齊了。
     * 墊的是 {@code minecraft:space} 字型的位移字元，不是空格——空格的寬度
     * 是固定的 4px，湊不出任意寬度。
     */
    private static int indent(List<Piece> pieces, ToIntFunction<Component> width) {
        List<Piece> lead = new ArrayList<>();
        for (Piece p : pieces) {
            if (p.text().codePoints().anyMatch(
                    cp -> !GlyphSplitter.isGlyphCodePoint(cp)
                            && !Character.isWhitespace(cp))) {
                break;                         // 開始有字了
            }
            lead.add(p);
        }
        return lead.isEmpty() ? 0 : width.applyAsInt(join(lead));
    }

    /** 前面墊 {@code px} 寬，讓這一行對齊上一行的名字。見 {@link #indent}。 */
    private static Component indented(int px, List<Piece> body) {
        Component line = join(body);
        if (px <= 0) {
            return line;
        }
        String offset = SpaceOffset.encode(px);
        return offset.isEmpty() ? line
                : Component.empty()
                        .append(Component.literal(offset)
                                .setStyle(SpaceOffset.styleFor(Style.EMPTY)))
                        .append(line);
    }

    @SafeVarargs
    private static Component join(List<Piece>... parts) {
        MutableComponent out = Component.empty();
        for (List<Piece> part : parts) {
            for (Piece p : part) {
                out.append(Component.literal(p.text()).setStyle(p.style()));
            }
        }
        return out;
    }
}
