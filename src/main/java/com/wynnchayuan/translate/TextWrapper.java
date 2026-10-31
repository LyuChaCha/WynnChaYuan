package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

final class TextWrapper {

    private static final char NEWLINE = '\n';

    private static final String NL = "\n";

    private TextWrapper() {}

    static final int MAX_LABEL_LENGTH = 40;

    static boolean needsWrap(String translated, int originalLines, boolean flowed) {
        return flowed || lines(translated) < originalLines;
    }

    static String wrapBalanced(String text, int maxPx, ToIntFunction<String> measure) {
        return wrapBalanced(text, maxPx, Integer.MAX_VALUE, measure);
    }

    static String wrapBalanced(String text, int maxPx, int rows,
                               ToIntFunction<String> measure) {
        String wrapped = wrapBalanced(text, maxPx, rows, measure, true);
        if (rows == Integer.MAX_VALUE || widestRow(wrapped, measure) <= maxPx) {
            return wrapped;
        }
        String inside = wrapBalanced(text, maxPx, rows, measure, false);
        return lines(inside) <= rows && widestRow(inside, measure) < widestRow(wrapped, measure)
                ? inside : wrapped;
    }

    private static String wrapBalanced(String text, int maxPx, int rows,
                                       ToIntFunction<String> measure, boolean reach) {
        int width = maxPx;
        Keep keep = Keep.NONE;
        String wrapped = null;
        for (int attempt = 0; ; attempt++) {
            for (Keep level : Keep.values()) {
                String tried = wrapToWidth(text, width, measure, level, reach);
                if (lines(tried) <= rows) {
                    keep = level;
                    wrapped = tried;
                    break;
                }
            }
            if (wrapped != null || attempt >= WRAP_RETRIES) {
                break;
            }
            width = width * 11 / 10;
        }
        if (wrapped == null) {
            wrapped = wrapToWidth(text, width, measure, Keep.NONE, reach);
        }
        if (lines(wrapped) == 2 && measure.applyAsInt(text) <= width * SNUG / 100) {
            return text;
        }
        String head = labelBreak(text, width, rows, measure, keep, reach);
        if (head != null && lines(head) <= lines(wrapped)) {
            return head;
        }
        return balance(text, wrapped, width, measure, keep, reach);
    }

    private static String labelBreak(String text, int width, int rows,
                                     ToIntFunction<String> measure, Keep keep,
                                     boolean reach) {
        int colon = text.indexOf(": ");
        if (colon <= 0 || colon > MAX_LABEL_LENGTH || text.indexOf(NEWLINE) >= 0) {
            return null;
        }
        String label = text.substring(0, colon + 1);
        if (measure.applyAsInt(label) > width) {
            return null;
        }
        String body = text.substring(colon + 2);
        String rest = wrapToWidth(body, width, measure, keep, reach);
        rest = balance(body, rest, width, measure, keep, reach);
        return 1 + lines(rest) > rows ? null : label + NEWLINE + rest;
    }

    private static final int SNUG = 125;

    private static String balance(String text, String wrapped, int width,
                                  ToIntFunction<String> measure, Keep keep, boolean reach) {
        int rows = lines(wrapped);
        if (rows < 2) {
            return wrapped;
        }
        String best = wrapped;
        int bestCost = wrapCost(text, wrapped, keep);
        int at = width;
        for (int step = 0; step < BALANCE_STEPS; step++) {
            int narrower = at * 19 / 20;
            if (narrower <= 0 || narrower == at) {
                break;
            }
            String tighter = wrapToWidth(text, narrower, measure, keep, reach);
            if (lines(tighter) != rows) {
                break;
            }
            if (splitsWord(tighter) && !splitsWord(wrapped)) {
                break;
            }
            at = narrower;
            int cost = wrapCost(text, tighter, keep);
            if (cost <= bestCost) {
                best = tighter;
                bestCost = cost;
            }
        }
        return best;
    }

