package com.wynnchayuan.translate;

import com.wynnchayuan.ModLog;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.wynnchayuan.translate.TranslationStore.normalise;

final class TranslationLoader {

    private final TranslationIndex translations;

    TranslationLoader(TranslationIndex translations) {
        this.translations = translations;
    }

    void load(List<Path> dirs) {
        translations.topLayer = Math.max(0, dirs.size() - 1);
        for (layer = 0; layer < dirs.size(); layer++) {
            readOne(dirs.get(layer));
        }
        layer = translations.topLayer;
        translations.nameRules.gearNameKeys.removeAll(translations.nameRules.nameKeys);
        translations.nameFrames = com.wynnchayuan.capture.NameFrames.of(translations.seenSources);
        translations.chatSpeakers = TranslationIndex.learnChatSpeakers(translations.entries);
        report(dirs.isEmpty() ? null : dirs.get(dirs.size() - 1));
    }

    int layer = 0;

    final java.util.Set<String> thisLayer = new java.util.HashSet<>();

    void layered(java.util.Map<String, String> index, String tag,
                         String key, String value) {
        if (thisLayer.add(tag + '\u0000' + key)) {
            index.put(key, value);
            translations.indexLayer.put(tag + '\0' + key, layer);
        }
    }

    private void readOne(Path dir) {
        thisLayer.clear();
        translations.unwrappedFromWrapped.clear();
        if (!Files.isDirectory(dir)) {
            try {
                Files.createDirectories(dir);
                translations.lastResult = com.wynnchayuan.client.T.s("status.folder_created");
                ModLog.info("譯文資料夾不存在，已建立：" + dir);
            } catch (Exception e) {
                translations.lastResult = com.wynnchayuan.client.T.s("status.folder_failed", e.getMessage());
                ModLog.warn("建立譯文資料夾失敗 " + dir + ": " + e.getMessage());
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
            translations.lastResult = com.wynnchayuan.client.T.s("status.read_failed", e.getMessage());
            ModLog.warn("讀取譯文目錄失敗 " + dir + ": " + e.getMessage());
        }
    }

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
                        translations.scopedOnly.computeIfAbsent(scope, k -> new java.util.HashMap<>());
                for (String key : obj.keySet()) {
                    JsonElement v = obj.get(key);
                    if (!key.startsWith("_") && v.isJsonPrimitive()
                            && !v.getAsString().isBlank()) {
                        into.put(key.strip(), DisplayStyles.deIcon(
                                v.getAsString().strip()));
                    }
                }
            } catch (Exception e) {
                ModLog.warn("讀不到 " + file + "：" + e.getMessage());
            }
        }
    }

    private void report(Path dir) {
        if (dir == null) {
            return;
        }
        if (translations.loadedFiles == 0) {
            translations.lastResult = com.wynnchayuan.client.T.s("status.no_json");
            ModLog.info(dir + " 裡沒有譯文檔");
        } else if (translations.entries.isEmpty()) {
            translations.lastResult = com.wynnchayuan.client.T.s("status.no_dst", translations.loadedFiles);
            ModLog.info("讀了 " + translations.loadedFiles + " 個檔案，但沒有任何已填寫的 dst");
        } else {
            translations.lastResult = com.wynnchayuan.client.T.s("status.loaded", translations.entries.size(), translations.loadedFiles);
            ModLog.info("已載入 " + translations.entries.size() + " 條譯文（" + translations.loadedFiles + " 個檔案）");
        }
    }

    private static int indexOf(List<String> order, String name) {
        int i = order.indexOf(name);
        return i < 0 ? Integer.MAX_VALUE : i;
    }

    private void load(Path file) {
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(r);
            if (!root.isJsonObject()) {
                translations.broken.add(file);
                ModLog.warn("略過 " + file.getFileName() + "：不是 JSON 物件");
                return;
            }
            JsonObject obj = root.getAsJsonObject();
            int before = translations.entries.size();

            if (obj.has("entries") && obj.get("entries").isJsonObject()) {
                readWorkspace(obj.getAsJsonObject("entries"), itemNames(obj), gearNames(obj),
                        tradable(file), abilityNames(file, obj));
            } else {
                readFlat(obj, file.getFileName().toString().endsWith("-terms.json"));
                if (file.getFileName().toString().equals("quest.json")) {
                    for (String key : obj.keySet()) {
                        if (!key.startsWith("_") && !key.isBlank()) {
                            translations.questIndex.unplacedLines.add(key.strip());
                        }
                    }
                }
                for (String key : obj.keySet()) {
                    String name = PageNames.nameOfTitle(key);
                    if (name != null && name.indexOf('{') < 0) {
                        translations.pageNames.add(name.strip());
                        JsonElement value = obj.get(key);
                        if (value != null && value.isJsonPrimitive()) {
                            translations.notePageTitle(name.strip(), value.getAsString());
                        }
                    }
                }
                if (file.getFileName().toString().equals(QUEST_NAMES)) {
                    for (String key : obj.keySet()) {
                        JsonElement value = obj.get(key);
                        if (!key.startsWith("_") && value.isJsonPrimitive()) {
                            translations.noteQuestName(key.strip(), value.getAsString().strip());
                        }
                    }
                }
                if (file.getFileName().toString().equals(UI_LABELS)) {
                    for (String key : obj.keySet()) {
                        if (!key.startsWith("_")) {
                            translations.uiLabels.add(key.strip());
                        }
                    }
                }
            }
            translations.loadedFiles++;
            ModLog.info("載入譯文 " + file.getFileName()
                    + "：+" + (translations.entries.size() - before) + " 條");
        } catch (com.google.gson.JsonParseException e) {
            if (!(e instanceof com.google.gson.JsonIOException)
                    || e.getCause() instanceof java.nio.charset.CharacterCodingException) {
                translations.broken.add(file);
            }
            ModLog.warn("略過 " + file.getFileName() + "：" + e.getMessage());
        } catch (Exception e) {
            ModLog.warn("略過 " + file.getFileName() + "：" + e.getMessage());
        }
    }

    private static final String UI_LABELS = "ui-labels.json";

    private static final String QUEST_NAMES = "quest-name.json";

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
            String dst = DisplayStyles.deIcon(optString(e, "dst"));
            if (gearNames && src != null && !src.isBlank()
                    && (dst == null || dst.isBlank())
                    && "name".equals(optString(e, "role"))) {
                translations.nameRules.gearNameKeys.add(src.strip());
            }
            if (src != null && !src.isBlank()) {
                translations.seenSources.add(src.strip());
                translations.questIndex.noteSequence(key, src.strip(), QuestDialogueIndex.questName(optString(e, "quest")),
                        optString(e, "kind"), optString(e, "speaker"),
                        "wiki".equals(optString(e, "source")));
            }
            if (src != null && dst != null && !dst.isBlank()) {
                String srcKey = src.strip();
                translations.entries.put(srcKey, dst.strip());
                if (!(itemNames && gearNames && "name".equals(optString(e, "role")))) {
                    translations.nameRules.otherOwners.add(srcKey);
                }
                if (!(itemNames && "name".equals(optString(e, "role")))) {
                    translations.nameRules.nonItemOwners.add(srcKey);
                } else {
                    translations.nameRules.itemNameDsts.computeIfAbsent(srcKey,
                            k -> ConcurrentHashMap.newKeySet()).add(dst.strip());
                }
                translations.layerOf.put(srcKey, layer);
                translations.questIndex.ordered.add(srcKey);
                if (srcKey.length() >= QuestDialogueIndex.MIN_PREFIX_LENGTH) {
                    translations.questIndex.prefixIndex.put(srcKey, dst.strip());
                }
                translations.noteBlockSize(srcKey);
                noteFlat(srcKey, dst.strip());
                translations.playerNames.notePlayerKey(srcKey);
                translations.playerNames.noteWords(srcKey);
                translations.noteUnwrapped(srcKey, dst.strip());
                translations.looseMatcher.noteIndented(srcKey, dst.strip());
                translations.looseMatcher.noteMarked(srcKey, dst.strip());
                translations.looseMatcher.noteLoose(srcKey, dst.strip());
                String quest = QuestDialogueIndex.questName(optString(e, "quest"));
                if (quest != null && !quest.isBlank()) {
                    translations.questIndex.byQuest.computeIfAbsent(quest.strip(),
                            q -> new java.util.TreeMap<>()).put(srcKey, dst.strip());
                }
                if (quest != null && !quest.isBlank()) {
                    translations.questIndex.questOfLine.put(srcKey, quest.strip());
                }
                if ("wiki".equals(optString(e, "source"))) {
                    translations.questIndex.fromWiki.add(srcKey);
                }
                String who = optString(e, "speaker");
                if (who != null && !who.isBlank()) {
                    translations.questIndex.speakers.put(srcKey, who.strip());
                    String name = com.wynnchayuan.capture.DialogueSpeaker.cleanName(who);
                    if (quest != null && !quest.isBlank() && name != null) {
                        translations.questIndex.questCast.computeIfAbsent(quest.strip(),
                                q -> ConcurrentHashMap.newKeySet()).add(name);
                    }
                }
                if ("name".equals(optString(e, "role"))) {
                    if (tradable) {
                        translations.market.add(srcKey, dst.strip());
                    }
                    if (!itemNames) {
                        translations.termIndex.noteTerm(srcKey, dst.strip());
                        if (abilityNames) {
                            translations.nameRules.abilityNameDsts.computeIfAbsent(srcKey,
                                    k -> ConcurrentHashMap.newKeySet()).add(dst.strip());
                        }
                    } else if (gearNames) {
                        translations.nameRules.nameKeys.add(srcKey);
                        translations.nameRules.noteBareName(srcKey);
                    } else {
                        translations.nameRules.plainNameKeys.add(srcKey);
                        translations.nameRules.noteBareName(srcKey);
                    }
                }
            }
        }
    }

    private void readFlat(JsonObject obj, boolean asTerms) {
        for (String key : obj.keySet()) {
            if (key.startsWith("_")) {
                continue;
            }
            JsonElement v = obj.get(key);
            translations.seenSources.add(key.strip());
            if (v.isJsonPrimitive() && !v.getAsString().isBlank()) {
                String flat = DisplayStyles.deIcon(v.getAsString().strip());
                translations.entries.put(key.strip(), flat);
                translations.nameRules.otherOwners.add(key.strip());
                translations.nameRules.nonItemOwners.add(key.strip());
                translations.layerOf.put(key.strip(), layer);
                translations.market.addListed(key.strip(), flat);
                translations.questIndex.ordered.add(key.strip());
                if (!asTerms && key.strip().length() >= QuestDialogueIndex.MIN_PREFIX_LENGTH) {
                    translations.questIndex.prefixIndex.put(key.strip(), flat);
                }
                translations.noteBlockSize(key.strip());
                noteFlat(key.strip(), flat);
                translations.playerNames.notePlayerKey(key.strip());
                translations.playerNames.noteWords(key.strip());
                translations.noteUnwrapped(key.strip(), flat);
                translations.looseMatcher.noteIndented(key.strip(), flat);
                translations.looseMatcher.noteMarked(key.strip(), flat);
                if (!asTerms) {
                    translations.looseMatcher.noteLoose(key.strip(), flat);
                }
                if (asTerms) {
                    translations.termIndex.noteTerm(key.strip(), flat, true);
                }
            }
        }
    }

    private void noteFlat(String key, String value) {
        if (key.length() < TranslationIndex.MIN_FLAT_LENGTH) {
            return;
        }
        layered(translations.flat, "flat", normalise(key), value);
    }

    private static String optString(JsonObject o, String field) {
        JsonElement el = o.get(field);
        return el != null && el.isJsonPrimitive() ? el.getAsString() : null;
    }
}
