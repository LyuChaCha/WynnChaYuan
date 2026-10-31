package com.wynnchayuan.translate;



import com.wynnchayuan.capture.NameFrames;
import com.wynnchayuan.client.T;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.wynnchayuan.translate.TranslationStore.BAR_ITEMS;
import static com.wynnchayuan.translate.TranslationStore.CHAT_LINE;
import static com.wynnchayuan.translate.TranslationStore.HOLD_APPENDED;
import static com.wynnchayuan.translate.TranslationStore.ICON;
import static com.wynnchayuan.translate.TranslationStore.LOOTRUN_TITLE;
import static com.wynnchayuan.translate.TranslationStore.NAME_LINE;
import static com.wynnchayuan.translate.TranslationStore.indentOf;
import static com.wynnchayuan.translate.TranslationStore.nameRowInner;
import static com.wynnchayuan.translate.TranslationStore.normalise;
import static com.wynnchayuan.translate.TranslationStore.respell;
import static com.wynnchayuan.translate.TranslationStore.unwrap;


final class TranslationIndex {

    final TranslationStore store;

    final TermIndex termIndex = new TermIndex(this);
    final PlayerNameGuesser playerNames = new PlayerNameGuesser(this);
    final LooseMatcher looseMatcher = new LooseMatcher(this);
    final ItemNameRules nameRules = new ItemNameRules(this);
    final QuestDialogueIndex questIndex = new QuestDialogueIndex(this);
    final TranslationLoader loader = new TranslationLoader(this);

    TranslationIndex(TranslationStore store) {
        this.store = store;
    }

    final Map<String, String> entries = new ConcurrentHashMap<>();

    final Set<String> seenSources = ConcurrentHashMap.newKeySet();

    private volatile int maxBlockLines = 1;

    final Map<String, String> flat = new ConcurrentHashMap<>();

    final MarketSearch market = new MarketSearch();

    public MarketSearch market() {
        return market;
    }

    volatile int loadedFiles = 0;

    final List<Path> broken = new CopyOnWriteArrayList<>();

    public List<Path> brokenFiles() {
        return List.copyOf(broken);
    }

    volatile String lastResult;

    public String chatSpeaker(String speaker) {
        return speaker == null ? null : chatSpeakers.get(speaker.strip());
    }

    volatile Map<String, String> chatSpeakers = Map.of();

    private static final Pattern CHAT_LINE_DST = Pattern.compile(
            "\\[\\{~[12]?}/\\{~[12]?}] (?<speaker>[^:：\\n]+?)(?:: |：)(?<line>[^\\n]+)");

    static Map<String, String> learnChatSpeakers(Map<String, String> entries) {
        Map<String, Map<String, Integer>> votes = new HashMap<>();
        for (Map.Entry<String, String> e : entries.entrySet()) {
            String key = e.getKey();
            if (!key.startsWith("[{~}/{~}] ")) {
                continue;
            }
            Matcher src = CHAT_LINE.matcher(key);
            Matcher dst = CHAT_LINE_DST.matcher(e.getValue());
            if (!src.matches() || !dst.matches()) {
                continue;
            }
            votes.computeIfAbsent(src.group("speaker").strip(), k -> new HashMap<>())
                 .merge(dst.group("speaker").strip(), 1, Integer::sum);
        }
        Map<String, String> out = new HashMap<>();
        for (Map.Entry<String, Map<String, Integer>> e : votes.entrySet()) {
            String best = null;
            int top = 0;
            boolean tie = false;
            for (Map.Entry<String, Integer> v : e.getValue().entrySet()) {
                if (v.getValue() > top) {
                    best = v.getKey();
                    top = v.getValue();
                    tie = false;
                } else if (v.getValue() == top) {
                    tie = true;
                }
            }
            if (best != null && !tie) {
                out.put(e.getKey(), best);
            }
        }
        return Map.copyOf(out);
    }

    public NameFrames nameFrames() {
        return nameFrames;
    }

