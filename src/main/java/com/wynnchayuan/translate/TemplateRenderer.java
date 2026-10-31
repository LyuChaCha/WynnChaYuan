package com.wynnchayuan.translate;

import com.wynnchayuan.capture.ColourAudit;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.capture.UnappliedAudit;
import com.wynnchayuan.translate.ColumnAligner.Piece;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

final class TemplateRenderer {

    private TemplateRenderer() {}

    static Component rebuild(String translated, LineParts parts,
                             TranslationStore store) {
        List<Component> one = rebuildAll(new String[] {translated}, List.of(parts), List.of(), store);
        return one == null ? null : one.get(0);
    }

    private static void noteUnapplied(List<LineParts> parts, TranslationStore store) {
        if (store == null || parts.isEmpty()) {
            return;
        }
        StringBuilder src = new StringBuilder();
        for (LineParts part : parts) {
            if (src.length() > 0) {
                src.append(LineTranslator.NEWLINE);
            }
            src.append(part.template());
        }
        String key = src.toString().strip();
        if (UnappliedAudit.asked(key)) {
            return;
        }
        String corpus = store.lookup(key);
        if (corpus != null && !corpus.isBlank()) {
            UnappliedAudit.note(key, corpus);
        }
    }

    static Component rebuild(String translated, LineParts parts,
                             List<LineParts.Piece> glyphs, TranslationStore store) {
        List<Component> one = rebuildAll(new String[] {translated}, List.of(parts),
                                         List.of(), glyphs, null, store);
        return one == null ? null : one.get(0);
    }

    static List<LineParts.Piece> unwrappedGlyphs(
            String template, List<LineParts.Piece> glyphs) {
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        List<LineParts.Piece> kept = new ArrayList<>();
        int at = 0;
        int index = 0;
        boolean lineStart = false;
        while (at < template.length() && index < glyphs.size()) {
            if (template.startsWith(glyph, at)) {
                if (!lineStart) {
                    kept.add(glyphs.get(index));
                }
                index++;
                at += glyph.length();
                continue;
            }
            char c = template.charAt(at);
            if (c == '\n') {
                lineStart = true;
            } else if (!Character.isWhitespace(c)) {
                lineStart = false;
            }
            at++;
        }
        while (index < glyphs.size()) {
            kept.add(glyphs.get(index++));
        }
        return kept;
    }

    private static List<Component> rebuildAll(String[] translated, List<LineParts> parts,
                                              List<LineParts.Piece> extraAccents,
                                              TranslationStore store) {
        return rebuildAll(translated, parts, extraAccents, null, null, store);
    }

