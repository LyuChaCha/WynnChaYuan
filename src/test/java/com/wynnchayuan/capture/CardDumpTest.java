package com.wynnchayuan.capture;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class CardDumpTest {

    private static int failures = 0;

    private static final String[] GOLDEN = {
            "Bring [24 Fluffy Fur] to the",
            "Slaying Post [Combat Lv. 88] at",
            "[139, 61, -4399]",
    };

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
        caves(dir.resolve("caves.json"));
        perLine(dir.resolve("per-line.json"));
        repeats(dir.resolve("repeats.json"));
        privacy(dir.resolve("privacy.json"));
        reload(dir.resolve("reload.json"));
        offSwitch(dir.resolve("off.json"));

        report();
    }

    private static void golden(Path file) throws Exception {
        CardDump.forget();
        CardDump.forTest(file);

        List<Component> tooltip = card();
        List<StyledText> styled = styled(tooltip);
        String key = joinTemplates(styled.subList(3, 6));

        CardDump.note(tooltip, styled, key, null);
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

    private static void repeats(Path file) throws Exception {
        CardDump.forget();
        CardDump.forTest(file);

        List<Component> tooltip = card();
        List<StyledText> styled = styled(tooltip);
        String key = joinTemplates(styled.subList(3, 6));

        for (int frame = 0; frame < 120; frame++) {
            CardDump.note(tooltip, styled, key, null);
        }
        CardDump.flush();
        check("★ 同一份 tooltip 重複送不會重複記", CardDump.size() == 1);
        check("★ seen 是滑過幾次、不是幀數（實際："
                        + firstRow(file).get("seen").getAsInt() + "）",
                firstRow(file).get("seen").getAsInt() == 1);

        String second = joinTemplates(styled.subList(0, 2));
        CardDump.note(tooltip, styled, second, null);
        check("★ 同一份裡的另一段照樣收得到", CardDump.size() == 2);

        List<Component> other = List.of(plain("Something Else Entirely", GREY),
                                        plain("A second line here", GREY));
        CardDump.note(other, styled(other), joinTemplates(styled(other)), null);
        for (int frame = 0; frame < 30; frame++) {
            CardDump.note(tooltip, styled, key, null);
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

    private static void privacy(Path file) throws Exception {
        CardDump.forget();
        CardDump.forTest(file);

        List<Component> head = List.of(
                plain("PoorChaCha", WHITE),
                plain("Left-Click to view this member's", GREY),
                plain("contribution to the guild", GREY));
        CardDump.fromPlayerHead(true);
        CardDump.note(head, styled(head), joinTemplates(styled(head).subList(1, 3)), null);
        check("★ 玩家頭顱那一格整份不收", CardDump.size() == 0);
        CardDump.fromPlayerHead(false);

        List<Component> party = List.of(
                plain("Gikyu Boss Fight", WHITE),
                plain("World: NA12", GREY),
                plain("This party's world is currently", GREY),
                plain("full and cannot be joined", GREY));
        CardDump.note(party, styled(party), joinTemplates(styled(party).subList(2, 4)), null);
        check("★ 隊伍卡不收（PlayerDataFilter#isPartyCard）", CardDump.size() == 0);

        List<Component> member = List.of(
                plain("Guild Members", WHITE),
                plain("Left-Click to set rank", GREY),
                plain("Hyedam_", GREY));
        CardDump.note(member, styled(member),
                joinTemplates(styled(member).subList(1, 3)), null);
        check("★ 段落中間夾著帳號名的不收（逐行問，不是只問第一行）",
                CardDump.size() == 0);

        List<Component> joined = List.of(
                plain("Party", WHITE),
                plain("Someone has joined", GREY),
                plain("your party", GREY));
        CardDump.note(joined, styled(joined),
                joinTemplates(styled(joined).subList(1, 3)), null);
        check("★ 被折行切開的廣播片語也擋得到（整段一起比）",
                CardDump.size() == 0);

        List<Component> ok = card();
        CardDump.note(ok, styled(ok), joinTemplates(styled(ok).subList(3, 6)), null);
        check("一般的卡片照收（濾網沒有誤擋整類）", CardDump.size() == 1);

        CardDump.flush();
        JsonObject events = read(file).getAsJsonObject("_meta")
                .getAsJsonObject("events");
        check("擋掉幾筆有記在 _meta.events 裡（看得出是沒收到還是被擋掉）",
                events.has("blocked.playerHead") && events.has("blocked.partyCard")
                        && events.has("blocked.playerData"));
    }

    private static void reload(Path file) throws Exception {
        CardDump.forget();
        CardDump.forTest(file);
        List<Component> tooltip = card();
        List<StyledText> styled = styled(tooltip);
        CardDump.note(tooltip, styled, joinTemplates(styled.subList(3, 6)), null);
        CardDump.flush();

        String filled = Files.readString(file).replaceFirst(
                "\"dst\": \"\"", "\"dst\": \"帶 [{~} 蓬鬆毛皮] 到 [{~}, {~}, -{~}] 的獵殺站\"");
        Files.writeString(file, filled);

        CardDump.forget();
        CardDump.forTest(file);
        check("重開之後條目也還在", CardDump.size() == 1);

        List<Component> again = card();
        CardDump.note(again, styled(again), joinTemplates(styled(again).subList(3, 6)), null);
        CardDump.flush();
        check("★ 重開並重寫之後，上次填的 dst 還在",
                firstRow(file).get("dst").getAsString().startsWith("帶 ["));
        check("seen 接著上次數，不是歸零",
                firstRow(file).get("seen").getAsInt() == 2);
    }

    private static void offSwitch(Path file) throws Exception {
        CardDump.forget();
        List<Component> tooltip = card();
        List<StyledText> styled = styled(tooltip);
        CardDump.note(tooltip, styled, joinTemplates(styled.subList(3, 6)), null);
        CardDump.flush();
        check("★ 開關關著時整支空轉，連檔案都不會生出來",
                CardDump.size() == 0 && !Files.exists(file));
    }

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
        check("★ 抬頭那一段不收（標題與狀態各自走逐行那條路，"
                      + "整段收進來只是噪音，實機 861 條裡有 533 條是這種）",
              CardDump.paragraphKey(styled, 0) == null);

        for (int i : new int[] {3, 7, 12, 17}) {
            String key = CardDump.paragraphKey(styled, i);
            if (key != null) {
                CardDump.note(tooltip, styled, key, null);
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

    private static void perLine(Path file) throws Exception {
        CardDump.forget();
        CardDump.forTest(file);

        List<Component> stats = List.of(
                plain("✔À Combat Lv. Min: 50", GREY),
                plain("Distance: Medium (500 Blocks)", GREY),
                plain("Length: Short", GREY));
        Path corpus = Files.createTempDirectory("card-dump-corpus");
        Files.writeString(corpus.resolve("misc.json"), "{\"Length: Short\": \"長度：短\"}");
        TranslationStore store = new TranslationStore();
        store.loadAll(corpus);

        CardDump.note(stats, styled(stats), joinTemplates(styled(stats)), store);
        check("★ 只缺其中幾行的整段不收（缺的那一行走 captured.json）",
              CardDump.size() == 0);

        CardDump.note(stats, styled(stats), joinTemplates(styled(stats)), null);
        check("拿不到語料時照收（不能因為查不了就把東西弄丟）",
              CardDump.size() == 1);

        CardDump.forget();
        CardDump.forTest(file);
        List<Component> desc = List.of(
                plain("Walk across the tall bridges", GREY),
                plain("and find the knights within", GREY));
        CardDump.note(desc, styled(desc), joinTemplates(styled(desc)), store);
        check("一行都查不到的整段照收（那才是這個檔要的東西）",
              CardDump.size() == 1);

        CardDump.forget();
        CardDump.forTest(file);
        List<Component> stitched = List.of(
                plain("Bring [24 Malt String] or [24", GREY),
                plain("Malt Grains] to the Gathering", GREY));
        Path pieces = Files.createTempDirectory("card-dump-pieces");
        Files.writeString(pieces.resolve("misc.json"),
                "{\"Bring [{~} Malt String] or [{~}\": \"把 [{~} 麥芽線] 或 [{~}\","
                + "\"Malt Grains] to the Gathering\": \"麥芽穀粒] 交到採集站，\"}");
        TranslationStore fragments = new TranslationStore();
        fragments.loadAll(pieces);
        CardDump.note(stitched, styled(stitched), joinTemplates(styled(stitched)), fragments);
        check("★ 每一行都是逐行碎片的整段<b>要收</b>（那正是最該換掉的）",
              CardDump.size() == 1);
    }

    private static void caves(Path file) throws Exception {
        CardDump.forget();
        CardDump.forTest(file);

        List<Component> tooltip = caveCard();
        List<StyledText> styled = styled(tooltip);

        String desc = CardDump.paragraphKey(styled, 3);
        check("洞窟敘述接得出三行的鍵",
              desc != null && desc.split("\\R", -1).length == 3);
        if (desc != null) {
            CardDump.note(tooltip, styled, desc, null);
        }
        CardDump.flush();

        check("★ 洞窟卡的敘述收得到（標題那一行是「某某 [Cave]」，"
                      + "先前被當成公會清單整張擋掉）",
              Files.exists(file) && read(file).getAsJsonObject("entries").size() == 1);

        check("卡片類型不再算公會標籤",
              !PlayerDataFilter.carriesPlayerData("The Barracks [Cave]"));
        check("真正的公會清單那一列照樣擋得住",
              PlayerDataFilter.carriesPlayerData("- RabbitHouse [Maya]"));
    }

    private static List<Component> caveCard() {
        List<Component> t = new ArrayList<>();
        t.add(parts("The Barracks ", WHITE, "[Cave]", GREY));
        t.add(plain("Can be explored", GREY));
        t.add(plain(" ", GREY));
        t.add(plain("Walk across the tall bridges", GREY));
        t.add(plain("and find the knights within the", GREY));
        t.add(plain("Barracks at [-1641, 151, -964]", GREY));
        t.add(plain(" ", GREY));
        t.add(plain("✔À Recommended Combat Lv: 119", GREY));
        t.add(plain("Distance: Far (1000+ Blocks)", GREY));
        t.add(plain("Length: Long (4m)", GREY));
        t.add(plain("Difficulty: Hard", GREY));
        t.add(plain(" ", GREY));
        t.add(plain("Rewards:", GREY));
        t.add(plain("- +8000000 XP", GREY));
        t.add(plain("- +Various Items", GREY));
        t.add(plain(" ", GREY));
        t.add(plain("Click To Track", GREY));
        t.add(plain("", GREY));
        t.add(plain("中鍵點擊在地圖上查看！", GREY));
        t.add(plain("右鍵點擊在維基上打開！", GREY));
        return List.copyOf(t);
    }

    private static List<Component> wholeCard() {
        List<Component> t = new ArrayList<>();
        t.add(plain("Slay Slimes [Mini-Quest]", WHITE));
        t.add(plain("Currently in progress", GREY));
        t.add(plain("", GREY));
        t.add(parts("Bring ", GREY, "[24 Fluffy Fur]", AQUA, " to the", GREY));
        t.add(parts("Slaying Post ", GREY, "[Combat Lv. 88]", AQUA, " at", GREY));
        t.add(plain(GOLDEN[2], WHITE));
        t.add(plain("", GREY));
        t.add(plain("Combat Lv. Min: 50", GREY));
        t.add(plain("Distance: Far (1000+ Blocks)", GREY));
        t.add(plain("Length: Short", GREY));
        t.add(plain("Difficulty: Easy", GREY));
        t.add(plain("", GREY));
        t.add(plain("Rewards:", GREY));
        t.add(plain("- +32400 XP", GREY));
        t.add(plain("", GREY));
        t.add(plain("Click To Track", GREY));
        t.add(plain("", GREY));
        t.add(plain("中鍵點擊在地圖上查看！", GREY));
        t.add(plain("右鍵點擊在維基上打開！", GREY));
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
