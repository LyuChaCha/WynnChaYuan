package com.wynnchayuan.translate;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 技能樹的「項目符號 + 標籤 + 技能名」整行，冒號前後各自查。
 *
 * <h2>為什麼需要這條路</h2>
 * 先前是靠<b>整行條目</b>收的，而技能樹的節點說明長這樣：
 *
 * <pre>
 *   ✔ Required Ability: Heal
 *   ✖ Required Ability: Dimensional Tear
 *   - Psychokinesis
 * </pre>
 *
 * <b>五個職業一千兩百多個技能 × 勾叉兩種 × 封鎖清單</b>，
 * 整行條目一種只收一個——收不完。上一版手動補了五組，這一份 capture
 * 又冒出八組沒收到的；{@code misc.json} 裡還躺著一批 dst 留空的同形狀條目，
 * 就是補不動的證據。
 *
 * <p>而兩邊本來就都有：{@code Required Ability:} 在
 * {@code ability-labels.json}，技能名在 {@code ability/} 底下五個職業檔裡。
 * 所以改成冒號前後各自查、再原樣組回去，一條標籤涵蓋五個職業。
 *
 * <h2>這條測試在盯什麼</h2>
 * 用<b>真正的語料</b>跑，五個職業各抽幾個技能。反面同樣重要：
 * 一般句子不能被誤判成技能列，那會拼出半中半英。
 */
public final class AbilityRowTest {

    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        // 這一份 capture 裡沒收到的八組（都是法師）
        row(store, "✔ Required Ability: Heal", "✔ 前置技能: 治療");
        row(store, "✔ Required Ability: Meteor", "✔ 前置技能: 隕石");
        row(store, "✔ Required Ability: Sunshower", "✔ 前置技能: 太陽雨");
        // 技能是「結晶」，狀態是「結晶化」（Crystallized）——查的是技能檔，
        // 不會被 ability-terms.json 的狀態名蓋掉
        row(store, "✔ Required Ability: Crystallize", "✔ 前置技能: 結晶");
        row(store, "✖ Required Ability: Arcane Transfer", "✖ 前置技能: 祕法回流");
        row(store, "✖ Required Ability: Pyrokinesis", "✖ 前置技能: 馭火");
        row(store, "✖ Required Ability: Time Dilation", "✖ 前置技能: 時流偏移");
        row(store, "✖ Required Ability: Dimensional Tear", "✖ 前置技能: 次元裂隙");

        // 同一條路要涵蓋另外四個職業，不然下次換職業又是一輪回報
        row(store, "✔ Required Ability: Bash", "✔ 前置技能: 重擊");
        row(store, "✔ Required Ability: Spin Attack", "✔ 前置技能: 迴刃斬");
        row(store, "✖ Required Ability: Arrow Storm", "✖ 前置技能: 箭矢風暴");
        row(store, "✖ Required Ability: Totem", "✖ 前置技能: 圖騰");
        row(store, "✔ Unlocked Ability: Vanish", "✔ 已解鎖技能: 隱身術");

        // 節點被別的技能擋住時，下面接一串封鎖清單。
        // 項目符號的「-」isDecoration 刻意不認（「- Converts up to」那種鍵
        // 就是這樣開頭的），所以要等整行查失敗之後才剝。
        row(store, "- Psychokinesis", "- 馭念");
        row(store, "- Void Acceleration", "- 虛空加速");
        row(store, "- Meteor Shower", "- 流星雨");

        // 整行條目仍然先命中。翻譯團隊手寫的那六組，這條路排在整行查後面，
        // 不會把它們蓋掉。
        row(store, "✔ Required Ability: Escape", "✔ 前置技能: 逃脫");
        row(store, "✖ Required Ability: Escape", "✖ 前置技能: 逃脫");

        // 同一片面板上的紅字，沒有冒號也沒有符號，就是一條普通條目
        row(store, "Blocked by another ability", "已被其他技能阻擋");

        // 同一條路也收 Lootrun 的洞窟清單。洞窟名是專有名詞，留原文。
        row(store, "✔ Cave: Eyeball Gauntlet", "✔ 洞穴: Eyeball Gauntlet");
        row(store, "✖ Cave: Spiteful Crossing", "✖ 洞穴: Spiteful Crossing");
        row(store, "✔ Cave: The Lantern Keeper's Abode",
                "✔ 洞穴: The Lantern Keeper's Abode");

        newAbility(store);
        negatives(store);

        System.out.println(failures == 0
                ? "AbilityRow: 全部通過" : "AbilityRow: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /**
     * Wynncraft 改版新增、還沒進語料的技能。
     *
     * <p>{@code Required Ability:} 後面必定是技能名，所以查不到也不放棄——
     * 標籤照翻、名字留原文。技能名是專有名詞，留原文玩家看得懂，
     * 整行英文才是使用者回報的那個問題。
     */
    private static void newAbility(TranslationStore store) {
        row(store, "✔ Required Ability: Totally New Spell",
                "✔ 前置技能: Totally New Spell");
        row(store, "✖ Unlocked Ability: Totally New Spell",
                "✖ 已解鎖技能: Totally New Spell");
    }

    /**
     * 不該被誤判成技能列的。
     *
     * <p>原樣接回去的只有 {@code Required Ability:} 那幾個標籤。其他標籤的
     * 冒號後面要是自己查不到，整條就得放棄——不然畫面上會出現一半中文
     * 一半英文的句子，那比整句英文更難讀。
     */
    private static void negatives(TranslationStore store) {
        // 標籤不認得：整條放棄
        nothing(store, "Deal damage: a lot of it");
        nothing(store, "Weird Made Up Label: Heal");

        // 標籤認得，但後面不是技能名而是查不到的東西：一樣放棄。
        // market 的篩選列就是這樣（「- Name Contains: Insulators」），
        // 那個標籤還沒收進語料，收進去之前不該硬翻一半。
        nothing(store, "- Name Contains: Insulators");

        // 負號不是項目符號。剝掉的話「-{~} to -{~}」會少一個減號，
        // 玩家看到的傷害就從負變正。
        nothing(store, "-{~} to -{~}");
        nothing(store, "-{~}%");

        // 冒號後面沒空白的不切（時間、比例）
        nothing(store, "12:30");

        // 剝掉符號之後也查不到就是查不到，不能硬湊
        nothing(store, "- Totally New Spell");
    }

    private static void row(TranslationStore store, String line, String want) {
        String hit = LineTranslator.lookup(line, store, false);
        report("「" + line + "」-> 「" + want + "」（實際：" + hit + "）", want.equals(hit));
    }

    private static void nothing(TranslationStore store, String line) {
        String hit = LineTranslator.lookup(line, store, false);
        report("「" + line + "」不該被切開（實際：" + hit + "）", hit == null);
    }

    private static void report(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) {
            failures++;
        }
    }
}
