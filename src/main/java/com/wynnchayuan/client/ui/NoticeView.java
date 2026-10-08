package com.wynnchayuan.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * 使用須知：一張置中的小卡片。
 *
 * <p>內文三句，介面不是英文的時候底下再附一份英文——這段話是在提醒玩家
 * 「跟別人講話請用原文」，看得懂原文的那一份本身就是重點。
 */
public final class NoticeView extends Surface {

    private final Shell shell;
    private final BooleanSupplier dismissed;
    private final Runnable toggleDismissed;
    private final Runnable close;

    public NoticeView(Shell shell, BooleanSupplier dismissed, Runnable toggleDismissed,
                      Runnable close) {
        this.shell = shell;
        this.dismissed = dismissed;
        this.toggleDismissed = toggleDismissed;
        this.close = close;
    }

    @Override
    protected int accentColour() {
        return shell.accent();
    }

    @Override
    protected void click() {
        shell.click();
    }

    @Override
    protected void draw() {
        float w = Math.min(310, screenW - 24);
        float inner = w - 28;
        List<String> body = new ArrayList<>();
        for (String key : new String[] {"notice.line1", "notice.line2", "notice.line3"}) {
            body.addAll(Ui.wrap(c, shell.tr(key), (int) inner));
        }
        List<String> english = new ArrayList<>();
        if (!shell.tr("notice.line1").equals(shell.tr("notice.en1"))) {
            for (String key : new String[] {"notice.en1", "notice.en2", "notice.en3"}) {
                english.addAll(Ui.wrap(c, shell.tr(key), (int) inner));
            }
        }
        float h = 14 + 16 + body.size() * 11 + (english.isEmpty() ? 0 : 8 + english.size() * 11)
                + 12 + CTRL_H + 10 + 18 + 12;
        float x = Math.round((screenW - w) / 2f);
        float y = Math.max(6, Math.round((screenH - h) / 2f));
        window(x, y, w, h, shell.blurred());
        // 點到卡片外面不關：這是要玩家讀過才走的東西
        zone(0, 0, screenW, screenH, (px, py, b) -> { });

        float cy = y + 14;
        c.icon(Icons.Icon.QUEST, x + 14, cy - 1, 11, Ui.GOLD);
        String title = shell.tr("notice.title");
        float s = c.scale(1.25f);
        c.text(title, x + 30, cy + 4 - 4 * s, Ui.GOLD, s);
        cy += 18;
        for (String line : body) {
            c.text(line, x + 14, cy, Ui.TEXT);
            cy += 11;
        }
        if (!english.isEmpty()) {
            cy += 8;
            for (String line : english) {
                c.text(line, x + 14, cy, Ui.TEXT_3);
                cy += 11;
            }
        }
        cy += 12;

        boolean on = dismissed.getAsBoolean();
        float right = x + w - 14;
        toggle("dismiss", right, cy, on, null, toggleDismissed);
        c.text(Ui.fit(c, shell.tr("notice.dontShow"), (int) (inner - 34)), x + 14, cy + 4,
               on ? Ui.TEXT : Ui.TEXT_2);
        cy += CTRL_H + 10;

        String ok = shell.tr("notice.ok");
        float bw = Math.max(80, buttonWidth(ok) + 16);
        button("ok", right - bw, cy, bw, 18, ok, true, true, null, close);
    }

    @Override
    public boolean key(int key, boolean ctrl) {
        if (key == KEY_ENTER || key == KEY_KP_ENTER) {
            close.run();
            return true;
        }
        return false;
    }
}
