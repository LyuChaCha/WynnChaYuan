package com.wynnchayuan.client;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
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

    private static final int COL_W = 196;
    private static final int ROW = 31;          // 按鈕 20 + 說明 11
    private static final int PAD = 10;
    private static final int TOP = 62;

    private Component status = Component.empty();
    private EditBox colorBox;

    public SettingsScreen() {
        super(Component.literal("WynnChaYuan"));
    }

    private int leftX() {
        return this.width / 2 - COL_W - PAD;
    }

    private int rightX() {
        return this.width / 2 + PAD;
    }

    private int dataY() {
        return TOP + ROW * 5 + 34;
    }

    @Override
    protected void init() {
        int left = leftX();
        int right = rightX();

        // ---- 顯示 ----
        row(left, TOP, this::panelLabel, b -> {
            WynnChaYuan.config().togglePanel();
            b.setMessage(panelLabel());
        });
        row(left, TOP + ROW, this::anchorLabel, b -> {
            WynnChaYuan.config().togglePanelAnchor();
            b.setMessage(anchorLabel());
        });
        addRenderableWidget(Button.builder(
                Component.literal("調整面板位置…"),
                b -> this.minecraft.setScreen(new PositionScreen(this)))
                .bounds(left, TOP + ROW * 2, COL_W, 20).build());
        row(left, TOP + ROW * 3, this::sideLabel, b -> {
            WynnChaYuan.config().cyclePanelSide();
            b.setMessage(sideLabel());
        });
        row(left, TOP + ROW * 4, this::gapLabel, b -> {
            WynnChaYuan.config().cyclePanelGap();
            b.setMessage(gapLabel());
        });

        colorBox = new EditBox(this.font, left, TOP + ROW * 5, COL_W - 46, 20,
                Component.literal("框線顏色"));
        colorBox.setValue(WynnChaYuan.config().accentColor());
        colorBox.setMaxLength(7);
        addRenderableWidget(colorBox);
        addRenderableWidget(Button.builder(Component.literal("套用"), b -> applyColor())
                .bounds(left + COL_W - 42, TOP + ROW * 5, 42, 20).build());

        // ---- 翻譯 ----
        row(right, TOP, this::itemNameLabel, b -> {
            boolean on = WynnChaYuan.config().toggleItemNames();
            WynnChaYuan.translations().setTranslateNames(on);
            b.setMessage(itemNameLabel());
        });
        row(right, TOP + ROW, this::nametagLabel, b -> {
            WynnChaYuan.config().cycleNametagMode();
            b.setMessage(nametagLabel());
        });
        row(right, TOP + ROW * 2, this::nametagHoldLabel, b -> {
            WynnChaYuan.config().cycleNametagHold();
            b.setMessage(nametagHoldLabel());
        });
        row(right, TOP + ROW * 3, this::dialogueHoldLabel, b -> {
            WynnChaYuan.config().cycleDialogueHold();
            b.setMessage(dialogueHoldLabel());
        });

        // ---- 資料 ----
        int dataY = dataY();
        row(right, dataY, this::sourceLabel, b -> {
            WynnChaYuan.config().toggleSource();
            b.setMessage(sourceLabel());
        });
        row(right, dataY + ROW, this::collectLabel, b -> {
            WynnChaYuan.config().toggleCollect();
            b.setMessage(collectLabel());
        });
        addRenderableWidget(Button.builder(
                Component.literal("重新載入譯文檔"), b -> reload())
                .bounds(right, dataY + ROW * 2, COL_W, 20).build());

        // ---- 底部 ----
        addRenderableWidget(Button.builder(Component.literal("關於／貢獻者"),
                b -> this.minecraft.setScreen(new CreditsScreen(this)))
                .bounds(this.width / 2 - 152, this.height - 28, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("完成"), b -> onClose())
                .bounds(this.width / 2 + 52, this.height - 28, 100, 20).build());
    }

    private void row(int x, int y, Supplier<Component> label, Consumer<Button> onPress) {
        addRenderableWidget(Button.builder(label.get(), onPress::accept)
                .bounds(x, y, COL_W, 20).build());
    }

    // ------------------------------------------------------------ 標籤

    private Component panelLabel() {
        return Component.literal("翻譯面板：" + onOff(WynnChaYuan.config().showPanel()));
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

    private Component gapLabel() {
        return Component.literal("與物品的間距：" + WynnChaYuan.config().panelGap() + " px");
    }

    private Component itemNameLabel() {
        return Component.literal("翻譯物品名稱：" + onOff(WynnChaYuan.config().translateItemNames()));
    }

    private Component nametagLabel() {
        String name = switch (WynnChaYuan.config().nametagMode()) {
            case OFF -> "關閉";
            case LOOK_AT -> "注視時顯示";
            case REPLACE -> "直接取代原文";
        };
        return Component.literal("NPC 名牌：" + name);
    }

    private Component nametagHoldLabel() {
        int ms = WynnChaYuan.config().nametagHoldMs();
        return Component.literal("名牌停留：" + (ms == 0 ? "立即消失" : (ms / 1000.0) + " 秒"));
    }

    private Component dialogueHoldLabel() {
        int ms = WynnChaYuan.config().dialogueHoldMs();
        return Component.literal("對話框停留："
                + (ms == Integer.MAX_VALUE ? "持續顯示" : (ms / 1000) + " 秒"));
    }

    private Component sourceLabel() {
        boolean github = WynnChaYuan.config().source() == CollectorConfig.Source.GITHUB;
        return Component.literal("譯文來源：" + (github ? "GitHub（統一）" : "本機（測試）"));
    }

    private Component collectLabel() {
        return Component.literal("收集未翻譯字串：" + onOff(WynnChaYuan.config().collect()));
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

    private void reload() {
        WynnChaYuan.reloadTranslations();
        String result = WynnChaYuan.translations().lastResult();
        boolean ok = WynnChaYuan.translations().size() > 0;
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
        Cards.panel(g, left - 8, TOP - 20, COL_W + 16, ROW * 6 + 14);
        Cards.panel(g, right - 8, TOP - 20, COL_W + 16, ROW * 4 + 14);
        Cards.panel(g, right - 8, dataY - 20, COL_W + 16, ROW * 3 + 14);

        super.render(g, mouseX, mouseY, delta);

        Cards.header(g, this.font, this.width, "WynnChaYuan",
                "Wynncraft 繁體中文翻譯 · v" + WynnChaYuan.version());

        Cards.title(g, this.font, left, TOP - 16, "顯示");
        Cards.title(g, this.font, right, TOP - 16, "翻譯");
        Cards.title(g, this.font, right, dataY - 16, "資料");

        Cards.hint(g, this.font, left + 2, TOP + 21, "關掉之後仍然照常收集字串");
        Cards.hint(g, this.font, left + 2, TOP + ROW + 21, "固定位置時面板不跟著滑鼠跑");
        Cards.hint(g, this.font, left + 2, TOP + ROW * 2 + 21, "拖曳示意方框決定位置");
        Cards.hint(g, this.font, left + 2, TOP + ROW * 3 + 21, "自動會依畫面空間左右讓位");
        Cards.hint(g, this.font, left + 2, TOP + ROW * 4 + 21, "面板與原本 tooltip 之間留多寬");
        Cards.hint(g, this.font, left + 2, TOP + ROW * 5 + 21, "所有小框的框線色，例如 #6FA8D8");

        Cards.hint(g, this.font, right + 2, TOP + 21, "裝備名稱多是專有名詞，通常保留原文");
        Cards.hint(g, this.font, right + 2, TOP + ROW + 21, "注視時顯示可保留原文，方便溝通");
        Cards.hint(g, this.font, right + 2, TOP + ROW * 2 + 21, "視線移開後譯文還留多久");
        Cards.hint(g, this.font, right + 2, TOP + ROW * 3 + 21, "對話結束後小框多久收起來");

        Cards.hint(g, this.font, right + 2, dataY + 21, "GitHub 會同步大家的最新翻譯");
        Cards.hint(g, this.font, right + 2, dataY + ROW + 21, "把沒翻到的句子記進 captured.json");
        Cards.hint(g, this.font, right + 2, dataY + ROW * 2 + 21, "改完 json 按這個就生效");

        Component now = status.getString().isEmpty()
                ? Component.literal(WynnChaYuan.translations().size() + " 條譯文已載入")
                        .withStyle(WynnChaYuan.translations().size() > 0
                                ? ChatFormatting.GRAY : ChatFormatting.RED)
                : status;
        g.drawCenteredString(this.font, now, this.width / 2, this.height - 44, 0xFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
