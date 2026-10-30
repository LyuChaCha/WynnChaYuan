package com.wynnchayuan.client.ui;

import java.util.Locale;

final class SettingsNavigation {

    private final SettingsView view;

    SettingsNavigation(SettingsView view) {
        this.view = view;
    }

    void rail() {
        float x = view.winX;
        float y = view.winY;
        boolean slim = view.railW < SettingsView.RAIL_W;
        view.clip(x, y, x + view.railW, y + view.winH);
        view.c.round(x, y, view.railW + Ui.R3 * 2, view.winH, Ui.R3, Ui.RAIL);
        view.unclip();
        view.c.fill(x + view.railW - view.c.px(), y + 1, x + view.railW, y + view.winH - 1, Ui.LINE);

        float cy;
        if (slim) {
            view.c.logo(x + (view.railW - 20) / 2f, y + 8, 20);
            cy = y + 36;
        } else {
            view.c.logo(x + 7, y + 8, 26);
            String title = view.host.title();
            float room = view.railW - 38 - 6;
            float big = view.c.scale(1.25f);
            float s = view.c.width(title) * big <= room ? big : 1f;
            view.c.text(title, x + 38, y + 21 - 4 * s, view.accent, s);
            view.c.text(title, x + 38 + view.c.px(), y + 21 - 4 * s, view.accent, s);
            cy = y + 40;
            for (String line : Ui.wrap(view.c, view.host.tagline(), (int) (view.railW - 16))) {
                view.c.text(line, x + 8, cy, Ui.TEXT_2);
                cy += 10;
            }
            view.c.text(view.host.version(), x + 8, cy, Ui.FAINT);
            cy += 15;
            cy = searchField(x + 7, cy, view.railW - 14) + 7;
        }

        boolean searching = view.searching();
        float pillY = view.animate("nav", cy + view.tab * (SettingsView.NAV_H + 2), 55f);
        if (!searching) {
            view.c.round(x + 6, pillY, view.railW - 12, SettingsView.NAV_H, Ui.R1, Ui.alpha(view.accent, 0x33));
        }
        for (int i = 0; i < view.tabs.size(); i++) {
            Row.Tab t = view.tabs.get(i);
            float ny = cy + i * (SettingsView.NAV_H + 2);
            boolean on = i == view.tab && !searching;
            boolean hot = view.over(x + 6, ny, view.railW - 12, SettingsView.NAV_H);
            float g = view.glow("nav" + i, hot && !on);
            if (g > 0.01f) {
                view.c.round(x + 6, ny, view.railW - 12, SettingsView.NAV_H, Ui.R1, Ui.fade(0x12FFFFFF, g));
            }
            int ink = on ? view.accent : (hot ? Ui.TEXT : Ui.TEXT_2);
            String name = t.name().get();
            if (slim) {
                view.c.icon(t.icon(), x + (view.railW - 12) / 2f, ny + 4.5f, 12, ink);
                if (hot) {
                    view.tip = new Surface.Tip(name, x + view.railW, ny - 4, 0, SettingsView.NAV_H);
                    view.hoverRow = "#nav" + i;
                }
            } else {
                view.c.icon(t.icon(), x + 12, ny + 5, 11, ink);
                String count = String.valueOf(rowCount(t));
                float countW = view.c.width(count);
                String shown = Ui.fit(view.c, name, (int) (view.railW - 12 - 22 - countW - 12));
                view.c.text(shown, x + 28, ny + 6.5f, ink);
                view.c.text(count, x + view.railW - 12 - countW, ny + 6.5f, on ? view.accent : Ui.FAINT);
                if (hot && !shown.equals(name)) {
                    view.tip = new Surface.Tip(name, x + view.railW, ny - 4, 0, SettingsView.NAV_H);
                    view.hoverRow = "#nav" + i;
                }
            }
            int index = i;
            view.zone(x + 6, ny, view.railW - 12, SettingsView.NAV_H, (px, py, b) -> view.switchTab(index));
        }

        float nh = 18;
        float ny = y + view.winH - 8 - nh;
        float nw = view.railW - 14;
        float nx = x + 7;
        boolean hot = view.over(nx, ny, nw, nh);
        float g = view.glow("notice", hot);
        Ui.halo(view.c, nx, ny, nw, nh, Ui.R1, view.accent, g);
        Ui.pane(view.c, nx, ny, nw, nh, Ui.R1, Ui.RAISED, view.edge(g));
        String notice = view.host.tr("notice.button");
        if (slim) {
            view.c.icon(Icons.Icon.QUEST, nx + (nw - 11) / 2f, ny + 3.5f, 11, Ui.GOLD);
            if (hot) {
                view.tip = new Surface.Tip(notice, x + view.railW, ny - 4, 0, nh);
                view.hoverRow = "#notice";
            }
        } else {
            view.c.icon(Icons.Icon.QUEST, nx + 6, ny + 3.5f, 11, Ui.GOLD);
            view.c.text(Ui.fit(view.c, notice, (int) (nw - 26)), nx + 21, ny + 5, Ui.GOLD);
        }
        view.zone(nx, ny, nw, nh, (px, py, b) -> {
            view.host.click();
            view.host.openNotice();
        });
    }

