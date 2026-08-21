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

        // tooltip 的分隔多半不是空行，而是由排版字元組成的分隔線。
        // 只看 isBlank 的話整份 tooltip 會被當成同一段，置中的區塊
        // 跟上面的名稱擠在一起判斷，結果整段被判成靠左、一行都沒調整。
        List<Component> withDivider = new java.util.ArrayList<>(List.of(
                line(0, "Waist Apron"),
                Component.literal(new String(Character.toChars(0xD0010)))
                        .withStyle(SPACE)));
        withDivider.addAll(centredBlock(
                "This item's power has been sealed,", "its potential."));
        boolean[] div = BlockLayout.centered(withDivider, WIDTH);
        check("符號組成的分隔線也算分段", !div[0] && div[2] && div[3]);

        check("空清單不會爆", BlockLayout.centered(List.of(), WIDTH).length == 0);
        check("單行沒有比較對象，當成靠左",
                !BlockLayout.centered(List.of(line(20, "Solo")), WIDTH)[0]);
        check("每行等寬時置中與靠左沒差別，當成靠左",
                !BlockLayout.centered(List.of(
                        line(0, "AAAA"), line(0, "BBBB")), WIDTH)[0]);

        // 未鑑定物品：名稱與稀有度跟後面三行之間「沒有任何分隔線」。
        // 整段一起判斷時前兩行對不上算式，結果五行全被判成靠左，
        // 譯文變短之後整塊往左塌——螢幕上就是那三行說明沒有跟著置中。
        // 寬度用每字 6 像素推：最寬的那行 34 字 = 204。
        List<Component> unidentified = List.of(
                line(0, "Waist Apron"),                          // 靠左，縮排 0
                line(0, "[RARE] LEGGINGS"),                       // 靠左，縮排 0
                line(0, "This item's power has been sealed,"),    // 置中，最寬
                line(21, "an Item Identifier can unlock"),        // (204-162)/2
                line(60, "its potential."));                      // (204-84)/2
        boolean[] sealedInPlace = BlockLayout.centered(unidentified, WIDTH);
        check("沒有分隔線時，仍認得出後面那串置中的說明",
                !sealedInPlace[0] && !sealedInPlace[1] && sealedInPlace[2]
                        && sealedInPlace[3] && sealedInPlace[4]);

        // 反面：縮排全是 0 的清單不能被當成置中，加了縮排整段就歪掉。
        check("縮排全是 0 的清單不算置中",
                noneOf(BlockLayout.centered(List.of(
                        line(0, "Can be used in recipes for"),
                        line(0, "Tailoring"),
                        line(0, "Weaponsmithing"),
                        line(0, "Jeweling")), WIDTH)));

        // 玩家實際回報的那一份 tooltip，數字取自 line-debug：
        //   an ◉ Item Identifier can unlock  整行 163、縮排 4  -> 內容 159
        //   詞條行整行都是 164、縮排 0
        // 剩下兩行的寬度用預設字型估：170 與 60。
        //
        // 這裡用「像素」直接餵，不經過字型——要驗的是判斷邏輯本身。
        int[] lead    = {  0,   0, 0,   4,  49, 0,  0,   0};
        int[] content = { 90,  80, 170, 159, 60, 62, 164, 164};
        check("回報的那份 tooltip：說明那三行判成置中",
                centredAt(lead, content, 2) && centredAt(lead, content, 3)
                        && centredAt(lead, content, 4));
        check("同一份裡的詞條行不受影響", !centredAt(lead, content, 6));

        // 同一份 tooltip，但那兩行改用 Wynncraft 自己的字型、量出來窄了一成半。
        // 像素對不上了，但「長的行縮排小」這件事還是成立。
        int[] offContent = { 90, 80, 145, 159, 51, 62, 164, 164};
        check("字型量偏一成半，說明那三行仍判成置中",
                centredAt(lead, offContent, 2) && centredAt(lead, offContent, 3)
                        && centredAt(lead, offContent, 4));

        // 反面：真正的縮排清單（越縮排的行反而越長）不能被當成置中
        check("越縮排越長的清單不算置中",
                !centredAt(new int[] {0, 0, 30}, new int[] {100, 90, 150}, 2));

        System.out.println(failures == 0
                ? "BlockLayout: 全部通過" : "BlockLayout: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /** 直接餵像素，不經過字型——要驗的是判斷邏輯本身。 */
    private static boolean centredAt(int[] lead, int[] content, int index) {
        List<Component> lines = new java.util.ArrayList<>();
        for (int i = 0; i < lead.length; i++) {
            lines.add(line(lead[i], "x".repeat(Math.max(1, content[i] / 6))));
        }
        return BlockLayout.centered(lines, c -> {
            String s = c.getString();
            int px = 0;
            for (int i = 0; i < s.length(); ) {
                int cp = s.codePointAt(i);
                i += Character.charCount(cp);
                px += (cp >= 0xD0000 - 1024 && cp <= 0xD0000 + 1024) ? cp - 0xD0000 : 6;
            }
            return px;
        })[index];
    }

    private static boolean noneOf(boolean[] flags) {
        for (boolean f : flags) {
            if (f) {
                return false;
            }
        }
        return true;
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + name);
        if (!ok) {
            failures++;
        }
    }
}
