package com.wynnchayuan.client.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

public final class ChatLogView extends Surface {

    public record Msg(Object key, List<Styled> lines, String time) {}

    public interface Host {
        List<Msg> messages(int wrapWidth);

        boolean recording();

        void startRecording();

        boolean multi();

        void setMulti(boolean multi);

        boolean copyText(List<Object> keys);

        void close();

        default float[] dock(int screenW, int screenH) {
            return null;
        }
    }

    static final float LINE = 10;
    static final float PAD_Y = 2;
    static final float GAP = 2;
    static final float CARD_PAD = 6;
    static final float CARD_GAP = 3;
    static final long STATUS_MS = 2600;
    static final long FLASH_MS = 520;
    static final long SWEEP_MS = 460;
    static final long FADE_MS = 360;

    private final Shell shell;
    private final Host host;

    private float scroll;
    private float scrollTarget;
    private float maxScroll;
    private boolean stick = true;

    private final Set<Object> selected = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<Object> beforeDrag = Collections.newSetFromMap(new IdentityHashMap<>());
    private List<Msg> shown = List.of();
    private float[] rowTop = new float[0];
    private float[] rowBottom = new float[0];
    private Object anchor;
    private boolean dragAdd;
    private float listTop;
    private float listBottom;

    private Object hovered;
    private long hoverSince;
    private Object flashKey;
    private long flashAt;

    private String status;
    private boolean statusGood;
    private long statusAt;

    private List<Msg> shot;
    private float[] shotRect;
    private boolean shotDrawn;
    private long shotTakenAt;

    private boolean docked;
    private float[] panel = new float[4];

    public ChatLogView(Shell shell, Host host) {
        this.shell = shell;
        this.host = host;
    }

    @Override
    protected int accentColour() {
        return shell.accent();
    }

    @Override
    protected void click() {
        shell.click();
    }

    public float[] pendingShot() {
        return shot != null && shotDrawn && shotTakenAt == 0 ? shotRect : null;
    }

    public void shotTaken() {
        if (shot != null && shotTakenAt == 0) {
            shotTakenAt = now == 0 ? 1 : now;
        }
    }

    public void report(String key, boolean good) {
        status = key;
        statusGood = good;
        statusAt = now;
    }

    public boolean inside(double x, double y) {
        return Ui.in(x, y, panel[0], panel[1], panel[2], panel[3]);
    }

    public boolean busy() {
        return shot != null || dragging != null;
    }

    @Override
    public boolean scroll(double dy) {
        if (shot != null) {
            return true;
        }
        scrollBy(-(float) dy * 3 * LINE);
        return true;
    }

