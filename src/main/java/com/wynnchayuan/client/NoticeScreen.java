package com.wynnchayuan.client;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.render.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * 警語：翻譯是為了自己看得懂，跟別的玩家講話請用原文。
 *
 * <p>第一次進到 Wynncraft 的角色選擇時自動跳出（見 {@link #maybeShowOnJoin}），
 * 之後從 F6 右上角的「!」打開。勾「不再顯示」只影響自動跳出，F6 永遠打得開。
 */
public final class NoticeScreen extends Screen {

    /** 卡片最寬多少；中文三行在這個寬度內不會折得太碎。 */
    private static final int CARD_MAX_W = 300;

    private static final int PAD = 12;

    private static final int LINE = 12;

    /** 這次開遊戲已經自動跳過了，換世界不要再跳。 */
    private static boolean shownThisSession = false;

    private final Screen parent;

    private Checkbox dontShow;

    public NoticeScreen(Screen parent) {
        super(T.c("notice.title"));
        this.parent = parent;
    }

    /** 進了 Wynncraft，等畫面空下來就跳。見 {@link #clientTick}。 */
    private static volatile boolean pending = false;

    /**
     * 進到角色選擇（或直接進世界）時呼叫：沒勾過「不再顯示」、這次開遊戲也還沒
     * 跳過，就記下「待顯示」。
     *
     * <p>不能當場跳：那一刻畫面上幾乎一定是<b>載入中的畫面</b>。第一版是「有別的
     * 介面就不搶」，結果角色選擇與進世界兩次都剛好卡在載入畫面，實機一次都沒跳出來。
     */
    public static void maybeShowOnJoin() {
        var config = WynnChaYuan.config();
        if (config == null || shownThisSession || config.noticeDismissed()) {
            return;
        }
        pending = true;
    }

    /** 每個 client tick 看一次：待顯示、玩家已在世界裡、畫面上沒有別的介面，才跳。 */
    public static void clientTick() {
        if (!pending) {
            return;
        }
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        pending = false;
        shownThisSession = true;
        mc.setScreen(new NoticeScreen(null));
    }

    private List<FormattedCharSequence> body() {
        List<FormattedCharSequence> out = new ArrayList<>();
        // 鍵要寫死：runLangKeyChecks 是掃程式裡的字面鍵，拼出來的它認不得
        for (var line : List.of(T.c("notice.line1"), T.c("notice.line2"), T.c("notice.line3"))) {
            out.addAll(this.font.split(line, cardW() - PAD * 2));
        }
        return out;
    }

    private int cardW() {
        return Math.min(CARD_MAX_W, this.width - 40);
    }

    private int cardH() {
        // 標題、內文、勾選框、按鈕
        return PAD + LINE + 8 + body().size() * LINE + 12 + 20 + 6 + 20 + PAD;
    }

    private int cardX() {
        return (this.width - cardW()) / 2;
    }

    private int cardY() {
        return Math.max(8, (this.height - cardH()) / 2);
    }

    @Override
    protected void init() {
        int y = cardY() + PAD + LINE + 8 + body().size() * LINE + 12;
        dontShow = Checkbox.builder(T.c("notice.dontShow"), this.font)
                .pos(cardX() + PAD, y)
                .selected(WynnChaYuan.config().noticeDismissed())
                .maxWidth(cardW() - PAD * 2)
                .build();
        addRenderableWidget(dontShow);
        addRenderableWidget(Button.builder(T.c("notice.ok"), b -> onClose())
                .bounds(this.width / 2 - 60, y + 26, 120, 20).build());
    }

    @Override
    public void onClose() {
        if (dontShow != null && dontShow.selected() != WynnChaYuan.config().noticeDismissed()) {
            WynnChaYuan.config().setNoticeDismissed(dontShow.selected());
        }
        this.minecraft.setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        Cards.panel(g, cardX(), cardY(), cardW(), cardH());
        super.render(g, mouseX, mouseY, delta);
        int accent = WynnChaYuan.config().accentARGB();
        int y = cardY() + PAD;
        g.drawCenteredString(this.font, this.title, this.width / 2, y, accent);
        y += LINE + 8;
        for (FormattedCharSequence line : body()) {
            g.drawString(this.font, line, cardX() + PAD, y, Colors.TEXT);
            y += LINE;
        }
    }
}
