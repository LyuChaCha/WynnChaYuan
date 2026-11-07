package com.wynnchayuan.client.ui;

import com.wynnchayuan.PanelGap;
import com.wynnchayuan.translate.TextWrapper;

import java.util.List;
import java.util.function.UnaryOperator;

public final class Preview {

    private Preview() {}

    public enum Scene {
        TOOLTIP, PANEL, DIALOGUE, WORLD, DATA,
        CAPE
    }

    public enum Spot {
        TOOLTIP(Scene.TOOLTIP), DIALOGUE(Scene.DIALOGUE), CHOICES(Scene.DIALOGUE),
        NAMEPLATE(Scene.WORLD), HELD(Scene.WORLD), TITLE(Scene.WORLD), BOSS(Scene.WORLD),
        TRACKER(Scene.WORLD), BOARD(Scene.WORLD), CHAT(Scene.WORLD);

        public final Scene scene;

        Spot(Scene scene) {
            this.scene = scene;
        }
    }

    public static final Spot[] SPOTS = Spot.values();

    public static final class State {
        public int tooltipMode;
        public int names = 2;
        public boolean anchorFixed;
        public int side;
        public int gap = 12;
        public int dialogueMode = 1;
        public int choiceMode = 1;
        public int holdSeconds = 6;
        public boolean overlays = true;
        public int nametag = 1;
        public int chat = 1;
        public boolean titles = true;
        public boolean bossbar = true;
        public int tracker;
        public boolean objectives = true;
        public boolean scoreboard = true;
        public boolean heldItem = true;

        public int loaded;
        public String loadedLabel = "";
        public String[][] facts = new String[0][];
        public int versionColour = Ui.TEXT;
        public String panelOffNote = "";

        public UnaryOperator<String> translate = s -> s;

        public final float[][] spots = new float[SPOTS.length][4];
        public final boolean[] shown = new boolean[SPOTS.length];

        void mark(Spot spot, float x, float y, float w, float h) {
            float[] box = spots[spot.ordinal()];
            if (!shown[spot.ordinal()]) {
                box[0] = x;
                box[1] = y;
                box[2] = w;
                box[3] = h;
                shown[spot.ordinal()] = true;
                return;
            }
            float x1 = Math.max(box[0] + box[2], x + w);
            float y1 = Math.max(box[1] + box[3], y + h);
            box[0] = Math.min(box[0], x);
            box[1] = Math.min(box[1], y);
            box[2] = x1 - box[0];
            box[3] = y1 - box[1];
        }
    }

    private static final String ITEM = "Bony Bow";
    private static final String[] ITEM_LINES = {"Durability", "Health", "Mana"};
    private static final String SPEAKER = "Aledar";
    private static final String LINE = "Well, what are we waiting for?";
    private static final String[] CHOICES = {"Yes", "Goodbye."};
    private static final String CONTINUE = "to continue";
    private static final String BOSS = "Zombie";
    private static final String NPC = "Party Finder";
    private static final String NPC_ROLE = "Find a party or queue for a raid";
    private static final String NPC_LABEL = String.join("\n", "{#}", NPC, NPC_ROLE);
    private static final int NPC_NAME = 0xFFFF55FF;
    private static final int NPC_ABOUT = 0xFFAAAAAA;
    private static final int TITLE_GOLD = 0xFFFFAA00;
    private static final String TITLE = "Level Up!";
    private static final String TRACK_1 = "World Event";
    private static final String TRACK_2 = "Click To Track";
    private static final String CHAT = "The trade has been completed!";
    private static final String SCORE_HEAD = "Daily Objective:";
    private static final String[] SCORE_LINES = {
        "- Loot Chests T{~}+: {~}/{~}", "- Mobs slain: {~}/{~}"};
    private static final String[][] SCORE_VALUES = {{"2", "3", "5"}, {"42", "100"}};

    static final String BG_STREET = "wynnchayuan:textures/gui/preview/street.png";
    static final String BG_WORLD = "wynnchayuan:textures/gui/preview/world.png";
    private static final String DIALOGUE_STYLE = "minecraft:textures/font/hud/dialogue/style/default/";
    private static final String DLG_BOX = DIALOGUE_STYLE + "box.png";
    private static final String DLG_FRAME = DIALOGUE_STYLE + "frame.png";
    private static final String DLG_PLATE = DIALOGUE_STYLE + "nameplate.png";
    private static final String DLG_FOOTER = DIALOGUE_STYLE + "footer.png";
    private static final String DLG_CHOICE = DIALOGUE_STYLE + "choice.png";
    private static final String DLG_FACE = "minecraft:textures/font/hud/dialogue/portrait/wynn/main/aledar.png";
    private static final String BOSS_BACK = "minecraft:textures/gui/sprites/boss_bar/red_background.png";
    private static final String BOSS_FILL = "minecraft:textures/gui/sprites/boss_bar/red_progress.png";
    private static final String TIP_BACK = "minecraft:textures/gui/sprites/tooltip/rare_background.png";
    private static final String TIP_FRAME = "minecraft:textures/gui/sprites/tooltip/rare_frame.png";

    private static final int TIP_BG = 0xF0160A22;
    private static final int TIP_EDGE = 0xFF4B2A8A;
    private static final int BOX_BG = 0xF00A1018;
    private static final int DLG_BG = 0xF022180E;
    private static final int DLG_EDGE = 0xFF7A5C2E;
    private static final int DLG_TEXT = 0xFFF1E3C4;
    private static final int SCENE_EDGE = 0xFF3F6B4C;
    private static final int SHADE = 0x40000000;