    private static int wrapCost(String text, String wrapped, Keep keep) {
        return unitsSplit(text, wrapped, keep) * 10 + breakCost(wrapped);
    }

    static int unitsSplit(String text, String wrapped, Keep keep) {
        List<List<int[]>> units = valueUnits(text, keep);
        if (units.isEmpty()) {
            return 0;
        }
        int count = 0;
        int t = 0;
        for (int w = 0; w < wrapped.length() && t <= text.length(); w++) {
            if (wrapped.charAt(w) != NEWLINE || (t < text.length() && text.charAt(t) == NEWLINE)) {
                t++;
                continue;
            }
            for (List<int[]> layer : units) {
                for (int[] unit : layer) {
                    if (unit[0] < t && t < unit[1]) {
                        count++;
                    }
                }
            }
            if (t < text.length() && text.charAt(t) == ' '
                    && (w + 1 >= wrapped.length() || wrapped.charAt(w + 1) != ' '
                        || (t + 1 < text.length() && text.charAt(t + 1) == ' '))) {
                t++;
            }
        }
        return count;
    }

    static int breakCost(String wrapped) {
        int cost = 0;
        for (int at = wrapped.indexOf(NEWLINE); at >= 0; at = wrapped.indexOf(NEWLINE, at + 1)) {
            int prev = at;
            while (prev > 0 && wrapped.charAt(prev - 1) == ' ') {
                prev--;
            }
            int next = at + 1;
            while (next < wrapped.length() && wrapped.charAt(next) == ' ') {
                next++;
            }
            if (prev == 0 || next >= wrapped.length() || wrapped.charAt(prev - 1) == NEWLINE
                    || wrapped.charAt(next) == NEWLINE) {
                continue;
            }
            char before = wrapped.charAt(prev - 1);
            char after = wrapped.charAt(next);
            if (cannotStartLine(after) || cannotEndLine(before)) {
                cost += 4;
            } else if (after == '(' || after == '（') {
                cost += 2;
            } else if (CLAUSE_END.indexOf(before) < 0 && ",;:".indexOf(before) < 0) {
                cost += 1;
            }
        }
        return cost;
    }

    private static boolean splitsWord(String wrapped) {
        for (int at = wrapped.indexOf(NEWLINE); at > 0 && at + 1 < wrapped.length();
                at = wrapped.indexOf(NEWLINE, at + 1)) {
            if (isWordChar(wrapped.charAt(at - 1)) && isWordChar(wrapped.charAt(at + 1))) {
                return true;
            }
        }
        return false;
    }

    private static final int BALANCE_STEPS = 20;

    private static final int WRAP_RETRIES = 5;

    private static int widestRow(String wrapped, ToIntFunction<String> measure) {
        int widest = 0;
        for (String row : wrapped.split(NL, -1)) {
            widest = Math.max(widest, measure.applyAsInt(row));
        }
        return widest;
    }

    private static int lines(String text) {
        int n = 1;
        for (int i = text.indexOf(NEWLINE); i >= 0; i = text.indexOf(NEWLINE, i + 1)) {
            n++;
        }
        return n;
    }

    static String wrapToWidth(String text, int maxPx, ToIntFunction<String> measure) {
        return wrapToWidth(text, maxPx, measure, Keep.NOTE);
    }

    static String wrapToWidth(String text, int maxPx, ToIntFunction<String> measure,
                              Keep keep) {
        return wrapToWidth(text, maxPx, measure, keep, true);
    }

