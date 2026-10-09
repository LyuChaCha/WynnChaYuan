package com.wynnchayuan.capture;

import com.google.gson.JsonArray;
import net.minecraft.network.chat.Style;

import java.util.List;

/**
 * 「原文有個顏色沒回到譯文上」的句子有沒有被記下來、記得對不對。
 *
 * <h2>為什麼要有</h2>
 * 使用者 2026-10-08：Lootrun、討伐戰漂浮字的敘述、任務說明的顏色「還是會有錯誤，
 * 需要人工對」。語料裡看不到顏色，譯者要對只能進遊戲一句一句看。
 * {@link ColourAudit} 把出問題的句子連同原文的顏色分段寫進 {@code captured.json}，
 * 這條測試盯的是它的判準：
 * <ul>
 *   <li>真的掉了一個顏色才記——每一句都記的話清單沒人看得完；</li>
 *   <li>譯者自己寫了色碼的不記；</li>
 *   <li>帶玩家名字的不記，數字換成佔位符（這份檔案是要交出去的）；</li>
 *   <li>修好之後再看到一次，那一句自己從清單上消失。</li>
 * </ul>
 */
public final class ColourAuditTest {

    private static int failures = 0;

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

        // 白色那一段沒貼上
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
        // 只有圖示與數字不同色的不算「兩種顏色」：那些靠佔位符自己帶顏色
        check("只有數字不同色的不記",
                ColourAudit.audit(src, dst, List.of(piece("Hold for ", GREY), piece("51", GOLD),
                        piece(" seconds", GREY)), List.of(piece("51", GOLD)),
                        new boolean[] {false}, GREY) == null);

        // 記下來、寫出去、讀回來
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
        // 修好＝譯文換了（這裡是加了一個字）。同一份譯文不會再看第二次，見 note 的說明
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

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
