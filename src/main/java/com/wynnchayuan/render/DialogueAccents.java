package com.wynnchayuan.render;

import com.wynnchayuan.capture.LineParts;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class DialogueAccents {

    private DialogueAccents() {}

    static List<LineParts.Piece> overlay(List<LineParts.Piece> inked, List<String> rows,
                                         int[] starts) {
        if (DialogueTyping.explicitText == null || DialogueTyping.explicit == null) {
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
            int found = DialogueTyping.explicitText.indexOf(row, cursor);
            if (found < 0) {
                return inked;
            }
            for (int k = 0; k < row.length() && starts[n] + k < length; k++) {
                at[starts[n] + k] = DialogueTyping.explicit[found + k];
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
                        || !Objects.equals(at[pos + k], at[pos + from]);
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

    static String iconsOf(String text) {
        StringBuilder out = new StringBuilder();
        text.codePoints().filter(DialogueIcons::isIcon).forEach(out::appendCodePoint);
        return out.toString();
    }

    static String pick(List<LineParts.Piece> pieces, int at) {
        return at >= 0 && at < pieces.size() ? pieces.get(at).text() : "";
    }

    static TextColor tone(List<String> texts, List<Style> styles,
                          List<Integer> body, List<Integer> ends) {
        Map<TextColor, Integer> chars = new LinkedHashMap<>();
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
                if (Objects.equals(style.getColor(), tone)
                        || shaderKey(style.getColor())) {
                    paint = flush(out, run, parts, paint);
                    continue;
                }
                if (paint == null || !Objects.equals(style.getColor(), paint.getColor())) {
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

    static boolean seen(List<LineParts.Piece> tint, String text) {
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
                    && DialogueLayout.isWord(out.charAt(out.length() - 1))
                    && DialogueLayout.isWord(rows.get(n).charAt(0))) {
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
}
