package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.CurrentQuest;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.translate.FlowedDebug;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.SpaceOffset;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DialogueOverlay {

    static final int BOTTOM_MARGIN = 70;

    static final int PADDING = 4;

    static volatile List<Component> current = List.of();

    static volatile List<List<Component>> choices = List.of();

    static volatile int picked = 0;

    // 選項不在 Wynntils 對話事件裡，只在原始 action bar
    static volatile List<String> rawChoices = List.of();

    static volatile String speaker = null;

    static volatile boolean needsShift = false;

    private static volatile List<String> settledFor = List.of();

    private static volatile List<List<Component>> shown = List.of();

    static volatile long lastUpdate = 0;

    static final TextColor SPEAKER_COLOUR = TextColor.fromRgb(0xFFD966);

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
            FlowedDebug.noteChoices(dialogue.getString());
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
                >= DialogueTyping.SKIP_JUMP;
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
            String template = GlyphSplitter.toTemplate(line);
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
        settledFor = Collections.unmodifiableList(new ArrayList<>(sources));
        settledRaws = Collections.unmodifiableList(seenRaws);
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
                .filter(cp -> !SpaceOffset.isOffset(cp)).forEach(out::appendCodePoint);
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
                || now - lineStart > DialogueTyping.HOLD_MS
                || raws[raws.length - 1].strip().length() > DialogueTyping.HOLD_CHARS) {
            return false;
        }
        StyledText line = DialogueIcons.line(raws[raws.length - 1], store);
        return store.couldStillMatch(GlyphSplitter.toTemplate(line));
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
        LineParts parts = LineParts.of(line);
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
        if (DialogueTyping.learnName(line.getString(), store)) {
            template = LineParts.of(line).template();
        }
        LineParts parts = LineParts.of(line);
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
                                       CurrentQuest.get());
            if (source == null) {
                source = store.nearQuestLine(template,
                                             CurrentQuest.get());
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
                && !DialogueTyping.sentenceEnd(typed);
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
        return canReuse(source, template, raws.get(index), raw) ? source : null;
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
                && DialogueTyping.within(source, template.strip());
    }

    static boolean canReuse(String source, String template, String before, String now) {
        return continues(before, now) && canReuse(source, template)
                && !numberStillGrowing(before, now);
    }

    static boolean numberStillGrowing(String before, String now) {
        return !now.equals(before) && endsInNumber(before);
    }

    private static boolean endsInNumber(String text) {
        int last = text.length() - 1;
        if (last > 0 && (text.charAt(last) == '.' || text.charAt(last) == ',')) {
            last--;
        }
        return last >= 0 && Character.isDigit(text.charAt(last));
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
        DialoguePanelRenderer.render(graphics);
    }
}
