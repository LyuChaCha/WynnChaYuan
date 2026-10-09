package com.wynnchayuan.client.ui;

import java.util.function.UnaryOperator;

/**
 * 設定畫面右邊的即時預覽。
 *
 * <h2>為什麼要有</h2>
 * 「面板間距 12」是多寬、「就地取代」跟「另開面板」差在哪——舊畫面只能調完、關掉
 * F6、找一件物品指上去才知道，不對再開回來。這裡把遊戲畫面縮成一張示意圖，
 * 左邊每動一下就跟著變：拖間距滑桿時兩個框即時拉開，換顏色時框線即時換色。
 *
 * <h2>為什麼是示意而不是真的畫面</h2>
 * 設定畫面開著的時候遊戲的 tooltip 與對話框都不會出現，沒有「真的那一個」可以畫。
 * 所以每一類各畫一張小場景，位置關係照遊戲裡的擺：tooltip 在滑鼠旁邊、對話框在
 * 底下、Boss 血條在頂端、任務追蹤在右上。
 *
 * <p>場景裡的字是真的語料：英文是遊戲原文，譯文是<b>現在這個譯文語言</b>查出來的
 * （查表由呼叫端給，見 {@link State#translate}），所以換語言之後預覽也跟著換。
 */
public final class Preview {

    private Preview() {}

    /**
     * {@code PANEL} 跟 {@code TOOLTIP} 是同一張圖，差在<b>一定畫出面板</b>：「面板」那一頁
     * 調的是面板放哪裡，物品翻譯設成就地取代或關閉的時候面板不會出現，預覽就整張不動
     * （使用者 2026-10-08 回報「不管用哪種模式都沒有動作」）。所以那一頁照樣把面板畫出來，
     * 底下再講一聲現在的設定下它不會出現。
     */
    public enum Scene { TOOLTIP, PANEL, DIALOGUE, WORLD, DATA }

    /**
     * 預覽要看的值。每一幀由 {@code SettingsScreen} 從設定填一次。
     *
     * <p>三段的模式用整數而不是設定檔的列舉——這一層不認得 {@code CollectorConfig}。
     * 每個欄位旁邊寫了哪個數字是哪一種。
     */
    public static final class State {
        /** 0 另開面板、1 就地取代、2 關閉。 */
        public int tooltipMode;
        /** 0 譯名、1 譯名加原文、2 原文。 */
        public int names = 2;
        public boolean anchorFixed;
        /** 0 自動、1 右側、2 左側。 */
        public int side;
        public int gap = 12;
        /** 0 另開小框、1 就地取代、2 關閉。 */
        public int dialogueMode = 1;
        public int choiceMode = 1;
        /** 0 是持續顯示。 */
        public int holdSeconds = 6;
        public boolean overlays = true;
        /** 0 注視時小框、1 就地取代、2 關閉。 */
        public int nametag = 1;
        /** 0 就地取代、1 原文加譯文、2 關閉。 */
        public int chat = 1;
        public boolean titles = true;
        public boolean bossbar = true;
        /** 0 就地取代、1 另開面板、2 關閉。 */
        public int tracker;
        public boolean objectives = true;
        /** 畫面右邊那一欄（記分板）翻不翻。 */
        public boolean scoreboard = true;
        public boolean heldItem = true;

        public int loaded;
        public String loadedLabel = "";
        public String[][] facts = new String[0][];
        public int versionColour = Ui.TEXT;
        /** 物品翻譯不是「另開面板」時，面板那一頁預覽底下的那句提醒。 */
        public String panelOffNote = "";

        /** 原文 → 現在這個語言的譯文；查不到就原樣回來。 */
        public UnaryOperator<String> translate = s -> s;
    }

    // 場景裡的原文。都是語料裡六個語言都翻了的短字串（2026-10-07 查過）。
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
    // 記分板：每日目標那一段。標題一行、底下兩行進度，數字是佔位符，
    // 照語料的鍵查了再把數字填回去（見 #fill）。
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

