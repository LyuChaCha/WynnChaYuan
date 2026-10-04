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
 *   <li><b>譯者自己改過的檔</b>——同步來的檔之後被人改了字。只看檔名分不出來，
 *       升一次版就整份蓋掉。戳記連同步那一刻的內容雜湊一起記，對不上就不碰，
 *       見 {@link #refreshAfterUpgrade}。</li>
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

    /**
     * 譯文是不是從 GitHub 同步來的（設定的 {@code source}）。由 {@code WynnChaYuan} 啟動時填入。
     *
     * <p>切成「只用本機檔案」的人，那個資料夾就是他自己的工作區，升版不該去動它——
     * {@link #refreshAfterUpgrade} 整支會跳過。見 #979。
     */
    public static volatile boolean syncsFromGitHub = true;

    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private TranslationCache() {}

    /**
     * 啟動時對一個語言資料夾做的事：格式對不上就移開，缺檔就從 jar 補。
     *
     * @return 從 jar 倒出幾個檔
     */
    public static int prepare(Path langDir, String lang) {
        check(langDir);
        int refreshed = refreshAfterUpgrade(langDir, lang);
        // scoped 的檔不在清單裡，升級上來的快取永遠等不到它們。見 StarterFiles#installMissingScoped
        return refreshed + StarterFiles.installIfEmpty(langDir, lang)
                + StarterFiles.installMissingScoped(langDir, lang);
    }

    /**
     * 換了模組版本：同步下來的檔先換成<b>這一版 jar 內建</b>的那一份。
     *
     * <h2>實機回報</h2>
     * 裝上 0.2.0_4 之後，右下角還是「贏得地城」。快取裡是 0.2.0_3 時從 GitHub
     * 抓的舊檔，而載入時快取蓋在 jar 內建的上面——新版 jar 裡的新譯文完全沒被讀到，
     * 要等背景同步整輪跑完、重載之後才會出現；同步慢或中途關遊戲就一直是舊的。
     *
     * <p>只換戳記裡記著「同步來的」那些檔：使用者自己放進資料夾的檔從來不在戳記裡，
     * 不會被蓋掉。換完把戳記的版本改成這一版，下次啟動就不再重做。
     * 背景同步照常跑，GitHub 上若比 jar 更新，隨後會再換上去。
     *
     * <h2>自己改過的檔不能換（#979）</h2>
     * 「同步來的檔」裡也包含譯者<b>後來自己改過</b>的那幾個——快取資料夾正是他們改檔
     * 的地方。只看檔名的話這兩種分不出來，於是升一次版就把人家改的字整份蓋掉，
     * 而且 F6 的「自動更新翻譯」關著也一樣會發生（那個開關管的是要不要去 GitHub 抓，
     * 這裡換的是 jar 內建的那一份，走的是另一條路，連聊天室都不會說一聲）。
     *
     * <p>兩道防線：
     * <ol>
     *   <li>設定切到「只用本機檔案」的人整支跳過（{@link #syncsFromGitHub}）。
     *       他沒有在同步，戳記裡的 {@code files} 只是以前留下的，拿它來蓋等於無故刪人家的稿。</li>
     *   <li>還在同步的人，戳記除了檔名也記每個檔的雜湊（{@code hashes}）：
     *       跟上次同步下來的那一份一模一樣才換，不一樣就是有人動過，留著不碰。</li>
     * </ol>
     *
     * <p>0.2.7 以前的戳記沒有雜湊，那一次分不出來，照舊行為換掉——否則上面那個
     * 0.2.0_4 的回報會原封不動地回來。換完就有雜湊了，之後都判得出來。
     *
     * @return 換了幾個檔
     */
    static int refreshAfterUpgrade(Path langDir, String lang) {
        if (modVersion == null || modVersion.isBlank() || "?".equals(modVersion)) {
            return 0;
        }
        if (!syncsFromGitHub) {
            return 0;                          // 只用本機檔案的人，資料夾是他的，不碰。見 #979
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
            // 檔不在了（now 為 null）就補一份回來；在的話要跟上次寫下的對得上才換。
            // was 為 null 是 0.2.7 以前的戳記，分不出來，照舊行為換掉（見 #979 的備註）
            if (now != null && was != null && !now.equals(was)) {
                // 記著的仍然是「上次同步下來的那一份」，不是他改成的樣子——
                // 寫成現在這樣的話，下一次升級就會把它判成「沒動過」而蓋掉
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

    /**
     * 檔案內容的雜湊，用來判斷「這個檔還是我們上次寫下的那一份嗎」。
     *
     * <p>讀不到、或根本不是個檔，回 {@code null}——呼叫端把那當成「不在了」，該補一份回來。
     */
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
