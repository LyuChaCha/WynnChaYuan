package com.wynnchayuan.translate;

import java.nio.file.Path;

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

    public static void main(String[] args) {
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

        // 數值原樣留著——數字被吃掉的話畫面上就少了一個屬性
        keeps(store, "Fire Spell Damage {#}+{~} [{~}]", "{#}+{~} [{~}]");
        keeps(store, "Elemental Spell Damage {#}+{~} ★{~} ⇧{~} ⇩{~}", "★{~} ⇧{~} ⇩{~}");

        // 數值中間的小字也要換掉，不然中文裡夾著一個英文
        contains(store, "Water Spell Damage{#}-{~} to -{~}", "至");
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