    static List<Component> rebuildAll(String[] translated, List<LineParts> parts,
                                      List<LineParts.Piece> extraAccents,
                                      List<LineParts.Piece> overrideGlyphs,
                                      List<LineParts.Piece> overridePlaces,
                                      TranslationStore store) {
        List<LineParts.Piece> glyphs = new ArrayList<>();
        List<LineParts.Piece> places = new ArrayList<>();
        List<LineParts.Piece> numbers = new ArrayList<>();
        List<LineParts.Piece> users = new ArrayList<>();
        List<LineParts.Piece> accents = new ArrayList<>(extraAccents);
        Style blockStyle = BlockTranslator.dominantStyle(parts);
        List<LineParts.Piece> allRuns = new ArrayList<>();
        for (LineParts part : parts) {
            glyphs.addAll(part.glyphs());
            places.addAll(part.places());
            numbers.addAll(part.numbers());
            users.addAll(part.users());
            allRuns.addAll(part.runs());
        }
        if (overridePlaces != null) {
            places = new ArrayList<>(overridePlaces);
        }
        if (overrideGlyphs != null) {
            glyphs = new ArrayList<>(overrideGlyphs);
        }
        accents.addAll(LineParts.accentsAgainst(allRuns, blockStyle));
        accents = TermSubstitution.withTranslations(accents, String.join(String.valueOf(LineTranslator.NEWLINE), translated), store);
        List<LineParts.Piece> segmented = StyledRebuilder.segmentAccents(parts, translated, blockStyle, accents);
        if (segmented != null) {
            accents.addAll(segmented);
        } else {
            accents.addAll(StyledRebuilder.labelValueAccents(parts, translated, blockStyle, accents));
        }
        accents.addAll(StyledRebuilder.wholeLineAccents(parts, allRuns, translated, blockStyle, accents));
        accents.addAll(StyledRebuilder.bracketAccents(allRuns, translated, blockStyle));

        String[] flowed = keepAccentsWhole(translated, accents);
        accents.addAll(halvesAcrossBreaks(flowed, accents));
        flowed = TermSubstitution.forcedTerms(flowed, store);

        List<List<ColourTemplate.Token>> lines = new ArrayList<>(flowed.length);
        long wantGlyphs = 0;
        long wantPlaces = 0;
        long wantNumbers = 0;
        long wantUsers = 0;
        int numbered = 0;
        for (String line : flowed) {
            List<ColourTemplate.Token> tokens = ColourTemplate.tokenize(line);
            lines.add(tokens);
            for (ColourTemplate.Token token : tokens) {
                switch (token.kind()) {
                    case GLYPH -> wantGlyphs++;
                    case PLACE -> wantPlaces++;
                    case NUMBER -> {
                        if (token.index() == 0) {
                            wantNumbers++;
                        } else {
                            numbered = Math.max(numbered, token.index());
                        }
                    }
                    case USER -> wantUsers++;
                    default -> { }
                }
            }
        }
        boolean numbersOk = numbered == 0
                ? wantNumbers == numbers.size()
                : wantNumbers <= numbers.size() && numbered <= numbers.size();
        if (wantGlyphs != glyphs.size() || wantPlaces != places.size()
                || !numbersOk || wantUsers > users.size()) {
            noteUnapplied(parts, store);
            return null;
        }

        Style textStyle = DisplayStyles.forDisplay(blockStyle);
        Style symbolStyle = leadingSymbolStyle(allRuns);
        if (symbolStyle == null) {
            symbolStyle = SpaceOffset.isSpaceFont(blockStyle) ? textStyle : blockStyle;
        }
        Style noteStyle = BlockTranslator.noteStyleOf(allRuns, blockStyle);
        boolean[] inNote = {false};
        boolean[] usedAccent = new boolean[accents.size()];
        int glyph = 0;
        int place = 0;
        int number = 0;
        int user = 0;

        List<Style> palette = ColourTemplate.palette(allRuns);
        List<Style> words = ColourTemplate.wordPalette(allRuns);
        List<ColourTemplate.Bar> bars = ColourTemplate.bars(allRuns);
        boolean[] usedBar = new boolean[bars.size()];
        FlowedDebug.palette(palette, allRuns, flowed);

        Style forced = null;
        List<Component> out = new ArrayList<>(lines.size());
        for (List<ColourTemplate.Token> tokens : lines) {
            MutableComponent line = Component.empty();
            Style justFilled = null;
            boolean afterNumber = false;
            for (int i = 0; i < tokens.size(); i++) {
                ColourTemplate.Token token = tokens.get(i);
                switch (token.kind()) {
                    case COLOR -> forced = ColourTemplate.colourOf(token, palette, words, textStyle);
                    case GLYPH -> {
                        LineParts.Piece piece = glyphs.get(glyph++);
                        line.append(Component.literal(piece.text()).withStyle(piece.style()));
                        justFilled = null;
                        afterNumber = false;
                    }
                    case PLACE -> {
                        LineParts.Piece piece = places.get(place++);
                        line.append(DisplayStyles.literal(piece.text(), DisplayStyles.forDisplay(piece.style())));
                        justFilled = piece.style();
                        afterNumber = false;
                    }
                    case NUMBER -> {
                        int at = token.index() > 0 ? token.index() - 1 : number++;
                        if (at < 0 || at >= numbers.size()) {
                            return null;
                        }
                        LineParts.Piece piece = numbers.get(at);
                        line.append(DisplayStyles.literal(piece.text(), DisplayStyles.numberDisplay(piece.style())));
                        justFilled = piece.style();
                        afterNumber = true;
                    }
                    case USER -> {
                        LineParts.Piece piece = users.get(user++);
                        line.append(DisplayStyles.literal(piece.text(), DisplayStyles.forDisplay(piece.style())));
                        justFilled = piece.style();
                        afterNumber = false;
                    }
                    case TEXT -> {
                        String text = token.text();
                        Style bigNext = i + 1 < tokens.size()
                                && tokens.get(i + 1).kind() == ColourTemplate.Kind.NUMBER
                                ? DisplayStyles.displayFont(peekStyle(tokens.get(i + 1), glyphs, places,
                                              numbers, users, glyph, place, number, user))
                                : null;
                        if (bigNext != null && !text.isEmpty()
                                && (text.endsWith("+") || text.endsWith("-"))) {
                            String head = text.substring(0, text.length() - 1);
                            if (!head.isEmpty()) {
                                appendHugging(line, head, textStyle, symbolStyle,
                                              noteStyle, inNote, accents, usedAccent,
                                              store, i > 0 ? justFilled : null, null,
                                              afterNumber);
                            }
                            line.append(DisplayStyles.literal(text.substring(text.length() - 1), bigNext));
                            justFilled = null;
                            afterNumber = false;
                            break;
                        }
                        if (forced != null) {
                            line.append(DisplayStyles.literal(token.text(), forced));
                        } else {
                            Style before = i > 0 ? justFilled : null;
                            Style after = i + 1 < tokens.size()
                                    ? peekStyle(tokens.get(i + 1), glyphs, places,
                                                numbers, users, glyph, place, number, user)
                                    : null;
                            int[] span = ColourTemplate.barSpan(token.text());
                            ColourTemplate.Bar bar = span == null ? null
                                    : ColourTemplate.takeBar(bars, usedBar, token.text().charAt(span[0]),
                                              span[1] - span[0]);
                            if (bar == null) {
                                appendHugging(line, token.text(), textStyle, symbolStyle,
                                              noteStyle, inNote, accents, usedAccent,
                                              store, before, after, afterNumber);
                            } else {
                                String head = token.text().substring(0, span[0]);
                                String tail = token.text().substring(span[1]);
                                if (!head.isEmpty()) {
                                    appendHugging(line, head, textStyle, symbolStyle,
                                                  noteStyle, inNote, accents, usedAccent,
                                                  store, before, null, afterNumber);
                                }
                                ColourTemplate.appendBar(line, bar);
                                if (!tail.isEmpty()) {
                                    appendHugging(line, tail, textStyle, symbolStyle,
                                                  noteStyle, inNote, accents, usedAccent,
                                                  store, null, after, false);
                                }
                            }
                        }
                        justFilled = null;
                        afterNumber = false;
                    }
                }
            }
            out.add(line);
        }
        List<String> texts = new ArrayList<>(accents.size());
        List<Style> styles = new ArrayList<>(accents.size());
        for (LineParts.Piece accent : accents) {
            texts.add(accent.text());
            styles.add(accent.style());
        }
        FlowedDebug.accents(texts, styles, usedAccent, flowed);
        List<String> templates = new ArrayList<>(parts.size());
        for (LineParts part : parts) {
            templates.add(part.template());
        }
        ColourAudit.note(templates, flowed, allRuns, accents, usedAccent, blockStyle);
        FlowedDebug.glyphs(glyphs, flowed);
        return out;
    }

