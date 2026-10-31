package com.wynnchayuan.listener;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class DialogueChoiceTest {

    public static void main(String[] args) {
        for (String real : new String[] {
                "Just saying hello",
                "Who are you?",
                "What are you looking at?",
                "I don't remember anything from before you.",
                "[Leave]",
                "…nothing."}) {
            check("不誤擋真選項：" + real,
                  !ActionBarListener.looksClipped(real));
        }

        for (String clipped : new String[] {
                "mber anything from before yo",
                "ber anything from before you",
                "er anything from before you",
                "so optimistic about being a"}) {
            check("擋得下殘句：" + clipped,
                  ActionBarListener.looksClipped(clipped));
        }

        for (String clipped : new String[] {
                "'m not going to help you wit",
                ", but I never saw him again.",
                ". Nobody told me about that."}) {
            check("符號開頭的殘句也擋得下：" + clipped,
                  ActionBarListener.looksClipped(clipped));
        }

        for (String real : new String[] {"…nothing.", "[Leave]", "(Say nothing)"}) {
            check("不誤擋符號開頭的真選項：" + real,
                  !ActionBarListener.looksClipped(real));
        }

        check("空字串不當成殘句", !ActionBarListener.looksClipped(""));
        check("null 不當成殘句", !ActionBarListener.looksClipped(null));

        if (failures > 0) {
            System.out.println("對話選項：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("對話選項：全部通過");
    }

}