    private static String wrapToWidth(String text, int maxPx, ToIntFunction<String> measure,
                                      Keep keep, boolean reach) {
        if (maxPx <= 0) {
            return text;
        }
        List<List<int[]>> units = valueUnits(text, keep);
        StringBuilder out = new StringBuilder(text.length() + 8);
        int lineStart = 0;
        int width = 0;
        int lastSpace = -1;
        int lastClause = -1;
        int i = 0;
        while (i < text.length()) {
            int at = i;
            if ((text.charAt(i) == '+' || text.charAt(i) == '-')
                    && i + 1 < text.length() && text.charAt(i + 1) == '{') {
                at = i + 1;
            }
            int close = text.charAt(at) == '{' ? text.indexOf('}', at) : -1;
            int end = close > at
                    ? close + 1
                    : i + Character.charCount(text.codePointAt(i));
            String piece = text.substring(i, end);
            int pieceWidth = measure.applyAsInt(piece);
            if (width + pieceWidth > maxPx && i > lineStart) {
                int cut = breaksWord(text, i) && lastSpace > lineStart ? lastSpace : i;
                int clause = clauseBreak(text, lineStart, i, lastClause, maxPx, measure,
                                         reach);
                if (clause > lineStart) {
                    cut = clause;
                }
                cut = avoidOrphan(text, cut, lineStart);
                cut = keepUnitWhole(text, units, cut, lineStart);
                cut = keepRangeWhole(text, cut, lineStart);
                int run = keepLatinRunWhole(text, cut, lineStart);
                if (run != cut) {
                    cut = keepGlyphWithWord(text, run, lineStart);
                }
                cut = keepOpenerWithNext(text, cut, lineStart);
                out.append(text, lineStart, cut).append(NEWLINE);
                boolean atSpace = cut < text.length() && text.charAt(cut) == ' ';
                lineStart = atSpace ? cut + 1 : cut;
                lastSpace = -1;
                lastClause = -1;
                if (atSpace && cut == i) {
                    width = 0;
                    i = end;
                    continue;
                }
                if (lineStart > i) {
                    width = 0;
                    i = lineStart;
                    continue;
                }
                width = measure.applyAsInt(text.substring(lineStart, i));
                for (int k = lineStart; k < i; k++) {
                    if (text.charAt(k) == ' ') {
                        lastSpace = k;
                    } else if (CLAUSE_END.indexOf(text.charAt(k)) >= 0) {
                        lastClause = k + 1;
                    }
                }
            }
            if (" ".equals(piece)) {
                lastSpace = i;
            }
            if (piece.length() == 1 && CLAUSE_END.indexOf(piece.charAt(0)) >= 0) {
                lastClause = end;
            }
            width += pieceWidth;
            i = end;
        }
        return out.append(text, lineStart, text.length()).toString();
    }

    private static int clauseBreak(String text, int lineStart, int at, int lastClause,
                                   int maxPx, ToIntFunction<String> measure, boolean reach) {
        int ahead = reach ? clauseAhead(text, lineStart, at, maxPx, measure) : lineStart;
        if (ahead > lineStart) {
            return ahead;
        }
        if (lastClause <= lineStart || lastClause >= at) {
            return lineStart;
        }
        int filled = measure.applyAsInt(text.substring(lineStart, lastClause));
        return filled * 100 >= maxPx * CLAUSE_FILL ? lastClause : lineStart;
    }

    private static int clauseAhead(String text, int lineStart, int at,
                                   int maxPx, ToIntFunction<String> measure) {
        int limit = Math.min(text.length(), at + CLAUSE_LOOKAHEAD);
        for (int i = at; i < limit; i++) {
            if (CLAUSE_END.indexOf(text.charAt(i)) < 0) {
                continue;
            }
            int end = i + 1;
            if (end >= text.length()) {
                return lineStart;
            }
            int wide = measure.applyAsInt(text.substring(lineStart, end));
            return wide * 100 <= maxPx * (100 + CLAUSE_REACH) ? end : lineStart;
        }
        return lineStart;
    }

    private static final String CLAUSE_END = "，。、；：！？》」』）】…";

    private static final int CLAUSE_FILL = 60;

    private static final int CLAUSE_REACH = 20;

    private static final int CLAUSE_LOOKAHEAD = 6;

