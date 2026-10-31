package com.wynnchayuan.render;

import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

// 實機錄下：文字 35px 尾隨 +81、93px +23、149px -33
public final class DialogueOffsetTest {

    public static void main(String[] args) {
        check("編碼 -116 再解回來還是 -116",
                DialogueLayout.offsetOf(DialogueLayout.offset(-116)) == -116);
        check("正的也一樣",
                DialogueLayout.offsetOf(DialogueLayout.offset(81)) == 81);
        check("0 也是合法的位移",
                DialogueLayout.offsetOf(DialogueLayout.offset(0)) == 0);
        check("超出上限就編不出來", DialogueLayout.offset(4097) == null);
        check("超出下限也編不出來", DialogueLayout.offset(-4097) == null);
        check("剛好在邊界上還是可以", DialogueLayout.offset(4096) != null);
        check("負的邊界也可以", DialogueLayout.offset(-4096) != null);
        check("編得出來的一定解得回去",
                DialogueLayout.offsetOf(DialogueLayout.offset(4096)) == 4096);

        check("35px 的文字 → 尾隨 +81", -(-116) - 35 == 81);
        check("93px 的文字 → 尾隨 +23", -(-116) - 93 == 23);
        check("149px 的文字 → 尾隨 -33", -(-116) - 149 == -33);

        check("The Cook 的前導是 -60", DialogueLayout.nameLead(40) == -60);
        check("Enzan 的前導是 -52", DialogueLayout.nameLead(25) == -52);

        check("尾隨的位移字要拿掉",
                Boxes.dropOffsets("Agh!" + new String(Character.toChars(0xD0063))).equals("Agh!"));
        check("前導的也一樣",
                Boxes.dropOffsets(new String(Character.toChars(0xCFF8C)) + "Agh!").equals("Agh!"));
        check("私用區的圖示不能拿掉",
                Boxes.dropOffsets(" 空手右鍵").equals(" 空手右鍵"));
        String badge = new String(Character.toChars(0xE060))
                + new String(Character.toChars(0xCFFFF))
                + new String(Character.toChars(0xE03B));
        check("徽章（圖示夾位移）整段不動",
                Boxes.dropOffsets(badge).equals(badge));
        check("位移加文字還是要拿掉",
                Boxes.dropOffsets("Agh!" + new String(Character.toChars(0xD0063)))
                        .equals("Agh!"));

        check("沒有位移字就原字串回去",
                Boxes.dropOffsets("啊！").equals("啊！"));

        badgeLine();

        check("英文不是位移", DialogueLayout.offsetOf("A") == null);
        check("中文不是位移", DialogueLayout.offsetOf("廚") == null);
        check("兩個字元不是位移", DialogueLayout.offsetOf("AB") == null);

        check("英文畫得出來", DialogueFonts.drawable("Let's hope no more grooks"));
        check("數字符號畫得出來", DialogueFonts.drawable("+12% (3/5) [80.0%]"));
        check("西班牙文畫得出來", DialogueFonts.drawable("Aumenta el dano magico"));
        check("法文重音畫得出來", DialogueFonts.drawable("Augmente les degats"));
        check("中文畫不出來", !DialogueFonts.drawable("希望別再有"));
        check("中英混排畫不出來", !DialogueFonts.drawable("希望別再有 grook"));
        check("日文畫不出來", !DialogueFonts.drawable("グルック"));
        check("諺文畫不出來", !DialogueFonts.drawable("계속하려면"));
        check("西里爾畫不出來", !DialogueFonts.drawable("продолжить"));
        check("ç 交給配對字型", !DialogueFonts.drawable("devam etmek için"));
        String control = "minecraft:hud/dialogue/text/control";
        String body = "minecraft:hud/dialogue/text/wynncraft/body_0";
        check("提示列：é 交給配對字型", !DialogueFonts.drawable("Muévete para continuar", control));
        check("提示列：純 ASCII 留在原字型", DialogueFonts.drawable("para continuar", control));
        check("內文列：é 留在原字型", DialogueFonts.drawable("Muévete para continuar", body));
        check("內文列：ç 交給配對字型", !DialogueFonts.drawable("devam etmek için", body));

        check("接第一行不補空格",
                DialogueRewriter.joinRow("", "Go on").equals("Go on"));
        check("接下一行補一個空格",
                DialogueRewriter.joinRow("My brother", "keeps sending")
                        .equals(" keeps sending"));
        check("前面已經有空白就不補",
                DialogueRewriter.joinRow("My brother ", "keeps")
                        .equals("keeps"));
        check("後面自己帶空白也不補",
                DialogueRewriter.joinRow("My brother", " keeps")
                        .equals(" keeps"));

        check("打完就是全部",
                DialogueTyping.typedSoFar("希望別再有人闖進來", 20, 20)
                        .equals("希望別再有人闖進來"));
        check("打一半就取一半",
                DialogueTyping.typedSoFar("希望別再有人闖進來", 10, 20)
                        .equals("希望別再有"));
        check("一個字都還沒打就空的",
                DialogueTyping.typedSoFar("希望別再有人闖進來", 0, 20)
                        .isEmpty());
        check("不切在佔位符中間",
                DialogueTyping.typedSoFar("你好 {~1} 位客人", 5, 10)
                        .equals("你好 "));
        check("佔位符整個進來了就留著",
                DialogueTyping.typedSoFar("你好 {~1} 位", 8, 10)
                        .equals("你好 {~1}"));

        List<String> narrow = DialogueLayout.wrap("abcdefgh", 2, null, 24);
        check("窄的時候一行只放得下四個字",
                narrow != null && narrow.get(0).equals("abcd")
                        && narrow.get(1).equals("efgh"));
        List<String> wide = DialogueLayout.wrap("abcdefgh", 2, null, 48);
        check("寬的時候一行就放得完",
                wide != null && wide.get(0).equals("abcdefgh"));

        List<String> id = DialogueLayout.wrap("走吧！Tasim、Green_teaTW，來比", 2, null, 84);
        check("玩家 ID 不從中間切（實際：" + id + "）",
                id != null && id.get(0).equals("走吧！Tasim、")
                        && id.get(1).startsWith("Green_teaTW"));

        String says = "I don't suppose you've seen {u} around here?";
        check("整句還沒打完，是同一句",
                DialogueTyping.within(says, "I don't suppose you've"));
        check("名字打到一半，還是同一句",
                DialogueTyping.within(says, "I don't suppose you've seen Green_te"));
        check("名字打完收成佔位符，還是同一句",
                DialogueTyping.within(says, "I don't suppose you've seen {u} around"));
        check("開頭就不一樣的，不是同一句",
                !DialogueTyping.within(says, "Well met, traveller!"));
        check("後面還接了別的東西，就不是同一句",
                !DialogueTyping.within("New Quest Started: King's Recruit",
                        "New Quest Started: King's Recruit [{~}/{~} ({~}%)]"));
        check("岔開之後長到離譜的，不算",
                !DialogueTyping.within(says,
                        "I don't suppose you've seen "
                                + "abcdefghijklmnopqrstuvwxyz0123456789"
                                + "abcdefghijklmnopqrstuvwxyz"));

        String rival = "They've been at it for a while now. "
                + "Which one do you think will beat the other tomorrow?";
        check("岔在句子中間的，是別句話（打到一半）",
                !DialogueTyping.within(rival,
                        "They've been a symbol of our town for lon"));
        check("岔在句子中間的，是別句話（打完）",
                !DialogueTyping.within(rival,
                        "They've been a symbol of our town for longer than anyone "
                                + "can remember, their original use long forgotten."));
        check("岔在佔位符上的仍然算同一句",
                DialogueTyping.within("Nice to see you again, {u}! How goes it?",
                        "Nice to see you again, Green_te"));

        check("字還在長就不算停下來",
                !DialogueTyping.settled("Bloc") && !DialogueTyping.settled("Block"));
        boolean early = false;
        for (int i = 0; i < 5; i++) {
            early |= DialogueTyping.settled("Block");
        }
        check("字與字之間的空檔不會被誤判成停下來", !early);
        boolean late = false;
        for (int i = 0; i < 4; i++) {
            late |= DialogueTyping.settled("Block");
        }
        check("停夠久就算停下來了", late);
        check("字又長出來就重新計算", !DialogueTyping.settled("Block t"));

        System.out.println(failures == 0
                ? "DialogueOffset: 全部通過"
                : "DialogueOffset: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void badgeLine() {
        net.minecraft.network.chat.Style pill =
                net.minecraft.network.chat.Style.EMPTY.withFont(
                        new net.minecraft.network.chat.FontDescription.Resource(
                                net.minecraft.resources.Identifier
                                        .withDefaultNamespace("banner/pill")));
        String bg = cp(0xE060) + cp(0xCFFFF) + cp(0xE062);
        String pull = cp(0xCFFE2);
        String lv = cp(0xE00B) + cp(0xE015) + cp(0xE029);

        net.minecraft.network.chat.Component plate =
                net.minecraft.network.chat.Component.empty()
                        .append(net.minecraft.network.chat.Component.literal(
                                "Kandon-Beda 市民 "))
                        .append(net.minecraft.network.chat.Component.literal(bg)
                                .setStyle(pill))
                        .append(net.minecraft.network.chat.Component.literal(pull)
                                .setStyle(pill))
                        .append(net.minecraft.network.chat.Component.literal(lv)
                                .setStyle(pill));
        List<net.minecraft.network.chat.Component> plateLines = Boxes.toLines(plate);
        check("名牌只有一行", plateLines.size() == 1);
        check("拆成獨立片段的定位位移要留著",
                plateLines.get(0).getString().contains(pull));
        check("徽章底圖與數字都還在",
                plateLines.get(0).getString().contains(bg)
                        && plateLines.get(0).getString().contains(lv));

        net.minecraft.network.chat.Component talk =
                net.minecraft.network.chat.Component.literal("Agh!" + cp(0xD0063));
        check("沒有圖示的行，位移還是照拿",
                !Boxes.toLines(talk).get(0).getString().contains(cp(0xD0063)));

        net.minecraft.network.chat.Component both =
                net.minecraft.network.chat.Component.empty()
                        .append(net.minecraft.network.chat.Component.literal("市民 "))
                        .append(net.minecraft.network.chat.Component.literal(bg)
                                .setStyle(pill))
                        .append(net.minecraft.network.chat.Component.literal(pull)
                                .setStyle(pill))
                        .append(net.minecraft.network.chat.Component.literal(lv)
                                .setStyle(pill))
                        .append(net.minecraft.network.chat.Component.literal(
                                "\n" + "Agh!" + cp(0xD0063)));
        List<net.minecraft.network.chat.Component> two = Boxes.toLines(both);
        check("兩行分開判斷：徽章那行留著",
                two.size() == 2 && two.get(0).getString().contains(pull));
        check("兩行分開判斷：文字那行拿掉",
                two.size() == 2 && !two.get(1).getString().contains(cp(0xD0063)));
    }

    private static String cp(int value) {
        return new String(Character.toChars(value));
    }

}
