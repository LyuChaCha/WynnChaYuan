package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * 譯文比英文<b>寬</b>時，整份 tooltip 一起撐開。
 *
 * <h2>為什麼逐行補償不夠</h2>
 * {@link LineTranslator} 的對齊是一行一行算的，前提是「譯文比原文短」——
 * 中文、日文都是，所以短掉的部分補回間隔，右緣就落回原位。
 *
 * <p>俄文<b>比英文長</b>。逐行那一套遇到放不下的行，只能把間隔收到下限
 * （{@link LineTranslator#MIN_GAP}），於是實機 line-debug 記到的是：
 *
 * <pre>
 *   Health Regen          69px  間隔 34 -&gt; 6   整行 165 -&gt; 196（+31）
 *   Haul Cost             51px  間隔 66 -&gt; 6   整行 179 -&gt; 196（+17）
 *   Mana Regen            58px  間隔 65 -&gt; 29  整行 207 -&gt; 207（守住）
 * </pre>
 *
 * 放得下的行守住舊的右緣，放不下的各自凸出不同的量——數值那一欄就參差不齊。
 * 置中的行也一樣：未鑑定物品的「an Item Identifier can unlock」寬了 71px，
 * 縮排卻沒動，整行跑出 Wynncraft 照英文寬度畫好的外框。
 *
 * <p>這兩件事都是<b>一行看不出來</b>的：新的右緣要看整份 tooltip 最寬的那行
 * 才知道在哪。所以等每一行都翻完之後，這裡再整份看一次。
 *
 * <h2>做什麼</h2>
 * <ol>
 *   <li>W = 原文的內容寬度（英文最寬那行）；W' = W 與「需要撐開的譯文行」
 *       （留足最小間隔之後）兩者取大。</li>
 *   <li>原本靠右對齊到共同右緣的欄位行，最後一個間隔一律放寬到剛好收在 W'。</li>
 *   <li>置中的行照 W' 重新置中：縮排 = (W' − 內容寬) / 2，不准是負的。</li>
 * </ol>
 *
 * <h2>什麼時候什麼都不做</h2>
 * 每一行都放得進 W（中文、日文就是這樣）時<b>原封不動回傳</b>，連物件都是同一個。
 * 這一步是給「寬的語言」的，不能讓短的語言多出任何一個像素的差別。
 */
public final class TooltipWiden {

    /**
     * 原文右緣差幾像素以內算「同一個右緣」。
     *
     * <p>Wynncraft 算欄位時是照它自己的字寬取整的，而數值不一定跟標籤同一個字型，
     * 量出來偶爾差一兩像素。放寬到 2 仍然遠小於「沒有靠右」的行差出去的量。
     */
    private static final int EDGE_TOLERANCE = 2;

    private TooltipWiden() {}

