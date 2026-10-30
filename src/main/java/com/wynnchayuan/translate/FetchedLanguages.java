package com.wynnchayuan.translate;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FetchedLanguages {

    private FetchedLanguages() {}

    private static final Set<String> DONE = ConcurrentHashMap.newKeySet();

    public static void mark(String lang) {
        if (lang != null && !lang.isBlank()) {
            DONE.add(lang);
        }
    }

    public static boolean firstTime(String lang) {
        return lang != null && !lang.isBlank() && DONE.add(lang);
    }

    static void reset() {
        DONE.clear();
    }
}
