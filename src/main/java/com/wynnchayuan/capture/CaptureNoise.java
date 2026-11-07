package com.wynnchayuan.capture;

import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class CaptureNoise {

    private CaptureNoise() {}

    static final String BLACKOUT = "skipped.blackout";
    static final String AREA_BAR = "skipped.areaName";
    static final String GUILD_TAG = "skipped.guildTag";
    static final String GUILD_TITLE = "skipped.guildTitle";
    static final String MESSAGE = "skipped.message";
    static final String HOUSING = "skipped.housing";
    static final String PET = "skipped.pet";

    static String reason(String template, String ctx) {
        if (template == null) {
            return null;
        }
        String ctxName = ctx == null ? "" : ctx;
        String core = template.strip();
        if (BLACKOUT_GLYPH.equals(SLOTS_AND_SPACE.matcher(core).replaceAll(""))) {
            return BLACKOUT;
        }
        if (ctxName.startsWith("label/bossbar") && core.equals(AREA_BOSSBAR)) {
            return AREA_BAR;
        }
        if (LEADING_GUILD_TAG.matcher(template).find()) {
            return GUILD_TAG;
        }
        if (TELEPORTER_SIGN.matcher(core).find()) {
            return HOUSING;
        }
        if (ctxName.equals("title") && GUILD_WELCOME.matcher(core).find()) {
            return GUILD_TITLE;
        }
        if (ctxName.equals("chat/notification")
                && (core.startsWith(INCOMING) || SENT_BETWEEN.matcher(core).find())) {
            return MESSAGE;
        }
        return null;
    }

    static boolean dependsOnScene(String ctx) {
        return ctx != null && (ctx.equals("title") || ctx.startsWith("label/floating"));
    }

    static String sceneReason(String template, String ctx, boolean housing, Supplier<String> place) {
        if (template == null || !dependsOnScene(ctx)) {
            return null;
        }
        if (housing) {
            return HOUSING;
        }
        if (!ctx.startsWith("label/floating") || !PLAIN_PLATE.matcher(template.strip()).matches()) {
            return null;
        }
        String here = place == null ? null : place.get();
        return here != null && TOWNS.contains(here) ? PET : null;
    }

    private static final String BLACKOUT_GLYPH = "Á";

    private static final Pattern SLOTS_AND_SPACE = Pattern.compile("\\{#}|\\s");

    private static final String AREA_BOSSBAR = "{p} {#}{#}";

    private static final Pattern LEADING_GUILD_TAG =
            Pattern.compile("(?m)^(?:\\{#}\\s*)*\\[[A-Za-z0-9]{3,4}]\\s");

    private static final Pattern TELEPORTER_SIGN =
            Pattern.compile("\\A(?:\\{#})*Teleporter\\nto \\S");

    private static final Pattern GUILD_WELCOME = Pattern.compile("^Welcome to \\S[^\\n]* Guild\\b");

    private static final String INCOMING = "← ";

    private static final Pattern SENT_BETWEEN = Pattern.compile("^[^\\n:→]+ → [^\\n:→]+: ");

    private static final Pattern PLAIN_PLATE =
            Pattern.compile("(?:[^\\n{}]|\\{~})*\\p{L}(?:[^\\n{}]|\\{~})* \\{#}\\{#}");

    // 城裡不生怪，「名字 {#}{#}」只會是寵物或坐騎
    private static final Set<String> TOWNS = Set.of(
            "Ragni", "Detlas", "Thesead", "Cinfras", "Rodoroc", "Llevigar");
}
