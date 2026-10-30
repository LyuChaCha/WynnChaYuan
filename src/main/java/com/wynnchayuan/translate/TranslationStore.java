package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public final class TranslationStore {

    private volatile TranslationIndex index = new TranslationIndex(this);

    public void loadAll(Path dir) {
        loadAll(List.of(dir));
    }

    public synchronized void loadAll(List<Path> dirs) {
        TranslationIndex next = new TranslationIndex(this);
        next.load(dirs);
        index = next;
        generation++;
    }

    public int generation() {
        return generation;
    }

    private volatile int generation;

    private volatile boolean peekPlainNames = false;

    public void setPeekPlainNames(boolean value) {
        this.peekPlainNames = value;
    }

    public boolean peeksPlainNames() {
        return peekPlainNames;
    }

    private volatile boolean translateNames = true;

    public void setTranslateNames(boolean value) {
        this.translateNames = value;
    }

    public boolean translatesNames() {
        return translateNames;
    }

    public void setNameMode(com.wynnchayuan.CollectorConfig.ItemNames mode) {
        this.translateNames = mode != com.wynnchayuan.CollectorConfig.ItemNames.OFF;
        this.namesWithOriginal = mode == com.wynnchayuan.CollectorConfig.ItemNames.BOTH;
    }

    public void setPageNameMode(com.wynnchayuan.CollectorConfig.ItemNames mode) {
        com.wynnchayuan.CollectorConfig.ItemNames next =
                mode == null ? com.wynnchayuan.CollectorConfig.ItemNames.ON : mode;
        if (next != pageNameMode) {
            generation++;
        }
        this.pageNameMode = next;
    }

    public com.wynnchayuan.CollectorConfig.ItemNames pageNameMode() {
        return pageNameMode;
    }

    private volatile com.wynnchayuan.CollectorConfig.ItemNames pageNameMode =
            com.wynnchayuan.CollectorConfig.ItemNames.ON;

    private volatile boolean namesWithOriginal = false;

    private volatile boolean abilityNamesWithOriginal = false;

    public void setAbilityNamesWithOriginal(boolean on) {
        if (on != abilityNamesWithOriginal) {
            generation++;
        }
        abilityNamesWithOriginal = on;
    }

    public boolean abilityNamesWithOriginal() {
        return abilityNamesWithOriginal;
    }

    public boolean namesWithOriginal() {
        return namesWithOriginal;
    }

    public MarketSearch market() {
        return index.market();
    }

    public boolean isPlainName(String key) {
        return index.nameRules.isPlainName(key);
    }

    public List<Path> brokenFiles() {
        return index.brokenFiles();
    }

    public String chatSpeaker(String speaker) {
        return index.chatSpeaker(speaker);
    }

    public com.wynnchayuan.capture.NameFrames nameFrames() {
        return index.nameFrames();
    }

    public String labelLookup(String template) {
        return index.labelLookup(template);
    }

    public String scopedLookup(String scope, String template) {
        return index.scopedLookup(scope, template);
    }

    public String lastResult() {
        return index.lastResult();
    }

    public boolean isUiLabel(String key) {
        return index.isUiLabel(key);
    }

    public String completeUiLabel(String prefix) {
        return index.completeUiLabel(prefix);
    }

    java.util.Set<String> keys() {
        return index.keys();
    }

    public Guess playerNameIn(String raw) {
        return index.playerNames.playerNameIn(raw);
    }

    public String lookupUnwrapped(String text) {
        return index.lookupUnwrapped(text);
    }

    public String lookupFlat(String text) {
        return index.lookupFlat(text);
    }

    public Term findTerm(String text, int from) {
        return index.termIndex.findTerm(text, from);
    }

    public String lookupTerm(String text) {
        return index.termIndex.lookupTerm(text);
    }

    java.util.Set<String> termNames() {
        return index.termIndex.termNames();
    }

    public boolean isPageName(String text) {
        return index.isPageName(text);
    }

    public String pageTitleName(String text) {
        return index.pageTitleName(text);
    }

    public String speakerOf(String template) {
        return index.questIndex.speakerOf(template);
    }

    public Boolean inCast(String quest, String speaker) {
        return index.questIndex.inCast(quest, speaker);
    }

    public String questOf(String template) {
        return index.questIndex.questOf(template);
    }

    public String lookupPrefix(String partial) {
        return index.questIndex.lookupPrefix(partial);
    }

    public String speakerOfPrefix(String partial) {
        return index.questIndex.speakerOfPrefix(partial);
    }

    public String matchPrefix(String partial) {
        return index.questIndex.matchPrefix(partial);
    }

    public String matchPrefix(String partial, int evidence) {
        return index.questIndex.matchPrefix(partial, evidence);
    }

    public String matchPrefix(String partial, int evidence, String quest) {
        return index.questIndex.matchPrefix(partial, evidence, quest);
    }

    public String nearQuestLine(String typed, String quest) {
        return index.questIndex.nearQuestLine(typed, quest);
    }

    public String sequenceQuestOf(String src, String... prefer) {
        return index.questIndex.sequenceQuestOf(src, prefer);
    }

    public int sequenceIndex(String quest, String src, int after) {
        return index.questIndex.sequenceIndex(quest, src, after);
    }

    public String forecast(String typed, String quest, int after, int window, String speaker, int floor,
                           java.util.function.BiPredicate<String, String> fits) {
        return index.questIndex.forecast(typed, quest, after, window, speaker, floor, fits);
    }

    public String forecastBySpeaker(String typed, String quest, String speaker, int floor,
                                    java.util.function.BiPredicate<String, String> fits) {
        return index.questIndex.forecastBySpeaker(typed, quest, speaker, floor, fits);
    }

    public String forecastNext(String typed, String quest, int after, int window, String speaker, int floor,
                               java.util.function.BiPredicate<String, String> fits) {
        return index.questIndex.forecastNext(typed, quest, after, window, speaker, floor, fits);
    }

    public String explainForecast(String typed, String quest, int after, int window, String speaker,
                                  java.util.function.BiPredicate<String, String> fits) {
        return index.questIndex.explainForecast(typed, quest, after, window, speaker, fits);
    }

    public String explainQuest(String typed, String quest) {
        return index.questIndex.explainQuest(typed, quest);
    }

    public boolean hasLonger(String key) {
        return index.questIndex.hasLonger(key);
    }

    public boolean couldStillMatch(String typed) {
        return index.questIndex.couldStillMatch(typed);
    }

    public boolean isBareGearName(String key) {
        return index.nameRules.isBareGearName(key);
    }

    public boolean itemNameOnly(String key) {
        return index.nameRules.itemNameOnly(key);
    }

    public boolean gearNameOnly(String key) {
        return index.nameRules.gearNameOnly(key);
    }

    public boolean isItemName(String key) {
        return index.nameRules.isItemName(key);
    }

    public String lookupExact(String key) {
        return index.lookupExact(key);
    }

    public String gearOwnName(String key) {
        return index.gearOwnName(key);
    }

    String lootrunName(String name) {
        return index.lootrunName(name);
    }

    public String lookup(String template) {
        return index.lookup(template);
    }

    public int appendedOriginalAt(String text) {
        return index.nameRules.appendedOriginalAt(text);
    }

    public List<int[]> keptOriginals(String text) {
        return index.nameRules.keptOriginals(text);
    }

    List<int[]> abilityOriginalSpans(String text) {
        return index.nameRules.abilityOriginalSpans(text);
    }

    public int[] appendedOriginalSpan(String text) {
        return index.nameRules.appendedOriginalSpan(text);
    }

    public java.util.Set<String> sourceKeys() {
        return index.sourceKeys();
    }

    public boolean hasTranslation(String template) {
        return index.hasTranslation(template);
    }

    public int maxBlockLines() {
        return index.maxBlockLines();
    }

    public int size() {
        return index.size();
    }

    public int loadedFiles() {
        return index.loadedFiles();
    }

    private static final java.util.regex.Pattern TOOL_NAME = java.util.regex.Pattern.compile(
            "^(?:\\{#})*[A-Z][a-z]+ (?:Pickaxe|Axe|Scythe|Rod) T\\{~}(?:\\{#})*$");

    static boolean isToolName(String key) {
        return key != null && TOOL_NAME.matcher(key).matches();
    }

    static final java.util.regex.Pattern CHAT_LINE = java.util.regex.Pattern.compile(
            "\\[\\{~}/\\{~}] (?<speaker>[^:\\n\\[\\]]+?): (?<line>[^\\n]+)");

    public record Guess(String name, String source) {}

    public static String normalise(String text) {
        return text == null ? "" : text.strip().replaceAll("\\s+", " ");
    }

    public static String unwrap(String text) {
        if (text == null) {
            return "";
        }
        String glyph = com.wynnchayuan.capture.GlyphSplitter.GLYPH_PLACEHOLDER;
        StringBuilder out = new StringBuilder(text.length());
        int at = 0;
        while (at < text.length()) {
            if (text.charAt(at) != '\n') {
                out.append(text.charAt(at));
                at++;
                continue;
            }
            at++;
            while (at < text.length()) {
                if (text.startsWith(glyph, at)) {
                    at += glyph.length();
                } else if (Character.isWhitespace(text.charAt(at))) {
                    at++;
                } else {
                    break;
                }
            }
            out.append(' ');
        }
        return normalise(out.toString());
    }

    public record Term(int start, int end, String translation) {}

    // Java 把漢字當字母，以 0x2E80 為界排除中日韓
    static boolean wordChar(char c) {
        return c < 0x2E80 && Character.isLetterOrDigit(c);
    }

    public static volatile boolean unplacedRivals = true;

    public static boolean slotFits(String src, String typed) {
        int room = Math.min(src.length(), typed.length());
        int agreed = 0;
        while (agreed < room && src.charAt(agreed) == typed.charAt(agreed)) {
            agreed++;
        }
        if (agreed >= src.length() || src.charAt(agreed) != '{'
                || typed.length() > agreed + FORECAST_NAME_ROOM) {
            return false;
        }
        int close = src.indexOf('}', agreed);
        String slot = close < 0 ? src.substring(agreed) : src.substring(agreed, close + 1);
        return couldBe(slot, typed.substring(agreed));
    }

    private static boolean couldBe(String slot, String partial) {
        if (partial.isEmpty()) {
            return true;
        }
        if (slot.equals("{u}")) {
            if (partial.length() > 16) {
                return false;
            }
            for (int i = 0; i < partial.length(); i++) {
                char c = partial.charAt(i);
                if (!(c < 128 && (Character.isLetterOrDigit(c) || c == '_'))) {
                    return false;
                }
            }
            List<String> names = com.wynnchayuan.capture.SelfNames.all();
            if (names.isEmpty()) {
                return true;
            }
            String low = partial.toLowerCase(Locale.ROOT);
            for (String name : names) {
                if (name.toLowerCase(Locale.ROOT).startsWith(low)) {
                    return true;
                }
            }
            return false;
        }
        if (slot.equals("{p}")) {
            return com.wynnchayuan.capture.PlaceNames.anyStartsWith(partial);
        }
        if (slot.equals("{#}")) {
            return GlyphSplitter.isGlyphCodePoint(partial.codePointAt(0));
        }
        if (slot.startsWith("{~")) {
            for (int i = 0; i < partial.length(); i++) {
                if ("0123456789,.-+".indexOf(partial.charAt(i)) < 0) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    private static final int FORECAST_NAME_ROOM = 24;

    static boolean sameLine(String a, String b) {
        java.util.Set<String> left = words(a);
        java.util.Set<String> right = words(b);
        if (left.isEmpty() || right.isEmpty()) {
            return false;
        }
        int both = 0;
        for (String w : left) {
            if (right.contains(w)) {
                both++;
            }
        }
        int union = left.size() + right.size() - both;
        return both * 100 >= union * SAME_LINE_PERCENT;
    }

    static java.util.Set<String> words(String text) {
        java.util.Set<String> out = new java.util.HashSet<>();
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = Character.toLowerCase(text.charAt(i));
            if (Character.isLetterOrDigit(c)) {
                word.append(c);
            } else if (word.length() > 0) {
                out.add(word.toString());
                word.setLength(0);
            }
        }
        if (word.length() > 0) {
            out.add(word.toString());
        }
        return out;
    }

    // 實測：官方改詞重疊 71%、不同任務目標 60%，取 65
    private static final int SAME_LINE_PERCENT = 65;

    static final ThreadLocal<Boolean> NAME_LINE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    static final ThreadLocal<Boolean> LOOTRUN_TITLE =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    static String originalOf(String key) {
        if (key.indexOf('{') < 0) {
            return key;
        }
        if (key.contains(GlyphSplitter.PLACE_PLACEHOLDER)
                || key.contains(GlyphSplitter.PLAYER_PLACEHOLDER)) {
            return null;
        }
        String bare = bareName(key);
        if (bare.isEmpty()) {
            return null;
        }
        String number = GlyphSplitter.NUMBER_PLACEHOLDER;
        StringBuilder out = new StringBuilder();
        int from = 0;
        int nth = 0;
        for (int at = bare.indexOf(number); at >= 0; at = bare.indexOf(number, from)) {
            out.append(bare, from, at).append("{~").append(++nth).append('}');
            from = at + number.length();
        }
        return out.append(bare, from, bare.length()).toString();
    }

    static String bareName(String key) {
        return key.indexOf(ICON) < 0 ? key
                : key.replace(ICON, " ").strip().replaceAll(" {2,}", " ");
    }

    static final ThreadLocal<Boolean> HOLD_APPENDED =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static boolean itemNamesBarred() {
        return BAR_ITEMS.get();
    }

    static final ThreadLocal<Boolean> BAR_ITEMS =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    static final String ICON = "{#}";

    private static final String ROLLED = " [{~}]";

    public static String nameRowInner(String key) {
        if (key == null) {
            return null;
        }
        int start = 0;
        while (key.startsWith(ICON, start)) {
            start += ICON.length();
        }
        int leading = start / ICON.length();
        int end = key.length();
        boolean rolled = key.endsWith(ROLLED);
        if (rolled) {
            end -= ROLLED.length();
        }
        int trailing = 0;
        while (end - ICON.length() >= start && key.startsWith(ICON, end - ICON.length())) {
            end -= ICON.length();
            trailing++;
        }
        if (leading == 0 || end <= start) {
            return null;
        }
        if (trailing == 0 && !rolled && leading < 3) {
            return null;
        }
        String inner = key.substring(start, end);
        return inner.indexOf('{') >= 0 || !inner.equals(inner.strip()) ? null : inner;
    }

    static String respell(String text) {
        String out = text;
        for (int i = 0; i < SPELLING.length; i++) {
            out = out.replace(SPELLING[i][0], SPELLING[i][1]);
            out = out.replace(SPELLING_UPPER[i][0], SPELLING_UPPER[i][1]);
        }
        return out;
    }

    private static String upper(String word) {
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    private static final String[][] SPELLING = {
        {"honour", "honor"}, {"colour", "color"}, {"armour", "armor"},
        {"favour", "favor"}, {"neighbour", "neighbor"}, {"rumour", "rumor"},
        {"behaviour", "behavior"}, {"harbour", "harbor"}, {"labour", "labor"},
        {"flavour", "flavor"}, {"humour", "humor"}, {"odour", "odor"},
        {"valour", "valor"}, {"vapour", "vapor"}, {"saviour", "savior"},
        {"endeavour", "endeavor"}, {"centre", "center"}, {"theatre", "theater"},
        {"fibre", "fiber"}, {"defence", "defense"}, {"offence", "offense"},
        {"travelled", "traveled"}, {"traveller", "traveler"},
        {"cancelled", "canceled"}, {"marvellous", "marvelous"},
        {"jewellery", "jewelry"}, {"realise", "realize"},
        {"apologise", "apologize"}, {"recognise", "recognize"},
        {"organise", "organize"},
    };

    private static final String[][] SPELLING_UPPER = upperPairs();

    private static String[][] upperPairs() {
        String[][] out = new String[SPELLING.length][];
        for (int i = 0; i < SPELLING.length; i++) {
            out[i] = new String[] {upper(SPELLING[i][0]), upper(SPELLING[i][1])};
        }
        return out;
    }

    private static final char INDENT = 'À';

    public static int indentOf(String text) {
        int n = 0;
        while (n < text.length() && text.charAt(n) == INDENT) {
            n++;
        }
        return n;
    }
}
