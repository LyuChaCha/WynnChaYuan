package com.wynnchayuan.client.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * 調整面板位置：五個小框同時擺在畫面上，拖到想要的地方。
 *
 * <h2>為什麼全部一起顯示</h2>
 * 一次只調一個的話，調完才發現對話框蓋到任務追蹤。全部擺出來，會不會互相擋到
 * 一眼就看得到。
 *
 * <h2>操作</h2>
 * 拖框搬位置，拉右下角改大小（只有對話、選項、任務追蹤能改）。靠近畫面中線會吸過去，
 * 吸住的時候那條中線會亮起來。底列可以只重置選到的那一個，或全部重置。
 * 按「儲存」才寫進設定；直接關掉等於取消。
 */
public final class PositionView extends Surface {

    /** 框裡的一行示意文字。 */
    public record Line(String text, int colour) {}

    /**
     * 一個可以擺的框。
     *
     * @param centred 設定檔裡記的是不是「中心的 x」（對話、選項、名牌是；它們會依內容變寬）
     */
    public record Spec(String id, String label, int defW, int defH, boolean resizable,
                       int minW, int maxW, boolean centred, List<Line> lines) {}

    /** 設定檔裡現在記的值。沒記過位置或大小就是 {@code null}。 */
    public record Saved(Integer x, Integer y, Integer w, Integer h) {}

    public interface Host {
        List<Spec> boxes();

        Saved saved(String id);

        /** 沒存過位置的時候放哪裡：{@code {x, y}}。 */
        int[] defaultPosition(String id, int w, int h, int screenW, int screenH);

        /** @param w 沒改過大小時是 {@code null}，設定檔裡那一筆要清掉 */
        void store(String id, int x, int y, Integer w, Integer h);

        /** 全部寫進磁碟。 */
        void commit();

        void close();
    }

    private static final class Box {
        final Spec spec;
        int x;
        int y;
        int w;
        int h;
        boolean resized;

        Box(Spec spec) {
            this.spec = spec;
            this.w = spec.defW();
            this.h = spec.defH();
        }
    }

    private static final int MIN_H = 24;
    private static final int MAX_H = 320;
    private static final int SNAP = 5;
    /** 頂列與每個框上面那條名稱要留的高度。 */
    private static final int TOP = 56;
    /** 底列要留的高度。 */
    private static final int FOOT = 40;

    private final Shell shell;
    private final Host host;
    private final List<Box> boxes = new ArrayList<>();
    private Box selected;
    private Box active;
    private boolean resizing;
    private boolean snapX;
    private boolean snapY;
    private double grabX;
    private double grabY;
    private long savedAt;
    private int laidOutW = -1;
    private int laidOutH = -1;

    public PositionView(Shell shell, Host host) {
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

    /** 第一次畫、或視窗大小變了：從設定檔把每個框擺好。 */
    private void layout() {
        if (laidOutW == screenW && laidOutH == screenH) {
            return;
        }
        boolean first = boxes.isEmpty();
        if (first) {
            for (Spec spec : host.boxes()) {
                boxes.add(new Box(spec));
            }
            for (Box box : boxes) {
                Saved saved = host.saved(box.spec.id());
                if (box.spec.resizable() && saved.w() != null && saved.h() != null) {
                    box.w = clamp(saved.w(), box.spec.minW(), Math.min(box.spec.maxW(), screenW));
                    box.h = clamp(saved.h(), MIN_H, MAX_H);
                    box.resized = true;
                }
                if (saved.x() != null && saved.y() != null) {
                    box.x = box.spec.centred() ? saved.x() - box.w / 2 : saved.x();
                    box.y = saved.y();
                } else {
                    reset(box);
                }
            }
            selected = boxes.get(boxes.size() - 1);
        }
        for (Box box : boxes) {
            intoScreen(box);
        }
        laidOutW = screenW;
        laidOutH = screenH;
    }

    private void reset(Box box) {
        box.w = box.spec.defW();
        box.h = box.spec.defH();
        box.resized = false;
        int[] at = host.defaultPosition(box.spec.id(), box.w, box.h, screenW, screenH);
        box.x = at[0];
        box.y = at[1];
        intoScreen(box);
    }

    private boolean atDefault(Box box) {
        int[] at = host.defaultPosition(box.spec.id(), box.spec.defW(), box.spec.defH(),
                                        screenW, screenH);
        Box probe = new Box(box.spec);
        probe.x = at[0];
        probe.y = at[1];
        intoScreen(probe);
        return !box.resized && box.x == probe.x && box.y == probe.y;
    }

    private void intoScreen(Box box) {
        int floor = Math.max(TOP, screenH - FOOT - box.h);
        box.x = Math.max(0, Math.min(box.x, Math.max(0, screenW - box.w)));
        box.y = Math.max(TOP, Math.min(box.y, floor));
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(v, Math.max(lo, hi)));
    }

