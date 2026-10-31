package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class GambitBlockTest {

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        block("共感者", store, new String[] {
                "When a player within 6 blocks",
                "of you takes damage, you also",
                "take 15% of that damage."}, "格內的玩家");

        block("淌血戰士", store, new String[] {
                "For each 1% health you lack,",
                "make your attacks 1% weaker"}, "攻擊就減弱");

        block("血友者", store, new String[] {
                "Every hit you take will deal",
                "at least 5% of your max",
                "health as damage"}, "最大生命");

        block("重負和平者", store, new String[] {
                "For every 2 mobs killed,",
                "reduce your received healing",
                "by 10% for 10 seconds."}, "持續");

        block("啟用愈多獎勵愈多", store, new String[] {
                "The more gambits you enable the",
                "more rewards next ones will give"}, "獎勵就愈多");

        block("開始討伐戰的提示", store, new String[] {
                "Starting a Raid will",
                "temporarily lock your",
                "equipment and potions"}, "裝備與藥水");

        block("墮天使", store, new String[] {
                "After being airborne for 2s",
                "you become unable to gain",
                "health in any way. This",
                "effect persists for 2.5s",
                "after landing."}, "落地後");

        for (String line : new String[] {
                "The more gambits you enable the", "more rewards next ones will give",
                "For each {~} health you lack,", "make your attacks {~} weaker",
                "Every hit you take will deal", "health as damage",
                "temporarily lock your", "equipment and potions",
                "After being airborne for {~}s"}) {
            check("★ 逐行條目沒有譯文：「" + line + "」",
                   TemplateLookup.lookup(line, store, false) == null);
        }

        System.out.println(failures == 0
                ? "賭注敘述：全部通過" : "賭注敘述：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void block(String what, TranslationStore store,
                              String[] lines, String want) {
        List<Component> in = new ArrayList<>(lines.length);
        for (String line : lines) {
            in.add(Component.literal(line));
        }
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(in, store);
        StringBuilder zh = new StringBuilder();
        for (Component c : out) {
            zh.append(c.getString());
        }
        String all = zh.toString();
        check(what + "：整段翻出來（實際「" + all + "」）",
               all.contains(want) && !all.contains(lines[0]));
        check(what + "：沒有照英文的斷點硬斷（原文 " + lines.length
                        + " 行，譯文 " + out.size() + " 行）",
               !out.isEmpty() && out.size() < lines.length);
    }

}
