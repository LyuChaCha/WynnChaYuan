package com.wynnchayuan.listener;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.ChoiceScroll;
import com.wynnchayuan.capture.CurrentQuest;
import com.wynnchayuan.capture.DialogueChoices;
import com.wynnchayuan.capture.DialogueProbe;
import com.wynnchayuan.capture.DialogueProgress;
import com.wynnchayuan.capture.DialogueSpeaker;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynnchayuan.render.DialogueIcons;
import com.wynnchayuan.render.DialogueOverlay;
import com.wynnchayuan.render.DialogueRewriter;
import com.wynnchayuan.render.DialogueStartLog;
import com.wynnchayuan.render.PairedFont;
import com.wynnchayuan.render.SelectorRealign;
import com.wynnchayuan.render.ThirdPartyLiterals;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.SpaceOffset;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import com.wynntils.handlers.actionbar.ActionBarSegment;
import com.wynntils.handlers.actionbar.event.ActionBarRenderEvent;
import com.wynntils.mc.event.SystemMessageEvent;
import com.wynntils.models.dialogue.actionbar.segments.DialogueSegment;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ActionBarListener {

    private static final int GRACE = 3;

    private static final int CONVERSATION_GAP = 20;

    private int missing = 0;

    private volatile boolean inDialogue = false;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGameInfo(SystemMessageEvent.GameInfoReceivedEvent event) {
        DialogueSpeaker.noteRaw(event.getMessage());
        DialogueProgress.noteRaw(event.getMessage());
        DialogueStartLog.frame(event.getMessage());
        DialogueProbe.record(event.getMessage());
        DialogueProbe.recordChoices(event.getMessage());
        List<String> picks = DialogueChoices.of(event.getMessage());
        DialogueOverlay.noteChoices(picks,
                DialogueChoices.selected(event.getMessage()));
        if (picks.isEmpty()) {
            ChoiceScroll.reset();
        } else {
            for (int i = 0; i < picks.size(); i++) {
                String stitched = ChoiceScroll.feed(i, picks.get(i));
                if (stitched != null) {
                    noteFull(i, stitched);
                }
            }
        }
        collect(picks);
    }

    private List<String> lastPicks = List.of();
    private List<String> settling = List.of();
    private long settlingSince;

    private static final long SETTLE_MS = 700;

    private void collect(List<String> picks) {
        if (picks.isEmpty() || !WynnChaYuan.config().collect()) {
            settling = List.of();
            return;
        }
        if (!picks.equals(settling)) {
            settling = picks;
            settlingSince = System.currentTimeMillis();
            return;
        }
        if (System.currentTimeMillis() - settlingSince < SETTLE_MS
                || picks.equals(lastPicks)) {
            return;
        }
        lastPicks = picks;
        for (int i = 0; i < picks.size(); i++) {
            String pick = picks.get(i);
            if (PlayerDataFilter.carriesPlayerData(pick)) {
                WynnChaYuan.store().noteEvent("dialogue.blocked.playerData");
                continue;
            }
            if (looksClipped(pick)) {
                WynnChaYuan.store().noteEvent("dialogue.blocked.clipped");
                continue;
            }
            if (!pick.equals(ChoiceScroll.first(i))) {
                WynnChaYuan.store().noteEvent("dialogue.blocked.midScroll");
                continue;
            }
            record(pick, ChoiceScroll.full(i));
        }
    }

    private void noteFull(int row, String stitched) {
        if (!WynnChaYuan.config().collect()) {
            return;
        }
        String firstWindow = ChoiceScroll.first(row);
        if (firstWindow == null || looksClipped(firstWindow)) {
            return;
        }
        record(firstWindow, stitched);
    }

    private void record(String pick, String full) {
        if (PlayerDataFilter.carriesPlayerData(pick)) {
            WynnChaYuan.store().noteEvent("dialogue.blocked.playerData");
            return;
        }
        if (full != null
                && PlayerDataFilter.carriesPlayerData(full)) {
            WynnChaYuan.store().noteEvent("dialogue.blocked.playerData");
            full = null;
        }
        StyledText line = StyledText.fromString(pick);
        String template = full == null ? null
                : GlyphSplitter.toTemplate(StyledText.fromString(full));
        WynnChaYuan.store().record(
                GlyphSplitter.toTemplate(line), "desc", "quest",
                CurrentQuest.tag("dialogue/choice", null), template);
    }

    static boolean looksClipped(String pick) {
        if (pick == null || pick.isEmpty()) {
            return false;
        }
        char first = pick.charAt(0);
        if (first >= 'a' && first <= 'z') {
            return true;
        }
        return first == '\'' || first == ',' || first == '.' || first == ';'
                || first == ':' || first == '!' || first == '?';
    }

    // 優先權最低：Wynntils 要先讀原始英文，否則自動翻頁會壞
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGameInfoIcons(SystemMessageEvent.GameInfoReceivedEvent event) {
        try {
            DialogueIcons.note(event.getMessage());
        } catch (Throwable t) {
            ErrorDebug.note("ActionBarListener#onGameInfoIcons", null, t);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onGameInfoRewrite(SystemMessageEvent.GameInfoReceivedEvent event) {
        if (WynnChaYuan.config().dialogueMode() != CollectorConfig.DialogueMode.REPLACE
                && WynnChaYuan.config().choiceMode() != CollectorConfig.DialogueMode.REPLACE) {
            return;
        }
        try {
            var swapped = DialogueRewriter.rewrite(
                    event.getMessage(), WynnChaYuan.translations());
            if (swapped != null) {
                event.setMessage(swapped);
                DialogueProbe.after(swapped);
            } else {
                DialogueProbe.miss(event.getMessage());
                DialogueProbe.plain(event.getMessage());
            }
        } catch (Throwable t) {
            ErrorDebug.note("ActionBarListener#onGameInfoRewrite", null, t);
            WynnChaYuan.store().noteEvent("dialogue.rewriteError");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onActionBarRender(ActionBarRenderEvent event) {
        boolean present = false;
        for (ActionBarSegment segment : event.getSegments()) {
            if (segment instanceof DialogueSegment) {
                present = true;
                break;
            }
        }

        inDialogue = present;
        if (!present) {
            if (missing + 1 >= CONVERSATION_GAP) {
                DialogueProgress.endConversation();
            }
            if (++missing >= GRACE && DialogueOverlay.hasContent()) {
                DialogueOverlay.clear();
            }
            return;
        }
        missing = 0;
        DialogueOverlay.stillOnScreen();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlainActionBar(SystemMessageEvent.GameInfoReceivedEvent event) {
        if (inDialogue) {
            return;
        }
        try {
            Component swapped = plainSwap(event.getMessage());
            if (swapped != null) {
                event.setMessage(swapped);
            }
        } catch (Throwable t) {
            ErrorDebug.note("ActionBarListener#onPlainActionBar", null, t);
            WynnChaYuan.store().noteEvent("actionbar.plainError");
        }
    }

    private static Component plainSwap(Component original) {
        if (original == null) {
            return null;
        }
        StyledText line = StyledText.fromComponent(original);
        if (line.isEmpty()
                || GlyphSplitter.isGlyphOnly(line)) {
            return null;
        }
        if (ThirdPartyLiterals.reserved(line.getStringWithoutFormatting())) {
            return null;
        }
        String template =
                GlyphSplitter.toTemplate(line);
        if (PlayerDataFilter.carriesPlayerData(template)) {
            return null;
        }
        Component hit;
        if (hasSelectorSlot(line)) {
            hit = columnSwap(line, WynnChaYuan.translations());
        } else {
            hit = LineTranslator.translate(line, WynnChaYuan.translations());
            if (hit == null) {
                hit = columnSwap(line, WynnChaYuan.translations());
            }
        }
        SelectorRealign.Result fixed = SelectorRealign.apply(hit != null ? hit : original);
        Component out = hit != null || fixed.changed() ? fixed.out() : null;
        DialogueProbe.columns(original, out, fixed.report());
        if (hit != null) {
            leftovers(line, hit);
            return out;
        }
        if (WynnChaYuan.config().collect()) {
            WynnChaYuan.store().record(template, "name", "actionbar", "actionbar");
        }
        return out;
    }

    private static final Pattern COLUMN_GAP = Pattern.compile("(?<=\\S) {2,}(?=\\S)");

    static Component columnSwap(StyledText line, TranslationStore store) {
        MutableComponent out = Component.empty();
        boolean any = false;
        for (StyledTextPart part : line) {
            String raw = part.getString(null, StyleType.NONE);
            if (raw.isEmpty()) {
                continue;
            }
            PartStyle ps = part.getPartStyle();
            Style style = ps == null ? Style.EMPTY : ps.getStyle();
            FontDescription ours = PairedFont.forStyle(style);
            if (ours != null) {
                PairedFont.warm(ours);
            }
            if (ours != null && PairedFont.MARKER.equals(raw)) {
                // 名字前的 U+0001 是 shader 定位標記，要用我們的字型畫
                out.append(literal(raw, style.withFont(ours)));
                continue;
            }
            if (GlyphSplitter.isGlyphPart(part)
                    || raw.isBlank()) {
                out.append(literal(raw, style));
                continue;
            }
            if (!COLUMN_GAP.matcher(raw).find()) {
                if (ours != null && GlyphSplitter.hasLetter(raw)) {
                    any |= column(out, raw, style, store);
                } else {
                    out.append(literal(raw, style));
                }
                continue;
            }
            MutableComponent rebuilt = Component.empty();
            Matcher m = COLUMN_GAP.matcher(raw);
            boolean swapped = false;
            int at = 0;
            while (m.find()) {
                swapped |= column(rebuilt, raw.substring(at, m.start()), style, store);
                rebuilt.append(literal(m.group(), style));
                at = m.end();
            }
            swapped |= column(rebuilt, raw.substring(at), style, store);
            if (!swapped) {
                out.append(literal(raw, style));
                continue;
            }
            any = true;
            out.append(rebuilt);
        }
        return any ? out : null;
    }

    private static Component archetypeName(String chunk, Style style, TranslationStore store) {
        String slot = PairedFont.slot(fontOf(style));
        if (slot == null || !slot.startsWith("selector_name_")) {
            return null;
        }
        String dst = store.scopedLookup("archetype", chunk);
        if (dst == null || dst.isBlank()) {
            return null;
        }
        return literal(dst, style.withFont(
                FontDescription.DEFAULT));
    }

    private static boolean hasSelectorSlot(StyledText line) {
        for (StyledTextPart part : line) {
            PartStyle ps = part.getPartStyle();
            if (ps != null && PairedFont.isHudScreen(ps.getStyle())) {
                return true;
            }
        }
        return false;
    }

    private static boolean column(MutableComponent out, String chunk, Style style,
                                  TranslationStore store) {
        Component done = archetypeName(chunk, style, store);
        if (done == null) {
            done = LineTranslator.translateChunk(chunk, style, store);
        }
        if (done == null) {
            out.append(literal(chunk, style));
            collect(chunk);
            return false;
        }
        if (!DialogueRewriter.renderable(done.getString())) {
            out.append(literal(chunk, style));
            return false;
        }
        Component swapped = refont(done, style);
        if (chunk.startsWith(PairedFont.MARKER)
                && !swapped.getString().startsWith(PairedFont.MARKER)) {
            FontDescription ours = PairedFont.forStyle(style);
            if (ours != null) {
                swapped = Component.empty().append(literal(PairedFont.MARKER, style.withFont(ours)))
                        .append(swapped);
            }
        }
        out.append(swapped);
        int gap = owed(chunk, style, swapped);
        if (gap != 0) {
            String fill = SpaceOffset.encode(gap);
            if (!fill.isEmpty()) {
                out.append(Component.literal(fill).setStyle(SpaceOffset.styleFor(style)));
            }
        }
        return true;
    }

    private static void leftovers(StyledText line, Component hit) {
        try {
            if (!WynnChaYuan.config().collect() || hit == null) {
                return;
            }
            String after = hit.getString();
            for (StyledTextPart part : line) {
                if (GlyphSplitter.isGlyphPart(part)) {
                    continue;
                }
                String raw = part.getString(
                        null, StyleType.NONE);
                if (raw == null || raw.strip().length() < MIN_LEFTOVER
                        || !after.contains(raw)) {
                    continue;
                }
                collect(raw);
            }
        } catch (Throwable t) {
            ErrorDebug.note("ActionBarListener#leftovers", null, t);
        }
    }

    private static final int MIN_LEFTOVER = 6;

    private static void collect(String chunk) {
        try {
            if (!WynnChaYuan.config().collect() || chunk == null || chunk.isBlank()) {
                return;
            }
            String template = GlyphSplitter.toTemplate(StyledText.fromString(chunk.strip()));
            if (template.isBlank()
                    || !GlyphSplitter.hasLetter(template)
                    || PlayerDataFilter.carriesPlayerData(template)
                    || PlayerDataFilter.looksPlayerNamed(template)) {
                return;
            }
            WynnChaYuan.store().record(template, "name", "actionbar", "actionbar/column");
        } catch (Throwable t) {
            ErrorDebug.note("ActionBarListener#collect", null, t);
        }
    }

    private static int owed(String chunk,
                            Style style, Component swapped) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.font == null) {
                return 0;
            }
            int was = mc.font.width(literal(chunk, style));
            int now = mc.font.width(swapped);
            if (was <= 0 || now <= 0) {
                return 0;
            }
            return was - now;
        } catch (Throwable t) {
            ErrorDebug.note("ActionBarListener#owed", null, t);
            return 0;
        }
    }

    // bottom_middle 的 ascent 是 -48，shift_y = 7 − ascent = 55
    private static Component refont(Component translated, Style original) {
        String name = pairedFont(fontOf(original));
        if (name == null) {
            return translated;
        }
        String lang = WynnChaYuan.language();
        if (!fontShipped(lang, name)) {
            return translated;
        }
        FontDescription ours = new FontDescription.Resource(Identifier.fromNamespaceAndPath(
                                WynnChaYuan.MOD_ID, "actionbar/" + lang + "/" + name));
        MutableComponent out = Component.empty();
        translated.visit((style, text) -> {
            Style use = GlyphSplitter.isCustomFont(style.getFont())
                            ? style : style.withFont(ours);
            out.append(Component.literal(text).setStyle(use));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static String pairedFont(String font) {
        return PairedFont.slot(font);
    }

    private static boolean fontShipped(String lang, String name) {
        return PairedFont.shipped(lang, name);
    }

    private static String fontOf(Style style) {
        return style == null || style.getFont() == null
                ? "" : String.valueOf(style.getFont());
    }

    private static Component literal(String text, Style style) {
        return Component.literal(text).setStyle(style);
    }
}
