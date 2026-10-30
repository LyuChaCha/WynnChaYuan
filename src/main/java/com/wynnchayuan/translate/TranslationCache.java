package com.wynnchayuan.translate;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynnchayuan.SafeFiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

public final class TranslationCache {

    public static final int FORMAT = 1;

    static final String STAMP = "_cache.json";

    static final String OLD = "translations.old";

    public static volatile String modVersion = "?";

    public static volatile boolean syncsFromGitHub = true;

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private TranslationCache() {}

    public static int prepare(Path langDir, String lang) {
        check(langDir);
        int refreshed = refreshAfterUpgrade(langDir, lang);
        return refreshed + StarterFiles.installIfEmpty(langDir, lang)
                + StarterFiles.installMissingScoped(langDir, lang);
    }

    static int refreshAfterUpgrade(Path langDir, String lang) {
        if (modVersion == null || modVersion.isBlank() || "?".equals(modVersion)) {
            return 0;
        }
        if (!syncsFromGitHub) {
            return 0;
        }
        Path stampFile = langDir.resolve(STAMP);
        JsonObject stamp = SafeFiles.readObject(stampFile, 1L << 20);
        if (stamp == null || !stamp.has("files") || !stamp.get("files").isJsonArray()) {
            return 0;
        }
        String mod = stamp.has("mod") && stamp.get("mod").isJsonPrimitive()
                ? stamp.get("mod").getAsString() : "";
        if (modVersion.equals(mod)) {
            return 0;
        }
        JsonObject hashes = stamp.has("hashes") && stamp.get("hashes").isJsonObject()
                ? stamp.getAsJsonObject("hashes") : null;
        JsonObject fresh = new JsonObject();
        int refreshed = 0;
        int kept = 0;
        for (JsonElement el : stamp.getAsJsonArray("files")) {
            if (!el.isJsonPrimitive()) {
                continue;
            }
            String name = el.getAsString();
            if (!name.endsWith(".json") || !FileIndex.safeName(name)) {
                continue;
            }
            Path file = langDir.resolve(name);
            String now = hashOf(file);
            String was = hashes != null && hashes.has(name) && hashes.get(name).isJsonPrimitive()
                    ? hashes.get(name).getAsString() : null;
            if (now != null && was != null && !now.equals(was)) {
                remember(fresh, name, was);
                kept++;
                continue;
            }
            if (StarterFiles.restore(langDir, lang, name)) {
                refreshed++;
                remember(fresh, name, hashOf(file));
            } else {
                remember(fresh, name, now);
            }
        }
        StarterFiles.restore(langDir, lang, "_index.json");
        stamp.addProperty("mod", modVersion);
        stamp.add("hashes", fresh);
        try {
            SafeFiles.writeAtomically(stampFile,
                    new GsonBuilder().setPrettyPrinting().create().toJson(stamp));
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 寫不出譯文快取記錄：" + e);
        }
        if (refreshed > 0) {
            System.out.println("[WynnChaYuan] 模組換了版本（" + mod + " → " + modVersion
                    + "），譯文快取 " + langDir.getFileName() + " 換回內建的 " + refreshed + " 個檔");
        }
        if (kept > 0) {
            System.out.println("[WynnChaYuan] 譯文快取 " + langDir.getFileName() + " 有 " + kept
                    + " 個檔跟上次同步下來的不一樣（有人改過），沒有動它們");
        }
        return refreshed;
    }

    private static void remember(JsonObject hashes, String name, String hash) {
        if (hash != null) {
            hashes.addProperty(name, hash);
        }
    }

