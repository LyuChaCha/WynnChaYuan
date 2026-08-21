package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 置中判斷的中間值。
 *
 * <h2>為什麼要自己一個檔</h2>
 * 這段輸出先前是掛在 {@link LineDebug} 上的，而<b>連續三次</b>回報回來的檔案裡
 * 一筆都沒有——共用緩衝區、共用去重集合、共用額度、還綁著收集開關，
 * 任何一環出問題都會讓它靜默消失，而且從外面看不出是哪一環。
 *
 * <p>所以拆開：自己的檔案、自己的額度、自己的去重、不看任何開關，
 * 而且整段包在 try 裡——診斷寫不出來絕不能反過來弄壞畫面。
 *
 * <p>寫的是「未鑑定物品的說明有沒有被判成置中」這件事。判斷失敗時畫面上
 * 只看得到「沒有跟著置中」，看不出是分段切錯、寬度量錯、還是算式差了幾像素——
 * 三種的修法完全不同。
 */
public final class LayoutDebug {

    /** 最多寫幾份 tooltip。夠看出問題，又不會把玩家的資料夾塞爆。 */
    private static final int LIMIT = 12;

    private static Path file;
    private static int written = 0;
    private static final StringBuilder buffer = new StringBuilder();
    private static final Set<String> seen = new HashSet<>();

    private LayoutDebug() {}

    public static void init(Path path) {
        file = path;
        buffer.setLength(0);
        buffer.append("# 置中判斷。每一行的縮排、內容寬度，以及置中時該有的縮排。")
              .append(System.lineSeparator())
              .append("# 三個數字對得起來卻判成靠左，就是分段切錯了。")
              .append(System.lineSeparator()).append(System.lineSeparator());
    }

    /** 一份 tooltip 的判斷結果。同一份只寫一次。 */
    public static void record(List<Component> lines, boolean[] centered) {
        try {
            if (file == null || written >= LIMIT || lines.isEmpty()) {
                return;
            }
            String key = lines.get(0).getString() + "/" + lines.size();
            if (!seen.add(key)) {
                return;
            }
            written++;
            buffer.append("=== ").append(written).append(" · ")
                  .append(oneLine(lines.get(0).getString()))
                  .append(" ===").append(System.lineSeparator())
                  .append(BlockLayout.explain(lines, centered))
                  .append(System.lineSeparator());
            String block = buffer.substring(buffer.length() - 1
                    - BlockLayout.explain(lines, centered).length());
            // 同時印到遊戲紀錄。檔案寫得出來與否受權限、路徑、防毒影響，
            // 而 latest.log 一定在——先前連續三次回報回來都是空的。
            System.out.println("[WynnChaYuan] 版面判斷" + System.lineSeparator() + block);
            Files.writeString(file, buffer.toString(), StandardCharsets.UTF_8);
        } catch (Throwable t) {
            // 診斷寫不出來就算了，絕不能反過來弄壞畫面
            file = null;
        }
    }

    private static String oneLine(String text) {
        String flat = text.replace('\n', ' ').strip();
        return flat.length() > 30 ? flat.substring(0, 30) + "…" : flat;
    }
}