    private static int rowCount(Row.Tab t) {
        int n = 0;
        for (Row.Group g : t.groups()) {
            n += g.rows().size();
        }
        return n;
    }

    private float searchField(float x, float y, float w) {
        float h = 18;
        boolean focused = view.focus == view.search;
        boolean active = focused || view.search.text.length() > 0;
        float g = view.glow("search", focused || view.over(x, y, w, h));
        Ui.halo(view.c, x, y, w, h, Ui.R1, view.accent, focused ? 1f : g * 0.6f);
        Ui.pane(view.c, x, y, w, h, Ui.R1, Ui.FIELD, active ? view.accent : view.edge(g));
        view.c.icon(Icons.Icon.SEARCH, x + 5, y + 4, 10, active ? view.accent : Ui.TEXT_3);
        float textX = x + 19;
        float textW = w - 19 - (view.search.text.length() > 0 ? 15 : 5);
        if (view.search.text.length() == 0 && !focused) {
            view.c.text(Ui.fit(view.c, view.host.tr("search.placeholder"), (int) textW), textX, y + 5, Ui.FAINT);
        } else {
            view.field(view.search, textX, y + 5, textW, focused);
        }
        view.zone(x, y, w, h, (px, py, b) -> {
            view.closePopovers();
            view.focus = view.search;
            view.search.caret = view.search.text.length();
        });
        if (view.search.text.length() > 0) {
            float cx = x + w - 14;
            boolean ch = view.over(cx - 1, y + 3, 12, 12);
            view.c.icon(Icons.Icon.CLOSE, cx + 1, y + 5, 8, ch ? Ui.TEXT : Ui.HINT);
            view.zone(cx - 1, y + 3, 12, 12, (px, py, b) -> {
                view.search.clear();
                view.onSearchChanged();
            });
        }
        return y + h;
    }

