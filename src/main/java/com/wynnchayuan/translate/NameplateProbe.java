package com.wynnchayuan.translate;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.LineParts;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 多行名牌「名字那一列自己翻得出來，整塊卻沒翻（或翻成別的名字）」時，把那塊名牌的
 * 片段結構寫進 {@code debug/nameplate-miss.txt}。
 *
 * <h2>為什麼需要這個</h2>
 * 實機回報「名牌原本有翻，一打下去頭上的名字就變回英文」。怪物被打之後名牌多出血條與
 * 狀態列，而那幾列長什麼樣子——哪個字型、哪些碼位、換行黏在哪一段——語料與
 * {@code captured.json} 都看不到：收集端只記<b>砍掉狀態列</b>之後的名字
 * （見 {@code CaptureListener#trimStatusLines}），而名字本身早就翻好了，所以缺口清單裡
 * 一條都不會出現。離線只能照猜的形狀測，猜錯了就修不到真正那一種。
 *
 * <p>所以只記「名字自己翻得出來、整塊卻不行」的那幾塊：那正是這個症狀，
 * 一般沒翻的名牌（名字本來就不在語料裡）不記，免得額度被洗光。
 *
 * <p>只在 F6 的「診斷檔」開著時寫（{@code WynnChaYuan} 照那個開關決定要不要 init），
 * 同一個模板只記一次，最多幾十筆。
 */
public final class NameplateProbe {

    private static final String FILE = "nameplate-miss.txt";

    /** 最多記幾塊。夠看出形狀就好。 */
    private static final int LIMIT = 40;

    /** 看過的模板（含路徑），最多記這麼多個，滿了就不再查。 */
    private static final int SEEN_LIMIT = 4096;

    private static Path file;
    private static int written;
    private static final java.util.Set<String> seen = new java.util.HashSet<>();
    private static final StringBuilder buffer = new StringBuilder();

    private NameplateProbe() {}

    public static void init(Path debugDir) {
        file = debugDir.resolve(FILE);
        buffer.append("# WynnChaYuan v").append(WynnChaYuan.version())
              .append("　多行名牌：名字那一列自己翻得出來，整塊卻沒翻（或名字翻得不一樣）")
              .append(System.lineSeparator())
              .append("# 每一段：字型、顏色、文字（看不見的碼位寫成 U+XXXX，換行寫成 ⏎）")
              .append(System.lineSeparator()).append(System.lineSeparator());
    }

    /**
     * 名牌翻完之後叫一次。
     *
     * @param path   哪一條路（TextDisplay 的名牌事件，或盔甲座的實體名稱）
     * @param label  原文
     * @param result 整塊的譯文；沒翻就是 {@code null}
     */
    public static void check(String path, StyledText label, Component result,
                             TranslationStore store) {
        if (file == null || label == null || store == null || written >= LIMIT
                || !WynnChaYuan.config().debugDumps()) {
            return;
        }
        try {
            String plain = label.getStringWithoutFormatting();
            if (plain.indexOf('\n') < 0) {
                return;
            }
            String template = LineParts.of(label).template();
            synchronized (seen) {
                if (seen.size() >= SEEN_LIMIT || !seen.add(path + '\0' + template)) {
                    return;
                }
            }
            StyledText[] rows = label.split("\n");
            if (rows.length == 0) {
                return;
            }
            Component alone = LineTranslator.translateLabel(rows[0], store);
            if (alone == null) {
                return;                        // 名字本來就翻不出來，不是這個症狀
            }
            String name = alone.getString().strip();
            String got = result == null ? null : result.getString().split("\n", -1)[0].strip();
            if (got != null && got.equals(name)) {
                return;                        // 名字照樣翻出來了
            }
            synchronized (buffer) {
                if (written >= LIMIT) {
                    return;
                }
                written++;
                String nl = System.lineSeparator();
                buffer.append("=== ").append(written).append(" · ").append(path)
                      .append(result == null ? " · 整塊沒翻" : " · 名字翻得不一樣").append(" ===").append(nl)
                      .append("模板: ").append(template.replace("\n", "⏎")).append(nl)
                      .append("第一列模板: ").append(LineParts.of(rows[0]).template()).append(nl)
                      .append("第一列單獨的譯文: ").append(name).append(nl)
                      .append("整塊的譯文: ")
                      .append(result == null ? "(null)" : result.getString().replace("\n", "⏎"))
                      .append(nl)
                      .append("列數（照 StyledText 切）: ").append(rows.length).append(nl);
                dump(label.getComponent());
                buffer.append(nl);
                Files.writeString(file, buffer.toString(), StandardCharsets.UTF_8);
            }
        } catch (Throwable t) {
            // 診斷本身絕不能影響遊戲
        }
    }

    private static void dump(Component component) {
        int[] index = {0};
        component.visit((style, text) -> {
            if (!text.isEmpty()) {
                buffer.append(String.format("  [%2d] font=%-32s color=%-8s %s text=%s%n",
                        index[0]++,
                        style.getFont() == null ? "(null)" : style.getFont().toString(),
                        style.getColor() == null ? "(null)" : style.getColor().serialize(),
                        (style.isBold() ? "粗" : "") + (style.isItalic() ? "斜" : ""),
                        describe(text)));
            }
            return Optional.empty();
        }, Style.EMPTY);
    }

    /** 看不見的碼位寫成 U+XXXX，換行寫成 ⏎，空白寫成 ␠。 */
    private static String describe(String text) {
        StringBuilder sb = new StringBuilder();
        text.codePoints().forEach(cp -> {
            if (cp == '\n') {
                sb.append('⏎');
            } else if (cp == ' ') {
                sb.append('␠');
            } else if (Character.isISOControl(cp) || cp > 0xFFFF
                    || Character.getType(cp) == Character.PRIVATE_USE
                    || Character.getType(cp) == Character.UNASSIGNED
                    || Character.getType(cp) == Character.FORMAT) {
                sb.append(String.format("<U+%04X>", cp));
            } else {
                sb.appendCodePoint(cp);
            }
        });
        return sb.toString();
    }
}
