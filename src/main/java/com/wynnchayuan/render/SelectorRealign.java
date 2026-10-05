package com.wynnchayuan.render;

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

/**
 * 把 {@code /class} 畫面裡站錯錨點的定位格拉回去。
 *
 * <h2>Wynncraft 自己的 bug</h2>
 * 那個畫面是<b>一行</b> action bar，十幾個 HUD 元件各自長成
 * {@code [跳 +a] 內容 [退回 -b]}，淨寬 0，所以每一格都從同一個起點（錨點）出發。
 * 唯一的例外是下方的提示列 {@code bottom_middle}：它的淨寬<b>不是 0</b>（實機
 * +39 px），而 Wynncraft 把它排在字串的<b>第二個</b>。於是排在它後面的每一格
 * 都從「行首 + 39」出發，排在它前面的那一格——第一個職業原型的圖示——從行首
 * 出發，整整少了 39 px，畫面上就是第一個圖示往左跑出去、另外兩個正常
 * （使用者 2026-10-05 的截圖；{@code actionbar-columns-2.txt} 原文側：
 * 第一格起點 460、其餘全部 499）。這跟有沒有翻譯無關，一個字都不動也是歪的。
 *
 * <h2>做法</h2>
 * 把整行攤成片段，量每一段的推進量，找出每一個定位格（連續的
 * {@link PairedFont#absolutelyPositioned} 片段）是從哪個游標位置出發的。
 * 最多片段共用的那個位置就是錨點；站錯的格子前面補一個 {@code +d}、後面補一個
 * {@code -d} 的 {@code minecraft:space} 位移字元，把它搬到錨點上再搬回來。
 * 這一格的淨寬不變，整行的總寬也不變，所以 action bar 的置中起點完全不受影響。
 *
 * <h2>為什麼它會自己失效</h2>
 * 要是 Wynncraft 哪天把那一格移到提示列後面、或把提示列的淨寬改成 0，
 * 所有格子的起點就會一致，這裡量出來的 {@code d} 全是 0，一個字元都不會插。
 * 不需要版本判斷，也不會因為裝了這個模組反而歪掉。
 *
 * <p>任何一段量不到寬度就整行原樣回傳——寧可歪，不能讓 action bar 掛掉。
 */
public final class SelectorRealign {

    private SelectorRealign() {
    }

    /** 超過這個就不是定位格的位移，多半是量錯了；不碰。 */
    private static final int MAX_SHIFT = 256;

    /**
     * @param out     矯正後的那一行；沒動時就是傳進來的那一個
     * @param changed 有沒有真的插了位移字元
     * @param report  給診斷檔看的說明；這一行沒有定位格時是空字串
     */
    public record Result(Component out, boolean changed, String report) {
        static Result same(Component out, String report) {
            return new Result(out, false, report);
        }
    }

    private record Frag(Style style, String text) {
    }

    /** 一個定位格：片段 [from, to)，從相對行首 {@code at} px 的位置出發。 */
    private record Run(int from, int to, int at) {
        int size() {
            return to - from;
        }
    }

    public static Result apply(Component message) {
        return apply(message, SelectorRealign::advance);
    }

    /**
     * @param width 量一段的推進量；量不到回 {@link Integer#MIN_VALUE}。
     *              負的是正常值（退回的片段就是負的），不能拿它當失敗。
     */
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
        // 錨點 = 整行走完游標停的地方。每一格的淨寬都是 0，只有提示列不是，所以
        // 排在提示列<b>後面</b>的格子全從「行首 + 提示列淨寬」出發，行尾也停在那裡；
        // 排在前面的從行首出發，就是站錯的那些（原文截圖：第一個圖示往左跑）。
        //
        // 先前拿「最多片段共用的起點」當錨點。Wynncraft 每次送來的元件順序並不
        // 固定，有一次排在提示列前面的格子比較多，票數反過來，整批對的被搬去
        // 遷就錯的，三個圖示一起跑出畫面（2026-10-05 實機）。
        int anchor = 0;
        for (int px : w) {
            anchor += px;
        }
        if (!votes.containsKey(anchor)) {
            // 行尾不落在任何一格的起點上：這不是我們認得的版面，不碰。
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

    /**
     * 連續的定位片段算一格。我們自己補進去的片段（配對字型的譯文、補白用的
     * 位移字元）夾在格子中間，要算進同一格，不然一個已翻的格子會被切成兩半。
     */
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
            return Integer.MIN_VALUE;
        }
    }
}
