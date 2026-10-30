package com.wynnchayuan;

import com.wynnchayuan.capture.DialogueBuffer;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynntils.core.text.StyledText;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;

public final class GlyphSplitterTest {

    private static int failures = 0;

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        check("null 字型不算自訂",
                !GlyphSplitter.isCustomFont(null));

        check("FontDescription.DEFAULT 不算自訂（這就是當初的 bug）",
                !GlyphSplitter.isCustomFont(FontDescription.DEFAULT));

        check("minecraft:default 不算自訂",
                !GlyphSplitter.isCustomFont(
                        new FontDescription.Resource(Identifier.withDefaultNamespace("default"))));

        check("語言字型底下的句點不是圖示",
                !GlyphSplitter.isGlyphPart(part(".", "language/wynncraft")));
        check("語言字型底下的整句話也不是圖示",
                !GlyphSplitter.isGlyphPart(
                        part("all elements", "language/wynncraft")));
        check("符號字型底下的標點仍算圖示",
                GlyphSplitter.isGlyphPart(part(".", "common")));

        check("common 算自訂（Wynncraft 的符號字型）",
                GlyphSplitter.isCustomFont(
                        new FontDescription.Resource(Identifier.withDefaultNamespace("common"))));

        StyledText plain = StyledText.fromString("Hello traveller!");
        check("純文字不是 glyphOnly", !GlyphSplitter.isGlyphOnly(plain));
        check("純文字模板保持原樣",
                "Hello traveller!".equals(GlyphSplitter.toTemplate(plain)));

        StyledText colored = StyledText.fromString("§7Speak to the §fGuard§7.");
        check("帶顏色碼的文字不是 glyphOnly", !GlyphSplitter.isGlyphOnly(colored));
        check("帶顏色碼的文字仍抽得到字",
                GlyphSplitter.toTemplate(colored).contains("Speak to the"));

        check("PUA 碼位被認出", GlyphSplitter.isPrivateUse(0xE007));
        check("一般字母不是 PUA", !GlyphSplitter.isPrivateUse('A'));
        check("純 PUA 字串沒有字母", !GlyphSplitter.hasLetter(""));
        check("中文算字母", GlyphSplitter.hasLetter("魔力消耗"));

        check("U+D005B 算圖示（第 13 平面）", GlyphSplitter.isGlyphCodePoint(0xD005B));
        check("U+D0000 算圖示", GlyphSplitter.isGlyphCodePoint(0xD0000));
        check("U+D0074 算圖示", GlyphSplitter.isGlyphCodePoint(0xD0074));
        check("U+D005B 不是 PUA（兩者要分得開）", !GlyphSplitter.isPrivateUse(0xD005B));
        check("真正的 PUA 也算圖示", GlyphSplitter.isGlyphCodePoint(0xE007));
        check("字母不算圖示", !GlyphSplitter.isGlyphCodePoint('A'));
        check("中文不算圖示", !GlyphSplitter.isGlyphCodePoint(0x4E2D));
        check("日文不算圖示", !GlyphSplitter.isGlyphCodePoint(0x3042));
        check("彎引號不算圖示", !GlyphSplitter.isGlyphCodePoint(0x2019));

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

    private static void dialogueBufferChecks() {
        System.out.println("\n  -- DialogueBuffer --");
        DialogueBuffer buf = new DialogueBuffer();
        String full = "H-hey, human- URGH! Help me out here! Some damn guards beat me bloody...";

        int emitted = 0;
        for (int i = 1; i <= full.length(); i++) {
            String frame = full.substring(0, i);
            if (buf.offer(frame, frame) != null) {
                emitted++;
            }
        }
        check("打字過程中一句都不送出", emitted == 0);
        check("flush 拿到完整的那句", full.equals(buf.flush()));
        check("flush 後緩衝區清空", buf.flush() == null);

        DialogueBuffer buf2 = new DialogueBuffer();
        buf2.offer("H-hey", "H-hey");
        buf2.offer("H-hey, human!", "H-hey, human!");
        String done = buf2.offer("I need somethin' to patch myself up...",
                                 "I need somethin' to patch myself up...");
        check("換句時送出前一句", "H-hey, human!".equals(done));
        check("新的一句留在緩衝區",
                "I need somethin' to patch myself up...".equals(buf2.flush()));

        DialogueBuffer buf3 = new DialogueBuffer();
        buf3.offer("Same text", "Same text");
        check("內容沒變不送出", buf3.offer("Same text", "Same text") == null);

        DialogueBuffer buf4 = new DialogueBuffer();
        String line = "Listen, human, you gotta get me out of here!";
        int leaked = 0;
        for (int i = 1; i <= line.length(); i++) {
            String cursor = new String(Character.toChars(0xD0000 + (i % 0x74)));
            String withCursor = line.substring(0, i) + cursor;
            if (buf4.offer(withCursor,
                    GlyphSplitter.stripGlyphChars(withCursor)) != null) {
                leaked++;
            }
        }
        check("帶游標的逐字輸入不產生碎片", leaked == 0);
        check("帶游標的逐字輸入最後拿到完整句", line.equals(buf4.flush()));

        DialogueBuffer buf5 = new DialogueBuffer();
        int fragments = 0;
        for (int i = 1; i <= line.length(); i++) {
            String cursor = new String(Character.toChars(0xD0000 + (i % 0x74)));
            String typed = line.substring(0, i) + cursor;
            if (buf5.offer(typed, GlyphSplitter.toTemplate(
                    com.wynntils.core.text.StyledText.fromString(typed))) != null) {
                fragments++;
            }
        }
        check("走 toTemplate 也不產生碎片（實際走法）", fragments == 0);
        check("走 toTemplate 最後拿到完整句", line.equals(buf5.flush()));
    }

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

        check("擋下: 經驗炸彈（夾帶玩家名）",
                PlayerDataFilter.carriesPlayerData(
                        "{#} IceKingDom120 has thrown a Combat Experience Bomb on AS15"));
        check("擋下: 經驗貢獻（夾帶公會名）",
                PlayerDataFilter.carriesPlayerData(
                        "You will now contribute 0% of your XP to YuChaYuan."));
    }

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

    private static com.wynntils.core.text.StyledTextPart part(String text,
                                                              String font) {
        net.minecraft.network.chat.Style style =
                net.minecraft.network.chat.Style.EMPTY.withFont(
                        new FontDescription.Resource(
                                Identifier.withDefaultNamespace(font)));
        return com.wynntils.core.text.StyledText.fromComponent(
                net.minecraft.network.chat.Component.literal(text)
                        .withStyle(style)).iterator().next();
    }

    private static void check(String name, boolean ok) {
        System.out.printf("  [%s] %s%n", ok ? "PASS" : "FAIL", name);
        if (!ok) {
            failures++;
        }
    }
}
