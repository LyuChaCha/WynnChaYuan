package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

// Wynncraft tooltip 外框是字型畫的，大小照英文寬度挑
public final class NameWrap {

    private NameWrap() {}

    public static Split split(List<Component> original, List<Component> translated,
                              boolean[] centered, TranslationStore store,
                              ToIntFunction<Component> width) {
        Split same = new Split(original, translated, centered);
        if (original == null || translated == null || centered == null || store == null
                || !store.namesWithOriginal()
                || (centered.length != translated.size() && centered.length != original.size())) {
            return same;
        }
        int frame = 0;
        for (Component row : original) {
            frame = Math.max(frame, width.applyAsInt(row));
        }
        int n = Math.min(original.size(), translated.size());
        for (int i = 0; i < n; i++) {
            if (i >= NAME_ROWS) {
                break;
            }
            Component line = translated.get(i);
            int budget = Math.max(width.applyAsInt(original.get(i)), frame);
            if (width.applyAsInt(original.get(i)) <= 0
                    || width.applyAsInt(line) <= budget) {
                continue;
            }
            List<Piece> pieces = pieces(line);
            int[] span = store.appendedOriginalSpan(text(pieces));
            if (span == null) {
                continue;
            }
            List<Component> out = new ArrayList<>(translated);
            out.set(i, join(slice(pieces, 0, span[0]),
                            slice(pieces, span[1], text(pieces).length())));
            out.add(i + 1, indented(indent(pieces, width),
                                    slice(pieces, span[0] + 1, span[1])));
            return new Split(insert(original, i), out, insert(centered, i));
        }
        return same;
    }

    private static final int NAME_ROWS = 2;

    public record Split(List<Component> original, List<Component> translated,
                        boolean[] centered) {}

    private static <T> List<T> insert(List<T> rows, int i) {
        List<T> out = new ArrayList<>(rows);
        if (i < out.size()) {
            out.add(i + 1, out.get(i));
        }
        return out;
    }

    private static boolean[] insert(boolean[] flags, int i) {
        boolean[] out = new boolean[flags.length + 1];
        System.arraycopy(flags, 0, out, 0, i + 1);
        out[i + 1] = flags[i];
        System.arraycopy(flags, i + 1, out, i + 2, flags.length - i - 1);
        return out;
    }

    private record Piece(String text, Style style) {}

    private static List<Piece> pieces(Component line) {
        List<Piece> out = new ArrayList<>();
        line.visit((style, text) -> {
            if (!text.isEmpty()) {
                out.add(new Piece(text, style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static String text(List<Piece> pieces) {
        StringBuilder sb = new StringBuilder();
        for (Piece p : pieces) {
            sb.append(p.text());
        }
        return sb.toString();
    }

    private static List<Piece> slice(List<Piece> pieces, int from, int to) {
        List<Piece> out = new ArrayList<>();
        int at = 0;
        for (Piece p : pieces) {
            int end = at + p.text().length();
            int a = Math.max(from, at);
            int b = Math.min(to, end);
            if (a < b) {
                out.add(new Piece(p.text().substring(a - at, b - at), p.style()));
            }
            at = end;
        }
        return out;
    }

    private static int indent(List<Piece> pieces, ToIntFunction<Component> width) {
        List<Piece> lead = new ArrayList<>();
        for (Piece p : pieces) {
            if (p.text().codePoints().anyMatch(
                    cp -> !GlyphSplitter.isGlyphCodePoint(cp)
                            && !Character.isWhitespace(cp))) {
                break;
            }
            lead.add(p);
        }
        return lead.isEmpty() ? 0 : width.applyAsInt(join(lead));
    }

    private static Component indented(int px, List<Piece> body) {
        Component line = join(body);
        if (px <= 0) {
            return line;
        }
        String offset = SpaceOffset.encode(px);
        return offset.isEmpty() ? line
                : Component.empty()
                        .append(Component.literal(offset)
                                .setStyle(SpaceOffset.styleFor(Style.EMPTY)))
                        .append(line);
    }

    @SafeVarargs
    private static Component join(List<Piece>... parts) {
        MutableComponent out = Component.empty();
        for (List<Piece> part : parts) {
            for (Piece p : part) {
                out.append(Component.literal(p.text()).setStyle(p.style()));
            }
        }
        return out;
    }
}
