package com.wynnchayuan.translate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class MarketSearch {

    private final Map<String, Set<String>> index = new TreeMap<>();

    private final Map<String, String> shown = new java.util.HashMap<>();

    private static final int MIN_NAME = 2;

    public void add(String english, String chinese) {
        if (english == null || chinese == null) {
            return;
        }
        String enName = quantityName(english);
        String zhName = quantityName(chinese);
        if (enName != null && zhName != null) {
            english = enName;
            chinese = zhName;
        }
        String zh = key(chinese);
        String en = bare(english);
        if (zh.length() < MIN_NAME || en.isEmpty() || zh.equals(key(en))
                || !looksLikeName(en)) {
            return;
        }
        index.computeIfAbsent(zh, k -> new LinkedHashSet<>()).add(en);
        shown.putIfAbsent(zh, bare(chinese));
        reverse = null;
    }

    private volatile Map<String, String> reverse;

    public String shownFor(String english) {
        if (english == null) {
            return null;
        }
        Map<String, String> map = reverse;
        if (map == null) {
            map = new java.util.HashMap<>();
            for (Map.Entry<String, Set<String>> e : index.entrySet()) {
                String zh = shown.getOrDefault(e.getKey(), e.getKey());
                for (String en : e.getValue()) {
                    map.putIfAbsent(singular(en).toLowerCase(java.util.Locale.ROOT), zh);
                }
            }
            reverse = map;
        }
        return map.get(singular(english.strip()).toLowerCase(java.util.Locale.ROOT));
    }

    public void addListed(String english, String chinese) {
        if (english == null || chinese == null) {
            return;
        }
        if (quantityName(english) == null && !LISTED.matcher(bare(english)).matches()) {
            return;
        }
        add(english, chinese);
    }

    private static final java.util.regex.Pattern LISTED = java.util.regex.Pattern.compile(
            "^(?!(?:Remove|Upgrade|Trade|Get|Buy|Sell|Only|Incompatible|Untradable|Tradable"
                    + "|Available|Converts)\\b)"
                    + "(?:[A-Z][A-Za-z'-]* )*"
                    + "(?:Amplifier|Insulator|Simulator|Share|Emerald|Powder|Rune|Key)s?"
                    + "(?: [IVX]+)?$");

    static String quantityName(String text) {
        java.util.regex.Matcher m = QUANTITY.matcher(text.strip());
        return m.matches() ? m.group(1).strip() : null;
    }

    private static final java.util.regex.Pattern QUANTITY =
            java.util.regex.Pattern.compile("^\\{~\\} ?[x×] (.+?)(?: *(?:\\{#\\})+)?$");

    public String exact(String typed) {
        if (typed == null) {
            return null;
        }
        String zh = key(typed);
        if (zh.length() < MIN_NAME) {
            return null;
        }
        Set<String> hit = index.get(zh);
        if (hit == null) {
            return null;
        }
        List<String> out = singulars(hit);
        return out.size() == 1 ? out.get(0) : null;
    }

    private static String bare(String english) {
        int at = 0;
        while (at < english.length()) {
            int cp = english.codePointAt(at);
            if (Character.isWhitespace(cp) || BULLETS.indexOf(cp) >= 0
                    || Character.getType(cp) == Character.OTHER_SYMBOL) {
                at += Character.charCount(cp);
            } else {
                break;
            }
        }
        int end = english.length();
        while (end > at && (english.charAt(end - 1) == ':'
                || Character.isWhitespace(english.charAt(end - 1)))) {
            end--;
        }
        return english.substring(at, end);
    }

    private static final String BULLETS = "-–—•*";

    public void clear() {
        index.clear();
        shown.clear();
        reverse = null;
    }

    public record Suggestion(String chinese, String english) {}

    public List<Suggestion> suggestions(String typed) {
        List<Suggestion> out = new ArrayList<>();
        if (typed == null) {
            return out;
        }
        String zh = key(typed);
        if (zh.isEmpty()) {
            return out;
        }
        List<String> keys = new ArrayList<>();
        for (String k : index.keySet()) {
            if (k.contains(zh)) {
                keys.add(k);
            }
        }
        keys.sort((a, b) -> {
            boolean ea = a.equals(zh);
            boolean eb = b.equals(zh);
            if (ea != eb) {
                return ea ? -1 : 1;
            }
            return Integer.compare(a.length(), b.length());
        });
        Set<String> seen = new java.util.HashSet<>();
        for (String k : keys) {
            for (String en : index.get(k)) {
                String name = singular(en);
                if (seen.add(name.toLowerCase(java.util.Locale.ROOT))) {
                    out.add(new Suggestion(shown.getOrDefault(k, k), name));
                }
            }
        }
        return out;
    }

    public int size() {
        return index.size();
    }

    private static boolean looksLikeName(String english) {
        if (english.length() > MAX_NAME || english.indexOf('\n') >= 0) {
            return false;
        }
        if (english.indexOf('{') >= 0 || english.indexOf('}') >= 0) {
            return false;
        }
        if (english.endsWith(".") || english.endsWith("!") || english.endsWith("?")) {
            return false;
        }
        String[] words = english.split(" ");
        return words.length <= MAX_WORDS && titleCase(words);
    }

    private static boolean titleCase(String[] words) {
        int lower = 0;
        for (String word : words) {
            if (word.isEmpty() || !Character.isLowerCase(word.codePointAt(0))) {
                continue;
            }
            if (word.length() > MAX_PARTICLE) {
                return false;
            }
            lower++;
        }
        return lower * 2 <= words.length;
    }

    private static final int MAX_PARTICLE = 4;

    private static final int MAX_NAME = 40;

    private static final int MAX_WORDS = 5;

    public String lookup(String chinese) {
        List<String> hits = candidates(chinese);
        return hits.size() == 1 ? hits.get(0) : null;
    }

    public List<String> candidates(String chinese) {
        List<String> out = new ArrayList<>();
        if (chinese == null) {
            return out;
        }
        String zh = key(chinese);
        if (zh.length() < MIN_NAME) {
            return out;
        }
        Set<String> exact = index.get(zh);
        if (exact != null) {
            return singulars(exact);
        }
        Set<String> loose = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> e : index.entrySet()) {
            if (e.getKey().contains(zh)) {
                loose.addAll(e.getValue());
            }
        }
        return singulars(loose);
    }

    private static List<String> singulars(Set<String> names) {
        Set<String> out = new LinkedHashSet<>();
        for (String name : names) {
            out.add(singular(name));
        }
        return covered(new ArrayList<>(out));
    }

    private static List<String> covered(List<String> names) {
        List<String> out = new ArrayList<>(names.size());
        for (String name : names) {
            String lower = name.toLowerCase(java.util.Locale.ROOT);
            boolean redundant = false;
            for (String other : names) {
                if (other.length() < name.length()
                        && lower.contains(other.toLowerCase(java.util.Locale.ROOT))) {
                    redundant = true;
                    break;
                }
            }
            if (!redundant) {
                out.add(name);
            }
        }
        return out;
    }

    public static String singular(String name) {
        int at = name.lastIndexOf(' ');
        String head = at < 0 ? "" : name.substring(0, at + 1);
        String last = name.substring(at + 1);
        if (last.length() < MIN_PLURAL || !last.endsWith("s")) {
            return name;
        }
        char before = last.charAt(last.length() - 2);
        if (before == 's' || before == '\'' || before == '’') {
            return name;
        }
        return head + last.substring(0, last.length() - 1);
    }

    private static final int MIN_PLURAL = 4;

    static String key(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                int close = text.indexOf('}', i);
                if (close > 0 && close - i <= 4) {
                    i = close;
                    continue;
                }
            }
            if (Character.isWhitespace(c)) {
                continue;
            }
            out.append(Character.toLowerCase(c));
        }
        return out.toString();
    }
}
