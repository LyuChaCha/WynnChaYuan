package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.translate.ColumnAligner.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

final class TermSubstitution {

    private TermSubstitution() {}

    static String breakBeforeOriginal(String text, int maxPx, TranslationStore store,
                                      ToIntFunction<String> measure) {
        if (store == null || maxPx <= 0 || text.indexOf('\n') >= 0) {
            return text;
        }
        int at = store.appendedOriginalAt(text);
        if (at < 0 || measure.applyAsInt(text) <= maxPx) {
            return text;
        }
        int close = text.lastIndexOf(')') + 1;
        return text.substring(0, at) + text.substring(close) + "\n"
                + text.substring(at + 1, close);
    }

    static String termsWithin(String text, TranslationStore store) {
        List<int[]> kept = store == null ? List.of() : store.keptOriginals(text);

        StringBuilder out = new StringBuilder();
        int from = 0;
        boolean any = false;
        TranslationStore.Term term;
        while (from < text.length() && (term = termOutside(store, text, from, kept)) != null) {
            String before = text.substring(from, term.start());
            if (dropsSpaceBefore(before, term.translation())) {
                before = before.substring(0, before.length() - 1);
            }
            out.append(before).append(term.translation());
            from = term.end();
            if (dropsSpaceAfter(term.translation(), text, from)) {
                from++;
            }
            any = true;
        }
        return any ? out.append(text.substring(Math.min(from, text.length()))).toString()
                   : null;
    }

    static String[] forcedTerms(String[] lines, TranslationStore store) {
        if (store == null || lines.length == 0) {
            return lines;
        }
        String joined = String.join(String.valueOf(LineTranslator.NEWLINE), lines);
        if (joined.indexOf("{c") < 0 && joined.indexOf("{w") < 0) {
            return lines;
        }
        StringBuilder plain = new StringBuilder(joined.length());
        int[] origin = new int[joined.length()];
        boolean[] inSpan = new boolean[joined.length()];
        boolean forced = false;
        for (int i = 0; i < joined.length(); ) {
            ColourTemplate.Token colour = ColourTemplate.colourToken(joined, i);
            if (colour != null) {
                forced = colour.index() != 0;
                i += colour.text().length();
                continue;
            }
            origin[plain.length()] = i;
            inSpan[plain.length()] = forced;
            plain.append(joined.charAt(i++));
        }
        String text = plain.toString();
        List<int[]> kept = store.keptOriginals(text);
        List<int[]> cuts = new ArrayList<>();
        List<String> with = new ArrayList<>();
        int from = 0;
        TranslationStore.Term term;
        while (from < text.length() && (term = termOutside(store, text, from, kept)) != null) {
            from = term.end();
            int start = term.start();
            int end = term.end();
            boolean whole = true;
            for (int k = start; k < end && whole; k++) {
                whole = inSpan[k];
            }
            if (!whole || origin[end - 1] - origin[start] != end - 1 - start) {
                continue;
            }
            if (dropsSpaceBefore(text.substring(0, start), term.translation())) {
                cuts.add(new int[] {origin[start - 1], origin[start - 1] + 1});
                with.add("");
            }
            cuts.add(new int[] {origin[start], origin[end - 1] + 1});
            with.add(term.translation());
            if (dropsSpaceAfter(term.translation(), text, end)) {
                cuts.add(new int[] {origin[end], origin[end] + 1});
                with.add("");
                from = end + 1;
            }
        }
        if (cuts.isEmpty()) {
            return lines;
        }
        StringBuilder out = new StringBuilder(joined);
        for (int k = cuts.size() - 1; k >= 0; k--) {
            out.replace(cuts.get(k)[0], cuts.get(k)[1], with.get(k));
        }
        return out.toString().split(String.valueOf(LineTranslator.NEWLINE), -1);
    }

    static TranslationStore.Term termOutside(TranslationStore store, String text,
                                             int from, List<int[]> kept) {
        int at = from;
        while (at < text.length()) {
            TranslationStore.Term term = store.findTerm(text, at);
            if (term == null) {
                return null;
            }
            int[] in = overlapping(kept, term.start(), term.end());
            if (in == null) {
                return term;
            }
            at = Math.max(term.start() + 1, in[1]);
        }
        return null;
    }

    static int[] overlapping(List<int[]> spans, int start, int end) {
        for (int[] span : spans) {
            if (start < span[1] && end > span[0]) {
                return span;
            }
        }
        return null;
    }

    static List<LineParts.Piece> withTranslations(List<LineParts.Piece> accents,
                                                  String translated,
                                                  TranslationStore store) {
        if (store == null || accents.isEmpty()) {
            return accents;
        }
        List<LineParts.Piece> out = new ArrayList<>(accents);
        for (LineParts.Piece accent : accents) {
            String core = accent.text().strip();
            if (core.isEmpty()) {
                continue;
            }
            LineTranslator.addStem(out, core, accent.style(), translated);
            String zh = store.lookup(core);
            if (zh == null || zh.isBlank()) {
                zh = store.lookupTerm(core);
            }
            if (zh == null || zh.isBlank()) {
                zh = lookupWordCore(core, store);
            }
            if (zh != null && !zh.isBlank() && !zh.equals(core)) {
                String wrapped = wrapLikeSource(core, zh, translated);
                out.add(new LineParts.Piece(wrapped != null ? wrapped : zh, accent.style()));
            }
        }
        return out;
    }

