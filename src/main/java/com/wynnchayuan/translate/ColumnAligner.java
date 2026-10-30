package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.translate.SpaceRuns.Run;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class ColumnAligner {

    private ColumnAligner() {}

    static boolean columnsAreLeftAligned(List<StyledText> lines) {
        if (lines == null || lines.size() < MIN_COLUMN_ROWS) {
            return true;
        }
        List<Integer> starts = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        for (StyledText line : lines) {
            int[] both = secondColumn(line);
            if (both != null) {
                starts.add(both[0]);
                ends.add(both[1]);
            }
        }
        if (starts.size() < MIN_COLUMN_ROWS) {
            return true;
        }
        return spread(starts) < spread(ends);
    }

    private static final int MIN_COLUMN_ROWS = 2;

    // Wynncraft 欄距藏在標籤尾巴（先退回再跳），不能用像素門檻判斷
    private static int[] secondColumn(StyledText line) {
        int x = 0;
        int start = -1;
        boolean sawGap = false;
        boolean sawText = false;
        List<Run> rs = SpaceRuns.splitGaps(SpaceRuns.runs(line.getComponent()));
        boolean[] gaps = SpaceRuns.tooltipGaps(rs);
        for (int i = 0; i < rs.size(); i++) {
            Run r = rs.get(i);
            if (r.space()) {
                if (gaps[i] && start < 0 && sawText) {
                    sawGap = true;
                }
                x += r.px();
                continue;
            }
            String raw = r.text();
            if (raw == null || raw.isEmpty()) {
                continue;
            }
            if (sawGap && start < 0 && !raw.isBlank()) {
                start = x;
            }
            if (GlyphSplitter.hasLetter(raw)) {
                sawText = true;
            }
            x += LineTranslator.widthOf(DisplayStyles.literal(raw, r.style()));
        }
        return start < 0 ? null : new int[] {start, x};
    }

    static int spread(List<Integer> values) {
        int low = Integer.MAX_VALUE;
        int high = Integer.MIN_VALUE;
        for (int v : values) {
            low = Math.min(low, v);
            high = Math.max(high, v);
        }
        return high - low;
    }

    static List<int[]> textGroups(List<Piece> pieces) {
        List<int[]> groups = new ArrayList<>();
        int start = -1;
        for (int i = 0; i < pieces.size(); i++) {
            if (pieces.get(i).isSpace()) {
                if (start >= 0) {
                    groups.add(new int[] {start, i});
                    start = -1;
                }
            } else if (start < 0) {
                start = i;
            }
        }
        if (start >= 0) {
            groups.add(new int[] {start, pieces.size()});
        }
        return groups;
    }

    static List<Piece> recenterColumns(List<Piece> pieces) {
        List<int[]> groups = textGroups(pieces);
        int[] wantStart = new int[groups.size()];
        int[] newWidth = new int[groups.size()];

        int x = 0;
        int g = 0;
        for (int i = 0; i < pieces.size(); i++) {
            Piece p = pieces.get(i);
            if (p.isSpace()) {
                x += p.spacePx();
                continue;
            }
            if (g < groups.size() && groups.get(g)[0] == i) {
                int origWidth = 0;
                int width = 0;
                for (int k = groups.get(g)[0]; k < groups.get(g)[1]; k++) {
                    origWidth += LineTranslator.widthOf(pieces.get(k).orig());
                    width += LineTranslator.widthOf(pieces.get(k).rendered());
                }
                newWidth[g] = width;
                wantStart[g] = x + (origWidth - width) / 2;
                x += origWidth;
                g++;
            }
        }

        List<Piece> out = new ArrayList<>(pieces.size());
        int cursor = 0;
        g = 0;
        for (int i = 0; i < pieces.size(); i++) {
            Piece p = pieces.get(i);
            if (!p.isSpace()) {
                out.add(p);
                if (g < groups.size() && groups.get(g)[1] == i + 1) {
                    cursor += newWidth[g];
                    g++;
                }
                continue;
            }
            boolean lastOfRun = i + 1 >= pieces.size() || !pieces.get(i + 1).isSpace();
            if (!lastOfRun) {
                out.add(Piece.space(0, p.style()));
                continue;
            }
            if (g >= groups.size()) {
                out.add(p);
                continue;
            }
            int gap = Math.max(0, wantStart[g] - cursor);
            out.add(Piece.space(gap, p.style()));
            cursor += gap;
        }
        return out;
    }

    static List<Piece> alignColumns(List<Piece> pieces, boolean leftAligned) {
        List<Piece> out = new ArrayList<>(pieces.size());
        int drift = 0;
        int lastAdjusted = -1;
        for (int i = 0; i < pieces.size(); i++) {
            Piece p = pieces.get(i);
            if (!p.isSpace()) {
                drift += LineTranslator.widthOf(p.rendered()) - LineTranslator.widthOf(p.orig());
                out.add(p);
                continue;
            }
            if (isAlignSpace(pieces, i) && !isBacktrack(p)) {
                out.add(Piece.space(narrowed(p.spacePx(), p.spacePx() - drift), p.style()));
                lastAdjusted = i;
                drift = 0;
            } else {
                out.add(p);
            }
        }

        if (lastAdjusted >= 0 && drift != 0 && !leftAligned
                && labelled(out, lastAdjusted)) {
            Piece p = out.get(lastAdjusted);
            out.set(lastAdjusted,
                    Piece.space(narrowed(p.spacePx(), p.spacePx() - drift), p.style()));
        }
        return out;
    }

    private static boolean isBacktrack(Piece space) {
        return space.spacePx() < 0;
    }

    static List<Piece> settle(List<Piece> pieces, StyledText original) {
        int last = -1;
        for (int i = 0; i < pieces.size(); i++) {
            if (isAlignSpace(pieces, i) && !isBacktrack(pieces.get(i))
                    && labelled(pieces, i)) {
                last = i;
            }
        }
        if (last < 0) {
            return pieces;
        }
        int shortfall = LineTranslator.widthOf(original.getComponent())
                - LineTranslator.widthOf(assemble(pieces, -1, 0));
        if (shortfall == 0) {
            return pieces;
        }
        List<Piece> out = new ArrayList<>(pieces);
        Piece p = out.get(last);
        out.set(last, Piece.space(narrowed(p.spacePx(), p.spacePx() + shortfall),
                                  p.style()));
        return out;
    }

    static boolean labelled(List<Piece> pieces, int index) {
        for (int i = 0; i < index && i < pieces.size(); i++) {
            Piece p = pieces.get(i);
            if (p.isSpace()) {
                continue;
            }
            String text = p.orig() == null ? null : p.orig().getString();
            for (int k = 0; text != null && k < text.length(); k++) {
                if (Character.isLetterOrDigit(text.charAt(k))) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean isAlignSpace(List<Piece> pieces, int index) {
        return pieces.get(index).isSpace()
                && !isLeading(pieces, index)
                && hasTextAfter(pieces, index);
    }

    static int leadingSpace(List<Piece> pieces) {
        for (int i = 0; i < pieces.size(); i++) {
            if (pieces.get(i).isSpace() && isLeading(pieces, i)
                    && hasTextAfter(pieces, i)) {
                return i;
            }
        }
        return -1;
    }

    static int findAlignPoint(List<Piece> pieces) {
        int firstAlign = firstAlignSpace(pieces);
        int valueStart = valueRegionStart(pieces);

        if (firstAlign >= 0 && (valueStart < 0 || firstAlign <= valueStart)) {
            return firstAlign;
        }
        if (valueStart >= 0) {
            return valueStart;
        }
        return literalGapBoundary(pieces);
    }

    private static int literalGapBoundary(List<Piece> pieces) {
        for (int i = pieces.size() - 2; i > 0; i--) {
            String text = pieces.get(i).text();
            if (text.isEmpty() || !text.isBlank() || text.length() < MIN_COLUMN_GAP) {
                continue;
            }
            if (!isLeading(pieces, i) && hasTextAfter(pieces, i)) {
                return i + 1;
            }
        }
        return -1;
    }

    static int narrowed(int original, int adjusted) {
        return original > 0 ? Math.max(Math.min(MIN_GAP, original), adjusted)
                            : Math.max(original, adjusted);
    }

    static final int MIN_GAP = 6;

    private static final int MIN_COLUMN_GAP = 2;

    static boolean hasColumnGap(List<Piece> pieces, int boundary) {
        int spaces = 0;
        for (int i = boundary - 1; i >= 0; i--) {
            String text = pieces.get(i).text();
            if (text.isEmpty()) {
                continue;
            }
            int n = 0;
            while (n < text.length()
                    && Character.isWhitespace(text.charAt(text.length() - 1 - n))) {
                n++;
            }
            spaces += n;
            if (n < text.length()) {
                break;
            }
        }
        for (int i = boundary; i < pieces.size(); i++) {
            String text = pieces.get(i).text();
            if (!text.isEmpty() && text.isBlank()) {
                spaces += text.length();
                continue;
            }
            break;
        }
        if (spaces >= MIN_COLUMN_GAP) {
            return true;
        }
        return spaces >= 1 && startsWithSign(pieces, boundary)
                && !labelEndsWithColon(pieces, boundary);
    }

    private static boolean labelEndsWithColon(List<Piece> pieces, int boundary) {
        for (int i = boundary - 1; i >= 0; i--) {
            String text = pieces.get(i).text().strip();
            if (text.isEmpty()) {
                continue;
            }
            return isTrailingColon(text.charAt(text.length() - 1));
        }
        return false;
    }

    private static boolean startsWithSign(List<Piece> pieces, int boundary) {
        if (boundary >= pieces.size()) {
            return false;
        }
        String text = pieces.get(boundary).text().strip();
        return !text.isEmpty() && (text.charAt(0) == '+' || text.charAt(0) == '-');
    }

    static String describe(List<Piece> before, List<Piece> after,
                           StyledText original) {
        StringBuilder sb = new StringBuilder();
        int want = LineTranslator.widthOf(original.getComponent());
        int got = LineTranslator.widthOf(assemble(after, -1, 0));
        sb.append(String.format("  整行寬度 %d -> %d%s%n", want, got,
                want == got ? "  (守住)" : "  ← 差 " + (got - want) + " px"));
        for (int i = 0; i < after.size(); i++) {
            Piece b = i < before.size() ? before.get(i) : null;
            Piece a = after.get(i);
            if (a.isSpace()) {
                sb.append(String.format("  [%d] 空白 %s px -> %s px%s%n", i,
                        b == null ? "?" : String.valueOf(b.spacePx()),
                        String.valueOf(a.spacePx()),
                        isAlignSpace(after, i) ? "  (對齊欄)" : "  (不調整)"));
            } else {
                sb.append(String.format(
                        "  [%d] 文字 「%s」 <- 「%s」 寬 %d -> %d  色 %s -> %s  字型 %s -> %s%n",
                        i, a.text(), a.orig().getString(),
                        LineTranslator.widthOf(a.orig()), LineTranslator.widthOf(a.rendered()),
                        colourOf(a.orig()), colourOf(a.rendered()),
                        fontOf(a.orig()), fontOf(a.rendered())));
            }
        }
        return sb.toString();
    }

    private static String fontOf(Component component) {
        if (component == null) {
            return "-";
        }
        String[] found = {"-"};
        component.visit((style, text) -> {
            if (!text.isEmpty()) {
                String name = com.wynnchayuan.render.PairedFont.nameOf(style);
                int open = name.indexOf('=');
                found[0] = open < 0 ? (name.isEmpty() ? "預設" : name)
                        : name.substring(open + 1).replace("]", "");
                return java.util.Optional.of(true);
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static String colourOf(Component component) {
        if (component == null) {
            return "-";
        }
        String[] found = {"繼承"};
        component.visit((style, text) -> {
            if (!text.isEmpty() && style.getColor() != null) {
                found[0] = style.getColor().serialize();
                return java.util.Optional.of(true);
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static int firstAlignSpace(List<Piece> pieces) {
        for (int i = 0; i < pieces.size(); i++) {
            if (isAlignSpace(pieces, i)) {
                return i;
            }
        }
        return -1;
    }

    private static int valueRegionStart(List<Piece> pieces) {
        int i = pieces.size() - 1;
        while (i >= 0 && looksLikeValue(pieces.get(i).text())) {
            i--;
        }
        int valueStart = i + 1;
        if (i < 0 || valueStart >= pieces.size()) {
            return -1;
        }
        for (int j = valueStart; j < pieces.size(); j++) {
            if (pieces.get(j).text().chars().anyMatch(Character::isDigit)) {
                return valueStart;
            }
        }
        return -1;
    }

    private static boolean hasTextAfter(List<Piece> pieces, int index) {
        for (int i = index + 1; i < pieces.size(); i++) {
            if (!pieces.get(i).isSpace() && !pieces.get(i).text().isBlank()) {
                return true;
            }
        }
        return false;
    }

    static boolean isLeading(List<Piece> pieces, int index) {
        for (int i = 0; i < index; i++) {
            if (!pieces.get(i).isSpace() && !pieces.get(i).text().isBlank()) {
                return false;
            }
        }
        return true;
    }

    private static boolean looksLikeValue(String text) {
        String t = text.strip();
        if (t.isEmpty()) {
            return true;
        }
        char c = t.charAt(0);
        if (c == '+' || c == '-' || c == '[' || c == '(') {
            return true;
        }
        if (t.equalsIgnoreCase("to")) {
            return true;
        }
        return t.chars().noneMatch(Character::isLetter);
    }

    static Component assemble(List<Piece> pieces, int adjustIndex, int adjustPx) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < pieces.size(); i++) {
            Piece p = pieces.get(i);
            if (!p.isSpace()) {
                out.append(p.rendered());
                continue;
            }
            int px = p.spacePx() + (i == adjustIndex ? adjustPx : 0);
            String encoded = SpaceOffset.encode(px);
            if (!encoded.isEmpty()) {
                out.append(DisplayStyles.literal(encoded, p.style()));
            }
        }
        return out;
    }

    static boolean isTrailingColon(char c) {
        return c == ':' || c == '：';
    }

    record Piece(Component rendered, Component orig, Style style, int spacePx, boolean isSpace) {

        static Piece text(String t, Style s) {
            Component c = DisplayStyles.literal(t, s);
            return new Piece(c, c, s, 0, false);
        }

        static Piece translated(Component rendered, Component orig) {
            return new Piece(rendered, orig, Style.EMPTY, 0, false);
        }

        static Piece space(int px, Style s) {
            return new Piece(null, null, s, px, true);
        }

        String text() {
            return rendered == null ? "" : rendered.getString();
        }
    }
}
