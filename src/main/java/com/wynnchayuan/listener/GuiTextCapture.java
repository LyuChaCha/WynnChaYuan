package com.wynnchayuan.listener;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
        record(tooltip, ItemStack.EMPTY);
    }

    /**
     * @param stack 這份 tooltip 屬於哪一格。玩家頭顱的第一行<b>一定</b>是帳號名，
     *              見下面的說明。
     */
    public static void record(List<Component> tooltip, ItemStack stack) {
        if (tooltip == null || tooltip.isEmpty()
                || !WynnChaYuan.config().collect()
                || !WynnChaYuan.config().collectGuiText()) {
            return;
        }
        if (tooltip.equals(lastSeen)) {
            return;                            // 同一份的下一幀，沒有新東西
        }
        lastSeen = List.copyOf(tooltip);

        // 玩家頭顱的標題就是帳號名，一個都不要收。
        //
        // <h2>為什麼要看格子而不是看字串</h2>
        // 帳號名<b>沒有形狀</b>可以認：Cynrik、Kicev、Neonrat、Idrisu 跟隨便一個
        // 奇幻 NPC 名長得一模一樣，只有底線與駝峰那種才擋得下來。實機開一次公會
        // 成員清單就漏了二十幾個真人 ID 進共享語料——那是不能出事的一類。
        //
        // 但「這一格是玩家頭顱」是<b>確定</b>的事實，不是猜的。公會成員、隊伍、
        // 好友、排行榜全部用頭顱擺人，所以擋這個就等於擋掉整類。
        //
        // 只跳過標題那一行：底下的「Left-Click to set rank」那些是正常的介面字，
        // 照收。
        boolean skipTitle = stack != null && stack.is(Items.PLAYER_HEAD);

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
            // 公會選單常帶成員名稱；隊伍與好友介面則是<b>整格就是一個帳號名</b>
            // （玩家頭顱的標題），那種片語比對抓不到——實機收到的 captured.json
            // 裡就混進了六個玩家與公會名。見 PlayerDataFilter#looksAccountNamed。
            if (first && skipTitle) {
                WynnChaYuan.store().noteEvent("gui.blocked.playerData");
                first = false;                 // 標題用掉了，下一行是內文不是標題
                continue;
            }
            if (PlayerDataFilter.carriesPlayerData(template)
                    || PlayerDataFilter.looksAccountNamed(template)) {
                WynnChaYuan.store().noteEvent("gui.blocked.playerData");
                continue;
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
