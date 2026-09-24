package com.wynnchayuan.capture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code cards.json} 收到的是不是<b>模組實際查表用的那個鍵</b>。
 *
 * <h2>為什麼這一份非測不可</h2>
 * 這個檔的全部價值就在「鍵跟查表端一模一樣」。差一個空白、少一個負號，
 * 譯者照著翻出來的條目<b>永遠不會被查到</b>——而畫面上看起來跟「還沒翻」
 * 一模一樣，沒有人查得出來是哪裡錯了。
 *
 * <p>所以黃金樣本直接取自實機的 {@code majorid-debug.txt}（「攤平後的鍵」那幾行），
 * 不是自己編的。
 */
public final class CardDumpTest {

    private static int failures = 0;

    /** 實機那張迷你任務卡上，被 tooltip 寬度斷成三行的那一段。 */
    private static final String[] GOLDEN = {
            "Bring [24 Fluffy Fur] to the",
            "Slaying Post [Combat Lv. 88] at",
            "[139, 61, -4399]",
    };

    /**
     * 模組接行用的是<b>一個半形空白</b>（見 {@code LineTranslator#rejoin}）。
     * 座標的正負號是鍵的一部分：{@code [-{~}, {~}, -{~}]} 是另一條。
     */
    private static final String GOLDEN_KEY =
            "Bring [{~} Fluffy Fur] to the Slaying Post [Combat Lv. {~}] at "
            + "[{~}, {~}, -{~}]";

    private static final int GREY = 0xAAAAAA;
    private static final int AQUA = 0x00AAAA;
    private static final int WHITE = 0xFFFFFF;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-cards");

        golden(dir.resolve("golden.json"));
        paragraphs(dir.resolve("paragraphs.json"));
        repeats(dir.resolve("repeats.json"));
        privacy(dir.resolve("privacy.json"));
        reload(dir.resolve("reload.json"));
        offSwitch(dir.resolve("off.json"));

