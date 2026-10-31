package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public final class LookAtTranslator {

    private static final double LABEL_BOX = 0.5;

    private static final double MIN_AIM_RADIUS = 1.4;

    private static final double LABEL_LIFT = 0.5;

    private static final double LINE_HEIGHT = 0.28;

    private static final Map<Entity, StyledText> LABELS = new WeakHashMap<>();

    private static final Map<StyledText, Component> FLOATING = WynntilsText.lru(64);

    private static long floatingAge = -1;

    private static volatile Component lastShown = null;
    private static volatile long lastSeen = 0;

    private static volatile Entity lastSource = null;

    private static volatile String lastTemplate = null;

    private static volatile long shownAt = 0;

    private static final long MIN_SHOW_MS = 600;

    private LookAtTranslator() {}

    public static String labelAbove(Entity target) {
        if (target == null) {
            return null;
        }
        Vec3 base = target.position();
        String best = null;
        double bestHorizontal = Double.MAX_VALUE;
        for (Map.Entry<Entity, StyledText> e : LABELS.entrySet()) {
            Entity label = e.getKey();
            if (label == null || !label.isAlive() || e.getValue() == null) {
                continue;
            }
            Vec3 d = label.position().subtract(base);
            double horizontal = Math.sqrt(d.x * d.x + d.z * d.z);
            if (d.y < 0 || d.y > ABOVE_HEIGHT || horizontal > ABOVE_RADIUS) {
                continue;
            }
            if (horizontal < bestHorizontal) {
                bestHorizontal = horizontal;
                best = e.getValue().getStringWithoutFormatting().strip();
            }
        }
        return best;
    }

    private static final double ABOVE_HEIGHT = 3.5;

    private static final double ABOVE_RADIUS = 0.75;

    public static void remember(Entity entity, StyledText label) {
        if (entity != null && label != null) {
            LABELS.put(entity, label);
        }
    }

    public static void clear() {
        LABELS.clear();
        FLOATING.clear();
        lastShown = null;
        lastSource = null;
        lastTemplate = null;
        shownAt = 0;
    }

    private static boolean sameContent(StyledText text) {
        return lastTemplate != null && text != null
                && lastTemplate.equals(GlyphSplitter.toTemplate(text));
    }

    public static void render(GuiGraphics graphics) {
        if (WynnChaYuan.config() == null
                || WynnChaYuan.config().nametagMode()
                        != ModConfig.NametagMode.LOOK_AT) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || LABELS.isEmpty()) {
            return;
        }
        if (lastShown != null && System.currentTimeMillis() - shownAt < MIN_SHOW_MS) {
            drawRow(graphics, mc, List.of(lastShown), 1.0f);
            return;
        }
        Entity aimed = findAimedLabel(mc);
        StyledText target = aimed == null ? null : LABELS.get(aimed);
        if (target != null) {
            Component translated = floating(target);
            if (translated == null) {
                noteOnce("nametag.noMatch");
                if (lastShown != null && (aimed == lastSource || sameContent(target))) {
                    lastSeen = System.currentTimeMillis();
                    drawRow(graphics, mc, List.of(lastShown), 1.0f);
                }
                return;
            }
            noteOnce("nametag.shown");
            String template = GlyphSplitter.toTemplate(target);
            if (!template.equals(lastTemplate)) {
                shownAt = System.currentTimeMillis();
            }
            lastShown = translated;
            lastSource = aimed;
            lastTemplate = template;
            lastSeen = System.currentTimeMillis();
            List<Component> row = new ArrayList<>();
            row.add(translated);
            for (Entity other : nearbyLabels(mc, aimed)) {
                if (other == aimed || row.size() >= MAX_NEARBY) {
                    continue;
                }
                StyledText label = LABELS.get(other);
                Component near = label == null ? null : floating(label);
                if (near != null) {
                    row.add(near);
                }
            }
            drawRow(graphics, mc, row, 1.0f);
            return;
        }
        if (lastSource != null
                && (!lastSource.isAlive() || !LABELS.containsKey(lastSource))) {
            lastShown = null;
            lastSource = null;
            return;
        }
        int hold = WynnChaYuan.config().nametagHoldMs();
        if (lastShown != null && hold > 0) {
            float alpha = Fade.alphaFor(lastSeen, hold);
            if (alpha > 0f) {
                drawRow(graphics, mc, List.of(lastShown), alpha);
            }
        }
    }

    private static Component floating(StyledText label) {
        TranslationStore store = WynnChaYuan.translations();
        if (floatingAge != store.cacheAge()) {
            FLOATING.clear();
            floatingAge = store.cacheAge();
        }
        Component known = FLOATING.get(label);
        if (known == null && !FLOATING.containsKey(label)) {
            known = LineTranslator.translateFloating(label, store);
            FLOATING.put(label, known);
        }
        return known;
    }

    private static boolean blocked(Minecraft mc, Vec3 eye, Vec3 head) {
        if (mc.level == null) {
            return false;
        }
        ClipContext context = new ClipContext(eye, head, ClipContext.Block.VISUAL,
                        ClipContext.Fluid.NONE, mc.player);
        return mc.level.clip(context).getType()
                != HitResult.Type.MISS;
    }

    private static List<Entity> nearbyLabels(Minecraft mc, Entity aimed) {
        double range = WynnChaYuan.config().nametagRange();
        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();

        List<Entity> found = new ArrayList<>();
        Map<Entity, Double> offAxis = new HashMap<>();
        for (Map.Entry<Entity, StyledText> e : LABELS.entrySet()) {
            Entity entity = e.getKey();
            if (entity == null || !entity.isAlive() || entity == aimed) {
                continue;
            }
            Vec3 centre = labelCentre(entity, e.getValue());
            Vec3 delta = centre.subtract(eye);
            double distance = delta.length();
            if (distance > range || distance < 0.1) {
                continue;
            }
            double along = delta.dot(look);
            if (along <= 0) {
                continue;
            }
            if (Math.toDegrees(Math.acos(along / distance)) > VIEW_CONE) {
                continue;
            }
            if (blocked(mc, eye, centre)) {
                continue;
            }
            found.add(entity);
            offAxis.put(entity, delta.subtract(look.scale(along)).length());
        }
        found.sort(Comparator.comparingDouble(offAxis::get));
        if (aimed != null) {
            found.add(0, aimed);
        }
        return found.size() > MAX_NEARBY ? found.subList(0, MAX_NEARBY) : found;
    }

    private static final int MAX_NEARBY = 5;

    private static final double VIEW_CONE = 35.0;

    private static AABB labelBox(Entity entity, StyledText label) {
        return labelBox(entity.getBoundingBox(), entity.position(),
                        lineCount(label));
    }

    static AABB labelBox(AABB body, Vec3 at, int lines) {
        AABB around = body.inflate(LABEL_BOX);
        double bottom = body.maxY + LABEL_LIFT;
        double top = bottom + lines * LINE_HEIGHT;
        return new AABB(
                Math.min(around.minX, at.x - LABEL_BOX),
                Math.min(around.minY, bottom),
                Math.min(around.minZ, at.z - LABEL_BOX),
                Math.max(around.maxX, at.x + LABEL_BOX),
                Math.max(around.maxY, top),
                Math.max(around.maxZ, at.z + LABEL_BOX));
    }

    private static Vec3 labelCentre(Entity entity, StyledText label) {
        return labelCentre(entity.getBoundingBox(), entity.position(),
                           lineCount(label));
    }

    static Vec3 labelCentre(AABB body, Vec3 at, int lines) {
        double bottom = body.maxY + LABEL_LIFT;
        double top = bottom + lines * LINE_HEIGHT;
        return new Vec3(at.x, (bottom + top) / 2, at.z);
    }

    static int lineCount(StyledText label) {
        if (label == null) {
            return 1;
        }
        String text = label.getString();
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }

    private static Entity findAimedLabel(Minecraft mc) {
        double range = WynnChaYuan.config().nametagRange();
        double minDot = Math.cos(Math.toRadians(WynnChaYuan.config().nametagAngle()));

        Vec3 eye = mc.player.getEyePosition();
        Vec3 look = mc.player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));

        Entity hit = null;
        double hitDistance = Double.MAX_VALUE;
        Entity nearest = null;
        double nearestPerp = Double.MAX_VALUE;

        for (Map.Entry<Entity, StyledText> e : LABELS.entrySet()) {
            Entity entity = e.getKey();
            if (entity == null || !entity.isAlive()) {
                continue;
            }
            Vec3 centre = labelCentre(entity, e.getValue());
            double distance = centre.distanceTo(eye);
            if (distance > range || distance < 0.1) {
                continue;
            }
            if (blocked(mc, eye, centre)) {
                continue;
            }
            AABB box = labelBox(entity, e.getValue());
            if (box.clip(eye, end).isPresent()) {
                if (distance < hitDistance) {
                    hitDistance = distance;
                    hit = entity;
                }
                continue;
            }
            if (hit != null) {
                continue;
            }
            Vec3 delta = centre.subtract(eye);
            double along = delta.dot(look);
            if (along <= 0) {
                continue;
            }
            double perpendicular = delta.subtract(look.scale(along)).length();
            double allowed = Math.max(Math.sin(Math.acos(minDot)) * distance,
                                      MIN_AIM_RADIUS);
            if (perpendicular <= allowed && perpendicular < nearestPerp) {
                nearestPerp = perpendicular;
                nearest = entity;
            }
        }
        return hit != null ? hit : nearest;
    }

    private static String lastNoted = "";

    private static void noteOnce(String event) {
        if (!event.equals(lastNoted)) {
            lastNoted = event;
            WynnChaYuan.store().noteEvent(event);
        }
    }

    private static void drawRow(GuiGraphics graphics, Minecraft mc,
                                List<Component> boxes, float alpha) {
        if (boxes.isEmpty()) {
            return;
        }
        int lineHeight = mc.font.lineHeight + 1;
        List<List<Component>> rows = new ArrayList<>();
        List<Integer> widths = new ArrayList<>();
        List<Integer> heights = new ArrayList<>();
        int total = 0;
        int tallest = 0;
        for (Component box : boxes) {
            List<Component> lines = Boxes.toLines(box);
            if (lines.isEmpty()) {
                continue;
            }
            int w = 0;
            for (Component line : lines) {
                w = Math.max(w, mc.font.width(line));
            }
            w += 8;
            if (total > 0 && total + GAP + w > graphics.guiWidth() - 20) {
                break;
            }
            int h = lines.size() * lineHeight + 6;
            rows.add(lines);
            widths.add(w);
            heights.add(h);
            total += (total > 0 ? GAP : 0) + w;
            tallest = Math.max(tallest, h);
        }
        if (rows.isEmpty()) {
            return;
        }

        int x = (graphics.guiWidth() - total) / 2;
        int y = defaultY(graphics);
        if (WynnChaYuan.config().hasOverlayPos(ModConfig.Overlay.NAMETAG)) {
            x = WynnChaYuan.config().overlayX(ModConfig.Overlay.NAMETAG) - total / 2;
            y = WynnChaYuan.config().overlayY(ModConfig.Overlay.NAMETAG);
        }
        int[] at = Boxes.intoScreen(x, y, total, tallest, graphics.guiWidth(), graphics.guiHeight());
        x = at[0];
        y = at[1];

        for (int i = 0; i < rows.size(); i++) {
            List<Component> lines = rows.get(i);
            int w = widths.get(i);
            float a = i == 0 ? alpha : alpha * 0.65f;
            int h = heights.get(i);
            int top = y + topOf(tallest, h);
            Boxes.draw(graphics, x, top, w, h, a);
            int ty = top + 4;
            for (Component line : lines) {
                graphics.drawString(mc.font, line, x + 4, ty, Colors.fade(Colors.TEXT, a));
                ty += lineHeight;
            }
            x += w + GAP;
        }
    }

    static int topOf(int tallest, int height) {
        return (tallest - height) / 2;
    }

    private static final int GAP = 4;

    private static int defaultY(GuiGraphics graphics) {
        return TOP_MARGIN;
    }

    private static final int TOP_MARGIN = 34;
}
