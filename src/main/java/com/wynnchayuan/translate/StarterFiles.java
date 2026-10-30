package com.wynnchayuan.translate;

import com.wynnchayuan.SafeFiles;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

public final class StarterFiles {

    private static final String INDEX = "_index.json";

    private static List<String> bundled(String lang) {
        List<String> names = new java.util.ArrayList<>(FileIndex.bundled(lang));
        names.addAll(FileIndex.SCOPED);
        names.add(INDEX);
        return names;
    }

    private StarterFiles() {}

    public static int installIfEmpty(Path dir) {
        return installIfEmpty(dir, Languages.DEFAULT);
    }

    public static int installIfEmpty(Path dir, String lang) {
        boolean resume = false;
        try {
            Files.createDirectories(dir);
            if (hasTranslations(dir)) {
                if (Files.isRegularFile(dir.resolve(INDEX))) {
                    return 0;
                }
                resume = true;
            }
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 無法準備譯文資料夾 " + dir + ": " + e.getMessage());
            return 0;
        }

        int written = 0;
        for (String name : bundled(lang)) {
            Path out = dir.resolve(name);
            if (resume && Files.exists(out)) {
                continue;
            }
            try (InputStream in = StarterFiles.class.getResourceAsStream(
                    Languages.resource(lang) + name)) {
                if (in == null) {
                    continue;
                }
                copyAtomically(in, out);
                written++;
            } catch (Exception e) {
                System.err.println("[WynnChaYuan] 寫出 " + name + " 失敗: " + e.getMessage());
            }
        }
        if (written > 0) {
            System.out.println("[WynnChaYuan] " + (resume ? "補上上次沒倒完的 " : "已放入 ")
                    + written + " 個翻譯工作檔於 " + dir);
        }
        return written;
    }

    public static int installMissingScoped(Path dir, String lang) {
        int written = 0;
        for (String name : FileIndex.SCOPED) {
            Path out = dir.resolve(name);
            if (Files.exists(out)) {
                continue;
            }
            if (restore(dir, lang, name)) {
                written++;
            }
        }
        if (written > 0) {
            System.out.println("[WynnChaYuan] 補上 " + written + " 個內建的 scoped 譯文檔");
        }
        return written;
    }

    public static boolean restore(Path dir, String lang, String name) {
        if (!FileIndex.safeName(name)) {
            return false;
        }
        try (InputStream in = StarterFiles.class.getResourceAsStream(
                Languages.resource(lang) + name)) {
            if (in == null) {
                return false;
            }
            copyAtomically(in, dir.resolve(name));
            return true;
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 補回 " + name + " 失敗: " + e.getMessage());
            return false;
        }
    }

    private static void copyAtomically(InputStream in, Path out) throws IOException {
        Files.createDirectories(out.toAbsolutePath().getParent());
        Path tmp = out.resolveSibling(out.getFileName() + ".tmp");
        Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        SafeFiles.moveAtomically(tmp, out);
    }

    private static boolean hasTranslations(Path dir) throws IOException {
        try (var files = Files.list(dir)) {
            return files.map(p -> p.getFileName().toString())
                        .anyMatch(n -> n.endsWith(".json") && !n.startsWith("_"));
        }
    }
}