    private static String hashOf(Path file) {
        try {
            if (!Files.isRegularFile(file)) {
                return null;
            }
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean check(Path langDir) {
        JsonObject stamp = SafeFiles.readObject(langDir.resolve(STAMP), 1L << 20);
        if (stamp == null) {
            return false;
        }
        int format = intOf(stamp.get("format"));
        if (format == FORMAT) {
            return false;
        }
        String mod = stamp.has("mod") && stamp.get("mod").isJsonPrimitive()
                ? stamp.get("mod").getAsString() : "?";
        return setAsideFolder(langDir, "快取格式 " + format + "（" + mod + " 寫的），這一版讀 " + FORMAT);
    }

    private static boolean setAsideFolder(Path langDir, String why) {
        try {
            Path configDir = langDir.toAbsolutePath().getParent().getParent();
            Path oldRoot = configDir.resolve(OLD);
            Files.createDirectories(oldRoot);
            String lang = langDir.getFileName().toString();
            Path target = oldRoot.resolve(lang + "-" + LocalDateTime.now().format(WHEN));
            for (int n = 2; Files.exists(target); n++) {
                target = oldRoot.resolve(lang + "-" + LocalDateTime.now().format(WHEN) + "-" + n);
            }
            Files.move(langDir, target);
            System.out.println("[WynnChaYuan] 譯文快取 " + lang + " 不能沿用（" + why
                    + "），已移到 " + OLD + "/" + target.getFileName() + "，改用內建譯文");
            pruneOld(oldRoot, lang, target);
            return true;
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 譯文快取 " + langDir.getFileName()
                    + " 不能沿用（" + why + "），也移不開：" + e);
            return false;
        }
    }

    private static void pruneOld(Path oldRoot, String lang, Path keep) {
        try (Stream<Path> dirs = Files.list(oldRoot)) {
            for (Path d : dirs.filter(p -> p.getFileName().toString().startsWith(lang + "-"))
                              .filter(p -> !p.equals(keep)).toList()) {
                deleteTree(d);
            }
        } catch (Exception ignored) {
        }
    }

    private static void deleteTree(Path root) throws Exception {
        try (Stream<Path> all = Files.walk(root)) {
            for (Path p : all.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }

    public static int record(Path langDir, Collection<String> fetched,
                             Collection<String> listed, boolean indexFresh) {
        Set<String> previous = new TreeSet<>();
        JsonObject old = SafeFiles.readObject(langDir.resolve(STAMP), 1L << 20);
        if (old != null && old.has("files") && old.get("files").isJsonArray()) {
            for (JsonElement el : old.getAsJsonArray("files")) {
                if (el.isJsonPrimitive()) {
                    previous.add(el.getAsString());
                }
            }
        }
        int removed = 0;
        if (indexFresh) {
            for (String name : previous) {
                if (listed.contains(name) || !name.endsWith(".json") || name.startsWith("_")
                        || !FileIndex.safeName(name)) {
                    continue;
                }
                try {
                    if (Files.deleteIfExists(langDir.resolve(name))) {
                        removed++;
                        System.out.println("[WynnChaYuan] 譯文檔 " + name + " 已不在清單上，刪掉快取裡的舊檔");
                    }
                } catch (Exception ignored) {
                }
            }
        }
        Set<String> tracked = new TreeSet<>(fetched);
        for (String name : previous) {
            if (listed.contains(name)) {
                tracked.add(name);
            }
        }
        JsonObject stamp = new JsonObject();
        stamp.addProperty("_note", "WynnChaYuan 的譯文快取記錄。format 對不上時整包會移到 "
                + OLD + "/；files 是從 GitHub 同步來的檔，不在這裡的檔（自己放的）不會被自動刪掉；"
                + "hashes 是同步下來那一刻的內容，之後對不上就表示有人自己改過，升版時不會被蓋掉。");
        stamp.addProperty("format", FORMAT);
        stamp.addProperty("mod", modVersion);
        JsonArray files = new JsonArray();
        tracked.forEach(files::add);
        stamp.add("files", files);
        JsonObject hashes = new JsonObject();
        for (String name : tracked) {
            remember(hashes, name, hashOf(langDir.resolve(name)));
        }
        stamp.add("hashes", hashes);
        try {
            SafeFiles.writeAtomically(langDir.resolve(STAMP),
                    new GsonBuilder().setPrettyPrinting().create().toJson(stamp));
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 寫不出譯文快取記錄：" + e);
        }
        return removed;
    }

    public static int loadRepairing(TranslationStore store, List<Path> layers) {
        store.loadAll(layers);
        List<Path> broken = store.brokenFiles();
        if (broken.isEmpty()) {
            return 0;
        }
        int restored = 0;
        for (Path file : broken) {
            Path layer = layerOf(file, layers);
            SafeFiles.setAside(file, "JSON 不完整", 1);
            if (layer != null) {
                String name = layer.relativize(file).toString().replace('\\', '/');
                if (StarterFiles.restore(layer, layer.getFileName().toString(), name)) {
                    restored++;
                }
            }
        }
        if (restored > 0) {
            System.out.println("[WynnChaYuan] 從內建譯文補回 " + restored + " 個讀不懂的檔，重新載入");
            store.loadAll(layers);
        }
        return broken.size();
    }

    private static Path layerOf(Path file, List<Path> layers) {
        Path abs = file.toAbsolutePath().normalize();
        for (Path layer : layers) {
            if (abs.startsWith(layer.toAbsolutePath().normalize())) {
                return layer;
            }
        }
        return null;
    }

    private static int intOf(JsonElement el) {
        try {
            return el != null && el.isJsonPrimitive() ? el.getAsInt() : -1;
        } catch (RuntimeException e) {
            return -1;
        }
    }
}
