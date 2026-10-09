package com.wynnchayuan.client;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.CollectorConfig.Overlay;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.PositionView;
import com.wynnchayuan.client.ui.Surface;
import com.wynnchayuan.client.ui.Ui;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * 調整面板位置：翻譯面板、任務追蹤、NPC 對話、對話選項、NPC 名牌五個小框各擺在哪。
 *
 * <p>畫面與拖曳的邏輯在 {@link PositionView}；這裡只負責跟設定檔之間的那一段：
 * 每個框多大、能不能改大小、沒擺過的時候放哪裡、存的時候寫到哪一欄。
 */
public final class PositionScreen extends CanvasScreen {

    private final PositionView view;

    public PositionScreen(Screen parent) {
        super(T.c("pos.title"), parent);
        this.view = new PositionView(SHELL, new PositionView.Host() {
            @Override
            public List<PositionView.Spec> boxes() {
                return specs();
            }

            @Override
            public PositionView.Saved saved(String id) {
                CollectorConfig cfg = WynnChaYuan.config();
                Overlay which = Overlay.valueOf(id);
                boolean pos = cfg.hasOverlayPos(which);
                boolean size = cfg.hasOverlaySize(which);
                return new PositionView.Saved(
                        pos ? cfg.overlayX(which) : null, pos ? cfg.overlayY(which) : null,
                        size ? cfg.overlayW(which) : null, size ? cfg.overlayH(which) : null);
            }

            @Override
            public int[] defaultPosition(String id, int w, int h, int screenW, int screenH) {
                return switch (Overlay.valueOf(id)) {
                    case TOOLTIP -> new int[] {20, 20};
                    case TRACKER -> new int[] {8, 40};
                    case DIALOGUE -> new int[] {(screenW - w) / 2, screenH - 60 - h};
                    case NAMETAG -> new int[] {(screenW - w) / 2, screenH / 2 + 16};
                    case CHOICES -> new int[] {screenW - w - 8, screenH * 5 / 8};
                };
            }

            @Override
            public void store(String id, int x, int y, Integer w, Integer h) {
                CollectorConfig cfg = WynnChaYuan.config();
                Overlay which = Overlay.valueOf(id);
                cfg.setOverlayPos(which, x, y);
                if (w != null && h != null) {
                    cfg.setOverlaySize(which, w, h);
                } else {
                    cfg.clearOverlaySize(which);
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
        });
    }

    @Override
    protected Surface view() {
        return view;
    }

    private static final int PURPLE = 0xFFB48EDE;
    private static final int YELLOW = 0xFFF5C56B;
    private static final int GREEN = 0xFF8FD694;

    private static PositionView.Line line(String key, int colour) {
        return new PositionView.Line(T.s(key), colour);
    }

    /**
     * 五個框。尺寸是遊戲裡那個小框大概的大小；能改大小的只有內容長短不一的三個——
     * 翻譯面板與名牌是照內容自己長的，給它固定的框沒有意義。
     */
    private static List<PositionView.Spec> specs() {
        List<PositionView.Spec> out = new ArrayList<>();
        out.add(new PositionView.Spec(Overlay.TOOLTIP.name(), T.s("pos.box.tooltip"), 150, 90,
                false, 90, 640, false, List.of(line("pos.demo.bow", PURPLE),
                        line("pos.demo.dps", Ui.TEXT), line("pos.demo.level", Ui.TEXT),
                        line("pos.demo.steal", Ui.TEXT))));
        out.add(new PositionView.Spec(Overlay.TRACKER.name(), T.s("pos.box.tracker"), 112, 62,
                true, 100, 640, false, List.of(line("pos.demo.tracker", YELLOW),
                        line("pos.demo.goto", Ui.TEXT), line("pos.demo.slay", Ui.TEXT))));
        out.add(new PositionView.Spec(Overlay.DIALOGUE.name(), T.s("pos.box.dialogue"), 200, 42,
                true, 100, 640, true, List.of(line("pos.demo.cook", GREEN),
                        line("pos.demo.line", Ui.TEXT))));
        out.add(new PositionView.Spec(Overlay.CHOICES.name(), T.s("pos.box.choices"), 150, 34,
                true, 100, 420, true, List.of(line("pos.demo.choice1", Ui.TEXT),
                        line("pos.demo.choice2", Ui.TEXT), line("pos.demo.choice3", Ui.TEXT))));
        out.add(new PositionView.Spec(Overlay.NAMETAG.name(), T.s("pos.box.nametag"), 96, 22,
                false, 100, 640, true, List.of(line("pos.demo.cook", GREEN))));
        return out;
    }
}
