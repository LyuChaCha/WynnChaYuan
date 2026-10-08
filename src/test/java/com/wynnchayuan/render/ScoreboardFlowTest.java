package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 記分板那幾列：折成好幾列的一句話併起來翻、譯文不帶換行、開關關得掉。
 *
 * <h2>實機回報（2026-10-08）</h2>
 * 討伐戰的目標在記分板上顯示成「守住□這座平台」——中間一個方框。那一列的原文是
 * 「Hold the platform」，查表不分換行與空白，於是對到漂浮字那一條
 * 「Hold\nthe platform」，譯文裡的換行被畫成方框。
 *
 * <p>同一次回報還要了記分板的開關，而玩家送來的 capture 裡記分板的半句一次就是
 * 六十幾條（「Slay the Knightmare」「by destroying its」「shields atop the」
 * 「statue.」）——半句沒辦法翻，所以多了「併成一句再切回去」。
 *
 * <p>譯文的措辭不寫死在這裡（術語統一的 PR 會改它）：只看有沒有換行、有沒有
 * 中文、切回去的列數對不對。整句那一條是這個測試自己疊上去的一層。
 */
public final class ScoreboardFlowTest {

    private static int failures = 0;

    private static final String SENTENCE =
            "Slay the Knightmare by destroying its shields atop the statue.";
    private static final String SENTENCE_ZH = "摧毀雕像頂端的護盾，擊殺夢魘騎士。";
    private static final String TIMED = "Hold the Upper and Lower platforms for {~} seconds";
    private static final String TIMED_ZH = "守住上層與下層平台 {~} 秒";

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        Path corpus = Path.of("src/main/resources/assets/wynnchayuan/translations",
                Languages.DEFAULT);
        Path extra = Files.createTempDirectory("wcy-board");
        Files.writeString(extra.resolve("raid.json"),
                "{\n \"" + SENTENCE + "\": \"" + SENTENCE_ZH + "\",\n \""
                        + TIMED + "\": \"" + TIMED_ZH + "\"\n}\n", StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(List.of(corpus, extra));
        check("疊上去的那一層讀得到", SENTENCE_ZH.equals(store.lookup(SENTENCE)));

        oneLine(store);
        merged(store);
        numbers(store);
        split();
        toggle(store);

        System.out.println(failures == 0 ? "\n記分板併句：全部通過"
                : "\n記分板併句：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    /** 回報的那一列：翻得出來，而且譯文裡沒有換行。 */
    private static void oneLine(TranslationStore store) {
        List<StyledText> rows = List.of(StyledText.fromString("Hold the platform"));
        ScoreboardFlow.Plan plan = ScoreboardFlow.plan(rows, store);
        String shown = plan.panel().get(0).getString();
        check("「Hold the platform」翻得出來（" + shown + "）", plan.any() && hasCjk(shown));
        check("★ 譯文裡沒有換行（換行會被畫成方框）", shown.indexOf('\n') < 0);
        StyledText inPlace = plan.rows().get("Hold the platform");
        check("就地取代那一份也沒有換行",
                inPlace != null && inPlace.getString().indexOf('\n') < 0);

        // 換行兩邊都是拉丁字的要補空白，不能黏成一個字
        Component latin = Component.literal("Удерживайте\nплатформу");
        check("拉丁字之間的換行換成空白",
                "Удерживайте платформу".equals(ScoreboardFlow.oneLine(latin).getString()));
        Component mixed = Component.literal("守住\n這座平台");
        check("中文之間的換行直接接起來",
                "守住這座平台".equals(ScoreboardFlow.oneLine(mixed).getString()));
        Component plain = Component.literal("沒有換行");
        check("沒有換行的原樣回傳同一個物件", ScoreboardFlow.oneLine(plain) == plain);
    }

    /** 四列是一句話：併起來翻，再切回四列。 */
    private static void merged(TranslationStore store) {
        List<StyledText> rows = List.of(
                StyledText.fromString("Slay the Knightmare"),
                StyledText.fromString("by destroying its"),
                StyledText.fromString("shields atop the"),
                StyledText.fromString("statue."));
        ScoreboardFlow.Plan plan = ScoreboardFlow.plan(rows, store);
        check("★ 四列併成一句：面板只有一行", plan.panel().size() == 1);
        check("面板那一行就是整句的譯文",
                SENTENCE_ZH.equals(plan.panel().get(0).getString()));
        check("★ 就地取代：四列各有一份（列數不能少）", plan.rows().size() == 4);
        StringBuilder back = new StringBuilder();
        for (StyledText row : rows) {
            StyledText piece = plan.rows().get(ScoreboardFlow.key(row));
            back.append(piece == null ? "<null>" : piece.getString());
        }
        check("四列接回去就是整句，一個字都沒掉（" + back + "）",
                SENTENCE_ZH.equals(back.toString()));
        StyledText last = plan.rows().get("statue.");
        check("標點不落單：最後一列不是只有一個句號",
                last == null || last.getString().length() != 1);

        // 前面多一列不相干的：那一列自己翻，後面四列照樣併
        List<StyledText> more = List.of(
                StyledText.fromString("Spikes: Activated"),
                rows.get(0), rows.get(1), rows.get(2), rows.get(3));
        ScoreboardFlow.Plan mixed = ScoreboardFlow.plan(more, store);
        check("前面有不相干的一列時，後面四列還是併成一句", mixed.panel().size() == 2
                && SENTENCE_ZH.equals(mixed.panel().get(1).getString()));

        // 查不到整句的，一列一列照舊——不可以因為併句就整段變空
        List<StyledText> unknown = List.of(
                StyledText.fromString("Zzyzx qwfp"),
                StyledText.fromString("arst neio."));
        ScoreboardFlow.Plan none = ScoreboardFlow.plan(unknown, store);
        check("查不到的維持一列一行、原文照擺", none.panel().size() == 2 && !none.any()
                && "Zzyzx qwfp".equals(none.panel().get(0).getString()));
    }

    /** 帶倒數的那一種：數字要回到譯文裡。 */
    private static void numbers(TranslationStore store) {
        List<StyledText> rows = List.of(
                StyledText.fromString("Hold the Upper and"),
                StyledText.fromString("Lower platforms for"),
                StyledText.fromString("51 seconds"));
        ScoreboardFlow.Plan plan = ScoreboardFlow.plan(rows, store);
        String shown = plan.panel().isEmpty() ? "" : plan.panel().get(0).getString();
        check("帶數字的句子併得起來，數字填回去了（" + shown + "）",
                plan.panel().size() == 1 && shown.contains("51") && !shown.contains("{~}"));
        check("鍵是當下畫面上的字（含數字），下一秒換一份",
                plan.rows().containsKey("51 seconds"));
    }

    private static void split() {
        List<Component> parts = ScoreboardFlow.split(
                Component.literal("擊敗 12 隻 Corrupted Zombie 之後回報"), 3);
        check("切成指定的列數", parts.size() == 3);
        StringBuilder all = new StringBuilder();
        boolean wordKept = false;
        for (Component part : parts) {
            String s = part.getString();
            all.append(s);
            wordKept |= s.contains("Corrupted");
            check("列首列尾沒有多餘的空白（「" + s + "」）", s.equals(s.strip()));
        }
        check("英文單字與數字不拆開", wordKept && !all.toString().contains("Corrupte d"));
        List<Component> few = ScoreboardFlow.split(Component.literal("好"), 3);
        check("字不夠分的時候後面是空的，列數不變", few.size() == 3
                && few.get(2).getString().isEmpty());
        // 樣式要跟著字走
        Component styled = Component.literal("守住").withStyle(s -> s.withColor(0xFF5555))
                .append(Component.literal("這座平台").withStyle(s -> s.withColor(0xFFFFFF)));
        List<Component> two = ScoreboardFlow.split(styled, 2);
        int[] red = {0};
        for (Component part : two) {
            part.visit((style, text) -> {
                if (style.getColor() != null && style.getColor().getValue() == 0xFF5555) {
                    red[0] += text.length();
                }
                return java.util.Optional.empty();
            }, net.minecraft.network.chat.Style.EMPTY);
        }
        check("切開之後顏色還在原本那幾個字上", red[0] == 2);
    }

    /** 開關：關掉之後記分板那幾列原樣回去，別的字照翻。 */
    private static void toggle(TranslationStore store) throws Exception {
        Path dir = Files.createTempDirectory("wcy-board-cfg");
        CollectorConfig on = new CollectorConfig(dir.resolve("on.json"));
        check("預設開著", on.translateScoreboard());
        Path offFile = dir.resolve("off.json");
        Files.writeString(offFile, "{\"translateScoreboard\": false}");
        CollectorConfig off = new CollectorConfig(offFile);
        check("關掉之後讀得到 false", !off.translateScoreboard());

        StyledText row = StyledText.fromString("Hold the platform");
        StyledText other = StyledText.fromString("Currently in progress");
        ScoreboardFlow.Plan plan = ScoreboardFlow.plan(List.of(row), store);
        WynntilsText.setScoreboard(Set.of("Raid:", "Hold the platform"), plan.rows());
        try {
            StyledText shown = WynntilsText.screenText(row, on, store);
            check("★ 開著：記分板那一列換成譯文，而且沒有換行",
                    shown != row && shown.getString().indexOf('\n') < 0);
            check("★ 關掉：記分板那一列原樣回去",
                    WynntilsText.screenText(row, off, store) == row);
            check("關掉只管記分板：別的字照翻",
                    WynntilsText.screenText(other, off, store) != other);

            // 沒有對照表的列走原本那條路，同樣不可以帶換行
            WynntilsText.setScoreboard(Set.of("Hold the platform"), Map.of());
            StyledText plainPath = WynntilsText.screenText(row, on, store);
            check("★ 走一般那條路時譯文也沒有換行",
                    plainPath != row && plainPath.getString().indexOf('\n') < 0);
        } finally {
            WynntilsText.setScoreboard(Set.of(), Map.of());
        }
        // 記分板上沒有這一列的時候，開關不影響它
        check("不在記分板上的字，關掉也照翻",
                WynntilsText.screenText(row, off, store) != row);
    }

    private static boolean hasCjk(String s) {
        return s.codePoints().anyMatch(cp -> cp >= 0x4E00 && cp <= 0x9FFF);
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
