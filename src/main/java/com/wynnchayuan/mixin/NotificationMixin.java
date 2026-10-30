package com.wynnchayuan.mixin;

import com.wynnchayuan.render.WynntilsText;
import com.wynntils.core.text.StyledText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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
