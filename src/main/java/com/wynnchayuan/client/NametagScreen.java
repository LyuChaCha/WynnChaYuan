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

/**
 * NPC 名牌的細項設定。
 *
 * <p>拉出來獨立一頁，是因為這幾項只有在「調到剛好」的時候才會去碰，
 * 平常擠在主設定裡只會讓那張卡片變得很長很難掃。
 *
 * <p>距離與夾角沒有一個值兩邊都好用：城裡 NPC 站得密，錐要收窄才不會一直
 * 抓到後排；曠野找人則希望掃過去就跳出來。所以做成可調而不是挑一個折衷值。
 */
public final class NametagScreen extends Screen {

    private static final int W = 220;

    /**
     * 一個輸入欄位佔多高（說明 + 輸入框 + 間距）。
     *
     * <p>說明畫在<b>輸入框上方</b>。先前是畫在下方，於是每一行說明看起來都在
     * 標示<b>下一個</b>欄位——「停留秒數」看起來標到了偵測距離的框，
     * 最後一行說明還掉出面板外面。
     */
    private static final int FIELD_ROW = 37;

    /** 第一個輸入欄位離面板頂端多遠。上面放模式按鈕與它的但書。 */
    private static final int FIRST_FIELD = 47;

    /** 說明離它所標示的輸入框多高。 */
    private static final int LABEL_LIFT = 11;

    private final Screen parent;

    private EditBox holdBox;
    private EditBox rangeBox;
    private EditBox angleBox;
    private Component status = Component.empty();

    public NametagScreen(Screen parent) {
        super(Component.literal("NPC 名牌設定"));
        this.parent = parent;
    }

    private int left() {
        return this.width / 2 - W / 2;
    }

    private int top() {
        return 62;
    }

    @Override
    protected void init() {
        int x = left();
        int y = top();

        addRenderableWidget(Button.builder(modeLabel(), b -> {
            WynnChaYuan.config().cycleNametagMode();
            b.setMessage(modeLabel());
        }).bounds(x, y, W, 20).build());

        holdBox = field(x, fieldY(0), hold());
        addRenderableWidget(Button.builder(Component.literal("套用"),
                b -> apply(Field.HOLD)).bounds(x + W - 42, fieldY(0), 42, 20).build());

        rangeBox = field(x, fieldY(1),
                trim(WynnChaYuan.config().nametagRange()));
        addRenderableWidget(Button.builder(Component.literal("套用"),
                b -> apply(Field.RANGE)).bounds(x + W - 42, fieldY(1), 42, 20).build());

        angleBox = field(x, fieldY(2),
                trim(WynnChaYuan.config().nametagAngle()));
        addRenderableWidget(Button.builder(Component.literal("套用"),
                b -> apply(Field.ANGLE)).bounds(x + W - 42, fieldY(2), 42, 20).build());

        addRenderableWidget(Button.builder(Component.literal("回到預設值"), b -> resetAll())
                .bounds(this.width / 2 - 105, this.height - 30, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("返回"), b -> onClose())
                .bounds(this.width / 2 + 5, this.height - 30, 100, 20).build());
    }

    /** 第 {@code index} 個輸入框的 y 座標。 */
    private int fieldY(int index) {
        return top() + FIRST_FIELD + index * FIELD_ROW;
    }

    private EditBox field(int x, int y, String value) {
        EditBox box = new EditBox(this.font, x, y, W - 46, 20, Component.literal("數值"));
        box.setValue(value);
        box.setMaxLength(5);
        addRenderableWidget(box);
        return box;
    }

    private enum Field { HOLD, RANGE, ANGLE }

    private void apply(Field which) {
        CollectorConfig cfg = WynnChaYuan.config();
        boolean ok = switch (which) {
            case HOLD -> cfg.setNametagHoldSeconds(holdBox.getValue());
            case RANGE -> cfg.setNametagRange(rangeBox.getValue());
            case ANGLE -> cfg.setNametagAngle(angleBox.getValue());
        };
        // 不管成不成功都把框裡的值換成「實際生效」的值 ——
        // 輸入 999 被夾成 64 的話，框裡還留著 999 會讓人以為沒生效
        refresh();
        status = ok
                ? Component.literal("✔ 已套用").withStyle(ChatFormatting.GREEN)
                : Component.literal("✘ 請輸入數字").withStyle(ChatFormatting.RED);
    }

    private void resetAll() {
        CollectorConfig cfg = WynnChaYuan.config();
        cfg.setNametagHoldSeconds("1");
        cfg.setNametagRange("24");
        cfg.setNametagAngle("6");
        refresh();
        status = Component.literal("✔ 已回到預設值").withStyle(ChatFormatting.GREEN);
    }

    private void refresh() {
        CollectorConfig cfg = WynnChaYuan.config();
        holdBox.setValue(hold());
        rangeBox.setValue(trim(cfg.nametagRange()));
        angleBox.setValue(trim(cfg.nametagAngle()));
    }

    private static String hold() {
        int ms = WynnChaYuan.config().nametagHoldMs();
        return ms == Integer.MAX_VALUE ? "0" : String.valueOf(ms / 1000);
    }

    /** 整數就不要顯示小數點，24.0 看起來像是可以填很精細的東西。 */
    private static String trim(double value) {
        return value == Math.floor(value)
                ? String.valueOf((int) value) : String.valueOf(value);
    }

    private Component modeLabel() {
        String name = switch (WynnChaYuan.config().nametagMode()) {
            case OFF -> "關閉";
            case LOOK_AT -> "注視時顯示";
            case REPLACE -> "直接取代原文";
        };
        return Component.literal("名牌翻譯：" + name);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int x = left();
        int y = top();

        int panelBottom = fieldY(2) + 20 + 8;
        Cards.panel(g, x - 10, y - 22, W + 20, panelBottom - (y - 22));
        super.render(g, mouseX, mouseY, delta);

        g.drawCenteredString(this.font, this.title, this.width / 2, 22, Colors.TEXT);
        g.drawCenteredString(this.font,
                Component.literal("注視時顯示會保留原文，方便跟其他玩家溝通")
                        .withStyle(ChatFormatting.GRAY),
                this.width / 2, 36, Colors.SUBTLE);

        // 模式按鈕的但書，緊接在按鈕下面
        Cards.hint(g, this.font, x + 2, y + 24, "取代原文則畫面上看不到英文名");

        // 其餘每一行都是它「下方」那個輸入框的標題
        Cards.hint(g, this.font, x + 2, fieldY(0) - LABEL_LIFT, "停留秒數（0 = 持續顯示）");
        Cards.hint(g, this.font, x + 2, fieldY(1) - LABEL_LIFT,
                "偵測距離：幾格內的名牌才算（2–64）");
        Cards.hint(g, this.font, x + 2, fieldY(2) - LABEL_LIFT,
                "準心夾角：幾度內才算在看它（1–45）");

        g.drawCenteredString(this.font,
                Component.literal("城裡 NPC 站得密就把角度調小；曠野找人可以調大")
                        .withStyle(ChatFormatting.DARK_GRAY),
                this.width / 2, panelBottom + 8, Colors.FAINT);

        if (!status.getString().isEmpty()) {
            g.drawCenteredString(this.font, status, this.width / 2, this.height - 46, Colors.TEXT);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