    volatile NameFrames nameFrames = NameFrames.EMPTY;

    int topLayer = 0;

    final Map<String, Integer> layerOf = new ConcurrentHashMap<>();

    final Map<String, Integer> indexLayer = new ConcurrentHashMap<>();

    int layerOf(String src) {
        Integer at = src == null ? null : layerOf.get(src);
        return at == null ? -1 : at;
    }

    final Map<String, Map<String, String>> scopedOnly = new HashMap<>();

    public String labelLookup(String template) {
        return scopedLookup("label", template);
    }

    public String scopedLookup(String scope, String template) {
        Map<String, String> m = scopedOnly.get(scope);
        return m == null || template == null ? null : m.get(template.strip());
    }

    public String lastResult() {
        return lastResult != null ? lastResult : T.s("status.not_loaded");
    }

    final Set<String> uiLabels = new HashSet<>();

    public boolean isUiLabel(String key) {
        return key != null && uiLabels.contains(key.strip());
    }

    private static final int MIN_TRUNCATED_LABEL = 6;

    public String completeUiLabel(String prefix) {
        if (prefix == null) {
            return null;
        }
        String head = prefix.stripLeading();
        if (head.strip().length() < MIN_TRUNCATED_LABEL) {
            return null;
        }
        String found = null;
        for (String key : uiLabels) {
            String base = labelBase(key);
            if (base.length() <= head.length() || !base.startsWith(head)) {
                continue;
            }
            if (found != null && !found.equals(base)) {
                return null;
            }
            found = base;
        }
        return found;
    }

    private static String labelBase(String key) {
        if (key.endsWith("%")) {
            return key.substring(0, key.length() - 1).stripTrailing();
        }
        if (key.endsWith(" Raw")) {
            return key.substring(0, key.length() - 4).stripTrailing();
        }
        return key;
    }

    Set<String> keys() {
        return Collections.unmodifiableSet(entries.keySet());
    }

    private final Map<String, String> unwrapped = new ConcurrentHashMap<>();

    void noteUnwrapped(String key, String value) {
        if (key.length() < MIN_FLAT_LENGTH) {
            return;
        }
        String joined = unwrap(key);
        boolean single = key.indexOf('\n') < 0;
        if (single && unwrappedFromWrapped.remove(joined)) {
            unwrapped.put(joined, value);
            indexLayer.put("unwrapped" + '\0' + joined, loader.layer);
            return;
        }
        if (!loader.thisLayer.contains("unwrapped" + '\u0000' + joined) && !single) {
            unwrappedFromWrapped.add(joined);
        }
        loader.layered(unwrapped, "unwrapped", joined, value);
    }

    final Set<String> unwrappedFromWrapped = new HashSet<>();

    public String lookupUnwrapped(String text) {
        if (text == null || text.length() < MIN_FLAT_LENGTH) {
            return null;
        }
        String key = unwrap(text);
        return preferTop(unwrapped.get(key), "unwrapped", key);
    }

    private String preferTop(String hit, String tag, String key) {
        if (hit == null || topLayer == 0 || fromTop(tag, key)) {
            return hit;
        }
        String top = lookupChain(key, true);
        return top != null ? top : hit;
    }

    public String lookupFlat(String text) {
        if (text == null || text.length() < MIN_FLAT_LENGTH) {
            return null;
        }
        String key = normalise(text);
        return preferTop(flat.get(key), "flat", key);
    }

    static final int MIN_FLAT_LENGTH = 24;

    void noteBlockSize(String key) {
        int lines = 1;
        for (int i = key.indexOf('\n'); i >= 0; i = key.indexOf('\n', i + 1)) {
            lines++;
        }
        if (lines > maxBlockLines) {
            maxBlockLines = lines;
        }
    }

    final Set<String> pageNames = new HashSet<>();

    public boolean isPageName(String text) {
        return text != null && pageNames.contains(text.strip());
    }

