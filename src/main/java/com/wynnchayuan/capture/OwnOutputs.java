package com.wynnchayuan.capture;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 我們<b>自己畫出去的字</b>——收集語料時不能把它們當成遊戲原文收進去。
 *
 * <h2>實機回報 2026-09-18</h2>
 * 使用者會切換語言測試各種內容。切過之後，{@code captured.json} 裡出現了
 * 「{@code {#} Мировое событие «…» начнётся через {~} мин}」「{@code ¿Usas Reddit?…}」
 * ——那是我們先前畫出去的俄文、西文譯文，換了語言之後在新語言的語料裡查不到，
 * 就被當成「還沒翻的原文」收了。還有模組自己的更新通知
 * 「{@code [WynnChaYuan] Ya está la versión…}」也一樣。
 *
 * <p>這些東西進了語料只會變成一條條沒有人對得上的假原文，匯入的人還得一條條挑掉。
 *
 * <h2>兩道判斷</h2>
 * <ol>
 *   <li><b>任何一種語言的譯文</b>：從 jar 裡把每一種語言的每一條譯文（多行的連同
 *       每一行）轉成收集端的模板寫法，存成排序好的 64 位元雜湊，二分搜尋。
 *       六種語言、三十萬行，只佔兩三 MB。不用字元判斷（西文的重音、西里爾字母）
 *       ——英文原文裡本來就有 29 條帶重音（法國口音的 NPC、亂碼特效），會被誤殺。</li>
 *   <li><b>模組自己送的聊天訊息</b>：送出前先 {@link #note} 一下。</li>
 * </ol>
 */
public final class OwnOutputs {

    private static final String ASSETS = "/assets/wynnchayuan/translations/";

    /** 所有語言譯文的雜湊，排好序。還沒建好之前是空陣列（一律放行，跟以前一樣）。 */
    private static volatile long[] translations = new long[0];

    /** 模組自己送出去的訊息（模板寫法 → 送出時間）。 */
    private static final Map<Long, Long> messages = new ConcurrentHashMap<>();

    /** 自己的訊息記多久。更新通知每一場只講一次，十分鐘綽綽有餘。 */
    private static final long MESSAGE_TTL_MS = 10 * 60 * 1000L;

    /** 我們自己的格式標記，模板裡不會有：{c1} {c:#FF55FF} {/} {w2}。 */
    private static final Pattern MARKUP = Pattern.compile("\\{c(?:\\d+|:[^}]*)}|\\{/}|\\{w\\d+}");
    private static final Pattern NUMBERED = Pattern.compile("\\{~\\d+}");
    private static final Pattern CONTINUATION = Pattern.compile("\\s*\\n\\s*(?:\\{#}\\s*)?");
    private static final Pattern DIGITS = Pattern.compile("\\d+(?:[.,]\\d+)*");

    private OwnOutputs() {}

    /**
     * 在背景把所有語言的譯文讀進來。收集沒開的話不必叫。
     */
    public static void buildAsync() {
        Thread t = new Thread(OwnOutputs::build, "WynnChaYuan-own-outputs");
        t.setDaemon(true);
        t.start();
    }

    static void build() {
        List<Long> out = new ArrayList<>(1 << 18);
        for (String lang : com.wynnchayuan.translate.Languages.bundled()) {
            for (String file : files(lang)) {
                JsonElement root = read(ASSETS + lang + "/" + file);
                if (root != null && root.isJsonObject()) {
                    collect(root.getAsJsonObject(), out);
                }
            }
        }
        long[] sorted = new long[out.size()];
        for (int i = 0; i < sorted.length; i++) {
            sorted[i] = out.get(i);
        }
        Arrays.sort(sorted);
        translations = sorted;
        System.out.println("[WynnChaYuan] 收集時會略過我們自己的譯文：" + sorted.length + " 行");
    }

    /** 給測試用：直接從一份 JSON 內容收。 */
    static void addForTest(JsonObject root) {
        List<Long> out = new ArrayList<>();
        collect(root, out);
        long[] merged = Arrays.copyOf(translations, translations.length + out.size());
        for (int i = 0; i < out.size(); i++) {
            merged[translations.length + i] = out.get(i);
        }
        Arrays.sort(merged);
        translations = merged;
    }

    private static void collect(JsonObject root, List<Long> out) {
        if (root.has("entries") && root.get("entries").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("entries").entrySet()) {
                if (e.getValue().isJsonObject()) {
                    JsonElement dst = e.getValue().getAsJsonObject().get("dst");
                    if (dst != null && dst.isJsonPrimitive()) {
                        add(dst.getAsString(), out);
                    }
                }
            }
            return;
        }
        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
            if (!e.getKey().startsWith("_") && e.getValue().isJsonPrimitive()) {
                add(e.getValue().getAsString(), out);
            }
        }
    }

    private static void add(String dst, List<Long> out) {
        if (dst == null || dst.isBlank()) {
            return;
        }
        String whole = canon(dst);
        if (!whole.isEmpty()) {
            out.add(hash(whole));
        }
        // 多行的譯文，畫面上是一行一行出現、也一行一行被收的
        if (dst.indexOf('\n') >= 0) {
            for (String line : dst.split("\n")) {
                String one = canon(line);
                if (!one.isEmpty()) {
                    out.add(hash(one));
                }
            }
        }
    }

    /**
     * 這一行（收集端的模板寫法）是不是我們自己畫出去的。
     */
    public static boolean isOwn(String template) {
        if (template == null || template.isBlank()) {
            return false;
        }
        long h = hash(canon(template));
        if (Arrays.binarySearch(translations, h) >= 0) {
            return true;
        }
        Long at = messages.get(h);
        return at != null && System.currentTimeMillis() - at < MESSAGE_TTL_MS;
    }

    /**
     * 模組自己要送一則聊天訊息了：先記下來，收集端看到就知道那不是遊戲原文。
     */
    public static void note(Component message) {
        if (message == null) {
            return;
        }
        long now = System.currentTimeMillis();
        messages.entrySet().removeIf(e -> now - e.getValue() > MESSAGE_TTL_MS);
        String text = message.getString();
        messages.put(hash(canon(DIGITS.matcher(text).replaceAll("{~}"))), now);
        for (String line : text.split("\n")) {
            messages.put(hash(canon(DIGITS.matcher(line).replaceAll("{~}"))), now);
        }
    }

    /** 兩邊都轉成同一種寫法：拿掉我們自己的格式標記、數字不編號、空白壓成一格。 */
    static String canon(String s) {
        String t = MARKUP.matcher(s).replaceAll("");
        t = NUMBERED.matcher(t).replaceAll("{~}");
        // 聊天訊息是伺服器照聊天欄寬度先折好的，續行開頭掛著頻道圖示——折在哪裡
        // 每次不一樣，所以「換行＋續行圖示」一律當成一個空格。
        t = CONTINUATION.matcher(t).replaceAll(" ");
        return t.strip().replaceAll("\\s+", " ");
    }

    /** FNV-1a 64 位元。三十萬行裡撞在一起的機率可以忽略。 */
    static long hash(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }

    private static List<String> files(String lang) {
        JsonElement index = read(ASSETS + lang + "/_index.json");
        List<String> out = new ArrayList<>();
        if (index != null && index.isJsonObject() && index.getAsJsonObject().has("files")) {
            for (JsonElement f : index.getAsJsonObject().getAsJsonArray("files")) {
                out.add(f.getAsString());
            }
        }
        return out;
    }

    private static JsonElement read(String resource) {
        try (InputStream in = OwnOutputs.class.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;                        // 一個檔讀不到不影響其他的
        }
    }
}
