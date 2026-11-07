package com.wynnchayuan.client.ui;

import java.util.ArrayList;
import java.util.List;

public final class CreditsView extends Surface {

    public record Member(String name, String minecraft, int colour, List<String> teams,
                         List<String> areas) {
        public Member {
            teams = teams == null ? List.of() : List.copyOf(teams);
            areas = areas == null ? List.of() : List.copyOf(areas);
        }

        boolean hasHead() {
            return minecraft != null && !minecraft.isBlank();
        }

        boolean tagged() {
            return !teams.isEmpty() || !areas.isEmpty();
        }

        boolean in(String team) {
            return team.isEmpty() || teams.contains(team);
        }
    }

    public record Section(String role, String about, int colour, boolean band,
                          List<Member> members) {
        public Section {
            members = members == null ? List.of() : List.copyOf(members);
        }
    }

    public interface Host {
        List<Section> sections();

        String teamName(String code);

        String title();

        String version();

        void close();
    }

    public static final String[] TEAM_ORDER = {
        "zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr"};

    private static final int EVERYONE = 0xFFA8A8A8;
    private static final float HEAD = 16;
    private static final float CELL_H = 26;
    private static final float CELL_MIN = 168;
    private static final int MAX_COLUMNS = 3;
    private static final float BAND_HEAD = 18;
    private static final float BAND_GAP = 12;
    private static final float CHIP_H = 14;

    private final Shell shell;
    private final Host host;
    private String team = "";
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

    public String team() {
        return team;
    }

    @Override
    protected void draw() {
        float w = Math.min(screenW - 20, 560);
        float h = Math.min(screenH - 14, 360);
        float x = Math.round((screenW - w) / 2f);
        float y = Math.round((screenH - h) / 2f);
        window(x, y, w, h, shell.blurred());
        zone(0, 0, screenW, screenH, (px, py, b) -> { });

        float left = x + 14;
        float right = x + w - 14;

        c.logo(left, y + 9, 24);
        float s = c.scale(1.5f);
        c.text(host.title(), left + 31, y + 9, Ui.TEXT, s);
        String sub = "v" + host.version() + "  ·  " + shell.tr("credits.tagline");
        c.text(Ui.fit(c, sub, (int) (right - left - 31)), left + 31, y + 9 + 8 * s + 3, Ui.TEXT_3);

        List<Section> sections = host.sections();
        List<String> present = present(sections);
        if (!team.isEmpty() && !present.contains(team)) {
            team = "";
        }
        float top = y + 42;
        if (!present.isEmpty()) {
            top = chipRow(present, left, top, right) + 8;
        }

        List<String> notes = Ui.wrap(c, shell.tr("credits.note2"), (int) (right - left));
        float footY = y + h - 28;
        float notesY = footY - 6 - notes.size() * 10;
        float bottom = notesY - 6;
        float listH = bottom - top;
        float listW = right - left - 8;
        int columns = Math.max(1, Math.min(MAX_COLUMNS, (int) (listW / CELL_MIN)));
        float cellW = listW / columns;

        float contentH = content(sections, columns, cellW, left, 0, false);
        maxScroll = Math.max(0, contentH - listH);
        scrollTarget = Math.max(0, Math.min(maxScroll, scrollTarget));
        scroll = Ui.ease(Math.max(0, Math.min(maxScroll, scroll)), scrollTarget, dt, 55f);

        clip(left - 2, top, right, bottom);
        content(sections, columns, cellW, left, top - scroll, true);
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
    }

    private static List<String> present(List<Section> sections) {
        List<String> out = new ArrayList<>();
        for (String code : TEAM_ORDER) {
            if (anyone(sections, code)) {
                out.add(code);
            }
        }
        return out;
    }

    private static boolean anyone(List<Section> sections, String code) {
        for (Section section : sections) {
            if (!section.band()) {
                continue;
            }
            for (Member m : section.members()) {
                if (m.teams().contains(code)) {
                    return true;
                }
            }
        }
        return false;
    }

    private float chipRow(List<String> present, float left, float y, float right) {
        float x = left;
        float rowY = y;
        for (int i = -1; i < present.size(); i++) {
            String code = i < 0 ? "" : present.get(i);
            String label = i < 0 ? shell.tr("credits.all") : host.teamName(code);
            c.language(i < 0 ? shell.language() : code);
            float cw = c.width(label) + 14;
            if (x > left && x + cw > right) {
                x = left;
                rowY += CHIP_H + 4;
            }
            boolean on = code.equals(team);
            boolean hot = !on && over(x, rowY, cw, CHIP_H);
            if (on) {
                c.round(x, rowY, cw, CHIP_H, Ui.R1, Ui.LEAF_STOPS[1]);
            } else {
                Ui.pane(c, x, rowY, cw, CHIP_H, Ui.R1, hot ? Ui.RAISED : Ui.FIELD, Ui.BORDER);
            }
            c.text(label, x + 7, rowY + 3, on ? Ui.ON_ACCENT : (hot ? Ui.TEXT : Ui.TEXT_2));
            c.language(shell.language());
            zone(x, rowY, cw, CHIP_H, (px, py, b) -> {
                if (!code.equals(team)) {
                    click();
                    team = code;
                    scroll = 0;
                    scrollTarget = 0;
                }
            }).key = "team:" + (code.isEmpty() ? "all" : code);
            x += cw + 4;
        }
        return rowY + CHIP_H;
    }

