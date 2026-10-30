package com.wynnchayuan.capture;

public final class CurrentQuest {

    private static final String SEPARATORS = "/#";

    private static volatile String name;

    private CurrentQuest() {}

    public static void set(String value) {
        name = value == null || value.isBlank() ? null : sanitise(value);
    }

    public static String get() {
        return name;
    }

    public static String tag(String base) {
        String current = name;
        return current == null ? base : base + "/" + current;
    }

    public static String tag(String base, String speaker) {
        return tag(base, speaker, true);
    }

    public static String tag(String base, String speaker, boolean withQuest) {
        String tagged = withQuest ? tag(base) : base;
        String who = speakerName(speaker);
        return who == null ? tagged : tagged + "#" + sanitise(who);
    }

    public static String speakerName(String raw) {
        if (raw == null) {
            return null;
        }
        int at = 0;
        while (at < raw.length()) {
            int cp = raw.codePointAt(at);
            if (!isPlateJunk(cp) && !Character.isWhitespace(cp)) {
                break;
            }
            at += Character.charCount(cp);
        }
        int end = at;
        while (end < raw.length()) {
            int cp = raw.codePointAt(end);
            if (cp == '\n' || cp == '\r' || isPlateJunk(cp)) {
                break;
            }
            end += Character.charCount(cp);
        }
        String name = raw.substring(at, end).strip();
        return name.isEmpty() ? null : name;
    }

    private static boolean isPlateJunk(int cp) {
        return GlyphSplitter.isGlyphCodePoint(cp)
                || com.wynnchayuan.translate.SpaceOffset.isOffset(cp);
    }

    private static String sanitise(String value) {
        StringBuilder sb = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            sb.append(SEPARATORS.indexOf(c) >= 0 ? ' ' : c);
        }
        return sb.toString().strip();
    }
}
