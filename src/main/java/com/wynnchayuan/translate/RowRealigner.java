package com.wynnchayuan.translate;

import com.wynnchayuan.translate.SpaceRuns.Run;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class RowRealigner {

    private RowRealigner() {}

    static Component realign(StyledText original, Component rebuilt,
                             boolean centered) {
        return realign(original, rebuilt, centered, true);
    }

    static Component realign(StyledText original, Component rebuilt,
                             boolean centered, boolean leftAligned) {
        List<Run> orig = SpaceRuns.splitGaps(SpaceRuns.runs(original.getComponent()));
        List<Run> made = SpaceRuns.splitGaps(SpaceRuns.runs(rebuilt));
        StringBuilder log = null;
        if (SpaceRuns.rows(orig) > 1 || SpaceRuns.rows(made) > 1) {
            log = new StringBuilder();
            log.append("  原文行數：").append(SpaceRuns.rows(orig))
               .append("  譯文行數：").append(SpaceRuns.rows(made))
               .append("  置中：").append(centered)
               .append(System.lineSeparator());
            Component perRow = realignRows(orig, made, centered, log);
            if (perRow != null) {
                FlowedDebug.rows(original.getString(), log.toString());
                return perRow;
            }
            log.append("  逐行重算：放棄；多行的不套整段那一路，原樣返回")
               .append(System.lineSeparator());
            FlowedDebug.rows(original.getString(), log.toString());
            return rebuilt;
        }
        if (leftAligned) {
            Component left = realignLeft(orig, made);
            if (left != null) {
                FlowedDebug.rows(original.getString(), "  靠左：每一欄的起點對回原文");
                return left;
            }
        }
        int spaces = SpaceRuns.countSpaces(made);
        if (spaces == 0 || spaces != SpaceRuns.countSpaces(orig)) {
            if (log != null) {
                log.append("  整段：空白數 原文=").append(SpaceRuns.countSpaces(orig))
                   .append(" 譯文=").append(spaces)
                   .append("，對不上就原樣返回")
                   .append(System.lineSeparator());
                FlowedDebug.rows(original.getString(), log.toString());
            }
            return rebuilt;
        }
        List<Integer> origSeg = SpaceRuns.segmentWidths(orig);
        List<Integer> madeSeg = SpaceRuns.segmentWidths(made);

        int[] adjust = new int[spaces];
        boolean leading = centered && origSeg.get(0) == 0 && madeSeg.get(0) == 0;

        if (leading) {
            int delta = SpaceRuns.sum(origSeg) - SpaceRuns.sum(madeSeg);
            adjust[0] = delta / 2;
        }
        int drift = 0;
        int lastAdjusted = -1;
        int[] gapPx = SpaceRuns.gapPixels(made);
        for (int k = leading ? 1 : 0; k < spaces; k++) {
            drift += madeSeg.get(k) - origSeg.get(k);
            if (madeSeg.get(k) > 0 && madeSeg.get(k + 1) > 0
                    && !SpaceRuns.overlayGap(made, k, gapPx[k])) {
                adjust[k] = -drift;
                drift = 0;
                lastAdjusted = k;
            }
        }

        int tail = madeSeg.get(spaces) - origSeg.get(spaces);
        if (!leftAligned && lastAdjusted >= 0 && tail != 0
                && SpaceRuns.labelledRun(made, lastAdjusted)) {
            adjust[lastAdjusted] -= tail;
        }

        if (log == null) {
            log = new StringBuilder();
        }
        log.append("  整段：").append(describeRow(origSeg, madeSeg, adjust, leading))
           .append("  右緣補正=").append(-tail)
           .append(leftAligned ? "（靠左，不套用）" : "")
           .append(System.lineSeparator());
        FlowedDebug.rows(original.getString(), log.toString());
        return SpaceRuns.apply(made, adjust);
    }

    private static Component realignLeft(List<Run> orig, List<Run> made) {
        List<Run> o = mergeSpaces(orig);
        List<Run> m = mergeSpaces(made);
        int gaps = 0;
        for (Run r : o) {
            if (r.space()) {
                gaps++;
            }
        }
        int madeGaps = 0;
        for (Run r : m) {
            if (r.space()) {
                madeGaps++;
            }
        }
        if (gaps == 0 || gaps != madeGaps) {
            return null;
        }
        int[] target = new int[gaps];
        int x = 0;
        int g = 0;
        for (Run r : o) {
            x += r.space() ? r.px() : LineTranslator.widthOf(DisplayStyles.literal(r.text(), r.style()));
            if (r.space()) {
                target[g++] = x;
            }
        }
        MutableComponent out = Component.empty();
        x = 0;
        g = 0;
        boolean textBefore = false;
        for (Run r : m) {
            if (!r.space()) {
                out.append(DisplayStyles.literal(r.text(), r.style()));
                x += LineTranslator.widthOf(DisplayStyles.literal(r.text(), r.style()));
                textBefore = true;
                continue;
            }
            int px = r.px();
            if (textBefore && px >= 0) {
                px = Math.max(Math.min(px, 1), target[g] - x);
            }
            g++;
            String encoded = SpaceOffset.encode(px);
            if (!encoded.isEmpty()) {
                out.append(DisplayStyles.literal(encoded, r.style()));
            }
            x += px;
        }
        return out;
    }

    static List<Run> mergeSpaces(List<Run> runs) {
        List<Run> out = new ArrayList<>(runs.size());
        for (Run r : runs) {
            if (r.space() && !out.isEmpty() && out.get(out.size() - 1).space()) {
                Run prev = out.remove(out.size() - 1);
                out.add(new Run(true, prev.px() + r.px(), prev.style(),
                        prev.text() + r.text()));
            } else {
                out.add(r);
            }
        }
        return out;
    }

    private static String describeRow(List<Integer> origSeg, List<Integer> madeSeg,
                                      int[] adjust, boolean leading) {
        return "前導=" + leading + " 原文段寬=" + origSeg + " 譯文段寬=" + madeSeg
                + " 補正=" + Arrays.toString(adjust);
    }

    private static Component realignRows(List<Run> orig, List<Run> made,
                                         boolean centered, StringBuilder log) {
        List<List<Run>> origRows = SpaceRuns.splitRows(orig);
        List<List<Run>> madeRows = SpaceRuns.splitRows(made);
        int[] keepOrig = solidRows(origRows);
        int[] keepMade = solidRows(madeRows);
        if (keepOrig[1] - keepOrig[0] != keepMade[1] - keepMade[0]) {
            if (log != null) {
                log.append("  去掉首尾空行之後：原文 ")
                   .append(keepOrig[1] - keepOrig[0]).append(" 行、譯文 ")
                   .append(keepMade[1] - keepMade[0]).append(" 行，還是對不上")
                   .append(System.lineSeparator());
            }
            return null;
        }
        MutableComponent out = Component.empty();
        for (int i = 0; i < madeRows.size(); i++) {
            if (i > 0) {
                out.append(Component.literal(LineTranslator.NL));
            }
            if (i < keepMade[0] || i >= keepMade[1]) {
                out.append(SpaceRuns.apply(madeRows.get(i), new int[0]));
                continue;
            }
            if (log != null) {
                log.append("  [").append(i).append("] ");
            }
            out.append(realignRow(origRows.get(keepOrig[0] + i - keepMade[0]),
                                  madeRows.get(i), centered, log));
        }
        return out;
    }

    static int[] solidRows(List<List<Run>> rows) {
        int from = 0;
        int to = rows.size();
        while (from < to && blankRow(rows.get(from))) {
            from++;
        }
        while (to > from && blankRow(rows.get(to - 1))) {
            to--;
        }
        return new int[] {from, to};
    }

    private static boolean blankRow(List<Run> row) {
        return SpaceRuns.rowText(row).isBlank();
    }

    private static Component realignRow(List<Run> orig, List<Run> made,
                                        boolean centered, StringBuilder log) {
        int spaces = SpaceRuns.countSpaces(made);
        if (spaces == 0 || spaces != SpaceRuns.countSpaces(orig)) {
            if (log != null) {
                log.append("不動：空白數 原文=").append(SpaceRuns.countSpaces(orig))
                   .append(" 譯文=").append(spaces)
                   .append("  譯文=「").append(SpaceRuns.rowText(made)).append("」")
                   .append(System.lineSeparator());
            }
            return SpaceRuns.apply(made, new int[0]);
        }
        List<Integer> origSeg = SpaceRuns.segmentWidths(orig);
        List<Integer> madeSeg = SpaceRuns.segmentWidths(made);
        int[] adjust = new int[spaces];
        boolean leading = centered && origSeg.get(0) == 0 && madeSeg.get(0) == 0;
        if (leading) {
            adjust[0] = (SpaceRuns.sum(origSeg) - SpaceRuns.sum(madeSeg)) / 2;
        }
        int drift = 0;
        int[] gapPx = SpaceRuns.gapPixels(made);
        for (int k = leading ? 1 : 0; k < spaces; k++) {
            drift += madeSeg.get(k) - origSeg.get(k);
            if (madeSeg.get(k) > 0 && madeSeg.get(k + 1) > 0
                    && !SpaceRuns.overlayGap(made, k, gapPx[k])) {
                adjust[k] = -drift;
                drift = 0;
            }
        }
        if (log != null) {
            log.append(describeRow(origSeg, madeSeg, adjust, leading))
               .append("  譯文=「").append(SpaceRuns.rowText(made)).append("」")
               .append(System.lineSeparator());
        }
        return SpaceRuns.apply(made, adjust);
    }
}
