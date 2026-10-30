package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

public final class TooltipWiden {

    // Wynncraft 照自己的字寬取整，量出來偶爾差一兩像素
    private static final int EDGE_TOLERANCE = 2;

    private TooltipWiden() {}

    public static List<Component> fit(List<Component> original, List<Component> translated,
                                      boolean[] centered, boolean leftAligned,
                                      ToIntFunction<Component> width) {
        if (original == null || translated == null || centered == null) {
            return translated;
        }
        int n = translated.size();
        if (original.size() != n || centered.length != n) {
            boolean measurable = false;
            for (Component line : original) {
                measurable |= width.applyAsInt(line) > 0;
            }
            return leftAligned || !measurable
                    ? translated : centreColumns(original, translated, width);
        }
        int[] origW = new int[n];
        int[] madeW = new int[n];
        int frame = 0;
        for (int i = 0; i < n; i++) {
            origW[i] = width.applyAsInt(original.get(i));
            madeW[i] = width.applyAsInt(translated.get(i));
            frame = Math.max(frame, origW[i]);
        }
        if (frame <= 0) {
            return translated;
        }
        if (!leftAligned) {
            List<Component> recentred = centreColumns(original, translated, width);
            if (recentred != translated) {
                translated = recentred;
                for (int i = 0; i < n; i++) {
                    madeW[i] = width.applyAsInt(translated.get(i));
                }
            }
        }

        List<List<Seg>> rows = new ArrayList<>(n);
        for (Component line : translated) {
            rows.add(segments(line, width));
        }

        int[][] gap = new int[n][];
        int edge = 0;
        for (int i = 0; i < n; i++) {
            if (leftAligned || centered[i]) {
                continue;
            }
            int[] was = lastColumnGap(segments(original.get(i), width));
            if (was == null || was[2] < LineTranslator.MIN_GAP_PX) {
                continue;
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
            int need = madeW[i] - gap[i][2] + LineTranslator.MIN_GAP;
            columnsNeed = Math.max(columnsNeed, need);
            columnsOver |= madeW[i] > edge || need > edge;
        }

        int[] lead = new int[n];
        int[] content = new int[n];
        boolean[] centre = new boolean[n];
        boolean centresOver = false;
        int centresNeed = 0;
        for (int i = 0; i < n; i++) {
            if (!centered[i] || !visible(rows.get(i))) {
                continue;
            }
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
            int box = 0;
            boolean any = false;
            for (int i = 0; i < n; i++) {
                box = Math.max(box, centre[i] ? content[i] : madeW[i]);
                any |= centre[i];
            }
            if (!any || box >= frame - EDGE_TOLERANCE) {
                return translated;
            }
            List<Component> out = null;
            StringBuilder log = new StringBuilder();
            log.append(String.format("  原文最寬 %d  譯文縮到 %d%n", frame, box));
            for (int i = 0; i < n; i++) {
                if (!centre[i]) {
                    continue;
                }
                int newLead = Math.max(0, (box - content[i]) / 2);
                if (Math.abs(newLead - lead[i]) <= 1) {
                    continue;
                }
                if (out == null) {
                    out = new ArrayList<>(translated);
                }
                out.set(i, withLead(rows.get(i), newLead));
                log.append(String.format("  [%d] 置中 內容 %d  縮排 %d -> %d  %s%n",
                        i, content[i], lead[i], newLead, text(rows.get(i))));
            }
            if (out == null) {
                return translated;
            }
            LineDebug.pieces("縮窄 " + translated.get(0).getString(), log.toString());
            return out;
        }

        int widened = Math.max(frame, Math.max(columnsNeed, centresNeed));
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

    static List<Component> centreColumns(List<Component> original, List<Component> translated,
                                         ToIntFunction<Component> width) {
        int n = translated.size();
        List<List<Seg>> madeSegs = new ArrayList<>(n);
        List<List<int[]>> madeChunks = new ArrayList<>(n);
        List<Integer> madeRows = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<Seg> m = segments(translated.get(i), width);
            List<int[]> mc = chunks(m, width);
            madeSegs.add(m);
            madeChunks.add(mc);
            if (mc != null && mc.size() >= 2) {
                madeRows.add(i);
            }
        }
        List<List<int[]>> origMulti = new ArrayList<>();
        for (Component line : original) {
            List<int[]> oc = chunks(segments(line, width), width);
            if (oc != null && oc.size() >= 2) {
                origMulti.add(oc);
            }
        }
        if (origMulti.size() != madeRows.size()) {
            return translated;
        }
        List<List<int[]>> origChunks = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            origChunks.add(null);
        }
        for (int k = 0; k < madeRows.size(); k++) {
            int row = madeRows.get(k);
            if (origMulti.get(k).size() != madeChunks.get(row).size()) {
                return translated;
            }
            origChunks.set(row, origMulti.get(k));
        }
        List<Component> out = null;
        for (int i = 0; i < n; i++) {
            List<int[]> oc = origChunks.get(i);
            if (oc == null) {
                continue;
            }
            List<int[]> mc = madeChunks.get(i);
            int[] target = new int[oc.size()];
            boolean moved = false;
            boolean any = false;
            for (int k = 0; k < oc.size(); k++) {
                int[] c = oc.get(k);
                boolean centred = sharesCentre(c, k, i, origChunks);
                any |= centred;
                target[k] = centred ? c[0] + (c[1] - mc.get(k)[1]) / 2 : c[0];
                moved |= target[k] != mc.get(k)[0];
            }
            if (!any || !moved) {
                continue;
            }
            Component rebuilt = placeChunks(madeSegs.get(i), mc, target, width);
            if (rebuilt == null) {
                continue;
            }
            if (out == null) {
                out = new ArrayList<>(translated);
            }
            out.set(i, rebuilt);
        }
        return out == null ? translated : out;
    }

