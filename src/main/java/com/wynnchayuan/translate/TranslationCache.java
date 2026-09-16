package com.wynnchayuan.translate;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.wynnchayuan.SafeFiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * 譯文快取（{@code translations/<語言>/}）跨版本留下來時要注意的事。
 *
 * <h2>為什麼要有</h2>
 * 這個資料夾同時是 GitHub 同步的快取、jar 內建工作檔倒出來的地方，也是譯者自己改檔的
 * 地方。升級時它整包留著，於是會遇到三種情況：
 *
 * <ol>
 *   <li><b>檔案只寫了一半</b>——遊戲在第一次倒工作檔、或下載到一半時被關掉。
 *       先前的做法是「讀不懂就略過」，那個檔的譯文就一直不見，而且看不出原因。
 *       現在改名放旁邊、從 jar 補一份回來，見 {@link #loadRepairing}。</li>
 *   <li><b>別的版本寫的快取</b>——格式哪天變了，舊快取不能硬讀。同步時蓋一個
 *       {@value #STAMP}，記下格式版本；對不上就整包移到 {@code translations.old/}，
 *       重新從 jar 倒一份，見 {@link #check}。</li>
 *   <li><b>已經不存在的檔</b>——repo 上刪掉或改名的譯文檔，快取裡的舊檔還在，而
 *       載入端會把「清單沒提到的檔」排在最後讀（那是給使用者自己丟檔用的），
 *       於是舊譯文<b>蓋掉</b>新的。戳記記得哪些是同步抓下來的，清單不再列就刪，
 *       見 {@link #record}。使用者自己放的檔從來不在戳記裡，不會被刪。</li>
 * </ol>
 */
public final class TranslationCache {

    /**
     * 快取格式版本。改了譯文檔的讀法、舊快取不能直接沿用時才加一。
     *
     * <p>0.1.9_6 以前沒有戳記；那些快取的格式跟 1 相同，所以缺戳記一律照用。
     */
    public static final int FORMAT = 1;

    /** 戳記檔名。底線開頭，載入端與清單都會跳過它。 */
    static final String STAMP = "_cache.json";

    /** 移開的舊快取放在 {@code config/wynnchayuan/} 底下的這個資料夾。 */
    static final String OLD = "translations.old";

    /** 寫進戳記的模組版本，純粹給回報時看是哪一版寫的。由 {@code WynnChaYuan} 啟動時填入。 */
    public static volatile String modVersion = "?";

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private TranslationCache() {}

    /**
     * 啟動時對一個語言資料夾做的事：格式對不上就移開，缺檔就從 jar 補。
     *
     * @return 從 jar 倒出幾個檔
     */
    public static int prepare(Path langDir, String lang) {
        check(langDir);
        return StarterFiles.installIfEmpty(langDir, lang);
    }

    /**
     * 快取是不是別的格式寫的；是的話整包移開。
     *
     * <p>移開而不是刪掉：切到「只用本機檔案」的譯者可能在裡面改過字。
     *
     * @return 有沒有移開
     */
    public static boolean check(Path langDir) {
        JsonObject stamp = SafeFiles.readObject(langDir.resolve(STAMP), 1L << 20);
        if (stamp == null) {
            return false;                      // 沒有戳記（舊版）或戳記壞了：格式相同，照用
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

    /** 同一種語言的舊快取只留剛移過去的那一份——一份就好幾 MB。 */
    private static void pruneOld(Path oldRoot, String lang, Path keep) {
        try (Stream<Path> dirs = Files.list(oldRoot)) {
            for (Path d : dirs.filter(p -> p.getFileName().toString().startsWith(lang + "-"))
                              .filter(p -> !p.equals(keep)).toList()) {
                deleteTree(d);
            }
        } catch (Exception ignored) {
            // 清不掉就留著
        }
    }

    private static void deleteTree(Path root) throws Exception {
        try (Stream<Path> all = Files.walk(root)) {
            for (Path p : all.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }

    /**
     * 同步完蓋戳記，順便刪掉清單已經不列的舊檔。
     *
     * @param fetched    這次成功抓下來的檔
     * @param listed     這次想抓的完整清單（內建清單加上剛抓到的遠端清單）
     * @param indexFresh 遠端清單這次有沒有抓到。沒抓到時清單只是內建那份，
     *                   拿它判斷「repo 上已經刪掉了」會誤刪，所以不刪
     * @return 刪掉幾個舊檔
     */
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
                    // 刪不掉就下次再試
                }
            }
        }
        Set<String> tracked = new TreeSet<>(fetched);
        for (String name : previous) {
            if (listed.contains(name)) {
                tracked.add(name);             // 這次沒抓成功，但仍是同步來的檔
            }
        }
        JsonObject stamp = new JsonObject();
        stamp.addProperty("_note", "WynnChaYuan 的譯文快取記錄。format 對不上時整包會移到 "
                + OLD + "/；files 是從 GitHub 同步來的檔，不在這裡的檔（自己放的）不會被自動刪掉。");
        stamp.addProperty("format", FORMAT);
        stamp.addProperty("mod", modVersion);
        JsonArray files = new JsonArray();
        tracked.forEach(files::add);
        stamp.add("files", files);
        try {
            SafeFiles.writeAtomically(langDir.resolve(STAMP),
                    new GsonBuilder().setPrettyPrinting().create().toJson(stamp));
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 寫不出譯文快取記錄：" + e);
        }
        return removed;
    }

    /**
     * 載入譯文；讀不懂的檔改名放旁邊，從 jar 補一份，再載一次。
     *
     * <p>只有真的補回了東西才重載——一次載入要一兩秒，不該為了沒變的結果再花一次。
     * GitHub 同步隨後會把補回的內建版換成最新的。
     *
     * @return 讀不懂的檔有幾個
     */
    public static int loadRepairing(TranslationStore store, List<Path> layers) {
        store.loadAll(layers);
        List<Path> broken = store.brokenFiles();
        if (broken.isEmpty()) {
            return 0;
        }
        int restored = 0;
        for (Path file : broken) {
            Path layer = layerOf(file, layers);
            // 快取檔動輒幾 MB，壞檔留一份給人看就夠
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