    public static void draw(Canvas c, int x, int y, int w, int h, Scene scene, State s,
                            int frame, int accent, float gapShown) {
        for (int i = 0; i < s.shown.length; i++) {
            s.shown[i] = false;
        }
        c.round(x, y, w, h, Ui.R2, Ui.SCENE);
        c.clip(x + 1, y + 1, x + w - 1, y + h - 1);
        String bg = switch (scene) {
            case WORLD -> BG_STREET;
            case TOOLTIP, PANEL, DIALOGUE -> BG_WORLD;
            default -> null;
        };
        boolean scenery = bg != null && backdrop(c, x + 1, y + 1, w - 2, h - 2, Ui.R2 - 1, bg);
        if (!scenery) {
            grid(c, x, y, w, h);
        }
        Canvas g = SCENE_TEXT.on(c);
        switch (scene) {
            case TOOLTIP -> tooltip(g, x, y, w, h, s, frame, gapShown, false);
            case PANEL -> tooltip(g, x, y, w, h, s, frame, gapShown, true);
            case DIALOGUE -> dialogue(g, x, y, w, h, s, frame);
            case WORLD -> world(g, x, y, w, h, s, frame, scenery);
            case CAPE -> { }
            default -> data(c, x, y, w, h, s, accent);
        }
        if (scene == Scene.PANEL && s.tooltipMode != 0 && !s.panelOffNote.isEmpty()) {
            notice(c, x, y, w, h, s.panelOffNote);
        }
        c.unclip();
        c.ring(x, y, w, h, Ui.R2, c.px(), SCENE_EDGE);
    }

    private static final SceneText SCENE_TEXT = new SceneText();

    private static void notice(Canvas c, int x, int y, int w, int h, String text) {
        float bx = x + 6;
        float bw = w - 12;
        List<String> lines = Ui.wrap(c, text, Math.round(bw - 23));
        if (lines.isEmpty() || bw < 40) {
            return;
        }
        float bh = lines.size() * 10 + 9;
        float by = y + h - 6 - bh;
        c.round(bx - 1, by + 1, bw + 2, bh + 1, Ui.R1 + 1, 0x30000000);
        Ui.pane(c, bx, by, bw, bh, Ui.R1, Ui.POP, Ui.alpha(Ui.AMBER, 0x8C));
        c.round(bx + 7, by + 7.5f, 4, 4, 2, Ui.AMBER);
        for (int i = 0; i < lines.size(); i++) {
            c.text(lines.get(i), bx + 16, by + 5 + i * 10, Ui.TEXT);
        }
    }

    static float snap(Canvas c, float want) {
        float px = c.px();
        if (want < px) {
            return want;
        }
        return (float) Math.floor(want / px + 0.001f) * px;
    }

    static final float SCENE_PAD = 6;
    static final float DIALOGUE_W = 287;
    static final float DIALOGUE_STACK_H = 252;

    static final float READABLE = 0.6f;

    static float fit(Canvas c, float want) {
        float px = c.px();
        float k = Math.max(0.25f, Math.min(1f, want));
        float steps = (float) Math.floor(k / px + 0.03f);
        return steps >= 1 ? Math.min(1f, steps * px) : k;
    }

    public static float snugWidth(Canvas c) {
        float px = c.px();
        float k = Math.min(1f, (float) Math.ceil(READABLE / px - 0.001f) * px);
        return (float) Math.ceil(DIALOGUE_W * k + SCENE_PAD * 2);
    }

    static float dialogueScale(Canvas c, int w, int h) {
        return fit(c, Math.min((w - SCENE_PAD * 2) / DIALOGUE_W,
                               (h - SCENE_PAD) / DIALOGUE_STACK_H));
    }

    private static void grid(Canvas c, int x, int y, int w, int h) {
        for (int gx = x + 16; gx < x + w - 1; gx += 16) {
            c.fill(gx, y + 1, gx + 1, y + h - 1, 0x09FFFFFF);
        }
        for (int gy = y + 16; gy < y + h - 1; gy += 16) {
            c.fill(x + 1, gy, x + w - 1, gy + 1, 0x09FFFFFF);
        }
    }

    private static boolean backdrop(Canvas c, float x, float y, float w, float h, float r,
                                    String id) {
        int tw = c.textureWidth(id);
        int th = c.textureHeight(id);
        if (tw <= 0 || th <= 0) {
            return false;
        }
        float px = c.px();
        float k = (float) Math.ceil(Math.max(w / tw, h / th) / px - 0.001f) * px;
        float u0 = (tw - w / k) / 2f;
        float v0 = (th - h / k) / 2f;
        float step = Math.max(px, 0.25f);
        float top = Math.min(r, h / 2f);
        for (float dy = 0; dy < top; dy += step) {
            float in = inset(r, dy + step / 2f);
            band(c, id, x, y, w, k, u0, v0, in, dy, step);
            band(c, id, x, y, w, k, u0, v0, in, h - dy - step, step);
        }
        if (h > top * 2) {
            c.image(id, x, y + top, w, h - top * 2, u0, v0 + top / k, w / k, (h - top * 2) / k,
                    0xFFFFFFFF);
        }
        c.round(x, y, w, h, r, SHADE);
        return true;
    }

