package com.wynnchayuan.render;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.LineParts;
import com.wynntils.core.text.StyledText;
import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// 換字後要重算尾隨偏移，否則名牌與外框整個位移
public final class DialogueRewriter {

    // hud/dialogue/text/ 也會命中 control 與 nameplate
    private static final String BODY = "/body_";

    private static final String NAMEPLATE = "hud/dialogue/text/nameplate";

    private static final String CONTROL = "hud/dialogue/text/control";

    private static final String CHOICE = "/choice_";

    private static boolean hasDialogueFonts(String lang) {
        return fontShipped(lang, "body_0");
    }

    private static final Map<String, int[]> COVERAGE = new java.util.HashMap<>();

    private static final Map<String, Boolean> FONTS = new java.util.HashMap<>();

    // 實機量到：內文左緣在游標左 116px
    private static final int BODY_LEFT = 116;

    private static final int NAME_CENTRE = 40;

    private static final int OFFSET_BASE = 0xD0000;

    private DialogueRewriter() {}

    private static boolean tooLong;

    public static boolean bodyTooLong() {
        return tooLong;
    }

    public static Component rewrite(Component message, TranslationStore store) {
        tooLong = false;
        if (message == null || store == null) {
            return null;
        }
        DialogueIcons.note(message);
        long now = clock.getAsLong();
        List<Style> styles = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        message.visit((style, text) -> {
            styles.add(style);
            texts.add(text);
            return Optional.empty();
        }, Style.EMPTY);

        List<Integer> body = new ArrayList<>();
        List<Integer> ends = new ArrayList<>();
        for (int i = 1; i + 1 < texts.size(); i++) {
            if (offsetOf(texts.get(i - 1)) == null) {
                continue;
            }
            int stop = i;
            while (stop < texts.size() && offsetOf(texts.get(stop)) == null) {
                stop++;
            }
            if (stop == i || stop >= texts.size()) {
                continue;
            }
            int last = stop - 1;
            if (!bodyRow(texts, styles, i, last)) {
                continue;
            }
            body.add(i);
            ends.add(last);
            i = last;
        }

        StringBuilder whole = new StringBuilder();
        for (int n = 0; n < body.size(); n++) {
            whole.append(joinRow(whole.toString(),
                    rowText(texts, body.get(n), ends.get(n))));
        }
        boolean changed = false;
        var mode = com.wynnchayuan.CollectorConfig.DialogueMode.REPLACE;
        boolean doBody = com.wynnchayuan.WynnChaYuan.config().dialogueMode() == mode;
        boolean doChoice = com.wynnchayuan.WynnChaYuan.config().choiceMode() == mode;
        int room = rowWidth(texts, body);
        String hit = body.isEmpty() || !doBody ? null
                : line(whole.toString(), store, body.size(),
                        styles.get(body.get(0)), room);
        Layout laid = hit == null ? null
                : layout(hit, body.size(), styles.get(body.get(0)), room, now);
        List<String> rows = laid == null ? null : laid.rows();
        for (int n = 0; rows != null && n < body.size(); n++) {
            if (fontMissing(fontOf(styles.get(body.get(n))))) {
                rows = null;
            }
        }
        if (rows != null) {
            changed = true;
        }

        boolean[] swapped = new boolean[texts.size()];
        Map<Integer, String> keep = new java.util.HashMap<>();
        Map<Integer, List<LineParts.Piece>> painted = new java.util.HashMap<>();
        for (int i = 1; doBody && i + 1 < texts.size(); i++) {
            if (!fontOf(styles.get(i)).contains(CONTROL) || !readable(texts.get(i))) {
                continue;
            }
            Integer lead = offsetOf(texts.get(i - 1));
            Integer trail = offsetOf(texts.get(i + 1));
            if (lead == null || trail == null) {
                continue;
            }
            int was = width(Component.literal(texts.get(i)).withStyle(styles.get(i)));
            String raw = texts.get(i);
            int at = 0;
            while (at < raw.length() && !readable(raw.substring(at, at + 1))) {
                at++;
            }
            String prefix = raw.substring(0, at);
            String tip = store.lookup(raw.substring(at).strip());
            if (tip == null || tip.isBlank() || !renderable(tip)) {
                continue;
            }
            tip = oneLine(tip);
            tip = DialogueIcons.stripMarks(tip);

            if (!drawable(tip, fontOf(styles.get(i))) && fontMissing(fontOf(styles.get(i)))) {
                continue;
            }
            int total = width(Component.literal(prefix).withStyle(styles.get(i)))
                    + width(tip, styles.get(i));
            String back = offset(trail + was - total);
            if (back == null) {
                continue;
            }
            keep.put(i, prefix);
            texts.set(i, tip);
            texts.set(i + 1, back);
            swapped[i] = true;
            changed = true;
        }
        for (int i = 1; doChoice && i + 1 < texts.size(); i++) {
            if (!fontOf(styles.get(i)).contains(CHOICE) || !readable(texts.get(i))) {
                continue;
            }
            if (offsetOf(texts.get(i - 1)) == null) {
                continue;
            }
            int end = i;
            while (end + 1 < texts.size() && offsetOf(texts.get(end + 1)) == null) {
                end++;
            }
            if (end + 1 >= texts.size()) {
                continue;
            }
            Integer trail = offsetOf(texts.get(end + 1));
            String row = rowText(texts, i, end);
            String pick = store.lookup(row.strip());
            String rowFont = fontOf(styles.get(i));
            if (pick != null && !pick.isBlank()) {
                Marquee.remember(rowFont, row.strip(), pick);
            } else {
                pick = Marquee.follow(rowFont, row.strip());
            }
            if (pick == null || pick.isBlank() || !renderable(pick)) {
                i = end;
                continue;
            }
            if (!drawable(pick, fontOf(styles.get(i))) && fontMissing(fontOf(styles.get(i)))) {
                i = end;
                continue;
            }
            pick = oneLine(pick);
            pick = DialogueIcons.stripMarks(pick);
            int was = 0;
            for (int k = i; k <= end; k++) {
                was += width(texts.get(k), styles.get(k));
            }
            Style rowStyle = styles.get(i);
            int[] cut = choiceWindow(rowFont, pick, was, s -> width(s, rowStyle), now);
            String full = pick;
            pick = full.substring(cut[0], cut[1]);
            String back = offset(trail + was - width(pick, styles.get(i)));
            if (back == null) {
                i = end;
                continue;
            }
            TextColor colour = tone(texts, styles, List.of(i), List.of(end));
            List<LineParts.Piece> tint =
                    accents(texts, styles, List.of(i), List.of(end), colour);
            painted.put(i, cut(paint(full, styles.get(i).withColor(colour),
                    tint, new boolean[tint.size()]), cut[0], cut[1]));
            texts.set(i, pick);
            for (int k = i + 1; k <= end; k++) {
                texts.set(k, "");
                swapped[k] = true;
            }
            texts.set(end + 1, back);
            swapped[i] = true;
            changed = true;
            i = end;
        }
        if (rows != null) {
            TextColor colour = tone(texts, styles, body, ends);
            List<LineParts.Piece> tint = accents(texts, styles, body, ends, colour);
            // 伺服器打字打到哪才送到哪的顏色，常只有半個名字
            for (LineParts.Piece each : DialogueTint.of(hit)) {
                if (!seen(tint, each.text())) {
                    tint.add(each);
                }
            }
            int[] starts = new int[body.size()];
            List<LineParts.Piece> inked = paint(flatten(rows, starts),
                    Style.EMPTY.withColor(colour), tint, new boolean[tint.size()]);
            List<LineParts.Piece> learned = new ArrayList<>();
            for (int n = 0; n < inked.size() - 1; n++) {
                LineParts.Piece piece = inked.get(n);
                if (!java.util.Objects.equals(piece.style().getColor(), colour)) {
                    learned.add(new LineParts.Piece(piece.text(),
                            Style.EMPTY.withColor(piece.style().getColor())));
                }
            }
            inked = overlay(inked, rows, starts);
            for (int n = 0; n < body.size(); n++) {
                int at = body.get(n);
                int end = ends.get(n);
                Integer after = offsetOf(texts.get(end + 1));
                if (after == null) {
                    continue;
                }
                int was = 0;
                for (int i = at; i <= end; i++) {
                    was += width(texts.get(i), styles.get(i));
                }
                String shown = laid.shown(n);
                int shrink = was - width(shown, styles.get(at));
                String back = offset(after + shrink);
                if (back == null) {
                    continue;
                }
                List<LineParts.Piece> pieces = new ArrayList<>();
                for (LineParts.Piece part : cutRow(laid, inked, starts, n)) {
                    pieces.add(new LineParts.Piece(part.text(),
                            styles.get(at).withColor(part.style().getColor())));
                }
                painted.put(at, pieces);
                texts.set(at, shown);
                for (int i = at + 1; i <= end; i++) {
                    texts.set(i, "");
                    swapped[i] = true;
                }
                texts.set(end + 1, back);
                swapped[at] = true;
            }
            DialogueTint.learn(hit, learned);
        }
        if (!changed) {
            return null;
        }

        MutableComponent out = Component.empty();
        String tofu = null;
        for (int i = 0; i < texts.size(); i++) {
            String prefix = keep.get(i);
            if (prefix != null && !prefix.isEmpty()) {
                out.append(Component.literal(prefix).withStyle(styles.get(i)));
            }
            List<LineParts.Piece> pieces = painted.get(i);
            if (pieces != null) {
                for (LineParts.Piece chunk : pieces) {
                    for (LineParts.Piece piece : iconRuns(chunk, styles.get(i))) {
                        if (piece.style() == null) {
                            continue;
                        }
                        if (DialogueIcons.hasIcon(piece.text())) {
                            out.append(Component.literal(piece.text()).withStyle(piece.style()));
                            continue;
                        }
                        Style worn = fitted(piece.text(), piece.style(), styles.get(i));
                        if (tofu == null) {
                            tofu = unpaintable(piece.text(), worn);
                        }
                        out.append(Component.literal(piece.text()).withStyle(worn));
                    }
                }
                continue;
            }
            Style style = styles.get(i);
            if (swapped[i] && !drawable(texts.get(i), fontOf(styles.get(i)))) {
                style = fitted(texts.get(i), style, styles.get(i));
            }
            if (swapped[i] && tofu == null) {
                tofu = unpaintable(texts.get(i), style);
            }
            out.append(Component.literal(texts.get(i)).withStyle(style));
        }
        if (tofu != null) {
            com.wynnchayuan.capture.DialogueProbe.tofu(message, out, tofu);
            return null;
        }
        return out;
    }

