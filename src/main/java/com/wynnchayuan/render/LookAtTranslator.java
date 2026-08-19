package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.LineTranslator;
import com.wynntils.core.text.StyledText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
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

    /**
     * 名牌實體本身幾乎沒有體積，直接拿它的碰撞箱去打射線會永遠打不中。
     * 撐開成大約一個方塊，對應玩家眼中「名牌那一塊」的大小。
     */
    private static final double LABEL_BOX = 0.5;

    /**
     * 記錄看過的名牌。
     *
     * <p>用 {@link WeakHashMap} 是刻意的：實體離開視野後 Minecraft 會回收它，
     * 這裡就跟著自動清掉，不需要自己管生命週期，也不會累積成記憶體洩漏。
     */
    private static final Map<Entity, StyledText> LABELS = new WeakHashMap<>();

    /** 上一次看到的譯文與時間，用來做「移開視線後再顯示一下」。 */
    private static volatile Component lastShown = null;
    private static volatile long lastSeen = 0;

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
        if (target != null) {
            Component translated = LineTranslator.translate(target, WynnChaYuan.translations());
            if (translated == null) {
                noteOnce("nametag.noMatch");
                return;                        // 沒有譯文就不打擾
            }
            noteOnce("nametag.shown");
            lastShown = translated;
            lastSeen = System.currentTimeMillis();
            drawBubble(graphics, mc, translated);
            return;
        }
        // 視線稍微移開時不要立刻消失 —— 準心對著 NPC 本來就不容易穩住，
        // 一離開就閃掉會讓人以為功能壞了。
        int hold = WynnChaYuan.config().nametagHoldMs();
        if (lastShown != null && hold > 0
                && System.currentTimeMillis() - lastSeen < hold) {
            drawBubble(graphics, mc, lastShown);
        }
    }

    /**
     * 找出玩家正對著的那個名牌。
     *
     * <h2>為什麼不是「夾角最小的贏」</h2>
     * 夾角會隨距離變小：遠處的 NPC 就算在畫面邊緣，夾角也可能比你面前這位還小。
     * 只比夾角的話，站在攤位前面卻翻到後面那排的名字。
     *
     * <p>所以分兩層：
     *
     * <ol>
     *   <li><b>準心真的打到誰</b>——用射線打名牌的碰撞箱，最近的那個贏。
     *       這是「我就是在看他」的情況，最明確，優先權最高。</li>
     *   <li>都沒打到才退回夾角錐，而且錐內<b>比距離</b>不比夾角——
     *       近的優先，因為那才是玩家眼前的東西。</li>
     * </ol>
     *
     * <p>距離與夾角都可以在設定裡調：城裡 NPC 站得密，錐要收窄；
     * 曠野找人則希望掃過去就跳。沒有一個值兩邊都好用。
     */
    private static StyledText findAimedLabel(Minecraft mc) {
        double range = WynnChaYuan.config().nametagRange();
        double minDot = Math.cos(Math.toRadians(WynnChaYuan.config().nametagAngle()));

        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));

        StyledText hit = null;
        double hitDistance = Double.MAX_VALUE;
        StyledText nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Map.Entry<Entity, StyledText> e : LABELS.entrySet()) {
            Entity entity = e.getKey();
            if (entity == null || !entity.isAlive()) {
                continue;
            }
            double distance = entity.position().distanceTo(eye);
            if (distance > range || distance < 0.1) {
                continue;
            }
            AABB box = entity.getBoundingBox().inflate(LABEL_BOX);
            if (box.clip(eye, end).isPresent()) {
                if (distance < hitDistance) {
                    hitDistance = distance;
                    hit = e.getValue();
                }
                continue;
            }
            if (hit != null) {
                continue;                      // 已經有直接命中的，錐內的就不用看了
            }
            Vec3 delta = entity.position().subtract(eye);
            if (look.dot(delta.scale(1.0 / distance)) >= minDot
                    && distance < nearestDistance) {
                nearestDistance = distance;
                nearest = e.getValue();
            }
        }
        return hit != null ? hit : nearest;
    }

    /**
     * 每幀都會走到，所以只在狀態變了才記一次，否則計數會被灌爆。
     */
    private static String lastNoted = "";

    private static void noteOnce(String event) {
        if (!event.equals(lastNoted)) {
            lastNoted = event;
            WynnChaYuan.store().noteEvent(event);
        }
    }

    /** 在準心下方畫一個小框。 */
    private static void drawBubble(GuiGraphics graphics, Minecraft mc, Component text) {
        // 名牌本來就是多行的，而且夾著 3D 版面用的對齊偏移 —— 都在這裡處理掉
        List<Component> lines = Boxes.toLines(text);
        if (lines.isEmpty()) {
            return;
        }
        int lineHeight = mc.font.lineHeight + 1;
        int w = 0;
        for (Component line : lines) {
            w = Math.max(w, mc.font.width(line));
        }
        w += 8;
        int h = lines.size() * lineHeight + 6;
        int x = (graphics.guiWidth() - w) / 2;
        int y = graphics.guiHeight() / 2 + 16;   // 準心下方，不擋住準心本身
        if (WynnChaYuan.config().hasOverlayPos(CollectorConfig.Overlay.NAMETAG)) {
            x = WynnChaYuan.config().overlayX(CollectorConfig.Overlay.NAMETAG);
            y = WynnChaYuan.config().overlayY(CollectorConfig.Overlay.NAMETAG);
        }

        Boxes.draw(graphics, x, y, w, h);
        int ty = y + 4;
        for (Component line : lines) {
            graphics.drawString(mc.font, line, x + 4, ty, Colors.TEXT);
            ty += lineHeight;
        }
    }
}
