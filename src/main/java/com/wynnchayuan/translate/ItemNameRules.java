package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.wynnchayuan.translate.TranslationStore.ICON;
import static com.wynnchayuan.translate.TranslationStore.bareName;
import static com.wynnchayuan.translate.TranslationStore.isToolName;
import static com.wynnchayuan.translate.TranslationStore.originalOf;

final class ItemNameRules {

    private final TranslationIndex translations;

    ItemNameRules(TranslationIndex translations) {
        this.translations = translations;
    }

    final java.util.Set<String> gearNameKeys = new java.util.HashSet<>();

    final java.util.Set<String> nameKeys =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    final java.util.Set<String> plainNameKeys =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    boolean peeked(String key) {
        return translations.store.peeksPlainNames() && (plainNameKeys.contains(key) || isToolName(key));
    }

    boolean isPlainName(String key) {
        return key != null && plainNameKeys.contains(key.strip());
    }

    final java.util.Set<String> otherOwners = new java.util.HashSet<>();

    final java.util.Set<String> nonItemOwners = new java.util.HashSet<>();

    final Map<String, java.util.Set<String>> itemNameDsts = new ConcurrentHashMap<>();

    final Map<String, java.util.Set<String>> abilityNameDsts = new ConcurrentHashMap<>();

    void noteBareName(String key) {
        if (key.indexOf(ICON) >= 0) {
            bareNames.putIfAbsent(bareName(key), key);
        }
    }

    boolean ownAbilityName(String key, String hit) {
        java.util.Set<String> own = abilityNameDsts.get(key);
        return own != null && hit != null && !hit.strip().equals(key) && own.contains(hit);
    }

    boolean isBareGearName(String key) {
        if (key == null) {
            return false;
        }
        String k = key.strip();
        return gearNameKeys.contains(k) || (!translations.store.translatesNames() && nameKeys.contains(k));
    }

    boolean gearOnly(String key) {
        return nameKeys.contains(key) && !otherOwners.contains(key);
    }

    private boolean plainOnly(String key) {
        return plainNameKeys.contains(key) && !nonItemOwners.contains(key);
    }

    boolean getsOriginal(String key) {
        return gearOnly(key) || plainOnly(key);
    }

    boolean ownItemName(String key, String hit) {
        java.util.Set<String> own = itemNameDsts.get(key);
        return own != null && hit != null && own.contains(hit);
    }

    boolean itemNameOnly(String key) {
        return key != null && getsOriginal(key.strip());
    }

    boolean gearNameOnly(String key) {
        return key != null && gearOnly(key.strip());
    }

    boolean isItemName(String key) {
        if (key == null) {
            return false;
        }
        String k = key.strip();
        return nameKeys.contains(k) || gearNameKeys.contains(k) || plainNameKeys.contains(k);
    }

    static String withOriginal(String hit, String key) {
        String original = originalOf(key);
        if (original == null) {
            return hit;
        }
        int tail = hit.length();
        while (tail >= ICON.length() && hit.startsWith(ICON, tail - ICON.length())) {
            tail -= ICON.length();
        }
        return hit.substring(0, tail) + " (" + original + ")" + hit.substring(tail);
    }

    String abilityLabelWithOriginal(String key, String hit) {
        if (key.length() < 2 || !isLabelColon(key.charAt(key.length() - 1))) {
            return null;
        }
        String hitCore = hit.stripTrailing();
        if (hitCore.isEmpty() || !isLabelColon(hitCore.charAt(hitCore.length() - 1))) {
            return null;
        }
        String name = key.substring(0, key.length() - 1).strip();
        String own = hitCore.substring(0, hitCore.length() - 1).strip();
        if (!ownAbilityName(name, own)) {
            return null;
        }
        return withOriginal(own, name) + hitCore.charAt(hitCore.length() - 1);
    }

    private static boolean isLabelColon(char c) {
        return c == ':' || c == '：';
    }

    private final Map<String, String> bareNames = new ConcurrentHashMap<>();

    int appendedOriginalAt(String text) {
        if (!translations.store.namesWithOriginal() || text == null) {
            return -1;
        }
        int end = text.length();
        while (end >= ICON.length() && text.startsWith(ICON, end - ICON.length())) {
            end -= ICON.length();
        }
        if (end == 0 || text.charAt(end - 1) != ')') {
            return -1;
        }
        int at = text.lastIndexOf(" (", end);
        if (at < 0) {
            return -1;
        }
        return isAppendedOriginal(text.substring(at + 2, end - 1),
                text.substring(0, at)) ? at : -1;
    }

    List<int[]> keptOriginals(String text) {
        List<int[]> out = new java.util.ArrayList<>();
        if (text == null) {
            return out;
        }
        int at = appendedOriginalAt(text);
        if (at >= 0) {
            out.add(new int[] {at, text.length()});
        }
        if (translations.store.pageNameMode() != com.wynnchayuan.CollectorConfig.ItemNames.ON) {
            out.addAll(LootrunNames.spans(text));
        }
        out.addAll(abilityOriginalSpans(text));
        return out;
    }

