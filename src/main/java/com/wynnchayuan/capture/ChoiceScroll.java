package com.wynnchayuan.capture;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Wynncraft 長選項是跑馬燈，每 tick 只送 27～28 字元
public final class ChoiceScroll {

    private static final int MIN_OVERLAP = 12;

    private static final int MAX_LEN = 400;

    private static final String ENDS = ".!?…）)]」』";

    private static final Map<Integer, Row> ROWS = new ConcurrentHashMap<>();

    private ChoiceScroll() {}

    private static final class Row {
        String first;
        String full;
        String last;
        boolean done;
        int reported;
    }

    public static String feed(int row, String window) {
        if (window == null) {
            return null;
        }
        String w = window.strip();
        if (w.isEmpty()) {
            return null;
        }
        Row r = ROWS.computeIfAbsent(row, k -> new Row());
        if (r.first == null) {
            start(r, w);
            return null;
        }
        if (w.equals(r.last)) {
            return null;
        }
        if (w.equals(r.first) && r.full.length() > r.first.length()) {
            r.done = true;
            r.last = w;
            return report(r);
        }
        if (r.done && r.full.contains(w)) {
            r.last = w;
            return null;
        }
        String merged = merge(r.full, w);
        if (merged == null || merged.length() > MAX_LEN) {
            String pending = report(r);
            start(r, w);
            return pending;
        }
        boolean wasDone = r.done;
        r.full = merged;
        r.last = w;
        if (complete(merged)) {
            r.done = true;
        }
        return !wasDone && r.done ? report(r) : null;
    }

    private static void start(Row r, String w) {
        r.first = w;
        r.full = w;
        r.last = w;
        r.done = complete(w);
        r.reported = 0;
    }

    private static String report(Row r) {
        if (!r.done || r.full.length() <= r.first.length() || r.full.length() <= r.reported) {
            return null;
        }
        r.reported = r.full.length();
        return r.full;
    }

    public static String full(int row) {
        Row r = ROWS.get(row);
        if (r == null || !r.done || r.full.length() <= r.first.length()) {
            return null;
        }
        return r.full;
    }

    public static String first(int row) {
        Row r = ROWS.get(row);
        return r == null ? null : r.first;
    }

    public static void reset() {
        ROWS.clear();
    }

    private static boolean complete(String s) {
        return !s.isEmpty() && ENDS.indexOf(s.charAt(s.length() - 1)) >= 0;
    }

    static String merge(String full, String w) {
        int max = Math.min(full.length(), w.length());
        for (int k = max; k >= MIN_OVERLAP; k--) {
            if (full.regionMatches(full.length() - k, w, 0, k)) {
                return full + w.substring(k);
            }
        }
        return null;
    }
}
