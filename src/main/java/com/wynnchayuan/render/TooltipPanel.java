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
        // 記下這一塊在哪，截圖才知道要裁哪裡。名字取自譯文第一行——
        // 那通常就是物品名稱，當檔名剛好。
        String title = lines.isEmpty() ? null : lines.get(0).getString();
        // 截圖要框住的是<b>整個 tooltip</b>，包含它自己的底色與框線。
        // Minecraft 的 tooltip 從 (x, y) 開始畫文字，底色往外多 3px，
        // 所以左上角要退 4px、寬高各多留一點，否則圖的四邊會缺一條。
        PanelShot.note(x - SHOT_MARGIN, y - SHOT_MARGIN,
                panelW + SHOT_MARGIN, panelH + SHOT_MARGIN, title);
        // 自動模式的判別依據是<b>整份內容</b>而不是標題：同名的裝備會因為
        // 詞條不同而有不同的譯文，只看標題會只拍到第一件。
        PanelShot.auto(String.join("\n",
                lines.stream().map(Component::getString).toList()));
    }

    /**
     * 截圖往外多框幾個像素。
     *
     * <p>Minecraft 的 tooltip 底色比文字區往外多 3px，框線再多 1px。
     * 只框文字區的話，拍出來的圖四邊都缺一條，看起來像被裁壞了。
     */
    private static final int SHOT_MARGIN = 4;

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
    public static List<Component> translateLines(List<Component> tooltip, TranslationStore store) {
        int n = tooltip.size();
        List<Component> out = new ArrayList<>(n);
        boolean anyTranslated = false;

        // 置中與靠左在單獨一行上長得一模一樣，得看整塊才分得出來
        boolean[] centered = BlockLayout.centered(tooltip);

        List<StyledText> styled = new ArrayList<>(n);
        for (Component line : tooltip) {
            styled.add(StyledText.fromComponent(line));
        }

        // 第二欄靠左還是靠右，一行看不出來，得看整份 tooltip
        boolean leftAligned = LineTranslator.columnsAreLeftAligned(styled);

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
            // 從最長試到兩行都沒中，才記一筆。記在這裡而不是 translateBlock 裡面，
            // 是因為那邊每試一個長度就會記一次——一份八行的素材清單灌十九筆進去，
            // 真正想查的那一段就永遠排不進診斷檔。
            //
            // 光是移到這裡還不夠。素材清單的每一行都查不到，而每一行都是一個新的
            // 起點——同一份清單以「整段往後挪一行」的方式被記了十幾次，
            // 二十個名額全被它吃光，使用者真正要查的那一段照樣看不到。
            //
            // 會被自動斷行的段落一定<b>接在空行後面，或從第一行開始</b>，
            // 所以起點落在段落中間的那些窗格本來就不是要查的東西，不必記。
            boolean paragraphStart =
                    i == 0 || styled.get(i - 1).getString().isBlank();
            if (longest >= 2 && paragraphStart) {
                StringBuilder key = new StringBuilder();
                for (int k = i; k < i + longest; k++) {
                    if (k > i) {
                        key.append(System.lineSeparator());
                    }
                    key.append(com.wynnchayuan.capture.LineParts.of(styled.get(k)).template());
                }
                LineTranslator.noteBlockMiss(key.toString(), store);
            }
            Component translated =
                    LineTranslator.translate(styled.get(i), store, centered[i],
                                             leftAligned);
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

    /**
     * 就地取代要用的版本：翻得到就換掉，翻不到的行<b>保留原文</b>。
     *
     * <p>與面板走的是同一套 {@link #translateLines}。先前就地取代自己寫了一個
     * 逐行迴圈，<b>沒有跨行查表</b>——Major ID 與技能敘述那類原文本來就是一整段，
     * 逐行永遠查不到，於是面板模式看得到中文、就地取代看到的還是英文。
     * 同一份語料兩種結果，那是最難查的一種問題。
     */
    public static List<Component> translateInPlace(List<Component> tooltip,
                                                   TranslationStore store) {
        List<Component> translated = translateLines(tooltip, store);
        return translated.isEmpty() ? List.of() : translated;
    }

    // 曾經在這裡用 Font.split 把過寬的面板折行，但那會把 Nori、Wynnpool 那類
    // 模組畫的<b>進度條</b>拆掉——那些條是一整串排版符號，中間斷開就散了。
    // 譯文過寬要處理，但不能用「照寬度硬折」這種對內容一無所知的做法。

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
