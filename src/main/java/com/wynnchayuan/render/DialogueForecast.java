package com.wynnchayuan.render;

import com.wynnchayuan.translate.TranslationStore;

import java.util.function.LongSupplier;

final class DialogueForecast {

    static final int WINDOW = 8;

    static final long FRESH_MS = 45_000;

    static final int CONFIRM = 8;

    static final int COLD_FLOOR = 4;

    static final int NEXT_WINDOW = 4;

    static volatile int nextFloor = 2;

    static volatile boolean nextLine = true;

    static volatile int speakerFloor = COLD_FLOOR;

    static volatile boolean speakerScope = true;

    static volatile boolean enabled = true;

    private String quest;
    private int index = -1;

    private int progress = -1;

    private int talk = -1;
    private String source;
    private long seen;

    private final LongSupplier clock;

    DialogueForecast() {
        this(() -> now.getAsLong());
    }

    static volatile LongSupplier now = System::currentTimeMillis;

    DialogueForecast(LongSupplier clock) {
        this.clock = clock;
    }

    static void forgetAll() {
        DialogueTyping.FORECAST.forget();
        DialogueOverlay.FORECAST.forget();
    }

    void forget() {
        asked = null;
        answer = null;
        quest = null;
        index = -1;
        source = null;
        seen = 0;
        progress = -1;
    }

    String guess(TranslationStore store, String typed) {
        String speaker = com.wynnchayuan.capture.DialogueSpeaker.current();
        if (!enabled || store == null || typed == null) {
            return null;
        }
        if (source != null && typed.length() > source.length() && typed.startsWith(source)) {
            return null;
        }
        boolean fresh = quest != null && clock.getAsLong() - seen <= FRESH_MS;
        String tracked = com.wynnchayuan.capture.CurrentQuest.get();
        String where = fresh ? quest : tracked;
        if (where == null) {
            return null;
        }
        boolean midway = fresh && progress > 0
                && progress < com.wynnchayuan.capture.DialogueProgress.FULL - 2
                && talk == com.wynnchayuan.capture.DialogueProgress.conversation();
        Query ask = new Query(typed, where, fresh ? index : -1, speaker, tracked,
                store.generation(), midway);
        if (ask.equals(asked)) {
            return answer;
        }
        String guess = null;
        if (midway && nextLine) {
            guess = store.forecastNext(typed, where, index, NEXT_WINDOW, speaker, nextFloor,
                    DialogueTyping::within);
        }
        if (guess == null && speaker != null) {
            guess = store.forecast(typed, where, fresh ? index : -1, WINDOW, speaker,
                    fresh ? 1 : COLD_FLOOR, DialogueTyping::within);
        }
        if (guess == null && fresh && !quest.equals(tracked)) {
            guess = store.matchPrefix(typed, 0, quest);
        }
        if (guess == null && speaker != null && speakerScope) {
            guess = store.forecastBySpeaker(typed, where, speaker, speakerFloor,
                    DialogueTyping::within);
        }
        asked = ask;
        answer = guess;
        return guess;
    }

    private record Query(String typed, String where, int after, String speaker, String tracked,
                         int generation, boolean midway) {}

    private Query asked;
    private String answer;

    void heard(TranslationStore store, String heard) {
        if (store == null || heard == null) {
            return;
        }
        long now = clock.getAsLong();
        if (heard.equals(source)) {
            seen = now;
            int bar = com.wynnchayuan.capture.DialogueProgress.settled();
            if (bar >= 0) {
                progress = bar;
                talk = com.wynnchayuan.capture.DialogueProgress.conversation();
            }
            return;
        }
        String q = store.sequenceQuestOf(heard, quest,
                com.wynnchayuan.capture.CurrentQuest.get());
        int at = q == null ? -1 : store.sequenceIndex(q, heard, q.equals(quest) ? index : -1);
        if (at < 0) {
            return;
        }
        quest = q;
        index = at;
        source = heard;
        seen = now;
        progress = -1;
    }

    int index() {
        return index;
    }

    String anchorQuest() {
        return quest;
    }

    boolean fresh() {
        return quest != null && clock.getAsLong() - seen <= FRESH_MS;
    }
}
