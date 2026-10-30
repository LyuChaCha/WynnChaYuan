package com.wynnchayuan.capture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.wynnchayuan.translate.ErrorDebug;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UnappliedAudit {

    private UnappliedAudit() {}

    static final int LIMIT = 300;

    private static final int ASKED_LIMIT = 4000;

    public record Row(String src, String dst, String why) {}

    private static final Map<String, Row> ROWS = new LinkedHashMap<>();
    private static final Set<String> ASKED = ConcurrentHashMap.newKeySet();
    private static volatile boolean dirty;

    public static boolean asked(String src) {
        if (src == null) {
            return true;
        }
        if (ASKED.size() >= ASKED_LIMIT) {
            ASKED.clear();
        }
        return !ASKED.add(src);
    }

    public static void note(String src, String dst) {
        try {
            Row row = audit(src, dst);
            if (row == null) {
                return;
            }
            synchronized (ROWS) {
                if (row.equals(ROWS.get(row.src())) || (!ROWS.containsKey(row.src())
                        && ROWS.size() >= LIMIT)) {
                    return;
                }
                ROWS.put(row.src(), row);
            }
            dirty = true;
        } catch (Throwable t) {
            ErrorDebug.note("UnappliedAudit#note", null, t);
        }
    }

    public static Row audit(String src, String dst) {
        if (src == null || dst == null || src.isBlank() || dst.isBlank()) {
            return null;
        }
        String why = why(src, dst);
        if (why == null) {
            return null;
        }
        if (PlayerDataFilter.carriesPlayerData(src)) {
            return null;
        }
        return new Row(src.strip(), dst.strip(), why);
    }

    private static final Pattern NUMBER = Pattern.compile("\\{~(\\d*)}");

    public static String why(String src, String dst) {
        StringBuilder out = new StringBuilder();
        for (String token : new String[] {GlyphSplitter.GLYPH_PLACEHOLDER,
                GlyphSplitter.PLACE_PLACEHOLDER}) {
            int want = count(src, token);
            int have = count(dst, token);
            if (want != have) {
                add(out, token, want, have);
            }
        }
        int wantUsers = count(src, GlyphSplitter.PLAYER_PLACEHOLDER);
        int haveUsers = count(dst, GlyphSplitter.PLAYER_PLACEHOLDER);
        if (haveUsers > wantUsers) {
            add(out, GlyphSplitter.PLAYER_PLACEHOLDER, wantUsers, haveUsers);
        }
        int wantNumbers = count(src, GlyphSplitter.NUMBER_PLACEHOLDER);
        int plain = 0;
        int highest = 0;
        Matcher m = NUMBER.matcher(dst);
        while (m.find()) {
            if (m.group(1).isEmpty() || "0".equals(m.group(1))) {
                plain++;
            } else {
                highest = Math.max(highest, parse(m.group(1)));
            }
        }
        boolean ok = highest == 0 ? plain == wantNumbers
                : plain <= wantNumbers && highest <= wantNumbers;
        if (!ok) {
            out.append(out.length() == 0 ? "" : "; ").append(GlyphSplitter.NUMBER_PLACEHOLDER)
               .append(": src ").append(wantNumbers).append(", dst ").append(plain);
            if (highest > 0) {
                out.append(" (max {~").append(highest).append("})");
            }
        }
        return out.length() == 0 ? null : out.toString();
    }

    private static void add(StringBuilder out, String token, int want, int have) {
        out.append(out.length() == 0 ? "" : "; ").append(token)
           .append(": src ").append(want).append(", dst ").append(have);
    }

    private static int parse(String digits) {
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    private static int count(String text, String token) {
        int n = 0;
        for (int at = text.indexOf(token); at >= 0; at = text.indexOf(token, at + token.length())) {
            n++;
        }
        return n;
    }

    public static boolean takeDirty() {
        boolean was = dirty;
        dirty = false;
        return was;
    }

    public static int size() {
        synchronized (ROWS) {
            return ROWS.size();
        }
    }

    public static JsonArray toJson() {
        JsonArray out = new JsonArray();
        synchronized (ROWS) {
            for (Row row : ROWS.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("src", row.src());
                o.addProperty("dst", row.dst());
                o.addProperty("why", row.why());
                out.add(o);
            }
        }
        return out;
    }

    public static void clear() {
        ASKED.clear();
        synchronized (ROWS) {
            if (!ROWS.isEmpty()) {
                ROWS.clear();
                dirty = true;
            }
        }
    }
}