    private static float inset(float r, float dy) {
        float d = r - dy;
        return r - (float) Math.sqrt(Math.max(0f, r * r - d * d));
    }

    private static void band(Canvas c, String id, float x, float y, float w, float k, float u0,
                             float v0, float in, float dy, float step) {
        float bw = w - in * 2;
        if (bw > 0) {
            c.image(id, x + in, y + dy, bw, step, u0 + in / k, v0 + dy / k, bw / k, step / k,
                    0xFFFFFFFF);
        }
    }

    private static boolean has(Canvas c, String id) {
        return c.textureWidth(id) > 0;
    }

    private static float edge(int i, float start, float near, float size, float far) {
        return switch (i) {
            case 0 -> start;
            case 1 -> start + near;
            case 2 -> start + size - far;
            default -> start + size;
        };
    }

    private static void nine(Canvas c, String id, float x, float y, float w, float h, int side,
                             int top) {
        int tw = c.textureWidth(id);
        int th = c.textureHeight(id);
        for (int i = 0; i < 3; i++) {
            float x0 = edge(i, x, side, w, side);
            float x1 = edge(i + 1, x, side, w, side);
            float u0 = edge(i, 0, side, tw, side);
            float u1 = edge(i + 1, 0, side, tw, side);
            for (int j = 0; j < 3; j++) {
                float y0 = edge(j, y, top, h, top);
                float y1 = edge(j + 1, y, top, h, top);
                float v0 = edge(j, 0, top, th, top);
                float v1 = edge(j + 1, 0, top, th, top);
                if (x1 > x0 && y1 > y0) {
                    c.image(id, x0, y0, x1 - x0, y1 - y0, u0, v0, u1 - u0, v1 - v0, 0xFFFFFFFF);
                }
            }
        }
    }

    private static void shadowed(Canvas c, String text, float x, float y, int argb) {
        c.text(text, x, y, argb);
    }

    public static String bareName(String translated, String original, int appendedAt) {
        if (appendedAt > 0) {
            return translated.substring(0, appendedAt);
        }
        String tail = " (" + original + ")";
        if (translated.endsWith(tail) && translated.length() > tail.length()) {
            return translated.substring(0, translated.length() - tail.length());
        }
        return translated;
    }

    private static void tooltip(Canvas c, int x, int y, int w, int h, State s,
                                int accent, float gapShown, boolean forcePanel) {
        c.fill(x, y, x + w, y + h, 0x50000000);
        String zhName = s.translate.apply(ITEM);
        String name = s.names == 0 ? zhName
                : s.names == 1 ? zhName + " (" + ITEM + ")" : ITEM;
        boolean replace = s.tooltipMode == 1 && !forcePanel;
        boolean panel = s.tooltipMode == 0 || forcePanel;

        String[] tip = new String[ITEM_LINES.length + 1];
        String[] side = new String[ITEM_LINES.length + 1];
        tip[0] = replace ? name : ITEM;
        side[0] = name;
        for (int i = 0; i < ITEM_LINES.length; i++) {
            String zh = s.translate.apply(ITEM_LINES[i]);
            tip[i + 1] = replace ? zh : ITEM_LINES[i];
            side[i + 1] = zh;
        }
        int[] tipColours = {0xFFFFFFFF, 0xFFB8C0CC, 0xFFF08A8A, 0xFF7FC8E8};
        boolean framed = has(c, TIP_BACK) && has(c, TIP_FRAME);
        int pad = framed ? 9 : 0;

        int room = w - 12 - pad * 2;
        int tipW = Math.min(widest(c, tip) + 10, panel ? room * 42 / 100 : room);
        int sideW = panel ? Math.min(widest(c, side) + 10, room * 42 / 100) : 0;
        int boxH = tip.length * 10 + 7;
        int top = y + Math.max(24, (h - boxH) / 2 - 4);

        if (panel && s.anchorFixed) {
            int tx = x + (w - tipW) / 2 + 12;
            gameTip(c, tx, top + 14, tipW, tip, tipColours, framed);
            lines(c, x + 6, y + 6, sideW, side, tipColours, BOX_BG, accent);
            Ui.glyph(c, x + sideW - 2, y + 2, Ui.ICON_PIN, Ui.GOLD);
            cursor(c, tx - 6 - pad, top + 10 - pad);
            s.mark(Spot.TOOLTIP, tx - pad, top + 14 - pad, tipW + pad * 2, boxH + pad * 2);
            s.mark(Spot.TOOLTIP, x + 6, y + 6, sideW, boxH);
            return;
        }
        int budget = Math.max(0, room - tipW - sideW);
        int gap = panel ? PanelGap.preview(gapShown, budget) : 0;
        int total = tipW + (panel ? gap + sideW + pad : 0);
        int left = x + (w - total) / 2;
        boolean leftSide = s.side == 2;
        int tipX = panel && leftSide ? left + sideW + gap + pad : left;
        int sideX = leftSide ? left : left + tipW + gap + pad;

        gameTip(c, tipX, top, tipW, tip, tipColours, framed);
        cursor(c, tipX - 5 - pad, top - 5 - pad);
        s.mark(Spot.TOOLTIP, tipX - pad, top - pad, tipW + pad * 2, boxH + pad * 2);
        if (!panel) {
            return;
        }
        lines(c, sideX, top, sideW, side, tipColours, BOX_BG, accent);
        Ui.glow(c, sideX, top, sideW, boxH, Ui.alpha(accent, 0x40));
        s.mark(Spot.TOOLTIP, sideX, top, sideW, boxH);

        int a = Math.min(tipX + tipW + (leftSide ? 0 : pad), sideX + sideW);
        int b = Math.max(tipX - (leftSide ? pad : 0), sideX);
        int ry = top - 8 - pad;
        c.fill(a, ry, a + 1, ry + 5, accent);
        c.fill(Math.max(a, b - 1), ry, Math.max(a + 1, b), ry + 5, accent);
        if (b - a > 2) {
            c.fill(a + 1, ry + 2, b - 1, ry + 3, accent);
        }
        String label = Math.round(gapShown) + " px";
        shadowed(c, label, (a + b - c.width(label)) / 2f, ry - 10, accent);
    }

