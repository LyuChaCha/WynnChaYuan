package com.wynnchayuan.capture;

import com.wynnchayuan.WynnChaYuan;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
    private static final int LIMIT = 8;

    private static int written = 0;

    private static Path dir;

    private static String lastPlain = "";

    /** 上一次錄到的台詞佔了幾段（也就是框裡有幾行）。 */
    private static int lastRows = -1;

    /**
     * 這一輪的 {@link #record} 有沒有真的寫出檔案。
     *
     * <p>{@link #after} 靠它決定要不要把取代後的樣子附到同一個檔案裡——
     * 兩者收到的是<b>同一條</b>訊息（record 掛 HIGHEST、取代掛 LOWEST），
     * 所以只有 record 寫了的那一輪，after 才有對照的對象。
     */
    private static boolean justWrote = false;

    private DialogueProbe() {}

    public static void init(Path configDir) {
        dir = configDir;
    }

    /**
     * 把對話框用到的字型定義原樣抄出來。
     *
     * <h2>為什麼需要這一份</h2>
     * {@code body_0}、{@code body_1}、{@code control} 不是普通字型——它們把
     * <b>「畫在第幾行」烘進了字型本身</b>（bitmap provider 的 {@code ascent}）。
     * 整條對話是一個 action bar 字串，MC 的位移字元只能左右移不能上下移，
     * 所以 Wynncraft 只能用字型編號來表示行號。
     *
     * <p>就地取代把字型換成預設，等於把那個高度丟掉，文字就掉到 action bar
     * 自己的基線去了。要補回來，得知道每一個 {@code body_N} 的 ascent 是多少——
     * 那是伺服器資源包裡的數字，猜不出來，但那個包就在玩家的硬碟上。
     */
    public static void dumpFonts() {
        if (dir == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getResourceManager() == null) {
            return;
        }
        String[] names = {
            "hud/dialogue/text/wynncraft/body_0",
            "hud/dialogue/text/wynncraft/body_1",
            "hud/dialogue/text/wynncraft/body_2",
            "hud/dialogue/text/wynncraft/body_3",
            "hud/dialogue/text/nameplate",
            "hud/dialogue/text/control",
            "hud/gameplay/default/bottom_middle",
        };
        StringBuilder sb = new StringBuilder();
        for (String name : names) {
            sb.append("=== ").append(name).append(" ===")
              .append(System.lineSeparator());
            Identifier id = Identifier.withDefaultNamespace("font/" + name + ".json");
            try {
                var found = mc.getResourceManager().getResource(id);
                if (found.isEmpty()) {
                    sb.append("  （沒有這個檔案）").append(System.lineSeparator());
                } else {
                    try (var in = found.get().open()) {
                        sb.append(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    }
                }
            } catch (Exception e) {
                sb.append("  讀不到：").append(e).append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
        }
        try {
            Files.writeString(dir.resolve("font-dump.txt"), sb.toString(),
                    StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 寫不出來就算了
        }
    }

    /**
     * 這一條 action bar 裡有沒有<b>已經打出字的台詞</b>。
     *
     * <h2>為什麼不能只認「有對話字型」</h2>
     * 第一版是這樣寫的，結果三份錄下來的全是同一個東西：對話框<b>淡入</b>的
     * 第 0、1、2 幀。那時候框已經在畫了（{@code effect/fade} 與
     * {@code style/default/box} 的字碼一幀一幀往上加），但一句台詞都還沒打出來，
     * {@code body_0} 裡只有兩個位移字元。
     *
     * <p>三份資料看起來很豐富，卻剛好漏掉唯一要看的東西。所以改成認
     * {@code hud/dialogue/text/…/body_N} 裡有沒有<b>看得懂的字</b>——
     * 有字才是我們要研究的那一幀。
     *
     * <p>{@code text/control}（「 to continue」）不算：那行提示從第一幀就在，
     * 認它等於沒有篩選。
     */
    private static boolean hasBodyText(Component message) {
        boolean[] found = {false};
        message.visit((style, text) -> {
            if (fontOf(style).contains("/body_") && readable(text)) {
                found[0] = true;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    /** 有沒有非排版、非圖示的字。 */
    private static boolean readable(String text) {
        return text.codePoints().anyMatch(cp ->
                cp >= 0x20 && cp < 0x7F && !Character.isWhitespace(cp));
    }

    /**
     * 換不掉的那些訊息，另外留位子。
     *
     * <h2>為什麼需要跟 {@link #record} 分開</h2>
     * {@code record} 的八個名額是先到先得，而玩家一進遊戲就會先跟 NPC 講一段話——
     * 那段話把八個名額全吃光，真正翻不出來的那則訊息（任務開始）連一次都錄不到。
     * 連續四次回報都是這樣：附了八份 probe，八份全是同一段對話。
     *
     * <p>這裡只收<b>改寫失敗</b>的訊息，而且同一則訊息一直覆寫同一個檔案——
     * 逐字打字的每一幀都會進來，最後留在檔案裡的自然是打完的那一幀，
     * 也就是最有診斷價值的那一幀。
     */
    public static void miss(Component message) {
        if (dir == null || message == null
                || !WynnChaYuan.config().debugDumps() || !hasBodyText(message)) {
            return;
        }
        String body = bodyOf(message);
        if (body.isBlank()) {
            return;
        }
        // 逐字打字的每一幀都是<b>前一幀再加幾個字</b>，所以「其中一個是另一個的
        // 開頭」就代表還是同一則訊息。
        //
        // 先前是比前八個字，於是「I don」與「I don't s」被當成兩則不同的訊息——
        // 四個名額又被同一句話的打字過程吃光，真正翻不出來的那則照樣錄不到。
        boolean same = !missKey.isEmpty()
                && (body.startsWith(missKey) || missKey.startsWith(body));
        if (!same) {
            if (misses >= MISS_LIMIT) {
                return;
            }
            misses++;
        }
        missKey = body;

        StringBuilder sb = new StringBuilder();
        sb.append("=== 改寫失敗（getString） ===").append(System.lineSeparator())
          .append(message.getString()).append(System.lineSeparator())
          .append(System.lineSeparator())
          .append("=== 逐片段 ===").append(System.lineSeparator());
        int[] index = {0};
        message.visit((style, text) -> {
            sb.append(String.format("  [%02d] font=%-38s color=%-9s text=%s%n",
                    index[0]++, fontOf(style),
                    style.getColor() == null ? "-" : style.getColor().serialize(),
                    describe(text)));
            return Optional.empty();
        }, Style.EMPTY);

        try {
            Files.writeString(dir.resolve("dialogue-miss-" + misses + ".txt"),
                    sb.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 寫不出來就算了，不要影響遊戲
        }
    }

    /** 改寫失敗的訊息最多留幾則。 */
    private static final int MISS_LIMIT = 4;

    private static int misses = 0;
    private static String missKey = "";

    public static void record(Component message) {
        justWrote = false;
        if (dir == null || written >= LIMIT || message == null
                || !WynnChaYuan.config().debugDumps() || !hasBodyText(message)) {
            return;
        }
        // 同一句話的每一個字都會送一次。比的是<b>台詞本身</b>的長度，
        // 不是整條訊息——整條訊息裡九成是固定不變的框線與位移字元，
        // 拿它去比，一句話從頭打到尾的長度變化根本不到門檻。
        String plain = message.getString();
        String body = bodyOf(message);
        int rows = rowsOf(message);
        // 行數變了也要錄。門檻只看長度的話，<b>換行的那一刻</b>剛好錄不到——
        // 而「超出方格有沒有換行」正是要看的東西。
        if (body.length() < lastPlain.length() + 12 && rows == lastRows) {
            return;
        }
        lastPlain = body;
        lastRows = rows;
        written++;
        if (written == 1) {
            dumpFonts();
        }

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
            justWrote = true;
        } catch (Exception e) {
            // 勘查寫不出來就算了，不要影響遊戲
        }
    }

    /**
     * 就地取代<b>之後</b>的樣子，附在同一個檔案後面。
     *
     * <h2>為什麼要錄這一份</h2>
     * 原始那一份告訴我們 Wynncraft 怎麼排版，卻不會告訴我們<b>我們自己送出去
     * 的是什麼</b>。譯文沒出現的時候，光看原始那一份完全分不出是查表沒中、
     * 位移算錯、還是根本沒被呼叫到——三種原因在畫面上長得一模一樣。
     *
     * <p>兩份寫在同一個檔案裡，是為了能直接上下對照同一句話。
     */
    public static void after(Component message) {
        if (!justWrote || dir == null || message == null) {
            return;
        }
        justWrote = false;

        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator())
          .append("=== 取代後（getString） ===").append(System.lineSeparator())
          .append(message.getString()).append(System.lineSeparator())
          .append(System.lineSeparator())
          .append("=== 取代後逐片段 ===").append(System.lineSeparator());
        int[] index = {0};
        message.visit((style, text) -> {
            sb.append(String.format("  [%02d] font=%-38s color=%-9s text=%s%n",
                    index[0]++, fontOf(style),
                    style.getColor() == null ? "-" : style.getColor().serialize(),
                    describe(text)));
            return Optional.empty();
        }, Style.EMPTY);

        try {
            Files.writeString(dir.resolve("dialogue-probe-" + written + ".txt"),
                    sb.toString(), StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {
            // 同上，寫不出來就算了
        }
    }

    /** 只把台詞那幾段接起來，用來判斷「這句話打到哪了」。 */
    private static String bodyOf(Component message) {
        StringBuilder out = new StringBuilder();
        message.visit((style, text) -> {
            if (fontOf(style).contains("/body_")) {
                out.append(text);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out.toString();
    }

    /** 台詞被拆成幾段（一段就是框裡的一行）。 */
    private static int rowsOf(Component message) {
        int[] rows = {0};
        message.visit((style, text) -> {
            if (fontOf(style).contains("/body_") && readable(text)) {
                rows[0]++;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return rows[0];
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
