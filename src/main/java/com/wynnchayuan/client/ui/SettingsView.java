package com.wynnchayuan.client.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SettingsView extends Surface {

    public interface Host {
        int accent();

        int frame();

        int tone(Row.Tone tone);

        default int toneDeep(Row.Tone tone) {
            return Ui.deep(tone(tone));
        }

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
        openRow = null;
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
        openRow = null;
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

    static final float OPTION_H = 31;
    static final float OPTION_GAP = 3;

    String openRow;

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
                    if (searching) {
                        for (Row d : r.details) {
                            if (matches(d, query)) {
                                rows.add(d);
                            }
                        }
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

        for (Card card : cards) {
            for (Row r : card.rows()) {
                if (r.folds()) {
                    animate("ex:" + r.id, r.id.equals(openRow) && !searching ? 1f : 0f, 55f);
                    animate("tri:" + r.id, r.id.equals(openRow) && !searching ? 1f : 0f, 45f);
                }
            }
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
                group(x, cy, cardW, cardH, g.title().get(),
                      card.rows().size() > 1 ? String.valueOf(card.rows().size()) : null, g.tool());
                float ry = cy + head(card) + 1;
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
            c.fill(x, y, x + w, y + h, Ui.fade(Ui.VEIL, 1f - bodyT));
        }
        if (maxScroll > 0) {
            float trackX = x + w - 3;
            c.round(trackX, y, 2.5f, h, 1.25f, Ui.LINE);
            float thumbH = Math.max(14, h * h / Math.max(1f, contentH));
            float thumbY = y + (h - thumbH) * (scroll / maxScroll);
            c.round(trackX, thumbY, 2.5f, thumbH, 1.25f, Ui.alpha(Ui.TITLE_FROM, 0xC8));
        }
    }

    static boolean matches(Row r, String query) {
        return r.name.get().toLowerCase(Locale.ROOT).contains(query)
                || r.hint.get().toLowerCase(Locale.ROOT).contains(query);
    }

    private static float head(Card card) {
        String title = card.group().title().get();
        return title == null || title.isEmpty() ? 2 : GROUP_HEAD + 4;
    }

    private float cardHeight(Card card) {
        float h = head(card) + 4;
        for (Row r : card.rows()) {
            h += rowHeight(r);
        }
        return h;
    }

    private float rowHeight(Row r) {
        float h = wide(r) ? ROW_WIDE_H : ROW_H;
        if (r.folds()) {
            h += foldHeight(r) * anim.getOrDefault("ex:" + r.id, 0f);
        }
        return h;
    }

    private float foldHeight(Row r) {
        float h = 4 + r.options.size() * OPTION_H + 6;
        for (Row d : r.details) {
            h += rowHeight(d);
        }
        return h;
    }

    private boolean wide(Row r) {
        return pageWide && stretches(r);
    }

    private static boolean stretches(Row r) {
        return (r.kind == Row.Kind.SEGMENT && !r.folds()) || r.kind == Row.Kind.SLIDER
                || r.kind == Row.Kind.SELECT;
    }

    private boolean measure(List<Card> cards, float inner, boolean searching) {
        sliderValueW = 0;
        for (Card card : cards) {
            for (Row r : card.rows()) {
                if (r.kind == Row.Kind.SLIDER) {
                    sliderValueW = Math.max(sliderValueW, ownValueWidth(r));
                }
                for (Row d : r.details) {
                    if (d.kind == Row.Kind.SLIDER) {
                        sliderValueW = Math.max(sliderValueW, ownValueWidth(d));
                    }
                }
            }
        }
        for (Card card : cards) {
            for (Row r : card.rows()) {
                if (fitsBeside(r, card, inner, searching)) {
                    continue;
                }
                return true;
            }
        }
        return false;
    }

    private boolean fitsBeside(Row r, Card card, float inner, boolean searching) {
        if (stretches(r)) {
            float label = c.width(r.name.get()) + 8;
            if (searching) {
                label += c.width(card.tabName()) + 13;
            }
            if (label + 10 + natural(r) > inner) {
                return false;
            }
        }
        for (Row d : r.details) {
            if (stretches(d) && c.width(d.name.get()) + 18 + natural(d) > inner - 8) {
                return false;
            }
        }
        return true;
    }

    private float natural(Row r) {
        switch (r.kind) {
            case TOGGLE: {
                String on = r.onText != null ? r.onText.get() : host.tr("mode.on");
                return 26 + 5 + Math.max(c.width(on), c.width(host.tr("mode.off")));
            }
            case SEGMENT: {
                if (r.folds()) {
                    float chip = 0;
                    for (Row.Option o : r.options) {
                        chip = Math.max(chip, chipWidth(Ui.fit(c, o.label(), 120)));
                    }
                    return chip + 11;
                }
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
            c.round(x, y, w, h, Ui.R1, Ui.fade(0x1A9FD0A6, g));
        }
        float labelX = x + 7;
        float right = x + w - 7;
        float labelY = wide ? y + 6 : y + 7;
        float ctrlY = wide ? y + 20 : y + (ROW_H - CTRL_H) / 2f;
        float ctrlLeft;
        float labelRoom;
        boolean folding = r.folds() && chip == null;
        boolean open = folding && r.id.equals(openRow);
        if (folding) {
            zone(x, y, w, h, (px, py, b) -> {
                click();
                openRow = r.id.equals(openRow) ? null : r.id;
            }).key = "f:" + r.id;
            ctrlLeft = foldChip(r, right, y + (ROW_H - CHIP_H) / 2f);
            labelRoom = ctrlLeft - 8 - labelX;
        } else if (wide) {
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
        int ink = Ui.mix(Ui.TEXT_2, Ui.TEXT, open ? 1f : g);
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
        if (hot && tip == null && !open) {
            hoverRow = r.id;
            String hint = r.hint.get();
            tip = new Tip(name.equals(full) ? hint : full + "\n" + hint, x, y, w, h);
        }
        placed.add(new Placed(r.id, x, y, w, h, labelRight, ctrlLeft, right, wide));
        if (folding) {
            unfolded(r, x, y + h, w, query);
        }
    }

    private int modeColour(Row.Tone tone) {
        return tone == Row.Tone.OFF ? Ui.MODE_OFF : host.tone(tone);
    }

    static final float CHIP_H = 14;
    static final float CHIP_R = 4;

    private float chipWidth(String label) {
        return 14 + c.width(label) + 7;
    }

    private float foldChip(Row r, float right, float y) {
        int at = Math.max(0, Math.min(r.options.size() - 1, r.selected.getAsInt()));
        Row.Option o = r.options.get(at);
        int m = colourTo("chip:" + r.id, modeColour(o.tone()));
        int deep = colourTo("chipd:" + r.id, host.toneDeep(o.tone()));
        float turn = anim.getOrDefault("tri:" + r.id, 0f);
        Ui.triangle(c, right - 3, y + CHIP_H / 2f, 3.2f, turn * 90f,
                    Ui.mix(Ui.TEXT_3, m, 0.5f + turn * 0.5f));
        String label = Ui.fit(c, o.label(), 120);
        float x = Math.round(right - 11 - chipWidth(label));
        float w = Math.round(chipWidth(label));
        Ui.fadePane(c, x, y, w, CHIP_H, CHIP_R, Ui.alpha(deep, 0x7A), 1f, 0.2f);
        c.ring(x, y, w, CHIP_H, CHIP_R, c.px(), Ui.alpha(m, 0x48));
        dot(x + 5, y + (CHIP_H - 6) / 2f, 6, o.tone(), m);
        c.text(label, x + 14, y + (CHIP_H - 8) / 2f, m);
        return x;
    }

    private void dot(float x, float y, float size, Row.Tone tone, int light) {
        if (tone == Row.Tone.OFF) {
            c.ring(x, y, size, size, size / 2f, c.px(), light);
        } else {
            c.round(x, y, size, size, size / 2f, light);
        }
    }

    private void unfolded(Row r, float x, float y, float w, String query) {
        float t = anim.getOrDefault("ex:" + r.id, 0f);
        if (t < 0.01f) {
            return;
        }
        float full = foldHeight(r);
        float shownH = full * t;
        float fade = Math.max(0f, Math.min(1f, (t - 0.25f) / 0.75f));
        clip(x, y, x + w, y + shownH);
        float bx = x + 4;
        float bw = w - 8;
        Ui.pane(c, bx, y, bw, full - 4, 6, Ui.OPTION_BOX, 0);
        int at = r.selected.getAsInt();
        for (int i = 0; i < r.options.size(); i++) {
            Row.Option o = r.options.get(i);
            float oy = y + 4 + i * OPTION_H;
            boolean on = i == at;
            boolean hot = over(bx + 3, oy, bw - 6, OPTION_H - OPTION_GAP);
            int m = modeColour(o.tone());
            int deep = host.toneDeep(o.tone());
            if (on) {
                Ui.fadePane(c, bx + 3, oy, (bw - 6) * 0.85f, OPTION_H - OPTION_GAP, 5,
                            Ui.alpha(deep, 0x5C), 1f, 0f);
            } else if (hot) {
                c.round(bx + 3, oy, bw - 6, OPTION_H - OPTION_GAP, 5, Ui.RAISED);
            }
            float rx = bx + 8;
            if (on) {
                c.ring(rx, oy + 5.5f, 9, 9, 4.5f, c.px(), m);
                c.round(rx + 2, oy + 7.5f, 5, 5, 2.5f, m);
            } else {
                c.ring(rx, oy + 5.5f, 9, 9, 4.5f, c.px(), Ui.RADIO);
            }
            float textX = rx + 15;
            int room = (int) (bx + bw - 8 - textX);
            c.text(Ui.fit(c, o.label(), room), textX, oy + 5.5f,
                   on ? m : Ui.mix(Ui.TEXT_2, Ui.TEXT, hot ? 1f : 0f));
            if (o.about() != null) {
                c.text(Ui.fit(c, o.about(), room), textX, oy + 17.5f, Ui.HINT);
            }
            int index = i;
            zone(bx + 3, oy, bw - 6, OPTION_H - OPTION_GAP, (px, py, b) -> {
                if (index != r.selected.getAsInt()) {
                    click();
                    r.pick.accept(index);
                }
            }).key = "c:" + r.id + ":" + i;
        }
        float dy = y + 4 + r.options.size() * OPTION_H;
        for (Row d : r.details) {
            c.fill(bx + 4, dy, bx + bw - 4, dy + c.px(), Ui.LINE);
            row(d, bx, dy, bw, query, null);
            dy += rowHeight(d);
        }
        if (fade < 0.99f) {
            c.fill(x, y, x + w, y + shownH, Ui.alpha(Ui.OPTION_BOX, Math.round((1f - fade) * 235)));
        }
        unclip();
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