    private void scrollBy(float by) {
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget + by));
        stick = scrollTarget >= maxScroll - 1;
    }

    @Override
    public boolean key(int key, boolean ctrl) {
        if (shot != null) {
            return true;
        }
        if (ctrl && key == 'C') {
            if (selected.isEmpty()) {
                return false;
            }
            copyText(new ArrayList<>(selected));
            return true;
        }
        if (docked && key != KEY_ESC && key != KEY_PAGE_UP && key != KEY_PAGE_DOWN) {
            return false;
        }
        if (ctrl && key == 'A' && host.multi()) {
            for (Msg m : shown) {
                selected.add(m.key());
            }
            return true;
        }
        switch (key) {
            case KEY_ESC -> {
                if (selected.isEmpty()) {
                    return false;
                }
                selected.clear();
            }
            case KEY_UP -> scrollBy(-LINE * 2);
            case KEY_DOWN -> scrollBy(LINE * 2);
            case KEY_PAGE_UP -> scrollBy(-(listBottom - listTop - LINE));
            case KEY_PAGE_DOWN -> scrollBy(listBottom - listTop - LINE);
            case KEY_HOME -> scrollBy(-maxScroll);
            case KEY_END -> scrollBy(maxScroll);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void draw() {
        float[] dock = host.dock(screenW, screenH);
        docked = dock != null;
        float x;
        float y;
        float w;
        float h;
        if (docked) {
            x = dock[0];
            y = dock[1];
            w = dock[2];
            h = dock[3];
            dockPane(x, y, w, h);
        } else {
            w = Math.min(screenW - 20, 540);
            h = Math.min(screenH - 14, 330);
            x = Math.round((screenW - w) / 2f);
            y = Math.round((screenH - h) / 2f);
            window(x, y, w, h, shell.blurred());
            zone(0, 0, screenW, screenH, (px, py, b) -> { });
        }
        panel = new float[] {x, y, w, h};

        float left = x + (docked ? 7 : 12);
        float right = x + w - (docked ? 7 : 12);
        boolean multi = host.multi();
        float headH = docked ? 22 : 40;
        float footH = docked ? 24 : 28;

        if (docked) {
            float cs = 12;
            boolean hot = over(right - cs, y + 5, cs, cs);
            float g = glow("dock-close", hot);
            c.round(right - cs, y + 5, cs, cs, Ui.R1, Ui.fade(0x22FFFFFF, g));
            c.icon(Icons.Icon.CLOSE, right - cs + 2.5f, y + 7.5f, 7, Ui.mix(Ui.TEXT_3, Ui.TEXT, g));
            zone(right - cs, y + 5, cs, cs, (px, py, b) -> {
                click();
                host.close();
            }).key = "b:dock-close";
            right -= cs + 6;
        }

        List<Row.Option> modes = List.of(
                new Row.Option(shell.tr("chatlog.mode.multi"), Row.Tone.ACCENT, ""),
                new Row.Option(shell.tr("chatlog.mode.quick"), Row.Tone.ACCENT, ""));
        float segW = Math.min(w * 0.42f, Math.max(c.width(modes.get(0).label()),
                c.width(modes.get(1).label())) * 2 + 34);
        segmented("mode", right - segW, right, y + (docked ? 4 : 12), modes, () -> multi ? 0 : 1, i -> {
            host.setMulti(i == 0);
            selected.clear();
        }, tone -> accent);

        float s = c.scale(docked ? 1f : 1.5f);
        c.text(shell.tr("chatlog.title"), left, y + (docked ? 8 : 9), docked ? Ui.TEXT_2 : Ui.TEXT, s);

        float footY = y + h - footH;
        listTop = y + headH;
        listBottom = footY - (docked ? 3 : 6);
        if (docked) {
            right = x + w - 7;
        }
        float timeW = c.width("00:00") + 8;
        float textX = left + timeW;
        float rowRight = right - 7;
        int wrap = (int) Math.max(40, rowRight - textX - 4);
        shown = host.messages(wrap);

        if (!docked) {
            String sub = shell.tr("chatlog.count", shown.size());
            c.text(Ui.fit(c, sub, (int) (right - segW - 10 - left)), left, y + 9 + 8 * s + 4,
                   Ui.FAINT);
        }

        if (shown.isEmpty()) {
            empty(x, w);
        } else {
            list(left, textX, rowRight, multi);
        }

        footer(x, w, left, right, footY, multi);

        if (shot != null) {
            popover = this::card;
        }
    }

    private void dockPane(float x, float y, float w, float h) {
        winX = x;
        winY = y;
        winW = w;
        winH = h;
        for (int i = 3; i >= 1; i--) {
            float g = i * 1.5f;
            c.round(x - g, y - g + 2, w + g * 2, h + g * 2, Ui.R2 + g, 0x14000000);
        }
        Ui.pane(c, x, y, w, h, Ui.R2, Ui.WINDOW_SOLID, 0);
        Ui.glowSpot(c, x + w * 0.1f, y - h * 0.1f, w * 0.7f, h * 0.6f, x, y, x + w, y + h, Ui.R2,
                    Ui.GLOW_TOP);
        c.ring(x, y, w, h, Ui.R2, c.px(), Ui.WIN_EDGE);
        c.fill(x + Ui.R2, y + c.px(), x + w - Ui.R2, y + c.px() * 2, 0x26FFFFFF);
        zone(x, y, w, h, (px, py, b) -> { });
    }

    private void empty(float x, float w) {
        float mid = (listTop + listBottom) / 2f;
        boolean on = host.recording();
        String line = shell.tr(on ? "chatlog.empty" : "chatlog.off");
        c.text(Ui.fit(c, line, (int) (w - 30)), x + (w - Math.min(c.width(line), w - 30)) / 2f,
               mid - (on ? 4 : 14), Ui.TEXT_3);
        if (!on) {
            String go = shell.tr("chatlog.start");
            float bw = buttonWidth(go) + 12;
            button("start", x + (w - bw) / 2f, mid + 2, bw, 18, go, true, true, null,
                   host::startRecording);
        }
    }

    private void list(float left, float textX, float rowRight, boolean multi) {
        float listH = listBottom - listTop;
        float contentH = 0;
        int n = shown.size();
        float[] heights = new float[n];
        for (int i = 0; i < n; i++) {
            heights[i] = shown.get(i).lines().size() * LINE + PAD_Y * 2;
            contentH += heights[i] + (i > 0 ? GAP : 0);
        }
        maxScroll = Math.max(0, contentH - listH);
        if (stick) {
            scrollTarget = maxScroll;
        }
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll = Ui.ease(Math.max(0, Math.min(maxScroll, scroll)), scrollTarget, dt, 45f);

        rowTop = new float[n];
        rowBottom = new float[n];
        Object hot = null;
        clip(left - 2, listTop, rowRight + 2, listBottom);
        float ry = listTop - scroll;
        for (int i = 0; i < n; i++) {
            Msg m = shown.get(i);
            float rh = heights[i];
            rowTop[i] = ry;
            rowBottom[i] = ry + rh;
            if (ry + rh >= listTop && ry < listBottom) {
                boolean over = shot == null && over(left - 2, ry, rowRight - left + 4, rh);
                if (over) {
                    hot = m.key();
                }
                row(m, i, left, textX, rowRight, ry, rh, over, multi);
            }
            ry += rh + GAP;
        }
        unclip();
        if (hot != hovered) {
            hovered = hot;
            hoverSince = now;
        }
        scrollbar(rowRight + 4, listTop, listH, contentH, scroll, maxScroll, to -> {
            scroll = scrollTarget = (float) to;
            stick = scrollTarget >= maxScroll - 1;
        });
    }

    private void row(Msg m, int index, float left, float textX, float rowRight, float ry,
                     float rh, boolean over, boolean multi) {
        float rx = left - 2;
        float rw = rowRight - rx;
        String id = "r" + System.identityHashCode(m.key());
        boolean on = multi && selected.contains(m.key());
        float g = glow(id, over);
        float pick = animate("s:" + id, on ? 1f : 0f, 45f);

        if (pick > 0.01f) {
            c.round(rx, ry, rw, rh, Ui.R1, Ui.fade(Ui.alpha(accent, 0x22), pick));
        }
        if (g > 0.01f) {
            glass(rx, ry, rw, rh, g, m.key());
        }
        if (pick > 0.01f) {
            dashed(rx, ry, rw, rh, Ui.fade(accent, pick), (now / 110f) % 5f);
        }
        if (m.key() == flashKey && now - flashAt < FLASH_MS) {
            float t = (now - flashAt) / (float) FLASH_MS;
            c.round(rx, ry, rw, rh, Ui.R1, Ui.fade(Ui.alpha(accent, 0x66), 1f - t));
        }

        c.text(m.time(), left + 1, ry + PAD_Y + 0.5f, on || over ? Ui.TEXT_3 : Ui.FAINT);
        float ly = ry + PAD_Y;
        for (Styled line : m.lines()) {
            if (ly + LINE >= listTop && ly < listBottom) {
                c.styled(line, textX, ly, 1f);
            }
            ly += LINE;
        }

        Zone z = zone(rx, ry, rw, rh, (px, py, b) -> press(m, index, b));
        z.key = "row:" + index;
        if (multi) {
            z.drag = (px, py, b) -> dragTo(py);
            z.release = beforeDrag::clear;
        }
    }

    private void glass(float x, float y, float w, float h, float g, Object key) {
        c.round(x, y, w, h, Ui.R1, Ui.fade(0x12FFFFFF, g));
        c.fill(x + Ui.R1, y, x + w - Ui.R1, y + c.px(), Ui.fade(0x40FFFFFF, g));
        c.fill(x + Ui.R1, y + h - c.px(), x + w - Ui.R1, y + h, Ui.fade(Ui.alpha(accent, 0x30), g));
        if (key != hovered) {
            return;
        }
        float t = (now - hoverSince) / (float) SWEEP_MS;
        if (t <= 0 || t >= 1) {
            return;
        }
        float e = 1f - (1f - t) * (1f - t) * (1f - t);
        float band = Math.min(48, w * 0.18f);
        float cx = x - band + (w + band * 2) * e;
        clip(x, y, x + w, y + h);
        int steps = 8;
        for (int i = 0; i < steps; i++) {
            float k = 1f - Math.abs((i + 0.5f) / steps * 2f - 1f);
            float bx = cx - band / 2f + band * i / steps;
            c.fill(bx, y, bx + band / steps + c.px(), y + h,
                   Ui.fade(0x2AFFFFFF, k * g * (1f - t * 0.6f)));
        }
        unclip();
    }

    private void dashed(float x, float y, float w, float h, int argb, float phase) {
        float t = Math.max(c.px(), 0.5f);
        float dash = 3;
        float period = 5;
        for (float d = -phase; d < w; d += period) {
            float a = Math.max(0, d);
            float b = Math.min(w, d + dash);
            if (b > a) {
                c.fill(x + a, y, x + b, y + t, argb);
                c.fill(x + w - b, y + h - t, x + w - a, y + h, argb);
            }
        }
        for (float d = -phase; d < h; d += period) {
            float a = Math.max(0, d);
            float b = Math.min(h, d + dash);
            if (b > a) {
                c.fill(x + w - t, y + a, x + w, y + b, argb);
                c.fill(x, y + h - b, x + t, y + h - a, argb);
            }
        }
    }

    private void press(Msg m, int index, int button) {
        if (shot != null) {
            return;
        }
        if (!host.multi()) {
            flashKey = m.key();
            flashAt = now;
            if (button == 1) {
                copyText(List.of(m.key()));
            } else if (button == 0) {
                copyImage(List.of(m));
            }
            return;
        }
        if (button != 0) {
            return;
        }
        anchor = m.key();
        beforeDrag.clear();
        beforeDrag.addAll(selected);
        dragAdd = !selected.contains(m.key());
        if (dragAdd) {
            selected.add(m.key());
        } else {
            selected.remove(m.key());
        }
    }

    private void dragTo(double py) {
        if (anchor == null || shown.isEmpty()) {
            return;
        }
        if (py < listTop + 6) {
            scrollBy(-LINE);
        } else if (py > listBottom - 6) {
            scrollBy(LINE);
        }
        int from = indexOf(anchor);
        int to = indexAt(py);
        if (from < 0 || to < 0) {
            return;
        }
        selected.clear();
        selected.addAll(beforeDrag);
        for (int i = Math.min(from, to); i <= Math.max(from, to); i++) {
            Object key = shown.get(i).key();
            if (dragAdd) {
                selected.add(key);
            } else {
                selected.remove(key);
            }
        }
    }

    private int indexOf(Object key) {
        for (int i = 0; i < shown.size(); i++) {
            if (shown.get(i).key() == key) {
                return i;
            }
        }
        return -1;
    }

    private int indexAt(double py) {
        if (rowTop.length == 0) {
            return -1;
        }
        for (int i = 0; i < rowTop.length; i++) {
            if (py < rowBottom[i] + GAP) {
                return i;
            }
        }
        return rowTop.length - 1;
    }

    private List<Msg> picked() {
        List<Msg> out = new ArrayList<>();
        for (Msg m : shown) {
            if (selected.contains(m.key())) {
                out.add(m);
            }
        }
        return out;
    }

    private void copyText(List<Object> keys) {
        List<Object> ordered = new ArrayList<>();
        for (Msg m : shown) {
            if (keys.contains(m.key())) {
                ordered.add(m.key());
            }
        }
        if (ordered.isEmpty()) {
            return;
        }
        boolean ok = host.copyText(ordered);
        lastCount = ordered.size();
        report(ok ? "chatlog.copied.text" : "chatlog.copy.failed", ok);
    }

    private int lastCount;

    private void copyImage(List<Msg> msgs) {
        if (msgs.isEmpty()) {
            return;
        }
        float h = CARD_PAD * 2;
        float w = 0;
        for (int i = 0; i < msgs.size(); i++) {
            for (Styled line : msgs.get(i).lines()) {
                w = Math.max(w, line.width());
                h += LINE;
            }
            if (i > 0) {
                h += CARD_GAP;
            }
        }
        if (h > screenH - 8) {
            report("chatlog.image.toolong", false);
            return;
        }
        w = Math.min(screenW - 8, w + CARD_PAD * 2);
        float x = Math.round((screenW - w) / 2f);
        float y = Math.round((screenH - h) / 2f);
        shot = msgs;
        shotRect = new float[] {x, y, w, h};
        shotDrawn = false;
        shotTakenAt = 0;
        lastCount = msgs.size();
    }

    private void card() {
        float x = shotRect[0];
        float y = shotRect[1];
        float w = shotRect[2];
        float h = shotRect[3];
        float alpha = 1f;
        if (shotTakenAt != 0) {
            float t = (now - shotTakenAt) / (float) FADE_MS;
            if (t >= 1f) {
                shot = null;
                shotRect = null;
                return;
            }
            alpha = 1f - t * t;
        }
        for (int i = 3; i >= 1; i--) {
            float g = i * 2f;
            c.round(x - g, y - g + 2, w + g * 2, h + g * 2, Ui.R2 + g, Ui.fade(0x22000000, alpha));
        }
        c.ring(x - 2, y - 2, w + 4, h + 4, Ui.R1, c.px(), Ui.fade(accent, alpha));
        c.fill(x, y, x + w, y + h, Ui.fade(0xFF101512, alpha));
        float ly = y + CARD_PAD;
        for (int i = 0; i < shot.size(); i++) {
            if (i > 0) {
                ly += CARD_GAP;
            }
            for (Styled line : shot.get(i).lines()) {
                c.styled(line, x + CARD_PAD, ly, alpha);
                ly += LINE;
            }
        }
        if (shotTakenAt == 0) {
            shotDrawn = true;
        }
    }

    private void footer(float x, float w, float left, float right, float footY, boolean multi) {
        c.fill(x + 1, footY, x + w - 1, footY + c.px(), Ui.LINE);
        float bh = docked ? 16 : 18;
        float by = footY + (docked ? 4 : 5);
        float end = right;
        if (!docked) {
            String close = shell.tr("button.close");
            float cw = buttonWidth(close) + 10;
            button("close", end - cw, by, cw, bh, close, true, false, null, host::close);
            end -= cw + 5;
        }

        if (multi && !shown.isEmpty()) {
            boolean any = !selected.isEmpty();
            String text = shell.tr("chatlog.copy.text");
            String image = shell.tr("chatlog.copy.image");
            String clear = shell.tr("chatlog.clear");
            float room = end - left - (docked ? 40 : 90);
            float tw = buttonWidth(text) + 8;
            float iw = buttonWidth(image) + 8;
            float xw = buttonWidth(clear) + 4;
            float need = tw + iw + xw + 10;
            if (need > room) {
                float k = Math.max(0.5f, room / need);
                tw *= k;
                iw *= k;
                xw *= k;
            }
            button("text", end - tw, by, tw, bh, text, any, true, null,
                   () -> copyText(new ArrayList<>(selected)));
            end -= tw + 5;
            button("image", end - iw, by, iw, bh, image, any, false, null,
                   () -> copyImage(picked()));
            end -= iw + 5;
            if (any) {
                button("clear", end - xw, by, xw, bh, clear, true, false, null, selected::clear);
                end -= xw + 5;
            }
        }

        String line;
        int ink;
        if (status != null && now - statusAt < STATUS_MS) {
            line = shell.tr(status, lastCount);
            ink = statusGood ? Ui.GREEN : Ui.RED;
        } else if (shown.isEmpty()) {
            line = "";
            ink = Ui.FAINT;
        } else if (!multi) {
            line = shell.tr("chatlog.hint.quick");
            ink = Ui.TEXT_3;
        } else if (selected.isEmpty()) {
            line = shell.tr("chatlog.hint.multi");
            ink = Ui.FAINT;
        } else {
            line = shell.tr("chatlog.selected", selected.size());
            ink = accent;
        }
        if (end - left > 30 && !line.isEmpty()) {
            c.text(Ui.fit(c, line, (int) (end - left - 4)), left, by + (bh - 8) / 2f, ink);
        }
    }
}
