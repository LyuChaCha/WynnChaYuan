package com.wynnchayuan.translate;

import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.translate.SpaceRuns.Run;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ChatPanelFlow {

    private ChatPanelFlow() {}

    static boolean chatPanel(List<StyledText> rows) {
        for (StyledText row : rows) {
            for (List<Run> line : SpaceRuns.splitRows(SpaceRuns.runs(row.getComponent()))) {
                if (spacedColumns(line) >= 2) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean panelRow(StyledText row) {
        for (List<Run> line : SpaceRuns.splitRows(SpaceRuns.runs(row.getComponent()))) {
            int columns = 0;
            boolean text = false;
            for (Run run : line) {
                if (run.space()) {
                    if (text && run.px() >= PANEL_GAP_MIN) {
                        columns++;
                        text = false;
                    }
                } else if (LineTranslator.hasContent(run.text())) {
                    text = true;
                }
            }
            if ((text ? columns + 1 : columns) >= 2) {
                return true;
            }
        }
        return false;
    }

    private static final int PANEL_GAP_MIN = 16;

    private static final int PANEL_RUN_MAX = 8;

    private record PanelRow(List<Run> lead, List<List<Run>> cells, List<List<Run>> gaps) {

        static PanelRow parse(List<Run> runs) {
            List<Run> lead = new ArrayList<>();
            List<List<Run>> cells = new ArrayList<>();
            List<List<Run>> gaps = new ArrayList<>();
            List<Run> cell = null;
            List<Run> gap = null;
            boolean text = false;
            for (Run run : runs) {
                if (!run.space() && run.text().indexOf('\n') >= 0) {
                    return null;
                }
                if (run.space() && cell == null && gap == null) {
                    lead.add(run);
                } else if (run.space() && (gap != null || (text && run.px() > 0))) {
                    if (gap == null) {
                        gap = new ArrayList<>();
                        cells.add(cell);
                        gaps.add(gap);
                        cell = null;
                        text = false;
                    }
                    gap.add(run);
                } else {
                    if (cell == null) {
                        cell = new ArrayList<>();
                        gap = null;
                    }
                    cell.add(run);
                    text |= !run.space() && LineTranslator.hasContent(run.text());
                }
            }
            if (cell != null) {
                cells.add(cell);
                gaps.add(new ArrayList<>());
            }
            return new PanelRow(lead, cells, gaps);
        }

        static Component text(List<Run> cell) {
            MutableComponent out = Component.empty();
            for (Run run : cell) {
                out.append(Component.literal(run.text()).withStyle(run.style()));
            }
            return out;
        }

        static boolean hasText(List<Run> cell) {
            for (Run run : cell) {
                if (!run.space() && LineTranslator.hasContent(run.text())) {
                    return true;
                }
            }
            return false;
        }
    }

    // Lootrun 信標面板是伺服器折好行逐列送的，要接成整句查
    static Component[] flowPanel(List<StyledText> rows, TranslationStore store,
                                 boolean[] centred) {
        return flow(rows, store, centred, 2, true);
    }

    static Component[] flowColumn(List<StyledText> rows, TranslationStore store,
                                  boolean[] centred) {
        return flow(rows, store, centred, 1, false);
    }

    private static Component[] flow(List<StyledText> rows, TranslationStore store,
                                    boolean[] centred, int minColumns, boolean inPanel) {
        int n = rows.size();
        Component[] out = new Component[n];
        List<PanelRow> parsed = new ArrayList<>(n);
        int columns = 0;
        for (StyledText row : rows) {
            PanelRow one = PanelRow.parse(SpaceRuns.runs(row.getComponent()));
            if (one == null) {
                return out;
            }
            parsed.add(one);
            columns = Math.max(columns, one.cells().size());
        }
        if (columns < minColumns) {
            return out;
        }
        Component[][] made = new Component[n][];
        for (int i = 0; i < n; i++) {
            made[i] = new Component[parsed.get(i).cells().size()];
        }
        for (int col = 0; col < columns; col++) {
            List<int[]> thread = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                int count = parsed.get(i).cells().size();
                int cell = count == columns ? col : (count == 1 ? 0 : -1);
                if (cell >= 0 && PanelRow.hasText(parsed.get(i).cells().get(cell))) {
                    thread.add(new int[] {i, cell});
                }
            }
            int s = 0;
            while (s < thread.size()) {
                int took = 0;
                for (int len = Math.min(PANEL_RUN_MAX, thread.size() - s); len >= 2; len--) {
                    List<Component> lines = panelSentence(parsed, made, thread, s, len, store);
                    if (lines != null) {
                        for (int k = 0; k < len; k++) {
                            int[] at = thread.get(s + k);
                            made[at[0]][at[1]] = lines.get(k);
                        }
                        took = len;
                        break;
                    }
                }
                s += Math.max(1, took);
            }
        }
        for (int i = 0; i < n; i++) {
            PanelRow row = parsed.get(i);
            boolean any = false;
            for (Component cell : made[i]) {
                any |= cell != null;
            }
            if (!any) {
                continue;
            }
            MutableComponent rebuilt = Component.empty();
            rebuilt.append(PanelRow.text(row.lead()));
            for (int c = 0; c < row.cells().size(); c++) {
                Component cell = made[i][c];
                if (cell == null) {
                    Component original = PanelRow.text(row.cells().get(c));
                    cell = LineTranslator.translate(StyledText.fromComponent(original), store);
                    if (cell == null) {
                        cell = panelCell(row, c, store);
                    }
                    if (cell == null) {
                        cell = original;
                    }
                }
                rebuilt.append(cell);
                rebuilt.append(PanelRow.text(row.gaps().get(c)));
            }
            out[i] = DisplayStyles.unslant(ChatRealigner.realignChat(rows.get(i), rebuilt,
                    centred == null ? null : centred[i], inPanel));
        }
        return out;
    }

    private static Component panelCell(PanelRow row, int c, TranslationStore store) {
        List<Run> before = c == 0 ? row.lead() : row.gaps().get(c - 1);
        List<Run> after = row.gaps().get(c);
        if (before.isEmpty() && after.isEmpty()) {
            return null;
        }
        List<Run> cell = row.cells().get(c);
        for (int shape = 0; shape < 3; shape++) {
            List<Run> framed = new ArrayList<>();
            if (shape != 2) {
                framed.addAll(before);
            }
            framed.addAll(cell);
            if (shape != 0) {
                framed.addAll(after);
            }
            if (framed.size() == cell.size()) {
                continue;
            }
            Component hit = LineTranslator.translate(StyledText.fromComponent(PanelRow.text(framed)), store);
            if (hit == null) {
                continue;
            }
            List<Run> made = new ArrayList<>(SpaceRuns.runs(hit));
            while (!made.isEmpty() && made.get(0).space()) {
                made.remove(0);
            }
            while (!made.isEmpty() && made.get(made.size() - 1).space()) {
                made.remove(made.size() - 1);
            }
            if (!made.isEmpty() && PanelRow.hasText(made)) {
                return PanelRow.text(made);
            }
        }
        return null;
    }

    private static List<Component> panelSentence(List<PanelRow> parsed, Component[][] made,
                                                 List<int[]> thread, int s, int len,
                                                 TranslationStore store) {
        List<StyledText> cells = new ArrayList<>(len);
        StringBuilder key = new StringBuilder();
        List<List<Run>> picked = new ArrayList<>(len);
        for (int k = 0; k < len; k++) {
            int[] at = thread.get(s + k);
            if (made[at[0]][at[1]] != null
                    || (k > 0 && at[0] != thread.get(s + k - 1)[0] + 1)) {
                return null;
            }
            picked.add(parsed.get(at[0]).cells().get(at[1]));
        }
        Style prose = proseColour(picked);
        for (int k = 0; k < len; k++) {
            StyledText cell = StyledText.fromComponent(
                    PanelRow.text(withProse(picked.get(k), prose)));
            cells.add(cell);
            if (k > 0) {
                key.append('\n');
            }
            key.append(GlyphSplitter.toTemplate(cell));
        }
        String template = key.toString();
        if (store.lookup(template) == null && store.lookupFlat(template) == null
                && store.lookupUnwrapped(template) == null) {
            return null;
        }
        List<Component> lines = LineTranslator.translateBlock(cells, store, new boolean[len]);
        if (lines == null || lines.isEmpty()) {
            return null;
        }
        if (lines.size() != len) {
            lines = TextSplit.split(TextSplit.join(lines), len);
        }
        return lines;
    }

    private static Style proseColour(List<List<Run>> cells) {
        boolean bare = false;
        Map<TextColor, Integer> letters = new LinkedHashMap<>();
        for (List<Run> cell : cells) {
            for (Run run : cell) {
                if (run.space() || !LineTranslator.hasContent(run.text())) {
                    continue;
                }
                TextColor colour = run.style().getColor();
                if (colour == null) {
                    bare = true;
                    continue;
                }
                int count = (int) run.text().codePoints().filter(Character::isLetter).count();
                if (count > 0) {
                    letters.merge(colour, count, Integer::sum);
                }
            }
        }
        if (!bare) {
            return null;
        }
        TextColor best = null;
        int most = 0;
        for (Map.Entry<TextColor, Integer> e : letters.entrySet()) {
            if (e.getValue() > most) {
                most = e.getValue();
                best = e.getKey();
            }
        }
        return best == null ? null : Style.EMPTY.withColor(best);
    }

    private static List<Run> withProse(List<Run> cell, Style prose) {
        if (prose == null) {
            return cell;
        }
        List<Run> out = new ArrayList<>(cell.size());
        for (Run run : cell) {
            out.add(run.space() || run.style().getColor() != null ? run
                    : new Run(false, run.px(), run.style().withColor(prose.getColor()), run.text()));
        }
        return out;
    }

    private static int spacedColumns(List<Run> row) {
        int columns = 0;
        boolean text = false;
        for (Run run : row) {
            if (run.space()) {
                if (text && run.px() > 0) {
                    columns++;
                    text = false;
                }
            } else if (LineTranslator.hasContent(run.text())) {
                text = true;
            }
        }
        return text ? columns + 1 : columns;
    }
}
