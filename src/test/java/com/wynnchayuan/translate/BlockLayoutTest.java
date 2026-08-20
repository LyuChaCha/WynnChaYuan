package com.wynnchayuan.translate;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;

import java.util.List;
import java.util.function.ToIntFunction;

/**
 * 驗證「這一塊是置中還是靠左」的判斷。
 *
 * <p>兩者在單獨一行上長得一模一樣，但處理方式完全相反：置中的行譯文變短時
 * 要加大縮排，靠左的行加了縮排整段就歪掉。判錯了畫面上只會覺得「排版怪怪的」，
 * 說不出哪裡怪——未鑑定物品與材料配方清單就這樣互相打架過好幾版。
 *
 * <p>寬度用假的量法注入：正式路徑要有字型才量得出來，headless 量不到。
 * 這裡假設每個字 6 像素，比例對就夠驗證判斷邏輯。
 */
public final class BlockLayoutTest {

    private static final Style SPACE = Style.EMPTY.withFont(
            new FontDescription.Resource(Identifier.withDefaultNamespace("space")));

    /** 前導偏移 + 每字 6 像素。與正式路徑的量法對應。 */
    private static final ToIntFunction<Component> WIDTH = c -> {
        String s = c.getString();
        int px = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            px += (cp >= 0xD0000 - 1024 && cp <= 0xD0000 + 1024) ? cp - 0xD0000 : 6;
        }
        return px;
    };

    private static int failures = 0;

    /** 一行：前導偏移 px 像素，後面接文字。 */
    private static Component line(int px, String text) {
        if (px == 0) {
            return Component.empty()
                    .append(Component.literal(
                            new String(Character.toChars(0xD0000))).withStyle(SPACE))
                    .append(Component.literal(text));
        }
        return Component.empty()
                .append(Component.literal(
                        new String(Character.toChars(0xD0000 + px))).withStyle(SPACE))
                .append(Component.literal(text));
    }

    /** 把一串文字置中排好，模擬伺服器的做法。 */
    private static List<Component> centredBlock(String... texts) {
        int widest = 0;
        for (String t : texts) {
            widest = Math.max(widest, t.length() * 6);
        }
        List<Component> out = new java.util.ArrayList<>();
        for (String t : texts) {
            out.add(line((widest - t.length() * 6) / 2, t));
        }
        return out;
    }

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        boolean[] sealed = BlockLayout.centered(centredBlock(
                "This item's power has been sealed,",
                "an Item Identifier can unlock",
                "its potential."), WIDTH);
        check("偏移吻合置中算式 -> 置中", sealed[0] && sealed[1] && sealed[2]);

        // 材料的配方清單：整塊靠左，縮排都是 0
        boolean[] recipes = BlockLayout.centered(List.of(
                line(0, "Crafting Level"),
                line(0, "Can be used in recipes for"),
                line(0, "Weaponsmithing  Woodworking"),
                line(0, "Tailoring  Armouring"),
                line(0, "Jeweling")), WIDTH);
        check("縮排全是 0 而寬度不一 -> 靠左",
                !recipes[0] && !recipes[1] && !recipes[2] && !recipes[3] && !recipes[4]);

        // 先前「偏移不全相同就是置中」會在這裡出事：
        // 只有一行的縮排不同，其餘靠左——那個縮排不是為了置中
        boolean[] oneOdd = BlockLayout.centered(List.of(
                line(8, "Crafting Level"),
                line(0, "Can be used in recipes for"),
                line(0, "Weaponsmithing  Woodworking"),
                line(0, "Jeweling")), WIDTH);
        check("只有一行縮排不同 -> 仍然是靠左（先前被誤判成置中）",
                !oneOdd[0] && !oneOdd[1] && !oneOdd[2] && !oneOdd[3]);

        // 空行分段：兩段各自判斷
        List<Component> mixed = new java.util.ArrayList<>(List.of(
                line(0, "Durability"),
                line(0, "Duration"),
                Component.literal(" ")));
        mixed.addAll(centredBlock("This item's power has been sealed,", "its potential."));
        boolean[] flags = BlockLayout.centered(mixed, WIDTH);
        check("空行前的靠左段不受影響", !flags[0] && !flags[1]);
        check("空行後的置中段判得出來", flags[3] && flags[4]);

        check("空清單不會爆", BlockLayout.centered(List.of(), WIDTH).length == 0);
        check("單行沒有比較對象，當成靠左",
                !BlockLayout.centered(List.of(line(20, "Solo")), WIDTH)[0]);
        check("每行等寬時置中與靠左沒差別，當成靠左",
                !BlockLayout.centered(List.of(
                        line(0, "AAAA"), line(0, "BBBB")), WIDTH)[0]);

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