    void header() {
        float x = view.winX + view.railW + 12;
        float right = view.winX + view.winW - 12;
        float y = view.winY;
        boolean searching = view.searching();

        String reset = view.host.tr("reset.page");
        int dirty = 0;
        for (Row.Group g : view.tabs.get(view.tab).groups()) {
            for (Row r : g.rows()) {
                if (r.resettable() && !r.isDefault.getAsBoolean()) {
                    dirty++;
                }
            }
        }
        boolean can = dirty > 0 && !searching;
        float room = right - x;
        boolean compact = view.c.width(reset) + 26 > room * 0.45f;
        float rw = compact ? view.CTRL_H : view.c.width(reset) + 26;
        float rx = right - rw;
        float ry = y + 11;
        boolean hot = can && view.over(rx, ry, rw, view.CTRL_H);
        float g = view.glow("resetpage", hot);
        Ui.halo(view.c, rx, ry, rw, view.CTRL_H, Ui.R1, view.accent, g);
        Ui.pane(view.c, rx, ry, rw, view.CTRL_H, Ui.R1, Ui.RAISED, view.edge(g));
        int ink = can ? Ui.mix(Ui.TEXT_2, Ui.TEXT, g) : Ui.fade(Ui.FAINT, 0.6f);
        view.c.icon(Icons.Icon.RESET, rx + (compact ? 3.5f : 6), ry + 3.5f, 9, ink);
        if (!compact) {
            view.c.text(reset, rx + 19, ry + 4, ink);
        } else if (view.over(rx, ry, rw, view.CTRL_H)) {
            view.tip = new Surface.Tip(reset, rx, ry, rw, view.CTRL_H);
            view.hoverRow = "#reset";
        }
        view.zone(rx, ry, rw, view.CTRL_H, (px, py, b) -> {
            if (!can) {
                return;
            }
            view.closePopovers();
            for (Row.Group grp : view.tabs.get(view.tab).groups()) {
                for (Row r : grp.rows()) {
                    if (r.resettable() && !r.isDefault.getAsBoolean()) {
                        r.reset.run();
                    }
                }
            }
            view.host.click();
            view.host.resetDone(view.tabs.get(view.tab).name().get());
        });

        float textRoom = rx - 10 - x;
        String title = searching ? view.host.tr("search.results") : view.tabs.get(view.tab).name().get();
        float s = view.c.scale(1.5f);
        if (view.c.width(title) * s > textRoom) {
            s = 1f;
        }
        view.c.text(Ui.fit(view.c, title, (int) (textRoom / s)), x, y + 8, Ui.TEXT, s);
        String about = searching
                ? view.host.tr("search.count", shownCount())
                : view.tabs.get(view.tab).about().get();
        view.c.text(Ui.fit(view.c, about, (int) textRoom), x, y + 8 + 8 * s + 4, Ui.TEXT_3);
    }

    private int shownCount() {
        String query = view.search.text.toString().strip().toLowerCase(Locale.ROOT);
        int n = 0;
        for (Row.Tab t : view.tabs) {
            for (Row.Group g : t.groups()) {
                for (Row r : g.rows()) {
                    if (SettingsView.matches(r, query)) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    void footer() {
        float y = view.winY + view.winH - SettingsView.FOOT_H;
        float left = view.winX + view.railW + 12;
        float right = view.winX + view.winW - 12;
        view.c.fill(view.winX + view.railW + (view.sheet ? 1 : 0), y, view.winX + view.winW - 1, y + view.c.px(), Ui.LINE);
        float h = 18;
        float by = y + (SettingsView.FOOT_H - h) / 2f;

        String done = view.host.tr(view.sheet ? "button.back" : "button.done");
        float dw = view.c.width(done) + 28;
        view.button("done", right - dw, by, dw, h, done, true, true, null, view.host::done);
        right -= dw + 5;

        String credits = view.host.tr("button.credits");
        float cw = view.c.width(credits) + 16;
        if (!view.sheet && right - cw - 60 > left) {
            view.button("credits", right - cw, by, cw, h, credits, true, false, null,
                   view.host::openCredits);
            right -= cw + 5;
        }

        boolean fresh = view.host.hasUpdate();
        String updates = view.host.tr(fresh ? "button.updates.new" : "button.updates");
        float uw = view.c.width(updates) + 16;
        if (!view.sheet && right - uw - 40 > left) {
            float ux = right - uw;
            boolean hot = view.over(ux, by, uw, h);
            float g = view.glow("b:updates", hot);
            Ui.halo(view.c, ux, by, uw, h, Ui.R1, fresh ? Ui.GOLD : view.accent, g);
            Ui.pane(view.c, ux, by, uw, h, Ui.R1, Ui.RAISED, fresh ? Ui.GOLD : view.edge(g));
            view.c.text(updates, ux + 8, by + 5, fresh ? Ui.GOLD : Ui.mix(Ui.TEXT_2, Ui.TEXT, g));
            view.zone(ux, by, uw, h, (px, py, b) -> {
                view.host.click();
                view.host.openUpdates();
            });
            right = ux - 8;
        }

        SettingsView.Status s = view.host.status();
        if (s != null && right - left > 30) {
            view.c.round(left, by + 6.5f, 5, 5, 2.5f, s.colour());
            view.c.text(Ui.fit(view.c, s.text(), (int) (right - left - 10)), left + 9, by + 5, s.colour());
        }
    }
}