    /**
     * @param frame    框線顏色：遊戲裡譯文小框的框，場景裡的小框都用它
     * @param accent   風格顏色：只有「資料」那一頁的大數字用，那不是遊戲畫面
     * @param gapShown 動畫中的間距（像素，已經緩動過）——直接拿設定值的話滑桿
     *                 用鍵盤一格一格跳的時候框會跟著跳
     */
    public static void draw(Canvas c, int x, int y, int w, int h, Scene scene, State s,
                            int frame, int accent, float gapShown) {
        Ui.pane(c, x, y, w, h, Ui.R2, Ui.SCENE, Ui.BORDER);
        c.clip(x + 2, y + 2, x + w - 2, y + h - 2);
        // 很淡的格線：讓「這是一張示意圖」一眼看得出來，也給間距一個參照
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
            default -> data(c, x, y, w, h, s, accent);
        }
        c.unclip();
    }

    // ------------------------------------------------------------ 物品與面板

    /**
     * 把譯名後面附的原文拿掉。
     *
     * @param appendedAt 語料認出來的「 (原文)」從哪裡開始；認不出來是 -1
     */
    public static String bareName(String translated, String original, int appendedAt) {
        if (appendedAt > 0) {
            return translated.substring(0, appendedAt);
        }
        // 語料認不出來的（它只認裝備名）也照字面收一次：結尾剛好是「 (原文)」
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
            // 固定位置：面板釘在角落，不跟著 tooltip 走
            int tx = x + (w - tipW) / 2 + 12;
            lines(c, tx, top + 14, tipW, tip, tipColours, TIP_BG, TIP_EDGE);
            lines(c, x + 6, y + 6, sideW, side, tipColours, BOX_BG, accent);
            Ui.glyph(c, x + sideW - 2, y + 2, Ui.ICON_PIN, Ui.GOLD);
            cursor(c, tx - 6, top + 10);
            return;
        }
        // 間距照比例縮：200 像素的上限要塞得進這張小圖
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

        // 兩個框之間的量尺與數字
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

    /** 一個小框，裡面幾行字；放不下的行截斷。 */
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

    /** 滑鼠游標的示意：tooltip 是跟著它出現的。 */
    private static void cursor(Canvas c, int x, int y) {
        String[] arrow = {"#....", "##...", "###..", "####.", "#####", "##...", "#.#.."};
        Ui.glyph(c, x + 1, y + 1, arrow, 0xA0000000);
        Ui.glyph(c, x, y, arrow, 0xFFFFFFFF);
    }

    // ------------------------------------------------------------ 對話

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

        // 另開小框：只有小框的總開關開著才會出現
        int above = dlgY - 6;
        if (s.dialogueMode == 0 && s.overlays) {
            int tw = Math.min(bw - 30, c.width(zhLine) + 10);
            int th = 26;
            int ty = above - th;
            Ui.box(c, bx, ty, tw, th, BOX_BG, accent);
            c.text(Ui.fit(c, zhLine, tw - 8), bx + 4, ty + 4, Ui.TEXT);
            // 停留時間：一條會走完的線。持續顯示就整條亮著
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

    // ------------------------------------------------------------ 世界與聊天

    /**
     * 把數字填回模板的佔位符。
     *
     * <p>語料的鍵是「{@code - Mobs slain: {~}/{~}}」，譯文可能照原順序寫 {@code {~}}，
     * 也可能因為語序不同寫成 {@code {~1}}、{@code {~2}}（見「數字會接錯欄位」那條規矩）。
     * 兩種都認：有編號的照編號，沒編號的照出現的順序。
     */
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

        // 頂端：Boss 血條
        String boss = s.bossbar ? zh.apply(BOSS) : BOSS;
        int barW = Math.min(90, w - 40);
        int bx = x + (w - barW) / 2;
        c.text(boss, x + (w - c.width(boss)) / 2, y + 5, 0xFFF0A0A0);
        c.fill(bx, y + 15, bx + barW, y + 18, 0xFF3A1414);
        c.fill(bx, y + 15, bx + barW * 62 / 100, y + 18, 0xFFC0392B);

        // 右上：任務追蹤。另開面板時多一圈主題色的框
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

        // 左邊：NPC 與名牌
        String tag = s.nametag == 1 ? zh.apply(NPC) : NPC;
        // 放在追蹤欄底下：俄文、西文的名牌很長，並排會蓋到右上那一塊
        int nx = x + 10;
        int ny = y + 74;
        int tagW = c.width(tag) + 6;
        c.fill(nx, ny, nx + tagW, ny + 11, 0x70000000);
        c.text(tag, nx + 3, ny + 2, 0xFFFFFFFF);
        int px = nx + Math.max(0, (tagW - 14) / 2);
        c.fill(px, ny + 14, px + 14, ny + 28, 0xFFB98B5E);
        c.fill(px + 2, ny + 28, px + 12, ny + 44, 0xFF4C6A8C);
        if (s.nametag == 0) {
            // 注視時小框：名牌留原文，旁邊跳一個小框
            String z = zh.apply(NPC);
            int zw = c.width(z) + 8;
            Ui.box(c, nx + tagW + 3, ny - 2, zw, 14, BOX_BG, accent);
            c.text(z, nx + tagW + 7, ny + 1, Ui.TEXT);
        }

        // 中央大字
        String title = s.titles ? zh.apply(TITLE) : TITLE;
        int cx = x + (w - c.width(title)) / 2;
        int cy = Math.max(ny + 52, y + h / 2 + 14);
        c.text(title, cx + 1, cy + 1, 0xA0000000);
        c.text(title, cx, cy, 0xFFF5C56B);

        // 左下：聊天
        String chatZh = zh.apply(CHAT);
        int cw = Math.min(w - 16, 150);
        int rows = s.chat == 1 ? 2 : 1;
        int chatY = y + h - 30 - rows * 10;
        c.fill(x + 4, chatY - 2, x + 4 + cw, chatY + rows * 10, 0x60000000);
        c.text(Ui.fit(c, s.chat == 0 ? chatZh : CHAT, cw - 6), x + 7, chatY, 0xFFB8C0CC);
        if (s.chat == 1) {
            c.text(Ui.fit(c, chatZh, cw - 6), x + 7, chatY + 10, accent);
        }

        // 右邊：記分板。遊戲裡它貼著畫面右緣、在畫面中段。
        // 平常放在中央大字底下、聊天上面那一段：整個寬度都能用，俄文、西文的
        // 長句子不用截。視窗矮到那一段放不下時，退到 NPC 旁邊（名牌那一列的
        // 下面一列），寬度讓開 NPC 的身體。
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

        // 底部：快捷列與手上那件的名字
        String held = s.heldItem ? zh.apply(ITEM) : ITEM;
        int hx = x + (w - 4 * 12) / 2;
        int hy = y + h - 14;
        c.text(held, x + (w - c.width(held)) / 2, hy - 11, 0xFFFFFFFF);
        for (int i = 0; i < 4; i++) {
            Ui.box(c, hx + i * 12, hy, 11, 11, 0x80000000, i == 0 ? 0xFFE6EDF5 : 0xFF55606A);
        }
    }

    // ------------------------------------------------------------ 資料

    private static void data(Canvas c, int x, int y, int w, int h, State s, int accent) {
        int px = x + 10;
        int py = y + 12;
        String big = String.format("%,d", s.loaded);
        float scale = c.scale(2f);
        c.text(big, px, py, accent, scale);
        py += Math.round(8 * scale) + 4;
        c.text(Ui.fit(c, s.loadedLabel, w - 20), px, py, Ui.HINT);

        // 名稱一行、值一行：俄文、西文的名稱很長，並排的話值只剩幾個字的位置
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
