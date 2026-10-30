package com.wynnchayuan.client.ui;

import java.util.List;

final class SettingsControls {

    private final SettingsView view;

    SettingsControls(SettingsView view) {
        this.view = view;
    }

    void control(Row r, float left, float right, float y) {
        switch (r.kind) {
            case TOGGLE -> toggle(r, right, y);
            case SEGMENT -> segment(r, left, right, y);
            case SLIDER -> slider(r, left, right, y);
            case COLOUR -> colour(r, right, y);
            case SELECT -> select(r, left, right, y);
            case ACTION -> action(r, left, right, y);
            default -> status(r, left, right, y);
        }
    }

    private void toggle(Row r, float right, float y) {
        boolean on = r.on.getAsBoolean();
        String text = on ? (r.onText != null ? r.onText.get() : view.host.tr("mode.on"))
                         : view.host.tr("mode.off");
        view.toggle(r.id, right, y, on, text, r.flip);
    }

    private void segment(Row r, float left, float right, float y) {
        float end = right;
        if (r.extra != null) {
            String label = r.extraLabel.get();
            float bw = view.c.width(label) + 12;
            view.button("x:" + r.id, end - bw, y, bw, view.CTRL_H, label, true, false, null, r.extra);
            end -= bw + 5;
        }
        view.segmented(r.id, left, end, y, r.options, r.selected, r.pick, view.host::tone);
    }

    private float resetButton(Row r, float right, float y) {
        float x = right - view.CTRL_H;
        boolean can = !r.isDefault.getAsBoolean();
        boolean hot = can && view.over(x, y, view.CTRL_H, view.CTRL_H);
        float g = view.glow("rs:" + r.id, hot);
        Ui.halo(view.c, x, y, view.CTRL_H, view.CTRL_H, Ui.R1, view.accent, g);
        Ui.pane(view.c, x, y, view.CTRL_H, view.CTRL_H, Ui.R1, Ui.RAISED, view.edge(g));
        view.c.icon(Icons.Icon.RESET, x + 3.5f, y + 3.5f, 9,
               can ? Ui.mix(Ui.TEXT_2, Ui.TEXT, g) : Ui.fade(Ui.FAINT, 0.6f));
        if (view.over(x, y, view.CTRL_H, view.CTRL_H)) {
            view.tip = new Surface.Tip(view.host.tr("reset.row"), x, y, view.CTRL_H, view.CTRL_H);
            view.hoverRow = "#rs:" + r.id;
        }
        view.zone(x, y, view.CTRL_H, view.CTRL_H, (px, py, b) -> {
            if (can) {
                view.host.click();
                r.reset.run();
                view.host.resetDone(r.name.get());
            }
        });
        return x;
    }

    private void slider(Row r, float left, float right, float y) {
        float end = r.resettable() ? resetButton(r, right, y) - 5 : right;
        int value = r.value.getAsInt();
        String text = r.format.apply(value);
        float textW = view.valueWidth(r);
        view.c.text(text, end - view.c.width(text), y + 4, Ui.TEXT);
        end -= textW + 6;
        float x = left + 5;
        float w = Math.max(30, end - x - 5);
        int span = Math.max(1, r.max - r.min);
        float t = (Math.max(r.min, Math.min(r.max, value)) - r.min) / (float) span;
        String key = "d:" + r.id;
        boolean dragged = view.dragging != null && key.equals(view.draggingKey);
        boolean hot = view.over(x - 5, y, w + 10, view.CTRL_H) || dragged;
        float g = view.glow("sl:" + r.id, hot);
        float ty = y + view.CTRL_H / 2f - 1.5f;
        view.c.round(x, ty, w, 3, 1.5f, Ui.TRACK_OFF);
        float knobX = x + w * t;
        view.c.round(x, ty, Math.max(3, knobX - x), 3, 1.5f, view.accent);
        float size = 9 + g * 1.5f;
        Ui.halo(view.c, knobX - size / 2f, y + (view.CTRL_H - size) / 2f, size, size, size / 2f, view.accent, g);
        view.c.round(knobX - size / 2f, y + (view.CTRL_H - size) / 2f, size, size, size / 2f, 0xFFFFFFFF);
        view.c.round(knobX - 2, y + view.CTRL_H / 2f - 2, 4, 4, 2, view.accent);
        float fx = x;
        float fw = w;
        Surface.Zone z = view.zone(x - 6, y - 2, w + 12, view.CTRL_H + 4, (px, py, b) -> slideTo(r, fx, fw, px));
        z.drag = (px, py, b) -> slideTo(r, fx, fw, px);
        z.release = r.commit;
        z.key = key;
    }

