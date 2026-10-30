package com.wynnchayuan;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import com.wynnchayuan.translate.ErrorDebug;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class SafeFiles {

    public static final String BROKEN = ".broken-";

    static final int KEEP_BROKEN = 3;

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private SafeFiles() {}

    public static void writeAtomically(Path file, String content) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        moveAtomically(tmp, file);
    }

    public static void moveAtomically(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static Path setAside(Path file, String why) {
        return setAside(file, why, KEEP_BROKEN);
    }

    public static Path setAside(Path file, String why, int keep) {
        try {
            if (!Files.exists(file)) {
                return null;
            }
            String base = file.getFileName() + BROKEN + LocalDateTime.now().format(STAMP);
            Path target = file.resolveSibling(base);
            for (int n = 2; Files.exists(target); n++) {
                target = file.resolveSibling(base + "-" + n);
            }
            Files.move(file, target);
            ModLog.warn(file.getFileName() + " 讀不懂（" + why
                    + "），已改名為 " + target.getFileName() + "，這次用預設值");
            prune(file, keep);
            return target;
        } catch (Exception e) {
            ModLog.warn(file.getFileName() + " 讀不懂（" + why
                    + "），也沒辦法改名：" + e);
            return null;
        }
    }

    private static void prune(Path file, int keep) {
        Path parent = file.toAbsolutePath().getParent();
        if (parent == null) {
            return;
        }
        String prefix = file.getFileName() + BROKEN;
        try (Stream<Path> siblings = Files.list(parent)) {
            List<Path> old = siblings
                    .filter(p -> p.getFileName().toString().startsWith(prefix))
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                    .toList();
            for (int i = Math.max(keep, 1); i < old.size(); i++) {
                Files.deleteIfExists(old.get(i));
            }
        } catch (Exception failure) {
            ErrorDebug.note("SafeFiles#prune", null, failure);
        }
    }

    public static JsonObject readObject(Path file, long maxBytes) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            if (maxBytes > 0 && Files.size(file) > maxBytes) {
                setAside(file, "檔案大到不合理");
                return null;
            }
        } catch (IOException e) {
            ModLog.warn("讀不到 " + file.getFileName() + "：" + e);
            return null;
        }
        JsonElement root;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(r);
        } catch (JsonIOException e) {
            if (e.getCause() instanceof CharacterCodingException) {
                setAside(file, "不是 UTF-8");
            } else {
                ModLog.warn("讀不到 " + file.getFileName() + "：" + e);
            }
            return null;
        } catch (JsonParseException | IllegalStateException e) {
            ErrorDebug.note("SafeFiles#readObject", null, e);
            setAside(file, "JSON 不完整");
            return null;
        } catch (IOException e) {
            if (e instanceof CharacterCodingException) {
                setAside(file, "不是 UTF-8");
            } else {
                ModLog.warn("讀不到 " + file.getFileName() + "：" + e);
            }
            return null;
        } catch (RuntimeException | StackOverflowError e) {
            setAside(file, e.getClass().getSimpleName());
            return null;
        }
        if (root == null || !root.isJsonObject()) {
            setAside(file, "不是 JSON 物件");
            return null;
        }
        return root.getAsJsonObject();
    }

    public static boolean parsesAsObject(Path file) {
        try (JsonReader in = new JsonReader(Files.newBufferedReader(file, StandardCharsets.UTF_8))) {
            if (in.peek() != JsonToken.BEGIN_OBJECT) {
                return false;
            }
            in.skipValue();
            return in.peek() == JsonToken.END_DOCUMENT;
        } catch (Exception | StackOverflowError e) {
            ErrorDebug.note("SafeFiles#parsesAsObject", null, e);
            return false;
        }
    }
}
