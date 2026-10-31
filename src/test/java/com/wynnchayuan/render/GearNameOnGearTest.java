package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
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

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class GearNameOnGearTest {

    private static final List<String> UI_WORDS = List.of(
            "Return", "Withdrawal", "Crystal", "Shield", "Torch", "Quill", "Remembrance",
            "Departure", "Secret", "Hidden", "Pursuit", "Lookout", "Relic");

    private static final String DESC_1 = "Click this to return to";
    private static final String DESC_2 = "the rest of your notes";

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        selfMadeCorpus();
        shippedCorpus();

        if (failures > 0) {
            System.err.println("GearNameOnGear: " + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("GearNameOnGear: 全部通過");
        System.exit(0);
    }

    private static void selfMadeCorpus() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-gear-on-gear");
        Files.writeString(dir.resolve("gear-accessory.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "r1": {"src": "Return", "dst": "RING-RETURN", "role": "name"},
                   "r2": {"src": "Redemption", "dst": "RING-REDEMPTION", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("gui.json"), """
                {"Click this to return to the rest of your notes": "BUTTON-DESC",
                 "{#}Redemption": "{#}MISSION-REDEMPTION",
                 "Health Regen {#}+{~} [{~}]": "REGEN {#}+{~} [{~}]",
                 "Objective: Get {~} Boons": "GOAL {~}"}
                """, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        for (CollectorConfig.ItemNames mode : List.of(CollectorConfig.ItemNames.ON,
                CollectorConfig.ItemNames.BOTH)) {
            store.setNameMode(mode);
            String ring = store.lookup("Return");
            check("[造] " + mode + "：前提——查表答得出戒指（實際 " + ring + "）",
                    ring != null && ring.contains("RING-RETURN"));

            List<String> button = shown(button("Return"), store);
            check("★ [造] " + mode + "：按鈕的名稱留原文（實際 " + button.get(0) + "）",
                    button.get(0).equals("Return"));
            check("★ [造] " + mode + "：按鈕的說明照常翻，沒被名稱拖回英文（實際 "
                            + button.subList(1, button.size()) + "）",
                    String.join("", button).contains("BUTTON-DESC"));

            List<String> vanilla = shown(vanillaGear("Return"), store);
            check("★ [造] " + mode + "：原版的戒指照舊翻（實際 " + vanilla.get(1) + "）",
                    vanilla.get(1).contains(ring) && vanilla.get(0).contains(ring));
            List<String> wynnmod = shown(wynnModGear("Return"), store);
            check("★ [造] " + mode + "：WynnMod 重畫的戒指照舊翻（實際 " + wynnmod.get(0) + "）",
                    wynnmod.get(0).contains(ring));

            List<String> mission = shown(missionCard("Redemption"), store);
            check("[造] " + mode + "：使命卡的標題照舊是使命名（實際 " + mission.get(0) + "）",
                    mission.get(0).contains("MISSION-REDEMPTION"));
        }
        store.setNameMode(CollectorConfig.ItemNames.ON);
    }

    private static void shippedCorpus() throws Exception {
        TranslationStore rs = new TranslationStore();
        rs.loadAll(Files.createTempDirectory("wynnchayuan-gear-on-gear-empty"));
        int covered = 0;
        for (String lang : List.of("zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr")) {
            Path real = Path.of("src/main/resources/assets/wynnchayuan/translations", lang);
            if (!Files.isDirectory(real)) {
                continue;
            }
            rs.loadAll(real);
            rs.setNameMode(CollectorConfig.ItemNames.BOTH);
            String desc = rs.lookupExact(DESC_1 + " " + DESC_2);
            List<String> notes = shown(button("Return"), rs);
            String rest = String.join(" ", notes.subList(1, notes.size()));
            check("出貨語料 " + lang + "：按鈕的說明收在語料裡", desc != null);
            check("★ 出貨語料 " + lang + "：按鈕的說明翻出來了（實際 " + rest + "）",
                    desc != null && !rest.contains(DESC_1) && !rest.contains(DESC_2));
            List<String> leaked = new ArrayList<>();
            List<String> lost = new ArrayList<>();
            int names = 0;
            for (String name : UI_WORDS) {
                String ring = rs.lookup(name);
                if (ring == null || !rs.itemNameOnly(name)) {
                    continue;
                }
                names++;
                List<String> button = shown(button(name), rs);
                if (!button.get(0).equals(name)) {
                    leaked.add(name + "→" + button.get(0));
                }
                List<String> vanilla = shown(vanillaGear(name), rs);
                List<String> wynnmod = shown(wynnModGear(name), rs);
                if (!vanilla.get(1).contains(ring) || !wynnmod.get(0).contains(ring)) {
                    lost.add(name + "→" + vanilla.get(1) + " / " + wynnmod.get(0));
                }
            }
            covered += names;
            check("★ 出貨語料 " + lang + "：" + names + " 個撞名的裝備名，按鈕上都留原文（翻了的 "
                    + leaked + "）", leaked.isEmpty());
            check("★ 出貨語料 " + lang + "：同一批名字在真的裝備上照舊翻（沒翻的 " + lost + "）",
                    lost.isEmpty());
        }
        check("前提——出貨語料裡真的有撞名的裝備名（不然上面什麼都沒驗）", covered > 0);
    }

    private static final Style LANG = font("minecraft:language/wynncraft");
    private static final Style EMBLEM_FRAME = font("minecraft:tooltip/emblem/frame");
    private static final Style EMBLEM_SPRITE = font("minecraft:tooltip/emblem/sprite");
    private static final Style BANNER_BOX = font("minecraft:banner/box");
    private static final Style SPACE = font("minecraft:space");

    private static Style font(String id) {
        return Style.EMPTY.withFont(new FontDescription.Resource(Identifier.parse(id)));
    }

    private static Component icon(Style style, int... cps) {
        StringBuilder sb = new StringBuilder();
        for (int cp : cps) {
            sb.appendCodePoint(cp);
        }
        return Component.literal(sb.toString()).withStyle(style);
    }

    private static List<Component> button(String name) {
        List<Component> rows = new ArrayList<>();
        rows.add(Component.literal(name).withStyle(s -> s.withColor(0xFFFFFF).withBold(true)));
        rows.add(Component.literal(DESC_1).withStyle(s -> s.withColor(0xAAAAAA)));
        rows.add(Component.literal(DESC_2).withStyle(s -> s.withColor(0xAAAAAA)));
        return rows;
    }

    private static Component banner() {
        MutableComponent row = Component.empty();
        row.append(icon(LANG, 0xD0026));
        row.append(icon(BANNER_BOX.withColor(0xAA00AA),
                0xE060, 0xCFFFF, 0xE03C, 0xCFFFF, 0xE048, 0xCFFFF, 0xE062, 0xCFFDC));
        row.append(icon(BANNER_BOX.withColor(0x000000), 0xE00C, 0xE018, 0xD0002));
        row.append(icon(LANG, 0xD0001));
        return row;
    }

    private static List<Component> vanillaGear(String name) {
        List<Component> rows = new ArrayList<>();
        MutableComponent hidden = Component.empty();
        hidden.append(icon(SPACE, 0xCF000));
        hidden.append(Component.literal(name).withStyle(s -> s.withColor(0xAA00AA)));
        hidden.append(icon(SPACE, 0xCF000));
        rows.add(hidden);
        MutableComponent seen = Component.empty();
        seen.append(icon(LANG, 0xCFFF0));
        seen.append(icon(EMBLEM_FRAME, 0xF050));
        seen.append(icon(LANG, 0xCFFCF));
        seen.append(icon(EMBLEM_SPRITE, 0xF006));
        seen.append(icon(LANG, 0xD0005));
        seen.append(Component.literal(name).withStyle(LANG.withColor(0xAA00AA)));
        seen.append(Component.literal(" [79.1%]").withStyle(LANG.withColor(0xB1FF55)));
        rows.add(seen);
        rows.add(banner());
        rows.add(Component.empty());
        rows.add(regen());
        return rows;
    }

    private static List<Component> wynnModGear(String name) {
        List<Component> rows = new ArrayList<>();
        rows.add(Component.literal(name).withStyle(s -> s.withColor(0xAA00AA)));
        rows.add(banner());
        rows.add(Component.empty());
        rows.add(regen());
        return rows;
    }

    private static Component regen() {
        MutableComponent row = Component.empty();
        row.append(Component.literal("Health Regen ").withStyle(s -> s.withColor(0x55FF55)));
        row.append(icon(SPACE, 0xD0010));
        row.append(Component.literal("+12% [80%]").withStyle(s -> s.withColor(0x55FF55)));
        return row;
    }

    private static List<Component> missionCard(String name) {
        List<Component> rows = new ArrayList<>();
        MutableComponent title = Component.empty();
        title.append(icon(EMBLEM_FRAME, 0xE040));
        title.append(Component.literal(name).withStyle(s -> s.withColor(0xFFAA00)));
        rows.add(title);
        rows.add(Component.literal("Objective: Get 5 Boons").withStyle(s -> s.withColor(0xAAAAAA)));
        return rows;
    }

    private static List<String> shown(List<Component> rows, TranslationStore store) {
        List<Component> out = TooltipPanel.translateInPlace(rows, store);
        List<Component> lines = out.isEmpty() ? rows : out;
        List<String> text = new ArrayList<>();
        for (Component line : lines) {
            StringBuilder sb = new StringBuilder();
            line.getString().codePoints().forEach(cp -> sb.appendCodePoint(
                    cp >= 0xE000 && cp <= 0xF8FF || cp >= 0xC0000 ? '#' : cp));
            text.add(sb.toString());
        }
        return text;
    }

}
