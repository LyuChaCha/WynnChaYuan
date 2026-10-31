package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class WynntilsBossBar {

    private WynntilsBossBar() {}

    static Component bossBar(Component name) {
        try {
            return bossBar(name, WynnChaYuan.config(), WynnChaYuan.translations());
        } catch (Throwable t) {
            ErrorDebug.note("WynntilsBossBar#bossBar", null, t);
            return name;
        }
    }

    static Component bossBar(Component name, ModConfig config, TranslationStore store) {
        if (name == null || store == null || config == null || !config.translateBossBar()) {
            return name;
        }
        if (barAge != store.cacheAge() || BARS.size() > 256) {
            BARS.clear();
            barAge = store.cacheAge();
        }
        Component hit = BARS.get(name);
        if (hit == null) {
            StyledText text = StyledText.fromComponent(name);
            hit = bossBarPlain(name, store);
            if (hit == null) {
                StyledText shown = WynntilsText.line(text, store);
                hit = shown == text ? name : shown.getComponent();
            }
            Component named = bossBarName(hit, store);
            if (named != null) {
                hit = named;
            }
            Component worded = bossBarWords(hit, store);
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

    static Component bossBarPlain(Component bar, TranslationStore store) {
        String raw = bar.getString();
        String key = raw.strip();
        if (key.isEmpty() || !key.equals(GlyphSplitter.stripGlyphChars(key))) {
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
        MutableComponent out = Component.empty();
        int[] at = {0};
        boolean[] headDone = {false};
        bar.visit((style, text) -> {
            int from = at[0];
            int to = from + text.length();
            at[0] = to;
            if (!headDone[0] && !text.isEmpty()) {
                out.append(Component.literal(head).withStyle(style));
                headDone[0] = true;
            }
            if (to > keep && !text.isEmpty()) {
                out.append(Component.literal(
                        text.substring(Math.max(0, keep - from))).withStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return headDone[0] ? out : null;
    }

    static Component bossBarName(Component bar, TranslationStore store) {
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
        MutableComponent out = Component.empty();
        boolean[] done = {false};
        bar.visit((style, text) -> {
            int at = done[0] ? -1 : text.indexOf(head);
            if (at >= 0) {
                out.append(Component.literal(text.substring(0, at) + translated
                                + text.substring(at + head.length())).withStyle(style));
                done[0] = true;
            } else if (!text.isEmpty()) {
                out.append(Component.literal(text).withStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return done[0] ? out : null;
    }

    static Component bossBarWords(Component bar, TranslationStore store) {
        MutableComponent out = Component.empty();
        boolean[] changed = {false};
        bar.visit((style, text) -> {
            if (text.isEmpty()) {
                return Optional.empty();
            }
            Matcher m = WORD.matcher(text);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                String dst = store.scopedLookup("bossbar", m.group());
                m.appendReplacement(sb, Matcher.quoteReplacement(dst != null ? dst : m.group()));
                changed[0] |= dst != null;
            }
            m.appendTail(sb);
            out.append(Component.literal(sb.toString()).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return changed[0] ? out : null;
    }

    private static final Pattern WORD = Pattern.compile("[A-Za-z]+");

    private static final Map<Component, Component> BARS = new HashMap<>();

    private static long barAge = -1;
}
