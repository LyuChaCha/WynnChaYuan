package com.wynnchayuan.client.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * F6 設定畫面的版面、互動與動畫。
 *
 * <h2>版面</h2>
 * 畫面中央一張浮起來的視窗，由上到下：
 * <pre>
 *   標題列   圖示、名稱與版本 ……………… 搜尋框  使用須知
 *   分類列   物品  面板  對話  世界與聊天  資料 ………… 重置本頁
 *   內容     左：這一類的設定（卡片分組，可捲動）   右：即時預覽
 *   底列     狀態 …………………… 更新說明  關於／貢獻者  完成
 * </pre>
 * 視窗太窄時右邊的預覽先收起來，再窄分類列的圖示也收起來——設定本身永遠放得下。
 *
 * <h2>為什麼是「邊畫邊登記可以點的地方」</h2>
 * 舊畫面漂掉的根源是按鈕的位置與畫出來的位置各算各的。這裡每畫一個控制項就
 * 順手登記一塊「點這裡會怎樣」（{@link #zone}），用的是<b>同一組座標</b>：
 * 畫在哪就點得到哪，結構上不可能對不上。點的時候從最後登記的往回找，所以後畫的
 * （彈出的色盤、下拉選單）自然蓋在先畫的上面。
 *
 * <h2>這一層不認得遊戲</h2>
 * 只碰 {@link Canvas} 與 {@link Host}。測試接一張假畫布就能整個跑起來，
 * 見 {@code SettingsViewTest}。
 */
public final class SettingsView {

    /** 這個畫面需要外面幫忙的事。 */
    public interface Host {
        /** 主題色（ARGB）。 */
        int accent();

        /** 介面字串。鍵不含 {@code wynnchayuan.} 前綴。 */
        String tr(String key, Object... args);

        String title();

        String subtitle();

        /** 底列現在要講的那一句與它的顏色。 */
        Status status();

        /** 有新版的模組：更新說明那顆要亮起來。 */
        boolean hasUpdate();

        void openNotice();

        void openUpdates();

        void openCredits();

        void done();

        String clipboard();

        void setClipboard(String text);

        /** 按下去的那一聲。 */
        void click();

        Preview.State preview();

        /** 重置之後要在底列講一聲。 */
        void resetDone(String what, int count);
    }

    public record Status(String text, int colour) {}

    // ------------------------------------------------------------ 版面常數

    static final int PAD = 8;
    static final int HEADER_H = 30;
    static final int TABS_H = 20;
    static final int FOOT_H = 26;
    static final int ROW_H = 22;
    static final int GROUP_HEAD = 14;
    static final int CTRL_H = 14;
    /** 名稱至少留這麼寬，控制項再怎麼長也不能把它擠沒。 */
    static final int MIN_LABEL = 56;
    /** 視窗比這個寬才放得下預覽。 */
    static final int PREVIEW_AT = 500;

    private static final long TIP_DELAY_MS = 380;

    // ------------------------------------------------------------ 狀態

    private final List<Row.Tab> tabs;
    private final Host host;

    private int tab;
    private float scroll;
    private float scrollTarget;
    private int maxScroll;

    private final Field search = new Field(40);
    private final Field hexField = new Field(7);
    private Field focus;

    private String openSelect;
    private boolean pickerOpen;
    private Row pickerRow;
    private final float[] hsv = {210, 0.5f, 0.85f};

    private String hoverRow;
    private String hoverSeen;
    private long hoverSince;
    private Zone dragging;

    private final Map<String, Float> anim = new HashMap<>();
    private float barX = -1;
    private float barW;
    private float bodyT = 1f;
    private float gapShown = -1;
    private long lastFrame;

    // 這一幀的東西
    private Canvas c;
    private int accent;
    private int mx;
    private int my;
    private long now;
    private float dt;
    private final List<Zone> zones = new ArrayList<>();
    private final List<int[]> clips = new ArrayList<>();
    private int winX;
    private int winY;
    private int winW;
    private int winH;
    private Runnable popover;
    private Tip tip;

    /** 給測試看的：每一列實際畫在哪、控制項佔到哪。 */
    public record Placed(String id, int x, int y, int w, int h, int ctrlLeft, int labelRight) {}

    private final List<Placed> placed = new ArrayList<>();

    public SettingsView(List<Row.Tab> tabs, Host host, int startTab) {
        this.tabs = tabs;
        this.host = host;
        this.tab = Math.max(0, Math.min(tabs.size() - 1, startTab));
    }

    public int tab() {
        return tab;
    }

    public String searchText() {
        return search.text.toString();
    }

    public List<Placed> placed() {
        return placed;
    }

    public int[] window() {
        return new int[] {winX, winY, winW, winH};
    }

    public boolean popoverOpen() {
        return pickerOpen || openSelect != null;
    }

    // ------------------------------------------------------------ 可以點的地方

    private interface Press {
        void at(double x, double y, int button);
    }

    private static final class Zone {
        int x;
        int y;
        int w;
        int h;
        Press press;
        /** 按住拖曳時每動一下叫一次；{@code null} 就是普通的按鈕。 */
        Press drag;
        Runnable release;
    }

    private Zone zone(int x, int y, int w, int h, Press press) {
        Zone z = new Zone();
        int x0 = x;
        int y0 = y;
        int x1 = x + w;
        int y1 = y + h;
        // 捲出可視範圍的那一截不能點——畫的時候被裁掉了，點得到就是鬼按鈕
        for (int[] clip : clips) {
            x0 = Math.max(x0, clip[0]);
            y0 = Math.max(y0, clip[1]);
            x1 = Math.min(x1, clip[2]);
            y1 = Math.min(y1, clip[3]);
        }
        z.x = x0;
        z.y = y0;
        z.w = Math.max(0, x1 - x0);
        z.h = Math.max(0, y1 - y0);
        z.press = press;
        zones.add(z);
        return z;
    }

    private void clip(int x0, int y0, int x1, int y1) {
        c.clip(x0, y0, x1, y1);
        clips.add(new int[] {x0, y0, x1, y1});
    }

    private void unclip() {
        c.unclip();
        clips.remove(clips.size() - 1);
    }

    /** 滑鼠在不在這塊上面（而且沒有被彈出層蓋住、也沒有正在拖別的東西）。 */
    private boolean over(int x, int y, int w, int h) {
        if (dragging != null) {
            return false;
        }
        for (int[] clip : clips) {
            if (mx < clip[0] || mx >= clip[2] || my < clip[1] || my >= clip[3]) {
                return false;
            }
        }
        return Ui.in(mx, my, x, y, w, h);
    }

    // ------------------------------------------------------------ 輸入

    public boolean mouseDown(double x, double y, int button) {
        for (int i = zones.size() - 1; i >= 0; i--) {
            Zone z = zones.get(i);
            if (z.w > 0 && z.h > 0 && Ui.in(x, y, z.x, z.y, z.w, z.h)) {
                if (z.drag != null) {
                    dragging = z;
                }
                z.press.at(x, y, button);
                return true;
            }
        }
        return false;
    }

    public boolean mouseDrag(double x, double y) {
        if (dragging == null) {
            return false;
        }
        dragging.drag.at(x, y, 0);
        return true;
    }

    public boolean mouseUp() {
        if (dragging == null) {
            return false;
        }
        Zone z = dragging;
        dragging = null;
        if (z.release != null) {
            z.release.run();
        }
        return true;
    }

    public boolean scroll(double dy) {
        if (popoverOpen()) {
            return true;
        }
        scrollTarget = clampScroll(scrollTarget - (float) dy * ROW_H * 1.5f);
        return true;
    }

    private float clampScroll(float v) {
        return Math.max(0, Math.min(maxScroll, v));
    }

    public static final int KEY_ESC = 256;
    public static final int KEY_ENTER = 257;
    public static final int KEY_BACKSPACE = 259;
    public static final int KEY_DELETE = 261;
    public static final int KEY_RIGHT = 262;
    public static final int KEY_LEFT = 263;
    public static final int KEY_DOWN = 264;
    public static final int KEY_UP = 265;
    public static final int KEY_PAGE_UP = 266;
    public static final int KEY_PAGE_DOWN = 267;
    public static final int KEY_HOME = 268;
    public static final int KEY_END = 269;
    public static final int KEY_KP_ENTER = 335;

    /**
     * @param ctrl Ctrl（mac 上是 Cmd）有沒有按著
     * @return 這個鍵有沒有被這裡用掉。Esc 沒東西可關的時候回 {@code false}，
     *         外面就照原版的做法關掉畫面
     */
    public boolean key(int key, boolean ctrl) {
        if (key == KEY_ESC) {
            if (popoverOpen()) {
                closePopovers();
                return true;
            }
            if (focus != null || search.text.length() > 0) {
                boolean had = search.text.length() > 0;
                focus = null;
                if (had) {
                    search.clear();
                    onSearchChanged();
                }
                return true;
            }
            return false;
        }
        if (ctrl && key == 'F') {
            focusSearch();
            return true;
        }
        if (focus != null) {
            if (key == KEY_ENTER || key == KEY_KP_ENTER) {
                if (focus == hexField) {
                    commitHex();
                }
                focus = null;
                return true;
            }
            boolean changed = focus.key(key, ctrl, host);
            if (changed) {
                fieldChanged();
            }
            return true;
        }
        switch (key) {
            case KEY_DOWN -> scrollTarget = clampScroll(scrollTarget + ROW_H);
            case KEY_UP -> scrollTarget = clampScroll(scrollTarget - ROW_H);
            case KEY_PAGE_DOWN -> scrollTarget = clampScroll(scrollTarget + ROW_H * 6);
            case KEY_PAGE_UP -> scrollTarget = clampScroll(scrollTarget - ROW_H * 6);
            case KEY_HOME -> scrollTarget = 0;
            case KEY_END -> scrollTarget = maxScroll;
            default -> {
                if (key >= '1' && key < '1' + tabs.size() && !ctrl) {
                    switchTab(key - '1');
                    return true;
                }
                return false;
            }
        }
        return true;
    }

    public boolean typed(String text) {
        if (focus == null) {
            if ("/".equals(text)) {
                focusSearch();
                return true;
            }
            return false;
        }
        if (focus.type(text)) {
            fieldChanged();
        }
        return true;
    }

    private void focusSearch() {
        closePopovers();
        focus = search;
        search.caret = search.text.length();
    }

    private void fieldChanged() {
        if (focus == search) {
            onSearchChanged();
        } else if (focus == hexField) {
            String text = hexField.text.toString();
            if (text.matches("#?[0-9a-fA-F]{6}") && pickerRow != null
                    && pickerRow.setHex.test(text)) {
                float[] next = Ui.toHsv(Ui.parseHex(text, accent));
                System.arraycopy(next, 0, hsv, 0, 3);
            }
        }
    }

    private void onSearchChanged() {
        scroll = 0;
        scrollTarget = 0;
        hoverRow = null;
    }

    private void commitHex() {
        if (pickerRow != null) {
            pickerRow.commit.run();
            hexField.set(pickerRow.hex.get());
        }
    }

    private void closePopovers() {
        if (pickerOpen && pickerRow != null) {
            pickerRow.commit.run();
        }
        pickerOpen = false;
        pickerRow = null;
        openSelect = null;
        if (focus == hexField) {
            focus = null;
        }
    }

    private void switchTab(int next) {
        if (next == tab && search.text.length() == 0) {
            return;
        }
        closePopovers();
        tab = next;
        search.clear();
        focus = null;
        scroll = 0;
        scrollTarget = 0;
        bodyT = 0f;
        hoverRow = null;
        host.click();
    }

    // ------------------------------------------------------------ 繪製

    public void render(Canvas canvas, int screenW, int screenH, int mouseX, int mouseY,
                       long nowMs) {
        this.c = canvas;
        this.mx = mouseX;
        this.my = mouseY;
        this.now = nowMs;
        this.dt = lastFrame == 0 ? 16f : Math.max(0f, Math.min(100f, nowMs - lastFrame));
        this.lastFrame = nowMs;
        this.accent = 0xFF000000 | (host.accent() & 0xFFFFFF);
        zones.clear();
        clips.clear();
        placed.clear();
        popover = null;
        tip = null;
        String wasHover = hoverRow;
        hoverRow = null;

        winW = Math.min(screenW - 12, 640);
        winH = Math.min(screenH - 10, 350);
        winX = (screenW - winW) / 2;
        winY = (screenH - winH) / 2;

        // 視窗：外面一圈暗邊當陰影，裡面深色底
        Ui.box(c, winX - 2, winY - 2, winW + 4, winH + 4, 0, 0x30000000);
        Ui.box(c, winX - 1, winY - 1, winW + 2, winH + 2, 0, 0x60000000);
        Ui.box(c, winX, winY, winW, winH, Ui.WINDOW, Ui.BORDER);

        // 點到視窗空白處：收起彈出層、放掉輸入框的焦點
        zone(0, 0, screenW, screenH, (x, y, b) -> {
            closePopovers();
            focus = null;
        });

        header();
        tabsBar();
        body();
        footer();

        bodyT = Ui.ease(bodyT, 1f, dt, 70f);
        scroll = Ui.ease(scroll, scrollTarget, dt, 55f);

        if (popover != null) {
            // 彈出層底下墊一塊「點外面就收起來」，而且不讓點擊穿到底下的控制項
            zone(0, 0, screenW, screenH, (x, y, b) -> closePopovers());
            popover.run();
        }

        // 說明：滑鼠在同一列上停一下才出現，不然掃過去整片都在閃
        if (hoverRow == null || !hoverRow.equals(wasHover)) {
            hoverSince = now;
        }
        hoverSeen = hoverRow;
        if (tip != null && !popoverOpen() && dragging == null
                && now - hoverSince >= TIP_DELAY_MS) {
            tooltip(tip, Math.min(1f, (now - hoverSince - TIP_DELAY_MS) / 120f), screenW, screenH);
        }
    }

    // ------------------------------------------------------------ 標題列

    private void header() {
        int x = winX;
        int y = winY;
        c.logo(x + 7, y + 5, 20);
        c.text(host.title(), x + 32, y + 6, accent);

        int right = x + winW - PAD;
        int noticeX = right - 16;
        boolean hot = over(noticeX, y + 7, 16, 16);
        Ui.box(c, noticeX, y + 7, 16, 16, Ui.RAISED, hot ? accent : Ui.BORDER);
        if (hot) {
            Ui.glow(c, noticeX, y + 7, 16, 16, Ui.alpha(accent, 0x50));
            tip = new Tip(host.tr("notice.button"), noticeX, y + 7, 16, 16);
            hoverRow = "#notice";
        }
        Ui.glyph(c, noticeX + 6, y + 11, Ui.ICON_NOTICE, Ui.GOLD);
        zone(noticeX, y + 7, 16, 16, (px, py, b) -> {
            host.click();
            host.openNotice();
        });

        int searchW = Math.max(70, Math.min(150, winW / 4));
        int sx = noticeX - 6 - searchW;
        boolean focused = focus == search;
        boolean active = focused || search.text.length() > 0;
        Ui.box(c, sx, y + 7, searchW, 16, Ui.FIELD, active ? accent : Ui.BORDER);
        if (focused) {
            Ui.glow(c, sx, y + 7, searchW, 16, Ui.alpha(accent, 0x50));
        }
        Ui.glyph(c, sx + 4, y + 12, Ui.ICON_SEARCH, active ? accent : Ui.HINT);
        int textX = sx + 15;
        int textW = searchW - 15 - (search.text.length() > 0 ? 14 : 4);
        if (search.text.length() == 0 && !focused) {
            c.text(Ui.fit(c, host.tr("search.placeholder"), textW), textX, y + 11, Ui.FAINT);
        } else {
            field(search, textX, y + 11, textW, focused);
        }
        zone(sx, y + 7, searchW, 16, (px, py, b) -> {
            closePopovers();
            focus = search;
            search.caret = search.text.length();
        });
        if (search.text.length() > 0) {
            int cx = sx + searchW - 12;
            boolean ch = over(cx - 2, y + 9, 12, 12);
            Ui.glyph(c, cx, y + 12, Ui.ICON_CLOSE, ch ? Ui.TEXT : Ui.HINT);
            zone(cx - 2, y + 9, 12, 12, (px, py, b) -> {
                search.clear();
                onSearchChanged();
            });
        }

        int subRoom = sx - 8 - (x + 32);
        if (subRoom > 60) {
            c.text(Ui.fit(c, host.subtitle(), subRoom), x + 32, y + 17, Ui.HINT);
        }
        c.fill(x + 1, y + HEADER_H - 1, x + winW - 1, y + HEADER_H, Ui.LINE);
    }

    /** 輸入框裡的字與游標；字比框長時讓游標那一段留在看得到的地方。 */
    private void field(Field f, int x, int y, int w, boolean focused) {
        String text = f.text.toString();
        String head = text.substring(0, Math.min(f.caret, text.length()));
        int shift = Math.max(0, c.width(head) - (w - 2));
        clip(x, y - 2, x + w, y + 10);
        if (f.all && text.length() > 0) {
            c.fill(x - shift, y - 1, x - shift + c.width(text), y + 9, Ui.alpha(accent, 0x70));
        }
        c.text(text, x - shift, y, Ui.TEXT);
        if (focused && (now / 500) % 2 == 0) {
            int cx = x - shift + c.width(head);
            c.fill(cx, y - 1, cx + 1, y + 9, Ui.TEXT);
        }
        unclip();
    }

    // ------------------------------------------------------------ 分類列

    private void tabsBar() {
        int y = winY + HEADER_H;
        int x0 = winX + PAD - 2;
        boolean searching = search.text.length() > 0;

        String reset = host.tr("reset.page");
        int resetW = c.width(reset) + 20;
        int avail = winW - PAD * 2 + 4;
        int[] natural = new int[tabs.size()];
        int total = 0;
        for (int i = 0; i < tabs.size(); i++) {
            natural[i] = c.width(tabs.get(i).name().get()) + 26;
            total += natural[i];
        }
        boolean icons = total + resetW + 6 <= avail;
        if (!icons) {
            total -= 12 * tabs.size();
            resetW = 16;                       // 放不下就只留圖示
        }
        float squeeze = Math.min(1f, (avail - resetW - 6) / (float) Math.max(1, total));

        int x = x0;
        int selX = x0;
        int selW = 0;
        for (int i = 0; i < tabs.size(); i++) {
            Row.Tab t = tabs.get(i);
            int w = Math.round((natural[i] - (icons ? 0 : 12)) * squeeze);
            boolean on = i == tab && !searching;
            boolean hot = over(x, y + 1, w, TABS_H - 2);
            if (hot && !on) {
                Ui.box(c, x, y + 2, w, TABS_H - 4, 0x10FFFFFF, 0);
            }
            int colour = on ? accent : (hot ? Ui.TEXT : Ui.TEXT_2);
            String name = t.name().get();
            int textRoom = w - (icons ? 24 : 10);
            String shown = Ui.fit(c, name, textRoom);
            int inner = c.width(shown) + (icons ? 12 : 0);
            int tx = x + (w - inner) / 2;
            if (icons) {
                Ui.glyph(c, tx, y + 6, t.icon(), colour);
                tx += 12;
            }
            c.text(shown, tx, y + 6, colour);
            if (i == tab) {
                selX = x + 5;
                selW = w - 10;
            }
            int index = i;
            zone(x, y, w, TABS_H, (px, py, b) -> switchTab(index));
            x += w;
        }

        // 選到的那一類底下一條會滑過去的主題色
        if (barX < 0) {
            barX = selX;
            barW = selW;
        }
        barX = Ui.ease(barX, selX, dt, 60f);
        barW = Ui.ease(barW, selW, dt, 60f);
        c.fill(winX + 1, y + TABS_H - 1, winX + winW - 1, y + TABS_H, Ui.LINE);
        if (!searching) {
            int bx = Math.round(barX);
            int bw = Math.max(2, Math.round(barW));
            c.fill(bx - 1, y + TABS_H - 3, bx + bw + 1, y + TABS_H, Ui.alpha(accent, 0x38));
            c.fill(bx, y + TABS_H - 2, bx + bw, y + TABS_H, accent);
        }

        // 重置本頁：這一頁有東西不是預設值才按得下去
        int rx = winX + winW - PAD - resetW;
        int dirty = 0;
        for (Row.Group g : tabs.get(tab).groups()) {
            for (Row r : g.rows()) {
                if (r.resettable() && !r.isDefault.getAsBoolean()) {
                    dirty++;
                }
            }
        }
        boolean can = dirty > 0 && !searching;
        boolean hot = can && over(rx, y + 3, resetW, CTRL_H);
        Ui.box(c, rx, y + 3, resetW, CTRL_H, Ui.RAISED, hot ? accent : Ui.BORDER);
        if (hot) {
            Ui.glow(c, rx, y + 3, resetW, CTRL_H, Ui.alpha(accent, 0x50));
        }
        int ink = can ? (hot ? Ui.TEXT : Ui.TEXT_2) : Ui.FAINT;
        Ui.glyph(c, rx + 4, y + 7, Ui.ICON_RESET, ink);
        if (resetW > 16) {
            c.text(reset, rx + 15, y + 6, ink);
        } else if (over(rx, y + 3, resetW, CTRL_H)) {
            tip = new Tip(reset, rx, y + 3, resetW, CTRL_H);
            hoverRow = "#reset";
        }
        int count = dirty;
        zone(rx, y + 3, resetW, CTRL_H, (px, py, b) -> {
            if (!can) {
                return;
            }
            closePopovers();
            for (Row.Group g : tabs.get(tab).groups()) {
                for (Row r : g.rows()) {
                    if (r.resettable() && !r.isDefault.getAsBoolean()) {
                        r.reset.run();
                    }
                }
            }
            host.click();
            host.resetDone(tabs.get(tab).name().get(), count);
        });
    }

    // ------------------------------------------------------------ 內容

    private void body() {
        int top = winY + HEADER_H + TABS_H + PAD;
        int bottom = winY + winH - FOOT_H - PAD;
        int h = bottom - top;
        int left = winX + PAD;
        int full = winW - PAD * 2;
        int previewW = winW >= 600 ? 204 : (winW >= PREVIEW_AT ? 170 : 0);
        if (h < 70) {
            previewW = 0;                      // 矮到這樣預覽也塞不下東西
        }
        int listW = previewW > 0 ? full - previewW - PAD : full;

        list(left, top, listW, h);
        if (previewW > 0) {
            preview(left + listW + PAD, top, previewW, h);
        }
    }

    private void list(int x, int y, int w, int h) {
        String query = search.text.toString().strip().toLowerCase(Locale.ROOT);
        boolean searching = !query.isEmpty();

        // 先量總高度才知道能捲多遠
        int contentH = 0;
        int shown = 0;
        List<Object[]> cards = new ArrayList<>();      // {Group, List<Row>, 分類名}
        for (int t = 0; t < tabs.size(); t++) {
            if (!searching && t != tab) {
                continue;
            }
            for (Row.Group g : tabs.get(t).groups()) {
                List<Row> rows = new ArrayList<>();
                for (Row r : g.rows()) {
                    if (!searching || matches(r, query)) {
                        rows.add(r);
                    }
                }
                if (rows.isEmpty()) {
                    continue;
                }
                shown += rows.size();
                cards.add(new Object[] {g, rows, tabs.get(t).name().get()});
                contentH += GROUP_HEAD + rows.size() * ROW_H + 5 + 6;
            }
        }
        contentH = Math.max(0, contentH - 6);
        maxScroll = Math.max(0, contentH - h);
        scrollTarget = clampScroll(scrollTarget);
        scroll = clampScroll(scroll);
        boolean bar = maxScroll > 0;
        int cardW = bar ? w - 5 : w;

        if (searching && shown == 0) {
            Ui.box(c, x, y, w, 30, Ui.CARD, Ui.LINE);
            String none = host.tr("search.none", search.text.toString().strip());
            c.text(Ui.fit(c, none, w - 16), x + (w - c.width(Ui.fit(c, none, w - 16))) / 2,
                   y + 11, Ui.HINT);
            return;
        }

        clip(x, y, x + w, y + h);
        // 換分類時整塊往上滑進來
        int cy = y - Math.round(scroll) + Math.round((1f - bodyT) * 6f);
        for (Object[] card : cards) {
            Row.Group g = (Row.Group) card[0];
            @SuppressWarnings("unchecked")
            List<Row> rows = (List<Row>) card[1];
            int cardH = GROUP_HEAD + rows.size() * ROW_H + 5;
            if (cy + cardH >= y && cy < y + h) {
                Ui.box(c, x, cy, cardW, cardH, Ui.CARD,
                       g.tool() ? Ui.alpha(accent, 0x70) : Ui.LINE);
                String title = g.title().get();
                String count = String.valueOf(rows.size());
                int titleInk = g.tool() ? accent : Ui.HINT;
                c.text(Ui.fit(c, title, cardW - 30), x + 8, cy + 5, titleInk);
                int lineL = x + 8 + c.width(Ui.fit(c, title, cardW - 30)) + 5;
                int lineR = x + cardW - 10 - c.width(count);
                if (lineR > lineL) {
                    c.fill(lineL, cy + 8, lineR - 3, cy + 9, Ui.LINE);
                }
                c.text(count, x + cardW - 7 - c.width(count), cy + 5, Ui.FAINT);
                int ry = cy + GROUP_HEAD + 1;
                for (Row r : rows) {
                    if (ry + ROW_H >= y && ry < y + h) {
                        row(r, x + 2, ry, cardW - 4, query, searching ? (String) card[2] : null);
                    }
                    ry += ROW_H;
                }
            }
            cy += cardH + 6;
        }
        unclip();

        // 換分類的淡入：蓋一層跟視窗同色、慢慢變透明的布
        if (bodyT < 0.99f) {
            c.fill(x, y, x + w, y + h, Ui.fade(Ui.alpha(Ui.WINDOW, 0xE0), 1f - bodyT));
        }

        if (bar) {
            int trackX = x + w - 3;
            c.fill(trackX, y, trackX + 2, y + h, Ui.LINE);
            int thumbH = Math.max(12, h * h / Math.max(1, contentH));
            int thumbY = y + Math.round((h - thumbH) * (scroll / maxScroll));
            c.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, Ui.alpha(accent, 0xC0));
        }
    }

    private static boolean matches(Row r, String query) {
        return r.name.get().toLowerCase(Locale.ROOT).contains(query)
                || r.hint.get().toLowerCase(Locale.ROOT).contains(query);
    }

    // ------------------------------------------------------------ 一列

    private void row(Row r, int x, int y, int w, String query, String chip) {
        boolean hot = over(x, y, w, ROW_H);
        if (hot) {
            c.fill(x + 1, y, x + w - 1, y + ROW_H, 0x10FFFFFF);
            hoverRow = r.id;
            tip = new Tip(r.hint.get(), x, y, w, ROW_H);
        }
        int right = x + w - 6;
        int cy = y + (ROW_H - CTRL_H) / 2;
        int labelX = x + 7;
        int room = right - labelX - MIN_LABEL - 6;     // 控制項最多能用多寬

        int ctrlLeft = switch (r.kind) {
            case TOGGLE -> toggle(r, right, cy);
            case SEGMENT -> segment(r, right, cy, room);
            case SLIDER -> slider(r, right, cy, room);
            case COLOUR -> colour(r, right, cy);
            case SELECT -> select(r, right, cy, room);
            case ACTION -> action(r, right, cy, room);
            default -> status(r, right, cy, room);
        };

        int labelRoom = ctrlLeft - 6 - labelX;
        int tx = labelX;
        if (chip != null) {
            String tag = Ui.fit(c, chip, Math.max(0, labelRoom / 3));
            if (!tag.isEmpty()) {
                int tw = c.width(tag) + 6;
                Ui.box(c, tx, y + 5, tw, 12, 0, Ui.BORDER);
                c.text(tag, tx + 3, y + 7, Ui.FAINT);
                tx += tw + 4;
                labelRoom -= tw + 4;
            }
        }
        boolean changed = r.resettable() && !r.isDefault.getAsBoolean();
        String name = Ui.fit(c, r.name.get(), labelRoom - (changed ? 7 : 0));
        int ink = hot ? Ui.TEXT : Ui.TEXT_2;
        int hit = query.isEmpty() ? -1 : name.toLowerCase(Locale.ROOT).indexOf(query);
        if (hit >= 0 && hit + query.length() <= name.length()) {
            // 搜尋命中的那幾個字反白
            String pre = name.substring(0, hit);
            String mid = name.substring(hit, hit + query.length());
            int px = tx + c.width(pre);
            c.fill(px - 1, y + 5, px + c.width(mid) + 1, y + 16, accent);
            c.text(pre, tx, y + 7, ink);
            c.text(mid, px, y + 7, Ui.ON_ACCENT);
            c.text(name.substring(hit + query.length()), px + c.width(mid), y + 7, ink);
        } else {
            c.text(name, tx, y + 7, ink);
        }
        int labelRight = tx + c.width(name);
        if (changed) {
            // 改過、不是預設值的記號
            c.fill(labelRight + 3, y + 9, labelRight + 6, y + 12, Ui.GOLD);
            labelRight += 6;
        }
        placed.add(new Placed(r.id, x, y, w, ROW_H, ctrlLeft, labelRight));
    }

    /** 一顆按鈕。@return 滑鼠有沒有在上面 */
    private boolean button(int x, int y, int w, String label, boolean enabled, boolean primary,
                           String[] glyph, Runnable go) {
        boolean hot = enabled && over(x, y, w, CTRL_H);
        int bg = primary && enabled ? accent : Ui.RAISED;
        int edge = primary && enabled ? accent : (hot ? accent : Ui.BORDER);
        Ui.box(c, x, y, w, CTRL_H, bg, edge);
        if (hot) {
            Ui.glow(c, x, y, w, CTRL_H, Ui.alpha(accent, 0x50));
        }
        int ink = !enabled ? Ui.FAINT : primary ? Ui.ON_ACCENT : (hot ? Ui.TEXT : Ui.TEXT_2);
        int glyphW = glyph == null ? 0 : glyph[0].length() + 4;
        String text = Ui.fit(c, label, w - 8 - glyphW);
        int tx = x + (w - c.width(text) - glyphW) / 2;
        c.text(text, tx, y + 3, ink);
        if (glyph != null) {
            Ui.glyph(c, tx + c.width(text) + 4, y + (CTRL_H - glyph.length) / 2, glyph,
                     enabled ? Ui.HINT : Ui.FAINT);
        }
        zone(x, y, w, CTRL_H, (px, py, b) -> {
            if (enabled) {
                host.click();
                go.run();
            }
        });
        return hot;
    }

    private float animate(String key, float target, float tau) {
        float at = anim.getOrDefault(key, target);
        at = Ui.ease(at, target, dt, tau);
        anim.put(key, at);
        return at;
    }

    // ---- 開關

    private int toggle(Row r, int right, int y) {
        boolean on = r.on.getAsBoolean();
        float t = animate("t:" + r.id, on ? 1f : 0f, 55f);
        int w = 24;
        int x = right - w;
        int h = 12;
        int ty = y + 1;
        boolean hot = over(x, ty, w, h);
        int track = Ui.mix(Ui.FIELD, Ui.alpha(accent, 0x58), t);
        int edge = Ui.mix(Ui.TRACK_OFF, accent, t);
        Ui.box(c, x, ty, w, h, track, edge);
        if (hot) {
            Ui.glow(c, x, ty, w, h, Ui.alpha(accent, 0x50));
        }
        int knobX = x + 2 + Math.round((w - 12) * t);
        Ui.box(c, knobX, ty + 2, 8, 8, Ui.mix(Ui.KNOB_OFF, accent, t), 0);
        zone(x - 2, y - 2, w + 4, CTRL_H + 4, (px, py, b) -> {
            host.click();
            r.flip.run();
        });
        String text = on ? (r.onText != null ? r.onText.get() : host.tr("mode.on"))
                         : host.tr("mode.off");
        int tw = c.width(text);
        c.text(text, x - 6 - tw, y + 3, Ui.mix(Ui.FAINT, accent, t));
        return x - 6 - tw;
    }

    // ---- 分段

    private int toneColour(Row.Tone tone) {
        return switch (tone) {
            case REPLACE -> complement(accent);
            case BOTH -> Ui.mix(accent, complement(accent), 0.5f);
            case OFF -> Ui.TRACK_OFF;
            default -> accent;
        };
    }

    /** 色相轉半圈，亮度拉到跟主題色差不多——預設的藍配出來是橘。 */
    static int complement(int argb) {
        float[] h = Ui.toHsv(argb);
        return Ui.hsv(h[0] + 180f, Math.max(0.45f, Math.min(0.7f, h[1])), Math.max(0.82f, h[2]));
    }

    private int segment(Row r, int right, int y, int room) {
        int end = right;
        if (r.extra != null) {
            String label = r.extraLabel.get();
            int bw = Math.min(c.width(label) + 12, Math.max(24, room / 4));
            button(end - bw, y, bw, label, true, false, null, r.extra);
            end -= bw + 4;
            room -= bw + 4;
        }
        int n = Math.max(1, r.options.size());
        int widest = 0;
        for (Row.Option o : r.options) {
            widest = Math.max(widest, c.width(o.label()));
        }
        int cell = Math.max(24, Math.min(widest + 10, Math.max(24, (room - 4) / n)));
        int w = cell * n + 4;
        int x = end - w;
        int at = Math.max(0, Math.min(n - 1, r.selected.getAsInt()));
        float pos = animate("s:" + r.id, at, 60f);
        boolean hotAny = over(x, y, w, CTRL_H);
        Ui.box(c, x, y, w, CTRL_H, Ui.FIELD, hotAny ? Ui.alpha(accent, 0xA0) : Ui.BORDER);

        Row.Tone tone = r.options.isEmpty() ? Row.Tone.PLAIN : r.options.get(at).tone();
        int thumb = toneColour(tone);
        int thumbX = x + 2 + Math.round(cell * pos);
        Ui.box(c, thumbX, y + 2, cell, CTRL_H - 4, thumb, 0);

        for (int i = 0; i < n; i++) {
            Row.Option o = r.options.get(i);
            int cx = x + 2 + cell * i;
            boolean on = i == at;
            boolean hot = !on && over(cx, y, cell, CTRL_H);
            String text = Ui.fit(c, o.label(), cell - 4);
            int ink = on ? (tone == Row.Tone.OFF ? Ui.TEXT : Ui.ON_ACCENT)
                         : (hot ? Ui.TEXT : Ui.HINT);
            c.text(text, cx + (cell - c.width(text)) / 2, y + 3, ink);
            int index = i;
            zone(cx, y, cell, CTRL_H, (px, py, b) -> {
                if (index != r.selected.getAsInt()) {
                    host.click();
                    r.pick.accept(index);
                }
            });
        }
        return x;
    }

    // ---- 滑桿

    private int resetButton(Row r, int right, int y) {
        int x = right - CTRL_H;
        boolean can = !r.isDefault.getAsBoolean();
        boolean hot = can && over(x, y, CTRL_H, CTRL_H);
        Ui.box(c, x, y, CTRL_H, CTRL_H, Ui.RAISED, hot ? accent : Ui.BORDER);
        if (hot) {
            Ui.glow(c, x, y, CTRL_H, CTRL_H, Ui.alpha(accent, 0x50));
        }
        Ui.glyph(c, x + 3, y + 4, Ui.ICON_RESET, can ? (hot ? Ui.TEXT : Ui.TEXT_2) : Ui.FAINT);
        zone(x, y, CTRL_H, CTRL_H, (px, py, b) -> {
            if (can) {
                host.click();
                r.reset.run();
                host.resetDone(r.name.get(), 0);
            }
        });
        return x;
    }

    private int slider(Row r, int right, int y, int room) {
        int end = r.resettable() ? resetButton(r, right, y) - 4 : right;
        int value = r.value.getAsInt();
        String text = r.format.apply(value);
        int textW = Math.max(c.width(r.format.apply(r.max)), c.width(text));
        c.text(text, end - c.width(text), y + 3, Ui.TEXT);
        end -= textW + 6;

        int w = Math.max(40, Math.min(100, room - (right - end)));
        int x = end - w;
        int span = Math.max(1, r.max - r.min);
        float t = (Math.max(r.min, Math.min(r.max, value)) - r.min) / (float) span;
        boolean hot = over(x - 3, y, w + 6, CTRL_H) || isDragging("d:" + r.id);
        int ty = y + CTRL_H / 2 - 1;
        c.fill(x, ty, x + w, ty + 3, Ui.BORDER);
        int knob = x + Math.round((w - 1) * t);
        c.fill(x, ty, knob, ty + 3, accent);
        if (hot) {
            Ui.box(c, knob - 4, y, 9, CTRL_H, 0, Ui.alpha(accent, 0x60));
        }
        Ui.box(c, knob - 3, y + 1, 7, CTRL_H - 2, Ui.TEXT, Ui.ON_ACCENT);

        Zone z = zone(x - 4, y - 2, w + 8, CTRL_H + 4, (px, py, b) -> slideTo(r, x, w, px));
        z.drag = (px, py, b) -> slideTo(r, x, w, px);
        z.release = r.commit;
        dragKeys.put(z, "d:" + r.id);
        return x - 4;
    }

    private final Map<Zone, String> dragKeys = new HashMap<>();
    private String draggingKey;

    private boolean isDragging(String key) {
        return dragging != null && key.equals(draggingKey);
    }

    private void slideTo(Row r, int x, int w, double px) {
        float t = (float) Math.max(0, Math.min(1, (px - x) / Math.max(1, w - 1)));
        int next = r.min + Math.round(t * (r.max - r.min));
        draggingKey = "d:" + r.id;
        if (next != r.value.getAsInt()) {
            r.slide.accept(next);
        }
    }

    // ---- 顏色

    private int colour(Row r, int right, int y) {
        int end = r.resettable() ? resetButton(r, right, y) - 4 : right;
        String hex = r.hex.get();
        int w = 22 + c.width("#MMMMMM") + 9;
        int x = end - w;
        boolean open = pickerOpen && pickerRow == r;
        boolean hot = over(x, y, w, CTRL_H);
        Ui.box(c, x, y, w, CTRL_H, Ui.FIELD, hot || open ? accent : Ui.BORDER);
        if (hot || open) {
            Ui.glow(c, x, y, w, CTRL_H, Ui.alpha(accent, 0x50));
        }
        Ui.box(c, x + 3, y + 3, 10, CTRL_H - 6, Ui.parseHex(hex, accent), 0x50FFFFFF);
        c.text(hex, x + 17, y + 3, Ui.TEXT);
        Ui.glyph(c, x + w - 9, y + 6, open ? Ui.ICON_UP : Ui.ICON_DOWN, Ui.HINT);
        zone(x, y, w, CTRL_H, (px, py, b) -> {
            host.click();
            boolean was = pickerOpen && pickerRow == r;
            closePopovers();
            if (!was) {
                pickerOpen = true;
                pickerRow = r;
                float[] next = Ui.toHsv(Ui.parseHex(r.hex.get(), accent));
                System.arraycopy(next, 0, hsv, 0, 3);
                hexField.set(r.hex.get());
            }
        });
        if (open) {
            int ax = x;
            int ay = y + CTRL_H + 2;
            popover = () -> picker(r, ax, ay, y - 2);
        }
        return x;
    }

    private static final String[] PRESETS = {
        "#6FA8D8", "#8FD694", "#C9A45C", "#E0706B", "#B48EDE", "#5FC8C0", "#F0A35E", "#C5CFDA"};

    /** 彈出的色盤：上面一塊飽和×明度，下面一條色相，再來幾個預設色與色碼。 */
    private void picker(Row r, int ax, int below, int above) {
        int w = 150;
        int svH = 62;
        int h = 6 + svH + 5 + 8 + 5 + 12 + 5 + CTRL_H + 6;
        int x = Math.max(winX + 4, Math.min(winX + winW - w - 4, ax));
        int y = below + h <= winY + winH - 4 ? below : Math.max(winY + 4, above - h);
        float in = animate("pop:picker", 1f, 45f);
        Ui.box(c, x - 1, y - 1, w + 2, h + 2, 0, 0x70000000);
        Ui.box(c, x, y, w, h, 0xFF131A23, Ui.alpha(accent, 0xB0));
        // 吃掉點在色盤空白處的點擊，不然會被當成「點外面」而收起來
        zone(x, y, w, h, (px, py, b) -> focus = null);

        int sx = x + 6;
        int sy = y + 6;
        int sw = w - 12;
        // 飽和×明度：用小格子鋪出漸層，每格兩像素
        int cell = 2;
        for (int gy = 0; gy < svH; gy += cell) {
            float v = 1f - gy / (float) (svH - cell);
            for (int gx = 0; gx < sw; gx += cell) {
                float s = gx / (float) (sw - cell);
                c.fill(sx + gx, sy + gy, Math.min(sx + sw, sx + gx + cell),
                       Math.min(sy + svH, sy + gy + cell), Ui.hsv(hsv[0], s, v));
            }
        }
        Ui.box(c, sx - 1, sy - 1, sw + 2, svH + 2, 0, Ui.BORDER);
        int hx = sx + Math.round(hsv[1] * (sw - 1));
        int hy = sy + Math.round((1f - hsv[2]) * (svH - 1));
        Ui.box(c, hx - 3, hy - 3, 7, 7, 0, Ui.ON_ACCENT);
        Ui.box(c, hx - 2, hy - 2, 5, 5, 0, 0xFFFFFFFF);
        Zone sv = zone(sx, sy, sw, svH, (px, py, b) -> pickSv(r, sx, sy, sw, svH, px, py));
        sv.drag = (px, py, b) -> pickSv(r, sx, sy, sw, svH, px, py);
        sv.release = r.commit;

        int by = sy + svH + 5;
        for (int i = 0; i < sw; i++) {
            c.fill(sx + i, by, sx + i + 1, by + 8, Ui.hsv(360f * i / sw, 1f, 1f));
        }
        Ui.box(c, sx - 1, by - 1, sw + 2, 10, 0, Ui.BORDER);
        int kx = sx + Math.round(hsv[0] / 360f * (sw - 1));
        Ui.box(c, kx - 2, by - 2, 5, 12, 0xFFFFFFFF, Ui.ON_ACCENT);
        Zone hue = zone(sx, by - 2, sw, 12, (px, py, b) -> pickHue(r, sx, sw, px));
        hue.drag = (px, py, b) -> pickHue(r, sx, sw, px);
        hue.release = r.commit;

        int py0 = by + 8 + 5;
        int step = (sw + 2) / PRESETS.length;
        String now0 = r.hex.get();
        for (int i = 0; i < PRESETS.length; i++) {
            String p = PRESETS[i];
            int px0 = sx + i * step;
            boolean on = p.equalsIgnoreCase(now0);
            boolean hot = over(px0, py0, step - 2, 12);
            Ui.box(c, px0, py0, step - 2, 12, Ui.parseHex(p, accent),
                   on ? 0xFFFFFFFF : (hot ? Ui.TEXT_2 : 0x40FFFFFF));
            zone(px0, py0, step - 2, 12, (qx, qy, b) -> {
                host.click();
                if (r.setHex.test(p)) {
                    float[] next = Ui.toHsv(Ui.parseHex(p, accent));
                    System.arraycopy(next, 0, hsv, 0, 3);
                    hexField.set(p);
                    r.commit.run();
                }
            });
        }

        int fy = py0 + 12 + 5;
        Ui.box(c, sx, fy, CTRL_H, CTRL_H, Ui.parseHex(now0, accent), 0x50FFFFFF);
        String def = host.tr("reset.default");
        int dw = Math.min(c.width(def) + 12, 60);
        int fx = sx + CTRL_H + 4;
        int fw = sw - CTRL_H - 4 - dw - 4;
        boolean focused = focus == hexField;
        Ui.box(c, fx, fy, fw, CTRL_H, Ui.FIELD, focused ? accent : Ui.BORDER);
        field(hexField, fx + 4, fy + 3, fw - 8, focused);
        zone(fx, fy, fw, CTRL_H, (qx, qy, b) -> {
            focus = hexField;
            hexField.caret = hexField.text.length();
            hexField.all = true;
        });
        button(sx + sw - dw, fy, dw, def, r.resettable() && !r.isDefault.getAsBoolean(), false,
               null, () -> {
                   r.reset.run();
                   float[] next = Ui.toHsv(Ui.parseHex(r.hex.get(), accent));
                   System.arraycopy(next, 0, hsv, 0, 3);
                   hexField.set(r.hex.get());
               });
        if (in < 0.98f) {
            c.fill(x, y, x + w, y + h, Ui.fade(0xFF131A23, 1f - in));
        }
    }

    private void pickSv(Row r, int sx, int sy, int sw, int sh, double px, double py) {
        hsv[1] = (float) Math.max(0, Math.min(1, (px - sx) / Math.max(1, sw - 1)));
        hsv[2] = 1f - (float) Math.max(0, Math.min(1, (py - sy) / Math.max(1, sh - 1)));
        applyHsv(r);
    }

    private void pickHue(Row r, int sx, int sw, double px) {
        hsv[0] = 359.9f * (float) Math.max(0, Math.min(1, (px - sx) / Math.max(1, sw - 1)));
        applyHsv(r);
    }

    private void applyHsv(Row r) {
        String hex = Ui.toHex(Ui.hsv(hsv[0], hsv[1], hsv[2]));
        if (r.setHex.test(hex)) {
            hexField.set(hex);
        }
    }

    // ---- 下拉

    private int select(Row r, int right, int y, int room) {
        int end = right;
        boolean locked = r.locked.getAsBoolean();
        if (r.apply != null) {
            String label = host.tr("button.apply");
            int bw = Math.min(c.width(label) + 12, Math.max(24, room / 3));
            boolean can = !locked && r.canApply.getAsBoolean();
            button(end - bw, y, bw, label, can, can, null, r.apply);
            end -= bw + 4;
            room -= bw + 4;
        }
        List<Row.Choice> list = r.choices.get();
        String id = r.current.get();
        Row.Choice cur = null;
        for (Row.Choice ch : list) {
            if (ch.id().equals(id)) {
                cur = ch;
            }
        }
        if (cur == null && !list.isEmpty()) {
            cur = list.get(0);
        }
        int w = Math.max(60, Math.min(150, room));
        int x = end - w;
        boolean open = r.id.equals(openSelect);
        boolean hot = !locked && over(x, y, w, CTRL_H);
        Ui.box(c, x, y, w, CTRL_H, Ui.FIELD, hot || open ? accent : Ui.BORDER);
        if (hot || open) {
            Ui.glow(c, x, y, w, CTRL_H, Ui.alpha(accent, 0x50));
        }
        String busy = r.busy.get();
        int tx = x + 3;
        if (cur != null && busy == null) {
            tx = badge(cur, x + 3, y + 2, true);
        }
        String text = busy != null ? busy : (cur == null ? "" : cur.label());
        c.text(Ui.fit(c, text, x + w - 12 - tx), tx, y + 3, locked ? Ui.HINT : Ui.TEXT);
        float turn = animate("chev:" + r.id, open ? 1f : 0f, 50f);
        Ui.glyph(c, x + w - 9, y + 6, turn > 0.5f ? Ui.ICON_UP : Ui.ICON_DOWN, Ui.HINT);
        zone(x, y, w, CTRL_H, (px, py, b) -> {
            if (locked) {
                return;
            }
            host.click();
            boolean was = r.id.equals(openSelect);
            closePopovers();
            if (!was) {
                openSelect = r.id;
                anim.put("pop:select", 0f);
            }
        });
        if (open) {
            int ay = y + CTRL_H + 2;
            popover = () -> dropdown(r, list, id, x, ay, y - 2, w);
        }
        return x;
    }

    /**
     * 語言的小徽章。
     *
     * <p>用代碼的字而不是國旗：語言不等於國家，同一面旗底下不只一種語言，
     * 同一種語言也不只一面旗——而且旗子畫成十像素見方誰也認不出來。
     *
     * @return 徽章之後字該從哪裡開始
     */
    private int badge(Row.Choice ch, int x, int y, boolean on) {
        String text = ch.badge();
        if (text == null || text.isEmpty()) {
            return x + 2;
        }
        int w = Math.max(12, c.width(text) + 4);
        int bg = ch.auto() ? Ui.GOLD : (on ? accent : Ui.HINT);
        Ui.box(c, x, y, w, CTRL_H - 4, bg, 0);
        c.text(text, x + (w - c.width(text)) / 2, y + 1, Ui.ON_ACCENT);
        return x + w + 4;
    }

    private void dropdown(Row r, List<Row.Choice> list, String id, int ax, int below,
                          int above, int minW) {
        int itemH = 14;
        int w = minW;
        for (Row.Choice ch : list) {
            w = Math.max(w, c.width(ch.label()) + 44);
        }
        w = Math.min(w, winW - 16);
        int h = list.size() * itemH + 4;
        int x = Math.max(winX + 4, Math.min(winX + winW - w - 4, ax));
        int y = below + h <= winY + winH - 4 ? below : Math.max(winY + 4, above - h);
        float in = animate("pop:select", 1f, 45f);
        Ui.box(c, x - 1, y - 1, w + 2, h + 2, 0, 0x70000000);
        Ui.box(c, x, y, w, h, 0xFF131A23, Ui.alpha(accent, 0xB0));
        zone(x, y, w, h, (px, py, b) -> { });
        for (int i = 0; i < list.size(); i++) {
            Row.Choice ch = list.get(i);
            int iy = y + 2 + i * itemH;
            boolean on = ch.id().equals(id);
            boolean hot = over(x + 2, iy, w - 4, itemH);
            if (hot) {
                Ui.box(c, x + 2, iy, w - 4, itemH, Ui.alpha(accent, 0x38), 0);
            }
            int tx = badge(ch, x + 4, iy + 2, on);
            c.text(Ui.fit(c, ch.label(), x + w - 14 - tx), tx, iy + 3,
                   on || hot ? Ui.TEXT : Ui.TEXT_2);
            if (on) {
                Ui.glyph(c, x + w - 12, iy + 4, Ui.ICON_CHECK, accent);
            }
            zone(x + 2, iy, w - 4, itemH, (px, py, b) -> {
                host.click();
                openSelect = null;
                r.choose.accept(ch.id());
            });
        }
        if (in < 0.98f) {
            c.fill(x, y, x + w, y + h, Ui.fade(0xFF131A23, 1f - in));
        }
    }

    // ---- 動作與狀態

    private int action(Row r, int right, int y, int room) {
        String label = r.label.get();
        boolean can = r.enabled.getAsBoolean();
        int w = Math.max(40, Math.min(c.width(label) + 22, room));
        button(right - w, y, w, label, can, false, Ui.ICON_RIGHT, r.run);
        return right - w;
    }

    private int status(Row r, int right, int y, int room) {
        String label = r.label.get();
        int ink = r.labelColour != null ? r.labelColour.getAsInt() : Ui.TEXT;
        int w = Math.max(40, Math.min(c.width(label) + 22, room));
        int x = right - w;
        boolean hot = over(x, y, w, CTRL_H);
        Ui.box(c, x, y, w, CTRL_H, Ui.FIELD, hot ? accent : Ui.BORDER);
        if (hot) {
            Ui.glow(c, x, y, w, CTRL_H, Ui.alpha(accent, 0x50));
        }
        String text = Ui.fit(c, label, w - 18);
        int tx = x + (w - c.width(text) - 9) / 2;
        Ui.box(c, tx, y + 5, 5, 5, ink, 0);
        c.text(text, tx + 9, y + 3, ink);
        zone(x, y, w, CTRL_H, (px, py, b) -> {
            host.click();
            r.run.run();
        });
        return x;
    }

    // ------------------------------------------------------------ 預覽

    private void preview(int x, int y, int w, int h) {
        Row.Tab t = tabs.get(tab);
        String title = host.tr("preview.title");
        // 標題前面一顆會呼吸的小方塊：這一塊是活的
        float pulse = 0.55f + 0.45f * (float) Math.sin(now / 420.0);
        c.fill(x, y + 2, x + 4, y + 6, Ui.fade(accent, pulse));
        c.text(title, x + 8, y, Ui.HINT);
        int lineL = x + 8 + c.width(title) + 5;
        if (x + w > lineL) {
            c.fill(lineL, y + 4, x + w, y + 5, Ui.LINE);
        }

        List<String> about = Ui.wrap(c, t.about().get(), w - 12);
        int lines = Math.min(3, about.size());
        int capH = lines * 10 + 8;
        int sceneY = y + 13;
        int sceneH = h - 13 - capH - 6;
        if (sceneH < 60) {
            // 太矮就只留場景，說明讓出來
            sceneH = h - 13;
            capH = 0;
        }
        Preview.State state = host.preview();
        if (gapShown < 0) {
            gapShown = state.gap;
        }
        gapShown = Ui.ease(gapShown, state.gap, dt, 45f);
        Preview.draw(c, x, sceneY, w, sceneH, t.scene(), state, accent, gapShown);

        if (capH > 0) {
            int cy = sceneY + sceneH + 6;
            Ui.box(c, x, cy, w, capH, Ui.CARD, Ui.LINE);
            for (int i = 0; i < lines; i++) {
                String line = about.get(i);
                if (i == lines - 1 && about.size() > lines) {
                    line = Ui.fit(c, line + "…", w - 12);
                }
                c.text(line, x + 6, cy + 4 + i * 10, Ui.TEXT_2);
            }
        }
    }

    // ------------------------------------------------------------ 底列

    private void footer() {
        int y = winY + winH - FOOT_H;
        c.fill(winX + 1, y, winX + winW - 1, y + 1, Ui.LINE);
        int by = y + 6;
        int right = winX + winW - PAD;

        String done = host.tr("button.done");
        int dw = c.width(done) + 24;
        button(right - dw, by, dw, done, true, true, null, host::done);
        right -= dw + 4;

        String credits = host.tr("button.credits");
        int cw = c.width(credits) + 14;
        button(right - cw, by, cw, credits, true, false, null, host::openCredits);
        right -= cw + 4;

        boolean fresh = host.hasUpdate();
        String updates = host.tr(fresh ? "button.updates.new" : "button.updates");
        int uw = c.width(updates) + 14 + (fresh ? 0 : 0);
        int ux = right - uw;
        boolean hot = over(ux, by, uw, CTRL_H);
        Ui.box(c, ux, by, uw, CTRL_H, Ui.RAISED, fresh ? Ui.GOLD : (hot ? accent : Ui.BORDER));
        if (hot) {
            Ui.glow(c, ux, by, uw, CTRL_H, Ui.alpha(fresh ? Ui.GOLD : accent, 0x50));
        }
        c.text(updates, ux + 7, by + 3, fresh ? Ui.GOLD : (hot ? Ui.TEXT : Ui.TEXT_2));
        zone(ux, by, uw, CTRL_H, (px, py, b) -> {
            host.click();
            host.openUpdates();
        });
        right = ux - 8;

        Status s = host.status();
        int left = winX + PAD;
        if (s != null && right - left > 30) {
            c.fill(left, by + 5, left + 4, by + 9, s.colour());
            c.text(Ui.fit(c, s.text(), right - left - 8), left + 8, by + 3, s.colour());
        }
    }

    // ------------------------------------------------------------ 說明

    private record Tip(String text, int x, int y, int w, int h) {}

    private void tooltip(Tip t, float alpha, int screenW, int screenH) {
        if (t.text() == null || t.text().isEmpty()) {
            return;
        }
        int maxW = Math.min(230, winW - 24);
        List<String> lines = Ui.wrap(c, t.text(), maxW - 12);
        int w = 12;
        for (String line : lines) {
            w = Math.max(w, c.width(line) + 12);
        }
        int h = lines.size() * 10 + 7;
        int x = Math.max(winX + 4, Math.min(winX + winW - w - 4, t.x() + 6));
        int y = t.y() + t.h() + 3;
        if (y + h > winY + winH - 4) {
            y = t.y() - h - 3;                 // 底下放不下就翻到上面
        }
        y = Math.max(2, Math.min(screenH - h - 2, y));
        Ui.box(c, x - 1, y - 1, w + 2, h + 2, 0, Ui.fade(0x70000000, alpha));
        Ui.box(c, x, y, w, h, Ui.fade(0xF50A0F15, alpha), Ui.fade(Ui.TRACK_OFF, alpha));
        c.fill(x + 1, y + 1, x + 3, y + h - 1, Ui.fade(accent, alpha));
        for (int i = 0; i < lines.size(); i++) {
            c.text(lines.get(i), x + 7, y + 4 + i * 10, Ui.fade(Ui.TEXT_2, alpha));
        }
    }

    // ------------------------------------------------------------ 輸入框

    /**
     * 一行文字的輸入框：搜尋與色碼各一個。
     *
     * <p>不用原版的 {@code EditBox}：那個要活在 {@code Screen} 的元件樹裡，
     * 這一層就得認得遊戲了。這裡只需要一行、不折行、游標在哪裡——夠用，
     * 而且輸入法送進來的字照樣收得到（{@code charTyped} 給的就是選好的字）。
     */
    static final class Field {
        final StringBuilder text = new StringBuilder();
        final int max;
        int caret;
        /** 全選：下一個打進來的字會把整段換掉。 */
        boolean all;

        Field(int max) {
            this.max = max;
        }

        void clear() {
            text.setLength(0);
            caret = 0;
            all = false;
        }

        void set(String value) {
            text.setLength(0);
            text.append(value == null ? "" : value);
            caret = text.length();
            all = false;
        }

        boolean type(String s) {
            if (s == null || s.isEmpty() || s.charAt(0) < ' ') {
                return false;
            }
            if (all) {
                clear();
            }
            if (text.codePointCount(0, text.length()) + s.codePointCount(0, s.length()) > max) {
                return false;
            }
            text.insert(caret, s);
            caret += s.length();
            return true;
        }

        boolean key(int key, boolean ctrl, Host host) {
            if (ctrl && key == 'A') {
                all = text.length() > 0;
                caret = text.length();
                return false;
            }
            if (ctrl && key == 'C') {
                host.setClipboard(text.toString());
                return false;
            }
            if (ctrl && key == 'V') {
                String clip = host.clipboard();
                if (clip == null) {
                    return false;
                }
                boolean changed = false;
                for (int i = 0; i < clip.length(); ) {
                    int cp = clip.codePointAt(i);
                    i += Character.charCount(cp);
                    if (cp >= ' ') {
                        changed |= type(new String(Character.toChars(cp)));
                    }
                }
                return changed;
            }
            switch (key) {
                case KEY_BACKSPACE -> {
                    if (all) {
                        clear();
                        return true;
                    }
                    if (caret > 0) {
                        int from = text.offsetByCodePoints(caret, -1);
                        text.delete(from, caret);
                        caret = from;
                        return true;
                    }
                }
                case KEY_DELETE -> {
                    if (all) {
                        clear();
                        return true;
                    }
                    if (caret < text.length()) {
                        text.delete(caret, text.offsetByCodePoints(caret, 1));
                        return true;
                    }
                }
                case KEY_LEFT -> {
                    all = false;
                    if (caret > 0) {
                        caret = text.offsetByCodePoints(caret, -1);
                    }
                }
                case KEY_RIGHT -> {
                    all = false;
                    if (caret < text.length()) {
                        caret = text.offsetByCodePoints(caret, 1);
                    }
                }
                case KEY_HOME -> {
                    all = false;
                    caret = 0;
                }
                case KEY_END -> {
                    all = false;
                    caret = text.length();
                }
                default -> { }
            }
            return false;
        }
    }
}
