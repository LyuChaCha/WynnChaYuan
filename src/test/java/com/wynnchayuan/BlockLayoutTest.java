package com.wynnchayuan;

import com.wynnchayuan.translate.BlockLayout;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;

import java.util.List;

/**
 * 驗證「這一塊是置中還是靠左」的判斷。
 *
 * <p>兩者在單獨一行上長得一模一樣，但處理方式完全相反：置中的行譯文變短時
 * 要加大縮排，靠左的行加了縮排整段就歪掉。判錯了畫面上只會覺得「排版怪怪的」，
 * 說不出哪裡怪——未鑑定物品與材料配方清單就這樣互相打架過好幾版。
 */
public final class BlockLayoutTest {

    private static final Style SPACE = Style.EMPTY.withFont(
            new FontDescription.Resource(Identifier.withDefaultNamespace("space")));

    private static int failures = 0;

    /** 一行：前導偏移 px 像素，後面接文字。 */
    private static Component line(int px, String text) {
        Component body = Component.literal(text);
        if (px == 0 && text.isEmpty()) {
            return body;
        }
        return Component.empty()
                .append(Component.literal(
                        new String(Character.toChars(0xD0000 + px))).withStyle(SPACE))
                .append(body);
    }

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        // 未鑑定物品的說明：偏移逐行不同，那就是它對齊中線的方式
        boolean[] sealed = BlockLayout.centered(List.of(
                line(0, "This item's power has been sealed,"),
                line(4, "an Item Identifier can unlock"),
                line(49, "its potential.")));
        check("偏移逐行不同 -> 置中",
                sealed[0] && sealed[1] && sealed[2]);

        // 材料的配方清單：整塊共用同一個縮排
        boolean[] recipes = BlockLayout.centered(List.of(
                line(0, "Can be used in recipes for"),
                line(0, "Weaponsmithing  Woodworking"),
                line(0, "Tailoring  Armouring"),
                line(0, "Jeweling")));
        check("偏移全部相同 -> 靠左（先前被誤判成置中，整塊被推歪）",
                !recipes[0] && !recipes[1] && !recipes[2] && !recipes[3]);

        // 空行分段：兩段各自判斷，不會互相影響
        boolean[] mixed = BlockLayout.centered(List.of(
                line(0, "Durability"),
                line(0, "Duration"),
                Component.literal(" "),
                line(0, "This item's power has been sealed,"),
                line(49, "its potential.")));
        check("空行前的靠左段不受影響", !mixed[0] && !mixed[1]);
        check("空行後的置中段判得出來", mixed[3] && mixed[4]);

        check("空清單不會爆", BlockLayout.centered(List.of()).length == 0);
        check("單行沒有比較對象，當成靠左（不動它最安全）",
                !BlockLayout.centered(List.of(line(20, "Solo"))) [0]);

        System.out.println(failures == 0
                ? "BlockLayout: 全部通過" : "BlockLayout: " + failures + " 項失敗");
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
