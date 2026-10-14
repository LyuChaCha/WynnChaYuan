package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.nio.file.Path;

/**
 * Lootrun 使命名：不被同名的裝備搶走，並且照「任務與洞窟名稱」的設定附原文。
 *
 * <h2>實機回報（使用者 2026-10-14）</h2>
 * 使命 {@code Equilibrium} 跟一頂頭盔同名，聊天面板上畫成「平衡之盔 (Equilibrium)」；
 * 另外希望使命名也能像物品一樣在後面附 (原文)。見 {@link LootrunNames}。
 *
 * <p>期望的字從語料取，不寫死措辭。
 */
public final class LootrunMissionTest {

    private static int failures = 0;
    private static final String CORPUS = "src/main/resources/assets/wynnchayuan/translations";
    private static final String BLOCK = "Collect your rewards!" + (char) 10
            + "- Time Left: {~}:{~}" + (char) 10 + "- Challenges: {~}/{~}" + (char) 10
            + "Sacrificial Ritual:" + (char) 10 + "- Open {~}/{~} Chests";

    public static void main(String[] args) {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of(CORPUS, Languages.DEFAULT));
        String gearName = store.lookupExact("Equilibrium");
        check("語料有那頂同名的頭盔（實際 " + gearName + "）", gearName != null);

        System.out.println("=== 譯名（預設）===");
        store.setPageNameMode(CollectorConfig.ItemNames.ON);
        String cell = store.lookup("{#}Equilibrium");
        check("使命那一格有自己的譯名（實際 " + cell + "）", cell != null && !cell.contains("Equilibrium"));
        check("★ 不是頭盔的名字", cell != null && gearName != null && !cell.contains(gearName));
        String panel = chat(store, "Sacrificial Ritual", "Equilibrium");
        check("★ 面板上不是頭盔（實際 " + panel + "）",
              panel != null && gearName != null && !panel.contains(gearName));
        check("譯名模式不附原文", panel != null && !panel.contains("("));
        String colon = store.lookup("Equilibrium:");
        check("記分板那一行查得到（實際 " + colon + "）", colon != null && !colon.contains("Equilibrium"));

        System.out.println("=== 中英對照 ===");
        store.setPageNameMode(CollectorConfig.ItemNames.BOTH);
        String ritual = store.lookup("Sacrificial Ritual:");
        check("★ 記分板：「譯名 (原文):」（實際 " + ritual + "）",
              ritual != null && ritual.endsWith(" (Sacrificial Ritual):"));
        String block = store.lookup(BLOCK);
        check("整塊記分板裡的那一行也附（實際 " + block + "）",
              block != null && block.contains(" (Sacrificial Ritual):"));
        check("整塊的其他行不動", block != null && !block.contains("(Collect"));
        panel = chat(store, "Mission Started", "Sacrificial Ritual");
        check("★ 使命開始的面板附原文（實際 " + panel + "）",
              panel != null && panel.contains("(Sacrificial Ritual)") && !panel.contains("Mission Started"));
        panel = chat(store, "Sacrificial Ritual", "Equilibrium");
        check("★ 兩個使命名各附一次（實際 " + panel + "）", panel != null
              && panel.contains("(Sacrificial Ritual)") && panel.contains("(Equilibrium)")
              && panel.indexOf("(Equilibrium)") == panel.lastIndexOf("(Equilibrium)"));
        check("中英對照也不是頭盔", panel != null && gearName != null && !panel.contains(gearName));

        System.out.println("=== 關閉 ===");
        store.setPageNameMode(CollectorConfig.ItemNames.OFF);
        String off = store.lookup("Sacrificial Ritual:");
        check("關閉時是原文（實際 " + off + "）", "Sacrificial Ritual:".equals(off));

        System.out.println("=== 不該動的 ===");
        CollectorConfig.ItemNames both = CollectorConfig.ItemNames.BOTH;
        check("句子裡提到使命名不動",
              "譯文".equals(LootrunNames.apply("Finish Sacrificial Ritual now", "譯文", both)));
        check("物品名稱那條路附過的不再附",
              "某某 (Equilibrium)".equals(LootrunNames.apply("Equilibrium", "某某 (Equilibrium)", both)));
        check("格數對不上就整條不碰",
              "甲乙".equals(LootrunNames.apply("{#}Hubris{#}x", "甲乙", both)));
        check("顏色標記收尾前面接原文",
              "{c1}傲慢 (Hubris){/}:".equals(LootrunNames.apply("Hubris:", "{c1}傲慢{/}:", both)));
        check("沒翻的名字不附", "Imperitia".equals(LootrunNames.apply("Imperitia", "Imperitia", both)));

        System.out.println(failures == 0
                ? "Lootrun 使命名：全部通過" : "Lootrun 使命名：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /** 兩欄併排的面板那一行：排版偏移、左欄、排版偏移、右欄。 */
    private static String chat(TranslationStore store, String left, String right) {
        MutableComponent row = Component.empty();
        row.append(offset(36)).append(Component.literal(left))
           .append(offset(65)).append(Component.literal(right));
        Component out = LineTranslator.translateChat(StyledText.fromComponent(row), store, null, true);
        return out == null ? null : out.getString();
    }

    private static Component offset(int px) {
        return Component.literal(SpaceOffset.encode(px))
                .setStyle(SpaceOffset.styleFor(Style.EMPTY));
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
