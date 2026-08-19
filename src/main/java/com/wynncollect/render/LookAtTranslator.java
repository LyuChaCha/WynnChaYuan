package com.wynncollect.render;

import com.wynncollect.WynnCollect;
import com.wynncollect.translate.LineTranslator;
import com.wynntils.core.text.StyledText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 看著 NPC 時，在準心旁邊顯示名牌的翻譯。
 *
 * <h2>為什麼不直接把名牌換成中文</h2>
 * 就地取代會讓<b>畫面上再也看不到原文</b>。Wynncraft 是多人遊戲，跟其他玩家討論
 * 「去找 Blacksmith」時，如果你的畫面只有「鐵匠」，就對不上話——尤其老玩家
 * 只認得英文名。
 *
 * <p>所以改成：原文名牌<b>完全不動</b>，注視時另外跳一個小框顯示譯文。
 * 想知道意思就看一眼，要跟人溝通時原文就在那裡。
 *
 * <h2>判定方式</h2>
 * 用<b>視線夾角</b>而不是螢幕座標——後者會在名牌被其他東西擋住、或轉頭很快時
 * 抖動。夾角判定只看「有沒有對著它」，穩定得多。
 */
public final class LookAtTranslator {

    /** 視線與目標的夾角餘弦下限。0.995 約等於 5.7 度。 */
    private static final double AIM_THRESHOLD = 0.995;

    /** 超過這個距離就不顯示，避免遠處一堆名牌互相搶。 */
    private static final double MAX_DISTANCE = 24.0;

    /**
     * 記錄看過的名牌。
     *
     * <p>用 {@link WeakHashMap} 是刻意的：實體離開視野後 Minecraft 會回收它，
     * 這裡就跟著自動清掉，不需要自己管生命週期，也不會累積成記憶體洩漏。
     */
    private static final Map<Entity, StyledText> LABELS = new WeakHashMap<>();

    private LookAtTranslator() {}

    /** 由名牌事件呼叫，記下這個實體對應的原文。 */
    public static void remember(Entity entity, StyledText label) {
        if (entity != null && label != null) {
            LABELS.put(entity, label);
        }
    }

    public static void clear() {
        LABELS.clear();
    }

    /** 每幀呼叫。沒有對著任何名牌時什麼都不畫。 */
    public static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || LABELS.isEmpty()) {
            return;
        }
        StyledText target = findAimedLabel(mc);
        if (target == null) {
            return;
        }
        Component translated = LineTranslator.translate(target, WynnCollect.translations());
        if (translated == null) {
            return;                            // 沒有譯文就不打擾
        }
        drawBubble(graphics, mc, translated);
    }

    /** 找出玩家正對著的那個名牌。 */
    private static StyledText findAimedLabel(Minecraft mc) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();

        StyledText best = null;
        double bestDot = AIM_THRESHOLD;

        for (Map.Entry<Entity, StyledText> e : LABELS.entrySet()) {
            Entity entity = e.getKey();
            if (entity == null || !entity.isAlive()) {
                continue;
            }
            Vec3 delta = entity.position().subtract(eye);
            double distance = delta.length();
            if (distance > MAX_DISTANCE || distance < 0.1) {
                continue;
            }
            double dot = look.dot(delta.scale(1.0 / distance));
            if (dot > bestDot) {               // 越接近 1 表示越正對著
                bestDot = dot;
                best = e.getValue();
            }
        }
        return best;
    }

    /** 在準心下方畫一個小框。 */
    private static void drawBubble(GuiGraphics graphics, Minecraft mc, Component text) {
        int w = mc.font.width(text) + 8;
        int h = mc.font.lineHeight + 6;
        int x = (graphics.guiWidth() - w) / 2;
        int y = graphics.guiHeight() / 2 + 16;   // 準心下方，不擋住準心本身

        graphics.fill(x, y, x + w, y + h, 0xC0100010);
        graphics.fill(x, y, x + w, y + 1, 0xFF3A1E5C);
        graphics.fill(x, y + h - 1, x + w, y + h, 0xFF3A1E5C);
        graphics.fill(x, y, x + 1, y + h, 0xFF3A1E5C);
        graphics.fill(x + w - 1, y, x + w, y + h, 0xFF3A1E5C);
        graphics.drawString(mc.font, text, x + 4, y + 4, 0xFFFFFF);
    }
}
