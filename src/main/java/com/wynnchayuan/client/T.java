package com.wynnchayuan.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模組自己的介面文字。
 *
 * <h2>為什麼要有</h2>
 * F6 那幾個畫面原本整片寫死繁體中文。對只會英文的人來說，那等於整個設定面板
 * 都看不懂——而這是個<b>翻譯</b>模組，看不懂設定面板的人正是最需要它的人。
 *
 * <h2>跟著誰走</h2>
 * <ul>
 *   <li>F6 裡<b>指名</b>了譯文語言時，介面也跟著那一種——把譯文切到簡體
 *       卻只有設定面板還是繁體，看起來像壞掉。</li>
 *   <li>沒指名（跟著遊戲走）時，交給 Minecraft 自己的語言檔，
 *       介面就跟著遊戲語言。</li>
 * </ul>
 *
 * <p>兩條路都以 {@code en_us} 為底：缺哪一條就顯示英文，不會變成空白，
 * 所以新語言只要放一個檔進去就生效。
 */
public final class T {

    private T() {}

    private static final String PREFIX = "wynnchayuan.";
    private static final String PATH = "/assets/wynnchayuan/lang/";

    /** 指名語言時自己讀的那幾份。讀過就留著，一場遊戲頂多兩三種。 */
    private static final Map<String, Map<String, String>> LOADED =
            new ConcurrentHashMap<>();

    /** 給要 {@code Component} 的地方。 */
    public static MutableComponent c(String key, Object... args) {
        String pattern = pinned(key);
        if (pattern == null) {
            return Component.translatable(PREFIX + key, args);
        }
        // 指名語言時自己套參數。Component.translatable 查的是<b>遊戲</b>的
        // 語言檔，這裡要的是另一份，所以只能自己格式化。
        return Component.literal(format(pattern, args));
    }

    /**
     * 給只收 {@code String} 的地方（量寬度、拼字串）。
     *
     * <p>會當場解析成目前語言的字。畫面上的東西一幀畫一次，這個成本
     * 跟畫字本身比可以忽略。
     */
    public static String s(String key, Object... args) {
        String pattern = pinned(key);
        return pattern == null ? c(key, args).getString() : format(pattern, args);
    }

    /**
     * F6 指名了語言時，那一份語言檔裡的原文。
     *
     * @return 指名語言底下這一條的原文；沒指名、或那一份沒有這一條時回傳
     *         {@code null}，由呼叫端退回遊戲語言那條路
     */
    private static String pinned(String key) {
        String lang;
        try {
            lang = com.wynnchayuan.WynnChaYuan.config().language();
        } catch (Exception e) {
            return null;                  // 設定還沒建立（測試、啟動極早期）
        }
        if (lang == null || lang.isBlank()) {
            return null;
        }
        String value = load(lang).get(PREFIX + key);
        // 指名的那一份沒有這一條，就退回 en_us；再沒有才交給遊戲語言。
        return value != null ? value : load("en_us").get(PREFIX + key);
    }

    private static Map<String, String> load(String lang) {
        return LOADED.computeIfAbsent(lang, T::read);
    }

    private static Map<String, String> read(String lang) {
        try (InputStream in = T.class.getResourceAsStream(PATH + lang + ".json")) {
            if (in == null) {
                return Map.of();
            }
            JsonObject root = JsonParser.parseString(
                    new String(in.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
            Map<String, String> out = new java.util.HashMap<>();
            for (String key : root.keySet()) {
                out.put(key, root.get(key).getAsString());
            }
            return Map.copyOf(out);
        } catch (Exception e) {
            return Map.of();              // 壞掉就當沒有，退回遊戲語言那條路
        }
    }

    /**
     * 把 {@code %s} 換成參數。
     *
     * <p>用 {@link String#format} 會在參數個數對不上時丟例外，而那是在
     * <b>畫面上</b>丟——整個 F6 就開不起來。這裡多的參數忽略、少的留原樣，
     * 最壞情況是畫面上看到一個 {@code %s}，比開不起來好得多。
     */
    private static String format(String pattern, Object... args) {
        if (args == null || args.length == 0) {
            return pattern;
        }
        StringBuilder out = new StringBuilder(pattern.length() + 16);
        int at = 0;
        for (int i = 0; i < pattern.length(); i++) {
            if (pattern.charAt(i) == '%' && i + 1 < pattern.length()
                    && pattern.charAt(i + 1) == 's' && at < args.length) {
                out.append(args[at++]);
                i++;
            } else {
                out.append(pattern.charAt(i));
            }
        }
        return out.toString();
    }
}
