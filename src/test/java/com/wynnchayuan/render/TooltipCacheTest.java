package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * tooltip 的翻譯結果快取，什麼時候可以用上一次的、什麼時候一定要重算。
 *
 * <h2>為什麼要有這支</h2>
 * 快取壞掉的樣子不是「當掉」，是<b>畫面上出現上一次的答案</b>——切了語言
 * 還是舊語言、Shift 放開了名稱還停在原文。那種錯看起來像翻譯漏了，
 * 不像快取有問題，查起來會繞很遠。
 *
 * <p>而且這一份結果會交給 Wynntils 的事件（{@code event.setTooltips}），
 * 後面還有別的模組的 listener 會跑。往 tooltip 清單後面加行是模組最常做的事，
 * 加到的如果是快取裡那一份，同一件物品會愈看愈長。所以「交出去的必須是複本」
 * 也釘在這裡。
 */
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

        // ★ 交出去的是複本：呼叫端亂改也不能弄髒快取
        report("兩次拿到的不是同一個 list 物件", first != second);
        // 要改<b>命中那一次</b>拿到的，不是第一次。第一次是 miss，走的是
        // 「翻完再包一份出去」那條路；會把快取裡那一份直接交出去的是 hit。
        // 先前這裡改的是 first，於是把 hit 改成回傳快取原件也照樣通過。
        try {
            second.add(Component.literal("別的模組加的一行"));
        } catch (UnsupportedOperationException e) {
            report("！命中時回傳的清單不可變——別的模組加行會炸在它自己身上", false);
        }
        List<Component> third = TooltipPanel.translateLinesCached(tooltip, store);
        report("★ 改過回傳的清單之後，快取那一份沒被弄髒（實際 "
                + third.size() + " 行）", third.size() == first.size());

        // ★ 名稱模式換了要重算：Shift 暫看原文走的就是這條。
        //
        // 這裡一定要拿<b>裝備名</b>來問。先前用的是「Combat Level」，那是介面
        // 標籤，不吃「翻譯物品名稱」那個開關——於是開關切過去結果一樣，
        // 而測試把「一樣」當成了快取沒失效。Halcyon 是繁中少數翻了名字的
        // mythic（靜星耀杖），關掉開關就會退回英文。
        List<Component> gear = List.of(
                Component.literal("Halcyon"),
                Component.literal("Halcyon"));
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

        // ★ 語料重載要重算：換語言走的就是這條（generation 會往前一格）
        int before = store.generation();
        store.loadAll(List.of(root.resolve("zh_tw"), root.resolve("zh_cn")));
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
