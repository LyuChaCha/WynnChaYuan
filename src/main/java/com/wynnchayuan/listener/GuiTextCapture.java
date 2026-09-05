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

        // 先把整份攤成模板再決定要不要收。
        //
        // 「這是不是一張隊伍卡」看的是<b>整份</b>——卡片上那一行伺服器世界
        // （World: NA{~}）是遊戲自己排的，而標題是玩家打的字。逐行看的時候
        // 標題還沒遇到那一行，判斷不出來。見 PlayerDataFilter#isPartyCard。
        List<String> templates = new java.util.ArrayList<>(tooltip.size());
        for (Component line : tooltip) {
            StyledText styled = StyledText.fromComponent(line);
            if (GlyphSplitter.isGlyphOnly(styled)) {
                templates.add(null);           // 純圖示的分隔行
                continue;
            }
            templates.add(GlyphSplitter.toTemplate(styled));
        }
        // 隊伍名是玩家自己取的，跟帳號名一樣不進共享語料。
        skipTitle = skipTitle || PlayerDataFilter.isPartyCard(templates);
        // 整段翻好了的，它的每一行都不是缺口。見 #covered。
        boolean[] covered = covered(templates, GuiTextCapture::whole);

        boolean first = true;
        for (int i = 0; i < templates.size(); i++) {
            String template = templates.get(i);
            if (template == null) {
                continue;                      // 純圖示的分隔行
            }
            if (covered[i]) {
                WynnChaYuan.store().noteEvent("gui.skipped.inBlock");
                first = false;                 // 標題用掉了，下一行是內文
                continue;
            }
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

    /**
     * 哪幾行屬於<b>整段已經翻好</b>的段落。
     *
     * <h2>為什麼需要這個</h2>
     * tooltip 的一段話在畫面上是被寬度切成好幾行的，而語料收的是<b>整段</b>：
     *
     * <pre>
     *   畫面  Contains one of several
     *         exclusive ingredients in
     *         high quantities.
     *   語料  Contains one of several exclusive ingredients in high quantities.
     *         內含數種專屬素材之一，數量豐沛。
     * </pre>
     *
     * 逐行問「這一行翻了沒」，三行都是「沒有」——於是整段明明翻好了，
     * captured.json 還是把三行都列成缺口。實機那一份 123 條裡有 70 條是這種。
     *
     * <p>後果不只是雜訊：翻譯團隊照著清單補，補出來的是<b>逐行</b>條目，
     * 而逐行條目會蓋掉整段那條路，畫面上就成了半中半英。
     *
     * <p>整段是什麼，這裡<b>知道</b>——同一份 tooltip 裡連續的幾行就是一段，
     * 中間隔著純圖示或空白行。所以在這裡問一次整段，比在別處猜便宜也準。
     * 空白接法與換行接法都問：語料兩種形狀都有。
     */
    static boolean[] covered(List<String> templates,
                             java.util.function.Predicate<List<String>> known) {
        boolean[] out = new boolean[templates.size()];
        int from = 0;
        while (from < templates.size()) {
            String line = templates.get(from);
            if (line == null || line.isBlank()) {
                from++;
                continue;
            }
            int to = from;
            while (to < templates.size() && templates.get(to) != null
                    && !templates.get(to).isBlank()) {
                to++;
            }
            if (to - from > 1 && known.test(templates.subList(from, to))) {
                for (int i = from; i < to; i++) {
                    out[i] = true;
                }
            }
            from = to;
        }
        return out;
    }

    /** 這幾行接起來，語料裡有沒有。見 {@link #covered}。 */
    private static boolean whole(List<String> lines) {
        return WynnChaYuan.translations().hasTranslation(String.join(" ", lines))
                || WynnChaYuan.translations().hasTranslation(String.join("\n", lines));
    }
}
