package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.translate.LootrunNames;
import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class LootrunCardTitleTest {

    private static int failures = 0;

    private static final Set<String> TRIALS = Set.of(
            "Adrenaline Junkie", "Dying Light", "Monochromokopia", "Chronotrigger", "All In",
            "Gambling Beast", "Hubris", "Lights Out", "Side Hustle", "Treasury Bill",
            "Ultimate Sacrifice", "Warmth Devourer", "Imperitia");

    private static final CollectorConfig.ItemNames ON = CollectorConfig.ItemNames.ON;
    private static final CollectorConfig.ItemNames BOTH = CollectorConfig.ItemNames.BOTH;
    private static final CollectorConfig.ItemNames OFF = CollectorConfig.ItemNames.OFF;

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        TranslationStore rs = new TranslationStore();
        rs.loadAll(Files.createTempDirectory("wynnchayuan-lootrun-title-empty"));
        int covered = 0;
        boolean hubrisSeen = false;
        for (String lang : List.of("zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr")) {
            Path real = Path.of("src/main/resources/assets/wynnchayuan/translations", lang);
            if (!Files.isDirectory(real)) {
                continue;
            }
            rs.loadAll(real);
            rs.setNameMode(BOTH);
            List<String> wrong = new ArrayList<>();
            int names = 0;
            for (String name : LootrunNames.names()) {
                if (!rs.isItemName(name)) {
                    continue;
                }
                String cell = missionCell(rs, name);
                if (cell == null) {
                    continue;
                }
                names++;
                hubrisSeen |= name.equals("Hubris");
                List<Component> card = TRIALS.contains(name) ? trialCard(name) : missionCard(name);
                expect(wrong, rs, lang, name, card, ON, cell);
                expect(wrong, rs, lang, name, card, BOTH, cell + " (" + name + ")");
                expect(wrong, rs, lang, name, card, OFF, name);
            }
            covered += names;
            check("★ 出貨語料 " + lang + "：" + names + " 個撞裝備名的 Lootrun 名稱，新版卡片的標題"
                    + "照內容名稱設定顯示（錯的 " + wrong + "）", wrong.isEmpty());

            rs.setPageNameMode(BOTH);
            List<String> trial = shown(trialCard("Hubris"), rs);
            String trialLabel = rs.lookupExact("Trial:");
            check("出貨語料 " + lang + "：試煉卡的「Trial:」照常翻（實際 " + trial.get(2) + "）",
                    trialLabel == null || trial.get(2).strip().equals(trialLabel));

            String oldCell = rs.lookupExact("{#}Redemption");
            if (oldCell != null) {
                rs.setPageNameMode(ON);
                List<String> old = shown(oldMissionCard("Redemption"), rs);
                check("出貨語料 " + lang + "：舊版使命卡 {#}Redemption 照舊（實際 " + old.get(0) + "）",
                        old.get(0).replace("#", "").strip()
                                .equals(oldCell.replace("{#}", "").strip()));
            }

            String necklace = rs.lookupExact("Hubris");
            if (necklace != null && !necklace.equals(missionCell(rs, "Hubris"))) {
                rs.setPageNameMode(BOTH);
                List<String> vanilla = shown(vanillaGear("Hubris"), rs);
                List<String> wynnmod = shown(wynnModGear("Hubris"), rs);
                check("★ 出貨語料 " + lang + "：原版的 Hubris 項鍊照舊是項鍊（實際 " + vanilla.get(1)
                                + "）",
                        vanilla.get(1).contains(necklace) && vanilla.get(1).contains("(Hubris)"));
                check("★ 出貨語料 " + lang + "：WynnMod 重畫的 Hubris 項鍊照舊是項鍊（實際 "
                                + wynnmod.get(0) + "）",
                        wynnmod.get(0).contains(necklace) && wynnmod.get(0).contains("(Hubris)"));
            }
        }
        check("前提——出貨語料裡真的有撞裝備名的 Lootrun 名稱（不然上面什麼都沒驗）", covered > 0);
        check("前提——回報的 Hubris 有驗到", hubrisSeen);

        if (failures > 0) {
            System.err.println("LootrunCardTitle: " + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("LootrunCardTitle: 全部通過");
        System.exit(0);
    }

    private static String missionCell(TranslationStore rs, String name) {
        String icon = rs.lookupExact("{#}" + name);
        if (icon != null && icon.startsWith("{#}") && icon.lastIndexOf("{#}") == 0) {
            return icon.substring(3).strip();
        }
        String colon = rs.lookupExact(name + ":");
        if (colon != null && (colon.endsWith(":") || colon.endsWith("："))) {
            return colon.substring(0, colon.length() - 1).strip();
        }
        return null;
    }

    private static void expect(List<String> wrong, TranslationStore rs, String lang, String name,
                               List<Component> card, CollectorConfig.ItemNames mode, String want) {
        rs.setPageNameMode(mode);
        String title = shown(card, rs).get(0).strip();
        if (!title.equals(want)) {
            wrong.add(mode + " " + name + "→" + title + "（應為 " + want + "）");
        }
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

    private static Component grey(String text) {
        return Component.literal(text).withStyle(s -> s.withColor(0xAAAAAA));
    }

    private static List<Component> trialCard(String name) {
        List<Component> rows = new ArrayList<>();
        rows.add(Component.literal(name).withStyle(s -> s.withColor(0xFF5555)));
        rows.add(Component.empty());
        rows.add(Component.literal("Trial:").withStyle(s -> s.withColor(0xFF5555)));
        rows.add(grey("For the next 10 challenges,"));
        rows.add(grey("dying will end your Lootrun."));
        rows.add(Component.empty());
        rows.add(Component.literal("Reward:").withStyle(s -> s.withColor(0x55FF55)));
        rows.add(grey("Gain +1 End Reward Reroll and"));
        rows.add(grey("+1 End Reward Sacrifice."));
        rows.add(Component.empty());
        rows.add(grey("Complete the objective"));
        rows.add(grey("to receive the rewards"));
        rows.add(Component.empty());
        rows.add(Component.literal("Click to choose!").withStyle(s -> s.withColor(0xFFFF55)));
        return rows;
    }

    private static List<Component> missionCard(String name) {
        List<Component> rows = new ArrayList<>();
        rows.add(Component.literal(name).withStyle(s -> s.withColor(0xFFAA00)));
        rows.add(Component.empty());
        rows.add(Component.literal("Objective:").withStyle(s -> s.withColor(0xFFAA00)));
        rows.add(grey("Get 5 Boons"));
        rows.add(Component.empty());
        rows.add(Component.literal("Reward:").withStyle(s -> s.withColor(0x55FF55)));
        rows.add(grey("Gain +1 End Reward Sacrifice."));
        rows.add(Component.empty());
        rows.add(grey("Complete the objective"));
        rows.add(grey("to receive the rewards"));
        rows.add(Component.empty());
        rows.add(Component.literal("Click to choose!").withStyle(s -> s.withColor(0xFFFF55)));
        return rows;
    }

    private static List<Component> oldMissionCard(String name) {
        List<Component> rows = new ArrayList<>();
        MutableComponent title = Component.empty();
        title.append(icon(EMBLEM_FRAME, 0xE040));
        title.append(Component.literal(name).withStyle(s -> s.withColor(0xFFAA00)));
        rows.add(title);
        rows.add(grey("Objective: Get 5 Boons"));
        rows.add(Component.empty());
        rows.add(grey("Reward: Gain +1 End Reward Sacrifice."));
        return rows;
    }

    private static Component banner() {
        MutableComponent row = Component.empty();
        row.append(icon(LANG, 0xD0026));
        row.append(icon(BANNER_BOX.withColor(0xFF55FF),
                0xE060, 0xCFFFF, 0xE03C, 0xCFFFF, 0xE048, 0xCFFFF, 0xE062, 0xCFFDC));
        row.append(icon(BANNER_BOX.withColor(0x000000), 0xE00C, 0xE018, 0xD0002));
        row.append(icon(LANG, 0xD0001));
        return row;
    }

    private static List<Component> vanillaGear(String name) {
        List<Component> rows = new ArrayList<>();
        MutableComponent hidden = Component.empty();
        hidden.append(icon(SPACE, 0xCF000));
        hidden.append(Component.literal(name).withStyle(s -> s.withColor(0xFF55FF)));
        hidden.append(icon(SPACE, 0xCF000));
        rows.add(hidden);
        MutableComponent seen = Component.empty();
        seen.append(icon(LANG, 0xCFFF0));
        seen.append(icon(EMBLEM_FRAME, 0xF050));
        seen.append(icon(LANG, 0xCFFCF));
        seen.append(icon(EMBLEM_SPRITE, 0xF006));
        seen.append(icon(LANG, 0xD0005));
        seen.append(Component.literal(name).withStyle(LANG.withColor(0xFF55FF)));
        seen.append(Component.literal(" [79.1%]").withStyle(LANG.withColor(0xB1FF55)));
        rows.add(seen);
        rows.add(banner());
        rows.add(Component.empty());
        rows.add(regen());
        return rows;
    }

    private static List<Component> wynnModGear(String name) {
        List<Component> rows = new ArrayList<>();
        rows.add(Component.literal(name).withStyle(s -> s.withColor(0xFF55FF)));
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

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) {
            failures++;
        }
    }
}
