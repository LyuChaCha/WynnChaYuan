package com.wynnchayuan.client;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.render.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

// 版面常數統一由 SettingsLayout 定義——分兩份會再走上舊版各算各的老路。
import static com.wynnchayuan.client.SettingsLayout.FOOT_H;
import static com.wynnchayuan.client.SettingsLayout.PAD;
import static com.wynnchayuan.client.SettingsLayout.TAB_W;
import static com.wynnchayuan.client.SettingsLayout.TOP;

/**
 * F6 開啟的設定畫面。
 *
 * <h2>為什麼重做</h2>
 * 舊版是「左右兩張卡片、每個按鈕底下印一行說明」。三個問題：
 *
 * <ol>
 *   <li><b>找不到東西。</b>分類是〈顯示〉〈翻譯〉〈資料〉，但「物品翻譯」在顯示、
 *       「翻譯物品名稱」在翻譯、「名牌漂浮字」也在翻譯——按功能分聽起來合理，
 *       用起來要一欄一欄掃。</li>
 *   <li><b>說明擠在按鈕底下很亂。</b>每一列佔兩倍高，整片畫面都是小字。</li>
 *   <li><b>版面會跟方框對不上。</b>這是結構問題：按鈕在 {@code init()} 用
 *       {@code TOP + rowH()*N} 擺，說明在 {@code render()} 用<b>另一組</b>手算座標擺，
 *       卡片高度是第三條公式。三邊各算各的，實際上早就漂掉了——第 2 列的按鈕是
 *       「任務對話」，底下印的卻是「對話框停留秒數」，而第 5 到第 8 列
 *       <b>一行說明都沒有</b>。加一列就會再壞一次。</li>
 * </ol>
 *
 * <h2>現在怎麼做</h2>
 * <ul>
 *   <li>左邊分類、右邊只顯示那一類。分類照<b>「這東西出現在遊戲的哪裡」</b>取名
 *       （物品／面板／對話／世界與聊天／資料），不是照程式怎麼分。</li>
 *   <li>一列 = 左邊名稱、右邊控制項。說明改成<b>滑鼠移上去</b>時顯示在底下那一條，
 *       清單本身乾淨。</li>
 *   <li><b>說明跟著那一列走</b>（{@link Row} 同時帶名稱、說明與控制項），
 *       座標只算一次。結構上不可能再漂掉。</li>
 *   <li>放不下就可以滾。以後再加二十個設定也不會擠爆。</li>
 * </ul>
 *
 * <p>只用原版 {@link Button} 與 {@link EditBox}，不引入 Cloth Config／YACL——
 * 設定就這幾項，多一個依賴只是多一個會壞掉的東西。
 */
public final class SettingsScreen extends Screen {

    // ------------------------------------------------------------ 版面

    /** 版面算式全部在 {@link SettingsLayout}——那裡是純算術，測得到。 */
    private SettingsLayout box() {
        return new SettingsLayout(this.width, this.height, rows.size());
    }

    // ------------------------------------------------------------ 狀態

    /**
     * 目前選到第幾個分類，以及清單捲到哪。
     *
     * <p>做成靜態的：關掉再開回來時停在原地，調完一個設定不必再找一次。
     * 捲動以<b>列</b>為單位，不是像素——半列露在外面比較難看。
     */
    private static int tab = 0;

    private static int scroll = 0;

    private Component status = Component.empty();

    /**
     * 動作結果是<b>什麼時候</b>設的。
     *
     * <p>先前 status 設了就不清，而它的優先序在說明之上——按過一次「套用」之後，
     * 底下那條就被「✔ 已套用顏色」佔住，之後滑到任何一列都看不到說明了。
     * 使用者回報「希望還是可以保有說明」講的就是這個。
     *
     * <p>兩件事都要看得到，所以改成<b>輪流</b>：結果先顯示幾秒——按下去的當下
     * 滑鼠通常正壓在那一列上，說明先讓開——過了就換回說明。
     */
    private long statusAt = 0;

    /** 動作結果顯示多久。見 {@link #statusAt}。 */
    private static final long STATUS_MS = 4000;

    /** 設一則動作結果。 */
    private void say(Component text) {
        status = text;
        statusAt = System.currentTimeMillis();
    }

    /** 正在向 GitHub 抓譯文。抓多久不知道，這期間那條訊息不能被說明蓋掉。 */
    private boolean fetching = false;

    private boolean statusFresh() {
        if (status.getString().isEmpty()) {
            return false;
        }
        return fetching || System.currentTimeMillis() - statusAt < STATUS_MS;
    }
    private final List<Row> rows = new ArrayList<>();
    private EditBox colorBox;
    private EditBox gapBox;
    private EditBox dialogueHoldBox;
    private Button reloadButton;

    public SettingsScreen() {
        super(Component.literal("WynnChaYuan"));
    }

    // ------------------------------------------------------------ 一列

    /**
     * 一列設定。
     *
     * <p>名稱、說明與控制項<b>綁在同一個物件上</b>，這正是舊版漂掉的地方：
     * 那時候三樣東西分別在兩個方法裡各自手算座標。
     */
    private static final class Row {
        final String name;
        final String hint;
        final List<AbstractWidget> widgets = new ArrayList<>();
        int y;

        Row(String name, String hint) {
            this.name = name;
            this.hint = hint;
        }
    }

