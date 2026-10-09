package com.wynnchayuan.client.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * 更新說明：一版一張卡片，新的在上面，可以捲。
 *
 * <p>說明有英文、繁中、簡中三份，右上角可以切——預設跟著介面語言，但介面是日文、
 * 韓文的人沒有自己語言的說明，要能自己挑看得懂的那一份。
 */
public final class NotesView extends Surface {

    /** 一版的說明。 */
    public record Note(String headline, List<String> items) {}

    public interface Host {
        /** 有說明的版本，新的在前。 */
        List<String> versions();

        /** 這一版在這個語言的說明；沒有就回 {@code null}。 */
        Note notes(String version, String lang);

        /** 現在跑的是哪一版。 */
        String running();

        /** 比現在新的版本；沒有就回 {@code null}。 */
        String newer();

        /** 說明有哪幾種語言可以選。 */
        String[] languages();

        /** 現在選的是第幾種。 */
        int language();

        void pickLanguage(int index);

        void download();

        void close();
    }

    private final Shell shell;
    private final Host host;
    private float scroll;
    private float scrollTarget;
    private float maxScroll;

    public NotesView(Shell shell, Host host) {
        this.shell = shell;
        this.host = host;
    }

    @Override
    protected int accentColour() {
        return shell.accent();
    }

    @Override
    protected void click() {
        shell.click();
    }

