package com.wynnchayuan.translate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class BundledCorpusTest {

    private static final String PREFIX = "assets/wynnchayuan/translations/";
    private static final int SAMPLES = 400;

    public static void main(String[] args) throws Exception {
        System.out.println("=== jar 內附的 xz 語料 ===");
        String built = System.getProperty("wcy.jar", "");
        Path jar = built.isBlank() ? null : Path.of(built);
        boolean present = jar != null && Files.isRegularFile(jar);
        check("有建好的 jar 可以測：" + built, present);
        if (present) {
            try (ZipFile zip = new ZipFile(jar.toFile())) {
                BundledCorpus.Source fromJar = entries(zip);
                for (String lang : Languages.inJar()) {
                    sameBytes(zip, fromJar, lang);
                    firstInstall(fromJar, lang);
                    cacheStillWins(fromJar, lang);
                }
                sameLookups(fromJar);
                loadTime(fromJar);
            } finally {
                BundledCorpus.readFrom(null);
            }
        }
        System.out.println(failures == 0
                ? "BundledCorpus：全部通過" : "BundledCorpus：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static BundledCorpus.Source entries(ZipFile zip) {
        return resource -> {
            ZipEntry entry = zip.getEntry(resource.startsWith("/") ? resource.substring(1) : resource);
            return entry == null ? null : zip.getInputStream(entry);
        };
    }

    private static Path sourceDir(String lang) throws Exception {
        URL index = BundledCorpus.class.getResource("/" + PREFIX + lang + "/_index.json");
        if (index == null) {
            throw new IllegalStateException(lang + " 在 classpath 上找不到原始 json");
        }
        return Path.of(index.toURI()).getParent();
    }

    private static TreeMap<String, Path> rawFiles(String lang) throws Exception {
        Path dir = sourceDir(lang);
        TreeMap<String, Path> out = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".json"))
                .forEach(p -> out.put(dir.relativize(p).toString().replace('\\', '/'), p));
        }
        return out;
    }

    private static byte[] unpack(BundledCorpus.Source source, String resource) throws IOException {
        BundledCorpus.readFrom(source);
        try (InputStream in = BundledCorpus.open(resource)) {
            return in == null ? null : in.readAllBytes();
        } finally {
            BundledCorpus.readFrom(null);
        }
    }

    private static void sameBytes(ZipFile zip, BundledCorpus.Source fromJar, String lang) throws Exception {
        String base = PREFIX + lang + "/";
        TreeSet<String> packed = new TreeSet<>();
        List<String> plain = new ArrayList<>();
        zip.stream().map(ZipEntry::getName).filter(n -> n.startsWith(base)).forEach(n -> {
            if (n.endsWith(".json" + BundledCorpus.PACKED)) {
                packed.add(n.substring(base.length(), n.length() - BundledCorpus.PACKED.length()));
            } else if (n.endsWith(".json")) {
                plain.add(n);
            }
        });
        TreeMap<String, Path> raw = rawFiles(lang);
        check(lang + "：jar 裡沒有原始 json（" + plain.size() + " 個）", plain.isEmpty());
        check(lang + "：xz 跟原始 json 是同一批檔（xz " + packed.size() + "、json " + raw.size() + "）",
                packed.equals(raw.keySet()) && packed.size() > 10);
        List<String> differ = new ArrayList<>();
        long bytes = 0;
        for (Map.Entry<String, Path> file : raw.entrySet()) {
            byte[] want = Files.readAllBytes(file.getValue());
            byte[] got = unpack(fromJar, "/" + base + file.getKey());
            bytes += want.length;
            if (!Arrays.equals(want, got)) {
                differ.add(file.getKey());
            }
        }
        check(lang + "：從 jar 解出來的 " + raw.size() + " 個檔（" + bytes / 1024 + " KB）跟原始 json 一模一樣"
                + (differ.isEmpty() ? "" : "，不同：" + differ.subList(0, Math.min(5, differ.size()))),
                differ.isEmpty() && bytes > 1_000_000);
    }

    private static void firstInstall(BundledCorpus.Source fromJar, String lang) throws Exception {
        Path fromXz = Files.createTempDirectory("wcy-xz-jar");
        Path fromJson = Files.createTempDirectory("wcy-xz-json");
        try {
            int a = install(fromJar, fromXz, lang);
            int b = install(null, fromJson, lang);
            check(lang + "：沒有快取時從 xz 倒出 " + a + " 個檔，跟原始 json 倒出的一樣多", a == b && a > 10);
            check(lang + "：倒出來的檔逐位元組相同", sameTree(fromXz, fromJson));
        } finally {
            deleteTree(fromXz);
            deleteTree(fromJson);
        }
    }

    private static void cacheStillWins(BundledCorpus.Source fromJar, String lang) throws Exception {
        Path dir = Files.createTempDirectory("wcy-xz-cache");
        try {
            install(fromJar, dir, lang);
            Path npc = dir.resolve("npc.json");
            String edited = "{\"Blacksmith\": \"快取裡的譯名\"}";
            Files.writeString(npc, edited, StandardCharsets.UTF_8);
            int again = install(fromJar, dir, lang);
            check(lang + "：有快取時不從 jar 倒（" + again + " 個），快取的檔沒被蓋掉",
                    again == 0 && edited.equals(Files.readString(npc, StandardCharsets.UTF_8)));
            BundledCorpus.readFrom(fromJar);
            boolean restored;
            try {
                restored = StarterFiles.restore(dir, lang, "npc.json");
            } finally {
                BundledCorpus.readFrom(null);
            }
            check(lang + "：壞掉的檔可以從 xz 補回原樣", restored
                    && Arrays.equals(Files.readAllBytes(npc), Files.readAllBytes(sourceDir(lang).resolve("npc.json"))));
        } finally {
            deleteTree(dir);
        }
    }

    private static void sameLookups(BundledCorpus.Source fromJar) throws Exception {
        List<String> langs = Languages.inJar();
        Path xzRoot = Files.createTempDirectory("wcy-xz-look-jar");
        Path jsonRoot = Files.createTempDirectory("wcy-xz-look-json");
        try {
            List<Path> xzLayers = new ArrayList<>();
            List<Path> jsonLayers = new ArrayList<>();
            for (String lang : langs) {
                install(fromJar, xzRoot.resolve(lang), lang);
                install(null, jsonRoot.resolve(lang), lang);
                xzLayers.add(xzRoot.resolve(lang));
                jsonLayers.add(jsonRoot.resolve(lang));
            }
            for (int i = 0; i < langs.size(); i++) {
                compareStores(langs.get(i), List.of(xzLayers.get(i)), List.of(jsonLayers.get(i)));
            }
            if (langs.size() > 1) {
                compareStores(String.join("＋", langs), xzLayers, jsonLayers);
            }
        } finally {
            deleteTree(xzRoot);
            deleteTree(jsonRoot);
        }
    }

    private static void compareStores(String what, List<Path> xzLayers, List<Path> jsonLayers) throws Exception {
        TranslationStore xz = new TranslationStore();
        TranslationStore json = new TranslationStore();
        xz.loadAll(xzLayers);
        json.loadAll(jsonLayers);
        List<String> keys = sample(jsonLayers.getLast());
        int hits = 0;
        int flatHits = 0;
        List<String> differ = new ArrayList<>();
        for (String key : keys) {
            String a = xz.lookup(key);
            String b = json.lookup(key);
            String fa = xz.lookupFlat(key);
            String fb = json.lookupFlat(key);
            if (!Objects.equals(a, b) || !Objects.equals(fa, fb)) {
                differ.add(key);
            }
            hits += b != null ? 1 : 0;
            flatHits += fb != null ? 1 : 0;
        }
        check(what + "：載入條數相同（" + xz.size() + " / " + json.size() + "）",
                xz.size() == json.size() && json.size() > 10000);
        check(what + "：抽 " + keys.size() + " 條查表（查到 " + hits + "、長句 " + flatHits + "），結果相同"
                + (differ.isEmpty() ? "" : "，不同：" + differ.subList(0, Math.min(3, differ.size()))),
                differ.isEmpty() && keys.size() >= SAMPLES && hits > SAMPLES / 4 && flatHits > 0);
    }

    private static List<String> sample(Path dir) throws Exception {
        List<String> keys = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path file : walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".json"))
                    .filter(p -> !p.getFileName().toString().startsWith("_"))
                    .sorted().toList()) {
                JsonElement root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8));
                if (!root.isJsonObject()) {
                    continue;
                }
                JsonObject obj = root.getAsJsonObject();
                JsonObject body = obj.has("entries") && obj.get("entries").isJsonObject()
                        ? obj.getAsJsonObject("entries") : obj;
                body.keySet().stream().filter(k -> !k.startsWith("_")).forEach(keys::add);
            }
        }
        int step = Math.max(1, keys.size() / SAMPLES);
        List<String> picked = new ArrayList<>();
        for (int i = 0; i < keys.size() && picked.size() < SAMPLES; i += step) {
            picked.add(keys.get(i));
        }
        return picked;
    }

    private static void loadTime(BundledCorpus.Source fromJar) throws Exception {
        Path before = Files.createTempFile("wcy-xz-before", ".jar");
        try {
            List<String> langs = Languages.inJar();
            try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(before))) {
                for (String lang : langs) {
                    for (Map.Entry<String, Path> file : rawFiles(lang).entrySet()) {
                        out.putNextEntry(new ZipEntry(PREFIX + lang + "/" + file.getKey()));
                        Files.copy(file.getValue(), out);
                        out.closeEntry();
                    }
                }
            }
            try (ZipFile deflated = new ZipFile(before.toFile())) {
                BundledCorpus.Source plain = entries(deflated);
                String which = String.join("＋", langs);
                compareTime("沒有快取時倒出 " + which + " 的內附語料", plain, fromJar, s -> installed(s, langs));
                compareTime("每次開遊戲背景讀一遍 " + which + " 的內附語料（OwnOutputs）", plain, fromJar,
                        s -> readAll(s, langs));
            }
        } finally {
            Files.deleteIfExists(before);
        }
    }

    @FunctionalInterface
    private interface Timed {
        long run(BundledCorpus.Source source) throws Exception;
    }

    private static void compareTime(String what, BundledCorpus.Source plain, BundledCorpus.Source packed,
                                    Timed job) throws Exception {
        job.run(plain);
        job.run(packed);
        long old = Long.MAX_VALUE;
        long now = Long.MAX_VALUE;
        for (int round = 0; round < 3; round++) {
            old = Math.min(old, job.run(plain));
            now = Math.min(now, job.run(packed));
        }
        System.out.printf("  [INFO] %s：改前（deflate json）%d ms，改後（xz）%d ms，多 %d ms%n",
                what, old, now, now - old);
    }

    private static long readAll(BundledCorpus.Source source, List<String> langs) throws Exception {
        long t0 = System.nanoTime();
        long bytes = 0;
        BundledCorpus.readFrom(source);
        try {
            for (String lang : langs) {
                List<String> names = new ArrayList<>(FileIndex.bundled(lang));
                names.add("_index.json");
                for (String name : names) {
                    try (InputStream in = BundledCorpus.open(Languages.resource(lang) + name)) {
                        bytes += in == null ? 0 : in.readAllBytes().length;
                    }
                }
            }
        } finally {
            BundledCorpus.readFrom(null);
        }
        if (bytes < 1_000_000) {
            throw new IllegalStateException("只讀到 " + bytes + " bytes");
        }
        return (System.nanoTime() - t0) / 1_000_000;
    }

    private static long installed(BundledCorpus.Source source, List<String> langs) throws Exception {
        Path root = Files.createTempDirectory("wcy-xz-time");
        try {
            long t0 = System.nanoTime();
            for (String lang : langs) {
                install(source, root.resolve(lang), lang);
            }
            return (System.nanoTime() - t0) / 1_000_000;
        } finally {
            deleteTree(root);
        }
    }

    private static int install(BundledCorpus.Source source, Path dir, String lang) {
        BundledCorpus.readFrom(source);
        try {
            return StarterFiles.installIfEmpty(dir, lang);
        } finally {
            BundledCorpus.readFrom(null);
        }
    }

    private static boolean sameTree(Path a, Path b) throws IOException {
        TreeMap<String, Path> left = files(a);
        TreeMap<String, Path> right = files(b);
        if (!left.keySet().equals(right.keySet()) || left.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, Path> e : left.entrySet()) {
            if (!Arrays.equals(Files.readAllBytes(e.getValue()), Files.readAllBytes(right.get(e.getKey())))) {
                return false;
            }
        }
        return true;
    }

    private static TreeMap<String, Path> files(Path root) throws IOException {
        TreeMap<String, Path> out = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                .forEach(p -> out.put(root.relativize(p).toString().replace('\\', '/'), p));
        }
        return out;
    }

    private static void deleteTree(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}
