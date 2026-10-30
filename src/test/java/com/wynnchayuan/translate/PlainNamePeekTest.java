package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class PlainNamePeekTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-peek");
        Files.writeString(dir.resolve("ingredient.json"), """
                {"_meta": {"itemNames": true, "gearNames": false},
                 "entries": {
                   "i1": {"src": "Acid Magma", "dst": "酸性岩漿", "role": "name"},
                   "i2": {"src": "Black Hole", "dst": "黑洞", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("material.json"), """
                {"_meta": {"itemNames": true, "gearNames": false},
                 "entries": {
                   "m1": {"src": "Copper Ingot", "dst": "銅錠", "role": "name"},
                   "m2": {"src": "Used to craft weapons", "dst": "用來製作武器", "role": "desc"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("gear-weapon.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {"g1": {"src": "Idol", "dst": "神像", "role": "name"}}}
                """, StandardCharsets.UTF_8);
        Files.createDirectories(dir.resolve("ability"));
        Files.writeString(dir.resolve("ability").resolve("assassin.json"), """
                {"_meta": {"itemNames": false},
                 "entries": {"s1": {"src": "Black Hole", "dst": "黑洞", "role": "name"}}}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("misc.json"),
                "{\"[+{~} Acid Magma]\": \"[+{~} 酸性岩漿]\"}", StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        check("平常素材照常翻（實際 " + store.lookup("Acid Magma") + "）",
              "酸性岩漿".equals(store.lookup("Acid Magma")));
        check("平常材料照常翻", "銅錠".equals(store.lookup("Copper Ingot")));
        check("認得出素材與材料的名稱",
              store.isPlainName("Acid Magma") && store.isPlainName("Copper Ingot"));
        check("裝備名與敘述不算在內",
              !store.isPlainName("Idol") && !store.isPlainName("Used to craft weapons"));

        store.setNameMode(CollectorConfig.ItemNames.OFF);
        check("★ F6 關掉裝備名稱時素材照常翻（實際 " + store.lookup("Acid Magma") + "）",
              "酸性岩漿".equals(store.lookup("Acid Magma")));
        check("而裝備名確實留了原文", store.lookup("Idol") == null);
        store.setNameMode(CollectorConfig.ItemNames.ON);

        store.setPeekPlainNames(true);
        check("★ 按住時素材看到原文（實際 " + store.lookup("Acid Magma") + "）",
              store.lookup("Acid Magma") == null);
        check("★ 按住時材料看到原文", store.lookup("Copper Ingot") == null);
        check("按住時材料的敘述照常翻",
              "用來製作武器".equals(store.lookup("Used to craft weapons")));
        check("按住時獎勵列那種框架照常翻——那是另一個鍵",
              "[+{~} 酸性岩漿]".equals(store.lookup("[+{~} Acid Magma]")));
        check("按住時裝備名不受這個旗標影響（它走名稱模式那條路）",
              "神像".equals(store.lookup("Idol")));

        store.setNameMode(CollectorConfig.ItemNames.BOTH);
        check("「譯名加原文」模式下按住，素材也看到原文",
              store.lookup("Acid Magma") == null);
        store.setNameMode(CollectorConfig.ItemNames.ON);

        store.setPeekPlainNames(false);

        store.setNameMode(CollectorConfig.ItemNames.BOTH);
        check("★ 譯名加原文：素材附原文（實際 " + store.lookup("Acid Magma") + "）",
              "酸性岩漿 (Acid Magma)".equals(store.lookup("Acid Magma")));
        check("★ 譯名加原文：材料附原文（實際 " + store.lookup("Copper Ingot") + "）",
              "銅錠 (Copper Ingot)".equals(store.lookup("Copper Ingot")));
        check("★ 那對括號認得出是我們附的（折行、詞表替換都靠這個不去動它）",
              store.appendedOriginalAt("酸性岩漿 (Acid Magma)") == "酸性岩漿".length());
        check("材料的敘述不附", "用來製作武器".equals(store.lookup("Used to craft weapons")));
        check("嵌著素材名的框架不附——那是另一個鍵",
              "[+{~} 酸性岩漿]".equals(store.lookup("[+{~} Acid Magma]")));
        check("★ 同名的技能不能被連坐：素材 Black Hole 也是刺客的技能，不附（實際 "
                + store.lookup("Black Hole") + "）",
              "黑洞".equals(store.lookup("Black Hole")));
        try (LookupScope scope = LookupScope.holdAppendedOriginal(true)) {
            check("★ 不是名稱那一行（敘述裡提到同一個名字）不附",
                  "酸性岩漿".equals(store.lookup("Acid Magma")));
        }
        store.setNameMode(CollectorConfig.ItemNames.OFF);
        check("★ 關掉物品名稱：素材照舊是譯名，不附原文（實際 " + store.lookup("Acid Magma") + "）",
              "酸性岩漿".equals(store.lookup("Acid Magma")));
        store.setNameMode(CollectorConfig.ItemNames.ON);
        check("只給譯名的模式不附", "酸性岩漿".equals(store.lookup("Acid Magma")));
        check("★ 放開之後馬上回到譯名", "酸性岩漿".equals(store.lookup("Acid Magma")));

        shipped();

        if (failures > 0) {
            System.out.println("\n" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("\n全部通過");
    }

    private static void shipped() throws Exception {
        Path root = Paths.get("src/main/resources/assets/wynnchayuan/translations/zh_tw");
        if (!Files.isDirectory(root)) {
            System.out.println("  [SKIP] 找不到出貨的語料，跳過");
            return;
        }
        TranslationStore store = new TranslationStore();
        store.loadAll(List.of(root));

        for (String file : List.of("ingredient.json", "material.json")) {
            com.google.gson.JsonObject entries = com.google.gson.JsonParser
                    .parseString(Files.readString(root.resolve(file)))
                    .getAsJsonObject().getAsJsonObject("entries");
            int names = 0;
            int missed = 0;
            int stillTranslated = 0;
            String example = null;
            for (String key : entries.keySet()) {
                com.google.gson.JsonObject e = entries.getAsJsonObject(key);
                if (!e.has("role") || !"name".equals(e.get("role").getAsString())
                        || e.get("dst").getAsString().isBlank()) {
                    continue;
                }
                String src = e.get("src").getAsString();
                names++;
                if (!store.isPlainName(src)) {
                    missed++;
                    example = src;
                    continue;
                }
                store.setPeekPlainNames(true);
                if (store.lookup(src) != null) {
                    stillTranslated++;
                    example = src;
                }
                store.setPeekPlainNames(false);
            }
            check("★ " + file + " 的 " + names + " 個名稱全部收到（漏 " + missed
                  + (example == null ? "" : "，例如 " + example) + "）",
                  names > 0 && missed == 0);
            check("★ " + file + " 按住時全部看到原文（還是譯名的 " + stillTranslated + " 個）",
                  stillTranslated == 0);
        }
        wholeTooltip(store, root);
    }

    private static void wholeTooltip(TranslationStore store, Path root) throws Exception {
        com.google.gson.JsonObject entries = com.google.gson.JsonParser
                .parseString(Files.readString(root.resolve("ingredient.json")))
                .getAsJsonObject().getAsJsonObject("entries");
        String name = null;
        for (String key : entries.keySet()) {
            String src = entries.getAsJsonObject(key).get("src").getAsString();
            if (src.length() >= 10 && src.indexOf('{') < 0) {
                name = src;
                break;
            }
        }
        if (name == null) {
            check("找得到一個素材名來測整份 tooltip", false);
            return;
        }
        net.minecraft.network.chat.Style white = net.minecraft.network.chat.Style.EMPTY
                .withColor(net.minecraft.network.chat.TextColor.fromRgb(0xFFFFFF));
        List<net.minecraft.network.chat.Component> lines = List.of(
                net.minecraft.network.chat.Component.literal(name).withStyle(white),
                net.minecraft.network.chat.Component.literal("Crafting Ingredient")
                        .withStyle(white));
        LineTranslator.measureForTest = c -> c.getString().length() * 6;
        try {
            String shown = text(com.wynnchayuan.render.TooltipPanel
                    .translateLines(lines, store), 0);
            check("★ 平常整份 tooltip 的名稱是譯名（" + name + " -> " + shown + "）",
                  shown != null && !shown.contains(name));
            store.setPeekPlainNames(true);
            String peek = text(com.wynnchayuan.render.TooltipPanel
                    .translateLines(lines, store), 0);
            store.setPeekPlainNames(false);
            check("★ 按住時整份 tooltip 的名稱是原文（實際 " + peek + "）",
                  peek == null || peek.contains(name));
        } finally {
            LineTranslator.measureForTest = null;
        }
    }

    private static String text(List<net.minecraft.network.chat.Component> out, int at) {
        return at < out.size() && out.get(at) != null ? out.get(at).getString() : null;
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + name);
        if (!ok) {
            failures++;
        }
    }
}
