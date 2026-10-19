package com.wynnchayuan.listener;

import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;

/** 給別的套件的測試用：{@link CaptureListener#chatTemplate} 是套件內可見的。 */
public final class CaptureProbe {

    private CaptureProbe() {}

    /** 這一則聊天收集端會記成什麼；不收回 {@code null}。 */
    public static String chatTemplate(StyledText message, TranslationStore store) {
        return CaptureListener.chatTemplate(message, event -> { }, store);
    }
}
