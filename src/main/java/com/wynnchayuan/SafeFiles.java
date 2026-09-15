package com.wynnchayuan;

import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

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

/**
 * {@code config/wynnchayuan} 底下的檔案怎麼讀、怎麼寫才不會害遊戲開不起來。
 *
 * <h2>為什麼要集中在這裡</h2>
 * 玩家回報：測過舊版、升上新版<b>沒刪</b> {@code config/wynnchayuan}，遊戲偶爾開不起來，
 * 刪掉資料夾就好了。這個資料夾裡的東西會跨版本留下來——舊版寫的設定、玩到一半被關掉
 * 而只寫了半截的 JSON、別的版本留下的快取。每一支讀檔的程式各自處理一次的話，
 * 總有一支漏掉。
 *
 * <p>規則只有三條：
 * <ol>
 *   <li><b>寫</b>一律先寫暫存檔再搬過去。遊戲被強制關掉時，舊檔要嘛完整、要嘛是新的，
 *       不會是一半。</li>
 *   <li><b>讀</b>不懂就把那個檔改名放旁邊（{@code *.broken-時間}），用預設值繼續。
 *       不刪：那裡面可能有玩家自己打的字，要留給人看。也不原地留著：留著的話
 *       每次啟動都再撞一次，而下一次存檔會把它蓋掉，連回報的線索都沒了。</li>
 *   <li>讀不到檔（被防毒軟體鎖住之類）跟<b>內容壞了</b>是兩回事。前者只記一筆，
 *       不動檔案——下次開遊戲它可能就好了。</li>
 * </ol>
 */
public final class SafeFiles {

    /** 改名放旁邊的檔名記號。結尾不是 {@code .json}，所以不會再被當成譯文或設定讀進來。 */
    public static final String BROKEN = ".broken-";

    /** 同一個檔最多留幾份壞掉的舊檔。夠回報用就好，不要讓資料夾越長越大。 */
    static final int KEEP_BROKEN = 3;

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private SafeFiles() {}

    /** 先寫 {@code 檔名.tmp}，寫完再一次換上去。 */
    public static void writeAtomically(Path file, String content) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, content, StandardCharsets.UTF_8);
        moveAtomically(tmp, file);
    }

    /**
     * 搬過去蓋掉舊的。
     *
     * <p>檔案系統不支援原子搬移（網路磁碟之類）就退回一般的取代——那仍然比直接
     * 開檔覆寫好：覆寫是先把舊檔清成零位元組再慢慢寫。
     */
    public static void moveAtomically(Path from, Path to) throws IOException {
        try {
            Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** 見 {@link #setAside(Path, String, int)}，保留 {@value #KEEP_BROKEN} 份。 */
    public static Path setAside(Path file, String why) {
        return setAside(file, why, KEEP_BROKEN);
    }

    /**
     * 把讀不懂的檔改名放旁邊。
     *
     * @param keep 同一個檔最多留幾份壞掉的舊檔；譯文快取動輒幾 MB，留一份就夠
     * @return 改名後的位置；檔案不存在或改名失敗時回傳 {@code null}
     */
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
            System.err.println("[WynnChaYuan] " + file.getFileName() + " 讀不懂（" + why
                    + "），已改名為 " + target.getFileName() + "，這次用預設值");
            prune(file, keep);
            return target;
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] " + file.getFileName() + " 讀不懂（" + why
                    + "），也沒辦法改名：" + e);
            return null;
        }
    }

    /** 同一個檔的壞檔只留最新的幾份。時間戳記照字典序排就是新舊順序。 */
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
        } catch (Exception ignored) {
            // 清不掉就留著，不影響功能
        }
    }

    /**
     * 讀一個應該是 JSON 物件的檔。
     *
     * <p>內容壞了（半截、空檔、不是物件、編碼不對、大到不合理）就改名放旁邊並回傳
     * {@code null}，呼叫端照「沒有這個檔」處理。讀檔本身失敗則只記一筆、不動檔案。
     *
     * @param maxBytes 超過就當成壞檔；{@code 0} 表示不設上限
     * @return 讀到的物件；檔案不存在或讀不懂時回傳 {@code null}
     */
    public static JsonObject readObject(Path file, long maxBytes) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            if (maxBytes > 0 && Files.size(file) > maxBytes) {
                // 這種大小不是這個模組寫得出來的東西。硬讀會在主執行緒上卡很久，
                // 甚至把記憶體吃光——而那正是「遊戲開不起來」。
                setAside(file, "檔案大到不合理");
                return null;
            }
        } catch (IOException e) {
            System.err.println("[WynnChaYuan] 讀不到 " + file.getFileName() + "：" + e);
            return null;
        }
        JsonElement root;
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(r);
        } catch (JsonIOException e) {
            if (e.getCause() instanceof CharacterCodingException) {
                // 用記事本以 ANSI／Big5 另存過的檔。內容已經不是 UTF-8 了。
                setAside(file, "不是 UTF-8");
            } else {
                System.err.println("[WynnChaYuan] 讀不到 " + file.getFileName() + "：" + e);
            }
            return null;
        } catch (JsonParseException | IllegalStateException e) {
            setAside(file, "JSON 不完整");
            return null;
        } catch (IOException e) {
            if (e instanceof CharacterCodingException) {
                setAside(file, "不是 UTF-8");
            } else {
                System.err.println("[WynnChaYuan] 讀不到 " + file.getFileName() + "：" + e);
            }
            return null;
        } catch (RuntimeException | StackOverflowError e) {
            setAside(file, e.getClass().getSimpleName());
            return null;
        }
        if (root == null || !root.isJsonObject()) {
            // 空檔也走這裡：JsonParser 讀到空內容回傳的是 JsonNull，不會丟例外
            setAside(file, "不是 JSON 物件");
            return null;
        }
        return root.getAsJsonObject();
    }

    /**
     * 這個檔從頭到尾是不是一個完整的 JSON 物件。不動檔案。
     *
     * <p>給下載用：先前只看「開頭是不是 {@code {}」，連線中途斷掉的半截檔照樣
     * 通過，然後蓋掉上一份好的快取。用串流跳過整份內容，不必把幾 MB 的樹建出來。
     */
    public static boolean parsesAsObject(Path file) {
        try (JsonReader in = new JsonReader(Files.newBufferedReader(file, StandardCharsets.UTF_8))) {
            if (in.peek() != JsonToken.BEGIN_OBJECT) {
                return false;
            }
            in.skipValue();
            return in.peek() == JsonToken.END_DOCUMENT;
        } catch (Exception | StackOverflowError e) {
            return false;
        }
    }
}
