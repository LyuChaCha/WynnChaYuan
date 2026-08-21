package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.BlockLayout;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
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

        // 譯文可能比原文寬得多（技能說明尤其明顯）。不設上限的話面板會一路長到
        // 畫面外，被 Minecraft 自己的邊界處理攔下來重排，文字就疊在一起了。
        //
        // 折行交給 Font.split：它認得樣式與字型，折出來的每一段都保留原本的顏色。
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        int limit = Math.max(MIN_PANEL_WIDTH, screenW * MAX_PANEL_PERCENT / 100);
        for (Component line : lines) {
            List<FormattedCharSequence> parts = mc.font.split(line, limit);
            if (parts.isEmpty()) {
                wrapped.add(FormattedCharSequence.EMPTY);   // 空行要留著，那是分段
            } else {
                wrapped.addAll(parts);
            }
        }

        int panelW = widthOf(mc, wrapped) + 8;  // 8 = tooltip 左右內距
        int panelH = wrapped.size() * 10 + 8;

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

        List<ClientTooltipComponent> components = wrapped.stream()
                .map(ClientTooltipComponent::create)
                .toList();

        // 位置我們自己算好了，所以用一個原樣回傳座標的 positioner，
        // 不讓 Minecraft 的預設定位器把面板挪回滑鼠旁邊。
        graphics.renderTooltip(mc.font, components, x, y, EXACT, null);
    }

    /** 原樣採用呼叫端算好的座標。 */
    private static final ClientTooltipPositioner EXACT =
            (screenWidth, screenHeight, x, y, tooltipWidth, tooltipHeight) -> new Vector2i(x, y);

    /**
     * 翻譯整份 tooltip；查不到的行轉成灰色原文。
     *
     * <p>先試<b>整段</b>再退回逐行。技能說明那類的原文本來就是一整段
     * （見 {@link LineTranslator#translateBlock}），逐行查表永遠查不到。
     * 由長到短試，讓涵蓋比較多行的條目優先——短的那些多半只是碰巧撞上。
     */
    private static List<Component> translateLines(List<Component> tooltip, TranslationStore store) {
        int n = tooltip.size();
        List<Component> out = new ArrayList<>(n);
        boolean anyTranslated = false;

        // 置中與靠左在單獨一行上長得一模一樣，得看整塊才分得出來
        boolean[] centered = BlockLayout.centered(tooltip);

        List<StyledText> styled = new ArrayList<>(n);
        for (Component line : tooltip) {
            styled.add(StyledText.fromComponent(line));
        }

        int i = 0;
        while (i < n) {
            int longest = Math.min(store.maxBlockLines(), n - i);
            List<Component> block = null;
            int used = 0;
            for (int len = longest; len >= 2 && block == null; len--) {
                boolean[] slice = new boolean[len];
                System.arraycopy(centered, i, slice, 0, len);
                block = LineTranslator.translateBlock(
                        styled.subList(i, i + len), store, slice);
                used = len;
            }
            if (block != null) {
                out.addAll(block);
                anyTranslated = true;
                i += used;
                continue;
            }
            Component translated =
                    LineTranslator.translate(styled.get(i), store, centered[i]);
            if (translated != null) {
                anyTranslated = true;
                out.add(translated);
            } else {
                out.add(LineTranslator.untranslated(styled.get(i)));
            }
            i++;
        }
        // 一行都沒翻到就別畫了，不然只是把原文再灰色複製一遍
        return anyTranslated ? out : List.of();
    }

    /** 面板最寬佔畫面的百分之幾。再寬就把原文擠出畫面了。 */
    private static final int MAX_PANEL_PERCENT = 38;

    /** 畫面很窄時的下限，免得折成一個字一行。 */
    private static final int MIN_PANEL_WIDTH = 160;

    private static int widthOf(Minecraft mc, List<FormattedCharSequence> lines) {
        int max = 0;
        for (FormattedCharSequence line : lines) {
            max = Math.max(max, mc.font.width(line));
        }
        return max;
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
