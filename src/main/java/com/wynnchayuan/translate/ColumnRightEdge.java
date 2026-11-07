package com.wynnchayuan.translate;

import com.wynnchayuan.translate.SpaceRuns.Run;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

// 伺服器的字寬表跟客戶端畫的不一樣，靠右欄的右緣會差幾 px；同一組列補到最寬那列
public final class ColumnRightEdge {

    private ColumnRightEdge() {}

    private static final int TOLERANCE = 12;

    public static void apply(List<StyledText> original, List<Component> out, int[] rowOf,
                             boolean[] hit, boolean[] centered, ToIntFunction<Component> width) {
        int n = original.size();
        int k = 0;
        while (k < n) {
            if (!rewindRow(original, out, rowOf, hit, centered, k)) {
                k++;
                continue;
            }
            int edge = width.applyAsInt(original.get(k).getComponent());
            int end = k + 1;
            while (end < n && rewindRow(original, out, rowOf, hit, centered, end)
                    && Math.abs(width.applyAsInt(original.get(end).getComponent()) - edge) <= TOLERANCE) {
                end++;
            }
            if (end - k >= 2) {
                pad(out, rowOf, k, end, width);
            }
            k = end;
        }
    }

    private static boolean rewindRow(List<StyledText> original, List<Component> out, int[] rowOf,
                                     boolean[] hit, boolean[] centered, int k) {
        int row = rowOf[k];
        if (row < 0 || row >= out.size() || !hit[k] || (centered != null && centered[k])) {
            return false;
        }
        List<Run> runs = RowRealigner.mergeSpaces(
                SpaceRuns.splitGaps(SpaceRuns.runs(original.get(k).getComponent())));
        for (int i = 1; i < runs.size(); i++) {
            Run gap = runs.get(i);
            Run before = runs.get(i - 1);
            if (!gap.space() || before.space()) {
                continue;
            }
            int label = LineTranslator.widthOf(DisplayStyles.literal(before.text(), before.style()));
            return RowRealigner.rewound(label, RowRealigner.leadingRewind(gap), gap.px());
        }
        return false;
    }

    private static void pad(List<Component> out, int[] rowOf, int from, int to,
                            ToIntFunction<Component> width) {
        int widest = Integer.MIN_VALUE;
        for (int k = from; k < to; k++) {
            widest = Math.max(widest, width.applyAsInt(out.get(rowOf[k])));
        }
        for (int k = from; k < to; k++) {
            Component made = out.get(rowOf[k]);
            int delta = widest - width.applyAsInt(made);
            if (delta > 0) {
                Component padded = padded(made, delta);
                if (padded != null) {
                    out.set(rowOf[k], padded);
                }
            }
        }
    }

    private static Component padded(Component line, int px) {
        MutableComponent out = Component.empty();
        boolean[] text = {false};
        boolean[] done = {false};
        line.visit((style, s) -> {
            if (s.isEmpty()) {
                return Optional.empty();
            }
            if (!done[0] && text[0] && SpaceOffset.isOffsetRun(s)) {
                out.append(Component.literal(SpaceOffset.encode(px)).withStyle(SpaceOffset.styleFor(style)));
                done[0] = true;
            }
            if (!s.isBlank() && !SpaceOffset.isOffsetRun(s)) {
                text[0] = true;
            }
            out.append(Component.literal(s).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return done[0] ? out : null;
    }
}
