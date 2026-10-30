package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class BlockProseColourTest {

    private static int failures = 0;

    private static final int BODY = 0xAAAAAA;
    private static final int ITEM = 0x00AAAA;
    private static final int COORD = 0xFFFFFF;

    public static void main(String[] args) throws Exception {
        realCard();
        longName();
        coordOnItsOwnRow();
        tealReallyWins();
        report();
    }

    private static void coordOnItsOwnRow() throws Exception {
        String flat = "Bring [{~} Kanderstone Ingots] or [{~} Kanderstone Gems] "
                + "to the Gathering Post [Mining Lv. {~}] at [-{~}, {~}, -{~}]";
        String dst = "把 [{~} Kanderstone 錠] 或 [{~} Kanderstone 寶石] "
                + "交到採集站 [採礦等級 {~}]，座標 [-{~}, {~}, -{~}]";
        List<StyledText> run = List.of(
                line("Bring ", BODY, "[40 Kanderstone Ingots]", ITEM, " or", BODY),
                line("[40 Kanderstone Gems]", ITEM, " to the", BODY),
                line("Gathering Post ", BODY, "[Mining Lv. 73]", ITEM, " at", BODY),
                line("[-712, 46, -5553]", COORD));

        Path debug = Files.createTempDirectory("block-prose-coord");
        FlowedDebug.init(debug);
        List<Component> built = translate(run, flat, dst);
        check("［座標自成一列］整段查得到譯文", built != null && !built.isEmpty());
        if (built == null || built.isEmpty()) {
            return;
        }
        dump("［座標自成一列］", built);

        String text = flatText(built);
        int at = text.indexOf("712");
        check("［座標自成一列］譯文裡有這個座標", at > 0);
        if (at <= 0) {
            return;
        }
        int bracket = text.lastIndexOf('[', at);
        Integer open = charColour(built, bracket);
        check("★［座標自成一列］座標開頭的「[」是白的，不是散文的灰（拿到 "
                + show(open) + "，實際分段：" + pieces(built) + "）",
                open != null && open == COORD);
        Integer minus = charColour(built, bracket + 1);
        check("★［座標自成一列］「[」後面那個負號也是白的（拿到 " + show(minus) + "）",
                minus != null && minus == COORD);
    }

    private static void longName() throws Exception {
        String flat = "Bring [{~} Arcane Anomalies] to the Slaying Post "
                + "[Combat Lv. {~}] at [-{~}, {~}, -{~}]";
        String dst = "把 [{~} 奧祕異象] 交到擊殺告示 [戰鬥等級 {~}]，座標 [-{~}, {~}, -{~}]";
        List<StyledText> run = List.of(
                line("Bring ", BODY, "[15 Arcane Anomalies]", ITEM, " to", BODY),
                line("the Slaying Post ", BODY, "[Combat Lv.", ITEM),
                line("75]", ITEM, " at ", BODY, "[-677, 46, -4948]", COORD));

        Path debug = Files.createTempDirectory("block-prose-long");
        FlowedDebug.init(debug);
        List<Component> built = translate(run, flat, dst);
        check("［長名稱］整段查得到譯文", built != null && !built.isEmpty());
        if (built == null || built.isEmpty()) {
            return;
        }
        dump("［長名稱］", built);

        String colour = blockColour(debug);
        check("★［長名稱］整段內文的底色是灰的（拿到 " + colour + "）",
                "#AAAAAA".equals(colour));

        Integer prose = colourOf(built, "交到擊殺告示");
        check("★［長名稱］散文是灰的、不是青的（拿到 " + show(prose) + "）",
                prose != null && prose == BODY);
        Integer coord = colourOf(built, "677");
        check("［長名稱］座標仍是原文的白（拿到 " + show(coord) + "）",
                coord != null && coord == COORD);
        Integer gap = colourOf(built, ", ");
        check("★［長名稱］座標裡的「, 」也是白的，不是散文的灰（拿到 "
                + show(gap) + "，實際分段：" + pieces(built) + "）",
                gap != null && gap == COORD);
    }

    private static void realCard() throws Exception {
        String flat = "Bring [{~} Fluffy Fur] to the Slaying Post "
                + "[Combat Lv. {~}] at [{~}, {~}, -{~}]";
        String dst = "把 [{~} 蓬鬆毛皮] 交到擊殺告示 [戰鬥等級 {~}]，座標 [{~}, {~}, -{~}]";
        List<StyledText> run = List.of(
                line("Bring ", BODY, "[24 Fluffy Fur]", ITEM, " to the", BODY),
                line("Slaying Post ", BODY, "[Combat Lv. 88]", ITEM, " at", BODY),
                line("[139, 61, -4399]", COORD));

        Path debug = Files.createTempDirectory("block-prose-card");
        FlowedDebug.init(debug);
        List<Component> built = translate(run, flat, dst);
        check("［任務卡］整段查得到譯文", built != null && !built.isEmpty());
        if (built == null || built.isEmpty()) {
            return;
        }
        dump("［任務卡］", built);

        String colour = blockColour(debug);
        check("［任務卡］診斷檔記到了這一段（拿到 " + colour + "）", colour != null);
        check("［任務卡］整段內文的底色是灰的（拿到 " + colour + "）",
                "#AAAAAA".equals(colour));

        Integer prose = colourOf(built, "交到擊殺告示");
        check("［任務卡］散文是灰的（拿到 " + show(prose) + "）",
                prose != null && prose == BODY);
        Integer coord = colourOf(built, "139");
        check("［任務卡］座標仍是原文的白（拿到 " + show(coord) + "）",
                coord != null && coord == COORD);
    }

    private static void tealReallyWins() throws Exception {
        String flat = "Get [{~} Fluffy Fur] and [{~} Soft Fur] right now";
        String dst = "取得 [{~} 蓬鬆毛皮] 與 [{~} 柔軟毛皮]，就是現在";
        List<StyledText> run = List.of(
                line("Get ", BODY, "[24 Fluffy Fur]", ITEM),
                line("and [88 Soft Fur] right now", ITEM));

        Path debug = Files.createTempDirectory("block-prose-teal");
        FlowedDebug.init(debug);
        List<Component> built = translate(run, flat, dst);
        check("［青色多數］整段查得到譯文", built != null && !built.isEmpty());
        if (built == null || built.isEmpty()) {
            return;
        }
        dump("［青色多數］", built);

        String colour = blockColour(debug);
        check("［青色多數］診斷檔記到了這一段（拿到 " + colour + "）", colour != null);
        check("［青色多數］整段內文的底色仍是青的（拿到 " + colour + "）",
                "#00AAAA".equals(colour));

        Integer prose = colourOf(built, "就是現在");
        check("［青色多數］散文是青的（拿到 " + show(prose) + "）",
                prose != null && prose == ITEM);
    }

    private static List<Component> translate(List<StyledText> run, String src, String dst,
                                             String... alsoKeyThenValue)
            throws Exception {
        Path dir = Files.createTempDirectory("block-prose");
        StringBuilder json = new StringBuilder("{\"" + src + "\": \"" + dst + "\"");
        for (int i = 0; i < alsoKeyThenValue.length; i += 2) {
            json.append(", \"").append(alsoKeyThenValue[i])
                .append("\": \"").append(alsoKeyThenValue[i + 1]).append('"');
        }
        Files.writeString(dir.resolve("quest.json"),
                json.append('}').toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return LineTranslator.translateBlock(run, store, new boolean[run.size()]);
    }

    private static String blockColour(Path dir) throws Exception {
        Path file = dir.resolve("majorid-debug.txt");
        if (!Files.exists(file)) {
            return null;
        }
        String mark = "整段內文的底色：";
        for (String row : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            int at = row.indexOf(mark);
            if (at >= 0) {
                return row.substring(at + mark.length()).strip();
            }
        }
        return null;
    }

    private static StyledText line(Object... pairs) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < pairs.length; i += 2) {
            out.append(Component.literal((String) pairs[i]).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb((Integer) pairs[i + 1]))));
        }
        return StyledText.fromComponent(out);
    }

    private static List<String> pieces(List<Component> built) {
        List<String> out = new ArrayList<>();
        for (Component c : built) {
            c.visit((style, t) -> {
                out.add(t);
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        return out;
    }

    private static String flatText(List<Component> built) {
        StringBuilder out = new StringBuilder();
        for (Component c : built) {
            c.visit((style, t) -> {
                out.append(t);
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        return out.toString();
    }

    private static Integer charColour(List<Component> built, int index) {
        List<Integer> hit = new ArrayList<>();
        int[] seen = {0};
        for (Component c : built) {
            c.visit((style, t) -> {
                if (hit.isEmpty() && index < seen[0] + t.length()) {
                    hit.add(style.getColor() == null ? null : style.getColor().getValue() & 0xFFFFFF);
                }
                seen[0] += t.length();
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        return hit.isEmpty() ? null : hit.get(0);
    }

    private static String show(Integer c) {
        return c == null ? "null" : "#" + String.format("%06X", c);
    }

    private static void dump(String tag, List<Component> built) {
        for (Component c : built) {
            StringBuilder row = new StringBuilder(tag + "行：");
            c.visit((style, s) -> {
                row.append('[').append(s).append(' ')
                   .append(style.getColor() == null ? "null" : style.getColor().serialize())
                   .append(']');
                return java.util.Optional.empty();
            }, Style.EMPTY);
            System.out.println(row);
        }
    }

    private static Integer colourOf(List<Component> built, String needle) {
        List<Integer> found = new ArrayList<>();
        for (Component c : built) {
            c.visit((style, s) -> {
                if (s.contains(needle) && style.getColor() != null) {
                    found.add(style.getColor().getValue() & 0xFFFFFF);
                }
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        return found.isEmpty() ? null : found.get(0);
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "[通過] " : "[失敗] ") + name);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            throw new AssertionError(failures + " 項檢查未通過");
        }
        System.out.println("全部通過");
    }
}
