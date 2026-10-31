package com.wynnchayuan.capture;

import com.google.gson.JsonArray;
import net.minecraft.network.chat.Style;

import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class ColourAuditTest {

    private static final Style GREY = Style.EMPTY.withColor(0xAAAAAA);
    private static final Style WHITE = Style.EMPTY.withColor(0xFFFFFF);
    private static final Style GOLD = Style.EMPTY.withColor(0xFFAA00).withBold(true);

    private static LineParts.Piece piece(String text, Style style) {
        return new LineParts.Piece(text, style);
    }

    public static void main(String[] args) {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        List<String> src = List.of("Defend the Giant Void Hole for {~} seconds.");
        String[] dst = {"守住巨型虛空洞 {~} 秒。"};
        List<LineParts.Piece> runs = List.of(
                piece("Defend the ", GREY),
                piece("Giant Void Hole", WHITE),
                piece(" for ", GREY),
                piece("51", GOLD),
                piece(" seconds.", GREY));
        List<LineParts.Piece> accents = List.of(
                piece("Giant Void Hole", WHITE), piece("51", GOLD));

        ColourAudit.Row row = ColourAudit.audit(src, dst, runs, accents,
                new boolean[] {false, true}, GREY);
        check("★ 掉了一個顏色的句子會被記下來", row != null);
        if (row != null) {
            check("掉的是白色那一段（" + row.missed() + "）",
                    row.missed().equals(List.of("#FFFFFF")));
            check("原文的鍵就是模板", row.src().equals(src.get(0)));
            check("相鄰同色的片段沒有被硬併：五段照舊是五段（" + row.runs().size() + "）",
                    row.runs().size() == 5);
            check("★ 數字換成佔位符，不把實際數值寫出去",
                    row.runs().get(3).text().equals("{~}"));
            check("粗體寫在色碼後面", row.runs().get(3).colour().equals("#FFAA00b"));
        }

        check("★ 每個顏色都貼上了就不記",
                ColourAudit.audit(src, dst, runs, accents, new boolean[] {true, true}, GREY) == null);
        check("主色（譯文的底色）不算掉了",
                ColourAudit.audit(src, dst, List.of(piece("Defend the ", GREY),
                        piece("Giant Void Hole", WHITE)), List.of(piece("Defend the ", GREY)),
                        new boolean[] {false}, WHITE) != null
                && ColourAudit.audit(src, dst, List.of(piece("Defend the ", GREY),
                        piece("Giant Void Hole", WHITE)), List.of(piece("Giant Void Hole", WHITE)),
                        new boolean[] {true}, GREY) == null);
        check("★ 譯者自己寫了色碼的不記",
                ColourAudit.audit(src, new String[] {"守住{c2}巨型虛空洞{/} {~} 秒。"}, runs, accents,
                        new boolean[] {false, true}, GREY) == null);
        check("只有一種顏色的句子不記",
                ColourAudit.audit(src, dst, List.of(piece("Defend the hole.", GREY)), List.of(),
                        new boolean[0], GREY) == null);
        check("★ 帶玩家名字佔位符的整條不記",
                ColourAudit.audit(List.of("{u} has entered the White Grotto"), dst, runs, accents,
                        new boolean[] {false, true}, GREY) == null);
        check("只有數字不同色的不記",
                ColourAudit.audit(src, dst, List.of(piece("Hold for ", GREY), piece("51", GOLD),
                        piece(" seconds", GREY)), List.of(piece("51", GOLD)),
                        new boolean[] {false}, GREY) == null);
        List<String> plate = List.of("Palace Soldier {#}{#}", "{#}", "{#} {#} {#} {~}s");
        String[] plateDst = {"宮廷士兵 {#}{#}", "{#}", "{#} {#} {#} {~}s"};
        check("★ 只有「數值＋單位」不同色的不記（名牌倒數）",
                ColourAudit.audit(plate, plateDst, List.of(piece("Palace Soldier", WHITE),
                        piece("12s", GREY)), List.of(piece("12s", GREY)),
                        new boolean[] {false}, WHITE) == null);
        check("數值後面隔著空白的字母不是單位，照舊要記（「12 x 名稱」的 x）",
                ColourAudit.audit(List.of("{~} x Zombie Eye {#}{#}{#}"),
                        new String[] {"{~} x 殭屍之眼 {#}{#}{#}"},
                        List.of(piece("12 x ", GREY), piece("Zombie Eye", WHITE)),
                        List.of(piece("12 x ", GREY)), new boolean[] {false}, WHITE) != null);

        ColourAudit.clear();
        ColourAudit.takeDirty();
        ColourAudit.note(src, dst, runs, accents, new boolean[] {false, true}, GREY);
        check("note 之後清單裡有一句，而且標成要存檔",
                ColourAudit.size() == 1 && ColourAudit.takeDirty());
        ColourAudit.note(src, dst, runs, accents, new boolean[] {false, true}, GREY);
        check("同一句每一幀都會來，不重複記、也不一直要求存檔",
                ColourAudit.size() == 1 && !ColourAudit.takeDirty());
        JsonArray json = ColourAudit.toJson();
        ColourAudit.clear();
        ColourAudit.load(json);
        check("寫出去再讀回來還是那一句", ColourAudit.size() == 1
                && ColourAudit.toJson().toString().equals(json.toString()));
        ColourAudit.takeDirty();
        String[] fixed = {"守住巨型虛空洞，撐 {~} 秒。"};
        ColourAudit.note(src, fixed, runs, accents, new boolean[] {true, true}, GREY);
        check("★ 修好之後再看到一次，那一句自己從清單上消失",
                ColourAudit.size() == 0 && ColourAudit.takeDirty());
        ColourAudit.load(new com.google.gson.JsonPrimitive("壞掉的格式"));
        check("讀到不認得的格式不會出事", ColourAudit.size() == 0);

        System.out.println(failures == 0 ? "\n顏色對照記錄：全部通過"
                : "\n顏色對照記錄：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

}
