package com.wynnchayuan.listener;

import com.wynnchayuan.capture.ConversationPartner;
import com.wynnchayuan.capture.CurrentQuest;
import com.wynnchayuan.capture.QuestGuess;
import com.wynnchayuan.render.DialogueOverlay;
import com.wynnchayuan.render.DialogueRewriter;
import com.wynnchayuan.render.LookAtTranslator;
import com.wynnchayuan.render.TrackerOverlay;

public final class WorldReset {

    private WorldReset() {}

    public static void reset() {
        TrackerOverlay.clear();
        DialogueOverlay.clear();
        DialogueRewriter.forget();
        LookAtTranslator.clear();
        ChatBlock.flushPending();
        ConversationPartner.clear();
        QuestGuess.reset();
        CurrentQuest.set(null);
    }
}
