package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

final class SpaceRuns {

    private SpaceRuns() {}

    static int leadWidth(List<Run> row) {
        int px = 0;
        for (Run run : row) {
            if (run.space()) {
                px += run.px();
                continue;
            }
            String text = run.text();
            int n = 0;
            while (n < text.length() && Character.isWhitespace(text.charAt(n))) {
                n++;
            }
            px += LineTranslator.widthOf(DisplayStyles.literal(text.substring(0, n), run.style()));
            if (n < text.length()) {
                return px;
            }
        }
        return px;
    }

    static int rowWidth(List<Run> row) {
        int px = 0;
        for (Run run : row) {
            px += run.space() ? run.px() : LineTranslator.widthOf(DisplayStyles.literal(run.text(), run.style()));
        }
        return px;
    }

    static String rowText(List<Run> runs) {
        StringBuilder sb = new StringBuilder();
        for (Run r : runs) {
            if (!r.space()) {
                sb.append(r.text());
            }
        }
        return sb.toString();
    }

    static List<List<Run>> splitRows(List<Run> runs) {
        List<List<Run>> out = new ArrayList<>();
        List<Run> row = new ArrayList<>();
        for (Run r : runs) {
            if (r.space() || r.text().indexOf('\n') < 0) {
                row.add(r);
                continue;
            }
            String[] parts = r.text().split(LineTranslator.NL, -1);
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) {
                    out.add(row);
                    row = new ArrayList<>();
                }
                if (!parts[i].isEmpty()) {
                    row.add(new Run(false, 0, r.style(), parts[i]));
                }
            }
        }
        out.add(row);
        return out;
    }

    static int rows(List<Run> runs) {
        return splitRows(runs).size();
    }

    record Run(boolean space, int px, Style style, String text) {}

    static List<Run> runs(Component component) {
        List<Run> out = new ArrayList<>();
        component.visit((style, text) -> {
            if (!text.isEmpty()) {
                out.add(isAdjustableSpace(style, text)
                        ? new Run(true, SpaceOffset.decode(text), style, text)
                        : new Run(false, 0, style, text));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    static boolean isAdjustableSpace(Style style, String text) {
        return isAdjustableSpace(style, text, LineTranslator.widthOf(DisplayStyles.literal(text, style)));
    }

    // language/wynncraft 字型也收了偏移碼位，量寬等於解碼值才算
    static boolean isAdjustableSpace(Style style, String text, int measured) {
        if (!SpaceOffset.isOffsetRun(text)) {
            return false;
        }
        return SpaceOffset.isSpaceFont(style) || measured == SpaceOffset.decode(text);
    }

    static boolean overlayGap(List<Run> runs, int gap, int px) {
        return px < 0 && !textAfterGap(runs, gap);
    }

    static boolean textAfterGap(List<Run> runs, int gap) {
        boolean[] gaps = tooltipGaps(runs);
        int seen = 0;
        boolean after = false;
        for (int i = 0; i < runs.size(); i++) {
            if (gaps[i]) {
                if (after) {
                    return false;
                }
                if (seen++ == gap) {
                    after = true;
                }
                continue;
            }
            if (!after) {
                continue;
            }
            String text = runs.get(i).text();
            for (int k = 0; text != null && k < text.length(); k++) {
                if (Character.isLetterOrDigit(text.charAt(k))) {
                    return true;
                }
            }
        }
        return false;
    }

    static int[] gapPixels(List<Run> runs) {
        boolean[] gaps = tooltipGaps(runs);
        int[] out = new int[count(gaps)];
        int n = 0;
        for (int i = 0; i < runs.size(); i++) {
            if (gaps[i]) {
                out[n++] = runs.get(i).px();
            }
        }
        return out;
    }

    static boolean labelledRun(List<Run> runs, int gap) {
        boolean[] gaps = tooltipGaps(runs);
        int seen = 0;
        for (int i = 0; i < runs.size(); i++) {
            if (gaps[i]) {
                if (seen++ == gap) {
                    return false;
                }
                continue;
            }
            String text = runs.get(i).text();
            for (int k = 0; text != null && k < text.length(); k++) {
                if (Character.isLetterOrDigit(text.charAt(k))) {
                    return true;
                }
            }
        }
        return false;
    }

    static List<Run> splitGaps(List<Run> runs) {
        return splitGaps(runs, SpaceRuns::isAdjustableSpace);
    }

    static List<Run> splitGaps(List<Run> runs,
                               BiPredicate<Style, String> isGap) {
        List<Run> out = new ArrayList<>(runs.size() + 2);
        boolean split = false;
        for (Run r : runs) {
            List<Run> pieces = r.space() ? null : splitOffsets(r, isGap);
            if (pieces == null) {
                out.add(r);
                continue;
            }
            out.addAll(pieces);
            split = true;
        }
        return split ? out : runs;
    }

    private static List<Run> splitOffsets(Run r,
                                          BiPredicate<Style, String> isGap) {
        String text = r.text();
        List<Run> out = new ArrayList<>(3);
        boolean any = false;
        int from = 0;
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            if (!SpaceOffset.isOffset(cp)) {
                i += Character.charCount(cp);
                continue;
            }
            int end = i;
            while (end < text.length() && SpaceOffset.isOffset(text.codePointAt(end))) {
                end += Character.charCount(text.codePointAt(end));
            }
            String gap = text.substring(i, end);
            if (gap.length() < text.length() && isGap.test(r.style(), gap)) {
                if (i > from) {
                    out.add(new Run(false, 0, r.style(), text.substring(from, i)));
                }
                out.add(new Run(true, SpaceOffset.decode(gap), r.style(), gap));
                from = end;
                any = true;
            }
            i = end;
        }
        if (!any) {
            return null;
        }
        if (from < text.length()) {
            out.add(new Run(false, 0, r.style(), text.substring(from)));
        }
        return out;
    }

    static final int MIN_GAP_PX = 8;

    static boolean isColumnGap(Run r) {
        return r.space() && Math.abs(r.px()) >= MIN_GAP_PX;
    }

    static boolean[] chatGaps(List<Run> runs) {
        boolean[] out = new boolean[runs.size()];
        for (int i = 0; i < runs.size(); i++) {
            Run r = runs.get(i);
            if (!r.space()) {
                continue;
            }
            if (isColumnGap(r)) {
                out[i] = true;
                continue;
            }
            if (r.px() < 0) {
                continue;
            }
            out[i] = textBefore(runs, i) && startsWithLetter(runs, i + 1);
        }
        return out;
    }

    private static boolean textBefore(List<Run> runs, int at) {
        for (int i = at - 1; i >= 0; i--) {
            Run r = runs.get(i);
            if (!r.space()) {
                return GlyphSplitter.hasLetter(r.text());
            }
        }
        return true;
    }

    private static boolean startsWithValue(List<Run> runs, int from) {
        String head = headAfter(runs, from);
        if (head.isEmpty()) {
            return false;
        }
        if (head.charAt(0) == '[') {
            return signedNumber(head, 1);
        }
        int first = head.codePointAt(0);
        if (Character.isLetterOrDigit(first)) {
            return true;
        }
        return signedNumber(head, 0);
    }

    private static boolean signedNumber(String head, int at) {
        if (head.length() <= at) {
            return false;
        }
        char c = head.charAt(at);
        if (Character.isDigit(c)) {
            return true;
        }
        return (c == '+' || c == '-')
                && head.length() > at + 1 && Character.isDigit(head.charAt(at + 1));
    }

    private static String headAfter(List<Run> runs, int from) {
        final int want = 3;
        StringBuilder head = new StringBuilder(want);
        for (int i = from; i < runs.size() && head.length() < want; i++) {
            Run r = runs.get(i);
            if (r.space()) {
                continue;
            }
            String text = head.isEmpty() ? r.text().stripLeading() : r.text();
            if (text.isEmpty()) {
                continue;
            }
            head.append(text, 0, Math.min(text.length(), want - head.length()));
        }
        return head.toString();
    }

    private static boolean startsWithLetter(List<Run> runs, int from) {
        for (int i = from; i < runs.size(); i++) {
            Run r = runs.get(i);
            if (r.space()) {
                continue;
            }
            String text = r.text().stripLeading();
            return !text.isEmpty() && Character.isLetter(text.codePointAt(0));
        }
        return false;
    }

    static boolean[] tooltipGaps(List<Run> runs) {
        boolean[] out = new boolean[runs.size()];
        for (int i = 0; i < runs.size(); i++) {
            Run r = runs.get(i);
            if (!r.space()) {
                continue;
            }
            if (isColumnGap(r)) {
                out[i] = true;
                continue;
            }
            if (r.px() <= 0) {
                continue;
            }
            out[i] = wordBefore(runs, i) && startsWithValue(runs, i + 1);
        }
        return out;
    }

    private static boolean wordBefore(List<Run> runs, int at) {
        for (int i = at - 1; i >= 0; i--) {
            Run r = runs.get(i);
            if (!r.space()) {
                return GlyphSplitter.hasLetter(r.text());
            }
        }
        return false;
    }

    static int count(boolean[] flags) {
        int n = 0;
        for (boolean f : flags) {
            if (f) {
                n++;
            }
        }
        return n;
    }

    static int runWidth(Run r) {
        return LineTranslator.widthOf(DisplayStyles.literal(r.text(), r.style()));
    }

    static List<Integer> chatSegmentWidths(List<Run> runs, ToIntFunction<Run> width) {
        boolean[] gaps = chatGaps(runs);
        List<Integer> out = new ArrayList<>();
        int total = 0;
        for (int i = 0; i < runs.size(); i++) {
            if (gaps[i]) {
                out.add(total);
                total = 0;
            } else if (!runs.get(i).space()) {
                total += width.applyAsInt(runs.get(i));
            }
        }
        out.add(total);
        return out;
    }

    static Component applyChat(List<Run> runs, int[] adjust) {
        boolean[] gaps = chatGaps(runs);
        MutableComponent out = Component.empty();
        int index = 0;
        for (int i = 0; i < runs.size(); i++) {
            Run r = runs.get(i);
            if (!gaps[i]) {
                out.append(DisplayStyles.literal(r.text(), r.style()));
                continue;
            }
            String encoded = SpaceOffset.encode(r.px() + adjust[index++]);
            if (!encoded.isEmpty()) {
                out.append(DisplayStyles.literal(encoded, r.style()));
            }
        }
        return out;
    }

    static int countSpaces(List<Run> runs) {
        return count(tooltipGaps(runs));
    }

    static List<Integer> segmentWidths(List<Run> runs) {
        boolean[] gaps = tooltipGaps(runs);
        List<Integer> out = new ArrayList<>();
        int width = 0;
        for (int i = 0; i < runs.size(); i++) {
            Run r = runs.get(i);
            if (gaps[i]) {
                out.add(width);
                width = 0;
            } else {
                width += LineTranslator.widthOf(DisplayStyles.literal(r.text(), r.style()));
            }
        }
        out.add(width);
        return out;
    }

    static int sum(List<Integer> values) {
        int total = 0;
        for (int v : values) {
            total += v;
        }
        return total;
    }

    static Component apply(List<Run> runs, int[] adjust) {
        boolean[] gaps = tooltipGaps(runs);
        MutableComponent out = Component.empty();
        int index = 0;
        for (int i = 0; i < runs.size(); i++) {
            Run r = runs.get(i);
            if (!gaps[i]) {
                out.append(DisplayStyles.literal(r.text(), r.style()));
                continue;
            }
            String encoded = SpaceOffset.encode(r.px() + adjust[index++]);
            if (!encoded.isEmpty()) {
                out.append(DisplayStyles.literal(encoded, r.style()));
            }
        }
        return out;
    }
}
