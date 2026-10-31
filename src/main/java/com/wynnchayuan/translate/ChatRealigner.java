package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.translate.SpaceRuns.Run;
import com.wynntils.core.text.StyledText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.ToIntFunction;

final class ChatRealigner {

    private ChatRealigner() {}

    static boolean[] chatCentred(List<StyledText> rows) {
        List<Component> lines = new ArrayList<>(rows.size());
        for (StyledText row : rows) {
            lines.add(row.getComponent());
        }
        boolean[] centred = BlockLayout.centered(lines);
        List<List<Run>> runRows = new ArrayList<>(rows.size());
        for (StyledText row : rows) {
            List<List<Run>> split = SpaceRuns.splitRows(SpaceRuns.runs(row.getComponent()));
            if (split.size() != 1) {
                return centred;
            }
            runRows.add(split.get(0));
        }
        sharedCentre(runRows, centred);
        spacePadded(runRows, centred);
        sameLeadList(runRows, centred);
        onChatCentre(runRows, centred);
        return centred;
    }

    static Component realignChat(StyledText original, Component rebuilt,
                                 Boolean centred) {
        return realignChat(original, rebuilt, centred, false);
    }

    static Component realignChat(StyledText original, Component rebuilt,
                                 Boolean centred, boolean inPanel) {
        List<List<Run>> origRows = SpaceRuns.splitRows(SpaceRuns.runs(original.getComponent()));
        List<List<Run>> madeRows = SpaceRuns.splitRows(SpaceRuns.runs(rebuilt));
        int[] keepOrig = RowRealigner.solidRows(origRows);
        int[] keepMade = RowRealigner.solidRows(madeRows);
        if (keepOrig[1] - keepOrig[0] != keepMade[1] - keepMade[0]) {
            FlowedDebug.chatRows(original.getString(),
                    "  去掉首尾空行之後：原文 " + (keepOrig[1] - keepOrig[0])
                    + " 行、譯文 " + (keepMade[1] - keepMade[0]) + " 行",
                    "行數對不上，原樣返回不動排版");
            return rebuilt;
        }
        boolean[] centre = centred == null ? centredRows(origRows) : null;
        if (centre != null) {
            sharedCentre(origRows, centre);
            spacePadded(origRows, centre);
            sameLeadList(origRows, centre);
            onChatCentre(origRows, centre);
        }
        boolean panel = inPanel || columnPanel(origRows);
        boolean anyCentre = Boolean.TRUE.equals(centred);
        if (centre != null) {
            for (boolean c : centre) {
                anyCentre |= c;
            }
        }
        if (!panel && !anyCentre) {
            Component flowed = reflowCjk(origRows, madeRows);
            if (flowed != null) {
                FlowedDebug.chatRows(original.getString(), "  中日文重新斷行", null);
                return flowed;
            }
        }
        StringBuilder log = new StringBuilder();
        MutableComponent out = Component.empty();
        for (int i = 0; i < madeRows.size(); i++) {
            if (i > 0) {
                out.append(Component.literal(LineTranslator.NL));
            }
            List<Run> made = madeRows.get(i);
            if (i < keepMade[0] || i >= keepMade[1]) {
                out.append(SpaceRuns.apply(made, new int[SpaceRuns.countSpaces(made)]));
                continue;
            }
            int at = keepOrig[0] + i - keepMade[0];
            out.append(chatRow(origRows.get(at), made,
                               centred == null ? centre[at] : centred, panel, log));
        }
        FlowedDebug.chatRows(original.getString(), log.toString(), null);
        return out;
    }

