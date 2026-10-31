package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.translate.TranslationStore.Guess;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.wynnchayuan.translate.TranslationStore.words;

final class PlayerNameGuesser {

    private final TranslationIndex translations;

    PlayerNameGuesser(TranslationIndex translations) {
        this.translations = translations;
    }

    private static final int NAME_EVIDENCE = 12;

    private static final int MAX_PLAYER_NAME = 24;

    private final List<String[]> playerKeys = new ArrayList<>();

    void notePlayerKey(String key) {
        int at = key.indexOf(GlyphSplitter.PLAYER_PLACEHOLDER);
        if (at < 0 || key.indexOf(GlyphSplitter.PLAYER_PLACEHOLDER,
                                 at + GlyphSplitter.PLAYER_PLACEHOLDER.length()) >= 0) {
            return;
        }
        String pre = key.substring(0, at);
        if (pre.indexOf('{') >= 0) {
            return;
        }
        String post = key.substring(at + GlyphSplitter.PLAYER_PLACEHOLDER.length());
        int stop = post.indexOf('{');
        if (stop >= 0) {
            post = post.substring(0, stop);
        }
        if (post.length() < NAME_EVIDENCE) {
            return;
        }
        playerKeys.add(new String[] {pre, post, stop >= 0 ? "open" : ""});
    }

    Guess playerNameIn(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        Guess found = null;
        for (String[] key : playerKeys) {
            String name = nameBetween(raw, key[0], key[1], !key[2].isEmpty());
            if (name == null) {
                continue;
            }
            if (found == null) {
                found = new Guess(name, key[0] + '\u0000' + key[1]);
            } else if (!found.name().equals(name)) {
                return null;
            }
        }
        return found;
    }

    private String nameBetween(String raw, String pre, String post, boolean open) {
        if (!raw.startsWith(pre)) {
            return null;
        }
        String rest = raw.substring(pre.length());
        if (open) {
            int at = rest.indexOf(post);
            if (at <= 0 || at > post.length()) {
                return null;
            }
            String name = rest.substring(0, at);
            return plausibleName(name) ? name : null;
        }
        int k = post.length();
        if (rest.length() > k && rest.regionMatches(rest.length() - k, post, 0, k)) {
            String name = rest.substring(0, rest.length() - k);
            if (name.length() > k) {
                return null;
            }
            return plausibleName(name) ? name : null;
        }
        return null;
    }

    private static final int MAX_NAME_WORDS = 3;

    private final Map<String, Integer> wordCount = new ConcurrentHashMap<>();

    private static final int COMMON_WORD = 2;

    void noteWords(String key) {
        for (String word : key.split("[^A-Za-z0-9_]+")) {
            if (word.length() >= 2) {
                wordCount.merge(word.toLowerCase(Locale.ROOT), 1, Integer::sum);
            }
        }
    }

    private boolean plausibleName(String name) {
        if (name.length() < 3 || name.length() > MAX_PLAYER_NAME) {
            return false;
        }
        if (name.strip().length() != name.length()) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != ' ') {
                return false;
            }
        }
        String[] words = name.split(" ");
        if (words.length > MAX_NAME_WORDS) {
            return false;
        }
        boolean own = false;
        for (String word : words) {
            if (wordCount.getOrDefault(word.toLowerCase(Locale.ROOT), 0) < COMMON_WORD) {
                own = true;
                break;
            }
        }
        if (!own) {
            return false;
        }
        return !translations.entries.containsKey(name) && !translations.questIndex.speakers.containsKey(name);
    }
}