    private void slideTo(Row r, float x, float w, double px) {
        float t = (float) Math.max(0, Math.min(1, (px - x) / Math.max(1f, w)));
        int next = r.min + Math.round(t * (r.max - r.min));
        if (next != r.value.getAsInt()) {
            r.slide.accept(next);
        }
    }

    private void colour(Row r, float right, float y) {
        float end = r.resettable() ? resetButton(r, right, y) - 5 : right;
        String hex = r.hex.get();
        float w = view.swatchWidth();
        float x = end - w;
        boolean open = view.pickerOpen && view.pickerRow == r;
        boolean hot = view.over(x, y, w, view.CTRL_H);
        float g = view.glow("c:" + r.id, hot || open);
        Ui.halo(view.c, x, y, w, view.CTRL_H, Ui.R1, view.accent, g);
        Ui.pane(view.c, x, y, w, view.CTRL_H, Ui.R1, Ui.FIELD, Ui.mix(Ui.BORDER, view.accent, g));
        Ui.pane(view.c, x + 4, y + 3.5f, 12, 9, 2.5f, Ui.parseHex(hex, view.accent), 0x66FFFFFF);
        view.c.text(hex, x + 21, y + 4, Ui.TEXT);
        view.c.icon(open ? Icons.Icon.UP : Icons.Icon.DOWN, x + w - 11, y + 4.5f, 7,
               open ? view.accent : Ui.TEXT_3);
        view.zone(x, y, w, view.CTRL_H, (px, py, b) -> {
            view.host.click();
            boolean was = view.pickerOpen && view.pickerRow == r;
            view.closePopovers();
            if (!was) {
                view.pickerOpen = true;
                view.pickerRow = r;
                float[] next = Ui.toHsv(Ui.parseHex(r.hex.get(), view.accent));
                System.arraycopy(next, 0, view.hsv, 0, 3);
                view.hexField.set(r.hex.get());
                view.anim.put("pop", 0f);
            }
        });
        if (open) {
            float ax = x + w;
            view.popover = () -> picker(r, ax, y + view.CTRL_H + 3, y - 3);
        }
    }

    private static final String[] PRESETS = {
        "#6FA8D8", "#8FD694", "#E0BC6E", "#E0706B", "#B48EDE", "#5FC8C0", "#F0A35E", "#C5CFDA"};

