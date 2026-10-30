package com.wynnchayuan.client.ui;

import java.util.ArrayList;
import java.util.List;

public final class FirstRunView extends Surface {

    public interface Host {
        boolean askConsent();

        boolean askSetup();

        boolean upgrading();

        void agree();

        void disagree();

        void openPolicy();

        void recommended();

        void settled();

        List<Row> custom();

        void finish();
    }

    public enum Step { CONSENT, SETUP, CUSTOM }

    private static final String[] FEATURES = {
        "firstrun.privacy.update", "firstrun.privacy.sync", "firstrun.privacy.checkin",
        "firstrun.privacy.capes", "firstrun.privacy.login"};

    private static final float PAD = 14;
    private static final float HEAD_H = 38;
    private static final float FOOT_H = 34;
    private static final float LINE = 11;

    private final Shell shell;
    private final Host host;
    private final int steps;
    private Step step;
    private List<Row> rows;
    private float scroll;
    private float scrollTarget;
    private float maxScroll;
    private float bodyTop;
    private float bodyBottom;

    public FirstRunView(Shell shell, Host host) {
        this.shell = shell;
        this.host = host;
        this.steps = (host.askConsent() ? 1 : 0) + (host.askSetup() ? 1 : 0);
        this.step = host.askConsent() ? Step.CONSENT : Step.SETUP;
    }

