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

public final class LangKeysTest {

    private static int failures = 0;

    private static final Path SRC = Path.of("src/main/java/com/wynnchayuan");
    private static final Path LANG =
            Path.of("src/main/resources/assets/wynnchayuan/lang");

    private static final Pattern USED =
            Pattern.compile("(?:\\bT\\.[cs]|\\.tr)\\(\\s*\"([^\"]+)\"");

    private static final Pattern ROW = Pattern.compile(
            "\\b(?:cycle|cycleWith)\\(\\s*\"([a-z][a-z.]+)\",\\s*$",
            Pattern.MULTILINE);

    private static final Pattern ARG = Pattern.compile("%s");

    public static void main(String[] args) throws Exception {
        JsonObject en = load("en_us");

        java.util.Map<String, JsonObject> others = new java.util.LinkedHashMap<>();
        for (String code : codes()) {
            if (!code.equals("en_us")) {
                others.put(code, load(code));
            }
        }

        check("en_us 讀得到（" + en.size() + " 條）", en.size() > 0);
        check("除了 en_us 還有 " + others.size() + " 種語言："
                        + others.keySet(), !others.isEmpty());
        for (var one : others.entrySet()) {
            check(one.getKey() + " 讀得到（" + one.getValue().size() + " 條）",
                    one.getValue().size() > 0);
        }

        Set<String> used = usedKeys();
        check("掃到程式裡用了 " + used.size() + " 個鍵", used.size() > 50);
        List<String> missing = new ArrayList<>();
        for (String key : used) {
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

        for (var entry : others.entrySet()) {
            String name = entry.getKey();
            JsonObject one = entry.getValue();
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

        List<String> mismatched = new ArrayList<>();
        for (String key : en.keySet()) {
            int want = count(en.get(key).getAsString());
            for (var one : others.entrySet()) {
                JsonObject other = one.getValue();
                if (other.has(key) && count(other.get(key).getAsString()) != want) {
                    mismatched.add(one.getKey() + ":" + key);
                }
            }
        }
        check("★ 每一條的 %s 個數每種語言都一致（對不上會在執行期丟例外）"
                        + (mismatched.isEmpty() ? "" : "：" + mismatched),
                mismatched.isEmpty());

        Set<String> mentioned = mentionedKeys();
        List<String> prefixes = mentioned.stream()
                .filter(k -> k.endsWith(".")).toList();
        List<String> unused = new ArrayList<>();
        for (String key : en.keySet()) {
            if (key.startsWith("key.")) {
                continue;
            }
            String bare = key.substring("wynnchayuan.".length());
            if (mentioned.contains(bare)
                    || prefixes.stream().anyMatch(bare::startsWith)) {
                continue;
            }
            unused.add(key);
        }
        check("語言檔裡沒有用不到的鍵"
                        + (unused.isEmpty() ? "" : "：" + unused), unused.isEmpty());

        List<String> binds = new ArrayList<>(keyBindings());
        binds.add("key.category.wynnchayuan.main");
        check("掃到程式裡註冊了 " + (binds.size() - 1) + " 個按鍵", binds.size() > 1);
        List<String> noName = new ArrayList<>();
        for (String key : binds) {
            if (!en.has(key)) {
                noName.add(key);
            }
        }
        check("★ 每個按鍵都有名字（少了原版會印出鍵名）"
                        + (noName.isEmpty() ? "" : "：" + noName), noName.isEmpty());

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

    private static List<String> codes() throws Exception {
        try (Stream<Path> files = Files.list(LANG)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".json"))
                    .map(n -> n.substring(0, n.length() - 5))
                    .sorted()
                    .toList();
        }
    }

    private static JsonObject load(String code) throws Exception {
        return JsonParser.parseString(
                Files.readString(LANG.resolve(code + ".json"))).getAsJsonObject();
    }

    private static Set<String> keyBindings() throws Exception {
        Set<String> out = new LinkedHashSet<>();
        Pattern bind = Pattern.compile("\"(key\\.[A-Za-z0-9_]+\\.[A-Za-z0-9_.]+)\"");
        try (Stream<Path> files = Files.walk(SRC)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher m = bind.matcher(Files.readString(file));
                while (m.find()) {
                    out.add(m.group(1));
                }
            }
        }
        return out;
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
        Matcher credit = Pattern.compile("\"key\"\\s*:\\s*\"([^\"]+)\"").matcher(
                Files.readString(Path.of("src/main/resources/assets/wynnchayuan/credits.json")));
        while (credit.find()) {
            out.add(credit.group(1));
        }
        for (String key : new String[] {"tab.items", "tab.panel", "tab.dialogue",
                                        "tab.world", "tab.data"}) {
            out.add(key);
            out.add(key + ".about");
        }
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
