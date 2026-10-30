package com.wynnchayuan.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class Marquee {

    private static final Map<String, String[]> last = new ConcurrentHashMap<>();

    private static final int MAX_STEP = 3;

    private static final int MIN_OVERLAP = 8;

    private Marquee() {}

    static void remember(String row, String raw, String translated) {
        if (row != null && raw != null && translated != null) {
            last.put(row, new String[] {raw, translated});
        }
    }

    static String follow(String row, String raw) {
        if (row == null || raw == null) {
            return null;
        }
        String[] before = last.get(row);
        if (before == null || !continues(before[0], raw)) {
            return null;
        }
        last.put(row, new String[] {raw, before[1]});
        return before[1];
    }

    static boolean continues(String prev, String next) {
        String p = prev.replace(" ", "");
        String n = next.replace(" ", "");
        for (int d = 0; d <= MAX_STEP && d < p.length(); d++) {
            String tail = p.substring(d);
            if (tail.length() >= MIN_OVERLAP && n.startsWith(tail)) {
                return true;
            }
        }
        return false;
    }

    static final long HOLD_START = 2000;

    static final long HOLD_END = 1500;

    static final int SPEED = 40;

    private static final Map<String, long[]> scrolls = new java.util.HashMap<>();

    private static final long FORGET_AFTER = 1000;

    static synchronized long since(String key, long now) {
        scrolls.values().removeIf(seen -> now - seen[1] > FORGET_AFTER || now < seen[0]);
        long[] seen = scrolls.computeIfAbsent(key, k -> new long[] {now, now});
        seen[1] = now;
        return Math.max(0, now - seen[0]);
    }

    static final String CHOICE_SAMPLE = "Do you remember anything fro";

    static int choiceRoom(int was, java.util.function.ToIntFunction<String> measure) {
        return Math.max(was, measure.applyAsInt(CHOICE_SAMPLE));
    }

    static int[] scroll(String key, String text, int room,
                        java.util.function.ToIntFunction<String> measure, long now) {
        if (measure.applyAsInt(text) <= room) {
            return new int[] {0, text.length()};
        }
        return window(text, room, measure, since(key, now));
    }

    static int[] window(String row, int room, java.util.function.ToIntFunction<String> measure,
                        long elapsed) {
        int length = row.length();
        int total = measure.applyAsInt(row);
        if (total <= room || length == 0) {
            return new int[] {0, length};
        }
        int over = total - room;
        long scroll = over * 1000L / SPEED;
        long cycle = HOLD_START + scroll + HOLD_END;
        long t = Math.max(0, elapsed) % cycle;
        int px;
        if (t < HOLD_START) {
            px = 0;
        } else if (t < HOLD_START + scroll) {
            px = (int) Math.min(over, (t - HOLD_START) * SPEED / 1000);
        } else {
            px = over;
        }
        int from = 0;
        while (from < length && measure.applyAsInt(row.substring(0, from)) < px) {
            from += Character.charCount(row.codePointAt(from));
        }
        if (from < length && row.charAt(from) == ' ') {
            from++;
        }
        int to = from;
        while (to < length) {
            int next = to + Character.charCount(row.codePointAt(to));
            if (measure.applyAsInt(row.substring(from, next)) > room) {
                break;
            }
            to = next;
        }
        return new int[] {from, to};
    }

    static synchronized void clear() {
        last.clear();
        scrolls.clear();
    }
}
