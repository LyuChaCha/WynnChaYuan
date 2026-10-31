package com.wynnchayuan.client;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.ChatLog;
import com.wynnchayuan.render.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class ChatCopyScreen extends Screen {

    private static final int LINE_H = 10;
    private static final int GAP = 4;
    private static final int PAD = 8;
    private static final int TOP = 34;
    private static final int PANEL_BG = 0xD0101018;
    private static final int PANEL_BORDER = 0xFF3A1E5C;
    private static final int ROW_HOVER = 0x40FFD24A;
    private static final int COPIED_MS = 1200;

    private record Row(int entry, FormattedCharSequence line, boolean last) {}

    private List<ChatLog.Entry> entries = List.of();
    private List<Row> rows = List.of();
    private int scroll;
    private long copiedAt;

    public ChatCopyScreen() {
        super(T.c("chatcopy.title"));
    }

    @Override
    protected void init() {
        entries = ChatLog.recent();
        rows = flatten();
        scroll = 0;
        addRenderableWidget(Button.builder(T.c("button.close"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20).build());
    }

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
                    T.c(WynnChaYuan.config().chatCopy()
                            ? "chatcopy.empty" : "chatcopy.disabled")
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
            g.drawString(this.font, row.line(), left + PAD, y, Colors.TEXT, false);
            if (row.last() && i + 1 < shown) {
                g.fill(left + PAD, y + LINE_H - 1, left + width - PAD,
                       y + LINE_H, 0x22FFFFFF);
            }
        }

        if (System.currentTimeMillis() - copiedAt < COPIED_MS) {
            g.drawCenteredString(this.font,
                    T.c("chatcopy.copied").withStyle(ChatFormatting.GREEN),
                    this.width / 2, this.height - 44, Colors.TEXT);
        } else {
            g.drawCenteredString(this.font,
                    T.c("chatcopy.hint").withStyle(ChatFormatting.DARK_GRAY),
                    this.width / 2, this.height - 44, Colors.FAINT);
        }
    }

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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
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
