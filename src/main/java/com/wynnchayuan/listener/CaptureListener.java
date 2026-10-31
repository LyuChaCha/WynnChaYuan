package com.wynnchayuan.listener;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.CombatText;
import com.wynnchayuan.capture.ConversationPartner;
import com.wynnchayuan.capture.CurrentQuest;
import com.wynnchayuan.capture.DialogueBuffer;
import com.wynnchayuan.capture.DialogueProbe;
import com.wynnchayuan.capture.DialogueSpeaker;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynnchayuan.capture.QuestGuess;
import com.wynnchayuan.render.DialogueIcons;
import com.wynnchayuan.render.DialogueOverlay;
import com.wynnchayuan.render.LookAtTranslator;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.NameplateProbe;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.ChatMessageEvent;
import com.wynntils.handlers.chat.type.RecipientType;
import com.wynntils.handlers.labels.event.TextDisplayChangedEvent;
import com.wynntils.models.dialogue.event.NpcDialogueEvent;
import com.wynntils.models.npc.label.NpcLabelInfo;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

public final class CaptureListener {

    // Wynntils 的 Finished 只在 requiresShift 轉真時發

    private final DialogueBuffer buffer = new DialogueBuffer();
    private volatile boolean lastHadChoices = false;

    private volatile String lineSpeaker = null;
    private volatile boolean lineHadChoices = false;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDialogueStarted(NpcDialogueEvent.Started event) {
        WynnChaYuan.store().noteEvent("dialogue.Started");
        feed(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDialogueUpdated(NpcDialogueEvent.Updated event) {
        WynnChaYuan.store().noteEvent("dialogue.Updated");
        feed(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDialogueFinished(NpcDialogueEvent.Finished event) {
        WynnChaYuan.store().noteEvent("dialogue.Finished");
        feed(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDialogueEnded(NpcDialogueEvent.Ended event) {
        WynnChaYuan.store().noteEvent("dialogue.Ended");
        feed(event);
    }

    private void feed(NpcDialogueEvent event) {
        String text = event.getDialogueText();
        if (text == null || text.isBlank()) {
            return;
        }
        StyledText styled = StyledText.fromString(text);
        if (GlyphSplitter.isGlyphOnly(styled)) {
            return;
        }
        lastHadChoices = event.hasChoices();
        DialogueProbe.noteHasChoices(lastHadChoices);
        DialogueOverlay.setShiftPrompt(event.requiresShift());
        DialogueOverlay.setCurrent(styled, WynnChaYuan.translations(), lastHadChoices);
        String speaker = DialogueSpeaker.of(styled);
        if (speaker == null) {
            speaker = DialogueSpeaker.currentFor(styled.getString());
        }
        String partner = ConversationPartner.forLine();
        String done = buffer.offer(styled.getString(), GlyphSplitter.toTemplate(
                DialogueIcons.line(text, WynnChaYuan.translations())));
        if (done != null) {
            record(done, lineSpeaker, lineHadChoices);
            lineSpeaker = speaker != null ? speaker : partner;
        } else if (speaker != null) {
            lineSpeaker = speaker;
        } else if (lineSpeaker == null) {
            lineSpeaker = partner;
        }
        lineHadChoices = lastHadChoices;
    }

    public void flushSettled() {
        String done = buffer.flushIfSettled();
        if (done != null) {
            record(done, lineSpeaker, lineHadChoices);
            lineSpeaker = null;
        }
        if (WynnChaYuan.config().collect()) {
            WynnChaYuan.store().settleChat(System.currentTimeMillis());
        }
    }

    private static void auditSpeaker(String template, String speaker) {
        try {
            String known = DialogueSpeaker.cleanName(
                    WynnChaYuan.translations().speakerOf(template));
            if (known == null) {
                return;
            }
            String got = DialogueSpeaker.cleanName(speaker);
            if (got == null) {
                WynnChaYuan.store().noteEvent("speaker.audit.none");
            } else if (got.equalsIgnoreCase(known)) {
                WynnChaYuan.store().noteEvent("speaker.audit.match");
            } else {
                WynnChaYuan.store().noteEvent("speaker.audit.mismatch");
            }
        } catch (Throwable failure) {
            ErrorDebug.note("CaptureListener#auditSpeaker", null, failure);
        }
    }

    private void record(String completed, String nameplate, boolean hadChoices) {
        if (completed == null || !WynnChaYuan.config().collect()) {
            return;
        }
        String speaker = nameplate;
        auditSpeaker(completed, speaker);
        String quest = CurrentQuest.get();
        boolean withQuest = QuestGuess.withQuest(quest, speaker,
                WynnChaYuan.translations().inCast(quest, speaker),
                ConversationPartner.fromBlock());
        auditQuest(completed, quest, withQuest);
        WynnChaYuan.store().record(
                completed, "desc", "quest",
                CurrentQuest.tag(hadChoices ? "dialogue/choices" : "dialogue",
                                 speaker, withQuest));
    }

    private static void auditQuest(String template, String quest, boolean withQuest) {
        try {
            if (quest == null) {
                return;
            }
            String known = WynnChaYuan.translations().questOf(template);
            if (known == null) {
                return;
            }
            String tracked = GlyphSplitter.stripGlyphChars(quest).strip();
            boolean same = tracked.equalsIgnoreCase(known.strip());
            String verdict = withQuest ? (same ? "match" : "mismatch")
                                       : (same ? "missed" : "avoided");
            WynnChaYuan.store().noteEvent("quest.audit." + verdict);
        } catch (Throwable failure) {
            ErrorDebug.note("CaptureListener#auditQuest", null, failure);
        }
    }

    private static final Set<RecipientType> SERVER_MESSAGES =
            EnumSet.of(RecipientType.INFO, RecipientType.GAME_MESSAGE);

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChat(ChatMessageEvent.Match event) {
        WynnChaYuan.store().noteEvent("chat." + event.getRecipientType());
        if (!WynnChaYuan.config().collect()) {
            return;
        }
        if (!SERVER_MESSAGES.contains(event.getRecipientType())) {
            return;
        }
        String template = chatTemplate(event.getMessage(), WynnChaYuan.store()::noteEvent,
                                       WynnChaYuan.translations());
        if (template == null) {
            return;
        }
        WynnChaYuan.store().recordChat(
                template, "chat/" + event.getRecipientType(), System.currentTimeMillis());
    }

    static String chatTemplate(StyledText message, Consumer<String> skipped) {
        return chatTemplate(message, skipped, null);
    }

    static String chatTemplate(StyledText message, Consumer<String> skipped,
                               TranslationStore store) {
        if (message == null || GlyphSplitter.isGlyphOnly(message)) {
            return null;
        }
        if (ChatBlock.isOurs(message)) {
            skipped.accept("chat.skipped.ownOutput");
            return null;
        }
        LineParts framed = LineTranslator.nameFramed(message, store);
        if (framed != null) {
            skipped.accept("chat.framed.playerName");
            return framed.template();
        }
        String template = GlyphSplitter.toTemplate(message);
        if (PlayerDataFilter.carriesPlayerData(template)) {
            skipped.accept("chat.blocked.playerData");
            return null;
        }
        return template;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onLabel(TextDisplayChangedEvent.Text event) {
        if (CombatText.isIndicator(event.getText())) {
            return;
        }
        event.getLabelInfo()
             .filter(NpcLabelInfo.class::isInstance)
             .map(NpcLabelInfo.class::cast)
             .ifPresent(info -> {
                 String name = info.getName();
                 if (name != null && !name.isBlank()) {
                     String tmpl = GlyphSplitter.toTemplate(StyledText.fromString(name));
                     if (!PlayerDataFilter.carriesPlayerData(tmpl)
                             && !PlayerDataFilter.looksPlayerNamed(tmpl)) {
                         WynnChaYuan.store().record(tmpl, "name", "npc", "npc/name");
                     }
                 }
             });

        StyledText full = event.getText();
        LineParts owned = full == null || GlyphSplitter.isGlyphOnly(full)
                ? null : LineTranslator.nameFramed(full, WynnChaYuan.translations());
        if (owned != null) {
            WynnChaYuan.store().record(trimStatusLines(owned.template()), "name", "label",
                                       "label/floating");
        } else if (full != null && !GlyphSplitter.isGlyphOnly(full)) {
            String template = trimStatusLines(GlyphSplitter.toTemplate(full));
            if (!template.isBlank() && GlyphSplitter.hasLetter(template)
                    && !PlayerDataFilter.carriesPlayerData(template)
                    && !PlayerDataFilter.looksPlayerNamed(template)) {
                boolean isNpc = event.getLabelInfo()
                        .filter(NpcLabelInfo.class::isInstance).isPresent();
                WynnChaYuan.store().record(
                        template, "name",
                        isNpc ? "npc" : "label",
                        isNpc ? "npc/nametag" : "label/floating");
            }
        }

        translateNametag(event);
    }

    static String trimStatusLines(String template) {
        if (template == null || template.indexOf('\n') < 0) {
            return template;
        }
        String[] lines = template.split("\n", -1);
        int keep = lines.length;
        while (keep > 1 && !GlyphSplitter.hasWordBesidesStatus(lines[keep - 1])) {
            keep--;
        }
        return keep == lines.length ? template
                : String.join("\n", Arrays.copyOf(lines, keep));
    }

    private void translateNametag(TextDisplayChangedEvent.Text event) {
        CollectorConfig.NametagMode mode = WynnChaYuan.config().nametagMode();
        try {
            StyledText original = event.getText();
            if (original == null || GlyphSplitter.isGlyphOnly(original)) {
                return;
            }
            if (CombatText.isIndicator(original)) {
                return;
            }
            LookAtTranslator.remember(event.getTextDisplay(), original);
            if (!WynnChaYuan.config().showOverlays()
                    || mode == CollectorConfig.NametagMode.OFF) {
                return;
            }
            if (mode == CollectorConfig.NametagMode.LOOK_AT) {
                return;
            }
            Component translated =
                    LineTranslator.translateLabel(original, WynnChaYuan.translations());
            NameplateProbe.check("TextDisplay", original, translated, WynnChaYuan.translations());
            if (translated != null) {
                event.setText(StyledText.fromComponent(translated));
            }
        } catch (Throwable t) {
            ErrorDebug.note("CaptureListener#translateNametag", null, t);
            WynnChaYuan.store().noteEvent("render.nametagError");
        }
    }
}
