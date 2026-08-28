package com.wynnchayuan.client;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.render.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * F6 開啟的設定畫面。
 *
 * <p>分成三張卡片：顯示、翻譯、資料。純按鈕堆疊看不出哪些設定是一組的，
 * 分卡之後不用看說明也大致猜得到。
 *
 * <p>只用原版 {@link Button} 與 {@link EditBox}，不引入 Cloth Config／YACL——
 * 設定就這幾項，多一個依賴只是多一個會壞掉的東西。
 */
public final class SettingsScreen extends Screen {

    private static final int COL_W = 204;
    private static final int PAD = 10;
    private static final int TOP = 84;   // 標題列 41 + 卡片標題與留白

    private Component status = Component.empty();
    private EditBox colorBox;
    private EditBox dialogueHoldBox;
    private Button reloadButton;
    private EditBox gapBox;

    public SettingsScreen() {
        super(Component.literal("WynnChaYuan"));
    }

    private int leftX() {
        return this.width / 2 - COL_W - PAD;
    }

    private int rightX() {
        return this.width / 2 + PAD;
    }

    /**
     * 一列的高度：按鈕 20 + 說明文字 + 呼吸空間。
     *
     * <p>畫面不夠高時自動縮排——寧可擠一點，也不要讓最下面那張卡被切掉。
     * 門檻是照最高那一欄（翻譯 5 列 + 資料 5 列）反推出來的。
     */
    private int rowH() {
        return this.height >= 510 ? 37 : 30;
    }

    /** 說明文字距離按鈕頂端多遠。 */
    private int hintDy() {
        return rowH() >= 36 ? 23 : 21;
    }

    /**
     * 一張卡片要多高。
     *
     * <p>先前寫成 {@code ROW * rows + 14}，漏算了<b>最後一列的說明文字</b>——
     * 那行字會掉到卡片外面。這裡改成從最後一行說明的底部往回推。
     */
    private int cardH(int rows) {
        return rowH() * (rows - 1) + hintDy() + 37;
    }

    /** 左欄「顯示」用掉幾列。 */
    private static final int SHOW_ROWS = 6;

    /** 右欄「翻譯」用掉幾列。 */
    private static final int TRANSLATE_ROWS = 8;

    /**
     * 「資料」卡接在<b>左欄</b>底下，不是右欄。
     *
     * <p>兩欄不一樣高：右欄一路加到八列，左欄只有六列。先前資料卡接在右欄
     * 底下、起點又照右欄的高度算，右欄每多一列就把它往下推一列——
     * 加完「聊天訊息」與「畫面中央大字」之後就整張掉出畫面。
     * 接在矮的那一欄底下，右欄以後再長也不會撞到它。
     */
    private int dataY() {
        return TOP - 20 + cardH(SHOW_ROWS) + 20 + 20;   // 顯示卡底部 + 間距 + 卡片標題
    }

