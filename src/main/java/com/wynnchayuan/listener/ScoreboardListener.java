package com.wynnchayuan.listener;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynnchayuan.render.ScoreboardFlow;
import com.wynnchayuan.render.TrackerOverlay;
import com.wynnchayuan.render.WynntilsText;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.scoreboard.ScoreboardSegment;
import com.wynntils.handlers.scoreboard.event.ScoreboardUpdatedEvent;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * 同一個框的上半部了，再收一次會變成同一件事寫兩遍。Lootrun 那一段也跳過，
 * 理由見 {@link #LOOTRUN_PART}。
 */
public final class ScoreboardListener {

    /** 追蹤任務那一段的類別名，見上面說明：它由 TrackerListener 負責。 */
    private static final String ACTIVITY_PART = "ActivityTrackerScoreboardPart";

    /**
     * Lootrun 那一段的類別名。
     *
     * <h2>為什麼不收</h2>
     * 那一段本來就有自己的位置——遊戲右邊的記分板，還有 Wynntils 自己的
     * Lootrun 疊層。我們的追蹤欄面板再抄一份，畫面上就是同一組
     * 「Lootrun: ／選擇一個信標！／剩餘時間／挑戰」出現兩次，而且它有四、五行，
     * 一進 Lootrun 就把面板上半部的任務擠掉。使用者回報的正是這個
     *（2026-09-28，「Lootrun 應該只待在旁邊記分板，但追蹤欄也出現了一份」）。
     *
     * <p>其他幾段（每日目標、公會目標、團隊、公會戰）照收：那幾段短，
     * 而且原文那一欄沒有第二個地方看得到譯文。
     */
    private static final String LOOTRUN_PART = "LootrunScoreboardPart";

    /**
     * 這一段要不要抄進我們的追蹤欄面板。
     *
     * @param part 那一段的 {@code ScoreboardPart} 類別簡名；認不出來時傳
     *             {@code null}（照收，寧可多一段也不要整欄憑空少東西）
     */
    static boolean mirrors(String part) {
        if (part == null) {
            return true;
        }
        return !part.contains(ACTIVITY_PART) && !part.contains(LOOTRUN_PART);
    }

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
        // 記分板上現在有哪幾列、哪幾列有整句切回來的譯文：給 Wynntils 自己畫的
        // 那一塊用（見 WynntilsText#screenText），每次更新整份換掉。
        Set<String> shown = new HashSet<>();
        Map<String, StyledText> replaced = new HashMap<>();
        boolean any = false;
        for (var pair : event.getScoreboardSegments()) {
            ScoreboardSegment segment = pair.b();
            if (segment == null || !segment.isVisible()) {
                continue;
            }
            String part = pair.a() == null ? null : pair.a().getClass().getSimpleName();
            // 追蹤中的任務整段不碰：它同時畫在 Wynntils 的追蹤欄，歸「右上任務追蹤」管，
            // 記分板的開關關掉時不該連那邊一起變回英文。
            if (part != null && part.contains(ACTIVITY_PART)) {
                continue;
            }
            List<StyledText> content = segment.getContent();
            shown.add(ScoreboardFlow.key(segment.getHeader()));
            for (StyledText row : content) {
                shown.add(ScoreboardFlow.key(row));
                capture(row, "desc");
            }
            captureBlock(content);
            ScoreboardFlow.Plan plan = ScoreboardFlow.plan(content, store);
            replaced.putAll(plan.rows());
            // Lootrun（記分板本來就在）不抄進面板，見 #mirrors；但上面那份對照表照做
            if (!mirrors(part)) {
                continue;
            }
            any |= header(lines, segment.getHeader(), store);
            lines.addAll(plan.panel());
            any |= plan.any();
        }
        WynnChaYuan.store().noteEvent(any ? "scoreboard.shown" : "scoreboard.noMatch");
        boolean on = WynnChaYuan.config() == null || WynnChaYuan.config().translateScoreboard();
        WynntilsText.setScoreboard(shown, replaced);
        // 一行都沒翻出來就整塊不擺：全是英文的話，右上角那一欄本來就看得到，
        // 我們再抄一次只是佔位置。開關關掉也不擺。
        TrackerOverlay.setExtras(any && on ? lines : List.of());
    }

    /**
     * 一段的標題：翻得出來就擺譯文，翻不出來擺原文（整塊的其他行可能翻得出來，
     * 少一行會讓人以為那一項不見了）。同時把沒譯文的收進語料。
     *
     * @return 這一行真的翻出來了嗎
     */
    private boolean header(List<Component> lines, StyledText row, TranslationStore store) {
        if (!capture(row, "name")) {
            return false;
        }
        Component translated = LineTranslator.translate(row, store);
        if (translated != null) {
            translated = ScoreboardFlow.oneLine(translated);
        }
        lines.add(translated != null ? translated : LineTranslator.untranslated(row));
        return translated != null;
    }

    /**
     * 把一列收進語料（沒譯文的才會真的留下來）。
     *
     * @return 這一列有沒有字（純圖示、空白的回 {@code false}）
     */
    private boolean capture(StyledText row, String role) {
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
        return true;
    }

    /**
     * 整段內容再收一份，列與列之間用換行接起來。
     *
     * <h2>為什麼一列一列收還不夠</h2>
     * 一句話被折成四列，一列一列收進去就是四條半句，而且中間翻過的那幾列
     * <b>不會出現在 capture 裡</b>（只列還沒翻的）——拿到檔案的人看到
     * {@code tower. [{~}/{~}]} 一條孤零零的，根本拼不回原句。整段收一份，
     * 譯者才看得到完整的句子，也才寫得出 {@link ScoreboardFlow} 要的整句條目。
     *
     * <p>只有一列的不必再收（跟上面那一份一模一樣）；任何一列帶著玩家資料就整段不收。
     */
    private void captureBlock(List<StyledText> content) {
        List<String> rows = new ArrayList<>();
        for (StyledText row : content) {
            if (row == null || GlyphSplitter.isGlyphOnly(row)) {
                continue;
            }
            String template = GlyphSplitter.toTemplate(row);
            if (template.isBlank()) {
                continue;
            }
            if (PlayerDataFilter.carriesPlayerData(template)
                    || PlayerDataFilter.mentionsOnlinePlayerLoose(template)) {
                return;
            }
            rows.add(template);
        }
        if (rows.size() >= 2) {
            WynnChaYuan.store().record(String.join("\n", rows), "desc", "quest",
                    "scoreboard/block");
        }
    }
}
