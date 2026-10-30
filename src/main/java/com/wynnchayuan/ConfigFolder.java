package com.wynnchayuan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class ConfigFolder {

    static final List<String> OBSOLETE = List.of("shared.json");

    public static final String DEBUG = "debug";

    static final java.util.regex.Pattern OLD_DEBUG = java.util.regex.Pattern.compile(
            "(?:actionbar-(?:columns|probe)-\\d+\\.txt"
                    + "|dialogue-(?:probe|miss|choice-probe|tofu)-\\d+\\.txt"
                    + "|font-dump\\.txt"
                    + "|(?:layout|line|majorid|shot)-debug\\.txt"
                    + "|overlay-debug(?:-\\d+)?\\.txt"
                    + "|tooltip-debug(?:-\\d+)?\\.json"
                    + "|tooltip-partial-\\d+\\.json"
                    + "|[a-z]+-tooltip-\\d+\\.json)");

    static final long DEBUG_KEEP_MS = 14L * 24 * 3600 * 1000;

    public static Path debugDir(Path dir) {
        Path debug = dir.resolve(DEBUG);
        try {
            Files.createDirectories(debug);
            return debug;
        } catch (Exception e) {
            return dir;
        }
    }

    private static final int TMP_DEPTH = 4;

    private ConfigFolder() {}

    public static int tidy(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return 0;
        }
        int removed = 0;
        for (String name : OBSOLETE) {
            removed += delete(dir.resolve(name));
        }
        try (Stream<Path> top = Files.list(dir)) {
            for (Path p : top.filter(ConfigFolder::isTmp).toList()) {
                removed += delete(p);
            }
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 整理設定資料夾失敗：" + e);
        }
        Path translations = dir.resolve("translations");
        if (Files.isDirectory(translations)) {
            try (Stream<Path> all = Files.walk(translations, TMP_DEPTH)) {
                for (Path p : all.filter(ConfigFolder::isTmp).toList()) {
                    removed += delete(p);
                }
            } catch (Exception e) {
                System.err.println("[WynnChaYuan] 整理譯文快取失敗：" + e);
            }
        }
        if (removed > 0) {
            System.out.println("[WynnChaYuan] 清掉舊版留下的 " + removed + " 個檔（暫存檔與不再使用的記錄）");
        }
        int moved = moveOldDebug(dir);
        if (moved > 0) {
            System.out.println("[WynnChaYuan] 把 " + moved + " 個診斷檔搬進 " + DEBUG + "/");
        }
        int stale = pruneDebug(dir, System.currentTimeMillis());
        if (stale > 0) {
            System.out.println("[WynnChaYuan] 刪掉 " + stale + " 個超過兩週沒更新的診斷檔");
        }
        return removed;
    }

    static int pruneDebug(Path dir, long now) {
        Path debug = dir.resolve(DEBUG);
        if (!Files.isDirectory(debug)) {
            return 0;
        }
        int removed = 0;
        try (Stream<Path> all = Files.list(debug)) {
            for (Path p : all.filter(Files::isRegularFile).toList()) {
                try {
                    if (now - Files.getLastModifiedTime(p).toMillis() > DEBUG_KEEP_MS) {
                        removed += delete(p);
                    }
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 清診斷檔失敗：" + e);
        }
        return removed;
    }

    static int moveOldDebug(Path dir) {
        int moved = 0;
        try (Stream<Path> top = Files.list(dir)) {
            List<Path> old = top.filter(Files::isRegularFile)
                    .filter(p -> OLD_DEBUG.matcher(p.getFileName().toString()).matches())
                    .toList();
            if (old.isEmpty()) {
                return 0;
            }
            Path debug = Files.createDirectories(dir.resolve(DEBUG));
            for (Path p : old) {
                Path to = debug.resolve(p.getFileName().toString());
                if (Files.exists(to)) {
                    continue;
                }
                Files.move(p, to);
                moved++;
            }
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 搬診斷檔失敗：" + e);
        }
        return moved;
    }

    private static boolean isTmp(Path p) {
        return p.getFileName().toString().endsWith(".tmp") && Files.isRegularFile(p);
    }

    private static int delete(Path p) {
        try {
            return Files.deleteIfExists(p) ? 1 : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
