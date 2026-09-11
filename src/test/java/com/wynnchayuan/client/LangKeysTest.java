package com.wynnchayuan.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 介面文字的語言檔對得起程式嗎。
 *
 * <h2>為什麼需要</h2>
 * {@code Component.translatable} 查不到鍵的時候<b>不會失敗</b>——它把鍵名原樣
 * 畫在畫面上。所以打錯一個字的後果是使用者看到 {@code wynnchayuan.data.sorce}，
 * 而開發這邊什麼事都沒有。
 *
 * <p>{@code %s} 的個數對不上更糟：那會在執行期丟例外，而丟例外的時機是
 * 「某個人剛好切到那個語言、剛好開到那一頁」。
 *
 * <p>這一條掃程式碼裡所有 {@code T.c("…")} / {@code T.s("…")} 的鍵，跟三份
 * 語言檔對照。
 */
public final class LangKeysTest {

    private static int failures = 0;

    private static final Path SRC = Path.of("src/main/java/com/wynnchayuan");
    private static final Path LANG =
            Path.of("src/main/resources/assets/wynnchayuan/lang");

    /** {@code T.c("key"} 或 {@code T.s("key"}。 */
    private static final Pattern USED =
            Pattern.compile("\\bT\\.[cs]\\(\\s*\"([^\"]+)\"");

    /** {@code cycle("key", …)} 那一路：名稱與說明是同一個鍵推出來的。 */
    private static final Pattern ROW = Pattern.compile(
            "\\b(?:cycle|cycleWith)\\(\\s*\"([a-z][a-z.]+)\",\\s*$",
            Pattern.MULTILINE);

    private static final Pattern ARG = Pattern.compile("%s");

    public static void main(String[] args) throws Exception {
        JsonObject en = load("en_us");
        JsonObject tw = load("zh_tw");
        JsonObject cn = load("zh_cn");

        check("en_us 讀得到（" + en.size() + " 條）", en.size() > 0);
        check("zh_tw 讀得到（" + tw.size() + " 條）", tw.size() > 0);
        check("zh_cn 讀得到（" + cn.size() + " 條）", cn.size() > 0);

        // ---- 程式用到的鍵，en_us 一定要有 ----
        Set<String> used = usedKeys();
        check("掃到程式裡用了 " + used.size() + " 個鍵", used.size() > 50);
        List<String> missing = new ArrayList<>();
        for (String key : used) {
            // 以點結尾的是<b>前綴</b>——鍵是拼出來的
            // （{@code T.s("tracker.heading." + type)}）。那種要求的是
            // 「這個前綴底下至少有東西」，不是「有這一條」。
            if (key.endsWith(".")) {
                boolean any = en.keySet().stream()
                        .anyMatch(k -> k.startsWith("wynnchayuan." + key));
                if (!any) {
                    missing.add(key + "*");
                }
                continue;
            }
            if (!en.has("wynnchayuan." + key)) {
                missing.add(key);
            }
        }
        check("★ 程式用到的鍵 en_us 全都有"
                        + (missing.isEmpty() ? "" : "（少了 " + missing + "）"),
                missing.isEmpty());

        // ---- 三份語言檔的鍵要一致 ----
        // en_us 是回退的底，所以它一定要最齊；其他語言少了哪一條只是顯示英文，
        // 但<b>多</b>了哪一條就表示鍵名打錯了——那一條永遠不會被用到。
        for (var entry : List.of(new String[] {"zh_tw"}, new String[] {"zh_cn"})) {
            String name = entry[0];
            JsonObject one = name.equals("zh_tw") ? tw : cn;
            List<String> extra = new ArrayList<>();
            List<String> absent = new ArrayList<>();
            for (String key : one.keySet()) {
                if (!en.has(key)) {
                    extra.add(key);
                }
            }
            for (String key : en.keySet()) {
                if (!one.has(key)) {
                    absent.add(key);
                }
            }
            check("★ " + name + " 沒有 en_us 以外的鍵（打錯字會變成永遠用不到）"
                            + (extra.isEmpty() ? "" : "：" + extra),
                    extra.isEmpty());
            check(name + " 沒有缺漏（缺了只會顯示英文，但這裡該是齊的）"
                            + (absent.isEmpty() ? "" : "：" + absent),
                    absent.isEmpty());
        }

        // ---- %s 的個數要一致 ----
        List<String> mismatched = new ArrayList<>();
        for (String key : en.keySet()) {
            int want = count(en.get(key).getAsString());
            for (JsonObject other : List.of(tw, cn)) {
                if (other.has(key) && count(other.get(key).getAsString()) != want) {
                    mismatched.add(key);
                }
            }
        }
        check("★ 每一條的 %s 個數三份語言一致（對不上會在執行期丟例外）"
                        + (mismatched.isEmpty() ? "" : "：" + mismatched),
                mismatched.isEmpty());

        // ---- 沒有用到的鍵 ----
        //
        // 這裡用<b>寬鬆</b>的掃法：程式碼裡任何長得像鍵的字面都算數，
        // 因為鍵常常寫在三元或 switch 裡（{@code T.s(on ? "mode.on" : "mode.off")}），
        // 嚴格的正規式抓不到。寧可把沒用到的當成有用到，也不要指控一條
        // 其實有在用的鍵——那會逼人把好好的檢查關掉。
        Set<String> mentioned = mentionedKeys();
        List<String> prefixes = mentioned.stream()
                .filter(k -> k.endsWith(".")).toList();
        List<String> unused = new ArrayList<>();
        for (String key : en.keySet()) {
            String bare = key.substring("wynnchayuan.".length());
            if (mentioned.contains(bare)
                    || prefixes.stream().anyMatch(bare::startsWith)) {
                continue;
            }
            unused.add(key);
        }
        check("語言檔裡沒有用不到的鍵"
                        + (unused.isEmpty() ? "" : "：" + unused), unused.isEmpty());

        report();
    }

