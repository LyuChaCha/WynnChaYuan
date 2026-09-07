package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 分隔線不能被併進上下的文字段。
 *
 * <h2>實機回報</h2>
 * Shiny 物品的「首領祭壇通關數」那一列跑到面板中間，而它上面那條橫線不見了。
 * 正常應該是：
 *
 * <pre>
 *   ────────────────────
 *   ✔ 戰鬥等級        89
 *   ────────────────────
 *   {圖示} 首領祭壇通關數  0
 * </pre>
 *
 * <h2>怎麼壞的</h2>
 * 分隔線那一行的模板是「{@code {#}{#}}」——<b>不是空的</b>，所以躲過了
 * {@code translateBlock} 開頭那道「空行不能併進來」的檢查。接著攤平查表
 * 把換行壓成空格，於是
 *
 * <pre>
 *   {#}{#}                                ← 分隔線
 *   {#} Boss Altars Won{#}{~} {#}{#}{#}   ← 統計列
 * </pre>
 *
 * 剛好對上語料裡的「{@code {#}{#} Boss Altars Won…}」——而那一條本身就是從
 * <b>攤平過的 capture</b> 收進來的，等於把 bug 醃進了語料。命中之後兩行被併成
 * 一行：分隔線消失，它的縮排偏移接到標籤前面，整列被推到中間。
 *
 * <h2>釘住什麼</h2>
 * 「攤平查表」這條路是給被 tooltip 寬度折斷的<b>一整段話</b>用的，段落裡有純排版
 * 的行時那個前提就不成立。整段收在語料裡的（{@code raid.json} 有幾條第一行就是
 * {@code {#}{#}}）走的是<b>精確</b>查表，不受影響——兩邊都要測，不然修法很容易
 * 從「別亂併」變成「整段那條路整個關掉」。
 */
public final class DividerBlockTest {

    private static int failures = 0;

    private static final String SPACE = "space";
    private static final String DIVIDER_FONT = "tooltip/divider";
    private static final String ICON = "common";
    private static final String TEXT = "language/wynncraft";

    private static Style font(String id) {
        return Style.EMPTY.withFont(new FontDescription.Resource(
                Identifier.withDefaultNamespace(id)));
    }

    private static MutableComponent part(String text, String fontId) {
        return Component.literal(text).withStyle(font(fontId));
    }

    /** 分隔線：一個偏移 + 一個圖示，沒有字母。實機模板是「{#}{#}」。 */
    private static MutableComponent divider() {
        MutableComponent c = Component.empty();
        c.append(part("", SPACE));
        c.append(part("", DIVIDER_FONT));
        return c;
    }

    /** Shiny 的統計列：圖示 + 標籤 + 欄距 + 數值 + 三個圖示。 */
    private static MutableComponent statRow() {
        MutableComponent c = Component.empty();
        c.append(part("", ICON));
        c.append(part(" Boss Altars Won", TEXT));
        c.append(part("", SPACE));
        c.append(part("0", TEXT));
        c.append(part(" ", TEXT));
        // 三個圖示要用不同的字型交錯：樣式一樣的相鄰片段會被併成一段，
        // 模板就只剩一個 {#}，測到的東西跟實機不一樣。
        c.append(part("", ICON));
        c.append(part("", DIVIDER_FONT));
        c.append(part("", ICON));
        return c;
    }

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        // ★ 前提一：語料裡確實躺著那條「吃掉分隔線」的鍵。哪天有人清掉了，
        //   這條測試就失去意義，所以先確認它還在。
        check("★ 攤平過的舊鍵還在語料裡（不然下面測的是空氣）",
              store.lookup("{#}{#} Boss Altars Won{#}{~} {#}{#}{#}") != null);

        // ★ 前提二：造出來的兩行，模板要跟實機一樣。不一樣的話下面測的是別的東西。
        String dividerTemplate = com.wynnchayuan.capture.LineParts
                .of(StyledText.fromComponent(divider())).template();
        String rowTemplate = com.wynnchayuan.capture.LineParts
                .of(StyledText.fromComponent(statRow())).template();
        check("★ 分隔線的模板是 {#}{#}（實際 " + dividerTemplate + "）",
              "{#}{#}".equals(dividerTemplate));
        check("★ 統計列的模板跟實機一樣（實際 " + rowTemplate + "）",
              "{#} Boss Altars Won{#}{~} {#}{#}{#}".equals(rowTemplate));

        List<Component> in = new ArrayList<>();
        in.add(divider());
        in.add(statRow());
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(in, store);

        check("★ 分隔線那一行還在（實際 " + out.size() + " 行）", out.size() == 2);
        if (out.size() == 2) {
            check("★ 分隔線沒被吃掉（實際「" + out.get(0).getString() + "」）",
                  !out.get(0).getString().contains("首領祭壇"));
            check("統計列翻出來了（實際「" + out.get(1).getString() + "」）",
                  out.get(1).getString().contains("首領祭壇通關數"));
        }

        // 反方向：整段收在語料裡、而且第一行本來就是分隔線的，照樣要翻得出來
        String key = "{#}{#}\nThe Purifying Light will continually move around the area.\n"
                + "Guide the Holder of the Decaying Crystal towards it\n"
                + "in order to progress through each of the {~} floors.";
        check("★ 精確查表那條路沒被擋掉（raid.json 第一行就是分隔線）",
              store.lookup(key) != null);
        List<Component> raid = new ArrayList<>();
        raid.add(divider());
        raid.add(part("The Purifying Light will continually move around the area.", TEXT));
        raid.add(part("Guide the Holder of the Decaying Crystal towards it", TEXT));
        raid.add(part("in order to progress through each of the 3 floors.", TEXT));
        StringBuilder zh = new StringBuilder();
        for (Component c : com.wynnchayuan.render.TooltipPanel.translateLines(raid, store)) {
            zh.append(c.getString()).append('\n');
        }
        check("整段命中的那種照舊翻得出來（實際「"
                        + zh.toString().replace('\n', '/').stripTrailing() + "」）",
              zh.toString().contains("淨化之光"));

        System.out.println(failures == 0
                ? "分隔線：全部通過" : "分隔線：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