    public String pageTitleName(String text) {
        if (text == null) {
            return null;
        }
        String name = text.strip();
        String title = pageTitleNames.get(name);
        return title != null || pageTitleSplit.contains(name) ? title : questNames.get(name);
    }

    private final Map<String, String> pageTitleNames = new HashMap<>();

    private final Set<String> pageTitleSplit = new HashSet<>();

    private final Map<String, String> questNames = new HashMap<>();

    // 名字含地名的任務，卡片的鍵是 {p} 版本，名字只能從這裡拿
    void noteQuestName(String name, String translated) {
        pageNames.add(name);
        if (!translated.isEmpty() && !translated.equals(name)) {
            questNames.put(name, translated);
        }
    }

    void notePageTitle(String name, String translated) {
        if (translated == null || pageTitleSplit.contains(name)) {
            return;
        }
        String shown = translated.strip();
        int cut = shown.lastIndexOf(" [");
        if (cut <= 0 || !shown.endsWith("]") || shown.indexOf('\n') >= 0) {
            return;
        }
        String local = shown.substring(0, cut).strip();
        if (local.isEmpty() || local.equals(name)) {
            return;
        }
        String had = pageTitleNames.putIfAbsent(name, local);
        if (had != null && !had.equals(local)) {
            pageTitleNames.remove(name);
            pageTitleSplit.add(name);
        }
    }

    public String lookupExact(String key) {
        if (key == null) {
            return null;
        }
        String hit = entries.get(key.strip());
        return hit == null || hit.isBlank() ? null : hit;
    }

    private static final String GEAR_SCOPE = "gear";

    public String gearOwnName(String key) {
        return key == null ? null : scopedLookup(GEAR_SCOPE, key);
    }

    String lootrunName(String name) {
        String icon = lookupExact(ICON + name);
        if (icon != null && icon.startsWith(ICON) && icon.lastIndexOf(ICON) == 0) {
            String cell = icon.substring(ICON.length()).strip();
            if (!cell.isEmpty()) {
                return cell;
            }
        }
        String colon = lookupExact(name + ":");
        if (colon != null && (colon.endsWith(":") || colon.endsWith("："))) {
            String cell = colon.substring(0, colon.length() - 1).strip();
            if (!cell.isEmpty()) {
                return cell;
            }
        }
        return null;
    }

    public String lookup(String template) {
        if (BAR_ITEMS.get() && template != null && nameRules.getsOriginal(template.strip())) {
            return null;
        }
        boolean holdAppended = HOLD_APPENDED.get();
        if (LOOTRUN_TITLE.get() && LootrunNames.isName(template)) {
            String name = template.strip();
            String own = lootrunName(name);
            if (own != null) {
                return holdAppended ? own : LootrunNames.apply(name, own, store.pageNameMode());
            }
            if (nameRules.gearOnly(name)) {
                return null;
            }
        }
        if (NAME_LINE.get() && template != null) {
            String own = gearOwnName(template);
            if (own != null) {
                if (!store.translatesNames()) {
                    return null;
                }
                return store.namesWithOriginal() && !holdAppended
                        ? ItemNameRules.withOriginal(own, template.strip()) : own;
            }
        }
        String hit = lookupBase(template);
        if (hit == null) {
            return nameRules.shiny(template);
        }
        if (store.namesWithOriginal() && !holdAppended && (nameRules.getsOriginal(template.strip())
                || NAME_LINE.get() && nameRules.ownItemName(template.strip(), hit))) {
            return ItemNameRules.withOriginal(hit, template.strip());
        }
        if (store.abilityNamesWithOriginal() && nameRules.ownAbilityName(template.strip(), hit)) {
            return ItemNameRules.withOriginal(hit, template.strip());
        }
        if (store.abilityNamesWithOriginal()) {
            String labelled = nameRules.abilityLabelWithOriginal(template.strip(), hit);
            if (labelled != null) {
                return labelled;
            }
        }
        return holdAppended ? hit : LootrunNames.apply(template, hit, store.pageNameMode());
    }

