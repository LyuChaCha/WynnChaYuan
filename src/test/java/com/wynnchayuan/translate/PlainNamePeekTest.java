package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 按住 Shift 看原文：素材與材料也要有。
 *
 * <h2>先前缺在哪</h2>
 * 「按住 Shift 看另一種」的做法是暫時把 F6「翻譯物品名稱」撥到另一邊。武器、
 * 防具、飾品、典籍、面向都歸那個開關管，所以有效；素材與材料<b>刻意</b>不歸它管
 * （那個開關是為了讓裝備對得上交易市場），於是撥了也沒反應——名稱永遠是譯名，
 * 要去 wiki 查配方的時候沒有地方看原文。
 *
 * <h2>這裡釘住什麼</h2>
 * 兩個方向都要測，只測一邊的話最省事的寫法都會過：
 * <ul>
 *   <li>按住的時候素材與材料的名稱回到原文；</li>
 *   <li>但 F6 關掉裝備名稱時素材<b>不能</b>跟著變英文（v1.99.71 踩過），
 *       按住的時候裝備、敘述、獎勵列也都不能被牽連。</li>
 * </ul>
 *
 * <p>後半拿出貨的語料驗，而且不寫死任何譯文——要守的是「看不看得到原文」，
 * 不是某個素材一定叫哪三個字。
 */
public final class PlainNamePeekTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-peek");
        Files.writeString(dir.resolve("ingredient.json"), """
                {"_meta": {"itemNames": true, "gearNames": false},
                 "entries": {
                   "i1": {"src": "Acid Magma", "dst": "酸性岩漿", "role": "name"}
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

        // ★ 反方向的守門：F6 關掉裝備名稱，素材不能跟著變英文。
        store.setNameMode(CollectorConfig.ItemNames.OFF);
        check("★ F6 關掉裝備名稱時素材照常翻（實際 " + store.lookup("Acid Magma") + "）",
              "酸性岩漿".equals(store.lookup("Acid Magma")));
        check("而裝備名確實留了原文", store.lookup("Idol") == null);
        store.setNameMode(CollectorConfig.ItemNames.ON);

        // ★ 按住 Shift。
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

        // 「譯名加原文」只替裝備附原文，素材沒有，所以按住一樣要看得到原文。
        store.setNameMode(CollectorConfig.ItemNames.BOTH);
        check("「譯名加原文」模式下按住，素材也看到原文",
              store.lookup("Acid Magma") == null);
        store.setNameMode(CollectorConfig.ItemNames.ON);

        store.setPeekPlainNames(false);
        check("★ 放開之後馬上回到譯名", "酸性岩漿".equals(store.lookup("Acid Magma")));

        shipped();

        if (failures > 0) {
            System.out.println("\n" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("\n全部通過");
    }

    /**
     * 出貨的語料：兩個檔的名稱是不是真的都被收進來了。
     *
     * <p>收不收靠的是檔頭的 {@code itemNames}／{@code gearNames} 兩個旗標，
     * 上面的假資料是自己寫的旗標——真的檔案哪天被改了旗標，上面全綠、
     * 遊戲裡按 Shift 卻沒反應。
     */
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

    /**
     * 整份 tooltip 走一次：名稱那一行在畫面上真的回到原文。
     *
     * <p>查表回 {@code null} 還不等於畫面上是原文——後面還有前綴比對、鬆化、
     * 逐段替換幾條補救的路，任何一條把名字接走，按住 Shift 看到的就還是譯名。
     * 所以拿跟遊戲同一個入口（{@code TooltipPanel#translateLines}）驗最後的字。
     */
    private static void wholeTooltip(TranslationStore store, Path root) throws Exception {
        com.google.gson.JsonObject entries = com.google.gson.JsonParser
                .parseString(Files.readString(root.resolve("ingredient.json")))
                .getAsJsonObject().getAsJsonObject("entries");
        String name = null;
        for (String key : entries.keySet()) {
            String src = entries.getAsJsonObject(key).get("src").getAsString();
            // 挑一個夠長的普通名字，不挑帶佔位符的那一個
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

    /** 第幾行的字；那一行沒有譯文（照原樣畫）時回傳 {@code null}。 */
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