    /** 分類的名字。順序就是左邊那一排的順序。 */
    private static final String[] TAB_KEYS = {
        "tab.items", "tab.panel", "tab.dialogue", "tab.world", "tab.data",
    };

    /**
     * 每一類在管什麼，印在清單上面那一行。
     *
     * <p>分類名稱只有兩三個字，光看「面板」不知道裡面有什麼。多這一行就
     * 不用一個一個滑過去才知道自己找對地方沒有。
     */
    private static String tabName(int which) {
        return T.s(TAB_KEYS[which]);
    }

    /** 每一類在管什麼，印在清單上面那一行。 */
    private static String tabAbout(int which) {
        return T.s(TAB_KEYS[which] + ".about");
    }

    // ------------------------------------------------------------ 佈局

    private int paneW() {
        return box().paneW();
    }

    private int originX() {
        return box().originX();
    }

    private int paneX() {
        return box().paneX();
    }

    private int ctrlW() {
        return box().ctrlW();
    }

    private int perPage() {
        return box().perPage();
    }

    @Override
    protected void init() {
        rows.clear();
        buildRows();

        // 換分類之後列數變少，捲動位置要跟著收回來，不然會停在空白處。
        scroll = Math.max(0, Math.min(scroll, rows.size() - perPage()));

        int right = box().ctrlX();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int at = i - scroll;
            row.y = box().rowY(at);
            boolean shown = at >= 0 && at < perPage();
            for (AbstractWidget w : row.widgets) {
                // 建列時座標是「相對控制項左緣」的偏移，這裡才平移到實際位置。
                w.setX(w.getX() + right);
                w.setY(row.y);
                w.visible = shown;
                w.active = shown;
                addRenderableWidget(w);
            }
        }

        // ---- 左邊的分類 ----
        for (int i = 0; i < TAB_KEYS.length; i++) {
            int which = i;
            addRenderableWidget(Button.builder(
                    // 選到的那一類靠<b>左邊那條主題色</b>表示，不加「▸」——
                    // 加了字會被推右，跟其他幾個對不齊，一眼就看得出來歪。
                    Component.literal(Cards.fit(this.font, tabName(i), TAB_W - 8)),
                    b -> {
                        tab = which;
                        scroll = 0;
                        rebuildWidgets();
                    })
                    .bounds(originX(), box().rowY(i), TAB_W, 20).build());
        }

