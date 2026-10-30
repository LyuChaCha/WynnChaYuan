package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;

final class WynntilsBossBar {

    private WynntilsBossBar() {}

    static net.minecraft.network.chat.Component bossBar(
            net.minecraft.network.chat.Component name) {
        try {
            return bossBar(name, WynnChaYuan.config(), WynnChaYuan.translations());
        } catch (Throwable t) {
            ErrorDebug.note("WynntilsBossBar#bossBar", null, t);
            return name;
        }
    }

    static net.minecraft.network.chat.Component bossBar(
            net.minecraft.network.chat.Component name, CollectorConfig config,
            TranslationStore store) {
        if (name == null || store == null || config == null || !config.translateBossBar()) {
            return name;
        }
        if (barAge != store.generation() || BARS.size() > 256) {
            BARS.clear();
            barAge = store.generation();
        }
        net.minecraft.network.chat.Component hit = BARS.get(name);
        if (hit == null) {
            StyledText text = StyledText.fromComponent(name);
            hit = bossBarPlain(name, store);
            if (hit == null) {
                StyledText shown = WynntilsText.line(text, store);
                hit = shown == text ? name : shown.getComponent();
            }
            net.minecraft.network.chat.Component named = bossBarName(hit, store);
            if (named != null) {
                hit = named;
            }
            net.minecraft.network.chat.Component worded = bossBarWords(hit, store);
            if (worded != null) {
                hit = worded;
            }
            BARS.put(name, hit);
            var captured = WynnChaYuan.store();
            if (captured != null) {
                captured.noteEvent(hit == name ? "bossbar.noMatch" : "bossbar.shown");
            }
            if (hit == name && config.collect()) {
                WynntilsNames.collectName(text, "label/bossbar");
            }
        }
        return hit;
    }

    static net.minecraft.network.chat.Component bossBarPlain(
            net.minecraft.network.chat.Component bar, TranslationStore store) {
        String raw = bar.getString();
        String key = raw.strip();
        if (key.isEmpty() || !key.equals(com.wynnchayuan.capture.GlyphSplitter.stripGlyphChars(key))) {
            return null;
        }
        String dst = store.lookup(key);
        if (dst == null || dst.isEmpty() || dst.contains("{")) {
            return null;
        }
        int same = 0;
        while (same < key.length() && same < dst.length()
                && key.charAt(key.length() - 1 - same) == dst.charAt(dst.length() - 1 - same)) {
            same++;
        }
        if (same == 0) {
            return null;
        }
        int start = raw.indexOf(key);
        int keep = start + key.length() - same;
        String head = raw.substring(0, start) + dst.substring(0, dst.length() - same);
        net.minecraft.network.chat.MutableComponent out =
                net.minecraft.network.chat.Component.empty();
        int[] at = {0};
        boolean[] headDone = {false};
        bar.visit((style, text) -> {
            int from = at[0];
            int to = from + text.length();
            at[0] = to;
            if (!headDone[0] && !text.isEmpty()) {
                out.append(net.minecraft.network.chat.Component.literal(head).withStyle(style));
                headDone[0] = true;
            }
            if (to > keep && !text.isEmpty()) {
                out.append(net.minecraft.network.chat.Component.literal(
                        text.substring(Math.max(0, keep - from))).withStyle(style));
            }
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return headDone[0] ? out : null;
    }

    static net.minecraft.network.chat.Component bossBarName(
            net.minecraft.network.chat.Component bar, TranslationStore store) {
        String plain = bar.getString();
        int cut = plain.indexOf(" - ");
        if (cut <= 0) {
            return null;
        }
        String head = plain.substring(0, cut).strip();
        if (head.isEmpty()) {
            return null;
        }
        String dst = store.lookup(head);
        if (dst == null) {
            String plate = store.lookup(head + " {#}{#}");
            if (plate != null && plate.endsWith("{#}{#}")) {
                dst = plate.substring(0, plate.length() - "{#}{#}".length()).strip();
            }
        }
        if (dst == null || dst.isEmpty() || dst.contains("{")) {
            return null;
        }
        String translated = dst;
        net.minecraft.network.chat.MutableComponent out =
                net.minecraft.network.chat.Component.empty();
        boolean[] done = {false};
        bar.visit((style, text) -> {
            int at = done[0] ? -1 : text.indexOf(head);
            if (at >= 0) {
                out.append(net.minecraft.network.chat.Component.literal(
                        text.substring(0, at) + translated
                                + text.substring(at + head.length())).withStyle(style));
                done[0] = true;
            } else if (!text.isEmpty()) {
                out.append(net.minecraft.network.chat.Component.literal(text).withStyle(style));
            }
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return done[0] ? out : null;
    }

    static net.minecraft.network.chat.Component bossBarWords(
            net.minecraft.network.chat.Component bar, TranslationStore store) {
        net.minecraft.network.chat.MutableComponent out =
                net.minecraft.network.chat.Component.empty();
        boolean[] changed = {false};
        bar.visit((style, text) -> {
            if (text.isEmpty()) {
                return java.util.Optional.empty();
            }
            java.util.regex.Matcher m = WORD.matcher(text);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                String dst = store.scopedLookup("bossbar", m.group());
                m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(
                        dst != null ? dst : m.group()));
                changed[0] |= dst != null;
            }
            m.appendTail(sb);
            out.append(net.minecraft.network.chat.Component.literal(sb.toString())
                    .withStyle(style));
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return changed[0] ? out : null;
    }

    private static final java.util.regex.Pattern WORD =
            java.util.regex.Pattern.compile("[A-Za-z]+");

    private static final java.util.Map<net.minecraft.network.chat.Component,
            net.minecraft.network.chat.Component> BARS = new java.util.HashMap<>();

    private static int barAge = -1;
}
