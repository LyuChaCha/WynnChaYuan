package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 被 tooltip 寬度<b>自動斷行</b>的長敘述要查得到。
 *
 * <h2>畫面上長什麼樣</h2>
 * 裝備的背景敘述在語料裡是<b>沒有斷行的一整句</b>，遊戲卻依畫面寬度把它切成
 * 十來行。兩邊字面上永遠不相等，所以查表得先把那幾行併回一句。
 *
 * <p>這條路先前有兩個各自獨立的破口，合起來讓<b>整批</b>裝備敘述都不生效——
 * 譯文明明在檔案裡，畫面上還是整段英文：
 *
 * <ol>
 *   <li>併起來的行數上限是 8，而敘述普遍十行以上，永遠湊不齊。</li>
 *   <li>地名是<b>逐行</b>認的。「the peak of the Tower of ⏎ Ascension,」
 *       被切開之後兩行都不含完整地名，模板裡留著原樣英文；
 *       而語料裡存的是 {@code {p}}。</li>
 * </ol>
 *
 * <p>兩個都得修才會生效，所以這條測試同時盯著它們。
 */
public final class FlowedBlockTest {

    private static int failures = 0;

    private static final int BODY = 0xAAAAAA;

    /** Oblivion 的背景敘述，照遊戲的寬度斷行。地名剛好斷在第二、三行之間。 */
    private static final String[] LORE = {
            "This sacrificial kris, fallen from",
            "the peak of the Tower of",
            "Ascension, is completely hollow.",
            "To wield it is to invite to",
            "yourself the same hollowness. To",
            "become such an aberration, to",
            "unwrite yourself in pursuit of",
            "power...it must be fed of its",
            "wielder til only the barest",
            "existence remains.",
    };

    public static void main(String[] args) {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations"));

        check("掃描深度夠深（實際 " + store.maxBlockLines() + "）",
                store.maxBlockLines() >= LORE.length);

        String lore = joined(block(LORE), store);
        check("十行的敘述查得出來（實際："
                        + shorten(lore) + "）",
                lore != null && lore.startsWith("這把從"));
        check("被斷行切開的地名有填回去", lore != null
                && lore.contains("Tower of Ascension"));
        check("譯文裡不留 {p}", lore != null && !lore.contains("{p}"));

        // 反面：湊不齊的那幾行不能硬翻。少了最後兩行就不是完整的那一句，
        // 這時候應該讓原文照樣顯示，而不是拿半句話去撞別的條目。
        check("湊不齊就不硬翻",
                LineTranslator.translateBlock(
                        block(java.util.Arrays.copyOf(LORE, 8)),
                        store, new boolean[8]) == null);

        // 整份 tooltip 走一遍。單獨一段會過，不代表放進上下文裡也會過——
        // 呼叫端是「由長到短試、中了就往後跳」，前面某一段多吃了幾行的話，
        // 起點就會落在段落中間，那一段從此再也湊不出來。
        List<Component> tip = new ArrayList<>();
        tip.add(plain("Oblivion"));
        tip.add(plain(""));
        for (String line : LORE) {
            tip.add(plain(line));
        }
        tip.add(plain(""));
        tip.add(plain("Combat Level Min: 101"));
        List<Component> out =
                com.wynnchayuan.render.TooltipPanel.translateLines(tip, store);
        String all = out.stream().map(Component::getString)
                .reduce("", (a, b) -> a + " " + b);
        check("放進整份 tooltip 也翻得出來（實際：" + shorten(all) + "）",
                all.contains("獻祭之刃"));

        System.out.println(failures == 0
                ? "FlowedBlock: 全部通過" : "FlowedBlock: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static String joined(List<StyledText> run, TranslationStore store) {
        List<Component> out = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        return out == null ? null
                : out.stream().map(Component::getString).reduce("", String::concat);
    }

    private static List<StyledText> block(String[] lines) {
        List<StyledText> run = new ArrayList<>(lines.length);
        for (String line : lines) {
            run.add(StyledText.fromComponent(plain(line)));
        }
        return run;
    }

    private static MutableComponent plain(String text) {
        return Component.literal(text)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(BODY)));
    }

    private static String shorten(String text) {
        if (text == null) {
            return "null";
        }
        String flat = text.replace("\n", " ").strip();
        return flat.length() <= 34 ? flat : flat.substring(0, 34) + "…";
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
