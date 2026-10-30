package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class WynntilsTextTest {

    private static int failures = 0;

    // 類別簡名要跟 WynntilsText.TRACKER 一樣
    private static final class ContentTrackerOverlay { }

    private static final class InfoBoxOverlay { }

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                Languages.DEFAULT));
        CollectorConfig config = new CollectorConfig(
                Files.createTempDirectory("wcy-overlay").resolve("config.json"));

        tracker(config, store);
        objectives(config, store);
        markers(config, store);
        heldItem(config, store);
        bossBar(config, store);
        entityName(config, store);
        switched(config);
        wynntilsScreens(config, store);
        beaconCount(config);
        statusEffects(config);

        System.out.println(failures == 0 ? "\n就地取代：全部通過"
                : "\n就地取代：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void tracker(CollectorConfig config, TranslationStore store) {
        StyledText[] lines = {
            StyledText.fromString("Quest"),
            StyledText.fromString("Find the meteor's crash site."),
        };
        Object tracker = new ContentTrackerOverlay();

        while (config.trackerMode() != CollectorConfig.DialogueMode.REPLACE) {
            config.cycleTrackerMode();
        }
        StyledText[] out = WynntilsText.lines(tracker, lines, config, store);
        check("追蹤欄第一行翻得出來（實際 " + out[0].getString() + "）",
              !out[0].getString().equals("Quest"));
        check("行數不變", out.length == lines.length);

        StyledText composed = StyledText.fromString(
                "§b§lQuest§7 - §fStar Thief");
        StyledText[] header = WynntilsText.lines(
                tracker, new StyledText[] {composed}, config, store);
        String shown = header[0].getStringWithoutFormatting();
        check("「Quest - 任務名」兩段都換掉（實際 " + shown + "）",
              shown.contains("任務") && shown.contains("竊星者"));
        check("顏色沒有被抹掉", header[0].getString().contains("§b"));

        StyledText single = StyledText.fromString("§aQuest – Dearly Departed");
        StyledText[] one = WynntilsText.lines(
                tracker, new StyledText[] {single}, config, store);
        String got = one[0].getStringWithoutFormatting();
        check("一整段的「Quest – 任務名」也換得掉（實際 " + got + "）",
              got.contains("任務") && got.contains("音容宛在"));

        StyledText fonted = StyledText.fromComponent(net.minecraft.network.chat.Component
                .literal("Quest – Dearly Departed")
                .withStyle(net.minecraft.network.chat.Style.EMPTY
                        .withColor(0x29CC96)
                        .withFont(new net.minecraft.network.chat.FontDescription.Resource(
                                net.minecraft.resources.Identifier.fromNamespaceAndPath(
                                        "minecraft", "language/wynncraft")))));
        StyledText[] withFont = WynntilsText.lines(
                tracker, new StyledText[] {fonted}, config, store);
        String[] font = {null};
        Integer[] colour = {null};
        withFont[0].getComponent().visit((style, text) -> {
            if (text.contains("音容宛在")) {
                font[0] = String.valueOf(style.getFont());
                colour[0] = style.getColor() == null ? null : style.getColor().getValue();
            }
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        check("標題換成中文時字型改回預設，不會變方框（實際 " + font[0] + "）",
              font[0] != null && !font[0].contains("language/wynncraft"));
        check("標題的顏色照抄", colour[0] != null && colour[0] == 0x29CC96);

        StyledText[] other = WynntilsText.lines(new InfoBoxOverlay(), lines, config, store);
        check("別的疊層原樣不動", other == lines);

        while (config.trackerMode() != CollectorConfig.DialogueMode.PANEL) {
            config.cycleTrackerMode();
        }
        check("面板模式下不動 Wynntils 的字",
              WynntilsText.lines(tracker, lines, config, store) == lines);

        while (config.trackerMode() != CollectorConfig.DialogueMode.OFF) {
            config.cycleTrackerMode();
        }
        check("關掉時不動 Wynntils 的字",
              WynntilsText.lines(tracker, lines, config, store) == lines);

        check("沒有設定或語料時原樣回去",
              WynntilsText.lines(tracker, lines, null, store) == lines
                      && WynntilsText.lines(tracker, lines, config, null) == lines);
    }

    private static void objectives(CollectorConfig config, TranslationStore store) {
        if (!config.translateObjectives()) {
            config.toggleObjectives();
        }
        String done = WynntilsText.objective("Slay Mobs: 12/100", config, store);
        check("目標那一條翻得出來（實際 " + done + "）", done.contains("擊殺怪物"));
        check("數字照抄", done.contains("12") && done.contains("100"));

        String odd = "Qwertyuiop: 1/2";
        check("翻不出來的原樣回去",
              WynntilsText.objective(odd, config, store).equals(odd));

        config.toggleObjectives();
        check("★ 關掉時原樣回去（快取不可以繞過開關）",
              WynntilsText.objective("Slay Mobs: 12/100", config, store)
                      .equals("Slay Mobs: 12/100"));
        config.toggleObjectives();

        String twice = WynntilsText.objective("Slay Mobs: 12/100", config, store);
        check("快取：同一條問兩次結果一樣（實際 " + twice + "）", done.equals(twice));

        int before = store.generation();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                "zh_cn"));
        check("快取：重載之後 generation 有往前（" + before + " -> "
              + store.generation() + "）", store.generation() != before);
        String cn = WynntilsText.objective("Slay Mobs: 12/100", config, store);
        check("★ 快取：重載成簡中之後拿到的是新語料的答案（實際 " + cn + "）",
              !cn.equals(done) && cn.contains("12") && cn.contains("100"));

        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                Languages.DEFAULT));
    }

    private static void markers(CollectorConfig config, TranslationStore store) {
        while (config.trackerMode() != CollectorConfig.DialogueMode.REPLACE) {
            config.cycleTrackerMode();
        }
        String name = WynntilsText.marker("Cook Assistant", config, store);
        check("任務指引的任務名翻得出來（實際 " + name + "）", name.contains("廚師助理"));
        String odd = "Qwertyuiop Zxcv";
        check("翻不出來的標記原樣回去", WynntilsText.marker(odd, config, store).equals(odd));
        while (config.trackerMode() != CollectorConfig.DialogueMode.OFF) {
            config.cycleTrackerMode();
        }
        check("追蹤欄關掉時指引也照原文",
              WynntilsText.marker("Cook Assistant", config, store).equals("Cook Assistant"));
        while (config.trackerMode() != CollectorConfig.DialogueMode.REPLACE) {
            config.cycleTrackerMode();
        }
    }

    private static void heldItem(CollectorConfig config, TranslationStore store) {
        net.minecraft.network.chat.Component scroll =
                net.minecraft.network.chat.Component.literal("Ragni Teleportation Scroll [3/3]");
        check("手持物品名稱預設打開", config.translateHeldItem());
        String shown = WynntilsText.heldItemName(scroll, config, store).getString();
        check("手持物品名稱翻得出來（實際 " + shown + "）",
              shown.contains("傳送卷軸") && shown.contains("3/3"));
        config.toggleHeldItem();
        check("關掉之後原樣回去", WynntilsText.heldItemName(scroll, config, store) == scroll);
        config.toggleHeldItem();
    }

    private static void bossBar(CollectorConfig config, TranslationStore store) {
        net.minecraft.network.chat.Component bar = net.minecraft.network.chat.Component.literal("Horse")
                .withStyle(net.minecraft.ChatFormatting.GREEN)
                .append(net.minecraft.network.chat.Component.literal(" - ")
                        .withStyle(net.minecraft.ChatFormatting.GRAY))
                .append(net.minecraft.network.chat.Component.literal("47❤")
                        .withStyle(net.minecraft.ChatFormatting.RED));
        String shown = WynntilsText.bossBar(bar, config, store).getString();
        check("boss bar 的生物名翻得出來、血量留著（實際 " + shown + "）",
              shown.startsWith("馬") && shown.endsWith("47❤"));
        check("同一條再畫一次拿到同一份", WynntilsText.bossBar(bar, config, store)
                == WynntilsText.bossBar(bar, config, store));
        while (config.nametagMode() != CollectorConfig.NametagMode.OFF) {
            config.cycleNametagMode();
        }
        check("名牌關掉不會連坐 boss bar",
                WynntilsText.bossBar(bar, config, store).getString().startsWith("馬"));
        while (config.nametagMode() == CollectorConfig.NametagMode.OFF) {
            config.cycleNametagMode();
        }

        config.toggleBossBar();
        check("boss bar 自己的開關關掉時原樣回去",
                WynntilsText.bossBar(bar, config, store) == bar);
        config.toggleBossBar();
        check("再打開就照常翻",
                WynntilsText.bossBar(bar, config, store).getString().startsWith("馬"));
    }

    private static void switched(CollectorConfig config) {
        while (config.nametagMode() != CollectorConfig.NametagMode.REPLACE) {
            config.cycleNametagMode();
        }
        String corpus = "src/main/resources/assets/wynnchayuan/translations";
        TranslationStore store = new TranslationStore();
        net.minecraft.network.chat.Component altar =
                net.minecraft.network.chat.Component.literal("Corrupted Altar");

        store.loadAll(Path.of(corpus, "zh_tw"));
        check("先是繁中（實際 "
                        + WynntilsText.entityName(altar, config, store).getString() + "）",
                "腐敗祭壇".equals(
                        WynntilsText.entityName(altar, config, store).getString()));

        store.loadAll(Path.of(corpus, "zh_cn"));
        check("換語言之後立刻跟著換（實際 "
                        + WynntilsText.entityName(altar, config, store).getString() + "）",
                "腐化祭坛".equals(
                        WynntilsText.entityName(altar, config, store).getString()));
    }

    private static void entityName(CollectorConfig config, TranslationStore store) {
        while (config.nametagMode() != CollectorConfig.NametagMode.REPLACE) {
            config.cycleNametagMode();
        }
        net.minecraft.network.chat.Component altar = net.minecraft.network.chat.Component
                .literal("Corrupted Altar").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE);
        net.minecraft.network.chat.Component shown = WynntilsText.entityName(altar, config, store);
        check("浮空字翻得出來（實際 " + shown.getString() + "）",
              shown.getString().equals("腐敗祭壇"));
        check("顏色照抄", shown.getStyle().getColor() != null
                || shown.getSiblings().stream().anyMatch(s -> s.getStyle().getColor() != null));
        net.minecraft.network.chat.Component odd =
                net.minecraft.network.chat.Component.literal("Qwertyuiop Zxcv");
        check("翻不出來的原樣回去", WynntilsText.entityName(odd, config, store) == odd);
        CollectorConfig.NametagMode was = config.nametagMode();
        while (config.nametagMode() != CollectorConfig.NametagMode.OFF) {
            config.cycleNametagMode();
        }
        check("名牌那一列關掉時原樣回去",
                WynntilsText.entityName(altar, config, store) == altar);
        config.cycleNametagMode();
        check("注視時顯示：原文不動（實際 "
                        + WynntilsText.entityName(altar, config, store).getString() + "）",
                WynntilsText.entityName(altar, config, store) == altar);
        check("注視時顯示：翻不出來的也原樣回去",
                WynntilsText.entityName(odd, config, store) == odd);
        while (config.nametagMode() != was) {
            config.cycleNametagMode();
        }
    }

    private static void beaconCount(CollectorConfig config) {
        System.out.println("=== 信標計數 ===");
        List<String> rows = List.of("\u00a7eYellow: 0", "\u00a79Blue: 0", "\u00a75Purple: 0",
                "\u00a77Gray: 0/3", "\u00a76Orange: 0 (+1) (5)", "\u00a7bAqua: 0",
                "\u00a78Dark Gray: 0/1", "\u00a7aGreen: 0", "\u00a7cRed: 0 (0)",
                "\u00a7fWhite: 0/1", "\u00a7dPink: 0", "\u00a74Crimson: 0/2",
                "\u00a72Rainbow: 0 (10)");
        for (String lang : List.of("zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es")) {
            TranslationStore store = new TranslationStore();
            store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations", lang));
            store.setNameMode(CollectorConfig.ItemNames.BOTH);
            StringBuilder left = new StringBuilder();
            StringBuilder gear = new StringBuilder();
            for (String row : rows) {
                StyledText in = StyledText.fromString(row);
                String plain = in.getStringWithoutFormatting();
                String colour = plain.substring(0, plain.indexOf(':'));
                String out = WynntilsText.screenText(in, config, store)
                        .getStringWithoutFormatting();
                if ("zh_cn".equals(lang)) {
                    System.out.println("      " + plain + "  ->  " + out);
                }
                if (out.equals(plain)) {
                    left.append(' ').append(colour);
                }
                if (out.contains("(" + colour + ")")) {
                    gear.append(' ').append(colour);
                }
                check(lang + "：" + plain + " 的數值原樣留著（實際 " + out + "）",
                      out.endsWith(plain.substring(plain.indexOf(':') + 1)));
            }
            check(lang + "：★ 沒有顏色被換成裝備名（實際" + gear + "）", gear.length() == 0);
            check(lang + "：十三個顏色都翻了（沒翻的" + left + "）", left.length() == 0);
        }
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations", "zh_tw"));
        store.setNameMode(CollectorConfig.ItemNames.BOTH);
        for (String name : List.of("Halcyon", "Cumulonimbus")) {
            check("前提：" + name + " 是翻好的裝備名", store.lookup(name) != null);
            StyledText in = StyledText.fromString("\u00a7f" + name + ": 0/1");
            StyledText out = WynntilsText.screenText(in, config, store);
            check("★ 「" + name + ": 0/1」不是屬性列，原樣留著（實際 "
                    + out.getStringWithoutFormatting() + "）", out == in);
        }
    }

    private static void statusEffects(CollectorConfig config) throws Exception {
        check("尾巴是倒數的時間：是狀態列", WynntilsText.statusRow("29% Resistance (00:27)")
                && WynntilsText.statusRow("Tree Manipulation (**:**)")
                && WynntilsText.statusRow("8% Strength (1:05:00) "));
        check("沒有時間的不是（物品名、一般的標籤、括號裡是別的東西）",
                !WynntilsText.statusRow("Resistance") && !WynntilsText.statusRow("Loot Chests: 0/12")
                        && !WynntilsText.statusRow("Oak Wood Dagger (Lv. 12)")
                        && !WynntilsText.statusRow(null));
        for (String lang : List.of("zh_tw", "ja_jp")) {
            TranslationStore store = new TranslationStore();
            store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations", lang));
            String gear = null;
            for (String candidate : List.of("Resistance", "Fatal", "Halcyon", "Warp")) {
                if (store.itemNameOnly(candidate) && store.lookup(candidate) != null) {
                    gear = candidate;
                    break;
                }
            }
            check(lang + "：前提：找得到一個只有裝備在用的名字", gear != null);
            if (gear == null) {
                continue;
            }
            String worn = store.lookup(gear);
            for (CollectorConfig.ItemNames mode : List.of(CollectorConfig.ItemNames.ON,
                    CollectorConfig.ItemNames.BOTH)) {
                store.setNameMode(mode);
                StyledText row = StyledText.fromString(
                        "\u00a7729% \u00a7f" + gear + " \u00a77(00:27)");
                String out = WynntilsText.screenText(row, config, store).getStringWithoutFormatting();
                check(lang + "／" + mode + "：★ 狀態列的「" + gear + "」不會變成裝備的譯名（實際 " + out + "）",
                        out.contains(gear) && !out.contains(worn));
                StyledText endless = StyledText.fromString("\u00a7f" + gear + " \u00a77(**:**)");
                out = WynntilsText.screenText(endless, config, store).getStringWithoutFormatting();
                check(lang + "／" + mode + "：★ 沒有期限的那一種也一樣（實際 " + out + "）",
                        out.contains(gear) && !out.contains(worn));
            }
            store.setNameMode(CollectorConfig.ItemNames.ON);
            String strength = store.lookup("Strength");
            if (strength != null && !strength.equals("Strength")) {
                StyledText row = StyledText.fromString("\u00a778% \u00a7fStrength \u00a77(00:" + (30 + lang.charAt(0) % 20 + lang.charAt(3) % 7) + ")");
                String out = WynntilsText.screenText(row, config, store).getStringWithoutFormatting();
                check(lang + "：屬性也在用的字照舊翻（實際 " + out + "）", out.contains(strength));
            }
        }
        check("狀態列查完之後旗標有還原", !TranslationStore.itemNamesBarred());
    }

    private static void wynntilsScreens(CollectorConfig config, TranslationStore store) {
        StyledText inProgress = StyledText.fromString("Currently in progress");
        StyledText shown = WynntilsText.screenText(inProgress, config, store);
        check("綜合頁面的狀態翻得出來（實際 " + shown.getString() + "）",
              "進行中".equals(shown.getString()));

        StyledText already = StyledText.fromString("進行中");
        check("已經是中文的原樣回去",
              WynntilsText.screenText(already, config, store) == already);

        StyledText odd = StyledText.fromString("Qwertyuiop Zxcv");
        check("查不到的原樣回去", WynntilsText.screenText(odd, config, store) == odd);

        config.toggleWynntilsUi();
        check("F6 關掉時完全不動",
              WynntilsText.screenText(inProgress, config, store) == inProgress);
        config.toggleWynntilsUi();
        check("再打開就又換得到",
              "進行中".equals(WynntilsText.screenText(inProgress, config, store).getString()));

        WynntilsText.holdRawText(true);
        check("★ 那一段裡的字原樣回去（公會名與簡稱都不是遊戲文案，撞名躲不完）",
              WynntilsText.screenText(inProgress, config, store) == inProgress);
        WynntilsText.holdRawText(false);
        check("★ 出了那一段就恢復",
              "進行中".equals(WynntilsText.screenText(inProgress, config, store).getString()));

        screenCache(config, store);
    }

    private static void screenCache(CollectorConfig config, TranslationStore store) {
        StyledText inProgress = StyledText.fromString("Currently in progress");

        String once = WynntilsText.screenText(inProgress, config, store).getString();
        String twice = WynntilsText.screenText(inProgress, config, store).getString();
        check("快取：同一個字串兩次結果一樣（實際 " + twice + "）", once.equals(twice));

        WynntilsText.holdRawText(true);
        check("★ 快取不可以繞過「這一段不翻」",
              WynntilsText.screenText(inProgress, config, store) == inProgress);
        WynntilsText.holdRawText(false);
        check("出了那一段又拿得到翻譯",
              once.equals(WynntilsText.screenText(inProgress, config, store).getString()));

        int before = store.generation();
        store.loadAll(java.util.List.of(
                java.nio.file.Path.of(
                        "src/main/resources/assets/wynnchayuan/translations/zh_cn")));
        check("快取：重載之後 generation 有往前（" + before + " -> "
              + store.generation() + "）", store.generation() != before);
        String after = WynntilsText.screenText(inProgress, config, store).getString();
        check("★ 快取：重載之後拿到的是新語料的答案（實際 " + after + "）",
              "进行中".equals(after));
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
