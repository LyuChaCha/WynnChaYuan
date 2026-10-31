package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.DialogueProbe;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

// 換字後要重算尾隨偏移，否則名牌與外框整個位移
public final class DialogueRewriter {

    // 實機量到：內文左緣在游標左 116px
    static final int BODY_LEFT = 116;

    static final int NAME_CENTRE = 40;

    static final int OFFSET_BASE = 0xD0000;

    private DialogueRewriter() {}

    static boolean tooLong;

    public static void forget() {
        tooLong = false;
        DialogueTyping.forget();
        DialogueForecast.forgetAll();
    }

    public static boolean bodyTooLong() {
        return tooLong;
    }

    public static Component rewrite(Component message, TranslationStore store) {
        tooLong = false;
        if (message == null || store == null) {
            return null;
        }
        DialogueIcons.note(message);
        long now = DialogueLayout.clock.getAsLong();
        Frame frame = Frame.of(message);
        findBody(frame);
        List<Integer> body = frame.body;
        List<Style> styles = frame.styles;
        var mode = ModConfig.DialogueMode.REPLACE;
        boolean doBody = WynnChaYuan.config().dialogueMode() == mode;
        boolean doChoice = WynnChaYuan.config().choiceMode() == mode;
        int room = rowWidth(frame.texts, body);
        String hit = body.isEmpty() || !doBody ? null
                : DialogueTyping.line(joinedBody(frame), store, body.size(),
                        styles.get(body.get(0)), room);
        DialogueLayout.Layout laid = hit == null ? null
                : DialogueLayout.layout(hit, body.size(), styles.get(body.get(0)), room, now);
        List<String> rows = laid == null ? null : laid.rows();
        for (int n = 0; rows != null && n < body.size(); n++) {
            if (DialogueFonts.fontMissing(DialogueFonts.fontOf(styles.get(body.get(n))))) {
                rows = null;
            }
        }
        if (rows != null) {
            frame.changed = true;
        }
        if (doBody) {
            swapControls(frame, store);
        }
        if (doChoice) {
            swapChoices(frame, store, now);
        }
        if (rows != null) {
            inkBody(frame, hit, laid, rows);
        }
        return frame.changed ? assemble(message, frame) : null;
    }

    private static final class Frame {
        final List<Style> styles = new ArrayList<>();
        final List<String> texts = new ArrayList<>();
        final List<Integer> body = new ArrayList<>();
        final List<Integer> ends = new ArrayList<>();
        final Map<Integer, String> keep = new HashMap<>();
        final Map<Integer, List<LineParts.Piece>> painted = new HashMap<>();
        boolean[] swapped;
        boolean changed;

        static Frame of(Component message) {
            Frame frame = new Frame();
            message.visit((style, text) -> {
                frame.styles.add(style);
                frame.texts.add(text);
                return Optional.empty();
            }, Style.EMPTY);
            frame.swapped = new boolean[frame.texts.size()];
            return frame;
        }
    }

