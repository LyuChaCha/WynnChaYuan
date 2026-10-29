package com.wynnchayuan.translate;

import com.wynnchayuan.CollectorConfig;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * F6「技能名稱附原文」：技能名稱與 Major ID 名稱後面括號附原文，跟物品名稱的
 * 「譯名加原文」同一個畫法。
 *
 * <p>用真正的語料跑（技能樹與 Major ID 的檔），但<b>不寫死譯名</b>——術語統一的 PR
 * 會改它。拿開關關著時查到的譯名當基準，開著時就該是「基準 (原文)」。
 * 另外疊一層自己的技能檔，放一個譯名跟原文一模一樣的名字。
 */
public final class AbilityNameOriginalTest {

    private static int failures = 0;

    private static final String ABILITY = "Arrow Bomb";
    private static final String MAJOR_ID = "Altruism";
    /** 疊上去的技能檔裡那一個「譯名就是原文」的名字。 */
    private static final String SAME = "Zzyzx Strike";

    private static final int NAME_COLOUR = 0xFF55FF;
    private static final int BODY_COLOUR = 0xAAAAAA;

    public static void main(String[] args) throws Exception {
        Path corpus = Path.of("src/main/resources/assets/wynnchayuan/translations",
                Languages.DEFAULT);
        Path extra = Files.createTempDirectory("wcy-ability-original");
        Files.createDirectories(extra.resolve("ability"));
        Files.writeString(extra.resolve("ability").resolve("zz-test.json"), """
                {
                  "_meta": {"domain": "ability/zz-test", "itemNames": false},
                  "entries": {
                    "zz#0": {"src": "Zzyzx Strike", "dst": "Zzyzx Strike", "role": "name"}
                  }
                }
                """, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(List.of(corpus, extra));

        defaults();
        String ability = store.lookup(ABILITY);
        String majorId = store.lookup(MAJOR_ID);
        check("前提：技能名有譯名、而且不是原文（" + ability + "）",
                ability != null && !ability.equals(ABILITY) && !ability.contains("("));
        check("前提：Major ID 名有譯名、而且不是原文（" + majorId + "）",
                majorId != null && !majorId.equals(MAJOR_ID) && !majorId.contains("("));
        if (ability == null || majorId == null) {
            finish();
            return;
        }

        lookups(store, ability, majorId);
        abilityRow(store);
        majorIdLine(store);

        finish();
    }

    /** 預設關著（跟物品名稱一樣不附原文），設定檔讀得回來。 */
    private static void defaults() throws Exception {
        Path dir = Files.createTempDirectory("wcy-ability-original-cfg");
        CollectorConfig fresh = new CollectorConfig(dir.resolve("fresh.json"));
        check("★ 預設關著", !fresh.abilityNamesOriginal());
        check("物品名稱的預設也是不附原文（OFF）",
                fresh.itemNames() == CollectorConfig.ItemNames.OFF);
        Path on = dir.resolve("on.json");
        Files.writeString(on, "{\"abilityNamesOriginal\": true}");
        check("設定檔寫 true 讀得回來", new CollectorConfig(on).abilityNamesOriginal());
        check("查詢表的預設也是關著", !new TranslationStore().abilityNamesWithOriginal());
    }

    /** 查表那一層：開著附原文，關著不附，譯名就是原文的不重複。 */
    private static void lookups(TranslationStore store, String ability, String majorId) {
        try {
            store.setAbilityNamesWithOriginal(true);
            check("★ 開著：技能名是「譯名 (原文)」",
                    (ability + " (" + ABILITY + ")").equals(store.lookup(ABILITY)));
            check("★ 開著：Major ID 名是「譯名 (原文)」",
                    (majorId + " (" + MAJOR_ID + ")").equals(store.lookup(MAJOR_ID)));
            String same = store.lookup(SAME);
            check("★ 譯名就是原文的不重複附（" + same + "）",
                    same == null || SAME.equals(same));
            check("還沒翻的名字照舊查不到", store.lookup("Qwfp Arst Zxcv") == null);
            check("開著不影響物品名稱的模式（物品名稱照舊不附）", !store.namesWithOriginal());
        } finally {
            store.setAbilityNamesWithOriginal(false);
        }
        check("★ 關著：技能名只有譯名", ability.equals(store.lookup(ABILITY)));
        check("★ 關著：Major ID 名只有譯名", majorId.equals(store.lookup(MAJOR_ID)));

        // 切換要讓繪製端的快取作廢
        int before = store.generation();
        store.setAbilityNamesWithOriginal(true);
        check("切換時世代往前推（快取跟著作廢）", store.generation() != before);
        store.setAbilityNamesWithOriginal(false);
    }

    /** 技能樹節點的標題那一列：整列翻出來，原文跟著譯名同一個顏色。 */
    private static void abilityRow(TranslationStore store) {
        StyledText row = StyledText.fromComponent(Component.literal(ABILITY).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF)).withBold(true)));
        Component off = LineTranslator.translate(row, store);
        check("關著：標題那一列沒有附原文（" + text(off) + "）",
                off != null && !text(off).contains(ABILITY));
        store.setAbilityNamesWithOriginal(true);
        try {
            Component on = LineTranslator.translate(row, store);
            check("★ 開著：標題那一列附了原文（" + text(on) + "）",
                    on != null && text(on).contains("(" + ABILITY + ")"));
            Integer colour = colourOf(on, "(" + ABILITY);
            check("附的原文跟譯名同一個顏色", colour != null && colour == 0x55FFFF);
        } finally {
            store.setAbilityNamesWithOriginal(false);
        }
    }

    /**
     * 裝備說明裡的 Major ID：「✦ Altruism: 說明…」名稱與說明各有顏色、依寬度斷行。
     * 走 tooltip 那一條路（第三行以後，物品名稱的「附原文」在那裡是擋住的）。
     */
    private static void majorIdLine(TranslationStore store) {
        List<StyledText> block = majorIdBlock();
        List<Component> off = LineTranslator.translateBlock(block, store, new boolean[block.size()]);
        String offText = join(off);
        check("前提：關著時整段翻得出來（" + shorten(offText) + "）", off != null && !off.isEmpty());
        check("★ 關著：Major ID 那一列沒有附原文", !offText.contains(MAJOR_ID));

        store.setAbilityNamesWithOriginal(true);
        TranslationStore.holdAppendedOriginal(true);   // 物品說明第三行以後的樣子
        try {
            List<Component> on = LineTranslator.translateBlock(block, store,
                    new boolean[block.size()]);
            String onText = join(on);
            check("★ 開著：Major ID 名稱附了原文（" + shorten(onText) + "）",
                    onText.contains("(" + MAJOR_ID + ")"));
            check("原文只附一次", onText.indexOf(MAJOR_ID) == onText.lastIndexOf(MAJOR_ID));
            Integer colour = colourOf(on, "(" + MAJOR_ID);
            check("附的原文是名稱的顏色，不是說明的（拿到 "
                            + (colour == null ? "null" : String.format("#%06X", colour)) + "）",
                    colour != null && colour == NAME_COLOUR);
        } finally {
            TranslationStore.holdAppendedOriginal(false);
            store.setAbilityNamesWithOriginal(false);
        }
    }

    private static List<StyledText> majorIdBlock() {
        String[][] rows = {
            {"✦ Altruism: ", "Whenever you gain"},
            {null, "health from your Life Steal"},
            {null, "and Health Regen IDs, allies"},
            {null, "within 16 blocks will receive"},
            {null, "the same amount."},
        };
        List<StyledText> out = new ArrayList<>();
        for (String[] row : rows) {
            MutableComponent line = Component.empty();
            if (row[0] != null) {
                line.append(Component.literal(row[0]).withStyle(
                        Style.EMPTY.withColor(TextColor.fromRgb(NAME_COLOUR))));
            }
            line.append(Component.literal(row[1]).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR))));
            out.add(StyledText.fromComponent(line));
        }
        return out;
    }

    /** 含 {@code needle} 開頭那個字的片段是什麼顏色。 */
    private static Integer colourOf(Component line, String needle) {
        return colourOf(line == null ? List.of() : List.of(line), needle);
    }

    private static Integer colourOf(List<Component> lines, String needle) {
        if (lines == null) {
            return null;
        }
        for (Component line : lines) {
            String all = line.getString();
            int at = all.indexOf(needle);
            if (at < 0) {
                continue;
            }
            int[] pos = {0};
            Integer[] found = {null};
            line.visit((style, piece) -> {
                int start = pos[0];
                pos[0] += piece.length();
                if (found[0] == null && at >= start && at < pos[0]) {
                    found[0] = style.getColor() == null ? null : style.getColor().getValue();
                }
                return Optional.empty();
            }, Style.EMPTY);
            return found[0];
        }
        return null;
    }

    private static String text(Component c) {
        return c == null ? "<null>" : c.getString();
    }

    private static String join(List<Component> lines) {
        StringBuilder out = new StringBuilder();
        if (lines != null) {
            for (Component line : lines) {
                out.append(line.getString()).append('\n');
            }
        }
        return out.toString();
    }

    private static String shorten(String s) {
        String one = s.replace('\n', '｜');
        return one.length() > 40 ? one.substring(0, 40) + "…" : one;
    }

    private static void finish() {
        System.out.println(failures == 0 ? "技能名稱附原文：全部通過"
                                         : "技能名稱附原文：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
