package com.wynnchayuan.translate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Wynnventory 的文字：價格框裡寫死的字、語言鍵那一層、按住 Shift、沒裝的人。
 *
 * <h2>這裡釘住什麼</h2>
 * <ul>
 *   <li>價格框：物品名、{@code No data yet.}、數值名、意象名換成譯文；價格、百分比、
 *       樣式（顏色）一個都不動；沒有東西可換的那一行回<b>同一個物件</b>；</li>
 *   <li>語言鍵：Wynnventory 的鍵照它的英文查我們的表，頭尾空白接回去
 *       （{@code Lowest: } 後面直接接價格）；別人的鍵原樣；</li>
 *   <li>不翻的時候（開關關著或按住 Shift）兩條路都原樣；</li>
 *   <li>沒裝 Wynnventory：語言不包；</li>
 *   <li>出貨的六個語言：Wynnventory 語言檔的每一句都有譯文，{@code %s} 與換行的數目對得上。</li>
 * </ul>
 */
public final class WynnventoryBridgeTest {

    private static int failures = 0;

    /** Wynnventory 2.2.5 的 en_us.json（CC0），發版時對過一次；它加了新句子這裡會先知道。 */
    private static final Path THEIR_LANG = Path.of("src/test/resources/wynnventory-en_us.json");

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        Path dir = Files.createTempDirectory("wynnchayuan-wynnventory");
        Files.createDirectories(dir.resolve("scoped"));
        Files.writeString(dir.resolve("gear-weapon.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "w1": {"src": "Oak Wood Dagger", "dst": "橡木匕首", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("misc.json"), """
                {"Walk Speed": "移動速度",
                 "Aspect of the Trampling Hooves": "踐踏之蹄意象",
                 "Lowest:": "不該用到這一條"}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("scoped").resolve("wynnventory.json"), """
                {"_meta": {"scope": "wynnventory"},
                 "Lowest: ": "最低: ",
                 "Trade Market Price Info": "交易市場價格資訊",
                 "No data yet.": "還沒有資料。",
                 "Mythic Aspects": "神話意象",
                 "Aspects": "意象",
                 "Mythic": "神話",
                 "%s in %s": "%s，在 %s",
                 "%s more...": "還有 %s 件...",
                 "New version available: %s. Attempting to auto-update...": "有新版本: %s。正在嘗試自動更新..."}
                """, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        store.setNameMode(com.wynnchayuan.CollectorConfig.ItemNames.ON);

        // ---- 一句英文 → 譯文 ----
        check("標籤：尾巴那個空白接回去（實際 [" + WynnventoryBridge.text(store, "Lowest: ") + "]）",
                "最低: ".equals(WynnventoryBridge.text(store, "Lowest: ")));
        check("表裡沒有的回 null", WynnventoryBridge.text(store, "Highest: ") == null);
        check("%s 留著", "有新版本: %s。正在嘗試自動更新...".equals(WynnventoryBridge.text(store,
                "New version available: %s. Attempting to auto-update...")));

        // ---- ★ 價格框 ----
        List<Component> box = new ArrayList<>();
        box.add(Component.literal("Trade Market Price Info")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        box.add(Component.literal("Oak Wood Dagger").withStyle(s -> s.withColor(0x55FFFF)));
        MutableComponent price = Component.literal("最低: ").withStyle(ChatFormatting.WHITE);
        price.append(Component.literal("12.5 LE").withStyle(ChatFormatting.GRAY));
        price.append(Component.literal(" (+8%)").withStyle(ChatFormatting.GREEN));
        box.add(price);
        MutableComponent factor = Component.literal("  ");
        factor.append(Component.literal("Walk Speed").withStyle(ChatFormatting.WHITE));
        factor.append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY));
        factor.append(Component.literal("87%").withStyle(ChatFormatting.GRAY));
        factor.append(Component.literal(" (").withStyle(ChatFormatting.DARK_GRAY));
        factor.append(Component.literal("+3 EB").withStyle(ChatFormatting.GREEN));
        factor.append(Component.literal(")").withStyle(ChatFormatting.DARK_GRAY));
        box.add(factor);
        box.add(Component.literal("No data yet.").withStyle(ChatFormatting.RED));
        box.add(Component.empty());
        box.add(Component.literal("•  Aspect of the Trampling Hooves")
                .withStyle(ChatFormatting.GRAY));

        List<Component> out = WynnventoryBridge.lines(store, box);
        check("★ 標題換了（實際 " + out.get(0).getString() + "）",
                "交易市場價格資訊".equals(out.get(0).getString()));
        check("★ 物品名換了（實際 " + out.get(1).getString() + "）",
                "橡木匕首".equals(out.get(1).getString()));
        check("★ 物品名的顏色還在", colours(out.get(1)).contains(0x55FFFF));
        check("★ 價格那一行沒有東西可換：回同一個物件", out.get(2) == box.get(2));
        check("★ 數值名換了，冒號、百分比、價格原樣（實際 " + out.get(3).getString() + "）",
                "  移動速度: 87% (+3 EB)".equals(out.get(3).getString()));
        check("★ 每一段的顏色都還在原位（實際 " + colours(out.get(3)) + "）",
                colours(out.get(3)).equals(colours(box.get(3))));
        check("★ No data yet. 換了，紅色還在（實際 " + out.get(4).getString() + "）",
                "還沒有資料。".equals(out.get(4).getString())
                        && colours(out.get(4)).equals(colours(box.get(4))));
        check("空行原樣", out.get(5) == box.get(5));
        check("★ 意象清單：圓點與職業圖示留在前面（實際 " + out.get(6).getString() + "）",
                "•  踐踏之蹄意象".equals(out.get(6).getString()));
        check("沒有東西可換的整份清單回同一個物件",
                WynnventoryBridge.lines(store, List.of(box.get(2))) != null
                        && WynnventoryBridge.lines(store, List.of(box.get(2))).get(0) == box.get(2));

        // 價格不能被當成名字查：一般語料裡就算剛好有同樣的字也不行
        check("數字開頭的不查（實際 " + WynnventoryBridge.piece(store, "12 Lowest:") + "）",
                WynnventoryBridge.piece(store, "12 Lowest:") == null);
        check("已經是譯文的不查", WynnventoryBridge.piece(store, "最低: ") == null);

        // 關掉物品名稱的人：價格框的物品名也留原文
        store.setNameMode(com.wynnchayuan.CollectorConfig.ItemNames.OFF);
        check("★ F6 關掉物品名稱時，價格框的物品名留原文（實際 "
                        + WynnventoryBridge.lines(store, box).get(1).getString() + "）",
                "Oak Wood Dagger".equals(WynnventoryBridge.lines(store, box).get(1).getString()));
        store.setNameMode(com.wynnchayuan.CollectorConfig.ItemNames.ON);

        // ---- ★ 語言那一層 ----
        Map<String, String> english = new HashMap<>();
        english.put("feature.wynnventory.tooltip.lowest", "Lowest: ");
        english.put("gui.wynnventory.reward.raid", "Raid");
        english.put("gui.done", "Done");
        Language base = new Fake(english);

        WynnventoryBridge.pretend(false, true);
        check("★ 沒裝 Wynnventory：語言不包", WynnventoryBridge.wrap(base) == base);

        WynnventoryBridge.pretend(true, true);
        Language wrapped = WynnventoryBridge.wrap(base);
        check("裝了：包一層", wrapped != base);
        check("★ 再包一次是新的物件（TranslatableContents 靠這個知道要重查）",
                WynnventoryBridge.wrap(wrapped) != wrapped);
        check("別人的鍵原樣", "Done".equals(wrapped.getOrDefault("gui.done", "?")));
        check("has 照轉", wrapped.has("gui.done") && !wrapped.has("nope"));
        check("認得哪些鍵是它的", WynnventoryBridge.ours("key.category.wynnventory.root")
                && WynnventoryBridge.ours("feature.wynnventory.tooltip.avg")
                && !WynnventoryBridge.ours("gui.done") && !WynnventoryBridge.ours(null));
        // lang() 用的是全域那份語料（遊戲裡才有），這裡驗它在沒有語料時原樣放行
        check("沒有語料可查的時候原樣（不炸）",
                "Lowest: ".equals(wrapped.getOrDefault("feature.wynnventory.tooltip.lowest", "?")));

        // ---- ★ 通知底下那一行 ----
        check("★ 通知：物品名換了、顏色碼與獎勵池的名字原樣（實際 "
                        + WynnventoryBridge.toast(store, "\u00a75Oak Wood Dagger\u00a7f in NOTG") + "）",
                "\u00a75橡木匕首\u00a7f，在 NOTG".equals(
                        WynnventoryBridge.toast(store, "\u00a75Oak Wood Dagger\u00a7f in NOTG")));
        check("★ 通知：語料沒有的物品名留著，句子照翻（實際 "
                        + WynnventoryBridge.toast(store, "Zzyzx in Sky Islands") + "）",
                "Zzyzx，在 Sky Islands".equals(WynnventoryBridge.toast(store, "Zzyzx in Sky Islands")));
        check("★ 通知：還有幾件（實際 " + WynnventoryBridge.toast(store, "3 more...") + "）",
                "還有 3 件...".equals(WynnventoryBridge.toast(store, "3 more...")));
        check("不是這兩種句子的不動", WynnventoryBridge.toast(store, "Hello there") == null);

        // ---- ★ 不翻的時候 ----
        // label／word 用的是全域那份語料，遊戲裡才有；這裡驗沒有語料時原樣放行、不炸
        WynnventoryBridge.pretend(true, true);
        check("沒有語料可查：標籤回 null（照它原本的畫）",
                WynnventoryBridge.label(Component.literal("Aspects")) == null);
        check("沒有語料可查：篩選名原樣", "Mythic".equals(WynnventoryBridge.word("Mythic")));
        WynnventoryBridge.pretend(true, false);
        check("★ 開關關著／按住 Shift：標籤照它原本的畫",
                WynnventoryBridge.label(Component.literal("Aspects")) == null);
        check("★ 開關關著／按住 Shift：篩選名原樣", "Mythic".equals(WynnventoryBridge.word("Mythic")));
        Component toast = Component.literal("3 more...");
        check("★ 開關關著／按住 Shift：通知原樣（同一個物件）", WynnventoryBridge.toast(toast) == toast);
        check("★ 開關關著／按住 Shift：價格框原樣（同一個物件）",
                WynnventoryBridge.lines(box) == box);
        check("★ 開關關著／按住 Shift：語言鍵原樣",
                "Lowest: ".equals(WynnventoryBridge.lang("feature.wynnventory.tooltip.lowest",
                        "Lowest: ")));

        // ---- ★ 出貨的語料 ----
        JsonObject theirs = JsonParser.parseString(
                Files.readString(THEIR_LANG, StandardCharsets.UTF_8)).getAsJsonObject();
        List<String> sentences = new ArrayList<>();
        for (String key : theirs.keySet()) {
            check("它的鍵都認得出來：" + key, WynnventoryBridge.ours(key));
            sentences.add(theirs.get(key).getAsString());
        }
        sentences.addAll(List.of("No data yet.", "Mythic Aspects", "Filters",
                // 獎勵畫面的分區標題與篩選名、通知的兩種句子：寫死在它的程式裡
                "Aspects", "Tomes", "Gear", "Misc", "Mythic", "Fabled", "Legendary", "Rare",
                "Unique", "Normal", "Common", "Set", "%s in %s", "%s more..."));
        for (String lang : List.of("zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es")) {
            Path real = Path.of("src/main/resources/assets/wynnchayuan/translations", lang);
            TranslationStore rs = new TranslationStore();
            rs.loadAll(real);
            List<String> missing = new ArrayList<>();
            List<String> broken = new ArrayList<>();
            for (String s : sentences) {
                String hit = WynnventoryBridge.text(rs, s);
                if (hit == null) {
                    // 譯文跟原文一樣的（Wynnventory、某些語言的 Lootrun）不必收
                    if (!SAME_IN_EVERY_LANGUAGE.contains(s.strip())
                            && rs.scopedLookup(WynnventoryBridge.SCOPE, s) == null) {
                        missing.add(s);
                    }
                    continue;
                }
                if (count(hit, "%s") != count(s, "%s") || count(hit, "\n") != count(s, "\n")
                        || !hit.startsWith(lead(s)) || !hit.endsWith(trail(s))) {
                    broken.add(s);
                }
            }
            check("★ " + lang + "：Wynnventory 的每一句都有譯文（缺 " + missing + "）",
                    missing.isEmpty());
            check("★ " + lang + "：%s、換行、頭尾空白都對得上（不對的 " + broken + "）",
                    broken.isEmpty());
        }

        if (failures > 0) {
            System.err.println(failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("WynnventoryBridgeTest 全部通過");
        System.exit(0);
    }

    /** 專有名詞，哪個語言都不翻。 */
    private static final List<String> SAME_IN_EVERY_LANGUAGE = List.of("Wynnventory", "Lootrun");

    private static int count(String text, String what) {
        int n = 0;
        for (int i = text.indexOf(what); i >= 0; i = text.indexOf(what, i + what.length())) {
            n++;
        }
        return n;
    }

    private static String lead(String s) {
        return s.substring(0, s.length() - s.stripLeading().length());
    }

    private static String trail(String s) {
        return s.substring(s.stripTrailing().length());
    }

    /** 一行裡每一段的顏色，照順序；沒上色的記 -1。 */
    private static List<Integer> colours(Component line) {
        List<Integer> out = new ArrayList<>();
        line.visit((style, text) -> {
            out.add(style.getColor() == null ? -1 : style.getColor().getValue());
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static final class Fake extends Language {
        private final Map<String, String> map;

        Fake(Map<String, String> map) {
            this.map = map;
        }

        @Override
        public String getOrDefault(String key, String defaultValue) {
            return map.getOrDefault(key, defaultValue);
        }

        @Override
        public boolean has(String key) {
            return map.containsKey(key);
        }

        @Override
        public boolean isDefaultRightToLeft() {
            return false;
        }

        @Override
        public FormattedCharSequence getVisualOrder(FormattedText text) {
            return FormattedCharSequence.EMPTY;
        }
    }

    private static void check(String what, boolean ok) {
        if (!ok || !what.startsWith("它的鍵都認得出來")) {
            System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        }
        if (!ok) {
            failures++;
        }
    }
}
