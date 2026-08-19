package com.wynnchayuan;

import com.wynnchayuan.capture.DialogueBuffer;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynntils.core.text.StyledText;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;

/**
 * 驗證符號判斷的行為。刻意寫成 main 而非 JUnit，方便直接用 loom 的 classpath 跑。
 *
 * <p>存在的理由：曾經把 {@code isCustomFont} 寫成 {@code font != null}，
 * 導致每一段普通文字都被當成符號、整個對話收不到任何東西。這個測試把那個
 * 情境固定下來。
 */
public final class GlyphSplitterTest {

    private static int failures = 0;

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        // --- isCustomFont ---------------------------------------------------
        check("null 字型不算自訂",
                !GlyphSplitter.isCustomFont(null));

        check("FontDescription.DEFAULT 不算自訂（這就是當初的 bug）",
                !GlyphSplitter.isCustomFont(FontDescription.DEFAULT));

        check("minecraft:default 不算自訂",
                !GlyphSplitter.isCustomFont(
                        new FontDescription.Resource(Identifier.withDefaultNamespace("default"))));

        check("common 算自訂（Wynncraft 的符號字型）",
                GlyphSplitter.isCustomFont(
                        new FontDescription.Resource(Identifier.withDefaultNamespace("common"))));

        // --- 純文字必須能通過 ------------------------------------------------
        StyledText plain = StyledText.fromString("Hello traveller!");
        check("純文字不是 glyphOnly", !GlyphSplitter.isGlyphOnly(plain));
        check("純文字模板保持原樣",
                "Hello traveller!".equals(GlyphSplitter.toTemplate(plain)));

        // 有顏色碼的文字（對話最常見的形態）
        StyledText colored = StyledText.fromString("§7Speak to the §fGuard§7.");
        check("帶顏色碼的文字不是 glyphOnly", !GlyphSplitter.isGlyphOnly(colored));
        check("帶顏色碼的文字仍抽得到字",
                GlyphSplitter.toTemplate(colored).contains("Speak to the"));

        // --- PUA 判斷 -------------------------------------------------------
        check("PUA 碼位被認出", GlyphSplitter.isPrivateUse(0xE007));
        check("一般字母不是 PUA", !GlyphSplitter.isPrivateUse('A'));
        check("純 PUA 字串沒有字母", !GlyphSplitter.hasLetter(""));
        check("中文算字母", GlyphSplitter.hasLetter("魔力消耗"));

        // --- 未分配平面（實際踩到的坑）-----------------------------------------
        // Wynncraft 的對話游標用 U+D0000..U+D0074，那是第 13 平面，完全未分配，
        // 不是 PUA。寫死 PUA 範圍會整批漏掉，導致前綴比對失敗、碎片全部被記錄。
        check("U+D005B 算圖示（第 13 平面）", GlyphSplitter.isGlyphCodePoint(0xD005B));
        check("U+D0000 算圖示", GlyphSplitter.isGlyphCodePoint(0xD0000));
        check("U+D0074 算圖示", GlyphSplitter.isGlyphCodePoint(0xD0074));
        check("U+D005B 不是 PUA（兩者要分得開）", !GlyphSplitter.isPrivateUse(0xD005B));
        check("真正的 PUA 也算圖示", GlyphSplitter.isGlyphCodePoint(0xE007));
        check("字母不算圖示", !GlyphSplitter.isGlyphCodePoint('A'));
        check("中文不算圖示", !GlyphSplitter.isGlyphCodePoint(0x4E2D));
        check("日文不算圖示", !GlyphSplitter.isGlyphCodePoint(0x3042));
        check("彎引號不算圖示", !GlyphSplitter.isGlyphCodePoint(0x2019));

        // --- 混在文字裡的圖示字元要被清掉 --------------------------------------
        check("第 13 平面字元被移除",
                "H-hey".equals(GlyphSplitter.stripGlyphChars(
                        "H-hey" + new String(Character.toChars(0xD005B)))));
        check("PUA 字元被移除",
                "H-hey".equals(GlyphSplitter.stripGlyphChars(
                        "H-hey" + new String(Character.toChars(0xE007)))));
        check("沒有圖示時字串不變",
                "Hello".equals(GlyphSplitter.stripGlyphChars("Hello")));

        dialogueBufferChecks();
        playerDataChecks();
        numberChecks();