    private static boolean sharesCentre(int[] c, int k, int row, List<List<int[]>> all) {
        if (leftAlignedColumn(k, all)) {
            return false;
        }
        int centre2 = 2 * c[0] + c[1];
        for (int j = 0; j < all.size(); j++) {
            if (j == row || all.get(j) == null) {
                continue;
            }
            for (int[] d : all.get(j)) {
                if (Math.abs(centre2 - (2 * d[0] + d[1])) <= 2
                        && Math.abs(c[0] - d[0]) > 2 * EDGE_TOLERANCE) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean leftAlignedColumn(int k, List<List<int[]>> all) {
        List<Integer> starts = new ArrayList<>();
        for (List<int[]> row : all) {
            if (row != null && k < row.size()) {
                starts.add(row.get(k)[0]);
            }
        }
        for (int i = 0; i < starts.size(); i++) {
            for (int j = i + 1; j < starts.size(); j++) {
                if (Math.abs(starts.get(i) - starts.get(j)) <= EDGE_TOLERANCE) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<int[]> chunks(List<Seg> segs, ToIntFunction<Component> width) {
        List<int[]> out = new ArrayList<>();
        int x = 0;
        int i = 0;
        boolean letters = false;
        while (i < segs.size()) {
            Seg s = segs.get(i);
            if (s.gap()) {
                if (s.px() < 0) {
                    return null;
                }
                x += s.px();
                i++;
                continue;
            }
            int start = x;
            int from = i;
            while (i < segs.size() && !segs.get(i).gap()) {
                Seg t = segs.get(i);
                x += width.applyAsInt(Component.literal(t.text()).withStyle(t.style()));
                letters |= t.text().codePoints().anyMatch(Character::isLetterOrDigit);
                i++;
            }
            out.add(new int[] {start, x - start, from, i - 1});
        }
        return letters ? out : null;
    }

    private static Component placeChunks(List<Seg> segs, List<int[]> chunks, int[] target,
                                         ToIntFunction<Component> width) {
        MutableComponent out = Component.empty();
        int x = 0;
        int next = 0;
        Style gapStyle = null;
        for (int i = 0; i < segs.size(); i++) {
            Seg s = segs.get(i);
            if (s.gap()) {
                gapStyle = gapStyle == null ? s.style() : gapStyle;
                if (next >= chunks.size()) {
                    append(out, s.text(), s.style());
                }
                continue;
            }
            if (next < chunks.size() && i == chunks.get(next)[2]) {
                int want = target[next];
                int px = next == 0 ? Math.max(0, want - x) : Math.max(1, want - x);
                Style st = gapStyle == null ? s.style() : gapStyle;
                append(out, SpaceOffset.encode(px), SpaceOffset.styleFor(st));
                x += px;
                gapStyle = null;
                int[] c = chunks.get(next);
                for (int k = c[2]; k <= c[3]; k++) {
                    append(out, segs.get(k).text(), segs.get(k).style());
                }
                x += c[1];
                i = c[3];
                next++;
            }
        }
        return next == chunks.size() ? out : null;
    }

    record Seg(String text, Style style, boolean gap, int px) {}

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

    private static boolean isGap(Style style, String text, ToIntFunction<Component> width) {
        if (!SpaceOffset.isOffsetRun(text)) {
            return false;
        }
        return SpaceOffset.isSpaceFont(style)
                || width.applyAsInt(Component.literal(text).withStyle(style))
                   == SpaceOffset.decode(text);
    }

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