    @Override
    protected void init() {
        int left = leftX();
        int right = rightX();

        // ---- 顯示 ----
        row(left, TOP, this::tooltipModeLabel, b -> {
            WynnChaYuan.config().cycleTooltipMode();
            b.setMessage(tooltipModeLabel());
        });
        row(left, TOP + rowH(), this::anchorLabel, b -> {
            WynnChaYuan.config().togglePanelAnchor();
            b.setMessage(anchorLabel());
        });
        addRenderableWidget(Button.builder(
                Component.literal("調整面板位置…"),
                b -> this.minecraft.setScreen(new PositionScreen(this)))
                .bounds(left, TOP + rowH() * 2, COL_W, 20).build());
        row(left, TOP + rowH() * 3, this::sideLabel, b -> {
            WynnChaYuan.config().cyclePanelSide();
            b.setMessage(sideLabel());
        });
        gapBox = new EditBox(this.font, left, TOP + rowH() * 4, COL_W - 46, 20,
                Component.literal("間距"));
        gapBox.setValue(String.valueOf(WynnChaYuan.config().panelGap()));
        gapBox.setMaxLength(3);
        addRenderableWidget(gapBox);
        addRenderableWidget(Button.builder(Component.literal("套用"), b -> applyGap())
                .bounds(left + COL_W - 42, TOP + rowH() * 4, 42, 20).build());

        colorBox = new EditBox(this.font, left, TOP + rowH() * 5, COL_W - 46, 20,
                Component.literal("框線顏色"));
        colorBox.setValue(WynnChaYuan.config().accentColor());
        colorBox.setMaxLength(7);
        addRenderableWidget(colorBox);
        addRenderableWidget(Button.builder(Component.literal("套用"), b -> applyColor())
                .bounds(left + COL_W - 42, TOP + rowH() * 5, 42, 20).build());

        // ---- 翻譯 ----
        row(right, TOP, this::itemNameLabel, b -> {
            boolean on = WynnChaYuan.config().toggleItemNames();
            WynnChaYuan.translations().setTranslateNames(on);
            b.setMessage(itemNameLabel());
        });
        addRenderableWidget(Button.builder(Component.literal("NPC 名牌設定…"),
                b -> this.minecraft.setScreen(new NametagScreen(this)))
                .bounds(right, TOP + rowH(), COL_W, 20).build());

        row(right, TOP + rowH() * 2, this::dialogueModeLabel, b -> {
            WynnChaYuan.config().cycleDialogueMode();
            b.setMessage(dialogueModeLabel());
        });

        dialogueHoldBox = secondsBox(right, TOP + rowH() * 3,
                WynnChaYuan.config().dialogueHoldMs());
        addRenderableWidget(Button.builder(Component.literal("套用"),
                b -> applySeconds(false))
                .bounds(right + COL_W - 42, TOP + rowH() * 3, 42, 20).build());

        row(right, TOP + rowH() * 4, this::overlayLabel, b -> {
            WynnChaYuan.config().toggleOverlays();
            b.setMessage(overlayLabel());
        });

        row(right, TOP + rowH() * 5, this::shotLabel, b -> {
            WynnChaYuan.config().cycleShotMode();
            b.setMessage(shotLabel());
        });

        row(right, TOP + rowH() * 6, this::chatModeLabel, b -> {
            WynnChaYuan.config().cycleChatMode();
            b.setMessage(chatModeLabel());
        });

        row(right, TOP + rowH() * 7, this::titleLabel, b -> {
            WynnChaYuan.config().toggleTitles();
            b.setMessage(titleLabel());
        });


        // ---- 資料 ----
        int dataY = dataY();
        row(left, dataY, this::guiCollectLabel, b -> {
            WynnChaYuan.config().toggleCollectGuiText();
            b.setMessage(guiCollectLabel());
        });
        row(left, dataY + rowH(), this::sourceLabel, b -> {
            WynnChaYuan.config().toggleSource();
            b.setMessage(sourceLabel());
            reloadButton.setMessage(reloadLabel());   // 按鈕的意思跟著來源變
        });
        row(left, dataY + rowH() * 2, this::collectLabel, b -> {
            WynnChaYuan.config().toggleCollect();
            b.setMessage(collectLabel());
        });
        row(left, dataY + rowH() * 3, this::debugLabel, b -> {
            WynnChaYuan.config().toggleDebugDumps();
            b.setMessage(debugLabel());
            status = Component.literal(WynnChaYuan.config().debugDumps()
                    ? "✔ 診斷檔已開啟——重進遊戲後才會開始寫"
                    : "✔ 診斷檔已關閉——重進遊戲後生效")
                    .withStyle(ChatFormatting.GREEN);
        });
        reloadButton = Button.builder(reloadLabel(), b -> reload())
                .bounds(left, dataY + rowH() * 4, COL_W, 20).build();
        addRenderableWidget(reloadButton);

        // ---- 底部 ----
        addRenderableWidget(Button.builder(Component.literal("關於／貢獻者"),
                b -> this.minecraft.setScreen(new CreditsScreen(this)))
                .bounds(this.width / 2 - 152, this.height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("完成"), b -> onClose())
                .bounds(this.width / 2 + 52, this.height - 28, 100, 20).build());
    }

    /** 秒數輸入框。持續顯示以 0 表示，比「999」直觀。 */
    private EditBox secondsBox(int x, int y, int ms) {
        EditBox box = new EditBox(this.font, x, y, COL_W - 46, 20,
                Component.literal("秒數"));
        box.setValue(ms == Integer.MAX_VALUE ? "0" : String.valueOf(ms / 1000));
        box.setMaxLength(3);
        addRenderableWidget(box);
        return box;
    }

    private void applySeconds(boolean unusedNametagFlag) {
        if (WynnChaYuan.config().setDialogueHoldSeconds(dialogueHoldBox.getValue())) {
            int ms = WynnChaYuan.config().dialogueHoldMs();
            dialogueHoldBox.setValue(
                    ms == Integer.MAX_VALUE ? "0" : String.valueOf(ms / 1000));
            status = Component.literal("✔ 已設定停留時間").withStyle(ChatFormatting.GREEN);
        } else {
            status = Component.literal("✘ 請輸入秒數（整數，0 = 持續顯示）")
                    .withStyle(ChatFormatting.RED);
        }
    }