        // ---- 底部 ----
        //
        // 「更新說明」擺在這裡而不是藏進某一個分類：它講的是<b>整個模組</b>，
        // 不屬於物品、面板或對話任何一類；而有新版時那個提示要從 F6 一打開
        // 就看得到，不能要人先點對分類。
        int mid = this.width / 2;
        int left = mid - 148;
        addRenderableWidget(Button.builder(updateLabel(),
                b -> this.minecraft.setScreen(new ReleaseNotesScreen(this)))
                .bounds(left, this.height - 26, 92, 20).build());
        addRenderableWidget(Button.builder(T.c("button.credits"),
                b -> this.minecraft.setScreen(new CreditsScreen(this)))
                .bounds(left + 96, this.height - 26, 108, 20).build());
        addRenderableWidget(Button.builder(T.c("button.done"), b -> onClose())
                .bounds(left + 208, this.height - 26, 88, 20).build());
    }

    // ------------------------------------------------------------ 建一列

    private Row add(String name, String hint) {
        Row row = new Row(name, hint);
        rows.add(row);
        return row;
    }

    /** 一顆佔滿控制項那一半的按鈕（切換、循環都用這個）。 */
    /**
     * 大部分的列，名稱與說明都照同一個鍵取：{@code key} 與 {@code key + ".hint"}。
     *
     * <p>兩個字串各寫一次的話，改名時一定有一邊會被忘掉——而忘掉的那一邊
     * 不會編譯失敗，畫面上直接印出鍵名。
     */
    private void cycle(String key, Supplier<Component> label,
                       Consumer<Button> onPress) {
        cycleNamed(T.s(key), T.s(key + ".hint"), label, onPress);
    }

    /**
     * 名稱與說明都<b>已經是現成的字</b>的版本。
     *
     * <h2>為什麼不跟上面那支同名</h2>
     * 兩支只差一個參數，而多的那個也是 {@code String}。於是
     * {@code cycle("items.shot", hint, …)} 編得過、跑得動，只是畫面上那一列
     * 印出來的是 {@code items.shot} 四個字——那一列的名字就這樣消失了一版。
     * 取不同的名字，這種傳錯就變成編譯錯誤。
     */
    private void cycleNamed(String name, String hint,
                            Supplier<Component> label, Consumer<Button> onPress) {
        add(name, hint).widgets.add(Button.builder(label.get(), onPress::accept)
                .bounds(0, 0, ctrlW(), 20).build());
    }

    /** 按鈕 + 右邊一顆小的（進階…）。 */
    private void cycleWith(String key, Supplier<Component> label,
                           Consumer<Button> onPress, String extra, Runnable action) {
        cycleWithNamed(T.s(key), T.s(key + ".hint"), label, onPress, extra, action);
    }

    /** 見 {@link #cycleNamed}：名稱與說明都已經是現成的字。 */
    private void cycleWithNamed(String name, String hint, Supplier<Component> label,
                                Consumer<Button> onPress, String extra,
                                Runnable action) {
        Row row = add(name, hint);
        row.widgets.add(Button.builder(label.get(), onPress::accept)
                .bounds(0, 0, ctrlW() - 46, 20).build());
        row.widgets.add(Button.builder(Component.literal(extra), b -> action.run())
                .bounds(ctrlW() - 42, 0, 42, 20).build());
    }

    /** 輸入框 + 套用。 */
    private EditBox field(String key, String hint, String value, int max,
                          Runnable apply) {
        return fieldNamed(T.s(key), hint, value, max, apply);
    }

    private EditBox fieldNamed(String name, String hint, String value, int max,
                               Runnable apply) {
        Row row = add(name, hint);
        EditBox box = new EditBox(this.font, 0, 0, ctrlW() - 46, 20,
                Component.literal(name));
        box.setValue(value);
        box.setMaxLength(max);
        row.widgets.add(box);
        row.widgets.add(Button.builder(T.c("button.apply"), b -> apply.run())
                .bounds(ctrlW() - 42, 0, 42, 20).build());
        return box;
    }

    /** 只有一顆按鈕的一列（開子畫面、執行動作）。 */
    private Button action(String key, String hint, Component label, Runnable go) {
        return actionNamed(T.s(key), hint, label, go);
    }

    private Button actionNamed(String name, String hint, Component label, Runnable go) {
        Button button = Button.builder(label, b -> go.run())
                .bounds(0, 0, ctrlW(), 20).build();
        add(name, hint).widgets.add(button);
        return button;
    }

    // ------------------------------------------------------------ 每個分類有哪些列

    /**
     * 分類照<b>「這東西出現在遊戲的哪裡」</b>取名。
     *
     * <p>舊版按功能分成〈顯示〉〈翻譯〉〈資料〉，聽起來合理，但玩家想關掉
     * 名牌翻譯的時候不會知道那是〈翻譯〉還是〈顯示〉。改成照畫面上的位置分，
     * 不用看說明也找得到。
     */
    private void buildRows() {
        switch (tab) {
            case 0 -> items();
            case 1 -> panel();
            case 2 -> dialogue();
            case 3 -> world();
            default -> data();
        }
    }

    private void items() {
        cycle("items.tooltip",
                this::tooltipModeLabel, b -> {
                    WynnChaYuan.config().cycleTooltipMode();
                    b.setMessage(tooltipModeLabel());
                });
        cycle("items.names",
                this::itemNameLabel, b -> {
                    boolean on = WynnChaYuan.config().toggleItemNames();
                    WynnChaYuan.translations().setTranslateNames(on);
                    b.setMessage(itemNameLabel());
                });
        cycle("items.market",
                this::marketLabel, b -> {
                    WynnChaYuan.config().toggleMarketSearch();
                    b.setMessage(marketLabel());
                });
        String clash = com.wynnchayuan.render.PanelShot.conflict();
        cycleNamed(T.s("items.shot"),
                clash == null ? T.s("items.shot.hint") : T.s("items.shot.clash", clash),
                this::shotLabel, b -> {
                    WynnChaYuan.config().cycleShotMode();
                    b.setMessage(shotLabel());
                });
    }

    private void panel() {
        cycle("panel.anchor",
                this::anchorLabel, b -> {
                    WynnChaYuan.config().togglePanelAnchor();
                    b.setMessage(anchorLabel());
                });
        action("panel.place", T.s("panel.place.hint"), T.c("button.adjust"),
                () -> this.minecraft.setScreen(new PositionScreen(this)));
        cycle("panel.side",
                this::sideLabel, b -> {
                    WynnChaYuan.config().cyclePanelSide();
                    b.setMessage(sideLabel());
                });
        gapBox = field("panel.gap", T.s("panel.gap.hint"),
                String.valueOf(WynnChaYuan.config().panelGap()), 3, this::applyGap);
        colorBox = field("panel.colour", T.s("panel.colour.hint"),
                WynnChaYuan.config().accentColor(), 7, this::applyColor);
    }

    private void dialogue() {
        cycle("dialogue.mode",
                this::dialogueModeLabel, b -> {
                    WynnChaYuan.config().cycleDialogueMode();
                    b.setMessage(dialogueModeLabel());
                });
        // 選項是<b>另一條訊息、另一個框</b>，所以自己一列。見 CollectorConfig#choiceMode
        cycle("dialogue.choices",
                this::choiceModeLabel, b -> {
                    WynnChaYuan.config().cycleChoiceMode();
                    b.setMessage(choiceModeLabel());
                });
        dialogueHoldBox = field("dialogue.hold", T.s("dialogue.hold.hint"),
                holdSeconds(), 3, this::applySeconds);
        cycle("dialogue.overlays",
                this::overlayLabel, b -> {
                    WynnChaYuan.config().toggleOverlays();
                    b.setMessage(overlayLabel());
                });
    }

    private void world() {
        // 名牌與漂浮字的三段模式擺在最上面。
        //
        // 它本來只在子畫面裡，而子畫面又叫「NPC 名牌設定」——
        // 想把畫面中央那些大字換成中文的人，根本不會點進去。
        // 剩下的參數（停留秒數、偵測距離與夾角）才留在子畫面。
        cycleWith("world.nametag",
                this::nametagLabel, b -> {
                    WynnChaYuan.config().cycleNametagMode();
                    b.setMessage(nametagLabel());
                }, T.s("button.advanced"), () -> this.minecraft.setScreen(new NametagScreen(this)));
        cycle("world.chat",
                this::chatModeLabel, b -> {
                    WynnChaYuan.config().cycleChatMode();
                    b.setMessage(chatModeLabel());
                });
        cycle("world.titles",
                this::titleLabel, b -> {
                    WynnChaYuan.config().toggleTitles();
                    b.setMessage(titleLabel());
                });
        cycle("world.chatcopy",
                this::chatCopyLabel, b -> {
                    WynnChaYuan.config().toggleChatCopy();
                    b.setMessage(chatCopyLabel());
                });
    }

    private void data() {
        // 語言擺在「譯文來源」前面：先決定要哪一種語言，再談那一種從哪裡來。
        languageRow();
        fallbackRow();
        cycle("data.source",
                this::sourceLabel, b -> {
                    WynnChaYuan.config().toggleSource();
                    b.setMessage(sourceLabel());
                    reloadButton.setMessage(reloadLabel());   // 按鈕的意思跟著來源變
                });
        reloadButton = action("data.reload",
                T.s(WynnChaYuan.config().source() == CollectorConfig.Source.GITHUB
                        ? "data.reload.github" : "data.reload.local"),
                reloadLabel(), this::reload);
        cycle("data.collect",
                this::collectLabel, b -> {
                    WynnChaYuan.config().toggleCollect();
                    b.setMessage(collectLabel());
                });
        cycle("data.collectgui",
                this::guiCollectLabel, b -> {
                    WynnChaYuan.config().toggleCollectGuiText();
                    b.setMessage(guiCollectLabel());
                });
        cycle("data.share",
                this::shareLabel, b -> {
                    boolean on = WynnChaYuan.config().toggleShareCaptures();
                    b.setMessage(shareLabel());
                    say(T.c(on ? "data.share.on" : "data.share.off")
                            .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.GRAY));
                });
        cycle("data.debug",
                this::debugLabel, b -> {
                    WynnChaYuan.config().toggleDebugDumps();
                    b.setMessage(debugLabel());
                    say(T.c(WynnChaYuan.config().debugDumps()
                            ? "data.debug.on" : "data.debug.off")
                            .withStyle(ChatFormatting.GREEN));
                });
    }

    // ------------------------------------------------------------ 繪製

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        SettingsLayout box = box();
        int x0 = originX();
        int pane = paneW();
        int px = paneX();
        int listH = box.listH();

        // 卡片墊在按鈕底下，所以先於 super.render。
        // 左右緣都問 SettingsLayout——先前兩張卡片各自 ±8，實際上疊了 4px。
        Cards.panel(g, box.tabsCardX(), box.tabsY(),
                box.tabsCardW(), box.tabsBottom() - box.tabsY());
        Cards.panel(g, box.listCardX(), box.tabsY(),
                box.listCardW(), box.listBottom() - box.tabsY());

        super.render(g, mouseX, mouseY, delta);

        Cards.header(g, this.font, this.width, "WynnChaYuan",
                T.s("header.subtitle", WynnChaYuan.version()));

        // 選到的那一類，左邊一條主題色。貼著按鈕畫，不要壓在卡片框線上。
        g.fill(x0 - PAD + 2, box.rowY(tab), x0 - 1, box.rowY(tab) + 20,
                WynnChaYuan.config().accentARGB());

        // 這一頁在管什麼。標題列與卡片之間那一行。
        int accent = WynnChaYuan.config().accentARGB();
        g.drawString(this.font, Component.literal(tabName(tab)),
                box.tabsCardX() + 2, SettingsLayout.LEAD_Y, accent);
        int lead = box.tabsCardX() + 2 + this.font.width(tabName(tab)) + 8;
        g.drawString(this.font,
                Component.literal(Cards.fit(this.font, tabAbout(tab),
                        box.tabsCardX() + box.footerW() - lead - 4)),
                lead, SettingsLayout.LEAD_Y, Colors.FAINT);

        // 每一列的名稱。控制項自己會畫。
        String hovered = null;
        for (int i = 0; i < rows.size(); i++) {
            int at = i - scroll;
            if (at < 0 || at >= perPage()) {
                continue;
            }
            Row row = rows.get(i);
            // 指到這一列的判定範圍就是卡片內緣，跟底下畫的高亮同一組座標。
            int hiL = box.listCardX() + 1;
            int hiR = box.listCardX() + box.listCardW() - 1;
            boolean on = mouseX >= hiL && mouseX <= hiR
                    && mouseY >= row.y - 2 && mouseY < row.y + 22;
            if (on) {
                hovered = row.hint;
                g.fill(hiL, row.y - 2, hiR, row.y + 22, 0x18FFFFFF);
            }
            // 列與列之間一條很淡的線。滑鼠沒指著任何一列時，眼睛也分得出來
            // 哪個控制項配哪個名稱——右邊那一欄離名稱有一段距離。
            if (at > 0) {
                g.fill(hiL + 3, row.y - (box.rowH() - 20) / 2 - 1,
                       hiR - 3, row.y - (box.rowH() - 20) / 2,
                       0x14FFFFFF);
            }
            // 真的還是放不下就截斷。凸出去比截斷難看得多。
            g.drawString(this.font,
                    Component.literal(Cards.fit(this.font, row.name, pane - ctrlW() - 6)),
                    px, row.y + 6, on ? Colors.TEXT : Colors.HINT);
        }

        // 還有更多列的時候講一聲，不然使用者不知道可以滾
        if (rows.size() > perPage()) {
            g.drawString(this.font,
                    T.c("footer.scroll", scroll + 1,
                            Math.min(rows.size(), scroll + perPage()),
                            rows.size()),
                    px, TOP + listH + 2, Colors.HINT);
        }

        footer(g, x0, pane, hovered);
    }

    /**
     * 底下那一條說明。
     *
     * <p>三種東西共用同一行，優先序：剛做完的動作結果 &gt; 滑鼠指著的那一列的說明
     * &gt; 載入了幾條譯文。分成三個位置的話，畫面下緣會空一大片沒人看的字。
     */
    private void footer(GuiGraphics g, int x0, int pane, String hovered) {
        int w = box().footerW();
        int y = box().footerY();
        Cards.panel(g, box().tabsCardX(), y, w, 20);

        Component line;
        if (statusFresh()) {
            line = status;                     // 剛做完的事先講，幾秒後讓開
        } else if (hovered != null) {
            line = Component.literal(hovered).withStyle(ChatFormatting.GRAY);
        } else {
            // 沒指著任何一列時順便教一次——不然沒人知道說明藏在滑鼠底下。
            int size = WynnChaYuan.translations().size();
            line = T.c("footer.hint", size)
                    .withStyle(size > 0 ? ChatFormatting.DARK_GRAY : ChatFormatting.RED);
        }
        // GitHub 回來的訊息長度事先不知道（「連線失敗：UnknownHostException…」），
        // 不截的話會跑到卡片外面去。顏色要留著，所以截字不截 Component。
        g.drawString(this.font,
                Component.literal(Cards.fit(this.font, line.getString(), w - 10))
                        .withStyle(line.getStyle()),
                x0 - 2, y + 6, Colors.TEXT);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        if (rows.size() > perPage()) {
            int max = rows.size() - perPage();
            int next = Math.max(0, Math.min(max, scroll - (int) Math.signum(dy)));
            if (next != scroll) {
                scroll = next;
                rebuildWidgets();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, dx, dy);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------ 標籤
    //
    // 只寫「值」，不寫「名稱：值」——名稱已經在左邊那一欄了。
    //
    // 全部走 #ctrl／#ctrlNarrow：按鍵名稱這種<b>長度事先不知道</b>的字
    // （「Left Control」、「Mouse Button 4」）不截的話會凸出按鈕外面。

    /**
     * 開關類的值：開著用主題色、關著灰色。
     *
     * <p>一眼掃過去就知道哪幾項是關的，不必逐行讀字。灰色也順便暗示
     * 「這一項現在沒在做事」。
     *
     * <p>本來用的是原版的綠（{@code #55FF55}）。那個綠很飽和，畫在按鈕的
     * 淺色底上刺眼——使用者回報「有點小刺眼」。改用<b>主題色</b>：那個顏色
     * 本來就在畫框線與標題，畫面上早就看習慣了，而且使用者可以在
     * 〈面板〉→〈框線顏色〉自己換成任何喜歡的顏色。
     */
    private Component ctrl(String text) {
        return state(Component.literal(Cards.fit(this.font, text, ctrlW() - 8)), text);
    }

    private Component state(Component text, String value) {
        if (T.s("mode.off").equals(value)) {
            return text.copy().withStyle(ChatFormatting.GRAY);
        }
        return text.copy().withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(onColour())));
    }

    /**
     * 「開著」用什麼顏色。
     *
     * <p>主題色太暗的話畫在按鈕上看不清楚，那時候退回一個柔和的青綠。
     * 亮度用的是常見的加權公式，不是三個通道的平均——綠色對亮度的貢獻
     * 遠大於藍色，取平均會把深藍判成夠亮。
     */
    private int onColour() {
        int accent = WynnChaYuan.config().accentARGB();
        int r = (accent >> 16) & 0xFF;
        int g = (accent >> 8) & 0xFF;
        int b = accent & 0xFF;
        int lum = (r * 299 + g * 587 + b * 114) / 1000;
        return lum >= 96 ? (accent | 0xFF000000) : SOFT_ON;
    }

    /** 主題色太暗時的替代色：柔和的青綠，不像原版的綠那麼跳。 */
    private static final int SOFT_ON = 0xFF8FD3B6;

    /**
     * 選擇類的值：固定位置／跟隨滑鼠、GitHub／本機。
     *
     * <p>這種沒有「開」與「關」之分，兩個選項一樣正常，所以不上綠也不上灰——
     * 上了顏色反而像在暗示哪一個才對。
     */
    private Component pick(String text) {
        return Component.literal(Cards.fit(this.font, text, ctrlW() - 8));
    }

    /** 右邊還帶一顆小按鈕的那一列，主按鈕窄 46px。見 {@link #cycleWith}。 */
    private Component ctrlNarrow(String text) {
        return state(Component.literal(Cards.fit(this.font, text, ctrlW() - 46 - 8)), text);
    }

    private Component tooltipModeLabel() {
        return ctrl(switch (WynnChaYuan.config().tooltipMode()) {
            case PANEL -> T.s("mode.panel");
            case REPLACE -> T.s("mode.replace");
            case OFF -> T.s("mode.off");
        });
    }

    private Component dialogueModeLabel() {
        return ctrl(switch (WynnChaYuan.config().dialogueMode()) {
            case PANEL -> T.s("mode.box");
            case REPLACE -> T.s("mode.replace");
            case OFF -> T.s("mode.off");
        });
    }

    /** 對話<b>選項</b>那幾列，跟上面的內文分開管。 */
    private Component choiceModeLabel() {
        return ctrl(switch (WynnChaYuan.config().choiceMode()) {
            case PANEL -> T.s("mode.box");
            case REPLACE -> T.s("mode.replace");
            case OFF -> T.s("mode.off");
        });
    }

    /** 聊天視窗裡的伺服器訊息。玩家發言不在範圍內，見 {@code ChatListener}。 */
    private Component chatModeLabel() {
        return ctrl(switch (WynnChaYuan.config().chatMode()) {
            case OFF -> T.s("mode.off");
            case REPLACE -> T.s("mode.replace");
            case BOTH -> T.s("mode.both");
        });
    }

    /** 螢幕正中央那行大字。沒有面板選項——那裡沒空間，見 {@code TitleListener}。 */
    private Component titleLabel() {
        return ctrl(T.s(WynnChaYuan.config().translateTitles()
                ? "mode.replace" : "mode.off"));
    }

    /** 名牌與漂浮字。跟 {@code NametagScreen} 那一個是同一個設定。 */
    private Component nametagLabel() {
        return ctrlNarrow(switch (WynnChaYuan.config().nametagMode()) {
            case OFF -> T.s("mode.off");
            case LOOK_AT -> T.s("mode.lookat");
            case REPLACE -> T.s("mode.replace");
        });
    }

    private Component chatCopyLabel() {
        return ctrl(onOff(WynnChaYuan.config().chatCopy()));
    }

    private Component shotLabel() {
        return ctrl(switch (WynnChaYuan.config().shotMode()) {
            case OFF -> T.s("mode.off");
            case KEY -> T.s("mode.key", com.wynnchayuan.render.PanelShot.keyName());
            case AUTO -> T.s("mode.auto");
        });
    }

    private Component overlayLabel() {
        return ctrl(onOff(WynnChaYuan.config().showOverlays()));
    }

    private Component anchorLabel() {
        boolean fixed = WynnChaYuan.config().panelAnchor() == CollectorConfig.PanelAnchor.FIXED;
        return pick(T.s(fixed ? "mode.pinned" : "mode.follow"));
    }

    private Component sideLabel() {
        return pick(switch (WynnChaYuan.config().panelSide()) {
            case AUTO -> T.s("mode.auto");
            case RIGHT -> T.s("mode.right");
            case LEFT -> T.s("mode.left");
        });
    }

    /** 市集搜尋打中文自動換成英文。見 {@code MarketListener}。 */
    private Component marketLabel() {
        return ctrl(onOff(WynnChaYuan.config().marketSearch()));
    }

    private Component itemNameLabel() {
        return ctrl(onOff(WynnChaYuan.config().translateItemNames()));
    }

    /**
     * 更新按鈕的字。
     *
     * <p>有新版時加一個亮黃色的點。按鈕上只有四個字的空間，寫不下版本號——
     * 但「這裡有東西要看」一個點就夠了，版本號點進去就看得到。
     */
    private Component updateLabel() {
        return com.wynnchayuan.Releases.newer() == null
                ? T.c("button.updates")
                : T.c("button.updates.new").withStyle(ChatFormatting.YELLOW);
    }

    /**
     * 譯文語言那一列。
     *
     * <h2>為什麼要按過套用才真的換</h2>
     * 換一種語言＝把那一種的<b>整份</b>譯文從 GitHub 抓下來，三十幾個檔。
     * 先前是「點一下就換」，於是從〈跟著遊戲〉走到〈简体中文〉的路上，
     * <b>每點一下都會觸發一次完整下載</b>，而按鈕上只有一個「…」——
     * 分不出是在下載還是當掉了，也很容易停在半路上那個沒人要的語言。
     *
     * <p>所以拆成兩件事：點按鈕只改<b>待套用</b>的選擇（不連線，立刻反應），
     * 按下套用才真的換，期間按鈕上顯示抓到第幾個檔。
     */
    private void languageRow() {
        Row row = add(T.s("data.language"), T.s("data.language.hint"));
        languageButton = Button.builder(languageLabel(), b -> {
            pendingLanguage = nextLanguage();
            b.setMessage(languageLabel());
            refreshApply();
        }).bounds(0, 0, ctrlW() - 46, 20).build();
        languageApply = Button.builder(T.c("button.apply"), b -> applyLanguage())
                .bounds(ctrlW() - 42, 0, 42, 20).build();
        row.widgets.add(languageButton);
        row.widgets.add(languageApply);
        refreshApply();
    }

    /** 輔助語言那一列。跟 {@link #languageRow} 同一套理由。 */
    private void fallbackRow() {
        Row row = add(T.s("data.fallback"), T.s("data.fallback.hint"));
        fallbackButton = Button.builder(fallbackLabel(), b -> {
            pendingFallback = nextFallback();
            b.setMessage(fallbackLabel());
            refreshApply();
        }).bounds(0, 0, ctrlW() - 46, 20).build();
        fallbackApply = Button.builder(T.c("button.apply"), b -> applyFallback())
                .bounds(ctrlW() - 42, 0, 42, 20).build();
        row.widgets.add(fallbackButton);
        row.widgets.add(fallbackApply);
        refreshApply();
    }

    /** 選的跟現在用的不一樣，套用才亮得起來。 */
    private void refreshApply() {
        if (languageApply != null) {
            languageApply.active = pendingLanguage != null
                    && !pendingLanguage.equals(WynnChaYuan.config().language());
        }
        if (fallbackApply != null) {
            fallbackApply.active = pendingFallback != null
                    && !pendingFallback.equals(WynnChaYuan.config().fallbackLanguage());
        }
    }

    /** 切換期間全部鎖住——同時跑兩個切換，最後是哪一種說了不算。 */
    private void lockLanguageRows(boolean locked) {
        fetching = locked;
        if (languageButton != null) {
            languageButton.active = !locked;
        }
        if (fallbackButton != null) {
            fallbackButton.active = !locked;
        }
        if (locked) {
            if (languageApply != null) {
                languageApply.active = false;
            }
            if (fallbackApply != null) {
                fallbackApply.active = false;
            }
        } else {
            refreshApply();
        }
    }

    private void applyLanguage() {
        String next = pendingLanguage;
        if (next == null) {
            return;
        }
        lockLanguageRows(true);
        say(T.c("data.language.switching").withStyle(ChatFormatting.GRAY));
        WynnChaYuan.switchLanguage(next, result -> {
            pendingLanguage = null;
            pendingFallback = null;
            lockLanguageRows(false);
            languageButton.setMessage(languageLabel());
            fallbackButton.setMessage(fallbackLabel());
            say(Component.literal("✔ " + result)
                    .withStyle(ChatFormatting.GREEN));
        }, (done, total) -> showProgress(languageButton, done, total));
    }

    private void applyFallback() {
        String next = pendingFallback;
        if (next == null) {
            return;
        }
        lockLanguageRows(true);
        say(T.c("data.language.switching").withStyle(ChatFormatting.GRAY));
        WynnChaYuan.switchFallback(next, result -> {
            pendingFallback = null;
            lockLanguageRows(false);
            fallbackButton.setMessage(fallbackLabel());
            say(Component.literal("✔ " + result)
                    .withStyle(ChatFormatting.GREEN));
        }, (done, total) -> showProgress(fallbackButton, done, total));
    }

    /**
     * 抓到第幾個檔了。
     *
     * <p>回呼是在背景執行緒上叫的，動畫面上的東西一定要先回主執行緒。
     */
    private void showProgress(Button button, int done, int total) {
        net.minecraft.client.Minecraft.getInstance().execute(() -> {
            button.setMessage(ctrl(T.s("data.language.progress", done, total)));
            say(T.c("data.language.progress", done, total)
                    .withStyle(ChatFormatting.GRAY));
        });
    }

    /** 還沒套用的選擇。{@code null} 表示沒改過，畫面上顯示現在用的那一種。 */
    private String pendingLanguage;
    private String pendingFallback;
    private Button languageButton;
    private Button languageApply;
    private Button fallbackButton;
    private Button fallbackApply;

    /**
     * 下一個要切到的語言。
     *
     * <p>順序是「跟著遊戲」→ 打包進來的每一種 → 回到「跟著遊戲」。
     * 空字串代表跟著遊戲走，它排在最前面是因為那是預設、也是多數人要的。
     */
    private String nextLanguage() {
        java.util.List<String> all = new java.util.ArrayList<>();
        all.add("");                       // 跟著遊戲
        all.addAll(com.wynnchayuan.translate.Languages.bundled());
        String now = pendingLanguage != null
                ? pendingLanguage : WynnChaYuan.config().language();
        int at = all.indexOf(now);
        return all.get((at + 1 + all.size()) % all.size());
    }

    /**
     * 語言按鈕的字。
     *
     * <p>「跟著遊戲」時把實際選到的那一種寫在括號裡——不寫的話，
     * 玩家看到的是「跟著遊戲」四個字，而畫面上是中文，他無從確認這兩件事
     * 是不是同一回事。
     */
    private Component languageLabel() {
        String chosen = pendingLanguage != null
                ? pendingLanguage : WynnChaYuan.config().language();
        // 「跟著遊戲」時要算的是<b>遊戲</b>語言會挑到哪一種，不能拿現在用的那一種。
        // 現在釘著簡體、待套用選了「跟著遊戲」的話，那兩件事是不一樣的。
        String inUse = com.wynnchayuan.translate.Languages.nativeName(
                chosen.isEmpty() ? WynnChaYuan.autoLanguage() : chosen);
        return ctrl(chosen.isEmpty() ? T.s("data.language.auto", inUse) : inUse);
    }

    /**
     * 下一個要切到的輔助語言。
     *
     * <p>順序：自動 → 不墊（顯示原文）→ 打包進來的每一種 → 回到自動。
     * 目前這一種語言自己不列——拿自己墊自己沒有意義。
     */
    private String nextFallback() {
        java.util.List<String> all = new java.util.ArrayList<>();
        all.add("");                       // 自動
        all.add(WynnChaYuan.OFF);          // 不墊
        for (String lang : com.wynnchayuan.translate.Languages.bundled()) {
            if (!lang.equals(WynnChaYuan.language())) {
                all.add(lang);
            }
        }
        String now = pendingFallback != null
                ? pendingFallback : WynnChaYuan.config().fallbackLanguage();
        int at = all.indexOf(now);
        return all.get((at + 1 + all.size()) % all.size());
    }

    /**
     * 輔助語言按鈕的字。
     *
     * <p>「自動」時把實際墊的那一種寫在括號裡，沒有墊就寫「顯示原文」——
     * 不寫的話「自動」兩個字看不出它到底做了什麼。
     */
    private Component fallbackLabel() {
        String chosen = pendingFallback != null
                ? pendingFallback : WynnChaYuan.config().fallbackLanguage();
        if (WynnChaYuan.OFF.equals(chosen)) {
            return ctrl(T.s("data.fallback.off"));
        }
        if (!chosen.isEmpty()) {
            return ctrl(com.wynnchayuan.translate.Languages.nativeName(chosen));
        }
        String under = WynnChaYuan.fallbackLanguage();
        return ctrl(under == null
                ? T.s("data.fallback.auto.none")
                : T.s("data.fallback.auto",
                      com.wynnchayuan.translate.Languages.nativeName(under)));
    }

    private Component sourceLabel() {
        boolean github = WynnChaYuan.config().source() == CollectorConfig.Source.GITHUB;
        return pick(T.s(github ? "data.source.github" : "data.source.local"));
    }

    private Component collectLabel() {
        return ctrl(onOff(WynnChaYuan.config().collect()));
    }

    private Component guiCollectLabel() {
        return ctrl(onOff(WynnChaYuan.config().collectGuiText()));
    }

    /**
     * 分享語料的開關。
     *
     * <p>這是唯一一個會把字串送出去的開關，所以預設關閉、說明寫在旁邊，
     * 打開時還會在聊天室說一次到底送什麼。見 {@code CorpusUpload}。
     */
    private Component shareLabel() {
        return ctrl(onOff(WynnChaYuan.config().shareCaptures()));
    }

    /**
     * 診斷檔開關。
     *
     * <p>預設關閉：那些檔案是回報問題用的，一般玩家的 config 資料夾不該被
     * 十幾個 txt 洗版。跟「收集未翻譯字串」是兩件事——那個是缺哪些句子要翻，
     * 這個是翻了但畫面上不對。
     */
    private Component debugLabel() {
        return ctrl(onOff(WynnChaYuan.config().debugDumps()));
    }

    private Component reloadLabel() {
        return pick(T.s(WynnChaYuan.config().source() == CollectorConfig.Source.GITHUB
                ? "data.reload.fetch" : "data.reload.reread"));
    }

    private static String onOff(boolean on) {
        return T.s(on ? "mode.on" : "mode.off");
    }

    // ------------------------------------------------------------ 動作

    /** 秒數以 0 表示持續顯示，比「999」直觀。 */
    private String holdSeconds() {
        int ms = WynnChaYuan.config().dialogueHoldMs();
        return ms == Integer.MAX_VALUE ? "0" : String.valueOf(ms / 1000);
    }

    private void applySeconds() {
        if (WynnChaYuan.config().setDialogueHoldSeconds(dialogueHoldBox.getValue())) {
            dialogueHoldBox.setValue(holdSeconds());
            say(T.c("status.hold.ok").withStyle(ChatFormatting.GREEN));
        } else {
            say(T.c("status.hold.bad")
                    .withStyle(ChatFormatting.RED));
        }
    }

    private void applyGap() {
        if (WynnChaYuan.config().setPanelGap(gapBox.getValue())) {
            // 超出範圍會被夾住，把實際生效的值寫回去，免得使用者以為沒生效
            gapBox.setValue(String.valueOf(WynnChaYuan.config().panelGap()));
            say(T.c("status.gap.ok").withStyle(ChatFormatting.GREEN));
        } else {
            say(T.c("status.gap.bad")
                    .withStyle(ChatFormatting.RED));
        }
    }

    /** 套用色碼；格式不對就講清楚並還原，不要靜靜地忽略。 */
    private void applyColor() {
        if (WynnChaYuan.config().setAccentColor(colorBox.getValue())) {
            colorBox.setValue(WynnChaYuan.config().accentColor());
            say(T.c("status.colour.ok").withStyle(ChatFormatting.GREEN));
        } else {
            colorBox.setValue(WynnChaYuan.config().accentColor());
            say(T.c("status.colour.bad").withStyle(ChatFormatting.RED));
        }
    }

    /**
     * 取得最新譯文。
     *
     * <p>「最新」是什麼意思要看譯文來源：設在 GitHub 就得<b>重新連線抓</b>，
     * 只重讀本機檔案的話讀到的還是同一份舊快取——譯者在 GitHub 上改完之後
     * 按這個按鈕沒反應，就是因為這個。
     */
    private void reload() {
        if (WynnChaYuan.config().source() == CollectorConfig.Source.GITHUB) {
            say(T.c("status.fetching").withStyle(ChatFormatting.GRAY));
            fetching = true;
            reloadButton.active = false;
            WynnChaYuan.resyncTranslations(result -> {
                fetching = false;
                reloadButton.active = true;
                report(result, WynnChaYuan.translations().size() > 0);
            });
            return;
        }
        WynnChaYuan.reloadTranslations();
        report(WynnChaYuan.translations().lastResult(),
                WynnChaYuan.translations().size() > 0);
    }

    private void report(String result, boolean ok) {
        say(Component.literal((ok ? "✔ " : "✘ ") + result)
                .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED));
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.literal("[WynnChaYuan] " + result)
                            .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        }
    }
}
