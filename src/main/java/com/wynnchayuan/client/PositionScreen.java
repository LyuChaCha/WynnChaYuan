package com.wynnchayuan.client;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.render.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 拖曳調整翻譯面板的固定位置。
 *
 * <p>先前是「在遊戲裡按住 ALT 讓面板跟著滑鼠」——那需要同時開著物品欄、
 * 滑到物品上、還要記得按鍵，而且看不到自己拖到哪。這個畫面直接給一個
 * 可以抓著走的示意面板，所見即所得。
 *
 * <p>拖曳時抓的是<b>滑鼠與面板左上角的相對位移</b>，不是把角落瞬移到滑鼠——
 * 否則一按下去面板就會跳一下。
 */
public final class PositionScreen extends Screen {

    private static final int BOX_W = 150;
    private static final int BOX_H = 90;

    private final Screen parent;

    private int x;
    private int y;
    private boolean dragging = false;
    private int grabX;
    private int grabY;

    /** 儲存後短暫顯示確認 —— 按下去沒反應會讓人不確定有沒有存到。 */
    private boolean saved = false;
    private long savedAt = 0;

    public PositionScreen(Screen parent) {
        super(Component.literal("調整面板位置"));
        this.parent = parent;
        this.x = WynnChaYuan.config().fixedX();
        this.y = WynnChaYuan.config().fixedY();
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int bottom = this.height - 30;

        addRenderableWidget(Button.builder(Component.literal("置中"), b -> {
            x = (this.width - BOX_W) / 2;
            y = (this.height - BOX_H) / 2;
        }).bounds(cx - 155, bottom, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("重設"), b -> {
            x = 20;
            y = 20;
        }).bounds(cx - 50, bottom, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("儲存"), b -> {
            WynnChaYuan.config().setFixedPos(x, y);
            WynnChaYuan.config().saveIfDirty();
            saved = true;                      // 關閉前先讓使用者看到確認
            savedAt = System.currentTimeMillis();
        }).bounds(cx + 55, bottom, 100, 20).build());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && inBox(event.x(), event.y())) {
            dragging = true;
            // 記住抓取點的相對位移，面板才不會一按下去就跳到滑鼠底下
            grabX = (int) event.x() - x;
            grabY = (int) event.y() - y;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            x = clamp((int) event.x() - grabX, this.width - BOX_W);
            y = clamp((int) event.y() - grabY, this.height - BOX_H);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        return super.mouseReleased(event);
    }

    private boolean inBox(double mx, double my) {
        return mx >= x && mx <= x + BOX_W && my >= y && my <= y + BOX_H;
    }

    private static int clamp(int v, int max) {
        return Math.max(0, Math.min(v, Math.max(0, max)));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        g.drawCenteredString(this.font, this.title, this.width / 2, 16, Colors.TEXT);
        g.drawCenteredString(this.font,
                Component.literal("拖曳下面的方框到你想要的位置").withStyle(ChatFormatting.GRAY),
                this.width / 2, 30, Colors.SUBTLE);
        g.drawCenteredString(this.font,
                Component.literal("X: " + x + "   Y: " + y).withStyle(ChatFormatting.DARK_GRAY),
                this.width / 2, 44, Colors.DIM);

        drawPreview(g);

        if (saved && System.currentTimeMillis() - savedAt < 2000) {
            g.drawCenteredString(this.font,
                    Component.literal("✔ 已儲存位置").withStyle(ChatFormatting.GREEN),
                    this.width / 2, this.height - 48, Colors.TEXT);
        }
    }

    /** 畫一個假的翻譯面板，長得像實際會出現的樣子。 */
    private void drawPreview(GuiGraphics g) {
        // 用實際的主題色畫，這樣調位置時看到的就是遊戲裡的樣子
        com.wynnchayuan.render.Boxes.draw(g, x, y, BOX_W, BOX_H);
        if (dragging) {
            int hl = 0x80FFFFFF;               // 拖曳中加一層高亮，看得出抓住了
            g.fill(x, y, x + BOX_W, y + 1, hl);
            g.fill(x, y + BOX_H - 1, x + BOX_W, y + BOX_H, hl);
            g.fill(x, y, x + 1, y + BOX_H, hl);
            g.fill(x + BOX_W - 1, y, x + BOX_W, y + BOX_H, hl);
        }

        List<Component> sample = List.of(
                Component.literal("翻譯面板").withStyle(ChatFormatting.AQUA),
                Component.literal("戰鬥等級        118").withStyle(ChatFormatting.GRAY),
                Component.literal("生命回復    +10/5s").withStyle(ChatFormatting.GREEN),
                Component.literal("元素防禦       +34%").withStyle(ChatFormatting.GREEN),
                Component.empty(),
                Component.literal("（示意）").withStyle(ChatFormatting.DARK_GRAY));

        // 框上方掛一個小標題，讓人知道這塊代表什麼
        int accent = WynnChaYuan.config().accentARGB();
        g.fill(x, y - 12, x + BOX_W, y - 1, 0xD00B1119);
        g.fill(x, y - 12, x + BOX_W, y - 11, accent);
        g.drawString(this.font,
                Component.literal(dragging ? "拖曳中…" : "翻譯面板"), x + 4, y - 10, accent);

        int ty = y + 6;
        for (Component line : sample) {
            g.drawString(this.font, line, x + 6, ty, Colors.TEXT);
            ty += this.font.lineHeight + 1;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
