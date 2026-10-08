package com.wynnchayuan.client.ui;

import java.util.List;

/**
 * 關於與貢獻者。
 *
 * <p>翻譯是靠很多人一條一條填出來的，名單值得放在看得到的地方。
 * 人一多一人一列會太長，所以先算一欄多寬、排得下幾欄，再照那個欄數排；
 * 還是放不下就捲。
 */
public final class CreditsView extends Surface {

    /** 名單上的一位。{@code minecraft} 是空的就沒有頭像（資料來源那一類）。 */
    public record Member(String name, String minecraft) {
        boolean hasHead() {
            return minecraft != null && !minecraft.isBlank();
        }
    }

    public record Section(String role, int colour, List<Member> members) {}

    public interface Host {
        List<Section> sections();

        String title();

        String version();

        /** 「遊戲內標記：開啟」那顆按鈕上的字。 */
        String badgeLabel();

        void toggleBadges();

        String styleLabel();

        void cycleStyle();

        void close();
    }

    private static final float HEAD = 11;
    private static final float ROW = 15;
    private static final int MAX_COLUMNS = 3;

    private final Shell shell;
    private final Host host;
    private float scroll;
    private float scrollTarget;
    private float maxScroll;

    public CreditsView(Shell shell, Host host) {
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
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget - (float) dy * 24f));
        return true;
    }

    @Override
    protected void draw() {
        float w = Math.min(screenW - 20, 500);
        float h = Math.min(screenH - 14, 334);
        float x = Math.round((screenW - w) / 2f);
        float y = Math.round((screenH - h) / 2f);
        window(x, y, w, h, shell.blurred());
        zone(0, 0, screenW, screenH, (px, py, b) -> { });

        float left = x + 14;
        float right = x + w - 14;

        // 頁首：圖示、標題、版本與一句話
        c.logo(left, y + 9, 24);
        float s = c.scale(1.5f);
        c.text(host.title(), left + 31, y + 9, Ui.TEXT, s);
        String sub = "v" + host.version() + "  ·  " + shell.tr("credits.tagline");
        c.text(Ui.fit(c, sub, (int) (right - left - 31)), left + 31, y + 9 + 8 * s + 3, Ui.TEXT_3);

        // 底下兩句說明與按鈕先量好，名單用剩下的高度
        List<String> notes = new java.util.ArrayList<>();
        notes.addAll(Ui.wrap(c, shell.tr("credits.note1"), (int) (right - left)));
        notes.addAll(Ui.wrap(c, shell.tr("credits.note2"), (int) (right - left)));
        float footY = y + h - 28;
        float notesY = footY - 6 - notes.size() * 10;
        float top = y + 42;
        float bottom = notesY - 6;
        float listH = bottom - top;
        float listW = right - left - 6;

        List<Section> sections = host.sections();
        float cell = widest(sections) + 18;
        int columns = Math.max(1, Math.min(MAX_COLUMNS, (int) (listW / Math.max(1f, cell))));
        float contentH = 0;
        for (Section section : sections) {
            contentH += sectionHeight(section, columns) + 8;
        }
        contentH = Math.max(0, contentH - 8);
        maxScroll = Math.max(0, contentH - listH);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll = Ui.ease(Math.max(0, Math.min(maxScroll, scroll)), scrollTarget, dt, 55f);

        clip(left, top, right, bottom);
        float cy = top - scroll + Math.max(0, (listH - contentH) / 2f);
        float centre = left + listW / 2f;
        for (Section section : sections) {
            // 分類標題：左右各一條同色的細線，比純文字更看得出是分隔
            String role = section.role();
            float rw = c.width(role);
            c.text(role, centre - rw / 2f, cy, section.colour());
            int line = Ui.alpha(section.colour(), 0x60);
            c.fill(centre - rw / 2f - 46, cy + 4, centre - rw / 2f - 8, cy + 4 + c.px(), line);
            c.fill(centre + rw / 2f + 8, cy + 4, centre + rw / 2f + 46, cy + 4 + c.px(), line);
            cy += 15;
            List<Member> members = section.members();
            if (members.isEmpty()) {
                String empty = shell.tr("credits.empty");
                c.text(empty, centre - c.width(empty) / 2f, cy, Ui.FAINT);
                cy += ROW;
            }
            int used = Math.min(columns, Math.max(1, members.size()));
            float start = centre - used * cell / 2f;
            for (int i = 0; i < members.size(); i++) {
                int column = i % used;
                member(members.get(i), start + column * cell + cell / 2f, cy, section.colour());
                if (column == used - 1 || i == members.size() - 1) {
                    cy += ROW;
                }
            }
            cy += 8;
        }
        unclip();
        scrollbar(right - 3, top, listH, contentH, scroll, maxScroll);

        float ny = notesY;
        for (String line : notes) {
            c.text(line, x + (w - c.width(line)) / 2f, ny, Ui.FAINT);
            ny += 10;
        }

        c.fill(x + 1, footY, x + w - 1, footY + c.px(), Ui.LINE);
        float by = footY + 5;
        String back = shell.tr("button.back");
        float bw = buttonWidth(back) + 14;
        button("back", right - bw, by, bw, 18, back, true, true, null, host::close);
        float room = right - bw - 8 - left;
        String badge = host.badgeLabel();
        String style = host.styleLabel();
        float aw = Math.min(buttonWidth(badge), room / 2f - 3);
        float sw = Math.min(buttonWidth(style), room / 2f - 3);
        // 標記的開關放在名單這一頁，因為它就是「這份名單要不要顯示在遊戲裡」
        button("badge", left, by, aw, 18, badge, true, false, null, host::toggleBadges);
        button("style", left + aw + 5, by, sw, 18, style, true, false, null, host::cycleStyle);
    }

    /** 一位貢獻者：頭像、暱稱、Minecraft ID。暱稱用分類的顏色，ID 用灰色，才分得出哪個是哪個。 */
    private void member(Member m, float centre, float y, int colour) {
        float total = memberWidth(m);
        float x = centre - total / 2f;
        if (m.hasHead()) {
            c.head(m.minecraft(), x, y - 1.5f, HEAD);
            x += HEAD + 5;
        }
        c.text(m.name(), x, y, colour);
        if (m.hasHead()) {
            c.text(m.minecraft(), x + c.width(m.name()) + 6, y, Ui.HINT);
        }
    }

    private float memberWidth(Member m) {
        float w = c.width(m.name());
        if (m.hasHead()) {
            w += HEAD + 5 + 6 + c.width(m.minecraft());
        }
        return w;
    }

    /** 最寬的那一位有多寬。所有欄同寬，名字才對得齊。 */
    private float widest(List<Section> sections) {
        float widest = 40;
        for (Section section : sections) {
            for (Member m : section.members()) {
                widest = Math.max(widest, memberWidth(m));
            }
        }
        return widest;
    }

    private static float sectionHeight(Section section, int columns) {
        int count = section.members().size();
        int used = Math.min(columns, Math.max(1, count));
        return 15 + (count == 0 ? ROW : (count + used - 1) / used * ROW);
    }
}
