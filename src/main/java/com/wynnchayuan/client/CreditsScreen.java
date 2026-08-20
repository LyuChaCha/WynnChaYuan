package com.wynnchayuan.client;

import com.wynnchayuan.CollectorConfig;
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

    /** 頁尾兩行說明加上兩排按鈕要留多少空間。名單不能長進這一塊。 */
    private static final int FOOTER_HEIGHT = 92;

    /** 名單最多排幾欄。再多就會窄到名字跟 ID 擠在一起。 */
    private static final int MAX_COLUMNS = 3;

    /** 欄與欄之間的空隙。 */
    private static final int COLUMN_GAP = 20;

    private final Screen parent;

    public CreditsScreen(Screen parent) {
        super(Component.literal("關於 " + WynnChaYuan.MOD_NAME));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // 標記的開關放在名單這一頁，因為它就是「這份名單要不要顯示在遊戲裡」。
        // 塞進 F6 主頁的話，看到那個選項的人不會知道它在講誰。
        addRenderableWidget(Button.builder(badgeLabel(), b -> {
            WynnChaYuan.config().toggleBadges();
            b.setMessage(badgeLabel());
        }).bounds(this.width / 2 - 155, this.height - 54, 150, 20).build());

        addRenderableWidget(Button.builder(styleLabel(), b -> {
            WynnChaYuan.config().cycleBadgeStyle();
            b.setMessage(styleLabel());
        }).bounds(this.width / 2 + 5, this.height - 54, 150, 20).build());

        addRenderableWidget(Button.builder(Component.literal("返回"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20).build());
    }

    private Component badgeLabel() {
        return Component.literal("遊戲內標記："
                + (WynnChaYuan.config().showBadges() ? "開" : "關"));
    }

    private Component styleLabel() {
        boolean gradient = WynnChaYuan.config().badgeStyle()
                == CollectorConfig.BadgeStyle.GRADIENT;
        return Component.literal("多重身分：" + (gradient ? "全部（漸層）" : "只顯示主要"));
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

        // 人變多之後一人一列會直接壓到頁尾說明與按鈕上（實際發生過）。
        // 先算得下幾欄，再照那個欄數排——名單本來就是一堆短字串，分欄比捲動好讀。
        int bottom = this.height - FOOTER_HEIGHT;
        int columns = columnsThatFit(y, bottom);
        int columnWidth = widestMember() + COLUMN_GAP;

        for (Credits.Section section : Credits.sections()) {
            drawSectionTitle(g, cx, y, section);
            y += 14;
            List<Credits.Member> members = section.members();
            if (members.isEmpty()) {
                g.drawCenteredString(this.font,
                        Component.literal("—— 等你加入 ——"), cx, y, Colors.FAINT);
                y += 13;
            }
            int used = Math.min(columns, Math.max(1, members.size()));
            int left = cx - used * columnWidth / 2;
            for (int i = 0; i < members.size(); i++) {
                int column = i % used;
                int centre = left + column * columnWidth + columnWidth / 2;
                drawMember(g, centre, y, members.get(i), section.color());
                if (column == used - 1) {
                    y += HEAD + 4;
                }
            }
            if (members.size() % used != 0) {
                y += HEAD + 4;          // 最後一列沒排滿，補上它的高度
            }
            y += 8;
        }

        y = this.height - FOOTER_HEIGHT + 8;
        for (String line : List.of(
                "名單上的人，名牌上方會多一行標記 —— 只有裝了本模組的人看得到",
                "本模組依賴 Wynntils，物品與技能資料取自其公開 CDN")) {
            g.drawCenteredString(this.font,
                    Component.literal(line).withStyle(ChatFormatting.DARK_GRAY), cx, y, Colors.FAINT);
            y += 11;
        }
    }

    /**
     * 名單排幾欄才塞得進 {@code top} 到 {@code bottom} 之間。
     *
     * <p>由少到多試，第一個放得下的就用——欄數越少越好讀。
     * 到了上限還是放不下就用上限，寧可擠一點也不要蓋到按鈕。
     */
    private int columnsThatFit(int top, int bottom) {
        for (int columns = 1; columns < MAX_COLUMNS; columns++) {
            if (top + heightOf(columns) <= bottom) {
                return columns;
            }
        }
        return MAX_COLUMNS;
    }

    /** 名單排成這麼多欄時，整份要多高。 */
    private int heightOf(int columns) {
        int height = 0;
        for (Credits.Section section : Credits.sections()) {
            int count = section.members().size();
            height += 14 + 8;                                  // 分區標題與段距
            height += count == 0
                    ? 13
                    : (count + columns - 1) / columns * (HEAD + 4);
        }
        return height;
    }

    /** 最寬的那一位有多寬。所有欄同寬，名字才對得齊。 */
    private int widestMember() {
        int widest = 0;
        for (Credits.Section section : Credits.sections()) {
            for (Credits.Member member : section.members()) {
                widest = Math.max(widest, memberWidth(member));
            }
        }
        return widest;
    }

    private int memberWidth(Credits.Member member) {
        int width = this.font.width(member.name());
        if (member.hasHead()) {
            width += HEAD + 5 + 6 + this.font.width(member.mc());
        }
        return width;
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
