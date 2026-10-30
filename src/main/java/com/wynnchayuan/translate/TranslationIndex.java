package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.wynnchayuan.translate.TranslationStore.BAR_ITEMS;
import static com.wynnchayuan.translate.TranslationStore.CHAT_LINE;
import static com.wynnchayuan.translate.TranslationStore.HOLD_APPENDED;
import static com.wynnchayuan.translate.TranslationStore.ICON;
import static com.wynnchayuan.translate.TranslationStore.LOOTRUN_TITLE;
import static com.wynnchayuan.translate.TranslationStore.NAME_LINE;
import static com.wynnchayuan.translate.TranslationStore.bareName;
import static com.wynnchayuan.translate.TranslationStore.indentOf;
import static com.wynnchayuan.translate.TranslationStore.isToolName;
import static com.wynnchayuan.translate.TranslationStore.nameRowInner;
import static com.wynnchayuan.translate.TranslationStore.normalise;
import static com.wynnchayuan.translate.TranslationStore.originalOf;
import static com.wynnchayuan.translate.TranslationStore.respell;
import static com.wynnchayuan.translate.TranslationStore.sameLine;
import static com.wynnchayuan.translate.TranslationStore.slotFits;
import static com.wynnchayuan.translate.TranslationStore.unplacedRivals;
import static com.wynnchayuan.translate.TranslationStore.unwrap;
import static com.wynnchayuan.translate.TranslationStore.wordChar;
import static com.wynnchayuan.translate.TranslationStore.words;

import com.wynnchayuan.translate.TranslationStore.Guess;
import com.wynnchayuan.translate.TranslationStore.Term;

final class TranslationIndex {

    private final TranslationStore store;

    TranslationIndex(TranslationStore store) {
        this.store = store;
    }

    private final Map<String, String> entries = new ConcurrentHashMap<>();

    private final java.util.Set<String> seenSources =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private volatile int maxBlockLines = 1;

    private final Map<String, String> flat = new ConcurrentHashMap<>();

    private final Map<String, String> terms = new ConcurrentHashMap<>();

    private volatile int maxTermWords = 1;

    private final java.util.Set<String> gearNameKeys = new java.util.HashSet<>();

    private final MarketSearch market = new MarketSearch();

    public MarketSearch market() {
        return market;
    }

    private final java.util.Set<String> nameKeys =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private final java.util.Set<String> plainNameKeys =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    private boolean peeked(String key) {
        return store.peeksPlainNames() && (plainNameKeys.contains(key) || isToolName(key));
    }

    public boolean isPlainName(String key) {
        return key != null && plainNameKeys.contains(key.strip());
    }

    private final java.util.Set<String> otherOwners = new java.util.HashSet<>();

    private final java.util.Set<String> nonItemOwners = new java.util.HashSet<>();

    private final Map<String, java.util.Set<String>> itemNameDsts = new ConcurrentHashMap<>();

    private final Map<String, java.util.Set<String>> abilityNameDsts = new ConcurrentHashMap<>();
    private volatile int loadedFiles = 0;

    private final List<Path> broken = new java.util.concurrent.CopyOnWriteArrayList<>();

    public List<Path> brokenFiles() {
        return List.copyOf(broken);
    }

    private volatile String lastResult;

    void load(List<Path> dirs) {
        topLayer = Math.max(0, dirs.size() - 1);
        for (layer = 0; layer < dirs.size(); layer++) {
            readOne(dirs.get(layer));
        }
        layer = topLayer;
        gearNameKeys.removeAll(nameKeys);
        nameFrames = com.wynnchayuan.capture.NameFrames.of(seenSources);
        chatSpeakers = learnChatSpeakers(entries);
        report(dirs.isEmpty() ? null : dirs.get(dirs.size() - 1));
    }

    public String chatSpeaker(String speaker) {
        return speaker == null ? null : chatSpeakers.get(speaker.strip());
    }

    private volatile Map<String, String> chatSpeakers = Map.of();

    private static final java.util.regex.Pattern CHAT_LINE_DST = java.util.regex.Pattern.compile(
            "\\[\\{~[12]?}/\\{~[12]?}] (?<speaker>[^:：\\n]+?)(?:: |：)(?<line>[^\\n]+)");

    private static Map<String, String> learnChatSpeakers(Map<String, String> entries) {
        Map<String, Map<String, Integer>> votes = new java.util.HashMap<>();
        for (Map.Entry<String, String> e : entries.entrySet()) {
            String key = e.getKey();
            if (!key.startsWith("[{~}/{~}] ")) {
                continue;
            }
            java.util.regex.Matcher src = CHAT_LINE.matcher(key);
            java.util.regex.Matcher dst = CHAT_LINE_DST.matcher(e.getValue());
            if (!src.matches() || !dst.matches()) {
                continue;
            }
            votes.computeIfAbsent(src.group("speaker").strip(), k -> new java.util.HashMap<>())
                 .merge(dst.group("speaker").strip(), 1, Integer::sum);
        }
        Map<String, String> out = new java.util.HashMap<>();
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

    public com.wynnchayuan.capture.NameFrames nameFrames() {
        return nameFrames;
    }

    private volatile com.wynnchayuan.capture.NameFrames nameFrames =
            com.wynnchayuan.capture.NameFrames.EMPTY;

    private int layer = 0;

    private int topLayer = 0;

    private final Map<String, Integer> layerOf = new ConcurrentHashMap<>();

    private final Map<String, Integer> indexLayer = new ConcurrentHashMap<>();

    private int layerOf(String src) {
        Integer at = src == null ? null : layerOf.get(src);
        return at == null ? -1 : at;
    }

    private final java.util.Set<String> thisLayer = new java.util.HashSet<>();

    private void layered(java.util.Map<String, String> index, String tag,
                         String key, String value) {
        if (thisLayer.add(tag + '\u0000' + key)) {
            index.put(key, value);
            indexLayer.put(tag + '\0' + key, layer);
        }
    }

    private void readOne(Path dir) {
        thisLayer.clear();
        unwrappedFromWrapped.clear();
        if (!Files.isDirectory(dir)) {
            try {
                Files.createDirectories(dir);
                lastResult = com.wynnchayuan.client.T.s("status.folder_created");
                System.out.println("[WynnChaYuan] 譯文資料夾不存在，已建立：" + dir);
            } catch (Exception e) {
                lastResult = com.wynnchayuan.client.T.s("status.folder_failed", e.getMessage());
                System.err.println("[WynnChaYuan] 建立譯文資料夾失敗 " + dir + ": " + e.getMessage());
            }
            return;
        }

        try {
            List<String> order = FileIndex.forDirectory(dir);
            java.util.LinkedHashSet<Path> wanted = new java.util.LinkedHashSet<>();
            for (String name : order) {
                if (name.startsWith("_") || !name.endsWith(".json")) {
                    continue;
                }
                Path file = dir.resolve(name);
                if (Files.isRegularFile(file)) {
                    wanted.add(file);
                }
            }
            try (var extra = Files.list(dir)) {
                extra.filter(p -> p.getFileName().toString().endsWith(".json"))
                     .filter(p -> !p.getFileName().toString().startsWith("_"))
                     .sorted()
                     .forEach(wanted::add);
            }
            wanted.forEach(this::load);
            loadScoped(dir);
        } catch (Exception e) {
            lastResult = com.wynnchayuan.client.T.s("status.read_failed", e.getMessage());
            System.err.println("[WynnChaYuan] 讀取譯文目錄失敗 " + dir + ": " + e.getMessage());
        }
    }

    private final Map<String, Map<String, String>> scopedOnly = new java.util.HashMap<>();

    private void loadScoped(Path dir) {
        for (String name : FileIndex.SCOPED) {
            Path file = dir.resolve(name);
            if (!Files.isRegularFile(file)) {
                continue;
            }
            String scope = file.getFileName().toString().replaceFirst("\\.json$", "");
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(r);
                if (!root.isJsonObject()) {
                    continue;
                }
                JsonObject obj = root.getAsJsonObject();
                Map<String, String> into =
                        scopedOnly.computeIfAbsent(scope, k -> new java.util.HashMap<>());
                for (String key : obj.keySet()) {
                    JsonElement v = obj.get(key);
                    if (!key.startsWith("_") && v.isJsonPrimitive()
                            && !v.getAsString().isBlank()) {
                        into.put(key.strip(), LineTranslator.deIcon(
                                v.getAsString().strip()));
                    }
                }
            } catch (Exception e) {
                System.err.println("[WynnChaYuan] 讀不到 " + file + "：" + e.getMessage());
            }
        }
    }