    static String wrapLikeSource(String core, String zh, String translated) {
        if (core == null || zh == null || translated == null
                || core.length() < 3 || zh.isBlank()) {
            return null;
        }
        if (core.charAt(0) != '[' || core.charAt(core.length() - 1) != ']') {
            return null;
        }
        String inner = core.substring(1, core.length() - 1);
        if (inner.isBlank() || inner.indexOf('[') >= 0 || inner.indexOf(']') >= 0) {
            return null;
        }
        if (zh.indexOf('[') >= 0 || zh.indexOf(']') >= 0) {
            return null;
        }
        String wrapped = "[" + zh + "]";
        if (translated.contains(wrapped)) {
            return wrapped;
        }
        return countedWrap(zh, translated);
    }

    private static String countedWrap(String zh, String translated) {
        int at = 0;
        while ((at = translated.indexOf('[', at)) >= 0) {
            int close = translated.indexOf(']', at + 1);
            if (close < 0) {
                return null;
            }
            String inner = translated.substring(at + 1, close);
            if (inner.endsWith(zh)
                    && onlyCount(inner.substring(0, inner.length() - zh.length()))) {
                return translated.substring(at, close + 1);
            }
            at = close + 1;
        }
        return null;
    }

    private static boolean onlyCount(String lead) {
        if (lead.isBlank()) {
            return false;
        }
        String bare = lead.replace(com.wynnchayuan.capture.GlyphSplitter.NUMBER_PLACEHOLDER, "")
                          .replace(" ", "");
        for (int i = 0; i < bare.length(); i++) {
            if (!Character.isDigit(bare.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    static String lookupWordCore(String text, TranslationStore store) {
        int from = 0;
        int to = text.length();
        while (from < to && !Character.isLetterOrDigit(text.charAt(from))) {
            from++;
        }
        while (to > from && !Character.isLetterOrDigit(text.charAt(to - 1))) {
            to--;
        }
        if (from == 0 && to == text.length()) {
            return null;
        }
        String core = text.substring(from, to).strip();
        if (core.isEmpty()) {
            return null;
        }
        String zh = store.lookup(core);
        if (zh == null || zh.isBlank()) {
            zh = store.lookupTerm(core);
        }
        return zh == null || zh.isBlank() ? withoutCount(core, store) : zh;
    }

    private static String withoutCount(String core, TranslationStore store) {
        int at = 0;
        while (at < core.length() && Character.isDigit(core.charAt(at))) {
            at++;
        }
        if (at > 0) {
            while (at + 1 < core.length() && (core.charAt(at) == '.' || core.charAt(at) == ',')
                    && Character.isDigit(core.charAt(at + 1))) {
                at++;
                while (at < core.length() && Character.isDigit(core.charAt(at))) {
                    at++;
                }
            }
            if (at < core.length() && core.charAt(at) == '%') {
                at++;
            }
        }
        if (at == 0 || at >= core.length() || core.charAt(at) != ' ') {
            return null;
        }
        String rest = core.substring(at + 1).strip();
        if (rest.isEmpty() || !com.wynnchayuan.capture.GlyphSplitter.hasLetter(rest)) {
            return null;
        }
        String zh = store.lookup(rest);
        if (zh == null || zh.isBlank()) {
            zh = store.lookupTerm(rest);
        }
        return zh == null || zh.isBlank() ? asSingular(rest, store) : zh;
    }

    private static String asSingular(String plural, TranslationStore store) {
        List<String> tries = new ArrayList<>(2);
        if (plural.endsWith("ies") && plural.length() > 3) {
            tries.add(plural.substring(0, plural.length() - 3) + "y");
        } else if (plural.endsWith("es") && plural.length() > 2) {
            tries.add(plural.substring(0, plural.length() - 2));
            tries.add(plural.substring(0, plural.length() - 1));
        } else if (plural.endsWith("s") && !plural.endsWith("ss")
                && plural.length() > 1) {
            tries.add(plural.substring(0, plural.length() - 1));
        }
        for (String one : tries) {
            String zh = store.lookup(one);
            if (zh == null || zh.isBlank()) {
                zh = store.lookupTerm(one);
            }
            if (zh != null && !zh.isBlank()) {
                return zh;
            }
        }
        return null;
    }

    static boolean dropsSpaceBefore(String before, String translation) {
        return before.length() >= 2
                && before.charAt(before.length() - 1) == ' '
                && isHan(before.charAt(before.length() - 2))
                && !translation.isEmpty() && isHan(translation.charAt(0));
    }

    static boolean dropsSpaceAfter(String translation, String text, int at) {
        return !translation.isEmpty() && isHan(translation.charAt(translation.length() - 1))
                && at + 1 < text.length()
                && text.charAt(at) == ' '
                && isHan(text.charAt(at + 1));
    }

    static boolean isPictograph(char c) {
        return c > 0x7F && !isHan(c) && !Character.isLetterOrDigit(c)
                && !Character.isWhitespace(c);
    }

    static boolean isHan(char c) {
        Character.UnicodeScript script = Character.UnicodeScript.of(c);
        return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA
                || (c >= '＀' && c <= '￯')
                || (c >= '　' && c <= '〿');
    }
}
