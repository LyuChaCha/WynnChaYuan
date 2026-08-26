package com.wynnchayuan.translate;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * 首次啟動時把內建的翻譯工作檔倒進 config 目錄。
 *
 * <h2>為什麼要內建</h2>
 * 沒有這一步，玩家裝好 mod 只會看到一個空資料夾和「資料夾裡沒有 .json 檔案」，
 * 完全不知道該放什麼、格式長什麼樣。內建之後裝好就有東西可以翻，
 * 也等於附上了格式範例。
 *
 * <p>檔案來自 {@code corpus/} 的離線語料（裝備、技能、材料…）加上一份手寫的
 * 介面標籤。{@code dst} 全部留空，等人填。
 *
 * <h2>只在資料夾是空的時候寫入</h2>
 * 已經有任何 .json 就完全不碰——否則每次啟動都會蓋掉玩家翻好的內容。
 * 想拿回原始檔就把資料夾清空再啟動一次。
 */
public final class StarterFiles {

    /** 清單來自 _index.json —— 新增譯文檔不必改這裡。 */
    private static List<String> bundled(String lang) {
        List<String> names = new java.util.ArrayList<>(FileIndex.bundled(lang));
        names.add("_index.json");              // 清單本身也要放出去，使用者才能自己加檔案
        return names;
    }

    private StarterFiles() {}

    /**
     * @return 實際寫出的檔案數；資料夾已有內容時回傳 0
     */
    public static int installIfEmpty(Path dir) {
        return installIfEmpty(dir, Languages.DEFAULT);
    }

    /**
     * @param lang 要倒出哪一種語言的工作檔
     */
    public static int installIfEmpty(Path dir, String lang) {
        try {
            Files.createDirectories(dir);
            if (hasJson(dir)) {
                return 0;                      // 已有內容，不覆蓋
            }
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 無法準備譯文資料夾 " + dir + ": " + e.getMessage());
            return 0;
        }

        int written = 0;
        for (String name : bundled(lang)) {
            try (InputStream in = StarterFiles.class.getResourceAsStream(
                    Languages.resource(lang) + name)) {
                if (in == null) {
                    continue;
                }
                Path out = dir.resolve(name);
                // 清單裡的名字可能帶資料夾（`ability/mage.json`），先把它建出來
                Files.createDirectories(out.getParent());
                Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                written++;
            } catch (Exception e) {
                System.err.println("[WynnChaYuan] 寫出 " + name + " 失敗: " + e.getMessage());
            }
        }
        if (written > 0) {
            System.out.println("[WynnChaYuan] 已放入 " + written + " 個翻譯工作檔於 " + dir);
        }
        return written;
    }

    private static boolean hasJson(Path dir) throws Exception {
        try (var files = Files.list(dir)) {
            return files.anyMatch(p -> p.getFileName().toString().endsWith(".json"));
        }
    }
}
