package com.wynnchayuan.translate;

import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.translate.SpaceRuns.Run;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class DiscoveryBlock {

    private DiscoveryBlock() {}

    private static final Pattern HEAD = Pattern.compile("(?:Area Discovered|Secret Discovery): .+");

    private static final Pattern TITLE = Pattern.compile(".+ \\[\\d+/\\d+]");

    // 伺服器用純空白置中，算得比實際寬：短列中心在 128、長列在 151
    private static final int CENTRE_MIN_X2 = 240;
    private static final int CENTRE_MAX_X2 = 320;

    public record Block(int head, int title, int from, int to) {

        public boolean hasProse() {
            return to > from;
        }
    }

    public static boolean isHead(StyledText row) {
        return HEAD.matcher(row.getStringWithoutFormatting().strip()).matches();
    }

    public static List<Block> blocks(List<StyledText> rows) {
        List<Block> out = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            if (isHead(rows.get(i))) {
                Block block = parse(rows, i);
                out.add(block);
                i = Math.max(i, block.to() - 1);
            }
        }
        return out;
    }

    private static Block parse(List<StyledText> rows, int head) {
        int at = head + 1;
        int title = -1;
        if (at < rows.size() && TITLE.matcher(rows.get(at).getStringWithoutFormatting().strip()).matches()) {
            title = at++;
        }
        while (at < rows.size() && !hasText(rows.get(at))) {
            at++;
        }
        int from = at;
        if (at < rows.size()) {
            TextColor colour = colourOf(rows.get(at));
            while (at < rows.size() && colour != null && proseRow(rows.get(at), colour)) {
                at++;
            }
        }
        return new Block(head, title, from, at);
    }

    private static boolean hasText(StyledText row) {
        return LineTranslator.hasContent(row.getStringWithoutFormatting());
    }

    private static TextColor colourOf(StyledText row) {
        TextColor found = null;
        for (Run run : SpaceRuns.runs(row.getComponent())) {
            if (run.space() || !LineTranslator.hasContent(run.text())) {
                continue;
            }
            TextColor colour = run.style().getColor();
            if (colour == null || (found != null && !found.equals(colour))) {
                return null;
            }
            found = colour;
        }
        return found;
    }

    private static boolean proseRow(StyledText row, TextColor colour) {
        if (!colour.equals(colourOf(row))) {
            return false;
        }
        List<Run> runs = SpaceRuns.runs(row.getComponent());
        int centre = SpaceRuns.leadWidth(runs) + SpaceRuns.rowWidth(runs);
        return centre >= CENTRE_MIN_X2 && centre <= CENTRE_MAX_X2;
    }

    private static StyledText sentence(List<StyledText> rows, Block block) {
        List<Component> lines = new ArrayList<>(block.to() - block.from());
        for (StyledText row : rows.subList(block.from(), block.to())) {
            lines.add(row.getComponent());
        }
        return StyledText.fromComponent(TextSplit.lines(TextSplit.join(lines)).get(0));
    }

    public static String sentenceTemplate(List<StyledText> rows, Block block) {
        return LineParts.of(sentence(rows, block)).template();
    }

    public static String headTemplate(List<StyledText> rows, Block block) {
        return LineParts.of(rows.get(block.head())).template();
    }

    public static Component prose(List<StyledText> rows, Block block, TranslationStore store) {
        LineParts parts = LineParts.of(sentence(rows, block));
        String hit = lookup(headTemplate(rows, block), parts.template(), store);
        Component rebuilt = hit == null ? null : TemplateRenderer.rebuild(hit, parts, store);
        return rebuilt == null ? null : DisplayStyles.unslant(ChatRealigner.centredProse(rebuilt));
    }

    private static String lookup(String head, String sentence, TranslationStore store) {
        String hit = TemplateLookup.lookup(sentence, store);
        if (hit != null && !hit.isBlank()) {
            return hit;
        }
        String both = store.lookupUnwrapped(head + " " + sentence);
        int row = both == null ? -1 : both.indexOf('\n');
        return row < 0 || both.substring(row).isBlank() ? null : both.substring(row + 1);
    }

    public static Component centred(Component row) {
        return ChatRealigner.onCentreLine(row);
    }
}
