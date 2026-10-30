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

public final class AbilityNameOriginalTest {

    private static int failures = 0;

    private static final String ABILITY = "Arrow Bomb";
    private static final String MAJOR_ID = "Altruism";
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
        majorIdLine(store, MAJOR_ID, ALTRUISM_ROWS);
        majorIdLine(store, "Requiem", REQUIEM_ROWS);
        termLookups(store);
        inDescription(store, "Major ID 說明", majorIdBlock(REQUIEM_ROWS), true);
        inDescription(store, "技能樹說明", greyBlock(SUNFLARE_TREE_ROWS), false);
        alreadyAppended(corpus, store);

        finish();
    }

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

        int before = store.generation();
        store.setAbilityNamesWithOriginal(true);
        check("切換時世代往前推（快取跟著作廢）", store.generation() != before);
        store.setAbilityNamesWithOriginal(false);
    }

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

    private static void majorIdLine(TranslationStore store, String name, String[][] rows) {
        List<StyledText> block = majorIdBlock(rows);
        List<Component> off = LineTranslator.translateBlock(block, store, new boolean[block.size()]);
        String offText = join(off);
        check("前提：關著時整段翻得出來（" + shorten(offText) + "）", off != null && !off.isEmpty());
        check("★ 關著：Major ID 那一列沒有附原文", !offText.contains(name));

        store.setAbilityNamesWithOriginal(true);
        TranslationStore.holdAppendedOriginal(true);
        try {
            List<Component> on = LineTranslator.translateBlock(block, store,
                    new boolean[block.size()]);
            String onText = join(on);
            check("★ 開著：Major ID 名稱附了原文（" + shorten(onText) + "）",
                    onText.contains("(" + name + ")"));
            check("原文只附一次", onText.indexOf(name) == onText.lastIndexOf(name));
            Integer colour = colourOf(on, "(" + name);
            check("附的原文是名稱的顏色，不是說明的（拿到 "
                            + (colour == null ? "null" : String.format("#%06X", colour)) + "）",
                    colour != null && colour == NAME_COLOUR);
        } finally {
            TranslationStore.holdAppendedOriginal(false);
            store.setAbilityNamesWithOriginal(false);
        }
    }

    private static final String TERM = "Sunflare";

    private static final String[] SUNFLARE_TREE_ROWS = {
        "While Sunflare is active, you will restore",
        "health and mana to all nearby allies, and",
        "your Ophanim orbs will attack constantly.",
    };

    private static void termLookups(TranslationStore store) {
        String term = store.lookupTerm(TERM);
        check("前提：Sunflare 在詞表裡、譯名不是原文（" + term + "）",
                term != null && !term.equals(TERM) && !term.contains("("));
        TranslationStore.Term fishingOff = store.findTerm("Lv. 52 Fishing", 0);
        TranslationStore.Term off = store.findTerm(TERM + " regenerates", 0);
        check("關著：詞表換出來只有譯名", off != null && off.translation().equals(term));
        store.setAbilityNamesWithOriginal(true);
        try {
            String want = term + " (" + TERM + ")";
            check("★ 開著：lookupTerm 附原文（" + store.lookupTerm(TERM) + "）",
                    want.equals(store.lookupTerm(TERM)));
            check("★ 開著：lookupTerm 剝掉尾巴的那條也附原文",
                    (want + " ✺").equals(store.lookupTerm(TERM + " ✺")));
            TranslationStore.Term on = store.findTerm(TERM + " regenerates", 0);
            check("★ 開著：findTerm 附原文，範圍仍是原文那個詞",
                    on != null && want.equals(on.translation())
                            && on.start() == 0 && on.end() == TERM.length());
            TranslationStore.Term fishing = store.findTerm("Lv. 52 Fishing", 0);
            check("不是技能名的詞不附原文（Fishing）", fishingOff != null && fishing != null
                    && fishingOff.translation().equals(fishing.translation()));
            check("附上去的原文被擋著不再換", store.keptOriginals(want + " x").size() == 1);
            check("折行把附的原文推到下一行開頭，照樣擋著",
                    store.keptOriginals("(" + TERM + ") x").size() == 1);
            check("折行切在譯名中間，下一行開頭是譯名的後半，照樣擋著",
                    term.length() < 2 || store.keptOriginals(
                            term.substring(1) + " (" + TERM + ") x").size() == 1);
        } finally {
            store.setAbilityNamesWithOriginal(false);
        }
    }

    private static void inDescription(TranslationStore store, String what,
                                      List<StyledText> block, boolean hold) {
        String term = store.lookupTerm(TERM);
        if (term == null) {
            return;
        }
        String appended = "(" + TERM + ")";
        String offText = join(LineTranslator.translateBlock(block, store,
                new boolean[block.size()]));
        check(what + "：關著時技能名換成譯名、沒有附原文（" + shorten(offText) + "）",
                offText.contains(term) && !offText.contains(appended));
        store.setAbilityNamesWithOriginal(true);
        TranslationStore.holdAppendedOriginal(hold);
        try {
            List<Component> on = LineTranslator.translateBlock(block, store,
                    new boolean[block.size()]);
            String onText = join(on);
            check("★ " + what + "：開著時句子裡的技能名附了原文（" + shorten(onText) + "）",
                    onText.contains(term + " " + appended));
            check(what + "：原文只出現一次", onText.indexOf(TERM) == onText.lastIndexOf(TERM));
            check(what + "：附的原文沒有再被換成譯名", !onText.contains("(" + term));
            Integer colour = colourOf(on, appended);
            check(what + "：附的原文是說明的顏色（拿到 "
                            + (colour == null ? "null" : String.format("#%06X", colour)) + "）",
                    colour != null && colour == BODY_COLOUR);
            check(what + "：行數跟關著時一樣",
                    on != null && onText.split("\n", -1).length == offText.split("\n", -1).length);
        } finally {
            TranslationStore.holdAppendedOriginal(false);
            store.setAbilityNamesWithOriginal(false);
        }
    }

    private static void alreadyAppended(Path corpus, TranslationStore base) throws Exception {
        String term = base.lookupTerm(TERM);
        if (term == null) {
            return;
        }
        Path dir = Files.createTempDirectory("wcy-ability-original-flat");
        com.google.gson.JsonObject flat = new com.google.gson.JsonObject();
        String key = "Zq the " + TERM + " line.";
        flat.addProperty(key, "zq " + term + " (" + TERM + ") zq");
        Files.writeString(dir.resolve("npc.json"), flat.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(List.of(corpus, dir));
        store.setAbilityNamesWithOriginal(true);
        StyledText row = StyledText.fromComponent(Component.literal(key).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR))));
        String got = text(LineTranslator.translate(row, store));
        check("★ 譯文本來就寫了「譯名 (原文)」：不再附第二次（" + got + "）",
                got.contains(term + " (" + TERM + ")")
                        && got.indexOf(TERM) == got.lastIndexOf(TERM));
    }

    private static List<StyledText> greyBlock(String[] rows) {
        List<StyledText> out = new ArrayList<>();
        for (String row : rows) {
            out.add(StyledText.fromComponent(Component.literal(row).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR)))));
        }
        return out;
    }

    private static final String[][] ALTRUISM_ROWS = {
        {"✦ Altruism: ", "Whenever you gain"},
        {null, "health from your Life Steal"},
        {null, "and Health Regen IDs, allies"},
        {null, "within 16 blocks will receive"},
        {null, "the same amount."},
    };

    private static final String[][] REQUIEM_ROWS = {
        {"✦ Requiem: ", "Sunflare regenerates"},
        {null, "twice the health and mana."},
    };

    private static List<StyledText> majorIdBlock(String[][] rows) {
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
