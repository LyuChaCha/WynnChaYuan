package com.wynnchayuan.listener;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.DialogueProbe;
import com.wynnchayuan.render.DialogueOverlay;
import com.wynnchayuan.render.DialogueRewriter;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynntils.handlers.actionbar.ActionBarSegment;
import com.wynntils.handlers.actionbar.event.ActionBarRenderEvent;
import com.wynntils.mc.event.SystemMessageEvent;
import com.wynntils.models.dialogue.actionbar.segments.DialogueSegment;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

public final class ActionBarListener {

    private static final int GRACE = 3;

    private static final int CONVERSATION_GAP = 20;

    private int missing = 0;

    private volatile boolean inDialogue = false;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGameInfo(SystemMessageEvent.GameInfoReceivedEvent event) {
        com.wynnchayuan.capture.DialogueSpeaker.noteRaw(event.getMessage());
        com.wynnchayuan.capture.DialogueProgress.noteRaw(event.getMessage());
        com.wynnchayuan.render.DialogueStartLog.frame(event.getMessage());
        DialogueProbe.record(event.getMessage());
        DialogueProbe.recordChoices(event.getMessage());
        java.util.List<String> picks =
                com.wynnchayuan.capture.DialogueChoices.of(event.getMessage());
        DialogueOverlay.noteChoices(picks,
                com.wynnchayuan.capture.DialogueChoices.selected(event.getMessage()));
        if (picks.isEmpty()) {
            com.wynnchayuan.capture.ChoiceScroll.reset();
        } else {
            for (int i = 0; i < picks.size(); i++) {
                String stitched = com.wynnchayuan.capture.ChoiceScroll.feed(i, picks.get(i));
                if (stitched != null) {
                    noteFull(i, stitched);
                }
            }
        }
        collect(picks);
    }

    private java.util.List<String> lastPicks = java.util.List.of();
    private java.util.List<String> settling = java.util.List.of();
    private long settlingSince;

    private static final long SETTLE_MS = 700;

    private void collect(java.util.List<String> picks) {
        if (picks.isEmpty() || !WynnChaYuan.config().collect()) {
            settling = java.util.List.of();
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
            if (com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(pick)) {
                WynnChaYuan.store().noteEvent("dialogue.blocked.playerData");
                continue;
            }
            if (looksClipped(pick)) {
                WynnChaYuan.store().noteEvent("dialogue.blocked.clipped");
                continue;
            }
            if (!pick.equals(com.wynnchayuan.capture.ChoiceScroll.first(i))) {
                WynnChaYuan.store().noteEvent("dialogue.blocked.midScroll");
                continue;
            }
            record(pick, com.wynnchayuan.capture.ChoiceScroll.full(i));
        }
    }

    private void noteFull(int row, String stitched) {
        if (!WynnChaYuan.config().collect()) {
            return;
        }
        String firstWindow = com.wynnchayuan.capture.ChoiceScroll.first(row);
        if (firstWindow == null || looksClipped(firstWindow)) {
            return;
        }
        record(firstWindow, stitched);
    }

