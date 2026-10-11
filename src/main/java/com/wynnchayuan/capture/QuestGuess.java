package com.wynnchayuan.capture;

/**
 * 這句對話要不要掛上追蹤中的任務名。
 *
 * <h2>為什麼要有（issue #1138）</h2>
 * {@link CurrentQuest} 是追蹤欄上的任務，不是這段對話的任務。玩家追著
 * Undersupply 的時候順路跟 Marseled 聊天、跑完祕密發現 Solace in Pain，
 * 一百多句全被記成 Undersupply——而 {@code import-captured.py} 把這個名字當成
 * 劇情檔，這些台詞會進錯的任務檔。遊戲本身不標（任務對話跟一般閒聊用的是
 * 同一套 {@code style/default} 對話框），只能從說話者推。
 *
 * <h2>規則</h2>
 * <ul>
 *   <li>有說話者、而追蹤中的任務在語料裡有說話者名單：在名單上才掛。</li>
 *   <li>沒有說話者（旁白、日記、告示）：跟著<b>同一段對話</b>前面那句的判斷走；
 *       剛右鍵點了方塊（日記、書）才開始的，不掛。</li>
 *   <li>任務在語料裡沒有名單（新任務）：判斷不了，照舊掛。</li>
 * </ul>
 * 共用 NPC（同一個村子的衛兵出現在兩個任務裡）仍然可能掛錯，但比
 * 「看追蹤欄就全掛上去」少得多。
 */
public final class QuestGuess {

    /** 沒有說話者的句子沿用前一句判斷的時限；跟 {@link ConversationPartner} 同一個量級。 */
    static final long CARRY_MS = 20_000;

    private static volatile Boolean lastVerdict;
    private static volatile long lastVerdictAt;

    private QuestGuess() {}

    /**
     * @param quest    追蹤中的任務（{@link CurrentQuest#get()}）
     * @param speaker  這句的說話者，沒有就 {@code null}
     * @param inCast   語料的判斷：說話者在不在這個任務的名單上；判斷不了是 {@code null}
     * @param fromBlock 這段對話是不是剛右鍵點了方塊才開始的
     * @return 要不要掛任務名
     */
    public static boolean withQuest(String quest, String speaker, Boolean inCast, boolean fromBlock) {
        if (quest == null) {
            return false;                     // 沒在追蹤，本來就沒有可掛的
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

    /** 換任務、換世界的時候清掉，免得上一段的判斷帶進下一段。 */
    public static void reset() {
        lastVerdict = null;
    }
}
