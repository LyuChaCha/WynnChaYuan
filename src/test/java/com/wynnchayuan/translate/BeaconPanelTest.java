package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 信標面板的接續行要跟著它那一欄一起移動。
 *
 * <h2>實機回報</h2>
 * 「Lootrun 有時候不對齊」。診斷檔的數字說得很清楚：
 *
 * <pre>
 *   聊天對齊 13  󐀤Greatly Empower󐁁+6 Challenges to   欄距補正=[23, 39]
 *   聊天對齊 15  󐁍󐁧no Time Bonus for                 補=0
 *   聊天對齊 16  󐁍󐁭completing them.                  補=0
 * </pre>
 *
 * 多欄的行照<b>每欄各自置中</b>往右移了 16px（中文短掉的一半），而同一個
 * 信標折下來的那兩行是<b>單欄</b>，走「靠左、原地不動」那條路——兩者就錯開。
 *
 * <h2>為什麼一行一行看修不好</h2>
 * 信標面板是<b>一行一則訊息</b>送過來的。{@code columnPanel} 只看得到這一則
 * 裡的那一行，永遠答「不是面板」。{@code chatCentred} 也接不住：它問的是
 * 「這一行在<b>整塊</b>裡置不置中」，而欄位的接續行是置中在<b>自己那一欄</b>
 * 上——所以 {@code BlockLayout} 會把它判成靠左（實機 layout-debug 的
 * 第 30 塊裡，[7] 到 [10] 全是靠左）。
 *
 * <p>所以跟置中一樣，由 {@code ChatBlock} 整塊算一次再逐行傳下去。
 */
public final class BeaconPanelTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-beacon");
        Files.writeString(dir.resolve("misc.json"),
                "{\"no Time Bonus for\": \"完成它們不會\"}", StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        // 實機那一行：行首是 180px 的排版偏移，接著「no Time Bonus for」。
        StyledText line = indented(180, "no Time Bonus for");

        // 補多少是<b>照字寬</b>算的，而測試環境沒有字型（widthOf 回 0），
        // 所以這裡量不到位移量。能釘住的是：兩條路都不會把行弄壞、
        // 縮排不會憑空跑掉。真正的位移只有實機看得出來。
        for (boolean inPanel : new boolean[] {false, true}) {
            int lead = leadOf(LineTranslator.translateChat(line, store, null, inPanel));
            report("面板旗標 " + inPanel + " 時翻得出來、縮排不亂跑（實際 "
                            + lead + "px）", lead == 180);
        }

        panelDetection();

        System.out.println(failures == 0
                ? "信標面板：全部通過" : "信標面板：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /**
     * 整塊看得出是不是多欄面板。
     *
     * <p>反面同樣重要：一般的聊天訊息不能被誤判成面板，否則所有有縮排的
     * 訊息都會被重新置中。
     */
    private static void panelDetection() {
        // 兩欄併排的那一行（中間夾著排版偏移），加上單欄的接續行
        MutableComponent two = Component.empty();
        two.append(offset(36)).append(Component.literal("Greatly Empower"))
           .append(offset(65)).append(Component.literal("+6 Challenges to"));
        List<StyledText> beacon = List.of(
                StyledText.fromComponent(two),
                indented(180, "no Time Bonus for"));
        report("★ 有多欄的行 -> 整塊算面板", LineTranslator.chatPanel(beacon));

        // 一般聊天：每一行都只有一欄，即使有行首縮排
        List<StyledText> normal = List.of(
                indented(89, "Welcome to Wynncraft!"),
                indented(59, "play.wynncraft.com"));
        report("一般聊天不算面板（只有行首縮排不算欄）",
                !LineTranslator.chatPanel(normal));
    }

    /** 行首一段排版偏移，接著一段文字。 */
    private static StyledText indented(int px, String text) {
        MutableComponent out = Component.empty();
        out.append(offset(px)).append(Component.literal(text));
        return StyledText.fromComponent(out);
    }

    private static Component offset(int px) {
        return Component.literal(SpaceOffset.encode(px))
                .setStyle(SpaceOffset.styleFor(Style.EMPTY));
    }

    /** 這一行行首的排版偏移有多寬。 */
    private static int leadOf(Component line) {
        if (line == null) {
            return -1;
        }
        int[] px = {0};
        boolean[] done = {false};
        line.visit((style, text) -> {
            if (done[0] || text.isEmpty()) {
                return java.util.Optional.empty();
            }
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                px[0] += SpaceOffset.decode(text);
            } else {
                done[0] = true;               // 碰到實字就停
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return px[0];
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
