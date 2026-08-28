package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天訊息的譯文要保住原文的顏色。
 *
 * <h2>畫面上長什麼樣</h2>
 * 進伺服器的歡迎訊息裡，{@code Welcome to Wynncraft!} 是金色粗體。
 * 譯文「歡迎來到 Wynncraft！」先前整行掉成灰色，兩行擺在一起一亮一暗，
 * 一看就知道下面那行是外掛貼的。
 *
 * <h2>先前為什麼會掉色</h2>
 * 上色是拿原文的<b>字面</b>到譯文裡找。單一個詞還有救——{@code withTranslations}
 * 會把詞的譯文也登記一份；但整句不行：{@code Welcome to Wynncraft!} 不是語料的鍵
 * （鍵是整段四行），lookup、lookupTerm、lookupWordCore 三條路全落空，
 * 於是那一段被記成「譯文裡找不到」，顏色就沒貼上去。
 *
 * <p>而這種情況根本不必查表——原文那一行從頭到尾只有一個顏色，
 * 譯文那一行就整行照它。見 {@code LineTranslator#wholeLineStyles}。
 */
public final class ChatColourTest {

    private static int failures = 0;

    private static final int GOLD = 0xFFAA00;     // Welcome to Wynncraft!
    private static final int GREY = 0xAAAAAA;     // 網址與說明
    private static final int RED  = 0xFF5555;     // TIP:
    private static final int WHITE = 0xFFFFFF;    // /char

    /** 行首那個符號。語料裡它是 {@code {#}}，實際送來的是私用區碼位。 */
    private static final String ICON = "\ue001";

    /**
     * 遊戲送來的樣子，取自使用者回報的 majorid-debug.txt 重點段 8。
     *
     * <p>聊天訊息是<b>一個</b> StyledText，換行包在裡面——
     * 不是 tooltip 那種一行一個的清單。{@code ChatListener} 走的是
     * {@code LineTranslator#translate}，不是 {@code translateBlock}。
     */
    private static StyledText welcome() {
        MutableComponent all = Component.empty();
        all.append(lit(ICON, GOLD, true));
        all.append(lit("Welcome to Wynncraft!", GOLD, true));
        all.append(lit("\n", GREY, false));
        all.append(lit(ICON, GREY, false));
        all.append(lit("play.wynncraft.com ", GREY, false));
        all.append(lit("-/-", GREY, false));
        all.append(lit(" wynncraft.com", GREY, false));
        all.append(lit("\n\n", GREY, false));
        all.append(lit(ICON, RED, true));
        all.append(lit("TIP: ", RED, true));
        all.append(lit("Type ", GREY, false));
        all.append(lit("/char", WHITE, false));
        all.append(lit(" to switch character", GREY, false));
        return StyledText.fromComponent(all);
    }

    private static MutableComponent lit(String text, int colour, boolean bold) {
        return Component.literal(text).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(colour)).withBold(bold));
    }

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));
        FlowedDebug.init(java.nio.file.Files.createTempDirectory("wynnchayuan"));

        Component hit = LineTranslator.translate(welcome(), store);
        check("歡迎訊息查得到譯文", hit != null);
        if (hit == null) {
            report();
            return;
        }
        List<Component> built = List.of(hit);
        System.out.println("      輸出：" + hit.getString().replace("\n", " ⏎ "));
        String all = hit.getString();
        check("標題有翻出來", all.contains("歡迎來到"));

        Integer title = colourOf(built, "歡迎來到");
        check("標題查得到顏色（拿到 "
                        + (title == null ? "null" : "#" + String.format("%06X", title))
                        + "）", title != null);
        check("標題不是內文的灰色", title != null && title != GREY);
        check("標題沿用原文的金色", title != null && title == GOLD);

        Boolean bold = boldOf(built, "歡迎來到");
        check("標題跟原文一樣是粗體", Boolean.TRUE.equals(bold));

        // 網址那一行原文就是灰的，不該被標題的金色波及
        Integer url = colourOf(built, "wynncraft.com");
        check("網址那一行仍是灰色（拿到 "
                        + (url == null ? "null" : "#" + String.format("%06X", url))
                        + "）", url != null && url == GREY);

        // 一行裡有好幾個顏色的，不能整行套同一色——那是另一種壞法
        Integer slash = colourOf(built, "/char");
        check("一行多色時 /char 仍保住自己的白色（拿到 "
                        + (slash == null ? "null" : "#" + String.format("%06X", slash))
                        + "）", slash == null || slash == WHITE);

        // 整行同色的行不能把已經登記過的重點段再登一次。
        //
        // 兩條一模一樣的重點段，只有第一條會被用到，第二條在診斷檔裡
        // 成了「★在譯文裡卻沒貼上」（使用者回報的 majorid-debug 裡那一排星號）；
        // 多余的長串還會跟真正該貼的重點段互投位置。
        long titles = flatten(hit).stream()
                .filter(c -> c.getString().contains("歡迎來到"))
                .count();
        check("標題只被貼一次（實際 " + titles + " 段）", titles == 1);

        // 多行系統訊息：每一行前面都有一個自己的置中縮排，
        // 而那些數字是照英文寬度算的（診斷檔「填回去的符號 2」）。
        // 原樣填回去，中文那一块就歪了——回報的聊天排版問題。
        //
        // 釘的是「三個縮排都還在」：逐行重算不能把它們弄不見，
        // 也不能把換行吃掉。具體的像素值跟字型渲染有關，這裡不釘。
        long rows = hit.getString().chars().filter(c -> c == 10).count() + 1;
        check("多行訊息的行數保住不變（實際 " + rows + " 行）", rows == 4);

        report();
    }

    private static Integer colourOf(List<Component> lines, String needle) {
        for (Component line : lines) {
            for (Component part : flatten(line)) {
                if (part.getString().contains(needle)) {
                    TextColor colour = part.getStyle().getColor();
                    return colour == null ? null : colour.getValue();
                }
            }
        }
        return null;
    }

    private static Boolean boldOf(List<Component> lines, String needle) {
        for (Component line : lines) {
            for (Component part : flatten(line)) {
                if (part.getString().contains(needle)) {
                    return part.getStyle().isBold();
                }
            }
        }
        return null;
    }

    private static List<Component> flatten(Component component) {
        List<Component> out = new ArrayList<>();
        if (component.getSiblings().isEmpty()) {
            out.add(component);
        }
        for (Component child : component.getSiblings()) {
            out.addAll(flatten(child));
        }
        return out;
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        System.out.println(failures == 0
                ? "ChatColour: 全部通過" : "ChatColour: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
