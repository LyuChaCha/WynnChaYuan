package com.wynnchayuan.client;

import com.wynnchayuan.Releases;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.render.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * 每一版改了什麼。
 *
 * <h2>為什麼一版一個框</h2>
 * 先前只印手上這一版的條目，一長串灰字接在一起。兩件事看不出來：
 * 哪幾條屬於哪一版，以及自己落後了幾版。一版一個框、框線用主題色，
 * 這兩件事一眼就有答案。
 *
 * <p>內容讀自 {@link Releases}：線上讀得到就用線上那份（比較新），
 * 讀不到就用打包進 jar 的那份——所以斷網時「本版改了什麼」照樣看得到，
 * 只是不會說「有新版」。
 */
public final class ReleaseNotesScreen extends Screen {

    /** 內文左右各留多少。太寬的行讀起來會跳行。 */
    private static final int MARGIN = 34;

    /** 框內縮排。 */
    private static final int PAD = 8;

    /** 每一條前面那個點佔的寬度，續行對齊在它右邊。 */
    private static final int BULLET = 9;

    /** 兩個版本框之間的距離。 */
    private static final int GAP = 8;

    private static final int ROW = 11;

    /** 框的底色。 */
    private static final int CARD_BG = 0xC0121A24;

    /** 手上這一版的框稍亮一點，一眼找得到自己在哪。 */
    private static final int CARD_BG_CURRENT = 0xC01B2838;

    private final Screen parent;

    private int scroll = 0;
    private int contentHeight = 0;

    public ReleaseNotesScreen(Screen parent) {
        super(T.c("notes.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        String latest = Releases.newer();
        if (latest != null) {
            // 有新版才給下載按鈕。沒有新版時擺一顆按不出東西的按鈕，
            // 只會讓人以為自己漏看了什麼。
            String url = Releases.downloadUrl();
            addRenderableWidget(Button.builder(
                    T.c("notes.download", latest),
                    b -> ConfirmLinkScreen.confirmLinkNow(this, url))
                    .bounds(cx - 100, this.height - 52, 200, 20).build());
        }
        addRenderableWidget(Button.builder(T.c("button.back"), b -> onClose())
                .bounds(cx - 50, this.height - 26, 100, 20).build());
    }

    private int top() {
        return 44;
    }

    private int bottom() {
        return this.height - (Releases.newer() != null ? 60 : 34);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        int cx = this.width / 2;
        g.drawCenteredString(this.font, this.title, cx, 16, Colors.TEXT);

        String latest = Releases.newer();
        String running = WynnChaYuan.version();
        g.drawCenteredString(this.font, latest != null
                        ? T.c("notes.newer", latest, running)
                                .withStyle(ChatFormatting.YELLOW)
                        : T.c("notes.uptodate", running)
                                .withStyle(ChatFormatting.DARK_GRAY),
                cx, 30, latest != null ? Colors.TEXT : Colors.DIM);

        List<String> versions = Releases.versions();
        if (versions.isEmpty()) {
            g.drawCenteredString(this.font,
                    T.c("notes.none").withStyle(ChatFormatting.DARK_GRAY),
                    cx, top() + 20, Colors.FAINT);
            return;
        }

        int left = MARGIN;
        int width = this.width - MARGIN * 2;
        int accent = WynnChaYuan.config().accentARGB();

        // 畫到可視範圍外的要剪掉，不然捲動時會畫到標題與按鈕上。
        g.enableScissor(0, top(), this.width, bottom());
        int y = top() - scroll;
        int total = 0;
        for (String version : versions) {
            Releases.Notes notes = Releases.notesFor(version);
            if (notes == null) {
                continue;
            }
            int h = cardHeight(notes, width);
            if (y + h > top() && y < bottom()) {
                drawCard(g, left, y, width, h, version, notes, accent,
                         version.equals(running));
            }
            y += h + GAP;
            total += h + GAP;
        }
        g.disableScissor();
        contentHeight = total;

        if (contentHeight > bottom() - top()) {
            g.drawCenteredString(this.font,
                    T.c("notes.scroll").withStyle(ChatFormatting.DARK_GRAY),
                    cx, bottom() + 3, Colors.FAINT);
        }
    }

    private List<FormattedCharSequence> wrap(String text, int room) {
        return new ArrayList<>(this.font.split(Component.literal(text), room));
    }

    /** 這一版的框要多高。跟 {@link #drawCard} 是同一套算法，改要一起改。 */
    private int cardHeight(Releases.Notes notes, int width) {
        int h = PAD + ROW + 2;                       // 版本號那一行
        if (!notes.headline().isBlank()) {
            h += wrap(notes.headline(), width - PAD * 2).size() * ROW + 2;
        }
        for (String item : notes.items()) {
            h += wrap(item, width - PAD * 2 - BULLET).size() * ROW;
        }
        return h + PAD;
    }

    private void drawCard(GuiGraphics g, int x, int y, int w, int h,
                          String version, Releases.Notes notes, int accent,
                          boolean current) {
        g.fill(x, y, x + w, y + h, current ? CARD_BG_CURRENT : CARD_BG);
        g.renderOutline(x, y, w, h, accent);
        // 左緣多一條實色的邊。純外框的卡片疊在一起看起來像表格，
        // 加這一條才有「一張一張」的樣子。
        g.fill(x, y, x + 2, y + h, accent);

        int textY = y + PAD;
        g.drawString(this.font, Component.literal("v" + version),
                x + PAD, textY, accent, false);
        if (current) {
            int at = x + PAD + this.font.width("v" + version) + 6;
            g.drawString(this.font,
                    T.c("notes.current").withStyle(ChatFormatting.DARK_GRAY),
                    at, textY, Colors.FAINT, false);
        }
        textY += ROW + 2;

        if (!notes.headline().isBlank()) {
            for (FormattedCharSequence line : wrap(notes.headline(), w - PAD * 2)) {
                g.drawString(this.font, line, x + PAD, textY, Colors.TEXT, false);
                textY += ROW;
            }
            textY += 2;
        }
        for (String item : notes.items()) {
            g.drawString(this.font, Component.literal("·")
                            .withStyle(ChatFormatting.DARK_GRAY),
                    x + PAD, textY, Colors.DIM, false);
            for (FormattedCharSequence line : wrap(item, w - PAD * 2 - BULLET)) {
                g.drawString(this.font, line, x + PAD + BULLET, textY,
                             Colors.SUBTLE, false);
                textY += ROW;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double deltaX, double deltaY) {
        int room = bottom() - top();
        if (contentHeight > room) {
            scroll = Math.max(0, Math.min(contentHeight - room,
                    scroll - (int) (deltaY * 16)));
        }
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
