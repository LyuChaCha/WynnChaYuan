package com.wynnchayuan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * 啟動時先把 {@code config/wynnchayuan} 整理一次，再讓任何人讀它。
 *
 * <h2>為什麼要有</h2>
 * 玩家不會在升級時刪這個資料夾，也不該需要刪。但裡面會留下兩種東西：
 *
 * <ul>
 *   <li><b>舊版才用的檔</b>。0.1.9_6 拿掉了自動分享語料，{@code shared.json}
 *       （已分享過的雜湊清單）從此沒有人讀，卻會一直躺在那裡。</li>
 *   <li><b>寫到一半的暫存檔</b>。設定、{@code captured.json}、譯文快取都是先寫
 *       {@code *.tmp} 再換上去；遊戲在那一瞬間被關掉，{@code .tmp} 就留下來了。
 *       它不會被讀，但會讓人以為那是資料，而且譯文快取的暫存檔一個就好幾 MB。</li>
 * </ul>
 *
 * <p>只刪<b>這兩種</b>。玩家自己放的檔（{@code third-party-literals.json}、
 * 自己加的譯文）、診斷檔、匯出檔一律不碰。舊版寫在外面的診斷檔只是搬進
 * {@code debug/}，不刪。
 */
public final class ConfigFolder {

    /**
     * 舊版寫過、這一版已經不讀的檔。
     *
     * <p>{@code shared.json} 是 0.1.9_5 以前的自動分享記錄（只有雜湊，沒有原文）。
     */
    static final List<String> OBSOLETE = List.of("shared.json");

    /** 診斷檔的子資料夾。 */
    public static final String DEBUG = "debug";

    /**
     * 舊版直接寫在外面的診斷檔。啟動時搬進 {@link #DEBUG}，不刪——回報問題的人
     * 可能正要附上它們。
     */
    static final java.util.regex.Pattern OLD_DEBUG = java.util.regex.Pattern.compile(
            "(?:actionbar-(?:columns|probe)-\\d+\\.txt"
                    + "|dialogue-(?:probe|miss|choice-probe|tofu)-\\d+\\.txt"
                    + "|font-dump\\.txt"
                    + "|(?:layout|line|majorid|shot)-debug\\.txt"
                    + "|overlay-debug(?:-\\d+)?\\.txt"
                    + "|tooltip-debug(?:-\\d+)?\\.json"
                    + "|tooltip-partial-\\d+\\.json"
                    + "|[a-z]+-tooltip-\\d+\\.json)");

    /**
     * debug/ 裡多久沒更新就刪。診斷檔是拿來回報最近的問題的，兩週前的已經沒有人會看；
     * 不清的話每查一次 bug 就多一批，資料夾越長越大（2026-10-15 使用者的資料夾裡有五十個）。
     */
    static final long DEBUG_KEEP_MS = 14L * 24 * 3600 * 1000;

    /** 診斷檔的資料夾，沒有就建。建不起來回傳外層，至少檔案還寫得出去。 */
    public static Path debugDir(Path dir) {
        Path debug = dir.resolve(DEBUG);
        try {
            Files.createDirectories(debug);
            return debug;
        } catch (Exception e) {
            return dir;
        }
    }

    /** 譯文快取往下找暫存檔最多幾層：{@code translations/<語言>/ability/x.json.tmp}。 */
    private static final int TMP_DEPTH = 4;

    private ConfigFolder() {}

    /**
     * @return 刪掉幾個檔
     */
    public static int tidy(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return 0;                          // 第一次安裝，沒有東西可以整理
        }
        int removed = 0;
        for (String name : OBSOLETE) {
            removed += delete(dir.resolve(name));
        }
        // 暫存檔。這時候還沒有任何執行緒在寫檔——同步是這一步之後才開始的。
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

    /** debug/ 裡超過 {@link #DEBUG_KEEP_MS} 沒更新的檔刪掉。只看這一層，不往下找。 */
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
                    // 讀不到時間就留著
                }
            }
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 清診斷檔失敗：" + e);
        }
        return removed;
    }

    /** 舊版寫在外面的診斷檔搬進 debug/；同名的新檔已經在裡面就留著舊的不動。 */
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
            return 0;                          // 刪不掉就留著，不影響啟動
        }
    }
}
