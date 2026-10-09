package com.wynnchayuan.render;

import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 跟技能同名的裝備，在物品說明的名稱列要用<b>它自己的</b>名字。
 *
 * <h2>為什麼要有</h2>
 * 一般語料一個原文只能一種譯法。{@code Frenzy} 是弓箭手技能「得寸進尺」，
 * 也是一把長矛——那把矛在畫面上就叫「得寸進尺」。這種裝備有五十幾件
 * （使用者 2026-10-09：「46 件也要處理」）。{@code scoped/gear.json} 另外收它們
 * 當物品名稱時的譯名，只在名稱列生效。
 *
 * <h2>這裡釘住什麼</h2>
 * <ul>
 *   <li>物品說明的名稱列用裝備自己的名字；三種名稱模式都照規矩；</li>
 *   <li>技能樹的節點、Lootrun 使命卡、敘述裡提到的同一個字，<b>不</b>變成裝備名；</li>
 *   <li>名稱列以外直接查表，拿到的還是一般語料那一份；</li>
 *   <li>一般語料裡還沒翻、但另外取了名字的裝備（跟介面詞同名的），名稱列也翻得出來。</li>
 * </ul>
 */
public final class GearOwnNameTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        Path dir = Files.createTempDirectory("wynnchayuan-gear-own-name");
        Files.createDirectories(dir.resolve("scoped"));
        Files.createDirectories(dir.resolve("ability"));
        Files.writeString(dir.resolve("gear-weapon.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "w1": {"src": "Frenzy", "dst": "得寸進尺", "role": "name"},
                   "w2": {"src": "Oak Wood Dagger", "dst": "橡木匕首", "role": "name"},
                   "w3": {"src": "Chief", "dst": "", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("gear-armour.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "a1": {"src": "Redemption", "dst": "救贖", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        // 技能、使命、介面詞：跟上面三件裝備同一個原文
        Files.writeString(dir.resolve("ability").resolve("archer.json"), """
                {"_meta": {"itemNames": false},
                 "entries": {
                   "s1": {"src": "Frenzy", "dst": "得寸進尺", "role": "name"},
                   "s2": {"src": "Ability Points:", "dst": "技能點數:", "role": "desc"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("misc.json"), """
                {"Redemption": "救贖",
                 "Chief": "首領",
                 "{#}Redemption": "{#}救贖",
                 "Gain a stack of Frenzy on hit": "命中時獲得一層得寸進尺",
                 "Attack Speed": "攻擊速度"}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("scoped").resolve("gear.json"), """
                {"_meta": {"scope": "gear"},
                 "Frenzy": "狂躁之矛",
                 "Redemption": "救贖護腿",
                 "Chief": "酋長之矛"}
                """, StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        check("裝備專用的名字讀得到", "狂躁之矛".equals(store.gearOwnName("Frenzy")));
        check("沒有另外取名的回 null", store.gearOwnName("Oak Wood Dagger") == null);

        // ---- ★ 物品說明：名稱列用裝備自己的名字 ----
        store.setNameMode(com.wynnchayuan.CollectorConfig.ItemNames.ON);
        List<String> item = shown(item("Frenzy"), store);
        check("★ 看得見的那一行是裝備自己的名字（實際 " + item.get(1) + "）",
                item.get(1).contains("狂躁之矛") && !item.get(1).contains("得寸進尺"));
        check("★ 第 0 行也是（實際 " + item.get(0) + "）", item.get(0).contains("狂躁之矛"));
        check("敘述裡提到的同一個字照舊是技能名（實際 " + item.get(2) + "）",
                item.get(2).contains("得寸進尺") && !item.get(2).contains("狂躁之矛"));

        // ---- ★ 名稱列以外，同一個字還是一般語料那一份 ----
        check("★ 名稱列畫完之後直接查表，拿到的是技能名（實際 " + store.lookup("Frenzy") + "）",
                "得寸進尺".equals(store.lookup("Frenzy")));

        // ---- 譯名加原文 ----
        store.setNameMode(com.wynnchayuan.CollectorConfig.ItemNames.BOTH);
        List<String> both = shown(item("Frenzy"), store);
        check("★ 譯名加原文：附的是英文原名（實際 " + both.get(1) + "）",
                both.get(1).contains("狂躁之矛 (Frenzy)"));
        check("原文只附一次", both.get(1).indexOf("(Frenzy)") == both.get(1).lastIndexOf("(Frenzy)"));
        check("★ 那對括號認得出是我們附的（NameWrap 靠這個把原文挪到下一行）",
                store.appendedOriginalAt("狂躁之矛 (Frenzy)") == "狂躁之矛".length());
        check("別處帶括號的同一個字不算（實際 " + store.appendedOriginalAt("得寸進尺 (Frenzy)") + "）",
                store.appendedOriginalAt("得寸進尺 (Frenzy)") < 0);
        check("沒撞名的裝備照舊附原文",
                shown(item("Oak Wood Dagger"), store).get(1).contains("橡木匕首 (Oak Wood Dagger)"));

        // ---- F6 關掉物品名稱 ----
        store.setNameMode(com.wynnchayuan.CollectorConfig.ItemNames.OFF);
        List<String> off = shown(item("Frenzy"), store);
        check("★ 關掉物品名稱時留原文（實際 " + off.get(1) + "）",
                off.get(1).contains("Frenzy") && !off.get(1).contains("狂躁之矛")
                        && !off.get(1).contains("得寸進尺"));
        store.setNameMode(com.wynnchayuan.CollectorConfig.ItemNames.ON);

        // ---- ★ 一般語料沒翻、但另外取了名字的裝備 ----
        List<String> chief = shown(item("Chief"), store);
        check("★ 跟介面詞同名的裝備：用裝備的名字，不是介面那個詞（實際 " + chief.get(1) + "）",
                chief.get(1).contains("酋長之矛") && !chief.get(1).contains("首領"));

        // ---- ★ 不是物品的面板不受影響 ----
        List<String> node = shown(abilityNode("Frenzy"), store);
        check("★ 技能樹的節點標題還是技能名（實際 " + node.get(0) + "）",
                node.get(0).contains("得寸進尺") && !node.get(0).contains("狂躁之矛"));
        List<String> mission = shown(missionCard("Redemption"), store);
        check("★ Lootrun 使命卡的標題還是使命名（實際 " + mission.get(0) + "）",
                mission.get(0).contains("救贖") && !mission.get(0).contains("護腿"));

        // ---- ★ 真實語料：scoped/gear.json 的每一條都要真的顯示在名稱列 ----
        // 上面是自己造的語料。這裡拿出貨的那一份跑：某一條被別的守門擋掉
        // （名字像玩家資料、跟整列條目撞到…）只有這樣才看得出來。不寫死任何譯名。
        //
        // 六個語言用同一個 store 重載：TooltipPanel 的快取認的是「同樣的內容＋
        // 同一個 generation」，各開一個新的 store 的話每個都是第 1 代，後面五個語言
        // 會直接拿到繁中那一輪的結果（實機只有一個 store，不會遇到）。先載一次上面
        // 那份自己造的語料，讓真實語料從第 2 代開始，也不跟上面那個 store 撞。
        TranslationStore rs = new TranslationStore();
        rs.loadAll(dir);
        for (String lang : List.of("zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es")) {
            Path real = Path.of("src/main/resources/assets/wynnchayuan/translations", lang);
            Path file = real.resolve("scoped").resolve("gear.json");
            if (!Files.exists(file)) {
                check("真實語料 " + lang + "：scoped/gear.json 在", false);
                continue;
            }
            rs.loadAll(real);
            rs.setNameMode(com.wynnchayuan.CollectorConfig.ItemNames.ON);
            com.google.gson.JsonObject own = com.google.gson.JsonParser.parseString(
                    Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            int total = 0;
            List<String> missed = new ArrayList<>();
            List<String> leaked = new ArrayList<>();
            for (String key : own.keySet()) {
                if (key.startsWith("_")) {
                    continue;
                }
                total++;
                String name = own.get(key).getAsString();
                List<String> rows = shown(item(key), rs);
                if (!rows.get(1).contains(name) || !rows.get(0).contains(name)) {
                    missed.add(key + "→" + rows.get(1));
                }
                // 技能樹的節點不能變成裝備名。裝備的名字本來就跟技能那個一樣的
                // （Dancing Blade 兩邊都是「舞動之刃」那一類）看不出差別，不算。
                String title = shown(abilityNode(key), rs).get(0);
                if (title.contains(name) && !name.equals(rs.lookup(key))) {
                    leaked.add(key + "→" + title);
                }
            }
            check("★ 真實語料 " + lang + "：" + total + " 條裝備專用名都顯示在名稱列（沒顯示的 "
                    + missed + "）", total > 0 && missed.isEmpty());
            check("★ 真實語料 " + lang + "：技能樹節點沒有變成裝備名（變了的 " + leaked + "）",
                    leaked.isEmpty());
        }

        if (failures > 0) {
            System.err.println(failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("GearOwnNameTest 全部通過");
        System.exit(0);
    }

    // ------------------------------------------------------------ 造 tooltip

    private static final Style LANG = font("minecraft:language/wynncraft");
    private static final Style FRAME = font("minecraft:tooltip/emblem/frame");
    private static final Style SPACE = font("minecraft:space");

    private static Style font(String id) {
        return Style.EMPTY.withFont(new FontDescription.Resource(Identifier.parse(id)));
    }

    private static Component icon(int cp, Style style) {
        return Component.literal(new String(Character.toChars(cp))).withStyle(style);
    }

    /** 照實機的分段：第 0 行寬度是 0 的名稱、第 1 行五個圖示＋名稱＋鑑定度、第 2 行敘述。 */
    private static List<Component> item(String name) {
        List<Component> rows = new ArrayList<>();
        MutableComponent hidden = Component.empty();
        hidden.append(icon(0xCF000, SPACE));
        hidden.append(Component.literal(name).withStyle(s -> s.withColor(0x55FFFF)));
        hidden.append(icon(0xCF000, SPACE));
        rows.add(hidden);
        MutableComponent seen = Component.empty();
        seen.append(icon(0xCFFF0, LANG));
        seen.append(icon(0xE040, FRAME));
        seen.append(icon(0xCFFCF, LANG));
        seen.append(icon(0xE006, FRAME));
        seen.append(icon(0xD0005, LANG));
        seen.append(Component.literal(name).withStyle(LANG.withColor(0x55FFFF)));
        seen.append(Component.literal(" [58.2%]").withStyle(LANG.withColor(0xFFDD33)));
        rows.add(seen);
        rows.add(Component.literal("Gain a stack of Frenzy on hit")
                .withStyle(s -> s.withColor(0xAAAAAA)));
        rows.add(Component.literal("Attack Speed").withStyle(s -> s.withColor(0xAAAAAA)));
        return rows;
    }

    /** 技能樹的節點：標題一行，底下有「Ability Points:」。 */
    private static List<Component> abilityNode(String name) {
        List<Component> rows = new ArrayList<>();
        MutableComponent title = Component.empty();
        title.append(icon(0xCF000, SPACE));
        title.append(Component.literal(name).withStyle(s -> s.withColor(0x55FF55)));
        title.append(icon(0xCF000, SPACE));
        rows.add(title);
        rows.add(Component.literal("Gain a stack of Frenzy on hit")
                .withStyle(s -> s.withColor(0xAAAAAA)));
        rows.add(Component.literal("Ability Points:").withStyle(s -> s.withColor(0xAAAAAA)));
        return rows;
    }

    /** Lootrun 使命卡：標題只有開頭一個圖示。 */
    private static List<Component> missionCard(String name) {
        List<Component> rows = new ArrayList<>();
        MutableComponent title = Component.empty();
        title.append(icon(0xE040, FRAME));
        title.append(Component.literal(name).withStyle(s -> s.withColor(0xFFAA00)));
        rows.add(title);
        rows.add(Component.literal("Attack Speed").withStyle(s -> s.withColor(0xAAAAAA)));
        return rows;
    }

    /** 翻完的每一行；圖示字元換成 # 好讀。沒翻到的行回原文。 */
    private static List<String> shown(List<Component> rows, TranslationStore store) {
        List<Component> out = TooltipPanel.translateInPlace(rows, store);
        List<String> text = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Component line = i < out.size() ? out.get(i) : rows.get(i);
            StringBuilder sb = new StringBuilder();
            line.getString().codePoints().forEach(cp -> sb.appendCodePoint(
                    cp >= 0xE000 && cp <= 0xF8FF || cp >= 0xC0000 ? '#' : cp));
            text.add(sb.toString());
        }
        return text;
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) {
            failures++;
        }
    }
}
