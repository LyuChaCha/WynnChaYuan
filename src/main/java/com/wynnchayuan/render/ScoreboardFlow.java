package com.wynnchayuan.render;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TextSplit;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 記分板上<b>被折成好幾列的一句話</b>，併起來當一句翻。
 *
 * <h2>為什麼要有</h2>
 * 記分板一列只有二十個字上下，伺服器把目標敘述自己折好再送：
 *
 * <pre>
 *   Slay the Knightmare
 *   by destroying its
 *   shields atop the
 *   statue.
 * </pre>
 *
 * 一列一列查，查到的是四個半句——「the」這種列根本沒得翻，而中文的語序跟英文
 * 不一樣，半句照原順序排出來也不成句。玩家送來的 capture 裡這種半句一次就是
 * 六十幾條（2026-10-08）。
 *
 * <p>所以先試「連續幾列併成一句」查不查得到：查得到就整句翻，再照原本的列數
 * 切回去（就地取代那條路一次只畫一列，列數不能變）；查不到才退回一列一列查，
 * 跟原本一樣。
 *
 * <h2>順便擋掉的一個方框</h2>
 * 一列一列查的時候，「Hold the platform」會對到漂浮字那一條
 * 「Hold\nthe platform」（查表不分換行與空白），拿回來的譯文帶著換行。
 * 記分板一列只畫一行，換行字元就被畫成一個方框——使用者 2026-10-08 回報的
 * 「守住□這座平台」。原文沒有換行的地方，譯文裡的換行一律併掉，見 {@link #oneLine}。
 *
 * <p>這個類別不碰遊戲的畫面，只算；設定與面板由呼叫的那一邊決定。
 */
public final class ScoreboardFlow {

    private ScoreboardFlow() {}

    /** 一句話最多折成幾列。再長的敘述記分板放不下，試下去只是白查。 */
    static final int MAX_RUN = 6;

    /**
     * 一段內容排好的結果。
     *
     * @param panel 給我們自己那個面板的：一句一行，面板會自己折
     * @param rows  給就地取代的：原本那一列的字（去掉樣式）→ 換上去的那一列
     * @param any   有沒有任何一列真的翻出來
     */
    public record Plan(List<Component> panel, Map<String, StyledText> rows, boolean any) {}

    /**
     * 排一段內容。
     *
     * @param content 那一段標題底下的每一列，照畫面上的順序
     */
    public static Plan plan(List<StyledText> content, TranslationStore store) {
        List<StyledText> live = new ArrayList<>();
        if (content != null) {
            for (StyledText row : content) {
                if (row != null && !GlyphSplitter.isGlyphOnly(row)
                        && !GlyphSplitter.toTemplate(row).isBlank()) {
                    live.add(row);
                }
            }
        }
        List<Component> panel = new ArrayList<>();
        Map<String, StyledText> rows = new LinkedHashMap<>();
        boolean any = false;
        int i = 0;
        while (i < live.size()) {
            int end = -1;
            Component whole = null;
            // 從最長的開始試：四列是一句的時候，前兩列湊巧也是一句的話要整句優先
            for (int j = Math.min(live.size(), i + MAX_RUN) - 1; j > i; j--) {
                whole = sentence(live.subList(i, j + 1), store);
                if (whole != null) {
                    end = j;
                    break;
                }
            }
            if (whole != null) {
                panel.add(whole);
                List<Component> parts = split(whole, end - i + 1);
                for (int k = 0; k <= end - i; k++) {
                    rows.put(key(live.get(i + k)), styled(parts.get(k)));
                }
                any = true;
                i = end + 1;
                continue;
            }
            StyledText row = live.get(i);
            Component one = LineTranslator.translate(row, store);
            if (one != null) {
                one = oneLine(one);
                rows.put(key(row), styled(one));
                any = true;
            }
            panel.add(one != null ? one : LineTranslator.untranslated(row));
            i++;
        }
        return new Plan(panel, rows, any);
    }

    /** 那一列在對照表裡的鍵：畫面上的字，去掉樣式。 */
    public static String key(StyledText row) {
        return row == null ? "" : row.getStringWithoutFormatting();
    }

    private static StyledText styled(Component part) {
        return part == null || part.getString().isEmpty()
                ? StyledText.EMPTY : StyledText.fromComponent(part);
    }

    /**
     * 這幾列併成一句查不查得到；查得到就回整句的譯文。
     *
     * <p>先問語料「有沒有這一整句」才翻：{@link LineTranslator#translate} 查不到整句
     * 時會退去換句子裡的詞，那樣隨便哪幾列併起來都「翻得出一點東西」，分不出
     * 哪幾列真的是一句。
     */
    static Component sentence(List<StyledText> run, TranslationStore store) {
        StyledText joined = StyledText.join(" ", run);
        String template = GlyphSplitter.toTemplate(joined);
        if (template.isBlank() || store.lookup(template) == null) {
            return null;
        }
        Component hit = LineTranslator.translate(joined, store);
        return hit == null ? null : oneLine(hit);
    }

    // 併掉換行與切列的算法在 TextSplit（信標面板的兩欄也用同一套），這裡只是轉手

    /** 見 {@link TextSplit#oneLine}。 */
    public static Component oneLine(Component text) {
        return TextSplit.oneLine(text);
    }

    /** 見 {@link TextSplit#split}。 */
    static List<Component> split(Component whole, int rows) {
        return TextSplit.split(whole, rows);
    }
}
