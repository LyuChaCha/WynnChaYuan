package com.wynnchayuan.client;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class ChatButtonsTest {

    private static final int CHAR = 6;

    private static int measure(Component part) {
        return part.getString().length() * CHAR;
    }

    private static Component clickable(String label, String command) {
        return ChatButtons.button(Component.literal(label)).withStyle(style -> style
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(command))));
    }

    private static List<String> commands(Component row) {
        List<String> out = new ArrayList<>();
        row.visit((style, text) -> {
            if (style.getClickEvent() instanceof ClickEvent.RunCommand run
                    && style.getHoverEvent() != null && !out.contains(run.command())) {
                out.add(run.command());
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    public static void main(String[] args) {
        Component first = clickable("first", "/one");
        Component second = clickable("second", "/two");
        int both = measure(first) + 2 * CHAR + measure(second);

        List<Component> wide = ChatButtons.rows(List.of(first, second), both, ChatButtonsTest::measure);
        check("放得下就併一列（實際 " + wide.size() + " 列）", wide.size() == 1);
        check("兩顆按鈕各留自己的點擊與提示", commands(wide.get(0)).equals(List.of("/one", "/two")));

        List<Component> narrow = ChatButtons.rows(List.of(first, second), both - 1, ChatButtonsTest::measure);
        check("差一格就整顆換列（實際 " + narrow.size() + " 列）", narrow.size() == 2);
        check("換列後第一列是第一顆", commands(narrow.get(0)).equals(List.of("/one")));
        check("換列後第二列是第二顆", commands(narrow.get(1)).equals(List.of("/two")));

        check("只有一顆就一列", ChatButtons.rows(List.of(second), 1, ChatButtonsTest::measure).size() == 1);
        check("沒有按鈕就沒有列", ChatButtons.rows(List.of(), 100, ChatButtonsTest::measure).isEmpty());

        System.out.println(failures == 0 ? "聊天按鈕：全部通過" : "聊天按鈕：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }
}
