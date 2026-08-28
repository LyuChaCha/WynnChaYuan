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

    /**
     * 面板寬度。
     *
     * <p>先前寫死 220，說明文字比它長就直接畫出框外。改成量過每一行再決定——
     * 中文與英文的寬度差很多，其他語言的說明也未必跟中文一樣長，
     * 寫死任何一個數字都只是把問題往後推。
     */
    private int W = 220;

    /** 面板至少這麼寬，免得只有短句時縮成一條。 */
    private static final int MIN_W = 220;

    /** 面板上緣要離第一個按鈕多遠。副標題畫在面板外，這個值得留得下它。 */
    private static final int PANEL_PAD = 14;

    /**
     * 一個輸入欄位佔多高（說明 + 輸入框 + 間距）。
     *
     * <p>說明畫在<b>輸入框上方</b>。先前是畫在下方，於是每一行說明看起來都在
     * 標示<b>下一個</b>欄位——「停留秒數」看起來標到了偵測距離的框，
     * 最後一行說明還掉出面板外面。
     */
    private static final int FIELD_ROW = 37;

    /**
     * 第一個輸入欄位離面板頂端多遠。上面放模式按鈕與它的但書。
     *
     * <p>先前是 47，模式的但書跟第一個欄位的標題正好只差一行高，兩行黏在一起
     * 看起來像同一段——標題就好像在解釋上面那句話，而不是下面那個框。
     */
    private static final int FIRST_FIELD = 60;

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

    /** 面板裡會畫到的每一行說明。寬度就是照它們量出來的。 */
    private String[] insideLines() {
        return new String[] {
            modeHint(),
            "停留秒數（0 = 持續顯示）",
            "偵測距離：幾格內的名牌才算（2–64 格）",
            "準心夾角：偏離準心幾度內還算在看（1–45 度）",
        };
    }

    /**
     * 畫在面板<b>外面</b>、但橫跨面板的幾行：副標題與底部那句提示。
     *
     * <p>它們是照畫面中心置中的，面板也是——所以只要它們比面板寬就會兩頭露出來，
     * 看起來像「字跑出格子」。量寬度時得把它們算進去。
     */
    private String[] aroundLines() {
        return new String[] {
            "名牌要怎麼顯示，還有什麼情況下才算「你在看它」",
            "城裡 NPC 站得密就把角度調小；曠野找人可以調大",
        };
    }

    private int measure() {
        int widest = MIN_W;
        for (String line : insideLines()) {
            widest = Math.max(widest, this.font.width(line) + 8);
        }
        for (String line : aroundLines()) {
            // 面板本身比 W 多 20（左右各 10），所以外圍的行只要不超過那個就好
            widest = Math.max(widest, this.font.width(line) + 8 - 20);
        }
        return Math.max(widest, this.font.width(modeLabel()) + 20);
    }

    @Override
    protected void init() {
        W = measure();
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
        return Component.literal("名牌翻譯: " + name);   // 標籤用半形，跟語料一致
    }

    /** 目前這個模式的但書。 */
    private String modeHint() {
        return switch (WynnChaYuan.config().nametagMode()) {
            case OFF -> "名牌完全不動，畫面上只有原本的英文名";
            case LOOK_AT -> "原文保留著，看著誰才在旁邊補上譯名";
            case REPLACE -> "英文名會被換掉，畫面上看不到原文";
        };
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int x = left();
        int y = top();

        // 面板上下都要讓出空間給畫在外面的那兩行（副標題、底部提示）。
        // 先前上緣只留 22，而副標題畫在 36——文字底部剛好壓在框線上；
        // 底部提示也只隔 8，同樣貼著框。兩邊都加寬。
        int panelTop = y - PANEL_PAD;
        int panelBottom = fieldY(2) + 20 + 8;
        Cards.panel(g, x - 10, panelTop, W + 20, panelBottom - panelTop);
        super.render(g, mouseX, mouseY, delta);

        g.drawCenteredString(this.font, this.title, this.width / 2, 22, Colors.TEXT);
        g.drawCenteredString(this.font,
                Component.literal(aroundLines()[0])
                        .withStyle(ChatFormatting.GRAY),
                this.width / 2, 36, Colors.SUBTLE);

        // 模式按鈕的但書，緊接在按鈕下面——說的必須是<b>現在選的</b>那個模式。
        // 先前這裡固定寫「取代原文則……」，於是選著「注視時顯示」的人看到的
        // 是另一個模式的但書，跟按鈕上的字對不起來。
        Cards.hint(g, this.font, x, y + 24, modeHint());

        // 其餘每一行都是它「下方」那個輸入框的標題
        String[] labels = insideLines();
        for (int n = 0; n < 3; n++) {
            Cards.hint(g, this.font, x, fieldY(n) - LABEL_LIFT, labels[n + 1]);
        }

        g.drawCenteredString(this.font,
                Component.literal(aroundLines()[1])
                        .withStyle(ChatFormatting.DARK_GRAY),
                this.width / 2, panelBottom + 16, Colors.FAINT);

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
