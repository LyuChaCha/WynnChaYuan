package com.wynnchayuan.capture;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

/**
 * 對話框的名牌：要從 action bar 原始訊息讀，而且只給內文對得上的那一句。
 *
 * <h2>釘住的是實機資料（issue #1138）</h2>
 * {@code dialogue-probe-*.txt} 錄到的名牌是一片獨立的字型：
 * <pre>
 *   font=…/hud/dialogue/text/nameplate  text=&lt;U+CFFB9&gt;
 *   font=…/hud/dialogue/text/nameplate  text=Espren Citizen
 *   font=…/hud/dialogue/text/nameplate  text=&lt;U+D0009&gt;
 * </pre>
 * 而 {@code NpcDialogueEvent#getDialogueText()} 只有內文，沒有這一片，所以先前
 * 收集到的對話一句說話者都沒有。換句的那一刻原始訊息已經是下一個人的名牌，
 * Wynntils 卻還用上一句的文字補發事件——母子對話因此整段錯開一句。
 */
public final class DialogueSpeakerTest {

    private static int failures = 0;

    private static Style font(String path) {
        return Style.EMPTY.withFont(new FontDescription.Resource(
                Identifier.fromNamespaceAndPath("minecraft", path)));
    }

    /** 照 probe 的樣子組一個對話框：名牌兩側帶排版偏移，內文分在 body_N。 */
    private static Component box(String speaker, String... rows) {
        MutableComponent out = Component.empty();
        Style plate = font("hud/dialogue/text/nameplate");
        for (int i = 0; i < rows.length; i++) {
            out.append(Component.literal(rows[i])
                    .withStyle(font("hud/dialogue/text/wynncraft/body_" + i)));
        }
        out.append(Component.literal(new String(Character.toChars(0xCFFB9))).withStyle(plate));
        out.append(Component.literal(speaker).withStyle(plate));
        out.append(Component.literal(new String(Character.toChars(0xD0009))).withStyle(plate));
        return out;
    }

    public static void main(String[] args) {
        String child = "Mama, why does papa yell about Mister Zhiraok all the time?";
        String mother = "Oh, sugar...well, first off, remember to address him properly- it's polite.";

        // 1. 名牌在原始訊息裡讀得到，排版偏移剝乾淨
        DialogueSpeaker.noteRaw(box("Espren Child", "Mama, why does papa yell about Mister",
                                    "Zhiraok all the time?"));
        check("讀到名牌（實際 " + DialogueSpeaker.currentFor(child) + "）",
              "Espren Child".equals(DialogueSpeaker.currentFor(child)));

        // 2. 打字打到一半也對得上（內文是畫面上已經打出來的那一段）
        check("打到一半也算同一句",
              "Espren Child".equals(DialogueSpeaker.currentFor("Mama, why does pa")));

        // 3. 換句：原始訊息已經是母親那一句，上一句的補發事件不能拿到她的名牌
        DialogueSpeaker.noteRaw(box("Espren Mother", "Oh, sugar...well, first off,",
                                    "remember to address him properly- it's polite."));
        check("上一句的補發事件拿不到下一個人的名牌",
              DialogueSpeaker.currentFor(child) == null);
        check("這一句拿到自己的名牌",
              "Espren Mother".equals(DialogueSpeaker.currentFor(mother)));

        // 4. 換行點在哪裡不影響比對（原始訊息分行，事件文字是一整句）
        check("跨行的內文照樣對得上",
              "Espren Mother".equals(DialogueSpeaker.currentFor("first off, remember to")));

        // 5. 沒有名牌的對話框（旁白、日記）
        MutableComponent narration = Component.empty();
        narration.append(Component.literal("You open the journal to a random page.")
                .withStyle(font("hud/dialogue/text/wynncraft/body_0")));
        DialogueSpeaker.noteRaw(narration);
        check("沒有名牌就沒有說話者",
              DialogueSpeaker.currentFor("You open the journal to a random page.") == null);

        // 6. 不是對話的 action bar
        DialogueSpeaker.noteRaw(null);
        check("沒有訊息就沒有說話者", DialogueSpeaker.currentFor(mother) == null);

        // 7. 頭頂名牌只留名字：第一列，剝掉底下那一列等級牌圖示
        String plate = "Espren Guard\n" + new String(Character.toChars(0xE060))
                + new String(Character.toChars(0xCFFFF)) + new String(Character.toChars(0xD0002));
        check("頭頂名牌只留名字（實際 " + DialogueSpeaker.cleanName(plate) + "）",
              "Espren Guard".equals(DialogueSpeaker.cleanName(plate)));
        check("名字前後的排版偏移剝掉",
              "Espren Citizen".equals(DialogueSpeaker.cleanName(
                      "Espren Citizen " + new String(Character.toChars(0xE060)))));
        check("全是圖示就是沒有名字",
              DialogueSpeaker.cleanName(new String(Character.toChars(0xE060))) == null);

        // 8. 名牌位置只放了間隔字 À（實機 Tunnel Trouble）
        DialogueSpeaker.noteRaw(box("À", "Looks like you freed the cows successfully!"));
        check("只有 À 的名牌不算說話者",
              DialogueSpeaker.currentFor("Looks like you freed the cows") == null);
        check("頭頂名牌只有 À 也不算名字", DialogueSpeaker.cleanName("À") == null);
        check("名字裡的 À 照留", "Àlvaro".equals(DialogueSpeaker.cleanName("Àlvaro")));
        check("換成別的佔位字（ÀÀ、Ï）也不算", DialogueSpeaker.cleanName("ÀÀ") == null
                && DialogueSpeaker.cleanName("Ï") == null);
        check("只有符號或數字不算", DialogueSpeaker.cleanName("#*") == null
                && DialogueSpeaker.cleanName("12") == null);
        check("藏名字的「???」照留", "???".equals(DialogueSpeaker.cleanName("???")));
        check("短的真名照留（Bob）", "Bob".equals(DialogueSpeaker.cleanName("Bob")));

        report();
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        if (failures == 0) {
            System.out.println("對話名牌：全部通過");
        } else {
            System.out.println("對話名牌：" + failures + " 項失敗");
            System.exit(1);
        }
    }
}
