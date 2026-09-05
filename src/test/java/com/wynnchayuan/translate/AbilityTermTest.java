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
 * 敘述裡的技能名稱，畫面上要換成中文。
 *
 * <h2>為什麼語料裡是英文</h2>
 * 技能名稱在原文裡帶著自己的顏色與底線，而顏色是拿<b>原文的字面</b>到譯文裡
 * 找的。語料裡先換成中文，顏色就貼不回去了。所以語料寫的是
 * 「Bash 的打擊次數變為 +{~} 倍。」——真正換掉名稱的是<b>詞表</b>
 * （{@link TranslationStore#findTerm}），在畫的時候逐個替換。
 *
 * <p>也就是說：「語料裡是英文」是對的，「<b>畫面上</b>還是英文」才是壞的。
 *
 * <h2>這條測試在盯什麼</h2>
 * {@code MajorIdTermsTest} 已經在盯詞表本身——名稱有沒有進去、夠不夠長。
 * 但詞表有東西不代表換得掉：畫的時候，詞表要跟<b>重點段</b>（原文裡樣式不同
 * 的片段）搶同一個位置，而技能名稱兩邊都算——它在原文裡就是帶樣式的。
 *
 * <p>兩者同一個位置、同樣長時，先前是重點段勝，而重點段做的事是
 * <b>原樣貼回英文</b>。於是詞表滿的、validate 綠的、{@code MajorIdTermsTest}
 * 也綠的，畫面上卻始終是「Bash 的打擊次數」，而對照表寫的是「重擊」。
 * 使用者回報的正是這個。
 *
 * <p>所以這裡不問詞表，直接<b>把整段畫出來</b>再看畫面上剩什麼。
 */
public final class AbilityTermTest {

    private static int failures = 0;

    /** 技能名稱在原文裡的樣子：灰色、加底線，跟本文不同色。 */
    private static final int NAME_COLOUR = 0xAAAAAA;

    private static final int BODY_COLOUR = 0xFFFFFF;

    /**
     * 一行意象敘述：名稱一段、本文一段，兩段樣式不同。
     *
     * <p>這正是實機送過來的形狀——名稱之所以同時是重點段又是詞表裡的詞，
     * 就是因為它自己帶了樣式。
     */
    private static List<StyledText> block(String name, String rest) {
        MutableComponent line = Component.empty();
        line.append(Component.literal(name).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(NAME_COLOUR)).withUnderlined(true)));
        line.append(Component.literal(rest).withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(BODY_COLOUR))));
        List<StyledText> out = new ArrayList<>();
        out.add(StyledText.fromComponent(line));
        return out;
    }

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        swaps(store, "Bash", " gains +2x as many hits.", "重擊");
        swaps(store, "Charge", " deals damage and knocks back enemies.", "衝鋒");

        System.out.println(failures == 0
                ? "技能名稱替換：全部通過" : "技能名稱替換：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /**
     * 畫出來之後，名稱是中文而不是英文。
     *
     * <p>只在<b>語料真的收了這一句</b>時才判斷。這條測試盯的是替換，不是覆蓋率——
     * 語料沒有那一句時整段查不到是正常的，在這裡當成失敗只會製造假警報。
     */
    private static void swaps(TranslationStore store, String name, String rest,
                              String want) {
        String term = store.lookup(name);
        report("詞表裡有「" + name + "」（實際：" + term + "）", want.equals(term));

        Component built = LineTranslator.translate(block(name, rest).get(0), store);
        if (built == null) {
            System.out.println("  [跳過] 語料裡沒有「" + name + rest + "」這一句");
            return;
        }
        String all = built.getString();
        report("畫面上是「" + want + "」（實際：" + all + "）", all.contains(want));
        report("畫面上沒有留著英文的「" + name + "」（實際：" + all + "）", !all.contains(name));
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
