package com.wynnchayuan.listener;

import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.MarketSearch;
import com.wynnchayuan.translate.TranslationStore;

import java.nio.file.Path;
import java.util.List;

public final class MarketSearchTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));
        MarketSearch market = store.market();
        System.out.println("  索引裡有 " + market.size() + " 個中文物品名");

        turns(market, "Corkian增幅器 III", "Corkian Amplifier III");
        turns(market, "Corkian 增幅器 III", "Corkian Amplifier III");
        turns(market, "熟成金果", "Ripe Aureate Fruit");
        turns(market, "厄運之石", "Doom Stone");

        turns(market, "  熟成金果  ", "Ripe Aureate Fruit");

        turns(market, "增幅器", "Amplifier");
        turns(market, "絕緣器", "Insulator");

        List<String> emerald = market.candidates("綠寶石");
        report("「綠寶石」收斂成一個（實際：" + emerald + "）",
                emerald.size() == 1 && "Emerald".equals(emerald.get(0)));
        for (String hit : market.candidates("綠寶石")) {
            report("候選裡沒有佔位符（" + hit + "）",
                    hit.indexOf('{') < 0 && hit.indexOf('}') < 0);
        }

        keepsSingular("Glass", "Glass");
        keepsSingular("Bob's Tear", "Bob's Tear");
        keepsSingular("Corkian Amplifier III", "Corkian Amplifier III");
        keepsSingular("Boots", "Boot");

        List<String> corkian = market.candidates("Corkian 增幅器");
        report("「Corkian 增幅器」只對到一個（實際：" + corkian + "）",
                corkian.size() == 1);

        sameItemOnce(market);
        tradableOnly(market);
        shapes();

        report("查不到的回 null", market.lookup("這不是任何一個物品的名字") == null);
        report("太短的不查", market.lookup("石") == null);

        MarketListener.searching(false);
        keeps("沒在市集裡", "熟成金果");
        keeps("沒在市集裡（英文）", "Ripe Aureate Fruit");

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

    private static void sameItemOnce(MarketSearch market) {
        List<String> shares = market.candidates("股份");
        report("「股份」的候選收斂了（實際 " + shares.size() + " 個：" + shares + "）",
                shares.size() <= 4);
        report("真正的物品還在", shares.contains("Silverbull Share"));

        for (String dup : new String[] {"✮ Silverbull Share", "Trade Silverbull Share",
                                        "Trade Silverbull Shares",
                                        "Get Tradable Silverbull Share"}) {
            report("不再重複問「" + dup + "」", !shares.contains(dup));
        }
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

    private static void tradableOnly(MarketSearch market) {
        List<String> hearts = market.suggestions("之心").stream()
                .map(MarketSearch.Suggestion::english).toList();
        for (String junk : new String[] {"An Iron Heart Part I", "The Heart of the Decay",
                                         "Isoptera Heart"}) {
            report("「之心」不列「" + junk + "」（實際：" + hearts + "）", !hearts.contains(junk));
        }
        report("素材還在（Snow Heart）", hearts.contains("Snow Heart"));
        report("任務名不當候選", market.candidates("致命的失足").isEmpty());
        report("NPC 名不當候選（實際：" + market.candidates("鐵匠") + "）",
               !market.candidates("鐵匠").contains("Blacksmith")
                       && !market.candidates("鐵匠").contains("The Blacksmith"));
        report("面向不當候選", market.candidates("城垛加固意象").isEmpty());
        report("技能名不當候選", market.candidates("箭矢炸彈").isEmpty());

        MarketSearch m = new MarketSearch();
        m.addListed("Corkian Amplifier III", "Corkian 增幅器 III");
        m.addListed("Silverbull Share", "Silverbull 股份");
        m.addListed("{~} x Dragon Aura {#}{#}", "{~} x 龍之靈氣 {#}{#}");
        m.addListed("Incompatible with Amplifiers", "與增幅器不相容");
        m.addListed("Remove Powders", "移除粉末");
        m.addListed("A Grave Mistake", "致命的失足");
        m.addListed("Blacksmith", "鐵匠");
        m.addListed("DecrepitÀÀÀSewers Key", "DecrepitÀÀÀSewers 鑰匙");
        report("扁平檔只收物品類別與數量行（實際 " + m.size() + " 條）", m.size() == 3);
    }

    private static void shapes() {
        MarketSearch m = new MarketSearch();
        m.add("Silverbull Share", "Silverbull 股份");
        m.add("✮ Silverbull Share", "✮ Silverbull 股份");
        m.add("Trade Silverbull Shares", "交易 Silverbull 股份");
        m.add("Only tradable shares can be", "只有可交易的股份才能");
        m.add("Ring of the Wild", "荒野之戒");
        m.add("Tradable Shares:", "可交易股份:");

        List<String> all = m.candidates("股份");
        report("同一個物品只留一條（實際：" + all + "）",
                all.equals(List.of("Silverbull Share", "Tradable Share")));

        List<String> ring = m.candidates("荒野之戒");
        report("「Ring of the Wild」不是句子（實際：" + ring + "）",
                ring.size() == 1 && "Ring of the Wild".equals(ring.get(0)));
    }

    private static void prompts() {
        MarketListener listener = new MarketListener();
        MarketListener.searching(false);

        listener.onChat(chat("§5✦ Type the item name or type 'cancel' to cancel:"));
        report("英文提示會啟動", MarketListener.armed());

        MarketListener.searching(false);
        listener.onChat(chat("§5✦ 輸入物品名稱，或輸入 'cancel' 取消："));
        report("中文提示也會啟動", MarketListener.armed());

        listener.onChat(chat("§4✖ You moved and your chat input was canceled."));
        report("取消之後就關掉", !MarketListener.armed());

        MarketListener.searching(false);
        listener.onChat(chat("§7有人說：這個增幅器不錯"));
        report("一般聊天不會啟動", !MarketListener.armed());

        listener.onChat(chat("§5✦ 输入物品名称，或输入 'cancel' 取消："));
        report("簡體提示也會啟動", MarketListener.armed());
        listener.onChat(chat("§4✖ 你移动了，聊天输入已取消。"));
        report("簡體的取消也會關掉", !MarketListener.armed());
        MarketListener.searching(false);

        String core = MarketListener.core("{#} \n{#} 입력하세요 'cancel'\n{#}");
        report("譯文剝掉圖示後取最長的那一行（實際：" + core + "）",
                "입력하세요 'cancel'".equals(core));

        simplified();
        quantityLines();
        latinNames();
        pickerList();
    }

    private static void pickerList() {
        MarketSearch m = new MarketSearch();
        m.add("Stone", "石頭");
        m.add("Stone Fragment", "石頭碎片");
        m.add("Doom Stone", "厄運之石");
        m.add("Silverbull Share", "Silverbull 股份");
        m.add("✮ Silverbull Share", "✮ Silverbull 股份");
        m.add("Stones", "石頭");

        List<MarketSearch.Suggestion> stone = m.suggestions("石頭");
        report("完全相同的排第一（實際：" + stone + "）",
                !stone.isEmpty() && "Stone".equals(stone.get(0).english())
                        && "石頭".equals(stone.get(0).chinese()));
        report("「石頭碎片」不被收進「石頭」",
                stone.stream().anyMatch(s -> "Stone Fragment".equals(s.english())));
        report("複數與單數只列一次",
                stone.stream().filter(s -> "Stone".equals(s.english())).count() == 1);

        List<MarketSearch.Suggestion> one = m.suggestions("石");
        report("一個字也列（實際 " + one.size() + " 個）", one.size() == 3);

        List<MarketSearch.Suggestion> shares = m.suggestions("股份");
        report("項目符號版本不重複列（實際：" + shares + "）",
                shares.size() == 1 && "Silverbull 股份".equals(shares.get(0).chinese()));

        report("空字串不列", m.suggestions("   ").isEmpty());

        MarketListener.searching(false);
        report("沒在市集裡時 active() 是 false", !MarketListener.active());
        MarketListener.searching(true);
        report("市集在等搜尋字時 active() 是 true", MarketListener.active());
        MarketListener.searching(false);
    }

    private static void simplified() {
        TranslationStore store = new TranslationStore();
        Path base = Path.of("src/main/resources/assets/wynnchayuan/translations");
        store.loadAll(List.of(base.resolve("zh_cn")));
        MarketSearch cn = store.market();
        TranslationStore twStore = new TranslationStore();
        twStore.loadAll(List.of(base.resolve(Languages.DEFAULT)));
        String tw = twStore.lookup("Dragon Aura");
        report("繁體有 Dragon Aura 的譯名（" + tw + "）", tw != null);
        report("★ 簡體不墊繁體：打繁體名字查不到（實際：" + (tw == null ? null : cn.lookup(tw)) + "）",
                tw != null && cn.lookup(tw) == null);
        String own = store.lookup("Dragon Aura");
        if (own != null) {
            turns(cn, own, "Dragon Aura");
        }
    }

    private static void quantityLines() {
        MarketSearch m = new MarketSearch();
        m.add("{~} x Dragon Aura {#}{#}{#}{#}", "{~} x 龙之灵气 {#}{#}{#}{#}");
        m.add("{~} x Doom Stone {#}{#}{#}", "{~} x 厄运之石 {#}{#}{#}");
        List<String> aura = m.candidates("龙之灵气");
        report("數量行裡的名字也收（實際：" + aura + "）",
                aura.equals(List.of("Dragon Aura")));
        turns(m, "厄运之石", "Doom Stone");

        MarketSearch odd = new MarketSearch();
        odd.add("{~} x Dragon Aura {#}{#}{#}{#}", "龙之灵气 ×{~}");
        report("只有一邊是數量行時不收（實際 " + odd.size() + " 條）", odd.size() == 0);
    }

    private static void latinNames() {
        MarketSearch m = new MarketSearch();
        m.add("Doom Stone", "Pierre du Destin");
        m.add("Cataratite", "Cataratite");
        report("拉丁字母的譯名整個對上才換",
                "Doom Stone".equals(m.exact("pierre du destin")));
        report("拉丁字母只打片段不換", m.exact("Pierre") == null);
        report("英文原名打進去不會對到東西", m.exact("Doom Stone") == null);
        report("跟原文一樣的不收", m.exact("Cataratite") == null && m.size() == 1);
    }

    private static com.wynntils.handlers.chat.event.ChatMessageEvent.Match chat(String text) {
        return new com.wynntils.handlers.chat.event.ChatMessageEvent.Match(
                com.wynntils.core.text.StyledText.fromString(text),
                com.wynntils.handlers.chat.type.RecipientType.INFO);
    }

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
