package com.wynnchayuan;

import com.wynnchayuan.capture.CombatText;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class CombatTextTest {

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        check("Dodged", CombatText.isIndicator("Dodged"));
        check("帶驚嘆號的 Dodged!", CombatText.isIndicator("Dodged!"));
        check("小寫 dodged", CombatText.isIndicator("dodged"));
        check("Blocked", CombatText.isIndicator("Blocked"));
        check("純傷害數字 -1,234", CombatText.isIndicator("-1,234"));
        check("治療 +560", CombatText.isIndicator("+560"));
        check("百分比 98%", CombatText.isIndicator("98%"));
        check("縮寫傷害 12.3k", CombatText.isIndicator("12.3k"));
        check("一次跳好幾發 12.3k 4.5k", CombatText.isIndicator("12.3k 4.5k"));
        check("秒數 3s", CombatText.isIndicator("3s"));
        check("縮寫治療 +1.2m", CombatText.isIndicator("+1.2m"));

        check("NPC 名稱不受影響", !CombatText.isIndicator("Blacksmith"));
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

}
