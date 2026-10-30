package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ObjectiveTailColourTest {

    private static int failures = 0;

    private static final int BODY = 0xAAAAAA;
    private static final int COORD = 0xFFFFFF;

    private static final String LINE_ONE = "Pick up your post in the Post";
    private static final String LINE_TWO_TEXT = "Office at ";
    private static final String COORDS = "[-2156, 30, -944]";

    private static Component line(Object... pairs) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < pairs.length; i += 2) {
            out.append(Component.literal((String) pairs[i]).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb((Integer) pairs[i + 1]))));
        }
        return out;
    }

    private static List<StyledText> block() {
        return List.of(
                StyledText.fromComponent(line(LINE_ONE, BODY)),
                StyledText.fromComponent(line(LINE_TWO_TEXT, BODY, COORDS, COORD)));
    }

    public static void main(String[] args) throws Exception {
        FlowedDebug.init(java.nio.file.Files.createTempDirectory("wynnchayuan"));

        oneLine();
        twoLines();

        report();
    }

    private static void oneLine() throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));
        List<StyledText> run = block();
        List<Component> built = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        check("［一行］整段查得到譯文", built != null && !built.isEmpty());
        if (built == null || built.isEmpty()) {
            return;
        }
        verify("［一行］", built);
    }

    private static void twoLines() throws Exception {
        Path dir = java.nio.file.Files.createTempDirectory("objective-tail");
        String src = LINE_ONE + "\\n" + LINE_TWO_TEXT.strip() + " [-{~}, {~}, -{~}]";
        String dst = "到 [-{~}, {~}, -{~}]\\n的郵局領取你的郵件。";
        java.nio.file.Files.writeString(dir.resolve("quest.json"),
                "{\"" + src + "\": \"" + dst + "\"}");
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        List<StyledText> run = block();
        List<Component> built = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        check("［兩行］整段查得到譯文", built != null && !built.isEmpty());
        if (built == null || built.isEmpty()) {
            return;
        }
        check("［兩行］譯文真的是兩行（實際 " + built.size() + " 行）", built.size() == 2);
        verify("［兩行］", built);
    }

    private static void verify(String tag, List<Component> built) {
        String all = text(built);
        System.out.println(tag + "譯文：" + all.replace("\n", "⏎"));
        dump(built);
        check(tag + "座標填回來了（實際：" + all + "）", all.contains(COORDS));
        check(tag + "後半翻出來了（實際：" + all + "）", all.contains("領取你的郵件"));

        Integer coord = colourOf(built, "2156");
        check(tag + "座標是原文的白（拿到 " + show(coord) + "）",
                coord != null && coord == COORD);
        Integer tail = colourOf(built, "郵局");
        check(tag + "座標後面的說明仍是灰的（拿到 " + show(tail) + "）",
                tail != null && tail == BODY);
        Integer head = colourOf(built, "到");
        check(tag + "座標前面的字也是灰的（拿到 " + show(head) + "）",
                head != null && head == BODY);
    }

    private static String show(Integer c) {
        return c == null ? "null" : "#" + String.format("%06X", c);
    }

    private static String text(List<Component> built) {
        StringBuilder out = new StringBuilder();
        for (Component c : built) {
            out.append(c.getString());
        }
        return out.toString();
    }

    private static void dump(List<Component> built) {
        for (Component c : built) {
            StringBuilder row = new StringBuilder("  行：");
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