    private static Style peekStyle(ColourTemplate.Token next,
                                   List<LineParts.Piece> glyphs, List<LineParts.Piece> places,
                                   List<LineParts.Piece> numbers, List<LineParts.Piece> users,
                                   int glyph, int place, int number, int user) {
        return switch (next.kind()) {
            case GLYPH -> null;
            case PLACE -> at(places, place);
            case NUMBER -> at(numbers, next.index() > 0 ? next.index() - 1 : number);
            case USER -> at(users, user);
            case TEXT, COLOR -> null;
        };
    }

    private static Style at(List<LineParts.Piece> pool, int index) {
        return index >= 0 && index < pool.size() ? pool.get(index).style() : null;
    }

    static boolean leadTakesNext(String text, int lead, Style after) {
        return after != null && lead == text.length();
    }

    private static void appendHugging(MutableComponent out, String text,
                                      Style base, Style symbol, Style note, boolean[] depth,
                                      List<LineParts.Piece> accents, boolean[] used,
                                      TranslationStore store, Style before, Style after,
                                      boolean afterNumber) {
        if (before != null && after != null && StyledRebuilder.sameColour(before, after)
                && !StyledRebuilder.hasLetter(text)) {
            out.append(DisplayStyles.literal(text, DisplayStyles.forDisplay(before)));
            return;
        }
        int lead = 0;
        if (before != null) {
            while (lead < text.length() && hugs(text.charAt(lead))
                    && !hasOwnColour(text, lead, accents, used, before)) {
                lead++;
            }
        }
        int tail = text.length();
        if (after != null) {
            while (tail > lead && hugs(text.charAt(tail - 1))
                    && !hasOwnColour(text, tail - 1, accents, used, after)) {
                tail--;
            }
        }
        if (lead > 0) {
            Style owner = leadTakesNext(text, lead, after) ? after : before;
            out.append(DisplayStyles.literal(text.substring(0, lead), DisplayStyles.forDisplay(owner)));
        }
        int unit = before == null ? 0 : unitLength(text, afterNumber, lead, tail);
        if (unit > 0) {
            out.append(DisplayStyles.literal(text.substring(0, unit), DisplayStyles.forDisplay(before)));
            lead = unit;
        }
        int mid = lead;
        while (mid < tail && TermSubstitution.isPictograph(text.charAt(mid))) {
            mid++;
        }
        if (mid > lead) {
            Style paint = symbol;
            int own = exactAccent(text.substring(lead, mid), accents, used);
            if (own >= 0) {
                used[own] = true;
                Style mine = accents.get(own).style();
                paint = mine == null || paint == null ? DisplayStyles.forDisplay(mine)
                        : paint.withColor(mine.getColor());
            }
            out.append(DisplayStyles.literal(text.substring(lead, mid), paint));
            lead = mid;
        }
        int back = tail;
        if (after != null) {
            int end = tail;
            while (end > lead && text.charAt(end - 1) == ' ') {
                end--;
            }
            int start = end;
            while (start > lead && TermSubstitution.isPictograph(text.charAt(start - 1))) {
                start--;
            }
            if (start < end && exactAccent(text.substring(start, end), accents, used) < 0) {
                back = start;
            }
        }
        if (back > lead) {
            BlockTranslator.appendNoting(out, text.substring(lead, back), base, note,
                         depth, accents, used, store);
        }
        if (back < tail) {
            out.append(DisplayStyles.literal(text.substring(back, tail), DisplayStyles.forDisplay(after)));
        }
        if (tail < text.length()) {
            out.append(DisplayStyles.literal(text.substring(tail), DisplayStyles.forDisplay(after)));
        }
    }

