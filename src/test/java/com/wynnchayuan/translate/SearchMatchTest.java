package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 用譯文搜尋 Wynntils 的畫面。
 *
 * <h2>釘住什麼</h2>
 * 實機回報：內容書那張卡寫著「毀滅前奏」，打「毀滅」一張都沒亮，打
 * {@code anni} 才亮——因為 Wynntils 拿去比的是它手上的英文
 * {@code Prelude to Annihilation}。
 *
 * <p>這裡用<b>真的語料</b>（繁中那一份）跑，不是自己造的假資料：會不會中
 * 取決於語料裡那一條長什麼樣，假資料測起來全過、實機照樣搜不到。
 *
 * <p>另外釘住三件「不可以」的事，那才是這一道的風險所在：
 * <ul>
 *   <li>設定關著時一律不中（畫面是英文，中文卻搜得到只會讓人困惑）；</li>
 *   <li>語料查不到的原文不中（不能自己發明比對）；</li>
 *   <li>空查詢不中（那由 Wynntils 自己處理，它回傳全中）。</li>
 * </ul>
 */
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

    /** 比對規則要跟 Wynntils 一樣是子序列，不是「包含」。 */
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

    /** 真的語料：畫面上看得到的字要搜得到。 */
    private static void realCorpus(CollectorConfig config, TranslationStore store) {
        String[][] cases = {
            // {原文, 玩家打的中文}
            {"Prelude to Annihilation", "毀滅"},
            {"Prelude to Annihilation", "前奏"},
        };
        for (String[] c : cases) {
            String shown = store.lookup(c[0]);
            check("語料查得到「" + c[0] + "」→ " + shown, shown != null);
            check("打「" + c[1] + "」搜得到「" + c[0] + "」",
                    SearchMatch.alsoMatches(c[0], c[1], config, store));
        }
        // 英文照樣能搜：那一關本來就由 Wynntils 自己過，這裡只確認我們沒有
        // 因為「加了中文」而讓英文變得搜不到——我們只在它說沒中時才被問到。
        check("原文沒中而譯文也沒中時，維持沒中",
                !SearchMatch.alsoMatches("Prelude to Annihilation", "zzzz", config, store));
    }

    /** 三件不可以做的事。 */
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

    /**
     * 銀行與背包的搜尋框：打畫面上看得到的中文名，要找得到那件裝備。
     *
     * <p>實機回報：倉庫裡那雙靴子寫著「深淵雨靴」，打「深淵」一格都不亮——
     * Wynntils 拿去比的是它解析出來的 {@code Abysso Galoshes}。
     *
     * <p>這一支的規則跟上面那些畫面<b>不一樣</b>：Wynntils 的
     * {@code itemNameMatches} 用「整串包含」而不是子序列，我們照它的來，
     * 免得玩家覺得「中文跟英文的搜尋方式不一樣」。
     */
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