    public Step step() {
        return step;
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
    public boolean scroll(double dy) {
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget - (float) dy * 24f));
        return true;
    }

    @Override
    public boolean key(int key, boolean ctrl) {
        switch (key) {
            case KEY_DOWN -> scroll(-1);
            case KEY_UP -> scroll(1);
            case KEY_PAGE_DOWN -> scroll(-5);
            case KEY_PAGE_UP -> scroll(5);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void go(Step next) {
        step = next;
        scroll = 0;
        scrollTarget = 0;
        maxScroll = 0;
    }

    private void answered(boolean agreed) {
        if (agreed) {
            host.agree();
        } else {
            host.disagree();
        }
        if (host.askSetup()) {
            go(Step.SETUP);
        } else {
            host.finish();
        }
    }

    private List<Row> rows() {
        if (rows == null) {
            rows = host.custom();
        }
        return rows;
    }

    @Override
    protected void draw() {
        float w = Math.min(380, screenW - 24);
        float inner = w - PAD * 2;
        float contentH = switch (step) {
            case CONSENT -> consentHeight(inner);
            case SETUP -> setupHeight(inner);
            case CUSTOM -> customHeight(inner);
        };
        float foot = step == Step.SETUP ? 10 : FOOT_H;
        float h = Math.min(screenH - 12, HEAD_H + contentH + foot);
        float x = Math.round((screenW - w) / 2f);
        float y = Math.max(6, Math.round((screenH - h) / 2f));
        window(x, y, w, h, shell.blurred());
        zone(0, 0, screenW, screenH, (px, py, b) -> { });

        header(x, y, w);

        float top = y + HEAD_H;
        float bottom = y + h - foot;
        float bodyH = bottom - top;
        maxScroll = Math.max(0, contentH - bodyH);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll = Ui.ease(Math.max(0, Math.min(maxScroll, scroll)), scrollTarget, dt, 55f);

        bodyTop = top;
        bodyBottom = bottom;
        clip(x + 1, top, x + w - 1, bottom);
        float left = x + PAD;
        float cy = top - scroll;
        switch (step) {
            case CONSENT -> consent(left, cy, inner);
            case SETUP -> setup(left, cy, inner);
            case CUSTOM -> custom(left, cy, inner);
        }
        unclip();
        scrollbar(x + w - 6, top, bodyH, contentH, scroll, maxScroll);

        if (step != Step.SETUP) {
            footer(x, y + h - FOOT_H, w);
        }
    }

    private void header(float x, float y, float w) {
        float left = x + PAD;
        c.logo(left, y + 11, 16);
        float s = c.scale(1.25f);
        String title = shell.tr("firstrun.title");
        String count = steps > 1 ? (step == Step.CONSENT ? 1 : 2) + " / " + steps : "";
        float countW = count.isEmpty() ? 0 : c.width(count) + 8;
        c.text(Ui.fit(c, title, (int) ((w - PAD * 2 - 22 - countW) / s)), left + 22,
               y + 19 - 4 * s, Ui.TEXT, s);
        if (!count.isEmpty()) {
            c.text(count, x + w - PAD - c.width(count), y + 15, Ui.HINT);
        }
        c.fill(x + 1, y + HEAD_H - 6, x + w - 1, y + HEAD_H - 6 + c.px(), Ui.LINE);
    }

    private void footer(float x, float y, float w) {
        c.fill(x + 1, y, x + w - 1, y + c.px(), Ui.LINE);
        float by = y + 8;
        float right = x + w - PAD;
        if (step == Step.CONSENT) {
            String yes = shell.tr("firstrun.agree");
            String no = shell.tr("firstrun.disagree");
            float yw = Math.max(70, buttonWidth(yes) + 10);
            float nw = Math.max(70, buttonWidth(no) + 10);
            button("agree", right - yw, by, yw, 18, yes, true, true, null, () -> answered(true));
            button("disagree", right - yw - 6 - nw, by, nw, 18, no, true, false, null,
                   () -> answered(false));
        } else {
            String done = shell.tr("button.done");
            String back = shell.tr("button.back");
            float dw = Math.max(70, buttonWidth(done) + 10);
            float bw = Math.max(60, buttonWidth(back) + 10);
            button("done", right - dw, by, dw, 18, done, true, true, null, () -> {
                host.settled();
                host.finish();
            });
            button("back", x + PAD, by, bw, 18, back, true, false, null, () -> go(Step.SETUP));
        }
    }

    private boolean visible(float y, float h) {
        return y + h > bodyTop && y < bodyBottom;
    }

    private float heading(String key, float left, float y) {
        c.text(shell.tr(key), left, y, Ui.mix(accent, Ui.TEXT, 0.35f));
        return y + 15;
    }

    private float paragraph(String text, float left, float y, float width, int colour) {
        for (String line : Ui.wrap(c, text, (int) width)) {
            c.text(line, left, y, colour);
            y += LINE;
        }
        return y;
    }

    private float paragraphHeight(String text, float width) {
        return Ui.wrap(c, text, (int) width).size() * LINE;
    }

    private float consentHeight(float inner) {
        float h = 15 + paragraphHeight(shell.tr("firstrun.privacy.intro"), inner) + 4;
        for (String key : FEATURES) {
            h += paragraphHeight(shell.tr(key), inner - 10) + 2;
        }
        h += 6 + paragraphHeight(shell.tr("firstrun.privacy.decline"), inner) + 6;
        return h + 14 + 4;
    }

    private void consent(float left, float y, float inner) {
        y = heading("firstrun.privacy.title", left, y);
        y = paragraph(shell.tr("firstrun.privacy.intro"), left, y, inner, Ui.TEXT) + 4;
        for (String key : FEATURES) {
            c.text("·", left + 2, y, accent);
            y = paragraph(shell.tr(key), left + 10, y, inner - 10, Ui.TEXT_2) + 2;
        }
        y = paragraph(shell.tr("firstrun.privacy.decline"), left, y + 6, inner, Ui.TEXT_3) + 6;
        String link = shell.tr("firstrun.privacy.policy");
        float lw = Math.min(inner, c.width(link) + 4);
        boolean hot = over(left, y, lw, 14);
        int ink = hot ? Ui.TEXT : Ui.mix(accent, Ui.TEXT, 0.25f);
        String shown = Ui.fit(c, link, (int) inner);
        c.text(shown, left, y + 3, ink);
        c.fill(left, y + 12, left + c.width(shown), y + 12 + c.px(), Ui.alpha(ink, hot ? 0xFF : 0x80));
        if (visible(y, 14)) {
            zone(left, y, lw, 14, (px, py, b) -> {
                click();
                host.openPolicy();
            }).key = "b:policy";
        }
    }

    private record Card(String key, String title, String desc, boolean primary, Runnable go) {}

    private List<Card> cards() {
        List<Card> out = new ArrayList<>();
        out.add(new Card("recommended", shell.tr("firstrun.recommended"),
                shell.tr("firstrun.recommended.desc"), true, () -> {
                    host.recommended();
                    host.settled();
                    host.finish();
                }));
        out.add(new Card("custom", shell.tr("firstrun.custom"), shell.tr("firstrun.custom.desc"),
                false, () -> go(Step.CUSTOM)));
        if (host.upgrading()) {
            out.add(new Card("keep", shell.tr("firstrun.keep"), shell.tr("firstrun.keep.desc"),
                    false, () -> {
                        host.settled();
                        host.finish();
                    }));
        }
        return out;
    }

    private float cardHeight(Card card, float inner) {
        return 8 + 12 + paragraphHeight(card.desc(), inner - 16) + 6;
    }

    private float setupHeight(float inner) {
        float h = 15 + paragraphHeight(shell.tr("firstrun.setup.intro"), inner) + 6;
        for (Card card : cards()) {
            h += cardHeight(card, inner) + 6;
        }
        return h;
    }

    private void setup(float left, float y, float inner) {
        y = heading("firstrun.setup.title", left, y);
        y = paragraph(shell.tr("firstrun.setup.intro"), left, y, inner, Ui.TEXT_2) + 6;
        for (Card card : cards()) {
            float ch = cardHeight(card, inner);
            boolean hot = over(left, y, inner, ch);
            float g = glow("card:" + card.key(), hot);
            Ui.halo(c, left, y, inner, ch, Ui.R2, accent, g);
            int fill = card.primary() ? Ui.mix(Ui.RAISED, Ui.alpha(accent, 0x30), 0.6f) : Ui.RAISED;
            int edge = card.primary() ? Ui.mix(Ui.alpha(accent, 0x8C), accent, g) : edge(g);
            Ui.pane(c, left, y, inner, ch, Ui.R2, fill, edge);
            float tx = left + 8;
            String title = Ui.fit(c, card.title(), (int) (inner - 30));
            c.text(title, tx, y + 8, card.primary() ? Ui.mix(accent, Ui.TEXT, 0.3f) : Ui.TEXT);
            c.icon(Icons.Icon.RIGHT, left + inner - 16, y + 8, 7, hot ? Ui.TEXT : Ui.TEXT_3);
            paragraph(card.desc(), tx, y + 20, inner - 16, Ui.TEXT_3);
            Runnable go = card.go();
            if (visible(y, ch)) {
                zone(left, y, inner, ch, (px, py, b) -> {
                    click();
                    go.run();
                }).key = "b:" + card.key();
            }
            y += ch + 6;
        }
    }

    private static final float ROW_H = 22;
    private static final float ROW_TALL = 34;

    private float controlWidth(Row row) {
        if (row.kind == Row.Kind.TOGGLE) {
            return 26;
        }
        float total = 2;
        for (Row.Option option : row.options) {
            total += c.width(option.label()) + 12;
        }
        return total;
    }

    private boolean tall(float inner) {
        for (Row row : rows()) {
            if (c.width(row.name.get()) + 10 + controlWidth(row) > inner) {
                return true;
            }
        }
        return false;
    }

    private float customHeight(float inner) {
        float each = tall(inner) ? ROW_TALL : ROW_H;
        return 15 + paragraphHeight(shell.tr("firstrun.custom.intro"), inner) + 4
                + rows().size() * each;
    }

    private void custom(float left, float y, float inner) {
        y = heading("firstrun.custom.title", left, y);
        y = paragraph(shell.tr("firstrun.custom.intro"), left, y, inner, Ui.TEXT_2) + 4;
        boolean tall = tall(inner);
        float right = left + inner;
        for (Row row : rows()) {
            float rh = tall ? ROW_TALL : ROW_H;
            if (!visible(y, rh)) {
                y += rh;
                continue;
            }
            c.fill(left, y, right, y + c.px(), Ui.LINE);
            String name = Ui.fit(c, row.name.get(), (int) inner);
            c.text(name, left, y + 7, Ui.TEXT_2);
            float cy = tall ? y + 16 : y + 3;
            if (row.kind == Row.Kind.TOGGLE) {
                toggle(row.id, right, cy, row.on.getAsBoolean(), null, row.flip);
            } else {
                String[] labels = new String[row.options.size()];
                for (int i = 0; i < labels.length; i++) {
                    labels[i] = row.options.get(i).label();
                }
                chips(row.id, right, cy, labels, row.selected.getAsInt(), row.pick);
            }
            y += rh;
        }
    }
}
