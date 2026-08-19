package com.wynncollect.client;

import com.wynncollect.CollectorConfig;
import com.wynncollect.WynnCollect;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * F6 開啟的設定面板。
 *
 * <p>刻意只用原版 {@link Button}，不引入 Cloth Config／YACL——
 * 設定就這幾項，多一個依賴只是多一個會壞掉的東西。
 *
 * <p>分成「顯示」與「翻譯」兩欄，每個按鈕下面附一行說明——
 * 光看「面板定位：固定位置」猜不出那是什麼意思。
 */
public final class SettingsScreen extends Screen {

    private static final int COL_W = 200;
    private static final int ROW = 32;          // 按鈕 20 + 說明 12

    private Component status = Component.empty();

    public SettingsScreen() {
        super(Component.literal("WynnCollect 設定"));
    }

    @Override
    protected void init() {
        int left = this.width / 2 - COL_W - 10;
        int right = this.width / 2 + 10;
        int top = 52;

        // ---- 顯示 ----
        addRow(left, top, this::panelLabel, b -> {
            WynnCollect.config().togglePanel();
            b.setMessage(panelLabel());
        });
        addRow(left, top + ROW, this::anchorLabel, b -> {
            WynnCollect.config().togglePanelAnchor();
            b.setMessage(anchorLabel());
        });
        addRenderableWidget(Button.builder(
                Component.literal("調整面板位置…"),
                b -> this.minecraft.setScreen(new PositionScreen(this)))
                .bounds(left, top + ROW * 2, COL_W, 20).build());
        addRow(left, top + ROW * 3, this::sideLabel, b -> {
            WynnCollect.config().cyclePanelSide();
            b.setMessage(sideLabel());
        });
        addRow(left, top + ROW * 4, this::gapLabel, b -> {
            WynnCollect.config().cyclePanelGap();
            b.setMessage(gapLabel());
        });

        // ---- 翻譯 ----
        addRow(right, top, this::itemNameLabel, b -> {
            boolean on = WynnCollect.config().toggleItemNames();
            WynnCollect.translations().setTranslateNames(on);
            b.setMessage(itemNameLabel());
        });
        addRow(right, top + ROW, this::nametagLabel, b -> {
            WynnCollect.config().cycleNametagMode();
            b.setMessage(nametagLabel());
        });
        addRow(right, top + ROW * 2, this::collectLabel, b -> {
            WynnCollect.config().toggleCollect();
            b.setMessage(collectLabel());
        });
        addRenderableWidget(Button.builder(
                Component.literal("重新載入譯文檔"), b -> reload())
                .bounds(right, top + ROW * 3, COL_W, 20).build());

        addRenderableWidget(Button.builder(Component.literal("關於／貢獻者"),
                b -> this.minecraft.setScreen(new CreditsScreen(this)))
                .bounds(this.width / 2 - 155, this.height - 30, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("完成"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    private void addRow(int x, int y, Supplier<Component> label, Consumer<Button> onPress) {
        addRenderableWidget(Button.builder(label.get(), onPress::accept)
                .bounds(x, y, COL_W, 20).build());
    }

    // ------------------------------------------------------------ 標籤

    private Component panelLabel() {
        return Component.literal("翻譯面板：" + onOff(WynnCollect.config().showPanel()));
    }

    private Component anchorLabel() {
        boolean fixed = WynnCollect.config().panelAnchor() == CollectorConfig.PanelAnchor.FIXED;
        return Component.literal("面板定位：" + (fixed ? "固定位置" : "跟隨滑鼠"));
    }

    private Component sideLabel() {
        String name = switch (WynnCollect.config().panelSide()) {
            case AUTO -> "自動";
            case RIGHT -> "固定右側";
            case LEFT -> "固定左側";
        };
        return Component.literal("跟隨時放在：" + name);
    }

    private Component gapLabel() {
        return Component.literal("與物品的間距：" + WynnCollect.config().panelGap() + " px");
    }

    private Component itemNameLabel() {
        return Component.literal("翻譯物品名稱：" + onOff(WynnCollect.config().translateItemNames()));
    }

    private Component nametagLabel() {
        String name = switch (WynnCollect.config().nametagMode()) {
            case OFF -> "關閉";
            case LOOK_AT -> "注視時顯示";
            case REPLACE -> "直接取代原文";
        };
        return Component.literal("NPC 名牌：" + name);
    }

    private Component collectLabel() {
        return Component.literal("收集未翻譯字串：" + onOff(WynnCollect.config().collect()));
    }

    private static String onOff(boolean on) {
        return on ? "開" : "關";
    }

    // ------------------------------------------------------------ 繪製

    private void reload() {
        WynnCollect.reloadTranslations();
        String result = WynnCollect.translations().lastResult();
        boolean ok = WynnCollect.translations().size() > 0;
        status = Component.literal((ok ? "✔ " : "✘ ") + result)
                .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED);
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.literal("[WynnCollect] " + result)
                            .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED), false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        int left = this.width / 2 - COL_W - 10;
        int right = this.width / 2 + 10;
        int top = 52;

        g.drawCenteredString(this.font, this.title, this.width / 2, 16, 0xFFFFFF);

        header(g, left, top - 14, "顯示");
        header(g, right, top - 14, "翻譯");

        hint(g, left, top, "關掉之後仍然照常收集字串");
        hint(g, left, top + ROW, "固定位置時面板不跟著滑鼠跑");
        hint(g, left, top + ROW * 2, "拖曳示意方框決定位置");
        hint(g, left, top + ROW * 3, "自動會依畫面空間左右讓位");
        hint(g, left, top + ROW * 4, "面板與原本 tooltip 之間留多寬");

        hint(g, right, top, "裝備名稱多是專有名詞，通常保留原文");
        hint(g, right, top + ROW, "注視時顯示可保留原文，方便跟人溝通");
        hint(g, right, top + ROW * 2, "把沒翻到的句子記進 captured.json");
        hint(g, right, top + ROW * 3, "改完 json 按這個就生效，不用重開");

        Component now = status.getString().isEmpty()
                ? Component.literal(WynnCollect.translations().size() + " 條譯文已載入")
                        .withStyle(WynnCollect.translations().size() > 0
                                ? ChatFormatting.GRAY : ChatFormatting.RED)
                : status;
        g.drawCenteredString(this.font, now, this.width / 2, this.height - 48, 0xFFFFFF);
    }

    private void header(GuiGraphics g, int x, int y, String text) {
        g.drawString(this.font,
                Component.literal(text).withStyle(ChatFormatting.YELLOW), x, y, 0xFFD24A);
    }

    /** 按鈕下方的一行說明。 */
    private void hint(GuiGraphics g, int x, int y, String text) {
        g.drawString(this.font,
                Component.literal(text).withStyle(ChatFormatting.DARK_GRAY),
                x + 2, y + 21, 0x808080);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
