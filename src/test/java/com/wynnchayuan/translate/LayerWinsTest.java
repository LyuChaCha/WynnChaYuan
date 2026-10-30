package com.wynnchayuan.translate;

import java.nio.file.Path;
import java.util.List;

public final class LayerWinsTest {

    private static final Path ROOT =
            Path.of("src/main/resources/assets/wynnchayuan/translations");

    public static void main(String[] args) {
        TranslationStore cn = new TranslationStore();
        cn.loadAll(ROOT.resolve("zh_cn"));

        TranslationStore tw = new TranslationStore();
        tw.loadAll(ROOT.resolve("zh_tw"));

        TranslationStore stacked = new TranslationStore();
        stacked.loadAll(List.of(ROOT.resolve("zh_tw"), ROOT.resolve("zh_cn")));

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
                continue;
            }
            String got = path.at(stacked, src);
            if (got == null) {
                continue;
            }
            checked++;
            if (want.equals(got)) {
                continue;
            }
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