    /**
     * 整份撐寬。
     *
     * @param original    遊戲送來的原文，逐行
     * @param translated  翻好的每一行，與原文 1:1；沒翻的行是原文的複本
     * @param centered    每一行是不是置中的（{@link BlockLayout#centered}）
     * @param leftAligned 第二欄是不是靠左排的（{@link LineTranslator#columnsAreLeftAligned}）
     * @param width       量寬度；正式路徑是 {@code mc.font.width}，測試注入假字型
     * @return 不需要撐開時就是 {@code translated} 本身；否則是新的清單，
     *         只有真的動到的行換成新的元件
     */
    public static List<Component> fit(List<Component> original, List<Component> translated,
                                      boolean[] centered, boolean leftAligned,
                                      ToIntFunction<Component> width) {
        if (original == null || translated == null || centered == null) {
            return translated;
        }
        int n = translated.size();
        // 行數對不上就不知道哪一行對哪一行，寧可不動。
        if (original.size() != n || centered.length != n) {
            return translated;
        }
        int[] origW = new int[n];
        int[] madeW = new int[n];
        int frame = 0;
        for (int i = 0; i < n; i++) {
            origW[i] = width.applyAsInt(original.get(i));
            madeW[i] = width.applyAsInt(translated.get(i));
            frame = Math.max(frame, origW[i]);
        }
        // 量不到寬度（沒有字型，例如 headless 測試）就什麼都判斷不了。
        if (frame <= 0) {
            return translated;
        }

        List<List<Seg>> rows = new ArrayList<>(n);
        for (Component line : translated) {
            rows.add(segments(line, width));
        }

        // ---- 靠右的欄位行 ----
        //
        // 右緣是<b>原文</b>量出來的：英文每一行數值的結尾都落在同一個 x。
        // 先收集「原文有欄位間隔、而且譯文也還留著那個間隔」的行，
        // 再只留下原文右緣落在最右邊那一條線上的——沒有頂到那條線的行
        // 本來就不是靠右對齊到共同右緣的，動它只會把它推歪。
        int[][] gap = new int[n][];
        int edge = 0;
        for (int i = 0; i < n; i++) {
            if (leftAligned || centered[i]) {
                continue;                      // 靠左的第二欄、置中的行都不是這一種
            }
            int[] was = lastColumnGap(segments(original.get(i), width));
            if (was == null || was[2] < LineTranslator.MIN_GAP_PX) {
                continue;                      // 原文沒有夠寬的欄位間隔
            }
            int[] now = lastColumnGap(rows.get(i));
            if (now == null) {
                continue;
            }
            gap[i] = now;
            edge = Math.max(edge, origW[i]);
        }
        boolean columnsOver = false;
        int columnsNeed = 0;
        for (int i = 0; i < n; i++) {
            if (gap[i] == null) {
                continue;
            }
            if (origW[i] < edge - EDGE_TOLERANCE) {
                gap[i] = null;
                continue;
            }
            // 拿掉間隔之後的寬度，加上最小間隔，就是這一行最少要多寬。
            int need = madeW[i] - gap[i][2] + LineTranslator.MIN_GAP;
            columnsNeed = Math.max(columnsNeed, need);
            columnsOver |= madeW[i] > edge || need > edge;
        }

        // ---- 置中的行 ----
        int[] lead = new int[n];
        int[] content = new int[n];
        boolean[] centre = new boolean[n];
        boolean centresOver = false;
        int centresNeed = 0;
        for (int i = 0; i < n; i++) {
            if (!centered[i] || !visible(rows.get(i))) {
                continue;
            }
            // 原文的縮排是負的（物品名稱前面那種往回退的偏移）就不是單純的置中，不碰。
            if (leadingPx(segments(original.get(i), width)) < 0) {
                continue;
            }
            lead[i] = leadingPx(rows.get(i));
            content[i] = madeW[i] - lead[i];
            if (content[i] <= 0) {
                continue;
            }
            centre[i] = true;
            centresNeed = Math.max(centresNeed, content[i]);
            centresOver |= madeW[i] > frame || lead[i] < 0;
        }

        if (!columnsOver && !centresOver) {
            return translated;                 // 每一行都放得下：原封不動
        }

        int widened = Math.max(frame, Math.max(columnsNeed, centresNeed));
        // 欄位的右緣本來就頂著整份 tooltip 的右緣（絕大多數的物品都是）時，
        // 撐開之後也要跟著頂到新的右緣；否則只跟自己那一組對齊。
        boolean rideFrame = edge >= frame - EDGE_TOLERANCE;
        int columnTarget = rideFrame ? widened : Math.max(edge, columnsNeed);
        boolean moveColumns = columnsOver || (rideFrame && widened > frame);
        boolean moveCentres = centresOver || widened > frame;

        List<Component> out = new ArrayList<>(translated);
        StringBuilder log = new StringBuilder();
        log.append(String.format("  原文最寬 %d  欄位右緣 %d  撐開到 %d%s%n", frame, edge, widened,
                rideFrame ? "" : "（欄位不頂外框，欄位收在 " + columnTarget + "）"));
        for (int i = 0; i < n; i++) {
            if (moveColumns && gap[i] != null && madeW[i] != columnTarget) {
                int delta = columnTarget - madeW[i];
                out.set(i, withGap(rows.get(i), gap[i], delta));
                log.append(String.format("  [%d] 欄位 整行 %d -> %d  間隔 %d -> %d  %s%n",
                        i, madeW[i], columnTarget, gap[i][2], gap[i][2] + delta,
                        text(rows.get(i))));
            } else if (moveCentres && centre[i]) {
                int newLead = Math.max(0, (widened - content[i]) / 2);
                if (newLead != lead[i]) {
                    out.set(i, withLead(rows.get(i), newLead));
                    log.append(String.format("  [%d] 置中 內容 %d  縮排 %d -> %d  %s%n",
                            i, content[i], lead[i], newLead, text(rows.get(i))));
                }
            }
        }
        LineDebug.pieces("撐寬 " + translated.get(0).getString(), log.toString());
        return out;
    }

    /**
     * 一段：文字，或一段寬度偏移。
     *
     * <p>{@code visit} 給的是解析過的樣式，所以組回去之後不會繼承到別人的父層。
     */
    record Seg(String text, Style style, boolean gap, int px) {}

