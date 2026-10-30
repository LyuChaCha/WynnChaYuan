package com.wynnchayuan.render;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// 對話句中元素圖示是 body_N 字型的私用區 U+E000–E00A
public final class DialogueIcons {

    private DialogueIcons() {}

    public static boolean isIcon(int cp) {
        return cp >= 0xE000 && cp <= 0xF8FF;
    }

    public static boolean hasIcon(String text) {
        return text != null && text.codePoints().anyMatch(DialogueIcons::isIcon);
    }

    public static String strip(String text) {
        if (!hasIcon(text)) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        text.codePoints().filter(cp -> !isIcon(cp)).forEach(out::appendCodePoint);
        return out.toString();
    }

    private static volatile Map<Integer, Style> seen = Map.of();

    public static void note(Component message) {
        if (message == null) {
            return;
        }
        Map<Integer, Style> found = new HashMap<>();
        List<TextColor> colours = new ArrayList<>();
        List<TextColor> words = new ArrayList<>();
        boolean[] body = {false};
        message.visit((style, text) -> {
            if (!bodyFont(style)) {
                return Optional.empty();
            }
            body[0] = true;
            if (hasIcon(text)) {
                text.codePoints().filter(DialogueIcons::isIcon)
                    .forEach(cp -> found.putIfAbsent(cp, style));
            }
            if (hasContent(text) && !colours.contains(style.getColor())) {
                colours.add(style.getColor());
            }
            if (GlyphSplitter.hasLetter(text) && !words.contains(style.getColor())) {
                words.add(style.getColor());
            }
            return Optional.empty();
        }, Style.EMPTY);
        boolean changed = false;
        if (!found.isEmpty() && !found.equals(seen)) {
            seen = Map.copyOf(found);
            changed = true;
        }
        if (body[0] && (!colours.equals(palette) || !words.equals(wordPalette))) {
            palette = java.util.Collections.unmodifiableList(colours);
            wordPalette = java.util.Collections.unmodifiableList(words);
            changed = true;
        }
        if (changed) {
            generation++;
        }
    }

    private static boolean hasContent(String text) {
        return text.codePoints().anyMatch(cp -> !Character.isWhitespace(cp)
                && !GlyphSplitter.isGlyphCodePoint(cp));
    }

    private static volatile List<TextColor> palette = List.of();

    private static volatile List<TextColor> wordPalette = List.of();

    private static volatile int generation = 0;

    public static int generation() {
        return generation;
    }

    static List<TextColor> palette() {
        return palette;
    }

    public static int[] colourToken(String text, int at) {
        if (text.startsWith("{/}", at)) {
            return new int[] {3, 0};
        }
        if (at + 3 > text.length() || text.charAt(at) != '{') {
            return null;
        }
        char kind = text.charAt(at + 1);
        if (kind != 'c' && kind != 'w') {
            return null;
        }
        int end = text.indexOf('}', at + 2);
        if (end < 0) {
            return null;
        }
        String body = text.substring(at + 2, end);
        if (body.length() == 1 && body.charAt(0) >= '1' && body.charAt(0) <= '9') {
            return new int[] {end + 1 - at, 1};
        }
        if (kind == 'c' && body.length() > 1 && body.charAt(0) == ':') {
            return new int[] {end + 1 - at, 1};
        }
        return null;
    }

