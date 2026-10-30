package com.wynnchayuan.capture;

public final class DialogueBuffer {

    private static final long SETTLE_MS = 1500;

    private String shape = "";

    private String pending = "";

    private long lastChange = 0;

    public synchronized String offer(String raw, String template) {
        if (raw == null || template == null) {
            return null;
        }
        String next = shape(raw);
        String clean = trimCursor(template);
        if (next.isEmpty() || clean.isEmpty()) {
            return null;
        }
        if (next.equals(shape)) {
            pending = clean;
            return null;
        }
        if (shape.isEmpty() || next.startsWith(shape)) {
            shape = next;
            pending = clean;
            lastChange = System.currentTimeMillis();
            return null;
        }
        String done = pending;
        shape = next;
        pending = clean;
        lastChange = System.currentTimeMillis();
        return done;
    }

    private static String shape(String raw) {
        StringBuilder out = new StringBuilder(raw.length());
        raw.codePoints()
           .filter(cp -> !Character.isWhitespace(cp))
           .filter(cp -> !GlyphSplitter.isGlyphCodePoint(cp))
           .forEach(out::appendCodePoint);
        return out.toString();
    }

    private static String trimCursor(String text) {
        String out = text;
        while (true) {
            String was = out;
            if (out.endsWith(GlyphSplitter.GLYPH_PLACEHOLDER)) {
                out = out.substring(0,
                        out.length() - GlyphSplitter.GLYPH_PLACEHOLDER.length());
            } else if (!out.isEmpty() && GlyphSplitter.isGlyphCodePoint(
                    out.codePointBefore(out.length()))) {
                out = out.substring(0, out.length()
                        - Character.charCount(out.codePointBefore(out.length())));
            }
            out = out.stripTrailing();
            if (out.equals(was)) {
                return out.strip();
            }
        }
    }

    public synchronized String flush() {
        if (pending.isEmpty()) {
            return null;
        }
        String done = pending;
        pending = "";
        shape = "";
        lastChange = 0;
        return done;
    }

    public synchronized String flushIfSettled() {
        if (pending.isEmpty() || System.currentTimeMillis() - lastChange < SETTLE_MS) {
            return null;
        }
        return flush();
    }
}
