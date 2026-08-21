package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

/**
 * 判斷 tooltip 裡哪些行是<b>置中</b>的。
 *
 * <h2>為什麼一行一行看不出來</h2>
 * 置中與靠左，在單獨一行上長得<b>一模一樣</b>：都是「一段前導偏移，後面接文字」。
 *
 * <pre>
 *   未鑑定物品的說明（置中）      材料的配方清單（靠左）
 *   偏移 0  ← 最寬的那行          偏移 0
 *   偏移 4                        偏移 0
 *   偏移 49                       偏移 0
 * </pre>
 *
 * 兩邊的第一行都是「偏移 0」，但正確的處理完全相反：置中的行譯文變短時要
 * <b>加大</b>縮排才會維持置中；靠左的行加了縮排就變成整段歪掉。
 *
 * <h2>怎麼分</h2>
 * 置中的行，前導偏移必然滿足一條算式：
 *
 * <blockquote>{@code 偏移 ≈ (整塊最寬 − 本行寬) / 2}</blockquote>
 *
 * 逐行驗證這條算式，<b>每一行都吻合</b>才算這一塊是置中的。只要有一行對不上，
 * 那個縮排就是別的用途（版面裝飾、清單縮排），整塊都不該動。
 *
 * <p>先前試過「偏移不全相同就是置中」，那太寬鬆——材料 tooltip 裡
 * {@code 4 Crafting Level} 的偏移剛好和配方清單不同，整塊就被誤判成置中推歪了。
 *
 * <p>「一塊」以空行分隔。tooltip 本來就是用空行分段的，段落內的對齊方式一致。
 */
public final class BlockLayout {

    /**
     * 偏移與算式的容許誤差（像素）。
     *
     * <p>伺服器算置中時會取整，所以不能要求完全相等。6 像素大約是一個字元寬，
     * 放寬到這裡仍然遠小於「靠左」與「置中」之間的差距（動輒數十像素）。
     */
    private static final int TOLERANCE = 6;

    /**
     * 沒有分隔線時，一串連續的行至少要這麼多行才敢當成置中。
     *
     * <p>兩行的「巧合吻合」太容易發生——任兩行只要寬度差得夠少，
     * 縮排 0 就會落在容差內。三行以上才有足夠的證據。
     */
    private static final int MIN_RUN = 3;

    private BlockLayout() {}

    /**
     * 每一行是不是置中的。
     *
     * @return 與輸入等長；{@code true} 表示這一行的前導偏移該跟著譯文寬度調整
     */
    public static boolean[] centered(List<Component> lines) {
        boolean[] result = centered(lines, BlockLayout::measure);
        LayoutDebug.record(lines, result);
        return result;
    }

    /**
     * 把置中判斷的中間值寫成人看得懂的樣子。
     *
     * <p>判斷失敗時畫面上只看得到「沒有跟著置中」，看不出是分段切錯、寬度量錯、
     * 還是算式差了幾像素——三種的修法完全不同。
     */
    static String explain(List<Component> lines, boolean[] result) {
        return explain(lines, result, BlockLayout::measure);
    }

