package com.wynnchayuan.listener;

import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.MarketSearch;
import com.wynnchayuan.translate.TranslationStore;

import java.nio.file.Path;
import java.util.List;

/**
 * 市集搜尋打中文，送出前換成英文。
 *
 * <h2>為什麼這條要測得很兇</h2>
 * 這是整個模組<b>唯一</b>會改寫玩家自己打的字的地方。改錯的話，輕則搜不到，
 * 重則把一句正常的聊天發成別的東西。所以兩邊都要釘住：該換的要換得對，
 * 不該碰的一個字都不能動。
 *
 * <h2>用真的語料</h2>
 * 反查的準度取決於語料本身，捏的資料測不出撞名。實際量過：2789 條物品名裡
 * 只有 41 個中文名對到兩個以上英文名。
 */
public final class MarketSearchTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));
        MarketSearch market = store.market();
        System.out.println("  索引裡有 " + market.size() + " 個中文物品名");

        // ---- 該換的 ----
        // 使用者實際會打的樣子：中英之間<b>沒有空格</b>，而語料裡有。
        turns(market, "Corkian增幅器 III", "Corkian Amplifier III");
        turns(market, "Corkian 增幅器 III", "Corkian Amplifier III");
        turns(market, "熟成金果", "Ripe Aureate Fruit");
        turns(market, "厄運之石", "Doom Stone");

        // 大小寫與前後空白不該影響結果
        turns(market, "  熟成金果  ", "Ripe Aureate Fruit");

        // ---- 對不準的時候不要猜 ----
        // 「Corkian 增幅器」先前對到 Amplifier 與 Augments 兩個，那是 gui.json
        // 把 Augments 也翻成「增幅器」造成的（其他四條都是「強化道具」）。
        // 修好之後只剩一個，這裡順便釘住不要再撞回去。
        List<String> corkian = market.candidates("Corkian 增幅器");
        report("「Corkian 增幅器」只對到一個（實際：" + corkian + "）",
                corkian.size() == 1);

        report("查不到的回 null", market.lookup("這不是任何一個物品的名字") == null);
        report("太短的不查", market.lookup("石") == null);

        // ---- 不該碰的 ----
        // 沒在市集搜尋狀態時，一個字都不能動。這一關在最前面，
        // 連語料都不會去問——所以測起來也不需要整個模組起來。
        MarketListener.searching(false);
        keeps("沒在市集裡", "熟成金果");
        keeps("沒在市集裡（英文）", "Ripe Aureate Fruit");

        // 就算在市集裡，指令與空字串也不動
        MarketListener.searching(true);
        keeps("斜線指令", "/tm search 熟成金果");
        keeps("空字串", "");
        keeps("只有空白", "   ");
        MarketListener.searching(false);

        System.out.println(failures == 0
                ? "市集搜尋：全部通過" : "市集搜尋：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void turns(MarketSearch market, String zh, String want) {
        String got = market.lookup(zh);
        report("「" + zh + "」→「" + want + "」（實際：" + got + "）", want.equals(got));
    }

    private static void keeps(String what, String message) {
        String got = MarketListener.rewrite(message);
        report(what + "：原樣送出（實際：" + got + "）", message.equals(got));
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
