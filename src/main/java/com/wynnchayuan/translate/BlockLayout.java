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
        return centered(lines, BlockLayout::measure);
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
        int widest = 0;
        int minLead = Integer.MAX_VALUE;
        int maxLead = Integer.MIN_VALUE;
        for (int i = start; i < end; i++) {
            widest = Math.max(widest, lead[i] + content[i]);
            minLead = Math.min(minLead, lead[i]);
            maxLead = Math.max(maxLead, lead[i]);
        }
        // 縮排完全沒變化的話，這一塊「置中」與「靠左」畫出來一模一樣，
        // 當靠左最安全。這條也擋掉窄區塊的誤判：兩行只差十幾像素時，
        // 光靠容差會讓「縮排都是 0」剛好符合置中算式。
        if (widest <= 0 || maxLead - minLead <= TOLERANCE) {
            return false;
        }
        for (int i = start; i < end; i++) {
            int expected = (widest - content[i]) / 2;
            if (Math.abs(lead[i] - expected) > TOLERANCE) {
                return false;      // 有一行對不上，這個縮排就不是為了置中
            }
        }
        return true;
    }

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
}
