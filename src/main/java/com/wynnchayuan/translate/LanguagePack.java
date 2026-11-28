package com.wynnchayuan.translate;

import com.wynnchayuan.ModLog;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public final class LanguagePack {

    public enum Phase { READY, DOWNLOADING, FAILED }

    public record Status(Phase phase, int done, int total) {
        public int percent() {
            return total <= 0 ? 0 : Math.min(100, done * 100 / total);
        }
    }

    @FunctionalInterface
    public interface Fetcher {
        boolean fetch(Path dir, String lang, RemoteSync.Progress progress);
    }

    static final String PART = "translations.part";

    private static final long USABLE_TTL_MS = 3000L;

    private static final Map<String, Status> STATUS = new ConcurrentHashMap<>();

    private static final Set<String> RUNNING = ConcurrentHashMap.newKeySet();

    private record Seen(boolean usable, long at) {}

    private static final Map<Path, Seen> USABLE = new ConcurrentHashMap<>();

    private LanguagePack() {}

    public static boolean needsDownload(Path configDir, String lang) {
        if (lang == null || Languages.inJar(lang)) {
            return false;
        }
        return configDir == null || !usable(Languages.dir(configDir, lang));
    }

    public static boolean loadable(Path configDir, String lang) {
        return !needsDownload(configDir, lang);
    }

    public static List<Path> layers(Path configDir, String lang, String fallback) {
        List<Path> out = new ArrayList<>();
        if (fallback != null && loadable(configDir, fallback)) {
            out.add(Languages.dir(configDir, fallback));
        }
        if (loadable(configDir, lang)) {
            out.add(Languages.dir(configDir, lang));
        }
        return out;
    }

    public static boolean usable(Path dir) {
        long now = System.currentTimeMillis();
        Seen seen = USABLE.get(dir);
        if (seen != null && now - seen.at() < USABLE_TTL_MS) {
            return seen.usable();
        }
        boolean found = scan(dir);
        USABLE.put(dir, new Seen(found, now));
        return found;
    }

    private static boolean scan(Path dir) {
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(p -> p.getFileName().toString())
                        .anyMatch(n -> n.endsWith(".json") && !n.startsWith("_"));
        } catch (Exception e) {
            ErrorDebug.note("LanguagePack#scan", null, e);
            return false;
        }
    }

    public static Status status(String lang) {
        return lang == null ? null : STATUS.get(lang);
    }

    public static boolean running(String lang) {
        return lang != null && RUNNING.contains(lang);
    }

    public static Phase download(Path configDir, String lang, Fetcher fetcher,
                                 RemoteSync.Progress progress) {
        if (!needsDownload(configDir, lang)) {
            return Phase.READY;
        }
        if (!RUNNING.add(lang)) {
            return Phase.DOWNLOADING;
        }
        Path target = Languages.dir(configDir, lang);
        Path part = partDir(configDir, lang);
        try {
            STATUS.put(lang, new Status(Phase.DOWNLOADING, 0, 0));
            deleteTree(part);
            boolean complete = fetcher.fetch(part, lang, (done, total) -> {
                STATUS.put(lang, new Status(Phase.DOWNLOADING, done, total));
                if (progress != null) {
                    progress.at(done, total);
                }
            });
            if (!complete || !scan(part) || !Files.isRegularFile(part.resolve("_index.json"))) {
                deleteTree(part);
                STATUS.put(lang, new Status(Phase.FAILED, 0, 0));
                ModLog.info(lang + " 沒有下載完整，不留半套快取");
                return Phase.FAILED;
            }
            clear(configDir, target);
            Files.createDirectories(target.getParent());
            move(part, target);
            USABLE.remove(target);
            STATUS.put(lang, new Status(Phase.READY, 1, 1));
            ModLog.info(lang + " 下載完成，放進譯文快取");
            return Phase.READY;
        } catch (Exception e) {
            ErrorDebug.note("LanguagePack#download", null, e);
            try {
                deleteTree(part);
            } catch (Exception failure) {
                ErrorDebug.note("LanguagePack#download", null, failure);
            }
            STATUS.put(lang, new Status(Phase.FAILED, 0, 0));
            return Phase.FAILED;
        } finally {
            RUNNING.remove(lang);
        }
    }

    private static void move(Path from, Path to) throws Exception {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to);
        }
    }

    static Path partDir(Path configDir, String lang) {
        return configDir.resolve(PART).resolve(Languages.dir(configDir, lang).getFileName());
    }

    private static void clear(Path configDir, Path target) throws Exception {
        if (!Files.exists(target)) {
            return;
        }
        boolean holdsFiles;
        try (Stream<Path> all = Files.walk(target)) {
            holdsFiles = all.anyMatch(Files::isRegularFile);
        }
        if (!holdsFiles) {
            deleteTree(target);
            return;
        }
        Path aside = configDir.resolve(TranslationCache.OLD).resolve(target.getFileName() + "-before-download");
        deleteTree(aside);
        Files.createDirectories(aside.getParent());
        move(target, aside);
        ModLog.info(target.getFileName() + " 裡原本的檔案移到 " + TranslationCache.OLD + "/" + aside.getFileName());
    }

    private static void deleteTree(Path root) throws Exception {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> all = Files.walk(root)) {
            for (Path p : all.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }

    static void forget() {
        STATUS.clear();
        USABLE.clear();
    }
}
