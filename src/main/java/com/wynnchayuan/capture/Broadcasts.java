package com.wynnchayuan.capture;

import com.wynntils.core.text.StyledText;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Broadcasts {

    private Broadcasts() {}

    public record Spans(int userStart, int userEnd, int worldStart, int worldEnd) {}

    private static final String NAME = "(?<user>[^\\n:]{1,40}?)";

    private static final String BOMB = "[A-Z][A-Za-z]*(?: [A-Z][A-Za-z]*){0,3}";

    private static final String KNOWN = "(?:Combat Experience|Profession Experience|Profession Speed"
            + "|Combat XP|Profession XP|Scroll Charge|World Event|Loot Chest|Chest Loot"
            + "|Dungeon|Party|Item|Loot)";

    private static final List<Pattern> FRAMES = List.of(
            Pattern.compile("^\\s*" + NAME + " has thrown an? " + BOMB + " Bomb\\s+on\\s+"
                    + "(?<world>[A-Za-z]{1,6}\\d{0,4})\\s*$"),
            Pattern.compile("^\\s*" + NAME + " has thrown an?(?: " + BOMB + " Bomb\\.?)?\\s*$"),
            Pattern.compile("^\\s*" + NAME + " " + KNOWN + " Bomb has expired![\\s\\S]*$"),
            Pattern.compile("^\\s*Thank (?!you\\b)(?<user>[^\\n:!.]{1,40}?)\\s*$"));

    public static Spans find(StyledText message) {
        return message == null ? null : spans(LineParts.flat(message));
    }

    public static Spans spans(String flat) {
        if (flat == null || flat.isBlank()) {
            return null;
        }
        for (Pattern frame : FRAMES) {
            Matcher m = frame.matcher(flat);
            if (!m.matches()) {
                continue;
            }
            int from = m.start("user");
            int to = m.end("user");
            if (flat.substring(from, to).isBlank()) {
                continue;
            }
            int worldFrom = -1;
            int worldTo = -1;
            if (frame.pattern().contains("(?<world>")) {
                worldFrom = m.start("world");
                worldTo = m.end("world");
            }
            return new Spans(from, to, worldFrom, worldTo);
        }
        return null;
    }

    public static LineParts parts(StyledText message) {
        Spans spans = find(message);
        return spans == null ? LineParts.of(message) : LineParts.of(message, spans);
    }
}
