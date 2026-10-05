package com.wynnchayuan.render;

import com.wynnchayuan.translate.SpaceOffset;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@code /class} 的第一個原型圖示要被拉回跟其他格子同一個錨點；Wynncraft 自己
 * 修好之後這裡要自動變成什麼都不做。
 *
 * <p>資料照 2026-10-05 實機的 {@code actionbar-columns-2.txt} 原文側縮排：
 * 提示列 {@code bottom_middle} 淨寬 +39 排在第二個，第一格在它前面。
 */
public final class SelectorRealignTest {

    private static int failures = 0;

    private static final String ARCH0 = "hud/selector/default/center_left/0/archetype";
    private static final String ARCH1 = "hud/selector/default/center_left/1/archetype";
    private static final String DESC2 = "hud/selector/default/center_left/2/description";
    private static final String BOTTOM = "hud/selector/default/bottom_middle";
    private static final String OURS = "wynnchayuan:actionbar/zh_tw/selector_desc_2";

    public static void main(String[] args) {
        // 跟實機一樣的順序：第一格、提示列（淨 +39）、其餘格子
        MutableComponent broken = Component.empty();
        slot(broken, ARCH0, 12, "icon", 0);
        slot(broken, BOTTOM, -210, "hint-row", 39);
        slot(broken, DESC2, 36, "description", 0);
        slot(broken, ARCH1, 12, "icon", 0);

        SelectorRealign.Result fixed = SelectorRealign.apply(broken, SelectorRealignTest::fake);
        check("站錯的那一格被矯正", fixed.changed());
        check("整行總寬不變（" + width(broken) + " -> " + width(fixed.out()) + "）",
              width(broken) == width(fixed.out()));
        List<Integer> starts = starts(fixed.out());
        check("矯正後每一格的起點一致（實際 " + starts + "）",
              starts.stream().distinct().count() == 1);
        check("只插了一對位移字元（實際 " + spaces(fixed.out()) + " 個）",
              spaces(fixed.out()) == 2);
        check("說明寫出錨點", fixed.report().contains("錨點 = 行尾 +39"));

        SelectorRealign.Result again = SelectorRealign.apply(fixed.out(), SelectorRealignTest::fake);
        check("再跑一次不會再插（冪等）", !again.changed());

        // Wynncraft 修好的樣子之一：第一格移到提示列後面
        MutableComponent reordered = Component.empty();
        slot(reordered, BOTTOM, -210, "hint-row", 39);
        slot(reordered, ARCH0, 12, "icon", 0);
        slot(reordered, DESC2, 36, "description", 0);
        check("格子順序對了就什麼都不做",
              !SelectorRealign.apply(reordered, SelectorRealignTest::fake).changed());

        // Wynncraft 修好的樣子之二：提示列淨寬改成 0
        MutableComponent balanced = Component.empty();
        slot(balanced, ARCH0, 12, "icon", 0);
        slot(balanced, BOTTOM, -230, "hint-row", 0);
        slot(balanced, DESC2, 36, "description", 0);
        check("提示列淨寬 0 就什麼都不做",
              !SelectorRealign.apply(balanced, SelectorRealignTest::fake).changed());

        // 已翻的格子：配對字型的譯文加補白夾在中間，仍然是同一格
        MutableComponent translated = Component.empty();
        slot(translated, ARCH0, 12, "icon", 0);
        slot(translated, BOTTOM, -210, "hint-row", 39);
        translated.append(piece(DESC2, SpaceOffset.encode(36)));
        translated.append(piece(OURS, "譯文"));
        translated.append(piece("space", SpaceOffset.encode("description".length() - 2)));
        translated.append(piece(DESC2, SpaceOffset.encode(-36 - "description".length())));
        slot(translated, ARCH1, 12, "icon", 0);
        SelectorRealign.Result mixed = SelectorRealign.apply(translated, SelectorRealignTest::fake);
        check("已翻的格子不會被切成兩半（只插一對）", mixed.changed() && spaces(mixed.out()) == 3);
        check("已翻那一行的總寬也不變", width(translated) == width(mixed.out()));

        // Wynncraft 送來的元件順序不固定：排在提示列前面的格子比較多時，
        // 錨點仍然是行尾（提示列後面那一格的起點），不能靠多數決。
        MutableComponent frontHeavy = Component.empty();
        slot(frontHeavy, ARCH0, 12, "icon", 0);
        slot(frontHeavy, DESC2, 36, "description", 0);
        slot(frontHeavy, ARCH1, 12, "icon", 0);
        slot(frontHeavy, BOTTOM, -210, "hint-row", 39);
        slot(frontHeavy, DESC2, 36, "description", 0);
        SelectorRealign.Result heavy = SelectorRealign.apply(frontHeavy, SelectorRealignTest::fake);
        check("前面的格子多也以行尾為錨點", heavy.changed() && heavy.report().contains("行尾 +39"));
        check("前面那三格一起被搬到 +39（實際 " + starts(heavy.out()) + "）",
              starts(heavy.out()).stream().allMatch(s -> s == 39));
        check("後面那一格沒被動（只插一對）", spaces(heavy.out()) == 2);

        // 不是 selector 的行一個字都不碰
        MutableComponent plain = Component.empty();
        plain.append(piece("default", "100/100"));
        SelectorRealign.Result none = SelectorRealign.apply(plain, SelectorRealignTest::fake);
        check("沒有定位格就原樣回傳", !none.changed() && none.out() == plain && none.report().isEmpty());

        System.out.println(failures == 0 ? "定位格矯正：全部通過"
                                         : "定位格矯正：" + failures + " 項失敗");
        if (failures != 0) {
            System.exit(1);
        }
    }

