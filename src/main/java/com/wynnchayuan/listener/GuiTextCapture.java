package com.wynnchayuan.listener;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 收集介面文字（公會選單、任務書、製作台這類 GUI 的 tooltip）。
 *
 * <h2>為什麼這個要另外開關</h2>
 * 裝備與技能的文案可以從官方 CDN 完整離線取得，在遊戲裡掃是重工——
 * 所以預設不收 tooltip。
 *
 * <p>但伺服器自製的 GUI（公會週目標、任務書、製作台）<b>不在任何官方資料裡</b>，
 * 只能靠玩家打開那些畫面才會被記下來。這是唯一的取得方式。
 *
 * <p>做成獨立開關而不是預設開啟：平常玩的時候一直掃背包只會累積大量
 * 「其實 CDN 已經有」的裝備文字，把真正缺的 GUI 字串淹掉。
 * 想補 GUI 翻譯時再打開，逛一輪選單就夠了。
 */
public final class GuiTextCapture {

    /** 上一次處理過的內容，用來跳過同一份 tooltip 的後續幀。 */
    private static List<Component> lastSeen = List.of();

    private GuiTextCapture() {}

    public static void record(List<Component> tooltip) {
        if (tooltip == null || tooltip.isEmpty()
                || !WynnChaYuan.config().collect()
                || !WynnChaYuan.config().collectGuiText()) {
            return;
        }
        if (tooltip.equals(lastSeen)) {
            return;                            // 同一份的下一幀，沒有新東西
        }
        lastSeen = List.copyOf(tooltip);

        boolean first = true;
        for (Component line : tooltip) {
            StyledText styled = StyledText.fromComponent(line);
            if (GlyphSplitter.isGlyphOnly(styled)) {
                continue;                      // 純圖示的分隔行
            }
            String template = GlyphSplitter.toTemplate(styled);
            if (template.isBlank() || !GlyphSplitter.hasLetter(template)) {
                continue;
            }
            if (PlayerDataFilter.carriesPlayerData(template)) {
                WynnChaYuan.store().noteEvent("gui.blocked.playerData");
                continue;                      // 公會選單常帶成員名稱
            }
            WynnChaYuan.store().record(
                    template,
                    first ? "name" : "desc",
                    "gui",
                    first ? "gui/title" : "gui/line");
            first = false;
        }
    }
}