    private static int count(String text) {
        Matcher m = ARG.matcher(text);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    private static JsonObject load(String code) throws Exception {
        return JsonParser.parseString(
                Files.readString(LANG.resolve(code + ".json"))).getAsJsonObject();
    }

    private static Set<String> usedKeys() throws Exception {
        Set<String> out = new LinkedHashSet<>();
        try (Stream<Path> files = Files.walk(SRC)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher m = USED.matcher(Files.readString(file));
                while (m.find()) {
                    out.add(m.group(1));
                }
            }
        }
        // 分類名是照 TAB_KEYS 拼出來的，掃不到。
        for (String key : new String[] {"tab.items", "tab.panel", "tab.dialogue",
                                        "tab.world", "tab.data"}) {
            out.add(key);
            out.add(key + ".about");
        }
        // cycle("x", …) 這一路的名稱與說明是從同一個鍵推出來的（x 與 x.hint），
        // 所以 x.hint 在程式碼裡根本不會以字面出現。
        try (Stream<Path> files = Files.walk(SRC)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher m = ROW.matcher(Files.readString(file));
                while (m.find()) {
                    out.add(m.group(1));
                    out.add(m.group(1) + ".hint");
                }
            }
        }
        return out;
    }

    /** 程式碼裡任何長得像鍵的字面。見上面那段說明。 */
    private static Set<String> mentionedKeys() throws Exception {
        Set<String> out = new LinkedHashSet<>(usedKeys());
        Pattern any = Pattern.compile("\"([a-z][a-z0-9]*(?:\\.[a-z][a-z0-9]*)+)\"");
        try (Stream<Path> files = Files.walk(SRC)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher m = any.matcher(Files.readString(file));
                while (m.find()) {
                    out.add(m.group(1));
                    out.add(m.group(1) + ".hint");
                }
            }
        }
        return out;
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        System.out.println(failures == 0
                ? "LangKeys: 全部通過" : "LangKeys: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
