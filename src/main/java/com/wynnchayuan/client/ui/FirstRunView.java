package com.wynnchayuan.client.ui;

import java.util.ArrayList;
import java.util.List;

public final class FirstRunView extends Surface {

    public interface Host {
        boolean askNotice();

        void noticeRead();

        boolean askConsent();

        boolean askSetup();

        boolean upgrading();

        void agree();

        void disagree();

        void openPolicy();

        void recommended();

        void settled();

        List<Row.Tab> custom();

        int tone(Row.Tone tone);

        void finish();
    }

    public enum Step { NOTICE, CONSENT, SETUP, CUSTOM }

    private static final String[] FEATURES = {
        "firstrun.privacy.update", "firstrun.privacy.sync", "firstrun.privacy.checkin",
        "firstrun.privacy.capes", "firstrun.privacy.login"};

    private static final Icons.Icon[] FEATURE_ICONS = {
        Icons.Icon.DOWN, Icons.Icon.BOOK, Icons.Icon.LEAF, Icons.Icon.CAPE, Icons.Icon.QUEST};

    private static final float LINE = 10;

    private final Shell shell;
    private final Host host;
    private final List<Step> flow = new ArrayList<>();
    private Step step;
    private List<Row.Tab> tabs;
    private int category;
    private float scroll;
    private float scrollTarget;
    private float maxScroll;
    private float bodyT = 1f;
    private float railW;
    private float bodyTop;
    private float bodyBottom;

    public FirstRunView(Shell shell, Host host) {
        this.shell = shell;
        this.host = host;
        if (host.askNotice()) {
            flow.add(Step.NOTICE);
        }
        if (host.askConsent()) {
            flow.add(Step.CONSENT);
        }
        if (host.askSetup() || flow.isEmpty()) {
            flow.add(Step.SETUP);
        }
        this.step = flow.get(0);
    }

    private int stepIndex() {
        return Math.max(0, flow.indexOf(step == Step.CUSTOM ? Step.SETUP : step));
    }

    private void advance() {
        int next = stepIndex() + 1;
        if (next < flow.size()) {
            go(flow.get(next));
        } else {
            host.finish();
        }
    }

    public Step step() {
        return step;
    }

