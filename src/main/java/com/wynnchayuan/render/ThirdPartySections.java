package com.wynnchayuan.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.wynnchayuan.capture.GlyphSplitter;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 濾掉別的模組加進 tooltip 的區塊。
 *
 * <h2>問題長什麼樣</h2>
 * 有玩家回報：原本的 tooltip 沒有 Nori／Wynnpool 的評分，我們的翻譯面板卻有，
 * 兩邊行數對不上，看起來像是我們憑空多生了內容。
 *
 * <p>原因是我們讀的是 {@code ItemTooltipRenderEvent.Pre} 的行列表。這些模組
 * 會把自己的區塊<b>加進那份列表</b>，但 Wynntils 真正畫出來的 tooltip 是它自己
 * 重建的，不含那些行。我們忠實地鏡像了「列表」，而玩家看到的是「畫面」。
 *
 * <h2>為什麼是資料檔而不是寫死</h2>
 * Wynncraft 的第三方工具一直在增加。寫死在 Java 裡代表每冒出一個新的模組
 * 就要重新發版；放成資料檔的話，使用者自己在 config 裡加一行就解決了。
 * 反過來，想看這些區塊的譯文就把檔案清空。
 *
 * <p>比對的是<b>整行的可讀文字</b>要完全等於標籤名（忽略大小寫）。用「包含」
 * 會誤傷——例如某件裝備的敘述剛好提到那個字。
 */
public final class ThirdPartySections {

    private static final String RESOURCE = "/assets/wynnchayuan/third-party-sections.json";

    /** 單一區塊最多幾行。純粹是止血用的上限，見 {@link #strip}。 */
    private static final int MAX_BLOCK = 12;

    private static Set<String> badges = Set.of();

    private ThirdPartySections() {}

    /** 從 jar 讀內建清單，使用者 config 目錄裡有同名檔案就以那份為準。 */
    public static void load(Path configDir) {
        Set<String> found = new LinkedHashSet<>();
        Path local = configDir.resolve("third-party-sections.json");
        if (Files.isRegularFile(local)) {
            read(() -> Files.newInputStream(local), found);
        }
        if (found.isEmpty()) {
            read(() -> ThirdPartySections.class.getResourceAsStream(RESOURCE), found);
        }
        badges = Set.copyOf(found);
    }

    /**
     * 拿掉第三方區塊。
     *
     * <p>一個區塊的範圍是：標籤那一行，加上後面所有行，直到<b>沒有可讀文字</b>
     * 的那一行為止（含）——那種行是分隔線，也就是區塊的結尾。中途又碰到另一個
     * 標籤就繼續往下吃，因為兩個模組的區塊常常黏在一起。
     *
     * <p>區塊前面若剛好也是一條分隔線，一併拿掉，免得留下一條沒有內容的孤線。
     *
     * <p>最多吃 {@value #MAX_BLOCK} 行就停。萬一某個模組的區塊沒有結尾分隔線，
     * 沒有上限的話會把後面整段真正的裝備資訊一起刪掉——寧可多留幾行對不齊，
     * 也不要無聲地吃掉玩家要看的東西。
     */
    public static List<Component> strip(List<Component> lines) {
        if (badges.isEmpty() || lines == null || lines.isEmpty()) {
            return lines;
        }
        boolean[] drop = new boolean[lines.size()];
        boolean any = false;

        for (int i = 0; i < lines.size(); i++) {
            if (drop[i] || !isBadge(lines.get(i))) {
                continue;
            }
            any = true;
            if (i > 0 && !hasReadable(lines.get(i - 1))) {
                drop[i - 1] = true;             // 區塊前的分隔線
            }
            int limit = Math.min(lines.size(), i + MAX_BLOCK);
            for (int j = i; j < limit; j++) {
                drop[j] = true;
                if (!isBadge(lines.get(j)) && !hasReadable(lines.get(j))) {
                    break;                      // 分隔線，區塊到此為止
                }
            }
        }
        if (!any) {
            return lines;
        }
        List<Component> out = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            if (!drop[i]) {
                out.add(lines.get(i));
            }
        }
        return out;
    }

    private static boolean isBadge(Component line) {
        return badges.contains(readable(line).toLowerCase());
    }

    /** 去掉圖示與空白之後剩下的字。 */
    private static String readable(Component line) {
        StringBuilder sb = new StringBuilder();
        line.getString().codePoints().forEach(cp -> {
            if (!GlyphSplitter.isGlyphCodePoint(cp)) {
                sb.appendCodePoint(cp);
            }
        });
        return sb.toString().strip();
    }

    private static boolean hasReadable(Component line) {
        return !readable(line).isEmpty();
    }

    private static void read(StreamSource source, Set<String> into) {
        try (InputStream in = source.open()) {
            if (in == null) {
                return;
            }
            JsonElement root = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            JsonArray arr = root.getAsJsonObject().getAsJsonArray("sections");
            if (arr == null) {
                return;
            }
            for (JsonElement el : arr) {
                String name = el.getAsString().strip().toLowerCase();
                if (!name.isEmpty()) {
                    into.add(name);
                }
            }
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 第三方區塊清單讀取失敗: " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface StreamSource {
        InputStream open() throws Exception;
    }
}