    static List<LineParts.Piece> iconRuns(LineParts.Piece piece, Style row) {
        String text = piece.text();
        if (!DialogueIcons.hasIcon(text)) {
            return List.of(piece);
        }
        List<LineParts.Piece> out = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (!DialogueIcons.isIcon(cp)) {
                run.appendCodePoint(cp);
                continue;
            }
            if (!run.isEmpty()) {
                out.add(new LineParts.Piece(run.toString(), piece.style()));
                run.setLength(0);
            }
            out.add(new LineParts.Piece(new String(Character.toChars(cp)),
                    DialogueIcons.forRow(cp, row)));
        }
        if (!run.isEmpty()) {
            out.add(new LineParts.Piece(run.toString(), piece.style()));
        }
        return out;
    }

    private static String unpaintable(String text, Style style) {
        String font = fontOf(style);
        boolean ours = font.startsWith(WynnChaYuan.MOD_ID + ":");
        if (!ours && !font.startsWith("minecraft:hud/dialogue/text/")) {
            return null;
        }
        String lang = WynnChaYuan.language();
        StringBuilder bad = new StringBuilder();
        text.codePoints().forEach(cp -> {
            String one = new String(Character.toChars(cp));
            if (!(ours ? covered(one, lang) : drawable(one, font))) {
                bad.append(String.format("U+%04X ", cp));
            }
        });
        return bad.length() == 0 ? null
                : bad.toString().strip() + "  字型=" + font + "  文字=" + text;
    }

    static int rowsNeeded(String text, int rows, Style style, int limit) {
        for (int n = rows + 1; n <= rows + ROWS_TRIED; n++) {
            if (wrap(text, n, style, limit) != null) {
                return n;
            }
        }
        return rows + ROWS_TRIED;
    }

    static int rowsNeeded(String text, int rows) {
        return rowsNeeded(text, rows, null, BODY_LEFT * 2);
    }

    static final int ROWS_TRIED = 6;

    static List<String> wrap(String text, int rows) {
        return wrap(text, rows, null, BODY_LEFT * 2);
    }

    // 中日韓走自家字型一字 10px，用預設字型量會少算
    static List<String> wrap(String text, int rows, Style style, int limit) {
        List<String> out = wrap(text, rows, style, limit, true);
        if (out != null) {
            return out;
        }
        return wrap(text, rows, style, limit, false);
    }

    static List<String> wrap(String text, int rows, Style style, int limit,
                             boolean kinsoku) {
        List<String> out = new ArrayList<>(rows);
        int at = pour(text, rows, style, limit, kinsoku, out);
        return at >= text.length() ? out : null;
    }

    static List<String> overflow(String text, int rows, Style style, int limit) {
        if (text == null || rows <= 0) {
            return null;
        }
        List<String> out = new ArrayList<>(rows);
        int at = pour(text, rows - 1, style, limit, true, out);
        String rest = text.substring(Math.min(at, text.length())).strip();
        if (rest.isEmpty()) {
            return null;
        }
        String first = rest.substring(0, rest.offsetByCodePoints(0, 1));
        if (measure(first, style) > limit) {
            return null;
        }
        out.add(rest);
        return out;
    }

    record Layout(List<String> rows, int from, int to) {

        boolean scrolling() {
            return from >= 0;
        }

        int offset(int n) {
            return scrolling() && n == rows.size() - 1 ? from : 0;
        }

        String shown(int n) {
            String row = rows.get(n);
            return scrolling() && n == rows.size() - 1 ? row.substring(from, to) : row;
        }

        List<String> visible() {
            List<String> out = new ArrayList<>(rows.size());
            for (int n = 0; n < rows.size(); n++) {
                out.add(shown(n));
            }
            return out;
        }
    }

    static Layout layout(String text, int rows, Style style, int limit, long now) {
        List<String> fit = wrap(text, rows, style, limit);
        if (fit != null) {
            return new Layout(fit, -1, -1);
        }
        List<String> over = overflow(text, rows, style, limit);
        if (over == null) {
            return null;
        }
        String last = over.get(over.size() - 1);
        long elapsed = Marquee.since(last + '\u0000' + limit, now);
        int[] window = Marquee.window(last, limit, piece -> measure(piece, style), elapsed);
        return new Layout(List.copyOf(over), window[0], window[1]);
    }

    static List<LineParts.Piece> cutRow(Layout laid, List<LineParts.Piece> inked,
                                        int[] starts, int n) {
        int from = starts[n] + laid.offset(n);
        return cut(inked, from, from + laid.shown(n).length());
    }

    static int[] choiceWindow(String row, String pick, int was,
                              java.util.function.ToIntFunction<String> measure, long now) {
        int room = Marquee.choiceRoom(was, measure);
        return Marquee.scroll(row + '\u0000' + pick, pick, room, measure, now);
    }

    static java.util.function.LongSupplier clock = System::currentTimeMillis;

    private static int pour(String text, int rows, Style style, int limit, boolean kinsoku,
                            List<String> out) {
        int at = 0;
        for (int row = 0; row < rows; row++) {
            if (at >= text.length()) {
                out.add("");
                continue;
            }
            int cut = step(text, at, style, limit, kinsoku);
            out.add(cut >= text.length() ? text.substring(at)
                    : text.substring(at, cut).stripTrailing());
            at = cut < text.length() && text.charAt(cut) == ' ' ? cut + 1 : cut;
        }
        return at;
    }

    static List<int[]> flow(String text, Style style, int limit) {
        List<int[]> out = new ArrayList<>();
        int at = 0;
        while (at < text.length()) {
            int cut = step(text, at, style, limit, true);
            if (cut <= at) {
                cut = at + Character.charCount(text.codePointAt(at));
            }
            int end = cut;
            while (end > at && text.charAt(end - 1) == ' ') {
                end--;
            }
            out.add(new int[] {at, end});
            at = cut < text.length() && text.charAt(cut) == ' ' ? cut + 1 : cut;
        }
        return out;
    }

    private static int step(String text, int at, Style style, int limit, boolean kinsoku) {
        int end = at;
        int last = at;
        while (end < text.length()
                && measure(text.substring(at, end + 1), style) <= limit) {
            end++;
            if (end < text.length() && text.charAt(end) == ' ') {
                last = end;
            }
        }
        if (end >= text.length()) {
            return text.length();
        }
        int cut = end;
        if (isWord(text.charAt(end))) {
            int back = end;
            while (back > at && isWord(text.charAt(back - 1))) {
                back--;
            }
            if (back > at) {
                cut = back;
            } else if (last > at) {
                cut = last;
            }
        }
        if (kinsoku) {
            cut = kinsoku(text, at, cut);
        }
        return cut;
    }

    private static final String OPENING = "([{<「『【《〈（〔［｛“‘";

    private static final String CLOSING = ")]}>」』】》〉）〕］｝”’，。、；：！？%％・…‥";

    private static final int KINSOKU_MAX = 3;

    static int kinsoku(String text, int at, int cut) {
        int moved = cut;
        for (int step = 0; step < KINSOKU_MAX; step++) {
            if (moved <= at + 1) {
                break;
            }
            char head = moved < text.length() ? text.charAt(moved) : '\0';
            char tail = text.charAt(moved - 1);
            if (OPENING.indexOf(tail) >= 0 || CLOSING.indexOf(head) >= 0
                    || DialogueIcons.isIcon(tail)) {
                moved--;
                continue;
            }
            break;
        }
        return text.substring(at, moved).isBlank() ? cut : moved;
    }

    private static boolean isLatin(char c) {
        return c < 0x2E80 && Character.isLetterOrDigit(c);
    }

    private static boolean isWord(char c) {
        return isLatin(c) || c == '_';
    }

    static String typedSoFar(String full, int typed, int total) {
        if (total <= 0 || typed >= total) {
            return full;
        }
        int take = (int) Math.round(full.length() * (double) typed / total);
        take = Math.max(0, Math.min(full.length(), take));
        int brace = full.lastIndexOf('{', Math.max(0, take - 1));
        if (brace >= 0 && full.indexOf('}', brace) >= take) {
            take = brace;
        }
        return full.substring(0, take);
    }

    static String fill(String translated, LineParts parts) {
        StringBuilder out = new StringBuilder();
        List<TextColor> colours = new ArrayList<>();
        int place = 0;
        int user = 0;
        int number = 0;
        int glyph = 0;
        TextColor current = null;
        for (int i = 0; i < translated.length(); ) {
            int[] token = DialogueIcons.colourToken(translated, i);
            if (token != null) {
                current = token[1] == 0 ? null
                        : DialogueIcons.resolve(translated.substring(i, i + token[0]));
                i += token[0];
            } else if (translated.startsWith("{#}", i)) {
                LineParts.Piece piece = glyph < parts.glyphs().size()
                        ? parts.glyphs().get(glyph) : null;
                glyph++;
                i += 3;
                if (piece != null && DialogueIcons.hasIcon(piece.text())
                        && DialogueIcons.styleOf(piece.text().codePointAt(0)) != null) {
                    String icon = iconsOf(piece.text());
                    TextColor own = DialogueIcons.colourOf(icon.codePointAt(0));
                    append(out, colours, icon, own);
                    if (DialogueIcons.closes(translated, i) > 0) {
                        current = own;
                    }
                    continue;
                }
                while (i < translated.length() && translated.charAt(i) == ' '
                        && out.length() == 0) {
                    i++;
                }
            } else if (translated.startsWith("{p}", i)) {
                append(out, colours, pick(parts.places(), place++), current);
                i += 3;
            } else if (translated.startsWith("{u}", i)) {
                append(out, colours, pick(parts.users(), user++), current);
                i += 3;
            } else if (translated.startsWith("{~", i)
                    && translated.indexOf('}', i) > 0) {
                int close = translated.indexOf('}', i);
                String index = translated.substring(i + 2, close);
                int at = number;
                if (!index.isEmpty()) {
                    try {
                        at = Integer.parseInt(index) - 1;
                    } catch (NumberFormatException e) {
                        at = number;
                    }
                } else {
                    number++;
                }
                append(out, colours, pick(parts.numbers(), at), current);
                i = close + 1;
            } else {
                out.append(translated.charAt(i++));
                colours.add(current);
            }
        }
        String filled = oneLine(out.toString(), colours);
        boolean any = false;
        for (TextColor colour : colours) {
            any |= colour != null;
        }
        explicitText = any ? filled : null;
        explicit = any ? colours.toArray(new TextColor[0]) : null;
        return filled;
    }

    private static void append(StringBuilder out, List<TextColor> colours, String text,
                               TextColor colour) {
        out.append(text);
        for (int k = 0; k < text.length(); k++) {
            colours.add(colour);
        }
    }

    private static String explicitText;
    private static TextColor[] explicit;

    static String oneLine(String text, List<TextColor> colours) {
        if (text.indexOf('\n') < 0) {
            return text;
        }
        StringBuilder out = new StringBuilder();
        List<TextColor> moved = new ArrayList<>();
        boolean broke = false;
        for (int k = 0; k < text.length(); k++) {
            char c = text.charAt(k);
            if (c == '\r') {
                continue;
            }
            if (c == '\n') {
                broke = true;
                continue;
            }
            if (broke) {
                broke = false;
                boolean spaced = out.length() == 0 || out.charAt(out.length() - 1) == ' '
                        || c == ' ';
                boolean wide = out.length() > 0 && out.charAt(out.length() - 1) >= 0x2E80
                        && c >= 0x2E80;
                if (!spaced && !wide) {
                    out.append(' ');
                    moved.add(null);
                }
            }
            out.append(c);
            moved.add(colours.get(k));
        }
        colours.clear();
        colours.addAll(moved);
        return out.toString();
    }

    static List<LineParts.Piece> overlay(List<LineParts.Piece> inked, List<String> rows,
                                         int[] starts) {
        if (explicitText == null || explicit == null) {
            return inked;
        }
        int length = 0;
        for (LineParts.Piece piece : inked) {
            length += piece.text().length();
        }
        TextColor[] at = new TextColor[length];
        int cursor = 0;
        for (int n = 0; n < rows.size(); n++) {
            String row = rows.get(n);
            if (row.isEmpty()) {
                continue;
            }
            int found = explicitText.indexOf(row, cursor);
            if (found < 0) {
                return inked;
            }
            for (int k = 0; k < row.length() && starts[n] + k < length; k++) {
                at[starts[n] + k] = explicit[found + k];
            }
            cursor = found + row.length();
        }
        List<LineParts.Piece> out = new ArrayList<>();
        int pos = 0;
        for (LineParts.Piece piece : inked) {
            String text = piece.text();
            int from = 0;
            for (int k = 1; k <= text.length(); k++) {
                boolean cut = k == text.length()
                        || !java.util.Objects.equals(at[pos + k], at[pos + from]);
                if (cut) {
                    TextColor colour = at[pos + from];
                    Style style = colour == null ? piece.style()
                            : piece.style().withColor(colour);
                    out.add(new LineParts.Piece(text.substring(from, k), style));
                    from = k;
                }
            }
            pos += text.length();
        }
        return out;
    }

    private static String iconsOf(String text) {
        StringBuilder out = new StringBuilder();
        text.codePoints().filter(DialogueIcons::isIcon).forEach(out::appendCodePoint);
        return out.toString();
    }

    private static String pick(List<LineParts.Piece> pieces, int at) {
        return at >= 0 && at < pieces.size() ? pieces.get(at).text() : "";
    }

    private static String join(String text, TranslationStore store) {
        StringBuilder out = new StringBuilder();
        String rest = text.strip();
        int taken = 0;
        for (int part = 0; part < PARTS && !rest.isEmpty(); part++) {
            String head = null;
            String piece = null;
            for (int at = rest.length(); at > 0; at--) {
                if (at < rest.length() && rest.charAt(at) != ' ') {
                    continue;
                }
                head = rest.substring(0, at).strip();
                piece = plain(head) ? head : store.lookup(head);
                if (piece != null && !piece.isBlank()) {
                    break;
                }
                piece = null;
            }
            if (piece == null) {
                return null;
            }
            if (out.length() > 0 && gap(out.charAt(out.length() - 1), piece.charAt(0))) {
                out.append(' ');
            }
            out.append(piece);
            taken++;
            rest = rest.substring(head.length()).strip();
        }
        return rest.isEmpty() && taken >= 2 && out.length() > 0 ? out.toString() : null;
    }

    static boolean settled(String raw) {
        if (raw.equals(lastRaw)) {
            still++;
        } else {
            lastRaw = raw;
            still = 0;
        }
        return still >= SETTLE_FRAMES;
    }

    private static String lastRaw = "";
    private static int still = 0;

    static boolean sentenceEnd(String text) {
        if (text == null) {
            return false;
        }
        String bare = text.strip();
        boolean word = false;
        for (int i = 0; i < bare.length() && !word; i++) {
            word = Character.isLetterOrDigit(bare.charAt(i));
        }
        if (!word || bare.length() < ENDED_LENGTH) {
            return false;
        }
        int at = bare.length() - 1;
        while (at >= 0 && CLOSERS.indexOf(bare.charAt(at)) >= 0) {
            at--;
        }
        return at >= 0 && ENDINGS.indexOf(bare.charAt(at)) >= 0;
    }

    private static final String ENDINGS = ".!?…。！？";

    // 實測 1500 句：嘆詞類完整句都在 8 字元以下
    private static final int ENDED_LENGTH = 8;

    private static final String CLOSERS = "\"'”’）)』」]";

    static void forget() {
        lastRaw = "";
        still = 0;
        said = "";
        spoken = null;
        held = null;
        heldRaw = "";
        heldSource = null;
        lineShown = false;
        lineRaw = "";
        released = false;
        lineStart = 0;
        lastOut = null;
        skipNow = false;
    }

    private static final int SETTLE_FRAMES = 6;

    private static String said = "";

    private static String spoken;

    static boolean within(String source, String typed) {
        if (source.startsWith(typed)) {
            return true;
        }
        int room = Math.min(source.length(), typed.length());
        int agreed = 0;
        while (agreed < room && source.charAt(agreed) == typed.charAt(agreed)) {
            agreed++;
        }
        return (agreed >= AGREED && agreed < source.length()
                && source.charAt(agreed) == '{'
                && typed.length() <= source.length() + NAME_ROOM)
                || com.wynnchayuan.translate.TranslationStore.slotFits(source, typed);
    }

    private static final int AGREED = 12;

    private static final int NAME_ROOM = 24;

    private static final int PARTS = 4;

    private static boolean plain(String text) {
        String bare = text.replaceAll("\\{[^}]*\\}", "");
        for (int i = 0; i < bare.length(); i++) {
            char c = bare.charAt(i);
            if (Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean gap(char left, char right) {
        return (isLatin(right) || isLatin(left)) && left != ' ' && right != ' ';
    }

    private static boolean bodyRow(List<String> texts, List<Style> styles,
            int from, int to) {
        boolean any = false;
        for (int i = from; i <= to; i++) {
            String font = fontOf(styles.get(i));
            if (!font.contains(BODY) || font.contains(NAMEPLATE)) {
                return false;
            }
            any |= readable(texts.get(i));
        }
        return any;
    }

    private static String rowText(List<String> texts, int from, int to) {
        StringBuilder out = new StringBuilder();
        for (int i = from; i <= to; i++) {
            out.append(texts.get(i));
        }
        return out.toString();
    }

    private static int rowWidth(List<String> texts, List<Integer> body) {
        if (body.isEmpty()) {
            return BODY_LEFT * 2;
        }
        Integer lead = offsetOf(texts.get(body.get(0) - 1));
        if (lead == null || lead >= 0) {
            return BODY_LEFT * 2;
        }
        return -lead + BODY_LEFT;
    }

    static String joinRow(String sofar, String row) {
        if (sofar.isEmpty() || sofar.endsWith(" ") || row.startsWith(" ")) {
            return row;
        }
        return " " + row;
    }

    static boolean learnName(String raw, TranslationStore store) {
        if (raw == null || com.wynnchayuan.capture.SelfNames.find(raw) != null) {
            return false;
        }
        TranslationStore.Guess guess = store.playerNameIn(raw.strip());
        if (guess == null) {
            return false;
        }
        com.wynnchayuan.capture.SelfNames.propose(guess.name(), guess.source());
        return com.wynnchayuan.capture.SelfNames.find(raw) != null;
    }

    static String line(String text, TranslationStore store, int rows,
            Style style, int width) {
        String raw = text == null ? "" : text.strip();
        boolean newLine = lineRaw.isEmpty() || !raw.startsWith(lineRaw);
        skipNow = raw.length() - (newLine ? 0 : lineRaw.length()) >= SKIP_JUMP;
        String out = recognise(text, store, rows, style, width);
        long now = holdClock.getAsLong();
        if (newLine) {
            lineShown = false;
            lineStart = now;
            released = false;
            lastOut = null;
        }
        lineRaw = raw;
        if (out != null && !out.isEmpty()) {
            if (!lineShown) {
                DialogueStartLog.recognised(raw.length(), now - lineStart);
            }
            lineShown = true;
            boolean whole = heldSource == null && out.equals(held);
            out = whole || skipNow ? caughtUp(out) : paced(out);
            DialogueStartLog.frame("就地", raw.length(), out.codePointCount(0, out.length()));
            return out;
        }
        DialogueStartLog.frame("就地", raw.length(), out == null ? -1 : 0);
        if (out == null && !released && !skipNow && holds(raw, store, now)) {
            return "";
        }
        if (out == null && !lineShown && !released) {
            released = true;
            DialogueStartLog.gaveUp(raw.length(), now - lineStart);
        }
        return out;
    }

    static final int MAX_STEP = 2;

    static final int SKIP_JUMP = 8;

    private static boolean skipNow;

    private static String caughtUp(String out) {
        lastOut = out;
        return out;
    }

    private static String lastOut;

    private static String paced(String out) {
        String before = lastOut;
        if (before != null && out.startsWith(before)) {
            int allowed = before.codePointCount(0, before.length()) + MAX_STEP;
            if (out.codePointCount(0, out.length()) > allowed) {
                out = out.substring(0, out.offsetByCodePoints(0, allowed));
            }
        }
        lastOut = out;
        return out;
    }

    static java.util.function.LongSupplier holdClock = System::currentTimeMillis;

    private static long lineStart;

    private static boolean released;

    private static boolean holds(String raw, TranslationStore store, long now) {
        var config = com.wynnchayuan.WynnChaYuan.config();
        if ((config != null && !config.dialogueCover())
                || lineShown || tooLong || raw.isEmpty() || raw.length() > HOLD_CHARS
                || now - lineStart > HOLD_MS || still >= SETTLE_FRAMES || store == null) {
            return false;
        }
        String typed = DialogueIcons.parts(raw, store).template().strip();
        return store.couldStillMatch(typed);
    }

    static final long HOLD_MS = 300;

    static final int HOLD_CHARS = 12;

    private static boolean lineShown;

    private static String lineRaw = "";

    private static String recognise(String text, TranslationStore store, int rows,
            Style style, int width) {
        tooLong = false;
        String raw = text.strip();
        learnName(raw, store);
        LineParts parts = DialogueIcons.parts(raw, store);
        if (store.lookup(parts.template().strip()) == null) {
            parts = parts.namingPlayer();
        }
        String typed = parts.template().strip();
        String source = typed;
        boolean steady = settled(raw) || skipNow;
        String hit = store.lookup(typed);
        String exact = hit;
        if (hit != null && !steady && store.hasLonger(typed)) {
            hit = null;
        }
        if (hit == null) {
            if (spoken != null && !said.isEmpty() && raw.startsWith(said)
                    && within(spoken, typed)) {
                source = spoken;
                hit = store.lookup(spoken);
            }
        }
        if (hit == null) {
            source = store.matchPrefix(typed, raw.length(),
                                       com.wynnchayuan.capture.CurrentQuest.get());
            hit = source == null ? null : store.lookup(source);
        }
        boolean near = false;
        if (hit == null) {
            String close = store.nearQuestLine(typed,
                                               com.wynnchayuan.capture.CurrentQuest.get());
            if (close != null) {
                source = close;
                hit = store.lookup(close);
                near = hit != null;
            }
        }
        if (hit == null) {
            String guess = FORECAST.guess(store, typed);
            if (guess != null) {
                source = guess;
                hit = store.lookup(guess);
            }
        }
        if (hit != null && source != null) {
            said = raw;
            spoken = source;
            if (typed.length() >= DialogueForecast.CONFIRM || source.equals(typed)) {
                FORECAST.heard(store, source);
            }
        } else if (!raw.startsWith(said)) {
            said = "";
            spoken = null;
        }
        if (hit == null) {
            hit = join(typed, store);
            if (hit != null && !steady && store.hasLonger(typed)) {
                hit = null;
            }
            source = hit == null ? null : typed;
        }
        if (hit == null || hit.isBlank()) {
            String held = kept(raw, typed, steady);
            if (held != null || exact == null || !sentenceEnd(typed)) {
                return held;
            }
            hit = exact;
            source = typed;
            said = raw;
            spoken = typed;
            FORECAST.heard(store, typed);
        }
        if (!source.equals(typed)) {
            int cut = unfilledAt(hit, parts);
            if (cut >= 0) {
                hit = hit.substring(0, cut);
            }
        }
        hit = fill(hit, parts);
        hit = oneLine(hit);
        if (!renderable(DialogueIcons.strip(hit))) {
            return drop();
        }
        boolean done = source.equals(typed)
                || (near && (steady || sentenceEnd(typed))
                    && Math.abs(source.length() - typed.length())
                       <= Math.max(12, source.length() / 8));
        if (done) {
            if (wrap(hit, rows, style, width) != null) {
                com.wynnchayuan.capture.OverflowAudit.note(
                        com.wynnchayuan.capture.OverflowAudit.DIALOGUE, source, hit,
                        rows, rows, com.wynnchayuan.capture.OverflowAudit.ROWS_UNIT);
                return show(raw, hit, null);
            }
            com.wynnchayuan.capture.OverflowAudit.note(
                    com.wynnchayuan.capture.OverflowAudit.DIALOGUE, source, hit,
                    rows, rowsNeeded(hit, rows, style, width),
                    com.wynnchayuan.capture.OverflowAudit.ROWS_UNIT);
            if (overflow(hit, rows, style, width) != null) {
                return show(raw, hit, null);
            }
            tooLong = true;
            return drop();
        }
        String part = typedSoFar(hit, typed.length(), source.length());
        if (wrap(part, rows, style, width) == null) {
            int lo = 0;
            int hi = part.length();
            while (lo < hi) {
                int mid = (lo + hi + 1) / 2;
                if (wrap(typedSoFar(part, mid, part.length()), rows, style, width) != null) {
                    lo = mid;
                } else {
                    hi = mid - 1;
                }
            }
            part = typedSoFar(part, lo, part.length());
        }
        if (part.isEmpty() && !hit.isEmpty() && wrap(hit.substring(0, firstUnit(hit)),
                rows, style, width) != null) {
            part = hit.substring(0, firstUnit(hit));
        }
        return part.isEmpty() ? kept(raw, typed, steady) : show(raw, part, source);
    }

    static int unfilledAt(String translated, LineParts parts) {
        int place = 0;
        int user = 0;
        int number = 0;
        for (int i = 0; i < translated.length(); i++) {
            if (translated.startsWith("{p}", i)) {
                if (place++ >= parts.places().size()) {
                    return i;
                }
            } else if (translated.startsWith("{u}", i)) {
                if (user++ >= parts.users().size()) {
                    return i;
                }
            } else if (translated.startsWith("{~", i) && translated.indexOf('}', i) > 0) {
                String index = translated.substring(i + 2, translated.indexOf('}', i));
                int at = number;
                if (index.isEmpty()) {
                    number++;
                } else {
                    try {
                        at = Integer.parseInt(index) - 1;
                    } catch (NumberFormatException e) {
                        at = number++;
                    }
                }
                if (at >= parts.numbers().size()) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int firstUnit(String text) {
        return Character.charCount(text.codePointAt(0));
    }

    static final DialogueForecast FORECAST = new DialogueForecast();

    private static String held;
    private static String heldRaw = "";

    private static String heldSource;

    private static String show(String raw, String text, String source) {
        heldRaw = raw;
        held = text;
        heldSource = source;
        return text;
    }

    private static String kept(String raw, String typed, boolean steady) {
        if (held == null || heldRaw.isEmpty() || !raw.startsWith(heldRaw)
                || steady
                || (heldSource != null && !within(heldSource, typed))) {
            return drop();
        }
        return held;
    }

    private static String drop() {
        held = null;
        heldRaw = "";
        heldSource = null;
        return null;
    }

    private static String speaker(String text, TranslationStore store) {
        String hit = store.lookup(text.strip());
        return hit == null || hit.isBlank() ? null : hit;
    }

    static int nameLead(int width) {
        return -(NAME_CENTRE + width / 2);
    }

    static Integer offsetOf(String text) {
        if (text.codePointCount(0, text.length()) != 1) {
            return null;
        }
        int cp = text.codePointAt(0);
        if (cp < 0xCF000 || cp > 0xD1000) {
            return null;
        }
        return cp - OFFSET_BASE;
    }

    // 位移字元只在 U+CF000–U+D1000（±4096px）有定義
    static String offset(int px) {
        int cp = OFFSET_BASE + px;
        if (cp < 0xCF000 || cp > 0xD1000) {
            return null;
        }
        return new String(Character.toChars(cp));
    }

    static TextColor tone(List<String> texts, List<Style> styles,
                          List<Integer> body, List<Integer> ends) {
        Map<TextColor, Integer> chars = new java.util.LinkedHashMap<>();
        for (int n = 0; n < body.size(); n++) {
            for (int i = body.get(n); i <= ends.get(n); i++) {
                if (shaderKey(styles.get(i).getColor())) {
                    continue;
                }
                chars.merge(styles.get(i).getColor(), texts.get(i).length(),
                        Integer::sum);
            }
        }
        TextColor best = null;
        int most = -1;
        for (Map.Entry<TextColor, Integer> seen : chars.entrySet()) {
            if (seen.getValue() > most) {
                most = seen.getValue();
                best = seen.getKey();
            }
        }
        return best;
    }

    static List<LineParts.Piece> accents(
            List<String> texts, List<Style> styles,
            List<Integer> body, List<Integer> ends, TextColor tone) {
        List<LineParts.Piece> out = new ArrayList<>();
        StringBuilder run = new StringBuilder();
        List<String> parts = new ArrayList<>();
        Style paint = null;
        boolean wrapped = false;
        for (int n = 0; n < body.size(); n++) {
            for (int i = body.get(n); i <= ends.get(n); i++) {
                Style style = styles.get(i);
                if (java.util.Objects.equals(style.getColor(), tone)
                        || shaderKey(style.getColor())) {
                    paint = flush(out, run, parts, paint);
                    continue;
                }
                if (paint == null || !java.util.Objects.equals(
                        style.getColor(), paint.getColor())) {
                    flush(out, run, parts, paint);
                    paint = style;
                } else if (wrapped && run.length() > 0
                        && run.charAt(run.length() - 1) != ' '
                        && !texts.get(i).startsWith(" ")) {
                    run.append(' ');
                }
                wrapped = false;
                run.append(texts.get(i));
                parts.add(texts.get(i).strip());
            }
            wrapped = true;
        }
        flush(out, run, parts, paint);
        return out;
    }

    private static Style flush(List<LineParts.Piece> out, StringBuilder run,
                               List<String> parts, Style paint) {
        if (paint != null) {
            String whole = run.toString().strip();
            if (worthPainting(whole)) {
                out.add(new LineParts.Piece(whole, paint));
            }
            if (parts.size() > 1) {
                for (String part : parts) {
                    if (worthPainting(part) && !part.equals(whole)) {
                        out.add(new LineParts.Piece(part, paint));
                    }
                }
            }
        }
        run.setLength(0);
        parts.clear();
        return null;
    }

    static boolean shaderKey(TextColor colour) {
        if (colour == null) {
            return false;
        }
        int rgb = colour.getValue();
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        boolean movement = g == 235 && b % 4 == 0 && b <= 72;
        boolean effect = r < 4 && g >= 240 && g < 244 && b < 40;
        return movement || effect;
    }

    private static boolean worthPainting(String core) {
        if (core.length() < 2) {
            return false;
        }
        for (int i = 0; i < core.length(); i++) {
            if (Character.isLetterOrDigit(core.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean seen(List<LineParts.Piece> tint, String text) {
        for (LineParts.Piece each : tint) {
            if (each.text().equals(text)) {
                return true;
            }
        }
        return false;
    }

    static String flatten(List<String> rows, int[] starts) {
        StringBuilder out = new StringBuilder();
        for (int n = 0; n < rows.size(); n++) {
            if (out.length() > 0 && !rows.get(n).isEmpty()
                    && isWord(out.charAt(out.length() - 1))
                    && isWord(rows.get(n).charAt(0))) {
                out.append(' ');
            }
            starts[n] = out.length();
            out.append(rows.get(n));
        }
        return out.toString();
    }

    static List<LineParts.Piece> cut(
            List<LineParts.Piece> pieces, int from, int to) {
        List<LineParts.Piece> out = new ArrayList<>();
        int at = 0;
        for (LineParts.Piece piece : pieces) {
            int end = at + piece.text().length();
            int a = Math.max(at, from);
            int b = Math.min(end, to);
            if (a < b) {
                out.add(new LineParts.Piece(
                        piece.text().substring(a - at, b - at), piece.style()));
            }
            at = end;
        }
        return out;
    }

    static List<LineParts.Piece> paint(String row, Style base,
                                       List<LineParts.Piece> accents,
                                       boolean[] used) {
        List<LineParts.Piece> out = new ArrayList<>();
        int from = 0;
        while (from < row.length()) {
            int at = -1;
            int which = -1;
            int take = 0;
            for (int k = 0; k < accents.size(); k++) {
                if (used[k]) {
                    continue;
                }
                int[] found = reach(row, from, accents.get(k).text());
                if (found == null) {
                    continue;
                }
                boolean better = at < 0 || found[0] < at
                        || (found[0] == at && found[1] > take);
                if (better) {
                    at = found[0];
                    take = found[1];
                    which = k;
                }
            }
            if (at < 0) {
                break;
            }
            if (at > from) {
                out.add(new LineParts.Piece(row.substring(from, at), base));
            }
            int stop = bracket(row, at, at + take);
            out.add(new LineParts.Piece(row.substring(at, stop),
                    base.withColor(accents.get(which).style().getColor())));
            used[which] = true;
            from = stop;
        }
        if (from < row.length()) {
            out.add(new LineParts.Piece(row.substring(from), base));
        }
        return out;
    }

    static int[] reach(String row, int from, String want) {
        int at = row.indexOf(want, from);
        if (at >= 0) {
            return new int[] {at, want.length()};
        }
        if (want.length() < NAME_FLOOR) {
            return null;
        }
        int most = Math.min(want.length() - 1, row.length() - from);
        for (int n = most; n >= 1; n--) {
            if (row.regionMatches(row.length() - n, want, 0, n)) {
                return new int[] {row.length() - n, n};
            }
        }
        return null;
    }

    private static final int NAME_FLOOR = 3;

    private static int bracket(String row, int at, int stop) {
        if (row.charAt(at) != '[') {
            return stop;
        }
        int close = row.indexOf(']', Math.max(at, stop - 1));
        return close < 0 || close - at > NAME_LIMIT ? stop : close + 1;
    }

    private static final int NAME_LIMIT = 64;

    private static Style fitted(String text, Style style, Style original) {
        if (drawable(text, fontOf(original))) {
            return style;
        }
        FontDescription pair = paired(fontOf(original));
        return style.withFont(pair == null ? FontDescription.DEFAULT : pair);
    }

    private static int measure(String text, Style style) {
        return style == null ? width(text) : width(text, style);
    }

    private static int width(String text, Style original) {
        if (DialogueIcons.hasIcon(text)) {
            int total = 0;
            for (LineParts.Piece piece : iconRuns(new LineParts.Piece(text, original), original)) {
                if (!DialogueIcons.hasIcon(piece.text())) {
                    total += width(piece.text(), original);
                } else if (piece.style() != null && Minecraft.getInstance() != null) {
                    total += width(Component.literal(piece.text()).withStyle(piece.style()));
                } else if (piece.style() != null) {
                    total += width(piece.text());
                }
            }
            return total;
        }
        if (drawable(text, fontOf(original))) {
            return width(Component.literal(text).withStyle(original));
        }
        FontDescription pair = paired(fontOf(original));
        return width(Component.literal(text).withStyle(
                pair == null ? original.withFont(FontDescription.DEFAULT)
                        : original.withFont(pair)));
    }

    static java.util.function.ToIntFunction<String> widthForTest;

    private static int width(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            return mc.font.width(text);
        }
        return widthForTest != null ? widthForTest.applyAsInt(text) : text.length() * 6;
    }

    private static int width(Component text) {
        Minecraft mc = Minecraft.getInstance();
        return mc == null ? 0 : mc.font.width(text);
    }

    static boolean drawable(String text) {
        return text.codePoints().allMatch(cp ->
                plain(cp) || "ÀÁÂÃÄÅÆÈÉÊËÌÍÎÏàáâãäåæèéêëìíîï".indexOf(cp) >= 0);
    }

    static boolean drawable(String text, String font) {
        if (font != null && font.contains(CONTROL)) {
            return text.codePoints().allMatch(DialogueRewriter::plain);
        }
        return drawable(text);
    }

    private static boolean plain(int cp) {
        return (cp >= 0x20 && cp < 0x7F) || cp == 0x2014;
    }

    private static boolean readable(String text) {
        return text.codePoints().anyMatch(cp ->
                cp >= 0x20 && cp < 0x2E80 && !Character.isWhitespace(cp));
    }

    // MC ttf 的 shift y 正數往下；每列位移是 7 − ascent
    static String oneLine(String text) {
        if (text.indexOf('\n') < 0) {
            return text;
        }
        String[] parts = text.replace("\r", "").split("\n", -1);
        StringBuilder out = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            String next = parts[i];
            boolean spaced = out.length() == 0 || next.isEmpty()
                    || out.charAt(out.length() - 1) == ' ' || next.charAt(0) == ' ';
            boolean wide = out.length() > 0 && !next.isEmpty()
                    && out.charAt(out.length() - 1) >= 0x2E80 && next.charAt(0) >= 0x2E80;
            if (!spaced && !wide) {
                out.append(' ');
            }
            out.append(next);
        }
        return out.toString();
    }

    public static boolean renderable(String text) {
        return drawable(text) || covered(text, WynnChaYuan.language());
    }

    static boolean covered(String text, String lang) {
        int[] ranges = coverage(lang);
        if (ranges.length == 0) {
            return true;
        }
        return text.codePoints().allMatch(cp -> {
            if (cp < 0x80) {
                return true;
            }
            for (int i = 0; i < ranges.length; i += 2) {
                if (cp >= ranges[i] && cp <= ranges[i + 1]) {
                    return true;
                }
            }
            return false;
        });
    }

    private static int[] coverage(String lang) {
        int[] known = COVERAGE.get(lang);
        if (known != null) {
            return known;
        }
        List<Integer> flat = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        try {
            var id = Identifier.fromNamespaceAndPath(WynnChaYuan.MOD_ID,
                    "font/dialogue/" + lang + "/coverage.txt");
            var found = mc.getResourceManager().getResource(id);
            if (found.isPresent()) {
                try (var in = found.get().openAsReader()) {
                    for (String line : in.lines().toList()) {
                        if (line.isBlank() || line.startsWith("#")) {
                            continue;
                        }
                        int dash = line.indexOf('-');
                        flat.add(Integer.parseInt(line.substring(0, dash), 16));
                        flat.add(Integer.parseInt(line.substring(dash + 1), 16));
                    }
                }
            }
        } catch (Exception e) {
        }
        int[] out = new int[flat.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = flat.get(i);
        }
        COVERAGE.put(lang, out);
        return out;
    }

    // 缺一份字型時 FontManager 用 AllMissingGlyphProvider，整列方框
    static boolean fontMissing(String font) {
        String lang = WynnChaYuan.language();
        if (!hasDialogueFonts(lang)) {
            return false;
        }
        String name = pairedName(font);
        return name != null && !fontShipped(lang, name);
    }

    private static boolean fontShipped(String lang, String name) {
        return FONTS.computeIfAbsent(lang + "/" + name, key -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                return true;
            }
            var id = Identifier.fromNamespaceAndPath(WynnChaYuan.MOD_ID,
                    "font/dialogue/" + key + ".json");
            return mc.getResourceManager().getResource(id).isPresent();
        });
    }

    static String pairedName(String font) {
        String name = null;
        if (font.contains(BODY)) {
            int at = font.lastIndexOf(BODY);
            name = "body_" + font.substring(at + BODY.length());
        } else if (font.contains(CHOICE)) {
            int at = font.lastIndexOf(CHOICE);
            name = "choice_" + font.substring(at + CHOICE.length());
        } else if (font.contains(CONTROL)) {
            name = "control";
        } else if (font.contains(NAMEPLATE)) {
            name = "nameplate";
        }
        if (name == null) {
            return null;
        }
        StringBuilder clean = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '_') {
                clean.append(Character.toLowerCase(c));
            } else {
                break;
            }
        }
        return clean.isEmpty() ? null : clean.toString();
    }

    private static FontDescription paired(String font) {
        String clean = pairedName(font);
        if (clean == null) {
            return null;
        }
        String lang = WynnChaYuan.language();
        if (!hasDialogueFonts(lang)) {
            return null;
        }
        return new FontDescription.Resource(Identifier.fromNamespaceAndPath(
                WynnChaYuan.MOD_ID, "dialogue/" + lang + "/" + clean));
    }

    private static String fontOf(Style style) {
        return style == null || style.getFont() == null
                ? "" : String.valueOf(style.getFont());
    }
}
