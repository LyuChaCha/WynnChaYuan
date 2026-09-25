package com.wynnchayuan.translate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 「譯名 + 原文」模式附在後面的原文<b>不能</b>再被詞表換一次。
 *
 * <h2>實機回報</h2>
 * F6 的物品名稱選「譯名 + 原文」時，Legendary 頭盔 {@code Mask of Courage}
 * 畫出來是：
 *
 * <pre>
 *   勇氣面具 (假面 of 勇氣)
 * </pre>
 *
 * <p>括號裡本來要放的是<b>原封不動的英文</b>——那是這個模式存在的理由，
 * 玩家拿它去對 wiki 與交易市場。但 {@code 假面} 與 {@code 勇氣} 分別是
 * {@code ability-terms.json} 裡 Shaman 的 {@code Mask} 與 {@code Courage}，
 * 於是括號裡的英文被逐詞換掉，只剩 {@code of} 沒有對應的詞而留著。
 *
 * <p>換句話說：附原文的那一段被當成<b>還沒翻的內文</b>處理了。折行前的
 * {@code termsWithin} 對整句掃一次（技能名要在折行前換，見 LineTranslator），
 * 掃到的整句包含了剛剛才附上去的原文。
 *
 * <h2>這裡釘住什麼</h2>
 * 兩個方向都要測，只測前者的話「詞表整個關掉」也會過：
 *
 * <ul>
 *   <li>附在名稱後面的原文原封不動；</li>
 *   <li>同一批詞在<b>一般內文</b>裡照樣換——那是詞表本來的工作。</li>
 * </ul>
 */
public final class NameOriginalTermTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-name-original");

        // 裝備名翻好了，而且只有裝備檔有這個原文（gearOnly 才會附原文）。
        Files.writeString(dir.resolve("gear-armour.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "a1": {"src": "Mask of Courage", "dst": "勇氣面具", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);

        // Shaman 的技能詞，實機就是這兩條把括號裡的英文吃掉的。
        Files.writeString(dir.resolve("ability-terms.json"),
                "{\"Mask\": \"假面\", \"Courage\": \"勇氣\"}", StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        store.setNameMode(com.wynnchayuan.CollectorConfig.ItemNames.BOTH);

        String shown = store.lookup("Mask of Courage");
        check("查表先附上原文（實際 " + shown + "）",
              "勇氣面具 (Mask of Courage)".equals(shown));

        // ★ 回報的那一步：折行前對整句換詞。
        String swapped = LineTranslator.termsWithin(shown, store);
        String after = swapped == null ? shown : swapped;
        check("附的原文不被詞表動到（實際 " + after + "）",
              after.endsWith("(Mask of Courage)"));
        check("譯名本身還在（實際 " + after + "）", after.startsWith("勇氣面具"));

        // ★ 反方向：一般內文裡這兩個詞照換，否則「把詞表關掉」也會過。
        String body = "Mask of the Lunatic grants Courage";
        String bodyOut = LineTranslator.termsWithin(body, store);
        check("一般內文照樣換詞（實際 " + bodyOut + "）",
              bodyOut != null && bodyOut.contains("假面") && bodyOut.contains("勇氣"));

        // ★ 名稱後面<b>不是</b>附原文的括號時（敘述本來就帶括號），照樣換。
        String parens = "Mask (Courage)";
        String parensOut = LineTranslator.termsWithin(parens, store);
        check("不是附原文的括號照樣換（實際 " + parensOut + "）",
              parensOut != null && parensOut.contains("假面"));

        // ---- 附的原文太寬時要挪到下一行（回報的「版面過長」）----
        //
        // 量法在測試裡自己給：正式的要 Minecraft 的字型，測試環境量出來全是 0，
        // 寬度那幾條規則就永遠不會被觸發。這裡一個字算一格，中文算兩格。
        java.util.function.ToIntFunction<String> measure = s -> {
            int w = 0;
            for (int i = 0; i < s.length(); i++) {
                w += s.charAt(i) > 0x2E80 ? 2 : 1;
            }
            return w;
        };

        String one = "勇氣面具 (Mask of Courage)";
        int origin = measure.applyAsInt("Mask of Courage");   // 原文那一行的寬度
        String broken = LineTranslator.breakBeforeOriginal(one, origin, store, measure);
        check("太寬就斷在原文前面（實際 " + broken.replace("\n", "\\n") + "）",
              "勇氣面具\n(Mask of Courage)".equals(broken));

        // 放得進原文寬度的不斷——短名稱拆兩行只是白佔一行。
        String roomy = LineTranslator.breakBeforeOriginal(one, 999, store, measure);
        check("放得下就維持一行（實際 " + roomy.replace("\n", "\\n") + "）",
              one.equals(roomy));

        // 量不出寬度（測試環境、非面板路徑）時不動。
        check("量不出寬度就不動",
              one.equals(LineTranslator.breakBeforeOriginal(one, 0, store, measure)));

        // 不是附原文的一般長句不斷——那交給 wrapBalanced。
        String body2 = "這是一段很長的說明文字 (not a gear name)";
        check("不是附原文的括號不斷",
              body2.equals(LineTranslator.breakBeforeOriginal(body2, 1, store, measure)));

        if (failures > 0) {
            System.err.println(failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("NameOriginalTermTest 全部通過");
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) {
            failures++;
        }
    }
}
