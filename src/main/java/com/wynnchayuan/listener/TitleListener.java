package com.wynnchayuan.listener;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynnchayuan.translate.LineTranslator;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.SubtitleSetTextEvent;
import com.wynntils.mc.event.TitleSetTextEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

// Wynntils title 事件沒有 setter，取消後重送會再觸發
public final class TitleListener {

    private static boolean sending = false;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTitle(TitleSetTextEvent event) {
        Component hit = swap(event.getComponent());
        if (hit == null) {
            return;
        }
        event.setCanceled(true);
        send(() -> Minecraft.getInstance().gui.setTitle(hit));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onSubtitle(SubtitleSetTextEvent event) {
        Component hit = swap(event.getComponent());
        if (hit == null) {
            return;
        }
        event.setCanceled(true);
        send(() -> Minecraft.getInstance().gui.setSubtitle(hit));
    }

    private static void send(Runnable action) {
        sending = true;
        try {
            action.run();
        } finally {
            sending = false;
        }
    }

    private static Component swap(Component original) {
        if (sending || original == null || !WynnChaYuan.config().translateTitles()) {
            return null;
        }
        StyledText line = StyledText.fromComponent(original);
        if (line.isEmpty() || GlyphSplitter.isGlyphOnly(line)) {
            return null;
        }
        if (com.wynnchayuan.render.ThirdPartyLiterals.reserved(
                line.getStringWithoutFormatting())) {
            return null;
        }
        String template = GlyphSplitter.toTemplate(line);
        com.wynnchayuan.capture.LineParts framed =
                LineTranslator.nameFramed(line, WynnChaYuan.translations());
        if (framed != null) {
            if (WynnChaYuan.config().collect()) {
                WynnChaYuan.store().record(framed.template(), "desc", "title", "title");
            }
            return LineTranslator.translateFramed(line, framed, WynnChaYuan.translations());
        }
        if (PlayerDataFilter.carriesPlayerData(template)) {
            return null;
        }
        if (WynnChaYuan.config().collect()) {
            WynnChaYuan.store().record(template, "desc", "title", "title");
        }
        return LineTranslator.translate(line, WynnChaYuan.translations());
    }
}
