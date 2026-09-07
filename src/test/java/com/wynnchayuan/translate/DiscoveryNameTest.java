package com.wynnchayuan.translate;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 隱藏探索點的名稱，每一條都要真的查得到。
 *
 * <h2>為什麼需要這條</h2>
 * 名稱裡只要含地名，實機的模板就<b>不是</b>那個名稱本身：{@code LineParts}
 * 會把地名換成 {@code {p}}，於是「Ragni's Secret Library」到了查表那一層
 * 是「{@code {p}'s Secret Library}」。照官方名稱一條一條寫下去，
 * 有地名的那 14 條會全部查不到——寫了等於沒寫，而且畫面上看起來就是「沒翻」。
 *
 * <p>所以這條測試不問「檔案裡有沒有這個鍵」，問的是<b>把名稱當成實機文字跑一遍
 * 之後查不查得到</b>。這樣新增名稱時，忘了補模板會立刻紅燈。
 *
 * <p>數字同理：三條 {@code Bak'al's Destruction 1/2/3} 收斂成同一個
 * {@code Bak'al's Destruction {~}}。
 *
 * <h2>名稱從哪來</h2>
 * 一律是 wiki {@code Secret_Discoveries} 索引頁上的官方名稱，不自己造。
 * 索引頁與條目頁大小寫／單複數不一致的那幾個（{@code Far From the Roots}、
 * {@code Messengers From Beyond}、{@code Ruler of the Skies}、
 * {@code Relos' Secret Library}）兩種都收，因為分不出遊戲送的是哪一種。
 */
public final class DiscoveryNameTest {

    private static final Path FILE = Path.of(
            "src/main/resources/assets/wynnchayuan/translations",
            Languages.DEFAULT, "discovery-name.json");

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        JsonObject root;
        try (BufferedReader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(r).getAsJsonObject();
        }

        int checked = 0;
        int viaTemplate = 0;
        for (String name : root.keySet()) {
            if (name.equals("_meta") || name.indexOf('{') >= 0) {
                continue;                      // 模板條目本身不用再跑一次
            }
            checked++;
            String template = com.wynnchayuan.capture.LineParts
                    .of(StyledText.fromComponent(Component.literal(name))).template();
            if (!template.equals(name)) {
                viaTemplate++;
            }
            String zh = LineTranslator.lookup(template, store, false);
            report("「" + name + "」查得到（模板 " + template + " → " + zh + "）",
                   zh != null && !zh.isBlank());
        }

        // 這兩個數字本身也要盯著：模板那條路如果哪天壞了（例如 places.json 清空），
        // 上面每一條都會「直接命中」而通過，測試就變成廢話。
        // 121 個官方名稱，加上索引頁與條目頁寫法不同的那三個備用拼法。
        // Ne du Valeos du Ellach 刻意不收（精靈語照原樣顯示），所以不在裡面。
        report("★ 檢查了 124 個名稱（實際 " + checked + "）", checked == 124);
        report("★ 其中有 17 個是靠地名／數字模板命中的（實際 " + viaTemplate + "）",
               viaTemplate == 17);

        // 譯名不能留成英文原樣——那是「補了但沒翻」，畫面上看不出差別。
        //
        // 這裡比對的是<b>查表層</b>的結果，地名還是 {p}：把實際的地名貼回去是
        // rebuild 那一層的事。所以問的是譯文的中文部分，不是完整的畫面文字。
        for (String[] pair : new String[][] {
                {"Ragni's Secret Library", "祕密圖書館"}, {"Historical Maltic", "往昔"},
                {"Bak'al's Destruction 2", "毀滅"}, {"The Twains' Downfall", "Twain"},
                {"Wynn Plains Monument", "紀念碑"}, {"Aldwell Library", "圖書館"}}) {
            String template = com.wynnchayuan.capture.LineParts
                    .of(StyledText.fromComponent(Component.literal(pair[0]))).template();
            String zh = LineTranslator.lookup(template, store, false);
            report("「" + pair[0] + "」譯文含「" + pair[1] + "」（實際 " + zh + "）",
                   zh != null && zh.contains(pair[1]));
        }

        descriptions(store);

        System.out.println(failures == 0
                ? "探索點：全部通過" : "探索點：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    /**
     * 敘述那一份（{@code discovery.json}）踩的是同一個坑。
     *
     * <h2>實測</h2>
     * 117 條裡有 <b>34 條</b>含地名或年份，實機的鍵是 {@code {p}}／{@code {~}} 版本，
     * 照 wiki 原文收的那一份永遠查不到——翻好的中文一直沒出現在畫面上，
     * 而且看起來就跟「還沒翻」一模一樣。
     *
     * <p>修法是<b>另外收一份參數化的鍵</b>，原文那一份留著：
     * {@code tools/fetch-discoveries.py} 重抓時要靠它比對 wiki 改過哪幾條。
     * 所以這裡只問「每一條敘述都查得到」，不管檔案裡有幾個鍵。
     */
    private static void descriptions(TranslationStore store) throws Exception {
        Path file = Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT, "discovery.json");
        JsonObject root;
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(r).getAsJsonObject();
        }
        int dead = 0;
        for (String key : root.keySet()) {
            if (key.startsWith("_") || key.indexOf('{') >= 0) {
                continue;                      // 參數化的那一份不用再跑一次
            }
            String template = com.wynnchayuan.capture.LineParts
                    .of(StyledText.fromComponent(Component.literal(key))).template();
            if (LineTranslator.lookup(template, store, false) == null) {
                dead++;
                report("敘述查不到：" + template.substring(0, Math.min(90, template.length())),
                       false);
            }
        }
        report("★ 沒有查不到的敘述（實際 " + dead + " 條）", dead == 0);
    }

    private static void report(String what, boolean ok) {
        if (!ok) {
            failures++;
            System.out.println("  [FAIL] " + what);
        }
    }
}
