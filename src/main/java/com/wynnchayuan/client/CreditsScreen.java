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

    /** 頭像邊長。和文字高度接近，排在一起才不會一大一小。 */
    private static final int HEAD = 12;

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

        for (Credits.Section section : Credits.sections()) {
            drawSectionTitle(g, cx, y, section);
            y += 14;
            for (Credits.Member member : section.members()) {
                drawMember(g, cx, y, member, section.color());
                y += HEAD + 4;
            }
            if (section.members().isEmpty()) {
                g.drawCenteredString(this.font,
                        Component.literal("—— 等你加入 ——"), cx, y, Colors.FAINT);
                y += 13;
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

    /** 分區標題：左右各一條同色細線，比純文字更看得出是分隔。 */
    private void drawSectionTitle(GuiGraphics g, int cx, int y, Credits.Section section) {
        String role = section.role();
        int color = section.color();
        g.drawCenteredString(this.font, Component.literal(role), cx, y, color);

        int half = this.font.width(role) / 2;
        int faded = (color & 0x00FFFFFF) | 0x60000000;
        g.fill(cx - half - 34, y + 3, cx - half - 6, y + 4, faded);
        g.fill(cx + half + 6, y + 3, cx + half + 34, y + 4, faded);
    }

    /**
     * 一位貢獻者：頭像、暱稱、Minecraft ID。
     *
     * <p>暱稱用分區的顏色，ID 用灰色 —— 兩者都用同一個顏色的話，
     * 一眼看過去分不出哪個是稱呼、哪個是遊戲帳號。
     */
    private void drawMember(GuiGraphics g, int cx, int y, Credits.Member member, int color) {
        String name = member.name();
        String id = member.hasHead() ? member.mc() : "";
        int gap = id.isEmpty() ? 0 : 6;

        int nameW = this.font.width(name);
        int idW = id.isEmpty() ? 0 : this.font.width(id);
        int headW = member.hasHead() ? HEAD + 5 : 0;
        int total = headW + nameW + gap + idW;

        int x = cx - total / 2;
        if (member.hasHead()) {
            PlayerHeads.draw(g, member.mc(), x, y, HEAD);
            x += HEAD + 5;
        }
        // 頭像 8px 高、文字 9px，讓文字對到頭像的視覺中線
        int textY = y + (HEAD - 8) / 2;
        g.drawString(this.font, Component.literal(name), x, textY, color);
        if (!id.isEmpty()) {
            g.drawString(this.font, Component.literal(id), x + nameW + gap, textY, Colors.DIM);
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

}
