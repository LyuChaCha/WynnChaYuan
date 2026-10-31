package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

public final class BlockLayout {

    private static final int TOLERANCE = 6;

    private static final int MIN_RUN = 3;

    private BlockLayout() {}

    public static boolean[] centered(List<Component> lines) {
        boolean[] result = centered(lines, BlockLayout::measure);
        LayoutDebug.record(lines, result);
        return result;
    }

    static String explain(List<Component> lines, boolean[] result) {
        return explain(lines, result, BlockLayout::measure);
    }

    static String explain(List<Component> lines, boolean[] result,
                          ToIntFunction<Component> width) {
        StringBuilder sb = new StringBuilder();
        int widest = 0;
        for (int i = 0; i < lines.size(); i++) {
            widest = Math.max(widest, width.applyAsInt(lines.get(i)));
        }
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int lead = leadingOffset(line);
            int content = Math.max(0, width.applyAsInt(line) - lead);
            String text = line.getString().replace("\n", "  ");
            sb.append(String.format(
                    "  [%2d] %-8s 縮排 %4d  內容 %4d  置中應為 %4d  %s%n",
                    i,
                    isSeparator(line) ? "分隔線" : (result[i] ? "置中" : "靠左"),
                    lead, content, (widest - content) / 2,
                    text.length() > 34 ? text.substring(0, 34) + "…" : text));
        }
        sb.append(String.format("  整份最寬 %d px%n", widest));
        return sb.toString();
    }

    static boolean[] centered(List<Component> lines, ToIntFunction<Component> width) {
        int n = lines.size();
        boolean[] result = new boolean[n];
        if (n == 0) {
            return result;
        }
        int[] lead = new int[n];
        int[] content = new int[n];
        boolean[] blank = new boolean[n];
        for (int i = 0; i < n; i++) {
            Component line = lines.get(i);
            blank[i] = isSeparator(line);
            lead[i] = leadingOffset(line);
            content[i] = Math.max(0, width.applyAsInt(line) - lead[i]);
        }

        int start = 0;
        while (start < n) {
            if (blank[start]) {
                start++;
                continue;
            }
            int end = start;
            while (end < n && !blank[end]) {
                end++;
            }
            markCentred(result, lead, content, start, end, widest(lead, content, blank));
            start = end;
        }
        return result;
    }

    private static int widest(int[] lead, int[] content, boolean[] blank) {
        int max = 0;
        for (int i = 0; i < lead.length; i++) {
            if (!blank[i]) {
                max = Math.max(max, lead[i] + content[i]);
            }
        }
        return max;
    }

    private static void markCentred(boolean[] result, int[] lead, int[] content,
                                    int start, int end, int blockWidth) {
        if (end - start == 1 && isCentredAlone(lead, content, start, blockWidth)) {
            result[start] = true;
            return;
        }
        if (isCentredBlock(lead, content, start, end, blockWidth)) {
            Arrays.fill(result, start, end, true);
            return;
        }
        int bestFrom = -1;
        int bestLen = 0;
        for (int from = start; from + MIN_RUN <= end; from++) {
            for (int to = end; to - from > bestLen && to - from >= MIN_RUN; to--) {
                if (isCentredBlock(lead, content, from, to, blockWidth)) {
                    bestFrom = from;
                    bestLen = to - from;
                    break;
                }
            }
        }
        if (bestFrom >= 0) {
            Arrays.fill(result, bestFrom, bestFrom + bestLen, true);
        }
    }

    private static boolean isCentredAlone(int[] lead, int[] content,
                                          int at, int blockWidth) {
        if (lead[at] <= TOLERANCE || content[at] <= 0) {
            return false;
        }
        if (blockWidth - (lead[at] + content[at]) <= TOLERANCE) {
            return false;
        }
        int room = blockWidth - content[at];
        int allowed = Math.max(TOLERANCE * 2, blockWidth * SPAN_TOLERANCE_PERCENT / 100);
        return Math.abs(lead[at] - room / 2) <= allowed;
    }

    private static boolean isCentredBlock(int[] lead, int[] content, int start, int end,
                                          int blockWidth) {
        if (end - start < 2) {
            return false;
        }
        int minLead = Integer.MAX_VALUE;
        int maxLead = Integer.MIN_VALUE;
        int minSpan = Integer.MAX_VALUE;
        int maxSpan = Integer.MIN_VALUE;
        for (int i = start; i < end; i++) {
            minLead = Math.min(minLead, lead[i]);
            maxLead = Math.max(maxLead, lead[i]);
            int span = content[i] + lead[i] * 2;
            minSpan = Math.min(minSpan, span);
            maxSpan = Math.max(maxSpan, span);
        }
        if (maxSpan <= 0 || maxLead <= TOLERANCE) {
            return false;
        }
        int minContent = Integer.MAX_VALUE;
        int maxContent = Integer.MIN_VALUE;
        for (int i = start; i < end; i++) {
            minContent = Math.min(minContent, content[i]);
            maxContent = Math.max(maxContent, content[i]);
        }
        if (maxLead - minLead <= TOLERANCE && maxContent - minContent <= TOLERANCE) {
            return false;
        }
        int minRight = Integer.MAX_VALUE;
        int maxRight = Integer.MIN_VALUE;
        for (int i = start; i < end; i++) {
            minRight = Math.min(minRight, lead[i] + content[i]);
            maxRight = Math.max(maxRight, lead[i] + content[i]);
        }
        if (maxLead - minLead > TOLERANCE && maxRight - minRight <= TOLERANCE) {
            return false;
        }
        for (int i = start; i < end; i++) {
            for (int j = i + 1; j < end; j++) {
                if (contradicts(lead, content, i, j) || contradicts(lead, content, j, i)) {
                    return false;
                }
            }
        }
        for (int i = start; i < end; i++) {
            for (int j = i + 1; j < end; j++) {
                if (lead[i] == lead[j] && Math.abs(content[i] - content[j]) > TOLERANCE) {
                    return false;
                }
            }
        }
        int allowed = Math.max(TOLERANCE * 2, maxSpan * SPAN_TOLERANCE_PERCENT / 100);
        if (maxSpan - minSpan > allowed) {
            return false;
        }
        return maxSpan >= blockWidth - allowed;
    }

    private static boolean contradicts(int[] lead, int[] content, int a, int b) {
        return content[a] - content[b] > RANK_SLACK && lead[a] - lead[b] > RANK_SLACK;
    }

    private static final int RANK_SLACK = TOLERANCE * 3;

    private static final int SPAN_TOLERANCE_PERCENT = 15;

    private static boolean isSeparator(Component line) {
        String text = line.getString();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (!Character.isWhitespace(cp) && !GlyphSplitter.isGlyphCodePoint(cp)) {
                return false;
            }
        }
        return true;
    }

    private static int leadingOffset(Component line) {
        int[] px = {0};
        boolean[] done = {false};
        line.visit((style, text) -> {
            if (done[0] || text.isEmpty()) {
                return Optional.empty();
            }
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                px[0] += SpaceOffset.decode(text);
            } else {
                done[0] = true;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return px[0];
    }

    private static int measure(Component line) {
        return LineTranslator.width(line);
    }

}
