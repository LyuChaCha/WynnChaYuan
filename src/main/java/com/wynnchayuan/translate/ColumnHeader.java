package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;

public final class ColumnHeader {

    private ColumnHeader() {}

    private static final int TOLERANCE = 12;

    public static void apply(List<StyledText> original, List<Component> out, int[] rowOf,
                             boolean[] hit, boolean[] centered, ToIntFunction<Component> width) {
        int n = original.size();
        for (int k = 0; k + 1 < n; k++) {
            int row = rowOf[k];
            int below = rowOf[k + 1];
            if (row < 0 || row >= out.size() || below < 0 || below >= out.size() || !hit[k]
                    || (centered != null && centered[k + 1])) {
                continue;
            }
            Component head = original.get(k).getComponent();
            int lead = headerLead(head);
            if (lead <= 0) {
                continue;
            }
            int headEnd = inkEnd(head, width);
            int valueEnd = inkEnd(original.get(k + 1).getComponent(), width);
            if (headEnd < 0 || valueEnd <= lead || Math.abs(headEnd - valueEnd) > TOLERANCE) {
                continue;
            }
            Component made = out.get(row);
            int madeEnd = inkEnd(made, width);
            int madeValueEnd = inkEnd(out.get(below), width);
            int madeLead = headerLead(made);
            if (madeEnd < 0 || madeValueEnd < 0 || madeLead < 0) {
                continue;
            }
            int shift = Math.max(madeValueEnd - madeEnd, -madeLead);
            if (shift != 0) {
                out.set(row, shifted(made, shift));
            }
        }
    }

    private static int headerLead(Component line) {
        int[] lead = {0};
        boolean[] text = {false};
        boolean[] gapAfterText = {false};
        line.visit((style, s) -> {
            int i = 0;
            while (i < s.length()) {
                int cp = s.codePointAt(i);
                i += Character.charCount(cp);
                if (SpaceOffset.isOffset(cp)) {
                    if (text[0]) {
                        gapAfterText[0] = true;
                    } else {
                        lead[0] += SpaceOffset.decode(new String(Character.toChars(cp)));
                    }
                } else if (!Character.isWhitespace(cp)) {
                    text[0] = true;
                }
            }
            return Optional.empty();
        }, Style.EMPTY);
        return text[0] && !gapAfterText[0] ? lead[0] : -1;
    }

    private static int inkEnd(Component line, ToIntFunction<Component> width) {
        int[] x = {0};
        int[] end = {-1};
        line.visit((style, s) -> {
            if (s.isEmpty()) {
                return Optional.empty();
            }
            x[0] += width.applyAsInt(Component.literal(s).withStyle(style));
            if (inked(s)) {
                end[0] = x[0];
            }
            return Optional.empty();
        }, Style.EMPTY);
        return end[0];
    }

    private static boolean inked(String s) {
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (!Character.isWhitespace(cp) && !SpaceOffset.isOffset(cp)
                    && Character.getType(cp) != Character.PRIVATE_USE) {
                return true;
            }
        }
        return false;
    }

    private static Component shifted(Component line, int px) {
        MutableComponent out = Component.empty();
        out.append(Component.literal(SpaceOffset.encode(px)).withStyle(SpaceOffset.styleFor(Style.EMPTY)));
        line.visit((style, text) -> {
            if (!text.isEmpty()) {
                out.append(Component.literal(text).withStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }
}
