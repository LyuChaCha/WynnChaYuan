package com.wynnchayuan.render;

import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CraftedNameTest {

    private static int failures = 0;

    private static final Style WHITE = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFFFF));
    private static final Style GREY = Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA));
    private static final String FRAME = new String(Character.toChars(0xCF000));

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        Path dir = Path.of("src/main/resources/assets/wynnchayuan/translations", Languages.DEFAULT);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        String name = null;
        for (String candidate : new String[] {"XP 30", "Health Potion", "Strength", "Speed 3"}) {
            List<Component> got = TooltipPanel.translateInPlace(plain(candidate, null), store);
            if (!got.isEmpty() && !got.get(0).getString().equals(candidate)) {
                name = candidate;
                break;
            }
        }
        check("對照組：找得到一個平常會被翻掉的名字（沒有的話這份測試什麼都沒驗）", name != null);
        if (name == null) {
            done();
            return;
        }

        List<Component> crafted = TooltipPanel.translateInPlace(plain(name, "Crafted by skmile"), store);
        check("WynnMod 的排法：製作物的名字留原文（實際 " + first(crafted, name) + "）",
                first(crafted, name).equals(name));
        check("「Crafted by」那一行照常翻，製作者的名字原樣留著",
                crafted.size() > 2 && !crafted.get(2).getString().startsWith("Crafted by")
                        && crafted.get(2).getString().endsWith("skmile"));
        check("其他列照常翻", crafted.size() > 1
                && !crafted.get(1).getString().equals("Combat Level"));

        List<Component> framed = new ArrayList<>();
        framed.add(Component.literal(FRAME + name + FRAME).withStyle(WHITE));
        framed.add(Component.literal(FRAME + FRAME + FRAME + name).withStyle(WHITE));
        framed.add(Component.literal("Combat Level").withStyle(GREY));
        framed.add(Component.literal("Crafted by Some_One").withStyle(GREY));
        List<Component> vanilla = TooltipPanel.translateInPlace(framed, store);
        check("原版的排法：兩列名字都留原文",
                vanilla.size() == 4 && vanilla.get(0).getString().equals(FRAME + name + FRAME)
                        && vanilla.get(1).getString().equals(FRAME + FRAME + FRAME + name));
        check("原版的排法：其他列照常翻",
                vanilla.size() == 4 && !vanilla.get(2).getString().equals("Combat Level"));

        List<Component> normal = TooltipPanel.translateInPlace(plain(name, null), store);
        check("沒有製作者那一行：同一個名字照舊會翻", !first(normal, name).equals(name));
        List<Component> mention = TooltipPanel.translateInPlace(plain(name, "Handcrafted by nobody"), store);
        check("「Handcrafted by」不算（要是完整的 Crafted by）", !first(mention, name).equals(name));

        done();
    }

    private static List<Component> plain(String name, String maker) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(name).withStyle(WHITE));
        lines.add(Component.literal("Combat Level").withStyle(GREY));
        if (maker != null) {
            lines.add(Component.literal(maker).withStyle(GREY));
        }
        return lines;
    }

    private static String first(List<Component> lines, String original) {
        return lines.isEmpty() ? original : lines.get(0).getString();
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    private static void done() {
        System.out.println(failures == 0
                ? "CraftedName: 全部通過" : "CraftedName: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