    private void applyGap() {
        if (WynnChaYuan.config().setPanelGap(gapBox.getValue())) {
            // 超出範圍會被夾住，把實際生效的值寫回去，免得使用者以為沒生效
            gapBox.setValue(String.valueOf(WynnChaYuan.config().panelGap()));
            status = Component.literal("✔ 已設定間距").withStyle(ChatFormatting.GREEN);
        } else {
            status = Component.literal("✘ 請輸入整數像素（0–200）")
                    .withStyle(ChatFormatting.RED);
        }
    }

    private Component guiCollectLabel() {
        return Component.literal("收集介面文字："
                + onOff(WynnChaYuan.config().collectGuiText()));
    }

    private void row(int x, int y, Supplier<Component> label, Consumer<Button> onPress) {
        addRenderableWidget(Button.builder(label.get(), onPress::accept)
                .bounds(x, y, COL_W, 20).build());
    }

    // ------------------------------------------------------------ 標籤

    private Component tooltipModeLabel() {
        String name = switch (WynnChaYuan.config().tooltipMode()) {
            case PANEL -> "另開面板";
            case REPLACE -> "就地取代";
            case OFF -> "關閉";
        };
        return Component.literal("物品翻譯：" + name);
    }

    private Component dialogueModeLabel() {
        String name = switch (WynnChaYuan.config().dialogueMode()) {
            case PANEL -> "另開小框";
            case REPLACE -> "就地取代";
            case OFF -> "關閉";
        };
        return Component.literal("任務對話：" + name);
    }

    /** 聊天視窗裡的伺服器訊息。玩家發言不在範圍內，見 {@code ChatListener}。 */
    private Component chatModeLabel() {
        String name = switch (WynnChaYuan.config().chatMode()) {
            case OFF -> "關閉";
            case REPLACE -> "就地取代";
            case BOTH -> "原文加譯文";
        };
        return Component.literal("聊天訊息：" + name);
    }

    /** 螢幕正中央那行大字。沒有面板選項——那裡沒空間，見 {@code TitleListener}。 */
    private Component titleLabel() {
        return Component.literal("畫面中央大字："
                + (WynnChaYuan.config().translateTitles() ? "就地取代" : "關閉"));
    }

    private Component shotLabel() {
        String name = switch (WynnChaYuan.config().shotMode()) {
            case OFF -> "關閉";
            case KEY -> "按 F8 拍";
            case AUTO -> "自動";
        };
        return Component.literal("譯文截圖：" + name);
    }

    private Component overlayLabel() {
        return Component.literal("對話／追蹤小框：" + onOff(WynnChaYuan.config().showOverlays()));
    }

    private Component anchorLabel() {
        boolean fixed = WynnChaYuan.config().panelAnchor() == CollectorConfig.PanelAnchor.FIXED;
        return Component.literal("面板定位：" + (fixed ? "固定位置" : "跟隨滑鼠"));
    }

    private Component sideLabel() {
        String name = switch (WynnChaYuan.config().panelSide()) {
            case AUTO -> "自動";
            case RIGHT -> "固定右側";
            case LEFT -> "固定左側";
        };
        return Component.literal("跟隨時放在：" + name);
    }

    private Component itemNameLabel() {
        return Component.literal("翻譯物品名稱：" + onOff(WynnChaYuan.config().translateItemNames()));
    }

    private Component sourceLabel() {
        boolean github = WynnChaYuan.config().source() == CollectorConfig.Source.GITHUB;
        return Component.literal("譯文來源：" + (github ? "GitHub（統一）" : "本機（測試）"));
    }

    private Component collectLabel() {
        return Component.literal("收集未翻譯字串：" + onOff(WynnChaYuan.config().collect()));
    }

    /**
     * 診斷檔開關。
     *
     * <p>預設關閉：那些檔案是回報問題用的，一般玩家的 config 資料夾不該被
     * 十幾個 txt 洗版。跟上面的「收集未翻譯字串」是兩件事——那個是缺哪些句子
     * 要翻，這個是翻了但畫面上不對。
     */
    private Component debugLabel() {
        return Component.literal("寫出診斷檔：" + onOff(WynnChaYuan.config().debugDumps()));
    }

    private static String onOff(boolean on) {
        return on ? "開" : "關";
    }

    // ------------------------------------------------------------ 動作

