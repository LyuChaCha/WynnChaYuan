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
        boxes.add(new Box(Overlay.CHOICES, "對話選項", 150, 34));
        boxes.add(new Box(Overlay.NAMETAG, "NPC 名牌", 96, 22));

        CollectorConfig cfg = WynnChaYuan.config();
        for (Box box : boxes) {
            if (cfg.hasOverlayPos(box.which)) {
                // 置中的框存的是中心，畫面上要換回左緣
                box.x = centred(box.which)
                        ? cfg.overlayX(box.which) - box.w / 2
                        : cfg.overlayX(box.which);
                box.y = cfg.overlayY(box.which);
                clampIntoScreen(box);
            } else {
                resetToDefault(box);
            }
        }

        int cx = this.width / 2;
        int bottom = this.height - 26;   // 說明條在 height-48..-28，別壓到

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
            // 先前這個 case 漏掉了，於是「全部回到預設」會把對話選項丟到
            // 畫面左上角 (0,0)——那不是它的預設，是 switch 沒接到而已。
            // 數字要跟 DialogueOverlay 實際畫的一致：貼右緣、五分之八高。
            case CHOICES -> {
                box.x = this.width - box.w - 8;
                box.y = this.height * 5 / 8;
            }
        }
        clampIntoScreen(box);
    }

    /**
     * 拖到畫面外就再也抓不回來了，所以限制在畫面內。
     *
     * <p>上下再讓開標題列與底部那排按鈕——框疊在按鈕上的話，想按「儲存」
     * 會先抓到框。框自己的標題列也要留得下。
     */
    private void clampIntoScreen(Box box) {
        int top = HEADER_H + TITLE_H + 2;
        int floor = Math.max(top, this.height - FOOT - box.h);
        box.x = Math.max(0, Math.min(box.x, Math.max(0, this.width - box.w)));
        box.y = Math.max(top, Math.min(box.y, floor));
    }

    /**
     * 這個框的位置該存中心還是左緣。
     *
     * <p>對話框與名牌的寬度隨內容變動，存左緣的話拖到正中央、實際跳出來的
     * 短句卻會偏左。翻譯面板與任務追蹤是左對齊的清單，存左緣才對。
     */
    private static boolean centred(Overlay which) {
        return which == Overlay.DIALOGUE || which == Overlay.NAMETAG
                || which == Overlay.CHOICES;
    }

    private void save() {
        CollectorConfig cfg = WynnChaYuan.config();
        for (Box box : boxes) {
            cfg.setOverlayPos(box.which,
                    centred(box.which) ? box.x + box.w / 2 : box.x,
                    box.y);
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
            dragging.x = snap((int) event.x() - grabX, dragging.w, this.width);
            dragging.y = snap((int) event.y() - grabY, dragging.h, this.height);
            clampIntoScreen(dragging);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    /**
     * 靠近畫面中線就吸過去。
     *
     * <p>要把對話框擺正中央，靠手拖永遠差一兩格——而那一兩格在遊戲裡看得出來。
     * 吸附範圍給得小（{@value #SNAP}px），想擺在中線<b>旁邊</b>一點的人不會被綁住。
     */
    private static int snap(int at, int size, int screen) {
        int centred = (screen - size) / 2;
        return Math.abs(at - centred) <= SNAP ? centred : at;
    }

    /** 見 {@link #snap}。 */
    private static final int SNAP = 5;

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        return super.mouseReleased(event);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        // 標題列跟設定畫面同一套（含模組圖示），兩個畫面看起來才像同一個東西。
        Cards.header(g, this.font, this.width, "調整面板位置",
                "拖曳任一個方框。全部同時顯示，方便看有沒有互相擋到");

        // 拖曳時把畫面的中線畫出來。要把對話框擺正中央的話，沒有這條線
        // 只能靠眼睛猜——而框寬會隨對話長短變，猜不準。
        if (dragging != null) {
            g.fill(this.width / 2, HEADER_H, this.width / 2 + 1, this.height - FOOT,
                    0x30FFFFFF);
            g.fill(0, this.height / 2, this.width, this.height / 2 + 1, 0x30FFFFFF);
        }

        for (Box box : boxes) {
            drawBox(g, box, box.contains(mouseX, mouseY));
        }

        footer(g, mouseX, mouseY);
    }

    /** 標題列高度，跟 {@link Cards#header} 畫的一致。 */
    private static final int HEADER_H = 47;

    /** 底下留給說明條與按鈕列的高度。 */
    private static final int FOOT = 56;

    /**
     * 底部那一條，跟設定畫面同一套：剛做完的事 &gt; 滑鼠指著的框 &gt; 操作說明。
     *
     * <p>先前確認訊息浮在畫面中間、說明浮在最上面，兩句話各據一方，
     * 而中間那一大片正是要拖框的地方——訊息會壓在框上。
     */
    private void footer(GuiGraphics g, int mouseX, int mouseY) {
        int w = Math.min(420, this.width - 20);
        int x = (this.width - w) / 2;
        int y = this.height - FOOT + 8;
        Cards.panel(g, x, y, w, 20);

        String text;
        int colour = Colors.TEXT;
        if (saved && System.currentTimeMillis() - savedAt < 2000) {
            text = "✔ 已儲存所有方框的位置";
            colour = WynnChaYuan.config().accentARGB();
        } else if (dragging != null) {
            text = dragging.label + "：靠近中線會自動對齊";
        } else {
            Box under = null;
            for (int i = boxes.size() - 1; i >= 0; i--) {
                if (boxes.get(i).contains(mouseX, mouseY)) {
                    under = boxes.get(i);
                    break;
                }
            }
            text = under != null
                    ? under.label + "：按住拖曳到你想要的位置"
                    : "按住任一個方框拖曳；「全部回到預設」可以復原";
        }
        g.drawString(this.font,
                Component.literal(Cards.fit(this.font, text, w - 8)),
                x + 4, y + 6, colour);
    }

    private void drawBox(GuiGraphics g, Box box, boolean hovered) {
        int accent = WynnChaYuan.config().accentARGB();
        boolean active = box == dragging;

        // 滑鼠指著的框整塊淡淡地提亮——哪一塊抓得到，不必按下去才知道。
        if (hovered || active) {
            g.fill(box.x - 1, box.y - TITLE_H - 1, box.x + box.w + 1, box.y + box.h + 1,
                    0x18FFFFFF);
        }
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
            // 選項的示意用真的三句，長度才貼近實際——遊戲自己的選項在右上角，
            // 這一塊預設就是貼過去跟它並排的。
            case CHOICES -> List.of(
                    Component.literal("你要去哪裡嗎？"),
                    Component.literal("這裡的生活如何？"),
                    Component.literal("只是打個招呼。"));
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
