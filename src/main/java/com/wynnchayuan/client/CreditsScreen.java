package com.wynnchayuan.client;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.CreditsView;
import com.wynnchayuan.client.ui.Surface;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;

/**
 * 關於與貢獻者。
 *
 * <p>翻譯是靠很多人一條一條填出來的，名單值得放在看得到的地方。
 * 內容讀自 {@code assets/wynnchayuan/credits.json}，加人不必改程式。
 *
 * <p>畫面本身在 {@link CreditsView}。
 */
public final class CreditsScreen extends CanvasScreen {

    private final CreditsView view;

    public CreditsScreen(Screen parent) {
        super(T.c("credits.title", WynnChaYuan.MOD_NAME), parent);
        this.view = new CreditsView(SHELL, new CreditsView.Host() {
            @Override
            public List<CreditsView.Section> sections() {
                // 每一幀重新問：分類與資料來源的名字會跟著介面語言換
                List<CreditsView.Section> out = new ArrayList<>();
                for (Credits.Section section : Credits.sections()) {
                    List<CreditsView.Member> members = new ArrayList<>();
                    for (Credits.Member m : section.members()) {
                        members.add(new CreditsView.Member(m.name(), m.hasHead() ? m.mc() : null));
                    }
                    out.add(new CreditsView.Section(section.role(), section.color(), members));
                }
                return out;
            }

            @Override
            public String title() {
                return T.s("credits.title", WynnChaYuan.MOD_NAME);
            }

            @Override
            public String version() {
                return WynnChaYuan.version();
            }

            @Override
            public String badgeLabel() {
                return T.s("credits.badges",
                        T.s(WynnChaYuan.config().showBadges() ? "mode.on" : "mode.off"));
            }

            @Override
            public void toggleBadges() {
                WynnChaYuan.config().toggleBadges();
            }

            @Override
            public String styleLabel() {
                boolean gradient = WynnChaYuan.config().badgeStyle()
                        == CollectorConfig.BadgeStyle.GRADIENT;
                return T.s("credits.badges.style",
                        T.s(gradient ? "credits.badges.all" : "credits.badges.main"));
            }

            @Override
            public void cycleStyle() {
                WynnChaYuan.config().cycleBadgeStyle();
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
}
