package com.wynnchayuan.capture;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class ChatRows {

    static final long WINDOW_MS = 1000;

    static final int MAX_ROWS = 24;

    public record Row(String template, String ctx) {}

    private final Predicate<String> whole;
    private final Predicate<String> longer;

    private final List<Row> held = new ArrayList<>();
    private String run;
    private long last;

    public ChatRows(Predicate<String> whole, Predicate<String> longer) {
        this.whole = whole;
        this.longer = longer;
    }

    public synchronized List<Row> offer(String template, String ctx, long now) {
        List<Row> out = new ArrayList<>();
        if (template == null || template.isBlank()) {
            return out;
        }
        String row = template.strip();
        if (run != null && (now - last > WINDOW_MS || held.size() >= MAX_ROWS)) {
            release(out);
        }
        if (run != null) {
            for (String joined : List.of(run + " " + continuation(row), run + "\n" + row)) {
                boolean more = longer.test(joined);
                if (!more && !whole.test(joined)) {
                    continue;
                }
                if (more) {
                    run = joined;
                    held.add(new Row(row, ctx));
                    last = now;
                } else {
                    clear();
                }
                return out;
            }
            release(out);
        }
        if (longer.test(row)) {
            run = row;
            held.add(new Row(row, ctx));
            last = now;
        } else {
            out.add(new Row(row, ctx));
        }
        return out;
    }

    public synchronized List<Row> settle(long now) {
        List<Row> out = new ArrayList<>();
        if (run != null && now - last > WINDOW_MS) {
            release(out);
        }
        return out;
    }

    synchronized int holding() {
        return held.size();
    }

    private void release(List<Row> out) {
        if (!whole.test(run)) {
            out.addAll(held);
        }
        clear();
    }

    private void clear() {
        held.clear();
        run = null;
    }

    private static String continuation(String row) {
        String glyph = GlyphSplitter.GLYPH_PLACEHOLDER;
        int at = 0;
        while (at < row.length()) {
            if (row.startsWith(glyph, at)) {
                at += glyph.length();
            } else if (Character.isWhitespace(row.charAt(at))) {
                at++;
            } else {
                break;
            }
        }
        return row.substring(at);
    }
}