    public String labelLookup(String template) {
        return scopedLookup("label", template);
    }

    public String scopedLookup(String scope, String template) {
        Map<String, String> m = scopedOnly.get(scope);
        return m == null || template == null ? null : m.get(template.strip());
    }

    private void report(Path dir) {
        if (dir == null) {
            return;
        }
        if (loadedFiles == 0) {
            lastResult = com.wynnchayuan.client.T.s("status.no_json");
            System.out.println("[WynnChaYuan] " + dir + " 裡沒有譯文檔");
        } else if (entries.isEmpty()) {
            lastResult = com.wynnchayuan.client.T.s("status.no_dst", loadedFiles);
            System.out.println("[WynnChaYuan] 讀了 " + loadedFiles + " 個檔案，但沒有任何已填寫的 dst");
        } else {
            lastResult = com.wynnchayuan.client.T.s("status.loaded", entries.size(), loadedFiles);
            System.out.println("[WynnChaYuan] 已載入 " + entries.size() + " 條譯文（" + loadedFiles + " 個檔案）");
        }
    }

    private static int indexOf(List<String> order, String name) {
        int i = order.indexOf(name);
        return i < 0 ? Integer.MAX_VALUE : i;
    }

    public String lastResult() {
        return lastResult != null ? lastResult : com.wynnchayuan.client.T.s("status.not_loaded");
    }

