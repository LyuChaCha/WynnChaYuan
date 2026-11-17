package com.wynnchayuan.capture;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChatLog {

    public static final int CAPACITY = 1000;

    public record Entry(Component original, Component translated, long at) {

        public Entry(Component original, Component translated) {
            this(original, translated, 0L);
        }

        public String forMode(boolean replace, boolean both) {
            String from = original == null ? "" : original.getString();
            if (!hasText()) {
                return from;
            }
            String zh = translated.getString();
            if (both) {
                return from + "\n" + zh;
            }
            return replace ? zh : from;
        }

        public List<Component> shown(boolean replace, boolean both) {
            if (!hasText()) {
                return List.of(original);
            }
            if (both) {
                return List.of(original, translated);
            }
            return List.of(replace ? translated : original);
        }

        public boolean hasText() {
            return translated != null && !translated.getString().isBlank();
        }
    }

    private static final ArrayList<Entry> entries = new ArrayList<>();
    private static long version;

    private ChatLog() {}

    public static synchronized void add(Component original, Component translated) {
        if (original == null || original.getString().isBlank()) {
            return;
        }
        entries.add(new Entry(original, translated, System.currentTimeMillis()));
        while (entries.size() > CAPACITY) {
            entries.remove(0);
        }
        version++;
    }

    public static synchronized List<Entry> recent() {
        List<Entry> out = new ArrayList<>(entries);
        Collections.reverse(out);
        return out;
    }

    public static synchronized List<Entry> oldestFirst() {
        return List.copyOf(entries);
    }

    public static synchronized long version() {
        return version;
    }

    public static synchronized void clear() {
        entries.clear();
        version++;
    }

    public static synchronized int size() {
        return entries.size();
    }
}
