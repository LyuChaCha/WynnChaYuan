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

/**
 * Lootrun 試煉卡與使命卡的標題照使命名顯示，不留英文、也不變成同名的裝備。
 *
 * <h2>實機回報（使用者 2026-10-22，1.0.3_2）</h2>
 * 新版的試煉卡分成「Trial:」「Reward:」兩段，標題 {@code Hubris} 是紅字、<b>沒有</b>開頭
 * 的圖示（layout-debug 的足跡是「看過 Hubris/14」）。畫面上標題留英文，底下照常翻：
 *
 * <pre>
 *   Hubris                          ← 應為「傲慢 (Hubris)」（內容名稱：譯名加原文）
 *   試煉:
 *   接下來 10 場挑戰中，…
 *   獎勵:
 * </pre>
 *
 * 語料裡 Lootrun 那一份是 {@code {#}Hubris} 與 {@code Hubris:}；裸的 {@code Hubris}
 * 只有那條項鍊，於是名稱列的守門把它當成「說明不像物品的裝備名」留原文。
 * 使命卡也換成裸標題了（cards.json：{@code "card": "High Roller"}），而使命卡被認成
 * 「不是裝備的面板」、守門讓開，標題就查到裝備：「平衡之盔 (Equilibrium)」。
 *
 * <h2>這裡釘住什麼</h2>
 * <ul>
 *   <li>出貨語料、七個語言：每一個跟裝備撞名的 Lootrun 名稱，在新版卡片上照「內容名稱」的
 *       三種設定顯示使命名——譯名、譯名 (原文)、原文；</li>
 *   <li>舊版卡片（{@code {#}Redemption}）照舊；</li>
 *   <li>真的 Hubris 項鍊（原版兩列名稱、WynnMod 一列名稱，都有稀有度橫幅）照舊是項鍊。</li>
 * </ul>
 *
 * 期望的字從語料當場取，不寫死措辭。
 */
public final class LootrunCardTitleTest {

    private static int failures = 0;

    /** 試煉（Wynntils 4.2.8 的 TrialType）；其餘的名字是使命。 */
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
                    continue;                  // 沒跟物品撞名：不是這個 bug
                }
                String cell = missionCell(rs, name);
                if (cell == null) {
                    continue;                  // 這個語言沒翻這個使命名
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

            // 新版卡片的段落本身照常翻（前提：測的真的是那張卡）
            rs.setPageNameMode(BOTH);
            List<String> trial = shown(trialCard("Hubris"), rs);
            String trialLabel = rs.lookupExact("Trial:");
            check("出貨語料 " + lang + "：試煉卡的「Trial:」照常翻（實際 " + trial.get(2) + "）",
                    trialLabel == null || trial.get(2).strip().equals(trialLabel));

            // 舊版卡片：標題帶一個開頭圖示，語料另有整條
            String oldCell = rs.lookupExact("{#}Redemption");
            if (oldCell != null) {
                rs.setPageNameMode(ON);
                List<String> old = shown(oldMissionCard("Redemption"), rs);
                check("出貨語料 " + lang + "：舊版使命卡 {#}Redemption 照舊（實際 " + old.get(0) + "）",
                        old.get(0).replace("#", "").strip()
                                .equals(oldCell.replace("{#}", "").strip()));
            }

            // 真的 Hubris 項鍊：照舊是項鍊，「譯名加原文」照舊附原文
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

    /** 語料裡這個使命名自己的譯名：記分板那一行（名字:）與聊天面板那一格（{#}名字）。 */
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

    // ------------------------------------------------------------ 造 tooltip（照實機分段）

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

    /**
     * 新版試煉卡（實機 2026-10-22，14 列）：紅色標題、沒有圖示，接「Trial:」「Reward:」兩段。
     */
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

    /** 新版使命卡（cards.json：標題是裸名，接「Objective:」「Reward:」兩段）。 */
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

    /** 舊版使命卡：標題只有開頭一個圖示。 */
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

    /** 稀有度與類別的橫幅：整列都是圖示（實機 shiny-tooltip 的第 2 列）。 */
    private static Component banner() {
        MutableComponent row = Component.empty();
        row.append(icon(LANG, 0xD0026));
        row.append(icon(BANNER_BOX.withColor(0xFF55FF),
                0xE060, 0xCFFFF, 0xE03C, 0xCFFFF, 0xE048, 0xCFFFF, 0xE062, 0xCFFDC));
        row.append(icon(BANNER_BOX.withColor(0x000000), 0xE00C, 0xE018, 0xD0002));
        row.append(icon(LANG, 0xD0001));
        return row;
    }

    /** 原版的裝備：第 0 列寬度 0 的名稱、第 1 列五個圖示＋名稱＋鑑定度、橫幅、屬性。 */
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

    /** WynnMod 重畫的裝備：名稱只有一列、沒有外框，接著橫幅。 */
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

    /** 翻完的每一行；圖示字元換成 # 好讀。整份沒翻到就回原文。 */
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
