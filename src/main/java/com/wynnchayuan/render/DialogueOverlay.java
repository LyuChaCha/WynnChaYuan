package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class DialogueOverlay {

    private static final int BOTTOM_MARGIN = 70;

    private static final int PADDING = 4;

    private static volatile List<Component> current = List.of();

    private static volatile List<List<Component>> choices = List.of();

    private static volatile int picked = 0;

    // 選項不在 Wynntils 對話事件裡，只在原始 action bar
    private static volatile List<String> rawChoices = List.of();

    private static volatile String speaker = null;

    private static volatile boolean needsShift = false;

    private static volatile List<String> settledFor = List.of();

    private static volatile List<List<Component>> shown = List.of();

    private static volatile long lastUpdate = 0;

    private static final net.minecraft.network.chat.TextColor SPEAKER_COLOUR =
            net.minecraft.network.chat.TextColor.fromRgb(0xFFD966);

    private DialogueOverlay() {}

    public static void setShiftPrompt(boolean required) {
        needsShift = required;
    }

    public static void noteChoices(List<String> raw, int selected) {
        List<String> incoming = raw == null ? List.of() : List.copyOf(raw);
        picked = selected;
        if (!incoming.isEmpty()) {
            lastUpdate = System.currentTimeMillis();
        }
        List<String> before = lastWindows;
        lastWindows = incoming;
        if (incoming.equals(rawChoices)) {
            return;
        }
        if (scrolling(incoming) || sameScroll(before, incoming)) {
            return;
        }
        rawChoices = incoming;
        choices = incoming.isEmpty()
                || WynnChaYuan.config().choiceMode() == CollectorConfig.DialogueMode.OFF
                ? List.of()
                : translateChoices(WynnChaYuan.translations());
    }

    private static boolean scrolling(List<String> picks) {
        for (String pick : picks) {
            if (!pick.isEmpty() && pick.charAt(0) >= 'a' && pick.charAt(0) <= 'z') {
                return true;
            }
        }
        return false;
    }

    private static volatile List<String> lastWindows = List.of();

    static boolean sameScroll(List<String> before, List<String> now) {
        if (before == null || now == null || before.isEmpty()
                || before.size() != now.size() || before.equals(now)) {
            return false;
        }
        for (int i = 0; i < now.size(); i++) {
            String was = before.get(i);
            String is = now.get(i);
            if (!was.equals(is) && !Marquee.continues(was, is)) {
                return false;
            }
        }
        return true;
    }

    public static void setCurrent(StyledText dialogue, TranslationStore store) {
        setCurrent(dialogue, store, false);
    }

    public static void setCurrent(StyledText dialogue, TranslationStore store,
                                  boolean hasChoices) {
        if (hasChoices && dialogue != null) {
            com.wynnchayuan.translate.FlowedDebug.noteChoices(dialogue.getString());
        }
        boolean bodyOff = WynnChaYuan.config().dialogueMode()
                == CollectorConfig.DialogueMode.OFF;
        boolean choiceOff = WynnChaYuan.config().choiceMode()
                == CollectorConfig.DialogueMode.OFF;
        if (dialogue == null || (bodyOff && choiceOff)) {
            clear();
            return;
        }
        // 換句時 Wynntils 會用上一句補發事件；結尾偏移字元每幀換
        String whole = plainOf(dialogue.getString());
        long now = System.currentTimeMillis();
        boolean sameLine = !lineText.isEmpty() && whole.startsWith(lineText);
        boolean skip = whole.length() - (sameLine ? lineText.length() : 0)
                >= DialogueRewriter.SKIP_JUMP;
        if (sameLine) {
            lineText = whole;
        } else if (!previousText.isEmpty() && previousText.equals(whole)) {
            return;
        } else {
            previousText = lineText;
            lineText = whole;
            lineStart = now;
            lineLogged = false;
        }
        boolean wasShowing = !current.isEmpty();
        boolean steady = settled(dialogue.getString()) || skip;
        List<Component> lines = new ArrayList<>();
        List<List<Component>> options = choiceOff ? List.of() : choices;
        boolean any = false;
        String said = null;
        String[] raws = dialogue.getString().split("\n");
        for (int r = 0; r < raws.length; r++) {
            raws[r] = plainOf(raws[r]);
        }
        List<String> sources = new ArrayList<>(raws.length);
        List<String> seenRaws = new ArrayList<>(raws.length);
        List<List<Component>> rendered = new ArrayList<>(raws.length);
        for (int i = 0; i < raws.length; i++) {
            StyledText line = DialogueIcons.line(raws[i], store);
            String template = com.wynnchayuan.capture.GlyphSplitter.toTemplate(line);
            List<Component> target = lines;

            String settled = skip ? null : settledSource(i, raws[i], template);
            seenRaws.add(raws[i]);
            if (settled != null) {
                noteHeard(store, settled, template);
                List<Component> box = shown.get(i);
                if (shownGeneration != DialogueIcons.generation()) {
                    Component again = recolour(line, settled, store);
                    if (again != null) {
                        box = Boxes.toLines(again);
                    }
                }
                sources.add(settled);
                rendered.add(box);
                target.addAll(box);
                any = true;
                if (said == null) {
                    said = speakerFor(store, settled);
                }
                continue;
            }

            LineResult result = translateLine(line, template, store, steady);
            Component translated = result == null ? null : result.translated();
            String source = result == null ? null : result.source();
            if (translated != null) {
                any = true;
                noteHeard(store, source, template);
                if (said == null) {
                    said = speakerFor(store, template);
                }
                List<Component> box = Boxes.toLines(translated);
                sources.add(source);
                rendered.add(box);
                target.addAll(box);
            } else {
                sources.add(null);
                rendered.add(List.of());
                target.addAll(Boxes.toLines(LineTranslator.untranslated(line)));
            }
        }
        settledFor = java.util.Collections.unmodifiableList(new ArrayList<>(sources));
        settledRaws = java.util.Collections.unmodifiableList(seenRaws);
        shown = List.copyOf(rendered);
        shownGeneration = DialogueIcons.generation();
        speaker = said != null && !said.isBlank() ? said : null;
        WynnChaYuan.store().noteEvent(any ? "dialogue.shown" : "dialogue.noMatch");
        current = any && !bodyOff ? List.copyOf(lines) : List.of();
        if (!any && !bodyOff && wasShowing && !skip && covers(raws, store, steady, now)) {
            current = blankLike(lastShown);
            speaker = null;
            lastUpdate = now;
            logPanel();
            return;
        }
        if (any && !bodyOff) {
            lastShown = current;
            if (!lineLogged) {
                lineLogged = true;
                DialogueStartLog.recognised(lineText.length(), now - lineStart);
            }
        }
        if ((any && !bodyOff) || !options.isEmpty()) {
            lastUpdate = System.currentTimeMillis();
        }
        logPanel();
    }

    private static void logPanel() {
        int chars = 0;
        for (Component row : current) {
            chars += (int) row.getString().codePoints().filter(cp -> !Character.isWhitespace(cp)
                    && cp != 0x3000).count();
        }
        DialogueStartLog.panelFrame(lineText.length(), chars, current.size());
    }

    static String plainOf(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        text.codePoints()
                .filter(cp -> !com.wynnchayuan.translate.SpaceOffset.isOffset(cp))
                .forEach(out::appendCodePoint);
        return out.toString().strip();
    }

    private static String lineText = "";
    private static String previousText = "";
    private static long lineStart;
    private static boolean lineLogged;

    private static List<Component> lastShown = List.of();

    private static boolean covers(String[] raws, TranslationStore store, boolean steady, long now) {
        var config = WynnChaYuan.config();
        if ((config != null && !config.dialogueCover()) || steady || raws.length == 0
                || now - lineStart > DialogueRewriter.HOLD_MS
                || raws[raws.length - 1].strip().length() > DialogueRewriter.HOLD_CHARS) {
            return false;
        }
        StyledText line = DialogueIcons.line(raws[raws.length - 1], store);
        return store.couldStillMatch(com.wynnchayuan.capture.GlyphSplitter.toTemplate(line));
    }

    static List<Component> blankLike(List<Component> shown) {
        if (shown.isEmpty()) {
            return List.of(Component.literal(" "));
        }
        List<Component> out = new ArrayList<>(shown.size());
        for (Component row : shown) {
            StringBuilder sb = new StringBuilder();
            row.getString().codePoints().forEach(cp -> sb.append(cp >= 0x2E80 ? '　' : ' '));
            out.add(Component.literal(sb.toString()));
        }
        return List.copyOf(out);
    }

    static List<Component> currentForTest() {
        return current;
    }

    static void resetForTest() {
        clear();
    }

    private static volatile int shownGeneration = -1;

    static Component recolour(StyledText line, String source, TranslationStore store) {
        String dst = store.lookup(source);
        com.wynnchayuan.capture.LineParts parts = com.wynnchayuan.capture.LineParts.of(line);
        if (dst == null || dst.isBlank()
                || !(DialogueIcons.iconic(parts) || DialogueIcons.hasColourMarks(dst))) {
            return null;
        }
        return DialogueIcons.forPanel(LineTranslator.translateKnown(
                line, DialogueIcons.prepare(dst, parts.glyphs()), store));
    }

    static LineResult translateLine(StyledText line, String template, TranslationStore store) {
        return translateLine(line, template, store, true);
    }

    static LineResult translateLine(StyledText line, String template, TranslationStore store,
                                    boolean steady) {
        if (DialogueRewriter.learnName(line.getString(), store)) {
            template = com.wynnchayuan.capture.LineParts.of(line).template();
        }
        com.wynnchayuan.capture.LineParts parts = com.wynnchayuan.capture.LineParts.of(line);
        boolean iconic = DialogueIcons.iconic(parts);
        String exact = store.lookup(template.strip());
        Component translated;
        if (iconic || DialogueIcons.hasColourMarks(exact)) {
            translated = exact == null || exact.isBlank() ? null
                    : LineTranslator.translateKnown(line,
                            DialogueIcons.prepare(exact, parts.glyphs()), store);
        } else {
            translated = LineTranslator.translate(line, store);
        }
        String source = translated == null ? null : template;
        if (translated != null && !steady && unfinished(template, store)) {
            translated = null;
            source = null;
        }
        if (translated == null) {
            source = store.matchPrefix(template, template.strip().length(),
                                       com.wynnchayuan.capture.CurrentQuest.get());
            if (source == null) {
                source = store.nearQuestLine(template,
                                             com.wynnchayuan.capture.CurrentQuest.get());
            }
            if (source == null) {
                source = FORECAST.guess(store, template.strip());
            }
            if (source != null) {
                String dst = store.lookup(source);
                translated = LineTranslator.translateKnown(
                        line, DialogueIcons.prepare(dst, parts.glyphs()), store);
            }
        }
        translated = DialogueIcons.forPanel(translated);
        return translated == null ? null : new LineResult(source, translated);
    }

    record LineResult(String source, Component translated) {}

    private static boolean unfinished(String template, TranslationStore store) {
        String typed = template == null ? "" : template.strip();
        return typed.length() < LOOKS_WHOLE
                && store.hasLonger(typed)
                && !DialogueRewriter.sentenceEnd(typed);
    }

    private static final int LOOKS_WHOLE = 12;

    private static boolean settled(String raw) {
        if (raw.equals(lastRaw)) {
            still++;
        } else {
            lastRaw = raw;
            still = 0;
        }
        return still >= SETTLE_EVENTS;
    }

    private static String lastRaw = "";
    private static int still = 0;

    private static final int SETTLE_EVENTS = 6;

    private static String settledSource(int index, String raw, String template) {
        List<String> sources = settledFor;
        List<String> raws = settledRaws;
        List<List<Component>> boxes = shown;
        if (index >= sources.size() || index >= boxes.size() || index >= raws.size()) {
            return null;
        }
        String source = sources.get(index);
        return continues(raws.get(index), raw) && canReuse(source, template) ? source : null;
    }

    static boolean continues(String before, String now) {
        return before != null && now != null && now.startsWith(before);
    }

    private static volatile List<String> settledRaws = List.of();

    static final DialogueForecast FORECAST = new DialogueForecast();

    static void noteHeard(TranslationStore store, String source, String template) {
        if (source == null || template == null) {
            return;
        }
        String typed = template.strip();
        if (typed.length() >= DialogueForecast.CONFIRM || source.equals(typed)) {
            FORECAST.heard(store, source);
        }
    }

    static boolean canReuse(String source, String template) {
        return source != null && template != null
                && DialogueRewriter.within(source, template.strip());
    }

    static List<List<Component>> translateChoices(TranslationStore store) {
        List<String> raw = rawChoices;
        if (raw.isEmpty()) {
            return List.of();
        }
        List<List<Component>> out = new ArrayList<>(raw.size());
        for (String each : raw) {
            StyledText line = StyledText.fromString(each);
            Component hit = LineTranslator.translate(line, store);
            out.add(List.copyOf(Boxes.toLines(
                    hit != null ? hit : LineTranslator.untranslated(line))));
        }
        return List.copyOf(out);
    }

    private static String speakerFor(TranslationStore store, String template) {
        String said = store.speakerOf(template);
        return said != null ? said : store.speakerOfPrefix(template);
    }

    public static boolean hasContent() {
        return !current.isEmpty() || !choices.isEmpty();
    }

    public static void stillOnScreen() {
        boolean inPlace = WynnChaYuan.config().dialogueMode()
                == CollectorConfig.DialogueMode.REPLACE;
        lastUpdate = keptAlive(lastUpdate, System.currentTimeMillis(), hasContent(),
                needsShift, inPlace);
    }

    static long keptAlive(long lastUpdate, long now, boolean hasContent,
                          boolean waitsForShift, boolean inPlace) {
        return hasContent && (waitsForShift || inPlace) ? now : lastUpdate;
    }

    public static void clear() {
        lineText = "";
        previousText = "";
        lastShown = List.of();
        current = List.of();
        choices = List.of();
        rawChoices = List.of();
        lastWindows = List.of();
        picked = 0;
        speaker = null;
        needsShift = false;
        settledFor = List.of();
        settledRaws = List.of();
        shown = List.of();
        lastUpdate = 0;
    }

    public static void render(GuiGraphics graphics) {
        List<Component> lines = current;
        List<List<Component>> options = choices;
        if (lines.isEmpty() && options.isEmpty()) {
            return;
        }
        float alpha = Fade.alphaFor(lastUpdate, WynnChaYuan.config().dialogueHoldMs());
        if (alpha <= 0f) {
            current = List.of();
            choices = List.of();
            rawChoices = List.of();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (WynnChaYuan.config().dialogueMode() == CollectorConfig.DialogueMode.REPLACE
                && !DialogueRewriter.bodyTooLong()) {
            if (!options.isEmpty() && choicesInPanel()) {
                drawChoices(graphics, mc, options, alpha);
            }
            return;
        }
        if (speaker != null && !lines.isEmpty()) {
            List<Component> withName = new ArrayList<>(lines.size() + 1);
            withName.add(speakerLine(speaker));
            withName.addAll(lines);
            lines = withName;
        }
        int lineHeight = mc.font.lineHeight + 1;
        boolean sized = WynnChaYuan.config()
                .hasOverlaySize(CollectorConfig.Overlay.DIALOGUE);
        int cap = Math.min(MAX_BOX_W, graphics.guiWidth() - CHOICE_EDGE * 2);
        int natural = sized ? 0 : widthOf(mc, lines);
        boolean folded = !sized && natural > cap;
        int boxW = sized ? dialogueWidth(0) : folded ? cap : natural;
        int rowCount = folded ? wrapAll(mc, lines, boxW - PADDING * 2).size() : lines.size();
        int boxH = lines.isEmpty() ? 0
                : atLeastTall(rowCount * lineHeight + PADDING * 2);

        int x = (graphics.guiWidth() - boxW) / 2;
        int y = graphics.guiHeight() - BOTTOM_MARGIN - boxH;
        if (WynnChaYuan.config().hasOverlayPos(CollectorConfig.Overlay.DIALOGUE)) {
            x = WynnChaYuan.config().overlayX(CollectorConfig.Overlay.DIALOGUE) - boxW / 2;
            y = WynnChaYuan.config().overlayY(CollectorConfig.Overlay.DIALOGUE);
        }
        int[] at = Boxes.intoScreen(x, y, boxW, boxH, graphics.guiWidth(), graphics.guiHeight());
        x = at[0];
        y = at[1];

        if (!lines.isEmpty()) {
            if (sized || folded) {
                drawSized(graphics, mc, lines, x, y, boxW, alpha);
            } else {
                draw(graphics, mc, lines, x + boxW / 2, y, alpha, true);
            }
        }
        if (!options.isEmpty() && choicesInPanel()) {
            drawChoices(graphics, mc, options, alpha);
        }
    }

    private static boolean choicesInPanel() {
        return WynnChaYuan.config().choiceMode() == CollectorConfig.DialogueMode.PANEL;
    }

    private static void drawInPlace(GuiGraphics graphics, Minecraft mc,
                                    List<Component> lines, List<Component> options,
                                    float alpha) {
        int boxW = dialogueWidth(Math.max(MIN_BOX_W, Math.min(MAX_BOX_W,
                graphics.guiWidth() * 5 / 8)));
        int inner = boxW - PADDING * 2 - 2;
        int lineHeight = mc.font.lineHeight + 1;

        List<FormattedCharSequence> body = wrapAll(mc, lines, inner);
        List<FormattedCharSequence> picks = wrapAll(mc, options, inner);

        int bodyH = body.isEmpty() ? 0
                : atLeastTall(body.size() * lineHeight + PADDING * 2);
        int x = (graphics.guiWidth() - boxW) / 2;
        int y = graphics.guiHeight() - IN_PLACE_MARGIN - bodyH;

        if (!body.isEmpty()) {
            Boxes.draw(graphics, x, y, boxW, bodyH, alpha);
            int textY = y + PADDING;
            for (FormattedCharSequence line : body) {
                graphics.drawString(mc.font, line, x + PADDING + 1, textY,
                        Colors.fade(Colors.TEXT, alpha));
                textY += lineHeight;
            }
            if (speaker != null) {
                int nameW = mc.font.width(speaker) + PADDING * 2 + 2;
                int nameH = mc.font.lineHeight + PADDING;
                Boxes.draw(graphics, x, y - nameH + 1, nameW, nameH, alpha);
                graphics.drawString(mc.font, speaker, x + PADDING + 1,
                        y - nameH + PADDING / 2 + 2,
                        Colors.fade(SPEAKER_COLOUR.getValue() | 0xFF000000, alpha));
            }
            if (needsShift) {
                String hint = SHIFT_HINT.get();
                int hintW = mc.font.width(hint);
                graphics.drawString(mc.font, hint,
                        x + (boxW - hintW) / 2, y + bodyH + 3,
                        Colors.fade(Colors.TEXT, alpha * 0.75f));
            }
        }

        if (!picks.isEmpty()) {
            int pickH = picks.size() * lineHeight + PADDING * 2;
            int pickY = y - pickH - CHOICE_GAP - (speaker == null ? 0 : mc.font.lineHeight);
            Boxes.draw(graphics, x, pickY, boxW, pickH, alpha);
            int textY = pickY + PADDING;
            for (FormattedCharSequence line : picks) {
                graphics.drawString(mc.font, line, x + PADDING + 1, textY,
                        Colors.fade(Colors.TEXT, alpha));
                textY += lineHeight;
            }
        }
    }

    private static void drawChoices(GuiGraphics graphics, Minecraft mc,
                                    List<List<Component>> options, float alpha) {
        CollectorConfig cfg = WynnChaYuan.config();
        int boxW = cfg.hasOverlaySize(CollectorConfig.Overlay.CHOICES)
                ? cfg.overlayW(CollectorConfig.Overlay.CHOICES)
                : Math.max(MIN_CHOICE_W, Math.min(MAX_CHOICE_W,
                        graphics.guiWidth() / 4));
        int inner = boxW - PADDING * 2 - MARKER_W - 2;
        int lineHeight = mc.font.lineHeight + 1;

        List<List<FormattedCharSequence>> rows = new ArrayList<>(options.size());
        int textLines = 0;
        for (List<Component> option : options) {
            List<FormattedCharSequence> wrapped = wrapAll(mc, option, inner);
            if (wrapped.isEmpty()) {
                continue;
            }
            rows.add(wrapped);
            textLines += wrapped.size();
        }
        if (rows.isEmpty()) {
            return;
        }
        int boxH = textLines * lineHeight + PADDING * 2
                + (rows.size() - 1) * (ROW_GAP * 2 + 1);

        int x = graphics.guiWidth() - boxW - CHOICE_EDGE;
        int y = graphics.guiHeight() * 5 / 8;
        if (WynnChaYuan.config().hasOverlayPos(CollectorConfig.Overlay.CHOICES)) {
            x = WynnChaYuan.config().overlayX(CollectorConfig.Overlay.CHOICES)
                    - boxW / 2;
            y = WynnChaYuan.config().overlayY(CollectorConfig.Overlay.CHOICES);
        }
        x = Math.max(2, Math.min(graphics.guiWidth() - boxW - 2, x));
        y = Math.max(2, Math.min(graphics.guiHeight() - boxH - 2, y));

        Boxes.draw(graphics, x, y, boxW, boxH, alpha);

        int textY = y + PADDING;
        for (int i = 0; i < rows.size(); i++) {
            List<FormattedCharSequence> option = rows.get(i);
            int rowH = option.size() * lineHeight;
            boolean here = picked == i + 1;

            if (here) {
                graphics.fill(x + 1, textY - ROW_GAP + 1, x + boxW - 1,
                              textY + rowH + ROW_GAP - 1,
                              Colors.fade(SELECTED_BG, alpha));
                graphics.drawString(mc.font, MARKER, x + PADDING,
                        textY + (rowH - mc.font.lineHeight) / 2,
                        Colors.fade(Colors.HIGHLIGHT, alpha));
            }
            int colour = here ? Colors.HIGHLIGHT : Colors.TEXT;
            int lineY = textY;
            for (FormattedCharSequence line : option) {
                graphics.drawString(mc.font, line, x + PADDING + MARKER_W, lineY,
                        Colors.fade(colour, alpha));
                lineY += lineHeight;
            }
            textY += rowH;

            if (i < rows.size() - 1) {
                int sepY = textY + ROW_GAP;
                for (int dx = x + PADDING; dx < x + boxW - PADDING;
                        dx += DASH + DASH_GAP) {
                    int end = Math.min(dx + DASH, x + boxW - PADDING);
                    graphics.fill(dx, sepY, end, sepY + 1,
                                  Colors.fade(Colors.FAINT, alpha));
                }
                textY += ROW_GAP * 2 + 1;
            }
        }
    }

    private static final int SELECTED_BG = 0x40FFD24A;

    private static final String MARKER = "\u25B6";

    private static final int MARKER_W = 8;

    private static final int ROW_GAP = 2;

    private static final int DASH = 3;

    private static final int DASH_GAP = 2;

    private static final int MIN_CHOICE_W = 120;

    private static final int MAX_CHOICE_W = 260;

    private static final int CHOICE_EDGE = 8;

    // MC 的 font.split 只在空白斷行，中文標點會落到行首
    private static List<FormattedCharSequence> wrapAll(Minecraft mc,
                                                       List<Component> lines, int width) {
        List<FormattedCharSequence> out = new ArrayList<>();
        for (Component line : lines) {
            for (Component row : fold(line, Math.max(16, width))) {
                out.add(row.getVisualOrderText());
            }
        }
        return out;
    }

    static List<Component> fold(Component line, int width) {
        List<com.wynnchayuan.capture.LineParts.Piece> pieces = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        line.visit((style, text) -> {
            pieces.add(new com.wynnchayuan.capture.LineParts.Piece(text, style));
            plain.append(text);
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        List<int[]> rows = DialogueRewriter.flow(plain.toString(), null, width);
        if (rows.size() <= 1) {
            return List.of(line);
        }
        List<Component> out = new ArrayList<>(rows.size());
        for (int[] row : rows) {
            net.minecraft.network.chat.MutableComponent part = Component.empty();
            for (var piece : DialogueRewriter.cut(pieces, row[0], row[1])) {
                part.append(Component.literal(piece.text()).withStyle(piece.style()));
            }
            out.add(part);
        }
        return out;
    }

    private static Component speakerLine(String said) {
        return Component.literal(com.wynnchayuan.client.T.s("dialogue.speaker", said)).withStyle(
                net.minecraft.network.chat.Style.EMPTY
                        .withColor(SPEAKER_COLOUR).withBold(true));
    }

    private static final int MIN_BOX_W = 240;

    private static final int MAX_BOX_W = 420;

    private static final java.util.function.Supplier<String> SHIFT_HINT =
            () -> com.wynnchayuan.client.T.s("dialogue.shift_hint");

    private static final int IN_PLACE_MARGIN = 55;

    private static void draw(GuiGraphics graphics, Minecraft mc, List<Component> lines,
                             int centerX, int y, float alpha, boolean withBox) {
        int lineHeight = mc.font.lineHeight + 1;
        int boxW = widthOf(mc, lines);
        int boxH = lines.size() * lineHeight + PADDING * 2;
        int x = centerX - boxW / 2;
        if (withBox) {
            Boxes.draw(graphics, x, y, boxW, boxH, alpha);
        }
        int textY = y + PADDING;
        for (Component line : lines) {
            int lineX = withBox
                    ? x + PADDING
                    : centerX - mc.font.width(line) / 2;
            graphics.drawString(mc.font, line, lineX, textY,
                    Colors.fade(Colors.TEXT, alpha));
            textY += lineHeight;
        }
    }

    private static int widthOf(Minecraft mc, List<Component> lines) {
        int width = 0;
        for (Component line : lines) {
            width = Math.max(width, mc.font.width(line));
        }
        return width + PADDING * 2;
    }

    private static int dialogueWidth(int auto) {
        CollectorConfig cfg = WynnChaYuan.config();
        return cfg.hasOverlaySize(CollectorConfig.Overlay.DIALOGUE)
                ? cfg.overlayW(CollectorConfig.Overlay.DIALOGUE)
                : auto;
    }

    private static int atLeastTall(int auto) {
        CollectorConfig cfg = WynnChaYuan.config();
        return cfg.hasOverlaySize(CollectorConfig.Overlay.DIALOGUE)
                ? Math.max(auto, cfg.overlayH(CollectorConfig.Overlay.DIALOGUE))
                : auto;
    }

    private static void drawSized(GuiGraphics graphics, Minecraft mc,
                                  List<Component> lines, int x, int y,
                                  int boxW, float alpha) {
        int lineHeight = mc.font.lineHeight + 1;
        List<FormattedCharSequence> rows = wrapAll(mc, lines, boxW - PADDING * 2);
        int boxH = atLeastTall(rows.size() * lineHeight + PADDING * 2);
        Boxes.draw(graphics, x, y, boxW, boxH, alpha);
        int textY = y + PADDING;
        for (FormattedCharSequence row : rows) {
            graphics.drawString(mc.font, row, x + PADDING, textY,
                    Colors.fade(Colors.TEXT, alpha));
            textY += lineHeight;
        }
    }

    private static final int CHOICE_GAP = 3;
}
