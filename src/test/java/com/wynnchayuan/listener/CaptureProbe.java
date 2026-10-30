package com.wynnchayuan.listener;

import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;

public final class CaptureProbe {

    private CaptureProbe() {}

    public static String chatTemplate(StyledText message, TranslationStore store) {
        return CaptureListener.chatTemplate(message, event -> { }, store);
    }
}
