package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 賭注（gambit）的敘述要整段翻，不能照英文的斷行走。
 *
 * <h2>實機回報</h2>
 * 「gambit 幫忙整理一次排版，讓它好讀，而不是莫名斷句」。畫面上長這樣：
 *
 * <pre>
 *   你啟用的賭注愈多，
 *   之後的賭注給的獎勵就愈多
 * </pre>
 *
 * <p>那是<b>逐行</b>收的譯文：英文被 tooltip 寬度折成兩行，語料就照著收了兩條，
 * 於是中文也被釘死在英文的斷點上——句子中間硬斷開，還留了一個懸空的逗號。
 * 中文比英文短，本來一行就放得下。
 *
 * <h2>做法</h2>
 * 整段收一條（攤平查表會把換行壓成空格，所以<b>不管遊戲折在哪裡</b>都對得上），
 * 原本逐行的那些留空——空條目不會載入，也就蓋不掉整段。
 *
 * <p>鍵一律用 capture 到的碎片接起來，不是 wiki 的字面：wiki 寫
 * 「{@code You do 90% less}」，實機是「{@code You deal {~} less … from you}」。
 * 見 GLOSSARY 旁邊那條「wiki 只能查意思」。
 */
public final class GambitBlockTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        // 共感者：實機折成三行，譯文要收斂成一行
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

        // ★ 逐行條目必須是空的，否則它會先命中，整段那條路等於沒用
        for (String line : new String[] {
                "The more gambits you enable the", "more rewards next ones will give",
                "For each {~} health you lack,", "make your attacks {~} weaker",
                "Every hit you take will deal", "health as damage",
                "temporarily lock your", "equipment and potions"}) {
            report("★ 逐行條目沒有譯文：「" + line + "」",
                   LineTranslator.lookup(line, store, false) == null);
        }

        System.out.println(failures == 0
                ? "賭注敘述：全部通過" : "賭注敘述：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /**
     * 整段翻出來，而且<b>行數要比原文少</b>——中文比英文短，還照原文斷行就是
     * 使用者回報的那個症狀。
     */
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
        report(what + "：整段翻出來（實際「" + all + "」）",
               all.contains(want) && !all.contains(lines[0]));
        report(what + "：沒有照英文的斷點硬斷（原文 " + lines.length
                        + " 行，譯文 " + out.size() + " 行）",
               !out.isEmpty() && out.size() < lines.length);
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
