package com.wynnchayuan.listener;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynnchayuan.render.TrackerOverlay;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.scoreboard.ScoreboardSegment;
import com.wynntils.handlers.scoreboard.event.ScoreboardUpdatedEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 畫面右上那一欄（每日目標、世界事件、Lootrun、團隊）。
 *
 * <h2>那一欄是什麼</h2>
 * Wynncraft 把它放在<b>計分板</b>裡，Wynntils 讀進去、拆成一段一段
 *（{@link ScoreboardSegment}），再用自己的疊層畫在右上角。要翻譯那幾行，
 * 最乾淨的入口就是 Wynntils 拆好的結果——不必自己解計分板封包，
 * 也不必去動別的模組畫出來的東西。
 *
 * <h2>為什麼不直接改 Wynntils 畫的那一塊</h2>
 * 它是 Wynntils 自己的 overlay，內容由它的樣板系統展開。要換掉那裡的字
 * 只能 mixin 進另一個模組的算繪流程——Wynntils 一改版就會掛，而且是
 * <b>開不起來</b>那種掛。所以譯文畫在我們自己的框裡（見 {@link TrackerOverlay}），
 * 原文那一欄原封不動，兩邊並存。
 *
 * <p>追蹤中的任務那一段<b>跳過</b>：{@link TrackerListener} 已經把它畫在
 * 同一個框的上半部了，再收一次會變成同一件事寫兩遍。
 */
public final class ScoreboardListener {

    /** 追蹤任務那一段的類別名，見上面說明：它由 TrackerListener 負責。 */
    private static final String ACTIVITY_PART = "ActivityTrackerScoreboardPart";

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onScoreboardUpdated(ScoreboardUpdatedEvent event) {
        try {
            update(event);
        } catch (Throwable t) {
            // 計分板每幾秒就換一次，出錯絕不能讓遊戲停下來
            WynnChaYuan.store().noteEvent("scoreboard.error");
        }
    }

    private void update(ScoreboardUpdatedEvent event) {
        TranslationStore store = WynnChaYuan.translations();
        List<Component> lines = new ArrayList<>();
        boolean any = false;
        for (var pair : event.getScoreboardSegments()) {
            ScoreboardSegment segment = pair.b();
            if (segment == null || !segment.isVisible()) {
                continue;
            }
            if (pair.a() != null
                    && pair.a().getClass().getSimpleName().contains(ACTIVITY_PART)) {
                continue;                      // 追蹤中的任務，上半部已經有了
            }
            any |= add(lines, segment.getHeader(), store, "name");
            for (StyledText row : segment.getContent()) {
                any |= add(lines, row, store, "desc");
            }
        }
        WynnChaYuan.store().noteEvent(any ? "scoreboard.shown" : "scoreboard.noMatch");
        // 一行都沒翻出來就整塊不擺：全是英文的話，右上角那一欄本來就看得到，
        // 我們再抄一次只是佔位置。
        TrackerOverlay.setExtras(any ? lines : List.of());
    }

    /**
     * 收一行：翻得出來就擺譯文，翻不出來擺原文（整塊的其他行可能翻得出來，
     * 少一行會讓人以為那一項不見了）。同時把沒譯文的收進語料。
     *
     * @return 這一行真的翻出來了嗎
     */
    private boolean add(List<Component> lines, StyledText row,
                        TranslationStore store, String role) {
        if (row == null || GlyphSplitter.isGlyphOnly(row)) {
            return false;
        }
        String template = GlyphSplitter.toTemplate(row);
        if (template.isBlank()) {
            return false;
        }
        // 隊伍成員、公會名稱都會出現在這一欄，一律不進語料。
        //
        // 隊友那幾列的名字會被欄寬截斷、數字又先變成 {~}，字形判準認不出來，
        // 所以另外問一次分頁列上的玩家名。見 PlayerDataFilter#mentionsOnlinePlayerLoose。
        if (PlayerDataFilter.carriesPlayerData(template)
                || PlayerDataFilter.mentionsOnlinePlayerLoose(template)) {
            WynnChaYuan.store().noteEvent("scoreboard.blocked.playerData");
        } else {
            WynnChaYuan.store().record(template, role, "quest", "scoreboard");
        }
        Component translated = LineTranslator.translate(row, store);
        lines.add(translated != null ? translated : LineTranslator.untranslated(row));
        return translated != null;
    }
}