    private void picker(Row r, float anchorRight, float below, float above) {
        float w = 156;
        float svH = 70;
        float h = 8 + 10 + 6 + svH + 6 + 8 + 6 + 12 + 6 + view.CTRL_H + 8;
        float x = Math.max(view.winX + view.railW + 6, Math.min(view.winX + view.winW - w - 6, anchorRight - w));
        float y = above - h >= view.winY + 6 ? above - h : Math.min(below, view.winY + view.winH - h - 6);
        float in = view.animate("pop", 1f, 45f);
        y += (1f - in) * 4f;
        view.popPane(x, y, w, h, in);
        view.zone(x, y, w, h, (px, py, b) -> view.focus = null);

        float sx = x + 8;
        float sw = w - 16;
        float cy = y + 8;
        view.c.text(Ui.fit(view.c, r.name.get(), (int) (sw - 14)), sx, cy + 1, Ui.fade(Ui.TEXT_3, in));
        boolean closeHot = view.over(sx + sw - 11, cy - 1, 12, 12);
        view.c.icon(Icons.Icon.CLOSE, sx + sw - 9, cy + 1, 8, closeHot ? Ui.TEXT : Ui.HINT);
        view.zone(sx + sw - 11, cy - 1, 12, 12, (px, py, b) -> view.closePopovers());
        cy += 16;

        float sy = cy;
        view.c.svSquare(sx, sy, sw, svH, view.hsv[0]);
        view.c.ring(sx, sy, sw, svH, 2, view.c.px(), Ui.BORDER);
        float hx = sx + view.hsv[1] * sw;
        float hy = sy + (1f - view.hsv[2]) * svH;
        view.c.ring(hx - 4.5f, hy - 4.5f, 9, 9, 4.5f, 1f, Ui.ON_ACCENT);
        view.c.ring(hx - 3.5f, hy - 3.5f, 7, 7, 3.5f, 1f, 0xFFFFFFFF);
        Surface.Zone sv = view.zone(sx, sy, sw, svH, (px, py, b) -> pickSv(r, sx, sy, sw, svH, px, py));
        sv.drag = (px, py, b) -> pickSv(r, sx, sy, sw, svH, px, py);
        sv.release = r.commit;
        sv.key = "sv";
        cy += svH + 6;

        float by = cy;
        view.c.hueBar(sx, by, sw, 8);
        view.c.ring(sx, by, sw, 8, 2, view.c.px(), Ui.BORDER);
        float kx = sx + view.hsv[0] / 360f * sw;
        view.c.round(kx - 2.5f, by - 2, 5, 12, 2.5f, 0xFFFFFFFF);
        view.c.ring(kx - 2.5f, by - 2, 5, 12, 2.5f, view.c.px(), Ui.ON_ACCENT);
        Surface.Zone hue = view.zone(sx, by - 2, sw, 12, (px, py, b) -> pickHue(r, sx, sw, px));
        hue.drag = (px, py, b) -> pickHue(r, sx, sw, px);
        hue.release = r.commit;
        hue.key = "hue";
        cy += 8 + 6;

        float step = (sw + 3) / PRESETS.length;
        String current = r.hex.get();
        for (int i = 0; i < PRESETS.length; i++) {
            String p = PRESETS[i];
            float px0 = sx + i * step;
            boolean on = p.equalsIgnoreCase(current);
            boolean hot = view.over(px0, cy, step - 3, 12);
            Ui.pane(view.c, px0, cy, step - 3, 12, 3, Ui.parseHex(p, view.accent),
                    on ? 0xFFFFFFFF : (hot ? Ui.TEXT_2 : 0x40FFFFFF));
            view.zone(px0, cy, step - 3, 12, (qx, qy, b) -> {
                view.host.click();
                if (r.setHex.test(p)) {
                    float[] next = Ui.toHsv(Ui.parseHex(p, view.accent));
                    System.arraycopy(next, 0, view.hsv, 0, 3);
                    view.hexField.set(p);
                    r.commit.run();
                }
            });
        }
        cy += 12 + 6;

        Ui.pane(view.c, sx, cy, view.CTRL_H, view.CTRL_H, Ui.R1, Ui.parseHex(current, view.accent), 0x66FFFFFF);
        String def = view.host.tr("reset.default");
        float dw = Math.min(view.c.width(def) + 12, sw - view.CTRL_H - 5 - 50 - 5);
        float fx = sx + view.CTRL_H + 5;
        float fw = sw - view.CTRL_H - 5 - dw - 5;
        boolean focused = view.focus == view.hexField;
        Ui.pane(view.c, fx, cy, fw, view.CTRL_H, Ui.R1, Ui.FIELD, focused ? view.accent : Ui.BORDER);
        view.field(view.hexField, fx + 5, cy + 4, fw - 10, focused);
        view.zone(fx, cy, fw, view.CTRL_H, (qx, qy, b) -> {
            view.focus = view.hexField;
            view.hexField.caret = view.hexField.text.length();
            view.hexField.all = true;
        });
        view.button("pd", sx + sw - dw, cy, dw, view.CTRL_H, def,
               r.resettable() && !r.isDefault.getAsBoolean(), false, null, () -> {
                   r.reset.run();
                   float[] next = Ui.toHsv(Ui.parseHex(r.hex.get(), view.accent));
                   System.arraycopy(next, 0, view.hsv, 0, 3);
                   view.hexField.set(r.hex.get());
               });
    }

