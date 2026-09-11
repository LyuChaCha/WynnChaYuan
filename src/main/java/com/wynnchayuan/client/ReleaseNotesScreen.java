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
import net.minecraft.network.chat.FormattedText;

import java.util.ArrayList;
import java.util.List;

/**
 * 手上這一版改了什麼，以及有沒有更新的版本。
 *
 * <h2>為什麼值得一個畫面</h2>
 * 譯文會自己同步，所以大部分的更新玩家不必做任何事——正因為如此，「要換 jar」
 * 的那幾次反而更容易被漏掉。而更新說明放在 GitHub 的話，只有會去看的人看得到。
 *
 * <p>內容讀自 {@link Releases}：線上讀得到就用線上那份（比較新），
 * 讀不到就用打包進 jar 的那份——所以斷網時「本版改了什麼」照樣看得到，
 * 只是不會說「有新版」。
 */
public final class ReleaseNotesScreen extends Screen {

    /** 內文左右各留多少。太寬的行讀起來會跳行。 */
    private static final int MARGIN = 40;

    /** 每一條前面那個點的寬度，換行後的續行要對齊在它右邊。 */
    private static final int BULLET = 10;

    private final Screen parent;

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
                    .bounds(cx - 100, this.height - 54, 200, 20).build());
        }
        addRenderableWidget(Button.builder(T.c("button.back"), b -> onClose())
                .bounds(cx - 50, this.height - 28, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        int cx = this.width / 2;
        int y = 26;

        g.drawCenteredString(this.font, this.title, cx, y, Colors.TEXT);
        y += 16;

        String latest = Releases.newer();
        if (latest != null) {
            g.drawCenteredString(this.font, T.c("notes.newer", latest, WynnChaYuan.version())
                    .withStyle(ChatFormatting.YELLOW), cx, y, Colors.TEXT);
            y += 12;
            Releases.Notes fresh = Releases.notesFor(latest);
            if (fresh != null && !fresh.headline().isBlank()) {
                g.drawCenteredString(this.font,
                        Component.literal(fresh.headline()).withStyle(ChatFormatting.GRAY),
                        cx, y, Colors.SUBTLE);
                y += 12;
            }
            y += 10;
        } else {
            g.drawCenteredString(this.font,
                    T.c("notes.uptodate", WynnChaYuan.version())
                            .withStyle(ChatFormatting.DARK_GRAY), cx, y, Colors.DIM);
            y += 22;
        }

        Releases.Notes notes = Releases.running();
        if (notes == null) {
            g.drawCenteredString(this.font,
                    T.c("notes.none").withStyle(ChatFormatting.DARK_GRAY),
                    cx, y, Colors.FAINT);
            return;
        }

        g.drawCenteredString(this.font,
                T.c("notes.thisversion", WynnChaYuan.version()),
                cx, y, Colors.TEXT);
        y += 16;
        if (!notes.headline().isBlank()) {
            g.drawCenteredString(this.font,
                    Component.literal(notes.headline()).withStyle(ChatFormatting.GRAY),
                    cx, y, Colors.SUBTLE);
            y += 18;
        }

        int left = MARGIN;
        int wide = this.width - MARGIN * 2;
        int bottom = this.height - 64;
        for (String item : notes.items()) {
            if (y > bottom) {
                break;      // 放不下的就不畫，不要壓到按鈕上
            }
            // 一條一條畫，續行縮排對齊在點的右邊——不縮排的話兩條長的擠在一起
            // 看起來像一條。
            List<FormattedText> lines = wrap(item, wide - BULLET);
            g.drawString(this.font, Component.literal("·").withStyle(ChatFormatting.DARK_GRAY),
                    left, y, Colors.DIM, false);
            for (FormattedText line : lines) {
                if (y > bottom) {
                    break;
                }
                g.drawString(this.font, Component.literal(line.getString())
                        .withStyle(ChatFormatting.GRAY), left + BULLET, y, Colors.SUBTLE, false);
                y += 11;
            }
            y += 3;
        }
    }

    private List<FormattedText> wrap(String text, int wide) {
        return new ArrayList<>(this.font.getSplitter()
                .splitLines(Component.literal(text), wide, net.minecraft.network.chat.Style.EMPTY));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }
}
