package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class TextSplit {

    private TextSplit() {}

    private record Glyph(int cp, Style style) {}

    private static List<Glyph> glyphs(Component text) {
        List<Glyph> out = new ArrayList<>();
        text.visit((style, piece) -> {
            piece.codePoints().forEach(cp -> out.add(new Glyph(cp, style)));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static Component build(List<Glyph> glyphs) {
        MutableComponent out = Component.empty();
        StringBuilder run = new StringBuilder();
        Style style = null;
        for (Glyph g : glyphs) {
            if (style != null && !style.equals(g.style())) {
                out.append(Component.literal(run.toString()).withStyle(style));
                run.setLength(0);
            }
            style = g.style();
            run.appendCodePoint(g.cp());
        }
        if (run.length() > 0) {
            out.append(Component.literal(run.toString()).withStyle(style));
        }
        return out;
    }

    public static Component oneLine(Component text) {
        if (text == null || text.getString().indexOf('\n') < 0) {
            return text;
        }
        List<Glyph> in = glyphs(text);
        List<Glyph> out = new ArrayList<>(in.size());
        for (int i = 0; i < in.size(); i++) {
            Glyph g = in.get(i);
            if (g.cp() != '\n') {
                out.add(g);
                continue;
            }
            int next = i + 1;
            while (next < in.size() && Character.isWhitespace(in.get(next).cp())) {
                next++;
            }
            while (!out.isEmpty() && out.get(out.size() - 1).cp() == ' ') {
                out.remove(out.size() - 1);
            }
            boolean glue = out.isEmpty() || next >= in.size()
                    || wide(out.get(out.size() - 1).cp()) || wide(in.get(next).cp());
            if (!glue) {
                out.add(new Glyph(' ', g.style()));
            }
            i = next - 1;
        }
        return build(out);
    }

    public static List<Component> lines(Component text) {
        List<Component> out = new ArrayList<>();
        List<Glyph> line = new ArrayList<>();
        for (Glyph g : glyphs(text)) {
            if (g.cp() == '\n') {
                out.add(build(trim(line)));
                line = new ArrayList<>();
            } else {
                line.add(g);
            }
        }
        out.add(build(trim(line)));
        return out;
    }

    public static Component join(List<Component> lines) {
        MutableComponent all = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                all.append(Component.literal("\n"));
            }
            all.append(lines.get(i));
        }
        return oneLine(all);
    }

    public static List<Component> split(Component whole, int rows) {
        List<Glyph> all = glyphs(whole);
        List<List<Glyph>> atoms = new ArrayList<>();
        List<Glyph> current = new ArrayList<>();
        for (Glyph g : all) {
            if (!current.isEmpty() && breakable(current.get(current.size() - 1).cp(), g.cp())) {
                atoms.add(current);
                current = new ArrayList<>();
            }
            current.add(g);
        }
        if (!current.isEmpty()) {
            atoms.add(current);
        }
        int left = 0;
        for (List<Glyph> atom : atoms) {
            left += width(atom);
        }
        List<Component> out = new ArrayList<>(rows);
        int at = 0;
        for (int row = 0; row < rows; row++) {
            List<Glyph> line = new ArrayList<>();
            if (row == rows - 1) {
                while (at < atoms.size()) {
                    line.addAll(atoms.get(at++));
                }
            } else {
                int target = (int) Math.ceil(left / (double) (rows - row));
                int used = 0;
                while (at < atoms.size()) {
                    int w = width(atoms.get(at));
                    if (used > 0 && used + w > target) {
                        break;
                    }
                    line.addAll(atoms.get(at++));
                    used += w;
                }
                left -= used;
            }
            out.add(build(trim(line)));
        }
        return out;
    }

    private static List<Glyph> trim(List<Glyph> line) {
        int from = 0;
        int to = line.size();
        while (from < to && line.get(from).cp() == ' ') {
            from++;
        }
        while (to > from && line.get(to - 1).cp() == ' ') {
            to--;
        }
        return line.subList(from, to);
    }

    private static int width(List<Glyph> atom) {
        int w = 0;
        for (Glyph g : atom) {
            w += wide(g.cp()) ? 2 : 1;
        }
        return w;
    }

    private static boolean breakable(int a, int b) {
        if (CLOSERS.indexOf(b) >= 0 || OPENERS.indexOf(a) >= 0) {
            return false;
        }
        if (a == ' ' || b == ' ') {
            return true;
        }
        return wide(a) || wide(b);
    }

    private static final String CLOSERS = "，。！？、；：）」』】〉》］｝,.!?;:)]}%％…‥ー";
    private static final String OPENERS = "（「『【〈《［｛([{";

    public static boolean wide(int cp) {
        return (cp >= 0x2E80 && cp <= 0x9FFF)
                || (cp >= 0xAC00 && cp <= 0xD7AF)
                || (cp >= 0xF900 && cp <= 0xFAFF)
                || (cp >= 0xFF00 && cp <= 0xFF60)
                || (cp >= 0xFFE0 && cp <= 0xFFE6)
                || (cp >= 0x20000 && cp <= 0x3FFFF);
    }
}
