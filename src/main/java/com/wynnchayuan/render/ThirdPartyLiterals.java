package com.wynnchayuan.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 別的模組拿來<b>判斷遊戲狀態</b>的那幾句話，一律留英文。
 *
 * <h2>踩到什麼</h2>
 * 玩家回報打完討伐戰之後 WynnMod 的連勝反而掉了（截圖：{@code 1 → 0}）。
 *
 * <p>WynnMod 的 {@code RaidState.onTitle} 是這樣判斷這一場的結果的：
 *
 * <pre>
 *   title.contains("Raid Completed")  → 成功
 *   title.contains("Raid Failed")     → 失敗
 * </pre>
 *
 * 而我們的 {@link com.wynnchayuan.listener.TitleListener} 會把那一行整個換掉，
 * 換成「討伐戰完成！」。WynnMod 兩個條件都對不上，於是這一場<b>算不出結果</b>，
 * 上報給它的伺服器之後連勝就重置了。
 *
 * <p>而且我們只翻了 {@code Raid Completed!}、沒翻 {@code Raid Failed!}——
 * 失敗照樣認得出來、成功認不出來，所以連勝<b>只會往下掉</b>。
 * 這正是回報看到的樣子。
 *
 * <h2>為什麼不是「別翻就好」</h2>
 * 沒裝 WynnMod 的人不該為了別人的模組少一句譯文。所以這裡按<b>模組 ID</b> 分組，
 * 只有那個模組真的載入時才保留英文；沒裝的人照常看到中文。
 *
 * <h2>為什麼是資料檔</h2>
 * 和 {@link ThirdPartySections} 同一個理由：第三方模組一直在增加，也一直在改
 * 自己的判斷字串。寫死在 Java 裡代表每次都要重新發版；放成資料檔的話，
 * 使用者自己在 {@code config/wynnchayuan/third-party-literals.json} 補一行就好。
 *
 * <p>比對用<b>包含</b>而不是完全相等——遊戲送過來的那一行前後常有排版符號與
 * 色碼，完全相等永遠不會成立。這裡要的本來就是「這一行裡出現了那個關鍵詞」。
 */
public final class ThirdPartyLiterals {

    private static final String RESOURCE = "/assets/wynnchayuan/third-party-literals.json";

    /**
     * 已載入的那些模組要保留的字串。沒裝的模組不會進這份清單。
     *
     * <p>外層是「或」、內層是「且」：任何一組的每一個詞都出現在這一行，就保留英文。
     * 內層之所以需要「且」，見 {@link #reserved}。
     */
    private static List<List<String>> active = List.of();

    private ThirdPartyLiterals() {
    }

    public static void load(Path configDir) {
        active = groups(read(configDir),
                id -> FabricLoader.getInstance().isModLoaded(id),
                ThirdPartyLiterals::classPresent);
    }

    /** 鍵裡分開模組 ID 與條件的記號。見 {@link #groups}。 */
    private static final String UNLESS = "@unless:";

    /**
     * 清單裡現在生效的那幾組。
     *
     * <h2>鍵的寫法</h2>
     * <ul>
     *   <li>{@code "wynnmod"}：那個模組有裝就生效。</li>
     *   <li>{@code "wynnmod@unless:某個類別"}：有裝、而且<b>沒有</b>那個類別才生效。</li>
     * </ul>
     *
     * <h2>為什麼要有第二種</h2>
     * 同一個模組，讀字串的<b>時機</b>會隨版本改。WynnMod 讀聊天的地方在封包那一層
     * （包住 {@code handleSystemChat} 裡呼叫 {@code ChatListener.handleSystemMessage}
     * 那一處），Wynntils 的聊天事件是在 {@code handleSystemMessage} <b>裡面</b>才發的，
     * 我們又在它後面——它讀到的一定是原文，那幾列我們翻不翻都不影響它。這種時候還
     * 留英文，只是讓有裝它的人白白少一塊譯文（使用者 2026-10-12：「raid 的翻譯消失了」
     * ——就地取代模式的面板 1.0.0_1 起也守這份清單，整塊結算就留成英文了）。
     *
     * <p>「它是不是在我們之前讀」沒辦法直接問，問的是它那條路上的類別在不在：
     * 在，就不必讓；不在（舊版），照舊留英文。標題那幾條不適用——標題是 Wynntils
     * 先攔、我們取消再重送，它在後面，一定要留。
     *
     * @param modLoaded    這個模組 ID 有沒有裝
     * @param classPresent 這個類別在不在（不載入它，只問有沒有）
     */
    public static List<List<String>> groups(JsonObject root,
                                            java.util.function.Predicate<String> modLoaded,
                                            java.util.function.Predicate<String> classPresent) {
        List<List<String>> out = new ArrayList<>();
        if (root != null) {
            for (Map.Entry<String, JsonElement> mod : root.entrySet()) {
                if (mod.getKey().startsWith("_") || !mod.getValue().isJsonArray()) {
                    continue;             // _comment 之類的說明欄位
                }
                String id = mod.getKey();
                String unless = null;
                int at = id.indexOf(UNLESS);
                if (at > 0) {
                    unless = id.substring(at + UNLESS.length()).strip();
                    id = id.substring(0, at);
                }
                if (!modLoaded.test(id)) {
                    continue;             // 沒裝就不必讓步
                }
                if (unless != null && !unless.isEmpty() && classPresent.test(unless)) {
                    continue;             // 它在我們之前就讀完了，不必讓
                }
                for (JsonElement literal : mod.getValue().getAsJsonArray()) {
                    List<String> group = new ArrayList<>();
                    if (literal.isJsonArray()) {
                        for (JsonElement part : literal.getAsJsonArray()) {
                            String text = part.getAsString();
                            if (!text.isBlank()) {
                                group.add(text);
                            }
                        }
                    } else {
                        String text = literal.getAsString();
                        if (!text.isBlank()) {
                            group.add(text);
                        }
                    }
                    if (!group.isEmpty()) {
                        out.add(List.copyOf(group));
                    }
                }
            }
        }
        return List.copyOf(out);
    }