    static Component reflowCjk(List<List<Run>> origRows, List<List<Run>> madeRows) {
        int n = madeRows.size();
        if (n < 2 || origRows.size() != n) {
            return null;
        }
        int[] origBody = new int[n];
        int max = 0;
        for (int i = 0; i < n; i++) {
            List<Run> row = origRows.get(i);
            int lead = prefixEnd(row);
            if (lead < 0) {
                return null;
            }
            origBody[i] = runsWidth(row.subList(lead, row.size()));
            max = Math.max(max, origBody[i]);
        }
        if (max <= 0) {
            return null;
        }
        for (int i = 0; i < n - 1; i++) {
            if (origBody[i] * 10 < max * 6) {
                return null;
            }
        }
        List<List<Run>> prefixes = new ArrayList<>(n);
        List<Run> body = new ArrayList<>();
        boolean cjk = false;
        for (int i = 0; i < n; i++) {
            List<Run> row = madeRows.get(i);
            int lead = prefixEnd(row);
            if (lead < 0) {
                return null;
            }
            prefixes.add(row.subList(0, lead));
            List<Run> content = row.subList(lead, row.size());
            for (Run r : content) {
                if (r.space()) {
                    return null;
                }
                cjk |= r.text().codePoints().anyMatch(ChatRealigner::isCjkBreakable);
            }
            appendJoined(body, content);
        }
        if (!cjk || body.isEmpty()) {
            return null;
        }
        List<List<Run>> lines = wrapRuns(body, max);
        MutableComponent out = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                out.append(Component.literal(LineTranslator.NL));
            }
            List<Run> prefix = prefixes.get(Math.min(i, 1));
            for (Run r : prefix) {
                out.append(DisplayStyles.literal(r.space() ? SpaceOffset.encode(r.px()) : r.text(), r.style()));
            }
            for (Run r : lines.get(i)) {
                out.append(DisplayStyles.literal(r.text(), r.style()));
            }
        }
        return out;
    }

    private static int prefixEnd(List<Run> row) {
        for (int i = 0; i < row.size(); i++) {
            Run r = row.get(i);
            if (r.space()) {
                continue;
            }
            if (r.text().codePoints().anyMatch(cp -> Character.isLetterOrDigit(cp)
                    && !GlyphSplitter.isGlyphCodePoint(cp))) {
                return i;
            }
        }
        return -1;
    }

    private static int runsWidth(List<Run> runs) {
        int w = 0;
        for (Run r : runs) {
            w += r.space() ? r.px() : LineTranslator.widthOf(DisplayStyles.literal(r.text(), r.style()));
        }
        return w;
    }

    private static void appendJoined(List<Run> body, List<Run> content) {
        List<Run> trimmed = new ArrayList<>(content);
        while (!trimmed.isEmpty()) {
            Run first = trimmed.get(0);
            String t = first.text().stripLeading();
            if (!t.isEmpty()) {
                trimmed.set(0, new Run(false, 0, first.style(), t));
                break;
            }
            trimmed.remove(0);
        }
        if (trimmed.isEmpty()) {
            return;
        }
        if (!body.isEmpty()) {
            Run last = body.get(body.size() - 1);
            String t = last.text().stripTrailing();
            body.set(body.size() - 1, new Run(false, 0, last.style(), t));
            int a = t.isEmpty() ? ' ' : t.codePointBefore(t.length());
            int b = trimmed.get(0).text().codePointAt(0);
            if (isWordChar(a) && isWordChar(b)) {
                body.add(new Run(false, 0, last.style(), " "));
            }
        }
        body.addAll(trimmed);
    }

    private static boolean isWordChar(int cp) {
        return (Character.isLetterOrDigit(cp) && !isCjkBreakable(cp))
                || "%+-/.,".indexOf(cp) >= 0 && cp != ',';
    }

    static boolean isCjkBreakable(int cp) {
        Character.UnicodeScript s = Character.UnicodeScript.of(cp);
        return s == Character.UnicodeScript.HAN || s == Character.UnicodeScript.HIRAGANA
                || s == Character.UnicodeScript.KATAKANA;
    }

    private static final String BREAK_AFTER = "，。！？；：、）」』…,.!?;:";

    private static final String NO_LINE_START = "，。！？；：、）」』…ー,.!?;:%）)";

    private static List<List<Run>> wrapRuns(List<Run> body, int max) {
        List<int[]> cps = new ArrayList<>();
        for (int k = 0; k < body.size(); k++) {
            int kk = k;
            body.get(k).text().codePoints().forEach(cp -> cps.add(new int[] {cp, kk}));
        }
        int[] w = new int[cps.size()];
        for (int i = 0; i < cps.size(); i++) {
            w[i] = LineTranslator.widthOf(DisplayStyles.literal(new String(Character.toChars(cps.get(i)[0])),
                    body.get(cps.get(i)[1]).style()));
        }
        List<int[]> ranges = new ArrayList<>();
        int start = 0;
        while (start < cps.size()) {
            int x = 0;
            int end = start;
            while (end < cps.size() && x + w[end] <= max) {
                x += w[end];
                end++;
            }
            if (end >= cps.size()) {
                ranges.add(new int[] {start, cps.size()});
                break;
            }
            int cut = -1;
            int acc = x;
            for (int i = end; i > start; i--) {
                if (canBreakBefore(cps, i) && BREAK_AFTER.indexOf(cps.get(i - 1)[0]) >= 0
                        && acc * 2 >= max) {
                    cut = i;
                    break;
                }
                acc -= w[i - 1];
            }
            if (cut < 0) {
                for (int i = end; i > start; i--) {
                    if (canBreakBefore(cps, i)) {
                        cut = i;
                        break;
                    }
                }
            }
            if (cut <= start) {
                cut = Math.max(end, start + 1);
            }
            ranges.add(new int[] {start, cut});
            start = cut;
            while (start < cps.size() && cps.get(start)[0] == ' ') {
                start++;
            }
        }
        List<List<Run>> out = new ArrayList<>();
        for (int[] r : ranges) {
            List<Run> line = new ArrayList<>();
            StringBuilder sb = new StringBuilder();
            int run = -1;
            for (int i = r[0]; i < r[1]; i++) {
                int ri = cps.get(i)[1];
                if (ri != run && sb.length() > 0) {
                    line.add(new Run(false, 0, body.get(run).style(), sb.toString()));
                    sb.setLength(0);
                }
                run = ri;
                sb.appendCodePoint(cps.get(i)[0]);
            }
            if (sb.length() > 0) {
                String t = sb.toString().stripTrailing();
                if (!t.isEmpty()) {
                    line.add(new Run(false, 0, body.get(run).style(), t));
                }
            }
            out.add(line);
        }
        return out;
    }

    private static boolean canBreakBefore(List<int[]> cps, int i) {
        int a = cps.get(i - 1)[0];
        int b = cps.get(i)[0];
        if (NO_LINE_START.indexOf(b) >= 0) {
            return false;
        }
        if (a == ' ' || b == ' ') {
            return true;
        }
        if (BREAK_AFTER.indexOf(a) >= 0 && !isWordChar(b)) {
            return true;
        }
        if (BREAK_AFTER.indexOf(a) >= 0 && (a > 0x2FFF)) {
            return true;
        }
        return isCjkBreakable(a) || isCjkBreakable(b)
                ? !(isWordChar(a) && isWordChar(b)) : false;
    }

    // Wynncraft 聊天置中線在 154.5px（乘二存）
    private static final int CHAT_CENTRE_X2 = 309;

    private static final int CHAT_CENTRE_SLACK_X2 = 14;

    private static void onChatCentre(List<List<Run>> rows, boolean[] centre) {
        int only = -1;
        for (int i = 0; i < rows.size() && i < centre.length; i++) {
            List<Run> row = rows.get(i);
            if (SpaceRuns.rowWidth(row) - SpaceRuns.leadWidth(row) <= 0) {
                continue;
            }
            if (only >= 0) {
                return;
            }
            only = i;
        }
        if (only < 0 || centre[only]) {
            return;
        }
        List<Run> row = rows.get(only);
        int lead = SpaceRuns.leadWidth(row);
        int body = SpaceRuns.rowWidth(row) - lead;
        if (lead < CHAT_CENTRE_MIN_LEAD
                || columns(SpaceRuns.chatSegmentWidths(row, SpaceRuns::runWidth)) >= 2) {
            return;
        }
        if (Math.abs(2 * lead + body - CHAT_CENTRE_X2) <= CHAT_CENTRE_SLACK_X2) {
            centre[only] = true;
        }
    }

    private static final int CHAT_CENTRE_MIN_LEAD = 16;

    private static void sharedCentre(List<List<Run>> rows, boolean[] centre) {
        List<Integer> idx = new ArrayList<>();
        List<Integer> mids = new ArrayList<>();
        int minLead = Integer.MAX_VALUE;
        int maxLead = 0;
        for (int i = 0; i < rows.size(); i++) {
            List<Run> row = rows.get(i);
            int lead = SpaceRuns.leadWidth(row);
            int body = SpaceRuns.rowWidth(row) - lead;
            if (lead <= 0 || body <= 0) {
                continue;
            }
            if (columns(SpaceRuns.chatSegmentWidths(row, SpaceRuns::runWidth)) >= 2) {
                return;
            }
            idx.add(i);
            mids.add(lead + body / 2);
            minLead = Math.min(minLead, lead);
            maxLead = Math.max(maxLead, lead);
        }
        if (idx.size() < 2 || maxLead - minLead <= 8) {
            return;
        }
        List<Integer> sorted = new ArrayList<>(mids);
        Collections.sort(sorted);
        int median = sorted.get(sorted.size() / 2);
        for (int m : mids) {
            if (Math.abs(m - median) > 16) {
                return;
            }
        }
        for (int i : idx) {
            centre[i] = true;
        }
    }

    static void spacePadded(List<List<Run>> rows, boolean[] centre) {
        for (int i = 0; i < rows.size(); i++) {
            List<Run> row = rows.get(i);
            if (plainSpaceLead(row) >= 5
                    && columns(SpaceRuns.chatSegmentWidths(row, SpaceRuns::runWidth)) < 2) {
                centre[i] = true;
            }
        }
    }

    static void sameLeadList(List<List<Run>> rows, boolean[] centre) {
        int n = rows.size();
        int[] lead = new int[n];
        int[] body = new int[n];
        for (int i = 0; i < n; i++) {
            lead[i] = SpaceRuns.leadWidth(rows.get(i));
            body[i] = SpaceRuns.rowWidth(rows.get(i)) - lead[i];
        }
        boolean[] list = new boolean[n];
        for (int i = 0; i < n; i++) {
            if (lead[i] <= 0 || body[i] <= 0) {
                continue;
            }
            for (int j = i + 1; j < n; j++) {
                if (body[j] > 0 && Math.abs(lead[i] - lead[j]) <= 1
                        && Math.abs(body[i] - body[j]) > 6) {
                    list[i] = true;
                    list[j] = true;
                }
            }
        }
        for (int i = 0; i < n; i++) {
            if (list[i]) {
                centre[i] = false;
            }
        }
    }

    private static int plainSpaceLead(List<Run> row) {
        int n = 0;
        for (Run run : row) {
            if (run.space()) {
                return n == 0 ? 0 : n;
            }
            String text = run.text();
            for (int k = 0; k < text.length(); k++) {
                char c = text.charAt(k);
                if (c != ' ') {
                    return n;
                }
                n++;
            }
        }
        return 0;
    }

    private static boolean[] centredRows(List<List<Run>> rows) {
        List<Component> lines = new ArrayList<>(rows.size());
        for (List<Run> row : rows) {
            lines.add(SpaceRuns.apply(row, new int[SpaceRuns.countSpaces(row)]));
        }
        return BlockLayout.centered(lines);
    }

    private static boolean columnPanel(List<List<Run>> rows) {
        for (List<Run> row : rows) {
            if (columns(SpaceRuns.chatSegmentWidths(row, SpaceRuns::runWidth)) >= 2) {
                return true;
            }
        }
        return false;
    }

    private static Component chatRow(List<Run> orig, List<Run> made,
                                     boolean centred, boolean panel,
                                     StringBuilder log) {
        int leadOrig = SpaceRuns.leadWidth(orig);
        int leadMade = SpaceRuns.leadWidth(made);
        int bodyOrig = SpaceRuns.rowWidth(orig) - leadOrig;
        int bodyMade = SpaceRuns.rowWidth(made) - leadMade;
        boolean single = columns(SpaceRuns.chatSegmentWidths(orig, SpaceRuns::runWidth)) < 2;
        boolean centre = single && leadOrig > 0 && (centred || panel);
        int target = centre ? leadOrig + (bodyOrig - bodyMade) / 2 : leadOrig;
        int pad = target - leadMade;
        int[] columns = columnPad(orig, made);
        int fitted = fitWidth(orig, made, pad, columns);
        log.append("  ").append(centre ? "置中" : "靠左")
           .append(" 原文縮排=").append(leadOrig).append(" 內容=").append(bodyOrig)
           .append("  譯文縮排=").append(leadMade).append(" 內容=").append(bodyMade)
           .append("  補=").append(pad)
           .append(fitted != pad ? "（收寬後 " + fitted + "）" : "")
           .append("  譯文=").append(SpaceRuns.rowText(made))
           .append(System.lineSeparator());
        pad = fitted;
        MutableComponent row = Component.empty();
        String encoded = SpaceOffset.encode(pad);
        if (!encoded.isEmpty()) {
            row.append(DisplayStyles.literal(encoded, SpaceOffset.styleFor(Style.EMPTY)));
        }
        for (int px : columns) {
            if (px != 0) {
                log.append("        欄距補正=")
                   .append(Arrays.toString(columns)).append(System.lineSeparator());
                break;
            }
        }
        row.append(SpaceRuns.applyChat(made, columns));
        return row;
    }

    private static int[] columnPad(List<Run> orig, List<Run> made) {
        return chatColumnPad(orig, made, SpaceRuns::runWidth);
    }

    private static int fitWidth(List<Run> orig, List<Run> made, int pad, int[] columns) {
        return fitWidth(orig, made, pad, columns, chatWidth());
    }

    static int fitWidth(List<Run> orig, List<Run> made, int pad, int[] columns, int window) {
        int limit = SpaceRuns.rowWidth(orig);
        if (window > limit
                && columns(SpaceRuns.chatSegmentWidths(orig, SpaceRuns::runWidth)) < 2) {
            limit = window;
        }
        int over = SpaceRuns.rowWidth(made) + pad - limit;
        for (int px : columns) {
            over += px;
        }
        if (over <= 0) {
            return pad;
        }
        boolean[] gaps = SpaceRuns.chatGaps(made);
        int k = columns.length;
        for (int i = made.size() - 1; i >= 0 && over > 0; i--) {
            if (!gaps[i]) {
                continue;
            }
            k--;
            if (k < 0) {
                break;
            }
            int floor = contentBefore(made, i) ? MIN_CELL_GAP : 0;
            int room = made.get(i).px() + columns[k] - floor;
            if (room <= 0) {
                continue;
            }
            int take = Math.min(room, over);
            columns[k] -= take;
            over -= take;
        }
        if (over > 0 && pad > 0) {
            pad -= Math.min(pad, over);
        }
        return pad;
    }

    private static int chatWidth() {
        try {
            return ChatComponent.getWidth(Minecraft.getInstance().options.chatWidth().get());
        } catch (Throwable t) {
            ErrorDebug.note("ChatRealigner#chatWidth", null, t);
            return 0;
        }
    }

    private static final int MIN_CELL_GAP = 4;

    private static boolean contentBefore(List<Run> runs, int at) {
        for (int i = at - 1; i >= 0; i--) {
            if (!runs.get(i).space() && LineTranslator.hasContent(runs.get(i).text())) {
                return true;
            }
        }
        return false;
    }

    static int[] chatColumnPad(List<Run> orig, List<Run> made, ToIntFunction<Run> width) {
        boolean[] madeGaps = SpaceRuns.chatGaps(made);
        int spaces = SpaceRuns.count(madeGaps);
        if (SpaceRuns.count(SpaceRuns.chatGaps(orig)) != spaces) {
            return new int[spaces];
        }
        int[] px = new int[spaces];
        int index = 0;
        for (int i = 0; i < made.size(); i++) {
            if (madeGaps[i]) {
                px[index++] = made.get(i).px();
            }
        }
        return columnDrift(SpaceRuns.chatSegmentWidths(orig, width), SpaceRuns.chatSegmentWidths(made, width), px);
    }

    static int[] columnDrift(List<Integer> from, List<Integer> to, int[] spacePx) {
        int[] adjust = new int[spacePx.length];
        if (columns(from) >= 2) {
            return centreColumns(from, to, spacePx);
        }
        int drift = 0;
        for (int i = 1; i < spacePx.length; i++) {
            drift += to.get(i) - from.get(i);
            if (spacePx[i] >= 0) {
                adjust[i] = -drift;
                drift = 0;
            }
        }
        return adjust;
    }

    private static int columns(List<Integer> widths) {
        int n = 0;
        for (int w : widths) {
            if (w > 0) {
                n++;
            }
        }
        return n;
    }

    private static int[] centreColumns(List<Integer> from, List<Integer> to, int[] spacePx) {
        int[] adjust = new int[spacePx.length];
        for (int k = 0; k < spacePx.length; k++) {
            if (spacePx[k] < 0) {
                continue;
            }
            int before = from.get(k) - to.get(k);
            int after = k + 1 < from.size() ? from.get(k + 1) - to.get(k + 1) : 0;
            adjust[k] = (before + after) / 2;
        }
        return adjust;
    }
}
