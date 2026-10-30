package com.wynnchayuan.translate;

import java.io.IOException;
import java.nio.file.Path;

public final class AbilityRowTest {

    private static int failures = 0;

    public static void main(String[] args) throws IOException {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        row(store, "✔ Required Ability: Heal", "✔ 前置技能: 治療");
        row(store, "✔ Required Ability: Meteor", "✔ 前置技能: 隕石");
        row(store, "✔ Required Ability: Sunshower", "✔ 前置技能: 太陽雨");
        row(store, "✔ Required Ability: Crystallize", "✔ 前置技能: 結晶");
        row(store, "✖ Required Ability: Arcane Transfer", "✖ 前置技能: 祕法回流");
        row(store, "✖ Required Ability: Pyrokinesis", "✖ 前置技能: 馭火");
        row(store, "✖ Required Ability: Time Dilation", "✖ 前置技能: 時流偏移");
        row(store, "✖ Required Ability: Dimensional Tear", "✖ 前置技能: 次元裂隙");

        row(store, "✔ Required Ability: Bash", "✔ 前置技能: 重擊");
        row(store, "✔ Required Ability: Spin Attack", "✔ 前置技能: 迴刃斬");
        row(store, "✖ Required Ability: Arrow Storm", "✖ 前置技能: 箭矢風暴");
        row(store, "✖ Required Ability: Totem", "✖ 前置技能: 圖騰");
        row(store, "✔ Unlocked Ability: Vanish", "✔ 已解鎖技能: 隱身術");

        row(store, "- Psychokinesis", "- 馭念");
        row(store, "- Void Acceleration", "- 虛空加速");
        row(store, "- Meteor Shower", "- 流星雨");

        row(store, "✔ Required Ability: Escape", "✔ 前置技能: 逃脫");
        row(store, "✖ Required Ability: Escape", "✖ 前置技能: 逃脫");

        row(store, "Blocked by another ability", "已被其他技能阻擋");

        row(store, "✔ Cave: Eyeball Gauntlet", "✔ 洞窟: 眼球試煉");
        row(store, "✖ Cave: Spiteful Crossing", "✖ 洞窟: 怨恨渡口");
        row(store, "✔ Cave: The Lantern Keeper's Abode",
                "✔ 洞窟: 掌燈者的居所");

        newAbility(store);
        negatives(store);

        System.out.println(failures == 0
                ? "AbilityRow: 全部通過" : "AbilityRow: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void newAbility(TranslationStore store) {
        row(store, "✔ Required Ability: Totally New Spell",
                "✔ 前置技能: Totally New Spell");
        row(store, "✖ Unlocked Ability: Totally New Spell",
                "✖ 已解鎖技能: Totally New Spell");
    }

    private static void negatives(TranslationStore store) {
        nothing(store, "Deal damage: a lot of it");
        nothing(store, "Weird Made Up Label: Heal");

        nothing(store, "Total Damage: Totally Made Up Thing");

        nothing(store, "-{~} to -{~}");
        nothing(store, "-{~}%");

        nothing(store, "12:30");

        nothing(store, "- Totally New Spell");
    }

    private static void row(TranslationStore store, String line, String want) {
        String hit = TemplateLookup.lookup(line, store, false);
        report("「" + line + "」-> 「" + want + "」（實際：" + hit + "）", want.equals(hit));
    }

    private static void nothing(TranslationStore store, String line) {
        String hit = TemplateLookup.lookup(line, store, false);
        report("「" + line + "」不該被切開（實際：" + hit + "）", hit == null);
    }

    private static void report(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) {
            failures++;
        }
    }
}
