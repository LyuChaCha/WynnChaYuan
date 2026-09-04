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

        int[] panelBox = box(mc, lines);
        int panelW = panelBox[0] + 8;           // 8 = tooltip 左右內距
        int panelH = panelBox[1] + 8;

        int x;
        int y;
        if (WynnChaYuan.config().panelAnchor() == CollectorConfig.PanelAnchor.FIXED) {
            x = WynnChaYuan.config().fixedX();
            y = WynnChaYuan.config().fixedY();
        } else {
            // 原 tooltip 的位置：Minecraft 預設畫在滑鼠右下方 12/-12 處
            int originalW = box(mc, tooltip)[0] + 8;
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
                panelBox[0] + SHOT_MARGIN * 2, panelBox[1] + SHOT_MARGIN * 2, title);
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

        // 每一行有沒有換成中文。用來把「同一段翻一半」抓回來，見 evenOut。
        boolean[] hit = new boolean[n];

        int i = 0;
        // 物品名稱那一行：還沒翻的裝備名一律保持原文。
        //
        // 名稱後面可能掛著 Wynntils 加的註記，所以要先剝掉——見 #bareName。
        //
        // 裝備名是專有名詞，而語料裡有 50 組裝備名跟別的領域撞名（技能詞、
        // Major ID、介面詞）。沒有這一道，神話矛「Guardian」會拿到同名 Major ID
        // 的「守護者」。
        //
        // 擋在<b>這裡</b>而不是查表層，是因為同一個字在別的位置可能完全正當：
        // 有一把裝備叫 Reflection，在查表層擋掉會連屬性列的「遠程反傷」一起擋死。
        String gearName = n == 0 ? null : bareName(
                com.wynnchayuan.capture.LineParts.of(styled.get(0)).template());
        if (gearName != null && store.isBareGearName(gearName)) {
            out.add(LineTranslator.untranslated(styled.get(0)));
            i = 1;
            // 名稱其實是<b>兩行</b>：第 0 行寬度是 0，玩家看不到；
            // 看得到的名字在第 1 行。實機 layout-debug 記得很清楚：
            //
            // <pre>
            //   [0] 內容   0 px  󏀀Immolation󏀀
            //   [1] 內容 125 px  󏿰󏿏󐀅Immolation [66.3%]
            // </pre>
            //
            // 只擋第 0 行等於擋了看不見的那一行，第 1 行照樣落到逐段那條路，
            // 被詞彙表換掉——神話矛「Guardian」拿到同名 Major ID 的「守護者」
            // 就是這樣來的，而 Guardian 其實早就在 gearNameKeys 裡了。
            //
            // 條件收得很緊：第 1 行剝掉裝飾之後必須<b>就是同一個名字</b>。
            // 素材不在 gearNameKeys（ingredient.json 標了 gearNames:false），
            // 技能樹的節點標題也不是裝備名，兩者都碰不到這一段。
            if (n > 1 && gearName.equals(bareName(
                    com.wynnchayuan.capture.LineParts.of(styled.get(1)).template()))) {
                out.add(LineTranslator.untranslated(styled.get(1)));
                i = 2;
            }
        }
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
                for (int k = i; k < i + used; k++) {
                    hit[k] = true;
                }
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
                hit[i] = true;
                out.add(translated);
            } else {
                out.add(LineTranslator.untranslated(styled.get(i)));
            }
            i++;
        }
        // 同一段不能一半中文一半英文，見 evenOut。
        List<String> plain = new ArrayList<>(n);
        for (StyledText line : styled) {
            plain.add(com.wynnchayuan.capture.GlyphSplitter
                    .stripGlyphChars(line.getString()).strip());
        }
        if (evenOut(plain, hit)) {
            anyTranslated = false;
            for (int k = 0; k < n; k++) {
                if (hit[k]) {
                    anyTranslated = true;
                } else {
                    out.set(k, LineTranslator.untranslated(styled.get(k)));
                }
            }
        }
        // 一行都沒翻到就別畫了，不然只是把原文再灰色複製一遍
        if (!anyTranslated) {
            return List.of();
        }
        // 整塊拉正。中文不能跟著斜（會糊），但只拉正中文那幾行會讓同一份說明
        // 裡「翻好的正、沒翻的斜」參差不齊——見 LineTranslator#unslantAll。
        out.replaceAll(LineTranslator::unslantAll);
        return out;
    }

    /**
     * 同一段話不能一半中文一半英文。
     *
     * <h2>為什麼會發生</h2>
     * 說明文字是被 tooltip 的<b>寬度</b>折成好幾行的，而語料裡很多段落是
     * 一行一條記下來的——那等於把譯文釘死在<b>翻譯者當時的斷行</b>上。
     * 玩家的介面縮放不一樣、或句子裡的數字多一位數，斷點就跟著跑：
     * 有幾行對得上、有幾行對不上，畫面上就成了中英夾雜的一段。
     *
     * <p>實測倉庫裡有 83 條這種「只有半句、而且沒有整段條目罩著」的譯文，
     * 散在九個檔案裡。把它們併成整段是對的長期做法，但併錯了會產生
     * <b>錯誤的譯文</b>——試過自動併，大約每五段就有一段分組是錯的，
     * 那比中英夾雜更糟。所以先在算繪這一端擋住症狀：整段要嘛全中文，
     * 要嘛全英文。
     *
     * <p>這跟模組既有的規則是同一條：「一句話裡插幾個方框，比整句留著英文
     * 糟糕得多」。中英夾雜也是一樣的道理。
     *
     * <h2>怎麼認段落</h2>
     * 看<b>畫面上的原文</b>，不是看語料——原文就是實際的斷行結果，最準：
     * 上一行沒有收尾（結尾不是句讀），而這一行是小寫開頭，那兩行就是同一句。
     * 欄位列（{@code Health +120}）、標題、圖示列都不會符合這個形狀。
     *
     * @param plain 每一行剝掉圖示之後的純文字
     * @param hit   每一行有沒有換成中文；<b>會被就地改掉</b>
     * @return 有沒有動到東西
     */
    static boolean evenOut(List<String> plain, boolean[] hit) {
        boolean changed = false;
        int i = 0;
        while (i < hit.length) {
            int end = i + 1;
            while (end < hit.length && continues(plain.get(end - 1), plain.get(end))) {
                end++;
            }
            if (end - i > 1) {
                boolean some = false;
                boolean all = true;
                for (int k = i; k < end; k++) {
                    some |= hit[k];
                    all &= hit[k];
                }
                if (some && !all) {
                    for (int k = i; k < end; k++) {
                        hit[k] = false;
                    }
                    changed = true;
                }
            }
            i = end;
        }
        return changed;
    }

    /** 下一行是不是<b>接續</b>上一行的同一句話。 */
    private static boolean continues(String prev, String line) {
        if (prev.isEmpty() || line.isEmpty()) {
            return false;
        }
        char end = prev.charAt(prev.length() - 1);
        if (end == '.' || end == '!' || end == '?' || end == ':' || end == ';') {
            return false;
        }
        char start = line.charAt(0);
        return start >= 'a' && start <= 'z';
    }

    /**
     * 就地取代模式的截圖框。
     *
     * <p>截圖鍵要有東西可以裁，得先有人告訴它「這一幀的框在哪」。面板模式是
     * {@link #render} 畫完順手記的；就地取代模式<b>根本不會走到那裡</b>——
     * 譯文是寫回事件、由遊戲自己畫的。於是實機上按 F9 沒有反應，
     * 只有切成面板模式才拍得到。
     *
     * <p>這裡只記框、不畫東西。要在遊戲把 tooltip 畫完<b>之後</b>呼叫，
     * 記下的那一幀才是完整的。
     *
     * <p>框的算法跟遊戲畫 tooltip 的一樣：滑鼠右下 12/-12，寬度取最長那行，
     * 然後夾在畫面內。高度是估的（遊戲每行 10px 再加內距），所以寧可多框幾
     * 像素——多框到的是背景，少框到的是字。
     */
    public static void noteShot(GuiGraphics graphics, List<Component> tooltip,
                                int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null || tooltip == null || tooltip.isEmpty()) {
            return;
        }
        // 量<b>元件</b>而不是量文字。
        //
        // 先前是「最長那行的字寬」配上「行數 × 10」。兩個都不準：
        // tooltip 裡不是只有文字——物品圖示、屬性長條、空白列各有各的高度，
        // 而中文那幾行的寬度也不是 mc.font.width(Component) 算得出來的
        // （那條路不認得元件）。於是拍出來的框比內容大一截，四周夾著
        // 一片背包背景，使用者回報「圖片格式超出」。
        //
        // ClientTooltipComponent 是遊戲自己拿去畫的那個型別，問它就準。
        int[] shotBox = box(mc, tooltip);
        int w = shotBox[0];
        int h = shotBox[1];

        // 位置<b>問遊戲</b>，不要自己算。
        //
        // 先前是「滑鼠右下 12/-12，再夾在畫面內」。前半對，後半不對：
        // 畫面右緣放不下時遊戲是把 tooltip 翻到滑鼠<b>左邊</b>，不是把它
        // 推回畫面內。框於是整個偏掉，拍出來夾著一片背包背景。
        //
        // DefaultTooltipPositioner 就是遊戲畫 tooltip 時用的那一個，直接
        // 拿來算——之後版本改了規則，這裡跟著改，不會再各算各的。
        org.joml.Vector2ic at = net.minecraft.client.gui.screens.inventory.tooltip
                .DefaultTooltipPositioner.INSTANCE.positionTooltip(
                        graphics.guiWidth(), graphics.guiHeight(), mouseX, mouseY, w, h);
        int x = at.x();
        int y = at.y();
        PanelShot.note(x - SHOT_MARGIN, y - SHOT_MARGIN,
                       w + SHOT_MARGIN * 2, h + SHOT_MARGIN * 2,
                       tooltip.get(0).getString());
        PanelShot.auto(String.join("\n",
                tooltip.stream().map(Component::getString).toList()));
    }

    /**
     * 物品名稱那一行去掉裝飾之後的名字。
     *
     * <p>Wynntils 會在名稱後面掛自己的註記，最常見的是鑑定百分比：畫面上是
     * {@code Willow [67.5%]}，模板就成了 {@code Willow [{~}%]}。拿這個去比對
     * 裝備名清單當然對不上，於是<b>就地取代模式下裝備名還是被翻掉了</b>——
     * 實機回報的「Willow 變成柳木」就是這個（{@code npc.json} 裡的柳木是那棵樹，
     * 跟這件靴子撞名）。
     *
     * <p>剝的是<b>結尾</b>連續的方括號，因為註記一律掛在後面；名字本身帶方括號
     * 的裝備並不存在，而就算有，剝過頭也只是少擋一次，不會誤擋別的東西。
     */
    public static String bareName(String template) {
        String s = template
                .replace(com.wynnchayuan.capture.GlyphSplitter.GLYPH_PLACEHOLDER, "")
                .strip();
        while (s.endsWith("]")) {
            int open = s.lastIndexOf('[');
            if (open <= 0) {
                break;                         // 整行都是括號，那就不是名字
            }
            s = s.substring(0, open).strip();
        }
        return s;
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

    /**
     * 量出這一疊行畫出來有多大（回傳 {@code {寬, 高}}）。
     *
     * <p>問 {@link ClientTooltipComponent}——那是遊戲自己拿去畫的型別，
     * 問它就跟畫出來的一致。先前是「最長那行的字寬」配上「行數 × 10」，
     * 兩個都不準：tooltip 裡不是只有文字，物品圖示、屬性長條、
     * 空白列各有各的高度。拍出來的框因此跟內容對不上。
     */
    static int[] box(Minecraft mc, List<Component> lines) {
        List<ClientTooltipComponent> parts = lines.stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .toList();
        int w = 0;
        for (ClientTooltipComponent part : parts) {
            w = Math.max(w, part.getWidth(mc.font));
        }
        // 只有一個元件時遊戲會收掉那 2px 的行距，跟著收才不會多框一條。
        return new int[] {w, (parts.size() == 1 ? -2 : 0) + height(parts)};
    }

    /** 多行元件的總高度，見 {@link #box}。 */
    static int height(List<ClientTooltipComponent> components) {
        int h = 0;
        for (ClientTooltipComponent c : components) {
            h += c.getHeight(Minecraft.getInstance().font);
        }
        return h;
    }
}
