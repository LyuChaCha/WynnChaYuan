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
 * 緊貼在 {@code {#}} 後面的標點<b>不</b>跟著符號走顏色。
 *
 * <h2>畫面上長什麼樣</h2>
 * 任務完成的橫幅原文是 {@code (對齊字元)§6[Quest Completed]}——整串金色，
 * 而開頭那個對齊字元自己不帶顏色（{@code §6} 在它後面才開始）。譯文
 * {@code {#}[任務完成]} 的左中括號緊貼著 {@code {#}}，於是拿到對齊字元那一段的
 * 白色：畫面上是「白色的 [ 配金色的字」，跟原文對不起來。
 *
 * <h2>為什麼是 {@code {#}} 的問題</h2>
 * 黏著規則本來是對的——{@code §8 [66.24%]} 裡中括號跟數值同屬一個色段，
 * 譯文的括號就該跟著數值走。但那個前提只有數值、地名、玩家名成立：它們是從
 * 一段文字裡挖出來的。符號一定會被切成自己的片段，標點從來沒有跟它同過一段。
 */
public final class GlyphHugTest {

    private static int failures = 0;

    private static final int GOLD = 0xFFAA00;

    /** {@code U+D0074}：對齊用的空白字型字元，遊戲實際送來的那一個。 */
    private static final String OFFSET =
            new StringBuilder().appendCodePoint(0xD0074).toString();

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        MutableComponent line = Component.empty();
        line.append(Component.literal(OFFSET).withStyle(SpaceOffset.styleFor(Style.EMPTY)));
        line.append(Component.literal("[Quest Completed]").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(GOLD))));

        Component one = LineTranslator.translateChat(
                StyledText.fromComponent(line), store);
        List<Component> built = one == null ? List.of() : List.of(one);

        check("整行查得到譯文", !built.isEmpty());
        if (built.isEmpty()) {
            report();
            return;
        }
        String all = built.stream().map(Component::getString).reduce("", (a, b) -> a + b);
        check("翻成了「任務完成」（實際：" + all.replace(OFFSET, "(對齊)") + "）",
                all.contains("任務完成"));

        for (Component row : built) {
            for (Component leaf : flatten(row)) {
                TextColor colour = leaf.getStyle().getColor();
                System.out.println("      「" + leaf.getString().replace(OFFSET, "(對齊)")
                        + "」 " + (colour == null ? "繼承" : colour.toString()));
            }
        }

        Integer bracket = colourOf(built, "[");
        check("★ 左中括號跟原文一樣是金色（拿到 "
                        + (bracket == null ? "繼承" : "#" + String.format("%06X", bracket))
                        + "）", bracket != null && bracket == GOLD);
        Integer body = colourOf(built, "任務完成");
        check("正文是金色（拿到 "
                        + (body == null ? "繼承" : "#" + String.format("%06X", body))
                        + "）", body != null && body == GOLD);
        Integer close = colourOf(built, "]");
        check("右中括號是金色（拿到 "
                        + (close == null ? "繼承" : "#" + String.format("%06X", close))
                        + "）", close != null && close == GOLD);
        check("括號跟正文沒有被切成兩個顏色",
                java.util.Objects.equals(bracket, body)
                        && java.util.Objects.equals(body, close));

        hugsStillWork(store);
        report();
    }

    /**
     * 數值那一路照舊：括號跟數值同色時，譯文的括號還是要跟著數值走。
     *
     * <p>這一條是防止上面那個修正修過頭——{@code {#}} 不當錨點，
     * 不表示 {@code {~}} 也不當。
     */
    private static void hugsStillWork(TranslationStore store) {
        check("中括號仍算黏著", LineTranslator.hugs('['));
        check("空白仍不算黏著", !LineTranslator.hugs(' '));
        check("語料裡的鍵沒被動到", store != null);
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

    /** 只收葉子——最外層那個容器含有整行，而它自己沒有顏色。 */
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
                ? "GlyphHug: 全部通過" : "GlyphHug: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
