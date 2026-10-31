package com.wynnchayuan.translate;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;

import java.util.List;
import java.util.function.ToIntFunction;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class BlockLayoutTest {

    private static final Style SPACE = Style.EMPTY.withFont(
            new FontDescription.Resource(Identifier.withDefaultNamespace("space")));

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

        List<Component> cave = new java.util.ArrayList<>(List.of(
                line(92, "xxxxxxxxxxxxxx"),
                line(98, "xxxxxxxxxxxx"),
                Component.literal(" "),
                line(44, "xxxxxxxxx"),
                line(44, "xxxxxxxxxxxxxxxxxxx"),
                line(44, "xxxxxxxxxxxxxxxxxxxxx")));
        List<Component> koi = new java.util.ArrayList<>(List.of(
                line(0, "x".repeat(20)),
                line(0, "x".repeat(15)),
                Component.literal(" "),
                line(0, "x".repeat(17)),
                line(14, "x".repeat(13)),
                line(14, "x".repeat(11)),
                line(14, "x".repeat(12))));
        boolean[] koiFlags = BlockLayout.centered(koi, WIDTH);
        check("★ 縮排一樣、長短不同的幾行不是置中（實際 "
                        + java.util.Arrays.toString(koiFlags) + "）",
              !koiFlags[3] && !koiFlags[4] && !koiFlags[5] && !koiFlags[6]);

        boolean[] flags = BlockLayout.centered(cave, WIDTH);
        check("★ 置中的標題兩行：縮排差只有內容差的一半也要認得出來",
              flags[0] && flags[1]);
        check("★ 底下縮排相同的清單三行仍然靠左",
              !flags[3] && !flags[4] && !flags[5]);

        List<Component> oneReward = new java.util.ArrayList<>(List.of(
                line(0, "xxxxxxxxxxxxxxxxxxxxxxxxxx"),
                Component.literal(" "),
                line(14, "xxxxxxx"),
                line(4, "xxxxxxxxxx")));
        boolean[] rewardFlags = BlockLayout.centered(oneReward, WIDTH);
        check("★ 兩行的獎勵欄 span 遠窄於整塊 -> 靠左，不是置中",
              !rewardFlags[2] && !rewardFlags[3]);

        boolean[] recipes = BlockLayout.centered(List.of(
                line(0, "Crafting Level"),
                line(0, "Can be used in recipes for"),
                line(0, "Weaponsmithing  Woodworking"),
                line(0, "Tailoring  Armouring"),
                line(0, "Jeweling")), WIDTH);
        check("縮排全是 0 而寬度不一 -> 靠左",
                !recipes[0] && !recipes[1] && !recipes[2] && !recipes[3] && !recipes[4]);

        boolean[] oneOdd = BlockLayout.centered(List.of(
                line(8, "Crafting Level"),
                line(0, "Can be used in recipes for"),
                line(0, "Weaponsmithing  Woodworking"),
                line(0, "Jeweling")), WIDTH);
        check("只有一行縮排不同 -> 仍然是靠左（先前被誤判成置中）",
                !oneOdd[0] && !oneOdd[1] && !oneOdd[2] && !oneOdd[3]);

        List<Component> mixed = new java.util.ArrayList<>(List.of(
                line(0, "Durability"),
                line(0, "Duration"),
                Component.literal(" ")));
        mixed.addAll(centredBlock("This item's power has been sealed,", "its potential."));
        boolean[] mixedFlags = BlockLayout.centered(mixed, WIDTH);
        check("空行前的靠左段不受影響", !mixedFlags[0] && !mixedFlags[1]);
        check("空行後的置中段判得出來", mixedFlags[3] && mixedFlags[4]);

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
        Component gap = Component.literal(new String(Character.toChars(0xD0010)))
                .withStyle(SPACE);
        List<Component> alone = List.of(
                line(0, "AAAAAAAAAAAAAAAAAAAA"),
                gap,
                line(30, "BBBBBBBBBB"));
        boolean[] lone = BlockLayout.centered(alone, WIDTH);
        check("自成一段的置中行也認得出來", !lone[0] && lone[2]);

        List<Component> flush = List.of(
                line(0, "AAAAAAAAAAAAAAAAAAAA"),
                gap,
                line(0, "BBBBBBBBBB"));
        check("自成一段但靠左的不動", !BlockLayout.centered(flush, WIDTH)[2]);

        check("每行等寬時置中與靠左沒差別，當成靠左",
                !BlockLayout.centered(List.of(
                        line(0, "AAAA"), line(0, "BBBB")), WIDTH)[0]);

        List<Component> unidentified = List.of(
                line(0, "Waist Apron"),
                line(0, "[RARE] LEGGINGS"),
                line(0, "This item's power has been sealed,"),
                line(21, "an Item Identifier can unlock"),
                line(60, "its potential."));
        boolean[] sealedInPlace = BlockLayout.centered(unidentified, WIDTH);
        check("沒有分隔線時，仍認得出後面那串置中的說明",
                !sealedInPlace[0] && !sealedInPlace[1] && sealedInPlace[2]
                        && sealedInPlace[3] && sealedInPlace[4]);

        check("縮排全是 0 的清單不算置中",
                noneOf(BlockLayout.centered(List.of(
                        line(0, "Can be used in recipes for"),
                        line(0, "Tailoring"),
                        line(0, "Weaponsmithing"),
                        line(0, "Jeweling")), WIDTH)));

        int[] lead    = {  0,   0, 0,   4,  49, 0,  0,   0};
        int[] content = { 90,  80, 170, 159, 60, 62, 164, 164};
        check("回報的那份 tooltip：說明那三行判成置中",
                centredAt(lead, content, 2) && centredAt(lead, content, 3)
                        && centredAt(lead, content, 4));
        check("同一份裡的詞條行不受影響", !centredAt(lead, content, 6));

        int[] offContent = { 90, 80, 145, 159, 51, 62, 164, 164};
        check("字型量偏一成半，說明那三行仍判成置中",
                centredAt(lead, offContent, 2) && centredAt(lead, offContent, 3)
                        && centredAt(lead, offContent, 4));

        check("越縮排越長的清單不算置中",
                !centredAt(new int[] {0, 0, 30}, new int[] {100, 90, 150}, 2));

        int[] statLead = {10, 10, 10, 0, 3, 3, 0, 0, 3, 0};
        int[] statContent = new int[statLead.length];
        for (int i = 0; i < statLead.length; i++) {
            statContent[i] = 190 - statLead[i];
        }
        boolean anyStat = false;
        for (int i = 0; i < statLead.length; i++) {
            anyStat |= centredAt(statLead, statContent, i);
        }
        check("★ 右緣對齊的屬性表不算置中", !anyStat);
        int[] trueLead = new int[statLead.length];
        int[] trueContent = new int[statLead.length];
        for (int i = 0; i < statLead.length; i++) {
            trueLead[i] = statLead[i] + 10 * (i % 3);
            trueContent[i] = 190 - 2 * trueLead[i];
        }
        check("右緣跟著縮排內縮的仍是置中", centredAt(trueLead, trueContent, 1));

        System.out.println(failures == 0
                ? "BlockLayout: 全部通過" : "BlockLayout: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

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

}
