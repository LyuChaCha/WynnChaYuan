package com.wynnchayuan.translate;

import java.nio.file.Path;
import java.util.List;

/**
 * 疊層之後，<b>上面那一層</b>翻好的每一條都要贏。
 *
 * <h2>為什麼要全庫掃</h2>
 * {@link LanguageFallbackTest} 釘的是「墊底那一層還在」，用的是三條手挑的
 * 樣本。但實機回報的是另一個方向：<b>簡體明明翻好了，畫面上卻是繁體</b>。
 *
 * <p>那種洞是一個索引一個索引出現的——{@code flat} 修好了，{@code marked}
 * 還漏著；短句對了，長句錯了。手挑樣本永遠追不上，所以這一條直接拿
 * <b>簡體翻好的每一條</b>去問，有一條輸給繁體就紅燈。
 */
public final class LayerWinsTest {

    private static final Path ROOT =
            Path.of("src/main/resources/assets/wynnchayuan/translations");

    public static void main(String[] args) {
        TranslationStore cn = new TranslationStore();
        cn.loadAll(ROOT.resolve("zh_cn"));

        TranslationStore tw = new TranslationStore();
        tw.loadAll(ROOT.resolve("zh_tw"));

        // 畫面上的排法：簡體在上、繁體墊底
        TranslationStore stacked = new TranslationStore();
        stacked.loadAll(List.of(ROOT.resolve("zh_tw"), ROOT.resolve("zh_cn")));

        // 長句走的不是主查表，而是這幾個正規化過的輔助索引。每一個都要單獨問
        // 一次——先前 flat 修好了、marked 還漏著，就是只問主查表看不出來的。
        int failures = 0;
        failures += sweep("主查表 lookup", cn, tw, stacked,
                          TranslationStore::lookup, TranslationStore::lookup);
        failures += sweep("攤平索引 lookupFlat", cn, tw, stacked,
                          TranslationStore::lookup, TranslationStore::lookupFlat);
        failures += sweep("去折行索引 lookupUnwrapped", cn, tw, stacked,
                          TranslationStore::lookup, TranslationStore::lookupUnwrapped);

        failures += variants();

        System.out.println(failures == 0
                ? "疊層優先序：全部通過" : "疊層優先序：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /**
     * 上面那一層翻的是<b>另一個寫法</b>的同一句話。
     *
     * <p>全庫掃描問的鍵本身就是簡體翻好的那一條，所以問不到這一種：畫面上的字
     * 在繁體有精確的譯文、在簡體只有「只差標點或大小寫」的那一條。先前精確查表
     * 先命中繁體，簡體那條永遠輪不到。逐字對話的校訂版替換也是一樣。
     */
    private static int variants() {
        int lost = 0;
        try {
            Path tw = java.nio.file.Files.createTempDirectory("layer-tw");
            Path cn = java.nio.file.Files.createTempDirectory("layer-cn");
            java.nio.file.Files.writeString(tw.resolve("quest.json"), """
                    {"Waiting for Others..": "等待其他人……",
                     "Not yet, my good friend!": "還沒啦",
                     "Not yet, my good friend?": "還沒嗎",
                     "Hey, {u}! Are you alright in there? It looks like we've hit something.": "嘿，{u}！你在裡面還好嗎？"}
                    """);
            java.nio.file.Files.writeString(cn.resolve("quest.json"), """
                    {"Waiting for others": "等待其他人",
                     "Not yet, my good friend!": "还没呢"}
                    """);
            java.nio.file.Files.writeString(cn.resolve("npc.json"), """
                    {"entries": {"a": {
                      "src": "Hey, {u}! You alright in there? Looks like we hit something.",
                      "dst": "嘿，{u}！你在里面还好吗？",
                      "quest": "Layer Test", "source": "wiki"}}}
                    """);

            TranslationStore stacked = new TranslationStore();
            stacked.loadAll(List.of(tw, cn));

            lost += expect("只差標點與大小寫時用簡體",
                    "等待其他人", stacked.lookup("Waiting for Others.."));
            lost += expect("繁體層內撞鍵不會把簡體那條關掉",
                    "还没呢", stacked.lookup("NOT YET, MY GOOD FRIEND"));
            lost += expect("逐字對話不換到只有繁體翻的校訂版",
                    "Hey, {u}! You alright in there? Looks like we hit something.",
                    stacked.matchPrefix("Hey, {u}!", 9, "Layer Test"));

            // 反面：只有一層時行為照舊
            TranslationStore alone = new TranslationStore();
            alone.loadAll(tw);
            lost += expect("單層時精確鍵照舊",
                    "等待其他人……", alone.lookup("Waiting for Others.."));
        } catch (java.io.IOException e) {
            System.out.println("  [FAIL] 建立暫存語料失敗：" + e.getMessage());
            lost++;
        }
        return lost;
    }

    private static int expect(String what, String want, String got) {
        boolean ok = want.equals(got);
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what
                + (ok ? "" : "（要 " + want + "，實際 " + got + "）"));
        return ok ? 0 : 1;
    }

    private interface Look {
        String at(TranslationStore store, String src);
    }

    /**
     * 拿簡體翻好、而且跟繁體翻得不一樣的每一條，去問疊完之後的那一份。
     *
     * @param own  怎麼問「這一層自己」的譯文
     * @param path 怎麼問疊完那一份——要測的就是這一條路
     */
    private static int sweep(String what, TranslationStore cn, TranslationStore tw,
                             TranslationStore stacked, Look own, Look path) {
        int checked = 0;
        int lost = 0;
        StringBuilder first = new StringBuilder();
        for (String src : cn.sourceKeys()) {
            String want = own.at(cn, src);
            if (want == null || want.isBlank()) {
                continue;
            }
            String twOwn = own.at(tw, src);
            if (twOwn == null || twOwn.equals(want)) {
                continue;      // 繁體沒翻，或兩邊一模一樣——分不出勝負
            }
            String got = path.at(stacked, src);
            if (got == null) {
                continue;      // 這條路本來就不收這一種鍵
            }
            checked++;
            if (want.equals(got)) {
                continue;
            }
            // 攤平／去折行這兩個索引會把換行壓掉，於是<b>同一層裡</b>兩條不同的
            // 原文可能正規化成同一個鍵。那是既有的「層內先到先贏」，不是這條
            // 測試要抓的東西——只有拿到<b>繁體那一份</b>才算輸。
            if (!got.equals(path.at(tw, src))) {
                continue;
            }
            lost++;
            if (lost <= 3) {
                first.append("\n      原文 ").append(oneLine(src))
                     .append("\n      簡體 ").append(oneLine(want))
                     .append("\n      實際 ").append(oneLine(got));
            }
        }
        System.out.println("  [" + (lost == 0 ? "PASS" : "FAIL") + "] " + what
                + "：走這條路的 " + checked + " 條，輸給繁體 " + lost + " 條" + first);
        return lost == 0 ? 0 : 1;
    }

    private static String oneLine(String s) {
        if (s == null) {
            return "(查不到)";
        }
        String out = s.replace("\n", " ⏎ ");
        return out.length() > 70 ? out.substring(0, 70) + "…" : out;
    }
}
