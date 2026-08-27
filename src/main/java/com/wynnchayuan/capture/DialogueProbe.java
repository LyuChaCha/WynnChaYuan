package com.wynnchayuan.capture;

import com.wynnchayuan.WynnChaYuan;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 把 NPC 對話那一整條 action bar 原封不動記下來。
 *
 * <h2>為什麼需要這一份</h2>
 * Wynncraft 的對話框不是普通文字——它是用 {@code hud/dialogue/} 底下一整組
 * 自訂字型畫出來的：{@code text/body_1}、{@code text/body_2}……一行一個字型，
 * 外加 {@code text/choice}、{@code text/control}（SHIFT 提示）與
 * {@code effect/fade}。框線與頭像應該也在那組字型裡。
 *
 * <p>要「保留原本的框、只換掉裡面的字」，得先確定三件事，而這三件事都<b>看不出來</b>，
 * 只能從實際資料讀：
 *
 * <ul>
 *   <li>那組字型有沒有涵蓋中文。只有 ASCII 的話，中文會掉回預設字型，
 *       連帶失去 {@code body_N} 自帶的行位移——四行譯文會疊在同一行上。</li>
 *   <li>頭像與框是不是同一條訊息裡的字元。如果是，改寫文字時必須整段留著。</li>
 *   <li>每一行的寬度是誰決定的。若位移是照英文寬度算好的，換成中文就會歪。</li>
 * </ul>
 *
 * <p>{@link com.wynntils.models.dialogue.event.NpcDialogueEvent} 拿到的是
 * Wynntils 清理過的純文字，那些字型資訊在那一步就沒了。所以這裡直接接
 * action bar 的原始訊息。
 *
 * <p>只在收集模式下寫，而且只寫前幾次——這是一次性的勘查，不是常駐功能。
 */
public final class DialogueProbe {

    /** 錄幾份就停。同一句話會被送幾十次（NPC 逐字打字），不擋會寫爆。 */
    private static final int LIMIT = 3;

    private static int written = 0;

    private static Path dir;

    private static String lastPlain = "";

    private DialogueProbe() {}

    public static void init(Path configDir) {
        dir = configDir;
    }

    /** 這一條 action bar 有沒有對話字型；沒有的話不是對話，不記。 */
    private static boolean isDialogue(Component message) {
        boolean[] found = {false};
        message.visit((style, text) -> {
            if (fontOf(style).contains("hud/dialogue")) {
                found[0] = true;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    public static void record(Component message) {
        if (dir == null || written >= LIMIT || message == null
                || !WynnChaYuan.config().collect() || !isDialogue(message)) {
            return;
        }
        // 同一句話的每一個字都會送一次。只錄「內容變得夠多」的那幾份，
        // 否則三份會全是同一句話的前三個字。
        String plain = message.getString();
        if (plain.length() < lastPlain.length() + 20 && plain.startsWith(lastPlain)) {
            return;
        }
        lastPlain = plain;
        written++;

        List<String> fonts = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        sb.append("=== 原始（getString） ===").append(System.lineSeparator());
        sb.append(plain).append(System.lineSeparator()).append(System.lineSeparator());

        sb.append("=== 逐片段 ===").append(System.lineSeparator());
        int[] index = {0};
        message.visit((style, text) -> {
            String font = fontOf(style);
            if (!fonts.contains(font)) {
                fonts.add(font);
            }
            sb.append(String.format("  [%02d] font=%-38s color=%-9s text=%s%n",
                    index[0]++, font,
                    style.getColor() == null ? "-" : style.getColor().serialize(),
                    describe(text)));
            return Optional.empty();
        }, Style.EMPTY);

        sb.append(System.lineSeparator())
          .append("=== 用到的字型 ===").append(System.lineSeparator());
        for (String font : fonts) {
            sb.append("  ").append(font).append(System.lineSeparator());
        }

        try {
            Files.writeString(dir.resolve("dialogue-probe-" + written + ".txt"),
                    sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 勘查寫不出來就算了，不要影響遊戲
        }
    }

    private static String fontOf(Style style) {
        return style.getFont() == null ? "(預設)" : style.getFont().toString();
    }

    /**
     * 一段文字連同它每一個字的碼位。
     *
     * <p>對話框的內容大半是看不見的字元：{@code minecraft:space} 的寬度位移、
     * PUA 區的圖示、還有拿來當位置標記的控制字元。只印字串的話那些全都是空白，
     * 完全看不出結構——而結構正是這份勘查要看的東西。
     */
    private static String describe(String text) {
        StringBuilder out = new StringBuilder();
        text.codePoints().forEach(cp -> {
            if (cp >= 0x20 && cp < 0x7F) {
                out.append((char) cp);
            } else if (cp == ' ') {
                out.append('␠');
            } else {
                out.append(String.format("<U+%04X>", cp));
            }
        });
        return out.toString();
    }
}
