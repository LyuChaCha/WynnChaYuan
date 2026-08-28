package com.wynnchayuan.client;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.ChatLog;
import com.wynnchayuan.render.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * 「複製聊天」的挑選畫面。
 *
 * <h2>為什麼不是滑鼠指哪就複製哪</h2>
 * 那需要問 Minecraft「游標下面是第幾行聊天」。1.21.11 的 {@code ChatComponent}
 * 沒有公開這件事，資料是私有的，得靠 mixin 挖，還要自己重算它的排版幾何——
 * 而這個模組到目前為止一個 mixin 都沒有。
 *
 * <p>改成列出最近的訊息讓玩家挑，反而多了一個好處：<b>捲得回去</b>。
 *
 * <h2>畫成原本的樣子</h2>
 * 每一則都用 {@link net.minecraft.client.gui.Font#split} 依面板寬度斷行，
 * 再逐行畫出<b>原本的 {@link Component}</b>——顏色、粗體、圖示全部都在，
 * 跟聊天視窗裡看到的一樣。先前畫的是去掉格式的純文字，
 * 一整片灰色，根本認不出哪則是哪則。
 *
 * <h2>複製哪一份</h2>
 * 照「聊天訊息」那個設定走：只顯示原文就複製原文，就地取代就複製譯文，
 * 兩份都顯示就兩份都複製。畫面上看到什麼，複製起來就是什麼。
 */
public final class ChatCopyScreen extends Screen {

    private static final int LINE_H = 10;
    private static final int GAP = 4;            // 訊息之間空一行的高度
    private static final int PAD = 8;
    private static final int TOP = 34;
    private static final int PANEL_BG = 0xD0101018;
    private static final int PANEL_BORDER = 0xFF3A1E5C;
    private static final int ROW_HOVER = 0x40FFD24A;
    private static final int COPIED_MS = 1200;

    /** 攤平後的一行：知道它屬於哪一則，點下去才複製得到整則。 */
    private record Row(int entry, FormattedCharSequence line, boolean last) {}

    private List<ChatLog.Entry> entries = List.of();
    private List<Row> rows = List.of();
    private int scroll;
    private long copiedAt;

    public ChatCopyScreen() {
        super(Component.literal("複製聊天"));
    }

    @Override
    protected void init() {
        entries = ChatLog.recent();
        rows = flatten();
        scroll = 0;
        addRenderableWidget(Button.builder(Component.literal("關閉"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20).build());
    }

    /**
     * 把每一則拆成畫得出來的行。
     *
     * <p>{@code split} 同時處理<b>自動斷行與訊息本身的換行</b>，所以多行的
     * 系統訊息（歡迎訊息、任務獎勵清單）會跟遊戲裡斷在一樣的地方。
     */
    private List<Row> flatten() {
        int room = panelWidth() - PAD * 2;
        List<Row> out = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            ChatLog.Entry entry = entries.get(i);
            List<FormattedCharSequence> lines =
                    new ArrayList<>(this.font.split(entry.original(), room));
            if (entry.hasText()) {
                lines.addAll(this.font.split(entry.translated(), room));
            }
            for (int n = 0; n < lines.size(); n++) {
                out.add(new Row(i, lines.get(n), n == lines.size() - 1));
            }
        }
        return out;
    }

    private int perPage() {
        return Math.max(1, (this.height - 96) / LINE_H);
    }

    private int panelLeft() {
        return Math.max(12, this.width / 2 - 320);
    }

    private int panelWidth() {
        return Math.min(640, this.width - 24);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        g.drawCenteredString(this.font, this.title, this.width / 2, 16, Colors.TEXT);

        if (rows.isEmpty()) {
            g.drawCenteredString(this.font,
                    Component.literal(WynnChaYuan.config().chatCopy()
                            ? "還沒有收到聊天訊息"
                            : "「複製聊天」目前是關閉的，到 F6 設定裡打開")
                            .withStyle(ChatFormatting.GRAY),
                    this.width / 2, this.height / 2 - 4, Colors.SUBTLE);
            return;
        }

        int left = panelLeft();
        int width = panelWidth();
        int shown = Math.min(perPage(), rows.size() - scroll);
        int height = shown * LINE_H + PAD * 2;

        g.fill(left, TOP, left + width, TOP + height, PANEL_BG);
        g.renderOutline(left, TOP, width, height, PANEL_BORDER);

        int over = entryAt(mouseX, mouseY);
        for (int i = 0; i < shown; i++) {
            Row row = rows.get(scroll + i);
            int y = TOP + PAD + i * LINE_H;
            if (over == row.entry()) {
                g.fill(left + 1, y - 1, left + width - 1, y + LINE_H - 1, ROW_HOVER);
            }
            // 原樣畫出來——顏色、粗體、圖示都在原本的 Component 裡
            g.drawString(this.font, row.line(), left + PAD, y, Colors.TEXT, false);
            if (row.last() && i + 1 < shown) {
                g.fill(left + PAD, y + LINE_H - 1, left + width - PAD,
                       y + LINE_H, 0x22FFFFFF);
            }
        }

        if (System.currentTimeMillis() - copiedAt < COPIED_MS) {
            g.drawCenteredString(this.font,
                    Component.literal("已複製").withStyle(ChatFormatting.GREEN),
                    this.width / 2, this.height - 44, Colors.TEXT);
        } else {
            g.drawCenteredString(this.font,
                    Component.literal("點一則複製　·　滾輪捲動　·　"
                            + "複製的內容跟著「聊天訊息」設定走")
                            .withStyle(ChatFormatting.DARK_GRAY),
                    this.width / 2, this.height - 44, Colors.FAINT);
        }
    }

    /** 游標下面是第幾則；不在任何一則上就是 {@code -1}。 */
    private int entryAt(double mouseX, double mouseY) {
        int left = panelLeft();
        int width = panelWidth();
        int shown = Math.min(perPage(), Math.max(0, rows.size() - scroll));
        if (mouseX < left || mouseX > left + width) {
            return -1;
        }
        for (int i = 0; i < shown; i++) {
            int y = TOP + PAD + i * LINE_H;
            if (mouseY >= y - 1 && mouseY < y + LINE_H - 1) {
                return rows.get(scroll + i).entry();
            }
        }
        return -1;
    }

    // 1.21.11 把滑鼠事件包成 MouseButtonEvent，不再是三個散的參數。
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event,
                                boolean doubleClick) {
        int at = entryAt(event.x(), event.y());
        if (at >= 0) {
            copy(at);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void copy(int index) {
        if (index < 0 || index >= entries.size() || this.minecraft == null) {
            return;
        }
        CollectorConfig.ChatMode mode = WynnChaYuan.config().chatMode();
        this.minecraft.keyboardHandler.setClipboard(entries.get(index).forMode(
                mode == CollectorConfig.ChatMode.REPLACE,
                mode == CollectorConfig.ChatMode.BOTH));
        copiedAt = System.currentTimeMillis();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        int max = Math.max(0, rows.size() - perPage());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(dy) * 3));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