    private static Style leadingSymbolStyle(List<LineParts.Piece> runs) {
        for (LineParts.Piece run : runs) {
            String text = run.text();
            if (text == null || text.isEmpty()) {
                continue;
            }
            return TermSubstitution.isPictograph(text.charAt(0)) ? run.style() : null;
        }
        return null;
    }

    private static int exactAccent(String word, List<LineParts.Piece> accents,
                                   boolean[] used) {
        for (int k = 0; k < accents.size(); k++) {
            if (!used[k] && accents.get(k).text().equals(word)) {
                return k;
            }
        }
        return -1;
    }

    private static boolean hasOwnColour(String text, int at,
                                        List<LineParts.Piece> accents, boolean[] used,
                                        Style side) {
        for (int k = 0; k < accents.size(); k++) {
            if (used[k] || isLoneBracket(accents.get(k).text())) {
                continue;
            }
            String word = accents.get(k).text();
            if (!word.isEmpty() && text.startsWith(word, at)
                    && !StyledRebuilder.sameColour(accents.get(k).style(), side)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLoneBracket(String word) {
        if (word.length() != 1) {
            return false;
        }
        char c = word.charAt(0);
        return c == '[' || c == ']' || c == '【' || c == '】';
    }

    static int unitLength(String text, boolean afterNumber, int lead, int tail) {
        if (!afterNumber || lead != 0 || text.isEmpty()) {
            return 0;
        }
        int unit = 0;
        while (unit < tail && unit < TextWrapper.MAX_UNIT && TextWrapper.isUnitLetter(text.charAt(unit))) {
            unit++;
        }
        if (unit == 0) {
            return 0;
        }
        return unit >= text.length() || !Character.isLetter(text.charAt(unit)) ? unit : 0;
    }

    static boolean hugs(char c) {
        if (Character.isLetterOrDigit(c) || Character.isWhitespace(c) || TermSubstitution.isHan(c)) {
            return false;
        }
        return c != '(' && c != ')' && c != '（' && c != '）';
    }

    static String[] keepAccentsWhole(String[] lines, List<LineParts.Piece> accents) {
        if (lines.length < 2 || accents.isEmpty()) {
            return lines;
        }
        String[] out = lines.clone();
        for (int i = 0; i + 1 < out.length; i++) {
            int move = splitAcross(out[i], out[i + 1], accents);
            if (move > 0) {
                move += trailingGlyph(out[i], out[i].length() - move);
                move = out[i].length()
                        - TextWrapper.keepValueWithGlyph(out[i], out[i].length() - move, 0);
                String moved = out[i].substring(out[i].length() - move);
                out[i] = out[i].substring(0, out[i].length() - move);
                out[i + 1] = moved + out[i + 1];
            }
        }
        return out;
    }

    private static int trailingGlyph(String head, int from) {
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        int at = from;
        while (at > 0 && head.charAt(at - 1) == ' ') {
            at--;
        }
        if (at - glyph.length() <= 0 || !head.startsWith(glyph, at - glyph.length())) {
            return 0;
        }
        return from - (at - glyph.length());
    }

    static List<LineParts.Piece> halvesAcrossBreaks(
            String[] flowed, List<LineParts.Piece> accents) {
        List<LineParts.Piece> out = new ArrayList<>();
        if (flowed.length < 2) {
            return out;
        }
        for (LineParts.Piece accent : accents) {
            String word = accent.text();
            if (word.length() < 4) {
                continue;
            }
            for (int i = 0; i + 1 < flowed.length; i++) {
                int cut = cutBetween(flowed[i], flowed[i + 1], word);
                if (cut <= 0) {
                    continue;
                }
                addHalf(out, word.substring(0, cut), accent.style());
                addHalf(out, word.substring(cut), accent.style());
                break;
            }
        }
        return out;
    }

    private static int cutBetween(String head, String tail, String word) {
        for (int cut = 1; cut < word.length(); cut++) {
            if (head.endsWith(word.substring(0, cut))
                    && tail.startsWith(word.substring(cut))) {
                return cut;
            }
        }
        return 0;
    }

    private static void addHalf(List<LineParts.Piece> out, String half, Style style) {
        if (half.length() >= 2 && half.indexOf('{') < 0 && half.indexOf('}') < 0
                && StyledRebuilder.hasLetter(half)) {
            out.add(new LineParts.Piece(half, style));
        }
    }

    private static int splitAcross(String head, String tail,
                                   List<LineParts.Piece> accents) {
        if (head.isEmpty() || tail.isEmpty()) {
            return 0;
        }
        for (LineParts.Piece accent : accents) {
            String word = accent.text();
            if (word.length() < 2 || word.length() > MAX_REFLOW) {
                continue;
            }
            for (int cut = 1; cut < word.length(); cut++) {
                String left = word.substring(0, cut);
                if (!head.endsWith(left) || !tail.startsWith(word.substring(cut))) {
                    continue;
                }
                if (left.indexOf('{') >= 0 || left.indexOf('}') >= 0) {
                    break;
                }
                return cut;
            }
        }
        return 0;
    }

    private static final int MAX_REFLOW = 12;

    private static int indexOfWhole(String text, String word, int from) {
        if (word.isEmpty()) {
            return -1;
        }
        for (int at = text.indexOf(word, from); at >= 0;
                at = text.indexOf(word, at + 1)) {
            if (!splitsWord(text, at, word)) {
                return at;
            }
        }
        return -1;
    }

    private static boolean splitsWord(String text, int at, String word) {
        int end = at + word.length();
        boolean headCuts = TranslationStore.wordChar(word.charAt(0))
                && at > 0 && TranslationStore.wordChar(text.charAt(at - 1));
        boolean tailCuts = TranslationStore.wordChar(word.charAt(word.length() - 1))
                && end < text.length() && TranslationStore.wordChar(text.charAt(end));
        return headCuts || tailCuts;
    }

    static void appendText(MutableComponent out, String text, Style base,
                           List<LineParts.Piece> accents, boolean[] used,
                           TranslationStore store) {
        List<int[]> kept = store == null ? List.of() : store.keptOriginals(text);
        int from = 0;
        while (from < text.length()) {
            int at = -1;
            int which = -1;
            for (int k = 0; k < accents.size(); k++) {
                if (used[k] || isLoneBracket(accents.get(k).text())) {
                    continue;
                }
                int found = indexOfWhole(text, accents.get(k).text(), from);
                if (found < 0) {
                    continue;
                }
                boolean better = at < 0 || found < at
                        || (found == at && accents.get(k).text().length()
                                         > accents.get(which).text().length());
                if (better) {
                    at = found;
                    which = k;
                }
            }
            TranslationStore.Term term = store == null ? null : TermSubstitution.termOutside(store, text, from, kept);
            boolean longer = term != null && at >= 0 && term.start() == at
                    && term.end() - term.start() >= accents.get(which).text().length();
            if (term != null && (at < 0 || term.start() < at || longer)) {
                if (term.start() > from) {
                    String before = text.substring(from, term.start());
                    if (TermSubstitution.dropsSpaceBefore(before, term.translation())) {
                        before = before.substring(0, before.length() - 1);
                    }
                    out.append(DisplayStyles.literal(before, base));
                }
                Style style = base;
                for (int k = 0; k < accents.size(); k++) {
                    if (used[k]) {
                        continue;
                    }
                    String accentText = accents.get(k).text();
                    if (text.startsWith(accentText, term.start())
                            && term.start() + accentText.length() <= term.end()) {
                        style = DisplayStyles.forDisplay(accents.get(k).style());
                        used[k] = true;
                        break;
                    }
                }
                out.append(DisplayStyles.literal(term.translation(), style));
                from = term.end();
                if (TermSubstitution.dropsSpaceAfter(term.translation(), text, from)) {
                    from++;
                }
                continue;
            }
            if (at < 0) {
                out.append(DisplayStyles.literal(text.substring(from), base));
                return;
            }
            if (at > from) {
                out.append(DisplayStyles.literal(text.substring(from, at), base));
            }
            LineParts.Piece accent = accents.get(which);
            boolean keep = TermSubstitution.overlapping(kept, at, at + accent.text().length()) != null;
            String shown = store == null || keep ? null : store.lookupTerm(accent.text());
            if (shown == null && store != null && !keep) {
                shown = TermSubstitution.termsWithin(accent.text(), store);
            }
            out.append(DisplayStyles.literal(shown != null ? shown : accent.text(),
                               DisplayStyles.forDisplay(accent.style())));
            used[which] = true;
            from = at + accent.text().length();
        }
    }
}