    private void load(Path file) {
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(r);
            if (!root.isJsonObject()) {
                broken.add(file);
                System.err.println("[WynnChaYuan] 略過 " + file.getFileName() + "：不是 JSON 物件");
                return;
            }
            JsonObject obj = root.getAsJsonObject();
            int before = entries.size();

            if (obj.has("entries") && obj.get("entries").isJsonObject()) {
                readWorkspace(obj.getAsJsonObject("entries"), itemNames(obj), gearNames(obj),
                        tradable(file), abilityNames(file, obj));
            } else {
                readFlat(obj, file.getFileName().toString().endsWith("-terms.json"));
                if (file.getFileName().toString().equals("quest.json")) {
                    for (String key : obj.keySet()) {
                        if (!key.startsWith("_") && !key.isBlank()) {
                            unplacedLines.add(key.strip());
                        }
                    }
                }
                for (String key : obj.keySet()) {
                    String name = PageNames.nameOfTitle(key);
                    if (name != null && name.indexOf('{') < 0) {
                        pageNames.add(name.strip());
                        JsonElement value = obj.get(key);
                        if (value != null && value.isJsonPrimitive()) {
                            notePageTitle(name.strip(), value.getAsString());
                        }
                    }
                }
                if (file.getFileName().toString().equals(UI_LABELS)) {
                    for (String key : obj.keySet()) {
                        if (!key.startsWith("_")) {
                            uiLabels.add(key.strip());
                        }
                    }
                }
            }
            loadedFiles++;
            System.out.println("[WynnChaYuan] 載入譯文 " + file.getFileName()
                    + "：+" + (entries.size() - before) + " 條");
        } catch (com.google.gson.JsonParseException e) {
            if (!(e instanceof com.google.gson.JsonIOException)
                    || e.getCause() instanceof java.nio.charset.CharacterCodingException) {
                broken.add(file);
            }
            System.err.println("[WynnChaYuan] 略過 " + file.getFileName() + "：" + e.getMessage());
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 略過 " + file.getFileName() + "：" + e.getMessage());
        }
    }

    private static final String UI_LABELS = "ui-labels.json";

    private final java.util.Set<String> uiLabels = new java.util.HashSet<>();

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

    private static boolean itemNames(JsonObject root) {
        return metaFlag(root, "itemNames");
    }

    private static boolean gearNames(JsonObject root) {
        return metaFlag(root, "gearNames");
    }

    private static boolean tradable(Path file) {
        String name = file.getFileName().toString();
        return name.startsWith("gear-") || name.equals("ingredient.json")
                || name.equals("material.json");
    }

    private static boolean abilityNames(Path file, JsonObject root) {
        String name = file.getFileName().toString();
        Path parent = file.getParent();
        if (name.equals("major-id.json")
                || parent != null && parent.getFileName() != null
                        && parent.getFileName().toString().equals("ability")) {
            return true;
        }
        JsonElement meta = root.get("_meta");
        if (meta == null || !meta.isJsonObject()) {
            return false;
        }
        String domain = optString(meta.getAsJsonObject(), "domain");
        return domain != null && (domain.startsWith("ability/") || domain.equals("major-id"));
    }

    private static boolean metaFlag(JsonObject root, String name) {
        JsonElement meta = root.get("_meta");
        if (meta == null || !meta.isJsonObject()) {
            return true;
        }
        JsonElement flag = meta.getAsJsonObject().get(name);
        return flag == null || !flag.isJsonPrimitive() || flag.getAsBoolean();
    }

    private void readWorkspace(JsonObject entriesObj, boolean itemNames, boolean gearNames,
                               boolean tradable, boolean abilityNames) {
        for (String key : entriesObj.keySet()) {
            JsonElement el = entriesObj.get(key);
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject e = el.getAsJsonObject();
            String src = optString(e, "src");
            String dst = LineTranslator.deIcon(optString(e, "dst"));
            if (gearNames && src != null && !src.isBlank()
                    && (dst == null || dst.isBlank())
                    && "name".equals(optString(e, "role"))) {
                gearNameKeys.add(src.strip());
            }
            if (src != null && !src.isBlank()) {
                seenSources.add(src.strip());
                noteSequence(key, src.strip(), questName(optString(e, "quest")),
                        optString(e, "kind"), optString(e, "speaker"),
                        "wiki".equals(optString(e, "source")));
            }
            if (src != null && dst != null && !dst.isBlank()) {
                String srcKey = src.strip();
                entries.put(srcKey, dst.strip());
                if (!(itemNames && gearNames && "name".equals(optString(e, "role")))) {
                    otherOwners.add(srcKey);
                }
                if (!(itemNames && "name".equals(optString(e, "role")))) {
                    nonItemOwners.add(srcKey);
                } else {
                    itemNameDsts.computeIfAbsent(srcKey,
                            k -> ConcurrentHashMap.newKeySet()).add(dst.strip());
                }
                layerOf.put(srcKey, layer);
                ordered.add(srcKey);
                if (srcKey.length() >= MIN_PREFIX_LENGTH) {
                    prefixIndex.put(srcKey, dst.strip());
                }
                noteBlockSize(srcKey);
                noteFlat(srcKey, dst.strip());
                notePlayerKey(srcKey);
                noteWords(srcKey);
                noteUnwrapped(srcKey, dst.strip());
                noteIndented(srcKey, dst.strip());
                noteMarked(srcKey, dst.strip());
                noteLoose(srcKey, dst.strip());
                String quest = questName(optString(e, "quest"));
                if (quest != null && !quest.isBlank()) {
                    byQuest.computeIfAbsent(quest.strip(),
                            q -> new java.util.TreeMap<>()).put(srcKey, dst.strip());
                }
                if (quest != null && !quest.isBlank()) {
                    questOfLine.put(srcKey, quest.strip());
                }
                if ("wiki".equals(optString(e, "source"))) {
                    fromWiki.add(srcKey);
                }
                String who = optString(e, "speaker");
                if (who != null && !who.isBlank()) {
                    speakers.put(srcKey, who.strip());
                    String name = com.wynnchayuan.capture.DialogueSpeaker.cleanName(who);
                    if (quest != null && !quest.isBlank() && name != null) {
                        questCast.computeIfAbsent(quest.strip(),
                                q -> ConcurrentHashMap.newKeySet()).add(name);
                    }
                }
                if ("name".equals(optString(e, "role"))) {
                    if (tradable) {
                        market.add(srcKey, dst.strip());
                    }
                    if (!itemNames) {
                        noteTerm(srcKey, dst.strip());
                        if (abilityNames) {
                            abilityNameDsts.computeIfAbsent(srcKey,
                                    k -> ConcurrentHashMap.newKeySet()).add(dst.strip());
                        }
                    } else if (gearNames) {
                        nameKeys.add(srcKey);
                        noteBareName(srcKey);
                    } else {
                        plainNameKeys.add(srcKey);
                        noteBareName(srcKey);
                    }
                }
            }
        }
    }

    private void noteBareName(String key) {
        if (key.indexOf(ICON) >= 0) {
            bareNames.putIfAbsent(bareName(key), key);
        }
    }

    private void readFlat(JsonObject obj, boolean asTerms) {
        for (String key : obj.keySet()) {
            if (key.startsWith("_")) {
                continue;
            }
            JsonElement v = obj.get(key);
            seenSources.add(key.strip());
            if (v.isJsonPrimitive() && !v.getAsString().isBlank()) {
                String flat = LineTranslator.deIcon(v.getAsString().strip());
                entries.put(key.strip(), flat);
                otherOwners.add(key.strip());
                nonItemOwners.add(key.strip());
                layerOf.put(key.strip(), layer);
                market.addListed(key.strip(), flat);
                ordered.add(key.strip());
                if (!asTerms && key.strip().length() >= MIN_PREFIX_LENGTH) {
                    prefixIndex.put(key.strip(), flat);
                }
                noteBlockSize(key.strip());
                noteFlat(key.strip(), flat);
                notePlayerKey(key.strip());
                noteWords(key.strip());
                noteUnwrapped(key.strip(), flat);
                noteIndented(key.strip(), flat);
                noteMarked(key.strip(), flat);
                if (!asTerms) {
                    noteLoose(key.strip(), flat);
                }
                if (asTerms) {
                    noteTerm(key.strip(), flat, true);
                }
            }
        }
    }

    private void noteFlat(String key, String value) {
        if (key.length() < MIN_FLAT_LENGTH) {
            return;
        }
        layered(flat, "flat", normalise(key), value);
    }

    private static final int NAME_EVIDENCE = 12;

    private static final int MAX_PLAYER_NAME = 24;

    private final List<String[]> playerKeys = new java.util.ArrayList<>();

    private void notePlayerKey(String key) {
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

    java.util.Set<String> keys() {
        return java.util.Collections.unmodifiableSet(entries.keySet());
    }

    public Guess playerNameIn(String raw) {
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

    private void noteWords(String key) {
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
        return !entries.containsKey(name) && !speakers.containsKey(name);
    }

    private final Map<String, String> unwrapped = new ConcurrentHashMap<>();

    private void noteUnwrapped(String key, String value) {
        if (key.length() < MIN_FLAT_LENGTH) {
            return;
        }
        String joined = unwrap(key);
        boolean single = key.indexOf('\n') < 0;
        if (single && unwrappedFromWrapped.remove(joined)) {
            unwrapped.put(joined, value);
            indexLayer.put("unwrapped" + '\0' + joined, layer);
            return;
        }
        if (!thisLayer.contains("unwrapped" + '\u0000' + joined) && !single) {
            unwrappedFromWrapped.add(joined);
        }
        layered(unwrapped, "unwrapped", joined, value);
    }

    private final java.util.Set<String> unwrappedFromWrapped = new java.util.HashSet<>();

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

    private static final int MIN_FLAT_LENGTH = 24;

    private void noteTerm(String name, String translation) {
        noteTerm(name, translation, false);
    }

    private void noteTerm(String name, String translation, boolean curated) {
        if ((!curated && name.length() < MIN_TERM_LENGTH) || translation.isBlank()
                || name.indexOf('{') >= 0 || name.indexOf('\n') >= 0) {
            return;
        }
        terms.put(name, translation);
        maxTermWords = Math.max(maxTermWords, name.split(" ").length);
    }

    private static final int MIN_TERM_LENGTH = 4;

    public Term findTerm(String text, int from) {
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

    public String lookupTerm(String text) {
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
        if (store.abilityNamesWithOriginal() && ownAbilityName(name, hit)) {
            return withOriginal(hit, name);
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

    private void noteBlockSize(String key) {
        int lines = 1;
        for (int i = key.indexOf('\n'); i >= 0; i = key.indexOf('\n', i + 1)) {
            lines++;
        }
        if (lines > maxBlockLines) {
            maxBlockLines = lines;
        }
    }

    private static String optString(JsonObject o, String field) {
        JsonElement el = o.get(field);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : null;
    }

    private final java.util.Set<String> pageNames = new java.util.HashSet<>();

    public boolean isPageName(String text) {
        return text != null && pageNames.contains(text.strip());
    }

    public String pageTitleName(String text) {
        return text == null ? null : pageTitleNames.get(text.strip());
    }

    private final Map<String, String> pageTitleNames = new java.util.HashMap<>();

    private final java.util.Set<String> pageTitleSplit = new java.util.HashSet<>();

    private void notePageTitle(String name, String translated) {
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

    private boolean ownAbilityName(String key, String hit) {
        java.util.Set<String> own = abilityNameDsts.get(key);
        return own != null && hit != null && !hit.strip().equals(key) && own.contains(hit);
    }

    public String speakerOf(String template) {
        return template == null ? null : speakers.get(template.strip());
    }

    private final Map<String, String> speakers = new ConcurrentHashMap<>();

    public Boolean inCast(String quest, String speaker) {
        if (quest == null || speaker == null || speaker.isBlank()) {
            return null;
        }
        java.util.Set<String> cast = questCast.get(
                com.wynnchayuan.capture.GlyphSplitter.stripGlyphChars(quest).strip());
        if (cast == null || cast.isEmpty()) {
            return null;
        }
        return cast.contains(speaker.strip());
    }

    static String questName(String quest) {
        if (quest == null) {
            return null;
        }
        String q = quest.strip();
        return q.endsWith(" (Quest)") ? q.substring(0, q.length() - " (Quest)".length()).strip() : q;
    }

    public String questOf(String template) {
        return template == null ? null : questOfLine.get(template.strip());
    }

    private final Map<String, java.util.Set<String>> questCast = new ConcurrentHashMap<>();

    private final Map<String, String> questOfLine = new ConcurrentHashMap<>();

    public String lookupPrefix(String partial) {
        String source = matchPrefix(partial);
        return source == null ? null : prefixIndex.get(source);
    }

    public String speakerOfPrefix(String partial) {
        String source = matchPrefix(partial);
        return source == null ? null : speakers.get(source);
    }

    public String matchPrefix(String partial) {
        return matchPrefix(partial, partial == null ? 0 : partial.strip().length());
    }

    public String matchPrefix(String partial, int evidence) {
        return matchPrefix(partial, evidence, null);
    }

    public String matchPrefix(String partial, int evidence, String quest) {
        if (partial == null) {
            return null;
        }
        String key = partial.strip();
        if (key.isEmpty()) {
            return null;
        }
        if (quest != null && !quest.isBlank() && key.length() >= MIN_QUEST_PREFIX) {
            java.util.TreeMap<String, String> scoped = byQuest.get(
                    com.wynnchayuan.capture.GlyphSplitter.stripGlyphChars(quest).strip());
            if (scoped != null) {
                String hit = unique(scoped, key);
                if (hit != null) {
                    String better = curatedRival(hit, key);
                    String pick = better != null ? better : hit;
                    if (aloneInQuest(pick, key, GlyphSplitter.stripGlyphChars(quest).strip())) {
                        return pick;
                    }
                }
            }
        }
        if (evidence < MIN_PREFIX_LENGTH) {
            return null;
        }
        return unique(prefixIndex, key);
    }

    public String nearQuestLine(String typed, String quest) {
        if (typed == null || quest == null || quest.isBlank()) {
            return null;
        }
        java.util.TreeMap<String, String> scoped = byQuest.get(
                com.wynnchayuan.capture.GlyphSplitter.stripGlyphChars(quest).strip());
        if (scoped == null) {
            return null;
        }
        NearQuery last = lastNear;
        if (last != null && last.typed().equals(typed) && last.quest().equals(quest)) {
            return last.result();
        }
        String result = nearQuestLineUncached(typed, scoped);
        lastNear = new NearQuery(typed, quest, result);
        return result;
    }

    private String nearQuestLineUncached(String typed, java.util.TreeMap<String, String> scoped) {
        String[] said = nearWords(typed);
        if (said.length < NEAR_MIN_WORDS) {
            return null;
        }
        int allowed = Math.max(1, said.length / 8);
        String best = null;
        int bestCost = Integer.MAX_VALUE;
        int second = Integer.MAX_VALUE;
        for (String src : scoped.keySet()) {
            String[] words = nearWordCache.computeIfAbsent(src, TranslationIndex::nearWords);
            int cost = Integer.MAX_VALUE;
            for (int take = Math.max(1, said.length - allowed);
                 take <= Math.min(words.length, said.length + allowed); take++) {
                cost = Math.min(cost, wordDistance(said, words, take));
            }
            if (cost < bestCost) {
                second = bestCost;
                bestCost = cost;
                best = src;
            } else if (cost < second) {
                second = cost;
            }
        }
        if (best == null || bestCost > allowed || second - bestCost < 2) {
            return null;
        }
        return best;
    }

    record SeqLine(String src, String speaker, boolean wiki) {}

    private final Map<String, List<SeqLine>> questSeq = new ConcurrentHashMap<>();

    private final Map<String, Map<String, List<Integer>>> questSeqAt = new ConcurrentHashMap<>();

    private final Set<String> seqKeys = ConcurrentHashMap.newKeySet();

    private final Map<String, Set<String>> seqQuestOf = new ConcurrentHashMap<>();

    private final Map<String, Set<String>> bySpeaker = new ConcurrentHashMap<>();

    private final Set<String> unplacedLines = ConcurrentHashMap.newKeySet();

    private void noteSequence(String entryKey, String src, String quest, String kind,
                              String speaker, boolean wiki) {
        String who = speakerKey(speaker);
        if (!who.isEmpty()) {
            bySpeaker.computeIfAbsent(who, k -> ConcurrentHashMap.newKeySet()).add(src);
        }
        if (quest == null || quest.isBlank()) {
            return;
        }
        if (kind != null && !kind.isBlank() && !"dialogue".equals(kind)) {
            return;
        }
        String q = quest.strip();
        if (!seqKeys.add(q + '\0' + entryKey)) {
            return;
        }
        List<SeqLine> seq = questSeq.computeIfAbsent(q,
                k -> new java.util.concurrent.CopyOnWriteArrayList<>());
        questSeqAt.computeIfAbsent(q, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(src, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(seq.size());
        seq.add(new SeqLine(src, who, wiki));
        seqQuestOf.computeIfAbsent(src, k -> ConcurrentHashMap.newKeySet()).add(q);
    }

    public static String speakerKey(String raw) {
        String name = com.wynnchayuan.capture.DialogueSpeaker.cleanName(raw);
        if (name == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = Character.toLowerCase(name.charAt(i));
            if (Character.isLetterOrDigit(c) || c == '?') {
                out.append(c);
            }
        }
        return out.toString();
    }

    public String sequenceQuestOf(String src, String... prefer) {
        if (src == null) {
            return null;
        }
        String key = src.strip();
        Set<String> homes = seqQuestOf.get(key);
        if (homes == null || homes.isEmpty()) {
            return null;
        }
        for (String p : prefer) {
            if (p != null) {
                String q = GlyphSplitter.stripGlyphChars(p).strip();
                if (homes.contains(q)) {
                    return q;
                }
            }
        }
        return homes.size() == 1 ? homes.iterator().next() : null;
    }

    public int sequenceIndex(String quest, String src, int after) {
        if (quest == null || src == null) {
            return -1;
        }
        Map<String, List<Integer>> at = questSeqAt.get(quest);
        List<Integer> spots = at == null ? null : at.get(src.strip());
        if (spots == null || spots.isEmpty()) {
            return -1;
        }
        for (int spot : spots) {
            if (spot > after) {
                return spot;
            }
        }
        return spots.get(0);
    }

    public String forecast(String typed, String quest, int after, int window, String speaker,
                           int floor, java.util.function.BiPredicate<String, String> fits) {
        if (typed == null || quest == null || speaker == null) {
            return null;
        }
        String key = typed.strip();
        String who = speakerKey(speaker);
        if (key.isEmpty() || key.length() < floor || who.isEmpty()) {
            return null;
        }
        return forecastUncached(key, GlyphSplitter.stripGlyphChars(quest).strip(), after,
                window, who, fits, true);
    }

    public String forecastBySpeaker(String typed, String quest, String speaker, int floor,
                                    java.util.function.BiPredicate<String, String> fits) {
        if (typed == null || quest == null || speaker == null) {
            return null;
        }
        String key = typed.strip();
        String who = speakerKey(speaker);
        if (key.isEmpty() || key.length() < floor || who.isEmpty()) {
            return null;
        }
        return forecastUncached(key, GlyphSplitter.stripGlyphChars(quest).strip(), -1, 0, who,
                fits, false);
    }

    public String forecastNext(String typed, String quest, int after, int window, String speaker,
                               int floor, java.util.function.BiPredicate<String, String> fits) {
        if (typed == null || quest == null || after < 0) {
            return null;
        }
        String key = typed.strip();
        if (key.length() < floor) {
            return null;
        }
        List<SeqLine> seq = questSeq.get(GlyphSplitter.stripGlyphChars(quest).strip());
        if (seq == null) {
            return null;
        }
        String who = speaker == null ? "" : speakerKey(speaker);
        List<SeqLine> group = new java.util.ArrayList<>();
        for (int i = after + 1; i < Math.min(seq.size(), after + 1 + window); i++) {
            SeqLine line = seq.get(i);
            if (!who.isEmpty() && !line.speaker().isEmpty() && !line.speaker().equals(who)) {
                continue;
            }
            if (!looseFit(line.src(), key)) {
                continue;
            }
            if (!group.isEmpty() && !group.get(0).src().equals(line.src())
                    && !versionOf(group.get(0).src(), line.src())) {
                return null;
            }
            group.add(line);
        }
        String pick = null;
        for (SeqLine line : group) {
            String dst = entries.get(line.src());
            if (dst == null || dst.isBlank() || !fits.test(line.src(), key)) {
                continue;
            }
            if (pick == null || (!line.wiki() && fromWiki.contains(pick))) {
                pick = line.src();
            }
        }
        return pick;
    }

    private String forecastUncached(String key, String quest, int after, int window, String who,
                                    java.util.function.BiPredicate<String, String> fits,
                                    boolean elsewhere) {
        List<SeqLine> seq = questSeq.get(quest);
        if (seq == null) {
            return null;
        }
        int from = after >= 0 ? after + 1 : 0;
        int to = after >= 0 ? Math.min(seq.size(), from + window) : seq.size();
        List<SeqLine> group = new java.util.ArrayList<>();
        for (int i = from; i < to; i++) {
            SeqLine line = seq.get(i);
            if (!line.speaker().isEmpty() && !line.speaker().equals(who)) {
                continue;
            }
            if (!looseFit(line.src(), key)) {
                continue;
            }
            if (!group.isEmpty() && !group.get(0).src().equals(line.src())
                    && !versionOf(group.get(0).src(), line.src())) {
                return null;
            }
            group.add(line);
        }
        if (group.isEmpty()) {
            return null;
        }
        boolean vouched = false;
        for (SeqLine line : group) {
            vouched |= line.speaker().equals(who);
        }
        if (!vouched) {
            return null;
        }
        String pick = null;
        for (SeqLine line : group) {
            String dst = entries.get(line.src());
            if (dst == null || dst.isBlank() || !fits.test(line.src(), key)) {
                continue;
            }
            if (pick == null || (!line.wiki() && fromWiki.contains(pick))) {
                pick = line.src();
            }
        }
        if (pick == null) {
            return null;
        }
        for (SeqLine line : seq) {
            if ((line.speaker().isEmpty() || line.speaker().equals(who))
                    && rival(pick, line.src(), key)) {
                return null;
            }
        }
        if (!elsewhere) {
            return pick;
        }
        Set<String> theirs = bySpeaker.get(who);
        if (theirs != null) {
            for (String src : theirs) {
                if (rival(pick, src, key)) {
                    return null;
                }
            }
        }
        for (String src : unplacedRivals ? unplacedLines : Set.<String>of()) {
            if (rival(pick, src, key)) {
                return null;
            }
        }
        return pick;
    }


    public String explainForecast(String typed, String quest, int after, int window,
                                  String speaker, java.util.function.BiPredicate<String, String> fits) {
        String who = speakerKey(speaker);
        if (who.isEmpty()) {
            return "沒有名牌";
        }
        String key = typed.strip();
        List<SeqLine> seq = questSeq.get(quest == null ? "" : quest);
        if (seq == null) {
            return "沒有任務";
        }
        int from = after >= 0 ? after + 1 : 0;
        int to = after >= 0 ? Math.min(seq.size(), from + window) : seq.size();
        List<SeqLine> group = new java.util.ArrayList<>();
        for (int i = from; i < to; i++) {
            SeqLine line = seq.get(i);
            if (!line.speaker().isEmpty() && !line.speaker().equals(who)) {
                continue;
            }
            if (!looseFit(line.src(), key)) {
                continue;
            }
            if (!group.isEmpty() && !group.get(0).src().equals(line.src())
                    && !versionOf(group.get(0).src(), line.src())) {
                return "後面幾句裡有兩句同開頭";
            }
            group.add(line);
        }
        if (group.isEmpty()) {
            return "不在後面幾句裡";
        }
        boolean vouched = false;
        String pick = null;
        for (SeqLine line : group) {
            vouched |= line.speaker().equals(who);
            String dst = entries.get(line.src());
            if (dst != null && !dst.isBlank() && fits.test(line.src(), key)) {
                pick = line.src();
            }
        }
        if (!vouched) {
            return "語料沒標說話者";
        }
        if (pick == null) {
            return "沒有譯文";
        }
        for (SeqLine line : seq) {
            if ((line.speaker().isEmpty() || line.speaker().equals(who))
                    && rival(pick, line.src(), key)) {
                return line.src().startsWith(key) ? "任務裡別句同開頭" : "任務裡別句岔在佔位符";
            }
        }
        Set<String> theirs = bySpeaker.get(who);
        if (theirs != null) {
            for (String src : theirs) {
                if (rival(pick, src, key)) {
                    return "同一人別的任務有句同開頭";
                }
            }
        }
        for (String src : unplacedLines) {
            if (rival(pick, src, key)) {
                return "未歸屬台詞同開頭";
            }
        }
        return "可以";
    }

    public String explainQuest(String typed, String quest) {
        String key = typed.strip();
        if (quest == null) {
            return "沒有任務";
        }
        String q = GlyphSplitter.stripGlyphChars(quest).strip();
        if (key.length() < MIN_QUEST_PREFIX) {
            return "不到4字";
        }
        java.util.TreeMap<String, String> scoped = byQuest.get(q);
        if (scoped == null) {
            return "沒有任務";
        }
        String hit = unique(scoped, key);
        if (hit == null) {
            Map.Entry<String, String> first = scoped.ceilingEntry(key);
            return first == null || !first.getKey().startsWith(key) ? "任務裡沒有這句"
                    : "任務裡多句同開頭";
        }
        String better = curatedRival(hit, key);
        String pick = better != null ? better : hit;
        List<SeqLine> seq = questSeq.get(q);
        if (seq != null) {
            for (SeqLine line : seq) {
                if (rival(pick, line.src(), key)) {
                    if (!line.src().startsWith(key)) {
                        return "任務裡別句岔在佔位符";
                    }
                    String dst = entries.get(line.src());
                    return dst == null || dst.isBlank() ? "任務裡沒翻的句同開頭" : "任務裡短版本同開頭";
                }
            }
        }
        return "可以";
    }

    private boolean aloneInQuest(String pick, String key, String quest) {
        List<SeqLine> seq = questSeq.get(quest);
        if (seq == null) {
            return true;
        }
        for (SeqLine line : seq) {
            if (rival(pick, line.src(), key)) {
                return false;
            }
        }
        return true;
    }

    private boolean rival(String pick, String src, String key) {
        return !src.equals(pick) && looseFit(src, key) && !versionOf(pick, src);
    }

    static boolean looseFit(String src, String typed) {
        if (src.startsWith(typed) || slotFits(src, typed)) {
            return true;
        }
        String other = respell(typed);
        return !other.equals(typed) && src.startsWith(other);
    }


    private static final int NEAR_MIN_WORDS = 8;

    private record NearQuery(String typed, String quest, String result) {}

    private volatile NearQuery lastNear;

    private final Map<String, String[]> nearWordCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static final java.util.regex.Pattern NEAR_SPACE = java.util.regex.Pattern.compile("\\s+");

    private static final java.util.regex.Pattern NEAR_EDGES =
            java.util.regex.Pattern.compile("^[\\p{Punct}…“”‘’]+|[\\p{Punct}…“”‘’]+$");

    private static String[] nearWords(String text) {
        String[] raw = NEAR_SPACE.split(text.strip());
        List<String> out = new java.util.ArrayList<>(raw.length);
        for (String w : raw) {
            String t = NEAR_EDGES.matcher(w.toLowerCase(java.util.Locale.ROOT)).replaceAll("");
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out.toArray(new String[0]);
    }

    private static int wordDistance(String[] a, String[] b, int take) {
        int[] prev = new int[take + 1];
        int[] cur = new int[take + 1];
        for (int j = 0; j <= take; j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length; i++) {
            cur[0] = i;
            for (int j = 1; j <= take; j++) {
                int sub = prev[j - 1] + (a[i - 1].equals(b[j - 1]) ? 0 : 1);
                cur[j] = Math.min(sub, Math.min(prev[j] + 1, cur[j - 1] + 1));
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[take];
    }

    private String unique(java.util.TreeMap<String, String> index, String key) {
        Map.Entry<String, String> first = index.ceilingEntry(key);
        if (first == null || !first.getKey().startsWith(key)) {
            String other = respell(key);
            if (other.equals(key)) {
                return null;
            }
            first = index.ceilingEntry(other);
            if (first == null || !first.getKey().startsWith(other)) {
                return null;
            }
            key = other;
        }
        String next = index.higherKey(first.getKey());
        if (next != null && next.startsWith(key)) {
            return settle(index, key, first.getKey());
        }
        return first.getKey();
    }

    private String settle(java.util.TreeMap<String, String> index, String key, String first) {
        String curated = null;
        int scanned = 0;
        int best = -1;
        for (Map.Entry<String, String> e : index.tailMap(key).entrySet()) {
            if (!e.getKey().startsWith(key)) {
                break;
            }
            if (++scanned > RIVAL_SCAN || !sameLine(first, e.getKey())) {
                return null;
            }
            best = Math.max(best, layerOf(e.getKey()));
            if (!fromWiki.contains(e.getKey())) {
                if (curated != null) {
                    return null;
                }
                curated = e.getKey();
            }
        }
        return curated != null && layerOf(curated) < best ? null : curated;
    }

    private static final int MIN_PREFIX_LENGTH = 12;

    // 實測限定任務後平均第 4 個字就分得出
    private static final int MIN_QUEST_PREFIX = 4;

    private String curatedRival(String wikiHit, String key) {
        if (!fromWiki.contains(wikiHit)) {
            return null;
        }
        int scanned = 0;
        for (Map.Entry<String, String> e : prefixIndex.tailMap(key).entrySet()) {
            if (!e.getKey().startsWith(key) || ++scanned > RIVAL_SCAN) {
                break;
            }
            if (fromWiki.contains(e.getKey()) || e.getKey().equals(wikiHit)) {
                continue;
            }
            String home = questOfLine.get(wikiHit);
            String theirs = questOfLine.get(e.getKey());
            if (home != null && theirs != null && !home.equals(theirs)) {
                continue;
            }
            if (versionOf(wikiHit, e.getKey())
                    && layerOf(e.getKey()) >= layerOf(wikiHit)) {
                return e.getKey();
            }
        }
        return null;
    }

    static boolean versionOf(String a, String b) {
        return words(a).size() >= VERSION_WORDS && words(b).size() >= VERSION_WORDS
                && sameLine(a, b);
    }

    private static final int VERSION_WORDS = 6;

    private static final int RIVAL_SCAN = 16;

    private final java.util.Set<String> fromWiki =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    public boolean hasLonger(String key) {
        if (key == null) {
            return false;
        }
        String bare = key.strip();
        if (bare.isEmpty()) {
            return false;
        }
        String next = ordered.higher(bare);
        return next != null && next.startsWith(bare);
    }

    public boolean couldStillMatch(String typed) {
        if (typed == null) {
            return false;
        }
        String key = typed.strip();
        if (key.isEmpty()) {
            return true;
        }
        if (startsSome(key)) {
            return true;
        }
        String other = respell(key);
        if (!other.equals(key) && startsSome(other)) {
            return true;
        }
        for (int cut = key.length() - 1; cut >= 0; cut--) {
            String before = key.substring(0, cut);
            for (String slot : SLOTS) {
                String head = before + slot;
                int scanned = 0;
                for (String src : ordered.tailSet(head)) {
                    if (!src.startsWith(head) || ++scanned > 64) {
                        break;
                    }
                    if (slotFits(src, key)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static final String[] SLOTS = {"{u}", "{p}", "{~", "{#}"};

    private boolean startsSome(String key) {
        String next = ordered.ceiling(key);
        return next != null && next.startsWith(key);
    }

    private final java.util.concurrent.ConcurrentSkipListSet<String> ordered =
            new java.util.concurrent.ConcurrentSkipListSet<>();

    private final java.util.TreeMap<String, String> prefixIndex = new java.util.TreeMap<>();

    private final Map<String, java.util.TreeMap<String, String>> byQuest =
            new java.util.HashMap<>();

    public boolean isBareGearName(String key) {
        if (key == null) {
            return false;
        }
        String k = key.strip();
        return gearNameKeys.contains(k) || (!store.translatesNames() && nameKeys.contains(k));
    }

    private boolean gearOnly(String key) {
        return nameKeys.contains(key) && !otherOwners.contains(key);
    }

    private boolean plainOnly(String key) {
        return plainNameKeys.contains(key) && !nonItemOwners.contains(key);
    }

    private boolean getsOriginal(String key) {
        return gearOnly(key) || plainOnly(key);
    }

    private boolean ownItemName(String key, String hit) {
        java.util.Set<String> own = itemNameDsts.get(key);
        return own != null && hit != null && own.contains(hit);
    }

    public boolean itemNameOnly(String key) {
        return key != null && getsOriginal(key.strip());
    }

    public boolean gearNameOnly(String key) {
        return key != null && gearOnly(key.strip());
    }

    public boolean isItemName(String key) {
        if (key == null) {
            return false;
        }
        String k = key.strip();
        return nameKeys.contains(k) || gearNameKeys.contains(k) || plainNameKeys.contains(k);
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
        if (BAR_ITEMS.get() && template != null && getsOriginal(template.strip())) {
            return null;
        }
        boolean holdAppended = HOLD_APPENDED.get();
        if (LOOTRUN_TITLE.get() && LootrunNames.isName(template)) {
            String name = template.strip();
            String own = lootrunName(name);
            if (own != null) {
                return holdAppended ? own : LootrunNames.apply(name, own, store.pageNameMode());
            }
            if (gearOnly(name)) {
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
                        ? withOriginal(own, template.strip()) : own;
            }
        }
        String hit = lookupBase(template);
        if (hit == null) {
            return shiny(template);
        }
        if (store.namesWithOriginal() && !holdAppended && (getsOriginal(template.strip())
                || NAME_LINE.get() && ownItemName(template.strip(), hit))) {
            return withOriginal(hit, template.strip());
        }
        if (store.abilityNamesWithOriginal() && ownAbilityName(template.strip(), hit)) {
            return withOriginal(hit, template.strip());
        }
        if (store.abilityNamesWithOriginal()) {
            String labelled = abilityLabelWithOriginal(template.strip(), hit);
            if (labelled != null) {
                return labelled;
            }
        }
        return holdAppended ? hit : LootrunNames.apply(template, hit, store.pageNameMode());
    }

    private static String withOriginal(String hit, String key) {
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

    private String abilityLabelWithOriginal(String key, String hit) {
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

    private static final java.util.regex.Pattern NUMBERED =
            java.util.regex.Pattern.compile("\\{~\\d+}");

    private final Map<String, String> bareNames = new ConcurrentHashMap<>();

    public int appendedOriginalAt(String text) {
        if (!store.namesWithOriginal() || text == null) {
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

    public List<int[]> keptOriginals(String text) {
        List<int[]> out = new java.util.ArrayList<>();
        if (text == null) {
            return out;
        }
        int at = appendedOriginalAt(text);
        if (at >= 0) {
            out.add(new int[] {at, text.length()});
        }
        if (store.pageNameMode() != com.wynnchayuan.CollectorConfig.ItemNames.ON) {
            out.addAll(LootrunNames.spans(text));
        }
        out.addAll(abilityOriginalSpans(text));
        return out;
    }

    List<int[]> abilityOriginalSpans(String text) {
        if (!store.abilityNamesWithOriginal() || text == null || text.indexOf('(') < 0) {
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

    public int[] appendedOriginalSpan(String text) {
        if (!store.namesWithOriginal() || text == null) {
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
        String own = gearOwnName(inner);
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
                ? NUMBERED.matcher(inner).replaceAll(java.util.regex.Matcher.quoteReplacement(
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

    private String shiny(String template) {
        if (template == null || !store.translatesNames()) {
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
        String pattern = scopedLookup("name", "Shiny");
        if (pattern == null || !pattern.contains("{name}")) {
            return null;
        }
        String name = gearOwnName(rest);
        if (name == null) {
            name = bare ? null : lookupBase(rest);
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
        if (store.namesWithOriginal()) {
            out = out + " (" + key + ")";
        }
        return out;
    }

    private boolean isNameRow(String key) {
        String inner = nameRowInner(key);
        return inner != null && (nameKeys.contains(inner) || gearNameKeys.contains(inner)
                || plainNameKeys.contains(inner));
    }

    private String lookupBase(String template) {
        if (template == null) {
            return null;
        }
        String key = template.strip();
        if (!store.translatesNames() && gearOnly(key)) {
            return null;
        }
        if (peeked(key)) {
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
        if (hit == null) {
            String other = respell(key);
            if (!other.equals(key)) {
                hit = exact(other, topOnly);
                if (hit == null) {
                    hit = indented(other, topOnly);
                }
            }
        }
        if (hit == null && (!topOnly || fromTop("marked", sameMarks(key)))) {
            hit = lookupMarked(key);
        }
        if (hit == null) {
            hit = loose(key, topOnly);
        }
        if (hit == null) {
            String other = respell(key);
            if (!other.equals(key)) {
                hit = loose(other, topOnly);
            }
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
        return lookupIndented(key);
    }

    private String loose(String key, boolean topOnly) {
        if (topOnly && layerOf(looseSource(key)) != topLayer) {
            return null;
        }
        return lookupLoose(key);
    }

    private boolean fromTop(String tag, String key) {
        Integer at = indexLayer.get(tag + '\0' + key);
        return at != null && at == topLayer;
    }

    private static final char MARK_YES = '✔';

    private static final char MARK_NO = '✖';

    private final Map<String, String> marked = new ConcurrentHashMap<>();

    private static boolean hasMark(String text) {
        return text.indexOf(MARK_YES) >= 0 || text.indexOf(MARK_NO) >= 0;
    }

    private static String sameMarks(String text) {
        return text.replace(MARK_YES, MARK_NO);
    }

    private void noteMarked(String src, String dst) {
        if (hasMark(src) && countMarks(src) == countMarks(dst)) {
            layered(marked, "marked", sameMarks(src), dst);
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

    private String lookupMarked(String key) {
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

    private String lookupLoose(String key) {
        String src = looseSource(key);
        if (src == null) {
            return null;
        }
        if (!store.translatesNames() && gearOnly(src)) {
            return null;
        }
        if (peeked(src)) {
            return null;
        }
        return entries.get(src);
    }

    private String looseSource(String key) {
        if (key == null) {
            return null;
        }
        String loosened = loosen(key);
        if (!looseEnough(loosened) || looseClash.contains(loosened)) {
            return null;
        }
        return loose.get(loosened);
    }

    private void noteLoose(String src, String dst) {
        String loosened = loosen(src);
        if (!looseEnough(loosened)) {
            return;
        }
        boolean firstHere = thisLayer.add("loose" + '\0' + loosened);
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

    public java.util.Set<String> sourceKeys() {
        return java.util.Set.copyOf(seenSources);
    }

    public boolean hasTranslation(String template) {
        if (template == null) {
            return false;
        }
        String key = template.strip();
        if (entries.containsKey(key) || lookupIndented(key) != null) {
            return true;
        }
        if (isNameRow(key)) {
            return true;
        }
        if (lookupMarked(key) != null) {
            return true;
        }
        if (looseSource(key) != null || looseSource(respell(key)) != null) {
            return true;
        }
        if (LineTranslator.lookup(key, store, false) != null
                || LineTranslator.lookup(key, store, true) != null) {
            return true;
        }
        if (key.indexOf('\n') >= 0 && LineTranslator.eventFrame(unwrap(key), store) != null) {
            return true;
        }
        if (key.indexOf('\n') >= 0 && LineTranslator.countdownBoard(key, store) != null) {
            return true;
        }
        String other = respell(key);
        return !other.equals(key)
                && (entries.containsKey(other) || lookupIndented(other) != null);
    }

    private final Map<String, String> unindented = new ConcurrentHashMap<>();

    private void noteIndented(String src, String dst) {
        int n = indentOf(src);
        if (n > 0) {
            layered(unindented, "unindented",
                    src.substring(n), dst.substring(indentOf(dst)));
        }
    }

    private String lookupIndented(String key) {
        int indent = indentOf(key);
        if (indent == 0) {
            return null;
        }
        String body = unindented.get(key.substring(indent));
        return body == null ? null : key.substring(0, indent) + body;
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
}
