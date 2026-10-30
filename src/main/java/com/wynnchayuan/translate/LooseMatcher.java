package com.wynnchayuan.translate;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.wynnchayuan.translate.TranslationStore.indentOf;

final class LooseMatcher {

    private final TranslationIndex translations;

    LooseMatcher(TranslationIndex translations) {
        this.translations = translations;
    }

    private static final char MARK_YES = '✔';

    private static final char MARK_NO = '✖';

    private final Map<String, String> marked = new ConcurrentHashMap<>();

    private static boolean hasMark(String text) {
        return text.indexOf(MARK_YES) >= 0 || text.indexOf(MARK_NO) >= 0;
    }

    static String sameMarks(String text) {
        return text.replace(MARK_YES, MARK_NO);
    }

    void noteMarked(String src, String dst) {
        if (hasMark(src) && countMarks(src) == countMarks(dst)) {
            translations.loader.layered(marked, "marked", sameMarks(src), dst);
        }
    }

    private static int countMarks(String text) {
        int n = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == MARK_YES || c == MARK_NO) {
                n++;
            }
        }
        return n;
    }

    String lookupMarked(String key) {
        if (!hasMark(key)) {
            return null;
        }
        String hit = marked.get(sameMarks(key));
        if (hit == null || countMarks(hit) != countMarks(key)) {
            return null;
        }
        StringBuilder out = new StringBuilder(hit);
        int at = 0;
        for (int i = 0; i < out.length(); i++) {
            char c = out.charAt(i);
            if (c == MARK_YES || c == MARK_NO) {
                out.setCharAt(i, markAt(key, at++));
            }
        }
        return out.toString();
    }

    private static char markAt(String text, int n) {
        int seen = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == MARK_YES || c == MARK_NO) && seen++ == n) {
                return c;
            }
        }
        return MARK_NO;
    }

    String lookupLoose(String key) {
        String src = looseSource(key);
        if (src == null) {
            return null;
        }
        if (!translations.store.translatesNames() && translations.nameRules.gearOnly(src)) {
            return null;
        }
        if (translations.nameRules.peeked(src)) {
            return null;
        }
        return translations.entries.get(src);
    }

    String looseSource(String key) {
        if (key == null) {
            return null;
        }
        String loosened = loosen(key);
        if (!looseEnough(loosened) || looseClash.contains(loosened)) {
            return null;
        }
        return loose.get(loosened);
    }

    void noteLoose(String src, String dst) {
        String loosened = loosen(src);
        if (!looseEnough(loosened)) {
            return;
        }
        boolean firstHere = translations.loader.thisLayer.add("loose" + '\0' + loosened);
        String before = loose.get(loosened);
        if (before == null || firstHere) {
            loose.put(loosened, src);
            looseDst.put(loosened, dst);
            looseClash.remove(loosened);
        } else if (!before.equals(src) && !dst.equals(looseDst.get(loosened))) {
            looseClash.add(loosened);
        }
    }

    private static boolean looseEnough(String loosened) {
        return loosened.length() >= MIN_LOOSE_LENGTH && loosened.indexOf(' ') >= 0;
    }

    static String loosen(String text) {
        StringBuilder out = new StringBuilder(text.length());
        boolean space = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '‘' || c == '’' || c == 'ʼ') {
                c = '\'';
            } else if (c == '“' || c == '”') {
                c = '"';
            }
            if (Character.isWhitespace(c)) {
                space = out.length() > 0;
                continue;
            }
            if (space) {
                out.append(' ');
                space = false;
            }
            if (c == '…') {
                out.append("...");
            } else {
                out.append(c);
            }
        }
        String flat = out.toString().replaceAll("\\.{2,}", "...");
        int end = flat.length();
        while (end > 0 && ".!?~".indexOf(flat.charAt(end - 1)) >= 0) {
            end--;
        }
        return flat.substring(0, end).strip().toLowerCase(java.util.Locale.ROOT);
    }

    private static final int MIN_LOOSE_LENGTH = 12;

    private final Map<String, String> loose = new ConcurrentHashMap<>();

    private final Map<String, String> looseDst = new ConcurrentHashMap<>();

    private final java.util.Set<String> looseClash = ConcurrentHashMap.newKeySet();

    private final Map<String, String> unindented = new ConcurrentHashMap<>();

    void noteIndented(String src, String dst) {
        int n = indentOf(src);
        if (n > 0) {
            translations.loader.layered(unindented, "unindented",
                    src.substring(n), dst.substring(indentOf(dst)));
        }
    }

    String lookupIndented(String key) {
        int indent = indentOf(key);
        if (indent == 0) {
            return null;
        }
        String body = unindented.get(key.substring(indent));
        return body == null ? null : key.substring(0, indent) + body;
    }
}
