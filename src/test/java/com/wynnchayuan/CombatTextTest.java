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
        // 大傷害會被縮寫成 12.3k。那個 k 先前讓「有字母就是文字」這條規則
        // 失手，於是每一發大傷害都被當成名牌收進語料。
        check("縮寫傷害 12.3k", CombatText.isIndicator("12.3k"));
        check("一次跳好幾發 12.3k 4.5k", CombatText.isIndicator("12.3k 4.5k"));
        check("秒數 3s", CombatText.isIndicator("3s"));
        check("縮寫治療 +1.2m", CombatText.isIndicator("+1.2m"));

        // --- 不能誤殺 ---------------------------------------------------
        check("NPC 名稱不受影響", !CombatText.isIndicator("Blacksmith"));
        // 單位只認「緊貼數字、而且後面不再接字母」的那一個，所以帶數量的
        // 名字仍然收得到——3x 的 x 是單位，Bandit 的 B 不是。
        check("帶數量的名字不受影響", !CombatText.isIndicator("3x Bandit"));
        check("數字接單字不受影響", !CombatText.isIndicator("2 Guards"));
        check("縮寫後面還接字母的不受影響", !CombatText.isIndicator("12kg"));
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
