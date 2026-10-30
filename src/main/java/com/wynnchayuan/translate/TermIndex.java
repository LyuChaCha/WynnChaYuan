package com.wynnchayuan.translate;

import com.wynnchayuan.translate.TranslationStore.Term;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.wynnchayuan.translate.TranslationStore.wordChar;
import static com.wynnchayuan.translate.TranslationStore.words;

final class TermIndex {

    private final TranslationIndex translations;

    TermIndex(TranslationIndex translations) {
        this.translations = translations;
    }

    private final Map<String, String> terms = new ConcurrentHashMap<>();

    private volatile int maxTermWords = 1;

    void noteTerm(String name, String translation) {
        noteTerm(name, translation, false);
    }

    void noteTerm(String name, String translation, boolean curated) {
        if ((!curated && name.length() < MIN_TERM_LENGTH) || translation.isBlank()
                || name.indexOf('{') >= 0 || name.indexOf('\n') >= 0) {
            return;
        }
        terms.put(name, translation);
        maxTermWords = Math.max(maxTermWords, name.split(" ").length);
    }

    private static final int MIN_TERM_LENGTH = 4;

    Term findTerm(String text, int from) {
        if (terms.isEmpty()) {
            return null;
        }
        for (int i = from; i < text.length(); i++) {
            if (!startsWord(text, i) || !Character.isUpperCase(text.charAt(i))) {
                continue;
            }
            int end = i;
            int bestEnd = -1;
            String best = null;
            for (int words = 0; words < maxTermWords && end <= text.length(); words++) {
                end = wordEnd(text, end);
                String hit = terms.get(text.substring(i, end));
                if (hit != null) {
                    best = hit;
                    bestEnd = end;
                }
                if (end >= text.length() || text.charAt(end) != ' ') {
                    break;
                }
                end++;
            }
            if (best != null) {
                return new Term(i, bestEnd, shownTerm(text.substring(i, bestEnd), best));
            }
        }
        return null;
    }

    String lookupTerm(String text) {
        if (text == null) {
            return null;
        }
        String key = text.strip();
        String hit = terms.get(key);
        if (hit != null) {
            return shownTerm(key, hit);
        }
        int end = key.length();
        while (end > 0 && !Character.isLetterOrDigit(key.charAt(end - 1))) {
            end--;
        }
        if (end == key.length() || end == 0) {
            return null;
        }
        String coreKey = key.substring(0, end).strip();
        String core = terms.get(coreKey);
        return core == null ? null : shownTerm(coreKey, core) + key.substring(end);
    }

    private String shownTerm(String name, String hit) {
        if (translations.store.abilityNamesWithOriginal() && translations.nameRules.ownAbilityName(name, hit)) {
            return ItemNameRules.withOriginal(hit, name);
        }
        return hit;
    }

    java.util.Set<String> termNames() {
        return java.util.Collections.unmodifiableSet(terms.keySet());
    }

    private static boolean startsWord(String text, int at) {
        return at == 0 || !wordChar(text.charAt(at - 1));
    }

    private static int wordEnd(String text, int from) {
        int i = from;
        while (i < text.length() && (wordChar(text.charAt(i))
                || text.charAt(i) == '\'' || text.charAt(i) == '-')) {
            i++;
        }
        return i;
    }
}
