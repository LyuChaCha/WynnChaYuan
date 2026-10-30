package com.wynnchayuan.capture;

public final class QuestGuess {

    static final long CARRY_MS = 20_000;

    private static volatile Boolean lastVerdict;
    private static volatile long lastVerdictAt;

    private QuestGuess() {}

    public static boolean withQuest(String quest, String speaker, Boolean inCast, boolean fromBlock) {
        if (quest == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (speaker != null) {
            boolean verdict = inCast == null || inCast;
            lastVerdict = verdict;
            lastVerdictAt = now;
            return verdict;
        }
        if (fromBlock) {
            lastVerdict = false;
            lastVerdictAt = now;
            return false;
        }
        Boolean carried = lastVerdict;
        if (carried != null && now - lastVerdictAt <= CARRY_MS) {
            lastVerdictAt = now;
            return carried;
        }
        return true;
    }

    public static void reset() {
        lastVerdict = null;
    }
}
