package com.wynnchayuan.listener;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.DialogueProbe;
import com.wynnchayuan.render.DialogueOverlay;
import com.wynnchayuan.render.DialogueRewriter;
import com.wynntils.handlers.actionbar.ActionBarSegment;
import com.wynntils.handlers.actionbar.event.ActionBarRenderEvent;
import com.wynntils.mc.event.SystemMessageEvent;
import com.wynntils.models.dialogue.actionbar.segments.DialogueSegment;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * 就地取代模式下，把遊戲自己那段對話文字藏掉。
 *
 * <h2>對話其實是 action bar</h2>
 * Wynncraft 的 NPC 對話不是聊天訊息，也不是什麼獨立的 GUI——它整塊塞在
 * <b>action bar</b> 裡，用自訂字型與位移符號排成畫面下方那個框。Wynntils 把它
 * 認成一個 {@link DialogueSegment}，跟血量、魔力、座標那些段落並排。
 *
 * <p>所以要「就地取代」，不必去攔繪製、不必寫 mixin：在
 * {@link ActionBarRenderEvent} 上把那一段停用，Wynntils 的
 * {@code removeDisabledSegments} 會在送去畫之前把它從字串裡剪掉，
 * 其他段落原封不動。譯文再由 {@link DialogueOverlay} 畫在它空出來的位置上。
 *
 * <h2>沒有譯文就不藏</h2>
 * 藏原文之前一定要先確定<b>有東西可以擺上去</b>。查不到譯文卻把原文剪掉，
 * 玩家就是對著一片空白按 shift——比沒翻譯糟得多。
 *
 * <h2>順便解決了「對話什麼時候結束」</h2>
 * 這個事件在 action bar 每次更新時都會發，內容裡有沒有 {@link DialogueSegment}
 * 就是對話還在不在的直接答案。先前只能靠「多久沒更新就隱藏」的計時器猜，
 * 因為 Wynntils 的 {@code Ended} 事件每打一個字就發一次，根本不是結束。
 */
public final class ActionBarListener {

    /**
     * 對話消失後再等幾次更新才收掉譯文。
     *
     * <p>Wynncraft 在兩句之間會有幾幀沒有對話段落。收得太急，每換一句話
     * 譯文就會閃一下。
     */
    private static final int GRACE = 3;

    private int missing = 0;

    /**
     * action bar 的原始訊息，還沒被任何人動過。
     *
     * <p>{@link ActionBarRenderEvent} 拿到的是已經拆成段落的結果，而
     * {@link com.wynntils.models.dialogue.event.NpcDialogueEvent} 拿到的是
     * Wynntils 清理過的純文字——對話框那組自訂字型的資訊在那兩步都沒了。
     * 要弄清楚「保留原本的框、只換裡面的字」做不做得到，只能看這一手資料。
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onGameInfo(SystemMessageEvent.GameInfoReceivedEvent event) {
        DialogueProbe.record(event.getMessage());
        // 有選項的對話另外留格子錄——不然八格會被沒有選項的用光，
        // 而最需要看的偏偏是有選項的那一種。見 DialogueProbe#recordChoices。
        DialogueProbe.recordChoices(event.getMessage());
        // 選項的文字只在這一手資料裡看得到——Wynntils 的對話事件只帶 NPC 那一句。
        // 見 DialogueChoices 的說明（含實機錄到的字型分區）。
        java.util.List<String> picks =
                com.wynnchayuan.capture.DialogueChoices.of(event.getMessage());
        DialogueOverlay.noteChoices(picks,
                com.wynnchayuan.capture.DialogueChoices.selected(event.getMessage()));
        collect(picks);
    }

    /** 上一次收過的選項。action bar 每幀都發，不擋就會一直重複記。 */
    private java.util.List<String> lastPicks = java.util.List.of();
    /** 還在動、等它停下來的那一組選項。見 {@link #collect}。 */
    private java.util.List<String> settling = java.util.List.of();
    private long settlingSince;

    /**
     * 選項要<b>連續穩定這麼久</b>才收。
     *
     * <p>放太短擋不住跑馬燈，放太長會漏掉一閃而過的對話。對話框至少會停留幾秒，
     * 而跑馬燈是每幾個 tick 就換一格，中間隔著好幾個數量級。
     */
    private static final long SETTLE_MS = 700;