    /** 這個類別在不在。只問資源、不載入：載入別人的 mixin 事件類別沒有好處。 */
    private static boolean classPresent(String name) {
        try {
            return ThirdPartyLiterals.class.getClassLoader()
                    .getResource(name.replace('.', '/') + ".class") != null;
        } catch (Throwable t) {
            return false;                 // 問不到就當成不在：留英文是安全的那一邊
        }
    }

    /**
     * 這一行是別的模組在讀的嗎——是的話別動它。
     *
     * <h2>為什麼一組可以有好幾個詞</h2>
     * 單一個關鍵詞常常不夠準。{@code Reward Pulls} 同時出現在兩個地方：
     *
     * <pre>
     *   討伐戰結算   DMG 2.9M/9.7M (30.16%)   39 Reward Pulls   ← WynnMod 在解
     *   獵殺信標     Reward Pulls   next Beacon Effects         ← 跟它無關
     * </pre>
     *
     * 只寫 {@code Reward Pulls} 會把信標面板也一起擋掉。分別它們的是結算那一行
     * 的百分比：{@code (30.16%)}——所以寫成 {@code ["Reward Pulls", "%)"]}，
     * 兩個都出現才算。
     *
     * <p>用這種方式而不是把整條模板列出來，是因為那些數字有 K／M／B 好幾種寫法，
     * 列模板等於又要一種一條——名牌與信標都已經在這上面吃過虧。
     */
    public static boolean reserved(String text) {
        if (active.isEmpty() || text == null || text.isEmpty()) {
            return false;
        }
        for (List<String> group : active) {
            boolean all = true;
            for (String part : group) {
                if (!text.contains(part)) {
                    all = false;
                    break;
                }
            }
            if (all) {
                return true;
            }
        }
        return false;
    }

    /** 使用者自己的設定優先，沒有就用內建的。 */
    private static JsonObject read(Path configDir) {
        Path override = configDir == null ? null : configDir.resolve("third-party-literals.json");
        try {
            if (override != null && Files.isRegularFile(override)) {
                return JsonParser.parseString(Files.readString(override, StandardCharsets.UTF_8))
                        .getAsJsonObject();
            }
        } catch (Exception e) {
            // 使用者手改壞了就退回內建的，不要因此整個模組起不來
        }
        try (InputStream in = ThirdPartyLiterals.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 測試用：直接指定要保留哪些字串，不看模組裝了沒。
     *
     * <p>開放到 public 是因為測試在 {@code com.wynnchayuan} 底下，跟這裡不同套件。
     * 真正會踩到的情境（裝了 WynnMod）在 CI 上重現不了——沒有那個模組，
     * {@link #load} 就會把整組跳過。不給測試一個入口的話，比對邏輯一行都測不到。
     */
    public static void forTest(List<String> literals) {
        List<List<String>> out = new ArrayList<>();
        for (String literal : literals) {
            out.add(List.of(literal));
        }
        active = List.copyOf(out);
    }

    /** 測試用：直接指定「且」的組，不看模組裝了沒。見 {@link #forTest}。 */
    public static void forTestGroups(List<List<String>> groups) {
        active = List.copyOf(groups);
    }

    /** 目前生效的清單，診斷與測試用。 */
    public static List<List<String>> activeLiterals() {
        return active;
    }
}
