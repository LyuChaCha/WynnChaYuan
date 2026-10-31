package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

// 續行縮排是空白加 À（Wynncraft 字型裡 1px 寬）湊的
public final class IndentAlign {

    private IndentAlign() {}

    private static final int TOLERANCE = 2;

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
                continue;
            }
            List<Bit> made = bits(out.get(row));
            int madeEnd = leadEnd(made);
            if (madeEnd < 0) {
                continue;
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
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

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

    private static int leadEnd(List<Bit> bits) {
        for (int i = 0; i < bits.size(); i++) {
            int cp = bits.get(i).text().codePointAt(0);
            if (!(cp == 'À' || cp == ' ' || SpaceOffset.isOffset(cp))) {
                return i;
            }
        }
        return -1;
    }

    private static int headOf(List<StyledText> original, int k, boolean spaces) {
        for (int h = k - 1; h >= 0; h--) {
            List<Bit> bits = bits(original.get(h).getComponent());
            if (leadEnd(bits) < 0) {
                return -1;
            }
            if (indentEnd(bits, spaces) < 0) {
                return h;
            }
        }
        return -1;
    }

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

    private static Component shifted(Component line, int px, List<Bit> bits) {
        Style first = bits.isEmpty() ? Style.EMPTY : bits.get(0).style();
        MutableComponent out = Component.empty();
        out.append(Component.literal(SpaceOffset.encode(px)).withStyle(SpaceOffset.styleFor(first)));
        line.visit((style, text) -> {
            if (!text.isEmpty()) {
                out.append(Component.literal(text).withStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }
}
