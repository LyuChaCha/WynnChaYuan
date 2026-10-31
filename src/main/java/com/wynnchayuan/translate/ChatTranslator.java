package com.wynnchayuan.translate;

import com.wynnchayuan.capture.Broadcasts;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.translate.ColumnAligner.Piece;
import com.wynnchayuan.translate.SpaceRuns.Run;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ChatTranslator {

    private ChatTranslator() {}

    static Component translateChat(StyledText message, TranslationStore store) {
        return translateChat(message, store, null);
    }

    static LineParts framedOnMiss(StyledText line, LineParts plain,
                                  TranslationStore store) {
        if (line == null || plain == null || store == null) {
            return null;
        }
        Broadcasts.Spans spans = store.nameFrames().find(line, plain.template());
        return spans == null ? null : LineParts.of(line, spans);
    }

    static LineParts nameFramed(StyledText line, TranslationStore store) {
        if (line == null || store == null) {
            return null;
        }
        Broadcasts.Spans bomb = Broadcasts.find(line);
        if (bomb != null) {
            return LineParts.of(line, bomb);
        }
        LineParts plain = LineParts.of(line);
        String template = plain.template();
        if (template.isBlank() || !GlyphSplitter.hasLetter(template)) {
            return null;
        }
        String hit = TemplateLookup.lookup(template, store);
        if (hit == null || hit.isBlank()) {
            hit = store.lookupUnwrapped(template);
        }
        if (hit != null && !hit.isBlank()) {
            return null;
        }
        return framedOnMiss(line, plain, store);
    }

    static Component translateFramed(StyledText line, LineParts framed,
                                     TranslationStore store) {
        if (line == null || framed == null || store == null) {
            return null;
        }
        String translated = TemplateLookup.lookup(framed.template(), store);
        List<LineParts.Piece> glyphs = null;
        if (translated == null || translated.isBlank()) {
            translated = store.lookupUnwrapped(framed.template());
            if (translated == null || translated.isBlank()) {
                return null;
            }
            glyphs = TemplateRenderer.unwrappedGlyphs(framed.template(), framed.glyphs());
        }
        Component rebuilt = glyphs == null
                ? TemplateRenderer.rebuild(translated, framed, store)
                : TemplateRenderer.rebuild(translated, framed, glyphs, store);
        if (rebuilt == null) {
            return null;
        }
        return DisplayStyles.unslant(RowRealigner.realign(line, rebuilt, true, false));
    }

    static Component translateChat(StyledText message, TranslationStore store,
                                   Boolean centred) {
        return translateChat(message, store, centred, false);
    }

    static Component translateChat(StyledText message, TranslationStore store,
                                   Boolean centred, boolean inPanel) {
        LineParts parts = Broadcasts.parts(message);
        if (parts.template().isBlank() || !GlyphSplitter.hasLetter(parts.template())) {
            return null;
        }
        Component rebuilt = null;
        String tried = null;
        LineParts framed = null;
        for (int step = 0; step < 7 && rebuilt == null; step++) {
            LineParts using = parts;
            List<LineParts.Piece> glyphs = null;
            String hit = null;
            switch (step) {
                case 0 -> hit = TemplateLookup.lookup(parts.template(), store);
                case 1 -> hit = ColumnTemplates.byColumn(parts.template(), store);
                case 2 -> {
                    hit = store.lookupUnwrapped(parts.template());
                    glyphs = TemplateRenderer.unwrappedGlyphs(parts.template(), parts.glyphs());
                }
                case 3, 4 -> {
                    if (step == 3 && Broadcasts.find(message) == null) {
                        framed = framedOnMiss(message, parts, store);
                    }
                    if (framed != null) {
                        using = framed;
                        if (step == 3) {
                            hit = TemplateLookup.lookup(framed.template(), store);
                        } else {
                            hit = store.lookupUnwrapped(framed.template());
                            glyphs = TemplateRenderer.unwrappedGlyphs(framed.template(), framed.glyphs());
                        }
                    }
                }
                case 5 -> {
                    if (parts.template().indexOf('\n') >= 0) {
                        hit = TemplateLookup.eventFrame(TranslationStore.unwrap(parts.template()), store);
                        glyphs = TemplateRenderer.unwrappedGlyphs(parts.template(), parts.glyphs());
                    }
                }
                default -> hit = ColumnTemplates.welcomeBanner(parts.template(), store);
            }
            if (hit == null || hit.isBlank()) {
                continue;
            }
            tried = hit;
            rebuilt = glyphs == null
                    ? TemplateRenderer.rebuild(hit, using, store)
                    : TemplateRenderer.rebuild(hit, using, glyphs, store);
            if (rebuilt != null) {
                parts = using;
            }
        }
        if (rebuilt == null) {
            if (tried == null) {
                FlowedDebug.chatRows(message.getString(), "  鍵：" + parts.template(),
                                     "語料裡查不到這一塊");
            } else {
                FlowedDebug.chatRows(message.getString(), "  譯文：" + tried,
                                     "佔位符數量對不上，整塊放棄");
            }
            return null;
        }
        rebuilt = rowColours(message, rebuilt, BlockTranslator.dominantStyle(List.of(parts)));
        if (parts.template().startsWith(TemplateLookup.CHAT_COUNTER)) {
            rebuilt = npcChatColours(message, rebuilt);
        }
        return DisplayStyles.unslant(ChatRealigner.realignChat(message, rebuilt, centred, inPanel));
    }

    private static final Pattern CHAT_HEAD = Pattern.compile("^(\\[\\d+/\\d+])( )([^:：\\n]+?[:：])");

    static Component npcChatColours(StyledText message, Component rebuilt) {
        try {
            List<Run> src = SpaceRuns.runs(message.getComponent());
            String srcText = joinedText(src);
            Matcher s = CHAT_HEAD.matcher(srcText);
            if (!s.find()) {
                return rebuilt;
            }
            TextColor counter = uniformColour(src, s.start(1), s.end(1));
            TextColor speaker = uniformColour(src, s.start(3), s.end(3));
            List<Run> made = SpaceRuns.runs(rebuilt);
            Matcher d = CHAT_HEAD.matcher(joinedText(made));
            if (!d.find() || (counter == null && speaker == null)) {
                return rebuilt;
            }
            int[] cuts = {d.start(1), d.end(1), d.start(3), d.end(3)};
            MutableComponent out = Component.empty();
            int at = 0;
            for (Run run : made) {
                String text = run.text();
                int from = 0;
                while (from < text.length()) {
                    int next = text.length();
                    for (int cut : cuts) {
                        if (cut > at + from && cut < at + next) {
                            next = cut - at;
                        }
                    }
                    String piece = text.substring(from, next);
                    int pos = at + from;
                    Style style = run.style() == null ? Style.EMPTY : run.style();
                    if (!run.space()) {
                        if (counter != null && pos >= cuts[0] && pos < cuts[1]) {
                            style = style.withColor(counter);
                        } else if (speaker != null && pos >= cuts[2] && pos < cuts[3]) {
                            style = style.withColor(speaker);
                        }
                    }
                    out.append(Component.literal(piece).withStyle(style));
                    from = next;
                }
                at += text.length();
            }
            return out;
        } catch (Throwable t) {
            ErrorDebug.note("ChatTranslator#npcChatColours", null, t);
            return rebuilt;
        }
    }

    private static String joinedText(List<Run> runs) {
        StringBuilder out = new StringBuilder();
        for (Run run : runs) {
            out.append(run.text());
        }
        return out.toString();
    }

    private static TextColor uniformColour(List<Run> runs, int from, int to) {
        TextColor found = null;
        int at = 0;
        for (Run run : runs) {
            int start = at;
            int end = at + run.text().length();
            at = end;
            if (end <= from || start >= to || run.space()) {
                continue;
            }
            String inside = run.text().substring(Math.max(0, from - start),
                                                 Math.min(run.text().length(), to - start));
            if (inside.isBlank()) {
                continue;
            }
            TextColor colour = run.style() == null ? null : run.style().getColor();
            if (colour == null || found != null && !found.equals(colour)) {
                return null;
            }
            found = colour;
        }
        return found;
    }

    static Component rowColours(StyledText original, Component rebuilt, Style blockStyle) {
        TextColor block = blockStyle == null ? null : blockStyle.getColor();
        List<List<Run>> origRows = SpaceRuns.splitRows(SpaceRuns.runs(original.getComponent()));
        List<List<Run>> madeRows = SpaceRuns.splitRows(SpaceRuns.runs(rebuilt));
        int[] keepOrig = RowRealigner.solidRows(origRows);
        int[] keepMade = RowRealigner.solidRows(madeRows);
        int count = keepOrig[1] - keepOrig[0];
        if (count < 2 || count != keepMade[1] - keepMade[0]) {
            return rebuilt;
        }
        TextColor[] rowColour = new TextColor[count];
        boolean[] rowBold = new boolean[count];
        boolean any = false;
        for (int k = 0; k < count; k++) {
            rowBold[k] = allBold(origRows.get(keepOrig[0] + k))
                    && !allBold(madeRows.get(keepMade[0] + k));
            any |= rowBold[k];
            if (block == null) {
                continue;
            }
            Map<TextColor, Integer> weight = new LinkedHashMap<>();
            boolean usesBlock = false;
            for (Run run : origRows.get(keepOrig[0] + k)) {
                TextColor colour = run.style() == null ? null : run.style().getColor();
                if (run.space() || colour == null || !LineTranslator.hasContent(run.text())) {
                    continue;
                }
                if (colour.equals(block)) {
                    usesBlock = true;
                    break;
                }
                weight.merge(colour, run.text().strip().length(), Integer::sum);
            }
            if (usesBlock || weight.isEmpty()) {
                continue;
            }
            rowColour[k] = Collections.max(weight.entrySet(),
                    Map.Entry.comparingByValue()).getKey();
            any = true;
        }
        if (!any) {
            return rebuilt;
        }
        MutableComponent out = Component.empty();
        for (int r = 0; r < madeRows.size(); r++) {
            if (r > 0) {
                out.append(Component.literal("\n"));
            }
            int k = r - keepMade[0];
            TextColor colour = k >= 0 && k < count ? rowColour[k] : null;
            boolean bold = k >= 0 && k < count && rowBold[k];
            for (Run run : madeRows.get(r)) {
                Style style = run.style();
                if (colour != null && !run.space() && style != null
                        && block.equals(style.getColor())) {
                    style = style.withColor(colour);
                }
                if (bold && !run.space() && style != null && LineTranslator.hasContent(run.text())) {
                    style = style.withBold(true);
                }
                out.append(Component.literal(run.text()).withStyle(style));
            }
        }
        return out;
    }

    private static boolean allBold(List<Run> row) {
        boolean any = false;
        for (Run run : row) {
            if (run.space() || !LineTranslator.hasContent(run.text())) {
                continue;
            }
            if (run.style() == null || !Boolean.TRUE.equals(run.style().isBold())) {
                return false;
            }
            any = true;
        }
        return any;
    }
}
