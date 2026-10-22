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

/**
 * 裝備名只套在<b>真的是那件裝備</b>的說明上。
 *
 * <h2>實機回報（使用者 2026-10-22）</h2>
 * 任務《Off the Rails》的「Cemon Notes」書本介面，左上角返回箭頭的說明是
 *
 * <pre>
 *   Return
 *   Click this to return to
 *   the rest of your notes
 * </pre>
 *
 * 名稱那一列被翻成戒指 {@code Return} 的譯名，「譯名加原文」下還附了「(Return)」。
 * 那只是按鈕的名字。語料裡只有那枚戒指用這個原文，查表當然只答得出戒指。
 * 使用者說這類「裝備名翻到介面上別的東西」已經抓到好幾個。
 *
 * <h2>這裡釘住什麼</h2>
 * <ul>
 *   <li>介面按鈕的說明（沒有名稱列外框、沒有稀有度橫幅、沒有需求列）：名稱留原文；</li>
 *   <li>對照組：真的戒指——原版兩列名稱＋橫幅，與 WynnMod 重畫的一列名稱＋橫幅——照舊翻、
 *       「譯名加原文」照舊附原文；</li>
 *   <li>按鈕的說明文字照常翻，名稱留原文不會把它拖回英文；</li>
 *   <li>Lootrun 使命卡（{@code {#}Redemption}，語料另有整條）不受影響；</li>
 *   <li>出貨的語料、七個語言，跟介面詞撞名的裝備名每一個都照上面的規矩。</li>
 * </ul>
 *
 * 譯文一律從語料當場查，不寫死措辭。
 */
public final class GearNameOnGearTest {

    private static int failures = 0;

    /** 跟介面上的字撞名的裝備名（只有裝備用這個原文）。見回報。 */
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

    /** 自己造的語料：一枚戒指、按鈕的說明、一張使命卡。 */
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

    /**
     * 出貨的語料、七個語言：每一個跟介面詞撞名的裝備名都照規矩。
     *
     * <p>同一個 store 重載各語言：TooltipPanel 的快取認「同樣的內容＋同一個 generation」，
     * 各開一個新 store 的話每個都是第 1 代，後面的語言會拿到前一個語言的結果。
     */
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
            // 按鈕的說明：語料收了（攤平成一句的鍵）就要翻出來，名稱留原文不能把它拖回英文
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
                // 這個語言沒翻（西文的裝備名刻意空著）或別的檔也有同一個原文：不是這個 bug
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

    /** 介面按鈕：名稱一列，底下兩列說明。沒有外框、橫幅、需求列。 */
    private static List<Component> button(String name) {
        List<Component> rows = new ArrayList<>();
        rows.add(Component.literal(name).withStyle(s -> s.withColor(0xFFFFFF).withBold(true)));
        rows.add(Component.literal(DESC_1).withStyle(s -> s.withColor(0xAAAAAA)));
        rows.add(Component.literal(DESC_2).withStyle(s -> s.withColor(0xAAAAAA)));
        return rows;
    }

    /** 稀有度與類別的橫幅：整列都是圖示（實機 shiny-tooltip 的第 2 列）。 */
    private static Component banner() {
        MutableComponent row = Component.empty();
        row.append(icon(LANG, 0xD0026));
        row.append(icon(BANNER_BOX.withColor(0xAA00AA),
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

    /** WynnMod 重畫的裝備：名稱只有一列、沒有外框，接著橫幅。 */
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

    /** Lootrun 使命卡：標題只有開頭一個圖示。 */
    private static List<Component> missionCard(String name) {
        List<Component> rows = new ArrayList<>();
        MutableComponent title = Component.empty();
        title.append(icon(EMBLEM_FRAME, 0xE040));
        title.append(Component.literal(name).withStyle(s -> s.withColor(0xFFAA00)));
        rows.add(title);
        rows.add(Component.literal("Objective: Get 5 Boons").withStyle(s -> s.withColor(0xAAAAAA)));
        return rows;
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