    private void pickSv(Row r, float sx, float sy, float sw, float sh, double px, double py) {
        view.hsv[1] = (float) Math.max(0, Math.min(1, (px - sx) / Math.max(1f, sw)));
        view.hsv[2] = 1f - (float) Math.max(0, Math.min(1, (py - sy) / Math.max(1f, sh)));
        applyHsv(r);
    }

    private void pickHue(Row r, float sx, float sw, double px) {
        view.hsv[0] = 359.9f * (float) Math.max(0, Math.min(1, (px - sx) / Math.max(1f, sw)));
        applyHsv(r);
    }

    private void applyHsv(Row r) {
        String hex = Ui.toHex(Ui.hsv(view.hsv[0], view.hsv[1], view.hsv[2]));
        if (r.setHex.test(hex)) {
            view.hexField.set(hex);
        }
    }

    private void select(Row r, float left, float right, float y) {
        float end = right;
        boolean locked = r.locked.getAsBoolean();
        if (r.apply != null) {
            String label = view.host.tr("button.apply");
            float bw = view.c.width(label) + 14;
            boolean can = !locked && r.canApply.getAsBoolean();
            view.button("ap:" + r.id, end - bw, y, bw, view.CTRL_H, label, can, can, null, r.apply);
            end -= bw + 5;
        }
        List<Row.Choice> list = r.choices.get();
        String id = r.current.get();
        Row.Choice cur = null;
        for (Row.Choice ch : list) {
            if (ch.id().equals(id)) {
                cur = ch;
            }
        }
        if (cur == null && !list.isEmpty()) {
            cur = list.get(0);
        }
        float x = left;
        float w = Math.max(50, end - left);
        boolean open = r.id.equals(view.openSelect);
        boolean hot = !locked && view.over(x, y, w, view.CTRL_H);
        float g = view.glow("s:" + r.id, hot || open);
        Ui.halo(view.c, x, y, w, view.CTRL_H, Ui.R1, view.accent, g);
        Ui.pane(view.c, x, y, w, view.CTRL_H, Ui.R1, Ui.FIELD, Ui.mix(Ui.BORDER, view.accent, g));
        String busy = r.busy.get();
        float tx = x + 5;
        if (cur != null && busy == null) {
            tx = badge(cur, x + 3, y + 3, true);
        }
        String text = busy != null ? busy : (cur == null ? "" : cur.label());
        view.c.text(Ui.fit(view.c, text, (int) (x + w - 14 - tx)), tx, y + 4, locked ? Ui.HINT : Ui.TEXT);
        view.c.icon(open ? Icons.Icon.UP : Icons.Icon.DOWN, x + w - 11, y + 4.5f, 7,
               open ? view.accent : Ui.TEXT_3);
        view.zone(x, y, w, view.CTRL_H, (px, py, b) -> {
            if (locked) {
                return;
            }
            view.host.click();
            boolean was = r.id.equals(view.openSelect);
            view.closePopovers();
            if (!was) {
                view.openSelect = r.id;
                view.anim.put("pop", 0f);
            }
        });
        if (open) {
            float fw = w;
            view.popover = () -> dropdown(r, list, id, x, y + view.CTRL_H + 3, y - 3, fw);
        }
    }

