package com.wynnchayuan.client;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.CollectorConfig.Overlay;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.render.Boxes;
import com.wynnchayuan.render.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 拖曳排版：四個框一次全部擺出來。
 *
 * <h2>為什麼四個一起顯示</h2>
 * 一次只調一個的話，玩家沒辦法知道「對話框會不會壓到任務追蹤」——而那正是
 * 排版時唯一在意的事。四個同時擺出來，互相的距離所見即所得。
 *
 * <p>更早之前是「在遊戲裡按住 ALT 讓面板跟著滑鼠」，那需要同時開著物品欄、
 * 滑到物品上、還要記得按鍵，而且看不到自己拖到哪。
 *
 * <p>拖曳時抓的是<b>滑鼠與框左上角的相對位移</b>，不是把角落瞬移到滑鼠——
 * 否則一按下去框就會跳一下。
 */
public final class PositionScreen extends Screen {

    /** 標題列高度。標題也算在可抓取範圍內，框太小時比較好抓。 */
    private static final int TITLE_H = 12;

    /**
     * 一個可以拖的框。
     *
     * <p>尺寸取各自實際內容的概數。畫得跟實際差太多的話，玩家排好了進遊戲
     * 才發現會重疊，等於白排。
     */
    private static final class Box {
        final Overlay which;
        final String label;
        final int w;
        final int h;
        int x;
        int y;

        Box(Overlay which, String label, int w, int h) {
            this.which = which;
            this.label = label;
            this.w = w;
            this.h = h;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y - TITLE_H && my < y + h;
        }
    }

    private final Screen parent;
    private final List<Box> boxes = new ArrayList<>();

    private Box dragging;
    private int grabX;
    private int grabY;

    /** 儲存後短暫顯示確認 —— 按下去沒反應會讓人不確定有沒有存到。 */
    private boolean saved = false;
    private long savedAt = 0;

    public PositionScreen(Screen parent) {
        super(Component.literal("調整面板位置"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        boxes.clear();
        boxes.add(new Box(Overlay.TOOLTIP, "翻譯面板", 150, 90));
        boxes.add(new Box(Overlay.TRACKER, "任務追蹤", 112, 62));
        boxes.add(new Box(Overlay.DIALOGUE, "NPC 對話", 200, 42));
        boxes.add(new Box(Overlay.NAMETAG, "NPC 名牌", 96, 22));

        CollectorConfig cfg = WynnChaYuan.config();
        for (Box box : boxes) {
            if (cfg.hasOverlayPos(box.which)) {
                box.x = cfg.overlayX(box.which);
                box.y = cfg.overlayY(box.which);
                clampIntoScreen(box);
            } else {
                resetToDefault(box);
            }
        }

        int cx = this.width / 2;
        int bottom = this.height - 30;

        addRenderableWidget(Button.builder(Component.literal("全部回到預設"), b -> {
            for (Box box : boxes) {
                resetToDefault(box);
            }
        }).bounds(cx - 155, bottom, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("儲存"), b -> save())
                .bounds(cx - 50, bottom, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("取消"), b -> onClose())
                .bounds(cx + 55, bottom, 100, 20).build());
    }

    /**
     * 各自的預設錨點，要和實際繪製時的算法一致。
     *
     * <p>不一致的話，玩家按了「回到預設」看到框停在 A，進遊戲卻出現在 B。
     */
    private void resetToDefault(Box box) {
        switch (box.which) {
            case TOOLTIP -> {
                box.x = 20;
                box.y = 20;
            }
            case TRACKER -> {
                box.x = 8;
                box.y = 40;
            }
            case DIALOGUE -> {
                box.x = (this.width - box.w) / 2;
                box.y = this.height - 60 - box.h;
            }
            case NAMETAG -> {
                box.x = (this.width - box.w) / 2;
                box.y = this.height / 2 + 16;
            }
        }
        clampIntoScreen(box);
    }

    /** 拖到畫面外就再也抓不回來了，所以限制在畫面內（標題列也要留得下）。 */
    private void clampIntoScreen(Box box) {
        box.x = Math.max(0, Math.min(box.x, Math.max(0, this.width - box.w)));
        box.y = Math.max(TITLE_H, Math.min(box.y, Math.max(TITLE_H, this.height - box.h)));
    }

    private void save() {
        CollectorConfig cfg = WynnChaYuan.config();
        for (Box box : boxes) {
            cfg.setOverlayPos(box.which, box.x, box.y);
        }
        cfg.saveIfDirty();
        saved = true;                          // 關閉前先讓使用者看到確認
        savedAt = System.currentTimeMillis();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        // 由後往前找：畫在上層的先被抓到，和看到的疊放順序一致
        for (int i = boxes.size() - 1; i >= 0; i--) {
            Box box = boxes.get(i);
            if (box.contains(event.x(), event.y())) {
                dragging = box;
                grabX = (int) event.x() - box.x;
                grabY = (int) event.y() - box.y;
                boxes.remove(i);
                boxes.add(box);                // 移到最上層
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null) {
            dragging.x = (int) event.x() - grabX;
            dragging.y = (int) event.y() - grabY;
            clampIntoScreen(dragging);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        return super.mouseReleased(event);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        g.drawCenteredString(this.font, this.title, this.width / 2, 14, Colors.TEXT);
        g.drawCenteredString(this.font,
                Component.literal("拖曳任一個方框；四個同時顯示，方便看有沒有互相擋到")
                        .withStyle(ChatFormatting.GRAY),
                this.width / 2, 28, Colors.SUBTLE);

        for (Box box : boxes) {
            drawBox(g, box);
        }

        if (saved && System.currentTimeMillis() - savedAt < 2000) {
            g.drawCenteredString(this.font,
                    Component.literal("✔ 已儲存四個框的位置").withStyle(ChatFormatting.GREEN),
                    this.width / 2, this.height - 48, Colors.TEXT);
        }
    }

    private void drawBox(GuiGraphics g, Box box) {
        int accent = WynnChaYuan.config().accentARGB();
        boolean active = box == dragging;

        g.fill(box.x, box.y - TITLE_H, box.x + box.w, box.y - 1, 0xD00B1119);
        g.fill(box.x, box.y - TITLE_H, box.x + box.w, box.y - TITLE_H + 1, accent);
        g.drawString(this.font,
                Component.literal(active ? box.label + "（拖曳中…）" : box.label),
                box.x + 4, box.y - 10, accent);

        Boxes.draw(g, box.x, box.y, box.w, box.h);

        int ty = box.y + 6;
        for (Component line : sampleFor(box.which)) {
            g.drawString(this.font, line, box.x + 6, ty, Colors.TEXT);
            ty += this.font.lineHeight + 1;
        }
    }

    /** 示意內容。放真的譯文字樣，比空框更容易估出實際大小。 */
    private static List<Component> sampleFor(Overlay which) {
        return switch (which) {
            case TOOLTIP -> List.of(
                    Component.literal("神話 弓").withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.literal("每秒傷害 897"),
                    Component.literal("戰鬥等級 114"),
                    Component.literal("生命竊取 +978/3s"));
            case TRACKER -> List.of(
                    Component.literal("任務追蹤").withStyle(ChatFormatting.YELLOW),
                    Component.literal("前往 Ragni"),
                    Component.literal("擊敗 5 隻史萊姆"));
            case DIALOGUE -> List.of(
                    Component.literal("廚師").withStyle(ChatFormatting.GREEN),
                    Component.literal("希望別再有 grook 闖進來了。"));
            case NAMETAG -> List.of(
                    Component.literal("廚師").withStyle(ChatFormatting.GREEN));
        };
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
