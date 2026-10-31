package com.wynnchayuan.render;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.CurrentQuest;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.capture.OverflowAudit;
import com.wynnchayuan.capture.SelfNames;
import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

final class DialogueTyping {

    private DialogueTyping() {}

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
                    String icon = DialogueAccents.iconsOf(piece.text());
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
                append(out, colours, DialogueAccents.pick(parts.places(), place++), current);
                i += 3;
            } else if (translated.startsWith("{u}", i)) {
                append(out, colours, DialogueAccents.pick(parts.users(), user++), current);
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
                append(out, colours, DialogueAccents.pick(parts.numbers(), at), current);
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

    static String explicitText;

    static TextColor[] explicit;

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
                || TranslationStore.slotFits(source, typed);
    }

    private static final int AGREED = 12;

    private static final int NAME_ROOM = 24;

    private static final int PARTS = 4;

    private static final Pattern ANY_SLOT = Pattern.compile("\\{[^}]*\\}");

    private static boolean plain(String text) {
        String bare = ANY_SLOT.matcher(text).replaceAll("");
        for (int i = 0; i < bare.length(); i++) {
            char c = bare.charAt(i);
            if (Character.isLetter(c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean gap(char left, char right) {
        return (DialogueLayout.isLatin(right) || DialogueLayout.isLatin(left)) && left != ' ' && right != ' ';
    }

    static boolean learnName(String raw, TranslationStore store) {
        if (raw == null || SelfNames.find(raw) != null) {
            return false;
        }
        TranslationStore.Guess guess = store.playerNameIn(raw.strip());
        if (guess == null) {
            return false;
        }
        SelfNames.propose(guess.name(), guess.source());
        return SelfNames.find(raw) != null;
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

    static LongSupplier holdClock = System::currentTimeMillis;

    private static long lineStart;

    private static boolean released;

    private static boolean holds(String raw, TranslationStore store, long now) {
        var config = WynnChaYuan.config();
        if ((config != null && !config.dialogueCover())
                || lineShown || DialogueRewriter.tooLong || raw.isEmpty() || raw.length() > HOLD_CHARS
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
        DialogueRewriter.tooLong = false;
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
                                       CurrentQuest.get());
            hit = source == null ? null : store.lookup(source);
        }
        boolean near = false;
        if (hit == null) {
            String close = store.nearQuestLine(typed,
                                               CurrentQuest.get());
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
        if (!DialogueFonts.renderable(DialogueIcons.strip(hit))) {
            return drop();
        }
        boolean done = source.equals(typed)
                || (near && (steady || sentenceEnd(typed))
                    && Math.abs(source.length() - typed.length())
                       <= Math.max(12, source.length() / 8));
        if (done) {
            if (DialogueLayout.wrap(hit, rows, style, width) != null) {
                OverflowAudit.note(OverflowAudit.DIALOGUE, source, hit,
                        rows, rows, OverflowAudit.ROWS_UNIT);
                return show(raw, hit, null);
            }
            OverflowAudit.note(OverflowAudit.DIALOGUE, source, hit,
                    rows, DialogueLayout.rowsNeeded(hit, rows, style, width),
                    OverflowAudit.ROWS_UNIT);
            if (DialogueLayout.overflow(hit, rows, style, width) != null) {
                return show(raw, hit, null);
            }
            DialogueRewriter.tooLong = true;
            return drop();
        }
        String part = typedSoFar(hit, typed.length(), source.length());
        if (DialogueLayout.wrap(part, rows, style, width) == null) {
            int lo = 0;
            int hi = part.length();
            while (lo < hi) {
                int mid = (lo + hi + 1) / 2;
                if (DialogueLayout.wrap(typedSoFar(part, mid, part.length()), rows, style, width) != null) {
                    lo = mid;
                } else {
                    hi = mid - 1;
                }
            }
            part = typedSoFar(part, lo, part.length());
        }
        if (part.isEmpty() && !hit.isEmpty() && DialogueLayout.wrap(hit.substring(0, firstUnit(hit)),
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
}
