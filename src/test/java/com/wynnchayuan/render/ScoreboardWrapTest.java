package com.wynnchayuan.render;

import com.wynntils.core.text.StyledText;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.List;

/**
 * 記分板疊層的折行（{@link ScoreboardWrap#wrap}）。
 *
 * <p>實機回報（2026-10-19）：「獻祭儀式 (Sacrificial Ritual)」超出記分板的框。
 * 寬度用固定字寬的斷行器模擬：英文與空白 6 px，中文 9 px。
 */
public final class ScoreboardWrapTest {

    private static int failures = 0;

    private static final StringSplitter SPLITTER = new StringSplitter(
            (codePoint, style) -> codePoint >= 0x2E80 ? 9f : 6f);

    public static void main(String[] args) {
        int gold = 0xFFAA00;
        Style orange = Style.EMPTY.withColor(TextColor.fromRgb(gold));
        StyledText trial = StyledText.fromComponent(
                Component.literal("獻祭儀式 (Sacrificial Ritual)").withStyle(orange));
        float box = 6f * "play.wynncraft.com!!".length();   // 標題撐出來的框（120 px）

        List<StyledText> rows = ScoreboardWrap.wrap(trial, box, SPLITTER);
        check("★ 比框寬的那一列折成兩列（實際 " + rows.size() + " 列）", rows.size() == 2);
        check("★ 每一列都不超過框寬",
              rows.stream().allMatch(r -> SPLITTER.stringWidth(r.getComponent()) <= box + 0.5f));
        check("折在空白：第一列是譯名（實際「" + rows.get(0).getStringWithoutFormatting() + "」）",
              rows.get(0).getStringWithoutFormatting().strip().equals("獻祭儀式"));
        check("續行不以空白開頭（實際「" + rows.get(1).getStringWithoutFormatting() + "」）",
              rows.get(1).getStringWithoutFormatting().equals("(Sacrificial Ritual)"));
        check("顏色跟著走", colour(rows.get(1)) == gold);

        StyledText fits = StyledText.fromString("- 剩餘時間: 13:36");
        List<StyledText> same = ScoreboardWrap.wrap(fits, box, SPLITTER);
        check("塞得下的列原樣（同一個物件）", same.size() == 1 && same.get(0) == fits);

        MutableComponent bullet = Component.empty();
        bullet.append(Component.literal("- ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA))));
        bullet.append(Component.literal("獲得 1/11 結算抽數，另外再加上一段很長的說明").withStyle(orange));
        List<StyledText> listed = ScoreboardWrap.wrap(StyledText.fromComponent(bullet), box, SPLITTER);
        check("條列項目折開（實際 " + listed.size() + " 列）", listed.size() >= 2);
        check("條列的續行縮排對齊內文（實際「" + listed.get(1).getStringWithoutFormatting() + "」）",
              listed.get(1).getStringWithoutFormatting().startsWith("  ")
                      && !listed.get(1).getStringWithoutFormatting().startsWith("   "));

        if (failures > 0) {
            System.out.println("記分板折行：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("記分板折行：全部通過");
    }

    private static int colour(StyledText row) {
        int[] found = {-1};
        row.getComponent().visit((style, text) -> {
            if (found[0] < 0 && !text.isBlank() && style.getColor() != null) {
                found[0] = style.getColor().getValue();
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static void check(String what, boolean ok) {
        if (ok) {
            System.out.println("  [ok] " + what);
        } else {
            System.out.println("  [FAIL] " + what);
            failures++;
        }
    }
}