    private void save() {
        for (Box box : boxes) {
            host.store(box.spec.id(), box.spec.centred() ? box.x + box.w / 2 : box.x, box.y,
                       box.resized ? box.w : null, box.resized ? box.h : null);
        }
        host.commit();
        savedAt = now;
    }

    @Override
    protected void draw() {
        layout();
        winX = 0;
        winY = 0;
        winW = screenW;
        winH = screenH;
        int frame = 0xFF000000 | (shell.frame() & 0xFFFFFF);

        // 拖的時候畫出兩條中線；吸住的那一條亮起來
        if (active != null && !resizing) {
            c.fill(screenW / 2f, TOP - 14, screenW / 2f + c.px(), screenH - FOOT,
                   snapX ? accent : 0x30FFFFFF);
            c.fill(0, screenH / 2f, screenW, screenH / 2f + c.px(),
                   snapY ? accent : 0x30FFFFFF);
        }

        Box under = null;
        boolean underGrip = false;
        for (Box box : boxes) {
            if (over(box.x, box.y - 13, box.w, box.h + 13)) {
                under = box;
                underGrip = box.spec.resizable()
                        && over(box.x + box.w - 10, box.y + box.h - 10, 10, 10);
            }
        }
        for (Box box : boxes) {
            box(box, frame, box == under || box == active);
        }

        topBar();
        toolbar(under, underGrip);
    }

