package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.List;
import java.util.Optional;

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
 * <p>先前把所有有前導偏移的行都當成置中，結果是未鑑定物品修好了、
 * 配方清單卻被推成置中。單看一行資訊不足，只能<b>看整塊</b>。
 *
 * <h2>怎麼分</h2>
 * 置中的行，前導偏移必須<b>逐行不同</b>——那正是它用來對齊中線的方式。
 * 靠左的行則整塊共用同一個縮排。所以規則是：
 *
 * <blockquote>同一塊裡的前導偏移<b>不全都相同</b> → 這一塊是置中的。</blockquote>
 *
 * <p>「一塊」以空行分隔。tooltip 本來就是用空行分段的，段落內的對齊方式一致。
 *
 * <p>全部相同的置中塊（每行等寬）會被判成靠左，但那種情況本來就不需要調整，
 * 兩種判斷的結果一樣。
 */
public final class BlockLayout {

    private BlockLayout() {}

    /**
     * 每一行是不是置中的。
     *
     * @return 與輸入等長；{@code true} 表示這一行的前導偏移該跟著譯文寬度調整
     */
    public static boolean[] centered(List<Component> lines) {
        int n = lines.size();
        boolean[] result = new boolean[n];
        if (n == 0) {
            return result;
        }
        int[] lead = new int[n];
        boolean[] blank = new boolean[n];
        for (int i = 0; i < n; i++) {
            lead[i] = leadingOffset(lines.get(i));
            blank[i] = lines.get(i).getString().isBlank();
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
            // 這一段裡的前導偏移只要不是全部相同，就是靠改縮排來對齊中線
            boolean varies = false;
            for (int i = start + 1; i < end; i++) {
                if (lead[i] != lead[start]) {
                    varies = true;
                    break;
                }
            }
            for (int i = start; i < end; i++) {
                result[i] = varies;
            }
            start = end;
        }
        return result;
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
}