    public int category() {
        return category;
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
                if (step == Step.CUSTOM && !ctrl && key >= '1' && key < '1' + tabs().size()) {
                    pickCategory(key - '1');
                    return true;
                }
                return false;
            }
        }
        return true;
    }

    private void go(Step next) {
        step = next;
        restart();
    }

    private void restart() {
        scroll = 0;
        scrollTarget = 0;
        maxScroll = 0;
        bodyT = 0f;
    }

    private void pickCategory(int index) {
        if (index == category) {
            return;
        }
        category = index;
        restart();
        click();
    }

    private void answered(boolean agreed) {
        if (agreed) {
            host.agree();
        } else {
            host.disagree();
        }
        advance();
    }

    private List<Row.Tab> tabs() {
        if (tabs == null) {
            tabs = host.custom();
        }
        return tabs;
    }

    private Row.Tab currentTab() {
        List<Row.Tab> all = tabs();
        return all.isEmpty() ? null : all.get(Math.max(0, Math.min(all.size() - 1, category)));
    }

    @Override
    protected void draw() {
        float w = Math.min(screenW - 12, SettingsView.MAX_W);
        float h = Math.min(screenH - 14, SettingsView.MAX_H);
        window(Math.round((screenW - w) / 2f), Math.round((screenH - h) / 2f), w, h,
               shell.blurred());
        railW = winW >= 430 ? SettingsView.RAIL_W : SettingsView.RAIL_SLIM;
        zone(0, 0, screenW, screenH, (px, py, b) -> { });

        rail();
        float headBottom = header();
        body(headBottom);
        footer();

        bodyT = Ui.ease(bodyT, 1f, dt, 70f);
        scroll = Ui.ease(scroll, scrollTarget, dt, 55f);
    }

    private record Nav(Icons.Icon icon, String name, String trailing, boolean done) {}

    private List<Nav> navs() {
        List<Nav> out = new ArrayList<>();
        if (step == Step.CUSTOM) {
            for (Row.Tab t : tabs()) {
                int n = 0;
                for (Row.Group g : t.groups()) {
                    n += g.rows().size();
                }
                out.add(new Nav(t.icon(), t.name().get(), String.valueOf(n), false));
            }
            return out;
        }
        int at = stepIndex();
        for (int i = 0; i < flow.size(); i++) {
            Step s = flow.get(i);
            Icons.Icon icon = s == Step.NOTICE ? Icons.Icon.QUEST
                    : s == Step.CONSENT ? Icons.Icon.LEAF : Icons.Icon.BOOK;
            String name = shell.tr(s == Step.NOTICE ? "notice.title"
                    : s == Step.CONSENT ? "firstrun.privacy.short" : "firstrun.setup.title");
            out.add(new Nav(icon, name, String.valueOf(i + 1), i < at));
        }
        return out;
    }

    private int navIndex() {
        if (step == Step.CUSTOM) {
            return category;
        }
        return stepIndex();
    }

    private void rail() {
        float x = winX;
        float y = winY;
        boolean slim = railW < SettingsView.RAIL_W;
        clip(x, y, x + railW, y + winH);
        c.round(x, y, railW + Ui.R3 * 2, winH, Ui.R3, Ui.RAIL);
        unclip();
        c.fill(x + railW - c.px(), y + 1, x + railW, y + winH - 1, Ui.LINE);

        float cy;
        if (slim) {
            c.logo(x + (railW - 20) / 2f, y + 8, 20);
            cy = y + 36;
        } else {
            c.logo(x + 7, y + 8, 26);
            String title = "WynnChaYuan";
            float room = railW - 38 - 6;
            float big = c.scale(1.25f);
            float s = c.width(title) * big <= room ? big : 1f;
            c.text(title, x + 38, y + 21 - 4 * s, accent, s);
            c.text(title, x + 38 + c.px(), y + 21 - 4 * s, accent, s);
            cy = y + 40;
            for (String line : Ui.wrap(c, shell.tr("header.tagline"), (int) (railW - 16))) {
                c.text(line, x + 8, cy, Ui.TEXT_2);
                cy += 10;
            }
            cy += 8;
        }

        List<Nav> navs = navs();
        int current = navIndex();
        float navH = SettingsView.NAV_H;
        float[] tops = new float[navs.size()];
        float[] heights = new float[navs.size()];
        List<List<String>> labels = new ArrayList<>();
        float ny = cy;
        for (int i = 0; i < navs.size(); i++) {
            Nav nav = navs.get(i);
            float trailW = nav.done() ? 9 : c.width(nav.trailing());
            int room = (int) (railW - 12 - 22 - trailW - 12);
            List<String> lines = slim ? List.of(nav.name()) : Ui.wrap(c, nav.name(), room);
            if (lines.size() > 2) {
                lines = List.of(lines.get(0), Ui.fit(c, String.join(" ", lines.subList(1, lines.size())), room));
            }
            labels.add(lines);
            tops[i] = ny;
            heights[i] = navH + (lines.size() - 1) * LINE;
            ny += heights[i] + 2;
        }
        float pillY = animate(step == Step.CUSTOM ? "nav:c" : "nav:s", tops[current], 55f);
        float pillH = animate(step == Step.CUSTOM ? "navh:c" : "navh:s", heights[current], 55f);
        c.round(x + 6, pillY, railW - 12, pillH, Ui.R1, Ui.alpha(accent, 0x33));
        for (int i = 0; i < navs.size(); i++) {
            Nav nav = navs.get(i);
            float top = tops[i];
            float h = heights[i];
            boolean on = i == current;
            boolean clickable = step == Step.CUSTOM;
            boolean hot = clickable && over(x + 6, top, railW - 12, h);
            float g = glow("nav" + i, hot && !on);
            if (g > 0.01f) {
                c.round(x + 6, top, railW - 12, h, Ui.R1, Ui.fade(0x12FFFFFF, g));
            }
            int ink = on ? accent : (hot || nav.done() ? Ui.TEXT : Ui.TEXT_2);
            if (slim) {
                c.icon(nav.done() ? Icons.Icon.CHECK : nav.icon(), x + (railW - 12) / 2f,
                       top + 4.5f, 12, nav.done() ? Ui.GREEN : ink);
                if (over(x + 6, top, railW - 12, h)) {
                    tip = new Tip(nav.name(), x + railW, top - 4, 0, h);
                    hoverRow = "#nav" + i;
                }
            } else {
                c.icon(nav.icon(), x + 12, top + 5, 11, ink);
                float ly = top + 6.5f;
                for (String line : labels.get(i)) {
                    c.text(line, x + 28, ly, ink);
                    ly += LINE;
                }
                if (nav.done()) {
                    c.icon(Icons.Icon.CHECK, x + railW - 12 - 9, top + 6, 9, Ui.GREEN);
                } else {
                    c.text(nav.trailing(), x + railW - 12 - c.width(nav.trailing()), top + 6.5f,
                           on ? accent : Ui.FAINT);
                }
            }
            if (clickable) {
                int index = i;
                zone(x + 6, top, railW - 12, h, (px, py, b) -> pickCategory(index)).key = "n:" + i;
            }
        }
    }

    private float header() {
        float x = winX + railW + 12;
        float right = winX + winW - 12;
        float y = winY;
        String title;
        String about;
        if (step == Step.CUSTOM) {
            Row.Tab t = currentTab();
            title = t == null ? shell.tr("firstrun.custom.title") : t.name().get();
            about = t == null ? "" : t.about().get();
        } else {
            title = shell.tr(step == Step.NOTICE ? "notice.title"
                    : step == Step.CONSENT ? "firstrun.privacy.title" : "firstrun.setup.title");
            about = shell.tr("firstrun.title");
        }
        String count = step == Step.CUSTOM ? shell.tr("firstrun.custom.title")
                : (flow.size() > 1 ? (stepIndex() + 1) + " / " + flow.size() : "");
        float countW = 0;
        if (!count.isEmpty()) {
            float cw = c.width(count) + 12;
            Ui.pane(c, right - cw, y + 11, cw, CTRL_H, Ui.R1, Ui.FIELD, Ui.BORDER);
            c.text(count, right - cw + 6, y + 15, Ui.HINT);
            countW = cw + 10;
        }
        float textRoom = right - countW - x;
        float s = c.scale(1.5f);
        if (c.width(title) * s > textRoom) {
            s = 1f;
        }
        c.text(Ui.fit(c, title, (int) (textRoom / s)), x, y + 8, Ui.TEXT, s);
        float ay = y + 8 + 8 * s + 4;
        for (String line : Ui.wrap(c, about, (int) textRoom)) {
            c.text(line, x, ay, Ui.TEXT_3);
            ay += LINE;
        }
        return Math.max(y + SettingsView.HEAD_H, ay + 4);
    }

    private void footer() {
        float y = winY + winH - SettingsView.FOOT_H;
        float left = winX + railW + 12;
        float right = winX + winW - 12;
        c.fill(winX + railW, y, winX + winW - 1, y + c.px(), Ui.LINE);
        float h = 18;
        float by = y + (SettingsView.FOOT_H - h) / 2f;
        if (step == Step.NOTICE) {
            String ok = shell.tr("notice.ok");
            float ow = Math.max(64, c.width(ok) + 28);
            button("noticeok", right - ow, by, ow, h, ok, true, true, null, () -> {
                host.noticeRead();
                advance();
            });
        } else if (step == Step.CONSENT) {
            String yes = shell.tr("firstrun.agree");
            String no = shell.tr("firstrun.disagree");
            float yw = Math.max(64, c.width(yes) + 28);
            float nw = Math.max(64, c.width(no) + 16);
            button("agree", right - yw, by, yw, h, yes, true, true, null, () -> answered(true));
            button("disagree", right - yw - 5 - nw, by, nw, h, no, true, false, null,
                   () -> answered(false));
        } else if (step == Step.CUSTOM) {
            String done = shell.tr("button.done");
            String back = shell.tr("button.back");
            float dw = Math.max(64, c.width(done) + 28);
            float bw = Math.max(56, c.width(back) + 16);
            button("done", right - dw, by, dw, h, done, true, true, null, () -> {
                host.settled();
                host.finish();
            });
            button("back", left, by, bw, h, back, true, false, null, () -> go(Step.SETUP));
        } else {
            String hint = Ui.fit(c, shell.tr("firstrun.setup.intro"), (int) (right - left));
            c.text(hint, left, by + 5, Ui.FAINT);
        }
    }

    private boolean visible(float y, float h) {
        return y + h > bodyTop && y < bodyBottom;
    }

    private void body(float top) {
        float bottom = winY + winH - SettingsView.FOOT_H - 8;
        float left = winX + railW + 12;
        float w = winX + winW - 12 - left;
        float inner = w - 6;
        float contentH = switch (step) {
            case NOTICE -> noticeHeight(inner);
            case CONSENT -> consentHeight(inner);
            case SETUP -> setupHeight(inner);
            case CUSTOM -> customHeight(inner);
        };
        float h = bottom - top;
        maxScroll = Math.max(0, contentH - h);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        bodyTop = top;
        bodyBottom = bottom;
        clip(left, top, left + w, bottom);
        float cy = top - scroll + (1f - bodyT) * 7f;
        switch (step) {
            case NOTICE -> notice(left, cy, inner);
            case CONSENT -> consent(left, cy, inner);
            case SETUP -> setup(left, cy, inner);
            case CUSTOM -> custom(left, cy, inner);
        }
        unclip();
        if (bodyT < 0.99f) {
            c.fill(left, top, left + w, bottom, Ui.fade(0xC8181F2A, 1f - bodyT));
        }
        if (maxScroll > 0) {
            float trackX = left + w - 3;
            c.round(trackX, top, 2.5f, h, 1.25f, Ui.LINE);
            float thumbH = Math.max(14, h * h / Math.max(1f, contentH));
            float thumbY = top + (h - thumbH) * (scroll / maxScroll);
            c.round(trackX, thumbY, 2.5f, thumbH, 1.25f, Ui.alpha(accent, 0xC8));
        }
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

    private static final String[] NOTICE_LINES = {"notice.line1", "notice.line2", "notice.line3"};
    private static final String[] NOTICE_ENGLISH = {"notice.en1", "notice.en2", "notice.en3"};

    private boolean noticeEnglish() {
        return !shell.tr("notice.line1").equals(shell.tr("notice.en1"));
    }

    private String joined(String[] keys) {
        StringBuilder out = new StringBuilder();
        for (String key : keys) {
            String part = shell.tr(key).strip();
            if (out.length() > 0 && !part.isEmpty()) {
                int last = out.codePointBefore(out.length());
                if (last < 0x2E80) {
                    out.append(' ');
                }
            }
            out.append(part);
        }
        return out.toString();
    }

    private float noticeWidth(float inner, boolean icon) {
        return Math.min(inner - (icon ? 34 : 18), 380);
    }

    private float noticeCardHeight(String[] keys, float inner, boolean icon) {
        return 10 + paragraphHeight(joined(keys), noticeWidth(inner, icon)) + 8;
    }

    private float noticeHeight(float inner) {
        float h = noticeCardHeight(NOTICE_LINES, inner, true);
        if (noticeEnglish()) {
            h += 6 + noticeCardHeight(NOTICE_ENGLISH, inner, false);
        }
        return h;
    }

    private float noticeCard(String[] keys, float left, float y, float inner, int ink,
                             boolean icon) {
        float ch = noticeCardHeight(keys, inner, icon);
        Ui.pane(c, left, y, inner, ch, Ui.R2, Ui.CARD, icon ? Ui.alpha(Ui.GOLD, 0x66) : Ui.LINE);
        if (icon) {
            c.icon(Icons.Icon.QUEST, left + 9, y + 9, 11, Ui.GOLD);
        }
        paragraph(joined(keys), left + (icon ? 25 : 9), y + 10, noticeWidth(inner, icon), ink);
        return y + ch;
    }

    private void notice(float left, float y, float inner) {
        y = noticeCard(NOTICE_LINES, left, y, inner, Ui.TEXT, true);
        if (noticeEnglish()) {
            noticeCard(NOTICE_ENGLISH, left, y + 6, inner, Ui.TEXT_3, false);
        }
    }

    private float featureHeight(String key, float inner) {
        return Math.max(1, Ui.wrap(c, shell.tr(key), (int) (inner - 34)).size()) * LINE + 8;
    }

    private float consentHeight(float inner) {
        float h = paragraphHeight(shell.tr("firstrun.privacy.intro"), inner) + 6;
        h += SettingsView.GROUP_HEAD + 4;
        for (String key : FEATURES) {
            h += featureHeight(key, inner);
        }
        h += 6 + paragraphHeight(shell.tr("firstrun.privacy.decline"), inner) + 6;
        return h + 14 + 2;
    }

    private void consent(float left, float y, float inner) {
        y = paragraph(shell.tr("firstrun.privacy.intro"), left, y, inner, Ui.TEXT_2) + 6;
        float cardH = SettingsView.GROUP_HEAD + 4;
        for (String key : FEATURES) {
            cardH += featureHeight(key, inner);
        }
        card(left, y, inner, cardH, shell.tr("privacy.online"), String.valueOf(FEATURES.length),
             false);
        float ry = y + SettingsView.GROUP_HEAD + 1;
        for (int i = 0; i < FEATURES.length; i++) {
            float rh = featureHeight(FEATURES[i], inner);
            boolean hot = over(left + 2, ry, inner - 4, rh);
            float g = glow("feat" + i, hot);
            if (g > 0.01f) {
                c.round(left + 2, ry, inner - 4, rh, Ui.R1, Ui.fade(0x0EFFFFFF, g));
            }
            c.icon(FEATURE_ICONS[i], left + 9, ry + 4, 10, accent);
            paragraph(shell.tr(FEATURES[i]), left + 25, ry + 5, inner - 34,
                      Ui.mix(Ui.TEXT_2, Ui.TEXT, g));
            ry += rh;
        }
        y += cardH + 6;
        y = paragraph(shell.tr("firstrun.privacy.decline"), left, y, inner, Ui.TEXT_3) + 6;
        String link = Ui.fit(c, shell.tr("firstrun.privacy.policy"), (int) inner - 14);
        float lw = c.width(link) + 14;
        boolean hot = over(left, y, lw, 14);
        float g = glow("policy", hot);
        int ink = Ui.mix(Ui.mix(accent, Ui.TEXT, 0.25f), Ui.TEXT, g);
        c.icon(Icons.Icon.RIGHT, left, y + 4, 7, ink);
        c.text(link, left + 11, y + 3, ink);
        c.fill(left + 11, y + 12, left + 11 + c.width(link), y + 12 + c.px(),
               Ui.alpha(ink, 0x80 + (int) (0x7F * g)));
        if (visible(y, 14)) {
            zone(left, y, lw, 14, (px, py, b) -> {
                click();
                host.openPolicy();
            }).key = "b:policy";
        }
    }

    private record Choice(String key, String title, String desc, boolean primary, Runnable go) {}

    private List<Choice> choices() {
        List<Choice> out = new ArrayList<>();
        out.add(new Choice("recommended", shell.tr("firstrun.recommended"),
                shell.tr("firstrun.recommended.desc"), true, () -> {
                    host.recommended();
                    host.settled();
                    host.finish();
                }));
        out.add(new Choice("custom", shell.tr("firstrun.custom"), shell.tr("firstrun.custom.desc"),
                false, () -> go(Step.CUSTOM)));
        if (host.upgrading()) {
            out.add(new Choice("keep", shell.tr("firstrun.keep"), shell.tr("firstrun.keep.desc"),
                    false, () -> {
                        host.settled();
                        host.finish();
                    }));
        }
        return out;
    }

    private float choiceHeight(Choice choice, float inner) {
        return 7 + 11 + paragraphHeight(choice.desc(), inner - 34) + 7;
    }

    private float setupHeight(float inner) {
        float h = 0;
        for (Choice choice : choices()) {
            h += choiceHeight(choice, inner) + 6;
        }
        return Math.max(0, h - 6);
    }

    private void setup(float left, float y, float inner) {
        for (Choice choice : choices()) {
            float ch = choiceHeight(choice, inner);
            boolean hot = over(left, y, inner, ch);
            float g = glow("choice:" + choice.key(), hot);
            Ui.halo(c, left, y, inner, ch, Ui.R2, accent, g);
            int edge = choice.primary() ? Ui.mix(Ui.alpha(accent, 0x8C), accent, g) : edge(g);
            int fill = choice.primary() ? Ui.mix(Ui.CARD, Ui.alpha(accent, 0x26), 0.7f) : Ui.CARD;
            Ui.pane(c, left, y, inner, ch, Ui.R2, Ui.mix(fill, 0x16FFFFFF, g * 0.6f), edge);
            int titleInk = choice.primary() ? accent : Ui.TEXT;
            c.text(Ui.fit(c, choice.title(), (int) (inner - 34)), left + 9, y + 7,
                   Ui.mix(titleInk, Ui.TEXT, g * 0.4f));
            float arrow = animate("arrow:" + choice.key(), hot ? 3f : 0f, 45f);
            c.icon(Icons.Icon.RIGHT, left + inner - 17 + arrow, y + ch / 2f - 3.5f, 7,
                   Ui.mix(Ui.TEXT_3, accent, g));
            paragraph(choice.desc(), left + 9, y + 19, inner - 34, Ui.TEXT_3);
            Runnable go = choice.go();
            if (visible(y, ch)) {
                zone(left, y, inner, ch, (px, py, b) -> {
                    click();
                    go.run();
                }).key = "b:" + choice.key();
            }
            y += ch + 6;
        }
    }

    private float controlWidth(Row row) {
        if (row.kind == Row.Kind.TOGGLE) {
            String on = row.onText != null ? row.onText.get() : shell.tr("mode.on");
            return 26 + 5 + Math.max(c.width(on), c.width(shell.tr("mode.off")));
        }
        float widest = 0;
        for (Row.Option option : row.options) {
            widest = Math.max(widest, c.width(option.label()));
        }
        return Math.max(1, row.options.size()) * (widest + 10) + 2;
    }

    private boolean wide(float inner) {
        Row.Tab t = currentTab();
        if (t == null) {
            return false;
        }
        for (Row.Group g : t.groups()) {
            for (Row row : g.rows()) {
                if (row.kind == Row.Kind.SEGMENT
                        && c.width(row.name.get()) + 8 + 10 + controlWidth(row) > inner - 18) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> hint(Row row, float width) {
        String text = row.hint == null ? null : row.hint.get();
        return text == null || text.isBlank() ? List.of() : Ui.wrap(c, text, (int) width);
    }

    private float rowHeight(Row row, float inner, boolean wide) {
        boolean two = wide && row.kind == Row.Kind.SEGMENT;
        return (two ? 38 : 21) + hint(row, inner - 18).size() * LINE + 5;
    }

    private float groupHeight(Row.Group g, float inner, boolean wide) {
        float h = SettingsView.GROUP_HEAD + 4;
        for (Row row : g.rows()) {
            h += rowHeight(row, inner, wide);
        }
        return h;
    }

    private float customHeight(float inner) {
        Row.Tab t = currentTab();
        float h = paragraphHeight(shell.tr("firstrun.custom.intro"), inner) + 6;
        if (t == null) {
            return h;
        }
        boolean wide = wide(inner);
        for (Row.Group g : t.groups()) {
            h += groupHeight(g, inner, wide) + 6;
        }
        return h - 6;
    }

    private void custom(float left, float y, float inner) {
        y = paragraph(shell.tr("firstrun.custom.intro"), left, y, inner, Ui.TEXT_3) + 6;
        Row.Tab t = currentTab();
        if (t == null) {
            return;
        }
        boolean wide = wide(inner);
        for (Row.Group g : t.groups()) {
            float gh = groupHeight(g, inner, wide);
            if (visible(y, gh)) {
                card(left, y, inner, gh, g.title().get(),
                     g.rows().size() > 1 ? String.valueOf(g.rows().size()) : null, false);
            }
            float ry = y + SettingsView.GROUP_HEAD + 1;
            for (Row row : g.rows()) {
                float rh = rowHeight(row, inner, wide);
                if (visible(ry, rh)) {
                    row(row, left + 2, ry, inner - 4, rh, wide);
                }
                ry += rh;
            }
            y += gh + 6;
        }
    }

    private void row(Row row, float x, float y, float w, float h, boolean wide) {
        boolean hot = over(x, y, w, h);
        float g = glow("row:" + row.id, hot);
        if (g > 0.01f) {
            c.round(x, y, w, h, Ui.R1, Ui.fade(0x0EFFFFFF, g));
        }
        float labelX = x + 7;
        float right = x + w - 7;
        boolean two = wide && row.kind == Row.Kind.SEGMENT;
        float ctrlY = two ? y + 19 : y + (21 - CTRL_H) / 2f + 1;
        float labelRoom;
        if (row.kind == Row.Kind.TOGGLE) {
            boolean on = row.on.getAsBoolean();
            String text = on ? (row.onText != null ? row.onText.get() : shell.tr("mode.on"))
                             : shell.tr("mode.off");
            labelRoom = toggle(row.id, right, ctrlY, on, text, row.flip) - 8 - labelX;
        } else if (two) {
            segmented(row.id, labelX, right, ctrlY, row.options, row.selected, row.pick,
                      host::tone);
            labelRoom = right - labelX;
        } else {
            float ctrlLeft = right - controlWidth(row);
            segmented(row.id, ctrlLeft, right, ctrlY, row.options, row.selected, row.pick,
                      host::tone);
            labelRoom = ctrlLeft - 8 - labelX;
        }
        c.text(Ui.fit(c, row.name.get(), (int) labelRoom), labelX, y + 7,
               Ui.mix(Ui.TEXT_2, Ui.TEXT, g));
        float hy = y + (two ? 38 : 21);
        for (String line : hint(row, w - 14)) {
            c.text(line, labelX, hy, Ui.HINT);
            hy += LINE;
        }
    }
}