    private void box(Box box, int frame, boolean lit) {
        boolean on = box == selected;
        float g = glow("box:" + box.spec.id(), lit);
        // 名稱與大小：一條貼在框上面的小標籤
        String size = box.w + " × " + box.h;
        String label = box.spec.label();
        float lw = Math.min(box.w, c.width(label) + c.width(size) + 18);
        c.round(box.x, box.y - 13, lw, 12, 3, on ? accent : 0xE00B1119);
        c.text(Ui.fit(c, label, (int) (lw - 8)), box.x + 4, box.y - 11,
               on ? Ui.ON_ACCENT : Ui.TEXT_2);
        if (c.width(label) + c.width(size) + 18 <= box.w) {
            c.text(size, box.x + lw - 4 - c.width(size), box.y - 11,
                   on ? Ui.ON_ACCENT : Ui.FAINT);
        }

        Ui.halo(c, box.x, box.y, box.w, box.h, 2, accent, Math.max(g, box == active ? 1f : 0f));
        c.fill(box.x, box.y, box.x + box.w, box.y + box.h, 0xE60B1119);
        c.ring(box.x, box.y, box.w, box.h, 0, 1f, on ? accent : frame);
        clip(box.x + 2, box.y + 2, box.x + box.w - 2, box.y + box.h - 2);
        float ty = box.y + 6;
        for (Line line : box.spec.lines()) {
            c.text(line.text(), box.x + 6, ty, line.colour());
            ty += 10;
        }
        unclip();

        Zone body = zone(box.x, box.y - 13, box.w, box.h + 13, (px, py, b) -> {
            active = box;
            selected = box;
            resizing = false;
            grabX = px - box.x;
            grabY = py - box.y;
            // 抓到的那一個移到最上層
            boxes.remove(box);
            boxes.add(box);
        });
        body.drag = (px, py, b) -> {
            int nx = (int) Math.round(px - grabX);
            int ny = (int) Math.round(py - grabY);
            int cx = (screenW - box.w) / 2;
            int cy = (screenH - box.h) / 2;
            snapX = Math.abs(nx - cx) <= SNAP;
            snapY = Math.abs(ny - cy) <= SNAP;
            box.x = snapX ? cx : nx;
            box.y = snapY ? cy : ny;
            intoScreen(box);
        };
        body.release = this::drop;
        body.key = "box:" + box.spec.id();

        // 拉大小的那一角要後登記：後登記的在上面，不然整個框那一塊會先接到點擊
        if (box.spec.resizable()) {
            // 右下角三條斜線：這個框可以拉大小
            int ink = lit || on ? accent : Ui.alpha(frame, 0x90);
            float rx = box.x + box.w - 3;
            float by = box.y + box.h - 3;
            for (int i = 0; i < 3; i++) {
                float off = i * 2.5f;
                c.fill(rx - off - 1, by - 1, rx - off, by, ink);
                c.fill(rx - 1, by - off - 1, rx, by - off, ink);
            }
            Zone grip = zone(box.x + box.w - 10, box.y + box.h - 10, 10, 10, (px, py, b) -> {
                active = box;
                selected = box;
                resizing = true;
                grabX = px - box.w;
                grabY = py - box.h;
            });
            grip.drag = (px, py, b) -> {
                box.w = clamp((int) Math.round(px - grabX), box.spec.minW(),
                              Math.min(box.spec.maxW(), screenW - box.x));
                box.h = clamp((int) Math.round(py - grabY), MIN_H,
                              Math.min(MAX_H, screenH - FOOT - box.y));
                box.resized = true;
            };
            grip.release = this::drop;
            grip.key = "grip:" + box.spec.id();
        }
    }

    private void drop() {
        active = null;
        resizing = false;
        snapX = false;
        snapY = false;
    }

    private void topBar() {
        float x = 8;
        float y = 6;
        float w = screenW - 16;
        float h = 30;
        for (int i = 3; i >= 1; i--) {
            float g = i * 1.4f;
            c.round(x - g, y - g + 1.5f, w + g * 2, h + g * 2, Ui.R2 + g, 0x12000000);
        }
        Ui.pane(c, x, y, w, h, Ui.R2, shell.blurred() ? Ui.WINDOW : Ui.WINDOW_SOLID, Ui.WIN_EDGE);
        zone(x, y, w, h, (px, py, b) -> { });
        c.logo(x + 6, y + 5, 20);
        float right = x + w - 8;

        // 五個框的名字：框疊在一起、點不到底下那個的時候，從這裡選
        float chipsLeft = right;
        if (screenW >= 520) {
            for (int i = boxes.size() - 1; i >= 0; i--) {
                Box box = sorted(i);
                String label = box.spec.label();
                float cw = c.width(label) + 20;
                float cx = chipsLeft - cw;
                boolean on = box == selected;
                boolean hot = over(cx, y + 7, cw, CTRL_H);
                float g = glow("chip:" + box.spec.id(), hot);
                Ui.pane(c, cx, y + 7, cw, CTRL_H, Ui.R1, on ? Ui.alpha(accent, 0x30) : Ui.RAISED,
                        on ? accent : edge(g));
                c.round(cx + 6, y + 13, 4, 4, 1, atDefault(box) ? Ui.FAINT : Ui.GOLD);
                c.text(label, cx + 14, y + 11, on ? accent : Ui.mix(Ui.TEXT_2, Ui.TEXT, g));
                zone(cx, y + 7, cw, CTRL_H, (px, py, b) -> {
                    click();
                    selected = box;
                });
                chipsLeft = cx - 4;
            }
        }
        float room = chipsLeft - 6 - (x + 32);
        c.text(Ui.fit(c, shell.tr("pos.title"), (int) room), x + 32, y + 5, accent);
        c.text(Ui.fit(c, shell.tr("pos.hint"), (int) room), x + 32, y + 16, Ui.TEXT_3);
    }

