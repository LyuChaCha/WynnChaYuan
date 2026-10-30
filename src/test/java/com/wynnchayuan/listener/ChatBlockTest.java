package com.wynnchayuan.listener;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

public final class ChatBlockTest {

    private static int failures = 0;

    private static StyledText line(String text) {
        return StyledText.fromComponent(Component.literal(text));
    }

    public static void main(String[] args) {
        ChatBlock.clear();
        check("一開始什麼都沒攢", ChatBlock.size() == 0);
        check("沒東西攢的時候不會送", !ChatBlock.ready(Long.MAX_VALUE));

        long now = 1_000_000L;
        ChatBlock.queue(line("[Quest Completed]"), Component.literal("[任務完成]"));
        ChatBlock.arrivedAt(now);
        check("收到一行就攢起來", ChatBlock.size() == 1);
        check("剛收到不送", !ChatBlock.ready(now));
        check("差一點也不送", !ChatBlock.ready(now + 199));
        check("安靜夠久才送", ChatBlock.ready(now + 200));

        ChatBlock.queue(line("King's Recruit"), Component.literal("國王的新兵"));
        ChatBlock.arrivedAt(now + 150);
        check("又來一行就重新計時", !ChatBlock.ready(now + 200));
        check("攢的是兩行", ChatBlock.size() == 2);
        check("從最後一行起算滿了才送", ChatBlock.ready(now + 350));

        ChatBlock.queue(line("- Rewards:"), null);
        check("查不到譯文的行也算一行", ChatBlock.size() == 3);

        ChatBlock.clear();
        for (int i = 0; i < 30; i++) {
            ChatBlock.queue(line("row " + i), Component.literal("第 " + i + " 行"));
        }
        check("洗版時不會無限攢（實際 " + ChatBlock.size() + " 行）",
                ChatBlock.size() > 0 && ChatBlock.size() <= 24);

        ChatBlock.clear();
        check("清掉之後是空的", ChatBlock.size() == 0);

        check("有翻到就送", ChatBlock.worthSending(true, true));
        check("一行都沒翻到就不送", !ChatBlock.worthSending(true, false));
        check("沒東西可送當然不送", !ChatBlock.worthSending(false, false));

        try {
            separateMessages();
        } catch (Exception e) {
            e.printStackTrace();
            check("separateMessages 跑完", false);
        }

        System.out.println(failures == 0
                ? "ChatBlock: 全部通過" : "ChatBlock: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void separateMessages() throws Exception {
        java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("wynnchayuan-chatblock");
        com.wynnchayuan.translate.FlowedDebug.init(dir);
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("Having issues with your content tracker not updating?",
                         "內容追蹤器沒有更新嗎？");
        root.addProperty("[Quest Completed]", "[任務完成]");
        root.addProperty("- Rewards:", "- 獎勵:");
        java.nio.file.Files.writeString(dir.resolve("misc.json"), root.toString(),
                                        java.nio.charset.StandardCharsets.UTF_8);
        com.wynnchayuan.translate.TranslationStore store =
                new com.wynnchayuan.translate.TranslationStore();
        store.loadAll(dir);

        StyledText banner = line("\nWelcome to Wynncraft!\nplay.wynncraft.com -/- wynncraft.com\n\n"
                + "1 item was sold on the Trade Market\nand 2 Mounts finished eating while you were offline");
        StyledText tip = line("Having issues with your content tracker not updating?");
        StyledText wynnmod = line("                    \n          WYNNMOD INITIALIZED          \n"
                + "Commands: /wynnmod command · Config Menu: /wynnmod config");

        ChatBlock.clear();
        ChatBlock.queue(banner, null);
        ChatBlock.queue(tip, Component.literal("內容追蹤器沒有更新嗎？"));
        ChatBlock.queue(wynnmod, null);
        Component made = ChatBlock.preview(store);
        String all = made == null ? "" : made.getString();
        System.out.println("      | " + all.replace("\n", " / "));
        check("翻到的那一則照樣送", all.contains("內容追蹤器沒有更新嗎？"));
        check("★ 沒翻到的歡迎訊息不再貼一次", !all.contains("Welcome to Wynncraft!"));
        check("★ 沒翻到的 WynnMod 那一塊不再貼一次", !all.contains("INITIALIZED"));

        ChatBlock.clear();
        ChatBlock.queue(line("[Quest Completed]"), Component.literal("[任務完成]"));
        ChatBlock.queue(line("Totally Unknown Reward"), null);
        ChatBlock.queue(line("- Rewards:"), Component.literal("- 獎勵:"));
        Component quest = ChatBlock.preview(store);
        String rows = quest == null ? "" : quest.getString();
        check("一則一行的清單：沒翻到的列照舊帶上",
              rows.contains("[任務完成]") && rows.contains("Totally Unknown Reward")
                      && rows.contains("- 獎勵:"));

        ChatBlock.clear();
        ChatBlock.queue(banner, null);
        ChatBlock.queue(wynnmod, null);
        check("一則都沒翻到就不送", ChatBlock.preview(store) == null);
        ChatBlock.clear();

        boolean[] carry = ChatBlock.carried(new boolean[] {true, false, false, true},
                                            new boolean[] {false, true, false, false});
        check("多行的自成一組、連續一行的是一組",
              !carry[0] && carry[1] && carry[2] && !carry[3]);
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