    /** 套用色碼；格式不對就講清楚並還原，不要靜靜地忽略。 */
    private void applyColor() {
        if (WynnChaYuan.config().setAccentColor(colorBox.getValue())) {
            colorBox.setValue(WynnChaYuan.config().accentColor());
            status = Component.literal("✔ 已套用顏色").withStyle(ChatFormatting.GREEN);
        } else {
            colorBox.setValue(WynnChaYuan.config().accentColor());
            status = Component.literal("✘ 色碼格式要像 #6FA8D8").withStyle(ChatFormatting.RED);
        }
    }

    private Component reloadLabel() {
        return Component.literal(
                WynnChaYuan.config().source() == CollectorConfig.Source.GITHUB
                        ? "從 GitHub 重新抓譯文" : "重新載入本機譯文檔");
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
            status = Component.literal("… 正在從 GitHub 抓取").withStyle(ChatFormatting.GRAY);
            reloadButton.active = false;
            WynnChaYuan.resyncTranslations(result -> {
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
        status = Component.literal((ok ? "✔ " : "✘ ") + result)
                .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED);
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.literal("[WynnChaYuan] " + result)
                            .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        }
    }

    // ------------------------------------------------------------ 繪製

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int left = leftX();
        int right = rightX();
        int dataY = dataY();

        // 卡片要墊在按鈕底下，所以先於 super.render
        Cards.panel(g, left - 8, TOP - 20, COL_W + 16, cardH(SHOW_ROWS));
        Cards.panel(g, right - 8, TOP - 20, COL_W + 16, cardH(TRANSLATE_ROWS));
        Cards.panel(g, left - 8, dataY - 20, COL_W + 16, cardH(5));

        super.render(g, mouseX, mouseY, delta);

        Cards.header(g, this.font, this.width, "WynnChaYuan",
                "Wynncraft 繁體中文翻譯 · v" + WynnChaYuan.version());

        Cards.title(g, this.font, left, TOP - 16, "顯示");
        Cards.title(g, this.font, right, TOP - 16, "翻譯");
        Cards.title(g, this.font, left, dataY - 16, "資料");

        Cards.hint(g, this.font, left + 4, TOP + hintDy(), "面板保留原文；取代則畫面較乾淨");
        Cards.hint(g, this.font, left + 4, TOP + rowH() + hintDy(), "固定位置時面板不跟著滑鼠跑");
        Cards.hint(g, this.font, left + 4, TOP + rowH() * 2 + hintDy(), "拖曳示意方框決定位置");
        Cards.hint(g, this.font, left + 4, TOP + rowH() * 3 + hintDy(), "自動會依畫面空間左右讓位");
        Cards.hint(g, this.font, left + 4, TOP + rowH() * 4 + hintDy(),
                "面板與原本 tooltip 之間留多寬（像素）");
        Cards.hint(g, this.font, left + 4, TOP + rowH() * 5 + hintDy(),
                "框線顏色（16 進位色碼，例如 #6FA8D8）");

        Cards.hint(g, this.font, right + 4, TOP + hintDy(), "裝備名稱多是專有名詞，通常保留原文");
        Cards.hint(g, this.font, right + 4, TOP + rowH() + hintDy(),
                "模式、停留秒數、偵測距離與夾角");
        Cards.hint(g, this.font, right + 4, TOP + rowH() * 2 + hintDy(),
                "對話框停留秒數（0 = 持續顯示）");
        Cards.hint(g, this.font, right + 4, TOP + rowH() * 3 + hintDy(),
                "NPC 對話與任務追蹤的翻譯小框");

        Cards.hint(g, this.font, left + 4, dataY + hintDy(), "公會、任務書等 GUI 的文字（預設關）");
        Cards.hint(g, this.font, left + 4, dataY + rowH() + hintDy(), "GitHub 會同步大家的最新翻譯");
        Cards.hint(g, this.font, left + 4, dataY + rowH() * 2 + hintDy(), "把沒翻到的句子記進 captured.json");
        Cards.hint(g, this.font, left + 4, dataY + rowH() * 3 + hintDy(),
                "回報問題時才需要，重進遊戲後生效");
        Cards.hint(g, this.font, left + 4, dataY + rowH() * 4 + hintDy(),
                WynnChaYuan.config().source() == CollectorConfig.Source.GITHUB
                        ? "譯者剛改完 GitHub 的話按這個" : "改完 json 按這個就生效");

        Component now = status.getString().isEmpty()
                ? Component.literal(WynnChaYuan.translations().size() + " 條譯文已載入")
                        .withStyle(WynnChaYuan.translations().size() > 0
                                ? ChatFormatting.GRAY : ChatFormatting.RED)
                : status;
        g.drawCenteredString(this.font, now, this.width / 2, this.height - 44, Colors.TEXT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