    private static void gameTip(Canvas c, int x, int y, int w, String[] text, int[] colours,
                                boolean framed) {
        if (!framed) {
            lines(c, x, y, w, text, colours, TIP_BG, TIP_EDGE);
            return;
        }
        int h = text.length * 10 + 7;
        nine(c, TIP_BACK, x - 9, y - 9, w + 18, h + 18, 10, 12);
        nine(c, TIP_FRAME, x - 9, y - 9, w + 18, h + 18, 10, 12);
        for (int i = 0; i < text.length; i++) {
            shadowed(c, Ui.fit(c, text[i], w - 8), x + 4, y + 4 + i * 10,
                     colours[Math.min(i, colours.length - 1)]);
        }
    }

    private static void lines(Canvas c, int x, int y, int w, String[] text, int[] colours,
                              int bg, int edge) {
        int h = text.length * 10 + 7;
        Ui.box(c, x, y, w, h, bg, edge);
        for (int i = 0; i < text.length; i++) {
            c.text(Ui.fit(c, text[i], w - 8), x + 4, y + 4 + i * 10,
                   colours[Math.min(i, colours.length - 1)]);
        }
    }

    private static int widest(Canvas c, String[] text) {
        int out = 0;
        for (String line : text) {
            out = Math.max(out, c.width(line));
        }
        return out;
    }

    private static void cursor(Canvas c, int x, int y) {
        String[] arrow = {"#....", "##...", "###..", "####.", "#####", "##...", "#.#.."};
        Ui.glyph(c, x + 1, y + 1, arrow, 0xA0000000);
        Ui.glyph(c, x, y, arrow, 0xFFFFFFFF);
    }

    private static final String HUD = "minecraft:textures/font/hud/gameplay/default/bottom_middle/";
    private static final String HUD_BANNER = HUD + "hotbar.png";
    private static final String HUD_XP = HUD + "experience_bar.png";
    private static final String HUD_HEALTH = HUD + "health_background.png";
    private static final String HUD_HEALTH_BAR = HUD + "health_bar.png";
    private static final String HUD_MANA = HUD + "mana_background.png";
    private static final String HUD_MANA_BAR = HUD + "mana_bar.png";
    private static final String HUD_LEVEL = HUD + "hex_meter.png";
    private static final String HOTBAR = "minecraft:textures/gui/sprites/hud/hotbar.png";
    private static final String HOTBAR_PICK = "minecraft:textures/gui/sprites/hud/hotbar_selection.png";
    private static final String[] HOTBAR_ITEMS = {
        "minecraft:textures/item/bow.png", "minecraft:textures/wynn/potion/healing_full.png",
        "minecraft:textures/wynn/potion/mana_full.png"};
    private static final String HOTBAR_BOOK = "minecraft:textures/wynn/gui/player/content_book.png";
    private static final int HUD_W = 232;
    private static final int HUD_H = 55;
    private static final int OUTLINE = 0xFF3A2414;

    private static boolean wynnHud(Canvas c) {
        return has(c, HUD_BANNER) && has(c, HOTBAR) && has(c, HUD_HEALTH) && has(c, HUD_MANA)
                && has(c, HUD_LEVEL);
    }

    private static void tex(Canvas c, String id, float ox, float oy, float s, float tx, float ty,
                            float u, float v, float uw, float vh) {
        c.image(id, ox + tx * s, oy + ty * s, uw * s, vh * s, u, v, uw, vh, 0xFFFFFFFF);
    }

