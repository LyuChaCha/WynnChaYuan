package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class LabelColonTest {

    public static void main(String[] args) throws Exception {
        scopedLabel();
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));
        String glyph = com.wynnchayuan.capture.GlyphSplitter.GLYPH_PLACEHOLDER;

        check("帶冒號的鍵查得到", "傷害:".equals(store.lookup("Damage:")));
        check("單獨的標籤（拿到：[" + look("Damage: ", store, false) + "]）", "傷害: ".equals(look("Damage: ", store, false)));
        check("圖示 + 標籤", (glyph + " 傷害: ").equals(look(glyph + " Damage: ", store, false)));

        check("百分比的鍵存在", "火屬性傷害百分比:".equals(store.lookup("Fire Damage:%")));
        String pct = look("Fire Damage: ", store, true);
        check("百分比的行要用百分比的譯法（實際拿到：" + pct + "）",
                pct != null && pct.startsWith("火屬性傷害百分比:"));
        String raw = look("Fire Damage: ", store, false);
        check("非百分比的行用一般譯法（實際拿到：" + raw + "）",
                raw != null && raw.startsWith("火屬性傷害:") && !raw.contains("百分比"));

        for (String key : new String[] {"Damage: ", "Duration: ", "Cooldown: ", "Water: "}) {
            String got = look(glyph + " " + key, store, false);
            check(key.trim() + " 後面保留原文的空白（拿到：[" + got + "]）",
                    got != null && got.endsWith(" ") && !got.contains("："));
        }

        check("滿滾的物品名稱查得到（實際拿到："
                        + store.lookup("Perfect Ephemeral Tome of Mysticism II") + "）",
                "完美流光祕法書卷 II".equals(
                        look("Perfect Ephemeral Tome of Mysticism II", store, false)));
        check("最低滾的也查得到",
                look("Defective Ephemeral Tome of Mysticism II", store, false) != null);
        check("前綴後面不認得的名稱不硬翻",
                look("Perfect Nonexistent Item Name", store, false) == null);
        check("本來就叫 Perfect 的不受影響",
                java.util.Objects.equals(store.lookup("Perfect Recall"),
                        look("Perfect Recall", store, false)));

        for (String[] pair : new String[][] {
                {"Range:", "施放範圍"}, {"Total Damage:", "總傷害"},
                {"Duration:", "持續時間"}}) {
            String got = BlockTranslator.labelHead(glyph + " " + pair[0], store);
            check("只收帶冒號版本的標籤也查得到：" + pair[0] + "（拿到：[" + got + "]）",
                    got != null && got.contains(pair[1]));
        }
        check("不認得的標籤還是 null",
                BlockTranslator.labelHead(glyph + " Nonexistent Label:", store) == null);
        String head = BlockTranslator.labelHead(glyph + " Range:", store);
        check("名稱自帶冒號（" + head + "）", head != null && head.endsWith(":"));

        String search = look("- Name Contains: a", store, false);
        check("市集篩選只翻標籤、搜尋字留原文（拿到：[" + search + "]）",
                search != null && search.contains("名稱包含") && search.endsWith(" a"));
        String typed = look("- Name Contains: Insulators", store, false);
        check("查得到的搜尋字也留原文（拿到：[" + typed + "]）",
                typed != null && typed.contains("名稱包含") && typed.endsWith(" Insulators"));

        System.out.println(failures == 0
                ? "LabelColon: 全部通過" : "LabelColon: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static String look(String template, TranslationStore store, boolean percent) {
        return TemplateLookup.lookup(template, store, percent);
    }

    private static void scopedLabel() throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-scoped");
        Files.writeString(dir.resolve("gui.json"), "{\"Back\": \"返回\"}", StandardCharsets.UTF_8);
        Files.createDirectories(dir.resolve("scoped"));
        Files.writeString(dir.resolve("scoped/label.json"),
                "{\"_meta\": {\"scope\": \"label\"}, \"Back\": \"向後\", \"Left\": \"向左\"}",
                StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        check("一般查表：Back 還是返回", "返回".equals(store.lookup("Back")));
        check("scoped 的字不進一般語料", store.lookup("Left") == null);
        check("漂浮字專用：Back 是向後", "向後".equals(store.labelLookup("Back")));
        Component label = LineTranslator.translateLabel(StyledText.fromString("Back"), store);
        check("★ 地板上的 Back 畫成向後", label != null && "向後".equals(label.getString()));
        Component left = LineTranslator.translateLabel(StyledText.fromString("Left"), store);
        check("地板上的 Left 畫成向左", left != null && "向左".equals(left.getString()));
    }
}
