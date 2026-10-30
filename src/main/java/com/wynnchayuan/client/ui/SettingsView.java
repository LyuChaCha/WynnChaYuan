package com.wynnchayuan.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SettingsView extends Surface {

    public interface Host {
        int accent();

        int frame();

        int tone(Row.Tone tone);

        String tr(String key, Object... args);

        String title();

        String tagline();

        String version();

        Status status();

        boolean hasUpdate();

        boolean blurred();

        void openNotice();

        void openUpdates();

        void openCredits();

        void done();

        String clipboard();

        void setClipboard(String text);

        void click();

        Preview.State preview();

        void resetDone(String what);
    }

    public record Status(String text, int colour) {}

    static final float MAX_W = 628;
    static final float MAX_H = 334;
    static final float RAIL_W = 126;
    static final float RAIL_SLIM = 32;
    static final float HEAD_H = 38;
    static final float FOOT_H = 28;
    static final float ROW_H = 22;
    static final float ROW_WIDE_H = 40;
    static final float GROUP_HEAD = 15;
    static final float PREVIEW_W = 152;
    static final float NAV_H = 21;

    final List<Row.Tab> tabs;
    final Host host;

    final SettingsControls controls = new SettingsControls(this);
    final SettingsNavigation navigation = new SettingsNavigation(this);

    int tab;
    private float scroll;
    private float scrollTarget;
    private float maxScroll;

    final Field search = new Field(40);
    final Field hexField = new Field(7);
    Field focus;

    String openSelect;
    boolean pickerOpen;
    Row pickerRow;
    final float[] hsv = {210, 0.5f, 0.85f};

    private float bodyT = 1f;
    private float gapShown = -1;

    float railW;
    private boolean pageWide;

    public record Placed(String id, float x, float y, float w, float h, float labelRight,
                         float ctrlLeft, float ctrlRight, boolean wide) {}

    private final List<Placed> placed = new ArrayList<>();

    final boolean sheet;
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
        float h = Math.min(screenH - 14, sheet ? (sheetH > 0 ? sheetH : 236) : MAX_H);
        window(Math.round((screenW - w) / 2f), Math.round((screenH - h) / 2f), w, h,
               host.blurred());
        railW = sheet ? 0 : (winW >= 430 ? RAIL_W : RAIL_SLIM);

        zone(0, 0, screenW, screenH, (x, y, b) -> {
            closePopovers();
            focus = null;
        });

        if (!sheet) {
            navigation.rail();
        }
        navigation.header();
        body();
        navigation.footer();

        bodyT = Ui.ease(bodyT, 1f, dt, 70f);
        scroll = Ui.ease(scroll, scrollTarget, dt, 55f);
    }

    public int tab() {
        return tab;
    }

    public List<Placed> placed() {
        return placed;
    }

    public boolean pageWide() {
        return pageWide;
    }

    public float[] window() {
        return new float[] {winX, winY, winW, winH};
    }

    public boolean popoverOpen() {
        return pickerOpen || openSelect != null;
    }

    public void searchFor(String text) {
        search.set(text);
        onSearchChanged();
    }

    public float scrollPosition() {
        return scrollTarget;
    }

    public void restoreScroll(float at) {
        scroll = at;
        scrollTarget = at;
        maxScroll = Math.max(maxScroll, at);
    }

    public void showTab(int index) {
        tab = Math.max(0, Math.min(tabs.size() - 1, index));
        search.clear();
        scroll = 0;
        scrollTarget = 0;
        bodyT = 1f;
        settle();
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

    void onSearchChanged() {
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

    void closePopovers() {
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

    void switchTab(int next) {
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

    boolean searching() {
        return search.text.toString().strip().length() > 0;
    }

    private void body() {
        float top = winY + HEAD_H;
        float bottom = winY + winH - FOOT_H - 8;
        float h = bottom - top;
        float left = winX + railW + 12;
        float full = winX + winW - 12 - left;
        float previewW = full >= 380 ? PREVIEW_W : (full >= 320 ? 130 : 0);
        if (h < 190 || sheet) {
            previewW = 0;
        }
        float listW = previewW > 0 ? full - previewW - 8 : full;
        list(left, top, listW, h);
        if (previewW > 0) {
            preview(left + listW + 8, top, previewW, h);
        }
    }

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
        float cy = y - scroll + (1f - bodyT) * 7f;
        for (Card card : cards) {
            float cardH = cardHeight(card);
            if (cy + cardH >= y && cy < y + h) {
                Row.Group g = card.group();
                card(x, cy, cardW, cardH, g.title().get(),
                     card.rows().size() > 1 ? String.valueOf(card.rows().size()) : null, g.tool());
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

    static boolean matches(Row r, String query) {
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

    private boolean wide(Row r) {
        return pageWide && stretches(r.kind);
    }

    private static boolean stretches(Row.Kind kind) {
        return kind == Row.Kind.SEGMENT || kind == Row.Kind.SLIDER || kind == Row.Kind.SELECT;
    }

    private boolean measure(List<Card> cards, float inner, boolean searching) {
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

    private float sliderValueW;

    float valueWidth(Row r) {
        return Math.max(sliderValueW, ownValueWidth(r));
    }

    private float ownValueWidth(Row r) {
        return Math.max(c.width(r.format.apply(r.max)),
                        Math.max(c.width(r.format.apply(r.min)),
                                 c.width(r.format.apply(r.value.getAsInt()))));
    }

    float swatchWidth() {
        return 4 + 12 + 5 + c.width("#MMMMMM") + 5 + 7 + 4;
    }

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
            controls.control(r, labelX, right, ctrlY);
            labelRoom = right - labelX;
        } else {
            float span = right - labelX;
            float room = span - Math.min(span / 3f, c.width(r.name.get()) + 16);
            ctrlLeft = right - Math.min(natural(r), room);
            controls.control(r, ctrlLeft, right, ctrlY);
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
            c.round(labelRight + 4, labelY + 2, 4, 4, 2, Ui.GOLD);
            labelRight += 8;
        }
        if (hot && tip == null) {
            hoverRow = r.id;
            String hint = r.hint.get();
            tip = new Tip(name.equals(full) ? hint : full + "\n" + hint, x, y, w, h);
        }
        placed.add(new Placed(r.id, x, y, w, h, labelRight, ctrlLeft, right, wide));
    }

    private void preview(float x, float y, float w, float h) {
        Row.Tab t = tabs.get(searching() ? 0 : tab);
        String title = host.tr("preview.title");
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
        if (t.scene() == Preview.Scene.CAPE) {
            modelPreview(x, y + 13, w, h - 13);
        }
    }

    private float modelYaw = 205;
    private float modelPitch = -8;
    private double modelDragX;
    private double modelDragY;

    public float modelYaw() {
        return modelYaw;
    }

    public float modelPitch() {
        return modelPitch;
    }

    private void modelPreview(float x, float y, float w, float h) {
        String hint = Ui.fit(c, host.tr("preview.cape.hint"), (int) (w - 12));
        c.text(hint, x + (w - c.width(hint)) / 2, y + h - 14, Ui.TEXT_3);
        Zone z = zone(x + 2, y + 2, w - 4, h - 4, (px, py, b) -> {
            modelDragX = px;
            modelDragY = py;
        });
        z.key = "preview.model";
        z.drag = (px, py, b) -> {
            modelYaw = (float) ((modelYaw - (px - modelDragX) * 1.5) % 360.0);
            modelPitch = (float) Math.max(-35, Math.min(35, modelPitch - (py - modelDragY) * 0.6));
            modelDragX = px;
            modelDragY = py;
        };
    }
}
