package com.wynnchayuan.render;

import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.SpaceOffset;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.ToIntFunction;

// Wynncraft bug：/class 的 bottom_middle 淨寬 +39px
public final class SelectorRealign {

    private SelectorRealign() {
    }

    private static final int MAX_SHIFT = 256;

    public record Result(Component out, boolean changed, String report) {
        static Result same(Component out, String report) {
            return new Result(out, false, report);
        }
    }

    private record Frag(Style style, String text) {
    }

    private record Run(int from, int to, int at) {
        int size() {
            return to - from;
        }
    }

    public static Result apply(Component message) {
        return apply(message, SelectorRealign::advance);
    }

    static Result apply(Component message, ToIntFunction<Component> width) {
        if (message == null) {
            return Result.same(null, "");
        }
        List<Frag> frags = new ArrayList<>();
        boolean[] any = {false};
        message.visit((style, text) -> {
            frags.add(new Frag(style, text));
            any[0] |= PairedFont.absolutelyPositioned(style);
            return Optional.empty();
        }, Style.EMPTY);
        if (!any[0]) {
            return Result.same(message, "");
        }

        int n = frags.size();
        int[] w = new int[n];
        for (int i = 0; i < n; i++) {
            Frag f = frags.get(i);
            w[i] = width.applyAsInt(Component.literal(f.text()).setStyle(f.style()));
            if (w[i] == Integer.MIN_VALUE) {
                return Result.same(message, "定位格矯正：第 " + i + " 段量不到寬度，整行不碰");
            }
        }

        List<Run> runs = runs(frags, w);
        Map<Integer, Integer> votes = new LinkedHashMap<>();
        for (Run r : runs) {
            votes.merge(r.at(), r.size(), Integer::sum);
        }
        if (votes.size() <= 1) {
            return Result.same(message, "定位格矯正：所有格子的起點一致（相對行首 +"
                    + runs.get(0).at() + "），不必矯正");
        }
        int anchor = 0;
        for (int px : w) {
            anchor += px;
        }
        if (!votes.containsKey(anchor)) {
            return Result.same(message, "定位格矯正：行尾 +" + anchor
                    + " 不是任何一格的起點，不碰（候選 " + votes.keySet() + "）");
        }

        StringBuilder report = new StringBuilder("定位格矯正：錨點 = 行尾 +")
                .append(anchor).append("，候選");
        for (Map.Entry<Integer, Integer> e : votes.entrySet()) {
            report.append(" [+").append(e.getKey()).append(": ")
                  .append(e.getValue()).append(" 段]");
        }

        MutableComponent out = Component.empty();
        boolean changed = false;
        int ri = 0;
        int pending = 0;
        for (int i = 0; i < n; i++) {
            Frag f = frags.get(i);
            if (ri < runs.size() && i == runs.get(ri).from()) {
                Run r = runs.get(ri);
                int d = anchor - r.at();
                if (d != 0 && Math.abs(d) <= MAX_SHIFT) {
                    out.append(offset(d, f.style()));
                    pending = d;
                    changed = true;
                    report.append(System.lineSeparator())
                          .append("  [").append(r.from()).append('-').append(r.to() - 1)
                          .append("] ").append(PairedFont.nameOf(f.style()))
                          .append(" 起點 +").append(r.at())
                          .append(" → 前補 ").append(signed(d))
                          .append("、後補 ").append(signed(-d));
                } else if (d != 0) {
                    report.append(System.lineSeparator())
                          .append("  [").append(r.from()).append('-').append(r.to() - 1)
                          .append("] 差 ").append(d).append(" px，超過上限不碰");
                }
            }
            out.append(Component.literal(f.text()).setStyle(f.style()));
            if (ri < runs.size() && i == runs.get(ri).to() - 1) {
                if (pending != 0) {
                    out.append(offset(-pending, f.style()));
                    pending = 0;
                }
                ri++;
            }
        }
        return changed ? new Result(out, true, report.toString())
                       : Result.same(message, report.toString());
    }

    private static List<Run> runs(List<Frag> frags, int[] w) {
        List<Run> runs = new ArrayList<>();
        int cursor = 0;
        int open = -1;
        int openAt = 0;
        for (int i = 0; i < frags.size(); i++) {
            Style style = frags.get(i).style();
            boolean fixed = PairedFont.absolutelyPositioned(style);
            boolean ours = PairedFont.isOurs(style) || SpaceOffset.isSpaceFont(style);
            if (open < 0) {
                if (fixed) {
                    open = i;
                    openAt = cursor;
                }
            } else if (!fixed && !ours) {
                runs.add(new Run(open, i, openAt));
                open = -1;
            }
            cursor += w[i];
        }
        if (open >= 0) {
            runs.add(new Run(open, frags.size(), openAt));
        }
        return runs;
    }

    private static Component offset(int px, Style like) {
        return Component.literal(SpaceOffset.encode(px)).setStyle(SpaceOffset.styleFor(like));
    }

    private static String signed(int px) {
        return (px >= 0 ? "+" : "") + px;
    }

    private static int advance(Component piece) {
        try {
            Minecraft mc = Minecraft.getInstance();
            return mc == null || mc.font == null ? Integer.MIN_VALUE : mc.font.width(piece);
        } catch (Throwable t) {
            ErrorDebug.note("SelectorRealign#advance", null, t);
            return Integer.MIN_VALUE;
        }
    }
}