    List<int[]> abilityOriginalSpans(String text) {
        if (!translations.store.abilityNamesWithOriginal() || text == null || text.indexOf('(') < 0) {
            return List.of();
        }
        List<int[]> out = new java.util.ArrayList<>();
        int open = text.indexOf('(');
        while (open >= 0) {
            int close = text.indexOf(')', open + 1);
            if (close < 0) {
                break;
            }
            java.util.Set<String> own = abilityNameDsts.get(text.substring(open + 1, close));
            boolean spaced = open > 0 && text.charAt(open - 1) == ' ';
            int start = spaced ? open - 1 : open;
            if (own != null && follows(text.substring(0, start), own, spaced)) {
                out.add(new int[] {start, close + 1});
            }
            open = text.indexOf('(', open + 1);
        }
        return out;
    }

    private static boolean follows(String before, java.util.Set<String> own, boolean spaced) {
        if (before.isBlank()) {
            return true;
        }
        if (!spaced) {
            return false;
        }
        String lead = before.strip();
        for (String dst : own) {
            if (!dst.isEmpty() && (before.endsWith(dst) || dst.endsWith(lead))) {
                return true;
            }
        }
        return false;
    }

    int[] appendedOriginalSpan(String text) {
        if (!translations.store.namesWithOriginal() || text == null) {
            return null;
        }
        int at = text.indexOf(" (");
        while (at >= 0) {
            int close = text.indexOf(')', at + 2);
            if (close < 0) {
                return null;
            }
            if (isAppendedOriginal(text.substring(at + 2, close), text.substring(0, at))) {
                return new int[] {at, close + 1};
            }
            at = text.indexOf(" (", at + 2);
        }
        return null;
    }

    private boolean isAppendedOriginal(String inner, String before) {
        if (isAppendedOriginal(inner)) {
            return true;
        }
        if (before == null) {
            return false;
        }
        String own = translations.gearOwnName(inner);
        if (own != null) {
            return before.endsWith(own);
        }
        java.util.Set<String> item = itemNameDsts.get(inner);
        if (item != null) {
            for (String dst : item) {
                if (before.endsWith(dst)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isAppendedOriginal(String inner) {
        if (getsOriginal(inner)) {
            return true;
        }
        String templated = inner.indexOf('{') >= 0
                ? TranslationIndex.NUMBERED.matcher(inner).replaceAll(java.util.regex.Matcher.quoteReplacement(
                        GlyphSplitter.NUMBER_PLACEHOLDER))
                : GlyphSplitter.parametrizeNumbers(inner);
        if (!templated.equals(inner) && getsOriginal(templated)) {
            return true;
        }
        String wrapped = bareNames.get(templated);
        if (wrapped != null && getsOriginal(wrapped)) {
            return true;
        }
        if (inner.startsWith(SHINY)) {
            String rest = inner.substring(SHINY.length()).strip();
            return gearNameKeys.contains(rest) || nameKeys.contains(rest);
        }
        return false;
    }

    private static final String SHINY = "Shiny ";

    String shiny(String template) {
        if (template == null || !translations.store.translatesNames()) {
            return null;
        }
        if (template.indexOf('\n') >= 0 || template.indexOf('\r') >= 0) {
            return null;
        }
        String key = template.strip();
        String head = "";
        String tail = "";
        while (key.startsWith(ICON)) {
            head += ICON;
            key = key.substring(ICON.length());
        }
        while (key.endsWith(ICON)) {
            tail = ICON + tail;
            key = key.substring(0, key.length() - ICON.length());
        }
        int end = key.length();
        while (end > 0 && shinyMark(key.charAt(end - 1))) {
            end--;
        }
        tail = key.substring(end) + tail;
        key = key.substring(0, end).strip();
        if (head.isEmpty() && tail.isEmpty()) {
            return shinyName(key);
        }
        String core = shinyName(key);
        return core == null ? null : head + core + tail;
    }

    private static boolean shinyMark(char c) {
        return !Character.isLetterOrDigit(c) && !Character.isWhitespace(c) && "'.)!?-".indexOf(c) < 0;
    }

    private String shinyName(String key) {
        if (!key.startsWith(SHINY)) {
            return null;
        }
        String rest = key.substring(SHINY.length()).strip();
        boolean bare = gearNameKeys.contains(rest);
        if (!bare && !nameKeys.contains(rest)) {
            return null;
        }
        String pattern = translations.scopedLookup("name", "Shiny");
        if (pattern == null || !pattern.contains("{name}")) {
            return null;
        }
        String name = translations.gearOwnName(rest);
        if (name == null) {
            name = bare ? null : translations.lookupBase(rest);
        }
        if (name == null) {
            name = rest;
        }
        String out = pattern.replace("{name}", name);
        int at = pattern.indexOf("{name}");
        if (at > 0 && name.equals(rest) && !Character.isWhitespace(pattern.charAt(at - 1))
                && pattern.charAt(at - 1) > 0x2E80) {
            out = pattern.substring(0, at) + " " + name + pattern.substring(at + "{name}".length());
        }
        if (translations.store.namesWithOriginal()) {
            out = out + " (" + key + ")";
        }
        return out;
    }
}
