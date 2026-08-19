package com.wynnchayuan;

import com.wynnchayuan.capture.CombatText;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * 驗證戰鬥提示的判斷。
 *
 * <p>兩邊都會痛：漏放行的話打怪時每被打一下就冒一個翻譯框；誤殺的話
 * 真正的 NPC 名字會整個消失。所以正反兩面都測。
 */
public final class CombatTextTest {

    private static int failures = 0;

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        // --- 要擋掉的 ---------------------------------------------------
        check("Dodged", CombatText.isIndicator("Dodged"));
        check("帶驚嘆號的 Dodged!", CombatText.isIndicator("Dodged!"));
        check("小寫 dodged", CombatText.isIndicator("dodged"));
        check("Blocked", CombatText.isIndicator("Blocked"));
        check("純傷害數字 -1,234", CombatText.isIndicator("-1,234"));
        check("治療 +560", CombatText.isIndicator("+560"));
        check("百分比 98%", CombatText.isIndicator("98%"));

        // --- 不能誤殺 ---------------------------------------------------
        check("NPC 名稱不受影響", !CombatText.isIndicator("Blacksmith"));
        check("含有戰鬥字眼的名稱不受影響（不用包含比對）",
                !CombatText.isIndicator("Blocked Passage"));
        check("帶等級的名牌不受影響", !CombatText.isIndicator("Ragni Guard LV 10"));
        check("空字串不算", !CombatText.isIndicator(""));
        check("純符號交給 GlyphSplitter 處理", !CombatText.isIndicator("   "));
        check("沒有數字的符號串不算", !CombatText.isIndicator("+++"));

        System.out.println(failures == 0
                ? "CombatText: 全部通過"
                : "CombatText: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + name);
        if (!ok) {
            failures++;
        }
    }
}
