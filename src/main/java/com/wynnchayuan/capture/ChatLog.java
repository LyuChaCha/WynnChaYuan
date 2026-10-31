package com.wynnchayuan.capture;

import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChatLog {

    public static final int CAPACITY = 200;

    public record Entry(Component original, Component translated) {

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

        public boolean hasText() {
            return translated != null && !translated.getString().isBlank();
        }
    }

    private static final ArrayList<Entry> entries = new ArrayList<>();

    private ChatLog() {}

    public static synchronized void add(Component original, Component translated) {
        if (original == null || original.getString().isBlank()) {
            return;
        }
        entries.add(new Entry(original, translated));
        while (entries.size() > CAPACITY) {
            entries.remove(0);
        }
    }

    public static synchronized List<Entry> recent() {
        List<Entry> out = new ArrayList<>(entries);
        Collections.reverse(out);
        return out;
    }

    public static synchronized void clear() {
        entries.clear();
    }

    public static synchronized int size() {
        return entries.size();
    }
}
