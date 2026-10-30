package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TooltipCacheTest {

    private static int failures = 0;

    public static void main(String[] args) {
        Path root = Path.of(args.length > 0 ? args[0]
                : "src/main/resources/assets/wynnchayuan/translations");
        TranslationStore store = new TranslationStore();
        store.loadAll(List.of(root.resolve("zh_tw")));
        store.setNameMode(CollectorConfig.ItemNames.ON);

        List<Component> tooltip = List.of(
                Component.literal("Combat Level"),
                Component.literal("Combat Level"));

        List<Component> first = TooltipPanel.translateLinesCached(tooltip, store);
        List<Component> second = TooltipPanel.translateLinesCached(tooltip, store);
        report("同一份內容兩次結果一樣（實際 " + text(second) + "）",
                text(first).equals(text(second)));
        report("真的有翻到（不是兩次都空的）", !text(first).isBlank()
                && !text(first).equals("Combat Level\nCombat Level"));

        report("兩次拿到的不是同一個 list 物件", first != second);
        try {
            second.add(Component.literal("別的模組加的一行"));
        } catch (UnsupportedOperationException e) {
            report("！命中時回傳的清單不可變——別的模組加行會炸在它自己身上", false);
        }
        List<Component> third = TooltipPanel.translateLinesCached(tooltip, store);
        report("★ 改過回傳的清單之後，快取那一份沒被弄髒（實際 "
                + third.size() + " 行）", third.size() == first.size());

        List<Component> gear = List.of(
                Component.literal("Halcyon"),
                Component.literal("Halcyon"),
                banner());
        String on = text(TooltipPanel.translateLinesCached(gear, store));
        store.setNameMode(CollectorConfig.ItemNames.OFF);
        String off = text(TooltipPanel.translateLinesCached(gear, store));
        report("開著的時候裝備名是中文（實際 " + on.replace('\n', '/') + "）",
                !on.contains("Halcyon") && !on.isBlank());
        report("★ 關掉「翻譯物品名稱」之後結果會變（開 " + on.replace('\n', '/')
                + "，關 " + off.replace('\n', '/') + "）", !on.equals(off));
        store.setNameMode(CollectorConfig.ItemNames.ON);
        report("再開回來又回到原本那一份",
                on.equals(text(TooltipPanel.translateLinesCached(gear, store))));

        int before = store.generation();
        store.loadAll(List.of(root.resolve("zh_cn")));
        store.setNameMode(CollectorConfig.ItemNames.ON);
        report("重載之後 generation 有往前（" + before + " -> "
                + store.generation() + "）", store.generation() != before);
        String reloaded = text(TooltipPanel.translateLinesCached(tooltip, store));
        report("★ 重載之後拿到的是新語料的答案（實際 "
                + reloaded.replace('\n', '/') + "）",
                !reloaded.isBlank() && !reloaded.contains("Combat Level"));

        System.out.println(failures == 0 ? "tooltip 快取：全部通過"
                : "tooltip 快取：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static Component banner() {
        net.minecraft.network.chat.MutableComponent row = Component.empty();
        int[] colours = {0xAA00AA, 0x000000, 0xE0B3E6};
        int[] glyphs = {0xE060, 0xE03C, 0xE048};
        for (int k = 0; k < glyphs.length; k++) {
            int c = colours[k];
            row.append(Component.literal(new String(Character.toChars(glyphs[k])))
                    .withStyle(st -> st.withColor(c)));
        }
        return row;
    }

    private static String text(List<Component> lines) {
        StringBuilder sb = new StringBuilder();
        for (Component c : lines) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(c.getString());
        }
        return sb.toString();
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