    public static TextColor resolve(String token) {
        if (token == null || token.length() < 4) {
            return null;
        }
        String body = token.substring(2, token.length() - 1);
        if (body.length() == 1 && Character.isDigit(body.charAt(0))) {
            List<TextColor> from = token.charAt(1) == 'w' ? wordPalette : palette;
            int slot = body.charAt(0) - '1';
            return slot >= 0 && slot < from.size() ? from.get(slot) : null;
        }
        if (!body.startsWith(":")) {
            return null;
        }
        String spec = body.substring(1).strip();
        if (spec.startsWith("#")) {
            try {
                return TextColor.fromRgb(Integer.parseInt(spec.substring(1), 16));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        net.minecraft.ChatFormatting named = net.minecraft.ChatFormatting.getByName(spec);
        return named == null || named.getColor() == null ? null : TextColor.fromLegacyFormat(named);
    }

    public static String prepare(String dst, List<LineParts.Piece> glyphs) {
        if (dst == null) {
            return null;
        }
        String out = spans(dst, glyphs);
        if (out.indexOf('{') < 0) {
            return out;
        }
        StringBuilder done = new StringBuilder(out.length());
        int i = 0;
        while (i < out.length()) {
            int[] token = colourToken(out, i);
            if (token == null || token[1] == 0 || out.startsWith("{c:", i)) {
                int step = token == null ? 1 : token[0];
                done.append(out, i, i + step);
                i += step;
                continue;
            }
            String whole = out.substring(i, i + token[0]);
            TextColor colour = resolve(whole);
            done.append(colour == null ? whole
                    : String.format("{c:#%06X}", colour.getValue() & 0xFFFFFF));
            i += token[0];
        }
        return done.toString();
    }

    public static String stripMarks(String text) {
        if (!hasColourMarks(text)) {
            return text;
        }
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            int[] token = colourToken(text, i);
            if (token != null) {
                i += token[0];
            } else {
                out.append(text.charAt(i++));
            }
        }
        return out.toString();
    }

    public static boolean hasColourMarks(String dst) {
        if (dst == null || dst.indexOf('{') < 0) {
            return false;
        }
        for (int i = dst.indexOf('{'); i >= 0; i = dst.indexOf('{', i + 1)) {
            if (colourToken(dst, i) != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean iconic(LineParts parts) {
        for (LineParts.Piece glyph : parts.glyphs()) {
            if (hasIcon(glyph.text())) {
                return true;
            }
        }
        return false;
    }

    static void noteForTest(int cp, Style style) {
        Map<Integer, Style> next = new HashMap<>(seen);
        next.put(cp, style);
        seen = Map.copyOf(next);
    }

    static void forget() {
        seen = Map.of();
        palette = List.of();
        wordPalette = List.of();
        generation++;
    }

    static Style styleOf(int cp) {
        return seen.get(cp);
    }

    static TextColor colourOf(int cp) {
        Style style = seen.get(cp);
        return style == null ? null : style.getColor();
    }

    private static boolean bodyFont(Style style) {
        Identifier id = fontId(style);
        return id != null && id.getPath().startsWith("hud/dialogue/text/")
                && id.getPath().contains("/body_");
    }

    private static Identifier fontId(Style style) {
        return style != null && style.getFont() instanceof FontDescription.Resource r
                ? r.id() : null;
    }

    static Style forRow(int cp, Style row) {
        Style icon = seen.get(cp);
        if (icon == null) {
            return null;
        }
        Identifier mine = fontId(icon);
        Identifier theirs = fontId(row);
        if (mine == null || theirs == null) {
            return icon;
        }
        String path = mine.getPath();
        int at = path.lastIndexOf("/body_");
        int rowAt = theirs.getPath().lastIndexOf("/body_");
        if (at < 0 || rowAt < 0) {
            return icon;
        }
        String moved = path.substring(0, at) + theirs.getPath().substring(rowAt);
        return icon.withFont(new FontDescription.Resource(
                Identifier.fromNamespaceAndPath(mine.getNamespace(), moved)));
    }

    private static final FontDescription MARK = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath("wynnchayuan", "dialogue_icon"));

    static StyledText split(String raw) {
        MutableComponent out = Component.empty();
        StringBuilder run = new StringBuilder();
        boolean icons = false;
        int i = 0;
        while (i < raw.length()) {
            int cp = raw.codePointAt(i);
            boolean icon = isIcon(cp);
            if (icon != icons || (icon && !run.isEmpty()
                    && !java.util.Objects.equals(styleOf(run.codePointAt(0)), styleOf(cp)))) {
                emit(out, run, icons);
                icons = icon;
            }
            run.appendCodePoint(cp);
            i += Character.charCount(cp);
        }
        emit(out, run, icons);
        return StyledText.fromComponent(out);
    }

    private static void emit(MutableComponent out, StringBuilder run, boolean icons) {
        if (run.isEmpty()) {
            return;
        }
        Style style = Style.EMPTY;
        if (icons) {
            Style known = styleOf(run.codePointAt(0));
            style = known != null ? known : Style.EMPTY.withFont(MARK);
        }
        out.append(Component.literal(run.toString()).withStyle(style));
        run.setLength(0);
    }

    public static LineParts parts(String raw, TranslationStore store) {
        return LineParts.of(line(raw, store));
    }

    public static StyledText line(String raw, TranslationStore store) {
        StyledText legacy = StyledText.fromString(raw);
        if (!hasIcon(raw)) {
            return legacy;
        }
        StyledText icons = split(raw);
        if (store == null) {
            return icons;
        }
        String iconsT = LineParts.of(icons).template();
        String legacyT = LineParts.of(legacy).template();
        if (iconsT.equals(legacyT) || known(store, iconsT)) {
            return icons;
        }
        return known(store, legacyT) ? legacy : icons;
    }

    private static boolean known(TranslationStore store, String template) {
        String bare = template.strip();
        return store.lookup(bare) != null || store.hasLonger(bare);
    }

    public static String spans(String dst, List<LineParts.Piece> glyphs) {
        if (dst == null || !dst.contains(GlyphSplitter.GLYPH_PLACEHOLDER)) {
            return dst;
        }
        StringBuilder out = new StringBuilder(dst.length() + 32);
        int glyph = 0;
        int i = 0;
        while (i < dst.length()) {
            if (!dst.startsWith(GlyphSplitter.GLYPH_PLACEHOLDER, i)) {
                out.append(dst.charAt(i++));
                continue;
            }
            i += GlyphSplitter.GLYPH_PLACEHOLDER.length();
            out.append(GlyphSplitter.GLYPH_PLACEHOLDER);
            LineParts.Piece piece = glyph < glyphs.size() ? glyphs.get(glyph) : null;
            glyph++;
            TextColor colour = piece == null || !hasIcon(piece.text()) || piece.style() == null
                    ? null : piece.style().getColor();
            if (colour != null && closes(dst, i) > 0) {
                out.append(String.format("{c:#%06X}", colour.getValue() & 0xFFFFFF));
            }
        }
        return out.toString();
    }

    static int closes(String dst, int from) {
        int end = dst.indexOf("{/}", from);
        if (end <= from) {
            return -1;
        }
        String between = dst.substring(from, end);
        if (between.contains(GlyphSplitter.GLYPH_PLACEHOLDER) || between.contains("{c")
                || between.contains("{w") || between.indexOf('\n') >= 0) {
            return -1;
        }
        return end;
    }

    public static Component forPanel(Component line) {
        if (line == null) {
            return null;
        }
        boolean[] touched = {false};
        MutableComponent out = Component.empty();
        line.visit((style, text) -> {
            if (!hasIcon(text) || !ours(style)) {
                out.append(Component.literal(text).withStyle(style));
                return Optional.empty();
            }
            touched[0] = true;
            StringBuilder keep = new StringBuilder();
            Style plain = style.withFont(FontDescription.DEFAULT);
            text.codePoints().forEach(cp -> {
                if (!isIcon(cp)) {
                    keep.appendCodePoint(cp);
                    return;
                }
                String shown = chatGlyph(cp, style);
                if (shown != null) {
                    keep.append(shown);
                }
            });
            if (!keep.isEmpty()) {
                out.append(Component.literal(keep.toString()).withStyle(plain));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return touched[0] ? out : line;
    }

    private static boolean ours(Style style) {
        Identifier id = fontId(style);
        return id != null && (id.equals(((FontDescription.Resource) MARK).id())
                || id.getPath().startsWith("hud/dialogue/text/"));
    }

    private static final Map<Integer, String> CHAT = Map.of(
            0xE000, "❋",
            0xE001, "✤",
            0xE002, "✹",
            0xE003, "✦",
            0xE004, "❉");

    static String chatGlyph(int cp, Style style) {
        Identifier id = fontId(style);
        boolean common = id == null || id.getPath().contains("/common/")
                || id.equals(((FontDescription.Resource) MARK).id());
        return common ? CHAT.get(cp) : null;
    }
}
