package com.wynnchayuan.listener;

import com.wynnchayuan.translate.FlowedDebug;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.SpaceOffset;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 就地取代模式的多欄面板：整塊攔下來，等它跳完再一起翻。
 *
 * <h2>實機回報（2026-10-10）</h2>
 * 「又遇到老問題，字不會置中對齊 (lootrun)」。信標面板是一列一則訊息送來的，
 * 就地取代一列換一列，每一列只看得到自己：
 *
 * <pre>
 *   紅色信標
 *   +6 場挑戰以            ← 四列各自查到半句，語序是英文的
 *   本次 Lootrun。獲得
 *   完成它們不會           ← 單欄的接續列原地不動，跟上面兩列錯開
 *   獲得時間獎勵。
 * </pre>
 *
 * 原文加譯文那個模式沒有這個問題——它本來就等整塊跳完才算。所以就地取代從
 * 第一個多欄的列開始，把後面的伺服器訊息攔下來走同一條路（{@link ChatBlock#hold}）。
 *
 * <h2>這裡釘住什麼</h2>
 * <ol>
 *   <li>多欄的列認得出來（從這一列開始攔），單欄的標題列不算；</li>
 *   <li>攔下來的一整塊：同一欄折成幾列的敘述併成整句翻、切回原本的列數；</li>
 *   <li>★ 攔下來的東西<b>一列都不能少</b>——查不到譯文的列照原文送回去，
 *       整塊一列都沒翻到也要送（原文已經被攔掉了，不送就憑空消失）；</li>
 *   <li>沒被攔的（原文加譯文模式）照舊：一列都沒翻到就不送，不然每則訊息出現兩遍。</li>
 * </ol>
 *
 * <p>欄位的實際位移要量字寬，測試環境沒有字型，這裡釘不到；能釘的是內容與列數。
 */
public final class ChatHoldTest {

    private static int failures = 0;

    private static Component offset(int px) {
        return Component.literal(SpaceOffset.encode(px))
                .setStyle(SpaceOffset.styleFor(Style.EMPTY));
    }

    private static StyledText two(int a, String left, int b, String right) {
        MutableComponent out = Component.empty();
        out.append(offset(a)).append(Component.literal(left))
           .append(offset(b)).append(Component.literal(right));
        return StyledText.fromComponent(out);
    }

    private static StyledText one(int a, String text) {
        MutableComponent out = Component.empty();
        out.append(offset(a)).append(Component.literal(text));
        return StyledText.fromComponent(out);
    }

    private static StyledText blank() {
        return StyledText.fromComponent(Component.literal(""));
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-hold");
        FlowedDebug.init(dir);
        com.google.gson.JsonObject root = new com.google.gson.JsonObject();
        root.addProperty("{#}Purple Beacon", "{#}紫標");
        root.addProperty("{#}Red Beacon", "{#}紅標");
        root.addProperty("{#}Pink Beacon", "{#}粉標");
        root.addProperty("{#}Yellow Beacon", "{#}黃標");
        root.addProperty("{#}+{~} Beacon Reroll", "{#}重抽 +{~}");
        root.addProperty("{#}Spawn {~} Flying Chest", "{#}飛箱 {~} 個");
        root.addProperty("{#}Click here to reroll", "{#}點這裡重抽");
        root.addProperty("{#}({~} rerolls left)", "{#}(還剩 {~} 次)");
        // 整句：鍵是同一欄的幾格用換行接起來，譯文一列對一格
        root.addProperty("+{~} Curses, +{~} End\nReward Pulls", "詛咒 +{~}，\n結算 +{~}");
        root.addProperty("+{~} Challenges to\nthis Lootrun. Gain\nno Time Bonus for\ncompleting them.",
                         "這一趟\n多 {~} 場挑戰。\n打完它們\n沒有時間加成。");
        // 只有半句的舊條目也在：整句有的時候不能輪到它們
        root.addProperty("{#}+{~} Challenges to", "{#}+{~} 場挑戰以");
        root.addProperty("{#}this Lootrun. Gain", "{#}本次 Lootrun。獲得");
        root.addProperty("{#}no Time Bonus for", "{#}完成它們不會");
        root.addProperty("{#}completing them.", "{#}獲得時間獎勵。");
        Files.writeString(dir.resolve("lootrun.json"), root.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        List<StyledText> panel = List.of(
                two(33, "Purple Beacon", 69, "Red Beacon"),
                two(28, "+2 Curses, +2 End", 60, "+6 Challenges to"),
                two(40, "Reward Pulls", 80, "this Lootrun. Gain"),
                one(180, "no Time Bonus for"),
                one(186, "completing them."),
                blank(),
                two(36, "Pink Beacon", 72, "Yellow Beacon"),
                two(30, "+2 Beacon Reroll", 58, "Spawn 2 Flying Chest"),
                blank(),
                one(105, "Click here to reroll"),
                one(116, "(2 rerolls left)"),
                one(90, "Totally Unknown Row"));

        check("多欄的列認得出來（從這一列開始攔）", LineTranslator.panelRow(panel.get(0)));
        check("單欄的標題列不算", !LineTranslator.panelRow(one(102, "Choose a Beacon!")));
        // ★ 一般訊息裡圖示與字之間一兩個像素的字距微調不是欄界
        check("★ 字距微調不算兩欄", !LineTranslator.panelRow(two(40, "Some icon", 2, "and its text")));
        check("空行不算", !LineTranslator.panelRow(blank()));

        ChatBlock.clear();
        check("一開始沒有在攔", !ChatBlock.holding());
        for (StyledText row : panel) {
            ChatBlock.hold(row, false);
        }
        check("攔了之後後面的列也要跟著攔", ChatBlock.holding());
        check("攔了幾列就攢幾列（實際 " + ChatBlock.size() + "）", ChatBlock.size() == panel.size());

        Component made = ChatBlock.preview(store);
        check("攔下來的那一塊送得出來", made != null);
        if (made != null) {
            String[] rows = made.getString().split("\n", -1);
            for (String row : rows) {
                System.out.println("      | " + visible(row));
            }
            check("★ 一列都沒有少（實際 " + rows.length + "）", rows.length == panel.size());
            String all = made.getString();
            check("信標名稱翻了", all.contains("紫標") && all.contains("紅標")
                    && all.contains("粉標") && all.contains("黃標"));
            check("★ 右欄四列併成整句（不是四個半句）",
                  visible(rows[1]).endsWith("這一趟") && visible(rows[2]).endsWith("多 6 場挑戰。")
                          && visible(rows[3]).equals("打完它們") && visible(rows[4]).equals("沒有時間加成。"));
            check("半句的舊條目沒有被用到", !all.contains("場挑戰以") && !all.contains("Lootrun。獲得"));
            check("左欄兩列併成整句", visible(rows[1]).startsWith("詛咒 +2，")
                    && visible(rows[2]).startsWith("結算 +2"));
            check("單格的列照舊", all.contains("重抽 +2") && all.contains("飛箱 2 個")
                    && all.contains("點這裡重抽") && all.contains("(還剩 2 次)"));
            check("★ 查不到譯文的列照原文送回去", all.contains("Totally Unknown Row"));
            check("空行還在", visible(rows[5]).isEmpty() && visible(rows[8]).isEmpty());
        }

        // ★ 整塊一列都沒翻到：攔下來的也要送，不然伺服器送來的內容憑空消失
        ChatBlock.clear();
        ChatBlock.hold(two(10, "Nothing Here", 20, "Nothing There"), false);
        ChatBlock.hold(one(30, "Still Nothing"), false);
        Component raw = ChatBlock.preview(store);
        check("★ 一列都沒翻到的攔截塊照樣送出原文",
              raw != null && raw.getString().contains("Nothing Here")
                      && raw.getString().contains("Nothing There")
                      && raw.getString().contains("Still Nothing"));

        // ★ 攔下來但標了「不翻」的列（夾著別人的名字）：語料裡就算查得到也照原文
        ChatBlock.clear();
        ChatBlock.hold(two(33, "Purple Beacon", 69, "Red Beacon"), false);
        ChatBlock.hold(one(105, "Click here to reroll"), true);
        Component kept = ChatBlock.preview(store);
        check("★ 標了不翻的列照原文（實際 " + (kept == null ? "null" : visible(kept.getString())) + "）",
              kept != null && kept.getString().contains("Click here to reroll")
                      && kept.getString().contains("紫標"));

        // ★ 攔太久要強制送：後面一直有訊息進來，「安靜下來」等不到
        ChatBlock.clear();
        long now = System.currentTimeMillis();
        ChatBlock.hold(two(33, "Purple Beacon", 69, "Red Beacon"), false);
        ChatBlock.heldAt(now - 700);
        ChatBlock.arrivedAt(now);
        check("★ 剛來一則、但已經攔了 700ms -> 送", ChatBlock.ready(now + 10));
        ChatBlock.heldAt(now);
        check("才剛開始攔、又剛來一則 -> 還不送", !ChatBlock.ready(now + 10));
        check("安靜 200ms -> 送", ChatBlock.ready(now + 200));

        // 沒被攔的（原文加譯文模式）照舊：一列都沒翻到就不送
        ChatBlock.clear();
        ChatBlock.queue(two(10, "Nothing Here", 20, "Nothing There"), null);
        ChatBlock.queue(one(30, "Still Nothing"), null);
        check("沒被攔的塊一列都沒翻到就不送（原文自己會顯示）", ChatBlock.preview(store) == null);
        check("沒被攔的不算在攔", !ChatBlock.holding());
        ChatBlock.clear();

        realCorpus();

        System.out.println(failures == 0 ? "攔截面板：全部通過" : "攔截面板：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /**
     * 實際語料：log 裡還原出來的幾種信標效果，六個語言都要併成整句，不能留半句或英文。
     *
     * <p>列的英文原文照遊戲 log（Wynntils 記下的 {@code [CHAT/INFO]}）抄的。
     */
    private static void realCorpus() {
        Path base = Path.of("src/main/resources/assets/wynnchayuan/translations");
        List<StyledText> panel = List.of(
                two(33, "Purple Beacon", 69, "Red Beacon"),
                two(28, "+2 Curses, +2 End", 60, "+6 Challenges to"),
                two(40, "Reward Pulls", 80, "this Lootrun. Gain"),
                one(180, "no Time Bonus for"),
                one(186, "completing them."),
                blank(),
                two(40, "Aqua Beacon", 52, "Vibrant Yellow Beacon"),
                two(23, "Empower next Beacon", 48, "Spawn 3 Flying Chest"),
                one(58, "Effects"),
                blank(),
                one(86, "Vibrant Orange Beacon"),
                one(110, "+1 Beacon Choice"),
                one(109, "for 15 Challenges"),
                blank(),
                two(36, "Blue Beacon", 72, "Green Beacon"),
                two(30, "Choose a Boon at", 58, "+60s Time Bonus."),
                two(30, "100% Potency", 58, "Mobs gain no Buffs"),
                one(180, "this Challenge only"),
                blank(),
                one(105, "Click here to reroll"),
                one(116, "(2 rerolls left)"));
        for (String lang : List.of("zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es")) {
            TranslationStore store = new TranslationStore();
            store.loadAll(base.resolve(lang));
            ChatBlock.clear();
            for (StyledText row : panel) {
                ChatBlock.hold(row, false);
            }
            Component made = ChatBlock.preview(store);
            String all = made == null ? "" : made.getString();
            String[] rows = all.split(String.valueOf((char) 10), -1);
            if ("zh_tw".equals(lang) || "zh_cn".equals(lang)) {
                for (String row : rows) {
                    System.out.println("      " + lang + " | " + visible(row));
                }
            }
            check(lang + "：列數沒變（實際 " + rows.length + "）", rows.length == panel.size());
            boolean english = false;
            for (String word : List.of("Beacon", "Challenges", "Lootrun. Gain", "Reward Pulls",
                    "Time Bonus", "Effects", "Potency", "Flying Chest", "reroll", "Curses")) {
                english |= all.contains(word);
            }
            check(lang + "：整塊沒有留英文", !english);
        }
        ChatBlock.clear();
    }

    /** 去掉排版偏移之後看得見的字。 */
    private static String visible(String row) {
        return com.wynnchayuan.capture.GlyphSplitter.stripGlyphChars(row).strip();
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