    /**
     * 拆成段。文字尾端黏著的偏移也拆出來——Wynncraft 常把欄位間隔接在標籤後面
     * 同一個片段裡（見 {@link SpaceOffset#trailingOffsets}），不拆的話找不到那個間隔。
     */
    static List<Seg> segments(Component line, ToIntFunction<Component> width) {
        List<Seg> out = new ArrayList<>();
        line.visit((style, text) -> {
            if (text.isEmpty()) {
                return java.util.Optional.empty();
            }
            if (isGap(style, text, width)) {
                out.add(new Seg(text, style, true, SpaceOffset.decode(text)));
                return java.util.Optional.empty();
            }
            String tail = SpaceOffset.trailingOffsets(text);
            if (!tail.isEmpty() && tail.length() < text.length() && isGap(style, tail, width)) {
                out.add(new Seg(text.substring(0, text.length() - tail.length()), style, false, 0));
                out.add(new Seg(tail, style, true, SpaceOffset.decode(tail)));
            } else {
                out.add(new Seg(text, style, false, 0));
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    /**
     * 這段是不是能安全重新編碼的寬度偏移。
     *
     * <p>跟 {@link LineTranslator#isAdjustableSpace} 同一個判準：space 字型底下的偏移
     * 一定是；別的字型底下要<b>量出來的寬度剛好等於解碼值</b>才算，材質包圖示碰巧
     * 落在同一段碼位時才不會被當成間隔。
     */
    private static boolean isGap(Style style, String text, ToIntFunction<Component> width) {
        if (!SpaceOffset.isOffsetRun(text)) {
            return false;
        }
        return SpaceOffset.isSpaceFont(style)
                || width.applyAsInt(Component.literal(text).withStyle(style))
                   == SpaceOffset.decode(text);
    }

    /**
     * 最後一個<b>欄位間隔</b>：前面有標籤、後面有數值的那一串連續偏移。
     *
     * <p>前面只有圖示的是圖示與內文之間的排版（見 {@link LineTranslator#labelled}）；
     * 後面沒有實字的是圖示疊字的往回退（見 {@link LineTranslator#overlayGap}）。
     * 兩種都不是欄位，撐開時不能動。
     *
     * @return {@code {起, 迄, 總寬}}，迄不含；沒有就回傳 {@code null}
     */
    static int[] lastColumnGap(List<Seg> segs) {
        int[] found = null;
        int i = 0;
        while (i < segs.size()) {
            if (!segs.get(i).gap()) {
                i++;
                continue;
            }
            int from = i;
            int total = 0;
            while (i < segs.size() && segs.get(i).gap()) {
                total += segs.get(i).px();
                i++;
            }
            if (hasWord(segs, 0, from) && hasWord(segs, i, segs.size())) {
                found = new int[] {from, i, total};
            }
        }
        return found;
    }

    /** {@code [from, to)} 之間有沒有含字母或數字的文字段。方塊字、西里爾字母都算字母。 */
    private static boolean hasWord(List<Seg> segs, int from, int to) {
        for (int i = from; i < to; i++) {
            Seg s = segs.get(i);
            if (s.gap()) {
                continue;
            }
            for (int k = 0; k < s.text().length(); k++) {
                if (Character.isLetterOrDigit(s.text().charAt(k))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 行首那一串偏移的總寬；跟 {@link BlockLayout} 量縮排的方式一樣，碰到第一段內容就停。 */
    private static int leadingPx(List<Seg> segs) {
        int px = 0;
        for (Seg s : segs) {
            if (!s.gap()) {
                break;
            }
            px += s.px();
        }
        return px;
    }

    /** 有沒有看得見的字。分隔線、只有圖示的行都沒有。 */
    private static boolean visible(List<Seg> segs) {
        for (Seg s : segs) {
            if (s.gap()) {
                continue;
            }
            String t = s.text();
            for (int k = 0; k < t.length(); ) {
                int cp = t.codePointAt(k);
                k += Character.charCount(cp);
                if (!Character.isWhitespace(cp) && !GlyphSplitter.isGlyphCodePoint(cp)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 把欄位間隔放寬 {@code delta} 像素。
     *
     * <p>只改那一串偏移的<b>最後一個</b>：前面的若是往回退的偏移
     * （{@code 往回 41 再往前 145}），那是 Wynncraft 自己的寫法，照留。
     * 改過的那一段換成 space 字型重新編碼——那是保證認得偏移碼位的字型。
     */
    private static Component withGap(List<Seg> segs, int[] gap, int delta) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < segs.size(); i++) {
            Seg s = segs.get(i);
            if (i == gap[1] - 1) {
                append(out, SpaceOffset.encode(s.px() + delta), SpaceOffset.styleFor(s.style()));
            } else {
                append(out, s.text(), s.style());
            }
        }
        return out;
    }

    /**
     * 換掉行首的縮排。
     *
     * <p>原本多個偏移字元組成的縮排併成一個——重新編碼只需要總寬度相同
     * （見 {@link SpaceOffset#isOffsetRun}）。最寬的那行原本可能根本沒有縮排，
     * 那就在最前面補一個。
     */
    private static Component withLead(List<Seg> segs, int px) {
        MutableComponent out = Component.empty();
        int i = 0;
        while (i < segs.size() && segs.get(i).gap()) {
            i++;
        }
        Style base = segs.isEmpty() ? Style.EMPTY : segs.get(0).style();
        append(out, SpaceOffset.encode(px), SpaceOffset.styleFor(base));
        for (; i < segs.size(); i++) {
            append(out, segs.get(i).text(), segs.get(i).style());
        }
        return out;
    }

    private static void append(MutableComponent out, String text, Style style) {
        if (!text.isEmpty()) {
            out.append(Component.literal(text).withStyle(style));
        }
    }

    /** 診斷用：這一行的文字，偏移拿掉。 */
    private static String text(List<Seg> segs) {
        StringBuilder sb = new StringBuilder();
        for (Seg s : segs) {
            if (!s.gap()) {
                sb.append(s.text());
            }
        }
        return sb.toString();
    }
}
