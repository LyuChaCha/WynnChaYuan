package com.wynnchayuan.client;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.render.Colors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 關於與貢獻者。
 *
 * <p>翻譯是靠很多人一條一條填出來的，名單值得放在看得到的地方。
 * 內容讀自 {@code assets/wynnchayuan/credits.json}，加人不必改程式。
 */
public final class CreditsScreen extends Screen {

    private final Screen parent;

    public CreditsScreen(Screen parent) {
        super(Component.literal("關於 " + WynnChaYuan.MOD_NAME));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("返回"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 30, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);

        int cx = this.width / 2;
        int y = 30;

        g.drawCenteredString(this.font, this.title, cx, y, Colors.TEXT);
        y += 14;
        g.drawCenteredString(this.font,
                Component.literal("v" + WynnChaYuan.version()).withStyle(ChatFormatting.DARK_GRAY),
                cx, y, Colors.DIM);
        y += 22;

        g.drawCenteredString(this.font,
                Component.literal("Wynncraft 繁體中文翻譯").withStyle(ChatFormatting.GRAY),
                cx, y, Colors.SUBTLE);
        y += 24;

        for (Section section : Credits.sections()) {
            g.drawCenteredString(this.font,
                    Component.literal(section.title()).withStyle(ChatFormatting.YELLOW),
                    cx, y, Colors.HIGHLIGHT);
            y += 12;
            for (String name : section.names()) {
                g.drawCenteredString(this.font,
                        Component.literal(name).withStyle(ChatFormatting.WHITE), cx, y, Colors.TEXT);
                y += 11;
            }
            y += 8;
        }

        y = this.height - 62;
        for (String line : List.of(
                "本模組依賴 Wynntils，物品與技能資料取自其公開 CDN",
                "材質包符號與排版由 Wynncraft 提供，本模組僅顯示不修改")) {
            g.drawCenteredString(this.font,
                    Component.literal(line).withStyle(ChatFormatting.DARK_GRAY), cx, y, Colors.FAINT);
            y += 11;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** 名單的一個分區。 */
    public record Section(String title, List<String> names) {}
}