    /**
     * {@code [跳 +a] 內容 [退回 -b]} 三段，同一份字型；退回量照 {@code net}
     * 算回來（假的量寬是一個字 1 px，不能直接抄實機的數字）。
     */
    private static void slot(MutableComponent out, String font, int jump, String body, int net) {
        out.append(piece(font, SpaceOffset.encode(jump)));
        out.append(piece(font, body));
        out.append(piece(font, SpaceOffset.encode(net - jump - body.length())));
    }

    private static Component piece(String font, String text) {
        Identifier id = font.contains(":") ? Identifier.parse(font)
                                           : Identifier.withDefaultNamespace(font);
        return Component.literal(text).setStyle(
                Style.EMPTY.withFont(new FontDescription.Resource(id)));
    }

    /** 假的量寬：位移字元照碼位解、其餘一個字 1 px。 */
    private static int fake(Component piece) {
        int[] total = {0};
        piece.visit((style, text) -> {
            text.codePoints().forEach(cp -> {
                int px = cp - 0xD0000;
                total[0] += px >= -1024 && px <= 1024 ? px : 1;
            });
            return Optional.empty();
        }, Style.EMPTY);
        return total[0];
    }

    private static int width(Component message) {
        return fake(message);
    }

    /** 每一個定位格的起點（相對行首）。 */
    private static List<Integer> starts(Component message) {
        List<Integer> out = new ArrayList<>();
        int[] cursor = {0};
        boolean[] open = {false};
        message.visit((style, text) -> {
            boolean fixed = PairedFont.absolutelyPositioned(style);
            boolean ours = PairedFont.isOurs(style) || SpaceOffset.isSpaceFont(style);
            if (!open[0] && fixed) {
                out.add(cursor[0]);
                open[0] = true;
            } else if (open[0] && !fixed && !ours) {
                open[0] = false;
            }
            cursor[0] += fake(Component.literal(text).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static int spaces(Component message) {
        int[] count = {0};
        message.visit((style, text) -> {
            if (SpaceOffset.isSpaceFont(style)) {
                count[0]++;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return count[0];
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ✓ " : "  ✗ ") + what);
        if (!ok) {
            failures++;
        }
    }
}