    private static int avoidOrphan(String text, int cut, int lineStart) {
        int at = cut;
        while (at > lineStart + 1 && at < text.length() && cannotStartLine(text.charAt(at))) {
            at--;
        }
        while (at < cut && at > lineStart + 1 && breaksWord(text, at)) {
            at--;
        }
        at = keepGlyphWithWord(text, at, lineStart);
        at = keepValueWithGlyph(text, at, lineStart);
        int outside = outsidePlaceholder(text, at, lineStart);
        if (outside != at) {
            at = outside > lineStart ? outside : cut;
        }
        return at > lineStart ? at : cut;
    }

    private static int keepOpenerWithNext(String text, int cut, int lineStart) {
        int at = cut;
        while (at > lineStart && cannotEndLine(text.charAt(at - 1))) {
            at--;
        }
        if (at == cut) {
            return cut;
        }
        if (at > lineStart + 1 && text.charAt(at - 1) == ' ') {
            at--;
        }
        return at > lineStart ? at : cut;
    }

    private static boolean cannotEndLine(char c) {
        return "(（[［「『【〈《〔".indexOf(c) >= 0;
    }

    private static int outsidePlaceholder(String text, int cut, int lineStart) {
        int open = text.lastIndexOf('{', cut - 1);
        if (open < lineStart) {
            return cut;
        }
        int close = text.indexOf('}', open);
        return close >= cut ? open : cut;
    }

    private static int keepGlyphWithWord(String text, int cut, int lineStart) {
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        int at = cut;
        while (true) {
            int back = at;
            while (back > lineStart && text.charAt(back - 1) == ' ') {
                back--;
            }
            if (back - glyph.length() < lineStart
                    || !text.startsWith(glyph, back - glyph.length())) {
                return at > lineStart ? at : cut;
            }
            at = back - glyph.length();
        }
    }

    static int keepValueWithGlyph(String text, int cut, int lineStart) {
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        if (!text.startsWith(glyph, cut)) {
            return cut;
        }
        int back = cut;
        while (back > lineStart && text.charAt(back - 1) == ' ') {
            back--;
        }
        if (back == cut || back <= lineStart || text.charAt(back - 1) != '}') {
            return cut;
        }
        int open = text.lastIndexOf('{', back - 1);
        if (open < lineStart || !NUMBER_SLOT.matcher(text.substring(open, back)).matches()) {
            return cut;
        }
        if (open > lineStart && (text.charAt(open - 1) == '+' || text.charAt(open - 1) == '-')) {
            open--;
        }
        return open > lineStart ? open : cut;
    }

    enum Keep {
        NOTE,
        VALUE,
        NONE
    }

    private static final int LABEL_MAX = 6;

    private static final int NOTE_MAX = 16;