        System.out.println(failures == 0
                ? "\n全部通過"
                : "\n失敗 " + failures + " 項");
        System.exit(failures == 0 ? 0 : 1);
    }

    /**
     * 用真實資料驗證逐字打字的收斂。
     *
     * <p>這串前綴取自實際跑出來的 captured.json——當時 62 次 Finished 事件
     * 產生了 34 條垃圾碎片，只有 1 條是完整的。
     */
    private static void dialogueBufferChecks() {
        System.out.println("\n  -- DialogueBuffer --");
        DialogueBuffer buf = new DialogueBuffer();
        String full = "H-hey, human- URGH! Help me out here! Some damn guards beat me bloody...";

        int emitted = 0;
        for (int i = 1; i <= full.length(); i++) {
            if (buf.offer(full.substring(0, i)) != null) {
                emitted++;
            }
        }
        check("打字過程中一句都不送出", emitted == 0);
        check("flush 拿到完整的那句", full.equals(buf.flush()));
        check("flush 後緩衝區清空", buf.flush() == null);

        // 換到下一句時，上一句要被送出
        DialogueBuffer buf2 = new DialogueBuffer();
        buf2.offer("H-hey");
        buf2.offer("H-hey, human!");
        String done = buf2.offer("I need somethin' to patch myself up...");
        check("換句時送出前一句", "H-hey, human!".equals(done));
        check("新的一句留在緩衝區",
                "I need somethin' to patch myself up...".equals(buf2.flush()));

        // 重複內容不該重複送出
        DialogueBuffer buf3 = new DialogueBuffer();
        buf3.offer("Same text");
        check("內容沒變不送出", buf3.offer("Same text") == null);

        // 真實情境：每個按鍵後面都跟著一個「不同的」第 13 平面游標字元。
        // 這正是上一版失敗的原因——沒清掉游標，前綴比對就對不上，
        // 結果 459 條全是碎片。
        DialogueBuffer buf4 = new DialogueBuffer();
        String line = "Listen, human, you gotta get me out of here!";
        int leaked = 0;
        for (int i = 1; i <= line.length(); i++) {
            String cursor = new String(Character.toChars(0xD0000 + (i % 0x74)));
            String withCursor = line.substring(0, i) + cursor;
            if (buf4.offer(GlyphSplitter.stripGlyphChars(withCursor)) != null) {
                leaked++;
            }
        }
        check("帶游標的逐字輸入不產生碎片", leaked == 0);
        check("帶游標的逐字輸入最後拿到完整句", line.equals(buf4.flush()));
    }

    /** 用實際收集到的訊息驗證個資過濾。 */
    private static void playerDataChecks() {
        System.out.println();
        System.out.println("  -- PlayerDataFilter --");
        String[] shouldBlock = {
            "{#} Green_teaTW's friends (43): Haagen_Dazs69, WD69, GreenTEA6666,",
            "{#} {#} PoorChaCha {#} shouts: 晚安",
            "eric18960 has logged into server AS12 as a Knight",
            "{#}  - Rabbit_flower [Server: AS4]",
            "[!] Congratulations to I Tonk you Bonk for reaching level 110 in Woodworking!",
            "{#}你死在了 [-781, 89, -5563]",
        };
        for (String s : shouldBlock) {
            check("擋下: " + s.substring(0, Math.min(38, s.length())),
                    PlayerDataFilter.carriesPlayerData(s));
        }

        // 這些是真正該翻譯的介面字串，不能誤擋
        String[] shouldKeep = {
            "{#} You don't have enough mana to cast that spell!",
            "{#} Sorry, you can't teleport... Try moving away from blocks.",
            "{#} Type 'yes' to accept or 'no' to try again",
            "[You are now entering Ragni]",
            "{#} Online Friends:",
            "{#}Your ability tree is outdated, click here to update.",
        };
        for (String s : shouldKeep) {
            check("保留: " + s.substring(0, Math.min(38, s.length())),
                    !PlayerDataFilter.carriesPlayerData(s));
        }

        // 這兩則是上一輪漏掉的：模板內嵌玩家名／公會名
        check("擋下: 經驗炸彈（夾帶玩家名）",
                PlayerDataFilter.carriesPlayerData(
                        "{#} IceKingDom120 has thrown a Combat Experience Bomb on AS15"));
        check("擋下: 經驗貢獻（夾帶公會名）",
                PlayerDataFilter.carriesPlayerData(
                        "You will now contribute 0% of your XP to YuChaYuan."));
    }

    /** 數值參數化：同一句文案的不同實例要收斂成一條。 */
    private static void numberChecks() {
        System.out.println();
        System.out.println("  -- parametrizeNumbers --");
        check("獎勵數值收斂",
                GlyphSplitter.parametrizeNumbers("- +28 Emeralds")
                        .equals(GlyphSplitter.parametrizeNumbers("- +30 Emeralds")));
        check("結果形如 - +{~} Emeralds",
                "- +{~} Emeralds".equals(GlyphSplitter.parametrizeNumbers("- +28 Emeralds")));
        check("百分比一起收",
                "{~} of your XP".equals(GlyphSplitter.parametrizeNumbers("0% of your XP")));
        check("千分位一起收",
                "+{~} XP".equals(GlyphSplitter.parametrizeNumbers("+1,250 XP")));
        check("沒有數字時不變",
                "Great! You got the cake!".equals(
                        GlyphSplitter.parametrizeNumbers("Great! You got the cake!")));
        check("不會動到圖示佔位符",
                "{#} Rewards:".equals(GlyphSplitter.parametrizeNumbers("{#} Rewards:")));
    }

    private static void check(String name, boolean ok) {
        System.out.printf("  [%s] %s%n", ok ? "PASS" : "FAIL", name);
        if (!ok) {
            failures++;
        }
    }
}