    private static void findBody(Frame frame) {
        List<String> texts = frame.texts;
        List<Style> styles = frame.styles;
        List<Integer> body = frame.body;
        List<Integer> ends = frame.ends;
        for (int i = 1; i + 1 < texts.size(); i++) {
            if (DialogueLayout.offsetOf(texts.get(i - 1)) == null) {
                continue;
            }
            int stop = i;
            while (stop < texts.size() && DialogueLayout.offsetOf(texts.get(stop)) == null) {
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
    }

    private static String joinedBody(Frame frame) {
        StringBuilder whole = new StringBuilder();
        for (int n = 0; n < frame.body.size(); n++) {
            whole.append(joinRow(whole.toString(),
                    rowText(frame.texts, frame.body.get(n), frame.ends.get(n))));
        }
        return whole.toString();
    }

    private static void swapControls(Frame frame, TranslationStore store) {
        List<String> texts = frame.texts;
        List<Style> styles = frame.styles;
        boolean[] swapped = frame.swapped;
        Map<Integer, String> keep = frame.keep;
        for (int i = 1; i + 1 < texts.size(); i++) {
            if (!DialogueFonts.fontOf(styles.get(i)).contains(DialogueFonts.CONTROL) || !DialogueFonts.readable(texts.get(i))) {
                continue;
            }
            Integer lead = DialogueLayout.offsetOf(texts.get(i - 1));
            Integer trail = DialogueLayout.offsetOf(texts.get(i + 1));
            if (lead == null || trail == null) {
                continue;
            }
            int was = DialogueLayout.width(Component.literal(texts.get(i)).withStyle(styles.get(i)));
            String raw = texts.get(i);
            int at = 0;
            while (at < raw.length() && !DialogueFonts.readable(raw.substring(at, at + 1))) {
                at++;
            }
            String prefix = raw.substring(0, at);
            String tip = store.lookup(raw.substring(at).strip());
            if (tip == null || tip.isBlank() || !renderable(tip)) {
                continue;
            }
            tip = DialogueTyping.oneLine(tip);
            tip = DialogueIcons.stripMarks(tip);

            if (!DialogueFonts.drawable(tip, DialogueFonts.fontOf(styles.get(i))) && DialogueFonts.fontMissing(DialogueFonts.fontOf(styles.get(i)))) {
                continue;
            }
            int total = DialogueLayout.width(Component.literal(prefix).withStyle(styles.get(i)))
                    + DialogueLayout.width(tip, styles.get(i));
            String back = DialogueLayout.offset(trail + was - total);
            if (back == null) {
                continue;
            }
            keep.put(i, prefix);
            texts.set(i, tip);
            texts.set(i + 1, back);
            swapped[i] = true;
            frame.changed = true;
        }
    }

    private static void swapChoices(Frame frame, TranslationStore store, long now) {
        List<String> texts = frame.texts;
        List<Style> styles = frame.styles;
        boolean[] swapped = frame.swapped;
        Map<Integer, List<LineParts.Piece>> painted = frame.painted;
        for (int i = 1; i + 1 < texts.size(); i++) {
            if (!DialogueFonts.fontOf(styles.get(i)).contains(DialogueFonts.CHOICE) || !DialogueFonts.readable(texts.get(i))) {
                continue;
            }
            if (DialogueLayout.offsetOf(texts.get(i - 1)) == null) {
                continue;
            }
            int end = i;
            while (end + 1 < texts.size() && DialogueLayout.offsetOf(texts.get(end + 1)) == null) {
                end++;
            }
            if (end + 1 >= texts.size()) {
                continue;
            }
            Integer trail = DialogueLayout.offsetOf(texts.get(end + 1));
            String row = rowText(texts, i, end);
            String pick = store.lookup(row.strip());
            String rowFont = DialogueFonts.fontOf(styles.get(i));
            if (pick != null && !pick.isBlank()) {
                Marquee.remember(rowFont, row.strip(), pick);
            } else {
                pick = Marquee.follow(rowFont, row.strip());
            }
            if (pick == null || pick.isBlank() || !renderable(pick)) {
                i = end;
                continue;
            }
            if (!DialogueFonts.drawable(pick, DialogueFonts.fontOf(styles.get(i))) && DialogueFonts.fontMissing(DialogueFonts.fontOf(styles.get(i)))) {
                i = end;
                continue;
            }
            pick = DialogueTyping.oneLine(pick);
            pick = DialogueIcons.stripMarks(pick);
            int was = 0;
            for (int k = i; k <= end; k++) {
                was += DialogueLayout.width(texts.get(k), styles.get(k));
            }
            Style rowStyle = styles.get(i);
            int[] cut = DialogueLayout.choiceWindow(rowFont, pick, was, s -> DialogueLayout.width(s, rowStyle), now);
            String full = pick;
            pick = full.substring(cut[0], cut[1]);
            String back = DialogueLayout.offset(trail + was - DialogueLayout.width(pick, styles.get(i)));
            if (back == null) {
                i = end;
                continue;
            }
            TextColor colour = DialogueAccents.tone(texts, styles, List.of(i), List.of(end));
            List<LineParts.Piece> tint =
                    DialogueAccents.accents(texts, styles, List.of(i), List.of(end), colour);
            painted.put(i, DialogueAccents.cut(DialogueAccents.paint(full, styles.get(i).withColor(colour),
                    tint, new boolean[tint.size()]), cut[0], cut[1]));
            texts.set(i, pick);
            for (int k = i + 1; k <= end; k++) {
                texts.set(k, "");
                swapped[k] = true;
            }
            texts.set(end + 1, back);
            swapped[i] = true;
            frame.changed = true;
            i = end;
        }
    }

    private static void inkBody(Frame frame, String hit, DialogueLayout.Layout laid, List<String> rows) {
        List<String> texts = frame.texts;
        List<Style> styles = frame.styles;
        List<Integer> body = frame.body;
        List<Integer> ends = frame.ends;
        boolean[] swapped = frame.swapped;
        Map<Integer, List<LineParts.Piece>> painted = frame.painted;
        TextColor colour = DialogueAccents.tone(texts, styles, body, ends);
        List<LineParts.Piece> tint = DialogueAccents.accents(texts, styles, body, ends, colour);
        // 伺服器打字打到哪才送到哪的顏色，常只有半個名字
        for (LineParts.Piece each : DialogueTint.of(hit)) {
            if (!DialogueAccents.seen(tint, each.text())) {
                tint.add(each);
            }
        }
        int[] starts = new int[body.size()];
        List<LineParts.Piece> inked = DialogueAccents.paint(DialogueAccents.flatten(rows, starts),
                Style.EMPTY.withColor(colour), tint, new boolean[tint.size()]);
        List<LineParts.Piece> learned = new ArrayList<>();
        for (int n = 0; n < inked.size() - 1; n++) {
            LineParts.Piece piece = inked.get(n);
            if (!Objects.equals(piece.style().getColor(), colour)) {
                learned.add(new LineParts.Piece(piece.text(),
                        Style.EMPTY.withColor(piece.style().getColor())));
            }
        }
        inked = DialogueAccents.overlay(inked, rows, starts);
        for (int n = 0; n < body.size(); n++) {
            int at = body.get(n);
            int end = ends.get(n);
            Integer after = DialogueLayout.offsetOf(texts.get(end + 1));
            if (after == null) {
                continue;
            }
            int was = 0;
            for (int i = at; i <= end; i++) {
                was += DialogueLayout.width(texts.get(i), styles.get(i));
            }
            String shown = laid.shown(n);
            int shrink = was - DialogueLayout.width(shown, styles.get(at));
            String back = DialogueLayout.offset(after + shrink);
            if (back == null) {
                continue;
            }
            List<LineParts.Piece> pieces = new ArrayList<>();
            for (LineParts.Piece part : DialogueLayout.cutRow(laid, inked, starts, n)) {
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

    private static Component assemble(Component message, Frame frame) {
        List<String> texts = frame.texts;
        List<Style> styles = frame.styles;
        boolean[] swapped = frame.swapped;
        Map<Integer, String> keep = frame.keep;
        Map<Integer, List<LineParts.Piece>> painted = frame.painted;
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
                        Style worn = DialogueLayout.fitted(piece.text(), piece.style(), styles.get(i));
                        if (tofu == null) {
                            tofu = DialogueFonts.unpaintable(piece.text(), worn);
                        }
                        out.append(Component.literal(piece.text()).withStyle(worn));
                    }
                }
                continue;
            }
            Style style = styles.get(i);
            if (swapped[i] && !DialogueFonts.drawable(texts.get(i), DialogueFonts.fontOf(styles.get(i)))) {
                style = DialogueLayout.fitted(texts.get(i), style, styles.get(i));
            }
            if (swapped[i] && tofu == null) {
                tofu = DialogueFonts.unpaintable(texts.get(i), style);
            }
            out.append(Component.literal(texts.get(i)).withStyle(style));
        }
        if (tofu != null) {
            DialogueProbe.tofu(message, out, tofu);
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

    private static boolean bodyRow(List<String> texts, List<Style> styles,
            int from, int to) {
        boolean any = false;
        for (int i = from; i <= to; i++) {
            String font = DialogueFonts.fontOf(styles.get(i));
            if (!font.contains(DialogueFonts.BODY) || font.contains(DialogueFonts.NAMEPLATE)) {
                return false;
            }
            any |= DialogueFonts.readable(texts.get(i));
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
        Integer lead = DialogueLayout.offsetOf(texts.get(body.get(0) - 1));
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

    public static boolean renderable(String text) {
        return DialogueFonts.renderable(text);
    }
}