    private static float hotbar(Canvas c, int x, int y, int w, int h, float s) {
        if (!wynnHud(c)) {
            return drawnHotbar(c, x, y, w, h);
        }
        float ox = Math.round(x + (w - HUD_W * s) / 2f);
        float oy = Math.round(y + h - 2 - HUD_H * s);
        tex(c, HUD_BANNER, ox, oy, s, 12, -8, 0, 0, 208, 64);
        if (has(c, HUD_XP)) {
            tex(c, HUD_XP, ox, oy, s, 26, 25, 0, 102, 179, 3);
        }
        tex(c, HOTBAR, ox, oy, s, 25, 30, 0, 0, 182, 22);
        tex(c, HUD_HEALTH, ox, oy, s, 15, 5, 0, 0, 72, 20);
        if (has(c, HUD_HEALTH_BAR)) {
            c.image(HUD_HEALTH_BAR, ox + 26 * s, oy + 16 * s, 55 * s, 6 * s, 4, 0, 1, 6, 0xFFFFFFFF);
        }
        tex(c, HUD_MANA, ox, oy, s, 144, 5, 0, 0, 72, 20);
        if (has(c, HUD_MANA_BAR)) {
            c.image(HUD_MANA_BAR, ox + 149 * s, oy + 16 * s, 55 * s, 6 * s, 4, 0, 1, 6, 0xFFFFFFFF);
        }
        tex(c, HUD_LEVEL, ox, oy, s, 104, 3, 0, 0, 24, 27);
        for (int i = 0; i < HOTBAR_ITEMS.length; i++) {
            if (has(c, HOTBAR_ITEMS[i])) {
                tex(c, HOTBAR_ITEMS[i], ox, oy, s, 28 + i * 20, 33, 0, 0, 16, 16);
            }
        }
        if (has(c, HOTBAR_BOOK)) {
            tex(c, HOTBAR_BOOK, ox, oy, s, 28 + 8 * 20, 33, 0, 0, 16, 16);
        }
        if (has(c, HOTBAR_PICK)) {
            tex(c, HOTBAR_PICK, ox, oy, s, 24, 29, 0, 0, 24, 23);
        }
        hudNumber(c, "420 / 436", ox + 15 * s, oy + 7 * s, 72 * s, s, 0xFFFFFFFF, false);
        hudNumber(c, "62 / 62", ox + 144 * s, oy + 7 * s, 72 * s, s, 0xFFFFFFFF, false);
        hudNumber(c, "22", ox + 104 * s, oy + 12.5f * s, 24 * s, s, 0xFFFFFFFF, true);
        return oy + 4 * s;
    }

