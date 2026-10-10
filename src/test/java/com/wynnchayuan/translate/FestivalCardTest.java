package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 靈魂節小遊戲（Banish the Beyond）的說明卡要整張翻得出來。
 *
 * <h2>這種卡長什麼樣</h2>
 * 惡靈類型的說明是<b>條列</b>，每一項被寬度折成兩三行，續行開頭有一個縮排用的
 * 偏移字元（抽成模板就是行首的 {@code {#}}）：
 *
 * <pre>
 *   Special - False Facade
 *   - Using the Formless Heart, transform into
 *   {#}the most distant survivor. Upon disguising,
 *   {#}all survivors are inflicted with Paranoia.
 *   - Paranoia causes survivors' hearts to beat
 *   {#}whenever anyone approaches them within a
 *   …
 * </pre>
 *
 * 整段之間沒有空行，所以它在算繪端是<b>一段</b>。語料收的是「一個條列項目一句」：
 * 呼叫端從每一行開始、由長到短試，試到剛好是那一項的那幾行就會中。這條測試釘的
 * 就是那個假設——收成整段會被重新折行、條列全黏在一起；逐行收則是半中半英。
 *
 * <p>不釘譯文的措辭（翻譯團隊會改），釘的是「每一項都換掉了、沒有留半句英文」。
 */
public final class FestivalCardTest {

    private static int failures = 0;

    private static final Style GREY = Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA));

    public static void main(String[] args) {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        // 惡靈類型：標題列、條列項目、縮排的續行
        String[] facade = {
                "Special - False Facade",
                "- Using the Formless Heart, transform into",
                ">the most distant survivor. Upon disguising,",
                ">all survivors are inflicted with Paranoia.",
                "- Paranoia causes survivors' hearts to beat",
                ">whenever anyone approaches them within a",
                ">range of 16 blocks, fading over time if the",
                ">source is within line of sight or close by.",
                "- While disguised, your heartbeat radius is",
                ">reduced to 8 blocks, and you may enter",
                ">lockers or pretend to power Spirit Wells.",
                "- While disguised, your attack is delayed,",
                ">but will quickly reveal your true face",
                ">if it successfully hits a survivor.",
        };
        List<String> out = shown(card("The Hollowed", facade), store);
        allTranslated("惡靈類型的條列", out);
        check("四個條列項目各自成段（" + bullets(out) + " 個「- 」開頭）", bullets(out) == 4);
        check("第二項的 16 格還在", String.join(" ", out).contains("16"));
        check("第三項的 8 格還在", String.join(" ", out).contains("8"));

        // 續行以大寫開頭的那一項（Vulnerable）也要接得回去
        String[] shade = {
                "Special - Stalking Shade",
                "- Use the Stalking Shade to enter the hunt,",
                ">removing your heartbeat radius.",
                "- While hunting, crouch while making direct",
                ">line of sight with a survivor to stalk them.",
                "- Stalking a survivor for 5s will make them",
                ">Vulnerable for 12s, causing them to be",
                ">instantly downed by your attacks.",
                "- Being spotted while hunting will begin to",
                ">expose you, which will slow and stun you if",
                ">completely exposed, and will end the hunt.",
        };
        out = shown(card("The Stalker", shade), store);
        allTranslated("續行大寫開頭的條列", out);
        String all = String.join(" ", out);
        check("盯梢 5 秒、脆弱 12 秒沒有對調（實際：" + all + "）",
                all.indexOf("5") >= 0 && all.indexOf("5") < all.indexOf("12"));

        // 特長：被寬度折斷的一句，沒有條列
        String[] tunnel = {
                "After the final Spirit Well is powered,",
                "instantly lose the injured condition,",
                "gain Speed III for 10s, and move 15%",
                "quicker for the rest of the game.",
        };
        out = shown(card("Light in the Tunnel", tunnel), store);
        allTranslated("折斷的一句", out);

        // 一句引言加上幾個單行的條列
        String[] syndrome = {
                "While you are the last survivor alive,",
                "gain the following effects:",
                "- Your sprint trail lasts half as long.",
                "- Exit Gates are powered 25% faster.",
                "- The Escape Beacon glows when within 32 blocks.",
        };
        out = shown(card("Survivor Syndrome", syndrome), store);
        allTranslated("引言加單行條列", out);
        check("三個條列項目都在（" + bullets(out) + "）", bullets(out) == 3);

        System.out.println(failures == 0
                ? "FestivalCard: 全部通過" : "FestivalCard: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /** 標題、空行、內容。內容裡以 {@code >} 開頭的是帶縮排偏移的續行。 */
    private static List<Component> card(String title, String[] body) {
        List<Component> tip = new ArrayList<>();
        tip.add(Component.literal(title).withStyle(GREY));
        tip.add(Component.literal(""));
        for (String line : body) {
            if (!line.startsWith(">")) {
                tip.add(Component.literal(line).withStyle(GREY));
                continue;
            }
            MutableComponent row = Component.empty();
            row.append(Component.literal(SpaceOffset.encode(6))
                    .withStyle(SpaceOffset.styleFor(GREY)));
            row.append(Component.literal(line.substring(1)).withStyle(GREY));
            tip.add(row);
        }
        return tip;
    }

    /** 翻完之後內容那幾列的字（標題與空行不算，排版偏移剝掉）。 */
    private static List<String> shown(List<Component> tip, TranslationStore store) {
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(tip, store);
        List<String> rows = new ArrayList<>();
        for (int i = 2; i < out.size(); i++) {
            StringBuilder plain = new StringBuilder();
            out.get(i).getString().codePoints()
                    .filter(cp -> cp < 0xE000 || (cp > 0xF8FF && cp < 0xF0000))
                    .forEach(plain::appendCodePoint);
            rows.add(plain.toString().strip());
        }
        return rows;
    }

    private static long bullets(List<String> rows) {
        return rows.stream().filter(r -> r.startsWith("- ")).count();
    }

    /** 每一列都有中文，而且沒有留下兩個以上連在一起的英文單字（專有名詞一個字的不算）。 */
    private static void allTranslated(String what, List<String> rows) {
        check(what + "：有翻出東西（" + rows.size() + " 列）", !rows.isEmpty());
        for (String row : rows) {
            boolean han = row.codePoints().anyMatch(cp -> cp >= 0x4E00 && cp <= 0x9FFF);
            boolean english = row.matches(".*[A-Za-z]{2,}[ ,]+[a-z]{2,}.*");
            check(what + "：這一列是譯文（實際：" + row + "）", han && !english);
        }
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + name);
        if (!ok) {
            failures++;
        }
    }
}
