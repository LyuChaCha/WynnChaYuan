package com.wynnchayuan.client;

import com.wynnchayuan.client.ui.PositionView;
import com.wynnchayuan.client.ui.PositionView.Panel;
import com.wynnchayuan.client.ui.PositionView.Anchor;
import com.wynnchayuan.client.ui.PositionView.Rect;
import com.wynnchayuan.client.ui.PositionView.Shown;
import com.wynnchayuan.client.ui.PositionView.Size;
import com.wynnchayuan.client.ui.PositionView.Spot;
import com.wynnchayuan.client.ui.Shell;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class FakePanels implements PositionView.Host {

    static final Size TALL = new Size(100, 640, 24, 320, true);
    static final Size WIDE = new Size(100, 420, 0, 0, false);
    static final int ITEM_W = 120;
    static final int ITEM_H = 60;
    static final int GAP = 8;

    private final Shell shell;
    final Map<String, Spot> saved = new HashMap<>();
    final Map<String, Spot> stored = new LinkedHashMap<>();
    Boolean followSet;
    Integer sideSet;
    int side;
    boolean follows = true;
    int commits;
    int closes;
    int screenW = 640;
    int screenH = 360;

    FakePanels(Shell shell) {
        this.shell = shell;
        saved.put("TOOLTIP", new Spot(20, 20, null, null));
    }

    @Override
    public List<Panel> panels() {
        return List.of(
                new Panel("TOOLTIP", shell.tr("pos.box.tooltip"), false, 0, 4, null),
                new Panel("TRACKER", shell.tr("pos.box.tracker"), false, 2, 0, TALL),
                new Panel("DIALOGUE", shell.tr("pos.box.dialogue"), true, 2, 0, TALL),
                new Panel("CHOICES", shell.tr("pos.box.choices"), true, 2, 0, WIDE),
                new Panel("NAMETAG", shell.tr("pos.box.nametag"), true, 2, 0, null));
    }

    @Override
    public Spot saved(String id) {
        return saved.getOrDefault(id, Spot.AUTO);
    }

    @Override
    public Spot fallback(String id) {
        return id.equals("TOOLTIP") ? new Spot(20, 20, null, null) : Spot.AUTO;
    }

    @Override
    public boolean follows() {
        return follows;
    }

    @Override
    public Shown drawn(String id, Spot spot, Anchor anchor) {
        if (anchor != null && id.equals("TOOLTIP")) {
            return beside(anchor);
        }
        return new Shown(drawn(id, spot), null);
    }

    Rect itemFor(int mouseX, int mouseY) {
        int textW = ITEM_W - 8;
        int textH = ITEM_H - 8;
        int left = mouseX + 12;
        if (left + textW > screenW) {
            left = Math.max(left - 24 - textW, 4);
        }
        int top = mouseY - 12;
        if (top + textH + 3 > screenH) {
            top = screenH - textH - 3;
        }
        return new Rect(left - 4, top - 4, ITEM_W, ITEM_H);
    }

    private Shown beside(Anchor anchor) {
        Rect item = itemFor(anchor.x(), anchor.y());
        boolean itemOnRight = item.x() + 4 >= anchor.x();
        int right = item.x() + item.w() + GAP;
        int left = item.x() - GAP - 150;
        int x = switch (anchor.side()) {
            case 1 -> right;
            case 2 -> left;
            default -> itemOnRight ? left : right;
        };
        x = Math.max(0, Math.min(screenW - 150, x));
        int y = Math.max(0, Math.min(screenH - 70, item.y()));
        return new Shown(new Rect(x, y, 150, 70), item);
    }

    private Rect drawn(String id, Spot spot) {
        int[] natural = switch (id) {
            case "TOOLTIP" -> new int[] {150, 70};
            case "TRACKER" -> new int[] {120, 40};
            case "DIALOGUE" -> new int[] {220, 30};
            case "CHOICES" -> new int[] {140, 50};
            default -> new int[] {60, 16};
        };
        int w = natural[0];
        int h = natural[1];
        if (spot.sized()) {
            w = id.equals("TRACKER") ? Math.max(w, spot.w()) : spot.w();
            h = id.equals("CHOICES") ? h : Math.max(h, spot.h());
        }
        boolean centred = id.equals("DIALOGUE") || id.equals("CHOICES") || id.equals("NAMETAG");
        int x;
        int y;
        if (spot.placed()) {
            x = id.equals("TOOLTIP") ? spot.x() - 4 : centred ? spot.x() - w / 2 : spot.x();
            y = id.equals("TOOLTIP") ? spot.y() - 4 : spot.y();
        } else {
            x = switch (id) {
                case "TRACKER" -> 6;
                case "CHOICES" -> screenW - w - 8;
                default -> (screenW - w) / 2;
            };
            y = switch (id) {
                case "TRACKER" -> 90;
                case "DIALOGUE" -> screenH - 60 - h;
                case "CHOICES" -> screenH * 5 / 8;
                default -> 34;
            };
        }
        int edge = id.equals("TOOLTIP") ? 0 : 2;
        x = Math.max(edge, Math.min(screenW - w - edge, x));
        y = Math.max(edge, Math.min(screenH - h - edge, y));
        return new Rect(x, y, w, h);
    }

    @Override
    public void store(String id, Spot spot) {
        stored.put(id, spot);
    }

    @Override
    public void follow(boolean follow) {
        followSet = follow;
    }

    @Override
    public int side() {
        return side;
    }

    @Override
    public void side(int side) {
        sideSet = side;
    }

    @Override
    public void commit() {
        commits++;
    }

    @Override
    public void close() {
        closes++;
    }
}
