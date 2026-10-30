package com.wynnchayuan.client.ui;

import java.util.function.UnaryOperator;

public final class Preview {

    private Preview() {}

    public enum Scene {
        TOOLTIP, PANEL, DIALOGUE, WORLD, DATA,
        CAPE
    }

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
    }

    private static final String ITEM = "Bony Bow";
    private static final String[] ITEM_LINES = {"Durability", "Health", "Mana"};
    private static final String SPEAKER = "Aledar";
    private static final String LINE = "Well, what are we waiting for?";
    private static final String[] CHOICES = {"Yes", "Goodbye."};
    private static final String BOSS = "Zombie";
    private static final String NPC = "Weapon Merchant";
    private static final String TITLE = "Level Up!";
    private static final String TRACK_1 = "World Event";
    private static final String TRACK_2 = "Click To Track";
    private static final String CHAT = "The trade has been completed!";
    private static final String SCORE_HEAD = "Daily Objective:";
    private static final String[] SCORE_LINES = {
        "- Loot Chests T{~}+: {~}/{~}", "- Mobs slain: {~}/{~}"};
    private static final String[][] SCORE_VALUES = {{"2", "3", "5"}, {"42", "100"}};

    private static final int TIP_BG = 0xF0160A22;
    private static final int TIP_EDGE = 0xFF4B2A8A;
    private static final int BOX_BG = 0xF00A1018;
    private static final int DLG_BG = 0xF022180E;
    private static final int DLG_EDGE = 0xFF7A5C2E;
    private static final int DLG_TEXT = 0xFFF1E3C4;

    public static void draw(Canvas c, int x, int y, int w, int h, Scene scene, State s,
                            int frame, int accent, float gapShown) {
        Ui.pane(c, x, y, w, h, Ui.R2, Ui.SCENE, Ui.BORDER);
        c.clip(x + 2, y + 2, x + w - 2, y + h - 2);
        for (int gx = x + 16; gx < x + w - 1; gx += 16) {
            c.fill(gx, y + 1, gx + 1, y + h - 1, 0x09FFFFFF);
        }
        for (int gy = y + 16; gy < y + h - 1; gy += 16) {
            c.fill(x + 1, gy, x + w - 1, gy + 1, 0x09FFFFFF);
        }
        switch (scene) {
            case TOOLTIP -> tooltip(c, x, y, w, h, s, frame, gapShown, false);
            case PANEL -> tooltip(c, x, y, w, h, s, frame, gapShown, true);
            case DIALOGUE -> dialogue(c, x, y, w, h, s, frame);
            case WORLD -> world(c, x, y, w, h, s, frame);
            case CAPE -> { }
            default -> data(c, x, y, w, h, s, accent);
        }
        c.unclip();
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
        String zhName = s.translate.apply(ITEM);
        String name = s.names == 0 ? zhName
                : s.names == 1 ? zhName + " (" + ITEM + ")" : ITEM;
        boolean replace = s.tooltipMode == 1 && !forcePanel;
        boolean panel = s.tooltipMode == 0 || forcePanel;
        if (forcePanel && s.tooltipMode != 0 && !s.panelOffNote.isEmpty()) {
            java.util.List<String> note = Ui.wrap(c, s.panelOffNote, w - 16);
            int ny = y + h - 6 - note.size() * 10;
            c.fill(x + 2, ny - 5, x + w - 2, y + h - 2, 0xC0101822);
            for (String line : note) {
                c.text(line, x + 8, ny, Ui.AMBER);
                ny += 10;
            }
        }

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

        int room = w - 12;
        int tipW = Math.min(widest(c, tip) + 10, panel ? room * 48 / 100 : room);
        int sideW = panel ? Math.min(widest(c, side) + 10, room * 48 / 100) : 0;
        int boxH = tip.length * 10 + 7;
        int top = y + Math.max(20, (h - boxH) / 2 - 4);

        if (panel && s.anchorFixed) {
            int tx = x + (w - tipW) / 2 + 12;
            lines(c, tx, top + 14, tipW, tip, tipColours, TIP_BG, TIP_EDGE);
            lines(c, x + 6, y + 6, sideW, side, tipColours, BOX_BG, accent);
            Ui.glyph(c, x + sideW - 2, y + 2, Ui.ICON_PIN, Ui.GOLD);
            cursor(c, tx - 6, top + 10);
            return;
        }
        int budget = Math.max(0, room - tipW - sideW);
        int gap = panel ? Math.min(Math.round(gapShown * 0.3f), budget) : 0;
        int total = tipW + (panel ? gap + sideW : 0);
        int left = x + (w - total) / 2;
        boolean leftSide = s.side == 2;
        int tipX = panel && leftSide ? left + sideW + gap : left;
        int sideX = leftSide ? left : left + tipW + gap;

        lines(c, tipX, top, tipW, tip, tipColours, TIP_BG, TIP_EDGE);
        cursor(c, tipX - 5, top - 5);
        if (!panel) {
            return;
        }
        lines(c, sideX, top, sideW, side, tipColours, BOX_BG, accent);
        Ui.glow(c, sideX, top, sideW, boxH, Ui.alpha(accent, 0x40));

        int a = Math.min(tipX + tipW, sideX + sideW);
        int b = Math.max(tipX, sideX);
        int ry = top - 8;
        c.fill(a, ry, a + 1, ry + 5, accent);
        c.fill(Math.max(a, b - 1), ry, Math.max(a + 1, b), ry + 5, accent);
        if (b - a > 2) {
            c.fill(a + 1, ry + 2, b - 1, ry + 3, accent);
        }
        String label = Math.round(gapShown) + " px";
        c.text(label, (a + b - c.width(label)) / 2, ry - 10, accent);
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

    private static void dialogue(Canvas c, int x, int y, int w, int h, State s, int accent) {
        String zhLine = s.translate.apply(LINE);
        boolean lineZh = s.dialogueMode == 1;
        boolean choiceZh = s.choiceMode == 1;
        int bx = x + 8;
        int bw = w - 16;
        int dlgH = 12 + 10 + CHOICES.length * 10 + 6;
        int dlgY = y + h - dlgH - 8;

        Ui.box(c, bx, dlgY, bw, dlgH, DLG_BG, DLG_EDGE);
        c.text(SPEAKER, bx + 5, dlgY + 4, Ui.GOLD);
        c.text(Ui.fit(c, lineZh ? zhLine : LINE, bw - 10), bx + 5, dlgY + 14, DLG_TEXT);
        for (int i = 0; i < CHOICES.length; i++) {
            String text = "[" + (i + 1) + "] "
                    + (choiceZh ? s.translate.apply(CHOICES[i]) : CHOICES[i]);
            c.text(Ui.fit(c, text, bw - 10), bx + 5, dlgY + 26 + i * 10, 0xFFC9B38A);
        }

        int above = dlgY - 6;
        if (s.dialogueMode == 0 && s.overlays) {
            int tw = Math.min(bw - 30, c.width(zhLine) + 10);
            int th = 26;
            int ty = above - th;
            Ui.box(c, bx, ty, tw, th, BOX_BG, accent);
            c.text(Ui.fit(c, zhLine, tw - 8), bx + 4, ty + 4, Ui.TEXT);
            int barW = tw - 8;
            int lit = s.holdSeconds <= 0 ? barW
                    : Math.max(2, Math.min(barW, barW * s.holdSeconds / 30));
            c.fill(bx + 4, ty + 17, bx + 4 + barW, ty + 18, Ui.LINE);
            c.fill(bx + 4, ty + 17, bx + 4 + lit, ty + 18, accent);
            above = ty - 4;
        }
        if (s.choiceMode == 0 && s.overlays) {
            String[] zh = new String[CHOICES.length];
            int tw = 0;
            for (int i = 0; i < zh.length; i++) {
                zh[i] = "[" + (i + 1) + "] " + s.translate.apply(CHOICES[i]);
                tw = Math.max(tw, c.width(zh[i]));
            }
            tw = Math.min(bw - 20, tw + 10);
            int th = zh.length * 10 + 7;
            int tx = bx + bw - tw;
            int ty = above - th;
            Ui.box(c, tx, ty, tw, th, BOX_BG, accent);
            for (int i = 0; i < zh.length; i++) {
                c.text(Ui.fit(c, zh[i], tw - 8), tx + 4, ty + 4 + i * 10, Ui.TEXT);
            }
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

    private static void world(Canvas c, int x, int y, int w, int h, State s, int accent) {
        UnaryOperator<String> zh = s.translate;

        String boss = s.bossbar ? zh.apply(BOSS) : BOSS;
        int barW = Math.min(90, w - 40);
        int bx = x + (w - barW) / 2;
        c.text(boss, x + (w - c.width(boss)) / 2, y + 5, 0xFFF0A0A0);
        c.fill(bx, y + 15, bx + barW, y + 18, 0xFF3A1414);
        c.fill(bx, y + 15, bx + barW * 62 / 100, y + 18, 0xFFC0392B);

        String t1 = s.tracker == 0 ? zh.apply(TRACK_1) : TRACK_1;
        String t2 = s.tracker == 0 ? zh.apply(TRACK_2) : TRACK_2;
        int tw = Math.min(w / 2 - 4, Math.max(c.width(t1), c.width(t2)) + 8);
        int tx = x + w - tw - 5;
        int ty = y + 26;
        Ui.box(c, tx, ty, tw, 26, 0xB0080C12, s.tracker == 1 ? accent : 0x20FFFFFF);
        c.text(Ui.fit(c, t1, tw - 6), tx + 3, ty + 4, 0xFFF5C56B);
        c.text(Ui.fit(c, t2, tw - 6), tx + 3, ty + 14, s.objectives ? 0xFF8FD694 : 0xFFB8C0CC);
        if (s.tracker == 1) {
            String p1 = zh.apply(TRACK_1);
            int pw = Math.min(w / 2 - 4, c.width(p1) + 8);
            Ui.box(c, tx + tw - pw, ty + 28, pw, 14, BOX_BG, accent);
            c.text(Ui.fit(c, p1, pw - 6), tx + tw - pw + 3, ty + 31, Ui.TEXT);
        }

        String tag = s.nametag == 1 ? zh.apply(NPC) : NPC;
        int nx = x + 10;
        int ny = y + 74;
        int tagW = c.width(tag) + 6;
        c.fill(nx, ny, nx + tagW, ny + 11, 0x70000000);
        c.text(tag, nx + 3, ny + 2, 0xFFFFFFFF);
        int px = nx + Math.max(0, (tagW - 14) / 2);
        c.fill(px, ny + 14, px + 14, ny + 28, 0xFFB98B5E);
        c.fill(px + 2, ny + 28, px + 12, ny + 44, 0xFF4C6A8C);
        if (s.nametag == 0) {
            String z = zh.apply(NPC);
            int zw = c.width(z) + 8;
            Ui.box(c, nx + tagW + 3, ny - 2, zw, 14, BOX_BG, accent);
            c.text(z, nx + tagW + 7, ny + 1, Ui.TEXT);
        }

        String title = s.titles ? zh.apply(TITLE) : TITLE;
        int cx = x + (w - c.width(title)) / 2;
        int cy = Math.max(ny + 52, y + h / 2 + 14);
        c.text(title, cx + 1, cy + 1, 0xA0000000);
        c.text(title, cx, cy, 0xFFF5C56B);

        String chatZh = zh.apply(CHAT);
        int cw = Math.min(w - 16, 150);
        int rows = s.chat == 1 ? 2 : 1;
        int chatY = y + h - 30 - rows * 10;
        c.fill(x + 4, chatY - 2, x + 4 + cw, chatY + rows * 10, 0x60000000);
        c.text(Ui.fit(c, s.chat == 0 ? chatZh : CHAT, cw - 6), x + 7, chatY, 0xFFB8C0CC);
        if (s.chat == 1) {
            c.text(Ui.fit(c, chatZh, cw - 6), x + 7, chatY + 10, accent);
        }

        String[] board = new String[1 + SCORE_LINES.length];
        board[0] = s.scoreboard ? zh.apply(SCORE_HEAD) : SCORE_HEAD;
        int boardW = c.width(board[0]);
        for (int i = 0; i < SCORE_LINES.length; i++) {
            String line = s.scoreboard ? zh.apply(SCORE_LINES[i]) : SCORE_LINES[i];
            board[i + 1] = fill(line, SCORE_VALUES[i]);
            boardW = Math.max(boardW, c.width(board[i + 1]));
        }
        int boardH = 3 + board.length * 9;
        int sy = cy + 13;
        int room = w - 10;
        if (sy + boardH > chatY - 4) {
            sy = ny + 15;
            room = x + w - 5 - (px + 14 + 4);
        }
        boardW = Math.min(room, boardW + 6);
        int sx = x + w - boardW - 5;
        c.fill(sx, sy, sx + boardW, sy + boardH, 0x60000000);
        for (int i = 0; i < board.length; i++) {
            c.text(Ui.fit(c, board[i], boardW - 6), sx + 3, sy + 2 + i * 9,
                    i == 0 ? 0xFFF5C56B : 0xFFB8C0CC);
        }

        String held = s.heldItem ? zh.apply(ITEM) : ITEM;
        int hx = x + (w - 4 * 12) / 2;
        int hy = y + h - 14;
        c.text(held, x + (w - c.width(held)) / 2, hy - 11, 0xFFFFFFFF);
        for (int i = 0; i < 4; i++) {
            Ui.box(c, hx + i * 12, hy, 11, 11, 0x80000000, i == 0 ? 0xFFE6EDF5 : 0xFF55606A);
        }
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
