package com.wynnchayuan.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 設定畫面的版面、互動與動畫。
 *
 * <h2>版面</h2>
 * 畫面中央一張霧面玻璃的視窗：
 * <pre>
 *   側欄            | 分類名稱與一句說明 ……………………………… 重置本頁
 *   圖示、名稱、版本 | 左：這一類的設定（卡片分組，可捲動）   右：即時預覽
 *   搜尋            |
 *   物品／面板／…   |
 *   使用須知        | 狀態 ………………… 更新說明  關於／貢獻者  完成
 * </pre>
 * 三塊的寬度是固定的：字再長也只會讓某一列變高，不會把側欄或預覽擠掉。
 *
 * <h2>字太長的時候</h2>
 * 俄文、西班牙文的選項常常放不到名稱右邊。逐列各自換行的話，同一頁會有的擠、
 * 有的鬆，很難看。所以規則是<b>整頁一起</b>：這一頁只要有一列放不下，分段、
 * 下拉與滑桿就全部改成「名稱一列、控制項整列寬」；開關與按鈕本來就短，留在右邊。
 * 見 {@link #measure}。
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
public final class SettingsView extends Surface {

    /** 這個畫面需要外面幫忙的事。 */
    public interface Host {
        /** 風格顏色（ARGB）：這個畫面自己的重點色。 */
        int accent();

        /** 框線顏色（ARGB）：遊戲裡譯文小框的框，只有預覽會用到。 */
        int frame();

        /** 分段控制器選到某一種顯示方式時的顏色。 */
        int tone(Row.Tone tone);

        /** 介面字串。鍵不含 {@code wynnchayuan.} 前綴。 */
        String tr(String key, Object... args);

        String title();

        /** 標題底下那一句：這個模組是做什麼的。 */
        String tagline();

        String version();

        /** 底列現在要講的那一句與它的顏色。 */
        Status status();

        /** 有新版的模組：更新說明那顆要亮起來。 */
        boolean hasUpdate();

        /** 視窗底下有沒有模糊。沒有的話玻璃要不透明一點，不然字讀不清楚。 */
        boolean blurred();

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
        void resetDone(String what);
    }

    public record Status(String text, int colour) {}

    // ------------------------------------------------------------ 版面常數

    static final float MAX_W = 628;
    static final float MAX_H = 334;
    static final float RAIL_W = 126;
    static final float RAIL_SLIM = 32;
    static final float HEAD_H = 38;
    static final float FOOT_H = 28;
    static final float ROW_H = 22;
    /** 名稱一列、控制項一列的那種列。 */
    static final float ROW_WIDE_H = 40;
    static final float GROUP_HEAD = 15;
    static final float PREVIEW_W = 152;
    static final float NAV_H = 21;


    // ------------------------------------------------------------ 狀態

    private final List<Row.Tab> tabs;
    private final Host host;

    private int tab;
    private float scroll;
    private float scrollTarget;
    private float maxScroll;

    private final Field search = new Field(40);
    private final Field hexField = new Field(7);
    private Field focus;

    private String openSelect;
    private boolean pickerOpen;
    private Row pickerRow;
    private final float[] hsv = {210, 0.5f, 0.85f};


    private float bodyT = 1f;
    private float gapShown = -1;

    private float railW;
    private boolean pageWide;

    /** 給測試看的：每一列實際畫在哪、名稱與控制項各佔到哪。 */
    public record Placed(String id, float x, float y, float w, float h, float labelRight,
                         float ctrlLeft, float ctrlRight, boolean wide) {}

    private final List<Placed> placed = new ArrayList<>();

    /**
     * 只有一類設定的小視窗（名牌的進階設定）：沒有側欄與搜尋，底列只有一顆「返回」。
     * 列的畫法、重置、說明、預覽都跟完整的設定畫面是同一份。
     */
    private final boolean sheet;
    /** 小視窗這一幀量到的「剛好放得下」的高度，下一幀照它開；還沒量過是 0。 */
    private float sheetH;

    public SettingsView(List<Row.Tab> tabs, Host host, int startTab) {
        this(tabs, host, startTab, false);
    }

    public SettingsView(List<Row.Tab> tabs, Host host, int startTab, boolean sheet) {
        this.tabs = tabs;
        this.host = host;
        this.tab = Math.max(0, Math.min(tabs.size() - 1, startTab));
        this.sheet = sheet;
    }

    @Override
    protected int accentColour() {
        return host.accent();
    }

    @Override
    protected boolean covered() {
        return popoverOpen();
    }

    @Override
    protected void dismiss() {
        closePopovers();
    }

    @Override
    protected String clipboard() {
        return host.clipboard();
    }

    @Override
    protected void setClipboard(String text) {
        host.setClipboard(text);
    }

    @Override
    protected void click() {
        host.click();
    }

    @Override
    protected void draw() {
        placed.clear();
        float w = Math.min(screenW - 12, sheet ? 470 : MAX_W);
        // 小視窗照內容長：四列的中文與換成兩列的俄文差了快一倍，固定高度不是空一截就是要捲
        float h = Math.min(screenH - 14, sheet ? (sheetH > 0 ? sheetH : 236) : MAX_H);
        window(Math.round((screenW - w) / 2f), Math.round((screenH - h) / 2f), w, h,
               host.blurred());
        railW = sheet ? 0 : (winW >= 430 ? RAIL_W : RAIL_SLIM);

        // 點到空白處：收起彈出層、放掉輸入框的焦點
        zone(0, 0, screenW, screenH, (x, y, b) -> {
            closePopovers();
            focus = null;
        });

        if (!sheet) {
            rail();
        }
        header();
        body();
        footer();

        bodyT = Ui.ease(bodyT, 1f, dt, 70f);
        scroll = Ui.ease(scroll, scrollTarget, dt, 55f);
    }

    public int tab() {
        return tab;
    }

    public List<Placed> placed() {
        return placed;
    }

    /** 這一頁是不是整頁改成兩列了。 */
    public boolean pageWide() {
        return pageWide;
    }

    public float[] window() {
        return new float[] {winX, winY, winW, winH};
    }

    public boolean popoverOpen() {
        return pickerOpen || openSelect != null;
    }

    /** 給測試用：直接把搜尋字設好。 */
    public void searchFor(String text) {
        search.set(text);
        onSearchChanged();
    }

    /** 給測試用：切到第幾個分類，不播動畫。 */
    public void showTab(int index) {
        tab = Math.max(0, Math.min(tabs.size() - 1, index));
        search.clear();
        scroll = 0;
        scrollTarget = 0;
        bodyT = 1f;
        settle();
    }

    // ------------------------------------------------------------ 可以點的地方

    // ------------------------------------------------------------ 輸入

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
            boolean changed = focus.key(key, ctrl);
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

    private boolean searching() {
        return search.text.toString().strip().length() > 0;
    }

    // ------------------------------------------------------------ 繪製

    // ------------------------------------------------------------ 側欄

    private void rail() {
        float x = winX;
        float y = winY;
        boolean slim = railW < RAIL_W;
        // 側欄比視窗再亮一點點；左邊兩個角要跟著視窗圓，所以畫寬一點再裁掉右半
        clip(x, y, x + railW, y + winH);
        c.round(x, y, railW + Ui.R3 * 2, winH, Ui.R3, Ui.RAIL);
        unclip();
        c.fill(x + railW - c.px(), y + 1, x + railW, y + winH - 1, Ui.LINE);

        float cy;
        if (slim) {
            c.logo(x + (railW - 20) / 2f, y + 8, 20);
            cy = y + 36;
        } else {
            c.logo(x + 7, y + 8, 26);
            String title = host.title();
            float room = railW - 38 - 6;
            float big = c.scale(1.25f);
            float s = c.width(title) * big <= room ? big : 1f;
            // 同一行字往右錯開一個點再畫一次：點陣字沒有粗體，這樣筆畫會厚一點，
            // 在圖示旁邊才撐得住
            c.text(title, x + 38, y + 21 - 4 * s, accent, s);
            c.text(title, x + 38 + c.px(), y + 21 - 4 * s, accent, s);
            cy = y + 40;
            for (String line : Ui.wrap(c, host.tagline(), (int) (railW - 16))) {
                c.text(line, x + 8, cy, Ui.TEXT_2);
                cy += 10;
            }
            c.text(host.version(), x + 8, cy, Ui.FAINT);
            cy += 15;
            cy = searchField(x + 7, cy, railW - 14) + 7;
        }

        boolean searching = searching();
        float pillY = animate("nav", cy + tab * (NAV_H + 2), 55f);
        if (!searching) {
            c.round(x + 6, pillY, railW - 12, NAV_H, Ui.R1, Ui.alpha(accent, 0x33));
        }
        for (int i = 0; i < tabs.size(); i++) {
            Row.Tab t = tabs.get(i);
            float ny = cy + i * (NAV_H + 2);
            boolean on = i == tab && !searching;
            boolean hot = over(x + 6, ny, railW - 12, NAV_H);
            float g = glow("nav" + i, hot && !on);
            if (g > 0.01f) {
                c.round(x + 6, ny, railW - 12, NAV_H, Ui.R1, Ui.fade(0x12FFFFFF, g));
            }
            int ink = on ? accent : (hot ? Ui.TEXT : Ui.TEXT_2);
            String name = t.name().get();
            if (slim) {
                c.icon(t.icon(), x + (railW - 12) / 2f, ny + 4.5f, 12, ink);
                if (hot) {
                    tip = new Tip(name, x + railW, ny - 4, 0, NAV_H);
                    hoverRow = "#nav" + i;
                }
            } else {
                c.icon(t.icon(), x + 12, ny + 5, 11, ink);
                String count = String.valueOf(rowCount(t));
                float countW = c.width(count);
                String shown = Ui.fit(c, name, (int) (railW - 12 - 22 - countW - 12));
                c.text(shown, x + 28, ny + 6.5f, ink);
                c.text(count, x + railW - 12 - countW, ny + 6.5f, on ? accent : Ui.FAINT);
                if (hot && !shown.equals(name)) {
                    tip = new Tip(name, x + railW, ny - 4, 0, NAV_H);
                    hoverRow = "#nav" + i;
                }
            }
            int index = i;
            zone(x + 6, ny, railW - 12, NAV_H, (px, py, b) -> switchTab(index));
        }

        // 使用須知：釘在側欄最底下
        float nh = 18;
        float ny = y + winH - 8 - nh;
        float nw = railW - 14;
        float nx = x + 7;
        boolean hot = over(nx, ny, nw, nh);
        float g = glow("notice", hot);
        Ui.halo(c, nx, ny, nw, nh, Ui.R1, accent, g);
        Ui.pane(c, nx, ny, nw, nh, Ui.R1, Ui.RAISED, edge(g));
        String notice = host.tr("notice.button");
        if (slim) {
            c.icon(Icons.Icon.QUEST, nx + (nw - 11) / 2f, ny + 3.5f, 11, Ui.GOLD);
            if (hot) {
                tip = new Tip(notice, x + railW, ny - 4, 0, nh);
                hoverRow = "#notice";
            }
        } else {
            c.icon(Icons.Icon.QUEST, nx + 6, ny + 3.5f, 11, Ui.GOLD);
            c.text(Ui.fit(c, notice, (int) (nw - 26)), nx + 21, ny + 5, Ui.GOLD);
        }
        zone(nx, ny, nw, nh, (px, py, b) -> {
            host.click();
            host.openNotice();
        });
    }

    private static int rowCount(Row.Tab t) {
        int n = 0;
        for (Row.Group g : t.groups()) {
            n += g.rows().size();
        }
        return n;
    }

    /** @return 搜尋框的下緣 */
    private float searchField(float x, float y, float w) {
        float h = 18;
        boolean focused = focus == search;
        boolean active = focused || search.text.length() > 0;
        float g = glow("search", focused || over(x, y, w, h));
        Ui.halo(c, x, y, w, h, Ui.R1, accent, focused ? 1f : g * 0.6f);
        Ui.pane(c, x, y, w, h, Ui.R1, Ui.FIELD, active ? accent : edge(g));
        c.icon(Icons.Icon.SEARCH, x + 5, y + 4, 10, active ? accent : Ui.TEXT_3);
        float textX = x + 19;
        float textW = w - 19 - (search.text.length() > 0 ? 15 : 5);
        if (search.text.length() == 0 && !focused) {
            c.text(Ui.fit(c, host.tr("search.placeholder"), (int) textW), textX, y + 5, Ui.FAINT);
        } else {
            field(search, textX, y + 5, textW, focused);
        }
        zone(x, y, w, h, (px, py, b) -> {
            closePopovers();
            focus = search;
            search.caret = search.text.length();
        });
        if (search.text.length() > 0) {
            float cx = x + w - 14;
            boolean ch = over(cx - 1, y + 3, 12, 12);
            c.icon(Icons.Icon.CLOSE, cx + 1, y + 5, 8, ch ? Ui.TEXT : Ui.HINT);
            zone(cx - 1, y + 3, 12, 12, (px, py, b) -> {
                search.clear();
                onSearchChanged();
            });
        }
        return y + h;
    }

    // ------------------------------------------------------------ 頁首

    private void header() {
        float x = winX + railW + 12;
        float right = winX + winW - 12;
        float y = winY;
        boolean searching = searching();

        // 重置本頁：這一頁有東西不是預設值才按得下去
        String reset = host.tr("reset.page");
        int dirty = 0;
        for (Row.Group g : tabs.get(tab).groups()) {
            for (Row r : g.rows()) {
                if (r.resettable() && !r.isDefault.getAsBoolean()) {
                    dirty++;
                }
            }
        }
        boolean can = dirty > 0 && !searching;
        float room = right - x;
        boolean compact = c.width(reset) + 26 > room * 0.45f;
        float rw = compact ? CTRL_H : c.width(reset) + 26;
        float rx = right - rw;
        float ry = y + 11;
        boolean hot = can && over(rx, ry, rw, CTRL_H);
        float g = glow("resetpage", hot);
        Ui.halo(c, rx, ry, rw, CTRL_H, Ui.R1, accent, g);
        Ui.pane(c, rx, ry, rw, CTRL_H, Ui.R1, Ui.RAISED, edge(g));
        int ink = can ? Ui.mix(Ui.TEXT_2, Ui.TEXT, g) : Ui.fade(Ui.FAINT, 0.6f);
        c.icon(Icons.Icon.RESET, rx + (compact ? 3.5f : 6), ry + 3.5f, 9, ink);
        if (!compact) {
            c.text(reset, rx + 19, ry + 4, ink);
        } else if (over(rx, ry, rw, CTRL_H)) {
            tip = new Tip(reset, rx, ry, rw, CTRL_H);
            hoverRow = "#reset";
        }
        zone(rx, ry, rw, CTRL_H, (px, py, b) -> {
            if (!can) {
                return;
            }
            closePopovers();
            for (Row.Group grp : tabs.get(tab).groups()) {
                for (Row r : grp.rows()) {
                    if (r.resettable() && !r.isDefault.getAsBoolean()) {
                        r.reset.run();
                    }
                }
            }
            host.click();
            host.resetDone(tabs.get(tab).name().get());
        });

        float textRoom = rx - 10 - x;
        String title = searching ? host.tr("search.results") : tabs.get(tab).name().get();
        float s = c.scale(1.5f);
        if (c.width(title) * s > textRoom) {
            s = 1f;
        }
        c.text(Ui.fit(c, title, (int) (textRoom / s)), x, y + 8, Ui.TEXT, s);
        String about = searching
                ? host.tr("search.count", shownCount())
                : tabs.get(tab).about().get();
        c.text(Ui.fit(c, about, (int) textRoom), x, y + 8 + 8 * s + 4, Ui.TEXT_3);
    }

    private int shownCount() {
        String query = search.text.toString().strip().toLowerCase(Locale.ROOT);
        int n = 0;
        for (Row.Tab t : tabs) {
            for (Row.Group g : t.groups()) {
                for (Row r : g.rows()) {
                    if (matches(r, query)) {
                        n++;
                    }
                }
            }
        }
        return n;
    }

    // ------------------------------------------------------------ 內容

    private void body() {
        float top = winY + HEAD_H;
        float bottom = winY + winH - FOOT_H - 8;
        float h = bottom - top;
        float left = winX + railW + 12;
        float full = winX + winW - 12 - left;
        float previewW = full >= 380 ? PREVIEW_W : (full >= 320 ? 130 : 0);
        if (h < 190 || sheet) {
            previewW = 0;                      // 矮到這樣示意圖裡的東西會疊在一起，乾脆不畫
        }
        float listW = previewW > 0 ? full - previewW - 8 : full;
        list(left, top, listW, h);
        if (previewW > 0) {
            preview(left + listW + 8, top, previewW, h);
        }
    }

    /** 一張卡片：一組設定與這次要顯示的那幾列。 */
    private record Card(Row.Group group, List<Row> rows, String tabName) {}

    private void list(float x, float y, float w, float h) {
        String query = search.text.toString().strip().toLowerCase(Locale.ROOT);
        boolean searching = !query.isEmpty();

        List<Card> cards = new ArrayList<>();
        int shown = 0;
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
                if (!rows.isEmpty()) {
                    shown += rows.size();
                    cards.add(new Card(g, rows, tabs.get(t).name().get()));
                }
            }
        }

        if (searching && shown == 0) {
            Ui.pane(c, x, y, w, 34, Ui.R2, Ui.CARD, Ui.LINE);
            String none = Ui.fit(c, host.tr("search.none", search.text.toString().strip()),
                                 (int) (w - 16));
            c.text(none, x + (w - c.width(none)) / 2f, y + 13, Ui.TEXT_3);
            pageWide = false;
            maxScroll = 0;
            return;
        }

        // 捲軸那一條永遠留著位置：出不出現會改變列寬，列寬又決定要不要換成兩列，
        // 留著就不會有「多一條捲軸 → 變兩列 → 更長」這種來回跳
        float cardW = w - 6;
        float rowW = cardW - 4;
        pageWide = measure(cards, rowW - 14, searching);
        float contentH = 0;
        for (Card card : cards) {
            contentH += cardHeight(card) + 6;
        }
        contentH = Math.max(0, contentH - 6);
        if (sheet) {
            sheetH = HEAD_H + contentH + 8 + FOOT_H + 1;
        }
        maxScroll = Math.max(0, contentH - h);
        scrollTarget = clampScroll(scrollTarget);
        scroll = clampScroll(scroll);

        clip(x, y, x + w, y + h);
        // 換分類時整塊往上滑進來
        float cy = y - scroll + (1f - bodyT) * 7f;
        for (Card card : cards) {
            float cardH = cardHeight(card);
            if (cy + cardH >= y && cy < y + h) {
                Row.Group g = card.group();
                Ui.pane(c, x, cy, cardW, cardH, Ui.R2, Ui.CARD,
                        g.tool() ? Ui.alpha(accent, 0x66) : Ui.LINE);
                String count = String.valueOf(card.rows().size());
                String title = Ui.fit(c, g.title().get(), (int) (cardW - 34));
                c.text(title, x + 9, cy + 6, g.tool() ? accent : Ui.TEXT_3);
                float lineL = x + 9 + c.width(title) + 6;
                float lineR = x + cardW - 12 - c.width(count);
                if (lineR > lineL) {
                    c.fill(lineL, cy + 9.5f, lineR, cy + 9.5f + c.px(), Ui.LINE);
                }
                c.text(count, x + cardW - 8 - c.width(count), cy + 6, Ui.FAINT);
                float ry = cy + GROUP_HEAD + 1;
                for (Row r : card.rows()) {
                    float rh = rowHeight(r);
                    if (ry + rh >= y && ry < y + h) {
                        row(r, x + 2, ry, rowW, query, searching ? card.tabName() : null);
                    }
                    ry += rh;
                }
            }
            cy += cardH + 6;
        }
        unclip();

        // 換分類的淡入：蓋一層跟視窗同色、慢慢變透明的布
        if (bodyT < 0.99f) {
            c.fill(x, y, x + w, y + h, Ui.fade(0xC8181F2A, 1f - bodyT));
        }
        if (maxScroll > 0) {
            float trackX = x + w - 3;
            c.round(trackX, y, 2.5f, h, 1.25f, Ui.LINE);
            float thumbH = Math.max(14, h * h / Math.max(1f, contentH));
            float thumbY = y + (h - thumbH) * (scroll / maxScroll);
            c.round(trackX, thumbY, 2.5f, thumbH, 1.25f, Ui.alpha(accent, 0xC8));
        }
    }

    private static boolean matches(Row r, String query) {
        return r.name.get().toLowerCase(Locale.ROOT).contains(query)
                || r.hint.get().toLowerCase(Locale.ROOT).contains(query);
    }

    private float cardHeight(Card card) {
        float h = GROUP_HEAD + 4;
        for (Row r : card.rows()) {
            h += rowHeight(r);
        }
        return h;
    }

    private float rowHeight(Row r) {
        return wide(r) ? ROW_WIDE_H : ROW_H;
    }

    /** 這一列是不是「名稱一列、控制項一列」。 */
    private boolean wide(Row r) {
        return pageWide && stretches(r.kind);
    }

    /** 整頁換成兩列時，會跟著換的那三種控制項。 */
    private static boolean stretches(Row.Kind kind) {
        return kind == Row.Kind.SEGMENT || kind == Row.Kind.SLIDER || kind == Row.Kind.SELECT;
    }

    /**
     * 這一頁要不要整頁改成兩列。
     *
     * @param inner 一列裡名稱加控制項總共能用多寬
     */
    private boolean measure(List<Card> cards, float inner, boolean searching) {
        // 同一頁的滑桿，數值那一欄留一樣寬：不然「1 秒」「6」「6°」各自留各自的，
        // 幾條軌道的左緣就參差不齊
        sliderValueW = 0;
        for (Card card : cards) {
            for (Row r : card.rows()) {
                if (r.kind == Row.Kind.SLIDER) {
                    sliderValueW = Math.max(sliderValueW, ownValueWidth(r));
                }
            }
        }
        for (Card card : cards) {
            for (Row r : card.rows()) {
                if (!stretches(r.kind)) {
                    continue;
                }
                float label = c.width(r.name.get()) + 8;
                if (searching) {
                    label += c.width(card.tabName()) + 13;
                }
                if (label + 10 + natural(r) > inner) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 控制項照自己的內容排，需要多寬。 */
    private float natural(Row r) {
        switch (r.kind) {
            case TOGGLE: {
                String on = r.onText != null ? r.onText.get() : host.tr("mode.on");
                return 26 + 5 + Math.max(c.width(on), c.width(host.tr("mode.off")));
            }
            case SEGMENT: {
                float widest = 0;
                for (Row.Option o : r.options) {
                    widest = Math.max(widest, c.width(o.label()));
                }
                float w = Math.max(1, r.options.size()) * (widest + 10) + 2;
                if (r.extra != null) {
                    w += 5 + c.width(r.extraLabel.get()) + 12;
                }
                return w;
            }
            case SLIDER:
                return 96 + 6 + valueWidth(r) + (r.resettable() ? 5 + CTRL_H : 0);
            case COLOUR:
                return swatchWidth() + (r.resettable() ? 5 + CTRL_H : 0);
            case SELECT:
                return r.apply != null
                        ? 120 + 5 + c.width(host.tr("button.apply")) + 14
                        : 160;
            default:
                return c.width(r.label.get()) + 22;
        }
    }

    /** 這一頁滑桿的數值欄要留多寬；{@link #measure} 每一幀先算好。 */
    private float sliderValueW;

    private float valueWidth(Row r) {
        return Math.max(sliderValueW, ownValueWidth(r));
    }

    private float ownValueWidth(Row r) {
        return Math.max(c.width(r.format.apply(r.max)),
                        Math.max(c.width(r.format.apply(r.min)),
                                 c.width(r.format.apply(r.value.getAsInt()))));
    }

    private float swatchWidth() {
        return 4 + 12 + 5 + c.width("#MMMMMM") + 5 + 7 + 4;
    }

    // ------------------------------------------------------------ 一列

    private void row(Row r, float x, float y, float w, String query, String chip) {
        boolean wide = wide(r);
        float h = wide ? ROW_WIDE_H : ROW_H;
        boolean hot = over(x, y, w, h);
        float g = glow("row:" + r.id, hot);
        if (g > 0.01f) {
            c.round(x, y, w, h, Ui.R1, Ui.fade(0x0EFFFFFF, g));
        }
        float labelX = x + 7;
        float right = x + w - 7;
        float labelY = wide ? y + 6 : y + 7;
        float ctrlY = wide ? y + 20 : y + (ROW_H - CTRL_H) / 2f;
        float ctrlLeft;
        float labelRoom;
        if (wide) {
            ctrlLeft = labelX;
            control(r, labelX, right, ctrlY);
            labelRoom = right - labelX;
        } else {
            // 名稱至少留三分之一；控制項再長也只能吃掉剩下的
            float span = right - labelX;
            float room = span - Math.min(span / 3f, c.width(r.name.get()) + 16);
            ctrlLeft = right - Math.min(natural(r), room);
            control(r, ctrlLeft, right, ctrlY);
            labelRoom = ctrlLeft - 8 - labelX;
        }

        float tx = labelX;
        if (chip != null) {
            String tag = Ui.fit(c, chip, (int) Math.max(0, labelRoom / 3));
            if (!tag.isEmpty()) {
                float tw = c.width(tag) + 8;
                Ui.pane(c, tx, labelY - 2.5f, tw, 12, 3, 0, Ui.BORDER);
                c.text(tag, tx + 4, labelY, Ui.FAINT);
                tx += tw + 5;
                labelRoom -= tw + 5;
            }
        }
        boolean changed = r.resettable() && !r.isDefault.getAsBoolean();
        String full = r.name.get();
        String name = Ui.fit(c, full, (int) (labelRoom - (changed ? 8 : 0)));
        int ink = Ui.mix(Ui.TEXT_2, Ui.TEXT, g);
        int hit = query.isEmpty() ? -1 : name.toLowerCase(Locale.ROOT).indexOf(query);
        if (hit >= 0 && hit + query.length() <= name.length()) {
            // 搜尋命中的那幾個字反白
            String pre = name.substring(0, hit);
            String mid = name.substring(hit, hit + query.length());
            float px = tx + c.width(pre);
            c.round(px - 1, labelY - 2, c.width(mid) + 2, 12, 2, accent);
            c.text(pre, tx, labelY, ink);
            c.text(mid, px, labelY, Ui.ON_ACCENT);
            c.text(name.substring(hit + query.length()), px + c.width(mid), labelY, ink);
        } else {
            c.text(name, tx, labelY, ink);
        }
        float labelRight = tx + c.width(name);
        if (changed) {
            // 改過、不是預設值的記號
            c.round(labelRight + 4, labelY + 2, 4, 4, 2, Ui.GOLD);
            labelRight += 8;
        }
        if (hot && tip == null) {
            hoverRow = r.id;
            // 名稱被截斷的時候，說明的第一行補上完整的名稱
            String hint = r.hint.get();
            tip = new Tip(name.equals(full) ? hint : full + "\n" + hint, x, y, w, h);
        }
        placed.add(new Placed(r.id, x, y, w, h, labelRight, ctrlLeft, right, wide));
    }

    /** 把控制項畫在 {@code [left, right]} 這一段裡。 */
    private void control(Row r, float left, float right, float y) {
        switch (r.kind) {
            case TOGGLE -> toggle(r, right, y);
            case SEGMENT -> segment(r, left, right, y);
            case SLIDER -> slider(r, left, right, y);
            case COLOUR -> colour(r, right, y);
            case SELECT -> select(r, left, right, y);
            case ACTION -> action(r, left, right, y);
            default -> status(r, left, right, y);
        }
    }

    // ---- 開關

    private void toggle(Row r, float right, float y) {
        boolean on = r.on.getAsBoolean();
        float t = animate("t:" + r.id, on ? 1f : 0f, 55f);
        float w = 26;
        float h = 14;
        float x = right - w;
        float ty = y + 1;
        boolean hot = over(x - 2, y - 2, w + 4, CTRL_H + 4);
        float g = glow("tg:" + r.id, hot);
        Ui.halo(c, x, ty, w, h, h / 2f, accent, g);
        c.round(x, ty, w, h, h / 2f, Ui.mix(Ui.FIELD, Ui.alpha(accent, 0x66), t));
        c.ring(x, ty, w, h, h / 2f, c.px(), Ui.mix(Ui.TRACK_OFF, accent, t));
        float knob = 10;
        float kx = x + 2 + (w - 4 - knob) * t;
        c.round(kx, ty + 2, knob, knob, knob / 2f, Ui.mix(Ui.KNOB_OFF, accent, t));
        zone(x - 2, y - 2, w + 4, CTRL_H + 4, (px, py, b) -> {
            host.click();
            r.flip.run();
        });
        String text = on ? (r.onText != null ? r.onText.get() : host.tr("mode.on"))
                         : host.tr("mode.off");
        c.text(text, x - 5 - c.width(text), y + 4, Ui.mix(Ui.HINT, accent, t));
    }

    // ---- 分段

    private void segment(Row r, float left, float right, float y) {
        float end = right;
        if (r.extra != null) {
            String label = r.extraLabel.get();
            float bw = c.width(label) + 12;
            button("x:" + r.id, end - bw, y, bw, CTRL_H, label, true, false, null, r.extra);
            end -= bw + 5;
        }
        int n = Math.max(1, r.options.size());
        float w = end - left;
        float inner = w - 2;
        float[] cellX = new float[n + 1];
        float widest = 0;
        float sum = 0;
        for (Row.Option o : r.options) {
            widest = Math.max(widest, c.width(o.label()));
            sum += c.width(o.label()) + 12;
        }
        // 等寬排得下就等寬；排不下改成照字數分，長的那一格多拿一點
        boolean even = widest + 6 <= inner / n;
        cellX[0] = left + 1;
        for (int i = 0; i < n; i++) {
            float cw = even || r.options.isEmpty() ? inner / n
                    : inner * (c.width(r.options.get(i).label()) + 12) / Math.max(1f, sum);
            cellX[i + 1] = cellX[i] + cw;
        }
        int at = Math.max(0, Math.min(n - 1, r.selected.getAsInt()));
        boolean hotAny = over(left, y, w, CTRL_H);
        float g = glow("sg:" + r.id, hotAny);
        Ui.halo(c, left, y, w, CTRL_H, Ui.R1, accent, g * 0.6f);
        Ui.pane(c, left, y, w, CTRL_H, Ui.R1, Ui.FIELD,
                Ui.mix(Ui.BORDER, Ui.alpha(accent, 0x70), g));
        Row.Tone tone = r.options.isEmpty() ? Row.Tone.PLAIN : r.options.get(at).tone();
        // 滑塊：位置與寬度各自往選到的那一格靠過去
        float tx = animate("sx:" + r.id, cellX[at] - left, 60f);
        float tw = animate("sw:" + r.id, cellX[at + 1] - cellX[at], 60f);
        c.round(left + tx, y + 1.5f, tw, CTRL_H - 3, Ui.R1 - 1.5f,
                colourTo("sc:" + r.id, host.tone(tone)));
        for (int i = 0; i < r.options.size(); i++) {
            Row.Option o = r.options.get(i);
            float cx = cellX[i];
            float cw = cellX[i + 1] - cellX[i];
            boolean on = i == at;
            boolean hot = !on && over(cx, y, cw, CTRL_H);
            String text = Ui.fit(c, o.label(), (int) (cw - 4));
            int ink = on ? (tone == Row.Tone.OFF ? Ui.TEXT : Ui.ON_ACCENT)
                         : (hot ? Ui.TEXT : Ui.TEXT_3);
            c.text(text, cx + (cw - c.width(text)) / 2f, y + 4, ink);
            int index = i;
            zone(cx, y, cw, CTRL_H, (px, py, b) -> {
                if (index != r.selected.getAsInt()) {
                    host.click();
                    r.pick.accept(index);
                }
            });
        }
    }

    // ---- 滑桿

    private float resetButton(Row r, float right, float y) {
        float x = right - CTRL_H;
        boolean can = !r.isDefault.getAsBoolean();
        boolean hot = can && over(x, y, CTRL_H, CTRL_H);
        float g = glow("rs:" + r.id, hot);
        Ui.halo(c, x, y, CTRL_H, CTRL_H, Ui.R1, accent, g);
        Ui.pane(c, x, y, CTRL_H, CTRL_H, Ui.R1, Ui.RAISED, edge(g));
        c.icon(Icons.Icon.RESET, x + 3.5f, y + 3.5f, 9,
               can ? Ui.mix(Ui.TEXT_2, Ui.TEXT, g) : Ui.fade(Ui.FAINT, 0.6f));
        if (over(x, y, CTRL_H, CTRL_H)) {
            tip = new Tip(host.tr("reset.row"), x, y, CTRL_H, CTRL_H);
            hoverRow = "#rs:" + r.id;
        }
        zone(x, y, CTRL_H, CTRL_H, (px, py, b) -> {
            if (can) {
                host.click();
                r.reset.run();
                host.resetDone(r.name.get());
            }
        });
        return x;
    }

    private void slider(Row r, float left, float right, float y) {
        float end = r.resettable() ? resetButton(r, right, y) - 5 : right;
        int value = r.value.getAsInt();
        String text = r.format.apply(value);
        float textW = valueWidth(r);
        c.text(text, end - c.width(text), y + 4, Ui.TEXT);
        end -= textW + 6;
        float x = left + 5;
        float w = Math.max(30, end - x - 5);
        int span = Math.max(1, r.max - r.min);
        float t = (Math.max(r.min, Math.min(r.max, value)) - r.min) / (float) span;
        String key = "d:" + r.id;
        boolean dragged = dragging != null && key.equals(draggingKey);
        boolean hot = over(x - 5, y, w + 10, CTRL_H) || dragged;
        float g = glow("sl:" + r.id, hot);
        float ty = y + CTRL_H / 2f - 1.5f;
        c.round(x, ty, w, 3, 1.5f, Ui.TRACK_OFF);
        float knobX = x + w * t;
        c.round(x, ty, Math.max(3, knobX - x), 3, 1.5f, accent);
        float size = 9 + g * 1.5f;
        Ui.halo(c, knobX - size / 2f, y + (CTRL_H - size) / 2f, size, size, size / 2f, accent, g);
        c.round(knobX - size / 2f, y + (CTRL_H - size) / 2f, size, size, size / 2f, 0xFFFFFFFF);
        c.round(knobX - 2, y + CTRL_H / 2f - 2, 4, 4, 2, accent);
        float fx = x;
        float fw = w;
        Zone z = zone(x - 6, y - 2, w + 12, CTRL_H + 4, (px, py, b) -> slideTo(r, fx, fw, px));
        z.drag = (px, py, b) -> slideTo(r, fx, fw, px);
        z.release = r.commit;
        z.key = key;
    }

    private void slideTo(Row r, float x, float w, double px) {
        float t = (float) Math.max(0, Math.min(1, (px - x) / Math.max(1f, w)));
        int next = r.min + Math.round(t * (r.max - r.min));
        if (next != r.value.getAsInt()) {
            r.slide.accept(next);
        }
    }

    // ---- 顏色

    private void colour(Row r, float right, float y) {
        float end = r.resettable() ? resetButton(r, right, y) - 5 : right;
        String hex = r.hex.get();
        float w = swatchWidth();
        float x = end - w;
        boolean open = pickerOpen && pickerRow == r;
        boolean hot = over(x, y, w, CTRL_H);
        float g = glow("c:" + r.id, hot || open);
        Ui.halo(c, x, y, w, CTRL_H, Ui.R1, accent, g);
        Ui.pane(c, x, y, w, CTRL_H, Ui.R1, Ui.FIELD, Ui.mix(Ui.BORDER, accent, g));
        Ui.pane(c, x + 4, y + 3.5f, 12, 9, 2.5f, Ui.parseHex(hex, accent), 0x66FFFFFF);
        c.text(hex, x + 21, y + 4, Ui.TEXT);
        c.icon(open ? Icons.Icon.UP : Icons.Icon.DOWN, x + w - 11, y + 4.5f, 7,
               open ? accent : Ui.TEXT_3);
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
                anim.put("pop", 0f);
            }
        });
        if (open) {
            float ax = x + w;
            popover = () -> picker(r, ax, y + CTRL_H + 3, y - 3);
        }
    }

    private static final String[] PRESETS = {
        "#6FA8D8", "#8FD694", "#E0BC6E", "#E0706B", "#B48EDE", "#5FC8C0", "#F0A35E", "#C5CFDA"};

    /** 彈出的色盤：上面一塊飽和×明度，下面一條色相，再來幾個預設色與色碼。 */
    private void picker(Row r, float anchorRight, float below, float above) {
        float w = 156;
        float svH = 70;
        float h = 8 + 10 + 6 + svH + 6 + 8 + 6 + 12 + 6 + CTRL_H + 8;
        float x = Math.max(winX + railW + 6, Math.min(winX + winW - w - 6, anchorRight - w));
        float y = above - h >= winY + 6 ? above - h : Math.min(below, winY + winH - h - 6);
        float in = animate("pop", 1f, 45f);
        y += (1f - in) * 4f;
        popPane(x, y, w, h, in);
        // 吃掉點在色盤空白處的點擊，不然會被當成「點外面」而收起來
        zone(x, y, w, h, (px, py, b) -> focus = null);

        float sx = x + 8;
        float sw = w - 16;
        float cy = y + 8;
        c.text(Ui.fit(c, r.name.get(), (int) (sw - 14)), sx, cy + 1, Ui.fade(Ui.TEXT_3, in));
        boolean closeHot = over(sx + sw - 11, cy - 1, 12, 12);
        c.icon(Icons.Icon.CLOSE, sx + sw - 9, cy + 1, 8, closeHot ? Ui.TEXT : Ui.HINT);
        zone(sx + sw - 11, cy - 1, 12, 12, (px, py, b) -> closePopovers());
        cy += 16;

        float sy = cy;
        c.svSquare(sx, sy, sw, svH, hsv[0]);
        c.ring(sx, sy, sw, svH, 2, c.px(), Ui.BORDER);
        float hx = sx + hsv[1] * sw;
        float hy = sy + (1f - hsv[2]) * svH;
        c.ring(hx - 4.5f, hy - 4.5f, 9, 9, 4.5f, 1f, Ui.ON_ACCENT);
        c.ring(hx - 3.5f, hy - 3.5f, 7, 7, 3.5f, 1f, 0xFFFFFFFF);
        Zone sv = zone(sx, sy, sw, svH, (px, py, b) -> pickSv(r, sx, sy, sw, svH, px, py));
        sv.drag = (px, py, b) -> pickSv(r, sx, sy, sw, svH, px, py);
        sv.release = r.commit;
        sv.key = "sv";
        cy += svH + 6;

        float by = cy;
        c.hueBar(sx, by, sw, 8);
        c.ring(sx, by, sw, 8, 2, c.px(), Ui.BORDER);
        float kx = sx + hsv[0] / 360f * sw;
        c.round(kx - 2.5f, by - 2, 5, 12, 2.5f, 0xFFFFFFFF);
        c.ring(kx - 2.5f, by - 2, 5, 12, 2.5f, c.px(), Ui.ON_ACCENT);
        Zone hue = zone(sx, by - 2, sw, 12, (px, py, b) -> pickHue(r, sx, sw, px));
        hue.drag = (px, py, b) -> pickHue(r, sx, sw, px);
        hue.release = r.commit;
        hue.key = "hue";
        cy += 8 + 6;

        float step = (sw + 3) / PRESETS.length;
        String current = r.hex.get();
        for (int i = 0; i < PRESETS.length; i++) {
            String p = PRESETS[i];
            float px0 = sx + i * step;
            boolean on = p.equalsIgnoreCase(current);
            boolean hot = over(px0, cy, step - 3, 12);
            Ui.pane(c, px0, cy, step - 3, 12, 3, Ui.parseHex(p, accent),
                    on ? 0xFFFFFFFF : (hot ? Ui.TEXT_2 : 0x40FFFFFF));
            zone(px0, cy, step - 3, 12, (qx, qy, b) -> {
                host.click();
                if (r.setHex.test(p)) {
                    float[] next = Ui.toHsv(Ui.parseHex(p, accent));
                    System.arraycopy(next, 0, hsv, 0, 3);
                    hexField.set(p);
                    r.commit.run();
                }
            });
        }
        cy += 12 + 6;

        Ui.pane(c, sx, cy, CTRL_H, CTRL_H, Ui.R1, Ui.parseHex(current, accent), 0x66FFFFFF);
        String def = host.tr("reset.default");
        float dw = Math.min(c.width(def) + 12, sw - CTRL_H - 5 - 50 - 5);
        float fx = sx + CTRL_H + 5;
        float fw = sw - CTRL_H - 5 - dw - 5;
        boolean focused = focus == hexField;
        Ui.pane(c, fx, cy, fw, CTRL_H, Ui.R1, Ui.FIELD, focused ? accent : Ui.BORDER);
        field(hexField, fx + 5, cy + 4, fw - 10, focused);
        zone(fx, cy, fw, CTRL_H, (qx, qy, b) -> {
            focus = hexField;
            hexField.caret = hexField.text.length();
            hexField.all = true;
        });
        button("pd", sx + sw - dw, cy, dw, CTRL_H, def,
               r.resettable() && !r.isDefault.getAsBoolean(), false, null, () -> {
                   r.reset.run();
                   float[] next = Ui.toHsv(Ui.parseHex(r.hex.get(), accent));
                   System.arraycopy(next, 0, hsv, 0, 3);
                   hexField.set(r.hex.get());
               });
    }

    private void pickSv(Row r, float sx, float sy, float sw, float sh, double px, double py) {
        hsv[1] = (float) Math.max(0, Math.min(1, (px - sx) / Math.max(1f, sw)));
        hsv[2] = 1f - (float) Math.max(0, Math.min(1, (py - sy) / Math.max(1f, sh)));
        applyHsv(r);
    }

    private void pickHue(Row r, float sx, float sw, double px) {
        hsv[0] = 359.9f * (float) Math.max(0, Math.min(1, (px - sx) / Math.max(1f, sw)));
        applyHsv(r);
    }

    private void applyHsv(Row r) {
        String hex = Ui.toHex(Ui.hsv(hsv[0], hsv[1], hsv[2]));
        if (r.setHex.test(hex)) {
            hexField.set(hex);
        }
    }

    // ---- 下拉

    private void select(Row r, float left, float right, float y) {
        float end = right;
        boolean locked = r.locked.getAsBoolean();
        if (r.apply != null) {
            String label = host.tr("button.apply");
            float bw = c.width(label) + 14;
            boolean can = !locked && r.canApply.getAsBoolean();
            button("ap:" + r.id, end - bw, y, bw, CTRL_H, label, can, can, null, r.apply);
            end -= bw + 5;
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
        float x = left;
        float w = Math.max(50, end - left);
        boolean open = r.id.equals(openSelect);
        boolean hot = !locked && over(x, y, w, CTRL_H);
        float g = glow("s:" + r.id, hot || open);
        Ui.halo(c, x, y, w, CTRL_H, Ui.R1, accent, g);
        Ui.pane(c, x, y, w, CTRL_H, Ui.R1, Ui.FIELD, Ui.mix(Ui.BORDER, accent, g));
        String busy = r.busy.get();
        float tx = x + 5;
        if (cur != null && busy == null) {
            tx = badge(cur, x + 3, y + 3, true);
        }
        String text = busy != null ? busy : (cur == null ? "" : cur.label());
        c.text(Ui.fit(c, text, (int) (x + w - 14 - tx)), tx, y + 4, locked ? Ui.HINT : Ui.TEXT);
        c.icon(open ? Icons.Icon.UP : Icons.Icon.DOWN, x + w - 11, y + 4.5f, 7,
               open ? accent : Ui.TEXT_3);
        zone(x, y, w, CTRL_H, (px, py, b) -> {
            if (locked) {
                return;
            }
            host.click();
            boolean was = r.id.equals(openSelect);
            closePopovers();
            if (!was) {
                openSelect = r.id;
                anim.put("pop", 0f);
            }
        });
        if (open) {
            float fw = w;
            popover = () -> dropdown(r, list, id, x, y + CTRL_H + 3, y - 3, fw);
        }
    }

    /**
     * 語言的小徽章。
     *
     * <p>用代碼的字而不是國旗：語言不等於國家，同一面旗底下不只一種語言，
     * 同一種語言也不只一面旗——而且旗子畫成十像素見方誰也認不出來。
     *
     * @return 徽章之後字該從哪裡開始
     */
    private float badge(Row.Choice ch, float x, float y, boolean on) {
        String text = ch.badge();
        if (text == null || text.isEmpty()) {
            return x + 2;
        }
        float w = Math.max(14, c.width(text) + 5);
        int bg = ch.auto() ? Ui.GOLD : (on ? accent : Ui.HINT);
        c.round(x, y, w, 10, 3, bg);
        c.text(text, x + (w - c.width(text)) / 2f, y + 1, Ui.ON_ACCENT);
        return x + w + 5;
    }

    private void dropdown(Row r, List<Row.Choice> list, String id, float ax, float below,
                          float above, float minW) {
        float itemH = 15;
        float w = minW;
        for (Row.Choice ch : list) {
            w = Math.max(w, c.width(ch.label()) + 48);
        }
        w = Math.min(w, winW - railW - 16);
        float h = list.size() * itemH + 6;
        float x = Math.max(winX + railW + 6, Math.min(winX + winW - w - 6, ax));
        float y = below + h <= winY + winH - 6 ? below : Math.max(winY + 6, above - h);
        float in = animate("pop", 1f, 45f);
        y += (1f - in) * 4f;
        popPane(x, y, w, h, in);
        zone(x, y, w, h, (px, py, b) -> { });
        for (int i = 0; i < list.size(); i++) {
            Row.Choice ch = list.get(i);
            float iy = y + 3 + i * itemH;
            boolean on = ch.id().equals(id);
            boolean hot = over(x + 3, iy, w - 6, itemH);
            if (hot) {
                c.round(x + 3, iy, w - 6, itemH, 3.5f, Ui.alpha(accent, 0x38));
            }
            float tx = badge(ch, x + 6, iy + 2.5f, on);
            c.text(Ui.fit(c, ch.label(), (int) (x + w - 18 - tx)), tx, iy + 3.5f,
                   Ui.fade(on || hot ? Ui.TEXT : Ui.TEXT_2, in));
            if (on) {
                c.icon(Icons.Icon.CHECK, x + w - 15, iy + 3.5f, 8, accent);
            }
            zone(x + 3, iy, w - 6, itemH, (px, py, b) -> {
                host.click();
                openSelect = null;
                r.choose.accept(ch.id());
            });
        }
    }

    // ---- 動作與狀態

    private void action(Row r, float left, float right, float y) {
        String label = r.label.get();
        float w = Math.min(c.width(label) + 22, right - left);
        button("a:" + r.id, right - w, y, w, CTRL_H, label, r.enabled.getAsBoolean(), false,
               Icons.Icon.RIGHT, r.run);
    }

    private void status(Row r, float left, float right, float y) {
        String label = r.label.get();
        int ink = r.labelColour != null ? r.labelColour.getAsInt() : Ui.TEXT;
        float w = Math.min(c.width(label) + 22, right - left);
        float x = right - w;
        boolean hot = over(x, y, w, CTRL_H);
        float g = glow("st:" + r.id, hot);
        Ui.halo(c, x, y, w, CTRL_H, Ui.R1, accent, g);
        Ui.pane(c, x, y, w, CTRL_H, Ui.R1, Ui.FIELD, edge(g));
        String text = Ui.fit(c, label, (int) (w - 18));
        float tx = x + (w - c.width(text) - 9) / 2f;
        c.round(tx, y + 5.5f, 5, 5, 2.5f, ink);
        c.text(text, tx + 9, y + 4, ink);
        zone(x, y, w, CTRL_H, (px, py, b) -> {
            host.click();
            r.run.run();
        });
    }

    // ------------------------------------------------------------ 預覽

    private void preview(float x, float y, float w, float h) {
        Row.Tab t = tabs.get(searching() ? 0 : tab);
        String title = host.tr("preview.title");
        // 標題前面一顆會呼吸的小點：這一塊是活的
        float pulse = 0.55f + 0.45f * (float) Math.sin(now / 420.0);
        c.round(x, y + 2, 4, 4, 2, Ui.fade(accent, pulse));
        String shown = Ui.fit(c, title, (int) (w - 10));
        c.text(shown, x + 8, y, Ui.TEXT_3);
        float lineL = x + 8 + c.width(shown) + 6;
        if (x + w > lineL) {
            c.fill(lineL, y + 3.5f, x + w, y + 3.5f + c.px(), Ui.LINE);
        }
        Preview.State state = host.preview();
        if (gapShown < 0) {
            gapShown = state.gap;
        }
        gapShown = Ui.ease(gapShown, state.gap, dt, 45f);
        Preview.draw(c, Math.round(x), Math.round(y + 13), Math.round(w), Math.round(h - 13),
                     t.scene(), state, 0xFF000000 | (host.frame() & 0xFFFFFF), accent, gapShown);
    }

    // ------------------------------------------------------------ 底列

    private void footer() {
        float y = winY + winH - FOOT_H;
        float left = winX + railW + 12;
        float right = winX + winW - 12;
        c.fill(winX + railW + (sheet ? 1 : 0), y, winX + winW - 1, y + c.px(), Ui.LINE);
        float h = 18;
        float by = y + (FOOT_H - h) / 2f;

        String done = host.tr(sheet ? "button.back" : "button.done");
        float dw = c.width(done) + 28;
        button("done", right - dw, by, dw, h, done, true, true, null, host::done);
        right -= dw + 5;

        String credits = host.tr("button.credits");
        float cw = c.width(credits) + 16;
        if (!sheet && right - cw - 60 > left) {
            button("credits", right - cw, by, cw, h, credits, true, false, null,
                   host::openCredits);
            right -= cw + 5;
        }

        boolean fresh = host.hasUpdate();
        String updates = host.tr(fresh ? "button.updates.new" : "button.updates");
        float uw = c.width(updates) + 16;
        if (!sheet && right - uw - 40 > left) {
            float ux = right - uw;
            boolean hot = over(ux, by, uw, h);
            float g = glow("b:updates", hot);
            Ui.halo(c, ux, by, uw, h, Ui.R1, fresh ? Ui.GOLD : accent, g);
            Ui.pane(c, ux, by, uw, h, Ui.R1, Ui.RAISED, fresh ? Ui.GOLD : edge(g));
            c.text(updates, ux + 8, by + 5, fresh ? Ui.GOLD : Ui.mix(Ui.TEXT_2, Ui.TEXT, g));
            zone(ux, by, uw, h, (px, py, b) -> {
                host.click();
                host.openUpdates();
            });
            right = ux - 8;
        }

        Status s = host.status();
        if (s != null && right - left > 30) {
            c.round(left, by + 6.5f, 5, 5, 2.5f, s.colour());
            c.text(Ui.fit(c, s.text(), (int) (right - left - 10)), left + 9, by + 5, s.colour());
        }
    }

    // ------------------------------------------------------------ 說明

    // ------------------------------------------------------------ 輸入框

}
