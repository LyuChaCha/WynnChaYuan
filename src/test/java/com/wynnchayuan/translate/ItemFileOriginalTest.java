package com.wynnchayuan.translate;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.render.TooltipPanel;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class ItemFileOriginalTest {

    private static final String ICON = String.valueOf((char) 0xE000);

    private static final String TAIL = String.valueOf((char) 0xE001);

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        Path dir = Files.createTempDirectory("wynnchayuan-item-original");
        Files.writeString(dir.resolve("ingredient.json"), """
                {"_meta": {"itemNames": true, "gearNames": false},
                 "entries": {
                   "i1": {"src": "Void Shard", "dst": "虛空碎片", "role": "name"},
                   "i2": {"src": "Acid Magma", "dst": "酸性岩漿", "role": "name"},
                   "i3": {"src": "Hiker's Journal", "dst": "旅人手記", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("gear-armour.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "a1": {"src": "Hero's Mask", "dst": "英雄面具", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.createDirectories(dir.resolve("ability"));
        Files.writeString(dir.resolve("ability").resolve("assassin.json"), """
                {"_meta": {"itemNames": false},
                 "entries": {"s1": {"src": "Void Shard", "dst": "虛空碎片", "role": "name"}}}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("npc.json"), """
                {"Journal": "日誌", "Mask": "假面"}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("misc.json"), """
                {"{owner}'s {thing}": "{owner} 的{thing}"}
                """, StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        String shard = store.lookupExact("Void Shard");
        String acid = store.lookupExact("Acid Magma");
        String journal = store.lookupExact("Hiker's Journal");
        String mask = store.lookupExact("Hero's Mask");
        check("假語料載入了", shard != null && acid != null && journal != null && mask != null);

        same("第 0 行的模板", "{#}Void Shard{#}", template(hidden("Void Shard")));
        same("第 1 行的模板", "{#}{#}{#}{#}{#}Void Shard", template(shown("Void Shard")));

        store.setNameMode(ModConfig.ItemNames.BOTH);

        List<String> control = tooltip(store, "Acid Magma");
        check("對照組：第 1 行附原文（實際 " + plain(control.get(1)) + "）",
                control.get(1).contains(acid + " (Acid Magma)"));

        List<String> both = tooltip(store, "Void Shard");
        check("★ 跟技能同名的素材：第 0 行附原文（實際 " + plain(both.get(0)) + "）",
                both.get(0).contains(shard + " (Void Shard)"));
        check("★ 跟技能同名的素材：第 1 行附原文（實際 " + plain(both.get(1)) + "）",
                both.get(1).contains(shard + " (Void Shard)"));
        check("原文沒有被詞表換成中文（實際 " + plain(both.get(1)) + "）",
                !both.get(1).contains("(" + shard + ")"));
        check("原文只附一次", both.get(1).indexOf("(Void Shard)")
                == both.get(1).lastIndexOf("(Void Shard)"));
        check("★ 認得出那是我們附上去的原文（折行與詞表保護靠它）",
                store.appendedOriginalAt(shard + " (Void Shard)") >= 0
                        && store.appendedOriginalSpan(ICON + shard + " (Void Shard) [1]") != null);

        check("★ 敘述裡提到同一個字不附原文（實際 " + plain(both.get(2)) + "）",
                both.get(2).contains(shard) && !both.get(2).contains("(Void Shard)"));
        check("★ 技能樹、名牌那種沒有掛名稱列的查表不附（實際 " + store.lookup("Void Shard") + "）",
                shard.equals(store.lookup("Void Shard")));
        String plate = translate(row("", "Void Shard", ICON + TAIL), store);
        check("名牌形狀不附原文（實際 " + plain(plate) + "）", !plate.contains("(Void Shard)"));
        check("別處的括號不算我們附的", store.appendedOriginalAt("別的字 (Void Shard)") < 0);

        List<String> hiker = tooltip(store, "Hiker's Journal");
        check("★ 所有格的素材：第 1 行是它自己的譯名並附原文（實際 " + plain(hiker.get(1)) + "）",
                hiker.get(1).contains(journal + " (Hiker's Journal)"));
        check("第 0 行也一樣（實際 " + plain(hiker.get(0)) + "）",
                hiker.get(0).contains(journal + " (Hiker's Journal)"));
        List<String> hero = tooltip(store, "Hero's Mask");
        check("★ 沒有鑑定度的所有格裝備：第 1 行是它自己的譯名並附原文（實際 "
                        + plain(hero.get(1)) + "）",
                hero.get(1).contains(mask + " (Hero's Mask)"));

        store.setNameMode(ModConfig.ItemNames.ON);
        List<String> hikerOn = tooltip(store, "Hiker's Journal");
        check("★ 只給譯名的模式：所有格的素材也是它自己的譯名（實際 " + plain(hikerOn.get(1)) + "）",
                hikerOn.get(1).contains(journal) && !hikerOn.get(1).contains("Hiker"));
        List<String> shardOn = tooltip(store, "Void Shard");
        check("只給譯名的模式不附原文（實際 " + plain(shardOn.get(1)) + "）",
                shardOn.get(1).contains(shard) && !shardOn.get(1).contains("(Void Shard)"));
        store.setNameMode(ModConfig.ItemNames.OFF);
        List<String> heroOff = tooltip(store, "Hero's Mask");
        check("關掉物品名稱時所有格的裝備留原文，不拼成「某人的東西」（實際 "
                        + plain(heroOff.get(1)) + "）",
                heroOff.get(1).contains("Hero's Mask"));
        store.setNameMode(ModConfig.ItemNames.BOTH);

        String owner = translate(row("", "PoorChaCha's Journal", ""), store);
        check("玩家的東西照舊翻（實際 " + plain(owner) + "）",
                owner.contains("PoorChaCha") && owner.contains(store.lookup("Journal")));

        if (failures > 0) {
            System.err.println(failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("ItemFileOriginalTest 全部通過");
        System.exit(0);
    }

    private static List<String> tooltip(TranslationStore store, String name) {
        List<Component> lines = List.of(hidden(name), shown(name),
                Component.literal(name).withStyle(s -> s.withColor(0xAAAAAA)));
        List<Component> out = TooltipPanel.translateLines(lines, store);
        return (out.isEmpty() ? lines : out).stream().map(Component::getString).toList();
    }

    private static Component hidden(String name) {
        return row(ICON, name, TAIL);
    }

    private static Component shown(String name) {
        return row(ICON.repeat(5), name, "");
    }

    private static Component row(String before, String text, String after) {
        MutableComponent line = Component.empty();
        icons(line, before, 0);
        line.append(Component.literal(text).withStyle(s -> s.withColor(0x3CB0E6)));
        icons(line, after, before.length());
        return line;
    }

    private static void icons(MutableComponent line, String icons, int offset) {
        for (int i = 0; i < icons.length(); i++) {
            String font = (offset + i) % 2 == 0
                    ? "minecraft:language/wynncraft" : "minecraft:tooltip/emblem/frame";
            net.minecraft.network.chat.Style style = net.minecraft.network.chat.Style.EMPTY
                    .withFont(new net.minecraft.network.chat.FontDescription.Resource(
                            net.minecraft.resources.Identifier.parse(font)));
            line.append(Component.literal(String.valueOf((char) (0xE000 + offset + i)))
                    .withStyle(style));
        }
    }

    private static String template(Component line) {
        return com.wynnchayuan.capture.LineParts.of(StyledText.fromComponent(line)).template();
    }

    private static String translate(Component line, TranslationStore store) {
        Component out = LineTranslator.translate(StyledText.fromComponent(line), store);
        return out == null ? line.getString() : out.getString();
    }

    private static String plain(String text) {
        StringBuilder sb = new StringBuilder();
        text.codePoints().forEach(
                cp -> sb.appendCodePoint(cp >= 0xE000 && cp <= 0xF8FF ? '#' : cp));
        return sb.toString();
    }

    private static void same(String what, String want, String got) {
        check(what + "（實際 " + got + "）", java.util.Objects.equals(want, got));
    }

}
