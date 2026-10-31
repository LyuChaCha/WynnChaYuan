package com.wynnchayuan.listener;

import com.wynnchayuan.capture.ConversationPartner;
import com.wynnchayuan.capture.CurrentQuest;
import com.wynnchayuan.capture.QuestGuess;
import com.wynnchayuan.render.ComponentMemo;
import com.wynnchayuan.render.DialogueOverlay;
import com.wynnchayuan.render.DialogueRewriter;
import com.wynnchayuan.render.LookAtTranslator;
import com.wynnchayuan.render.TrackerOverlay;
import com.wynnchayuan.render.WynnventoryPriceCache;

public final class WorldReset {

    private WorldReset() {}

    public static void reset() {
        TrackerOverlay.clear();
        DialogueOverlay.clear();
        DialogueRewriter.forget();
        LookAtTranslator.clear();
        ComponentMemo.forget();
        WynnventoryPriceCache.forget();
        ChatBlock.flushPending();
        ConversationPartner.clear();
        QuestGuess.reset();
        CurrentQuest.set(null);
    }
}