    static String explain(List<Component> lines, boolean[] result,
                          ToIntFunction<Component> width) {
        StringBuilder sb = new StringBuilder();
        int widest = 0;
        for (int i = 0; i < lines.size(); i++) {
            widest = Math.max(widest, width.applyAsInt(lines.get(i)));
        }
        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int lead = leadingOffset(line);
            int content = Math.max(0, width.applyAsInt(line) - lead);
            String text = line.getString().replace("\n", "  ");
            sb.append(String.format(
                    "  [%2d] %-8s 縮排 %4d  內容 %4d  置中應為 %4d  %s%n",
                    i,
                    isSeparator(line) ? "分隔線" : (result[i] ? "置中" : "靠左"),
                    lead, content, (widest - content) / 2,
                    text.length() > 34 ? text.substring(0, 34) + "…" : text));
        }
        sb.append(String.format("  整份最寬 %d px%n", widest));
        return sb.toString();
    }

    /** 供測試注入寬度計算——正式路徑要有字型才量得出來。 */
    static boolean[] centered(List<Component> lines, ToIntFunction<Component> width) {
        int n = lines.size();
        boolean[] result = new boolean[n];
        if (n == 0) {
            return result;
        }
        int[] lead = new int[n];
        int[] content = new int[n];
        boolean[] blank = new boolean[n];
        for (int i = 0; i < n; i++) {
            Component line = lines.get(i);
            blank[i] = isSeparator(line);
            lead[i] = leadingOffset(line);
            // 量出來的寬度含前導偏移，扣掉才是實際內容有多寬
            content[i] = Math.max(0, width.applyAsInt(line) - lead[i]);
        }

        int start = 0;
        while (start < n) {
            if (blank[start]) {
                start++;
                continue;
            }
            int end = start;
            while (end < n && !blank[end]) {
                end++;
            }
            markCentred(result, lead, content, start, end);
            start = end;
        }
        return result;
    }

    /**
     * 標出這一段裡置中的行。
     *
     * <h2>為什麼不能只看整段</h2>
     * tooltip 的分段<b>不一定有分隔線</b>。未鑑定物品就是這樣：
     *
     * <pre>
     *   {@literal 🔒} Waist Apron              ← 靠左，縮排 0
     *   [RARE] LEGGINGS                ← 靠左，縮排 0
     *   This item's power has been sealed,   ← 置中，縮排 0（最寬的那行）
     *     an {@literal ◉} Item Identifier can unlock  ← 置中，縮排 4
     *            its potential.        ← 置中，縮排 52
     * </pre>
     *
     * 名稱、稀有度跟後面那三行中間沒有任何分隔，整段一起判斷時前兩行對不上
     * 置中的算式，於是<b>五行全部</b>被判成靠左，一行都沒調整——譯文變短之後
     * 整塊往左塌。
     *
     * <p>所以整段對不上時，再找<b>段內最長的一串</b>吻合的行。要求至少
     * {@value #MIN_RUN} 行，而且其中要有明顯的縮排：兩行的巧合太容易發生，
     * 縮排全是 0 的清單則本來就不是置中。
     */
    private static void markCentred(boolean[] result, int[] lead, int[] content,
                                    int start, int end) {
        if (isCentredBlock(lead, content, start, end)) {
            java.util.Arrays.fill(result, start, end, true);
            return;
        }
        int bestFrom = -1;
        int bestLen = 0;
        for (int from = start; from + MIN_RUN <= end; from++) {
            for (int to = end; to - from > bestLen && to - from >= MIN_RUN; to--) {
                if (isCentredBlock(lead, content, from, to)) {
                    bestFrom = from;
                    bestLen = to - from;
                    break;              // 由長到短試，第一個吻合的就是這個起點最長的
                }
            }
        }
        if (bestFrom >= 0) {
            java.util.Arrays.fill(result, bestFrom, bestFrom + bestLen, true);
        }
    }

    /**
     * 這一段的每一行，前導偏移是不是都吻合置中的算式。
     *
     * <p>只有一行、或每行都一樣寬時，「置中」與「靠左」畫出來沒有差別，
     * 一律當靠左——不動它最安全。
     */
    private static boolean isCentredBlock(int[] lead, int[] content, int start, int end) {
        if (end - start < 2) {
            return false;
        }
        int minLead = Integer.MAX_VALUE;
        int maxLead = Integer.MIN_VALUE;
        int minSpan = Integer.MAX_VALUE;
        int maxSpan = Integer.MIN_VALUE;
        for (int i = start; i < end; i++) {
            minLead = Math.min(minLead, lead[i]);
            maxLead = Math.max(maxLead, lead[i]);
            // 置中的行，左右留白一樣寬，所以「本行內容 + 兩側留白」就是整塊的寬度。
            // 每一行算出來的這個值都該相同。
            int span = content[i] + lead[i] * 2;
            minSpan = Math.min(minSpan, span);
            maxSpan = Math.max(maxSpan, span);
        }
        // 縮排完全沒變化的話，這一塊「置中」與「靠左」畫出來一模一樣，當靠左最安全。
        if (maxSpan <= 0 || maxLead - minLead <= TOLERANCE) {
            return false;
        }
        // 越長的行縮排越小——這是置中的定義，而且它<b>不受量測誤差影響</b>：
        // 就算整塊的寬度都量偏了，長短的順序還是對的。
        //
        // 這條很重要。未鑑定物品那三行有兩行用 Wynncraft 自己的字型
        // （language/wynncraft），量出來跟預設字型差得夠多，光比像素會對不上，
        // 但長的行縮排小、短的行縮排大這件事永遠成立。
        for (int i = start; i < end; i++) {
            for (int j = i + 1; j < end; j++) {
                if (contradicts(lead, content, i, j) || contradicts(lead, content, j, i)) {
                    return false;
                }
            }
        }
        // 像素上的容差用<b>比例</b>而不是固定值：量測誤差會隨字型與行長放大，
        // 固定幾像素在長行上太嚴，在短行上又太鬆。
        int allowed = Math.max(TOLERANCE * 2, maxSpan * SPAN_TOLERANCE_PERCENT / 100);
        return maxSpan - minSpan <= allowed;
    }

    /**
     * {@code a} 明顯比 {@code b} 長，縮排卻也明顯比較大——置中不可能長這樣。
     *
     * <p>兩邊都要求「明顯」，是因為量測本來就有誤差：差幾像素的兩行誰長誰短
     * 說不準，拿它當否決條件會把真正置中的區塊擋掉。要否決就得是那種
     * 一看就知道不對的差距。
     */
    private static boolean contradicts(int[] lead, int[] content, int a, int b) {
        return content[a] - content[b] > RANK_SLACK && lead[a] - lead[b] > RANK_SLACK;
    }

    /** 見 {@link #contradicts}。約三個字元寬。 */
    private static final int RANK_SLACK = TOLERANCE * 3;

    /** 見 {@link #isCentredBlock}。整塊寬度的百分之幾以內都算吻合。 */
    private static final int SPAN_TOLERANCE_PERCENT = 15;

    /**
     * 這一行是不是段落分隔。
     *
     * <h2>為什麼不能只看 {@code isBlank}</h2>
     * tooltip 的分隔不一定是空行——很多是<b>由排版字元組成的分隔線</b>
     * （{@code ◇} 那種、或純粹的寬度偏移）。那些行 {@code isBlank()} 是 false，
     * 於是整份 tooltip 被當成<b>同一段</b>：未鑑定物品那三行置中的說明，
     * 跟上面的物品名稱、稀有度擠在一起判斷，當然對不上置中的算式，
     * 整段就被判成靠左，一行都沒有被調整。
     *
     * <p>改成看「有沒有可讀的字」。剝掉符號之後什麼都不剩的，就是分隔線。
     */
    private static boolean isSeparator(Component line) {
        String text = line.getString();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (!Character.isWhitespace(cp) && !GlyphSplitter.isGlyphCodePoint(cp)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 這一行開頭那段排版偏移有多寬。
     *
     * <p>只看<b>第一段</b>：後面的偏移是欄位對齊，跟縮排無關。
     */
    private static int leadingOffset(Component line) {
        int[] px = {0};
        boolean[] done = {false};
        line.visit((style, text) -> {
            if (done[0] || text.isEmpty()) {
                return Optional.empty();
            }
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                px[0] += SpaceOffset.decode(text);
            } else {
                done[0] = true;                // 碰到實際內容就停
            }
            return Optional.empty();
        }, Style.EMPTY);
        return px[0];
    }

    private static int measure(Component line) {
        Minecraft mc = Minecraft.getInstance();
        return mc == null || mc.font == null ? 0 : mc.font.width(line);
    }

    /**
     * 這一行的數值是不是站在一個<b>真正的對齊欄</b>上。
     *
     * <h2>為什麼不能只看單行</h2>
     * 單行看起來都一樣：{@code 標籤 空白 +數值}。先前的判斷是「數值以正負號
     * 開頭就算欄位」，於是<b>每一條詞條</b>都被當成欄位去補償——中文標籤比英文
     * 短，補償就是一大塊空白，畫面上變成「火屬性傷害:␣␣␣␣␣+3-5」，
     * 間隔跟沒翻譯時一樣寬。
     *
     * <p>真正的欄位是伺服器<b>刻意排出來的</b>：它會在短標籤後面塞一段明顯的
     * 留白，把數值頂到定位。所以先找出這份 tooltip 裡「留白兩格以上」的那些行，
     * 它們的數值 x 就是這份 tooltip 的欄位位置；其他行只有在數值剛好落在同一個
     * x 上時才算同一欄。
     *
     * <p>{@code Elemental Spell Damage +372} 就是後者——標籤長到剛好把數值頂到
     * 欄位上，中間只剩一個空格，但它確實跟上下對齊，所以要補償。
     * {@code Main Attack Damage: +5%} 則不是：整份 tooltip 沒有任何一行的欄位
     * 落在它那個 x 上。
     */
    public static boolean[] valueColumns(List<Component> lines) {
        int n = lines == null ? 0 : lines.size();
        boolean[] result = new boolean[n];
        if (n == 0) {
            return result;
        }
        int[] valueX = new int[n];
        boolean[] padded = new boolean[n];
        for (int i = 0; i < n; i++) {
            valueX[i] = -1;
            String text = lines.get(i).getString();
            int gapStart = valueGap(text);
            if (gapStart < 0) {
                continue;
            }
            int gapEnd = gapStart;
            while (gapEnd < text.length() && Character.isWhitespace(text.charAt(gapEnd))) {
                gapEnd++;
            }
            padded[i] = gapEnd - gapStart >= MIN_PADDING;
            valueX[i] = measure(Component.literal(text.substring(0, gapEnd)));
        }
        for (int i = 0; i < n; i++) {
            if (padded[i]) {
                result[i] = true;
                continue;
            }
            if (valueX[i] < 0) {
                continue;
            }
            for (int j = 0; j < n; j++) {
                if (padded[j] && Math.abs(valueX[i] - valueX[j]) <= COLUMN_TOLERANCE) {
                    result[i] = true;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 數值前面那段空白從哪裡開始，沒有就回 -1。
     *
     * <p>從尾端往回找最後一段空白：兩格以上直接算數；只有一格的話，後面得接
     * 正負號才算——詞條的數值一定帶正負號，{@code Emerald Pouch [Tier 8]}
     * 那種接在名稱後面的標籤則不會。
     */
    private static int valueGap(String text) {
        int best = -1;
        int i = text.length();
        while (i > 0) {
            int end = i;
            while (end > 0 && !Character.isWhitespace(text.charAt(end - 1))) {
                end--;
            }
            if (end == 0) {
                break;
            }
            int start = end;
            while (start > 0 && Character.isWhitespace(text.charAt(start - 1))) {
                start--;
            }
            int width = end - start;
            String after = text.substring(end).strip();
            boolean signed = !after.isEmpty()
                    && (after.charAt(0) == '+' || after.charAt(0) == '-');
            if (width >= MIN_PADDING || signed) {
                best = start;
                break;
            }
            i = start;
        }
        return best;
    }

    /** 幾格空白才算「刻意排出來的留白」而不是詞距。 */
    private static final int MIN_PADDING = 2;

    /** 兩行的數值 x 差幾像素以內算同一欄。 */
    private static final int COLUMN_TOLERANCE = 2;
}