    private float badge(Row.Choice ch, float x, float y, boolean on) {
        String text = ch.badge();
        if (text == null || text.isEmpty()) {
            return x + 2;
        }
        float w = Math.max(14, view.c.width(text) + 5);
        int bg = ch.auto() ? Ui.GOLD : (on ? view.accent : Ui.HINT);
        view.c.round(x, y, w, 10, 3, bg);
        view.c.text(text, x + (w - view.c.width(text)) / 2f, y + 1, Ui.ON_ACCENT);
        return x + w + 5;
    }

    private void dropdown(Row r, List<Row.Choice> list, String id, float ax, float below,
                          float above, float minW) {
        float itemH = 15;
        float w = minW;
        for (Row.Choice ch : list) {
            w = Math.max(w, view.c.width(ch.label()) + 48);
        }
        w = Math.min(w, view.winW - view.railW - 16);
        float h = list.size() * itemH + 6;
        float x = Math.max(view.winX + view.railW + 6, Math.min(view.winX + view.winW - w - 6, ax));
        float y = below + h <= view.winY + view.winH - 6 ? below : Math.max(view.winY + 6, above - h);
        float in = view.animate("pop", 1f, 45f);
        y += (1f - in) * 4f;
        view.popPane(x, y, w, h, in);
        view.zone(x, y, w, h, (px, py, b) -> { });
        for (int i = 0; i < list.size(); i++) {
            Row.Choice ch = list.get(i);
            float iy = y + 3 + i * itemH;
            boolean on = ch.id().equals(id);
            boolean hot = view.over(x + 3, iy, w - 6, itemH);
            if (hot) {
                view.c.round(x + 3, iy, w - 6, itemH, 3.5f, Ui.alpha(view.accent, 0x38));
            }
            float tx = badge(ch, x + 6, iy + 2.5f, on);
            view.c.text(Ui.fit(view.c, ch.label(), (int) (x + w - 18 - tx)), tx, iy + 3.5f,
                   Ui.fade(on || hot ? Ui.TEXT : Ui.TEXT_2, in));
            if (on) {
                view.c.icon(Icons.Icon.CHECK, x + w - 15, iy + 3.5f, 8, view.accent);
            }
            view.zone(x + 3, iy, w - 6, itemH, (px, py, b) -> {
                view.host.click();
                view.openSelect = null;
                r.choose.accept(ch.id());
            });
        }
    }

    private void action(Row r, float left, float right, float y) {
        String label = r.label.get();
        if (r.danger != null) {
            float w = Math.min(view.c.width(label) + 16, right - left);
            view.dangerButton("a:" + r.id, right - w, y, w, view.CTRL_H, label, r.enabled.getAsBoolean(),
                         r.danger.getAsBoolean(), r.run);
            return;
        }
        float w = Math.min(view.c.width(label) + 22, right - left);
        view.button("a:" + r.id, right - w, y, w, view.CTRL_H, label, r.enabled.getAsBoolean(), false,
               Icons.Icon.RIGHT, r.run);
    }

    private void status(Row r, float left, float right, float y) {
        String label = r.label.get();
        int ink = r.labelColour != null ? r.labelColour.getAsInt() : Ui.TEXT;
        float w = Math.min(view.c.width(label) + 22, right - left);
        float x = right - w;
        boolean hot = view.over(x, y, w, view.CTRL_H);
        float g = view.glow("st:" + r.id, hot);
        Ui.halo(view.c, x, y, w, view.CTRL_H, Ui.R1, view.accent, g);
        Ui.pane(view.c, x, y, w, view.CTRL_H, Ui.R1, Ui.FIELD, view.edge(g));
        String text = Ui.fit(view.c, label, (int) (w - 18));
        float tx = x + (w - view.c.width(text) - 9) / 2f;
        view.c.round(tx, y + 5.5f, 5, 5, 2.5f, ink);
        view.c.text(text, tx + 9, y + 4, ink);
        view.zone(x, y, w, view.CTRL_H, (px, py, b) -> {
            view.host.click();
            r.run.run();
        });
    }
}
