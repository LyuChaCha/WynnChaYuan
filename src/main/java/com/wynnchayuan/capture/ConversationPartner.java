package com.wynnchayuan.capture;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * 玩家剛剛右鍵點了哪個 NPC——對話框沒有名牌時的說話者。
 *
 * <h2>為什麼要有（issue #1138）</h2>
 * 說話者先看對話框的名牌（{@link DialogueSpeaker#currentFor}）。名牌讀不到的時候
 * ——框裡只放了佔位字、或是那一片沒跟著這一句——先前退回「玩家正前方那塊名牌」
 * 去猜，猜到的是寵物，或是旁邊另一個 NPC。但對話是<b>玩家右鍵點了某個 NPC</b>
 * 才開始的，點的是誰最清楚：記下那個 NPC 頭頂的名牌，沒名牌的句子就算他的。
 *
 * <h2>什麼時候不算</h2>
 * <ul>
 *   <li>點下去之後 {@link #START_MS} 內沒有出現對話：點的是別的東西（交易、坐騎）。</li>
 *   <li>對話停了 {@link #IDLE_MS} 以上：那段對話已經結束。</li>
 *   <li>右鍵點了方塊（日記、書、門）：之後的旁白不是剛才那個 NPC 說的。</li>
 *   <li>點的是真的玩家（頭頂名牌就是帳號名），或名字被玩家資料濾網擋下：不記。</li>
 * </ul>
 * 對話途中插進來、自己又沒名牌的另一個 NPC 仍然會被算成點的那個人——
 * 這是剩下的已知誤差，比先前「看著誰就算誰」小得多。
 */
public final class ConversationPartner {

    /** 點完之後多久內要出現對話，才算是跟這個 NPC 講話。 */
    static final long START_MS = 4_000;

    /** 對話停了多久就當作結束。玩家看完一句再按 shift 通常不會停這麼久。 */
    static final long IDLE_MS = 20_000;

    private static volatile String name;
    private static volatile long clickedAt;
    private static volatile long lastLineAt;
    private static volatile boolean talking;

    private ConversationPartner() {}

    /** 右鍵點了實體。 */
    public static void clicked(Entity entity) {
        String raw = com.wynnchayuan.render.LookAtTranslator.labelAbove(entity);
        String found = DialogueSpeaker.cleanName(raw);
        if (found != null && isRealPlayer(entity, found)) {
            note("partner.reject.player");
            found = null;
        }
        if (found != null && (PlayerDataFilter.carriesPlayerData(found)
                || SelfNames.all().contains(found))) {
            note("partner.reject.filter");
            found = null;
        }
        note(found == null ? "partner.unset" : "partner.set");
        name = found;
        blockAt = 0;
        clickedAt = System.currentTimeMillis();
        talking = false;
    }

    /** 右鍵點了方塊（日記、書、門）：之後的旁白不是剛才那個 NPC 說的。 */
    public static void blockClicked() {
        clear();
        blockAt = System.currentTimeMillis();
    }

    /**
     * 這段對話是不是剛右鍵點了方塊才開始的（日記、書、告示）。
     * 給 {@link QuestGuess} 判斷旁白要不要掛任務名。
     */
    public static boolean fromBlock() {
        return name == null && System.currentTimeMillis() - blockAt <= IDLE_MS;
    }

    private static volatile long blockAt;

    /** 任何確定不是同一段對話的時候。 */
    public static void clear() {
        if (name != null) {
            note("partner.cleared");
        }
        name = null;
        talking = false;
    }

    /**
     * 有一句對話進來了：現在的說話者是誰。
     *
     * @return 點過的 NPC 名字；不算數時回傳 {@code null}
     */
    public static String forLine() {
        String current = name;
        if (current == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        boolean expired = talking
                ? now - lastLineAt > IDLE_MS
                : now - clickedAt > START_MS;
        if (expired) {
            note("partner.expired");
            clear();
            return null;
        }
        if (!talking) {
            note("partner.used");
        }
        talking = true;
        lastLineAt = now;
        return current;
    }

    /** 診斷計數，寫進 captured.json 的 {@code _meta.events}。 */
    private static void note(String event) {
        try {
            com.wynnchayuan.WynnChaYuan.store().noteEvent(event);
        } catch (Throwable ignored) {
            // 計數失敗不影響任何事
        }
    }

    /**
     * 點的是真的玩家嗎。
     *
     * <h2>為什麼不看線上名單</h2>
     * Wynncraft 的 NPC 全是玩家模型的實體，而 tab 名單也被伺服器塞滿了假條目——
     * 實測點了十次 NPC，十次都在「線上玩家」裡。分得開的是<b>頭頂寫什麼</b>：
     * 玩家的名牌寫的是自己的帳號名，NPC 的帳號名是亂碼或空的，名牌寫的是
     * 「Espren Guard」。名牌跟帳號名對得上才算玩家。
     */
    private static boolean isRealPlayer(Entity entity, String label) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        try {
            String account = player.getGameProfile().name();
            if (account == null || account.isBlank()) {
                return false;
            }
            String a = account.strip().toLowerCase(java.util.Locale.ROOT);
            String l = label.toLowerCase(java.util.Locale.ROOT);
            return l.equals(a) || (a.length() >= 3 && l.contains(a));
        } catch (Throwable t) {
            return true;   // 判斷不了就當成玩家，寧可不記
        }
    }
}
