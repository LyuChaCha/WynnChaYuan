package com.wynnchayuan.translate;

import java.nio.file.Path;

/**
 * Major ID 的說明在遊戲裡查不查得到。
 *
 * <p>這條路上有<b>四個</b>地方都會讓它靜默失效，而且四個都真的發生過：
 *
 * <ol>
 *   <li>語料裡的數字沒有參數化（{@code 16} vs {@code {~}}）</li>
 *   <li>攤平索引沒收到該收的條目</li>
 *   <li>空白正規化漏掉換行</li>
 *   <li>名稱前面的 {@code ✦} 沒剝掉，對不上語料裡的 {@code Altruism}</li>
 * </ol>
 *
 * <p>畫面上四種的症狀完全一樣：說明還是英文。所以用真正的 {@code major-id.json}
 * 跑一次，而不是自己捏假資料——捏的資料只會證明捏的人想的是對的。
 */
public final class MajorIdTest {

    private static int failures = 0;

    /** 遊戲畫面上的樣子：名稱與說明擠在第一行，其餘依 tooltip 寬度斷行。 */
    private static final String AS_RENDERED =
            "✦ Altruism: Allies within {~}\n"
            + "blocks gain {~} of the\n"
            + "health you gain from Health\n"
            + "Regen and Life Steal.";

    public static void main(String[] args) {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations"));

        check("語料本身讀得到名稱", "利他主義".equals(store.lookup("Altruism")));

        String flowed = LineTranslator.lookupFlowed(AS_RENDERED, store);
        check("整段（含 ✦ 與自動斷行）查得到", flowed != null);
        if (flowed != null) {
            check("名稱有翻到", flowed.contains("利他主義"));
            check("說明有翻到", flowed.contains("格內的友軍"));
            check("佔位符數量守住", count(flowed, "{~}") == count(AS_RENDERED, "{~}"));
        }

        // 反面：冒號兩邊只要有一邊不在語料裡就不該亂猜
        check("不認得的名稱不會硬翻",
                LineTranslator.lookupFlowed(
                        "✦ Nonexistent: Allies within {~} blocks gain {~} of the "
                        + "health you gain from Health Regen and Life Steal.", store) == null);

        System.out.println(failures == 0
                ? "MajorId: 全部通過" : "MajorId: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + 1)) {
            n++;
        }
        return n;
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + name);
        if (!ok) {
            failures++;
        }
    }
}