    private static void hudNumber(Canvas c, String text, float x, float y, float w, float s,
                                  int argb, boolean outline) {
        float tw = c.width(text) * s;
        float tx = x + (w - tw) / 2f;
        if (c instanceof SceneText scene) {
            float was = scene.size;
            boolean shadow = scene.shadow;
            scene.size = s;
            scene.shadow = !outline;
            if (outline) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx != 0 || dy != 0) {
                            c.text(text, tx + dx * s, y + dy * s, OUTLINE);
                        }
                    }
                }
            }
            c.text(text, tx, y, argb);
            scene.size = was;
            scene.shadow = shadow;
            return;
        }
        c.text(text, tx, y, argb);
    }

    private static float drawnHotbar(Canvas c, int x, int y, int w, int h) {
        float cell = Math.max(10, snap(c, Math.min(20, (w - 14) / 9f)));
        float bw = cell * 9 + 2;
        float bh = cell + 2;
        float bx = Math.round(x + (w - bw) / 2f);
        float by = Math.round(y + h - bh - 4);
        c.fill(bx, by, bx + bw, by + bh, 0xB0000000);
        for (int i = 0; i < 9; i++) {
            float cx = bx + 1 + i * cell;
            c.fill(cx + 1, by + 2, cx + cell - 1, by + bh - 2, 0x8C3A3A3A);
            c.fill(cx + 1, by + 2, cx + cell - 1, by + 3, 0x50FFFFFF);
        }
        c.ring(bx - 1, by - 1, cell + 4, bh + 2, 1, 1, 0xFFE8E8E8);
        float ix = bx + 1 + (cell - 9) / 2f;
        float iy = by + 1 + (cell - 9) / 2f;
        c.fill(ix + 2, iy, ix + 3, iy + 9, 0xFFB98A5E);
        c.fill(ix + 3, iy + 1, ix + 5, iy + 2, 0xFFB98A5E);
        c.fill(ix + 3, iy + 7, ix + 5, iy + 8, 0xFFB98A5E);
        c.fill(ix + 5, iy + 2, ix + 6, iy + 7, 0xFFE6E6E6);
        return by;
    }

    static List<String> wrap(Canvas c, String text, int maxW) {
        return List.of(TextWrapper.wrapToWidth(text, maxW, c::width).split("\n"));
    }

    private static void dialogue(Canvas c, int x, int y, int w, int h, State s, int accent) {
        String zhLine = s.translate.apply(LINE);
        boolean lineZh = s.dialogueMode == 1;
        boolean choiceZh = s.choiceMode == 1;
        float k = dialogueScale(c, w, h);
        float barTop = hotbar(c, x, y, w, h, k);
        SceneText scene = c instanceof SceneText t ? t : null;

        float dw = DIALOGUE_W * k;
        float dh = 106 * k;
        float dx = Math.round(x + (w - dw) / 2f);
        float dy = barTop - 2 - dh;
        boolean textured = has(c, DLG_BOX) && has(c, DLG_FRAME);
        if (textured) {
            if (has(c, DLG_FOOTER)) {
                c.image(DLG_FOOTER, dx + 47 * k, dy + 87 * k, 224 * k, 18 * k, 0, 0, 224, 18, 0xFFFFFFFF);
            }
            c.image(DLG_BOX, dx + 31 * k, dy + 9 * k, 256 * k, 80 * k, 0, 0, 256, 80, 0xFFFFFFFF);
            c.image(DLG_FRAME, dx, dy, 64 * k, 80 * k, 0, 0, 64, 80, 0xFFFFFFFF);
            if (has(c, DLG_FACE)) {
                c.image(DLG_FACE, dx + 8 * k, dy + 8 * k, 48 * k, 64 * k, 0, 0, 48, 64, 0xFFFFFFFF);
            }
            if (has(c, DLG_PLATE)) {
                c.image(DLG_PLATE, dx + 64 * k, dy, 112 * k, 20 * k, 0, 0, 112, 20, 0xFFFFFFFF);
            }
        } else {
            c.fill(dx + 47 * k, dy + 87 * k, dx + 271 * k, dy + 105 * k, 0xC0120D08);
            Ui.box(c, Math.round(dx + 31 * k), Math.round(dy + 9 * k), Math.round(256 * k),
                   Math.round(80 * k), DLG_BG, DLG_EDGE);
            Ui.box(c, Math.round(dx), Math.round(dy), Math.round(64 * k), Math.round(80 * k),
                   0xF0302010, DLG_EDGE);
            c.fill(dx + 12 * k, dy + 12 * k, dx + 52 * k, dy + 68 * k, 0xFF8A6A4A);
            Ui.box(c, Math.round(dx + 64 * k), Math.round(dy), Math.round(112 * k),
                   Math.round(20 * k), 0xF0302010, DLG_EDGE);
        }
        if (scene != null) {
            scene.size = k;
            scene.shadow = false;
            scene.dialogue = true;
        }
        String speaker = Ui.fit(c, SPEAKER, Math.round(108 * k));
        c.text(speaker, dx + 64 * k + (112 * k - c.width(speaker)) / 2f, dy + 6 * k, Ui.GOLD);
        List<String> rows = wrap(c, lineZh ? zhLine : LINE, Math.round(202 * k));
        for (int i = 0; i < Math.min(5, rows.size()); i++) {
            c.text(rows.get(i), dx + 71 * k, dy + (24 + i * 12) * k, DLG_TEXT);
        }
        String cont = lineZh ? s.translate.apply(CONTINUE) : CONTINUE;
        float keyW = (c.width("SHIFT") + 6);
        float hintW = keyW + 4 * k + c.width(cont);
        float hx = dx + 47 * k + (224 * k - hintW) / 2f;
        float hy = dy + 92 * k;
        c.round(hx, hy - 1 * k, keyW, 10 * k, 1.5f * k, 0xFFECE6DD);
        c.fill(hx, hy + 8 * k, hx + keyW, hy + 9 * k, 0xFF9A8F84);
        c.text("SHIFT", hx + 3 * k, hy, 0xFF3A2A22);
        c.text(cont, hx + keyW + 4 * k, hy, 0xFFE8DCC0);
        s.mark(Spot.DIALOGUE, dx, dy, dw, dh);

        float cw = 144 * k;
        float ch = 16 * k;
        float cx = dx + dw - cw;
        float cy = dy;
        boolean bars = has(c, DLG_CHOICE);
        for (int i = CHOICES.length - 1; i >= 0; i--) {
            cy -= ch + 1 * k;
            boolean on = i == 0;
            if (bars) {
                c.image(DLG_CHOICE, cx, cy, cw, 12 * k, on ? 144 : 0, 0, 144, 12, 0xFFFFFFFF);
            } else {
                Ui.box(c, Math.round(cx), Math.round(cy), Math.round(cw), Math.round(12 * k),
                       on ? 0xF03A2A14 : 0xE0201810, on ? 0xFFC9A257 : DLG_EDGE);
            }
            String text = choiceZh ? s.translate.apply(CHOICES[i]) : CHOICES[i];
            c.text(Ui.fit(c, text, Math.round((cw - 22 * k))), cx + 8 * k, cy + 2 * k,
                   on ? 0xFFFFF2C8 : 0xFFD8C8A0);
            if (on) {
                c.text(">", cx - 7 * k, cy + 2 * k, 0xFFFFE08A);
            }
        }
        s.mark(Spot.CHOICES, cx, cy, cw, dy - cy);

        if (scene != null) {
            scene.dialogue = false;
            scene.shadow = true;
        }
        float line = 10 * k;
        float above = cy - 6 * k;
        if (s.choiceMode == 0 && s.overlays) {
            String[] zh = new String[CHOICES.length];
            float tw = 0;
            for (int i = 0; i < zh.length; i++) {
                zh[i] = "[" + (i + 1) + "] " + s.translate.apply(CHOICES[i]);
                tw = Math.max(tw, c.width(zh[i]));
            }
            tw = Math.min(w - 20, tw + 10 * k);
            float th = zh.length * line + 7 * k;
            float tx = Math.round(Math.max(x + 4, Math.min(dx + dw - tw, x + w - 4 - tw)));
            float ty = Math.round(above - th);
            Ui.box(c, Math.round(tx), Math.round(ty), Math.round(tw), Math.round(th), BOX_BG, accent);
            for (int i = 0; i < zh.length; i++) {
                c.text(Ui.fit(c, zh[i], Math.round((tw - 8 * k))), tx + 4 * k,
                       ty + 4 * k + i * line, Ui.TEXT);
            }
            s.mark(Spot.CHOICES, tx, ty, tw, th);
        }
        if (s.dialogueMode == 0 && s.overlays) {
            float tw = Math.min(Math.min(dw * 0.75f, w - 8), c.width(zhLine) + 10 * k);
            List<String> say = wrap(c, zhLine, Math.round((tw - 8 * k)));
            int n = Math.min(3, say.size());
            float th = n * line + 11 * k;
            float tx = Math.round(Math.max(x + 4, Math.min(dx, x + w - 4 - tw)));
            float ty = Math.round(above - th);
            Ui.box(c, Math.round(tx), Math.round(ty), Math.round(tw), Math.round(th), BOX_BG, accent);
            for (int i = 0; i < n; i++) {
                c.text(say.get(i), tx + 4 * k, ty + 4 * k + i * line, Ui.TEXT);
            }
            float barW = tw - 8 * k;
            float lit = s.holdSeconds <= 0 ? barW
                    : Math.max(2, Math.min(barW, barW * s.holdSeconds / 30f));
            c.fill(tx + 4 * k, ty + th - 4 * k, tx + 4 * k + barW, ty + th - 3 * k, Ui.LINE);
            c.fill(tx + 4 * k, ty + th - 4 * k, tx + 4 * k + lit, ty + th - 3 * k, accent);
            s.mark(Spot.DIALOGUE, tx, ty, tw, th);
        }
        if (scene != null) {
            scene.size = 1f;
        }
    }

    public static String fill(String template, String... values) {
        StringBuilder out = new StringBuilder();
        int next = 0;
        int i = 0;
        while (i < template.length()) {
            if (template.startsWith("{~", i)) {
                int end = template.indexOf('}', i);
                if (end > 0) {
                    String inside = template.substring(i + 2, end);
                    int at = -1;
                    if (inside.isEmpty()) {
                        at = next++;
                    } else if (inside.chars().allMatch(Character::isDigit)) {
                        at = Integer.parseInt(inside) - 1;
                    }
                    if (at >= 0) {
                        out.append(at < values.length ? values[at] : "0");
                        i = end + 1;
                        continue;
                    }
                }
            }
            out.append(template.charAt(i));
            i++;
        }
        return out.toString();
    }

    private static void world(Canvas c, int x, int y, int w, int h, State s, int accent,
                              boolean scenery) {
        UnaryOperator<String> zh = s.translate;

        String boss = s.bossbar ? zh.apply(BOSS) : BOSS;
        int barW = Math.min(182, w - 60);
        int bx = x + (w - barW) / 2;
        int nameX = x + (w - c.width(boss)) / 2;
        shadowed(c, boss, nameX, y + 6, 0xFFF0A0A0);
        if (has(c, BOSS_BACK) && has(c, BOSS_FILL)) {
            c.image(BOSS_BACK, bx, y + 16, barW, 5, 0, 0, 182, 5, 0xFFFFFFFF);
            c.image(BOSS_FILL, bx, y + 16, barW * 0.62f, 5, 0, 0, 182 * 0.62f, 5, 0xFFFFFFFF);
        } else {
            c.fill(bx, y + 16, bx + barW, y + 21, 0xFF3A1414);
            c.fill(bx, y + 16, bx + barW * 62 / 100, y + 21, 0xFFC0392B);
        }
        s.mark(Spot.BOSS, Math.min(bx, nameX), y + 5, Math.max(barW, c.width(boss)), 17);

        String t1 = s.tracker == 0 ? zh.apply(TRACK_1) : TRACK_1;
        String t2 = s.tracker == 0 ? zh.apply(TRACK_2) : TRACK_2;
        int tw = Math.min(w / 2 - 6, Math.max(c.width(t1), c.width(t2)) + 8);
        int tx = x + w - tw - 6;
        int ty = y + 30;
        c.fill(tx, ty, tx + tw, ty + 24, 0x70000000);
        shadowed(c, Ui.fit(c, t1, tw - 6), tx + 3, ty + 3, 0xFFF5C56B);
        shadowed(c, Ui.fit(c, t2, tw - 6), tx + 3, ty + 13, s.objectives ? 0xFF8FD694 : 0xFFB8C0CC);
        s.mark(Spot.TRACKER, tx, ty, tw, 24);
        int below = ty + 28;
        if (s.tracker == 1) {
            String p1 = zh.apply(TRACK_1);
            String p2 = zh.apply(TRACK_2);
            int pw = Math.min(w / 2 - 6, Math.max(c.width(p1), c.width(p2)) + 8);
            Ui.box(c, tx + tw - pw, below, pw, 25, BOX_BG, accent);
            c.text(Ui.fit(c, p1, pw - 6), tx + tw - pw + 3, below + 4, 0xFFF5C56B);
            c.text(Ui.fit(c, p2, pw - 6), tx + tw - pw + 3, below + 14, Ui.TEXT);
            s.mark(Spot.TRACKER, tx + tw - pw, below, pw, 25);
            below += 29;
        }

        String[] board = new String[1 + SCORE_LINES.length];
        board[0] = s.scoreboard ? zh.apply(SCORE_HEAD) : SCORE_HEAD;
        int boardW = c.width(board[0]);
        for (int i = 0; i < SCORE_LINES.length; i++) {
            String line = s.scoreboard ? zh.apply(SCORE_LINES[i]) : SCORE_LINES[i];
            board[i + 1] = fill(line, SCORE_VALUES[i]);
            boardW = Math.max(boardW, c.width(board[i + 1]));
        }
        boardW = Math.min(w / 2 + 10, boardW + 6);
        int boardH = 3 + board.length * 10;
        int sx = x + w - boardW - 6;
        int sy = below + 4;
        c.fill(sx, sy, sx + boardW, sy + boardH, 0x60000000);
        for (int i = 0; i < board.length; i++) {
            shadowed(c, Ui.fit(c, board[i], boardW - 6), sx + 3, sy + 2 + i * 10,
                     i == 0 ? 0xFFF5C56B : 0xFFE0E4EA);
        }
        s.mark(Spot.BOARD, sx, sy, boardW, boardH);

        String[] english = {NPC, NPC_ROLE};
        String[] translated = npcLines(zh);
        String[] plate = s.nametag == 1 ? translated : english;
        float k = labelScale(c, Math.max(c.width(plate[0]), c.width(plate[1])), w - 12);
        float line = 10 * k;
        float plateW = Math.max(c.width(plate[0]), c.width(plate[1])) * k;
        float px = Math.round(x + (w - plateW) / 2f);
        float py = Math.round(scenery ? y + h * 0.41f - line * 2 : y + Math.max(64, h / 4f));
        if (px + plateW > sx - 2) {
            py = Math.max(py, sy + boardH + 4);
        }
        int[] colours = {NPC_NAME, NPC_ABOUT};
        for (int i = 0; i < plate.length; i++) {
            float lw = c.width(plate[i]) * k;
            c.text(plate[i], x + (w - lw) / 2f, py + i * line, colours[i], k);
        }
        if (!scenery) {
            float fx = Math.round(x + w / 2f - 8);
            float fy = py + line * 2 + 4;
            c.fill(fx + 2, fy, fx + 14, fy + 12, 0xFFB98B5E);
            c.fill(fx, fy + 12, fx + 16, fy + 30, 0xFF5A3A28);
            c.fill(fx + 2, fy + 30, fx + 14, fy + 45, 0xFF3C4E6C);
        }
        s.mark(Spot.NAMEPLATE, px, py, plateW, line * 2);
        if (s.nametag == 0) {
            int zw = Math.min(w - 8, Math.max(c.width(translated[0]), c.width(translated[1])) + 8);
            int zx = Math.round(Math.max(x + 4, Math.min(x + w - 4 - zw, x + (w - zw) / 2f)));
            int zy = Math.round(py + line * 2 + 4);
            Ui.box(c, zx, zy, zw, 25, BOX_BG, accent);
            c.text(Ui.fit(c, translated[0], zw - 8), zx + 4, zy + 4, NPC_NAME);
            c.text(Ui.fit(c, translated[1], zw - 8), zx + 4, zy + 14, NPC_ABOUT);
            s.mark(Spot.NAMEPLATE, zx, zy, zw, 25);
        }

        float barTop = hotbar(c, x, y, w, h,
                fit(c, (w - SCENE_PAD * 2) / (float) HUD_W));
        String held = s.heldItem ? zh.apply(ITEM) : ITEM;
        int hx = x + (w - c.width(held)) / 2;
        int hy = Math.round(barTop) - 13;
        shadowed(c, held, hx, hy, 0xFFFFFFFF);
        s.mark(Spot.HELD, hx, hy - 1, c.width(held), 10);

        String chatZh = zh.apply(CHAT);
        int cw = Math.min(w - 16, Math.max(150, w * 6 / 10));
        int rows = s.chat == 1 ? 2 : 1;
        int chatY = hy - 8 - rows * 10;
        c.fill(x + 4, chatY - 2, x + 4 + cw, chatY + rows * 10, 0x60000000);
        shadowed(c, Ui.fit(c, s.chat == 0 ? chatZh : CHAT, cw - 6), x + 7, chatY, 0xFFE0E4EA);
        if (s.chat == 1) {
            shadowed(c, Ui.fit(c, chatZh, cw - 6), x + 7, chatY + 10, accent);
        }
        s.mark(Spot.CHAT, x + 4, chatY - 2, cw, rows * 10 + 2);

        String title = s.titles ? zh.apply(TITLE) : TITLE;
        float big = 2f;
        float scale = c.width(title) * big <= w - 24 ? big : 1f;
        float titleW = c.width(title) * scale;
        float titleX = x + (w - titleW) / 2f;
        float titleY = Math.max(py + line * 2 + 4, Math.min(chatY - 14 - 8 * scale, y + h * 0.56f));
        c.text(title, titleX, titleY, TITLE_GOLD, scale);
        s.mark(Spot.TITLE, titleX, titleY - 1, titleW, 8 * scale + 2);
    }

    private static String[] npcLines(UnaryOperator<String> zh) {
        String block = zh.apply(NPC_LABEL);
        if (!block.equals(NPC_LABEL)) {
            String[] parts = block.split("\n");
            if (parts.length == 3) {
                return new String[] {parts[1], parts[2]};
            }
        }
        return new String[] {zh.apply(NPC), zh.apply(NPC_ROLE)};
    }

    static float labelScale(Canvas c, float width, float room) {
        float px = c.px();
        for (int n = (int) Math.floor(1.5f / px + 0.001f); n >= 1; n--) {
            if (width * n * px <= room) {
                return n * px;
            }
        }
        return px;
    }

    private static void data(Canvas c, int x, int y, int w, int h, State s, int accent) {
        int px = x + 10;
        int py = y + 12;
        String big = String.format("%,d", s.loaded);
        float scale = c.scale(2f);
        c.text(big, px, py, accent, scale);
        py += Math.round(8 * scale) + 4;
        c.text(Ui.fit(c, s.loadedLabel, w - 20), px, py, Ui.HINT);

        int row = py + 18;
        for (int i = 0; i < s.facts.length && row + 20 < y + h; i++) {
            String[] fact = s.facts[i];
            c.text(Ui.fit(c, fact[0], w - 20), px, row, Ui.HINT);
            boolean version = fact.length > 2;
            c.text(Ui.fit(c, fact[1], w - 20), px, row + 10,
                   version ? s.versionColour : Ui.TEXT);
            row += 25;
        }
    }
}
