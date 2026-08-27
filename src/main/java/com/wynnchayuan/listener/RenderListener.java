package com.wynnchayuan.listener;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.LineTranslator;
import com.wynntils.core.text.StyledText;
import com.wynnchayuan.render.DialogueOverlay;
import com.wynnchayuan.render.PendingTooltip;
import com.wynnchayuan.render.TooltipPanel;
import com.wynnchayuan.render.LookAtTranslator;
import com.wynnchayuan.render.TrackerOverlay;
import com.wynntils.mc.event.ItemTooltipRenderEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * 把物品 tooltip 的翻譯畫出來，兩種呈現方式擇一。
 *
 * <h2>兩種模式的差別只在「畫到哪裡」</h2>
 * 訂閱 {@code Pre} 是為了拿到那一份 tooltip 行列表——{@code Post} 拿不到它。
 *
 * <ul>
 *   <li>{@code PANEL}：只<b>讀取</b>，不呼叫 {@code setTooltips}。原始 tooltip
 *       一個位元都不動，譯文另外畫在旁邊，英文原名還看得見——想跟老玩家講
 *       裝備名稱時就靠這個。也因為不改，完全不會干擾其他同樣掛在這個事件上
 *       的 mod（例如 WynnScribe）。</li>
 *   <li>{@code REPLACE}：呼叫 {@code setTooltips} 把譯文寫回去，遊戲直接畫譯文。
 *       畫面乾淨、不必左右對照，代價是看不到英文原文。</li>
 * </ul>
 *
 * <p>兩者共用同一套 {@link LineTranslator} 逐片段替換，所以圖示、顏色與欄位對齊
 * 的保真程度是一樣的；差別純粹是要不要保留原文。
 *
 * <p>用 {@link EventPriority#LOWEST} 是為了拿到<b>所有 mod 都改完之後</b>的最終內容；
 * 在 {@code REPLACE} 模式下這也表示我們是最後一個動它的人。
 */
public final class RenderListener {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTooltip(ItemTooltipRenderEvent.Pre event) {
        try {
            // 介面 tooltip 的收集（預設關閉，見 GuiTextCapture 的說明）
            GuiTextCapture.record(event.getTooltips());
        } catch (Throwable t) {
            WynnChaYuan.store().noteEvent("capture.guiError");
        }
        // 裝備與技能刻意「只顯示、不收集」——文案可以從官方 CDN 完整離線取得，
        // 在遊戲裡逐件掃描是重工。收集只留給靜態資料拿不到的東西。
        CollectorConfig.TooltipMode mode = WynnChaYuan.config().tooltipMode();
        if (mode == CollectorConfig.TooltipMode.OFF) {
            return;
        }
        if (mode == CollectorConfig.TooltipMode.REPLACE) {
            replaceInPlace(event);
            return;
        }
        // 只記錄，不在這裡畫 —— Pre 早於原始 tooltip，畫了會被蓋掉。
        // 實際繪製在 renderAfterScreen()，見 PendingTooltip 的說明。
        PendingTooltip.set(event.getTooltips(), event.getMouseX(), event.getMouseY());
    }

    /**
     * 就地取代：把譯文寫回事件，讓遊戲直接畫譯文而不是原文。
     *
     * <p>用的是與側邊面板<b>完全相同</b>的逐片段替換，所以格式保真程度一樣——
     * 差別只在畫到哪裡。想看英文原名的人用面板模式，想要畫面乾淨的用這個。
     */
    private void replaceInPlace(ItemTooltipRenderEvent.Pre event) {
        try {
            List<Component> original = event.getTooltips();
            if (original == null || original.isEmpty()) {
                return;
            }
            // 跟側邊面板<b>完全同一套</b>，包含跨行查表。先前這裡自己寫了一個
            // 逐行迴圈，Major ID 與技能敘述那種一整段的原文永遠查不到，
            // 於是同一件物品在面板模式有中文、在就地取代模式沒有。
            List<Component> out = com.wynnchayuan.render.TooltipPanel
                    .translateInPlace(original, WynnChaYuan.translations());
            if (!out.isEmpty()) {
                event.setTooltips(out);
                WynnChaYuan.store().noteEvent("tooltip.replaced");
            }
        } catch (Throwable t) {
            WynnChaYuan.store().noteEvent("render.replaceError");
            com.wynnchayuan.translate.ErrorDebug.note("tooltip.replace", firstLine(event), t);
        }
    }

    /** 出事時記一下當時在處理哪件物品，光有堆疊很難重現。 */
    private static String firstLine(ItemTooltipRenderEvent.Pre event) {
        List<Component> lines = event.getTooltips();
        return lines == null || lines.isEmpty() ? null : lines.get(0).getString();
    }

    /**
     * 整個畫面（含原始 tooltip）都畫完之後才畫翻譯面板。
     *
     * <p>由 {@code ScreenEvents.afterRender} 每幀呼叫。
     */
    public static void renderAfterScreen(GuiGraphics graphics, int mouseX, int mouseY) {
        if (WynnChaYuan.config().tooltipMode() != CollectorConfig.TooltipMode.PANEL) {
            PendingTooltip.take();          // 關掉時也要清，否則會留一格殘影
            return;
        }
        List<Component> tooltip = PendingTooltip.take();
        if (tooltip.isEmpty()) {
            return;
        }
        try {
            TooltipPanel.render(graphics, tooltip,
                    PendingTooltip.mouseX(), PendingTooltip.mouseY(),
                    WynnChaYuan.translations());
        } catch (Throwable t) {
            // 渲染路徑上出錯絕不能讓遊戲崩潰，最多就是這一幀沒畫出面板
            WynnChaYuan.store().noteEvent("render.tooltipError");
            com.wynnchayuan.translate.ErrorDebug.note("tooltip.panel",
                    tooltip.isEmpty() ? null : tooltip.get(0).getString(), t);
        }
        // 待拍的截圖收在這裡。
        //
        // 之前收在 renderHud 尾巴，那是<b>錯的</b>：Minecraft 是先畫 HUD
        // 再畫畫面上的 GUI（GameRenderer 裡 gui.render() 在 screen.render()
        // 之前），而翻譯面板掛在 screen 之後。於是截圖拍到的是
        // 「世界＋HUD、物品欄還沒畫」的那一瞬間——使用者拿到的是一張地板。
        com.wynnchayuan.render.PanelShot.afterRender();
    }

    /** 對話小框由 HUD 每幀呼叫，見 {@link DialogueOverlay#render}。 */
    public static void renderHud(GuiGraphics graphics) {
        // 就地取代不受「小框」總開關管——那個開關是在關小框，而就地取代
        // 已經不是小框了。原文那時已經被藏掉，這裡再不畫就是一片空白。
        boolean inPlace = WynnChaYuan.config().dialogueMode()
                == CollectorConfig.DialogueMode.REPLACE;
        if (!WynnChaYuan.config().showOverlays() && !inPlace) {
            return;
        }
        try {
            DialogueOverlay.render(graphics);
            if (!WynnChaYuan.config().showOverlays()) {
                return;                        // 只放行對話，其餘小框照關
            }
            TrackerOverlay.render(graphics);
            LookAtTranslator.render(graphics);
        } catch (Throwable t) {
            WynnChaYuan.store().noteEvent("render.dialogueError");
        }
        // 沒有畫面開著時（例如只想拍對話小框），HUD 就是最後一層，收在這裡。
        // 有畫面開著時交給 renderAfterScreen——見那邊的說明。
        if (net.minecraft.client.Minecraft.getInstance().screen == null) {
            com.wynnchayuan.render.PanelShot.afterRender();
        }
    }
}
