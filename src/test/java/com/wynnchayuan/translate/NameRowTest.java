package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 物品名稱那一列<b>不能</b>被語料裡的整列條目蓋掉。
 *
 * <h2>實機回報（2026-10-09）</h2>
 * F6 的物品名稱選「譯名加原文」，簡中：
 *
 * <pre>
 *   橡木匕首 (Oak Wood Dagger)      ← 對
 *   越相杖                          ← 少了 (Warp)
 *   静星耀杖                        ← 少了 (Halcyon)
 * </pre>
 *
 * <p>差別不在稀有度，在語料：{@code misc.json} 裡有
 * {@code "{#}Warp{#}": "{#}越相杖{#}"} 這種<b>整列</b>條目（收集端把名稱還沒翻的
 * 那一列當成缺口記下來，後來被照著補上）。整列模板的優先度高於逐片段，
 * 名稱就不再經過「名稱」那條路，那條路管的事（附原文、F6 關閉、按住 Shift）
 * 對這幾件全部失效。
 *
 * <h2>這裡釘住什麼</h2>
 * <ul>
 *   <li>有整列條目的名稱，三種模式都跟沒有整列條目的名稱一樣；</li>
 *   <li>整列條目的譯文跟原文一樣（收進來時還沒翻）時，裝備檔的譯名照樣出來；</li>
 *   <li>只是<b>長得像</b>的不受影響：Lootrun 的使命名 {@code {#}Redemption}；</li>
 *   <li>收集端不再把名稱列記成缺口。</li>
 * </ul>
 */
public final class NameRowTest {

    private static int failures = 0;

    /** 測試用的圖示字元（私用區，會被當成 {@code {#}}）。 */
    private static final String ICON = String.valueOf((char) 0xE000);

    private static final String TAIL = String.valueOf((char) 0xE001);

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        Path dir = Files.createTempDirectory("wynnchayuan-name-row");
        Files.writeString(dir.resolve("gear-weapon.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "w1": {"src": "Warp", "dst": "越相杖", "role": "name"},
                   "w2": {"src": "Oak Wood Dagger", "dst": "橡木匕首", "role": "name"},
                   "w3": {"src": "Bonder", "dst": "", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("gear-armour.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {
                   "a1": {"src": "Withdrawal", "dst": "戒斷", "role": "name"},
                   "a2": {"src": "Redemption", "dst": "救贖", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("ingredient.json"), """
                {"_meta": {"itemNames": true, "gearNames": false},
                 "entries": {
                   "i1": {"src": "Filched Purse", "dst": "扒來的錢包", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        // ★ 實機語料裡的那幾種整列條目，原樣照抄。
        Files.writeString(dir.resolve("misc.json"), """
                {"{#}Warp{#}": "{#}越相杖{#}",
                 "{#}{#}{#}{#}{#}Warp [{~}]": "{#}{#}{#}{#}{#}越相杖 [{~}]",
                 "{#}{#}{#}Withdrawal{#}": "{#}{#}{#}Withdrawal{#}",
                 "{#}{#}{#}{#}{#}{#}{#}Withdrawal": "{#}{#}{#}{#}{#}{#}{#}Withdrawal",
                 "{#}Bonder{#}": "{#}Bonder{#}",
                 "{#}{#}{#}{#}{#}Filched Purse": "{#}{#}{#}{#}{#}扒來的錢包",
                 "{#}Redemption": "{#}救贖使命",
                 "{#}{#}{#}Daily Reward{#}": "{#}{#}{#}每日獎勵{#}"}
                """, StandardCharsets.UTF_8);

        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        // ---- 外框認得對不對 ----
        same("第 0 行", "Warp", TranslationStore.nameRowInner("{#}Warp{#}"));
        same("已鑑定", "Warp", TranslationStore.nameRowInner("{#}{#}{#}{#}{#}Warp [{~}]"));
        same("三個圖示＋尾巴", "Withdrawal",
                TranslationStore.nameRowInner("{#}{#}{#}Withdrawal{#}"));
        same("七個圖示", "Hero", TranslationStore.nameRowInner("{#}{#}{#}{#}{#}{#}{#}Hero"));
        same("多字名稱", "Oak Wood Dagger",
                TranslationStore.nameRowInner("{#}{#}{#}{#}{#}Oak Wood Dagger [{~}]"));
        same("單一圖示開頭的不是名稱列", null, TranslationStore.nameRowInner("{#}Redemption"));
        same("裸名不是名稱列", null, TranslationStore.nameRowInner("Warp"));
        same("裡面還有佔位符的不是", null, TranslationStore.nameRowInner("{#}{#}{#}Lv. {~}{#}"));
        same("只有圖示的不是", null, TranslationStore.nameRowInner("{#}{#}{#}{#}"));

        // ---- ★ 回報的那一種：譯名加原文 ----
        store.setNameMode(com.wynnchayuan.CollectorConfig.ItemNames.BOTH);
        String warp0 = row(ICON, "Warp", TAIL, store);
        same("測試列的模板跟實機語料的鍵同形", "{#}Warp{#}", lastTemplate);
        check("★ 第 0 行附原文（實際 " + plain(warp0) + "）", warp0.contains("越相杖 (Warp)"));
        String warp1 = rolled("Warp", store);
        same("已鑑定那一列也同形", "{#}{#}{#}{#}{#}Warp [{~}]", lastTemplate);
        check("★ 看得見的那一行附原文（實際 " + plain(warp1) + "）",
                warp1.contains("越相杖 (Warp)"));
        String oak = rolled("Oak Wood Dagger", store);
        check("對照組：沒有整列條目的本來就附（實際 " + plain(oak) + "）",
                oak.contains("橡木匕首 (Oak Wood Dagger)"));
        check("原文只附一次（實際 " + plain(warp1) + "）",
                warp1.indexOf("(Warp)") == warp1.lastIndexOf("(Warp)"));

        // ---- ★ 整列條目的譯文跟原文一樣：裝備檔的譯名要出來 ----
        store.setNameMode(com.wynnchayuan.CollectorConfig.ItemNames.ON);
        String withdrawal = row(ICON.repeat(3), "Withdrawal", TAIL, store);
        same("三個圖示那一列也同形", "{#}{#}{#}Withdrawal{#}", lastTemplate);
        check("★ 整列條目留英文時，譯名照樣出來（實際 " + plain(withdrawal) + "）",
                withdrawal.contains("戒斷") && !withdrawal.contains("Withdrawal"));
        String warpOn = rolled("Warp", store);
        check("只給譯名的模式不附原文（實際 " + plain(warpOn) + "）",
                warpOn.contains("越相杖") && !warpOn.contains("(Warp)"));

        // ---- ★ F6 關掉物品名稱：這幾件也要留原文 ----
        store.setNameMode(com.wynnchayuan.CollectorConfig.ItemNames.OFF);
        String off = rolled("Warp", store);
        check("★ 關掉物品名稱時留原文（實際 " + plain(off) + "）",
                off.contains("Warp") && !off.contains("越相杖"));
        store.setNameMode(com.wynnchayuan.CollectorConfig.ItemNames.ON);

        // ---- ★ 按住 Shift 看素材原文 ----
        String purse = row(ICON.repeat(5), "Filched Purse", "", store);
        same("素材那一列也同形", "{#}{#}{#}{#}{#}Filched Purse", lastTemplate);
        check("素材名稱平常是譯名（實際 " + plain(purse) + "）", purse.contains("扒來的錢包"));
        store.setPeekPlainNames(true);
        String peeked = row(ICON.repeat(5), "Filched Purse", "", store);
        check("★ 按住 Shift 時素材名稱是原文（實際 " + plain(peeked) + "）",
                peeked.contains("Filched Purse"));
        store.setPeekPlainNames(false);

        // ---- 長得像但不是名稱列的，照舊 ----
        same("Lootrun 使命名不受影響", "{#}救贖使命", store.lookup("{#}Redemption"));
        same("裡面不是物品名稱的整列條目不受影響", "{#}{#}{#}每日獎勵{#}",
                store.lookup("{#}{#}{#}Daily Reward{#}"));

        // ---- 收集端：名稱列不是缺口 ----
        check("★ 沒翻的名稱那一列不記成缺口",
                store.hasTranslation("{#}{#}{#}{#}{#}Bonder [{~}]"));
        check("翻好的名稱那一列也不是缺口",
                store.hasTranslation("{#}{#}{#}Oak Wood Dagger{#}"));
        check("真的沒收過的整列還是缺口",
                !store.hasTranslation("{#}{#}{#}Never Seen Before{#}"));

        if (failures > 0) {
            System.err.println(failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("NameRowTest 全部通過");
        System.exit(0);
    }

    /** 已鑑定的那一行：五個圖示 + 名稱 + 鑑定度。 */
    private static String rolled(String name, TranslationStore store) {
        return row(ICON.repeat(5), name + " [58.2%]", "", store);
    }

    /**
     * 照實機的分段組一列：每個圖示各自一段、字型交錯（實機是
     * {@code language/wynncraft} 的位移字元夾著 {@code tooltip/emblem/*} 的圖），
     * 所以五個圖示就是五個 {@code {#}}。全部塞在同一段的話只會算成一個，
     * 模板對不上實機語料的鍵，測試就守不到東西。
     */
    private static String row(String before, String text, String after, TranslationStore store) {
        net.minecraft.network.chat.MutableComponent line = Component.empty();
        icons(line, before, 0);
        line.append(Component.literal(text).withStyle(s -> s.withColor(0xAA00AA)));
        icons(line, after, before.length());
        StyledText styled = StyledText.fromComponent(line);
        String template = com.wynnchayuan.capture.LineParts.of(styled).template();
        lastTemplate = template;
        Component out = LineTranslator.translate(styled, store);
        return out == null ? line.getString() : out.getString();
    }

    /** 上一列實際算出來的模板，拿來確認測試列跟實機語料的鍵同形。 */
    private static String lastTemplate = "";

    private static void icons(net.minecraft.network.chat.MutableComponent line, String icons,
            int offset) {
        for (int i = 0; i < icons.length(); i++) {
            String font = (offset + i) % 2 == 0
                    ? "minecraft:language/wynncraft" : "minecraft:tooltip/emblem/frame";
            net.minecraft.network.chat.Style style = net.minecraft.network.chat.Style.EMPTY
                    .withFont(new net.minecraft.network.chat.FontDescription.Resource(
                            net.minecraft.resources.Identifier.parse(font)));
            line.append(Component.literal(String.valueOf((char) (0xE000 + offset + i)))
                    .withStyle(style));
        }
    }

    /** 圖示字元印出來是亂碼，換成 # 好讀。 */
    private static String plain(String text) {
        StringBuilder sb = new StringBuilder();
        text.codePoints().forEach(
                cp -> sb.appendCodePoint(cp >= 0xE000 && cp <= 0xF8FF ? '#' : cp));
        return sb.toString();
    }

    private static void same(String what, String want, String got) {
        check(what + "（實際 " + got + "）", java.util.Objects.equals(want, got));
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) {
            failures++;
        }
    }
}
