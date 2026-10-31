package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;

import java.util.List;
import java.util.Locale;

// 照抄 Wynntils StringUtils.partialMatch（子序列）
public final class SearchMatch {

    private SearchMatch() {}

    public static boolean alsoMatches(String content, String query) {
        try {
            noteHook();
            return alsoMatches(content, query,
                    WynnChaYuan.config(), WynnChaYuan.translations());
        } catch (Throwable t) {
            ErrorDebug.note("SearchMatch#alsoMatches", null, t);
            return false;
        }
    }

    private static volatile boolean noted = false;

    private static void noteHook() {
        if (noted) {
            return;
        }
        noted = true;
        WynnChaYuan.store().noteEvent("search.hook");
    }

    static boolean alsoMatches(String content, String query,
                               CollectorConfig config, TranslationStore store) {
        if (content == null || query == null || query.isEmpty()
                || config == null || store == null) {
            return false;
        }
        if (!config.wynntilsUi()) {
            return false;
        }
        String translated = store.lookup(content.strip());
        if (translated == null || translated.equals(content)) {
            return false;
        }
        return partialMatch(translated, query);
    }

    // 跟 Wynntils 一樣用 Locale.ROOT，土耳其語會把 I 折成 ı
    static boolean partialMatch(String content, String query) {
        String needle = query.toLowerCase(Locale.ROOT);
        String haystack = content.toLowerCase(Locale.ROOT);
        int from = 0;
        for (int i = 0; i < needle.length(); i++) {
            int at = haystack.indexOf(needle.charAt(i), from);
            if (at < 0) {
                return false;
            }
            from = at + 1;
        }
        return true;
    }

    public static boolean nameAlsoMatches(String itemName, List<String> tokens) {
        try {
            noteHook();
            return nameAlsoMatches(itemName, tokens, WynnChaYuan.translations());
        } catch (Throwable t) {
            ErrorDebug.note("SearchMatch#nameAlsoMatches", null, t);
            return false;
        }
    }

    static boolean nameAlsoMatches(String itemName, List<String> tokens, TranslationStore store) {
        if (itemName == null || tokens == null || tokens.isEmpty() || store == null) {
            return false;
        }
        String needle = String.join(" ", tokens).strip();
        if (needle.isEmpty()) {
            return false;
        }
        String translated = store.lookup(itemName.strip());
        if (translated == null || translated.isBlank() || translated.equals(itemName)) {
            return false;
        }
        return translated.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }
}
