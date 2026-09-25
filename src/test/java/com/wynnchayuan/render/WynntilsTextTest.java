package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 就地取代 Wynntils 疊層裡的字。
 *
 * <h2>為什麼要有這一份</h2>
 * 真正動手的是 mixin，而 mixin 只有在遊戲裡才跑得到。所以判斷與翻譯全部
 * 留在 {@link WynntilsText}，這裡直接呼叫它——關掉開關時一個字都不准動、
 * 打開時該翻的要翻、翻不出來的要<b>原樣</b>回去（換成空字串就是把
 * 別人的疊層弄壞）。
 */
public final class WynntilsTextTest {

    private static int failures = 0;

    /** 名字就是判斷依據（見 {@link WynntilsText#TRACKER}），所以照抄一個。 */
    private static final class ContentTrackerOverlay { }

    /** 別的疊層不准碰。 */
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
        wynntilsScreens(config, store);

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

        // 追蹤欄的第一行是 Wynntils 拼的：「Quest - <任務名>」。整行不在語料裡，
        // 但每一段都在——類型是介面字串、任務名在 quest-name.json。
        StyledText composed = StyledText.fromString(
                "§b§lQuest§7 - §fStar Thief");
        StyledText[] header = WynntilsText.lines(
                tracker, new StyledText[] {composed}, config, store);
        String shown = header[0].getStringWithoutFormatting();
        check("「Quest - 任務名」兩段都換掉（實際 " + shown + "）",
              shown.contains("任務") && shown.contains("竊星者"));
        check("顏色沒有被抹掉", header[0].getString().contains("§b"));

        // 實機那一行整行同一個顏色，Wynntils 送來的是<b>一整段</b>，破折號是 en dash。
        StyledText single = StyledText.fromString("§aQuest – Dearly Departed");
        StyledText[] one = WynntilsText.lines(
                tracker, new StyledText[] {single}, config, store);
        String got = one[0].getStringWithoutFormatting();
        check("一整段的「Quest – 任務名」也換得掉（實際 " + got + "）",
              got.contains("任務") && got.contains("音容宛在"));

        // 實機的標題帶著 Wynncraft 的 language/wynncraft 字型（ContentTrackerOverlay 的樣板
        // 用 with_font 包起來），那份字型沒有中文字，照抄字型就是一排方框。
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
        check("關掉時原樣回去",
              WynntilsText.objective("Slay Mobs: 12/100", config, store)
                      .equals("Slay Mobs: 12/100"));
        config.toggleObjectives();
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
        config.toggleNametags();
        check("名牌翻譯關掉時 boss bar 原樣回去", WynntilsText.bossBar(bar, config, store) == bar);
        config.toggleNametags();
    }

    /** 盔甲座疊出來的浮空字：討伐戰祭壇上方那種。 */
    private static void entityName(CollectorConfig config, TranslationStore store) {
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
        config.toggleNametags();
        check("名牌翻譯關掉時原樣回去", WynntilsText.entityName(altar, config, store) == altar);
        config.toggleNametags();
    }

    /**
     * Wynntils 自己那幾個畫面上的字（綜合頁面左邊那一列、分頁標題⋯⋯）。
     *
     * <h2>為什麼這一條值得釘</h2>
     * 那些字有一半是 Wynncraft 送來的內容（任務名、洞穴名、
     * 「Currently in progress」），Wynntils 的語言檔永遠不會有它們，
     * 但我們的語料裡早就有。打在它的 {@code FontRenderer} 入口就換得到。
     *
     * <p>真正的風險是<b>換太多</b>：這個入口所有字都會經過，
     * 所以「查不到就原樣回去」與「關掉就完全不動」兩邊都要測。
     */
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

        // 公會戰地圖那一格領地標籤：從 TerritoryPoi 進去到它出來，一律不翻。
        // 實機那一格的公會叫 Fox，被 npc.json 的「Fox: 狐狸」換掉了。
        WynntilsText.holdTerritoryLabels(true);
        check("★ 領地標籤裡的字原樣回去（公會名是玩家取的，撞名躲不完）",
              WynntilsText.screenText(inProgress, config, store) == inProgress);
        WynntilsText.holdTerritoryLabels(false);
        check("★ 出了領地標籤就恢復",
              "進行中".equals(WynntilsText.screenText(inProgress, config, store).getString()));
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
