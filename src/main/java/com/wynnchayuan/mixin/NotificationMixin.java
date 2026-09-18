package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import com.wynntils.core.text.StyledText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Wynntils 的通知框：被它從聊天攔下、改畫在畫面上的那幾行
 *（討伐戰的「Enter the Void Hole to reach the Hold Area!」）。
 *
 * <p>換進隊列之前的那一份就好：之後的排版、計時、淡出都照 Wynntils 的。
 * 判斷與翻譯在 {@link WynntilsText#notification}。
 */
@Mixin(targets = "com.wynntils.core.notifications.NotificationManager", remap = false)
public abstract class NotificationMixin {

    @ModifyVariable(method = "queueMessage(Lcom/wynntils/core/text/StyledText;)Lcom/wynntils/core/notifications/MessageContainer;",
                    at = @At("HEAD"), argsOnly = true, remap = false)
    private StyledText wynnchayuan$queue(StyledText text) {
        return WynntilsText.notification(text);
    }

    @ModifyVariable(method = "editMessage", at = @At("HEAD"), argsOnly = true, remap = false)
    private StyledText wynnchayuan$edit(StyledText text) {
        return WynntilsText.notification(text);
    }
}