    /**
     * 把選項收進語料。
     *
     * <h2>為什麼要另外收</h2>
     * {@code CaptureListener#record} 記的是 Wynntils 給的對話文字，而那裡面
     * <b>只有 NPC 那一句</b>。選項是我們自己從原始 action bar 抽出來的，
     * 從來沒有進過收集流程——所以它們不會出現在 `captured.json`，
     * 也就永遠不會有人翻。畫面上看得到、語料裡卻沒有，等於只做了一半。
     */
    private void collect(java.util.List<String> picks) {
        if (picks.isEmpty() || !WynnChaYuan.config().collect()) {
            settling = java.util.List.of();
            return;
        }
        // ★ 等選項<b>停下來</b>再收。
        //
        // 太長的選項 Wynncraft 會做成跑馬燈，一格一格往左捲。先前這裡只擋
        // 「跟上一次完全相同」，於是每一格都是新字串、每一格都收一條——
        // 實機回報：四個選項的對話收出了 56 條，而且每一條都是切一半的視窗
        //（「mber anything from before yo」「ber anything from before you」）。
        //
        // 跑馬燈永遠不會停，所以永遠不會被收——這是對的：那句話從來沒有
        // 完整出現在畫面上，收進來的每一格都是殘句，翻了也對不上。
        if (!picks.equals(settling)) {
            settling = picks;
            settlingSince = System.currentTimeMillis();
            return;
        }
        if (System.currentTimeMillis() - settlingSince < SETTLE_MS
                || picks.equals(lastPicks)) {
            return;
        }
        lastPicks = picks;
        for (String pick : picks) {
            if (com.wynnchayuan.capture.PlayerDataFilter.carriesPlayerData(pick)) {
                WynnChaYuan.store().noteEvent("dialogue.blocked.playerData");
                continue;
            }
            // 第二道：漏網的殘句幾乎都是從字中間切開的。真正的選項是完整的
            // 句子，一律大寫或符號開頭。
            if (looksClipped(pick)) {
                WynnChaYuan.store().noteEvent("dialogue.blocked.clipped");
                continue;
            }
            com.wynntils.core.text.StyledText line =
                    com.wynntils.core.text.StyledText.fromString(pick);
            WynnChaYuan.store().record(
                    com.wynnchayuan.capture.GlyphSplitter.toTemplate(line),
                    "desc", "quest",
                    com.wynnchayuan.capture.CurrentQuest.tag("dialogue/choice", null));
        }
    }

    /**
     * 這一句是不是被跑馬燈從<b>字中間</b>切開的殘句。
     *
     * <p>Wynncraft 的選項都是寫成句子的，一律大寫字母或符號開頭
     *（{@code Just saying hello}、{@code Who are you?}）。捲動出來的視窗
     * 則是從單字中間切開的（{@code mber anything from before yo}）。
     *
     * <p>這是<b>第二道</b>：主要靠 {@link #collect} 的穩定判斷擋住。這一道只
     * 負責漏網的，所以寧可判準簡單也不要複雜到自己出錯。
     */
    static boolean looksClipped(String pick) {
        if (pick == null || pick.isEmpty()) {
            return false;
        }
        char first = pick.charAt(0);
        return first >= 'a' && first <= 'z';
    }

    /**
     * 就地取代：把譯文寫進 Wynncraft <b>自己那個</b>對話框裡。
     *
     * <p>優先權最低，所以 Wynntils 已經處理完才輪到我們——它那邊的段落解析
     * 讀的是原始英文，先被我們換成中文的話會整個認不出來
     * （自動翻頁那類功能就跟著壞掉）。
     *
     * <p>換不動就什麼都不做，原文照樣顯示。這比「藏掉原文卻補不上譯文」好，
     * 也比自己畫一個框好——框、名牌、頭像都在那條訊息裡，動它們就沒了。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onGameInfoRewrite(SystemMessageEvent.GameInfoReceivedEvent event) {
        // 內文與選項是兩個開關，任一邊要就地取代就得進來——
        // 到底換哪幾段由 DialogueRewriter 自己再判斷一次。
        if (WynnChaYuan.config().dialogueMode() != CollectorConfig.DialogueMode.REPLACE
                && WynnChaYuan.config().choiceMode() != CollectorConfig.DialogueMode.REPLACE) {
            return;
        }
        try {
            var swapped = DialogueRewriter.rewrite(
                    event.getMessage(), WynnChaYuan.translations());
            if (swapped != null) {
                event.setMessage(swapped);
                DialogueProbe.after(swapped);
            } else {
                DialogueProbe.miss(event.getMessage());
            }
        } catch (Throwable t) {
            // action bar 每 tick 都會走這裡，出錯絕不能讓遊戲停下來
            WynnChaYuan.store().noteEvent("dialogue.rewriteError");
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onActionBarRender(ActionBarRenderEvent event) {
        boolean present = false;
        for (ActionBarSegment segment : event.getSegments()) {
            if (segment instanceof DialogueSegment) {
                present = true;
                break;
            }
        }

        if (!present) {
            if (++missing >= GRACE && DialogueOverlay.hasContent()) {
                DialogueOverlay.clear();
            }
            return;
        }
        missing = 0;

        // 這裡不再藏原文。就地取代改成<b>改寫</b>那條訊息的內容
        // （見 onGameInfoRewrite）——藏掉的話，框、名牌、頭像會一起不見，
        // 那正是先前怎麼調都不像的原因。
    }
}