    @Override
    public boolean scroll(double dy) {
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget - (float) dy * 28f));
        return true;
    }

    @Override
    public boolean key(int key, boolean ctrl) {
        switch (key) {
            case KEY_DOWN -> scroll(-1);
            case KEY_UP -> scroll(1);
            case KEY_PAGE_DOWN -> scroll(-6);
            case KEY_PAGE_UP -> scroll(6);
            case KEY_HOME -> scrollTarget = 0;
            case KEY_END -> scrollTarget = maxScroll;
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void draw() {
        float w = Math.min(screenW - 20, 560);
        float h = Math.min(screenH - 14, 334);
        float x = Math.round((screenW - w) / 2f);
        float y = Math.round((screenH - h) / 2f);
        window(x, y, w, h, shell.blurred());
        zone(0, 0, screenW, screenH, (px, py, b) -> { });

        float left = x + 14;
        float right = x + w - 14;
        String[] codes = host.languages();
        String[] labels = new String[codes.length];
        for (int i = 0; i < codes.length; i++) {
            labels[i] = shell.tr("notes.lang." + codes[i]);
        }
        float chipsLeft = chips("lang", right, y + 11, labels, host.language(), index -> {
            host.pickLanguage(index);
            scroll = 0;
            scrollTarget = 0;
        });

        float s = c.scale(1.5f);
        c.text(shell.tr("notes.title"), left, y + 9, Ui.TEXT, s);
        String newer = host.newer();
        String status = newer != null
                ? shell.tr("notes.newer", newer, host.running())
                : shell.tr("notes.uptodate", host.running());
        c.text(Ui.fit(c, status, (int) (chipsLeft - 10 - left)), left, y + 9 + 8 * s + 4,
               newer != null ? Ui.AMBER : Ui.TEXT_3);

        float top = y + 40;
        float footY = y + h - 28;
        float bottom = footY - 6;
        float listH = bottom - top;
        float cardW = right - left - 6;

        String lang = codes[Math.max(0, Math.min(codes.length - 1, host.language()))];
        List<String> versions = host.versions();
        List<Object[]> cards = new ArrayList<>();          // {版本, 說明, 高度}
        // 說明可能是別的語言寫的：那一塊換成那個語言的字型，量寬度也要用它
        c.language(lang);
        float contentH = 0;
        for (String version : versions) {
            Note note = host.notes(version, lang);
            if (note == null) {
                continue;
            }
            note = plain(note);
            float ch = cardHeight(note, cardW);
            cards.add(new Object[] {version, note, ch});
            contentH += ch + 6;
        }
        contentH = Math.max(0, contentH - 6);
        maxScroll = Math.max(0, contentH - listH);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll = Ui.ease(Math.max(0, Math.min(maxScroll, scroll)), scrollTarget, dt, 55f);

        if (cards.isEmpty()) {
            c.language(shell.language());
            String none = shell.tr("notes.none");
            c.text(none, x + (w - c.width(none)) / 2f, top + 24, Ui.TEXT_3);
        } else {
            clip(left, top, right, bottom);
            float cy = top - scroll;
            for (Object[] card : cards) {
                float ch = (Float) card[2];
                if (cy + ch >= top && cy < bottom) {
                    card(left, cy, cardW, ch, (String) card[0], (Note) card[1]);
                }
                cy += ch + 6;
            }
            unclip();
            c.language(shell.language());
            scrollbar(right - 3, top, listH, contentH, scroll, maxScroll);
        }

        c.fill(x + 1, footY, x + w - 1, footY + c.px(), Ui.LINE);
        float by = footY + 5;
        String back = shell.tr("button.back");
        float bw = buttonWidth(back) + 14;
        float end = right;
        button("back", end - bw, by, bw, 18, back, true, newer == null, null, host::close);
        end -= bw + 5;
        if (newer != null) {
            String download = shell.tr("notes.download", newer);
            float dw = Math.min(buttonWidth(download) + 8, end - left);
            button("download", end - dw, by, dw, 18, download, true, true, null, host::download);
            end -= dw + 8;
        }
        if (maxScroll > 0 && end - left > 60) {
            c.text(Ui.fit(c, shell.tr("notes.scroll"), (int) (end - left)), left, by + 5,
                   Ui.FAINT);
        }
    }

    /**
     * 說明是照 GitHub 發版頁的寫法寫的，夾著 {@code <b>} 與反引號。
     * 這裡不畫粗體也不畫等寬字，留著只會原樣印出來，所以拿掉。
     */
    static Note plain(Note note) {
        List<String> items = new ArrayList<>();
        for (String item : note.items()) {
            items.add(plain(item));
        }
        return new Note(plain(note.headline()), items);
    }

    static String plain(String text) {
        return text.replace("<b>", "").replace("</b>", "").replace("`", "");
    }

    private float cardHeight(Note note, float w) {
        float h = 8 + 11 + 3;
        if (!note.headline().isBlank()) {
            h += Ui.wrap(c, note.headline(), (int) (w - 20)).size() * 11 + 3;
        }
        for (String item : note.items()) {
            h += Ui.wrap(c, item, (int) (w - 20 - 9)).size() * 11;
        }
        return h + 8;
    }

    private void card(float x, float y, float w, float h, String version, Note note) {
        boolean current = version.equals(host.running());
        Ui.pane(c, x, y, w, h, Ui.R2, current ? Ui.alpha(accent, 0x1E) : Ui.CARD,
                current ? Ui.alpha(accent, 0x80) : Ui.LINE);
        float ty = y + 8;
        String label = "v" + version;
        c.text(label, x + 10, ty, accent);
        if (current) {
            // 這一句是介面的字，不是說明的一部分：換回介面的字型
            c.language(shell.language());
            String tag = shell.tr("notes.current");
            float tx = x + 10 + c.width(label) + 6;
            c.round(tx, ty - 2, c.width(tag) + 8, 12, 3, Ui.alpha(accent, 0x33));
            c.text(tag, tx + 4, ty, accent);
            c.language(host.languages()[Math.max(0, host.language())]);
        }
        ty += 14;
        if (!note.headline().isBlank()) {
            for (String line : Ui.wrap(c, note.headline(), (int) (w - 20))) {
                c.text(line, x + 10, ty, Ui.TEXT);
                ty += 11;
            }
            ty += 3;
        }
        for (String item : note.items()) {
            c.round(x + 11.5f, ty + 3, 2.5f, 2.5f, 1.25f, Ui.FAINT);
            for (String line : Ui.wrap(c, item, (int) (w - 20 - 9))) {
                c.text(line, x + 19, ty, Ui.TEXT_2);
                ty += 11;
            }
        }
    }
}
