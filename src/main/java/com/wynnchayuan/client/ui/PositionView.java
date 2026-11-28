package com.wynnchayuan.client.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PositionView extends Surface {

    public record Size(int minW, int maxW, int minH, int maxH, boolean tall) {}

    public record Panel(String id, String label, boolean centred, int edge, int inset, Size size) {

        boolean resizable() {
            return size != null;
        }
    }

    public record Spot(Integer x, Integer y, Integer w, Integer h) {

        public static final Spot AUTO = new Spot(null, null, null, null);

        public boolean placed() {
            return x != null && y != null;
        }

        public boolean sized() {
            return w != null && h != null;
        }
    }

    public record Rect(int x, int y, int w, int h) {}

    public interface Host {
        List<Panel> panels();

        Spot saved(String id);

        Spot fallback(String id);

        boolean follows();

        Rect drawn(String id, Spot spot);

        void store(String id, Spot spot);

        void follow(boolean follow);

        int side();

        void side(int side);

        void commit();

        void close();
    }

    public static final String TOOLTIP = "TOOLTIP";

    private enum Grab { NONE, MOVE, RESIZE }

    private static final long NOTE_MS = 3500;
    private static final float GAP = 8;
    private static final float PAD = 5;
    private static final float MARGIN = 6;

    private final Shell shell;
    private final Host host;
    private final List<Panel> panels;
    private final Map<String, Spot> working = new HashMap<>();
    private final Map<String, Spot> opened = new HashMap<>();
    private final boolean openedFollow;
    private final int openedSide;
    private boolean follow;
    private int side;
    private int[] followAt;
    private float[] lastDock;
    private int showing;

    private Grab grab = Grab.NONE;
    private double pressX;
    private double pressY;
    private Rect pressRect;
    private boolean snapX;
    private boolean snapY;
    private long pinnedAt;
    private Rect lastRect;

    public PositionView(Shell shell, Host host) {
        this.shell = shell;
        this.host = host;
        this.panels = List.copyOf(host.panels());
        for (Panel panel : panels) {
            Spot saved = Objects.requireNonNullElse(host.saved(panel.id()), Spot.AUTO);
            working.put(panel.id(), saved);
            opened.put(panel.id(), saved);
        }
        this.openedFollow = host.follows();
        this.follow = openedFollow;
        this.openedSide = host.side();
        this.side = openedSide;
    }

    @Override
    protected int accentColour() {
        return shell.accent();
    }

    @Override
    protected void click() {
        shell.click();
    }

    public String showing() {
        return panels.isEmpty() ? null : panels.get(showing).id();
    }

    public Spot spot() {
        String id = showing();
        return id == null ? Spot.AUTO : working.get(id);
    }

    public boolean follows() {
        return follow;
    }

    public int side() {
        return side;
    }

    public int[] followPoint() {
        return followAt;
    }

    public void show(String id) {
        for (int i = 0; i < panels.size(); i++) {
            if (panels.get(i).id().equals(id)) {
                showing = i;
                grab = Grab.NONE;
                return;
            }
        }
    }

    public Rect drawn() {
        return lastRect;
    }

    private Panel panel() {
        return panels.get(showing);
    }

    private boolean changed(Panel panel) {
        return !Objects.equals(working.get(panel.id()), opened.get(panel.id()));
    }

    private boolean atFallback(Panel panel) {
        return Objects.equals(working.get(panel.id()), host.fallback(panel.id()));
    }

    private Spot fromRect(Panel panel, Rect r, Integer w, Integer h) {
        int x = panel.centred() ? r.x() + r.w() / 2 : r.x() + panel.inset();
        return new Spot(x, r.y() + panel.inset(), w, h);
    }

    private void placeAt(Panel panel, Rect at) {
        Spot now = working.get(panel.id());
        working.put(panel.id(), fromRect(panel, at, now.w(), now.h()));
        pinIfTooltip(panel);
    }

    private void pinIfTooltip(Panel panel) {
        if (TOOLTIP.equals(panel.id()) && follow) {
            follow = false;
            pinnedAt = now;
        }
    }

    public void save() {
        for (Panel panel : panels) {
            if (changed(panel)) {
                host.store(panel.id(), working.get(panel.id()));
            }
        }
        if (follow != openedFollow) {
            host.follow(follow);
        }
        if (side != openedSide) {
            host.side(side);
        }
        host.commit();
        host.close();
    }

    public void cancel() {
        working.putAll(opened);
        follow = openedFollow;
        side = openedSide;
        host.close();
    }

    private void resetThis() {
        Panel panel = panel();
        working.put(panel.id(), Objects.requireNonNullElse(host.fallback(panel.id()), Spot.AUTO));
    }

    @Override
    public boolean key(int key, boolean ctrl) {
        if (key == KEY_ENTER || key == KEY_KP_ENTER) {
            click();
            save();
            return true;
        }
        int dx = key == KEY_LEFT ? -1 : key == KEY_RIGHT ? 1 : 0;
        int dy = key == KEY_UP ? -1 : key == KEY_DOWN ? 1 : 0;
        if ((dx == 0 && dy == 0) || lastRect == null || panels.isEmpty()) {
            return false;
        }
        int step = ctrl ? 10 : 1;
        Rect r = lastRect;
        PanelDrag.Moved moved = PanelDrag.move(r.x() + dx * step, r.y() + dy * step, r.w(), r.h(),
                screenW, screenH, panel().edge());
        placeAt(panel(), new Rect(moved.x(), moved.y(), r.w(), r.h()));
        return true;
    }

    @Override
    protected void draw() {
        winX = 0;
        winY = 0;
        winW = screenW;
        winH = screenH;
        if (panels.isEmpty()) {
            return;
        }
        trackFollow();
        Panel panel = panel();
        Rect r = host.drawn(panel.id(), working.get(panel.id()));
        lastRect = r;
        boolean underGrip = false;
        if (r != null && r.w() > 0 && r.h() > 0) {
            underGrip = adorn(panel, r);
        }
        dock(panel, r, underGrip);
    }

    private void trackFollow() {
        boolean onScreen = mx >= 0 && my >= 0 && mx < screenW && my < screenH;
        boolean onDock = lastDock != null
                && Ui.in(mx, my, lastDock[0], lastDock[1], lastDock[2], lastDock[3]);
        if (onScreen && !onDock && grab == Grab.NONE) {
            followAt = new int[] {Math.round(mx), Math.round(my)};
        }
    }

    private boolean adorn(Panel panel, Rect r) {
        if (grab == Grab.MOVE) {
            c.fill(screenW / 2f, 0, screenW / 2f + c.px(), screenH,
                   snapX ? accent : 0x38FFFFFF);
            c.fill(0, screenH / 2f, screenW, screenH / 2f + c.px(),
                   snapY ? accent : 0x38FFFFFF);
        }
        boolean hot = over(r.x(), r.y(), r.w(), r.h());
        float g = glow("panel", hot || grab != Grab.NONE);
        c.ring(r.x() - 2, r.y() - 2, r.w() + 4, r.h() + 4, 2, c.px(),
               Ui.alpha(accent, (int) (0x70 + (0xFF - 0x70) * g)));

        Zone body = zone(r.x(), r.y(), r.w(), r.h(), (px, py, b) -> start(Grab.MOVE, px, py, r));
        body.drag = (px, py, b) -> drag(panel, px, py);
        body.release = this::drop;
        body.key = "box:" + panel.id();

        if (!panel.resizable()) {
            return false;
        }
        float gx = r.x() + r.w();
        float gy = r.y() + r.h();
        boolean overGrip = over(gx - 7, gy - 7, 12, 12);
        float gg = glow("grip", overGrip || grab == Grab.RESIZE);
        c.round(gx - 4, gy - 4, 7, 7, 2, Ui.mix(Ui.alpha(accent, 0xC0), accent, gg));
        c.round(gx - 2, gy - 2, 3, 3, 1, Ui.ON_ACCENT);
        Zone grip = zone(gx - 7, gy - 7, 12, 12, (px, py, b) -> start(Grab.RESIZE, px, py, r));
        grip.drag = (px, py, b) -> drag(panel, px, py);
        grip.release = this::drop;
        grip.key = "grip:" + panel.id();

        if (grab == Grab.RESIZE) {
            String size = r.w() + " × " + r.h();
            float sw = c.width(size) + 8;
            float sx = Math.min(screenW - sw - 2, gx + 6);
            float sy = Math.min(screenH - 14, gy + 6);
            c.round(sx, sy, sw, 12, 3, Ui.WINDOW_SOLID);
            c.text(size, sx + 4, sy + 2, accent);
        }
        return overGrip;
    }

    private void start(Grab kind, double px, double py, Rect r) {
        grab = kind;
        pressX = px;
        pressY = py;
        pressRect = r;
        snapX = false;
        snapY = false;
    }

    private void drag(Panel panel, double px, double py) {
        if (pressRect == null) {
            return;
        }
        int dx = (int) Math.round(px - pressX);
        int dy = (int) Math.round(py - pressY);
        Rect r = pressRect;
        if (grab == Grab.RESIZE && panel.resizable()) {
            int tallH = panel.size().tall() ? r.h() + dy : r.h();
            int[] size = PanelDrag.resize(r.w() + dx, tallH, r.x(), r.y(), panel.size(),
                    screenW, screenH, panel.edge());
            Rect at = new Rect(r.x(), r.y(), size[0], size[1]);
            working.put(panel.id(), fromRect(panel, at, size[0], size[1]));
            return;
        }
        if (dx == 0 && dy == 0) {
            return;
        }
        PanelDrag.Moved moved = PanelDrag.move(r.x() + dx, r.y() + dy, r.w(), r.h(),
                screenW, screenH, panel.edge());
        snapX = moved.snapX();
        snapY = moved.snapY();
        placeAt(panel, new Rect(moved.x(), moved.y(), r.w(), r.h()));
    }

    private void drop() {
        grab = Grab.NONE;
        pressRect = null;
        snapX = false;
        snapY = false;
    }

    private List<Row.Option> tabs() {
        List<Row.Option> out = new ArrayList<>(panels.size());
        for (Panel panel : panels) {
            out.add(new Row.Option(panel.label(), Row.Tone.ACCENT, null));
        }
        return out;
    }

    private List<Row.Option> anchors() {
        return List.of(new Row.Option(shell.tr("mode.follow"), Row.Tone.PLAIN, null),
                       new Row.Option(shell.tr("mode.pinned"), Row.Tone.PLAIN, null));
    }

    private List<Row.Option> sides() {
        return List.of(new Row.Option(shell.tr("mode.auto"), Row.Tone.PLAIN, null),
                       new Row.Option(shell.tr("mode.right"), Row.Tone.PLAIN, null),
                       new Row.Option(shell.tr("mode.left"), Row.Tone.PLAIN, null));
    }

    private float segmentWidth(List<Row.Option> options) {
        float w = 2;
        for (Row.Option o : options) {
            w += c.width(o.label()) + 12;
        }
        return w;
    }

    private interface Placer {
        void at(float x, float y, float w);
    }

    private record Piece(float w, boolean right, Placer draw) {}

    private void dock(Panel panel, Rect r, boolean underGrip) {
        boolean tooltip = TOOLTIP.equals(panel.id());
        boolean sided = tooltip && follow;
        float room = screenW - MARGIN * 2;
        float inner = room - PAD * 2;

        List<Piece> pieces = new ArrayList<>();
        List<Row.Option> tabs = tabs();
        pieces.add(new Piece(Math.min(inner, segmentWidth(tabs)), false, (x, y, w) ->
                segmented("tab", x, x + w, y, tabs, () -> showing, i -> {
                    showing = i;
                    grab = Grab.NONE;
                }, tone -> accent)));
        if (tooltip) {
            List<Row.Option> anchors = anchors();
            pieces.add(new Piece(Math.min(inner, segmentWidth(anchors)), false, (x, y, w) ->
                    segmented("anchor", x, x + w, y, anchors, () -> follow ? 0 : 1,
                              i -> follow = i == 0, tone -> accent)));
        }
        if (sided) {
            List<Row.Option> sides = sides();
            pieces.add(new Piece(Math.min(inner, segmentWidth(sides)), false, (x, y, w) ->
                    segmented("side", x, x + w, y, sides, () -> side, i -> side = i,
                              tone -> accent)));
        }
        String reset = shell.tr("pos.reset.this");
        String cancel = shell.tr("pos.cancel");
        String save = shell.tr("pos.save");
        float resetW = buttonWidth(reset);
        float cancelW = buttonWidth(cancel);
        float saveW = buttonWidth(save) + 6;
        boolean resettable = !atFallback(panel);
        pieces.add(new Piece(resetW + 4 + cancelW + 4 + saveW, true, (x, y, w) -> {
            float bx = x + w - saveW;
            button("save", bx, y, saveW, CTRL_H, save, true, true, null, this::save);
            bx -= 4 + cancelW;
            button("cancel", bx, y, cancelW, CTRL_H, cancel, true, false, null, this::cancel);
            bx -= 4 + resetW;
            button("reset", bx, y, resetW, CTRL_H, reset, resettable, false, null,
                   this::resetThis);
        }));

        List<List<Piece>> rows = new ArrayList<>();
        List<Float> widths = new ArrayList<>();
        for (Piece piece : pieces) {
            int last = rows.size() - 1;
            if (last < 0 || widths.get(last) + GAP + piece.w() > inner) {
                rows.add(new ArrayList<>(List.of(piece)));
                widths.add(piece.w());
            } else {
                rows.get(last).add(piece);
                widths.set(last, widths.get(last) + GAP + piece.w());
            }
        }
        float widest = 0;
        for (float rowW : widths) {
            widest = Math.max(widest, rowW);
        }
        float w = Math.min(room, widest + PAD * 2);
        float h = PAD + rows.size() * CTRL_H + (rows.size() - 1) * 4 + 4 + 9 + PAD;
        float x = Math.round((screenW - w) / 2f);
        boolean held = lastDock != null && Ui.in(mx, my, lastDock[0], lastDock[1], lastDock[2],
                lastDock[3]);
        float y = held ? Math.min(lastDock[1], screenH - h - MARGIN)
                : dockTop(sided ? null : r, x, w, h);

        for (int i = 3; i >= 1; i--) {
            float s = i * 1.4f;
            c.round(x - s, y - s + 1.5f, w + s * 2, h + s * 2, Ui.R2 + s, 0x14000000);
        }
        Ui.pane(c, x, y, w, h, Ui.R2, Ui.WINDOW_SOLID, Ui.WIN_EDGE);
        zone(x, y, w, h, (px, py, b) -> { });
        lastDock = new float[] {x, y, w, h};

        float rowY = y + PAD;
        for (List<Piece> row : rows) {
            float left = x + PAD;
            float right = x + w - PAD;
            for (Piece piece : row) {
                if (piece.right()) {
                    piece.draw().at(right - piece.w(), rowY, piece.w());
                } else {
                    piece.draw().at(left, rowY, piece.w());
                    left += piece.w() + GAP;
                }
            }
            rowY += CTRL_H + 4;
        }

        status(panel, x + PAD, y + h - PAD - 9, w - PAD * 2, underGrip);
    }

    private float dockTop(Rect r, float x, float w, float h) {
        float top = MARGIN;
        float bottom = screenH - h - MARGIN;
        if (r == null) {
            return top;
        }
        float upper = overlap(r, x, top, w, h);
        float lower = overlap(r, x, bottom, w, h);
        return upper > 0 && lower < upper ? bottom : top;
    }

    private static float overlap(Rect r, float x, float y, float w, float h) {
        float m = 6;
        float ox = Math.min(r.x() + r.w() + m, x + w) - Math.max(r.x() - m, x);
        float oy = Math.min(r.y() + r.h() + m, y + h) - Math.max(r.y() - m, y);
        return ox > 0 && oy > 0 ? ox * oy : 0;
    }

    private void status(Panel panel, float x, float y, float w, boolean underGrip) {
        String text;
        int ink = Ui.TEXT_3;
        if (pinnedAt != 0 && now - pinnedAt < NOTE_MS) {
            text = shell.tr("pos.pinned.now");
            ink = Ui.GOLD;
        } else if (grab == Grab.RESIZE) {
            text = shell.tr("pos.resizing", panel.label(), lastRect.w(), lastRect.h());
            ink = accent;
        } else if (grab == Grab.MOVE) {
            text = shell.tr(snapX || snapY ? "pos.snapping" : "pos.dragging", panel.label());
            ink = snapX || snapY ? accent : Ui.TEXT;
        } else if (TOOLTIP.equals(panel.id()) && follow) {
            text = shell.tr("pos.follow.hint");
            ink = Ui.AMBER;
        } else if (underGrip) {
            text = shell.tr("pos.grip", panel.label());
            ink = Ui.TEXT_2;
        } else {
            text = shell.tr(panel.resizable() ? "pos.idle.size" : "pos.idle");
        }
        c.round(x + 1, y + 2, 4, 4, 2, ink);
        c.text(Ui.fit(c, text, (int) (w - 10)), x + 9, y, ink);
    }
}
