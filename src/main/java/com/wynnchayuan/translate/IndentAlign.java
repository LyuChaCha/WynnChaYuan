package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * 行首縮排的續行：譯完之後起點要落回該在的位置。
 *
 * <h2>遊戲送來的樣子</h2>
 * 技能樹「效果」欄有多項時（審判 Judgement），第二、三項是另起一行、對齊在
 * 第一項的數值底下：
 *
 * <pre>
 *   ✦ Effects: +20% Damage Bonus (⚔) to Allies
 *              +20% Resistance Bonus (❁) to Allies     ← 「十個空白 ÀÀ  +20% …」
 * </pre>
 *
 * 縮排是一般空白（4px）湊到差不多，再用 {@code À}（Wynncraft 字型裡一像素寬的空白）
 * 補齊。Wynntils 快取 {@code dataStaticAbilitiesV2} 的 {@code margin-left} 就是它對這種
 * 寫法的記法（{@code StyledTextPart#toJson}：{@code thin} 是一個 À、{@code large} 是四個）。
 * 模板會剝掉行首空白（模板是「ÀÀ  +{~} …」，語料的鍵就是這樣收的），於是重建出來的
 * 那一行只剩兩個 {@code À} 與兩個空白，起點從數值底下縮回行首附近。傷害的小項
 * 「   (✣ Damage: 25%)」「  ÀÀÀÀ(…)」也是同一件事。
 *
 * <h2>做法</h2>
 * 不動模板（語料的鍵、Python 那一份都要跟著變），改成譯完之後<b>量</b>：
 *
 * <ul>
 *   <li>原文這一行縮排到第幾個像素（空白、À、偏移一起算）。</li>
 *   <li>往上找到這組續行的第一行（沒有縮排的那一行）。原文的續行剛好對齊在它
 *       「標籤: 」後面的數值上，就改成對齊<b>譯文</b>那一行的數值——「效果:」比
 *       「Effects:」短，數值往左挪了，續行要跟著挪。</li>
 *   <li>對不上任何東西就照原文的縮排。</li>
 * </ul>
 *
 * 差多少就在行首補一個偏移，其餘的字一個都不碰。
 */
public final class IndentAlign {

    private IndentAlign() {}

    /** 原文續行的起點跟第一行數值的起點差幾像素以內，算是「對齊在那裡」。 */
    private static final int TOLERANCE = 2;

    /**
     * @param original 原文每一行
     * @param out      譯文；會就地換掉需要補縮排的那幾行
     * @param rowOf    原文第 k 行在 {@code out} 的第幾行；不是一行一條翻的填 {@code -1}
     * @param hit      原文第 k 行有沒有翻到
     * @param centered 置中的行不動
     * @param spaces   只用一般空白縮排的行也算（技能樹的「   (✣ Damage: 25%)」）。
     *                 模板會把行首空白剝掉，譯文因此從行首畫起；只在認得出面板的
     *                 時候開，別的 tooltip 行首的空白各有用途，不去猜
     */
    public static void apply(List<StyledText> original, List<Component> out, int[] rowOf,
                             boolean[] hit, boolean[] centered, boolean spaces,
                             ToIntFunction<Component> width) {
        int n = original.size();
        for (int k = 0; k < n; k++) {
            int row = rowOf[k];
            if (row < 0 || row >= out.size() || !hit[k] || (centered != null && centered[k])) {
                continue;
            }
            List<Bit> orig = bits(original.get(k).getComponent());
            int origEnd = indentEnd(orig, spaces);
            if (origEnd <= 0) {
                continue;                      // 不是縮排的續行
            }
            List<Bit> made = bits(out.get(row));
            // 譯文那一行的縮排可能整個不見了（語料把 À 拿掉、模板剝掉行首空白），
            // 那就是從 0 開始畫，照樣補得回來
            int madeEnd = leadEnd(made);
            if (madeEnd < 0) {
                continue;                      // 整行都是空白
            }
            int origX = width(orig, origEnd, width);
            int madeX = width(made, madeEnd, width);
            int want = origX;
            int head = headOf(original, k, spaces);
            if (head >= 0) {
                List<Bit> headOrig = bits(original.get(head).getComponent());
                int valueAt = valueStart(headOrig);
                if (valueAt >= 0 && Math.abs(width(headOrig, valueAt, width) - origX) <= TOLERANCE
                        && rowOf[head] >= 0 && rowOf[head] < out.size() && hit[head]) {
                    List<Bit> headMade = bits(out.get(rowOf[head]));
                    int madeValue = valueStart(headMade);
                    if (madeValue >= 0) {
                        want = width(headMade, madeValue, width);
                    }
                }
            }
            if (want != madeX) {
                out.set(row, shifted(out.get(row), want - madeX, made));
            }
        }
    }

    /** 一個字元連同它的樣式。 */
    private record Bit(String text, Style style) {}

    private static List<Bit> bits(Component line) {
        List<Bit> out = new ArrayList<>();
        line.visit((style, text) -> {
            for (int i = 0; i < text.length(); ) {
                int cp = text.codePointAt(i);
                int len = Character.charCount(cp);
                out.add(new Bit(text.substring(i, i + len), style));
                i += len;
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    /**
     * 行首縮排（{@code À}、寬度偏移、空白）在第幾個字元結束。
     *
     * @param spaces 只有空白、沒有 {@code À} 的縮排也算
     * @return 不是縮排開頭、或整行都是縮排時回傳 {@code -1}
     */
    private static int indentEnd(List<Bit> bits, boolean spaces) {
        int end = leadEnd(bits);
        if (end <= 0) {
            return -1;
        }
        for (int i = 0; i < end; i++) {
            if (bits.get(i).text().codePointAt(0) == 'À') {
                return end;
            }
        }
        return spaces ? end : -1;
    }

    /** 第一個不是 {@code À}、寬度偏移、空白的字元在哪裡；整行都是時回傳 {@code -1}。 */
    private static int leadEnd(List<Bit> bits) {
        for (int i = 0; i < bits.size(); i++) {
            int cp = bits.get(i).text().codePointAt(0);
            if (!(cp == 'À' || cp == ' ' || SpaceOffset.isOffset(cp))) {
                return i;
            }
        }
        return -1;
    }

    /** 這一組續行的第一行：往上第一個沒有縮排的非空行。 */
    private static int headOf(List<StyledText> original, int k, boolean spaces) {
        for (int h = k - 1; h >= 0; h--) {
            List<Bit> bits = bits(original.get(h).getComponent());
            if (leadEnd(bits) < 0) {
                return -1;                     // 隔著空行就不是同一組
            }
            if (indentEnd(bits, spaces) < 0) {
                return h;
            }
        }
        return -1;
    }

    /**
     * 「標籤: 」後面那一欄從第幾個字元開始：第一個冒號、跳過後面的空白。
     *
     * @return 沒有冒號、或冒號後面沒有東西時回傳 {@code -1}
     */
    private static int valueStart(List<Bit> bits) {
        for (int i = 0; i < bits.size(); i++) {
            String t = bits.get(i).text();
            if (!t.equals(":") && !t.equals("：")) {
                continue;
            }
            int j = i + 1;
            while (j < bits.size() && (bits.get(j).text().equals(" ")
                    || SpaceOffset.isOffset(bits.get(j).text().codePointAt(0)))) {
                j++;
            }
            return j < bits.size() ? j : -1;
        }
        return -1;
    }

    /** 前 {@code end} 個字元畫出來有多寬。 */
    private static int width(List<Bit> bits, int end, ToIntFunction<Component> width) {
        MutableComponent prefix = Component.empty();
        StringBuilder run = new StringBuilder();
        Style style = null;
        for (int i = 0; i < end; i++) {
            Bit b = bits.get(i);
            if (style != null && !style.equals(b.style())) {
                prefix.append(Component.literal(run.toString()).withStyle(style));
                run.setLength(0);
            }
            style = b.style();
            run.append(b.text());
        }
        if (style != null) {
            prefix.append(Component.literal(run.toString()).withStyle(style));
        }
        return width.applyAsInt(prefix);
    }

    /** 行首補 {@code px} 像素的偏移，其餘原樣。 */
    private static Component shifted(Component line, int px, List<Bit> bits) {
        Style first = bits.isEmpty() ? Style.EMPTY : bits.get(0).style();
        MutableComponent out = Component.empty();
        out.append(Component.literal(SpaceOffset.encode(px)).withStyle(SpaceOffset.styleFor(first)));
        line.visit((style, text) -> {
            if (!text.isEmpty()) {
                out.append(Component.literal(text).withStyle(style));
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return out;
    }
}