    private float content(List<Section> sections, int columns, float cellW, float left, float y,
                          boolean paint) {
        float at = y;
        float width = cellW * columns;
        for (Section section : sections) {
            if (!section.band()) {
                continue;
            }
            List<Member> shown = new ArrayList<>();
            for (Member m : section.members()) {
                if (m.in(team)) {
                    shown.add(m);
                }
            }
            if (shown.isEmpty()) {
                continue;
            }
            if (paint) {
                bandHead(section, left, at, width);
            }
            at += BAND_HEAD;
            for (int i = 0; i < shown.size(); i++) {
                int column = i % columns;
                if (paint) {
                    member(shown.get(i), section.colour(), left + column * cellW, at, cellW);
                }
                if (column == columns - 1 || i == shown.size() - 1) {
                    at += CELL_H;
                }
            }
            at += BAND_GAP;
        }
        if (team.isEmpty()) {
            for (Section section : sections) {
                if (!section.band() && !section.members().isEmpty()) {
                    at = quiet(section, left, at, width, paint) + BAND_GAP;
                }
            }
        }
        if (paint) {
            everyone(left, at, width);
        }
        return at + 12 - y;
    }

    private void bandHead(Section section, float x, float y, float w) {
        c.icon(Icons.Icon.LEAF, x, y, 11, section.colour());
        c.text(section.role(), x + 15, y + 2, section.colour());
        float after = x + 15 + c.width(section.role()) + 8;
        String about = section.about();
        if (about != null && !about.isEmpty() && after < x + w - 24) {
            c.text(Ui.fit(c, about, (int) (x + w - after)), after, y + 2, Ui.TEXT_3);
        }
        c.fade(x, y + BAND_HEAD - 5, w, c.px(), Ui.alpha(section.colour(), 0x66), 1f, 0f);
    }

    private void member(Member m, int bandColour, float x, float y, float w) {
        if (over(x, y, w - 4, CELL_H - 3)) {
            c.round(x - 2, y - 1, w - 2, CELL_H - 2, Ui.R1, Ui.RAISED);
        }
        float hx = x + 2;
        float hy = y + (CELL_H - 2 - HEAD) / 2f;
        if (m.hasHead()) {
            c.head(m.minecraft(), hx, hy, HEAD);
        } else {
            Ui.pane(c, hx, hy, HEAD, HEAD, 3, Ui.FIELD, Ui.BORDER);
            c.icon(Icons.Icon.LEAF, hx + 3, hy + 3, HEAD - 6, Ui.FAINT);
        }
        float leafX = x + w - 18;
        c.icon(Icons.Icon.LEAF, leafX, y + (CELL_H - 2 - 9) / 2f, 9,
               m.colour() != 0 ? m.colour() : bandColour);
        float tx = hx + HEAD + 6;
        float room = leafX - 5 - tx;
        float nameY = m.tagged() ? y + 3 : y + (CELL_H - 2 - 8) / 2f;
        String name = Ui.fit(c, m.name(), (int) room);
        c.text(name, tx, nameY, Ui.TEXT);
        if (m.hasHead() && !m.minecraft().equals(m.name())) {
            float mx = tx + c.width(name) + 5;
            if (mx < tx + room - 14) {
                c.text(Ui.fit(c, m.minecraft(), (int) (tx + room - mx)), mx, nameY, Ui.HINT);
            }
        }
        if (m.tagged()) {
            tags(m, tx, y + 14, tx + room);
        }
    }

    private void tags(Member m, float x, float y, float limit) {
        List<String[]> parts = new ArrayList<>();
        for (String code : m.teams()) {
            parts.add(new String[] {host.teamName(code), code});
        }
        for (String area : m.areas()) {
            parts.add(new String[] {shell.tr("credits.area." + area), shell.language()});
        }
        float at = x;
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                String dot = " · ";
                if (at + c.width(dot) + 8 >= limit) {
                    return;
                }
                c.text(dot, at, y, Ui.FAINT);
                at += c.width(dot);
            }
            c.language(parts.get(i)[1]);
            String whole = parts.get(i)[0];
            String text = Ui.fit(c, whole, (int) (limit - at));
            c.text(text, at, y, Ui.FAINT);
            at += c.width(text);
            c.language(shell.language());
            if (!text.equals(whole)) {
                return;
            }
        }
    }

    private float quiet(Section section, float x, float y, float w, boolean paint) {
        if (paint) {
            c.text(section.role(), x + 2, y + 2, Ui.TEXT_3);
        }
        StringBuilder names = new StringBuilder();
        for (Member m : section.members()) {
            if (names.length() > 0) {
                names.append("  ·  ");
            }
            names.append(m.name());
        }
        float at = y + 15;
        for (String line : Ui.wrap(c, names.toString(), (int) (w - 4))) {
            if (paint) {
                c.text(line, x + 2, at, Ui.FAINT);
            }
            at += 10;
        }
        return at;
    }

    private void everyone(float x, float y, float w) {
        c.icon(Icons.Icon.LEAF, x + 1, y, 10, EVERYONE);
        String lead = shell.tr("credits.user");
        c.text(lead, x + 15, y + 1, Ui.TEXT_2);
        float after = x + 15 + c.width(lead) + 6;
        if (after < x + w - 24) {
            c.text(Ui.fit(c, shell.tr("credits.userb"), (int) (x + w - after)), after, y + 1,
                   Ui.FAINT);
        }
    }
}