    static final Pattern NUMBERED = Pattern.compile("\\{~\\d+}");

    private boolean isNameRow(String key) {
        String inner = nameRowInner(key);
        return inner != null && (nameRules.nameKeys.contains(inner) || nameRules.gearNameKeys.contains(inner)
                || nameRules.plainNameKeys.contains(inner));
    }

    String lookupBase(String template) {
        if (template == null) {
            return null;
        }
        String key = template.strip();
        if (!store.translatesNames() && nameRules.gearOnly(key)) {
            return null;
        }
        if (nameRules.peeked(key)) {
            return null;
        }
        if (isNameRow(key)) {
            return null;
        }

        if (topLayer > 0) {
            String top = lookupChain(key, true);
            if (top != null) {
                return top;
            }
        }
        return lookupChain(key, false);
    }

    private String lookupChain(String key, boolean topOnly) {
        String hit = exact(key, topOnly);
        if (hit == null) {
            hit = indented(key, topOnly);
        }
        if (hit != null) {
            return hit;
        }
        String other = respell(key);
        boolean respelled = !other.equals(key);
        if (respelled) {
            hit = exact(other, topOnly);
            if (hit == null) {
                hit = indented(other, topOnly);
            }
        }
        if (hit == null && (!topOnly || fromTop("marked", LooseMatcher.sameMarks(key)))) {
            hit = looseMatcher.lookupMarked(key);
        }
        if (hit == null) {
            hit = loose(key, topOnly);
        }
        if (hit == null && respelled) {
            hit = loose(other, topOnly);
        }
        return hit;
    }

    private String exact(String key, boolean topOnly) {
        return topOnly && layerOf(key) != topLayer ? null : entries.get(key);
    }

    private String indented(String key, boolean topOnly) {
        if (topOnly) {
            int n = indentOf(key);
            if (n == 0 || !fromTop("unindented", key.substring(n))) {
                return null;
            }
        }
        return looseMatcher.lookupIndented(key);
    }

    private String loose(String key, boolean topOnly) {
        if (topOnly && layerOf(looseMatcher.looseSource(key)) != topLayer) {
            return null;
        }
        return looseMatcher.lookupLoose(key);
    }

    private boolean fromTop(String tag, String key) {
        Integer at = indexLayer.get(tag + '\0' + key);
        return at != null && at == topLayer;
    }

    public Set<String> sourceKeys() {
        return Set.copyOf(seenSources);
    }

    public boolean hasTranslation(String template) {
        if (template == null) {
            return false;
        }
        String key = template.strip();
        if (entries.containsKey(key) || looseMatcher.lookupIndented(key) != null) {
            return true;
        }
        if (isNameRow(key)) {
            return true;
        }
        if (looseMatcher.lookupMarked(key) != null) {
            return true;
        }
        if (looseMatcher.looseSource(key) != null || looseMatcher.looseSource(respell(key)) != null) {
            return true;
        }
        if (TemplateLookup.lookup(key, store, false) != null
                || TemplateLookup.lookup(key, store, true) != null) {
            return true;
        }
        if (key.indexOf('\n') >= 0 && TemplateLookup.eventFrame(unwrap(key), store) != null) {
            return true;
        }
        if (key.indexOf('\n') >= 0 && TemplateLookup.countdownBoard(key, store) != null) {
            return true;
        }
        String other = respell(key);
        return !other.equals(key)
                && (entries.containsKey(other) || looseMatcher.lookupIndented(other) != null);
    }

    public int maxBlockLines() {
        return flat.isEmpty() ? maxBlockLines : Math.max(maxBlockLines, FLOWED_SCAN_LINES);
    }

    private static final int FLOWED_SCAN_LINES = 14;

    public int size() {
        return entries.size();
    }

    public int loadedFiles() {
        return loadedFiles;
    }

    void load(List<Path> dirs) {
        loader.load(dirs);
    }
}