    private void record(String pick, String full) {
        if (com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(pick)) {
            WynnChaYuan.store().noteEvent("dialogue.blocked.playerData");
            return;
        }
        if (full != null
                && com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(full)) {
            WynnChaYuan.store().noteEvent("dialogue.blocked.playerData");
            full = null;
        }
        com.wynntils.core.text.StyledText line =
                com.wynntils.core.text.StyledText.fromString(pick);
        String template = full == null ? null
                : com.wynnchayuan.capture.GlyphSplitter.toTemplate(
                        com.wynntils.core.text.StyledText.fromString(full));
        WynnChaYuan.store().record(
                com.wynnchayuan.capture.GlyphSplitter.toTemplate(line),
                "desc", "quest",
                com.wynnchayuan.capture.CurrentQuest.tag("dialogue/choice", null),
                template);
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
            com.wynnchayuan.render.DialogueIcons.note(event.getMessage());
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
                com.wynnchayuan.capture.DialogueProgress.endConversation();
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
            net.minecraft.network.chat.Component swapped =
                    plainSwap(event.getMessage());
            if (swapped != null) {
                event.setMessage(swapped);
            }
        } catch (Throwable t) {
            ErrorDebug.note("ActionBarListener#onPlainActionBar", null, t);
            WynnChaYuan.store().noteEvent("actionbar.plainError");
        }
    }

    private static net.minecraft.network.chat.Component plainSwap(
            net.minecraft.network.chat.Component original) {
        if (original == null) {
            return null;
        }
        com.wynntils.core.text.StyledText line =
                com.wynntils.core.text.StyledText.fromComponent(original);
        if (line.isEmpty()
                || com.wynnchayuan.capture.GlyphSplitter.isGlyphOnly(line)) {
            return null;
        }
        if (com.wynnchayuan.render.ThirdPartyLiterals.reserved(
                line.getStringWithoutFormatting())) {
            return null;
        }
        String template =
                com.wynnchayuan.capture.GlyphSplitter.toTemplate(line);
        if (com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(template)) {
            return null;
        }
        net.minecraft.network.chat.Component hit;
        if (hasSelectorSlot(line)) {
            hit = columnSwap(line, WynnChaYuan.translations());
        } else {
            hit = com.wynnchayuan.translate.LineTranslator.translate(
                    line, WynnChaYuan.translations());
            if (hit == null) {
                hit = columnSwap(line, WynnChaYuan.translations());
            }
        }
        com.wynnchayuan.render.SelectorRealign.Result fixed =
                com.wynnchayuan.render.SelectorRealign.apply(hit != null ? hit : original);
        net.minecraft.network.chat.Component out =
                hit != null || fixed.changed() ? fixed.out() : null;
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

    private static final java.util.regex.Pattern COLUMN_GAP =
            java.util.regex.Pattern.compile("(?<=\\S) {2,}(?=\\S)");

    static net.minecraft.network.chat.Component columnSwap(
            com.wynntils.core.text.StyledText line,
            com.wynnchayuan.translate.TranslationStore store) {
        net.minecraft.network.chat.MutableComponent out =
                net.minecraft.network.chat.Component.empty();
        boolean any = false;
        for (com.wynntils.core.text.StyledTextPart part : line) {
            String raw = part.getString(null, com.wynntils.core.text.type.StyleType.NONE);
            if (raw.isEmpty()) {
                continue;
            }
            com.wynntils.core.text.PartStyle ps = part.getPartStyle();
            net.minecraft.network.chat.Style style =
                    ps == null ? net.minecraft.network.chat.Style.EMPTY : ps.getStyle();
            net.minecraft.network.chat.FontDescription ours =
                    com.wynnchayuan.render.PairedFont.forStyle(style);
            if (ours != null) {
                com.wynnchayuan.render.PairedFont.warm(ours);
            }
            if (ours != null && com.wynnchayuan.render.PairedFont.MARKER.equals(raw)) {
                // 名字前的 U+0001 是 shader 定位標記，要用我們的字型畫
                out.append(literal(raw, style.withFont(ours)));
                continue;
            }
            if (com.wynnchayuan.capture.GlyphSplitter.isGlyphPart(part)
                    || raw.isBlank()) {
                out.append(literal(raw, style));
                continue;
            }
            if (!COLUMN_GAP.matcher(raw).find()) {
                if (ours != null && com.wynnchayuan.capture.GlyphSplitter.hasLetter(raw)) {
                    any |= column(out, raw, style, store);
                } else {
                    out.append(literal(raw, style));
                }
                continue;
            }
            net.minecraft.network.chat.MutableComponent rebuilt =
                    net.minecraft.network.chat.Component.empty();
            java.util.regex.Matcher m = COLUMN_GAP.matcher(raw);
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

    private static net.minecraft.network.chat.Component archetypeName(
            String chunk,
            net.minecraft.network.chat.Style style,
            com.wynnchayuan.translate.TranslationStore store) {
        String slot = com.wynnchayuan.render.PairedFont.slot(fontOf(style));
        if (slot == null || !slot.startsWith("selector_name_")) {
            return null;
        }
        String dst = store.scopedLookup("archetype", chunk);
        if (dst == null || dst.isBlank()) {
            return null;
        }
        return literal(dst, style.withFont(
                net.minecraft.network.chat.FontDescription.DEFAULT));
    }

    private static boolean hasSelectorSlot(com.wynntils.core.text.StyledText line) {
        for (com.wynntils.core.text.StyledTextPart part : line) {
            com.wynntils.core.text.PartStyle ps = part.getPartStyle();
            if (ps != null && com.wynnchayuan.render.PairedFont.isHudScreen(ps.getStyle())) {
                return true;
            }
        }
        return false;
    }

    private static boolean column(net.minecraft.network.chat.MutableComponent out,
                                  String chunk,
                                  net.minecraft.network.chat.Style style,
                                  com.wynnchayuan.translate.TranslationStore store) {
        net.minecraft.network.chat.Component done = archetypeName(chunk, style, store);
        if (done == null) {
            done = com.wynnchayuan.translate.LineTranslator.translateChunk(
                    chunk, style, store);
        }
        if (done == null) {
            out.append(literal(chunk, style));
            collect(chunk);
            return false;
        }
        if (!com.wynnchayuan.render.DialogueRewriter.renderable(done.getString())) {
            out.append(literal(chunk, style));
            return false;
        }
        net.minecraft.network.chat.Component swapped = refont(done, style);
        if (chunk.startsWith(com.wynnchayuan.render.PairedFont.MARKER)
                && !swapped.getString().startsWith(com.wynnchayuan.render.PairedFont.MARKER)) {
            net.minecraft.network.chat.FontDescription ours =
                    com.wynnchayuan.render.PairedFont.forStyle(style);
            if (ours != null) {
                swapped = net.minecraft.network.chat.Component.empty()
                        .append(literal(com.wynnchayuan.render.PairedFont.MARKER,
                                        style.withFont(ours)))
                        .append(swapped);
            }
        }
        out.append(swapped);
        int gap = owed(chunk, style, swapped);
        if (gap != 0) {
            String fill = com.wynnchayuan.translate.SpaceOffset.encode(gap);
            if (!fill.isEmpty()) {
                out.append(net.minecraft.network.chat.Component.literal(fill)
                        .setStyle(com.wynnchayuan.translate.SpaceOffset.styleFor(style)));
            }
        }
        return true;
    }

    private static void leftovers(com.wynntils.core.text.StyledText line,
                                  net.minecraft.network.chat.Component hit) {
        try {
            if (!WynnChaYuan.config().collect() || hit == null) {
                return;
            }
            String after = hit.getString();
            for (com.wynntils.core.text.StyledTextPart part : line) {
                if (com.wynnchayuan.capture.GlyphSplitter.isGlyphPart(part)) {
                    continue;
                }
                String raw = part.getString(
                        null, com.wynntils.core.text.type.StyleType.NONE);
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
            String template = com.wynnchayuan.capture.GlyphSplitter.toTemplate(
                    com.wynntils.core.text.StyledText.fromString(chunk.strip()));
            if (template.isBlank()
                    || !com.wynnchayuan.capture.GlyphSplitter.hasLetter(template)
                    || com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(template)
                    || com.wynnchayuan.capture.PlayerDataFilter.looksPlayerNamed(template)) {
                return;
            }
            WynnChaYuan.store().record(template, "name", "actionbar", "actionbar/column");
        } catch (Throwable t) {
            ErrorDebug.note("ActionBarListener#collect", null, t);
        }
    }

    private static int owed(String chunk,
                            net.minecraft.network.chat.Style style,
                            net.minecraft.network.chat.Component swapped) {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
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
    private static net.minecraft.network.chat.Component refont(
            net.minecraft.network.chat.Component translated,
            net.minecraft.network.chat.Style original) {
        String name = pairedFont(fontOf(original));
        if (name == null) {
            return translated;
        }
        String lang = WynnChaYuan.language();
        if (!fontShipped(lang, name)) {
            return translated;
        }
        net.minecraft.network.chat.FontDescription ours =
                new net.minecraft.network.chat.FontDescription.Resource(
                        net.minecraft.resources.Identifier.fromNamespaceAndPath(
                                WynnChaYuan.MOD_ID, "actionbar/" + lang + "/" + name));
        net.minecraft.network.chat.MutableComponent out =
                net.minecraft.network.chat.Component.empty();
        translated.visit((style, text) -> {
            net.minecraft.network.chat.Style use =
                    com.wynnchayuan.capture.GlyphSplitter.isCustomFont(style.getFont())
                            ? style : style.withFont(ours);
            out.append(net.minecraft.network.chat.Component.literal(text).setStyle(use));
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return out;
    }

    private static String pairedFont(String font) {
        return com.wynnchayuan.render.PairedFont.slot(font);
    }

    private static boolean fontShipped(String lang, String name) {
        return com.wynnchayuan.render.PairedFont.shipped(lang, name);
    }

    private static String fontOf(net.minecraft.network.chat.Style style) {
        return style == null || style.getFont() == null
                ? "" : String.valueOf(style.getFont());
    }

    private static net.minecraft.network.chat.Component literal(
            String text, net.minecraft.network.chat.Style style) {
        return net.minecraft.network.chat.Component.literal(text).setStyle(style);
    }
}
