package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;

import java.nio.file.Files;
import java.nio.file.Path;

public final class SearchMatchTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations", "zh_tw"));
        CollectorConfig config = new CollectorConfig(
                Files.createTempDirectory("wcy-search").resolve("config.json"));
        while (!config.wynntilsUi()) {
            config.toggleWynntilsUi();
        }

        subsequenceRule();
        realCorpus(config, store);
        guards(config, store);
        itemNames(store);

        System.out.println(failures == 0 ? "\n譯文搜尋：全部通過"
                : "\n譯文搜尋：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void subsequenceRule() {
        check("子序列：anni 中得了 Prelude to Annihilation",
                SearchMatch.partialMatch("Prelude to Annihilation", "anni"));
        check("子序列：不連續也算（ptа 之於 Prelude to Annihilation）",
                SearchMatch.partialMatch("Prelude to Annihilation", "pta"));
        check("順序不對就不中",
                !SearchMatch.partialMatch("Prelude to Annihilation", "iludeP"));
        check("不分大小寫", SearchMatch.partialMatch("Prelude", "PRE"));
        check("中文照樣是子序列", SearchMatch.partialMatch("毀滅前奏", "毀奏"));
        check("中文順序不對就不中", !SearchMatch.partialMatch("毀滅前奏", "奏毀"));
    }

    private static void realCorpus(CollectorConfig config, TranslationStore store) {
        String[][] cases = {
            {"Prelude to Annihilation", "毀滅"},
            {"Prelude to Annihilation", "前奏"},
        };
        for (String[] c : cases) {
            String shown = store.lookup(c[0]);
            check("語料查得到「" + c[0] + "」→ " + shown, shown != null);
            check("打「" + c[1] + "」搜得到「" + c[0] + "」",
                    SearchMatch.alsoMatches(c[0], c[1], config, store));
        }
        check("原文沒中而譯文也沒中時，維持沒中",
                !SearchMatch.alsoMatches("Prelude to Annihilation", "zzzz", config, store));
    }

    private static void guards(CollectorConfig config, TranslationStore store) {
        while (config.wynntilsUi()) {
            config.toggleWynntilsUi();
        }
        check("「翻譯 Wynntils 介面」關著時一律不中",
                !SearchMatch.alsoMatches("Prelude to Annihilation", "毀滅", config, store));
        while (!config.wynntilsUi()) {
            config.toggleWynntilsUi();
        }

        check("語料查不到的原文不中",
                !SearchMatch.alsoMatches("Zzzq Nonexistent Activity", "毀滅", config, store));
        check("空查詢不中（交給 Wynntils 自己回傳全中）",
                !SearchMatch.alsoMatches("Prelude to Annihilation", "", config, store));
        check("null 不會炸",
                !SearchMatch.alsoMatches(null, "毀滅", config, store)
                        && !SearchMatch.alsoMatches("Prelude to Annihilation", null,
                                config, store)
                        && !SearchMatch.alsoMatches("Prelude to Annihilation", "毀滅",
                                config, null));
    }

    private static void itemNames(TranslationStore store) {
        check("語料查得到 Abysso Galoshes → " + store.lookup("Abysso Galoshes"),
                "深淵雨靴".equals(store.lookup("Abysso Galoshes")));
        check("★ 打「深淵」找得到 Abysso Galoshes",
                SearchMatch.nameAlsoMatches("Abysso Galoshes",
                        java.util.List.of("深淵"), store));
        check("★ 打全名也找得到",
                SearchMatch.nameAlsoMatches("Abysso Galoshes",
                        java.util.List.of("深淵雨靴"), store));
        check("★ 打後半段也找得到",
                SearchMatch.nameAlsoMatches("Abysso Galoshes",
                        java.util.List.of("雨靴"), store));
        check("是「包含」不是子序列：打「深靴」不中",
                !SearchMatch.nameAlsoMatches("Abysso Galoshes",
                        java.util.List.of("深靴"), store));
        check("語料查不到的物品不中",
                !SearchMatch.nameAlsoMatches("Zzzq Nonexistent Item",
                        java.util.List.of("深淵"), store));
        check("沒有純文字片段時不中（那時 Wynntils 自己回傳全中）",
                !SearchMatch.nameAlsoMatches("Abysso Galoshes",
                        java.util.List.of(), store));
        check("片段只有空白時不中",
                !SearchMatch.nameAlsoMatches("Abysso Galoshes",
                        java.util.List.of(" "), store));
        check("null 不會炸",
                !SearchMatch.nameAlsoMatches(null, java.util.List.of("深淵"), store)
                        && !SearchMatch.nameAlsoMatches("Abysso Galoshes", null, store)
                        && !SearchMatch.nameAlsoMatches("Abysso Galoshes",
                                java.util.List.of("深淵"), null));
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + name);
        if (!ok) {
            failures++;
        }
    }
}
