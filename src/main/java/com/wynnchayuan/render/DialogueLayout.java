package com.wynnchayuan.render;

import com.wynnchayuan.capture.LineParts;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;

final class DialogueLayout {

    private DialogueLayout() {}

    static int rowsNeeded(String text, int rows, Style style, int limit) {
        for (int n = rows + 1; n <= rows + ROWS_TRIED; n++) {
            if (wrap(text, n, style, limit) != null) {
                return n;
            }
        }
        return rows + ROWS_TRIED;
    }

    static int rowsNeeded(String text, int rows) {
        return rowsNeeded(text, rows, null, DialogueRewriter.BODY_LEFT * 2);
    }

    static final int ROWS_TRIED = 6;

    static List<String> wrap(String text, int rows) {
        return wrap(text, rows, null, DialogueRewriter.BODY_LEFT * 2);
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
        return DialogueAccents.cut(inked, from, from + laid.shown(n).length());
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

    static boolean isLatin(char c) {
        return c < 0x2E80 && Character.isLetterOrDigit(c);
    }

    static boolean isWord(char c) {
        return isLatin(c) || c == '_';
    }

    static int nameLead(int width) {
        return -(DialogueRewriter.NAME_CENTRE + width / 2);
    }

    static Integer offsetOf(String text) {
        if (text.codePointCount(0, text.length()) != 1) {
            return null;
        }
        int cp = text.codePointAt(0);
        if (cp < 0xCF000 || cp > 0xD1000) {
            return null;
        }
        return cp - DialogueRewriter.OFFSET_BASE;
    }

    // 位移字元只在 U+CF000–U+D1000（±4096px）有定義
    static String offset(int px) {
        int cp = DialogueRewriter.OFFSET_BASE + px;
        if (cp < 0xCF000 || cp > 0xD1000) {
            return null;
        }
        return new String(Character.toChars(cp));
    }

    static Style fitted(String text, Style style, Style original) {
        if (DialogueFonts.drawable(text, DialogueFonts.fontOf(original))) {
            return style;
        }
        FontDescription pair = DialogueFonts.paired(DialogueFonts.fontOf(original));
        return style.withFont(pair == null ? FontDescription.DEFAULT : pair);
    }

    private static int measure(String text, Style style) {
        return style == null ? width(text) : width(text, style);
    }

    static int width(String text, Style original) {
        if (DialogueIcons.hasIcon(text)) {
            int total = 0;
            for (LineParts.Piece piece : DialogueRewriter.iconRuns(new LineParts.Piece(text, original), original)) {
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
        if (DialogueFonts.drawable(text, DialogueFonts.fontOf(original))) {
            return width(Component.literal(text).withStyle(original));
        }
        FontDescription pair = DialogueFonts.paired(DialogueFonts.fontOf(original));
        return width(Component.literal(text).withStyle(
                pair == null ? original.withFont(FontDescription.DEFAULT)
                        : original.withFont(pair)));
    }

    static java.util.function.ToIntFunction<String> widthForTest;

    static int width(String text) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            return mc.font.width(text);
        }
        return widthForTest != null ? widthForTest.applyAsInt(text) : text.length() * 6;
    }

    static int width(Component text) {
        Minecraft mc = Minecraft.getInstance();
        return mc == null ? 0 : mc.font.width(text);
    }
}
