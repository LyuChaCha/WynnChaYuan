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
 * 狀態名稱（標記、失血、血池…）在譯文裡要拿得到原文的顏色。
 *
 * <h2>怎麼壞的</h2>
 * 意象敘述上的「標記」是白的，而原文的 {@code Marks} 是青色。顏色是拿
 * <b>原文那一段的字面</b>到譯文裡找同樣的字：找得到英文就貼英文的位置，
 * 找不到就再找<b>詞表給那個字的譯法</b>。兩個都找不到，那一段就掉回底色。
 *
 * <pre>
 *   原文   Increase your maximum Marks {#} by +1.
 *          青色 ────────────────┘
 *   詞表   Marks → 印記
 *   譯文   標記上限增加 +1。          ← 英文沒有、「印記」也沒有 → 沒顏色
 * </pre>
 *
 * 詞表寫「印記」，而整份語料的遊戲機制一律寫「標記」——
 * {@code ability-labels.json} 的「標記持續時間」、{@code assassin.json} 的
 * {@code Marked → 標記}、{@code major-id.json} 的「標記上限」。
 * 詞表那一條是唯一的例外，於是它跟譯文對不起來，顏色就貼不上去。
 *
 * <h2>這條測試在盯什麼</h2>
 * 不是盯詞表有沒有那個詞（{@code MajorIdTermsTest} 在盯那個），而是盯
 * <b>詞表的譯法跟語料實際用的字一不一致</b>。不一致的時候語料不會錯、
 * validate 也是綠的，只有畫面上少一個顏色——沒有任何東西會叫。
 */
public final class StatusColourTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        // 詞表怎麼翻，語料就得怎麼寫，顏色才貼得回去
        agrees(store, "Marks", "標記");
        agrees(store, "Mark", "標記");
        agrees(store, "Bleeding", "失血");
        agrees(store, "Blood Pool", "血池");

        coloured(store);

        System.out.println(failures == 0
                ? "狀態名顏色：全部通過" : "狀態名顏色：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /** 詞表的譯法就是語料實際在用的那個詞。 */
    private static void agrees(TranslationStore store, String english, String want) {
        String zh = store.lookup(english);
        report("詞表的「" + english + "」是「" + want + "」（實際：" + zh + "）",
                want.equals(zh));
    }

    /**
     * 真的畫一次，看那個狀態名有沒有拿到原文的青色。
     *
     * <p>用實機那一行：意象「名帖之相」的敘述。原文把 {@code Marks} 單獨切成
     * 一段並上青色，那一段就是顏色的來源。
     */
    private static void coloured(TranslationStore store) {
        int cyan = 0x55FFFF;
        int body = 0xAAAAAA;

        // 語料收的是<b>整段</b>（兩句接成一行），所以兩句都要擺上來，
        // 不然模板對不上、整段查不到。
        MutableComponent line = Component.empty();
        line.append(literal("Increase your maximum ", body));
        line.append(literal("Marks ", cyan));
        line.append(glyph());
        line.append(literal(" by ", body));
        line.append(literal("+1", 0xFFFFFF));
        line.append(literal(". Decrease your damage per ", body));
        line.append(literal("Mark ", cyan));
        line.append(glyph());
        line.append(literal(" by ", body));
        line.append(literal("-0.5%", 0xFF5555));
        line.append(literal(".", body));

        Component built = LineTranslator.translate(
                StyledText.fromComponent(line), store);
        if (built == null) {
            System.out.println("  [跳過] 語料裡沒有這一行，換一句再測");
            return;
        }
        String all = built.getString();
        report("畫出來有「標記」（實際：" + all + "）", all.contains("標記"));

        Integer got = colourOf(built, "標記");
        report("「標記」拿到原文的青色（實際："
                        + (got == null ? "沒有顏色" : String.format("#%06X", got)) + "）",
                got != null && got == cyan);
    }

    /** 原文那個紅色小圖示。私用區字元＋自訂字型，會被收成 {@code {#}}。 */
    private static Component glyph() {
        return Component.literal("").withStyle(Style.EMPTY
                .withColor(TextColor.fromRgb(0xFF5555))
                .withFont(new net.minecraft.network.chat.FontDescription.Resource(
                        net.minecraft.resources.Identifier
                                .fromNamespaceAndPath("minecraft", "tooltip/status"))));
    }

    private static Component literal(String text, int rgb) {
        return Component.literal(text).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(rgb)));
    }

    /** 找出畫面上某一段字用的顏色。只看葉子——容器自己沒有顏色。 */
    private static Integer colourOf(Component root, String needle) {
        for (Component part : leaves(root)) {
            if (part.getString().contains(needle)) {
                TextColor colour = part.getStyle().getColor();
                return colour == null ? null : colour.getValue();
            }
        }
        return null;
    }

    private static List<Component> leaves(Component component) {
        List<Component> out = new ArrayList<>();
        if (component.getSiblings().isEmpty()) {
            out.add(component);
        }
        for (Component child : component.getSiblings()) {
            out.addAll(leaves(child));
        }
        return out;
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
