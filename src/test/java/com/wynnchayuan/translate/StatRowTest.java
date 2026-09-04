package com.wynnchayuan.translate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 裝備面板的「標籤 + 數值」整行，只查標籤、數值原樣接回去。
 *
 * <h2>為什麼需要這條路</h2>
 * 先前是靠<b>整行條目</b>收的，而同一個「法術傷害」在實機上長這些樣子：
 *
 * <pre>
 *   Fire Spell Damage {#}+{~} [{~}]          335 次
 *   Water Spell Damage{#}-{~} to -{~}        186 次
 *   Water Spell Damage {#}-{~} [{~}]         185 次
 *   Spell Damage {#}+{~} to {~}
 *   Elemental Spell Damage {#}+{~} ★{~} ⇧{~} ⇩{~}
 * </pre>
 *
 * 七種元素 × 正負 × 五六種數值格式，整行條目一種只收一個——收不完。
 * 每補一批，下一份 capture 又冒出新的排列。
 *
 * <p>而標籤本來就都在 {@code ui-labels.json} 裡（288 條）。所以改成把行尾
 * 純數值的那一段切掉、只查前面的標籤。一條標籤涵蓋所有排列。
 *
 * <h2>這條測試在盯什麼</h2>
 * 用<b>真正的語料</b>跑，不是捏的假資料——捏的資料只會證明捏的人想的是對的。
 * 反面同樣重要：句子不能被誤判成屬性列。
 */
public final class StatRowTest {

    private static int failures = 0;

    /** 見 {@link #labels()}：長得像屬性標籤的才掃。 */
    private static final java.util.regex.Pattern STAT_LABEL =
            java.util.regex.Pattern.compile("[A-Z][A-Za-z]*(?: [A-Za-z]+)*%?");

    /** {@code ui-labels.json} 的每一條鍵。 */
    private static List<String> labels() throws IOException {
        Path path = Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT, "ui-labels.json");
        JsonObject root = JsonParser.parseString(
                Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        List<String> out = new ArrayList<>();
        for (String key : root.keySet()) {
            // ui-labels.json 裡不只有屬性標籤，也躺著幾句句子片段
            // （「its potential.」「to sell (」）。那些後面接數值本來就不該翻，
            // 掃進來只會製造假警報。只認長得像屬性標籤的：大寫開頭、
            // 整條都是英文字與空白，可以帶 % 或 Raw 結尾。
            if (key.startsWith("_") || !STAT_LABEL.matcher(key).matches()
                    || root.get(key).getAsString().isBlank()) {
                continue;
            }
            out.add(key);
        }
        return out;
    }

    public static void main(String[] args) throws IOException {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        // 這一份 capture 裡出現次數最高的幾種排列
        starts(store, "Fire Spell Damage {#}+{~} [{~}]", "火屬性法術傷害");
        starts(store, "Water Spell Damage{#}-{~} to -{~}", "水屬性法術傷害");
        starts(store, "Water Spell Damage {#}-{~} [{~}]", "水屬性法術傷害");
        starts(store, "Air Spell Damage {#}+{~} [{~}]", "風屬性法術傷害");
        starts(store, "Elemental Spell Damage {#}+{~} [{~}, {~}]", "元素法術傷害");
        starts(store, "Damage Scale{#} [{~}]", "傷害適性");
        starts(store, "Main Scale{#} [{~}]", "主要適性");
        starts(store, "Walk Speed {#}+{~} [{~}]", "移動速度");
        starts(store, "Health Regen {#}-{~} [{~}]", "生命回復");
        starts(store, "Teleport Cost {#}-{~} [{~}]", "傳送消耗");

        // 實機截圖：同一件裝備上下兩行，一行是百分比、一行是實數。
        // 兩行都標成「法術傷害」的話，玩家分不出哪個是哪個——而那正是他要看的。
        starts(store, true, "Spell Damage {#}+{~} [{~}]", "法術傷害百分比");
        starts(store, false, "Spell Damage {#}+{~} [{~}]", "法術傷害值");

        everyLabel(store);
        ownerLabels(store);
        notAGap(store);

        // 數值原樣留著——數字被吃掉的話畫面上就少了一個屬性
        keeps(store, "Fire Spell Damage {#}+{~} [{~}]", "{#}+{~} [{~}]");
        keeps(store, "Elemental Spell Damage {#}+{~} ★{~} ⇧{~} ⇩{~}", "★{~} ⇧{~} ⇩{~}");

        // 數值中間的小字也要換掉，不然中文裡夾著一個英文
        contains(store, "Water Spell Damage{#}-{~} to -{~}", "到");
        contains(store, "Attack Speed {#}+{~} tier", "階");

        // 反面：句子不是屬性列。
        //
        // 這幾句<b>刻意不在語料裡</b>——語料裡有的句子本來就查得到，
        // 拿它們當反面只會測到別條路。第一句的尾巴確實是純數值，
        // 但前面那一長串不是標籤；第二句更像陷阱：句子裡<b>含有</b>
        // 「Fire Spell Damage」這個標籤，但整句不是標籤，一樣要回 null。
        nothing(store, "Grants extra courage to everyone nearby by +{~}.");
        nothing(store, "Deal Fire Spell Damage of a very strange kind {~}");
        nothing(store, "Allies within {~} blocks gain {~} of the health you gain");

        System.out.println(failures == 0
                ? "StatRow: 全部通過" : "StatRow: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /**
     * 「{@code <玩家名>'s <東西>}」的漂浮字。
     *
     * <h2>為什麼整行永遠查不到</h2>
     * 石碑放下去頭上浮的是「PoorChaCha's Mob Totem」——名字是玩家的，
     * 擷取那一關就擋掉了（見 {@code PlayerDataFilter}），所以這種句子
     * 從來沒進過語料，畫面上一直是英文。使用者回報了兩次。
     *
     * <p>做法是只翻後半、名字原樣留著。兩道關卡：前半要像 Minecraft 的 ID，
     * 後半要剛好是語料裡的一個鍵。
     */
    private static void ownerLabels(TranslationStore store) {
        owner(store, "PoorChaCha's Mob Totem", "PoorChaCha 的怪物石碑");
        owner(store, "PoorChaCha's Gathering Totem", "PoorChaCha 的採集石碑");
        // 全形撇號的版本，材質包兩種都出現過
        owner(store, "PoorChaCha’s Mob Totem", "PoorChaCha 的怪物石碑");

        // 反面：後半不是語料的鍵就整行不翻，免得中英夾雜
        report("後半不認得就不翻（實際："
                        + LineTranslator.lookup("Someone's Unknown Thingy", store, false) + "）",
                LineTranslator.lookup("Someone's Unknown Thingy", store, false) == null);
    }

    private static void owner(TranslationStore store, String row, String want) {
        String hit = LineTranslator.lookup(row, store, false);
        report("「" + row + "」-> 「" + want + "」（實際：" + hit + "）",
                want.equals(hit));
    }

    /**
     * 每一條標籤都接得回來。
     *
     * <h2>為什麼要全部掃一遍</h2>
     * 先前 {@code misc.json} 裡躺著<b>兩百多條</b>整行屬性列，是一次一次補出來的。
     * 這條路上線之後那些全部刪掉了——刪錯一條，畫面上就是一行英文，而且
     * validate 不會叫（語料本來就沒有那個鍵才是正常的）。
     *
     * <p>所以拿 {@code ui-labels.json} 的每一條標籤配上最常見的兩種數值形狀
     * 各跑一次。這比逐條盯那兩百多條更嚴——標籤是來源，行是它的排列。
     */
    private static void everyLabel(TranslationStore store) throws IOException {
        java.util.List<String> missed = new java.util.ArrayList<>();
        int checked = 0;
        for (String label : labels()) {
            for (String shape : new String[] {" {#}+{~} [{~}]", "{#}-{~} to -{~}"}) {
                checked++;
                if (LineTranslator.lookup(label + shape, store, false) == null) {
                    missed.add(label + shape);
                }
            }
        }
        report("每一條標籤配上數值都接得回來（掃了 " + checked + " 種，漏掉 "
                + (missed.size() > 6 ? missed.subList(0, 6) + "…" : missed) + "）",
                missed.isEmpty());
    }

    /**
     * 屬性列不會被記成「還沒翻」。
     *
     * <h2>為什麼這條很重要</h2>
     * {@code captured.json} 是翻譯團隊的工作清單。判斷「這條翻了沒」走的是
     * {@link TranslationStore#hasTranslation}，而那裡本來只查整行——屬性列是
     * 拆成標籤加數值查的，於是畫面上明明是中文，清單裡照樣列成缺口。
     *
     * <p>後果不是多幾條雜訊而已：團隊照著清單補，補出來的是<b>整行</b>條目，
     * 而整行條目會把標籤那條路整個蓋掉。先前 misc.json 裡那兩百多條就是這樣
     * 長出來的，蓋掉之後百分比與實數又分不出來了。這條測試把那個循環擋在門口。
     */
    private static void notAGap(TranslationStore store) {
        for (String row : new String[] {
                "Walk Speed {#}+{~} [{~}]",
                "Spell Damage {#}+{~} [{~}]",
                "Health Regen {#}-{~} [{~}]",
                "Water Spell Damage{#}-{~} to -{~}",
                "Attack Speed {#}+{~} tier"}) {
            report("「" + row + "」不會被記成缺口", store.hasTranslation(row));
        }
        // 反面：真的沒翻的還是要留在清單上，不然團隊就看不到了
        report("真的沒翻的還是缺口",
                !store.hasTranslation("Grants extra courage to everyone nearby by +{~}."));
    }

    private static void starts(TranslationStore store, boolean percent,
                               String row, String head) {
        String hit = LineTranslator.lookup(row, store, percent);
        report("「" + row + "」" + (percent ? "（百分比）" : "（實數）")
                + "換成「" + head + "」（實際：" + hit + "）",
                hit != null && hit.startsWith(head));
    }

    private static void starts(TranslationStore store, String row, String head) {
        String hit = LineTranslator.lookup(row, store, false);
        report("「" + row + "」的標籤換成「" + head + "」（實際：" + hit + "）",
                hit != null && hit.startsWith(head));
    }

    private static void keeps(TranslationStore store, String row, String tail) {
        String hit = LineTranslator.lookup(row, store, false);
        report("「" + row + "」的數值原樣留著（實際：" + hit + "）",
                hit != null && hit.endsWith(tail));
    }

    private static void contains(TranslationStore store, String row, String want) {
        String hit = LineTranslator.lookup(row, store, false);
        report("「" + row + "」裡有「" + want + "」（實際：" + hit + "）",
                hit != null && hit.contains(want));
    }

    private static void nothing(TranslationStore store, String sentence) {
        String hit = LineTranslator.lookup(sentence, store, false);
        report("句子不會被當成屬性列：" + sentence + "（實際：" + hit + "）", hit == null);
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
