package com.wynnchayuan.client;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.ModConfig.Overlay;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.PositionView;
import com.wynnchayuan.client.ui.PositionView.Panel;
import com.wynnchayuan.client.ui.PositionView.Rect;
import com.wynnchayuan.client.ui.PositionView.Size;
import com.wynnchayuan.client.ui.PositionView.Spot;
import com.wynnchayuan.client.ui.Surface;
import com.wynnchayuan.render.PanelBox;
import com.wynnchayuan.render.PanelPlace;
import com.wynnchayuan.render.PanelPreview;
import com.wynnchayuan.translate.ErrorDebug;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.Map;

public final class PositionScreen extends CanvasScreen {

    private static final Size TALL = new Size(100, 640, 24, 320, true);
    private static final Size WIDE = new Size(100, 420, 0, 0, false);
    private static final Spot TOOLTIP_HOME = new Spot(20, 20, null, null);

    private final PositionView view;
    private final Map<Overlay, PanelPreview.Sample> samples = PanelSamples.all();
    private final PanelPreview.Sample original = PanelSamples.original();
    private Rect preview;

    public PositionScreen(Screen parent) {
        super(T.c("pos.title"), parent);
        this.view = new PositionView(SHELL, new Places());
    }

    @Override
    protected Surface view() {
        return view;
    }

    @Override
    public void added() {
        super.added();
        PanelPreview.editing(true);
    }

    @Override
    public void removed() {
        PanelPreview.editing(false);
        super.removed();
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float delta) {
        if (this.minecraft == null || this.minecraft.level == null) {
            super.renderBackground(g, mouseX, mouseY, delta);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        preview = drawPreview(g);
        g.nextStratum();
        super.render(g, mouseX, mouseY, delta);
    }

    private Rect drawPreview(GuiGraphics g) {
        try {
            Overlay which = Overlay.valueOf(view.showing());
            PanelBox box;
            if (which == Overlay.TOOLTIP && view.follows()) {
                int[] mouse = view.followPoint();
                int mx = mouse == null ? this.width * 3 / 10 : mouse[0];
                int my = mouse == null ? this.height * 2 / 5 : mouse[1];
                box = PanelPreview.drawFollowing(g, original, samples.get(which), mx, my,
                        WynnChaYuan.config().panelGap(), ModConfig.PanelSide.values()[view.side()]);
            } else {
                Spot at = view.spot();
                box = PanelPreview.draw(g, which, samples.get(which),
                        new PanelPlace(at.x(), at.y(), at.w(), at.h()));
            }
            return new Rect(box.x(), box.y(), box.w(), box.h());
        } catch (RuntimeException e) {
            ErrorDebug.note("PositionScreen#drawPreview", view.showing(), e);
            return null;
        }
    }

    private final class Places implements PositionView.Host {

        @Override
        public List<Panel> panels() {
            return List.of(
                    new Panel(Overlay.TOOLTIP.name(), T.s("pos.box.tooltip"), false, 0, 4, null),
                    new Panel(Overlay.TRACKER.name(), T.s("pos.box.tracker"), false, 2, 0, TALL),
                    new Panel(Overlay.DIALOGUE.name(), T.s("pos.box.dialogue"), true, 2, 0, TALL),
                    new Panel(Overlay.CHOICES.name(), T.s("pos.box.choices"), true, 2, 0, WIDE),
                    new Panel(Overlay.NAMETAG.name(), T.s("pos.box.nametag"), true, 2, 0, null));
        }

        @Override
        public Spot saved(String id) {
            PanelPlace at = PanelPlace.saved(WynnChaYuan.config(), Overlay.valueOf(id));
            return new Spot(at.x(), at.y(), at.w(), at.h());
        }

        @Override
        public Spot fallback(String id) {
            return Overlay.TOOLTIP.name().equals(id) ? TOOLTIP_HOME : Spot.AUTO;
        }

        @Override
        public boolean follows() {
            return WynnChaYuan.config().panelAnchor() == ModConfig.PanelAnchor.FOLLOW;
        }

        @Override
        public Rect drawn(String id, Spot spot) {
            return preview;
        }

        @Override
        public void store(String id, Spot spot) {
            ModConfig cfg = WynnChaYuan.config();
            Overlay which = Overlay.valueOf(id);
            if (spot.placed()) {
                cfg.setOverlayPos(which, spot.x(), spot.y());
            } else {
                cfg.clearOverlayPos(which);
            }
            if (spot.sized()) {
                cfg.setOverlaySize(which, spot.w(), spot.h());
            } else {
                cfg.clearOverlaySize(which);
            }
        }

        @Override
        public void follow(boolean follow) {
            WynnChaYuan.config().setPanelAnchor(follow
                    ? ModConfig.PanelAnchor.FOLLOW : ModConfig.PanelAnchor.FIXED);
        }

        @Override
        public int side() {
            return WynnChaYuan.config().panelSide().ordinal();
        }

        @Override
        public void side(int side) {
            ModConfig.PanelSide[] all = ModConfig.PanelSide.values();
            ModConfig cfg = WynnChaYuan.config();
            ModConfig.PanelSide want = all[Math.floorMod(side, all.length)];
            if (cfg.panelSide() != want) {
                cfg.cyclePanelSide(want.ordinal() - cfg.panelSide().ordinal());
            }
        }

        @Override
        public void commit() {
            WynnChaYuan.config().saveIfDirty();
        }

        @Override
        public void close() {
            onClose();
        }
    }
}
