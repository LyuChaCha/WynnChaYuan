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

        // ---- 分類標題是複數，市集要的是單數 ----
        // 語料裡的分類標題常常是複數（Amplifiers、Insulators），而市集上的
        // 物品叫「Corkian Amplifier I」——用複數一筆都搜不到。實機回報的就是這個。
        turns(market, "增幅器", "Amplifier");
        turns(market, "絕緣器", "Insulator");

        // 「綠寶石」先前對到三個：Emeralds、Emerald、以及模板「{~} emeralds」。
        // 模板本來就不該當候選，複數收斂之後剩一個，就不必再問玩家了。
        List<String> emerald = market.candidates("綠寶石");
        report("「綠寶石」收斂成一個（實際：" + emerald + "）",
                emerald.size() == 1 && "Emerald".equals(emerald.get(0)));
        for (String hit : market.candidates("綠寶石")) {
            report("候選裡沒有佔位符（" + hit + "）",
                    hit.indexOf('{') < 0 && hit.indexOf('}') < 0);
        }

        // 去 s 要謹慎，不能把本來就以 s 結尾的字弄壞
        keepsSingular("Glass", "Glass");
        keepsSingular("Bob's Tear", "Bob's Tear");
        keepsSingular("Corkian Amplifier III", "Corkian Amplifier III");
        keepsSingular("Boots", "Boot");

        // ---- 對不準的時候不要猜 ----
        // 「Corkian 增幅器」先前對到 Amplifier 與 Augments 兩個，那是 gui.json
        // 把 Augments 也翻成「增幅器」造成的（其他四條都是「強化道具」）。
        // 修好之後只剩一個，這裡順便釘住不要再撞回去。
        List<String> corkian = market.candidates("Corkian 增幅器");
        report("「Corkian 增幅器」只對到一個（實際：" + corkian + "）",
                corkian.size() == 1);

        sameItemOnce(market);
        shapes();

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

        prompts();

        System.out.println(failures == 0
                ? "市集搜尋：全部通過" : "市集搜尋：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /**
     * 同一個物品只問一次。
     *
     * <h2>實機回報</h2>
     * 打「股份」跳出 <b>13 個</b>候選，而其中一半根本是同一個東西：
     *
     * <pre>
     *   Silverbull Share
     *   ✮ Silverbull Share              ← 差一個面板上的項目符號
     *   Trade Silverbull Shares         ← 按鈕文字
     *   Get Tradable Silverbull Shares  ← 按鈕文字
     *   Only tradable shares can be     ← 句子被折斷的一截
     *   Not enough tradable shares      ← 同上
     *   Shares can be used on           ← 同上
     * </pre>
     *
     * <p>三道處理：項目符號剝掉、句子碎片擋掉、<b>短的候選包含得到的長候選</b>
     * 直接不問（市集是包含比對，搜短的一定不比搜長的差）。
     */
    private static void sameItemOnce(MarketSearch market) {
        List<String> shares = market.candidates("股份");
        report("「股份」的候選收斂了（實際 " + shares.size() + " 個：" + shares + "）",
                shares.size() <= 4);
        report("真正的物品還在", shares.contains("Silverbull Share"));

        // ★ 同一個物品的其他寫法都不該再出現
        for (String dup : new String[] {"✮ Silverbull Share", "Trade Silverbull Share",
                                        "Trade Silverbull Shares",
                                        "Get Tradable Silverbull Share"}) {
            report("不再重複問「" + dup + "」", !shares.contains(dup));
        }
        // ★ 句子碎片不該當候選
        for (String junk : new String[] {"Only tradable shares can be",
                                         "Not enough tradable shares",
                                         "Shares can be used on"}) {
            report("句子碎片不當候選「" + junk + "」", !shares.contains(junk));
        }
        for (String hit : shares) {
            report("候選沒有裝飾符號（" + hit + "）",
                    hit.equals(hit.strip()) && Character.isLetterOrDigit(hit.charAt(0)));
        }
    }

    /**
     * 名字與句子的形狀判斷。
     *
     * <p>用<b>捏的</b>資料而不是真語料——這裡要釘住的是規則本身，
     * 真語料哪天被改動不該讓這幾條變成假通過或假失敗。
     */
    private static void shapes() {
        MarketSearch m = new MarketSearch();
        m.add("Silverbull Share", "Silverbull 股份");
        m.add("✮ Silverbull Share", "✮ Silverbull 股份");
        m.add("Trade Silverbull Shares", "交易 Silverbull 股份");
        m.add("Only tradable shares can be", "只有可交易的股份才能");
        m.add("Ring of the Wild", "荒野之戒");
        m.add("Tradable Shares:", "可交易股份:");

        // 四條「Silverbull 股份」併成一條；「可交易股份」是<b>別的</b>標籤，
        // 本來就該留著——收斂的是重複，不是把候選壓成一個。
        List<String> all = m.candidates("股份");
        report("同一個物品只留一條（實際：" + all + "）",
                all.equals(List.of("Silverbull Share", "Tradable Share")));

        // ★ 反面：名字裡本來就有的小寫虛詞不能被當成句子擋掉
        List<String> ring = m.candidates("荒野之戒");
        report("「Ring of the Wild」不是句子（實際：" + ring + "）",
                ring.size() == 1 && "Ring of the Wild".equals(ring.get(0)));
    }

    /**
     * 那句提示出現才會啟動。
     *
     * <h2>為什麼要自己認</h2>
     * Wynntils 判斷「市集在等你輸入」靠的是比對<b>英文原文加行尾錨點</b>：
     * {@code ^§5(圖示) Type the item name or type 'cancel' to cancel:$}。
     * 而我們自己會翻聊天——就地取代模式下那一行整個變成中文，原文加譯文模式下
     * 行尾也不再是 {@code cancel:}。兩種都會讓它比不到，狀態永遠不會進入搜尋輸入。
     * 我們的翻譯把它自己要用的訊號弄壞了，使用者回報「沒有真的轉換」就是這個。
     */
    private static void prompts() {
        MarketListener listener = new MarketListener();
        MarketListener.searching(false);

        listener.onChat(chat("§5✦ Type the item name or type 'cancel' to cancel:"));
        report("英文提示會啟動", MarketListener.armed());

        // 就地取代之後畫面上只剩中文，一樣要認得
        MarketListener.searching(false);
        listener.onChat(chat("§5✦ 輸入物品名稱，或輸入 'cancel' 取消："));
        report("中文提示也會啟動", MarketListener.armed());

        // 取消之後要關掉，不然下一句正常聊天會被改
        listener.onChat(chat("§4✖ You moved and your chat input was canceled."));
        report("取消之後就關掉", !MarketListener.armed());

        // 反面：一般聊天不能啟動
        MarketListener.searching(false);
        listener.onChat(chat("§7有人說：這個增幅器不錯"));
        report("一般聊天不會啟動", !MarketListener.armed());
    }

    private static com.wynntils.handlers.chat.event.ChatMessageEvent.Match chat(String text) {
        return new com.wynntils.handlers.chat.event.ChatMessageEvent.Match(
                com.wynntils.core.text.StyledText.fromString(text),
                com.wynntils.handlers.chat.type.RecipientType.INFO);
    }

    /** 去複數的 s 只該動真的複數。 */
    private static void keepsSingular(String name, String want) {
        String got = MarketSearch.singular(name);
        report("「" + name + "」→「" + want + "」（實際：" + got + "）", want.equals(got));
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