    /** 頂列的名字照固定順序排（設定檔的順序），不跟著「誰在最上層」跳來跳去。 */
    private Box sorted(int index) {
        Spec spec = host.boxes().get(index);
        for (Box box : boxes) {
            if (box.spec.id().equals(spec.id())) {
                return box;
            }
        }
        return boxes.get(index);
    }

    private void toolbar(Box under, boolean underGrip) {
        // 寬度照字算：俄文與西班牙文的按鈕長，固定寬度會把左邊那句提示擠到只剩幾個字
        float need = 9 + 9 + c.width(shell.tr("pos.idle")) + 12
                + buttonWidth(shell.tr("pos.save")) + 12 + 4
                + buttonWidth(shell.tr("pos.cancel")) + 10
                + buttonWidth(shell.tr("pos.reset")) + 4 + 6;
        if (selected != null) {
            need += buttonWidth(shell.tr("pos.reset.one", selected.spec.label())) + 6;
        }
        float w = Math.min(screenW - 16, Math.max(480, need));
        float h = 26;
        float x = Math.round((screenW - w) / 2f);
        float y = screenH - h - 7;
        for (int i = 3; i >= 1; i--) {
            float g = i * 1.4f;
            c.round(x - g, y - g + 1.5f, w + g * 2, h + g * 2, Ui.R2 + g, 0x12000000);
        }
        Ui.pane(c, x, y, w, h, Ui.R2, shell.blurred() ? Ui.WINDOW : Ui.WINDOW_SOLID, Ui.WIN_EDGE);
        zone(x, y, w, h, (px, py, b) -> { });
        float by = y + 4;
        float right = x + w - 6;

        String save = shell.tr("pos.save");
        float sw = buttonWidth(save) + 12;
        button("save", right - sw, by, sw, 18, save, true, true, null, this::save);
        right -= sw + 4;
        String cancel = shell.tr("pos.cancel");
        float cw = buttonWidth(cancel);
        button("cancel", right - cw, by, cw, 18, cancel, true, false, null, host::close);
        right -= cw + 5;
        c.fill(right, by + 2, right + c.px(), by + 16, Ui.BORDER);
        right -= 5;

        boolean anyMoved = false;
        for (Box box : boxes) {
            anyMoved |= !atDefault(box);
        }
        String all = shell.tr("pos.reset");
        float aw = buttonWidth(all);
        button("all", right - aw, by, aw, 18, all, anyMoved, false, null, () -> {
            for (Box box : boxes) {
                reset(box);
            }
        });
        right -= aw + 4;
        if (selected != null && right - (x + 8) > 190) {
            String one = shell.tr("pos.reset.one", selected.spec.label());
            float ow = buttonWidth(one);
            Box target = selected;
            button("one", right - ow, by, ow, 18, one, !atDefault(target), false, null,
                   () -> reset(target));
            right -= ow + 6;
        }

        String text;
        int ink = Ui.TEXT_3;
        if (savedAt != 0 && now - savedAt < 2000) {
            text = shell.tr("pos.saved");
            ink = Ui.GREEN;
        } else if (active != null && resizing) {
            text = shell.tr("pos.resizing", active.spec.label(), active.w, active.h);
            ink = accent;
        } else if (active != null) {
            text = shell.tr(snapX || snapY ? "pos.snapping" : "pos.dragging", active.spec.label());
            ink = snapX || snapY ? accent : Ui.TEXT;
        } else if (under == null) {
            text = shell.tr("pos.idle");
        } else {
            text = shell.tr(underGrip ? "pos.grip" : "pos.dragging", under.spec.label());
            ink = Ui.TEXT_2;
        }
        float left = x + 9;
        if (right - left > 30) {
            c.round(left, by + 6.5f, 5, 5, 2.5f, ink);
            c.text(Ui.fit(c, text, (int) (right - left - 12)), left + 9, by + 5, ink);
        }
    }
}
