package com.wynnchayuan.render;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TextSplit;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * <h2>整段條目不是一句話</h2>
 * 語料裡另外有一種鍵：記分板的一整段，各列用換行接起來（capture 的
 * {@code scoreboard/block} 收的就是它）——
 *
 * <pre>
 *   Slay! Wave {~} - {~} Mobs Left!\n- Time Left: {~}:{~}\n- Challenges: {~}/{~}
 * </pre>
 *
 * 查表不分換行與空白，所以三列併起來查也對得到這一條。那是三個各自成行的項目，
 * 不是折成三列的一句話：併掉換行再平均切回去，就成了使用者 2026-10-12 截圖裡的
 *
 * <pre>
 *   殲滅！第 1 波 - 還剩
 *   2 隻怪物！ - 剩餘時
 *   間: 07:20 - 挑戰: 1/12
 * </pre>
 *
 * 分辨的辦法：譯文的行數剛好等於併起來的列數，就是一行對一列，原樣擺回去，
 * 不重切。對不上的（譯文沒有換行、或行數不同）才當成一句話。
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
     * @param missing 有幾列沒翻出來（照原文擺的）
     */
    public record Plan(List<Component> panel, Map<String, StyledText> rows, boolean any,
                       int missing) {}

    /**
     * 排一段內容。
     *
     * @param content 那一段標題底下的每一列，照畫面上的順序
     */
    public static Plan plan(List<StyledText> content, TranslationStore store) {
        return plan(content, store, Set.of());
    }

    /**
     * 排一段內容，{@code kept} 裡的那幾列（去掉樣式的字）原樣照擺：不查表、
     * 不併進前後的句子、也不算「沒翻出來」。玩家名字那幾列用，見 {@link #keptRows}。
     */
    public static Plan plan(List<StyledText> content, TranslationStore store,
                            Set<String> kept) {
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
        int missing = 0;
        int i = 0;
        while (i < live.size()) {
            if (kept.contains(key(live.get(i)))) {
                panel.add(LineTranslator.untranslated(live.get(i)));
                i++;
                continue;
            }
            // 併句只到下一列玩家名字之前：名字那一列不能被拼進別人的句子裡
            int stop = Math.min(live.size(), i + MAX_RUN);
            for (int k = i + 1; k < stop; k++) {
                if (kept.contains(key(live.get(k)))) {
                    stop = k;
                    break;
                }
            }
            int end = -1;
            Component whole = null;
            // 從最長的開始試：四列是一句的時候，前兩列湊巧也是一句的話要整句優先
            for (int j = stop - 1; j > i; j--) {
                whole = sentence(live.subList(i, j + 1), store);
                if (whole != null) {
                    end = j;
                    break;
                }
            }
            if (whole != null) {
                int count = end - i + 1;
                List<Component> parts = TextSplit.lines(whole);
                if (parts.size() == count) {
                    // 整段條目：譯文一行對原文一列，面板也是一項一行
                    panel.addAll(parts);
                } else {
                    whole = oneLine(whole);
                    panel.add(whole);
                    parts = split(whole, count);
                }
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
            } else {
                missing++;
            }
            panel.add(one != null ? one : LineTranslator.untranslated(row));
            i++;
        }
        return new Plan(panel, rows, any, missing);
    }

    /** 隊伍那一段的類別簡名（Wynntils 的 {@code PartyScoreboardPart}）。 */
    static final String PARTY_PART = "PartyScoreboardPart";

    /** 隊伍那一段的標題：「Party: [Lv. 481]」。類別名認不出來時靠這個。 */
    private static final Pattern PARTY_HEADER =
            Pattern.compile("^\\s*Party:\\s*\\[Lv\\. ?\\d+]\\s*$");

    /** 名字裡可能有的字：Minecraft 帳號名只有這幾種。 */
    private static final Pattern NAME_TOKEN = Pattern.compile("[A-Za-z0-9_]{3,16}");

    /** 方括號那一段（血條、等級）。 */
    private static final Pattern BRACKETS = Pattern.compile("\\[[^\\]]*]");

    /**
     * 這一段裡哪幾列是<b>玩家的名字</b>，一律不翻。
     *
     * <h2>實機回報（2026-10-27 的截圖）</h2>
     * 隊伍那一段的四列成員裡，名叫 {@code General} 的那位變成了「綜合」——
     * Lootrun 的語料裡有 {@code "General": "綜合"}，記分板那一列照一般的路查表，
     * 名字就被當成介面文字換掉了。玩家名字是誰都可以取的，跟語料撞名只是遲早的事。
     *
     * <h2>怎麼認</h2>
     * <ul>
     *   <li><b>隊伍那一段整段都是成員</b>：認 Wynntils 的類別名，認不出來時認標題
     *       「Party: [Lv. N]」。標題本身照翻，底下每一列原樣。</li>
     *   <li>別段裡的列：只要有一個詞<b>就是</b>已知隊友的名字（不分大小寫——
     *       伺服器有時送全大寫）就原樣。名單是隊伍那一段解出來的名字，加上
     *       Wynntils 的 {@code PartyModel}，由呼叫的那一邊給。</li>
     * </ul>
     * 不靠「長得像不像名字」：{@code General} 跟介面文字在字形上分不出來。
     *
     * @param part    那一段的 {@code ScoreboardPart} 類別簡名；認不出來時 {@code null}
     * @param header  那一段的標題
     * @param content 標題底下的每一列
     * @param names   已知的玩家名字（大小寫不拘）
     * @return 要原樣照擺的那幾列（去掉樣式的字，跟 {@link #key} 一樣）
     */
    public static Set<String> keptRows(String part, StyledText header,
                                       List<StyledText> content, Collection<String> names) {
        if (content == null || content.isEmpty()) {
            return Set.of();
        }
        Set<String> kept = new HashSet<>();
        if (isParty(part, header)) {
            for (StyledText row : content) {
                kept.add(key(row));
            }
            return kept;
        }
        Set<String> known = new HashSet<>();
        if (names != null) {
            for (String name : names) {
                if (name != null && NAME_TOKEN.matcher(name).matches()) {
                    known.add(name.toLowerCase(Locale.ROOT));
                }
            }
        }
        if (known.isEmpty()) {
            return Set.of();
        }
        for (StyledText row : content) {
            String plain = key(row);
            for (String word : words(plain)) {
                if (known.contains(word.toLowerCase(Locale.ROOT))) {
                    kept.add(plain);
                    break;
                }
            }
        }
        return kept;
    }

    /** 這一段是不是隊伍那一段。 */
    public static boolean isParty(String part, StyledText header) {
        if (part != null && part.contains(PARTY_PART)) {
            return true;
        }
        return header != null && PARTY_HEADER.matcher(key(header)).matches();
    }

    /**
     * 隊伍那一段的一列裡的名字：「- [||||] General [120]」→ {@code General}，
     * 離線的「- General」也是。方括號（血條、等級）拿掉之後剩下的最後一個詞。
     *
     * @return 認不出來時 {@code null}
     */
    public static String partyName(StyledText row) {
        List<String> found = words(BRACKETS.matcher(key(row)).replaceAll(" "));
        return found.isEmpty() ? null : found.get(found.size() - 1);
    }

    /** 一列裡一個一個完整的詞（前後不黏著別的英數字），長度合乎帳號名。 */
    private static List<String> words(String plain) {
        List<String> out = new ArrayList<>();
        Matcher m = NAME_TOKEN.matcher(plain);
        while (m.find()) {
            boolean whole = (m.start() == 0 || !isNameChar(plain.charAt(m.start() - 1)))
                    && (m.end() == plain.length() || !isNameChar(plain.charAt(m.end())));
            if (whole) {
                out.add(m.group());
            }
        }
        return out;
    }

    private static boolean isNameChar(char c) {
        return c == '_' || (c < 128 && Character.isLetterOrDigit(c));
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
     * 這幾列併成一句查不查得到；查得到就回整句的譯文（換行還在，由 {@link #plan} 決定
     * 是一行對一列還是併成一句）。
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
        return LineTranslator.translate(joined, store);
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
