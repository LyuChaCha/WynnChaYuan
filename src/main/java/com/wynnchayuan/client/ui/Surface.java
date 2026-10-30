package com.wynnchayuan.client.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Surface {

    public static final int KEY_ESC = 256;
    public static final int KEY_ENTER = 257;
    public static final int KEY_BACKSPACE = 259;
    public static final int KEY_DELETE = 261;
    public static final int KEY_RIGHT = 262;
    public static final int KEY_LEFT = 263;
    public static final int KEY_DOWN = 264;
    public static final int KEY_UP = 265;
    public static final int KEY_PAGE_UP = 266;
    public static final int KEY_PAGE_DOWN = 267;
    public static final int KEY_HOME = 268;
    public static final int KEY_END = 269;
    public static final int KEY_KP_ENTER = 335;

    protected static final float CTRL_H = 16;
    private static final long TIP_DELAY_MS = 380;

    protected Canvas c;
    protected int accent;
    protected float mx;
    protected float my;
    protected long now;
    protected float dt;
    protected int screenW;
    protected int screenH;
    protected float winX;
    protected float winY;
    protected float winW;
    protected float winH;
    protected Runnable popover;
    protected Tip tip;
    protected String hoverRow;

    protected Zone dragging;
    protected String draggingKey;

    private final List<Zone> zones = new ArrayList<>();
    private final List<float[]> clips = new ArrayList<>();
    protected final Map<String, Float> anim = new HashMap<>();
    private final Map<String, Integer> colours = new HashMap<>();
    private boolean inPopover;
    private long hoverSince;
    private long lastFrame;

    protected abstract int accentColour();

    protected abstract void draw();

    protected boolean covered() {
        return false;
    }

    protected void dismiss() {
    }

    protected String clipboard() {
        return "";
    }

    protected void setClipboard(String text) {
    }

    protected void click() {
    }

    public final void render(Canvas canvas, int screenW, int screenH, double mouseX,
                             double mouseY, long nowMs) {
        this.c = canvas;
        this.screenW = screenW;
        this.screenH = screenH;
        this.mx = (float) mouseX;
        this.my = (float) mouseY;
        this.now = nowMs;
        this.dt = lastFrame == 0 ? 16f : Math.max(0f, Math.min(100f, nowMs - lastFrame));
        this.lastFrame = nowMs;
        this.accent = 0xFF000000 | (accentColour() & 0xFFFFFF);
        zones.clear();
        clips.clear();
        tip = null;
        popover = null;
        inPopover = false;
        String wasHover = hoverRow;
        hoverRow = null;

        draw();

        if (popover != null) {
            zone(0, 0, screenW, screenH, (x, y, b) -> dismiss());
            inPopover = true;
            popover.run();
            inPopover = false;
        }

        if (hoverRow == null || !hoverRow.equals(wasHover)) {
            hoverSince = now;
        }
        if (tip != null && !covered() && dragging == null && now - hoverSince >= TIP_DELAY_MS) {
            tooltip(tip, Math.min(1f, (now - hoverSince - TIP_DELAY_MS) / 120f));
        }
    }

    protected interface Press {
        void at(double x, double y, int button);
    }

    protected static final class Zone {
        float x;
        float y;
        float w;
        float h;
        Press press;
        public Press drag;
        public Runnable release;
        public String key;
    }

    protected Zone zone(float x, float y, float w, float h, Press press) {
        Zone z = new Zone();
        float x0 = x;
        float y0 = y;
        float x1 = x + w;
        float y1 = y + h;
        for (float[] clip : clips) {
            x0 = Math.max(x0, clip[0]);
            y0 = Math.max(y0, clip[1]);
            x1 = Math.min(x1, clip[2]);
            y1 = Math.min(y1, clip[3]);
        }
        z.x = x0;
        z.y = y0;
        z.w = Math.max(0, x1 - x0);
        z.h = Math.max(0, y1 - y0);
        z.press = press;
        zones.add(z);
        return z;
    }

    protected void clip(float x0, float y0, float x1, float y1) {
        c.clip(x0, y0, x1, y1);
        clips.add(new float[] {x0, y0, x1, y1});
    }

    protected void unclip() {
        c.unclip();
        clips.remove(clips.size() - 1);
    }

    protected boolean over(float x, float y, float w, float h) {
        if (dragging != null || (covered() && !inPopover)) {
            return false;
        }
        for (float[] clip : clips) {
            if (mx < clip[0] || mx >= clip[2] || my < clip[1] || my >= clip[3]) {
                return false;
            }
        }
        return Ui.in(mx, my, x, y, w, h);
    }

    public boolean mouseDown(double x, double y, int button) {
        for (int i = zones.size() - 1; i >= 0; i--) {
            Zone z = zones.get(i);
            if (z.w > 0 && z.h > 0 && Ui.in(x, y, z.x, z.y, z.w, z.h)) {
                if (z.drag != null) {
                    dragging = z;
                    draggingKey = z.key;
                }
                z.press.at(x, y, button);
                return true;
            }
        }
        return false;
    }

    public boolean mouseDrag(double x, double y) {
        if (dragging == null) {
            return false;
        }
        dragging.drag.at(x, y, 0);
        return true;
    }

    public boolean mouseUp() {
        if (dragging == null) {
            return false;
        }
        Zone z = dragging;
        dragging = null;
        draggingKey = null;
        if (z.release != null) {
            z.release.run();
        }
        return true;
    }

    public boolean scroll(double dy) {
        return false;
    }

    public float[] spot(String key) {
        for (int i = zones.size() - 1; i >= 0; i--) {
            Zone z = zones.get(i);
            if (key.equals(z.key)) {
                return new float[] {z.x, z.y, z.w, z.h};
            }
        }
        return null;
    }

    public java.util.Map<String, float[]> spots() {
        java.util.Map<String, float[]> out = new java.util.LinkedHashMap<>();
        for (Zone z : zones) {
            if (z.key != null) {
                out.put(z.key, new float[] {z.x, z.y, z.w, z.h});
            }
        }
        return out;
    }

    public boolean key(int key, boolean ctrl) {
        return false;
    }

    public boolean typed(String text) {
        return false;
    }

    protected float animate(String key, float target, float tau) {
        float at = anim.getOrDefault(key, target);
        at = Ui.ease(at, target, dt, tau);
        anim.put(key, at);
        return at;
    }

    protected float glow(String key, boolean hot) {
        return animate("g:" + key, hot ? 1f : 0f, hot ? 35f : 90f);
    }

    protected int colourTo(String key, int target) {
        int at = colours.getOrDefault(key, target);
        int next = at;
        if (at != target) {
            float k = 1f - (float) Math.exp(-dt / 60f);
            next = Ui.mix(at, target, Math.max(k, 0.08f));
            boolean close = true;
            for (int shift = 0; shift <= 24; shift += 8) {
                if (Math.abs(((next >>> shift) & 0xFF) - ((target >>> shift) & 0xFF)) > 3) {
                    close = false;
                }
            }
            if (close) {
                next = target;
            }
        }
        colours.put(key, next);
        return next;
    }

    protected void settle() {
        anim.clear();
        colours.clear();
    }

    protected int edge(float g) {
        return Ui.mix(Ui.BORDER, Ui.alpha(accent, 0x8C), g);
    }

    protected void window(float x, float y, float w, float h, boolean blurred) {
        winX = x;
        winY = y;
        winW = w;
        winH = h;
        for (int i = 4; i >= 1; i--) {
            float g = i * 1.6f;
            c.round(x - g, y - g + 2, w + g * 2, h + g * 2, Ui.R3 + g, 0x12000000);
        }
        Ui.pane(c, x, y, w, h, Ui.R3, blurred ? Ui.WINDOW : Ui.WINDOW_SOLID, Ui.WIN_EDGE);
    }

    protected void button(String key, float x, float y, float w, float h, String label,
                          boolean enabled, boolean primary, Icons.Icon icon, Runnable go) {
        boolean hot = enabled && over(x, y, w, h);
        float g = glow("b:" + key, hot);
        float pressed = animate("p:" + key, 0f, 70f);
        Ui.halo(c, x, y, w, h, Ui.R1, accent, g);
        if (primary && enabled) {
            c.round(x, y, w, h, Ui.R1, Ui.mix(Ui.mix(accent, 0xFFFFFFFF, g * 0.14f),
                    0xFF000000, pressed * 0.18f));
        } else {
            Ui.pane(c, x, y, w, h, Ui.R1, Ui.mix(Ui.RAISED, 0x30FFFFFF, pressed), edge(g));
        }
        int ink = !enabled ? Ui.fade(Ui.FAINT, 0.7f)
                : primary ? Ui.ON_ACCENT : Ui.mix(Ui.TEXT_2, Ui.TEXT, g);
        float iconW = icon == null ? 0 : 11;
        String text = Ui.fit(c, label, (int) (w - 10 - iconW));
        float tx = x + (w - c.width(text) - iconW) / 2f;
        c.text(text, tx, y + (h - 8) / 2f, ink);
        if (icon != null) {
            c.icon(icon, tx + c.width(text) + 4, y + (h - 7) / 2f, 7,
                   enabled ? Ui.TEXT_3 : Ui.FAINT);
        }
        zone(x, y, w, h, (px, py, b) -> {
            if (enabled) {
                anim.put("p:" + key, 1f);
                click();
                go.run();
            }
        }).key = "b:" + key;
    }

    protected void dangerButton(String key, float x, float y, float w, float h, String label,
                                boolean enabled, boolean armed, Runnable go) {
        boolean hot = enabled && over(x, y, w, h);
        float g = glow("b:" + key, hot);
        float pressed = animate("p:" + key, 0f, 70f);
        float fill = animate("arm:" + key, armed ? 1f : 0f, 60f);
        Ui.halo(c, x, y, w, h, Ui.R1, Ui.RED, g);
        int base = Ui.mix(Ui.alpha(Ui.RED, 0x22), Ui.RED, fill);
        Ui.pane(c, x, y, w, h, Ui.R1, Ui.mix(Ui.mix(base, 0x30FFFFFF, g * 0.4f), 0xFF000000,
                pressed * 0.18f), enabled ? Ui.alpha(Ui.RED, 0xB0) : Ui.BORDER);
        int ink = !enabled ? Ui.fade(Ui.FAINT, 0.7f) : Ui.mix(Ui.RED, Ui.ON_ACCENT, fill);
        String text = Ui.fit(c, label, (int) (w - 10));
        c.text(text, x + (w - c.width(text)) / 2f, y + (h - 8) / 2f, ink);
        zone(x, y, w, h, (px, py, b) -> {
            if (enabled) {
                anim.put("p:" + key, 1f);
                click();
                go.run();
            }
        }).key = "b:" + key;
    }

    protected void card(float x, float y, float w, float h, String title, String count,
                        boolean tool) {
        Ui.pane(c, x, y, w, h, Ui.R2, Ui.CARD, tool ? Ui.alpha(accent, 0x66) : Ui.LINE);
        String shown = Ui.fit(c, title, (int) (w - 34));
        c.text(shown, x + 9, y + 6, tool ? accent : Ui.TEXT_3);
        float lineL = x + 9 + c.width(shown) + 6;
        float lineR = x + w - 12 - (count == null ? 0 : c.width(count));
        if (lineR > lineL) {
            c.fill(lineL, y + 9.5f, lineR, y + 9.5f + c.px(), Ui.LINE);
        }
        if (count != null) {
            c.text(count, x + w - 8 - c.width(count), y + 6, Ui.FAINT);
        }
    }

    protected void segmented(String key, float left, float right, float y,
                             List<Row.Option> options, java.util.function.IntSupplier selected,
                             java.util.function.IntConsumer pick,
                             java.util.function.ToIntFunction<Row.Tone> tones) {
        int n = Math.max(1, options.size());
        float w = right - left;
        float inner = w - 2;
        float[] cellX = new float[n + 1];
        float widest = 0;
        float sum = 0;
        for (Row.Option o : options) {
            widest = Math.max(widest, c.width(o.label()));
            sum += c.width(o.label()) + 12;
        }
        boolean even = widest + 6 <= inner / n;
        cellX[0] = left + 1;
        for (int i = 0; i < n; i++) {
            float cw = even || options.isEmpty() ? inner / n
                    : inner * (c.width(options.get(i).label()) + 12) / Math.max(1f, sum);
            cellX[i + 1] = cellX[i] + cw;
        }
        int at = Math.max(0, Math.min(n - 1, selected.getAsInt()));
        boolean hotAny = over(left, y, w, CTRL_H);
        float g = glow("sg:" + key, hotAny);
        Ui.halo(c, left, y, w, CTRL_H, Ui.R1, accent, g * 0.6f);
        Ui.pane(c, left, y, w, CTRL_H, Ui.R1, Ui.FIELD,
                Ui.mix(Ui.BORDER, Ui.alpha(accent, 0x70), g));
        Row.Tone tone = options.isEmpty() ? Row.Tone.PLAIN : options.get(at).tone();
        float tx = animate("sx:" + key, cellX[at] - left, 60f);
        float tw = animate("sw:" + key, cellX[at + 1] - cellX[at], 60f);
        c.round(left + tx, y + 1.5f, tw, CTRL_H - 3, Ui.R1 - 1.5f,
                colourTo("sc:" + key, tones.applyAsInt(tone)));
        for (int i = 0; i < options.size(); i++) {
            Row.Option o = options.get(i);
            float cx = cellX[i];
            float cw = cellX[i + 1] - cellX[i];
            boolean on = i == at;
            boolean hot = !on && over(cx, y, cw, CTRL_H);
            String text = Ui.fit(c, o.label(), (int) (cw - 4));
            int ink = on ? (tone == Row.Tone.OFF ? Ui.TEXT : Ui.ON_ACCENT)
                         : (hot ? Ui.TEXT : Ui.TEXT_3);
            c.text(text, cx + (cw - c.width(text)) / 2f, y + 4, ink);
            int index = i;
            zone(cx, y, cw, CTRL_H, (px, py, b) -> {
                if (index != selected.getAsInt()) {
                    click();
                    pick.accept(index);
                }
            }).key = "c:" + key + ":" + i;
        }
    }

    protected float buttonWidth(String label) {
        return c.width(label) + 18;
    }

    protected float toggle(String key, float right, float y, boolean on, String text,
                           Runnable flip) {
        float t = animate("t:" + key, on ? 1f : 0f, 55f);
        float w = 26;
        float h = 14;
        float x = right - w;
        float ty = y + 1;
        boolean hot = over(x - 2, y - 2, w + 4, CTRL_H + 4);
        float g = glow("tg:" + key, hot);
        Ui.halo(c, x, ty, w, h, h / 2f, accent, g);
        c.round(x, ty, w, h, h / 2f, Ui.mix(Ui.FIELD, Ui.alpha(accent, 0x66), t));
        c.ring(x, ty, w, h, h / 2f, c.px(), Ui.mix(Ui.TRACK_OFF, accent, t));
        float knob = 10;
        float kx = x + 2 + (w - 4 - knob) * t;
        c.round(kx, ty + 2, knob, knob, knob / 2f, Ui.mix(Ui.KNOB_OFF, accent, t));
        zone(x - 2, y - 2, w + 4, CTRL_H + 4, (px, py, b) -> {
            click();
            flip.run();
        }).key = "t:" + key;
        if (text == null || text.isEmpty()) {
            return x;
        }
        c.text(text, x - 5 - c.width(text), y + 4, Ui.mix(Ui.HINT, accent, t));
        return x - 5 - c.width(text);
    }

    protected float chips(String key, float right, float y, String[] labels, int selected,
                          java.util.function.IntConsumer pick) {
        float total = 2;
        float[] widths = new float[labels.length];
        for (int i = 0; i < labels.length; i++) {
            widths[i] = c.width(labels[i]) + 12;
            total += widths[i];
        }
        float x = right - total;
        Ui.pane(c, x, y, total, CTRL_H, Ui.R1, Ui.FIELD, Ui.BORDER);
        float cx = x + 1;
        float tx = cx;
        for (int i = 0; i < selected && i < labels.length; i++) {
            tx += widths[i];
        }
        float at = animate("cx:" + key, tx - x, 60f);
        float aw = animate("cw:" + key, widths[Math.max(0, Math.min(labels.length - 1, selected))],
                           60f);
        c.round(x + at, y + 1.5f, aw, CTRL_H - 3, Ui.R1 - 1.5f, accent);
        for (int i = 0; i < labels.length; i++) {
            boolean on = i == selected;
            boolean hot = !on && over(cx, y, widths[i], CTRL_H);
            c.text(labels[i], cx + 6, y + 4, on ? Ui.ON_ACCENT : (hot ? Ui.TEXT : Ui.TEXT_3));
            int index = i;
            zone(cx, y, widths[i], CTRL_H, (px, py, b) -> {
                if (index != selected) {
                    click();
                    pick.accept(index);
                }
            }).key = "c:" + key + ":" + i;
            cx += widths[i];
        }
        return x;
    }

    protected void popPane(float x, float y, float w, float h, float in) {
        for (int i = 3; i >= 1; i--) {
            float g = i * 1.5f;
            c.round(x - g, y - g + 2, w + g * 2, h + g * 2, Ui.R2 + g, Ui.fade(0x18000000, in));
        }
        Ui.pane(c, x, y, w, h, Ui.R2, Ui.fade(Ui.POP, in), Ui.fade(Ui.alpha(accent, 0x8C), in));
    }

    protected void scrollbar(float x, float y, float h, float contentH, float scroll,
                             float maxScroll) {
        if (maxScroll <= 0) {
            return;
        }
        c.round(x, y, 2.5f, h, 1.25f, Ui.LINE);
        float thumbH = Math.max(14, h * h / Math.max(1f, contentH));
        float thumbY = y + (h - thumbH) * (scroll / maxScroll);
        c.round(x, thumbY, 2.5f, thumbH, 1.25f, Ui.alpha(accent, 0xC8));
    }

    protected record Tip(String text, float x, float y, float w, float h) {}

    private void tooltip(Tip t, float alpha) {
        if (t.text() == null || t.text().isEmpty()) {
            return;
        }
        float side = winX + winW - 4 - (t.x() + t.w() + 6);
        boolean beside = t.w() > 0 && side >= 120;
        float maxW = beside ? Math.min(240, side) : Math.min(240, Math.max(80, winW - 24));
        List<String> lines = Ui.wrap(c, t.text(), (int) (maxW - 14));
        float w = 14;
        for (String line : lines) {
            w = Math.max(w, c.width(line) + 14);
        }
        float h = lines.size() * 10 + 9;
        float x;
        float y;
        if (beside) {
            x = t.x() + t.w() + 6;
            y = Math.max(winY + 4, Math.min(winY + winH - h - 4, t.y() + (t.h() - h) / 2f));
        } else {
            x = Math.max(winX + 4, Math.min(winX + winW - w - 4, t.x() + 8));
            y = t.y() + t.h() + 4;
            if (y + h > winY + winH - 4) {
                y = t.y() - h - 4;
            }
        }
        y = Math.max(2, Math.min(screenH - h - 2, y)) + (1f - alpha) * 3f;
        popPane(x, y, w, h, alpha);
        for (int i = 0; i < lines.size(); i++) {
            c.text(lines.get(i), x + 7, y + 5 + i * 10, Ui.fade(Ui.TEXT, alpha));
        }
    }

    protected void field(Field f, float x, float y, float w, boolean focused) {
        String text = f.text.toString();
        String head = text.substring(0, Math.min(f.caret, text.length()));
        float shift = Math.max(0, c.width(head) - (w - 2));
        clip(x, y - 2, x + w, y + 10);
        if (f.all && text.length() > 0) {
            c.fill(x - shift, y - 1, x - shift + c.width(text), y + 9, Ui.alpha(accent, 0x70));
        }
        c.text(text, x - shift, y, Ui.TEXT);
        if (focused && (now / 500) % 2 == 0) {
            float cx = x - shift + c.width(head);
            c.fill(cx, y - 1, cx + Math.max(c.px(), 0.5f), y + 9, Ui.TEXT);
        }
        unclip();
    }

    protected final class Field {
        public final StringBuilder text = new StringBuilder();
        final int max;
        public int caret;
        public boolean all;

        public Field(int max) {
            this.max = max;
        }

        public void clear() {
            text.setLength(0);
            caret = 0;
            all = false;
        }

        public void set(String value) {
            text.setLength(0);
            text.append(value == null ? "" : value);
            caret = text.length();
            all = false;
        }

        public boolean type(String s) {
            if (s == null || s.isEmpty() || s.charAt(0) < ' ') {
                return false;
            }
            if (all) {
                clear();
            }
            if (text.codePointCount(0, text.length()) + s.codePointCount(0, s.length()) > max) {
                return false;
            }
            text.insert(caret, s);
            caret += s.length();
            return true;
        }

        public boolean key(int key, boolean ctrl) {
            if (ctrl && key == 'A') {
                all = text.length() > 0;
                caret = text.length();
                return false;
            }
            if (ctrl && key == 'C') {
                setClipboard(text.toString());
                return false;
            }
            if (ctrl && key == 'V') {
                String clip = clipboard();
                if (clip == null) {
                    return false;
                }
                boolean changed = false;
                for (int i = 0; i < clip.length(); ) {
                    int cp = clip.codePointAt(i);
                    i += Character.charCount(cp);
                    if (cp >= ' ') {
                        changed |= type(new String(Character.toChars(cp)));
                    }
                }
                return changed;
            }
            switch (key) {
                case KEY_BACKSPACE -> {
                    if (all) {
                        clear();
                        return true;
                    }
                    if (caret > 0) {
                        int from = text.offsetByCodePoints(caret, -1);
                        text.delete(from, caret);
                        caret = from;
                        return true;
                    }
                }
                case KEY_DELETE -> {
                    if (all) {
                        clear();
                        return true;
                    }
                    if (caret < text.length()) {
                        text.delete(caret, text.offsetByCodePoints(caret, 1));
                        return true;
                    }
                }
                case KEY_LEFT -> {
                    all = false;
                    if (caret > 0) {
                        caret = text.offsetByCodePoints(caret, -1);
                    }
                }
                case KEY_RIGHT -> {
                    all = false;
                    if (caret < text.length()) {
                        caret = text.offsetByCodePoints(caret, 1);
                    }
                }
                case KEY_HOME -> {
                    all = false;
                    caret = 0;
                }
                case KEY_END -> {
                    all = false;
                    caret = text.length();
                }
                default -> { }
            }
            return false;
        }
    }
}
