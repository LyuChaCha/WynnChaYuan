package com.wynnchayuan.translate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class LanguagePackTest {

    private static final String FAR = "ja_jp";

    public static void main(String[] args) throws Exception {
        System.out.println("=== 只打包預設語言，其他語言選到時才下載 ===");
        shippedSet();
        missingLanguageAsksForDownload();
        interruptedDownloadIsNotUsable();
        System.out.println(failures == 0
                ? "LanguagePack：全部通過" : "LanguagePack：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void shippedSet() {
        List<String> jar = Languages.inJar();
        check("打進 jar 的語言是 " + jar, jar.contains(Languages.DEFAULT) && jar.contains("zh_cn"));
        check("打進 jar 的語言都在可選的清單裡", Languages.bundled().containsAll(jar));
        check("★ 日文不打包", !Languages.inJar(FAR));
        check("可選的語言比打包的多（其他語言靠下載）", Languages.bundled().size() > jar.size());
    }

    private static void missingLanguageAsksForDownload() throws Exception {
        Path config = Files.createTempDirectory("wcy-pack");
        try {
            LanguagePack.forget();
            check("預設語言不用下載", !LanguagePack.needsDownload(config, Languages.DEFAULT)
                    && !LanguagePack.needsDownload(config, "zh_cn"));
            check("★ 沒打包又沒快取的語言要下載", LanguagePack.needsDownload(config, FAR));
            check("★ 下載前不載入那一層（沒有任何一層 → 顯示原文）",
                    LanguagePack.layers(config, FAR, null).isEmpty());
            check("★ 下載前先顯示輔助語言那一層",
                    LanguagePack.layers(config, FAR, Languages.DEFAULT)
                            .equals(List.of(Languages.dir(config, Languages.DEFAULT))));
            check("預設語言照舊載入，不看快取在不在",
                    LanguagePack.layers(config, "zh_cn", null)
                            .equals(List.of(Languages.dir(config, "zh_cn"))));
            check("空的語言資料夾不算有快取", emptyFolderIsNotUsable(config));

            List<String> asked = new ArrayList<>();
            LanguagePack.Phase got = LanguagePack.download(config, FAR, (dir, lang, progress) -> {
                asked.add(lang + "@" + config.relativize(dir).toString().replace('\\', '/'));
                progress.at(0, 2);
                write(dir, "_index.json", "{\"files\":[\"npc.json\"]}");
                progress.at(1, 2);
                write(dir, "npc.json", "{\"entries\":{}}");
                progress.at(2, 2);
                return true;
            }, null);
            check("★ 送出了一次下載請求：" + asked,
                    asked.equals(List.of(FAR + "@" + LanguagePack.PART + "/" + FAR)));
            check("★ 下載完成", got == LanguagePack.Phase.READY);
            check("下載完就不用再下載", !LanguagePack.needsDownload(config, FAR));
            check("下載完載入那一層", LanguagePack.layers(config, FAR, Languages.DEFAULT)
                    .equals(List.of(Languages.dir(config, Languages.DEFAULT), Languages.dir(config, FAR))));
            check("暫存資料夾收乾淨了", !Files.exists(LanguagePack.partDir(config, FAR)));
            LanguagePack.Status at = LanguagePack.status(FAR);
            check("進度記到 100%", at != null && at.phase() == LanguagePack.Phase.READY
                    && at.percent() == 100);
            check("已有快取時不再送請求", LanguagePack.download(config, FAR, (dir, lang, progress) -> {
                asked.add("again");
                return false;
            }, null) == LanguagePack.Phase.READY && asked.size() == 1);
        } finally {
            deleteTree(config);
        }
    }

    private static boolean emptyFolderIsNotUsable(Path config) throws Exception {
        Path dir = Languages.dir(config, FAR);
        Files.createDirectories(dir.resolve("scoped"));
        write(dir, "_cache.json", "{\"format\":1}");
        LanguagePack.forget();
        boolean ok = LanguagePack.needsDownload(config, FAR);
        deleteTree(dir);
        LanguagePack.forget();
        return ok;
    }

    private static void interruptedDownloadIsNotUsable() throws Exception {
        Path config = Files.createTempDirectory("wcy-pack-cut");
        try {
            LanguagePack.forget();
            LanguagePack.Phase cut = LanguagePack.download(config, FAR, (dir, lang, progress) -> {
                write(dir, "_index.json", "{\"files\":[\"npc.json\",\"quest.json\"]}");
                write(dir, "npc.json", "{\"entries\":{}}");
                return false;
            }, null);
            check("★ 下到一半失敗：回報失敗", cut == LanguagePack.Phase.FAILED);
            check("★ 下到一半失敗：不算有快取", LanguagePack.needsDownload(config, FAR));
            check("★ 下到一半失敗：語言資料夾沒有半套檔案",
                    !Files.exists(Languages.dir(config, FAR)));
            check("下到一半失敗：暫存也清掉", !Files.exists(LanguagePack.partDir(config, FAR)));
            check("下到一半失敗：狀態是失敗", LanguagePack.status(FAR).phase() == LanguagePack.Phase.FAILED);

            LanguagePack.Phase thrown = LanguagePack.download(config, FAR, (dir, lang, progress) -> {
                write(dir, "npc.json", "{\"entries\":{}}");
                throw new IllegalStateException("連線中斷");
            }, null);
            check("下載途中丟例外：回報失敗、不留檔", thrown == LanguagePack.Phase.FAILED
                    && !Files.exists(Languages.dir(config, FAR)));

            LanguagePack.Phase noIndex = LanguagePack.download(config, FAR, (dir, lang, progress) -> {
                write(dir, "npc.json", "{\"entries\":{}}");
                return true;
            }, null);
            check("說完成了卻沒有 _index.json：不收", noIndex == LanguagePack.Phase.FAILED
                    && LanguagePack.needsDownload(config, FAR));

            Path killed = LanguagePack.partDir(config, FAR);
            write(killed, "_index.json", "{\"files\":[\"npc.json\"]}");
            write(killed, "npc.json", "{\"entries\":{}}");
            write(Languages.dir(config, Languages.DEFAULT), "npc.json", "{\"entries\":{}}");
            LanguagePack.forget();
            check("★ 遊戲被關掉留下的暫存不算有快取", LanguagePack.needsDownload(config, FAR));
            Languages.migrateFlat(config, FAR);
            check("★ 搬舊版譯文時不會把暫存當成散檔搬進語言資料夾",
                    !Files.exists(Languages.dir(config, FAR)));

            LanguagePack.Phase retry = LanguagePack.download(config, FAR, (dir, lang, progress) -> {
                boolean fresh = !Files.exists(dir.resolve("npc.json"));
                write(dir, "_index.json", "{\"files\":[\"npc.json\"]}");
                write(dir, fresh ? "npc.json" : "stale.json", "{\"entries\":{}}");
                return true;
            }, null);
            check("重試時從乾淨的暫存開始", retry == LanguagePack.Phase.READY
                    && Files.exists(Languages.dir(config, FAR).resolve("npc.json"))
                    && !Files.exists(Languages.dir(config, FAR).resolve("stale.json")));
        } finally {
            deleteTree(config);
        }
    }

    private static void write(Path dir, String name, String text) {
        try {
            Path file = dir.resolve(name);
            Files.createDirectories(file.getParent());
            Files.writeString(file, text);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void deleteTree(Path root) throws Exception {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> all = Files.walk(root)) {
            for (Path p : all.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }
}
