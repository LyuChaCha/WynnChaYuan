package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 重點段的樣式不可以貼到<b>英文詞的中間</b>。
 *
 * <h2>實機回報</h2>
 * 使用者回報「{@code Lootrun} 的 {@code run} 顏色被分開了」。畫面上那一行是
 * 「本次 Lootrun 完成 20 場挑戰之後，寶物加成 +20%」——{@code Loot} 四個字母
 * 一種顏色，{@code run} 三個字母另一種，一個詞從中間斷成兩色。
 *
 * <h2>怎麼壞的</h2>
 * 原文是 {@code Lootrun, gain +20% Loot}，其中 {@code Loot}（寶物加成那個詞）
 * 自成一段、自己一個顏色，所以被收成重點段。重點段是照<b>字面</b>到譯文裡找
 * 位置貼回去的，而譯文裡 {@code Lootrun} 照慣例留著英文——
 * {@code indexOf("Loot")} 先配到的是它的前四個字母。
 *
 * <p>詞表那一路（{@link TranslationStore#findTerm}）本來就用
 * {@link TranslationStore#wordChar} 判詞界，不會犯這個錯；只有重點段這一路
 * 用的是裸的 {@code indexOf}。修法是兩邊共用同一條界線，見
 * {@code LineTranslator#indexOfWhole}。
 *
 * <h2>這條測試在盯什麼</h2>
 * 不是「顏色對不對」，而是<b>那個詞有沒有被切開</b>——切開之後無論兩半各是什麼
 * 顏色都是錯的。所以判斷的是「{@code Lootrun} 七個字母有沒有落在同一段裡」。
 *
 * <p>另外兩條是反向的：詞界只擋英文，中文與獨立的英文詞照樣要貼得上。
 * 只盯前者的話，把重點段整批關掉也會通過。
 */
public final class AccentWordBoundaryTest {

    private static int failures = 0;

    private static final int BODY = 0xAAAAAA;    // 本文：灰
    private static final int VALUE = 0xFFFFFF;   // 數值：白
    private static final int STAT = 0xFFAA00;    // 詞條名：橘

    public static void main(String[] args) throws Exception {
        FlowedDebug.init(Files.createTempDirectory("wynnchayuan"));
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                              Languages.DEFAULT));

        lootrunNotSplit(store);
        cjkAccentStillPastes();
        wholeEnglishWordStillPastes();

        System.out.println(failures == 0
                ? "重點段詞界：全部通過" : "重點段詞界：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /**
     * 使用者回報的那一行：{@code Loot} 不可以貼進 {@code Lootrun} 裡面。
     *
     * <p>走的是<b>整行命中</b>那條路，所以先釘住模板等於語料的鍵——模板一旦對不上
     * 就落到逐片段，那條路壓根不會走到這個分支，測了等於沒測。
     */
    private static void lootrunNotSplit(TranslationStore store) {
        String key = "Lootrun, gain +{~} Loot";
        String zh = store.lookup(key);
        report("語料收著這一句（實際：" + zh + "）", zh != null);
        if (zh == null) {
            return;                            // 語料被改掉了就別再往下假裝
        }
        report("★ 譯文裡 Lootrun 留著英文，這條測試才有意義（實際：" + zh + "）",
               zh.contains("Lootrun"));

        StyledText styled = line("Lootrun, gain ", BODY, "+20%", VALUE,
                                 " ", BODY, "Loot", STAT);
        report("★ 模板就是語料的鍵（實際："
               + com.wynnchayuan.capture.LineParts.of(styled).template() + "）",
               key.equals(com.wynnchayuan.capture.LineParts.of(styled).template()));

        Component built = LineTranslator.translate(styled, store);
        report("翻得出來", built != null);
        if (built == null) {
            return;
        }
        List<String> parts = pieces(built);
        System.out.println("  畫出去的分段：" + parts);

        boolean whole = false;
        for (String p : parts) {
            if (p.contains("Lootrun")) {
                whole = true;
            }
        }
        report("★ Lootrun 七個字母在同一段裡，沒有被切成 Loot + run（分段："
               + parts + "）", whole);
        report("整句畫出來還是完整的（實際：" + built.getString() + "）",
               built.getString().contains("Lootrun"));
    }

    /**
     * 反向：中文的重點段照樣貼得上。
     *
     * <p>Java 認為漢字是字母，所以詞界如果用 {@code Character.isLetterOrDigit}
     * 判，「寶物加成」夾在中文裡兩邊都算「同一個詞的一部分」，整批重點段就失效了。
     * {@code wordChar} 把 {@code 0x2E80} 以上排除在外正是為了這個。
     */
    private static void cjkAccentStillPastes() throws Exception {
        TranslationStore store = corpus("Gain +{~} Loot now", "現在獲得 +{~} 寶物加成");
        StyledText styled = line("Gain ", BODY, "+20%", VALUE,
                                 " Loot now", STAT);
        Component built = LineTranslator.translate(styled, store);
        report("［中文重點段］翻得出來", built != null);
        if (built == null) {
            return;
        }
        System.out.println("  畫出去的分段：" + pieces(built));
        report("★［中文重點段］「寶物加成」還是拿得到橘色（實際："
               + show(colourOf(built, "寶物加成")) + "）",
               Integer.valueOf(STAT).equals(colourOf(built, "寶物加成")));
    }

    /** 反向：獨立成詞的英文照樣貼得上，詞界擋的只有「貼到詞中間」。 */
    private static void wholeEnglishWordStillPastes() throws Exception {
        TranslationStore store = corpus("Reduce the cost of Bash", "降低 Bash 的消耗");
        StyledText styled = line("Reduce the cost of ", BODY, "Bash", STAT);
        Component built = LineTranslator.translate(styled, store);
        report("［獨立英文詞］翻得出來", built != null);
        if (built == null) {
            return;
        }
        System.out.println("  畫出去的分段：" + pieces(built));
        report("★［獨立英文詞］Bash 還是拿得到橘色（實際："
               + show(colourOf(built, "Bash")) + "）",
               Integer.valueOf(STAT).equals(colourOf(built, "Bash")));
    }

    // ------------------------------------------------------------ 小工具

    /** {@code 文字, 顏色, 文字, 顏色…} 串成一行，這就是實機送過來的形狀。 */
    private static StyledText line(Object... parts) {
        MutableComponent out = Component.empty();
        for (int i = 0; i < parts.length; i += 2) {
            out.append(Component.literal((String) parts[i]).withStyle(
                    Style.EMPTY.withColor(TextColor.fromRgb((Integer) parts[i + 1]))));
        }
        return StyledText.fromComponent(out);
    }

    private static TranslationStore corpus(String key, String zh) throws Exception {
        Path dir = Files.createTempDirectory("accent-word");
        Files.writeString(dir.resolve("misc.json"),
                "{\"" + key + "\": \"" + zh + "\"}");
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    private static List<String> pieces(Component built) {
        List<String> out = new ArrayList<>();
        built.visit((style, s) -> {
            out.add(s);
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    /** 含有 {@code needle} 的<b>整</b>段的顏色；被拆開就拿不到，回傳 null。 */
    private static Integer colourOf(Component built, String needle) {
        List<Integer> found = new ArrayList<>();
        built.visit((style, s) -> {
            if (s.contains(needle) && style.getColor() != null) {
                found.add(style.getColor().getValue() & 0xFFFFFF);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return found.isEmpty() ? null : found.get(0);
    }

    private static String show(Integer rgb) {
        return rgb == null ? "沒拿到（多半是被拆成兩段）" : String.format("#%06X", rgb);
    }

    private static void report(String name, boolean ok) {
        System.out.println((ok ? "  [通過] " : "  [失敗] ") + name);
        if (!ok) {
            failures++;
        }
    }
}