        report();
    }

    // ---------------------------------------------------------------- 黃金樣本

    private static void golden(Path file) throws Exception {
        CardDump.forget();
        CardDump.forTest(file);

        List<Component> tooltip = card();
        List<StyledText> styled = styled(tooltip);
        // 跟 TooltipPanel 完全一樣的接法：每一行的模板用行分隔符接起來。
        String key = joinTemplates(styled.subList(3, 6));

        CardDump.note(tooltip, styled, key);
        CardDump.flush();

        JsonObject row = firstRow(file);
        check("★ 攤平後的鍵跟實機一致（實際：" + row.get("src").getAsString() + "）",
                GOLDEN_KEY.equals(row.get("src").getAsString()));
        check("★ 座標的負號留住了（[-{~}… 與 [{~}… 是兩條不同的鍵）",
                row.get("src").getAsString().endsWith("[{~}, {~}, -{~}]"));
        check("接行用的是一個半形空白，不是換行也不是兩個空白",
                !row.get("src").getAsString().contains("  ")
                        && !row.get("src").getAsString().contains("\n"));

        check("dst 留空，填完可以直接併進語料",
                row.has("dst") && row.get("dst").getAsString().isEmpty());
        check("★ 帶著這一段屬於哪一張卡",
                "Slay Slimes [Mini-Quest]".equals(row.get("card").getAsString()));

        var lines = row.getAsJsonArray("lines");
        check("★ 另外保留沒攤平前的逐行原樣（看得出斷行斷在哪）",
                lines.size() == 3
                        && "Bring [{~} Fluffy Fur] to the".equals(lines.get(0).getAsString())
                        && "[{~}, {~}, -{~}]".equals(lines.get(2).getAsString()));
        check("逐行那一份是模板，不是原始色碼",
                !lines.get(0).getAsString().contains("§"));

        JsonObject root = read(file);
        check("有 _meta（照 captured.json 的風格）", root.has("_meta"));
        check("有 _note，用中文說明怎麼用",
                root.has("_note") && root.get("_note").getAsString().contains("攤平"));
        check("_note 有講清楚不要照 lines 逐行翻",
                root.get("_note").getAsString().contains("逐行"));
        check("seen 從 1 開始", row.get("seen").getAsInt() == 1);
    }

    // ---------------------------------------------------------- 去重與 seen 計數

    private static void repeats(Path file) throws Exception {
        CardDump.forget();
        CardDump.forTest(file);

        List<Component> tooltip = card();
        List<StyledText> styled = styled(tooltip);
        String key = joinTemplates(styled.subList(3, 6));

        // tooltip 是每一幀重畫的：滑鼠停兩秒就是一百多次同樣的呼叫。
        for (int frame = 0; frame < 120; frame++) {
            CardDump.note(tooltip, styled, key);
        }
        CardDump.flush();
        check("★ 同一份 tooltip 重複送不會重複記", CardDump.size() == 1);
        check("★ seen 是滑過幾次、不是幀數（實際："
                        + firstRow(file).get("seen").getAsInt() + "）",
                firstRow(file).get("seen").getAsInt() == 1);

        // 同一份 tooltip 裡的<b>另一段</b>照樣要進得來——只比「是不是同一份」
        // 的話，第二段會被第一段擋掉。
        String second = joinTemplates(styled.subList(0, 2));
        CardDump.note(tooltip, styled, second);
        check("★ 同一份裡的另一段照樣收得到", CardDump.size() == 2);

        // 滑走、再滑回來：這才算第二次。
        List<Component> other = List.of(plain("Something Else Entirely", GREY),
                                        plain("A second line here", GREY));
        CardDump.note(other, styled(other), joinTemplates(styled(other)));
        for (int frame = 0; frame < 30; frame++) {
            CardDump.note(tooltip, styled, key);
        }
        CardDump.flush();

        check("★ 滑走再滑回來才算第二次（實際："
                        + rowFor(file, GOLDEN_KEY).get("seen").getAsInt() + "）",
                rowFor(file, GOLDEN_KEY).get("seen").getAsInt() == 2);
        check("去重之後還是一條，不是兩條",
                read(file).getAsJsonObject("entries").entrySet().stream()
                        .filter(e -> GOLDEN_KEY.equals(
                                e.getValue().getAsJsonObject().get("src").getAsString()))
                        .count() == 1);
    }

    // -------------------------------------------------------------------- 隱私

    private static void privacy(Path file) throws Exception {
        CardDump.forget();
        CardDump.forTest(file);

        // 玩家頭顱：標題<b>一定</b>是帳號名，而我們每一筆都帶著標題，
        // 丟不掉，所以整份不收。判斷由 GuiTextCapture 交過來。
        List<Component> head = List.of(
                plain("PoorChaCha", WHITE),
                plain("Left-Click to view this member's", GREY),
                plain("contribution to the guild", GREY));
        CardDump.fromPlayerHead(true);
        CardDump.note(head, styled(head), joinTemplates(styled(head).subList(1, 3)));
        check("★ 玩家頭顱那一格整份不收", CardDump.size() == 0);
        CardDump.fromPlayerHead(false);

        // 隊伍卡：隊伍名是玩家自己打的字，沒有形狀可以認——
        // 但「卡上有一行 World: NA{~}」是整份才看得出來的事實。
        List<Component> party = List.of(
                plain("Gikyu Boss Fight", WHITE),
                plain("World: NA12", GREY),
                plain("This party's world is currently", GREY),
                plain("full and cannot be joined", GREY));
        CardDump.note(party, styled(party), joinTemplates(styled(party).subList(2, 4)));
        check("★ 隊伍卡不收（PlayerDataFilter#isPartyCard）", CardDump.size() == 0);

        // 帳號名長相（底線、駝峰）落在<b>段落的第二行</b>。
        //
        // 這個位置是故意的：PlayerDataFilter#looksAccountNamed 只看它拿到的
        // <b>第一行</b>，整段丟進去問等於只問了第一行。所以要逐行問——
        // 少了那個迴圈，這一條就會穿過去。
        List<Component> member = List.of(
                plain("Guild Members", WHITE),
                plain("Left-Click to set rank", GREY),
                plain("Hyedam_", GREY));
        CardDump.note(member, styled(member),
                joinTemplates(styled(member).subList(1, 3)));
        check("★ 段落中間夾著帳號名的不收（逐行問，不是只問第一行）",
                CardDump.size() == 0);

        // 跨行的片語：整段比才比得到（聊天廣播會被折行）。
        List<Component> joined = List.of(
                plain("Party", WHITE),
                plain("Someone has joined", GREY),
                plain("your party", GREY));
        CardDump.note(joined, styled(joined),
                joinTemplates(styled(joined).subList(1, 3)));
        check("★ 被折行切開的廣播片語也擋得到（整段一起比）",
                CardDump.size() == 0);

        // 正面對照：一般的卡片照收，不能把濾網做成全部擋掉。
        List<Component> ok = card();
        CardDump.note(ok, styled(ok), joinTemplates(styled(ok).subList(3, 6)));
        check("一般的卡片照收（濾網沒有誤擋整類）", CardDump.size() == 1);

        CardDump.flush();
        JsonObject events = read(file).getAsJsonObject("_meta")
                .getAsJsonObject("events");
        check("擋掉幾筆有記在 _meta.events 裡（看得出是沒收到還是被擋掉）",
                events.has("blocked.playerHead") && events.has("blocked.partyCard")
                        && events.has("blocked.playerData"));
    }

    // ------------------------------------------------------------ 重開不掉資料

    private static void reload(Path file) throws Exception {
        CardDump.forget();
        CardDump.forTest(file);
        List<Component> tooltip = card();
        List<StyledText> styled = styled(tooltip);
        CardDump.note(tooltip, styled, joinTemplates(styled.subList(3, 6)));
        CardDump.flush();

        // 譯者在這個檔上填了字。重開遊戲就被蓋掉的話，沒有人敢用它。
        String filled = Files.readString(file).replaceFirst(
                "\"dst\": \"\"", "\"dst\": \"帶 [{~} 蓬鬆毛皮] 到 [{~}, {~}, -{~}] 的獵殺站\"");
        Files.writeString(file, filled);

        CardDump.forget();
        CardDump.forTest(file);                // 等同重開遊戲
        check("重開之後條目也還在", CardDump.size() == 1);

        // 再滑一次，整個檔會被重寫——上次填的字要是沒讀回來，這時候就被空字串蓋掉了。
        List<Component> again = card();
        CardDump.note(again, styled(again), joinTemplates(styled(again).subList(3, 6)));
        CardDump.flush();
        check("★ 重開並重寫之後，上次填的 dst 還在",
                firstRow(file).get("dst").getAsString().startsWith("帶 ["));
        check("seen 接著上次數，不是歸零",
                firstRow(file).get("seen").getAsInt() == 2);
    }

    // ------------------------------------------------------------------ 開關

    private static void offSwitch(Path file) throws Exception {
        CardDump.forget();                     // 沒 init 過 = 開關關著
        List<Component> tooltip = card();
        List<StyledText> styled = styled(tooltip);
        CardDump.note(tooltip, styled, joinTemplates(styled.subList(3, 6)));
        CardDump.flush();
        check("★ 開關關著時整支空轉，連檔案都不會生出來",
                CardDump.size() == 0 && !Files.exists(file));
    }

    // ------------------------------------------------------------------ 工具

    /** 實機那張迷你任務卡：標題、狀態、空行，然後被斷成三行的敘述。 */
    // ------------------------------------------------ 收的是段落，不是 15 行的窗格

    /**
     * 整張卡餵進去，確認每一段各自成一條、沒有橫跨空行。
     *
     * <h2>這一段在擋什麼</h2>
     * {@code TooltipPanel} 丟給 {@code noteBlockMiss} 的是「從最長試到兩行」的
     * 診斷窗格，長度 {@code min(maxBlockLines, 剩下幾行)}；實測 maxBlockLines
     * 是 15，所以從敘述起點（第 3 行）算起會一路吃到第 17～18 行——Wynntils
     * 自己加的中文提示。那種鍵含中文會被隱私那關整段擋掉，擋掉的正是敘述本身。
     *
     * <p>所以 {@code cards.json} 自己算段落。這一份用<b>整張卡</b>當輸入，
     * 只餵三行的話這整類 bug 一個都測不到。
     */
    private static void paragraphs(Path file) throws Exception {
        CardDump.forget();
        CardDump.forTest(file);

        List<Component> tooltip = wholeCard();
        List<StyledText> styled = styled(tooltip);

        String desc = CardDump.paragraphKey(styled, 3);
        check("★ 敘述收的是那三行，不是 15 行的窗格（實際 "
                      + (desc == null ? 0 : desc.split("\\R", -1).length) + " 行）",
              desc != null && desc.split("\\R", -1).length == 3);
        check("★ 敘述那一條沒有把需求那一段黏進來",
              desc != null && !desc.contains("Combat Lv. Min"));
        check("★ 敘述那一條沒有把 Wynntils 的中文提示黏進來",
              desc != null && !desc.contains("中鍵點擊"));
        check("接好的敘述跟黃金樣本一致",
              desc != null && desc.equals(joinTemplates(styled.subList(3, 6))));

        String stats = CardDump.paragraphKey(styled, 7);
        check("★ 需求那一段自己成一條（實際 "
                      + (stats == null ? 0 : stats.split("\\R", -1).length) + " 行）",
              stats != null && stats.split("\\R", -1).length == 4);

        String rewards = CardDump.paragraphKey(styled, 12);
        check("★ 獎勵那一段自己成一條",
              rewards != null && rewards.split("\\R", -1).length == 2);

        check("★ 一行的段落不收（交給 captured.json，這裡分不出它翻到了沒）",
              CardDump.paragraphKey(styled, 15) == null);
        check("空行本身不是段落起點", CardDump.paragraphKey(styled, 2) == null);

        // Wynntils 自己的中文提示自成一段——收不進去，但不能連累別段。
        for (int i : new int[] {3, 7, 12, 17}) {
            String key = CardDump.paragraphKey(styled, i);
            if (key != null) {
                CardDump.note(tooltip, styled, key);
            }
        }
        CardDump.flush();

        JsonObject rows = read(file).getAsJsonObject("entries");
        check("★ 中文那一段被擋掉，其餘三段照收（實際 " + rows.size() + " 條）",
              rows.size() == 3);
        for (String k : rows.keySet()) {
            String src = rows.getAsJsonObject(k).get("src").getAsString();
            check("沒有任何一條橫跨空行（" + k + "）", !src.contains("  "));
        }
    }

    /** 實機那張 19 行的迷你任務卡，照 tooltip-partial-5 的結構。 */
    private static List<Component> wholeCard() {
        List<Component> t = new ArrayList<>();
        t.add(plain("Slay Slimes [Mini-Quest]", WHITE));      // 0
        t.add(plain("Currently in progress", GREY));          // 1
        t.add(plain("", GREY));                               // 2
        t.add(parts("Bring ", GREY, "[24 Fluffy Fur]", AQUA, " to the", GREY));
        t.add(parts("Slaying Post ", GREY, "[Combat Lv. 88]", AQUA, " at", GREY));
        t.add(plain(GOLDEN[2], WHITE));                       // 5
        t.add(plain("", GREY));                               // 6
        t.add(plain("Combat Lv. Min: 50", GREY));             // 7
        t.add(plain("Distance: Far (1000+ Blocks)", GREY));   // 8
        t.add(plain("Length: Short", GREY));                  // 9
        t.add(plain("Difficulty: Easy", GREY));               // 10
        t.add(plain("", GREY));                               // 11
        t.add(plain("Rewards:", GREY));                       // 12
        t.add(plain("- +32400 XP", GREY));                    // 13
        t.add(plain("", GREY));                               // 14
        t.add(plain("Click To Track", GREY));                 // 15 單行段落
        t.add(plain("", GREY));                               // 16
        t.add(plain("中鍵點擊在地圖上查看！", GREY));            // 17 Wynntils 自己加的
        t.add(plain("右鍵點擊在維基上打開！", GREY));            // 18
        return List.copyOf(t);
    }

    private static List<Component> card() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(plain("Slay Slimes [Mini-Quest]", WHITE));
        tooltip.add(plain("Currently in progress", GREY));
        tooltip.add(plain("", GREY));
        tooltip.add(parts("Bring ", GREY, "[24 Fluffy Fur]", AQUA, " to the", GREY));
        tooltip.add(parts("Slaying Post ", GREY, "[Combat Lv. 88]", AQUA, " at", GREY));
        tooltip.add(plain(GOLDEN[2], WHITE));
        return List.copyOf(tooltip);
    }

    /**
     * 跟 {@code TooltipPanel} 完全一樣的接法。
     *
     * <p>刻意在這裡再寫一次而不是呼叫它：這一份測的就是「兩邊算出來的鍵一樣」，
     * 直接呼叫同一支的話，兩邊一起錯了也測不出來。
     */
    private static String joinTemplates(List<StyledText> run) {
        StringBuilder key = new StringBuilder();
        for (int k = 0; k < run.size(); k++) {
            if (k > 0) {
                key.append(System.lineSeparator());
            }
            key.append(LineParts.of(run.get(k)).template());
        }
        return key.toString();
    }

    private static List<StyledText> styled(List<Component> tooltip) {
        List<StyledText> out = new ArrayList<>(tooltip.size());
        for (Component line : tooltip) {
            out.add(StyledText.fromComponent(line));
        }
        return out;
    }

    private static MutableComponent plain(String text, int colour) {
        return Component.literal(text)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }

    /** 一行裡有好幾個顏色，就像實機送過來的樣子。 */
    private static MutableComponent parts(Object... textThenColour) {
        MutableComponent out = plain((String) textThenColour[0],
                                     (Integer) textThenColour[1]);
        for (int i = 2; i < textThenColour.length; i += 2) {
            out.append(plain((String) textThenColour[i],
                             (Integer) textThenColour[i + 1]));
        }
        return out;
    }

    private static JsonObject read(Path file) throws Exception {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    private static JsonObject firstRow(Path file) throws Exception {
        JsonObject rows = read(file).getAsJsonObject("entries");
        return rows.entrySet().iterator().next().getValue().getAsJsonObject();
    }

    private static JsonObject rowFor(Path file, String src) throws Exception {
        JsonObject rows = read(file).getAsJsonObject("entries");
        for (var e : rows.entrySet()) {
            JsonObject row = e.getValue().getAsJsonObject();
            if (src.equals(row.get("src").getAsString())) {
                return row;
            }
        }
        throw new IllegalStateException("找不到 " + src);
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    private static void report() {
        System.out.println(failures == 0
                ? "CardDump: 全部通過"
                : "CardDump: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
