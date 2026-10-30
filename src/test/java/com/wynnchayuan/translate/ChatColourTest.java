package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ChatColourTest {

    private static int failures = 0;

    private static final int GOLD = 0xFFAA00;
    private static final int GREY = 0xAAAAAA;
    private static final int RED  = 0xFF5555;
    private static final int WHITE = 0xFFFFFF;
    private static final int BROWN = 0x8F663D;
    private static final int LIGHT = 0xBC8F62;
    private static final int LIME = 0x55FF55;
    private static final int MAGENTA = 0xFF55FF;
    private static final int CYAN = 0x55FFFF;

    private static final String ICON = "\ue001";

    private static StyledText welcome() {
        MutableComponent all = Component.empty();
        all.append(lit(ICON, GOLD, true));
        all.append(lit("Welcome to Wynncraft!", GOLD, true));
        all.append(lit("\n", GREY, false));
        all.append(lit(ICON, GREY, false));
        all.append(lit("play.wynncraft.com ", GREY, false));
        all.append(lit("-/-", GREY, false));
        all.append(lit(" wynncraft.com", GREY, false));
        all.append(lit("\n\n", GREY, false));
        all.append(lit(ICON, RED, true));
        all.append(lit("TIP: ", RED, true));
        all.append(lit("Type ", GREY, false));
        all.append(lit("/char", WHITE, false));
        all.append(lit(" to switch character", GREY, false));
        return StyledText.fromComponent(all);
    }

    private static StyledText mounts() {
        MutableComponent all = Component.empty();
        all.append(lit(ICON, GOLD, true));
        all.append(lit("Welcome to Wynncraft!", GOLD, true));
        all.append(lit("\n", GREY, false));
        all.append(lit(ICON, GREY, false));
        all.append(lit("play.wynncraft.com ", GREY, false));
        all.append(lit("-/-", GREY, false));
        all.append(lit(" wynncraft.com", GREY, false));
        all.append(lit("\n\n", GREY, false));
        all.append(lit(ICON, BROWN, false));
        all.append(lit("2 ", BROWN, false));
        all.append(lit("mounts have", BROWN, false));
        all.append(lit(" ", LIGHT, false));
        all.append(lit("no food", LIGHT, false));
        all.append(lit(" ", BROWN, false));
        all.append(lit("in their feeder", BROWN, false));
        return StyledText.fromComponent(all);
    }

    private static StyledText padded() {
        MutableComponent all = Component.empty();
        all.append(lit("\n", GREY, false));
        all.append(welcome().getComponent().copy());
        all.append(lit("\n", GREY, false));
        return StyledText.fromComponent(all);
    }

    private static MutableComponent lit(String text, int colour, boolean bold) {
        return Component.literal(text).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(colour)).withBold(bold));
    }

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));
        FlowedDebug.init(java.nio.file.Files.createTempDirectory("wynnchayuan"));

        Component hit = LineTranslator.translate(welcome(), store);
        check("歡迎訊息查得到譯文", hit != null);
        if (hit == null) {
            report();
            return;
        }
        List<Component> built = List.of(hit);
        System.out.println("      輸出：" + hit.getString().replace("\n", " ⏎ "));
        String all = hit.getString();
        check("標題有翻出來", all.contains("歡迎來到"));

        Integer title = colourOf(built, "歡迎來到");
        check("標題查得到顏色（拿到 "
                        + (title == null ? "null" : "#" + String.format("%06X", title))
                        + "）", title != null);
        check("標題不是內文的灰色", title != null && title != GREY);
        check("標題沿用原文的金色", title != null && title == GOLD);

        Boolean bold = boldOf(built, "歡迎來到");
        check("標題跟原文一樣是粗體", Boolean.TRUE.equals(bold));

        Integer url = colourOf(built, "wynncraft.com");
        check("網址那一行仍是灰色（拿到 "
                        + (url == null ? "null" : "#" + String.format("%06X", url))
                        + "）", url != null && url == GREY);

        Integer slash = colourOf(built, "/char");
        check("一行多色時 /char 仍保住自己的白色（拿到 "
                        + (slash == null ? "null" : "#" + String.format("%06X", slash))
                        + "）", slash == null || slash == WHITE);

        long titles = flatten(hit).stream()
                .filter(c -> c.getString().contains("歡迎來到"))
                .count();
        check("標題只被貼一次（實際 " + titles + " 段）", titles == 1);

        long rows = hit.getString().chars().filter(c -> c == 10).count() + 1;
        check("多行訊息的行數保住不變（實際 " + rows + " 行）", rows == 4);

        Component mount = LineTranslator.translate(mounts(), store);
        check("坐騎那則查得到譯文", mount != null);
        if (mount != null) {
            System.out.println("      輸出：" + mount.getString().replace("\n", " ⏎ "));
            List<Component> one = List.of(mount);
            check("坐騎那一行有翻出來", mount.getString().contains("坐騎"));
            Integer feeder = colourOf(one, "坐騎");
            check("混色的行不再掉成內文的灰（拿到 "
                            + (feeder == null ? "null" : "#" + String.format("%06X", feeder))
                            + "）", feeder != null && feeder != GREY);
            check("混色的行套的是原文的多數色", feeder != null && feeder == BROWN);
            Integer stillGold = colourOf(one, "歡迎來到");
            check("標題還是金色", stillGold != null && stillGold == GOLD);
            Integer stillGrey = colourOf(one, "wynncraft.com");
            check("網址還是灰色", stillGrey != null && stillGrey == GREY);
        }

        Component pad = LineTranslator.translate(padded(), store);
        check("前後多空行時仍翻得出來", pad != null);
        if (pad != null) {
            Integer padTitle = colourOf(List.of(pad), "歡迎來到");
            check("前後多空行時標題還是金色（拿到 "
                            + (padTitle == null ? "null" : "#" + String.format("%06X", padTitle))
                            + "）", padTitle != null && padTitle == GOLD);
            long padRows = pad.getString().chars().filter(c -> c == 10).count() + 1;
            check("譯文維持自己的行數（實際 " + padRows + " 行）", padRows == 4);
        }

        lootrunEnd(store);

        report();
    }

    private static void lootrunEnd(TranslationStore store) {
        MutableComponent heads = Component.empty();
        heads.append(lit(ICON, LIME, false));
        heads.append(lit("Rewards", LIME, false));
        heads.append(lit(ICON, MAGENTA, false));
        heads.append(lit("Statistics", MAGENTA, false));
        Component row = LineTranslator.translateChat(
                StyledText.fromComponent(heads), store);
        check("結算標題列翻得出來", row != null);
        if (row != null) {
            List<Component> one = List.of(row);
            Integer left = colourOf(one, "獎勵");
            Integer right = colourOf(one, "統計");
            check("「獎勵」是左欄的綠（拿到 " + hex(left) + "）",
                    left != null && left == LIME);
            check("「統計」是右欄的粉（拿到 " + hex(right) + "）",
                    right != null && right == MAGENTA);
        }

        MutableComponent pulls = Component.empty();
        pulls.append(lit(ICON, CYAN, false));
        pulls.append(lit("31", CYAN, false));
        pulls.append(lit(" Reward Pulls", WHITE, false));
        pulls.append(lit(ICON, WHITE, false));
        pulls.append(lit("Time Elapsed: 11:58", WHITE, false));
        Component stat = LineTranslator.translateChat(
                StyledText.fromComponent(pulls), store);
        check("結算抽數列翻得出來", stat != null);
        if (stat != null) {
            String zh = stat.getString();
            check("兩欄都換成中文（實際 " + zh + "）",
                    !zh.contains("Reward Pulls") && !zh.contains("Time Elapsed"));
            Integer number = colourOf(List.of(stat), "31");
            check("數字自己保住水藍（拿到 " + hex(number) + "）",
                    number != null && number == CYAN);
        }
    }

    private static String hex(Integer colour) {
        return colour == null ? "null" : "#" + String.format("%06X", colour);
    }

    private static Integer colourOf(List<Component> lines, String needle) {
        for (Component line : lines) {
            for (Component part : flatten(line)) {
                if (part.getString().contains(needle)) {
                    TextColor colour = part.getStyle().getColor();
                    return colour == null ? null : colour.getValue();
                }
            }
        }
        return null;
    }

    private static Boolean boldOf(List<Component> lines, String needle) {
        for (Component line : lines) {
            for (Component part : flatten(line)) {
                if (part.getString().contains(needle)) {
                    return part.getStyle().isBold();
                }
            }
        }
        return null;
    }

    private static List<Component> flatten(Component component) {
        List<Component> out = new ArrayList<>();
        if (component.getSiblings().isEmpty()) {
            out.add(component);
        }
        for (Component child : component.getSiblings()) {
            out.addAll(flatten(child));
        }
        return out;
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        System.out.println(failures == 0
                ? "ChatColour: 全部通過" : "ChatColour: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
