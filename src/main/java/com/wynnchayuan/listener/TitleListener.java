package com.wynnchayuan.listener;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynnchayuan.translate.LineTranslator;
import com.wynntils.core.text.StyledText;
import com.wynntils.mc.event.SubtitleSetTextEvent;
import com.wynntils.mc.event.TitleSetTextEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * 螢幕正中央那行大字（title）與它下面那行（subtitle）。
 *
 * <h2>為什麼只能就地取代</h2>
 * 這兩行是<b>畫面中央的大字</b>，兩三秒就消失，旁邊也沒有空間再開一個小框。
 * 「YOU ARE AFK」「Move to continue」、活動開始與擊殺提示都走這裡。
 * 所以這一項沒有「另開面板」的選項，只有開與關。
 *
 * <h2>怎麼換掉</h2>
 * Wynntils 的事件只給得到 {@code getComponent()}，沒有 setter，但它是可取消的。
 * 所以做法是：取消掉原本那一次，再自己呼叫一次 {@code Gui.setTitle}——
 * 那會<b>再次觸發同一個事件</b>，所以要有旗標擋住，否則會無限遞迴。
 */
public final class TitleListener {

    /** 自己送出去的那一次不要再處理。見類別說明。 */
    private static boolean sending = false;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTitle(TitleSetTextEvent event) {
        Component hit = swap(event.getComponent());
        if (hit == null) {
            return;
        }
        event.setCanceled(true);
        send(() -> Minecraft.getInstance().gui.setTitle(hit));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onSubtitle(SubtitleSetTextEvent event) {
        Component hit = swap(event.getComponent());
        if (hit == null) {
            return;
        }
        event.setCanceled(true);
        send(() -> Minecraft.getInstance().gui.setSubtitle(hit));
    }

    private static void send(Runnable action) {
        sending = true;
        try {
            action.run();
        } finally {
            sending = false;
        }
    }

    /**
     * @return 譯好的那一行；不該動或查不到時回傳 {@code null}（原文照舊）
     */
    private static Component swap(Component original) {
        if (sending || original == null || !WynnChaYuan.config().translateTitles()) {
            return null;
        }
        StyledText line = StyledText.fromComponent(original);
        if (line.isEmpty() || GlyphSplitter.isGlyphOnly(line)) {
            return null;
        }
        // 別的模組靠這一行的英文判斷遊戲狀態，翻掉會害它算錯。見 ThirdPartyLiterals。
        //
        // 這一項特別要緊，因為我們是<b>取消原事件再自己送一次</b>：翻掉之後
        // 對方連原本那一行都不一定看得到。
        if (com.wynnchayuan.render.ThirdPartyLiterals.reserved(
                line.getStringWithoutFormatting())) {
            return null;
        }
        String template = GlyphSplitter.toTemplate(line);
        if (PlayerDataFilter.carriesPlayerData(template)) {
            return null;             // 夾帶玩家名的（擊殺提示），原樣放過
        }
        if (WynnChaYuan.config().collect()) {
            // 沒收過的先記下來，語料才補得起來——這一行以前沒有任何地方收
            WynnChaYuan.store().record(template, "desc", "title", "title");
        }
        return LineTranslator.translate(line, WynnChaYuan.translations());
    }
}
