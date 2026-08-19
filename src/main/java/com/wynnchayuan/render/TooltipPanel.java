package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.List;

/**
 * 在原始 tooltip 旁邊畫一個翻譯面板。
 *
 * <p><b>原始 tooltip 完全不動</b>——遊戲照常畫它，所以它的格式、材質包符號、
 * 排版全都原封不動，零破圖風險。我們只是在旁邊多畫一塊。
 *
 * <p>逐行對照：譯文與原文行號 1:1 對齊，沒翻到的行顯示灰色原文。
 * 這讓「翻譯還不完整」這個必然狀態不會破版，也看得出進度。
 */
public final class TooltipPanel {


    /** 上一次做過診斷計數的 tooltip，用來避免每幀重複計數。 */
    private static List<Component> lastDiagnosed = List.of();

    private TooltipPanel() {}

    /**
     * 畫出翻譯面板。
     *
     * @param tooltip 原始 tooltip 的每一行（我們只讀不改）
     */
    public static void render(GuiGraphics graphics, List<Component> tooltip,
                              int mouseX, int mouseY, TranslationStore store) {
        if (tooltip == null || tooltip.isEmpty()) {
            return;
        }
        // 別的模組加進清單、但 Wynntils 沒有畫出來的區塊要先拿掉，
        // 否則面板會比原文多出好幾行，看起來像我們憑空生了內容。
        tooltip = ThirdPartySections.strip(tooltip);
        List<Component> lines = translateLines(tooltip, store);
        // 只在物品換了才記一次，否則每幀都記會灌爆計數
        if (!tooltip.equals(lastDiagnosed)) {
            lastDiagnosed = List.copyOf(tooltip);
            WynnChaYuan.store().noteEvent(lines.isEmpty() ? "panel.noMatch" : "panel.shown");
            if (lines.isEmpty()) {
                TooltipDebug.dump(tooltip);   // 把實際模板寫出來，才比對得出差在哪
            }
        }
        if (lines.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        int gap = WynnChaYuan.config().panelGap();
        int panelW = width(mc, lines) + 8;      // 8 = tooltip 左右內距
        int panelH = lines.size() * 10 + 8;

        int x;
        int y;
        if (WynnChaYuan.config().panelAnchor() == CollectorConfig.PanelAnchor.FIXED) {
            x = WynnChaYuan.config().fixedX();
            y = WynnChaYuan.config().fixedY();
        } else {
            // 原 tooltip 的位置：Minecraft 預設畫在滑鼠右下方 12/-12 處
            int originalW = width(mc, tooltip) + 8;
            int originalLeft = mouseX + 12;
            int rightSide = originalLeft + originalW + gap;
            int leftSide = originalLeft - gap - panelW;

            x = switch (WynnChaYuan.config().panelSide()) {
                case RIGHT -> rightSide;
                case LEFT -> leftSide;
                case AUTO -> rightSide + panelW <= screenW
                        ? rightSide
                        : (leftSide >= 0 ? leftSide : Math.max(0, screenW - panelW));
            };
            y = mouseY - 12;
        }
        // 夾在畫面內，避免超出邊界時 Minecraft 自動換行導致文字重疊
        x = Math.max(0, Math.min(x, Math.max(0, screenW - panelW)));
        y = Math.max(0, Math.min(y, Math.max(0, screenH - panelH)));

        List<ClientTooltipComponent> components = lines.stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .toList();

        // 位置我們自己算好了，所以用一個原樣回傳座標的 positioner，
        // 不讓 Minecraft 的預設定位器把面板挪回滑鼠旁邊。
        graphics.renderTooltip(mc.font, components, x, y, EXACT, null);
    }

    /** 原樣採用呼叫端算好的座標。 */
    private static final ClientTooltipPositioner EXACT =
            (screenWidth, screenHeight, x, y, tooltipWidth, tooltipHeight) -> new Vector2i(x, y);

    /** 逐行翻譯；查不到的行轉成灰色原文。 */
    private static List<Component> translateLines(List<Component> tooltip, TranslationStore store) {
        List<Component> out = new ArrayList<>(tooltip.size());
        boolean anyTranslated = false;

        for (Component line : tooltip) {
            StyledText styled = StyledText.fromComponent(line);
            Component translated = LineTranslator.translate(styled, store);
            if (translated != null) {
                anyTranslated = true;
                out.add(translated);
            } else {
                out.add(LineTranslator.untranslated(styled));
            }
        }
        // 一行都沒翻到就別畫了，不然只是把原文再灰色複製一遍
        return anyTranslated ? out : List.of();
    }

    private static int width(Minecraft mc, List<Component> lines) {
        int max = 0;
        for (Component line : lines) {
            max = Math.max(max, mc.font.width(line));
        }
        return max;
    }

    /** 保留給之後需要精確量測多行元件高度時使用。 */
    static int height(List<ClientTooltipComponent> components) {
        int h = 0;
        for (ClientTooltipComponent c : components) {
            h += c.getHeight(Minecraft.getInstance().font);
        }
        return h;
    }
}
