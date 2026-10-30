package com.wynnchayuan.capture;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.Optional;

public final class DialogueProgress {

    public static final int FULL = 0xDF;

    private static final String FONT = "hud/dialogue/effect/progress";

    private static final int STEADY = 3;

    private static volatile int value = -1;
    private static volatile int same = 0;

    private DialogueProgress() {}

    public static void noteRaw(Component message) {
        if (message == null) {
            return;
        }
        int[] found = {-1};
        message.visit((style, part) -> {
            if (style != null && style.getFont() != null
                    && style.getFont().toString().contains(FONT)) {
                part.codePoints().filter(cp -> cp >= 0xE000 && cp <= 0xE000 + FULL)
                        .forEach(cp -> found[0] = cp - 0xE000);
            }
            return Optional.empty();
        }, Style.EMPTY);
        set(found[0]);
    }

    public static void set(int now) {
        if (now == value) {
            same++;
        } else {
            value = now;
            same = 0;
        }
    }

    public static void settle(int now) {
        value = now;
        same = STEADY;
    }

    private static volatile int conversation;

    public static void endConversation() {
        conversation++;
        value = -1;
        same = 0;
    }

    public static int conversation() {
        return conversation;
    }

    public static int settled() {
        return same >= STEADY ? value : -1;
    }
}
