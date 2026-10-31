package com.wynnchayuan.translate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class NameOriginalTermTest {

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-name-original");

        Files.writeString(dir.resolve("gear-armour.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "a1": {"src": "Mask of Courage", "dst": "勇氣面具", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);

        Files.writeString(dir.resolve("ability-terms.json"),
                "{\"Mask\": \"假面\", \"Courage\": \"勇氣\"}", StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        store.setNameMode(com.wynnchayuan.ModConfig.ItemNames.BOTH);

        String shown = store.lookup("Mask of Courage");
        check("查表先附上原文（實際 " + shown + "）",
              "勇氣面具 (Mask of Courage)".equals(shown));

        String swapped = TermSubstitution.termsWithin(shown, store);
        String after = swapped == null ? shown : swapped;
        check("附的原文不被詞表動到（實際 " + after + "）",
              after.endsWith("(Mask of Courage)"));
        check("譯名本身還在（實際 " + after + "）", after.startsWith("勇氣面具"));

        String body = "Mask of the Lunatic grants Courage";
        String bodyOut = TermSubstitution.termsWithin(body, store);
        check("一般內文照樣換詞（實際 " + bodyOut + "）",
              bodyOut != null && bodyOut.contains("假面") && bodyOut.contains("勇氣"));

        String parens = "Mask (Courage)";
        String parensOut = TermSubstitution.termsWithin(parens, store);
        check("不是附原文的括號照樣換（實際 " + parensOut + "）",
              parensOut != null && parensOut.contains("假面"));

        java.util.function.ToIntFunction<String> measure = s -> {
            int w = 0;
            for (int i = 0; i < s.length(); i++) {
                w += s.charAt(i) > 0x2E80 ? 2 : 1;
            }
            return w;
        };

        String one = "勇氣面具 (Mask of Courage)";
        int origin = measure.applyAsInt("Mask of Courage");
        String broken = TermSubstitution.breakBeforeOriginal(one, origin, store, measure);
        check("太寬就斷在原文前面（實際 " + broken.replace("\n", "\\n") + "）",
              "勇氣面具\n(Mask of Courage)".equals(broken));

        String roomy = TermSubstitution.breakBeforeOriginal(one, 999, store, measure);
        check("放得下就維持一行（實際 " + roomy.replace("\n", "\\n") + "）",
              one.equals(roomy));

        check("量不出寬度就不動",
              one.equals(TermSubstitution.breakBeforeOriginal(one, 0, store, measure)));

        String body2 = "這是一段很長的說明文字 (not a gear name)";
        check("不是附原文的括號不斷",
              body2.equals(TermSubstitution.breakBeforeOriginal(body2, 1, store, measure)));

        lootrunNames();

        if (failures > 0) {
            System.err.println(failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("NameOriginalTermTest 全部通過");
    }

    private static void lootrunNames() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-lootrun-original");
        Files.writeString(dir.resolve("misc.json"), """
                {"Jester's Trick:": "甲乙之計:", "Thrill Seeker": "丙丁者",
                 "Thrill grants speed.": "Thrill 給予速度。"}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("ability-terms.json"),
                "{\"Trick\": \"戊己\", \"Thrill\": \"庚辛\"}", StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        store.setPageNameMode(com.wynnchayuan.ModConfig.ItemNames.BOTH);

        String board = store.lookup("Jester's Trick:");
        check("記分板那一格附上原文（實際 " + board + "）",
              "甲乙之計 (Jester's Trick):".equals(board));
        String swapped = TermSubstitution.termsWithin(board, store);
        String afterSwap = swapped == null ? board : swapped;
        check("★ 折行前的整句換詞不動附的原文（實際 " + afterSwap + "）",
              afterSwap.contains("(Jester's Trick)"));
        String drawn = drawn("Jester's Trick:", store);
        check("★ 畫出來的記分板也不動（實際 " + drawn + "）",
              drawn != null && drawn.contains("(Jester's Trick)") && !drawn.contains("戊己"));

        String card = drawn("Thrill Seeker", store);
        check("★ 卡片標題「譯名 (原文)」的原文原封不動（實際 " + card + "）",
              card != null && card.contains("(Thrill Seeker)") && !card.contains("庚辛"));

        String body = drawn("Thrill grants speed.", store);
        check("一般內文照樣換詞（實際 " + body + "）", body != null && body.contains("庚辛"));

        store.setPageNameMode(com.wynnchayuan.ModConfig.ItemNames.OFF);
        String off = drawn("Thrill Seeker", store);
        check("★ 只顯示原文時整格是英文名，不被換掉（實際 " + off + "）",
              off == null || (off.contains("Thrill Seeker") && !off.contains("庚辛")));
    }

    private static String drawn(String text, TranslationStore store) {
        net.minecraft.network.chat.Component out = LineTranslator.translate(
                com.wynntils.core.text.StyledText.fromComponent(
                        net.minecraft.network.chat.Component.literal(text)), store);
        return out == null ? null : out.getString();
    }

}