    static List<List<int[]>> valueUnits(String text, Keep keep) {
        if (keep == Keep.NONE) {
            return List.of();
        }
        List<int[]> values = new ArrayList<>();
        List<int[]> notes = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            int end = valueEnd(text, i);
            if (end <= i) {
                i++;
                continue;
            }
            int start = i;
            int stop = labelAfter(text, end);
            if (stop == end) {
                start = labelBefore(text, i);
            }
            values.add(new int[] {start, stop});
            notes.add(new int[] {start, noteAfter(text, stop)});
            i = end;
        }
        return keep == Keep.VALUE ? List.of(merged(values))
                                  : List.of(merged(notes), merged(values));
    }

    private static List<int[]> merged(List<int[]> spans) {
        spans.sort(java.util.Comparator.comparingInt(s -> s[0]));
        List<int[]> out = new ArrayList<>();
        for (int[] span : spans) {
            int[] last = out.isEmpty() ? null : out.get(out.size() - 1);
            if (last != null && span[0] < last[1]) {
                last[1] = Math.max(last[1], span[1]);
            } else {
                out.add(new int[] {span[0], span[1]});
            }
        }
        return out;
    }

    private static int keepUnitWhole(String text, List<List<int[]>> units, int cut,
                                     int lineStart) {
        for (List<int[]> layer : units) {
            for (int[] unit : layer) {
                if (unit[0] < cut && cut < unit[1]) {
                    if (unit[0] > lineStart) {
                        return keepGlyphWithWord(text, unit[0], lineStart);
                    }
                    break;
                }
            }
        }
        return cut;
    }

    static int keepRangeWhole(String text, int cut, int lineStart) {
        if (cut <= lineStart || cut >= text.length()) {
            return cut;
        }
        java.util.regex.Matcher m = NUMBER_RANGE.matcher(text);
        while (m.find()) {
            if (m.start() >= cut) {
                break;
            }
            if (cut < m.end()) {
                return m.start() > lineStart ? keepGlyphWithWord(text, m.start(), lineStart)
                                             : m.end();
            }
        }
        return cut;
    }

    private static final java.util.regex.Pattern NUMBER_SLOT = java.util.regex.Pattern.compile("\\{~\\d*\\}");

    private static final java.util.regex.Pattern NUMBER_RANGE = java.util.regex.Pattern.compile(
            "[+\\-−]?(?:\\{~\\d*}|\\d+(?:[.,]\\d+)*)%?"
            + "(?:[\\-+–−](?:\\{~\\d*}|\\d+(?:[.,]\\d+)*)%?)+");

    static int keepLatinRunWhole(String text, int cut, int lineStart) {
        if (cut <= lineStart || cut >= text.length() || !hasCjkLetter(text)) {
            return cut;
        }
        int left = cut;
        while (left > lineStart && text.charAt(left - 1) == ' ') {
            left--;
        }
        int right = cut;
        while (right < text.length() && text.charAt(right) == ' ') {
            right++;
        }
        if (left <= lineStart || right >= text.length() || right - left != 1
                || !isWordChar(text.charAt(left - 1)) || !isWordChar(text.charAt(right))) {
            return cut;
        }
        int start = left;
        while (start > lineStart) {
            char c = text.charAt(start - 1);
            if (isWordChar(c)) {
                start--;
            } else if (c == ' ' && start - 2 >= lineStart && isWordChar(text.charAt(start - 2))) {
                start--;
            } else {
                break;
            }
        }
        while (start > lineStart && cannotEndLine(text.charAt(start - 1))) {
            start--;
        }
        if (start <= lineStart) {
            return cut;
        }
        return text.charAt(start - 1) == ' ' && start - 1 > lineStart ? start - 1 : start;
    }

    private static boolean hasCjkLetter(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (isCjkLetter(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static int valueEnd(String text, int i) {
        int n = text.length();
        char c = text.charAt(i);
        boolean signed = false;
        int j = i;
        if (c == '+' || c == '-' || c == '−' || c == 'x' || c == '×') {
            if (c != '+' && i > 0 && isWordChar(text.charAt(i - 1))) {
                return -1;
            }
            signed = true;
            j++;
        }
        int k = numberEnd(text, j);
        if (k <= j) {
            return -1;
        }
        boolean placeholder = text.charAt(j) == '{';
        if (k < n && (text.charAt(k) == '%' || text.charAt(k) == '‰')) {
            k++;
            signed = true;
        }
        if (!signed && !placeholder) {
            return -1;
        }
        if (k < n && text.charAt(k) == '/') {
            int per = numberEnd(text, k + 1);
            int from = per > k + 1 ? per : k + 1;
            int letters = unitLetters(text, from);
            if (letters > k + 1) {
                k = letters;
            }
        }
        return unitLetters(text, k);
    }

    private static int unitLetters(String text, int from) {
        int k = from;
        while (k < text.length() && k - from < MAX_UNIT && isUnitLetter(text.charAt(k))) {
            k++;
        }
        return k < text.length() && Character.isLetter(text.charAt(k)) ? from : k;
    }

    private static int numberEnd(String text, int j) {
        int n = text.length();
        if (j >= n) {
            return -1;
        }
        if (text.startsWith("{~", j)) {
            int close = text.indexOf('}', j);
            if (close < 0) {
                return -1;
            }
            for (int d = j + 2; d < close; d++) {
                if (text.charAt(d) < '0' || text.charAt(d) > '9') {
                    return -1;
                }
            }
            return close + 1;
        }
        int k = j;
        while (k < n && text.charAt(k) >= '0' && text.charAt(k) <= '9') {
            k++;
            if (k + 1 < n && (text.charAt(k) == '.' || text.charAt(k) == ',')
                    && text.charAt(k + 1) >= '0' && text.charAt(k + 1) <= '9') {
                k++;
            }
        }
        return k > j ? k : -1;
    }

    private static int labelAfter(String text, int end) {
        int n = text.length();
        int j = end;
        if (j < n && text.charAt(j) == ' ') {
            j++;
        }
        int glyphEnd = -1;
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        if (text.startsWith(glyph, j)) {
            j += glyph.length();
            glyphEnd = j;
            if (j < n && text.charAt(j) == ' ') {
                j++;
            }
        }
        if (j < n && isCjkLetter(text.charAt(j))) {
            int k = j;
            while (k < n && k - j < LABEL_MAX && isCjkLetter(text.charAt(k))) {
                k++;
            }
            return k;
        }
        if (j < n && isLatinLetter(text.charAt(j))) {
            int k = wordEndAt(text, j);
            if (k - j <= 2 && k + 1 < n && text.charAt(k) == ' '
                    && isLatinLetter(text.charAt(k + 1))) {
                k = wordEndAt(text, k + 1);
            }
            return k;
        }
        return glyphEnd > 0 ? glyphEnd : end;
    }

    private static int labelBefore(String text, int start) {
        int j = start;
        if (j > 0 && text.charAt(j - 1) == ' ') {
            j--;
        }
        int k = j;
        if (k > 0 && isCjkLetter(text.charAt(k - 1))) {
            while (k > 0 && isCjkLetter(text.charAt(k - 1))) {
                k--;
            }
            if (j - k > LABEL_MAX) {
                return start;
            }
            String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
            if (k >= glyph.length() && text.startsWith(glyph, k - glyph.length())) {
                k -= glyph.length();
            }
        } else if (k > 0 && isLatinLetter(text.charAt(k - 1))) {
            while (k > 0 && isWordChar(text.charAt(k - 1))) {
                k--;
            }
        } else {
            return start;
        }
        if (k > 0 && (text.charAt(k - 1) == '(' || text.charAt(k - 1) == '（')) {
            k--;
        }
        return k;
    }

    private static int noteAfter(String text, int stop) {
        int j = stop;
        if (j < text.length() && text.charAt(j) == ' ') {
            j++;
        }
        if (j >= text.length() || (text.charAt(j) != '(' && text.charAt(j) != '（')) {
            return stop;
        }
        int close = text.indexOf(text.charAt(j) == '(' ? ')' : '）', j);
        return close > j && close - j <= NOTE_MAX ? close + 1 : stop;
    }

    private static int wordEndAt(String text, int from) {
        int k = from;
        while (k < text.length() && isWordChar(text.charAt(k))) {
            k++;
        }
        return k;
    }

    private static boolean isCjkLetter(char c) {
        return c >= 0x2E80 && Character.isLetter(c);
    }

    private static boolean isLatinLetter(char c) {
        return c < 0x2E80 && Character.isLetter(c);
    }

    private static boolean cannotStartLine(char c) {
        return "，。、；：！？）」』】〉》%‰°′″…・)]".indexOf(c) >= 0;
    }

    private static boolean breaksWord(String text, int at) {
        return at > 0 && at < text.length()
                && isWordChar(text.charAt(at - 1)) && isWordChar(text.charAt(at));
    }

    static boolean isWordChar(char c) {
        return c < 0x2E80 && (Character.isLetterOrDigit(c) || c == '-' || c == '\'');
    }

    static final int MAX_UNIT = 3;

    static boolean isUnitLetter(char c) {
        return c >= 'a' && c <= 'z';
    }
}
