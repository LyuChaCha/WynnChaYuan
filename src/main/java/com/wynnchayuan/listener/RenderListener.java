package com.wynnchayuan.listener;

import com.wynnchayuan.WynnChaYuan;
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
 * 把翻譯面板畫出來。
 *
 * <p>訂閱 {@code Pre} 只為了<b>讀取</b>那一份 tooltip 行列表——{@code Post} 拿不到它。
 * 我們絕不呼叫 {@code setTooltips}，所以原始 tooltip 完全不受影響，
 * 也不會干擾其他同樣掛在這個事件上的 mod（例如 WynnScribe）。
 *
 * <p>用 {@link EventPriority#LOWEST} 是為了拿到<b>所有 mod 都改完之後</b>的最終內容。
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
        // 裝備與技能刻意「只顯示、不收集」。
        // 裝備與技能的文案可以從官方 CDN 完整離線取得（見 corpus/，9,063 條），
        // 在遊戲裡逐件掃描是重工，而且永遠不確定掃齊了沒有。
        // 收集只留給靜態資料拿不到的東西：對話、聊天、名牌、任務追蹤。
        if (!WynnChaYuan.config().showPanel()) {
            return;
        }
        // 只記錄，不在這裡畫 —— Pre 早於原始 tooltip，畫了會被蓋掉。
        // 實際繪製在 renderAfterScreen()，見 PendingTooltip 的說明。
        PendingTooltip.set(event.getTooltips(), event.getMouseX(), event.getMouseY());
    }

    /**
     * 整個畫面（含原始 tooltip）都畫完之後才畫翻譯面板。
     *
     * <p>由 {@code ScreenEvents.afterRender} 每幀呼叫。
     */
    public static void renderAfterScreen(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!WynnChaYuan.config().showPanel()) {
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
        }
    }

    /** 對話小框由 HUD 每幀呼叫，見 {@link DialogueOverlay#render}。 */
    public static void renderHud(GuiGraphics graphics) {
        if (!WynnChaYuan.config().showPanel()) {
            return;
        }
        try {
            DialogueOverlay.render(graphics);
            TrackerOverlay.render(graphics);
            LookAtTranslator.render(graphics);
        } catch (Throwable t) {
            WynnChaYuan.store().noteEvent("render.dialogueError");
        }
    }
}
