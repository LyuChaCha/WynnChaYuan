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
 * 自己加的譯文）、診斷檔、匯出檔一律不碰。
 */
public final class ConfigFolder {

    /**
     * 舊版寫過、這一版已經不讀的檔。
     *
     * <p>{@code shared.json} 是 0.1.9_5 以前的自動分享記錄（只有雜湊，沒有原文）。
     */
    static final List<String> OBSOLETE = List.of("shared.json");

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
        return removed;
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
