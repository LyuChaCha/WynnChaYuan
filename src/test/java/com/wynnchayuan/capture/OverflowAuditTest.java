package com.wynnchayuan.capture;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * 「譯文比原本那一格大」的句子有沒有被記下來、記得對不對。
 *
 * <h2>為什麼要有</h2>
 * issue #864：譯文比原文長時，對話框打完字就退回英文。後來改成由小框接手，
 * 畫面不壞了——但也沒有人知道是哪幾句。{@link OverflowAudit} 把它們寫進
 * {@code captured.json}，這條測試盯的是它的判準：
 * <ul>
 *   <li>真的塞不下才記；塞得下的不記；</li>
 *   <li>物品說明寬一兩個像素不算（字型進位）；</li>
 *   <li>帶玩家資料的不記，數字換成佔位符（這份檔案是要交出去的）；</li>
 *   <li>帶 {@code {u}} 的台詞照收——那是佔位符不是名字；</li>
 *   <li>同一句只記一次；譯文改短之後再看到一次，那一句自己消失。</li>
 * </ul>
 */
public final class OverflowAuditTest {

    private static int failures = 0;

    private static final String D = OverflowAudit.DIALOGUE;
    private static final String T = OverflowAudit.TOOLTIP;
    private static final String ROWS = OverflowAudit.ROWS_UNIT;
    private static final String PX = OverflowAudit.PX_UNIT;

    public static void main(String[] args) {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        String src = "Did you buy the supply crate yet?";
        String dst = "¿Has comprado ya la caja de suministros que te pedí hace un rato?";

        // ---- 判準 ----
        OverflowAudit.Row row = OverflowAudit.audit(D, src, dst, 1, 2, ROWS);
        check("★ 要兩列、只有一列：記下來", row != null);
        if (row != null) {
            check("記的是哪個介面、容得下多少、要多少",
                    D.equals(row.where()) && row.room() == 1 && row.need() == 2
                            && ROWS.equals(row.unit()));
            check("原文與譯文都在", src.equals(row.src()) && dst.equals(row.dst()));
        }
        check("塞得下的不記", OverflowAudit.audit(D, src, dst, 2, 2, ROWS) == null);
        check("比需要的還寬裕的不記", OverflowAudit.audit(D, src, dst, 3, 2, ROWS) == null);
        check("沒有譯文的不記", OverflowAudit.audit(D, src, "  ", 1, 2, ROWS) == null);
        check("量不到（room 是 0）的不記", OverflowAudit.audit(T, src, dst, 0, 40, PX) == null);

        check("★ 物品說明寬 31 個像素：記下來",
                OverflowAudit.audit(T, "Health Regen", "Regeneración de salud", 165, 196, PX) != null);
        check("寬兩個像素是字型進位，不記",
                OverflowAudit.audit(T, "Health Regen", "Regeneración", 165, 167, PX) == null);

        // ---- 這份檔案是要交出去的 ----
        OverflowAudit.Row numbered = OverflowAudit.audit(D,
                "You need to be level 54 at least.", "Necesitas ser al menos de nivel 54.", 1, 2, ROWS);
        check("★ 數字換成佔位符（實際 " + (numbered == null ? null : numbered.src()) + "）",
                numbered != null && numbered.src().contains("{~}") && !numbered.src().contains("54")
                        && !numbered.dst().contains("54"));
        OverflowAudit.Row named = OverflowAudit.audit(D,
                "{u}, we're making progress!", "¡{u}, estamos avanzando muchísimo por aquí!", 1, 2, ROWS);
        check("★ 帶 {u} 的台詞照收——那是佔位符，不是名字", named != null);
        check("★ 中文譯文照收（濾網只問原文，不問譯文）",
                OverflowAudit.audit(D, src, "補給箱你到底買了沒有啊？我已經等了你好久好久了。", 1, 2, ROWS)
                        != null);
        // 濾網認得出來的就整條不收；判準跟 capture 其他部分同一支，這裡只盯「有沒有問它」。
        for (String line : new String[] {"Steve has joined your party!",
                "[Party] Steve: over here", "You have been invited to join Steve's party!"}) {
            boolean flagged = PlayerDataFilter.carriesPlayerData(line);
            check("帶玩家資料的句子照濾網的判斷處理（" + line + "：" + flagged + "）",
                    (OverflowAudit.audit(D, line, "譯文譯文譯文", 1, 2, ROWS) == null) == flagged);
        }

        // ---- 記、去重、自己消失 ----
        OverflowAudit.clear();
        OverflowAudit.takeDirty();
        OverflowAudit.note(D, src, dst, 1, 1, ROWS);
        check("清單是空的時候，塞得下的那種呼叫什麼都不做",
                OverflowAudit.size() == 0 && !OverflowAudit.takeDirty());
        OverflowAudit.note(D, src, dst, 1, 2, ROWS);
        check("★ 塞不下：進清單", OverflowAudit.size() == 1 && OverflowAudit.takeDirty());
        OverflowAudit.note(D, src, dst, 1, 2, ROWS);
        check("同一句再來一次：不重複、也不算有變動",
                OverflowAudit.size() == 1 && !OverflowAudit.takeDirty());
        OverflowAudit.note(T, src, dst, 100, 140, PX);
        check("同一句在另一個介面是另一條", OverflowAudit.size() == 2);

        // 存檔再讀回來
        JsonArray saved = OverflowAudit.toJson();
        JsonObject first = saved.get(0).getAsJsonObject();
        check("存出去的欄位齊全", first.has("where") && first.has("src") && first.has("dst")
                && first.has("room") && first.has("need") && first.has("unit"));
        OverflowAudit.clear();
        OverflowAudit.load(saved);
        check("讀回來一條不少（實際 " + OverflowAudit.size() + "）", OverflowAudit.size() == 2);
        OverflowAudit.load(new com.google.gson.JsonPrimitive("壞掉的"));
        check("格式不對的不會弄壞已經有的", OverflowAudit.size() == 2);

        // 譯文改短了
        OverflowAudit.takeDirty();
        OverflowAudit.note(D, src, "¿Ya compraste la caja?", 1, 1, ROWS);
        check("★ 改短之後再看到一次，那一句自己消失",
                OverflowAudit.size() == 1 && OverflowAudit.takeDirty());

        // ---- 要幾列：從現有的列數往上試 ----
        String longLine = "這是一段很長很長的譯文，長到原本那一列絕對放不下，"
                + "所以要算出它到底需要幾列才塞得進對話框裡，再把這個數字記下來。";
        int need = com.wynnchayuan.render.DialogueRewriterProbe.rowsNeeded(longLine, 1);
        check("★ 一列放不下的長句，算得出要幾列（實際 " + need + "）", need > 1);
        check("算出來的列數真的塞得下、少一列就塞不下",
                com.wynnchayuan.render.DialogueRewriterProbe.fits(longLine, need)
                        && !com.wynnchayuan.render.DialogueRewriterProbe.fits(longLine, need - 1));

        OverflowAudit.clear();
        if (failures > 0) {
            System.err.println(failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("OverflowAuditTest 全部通過");
        System.exit(0);
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) {
            failures++;
        }
    }
}
