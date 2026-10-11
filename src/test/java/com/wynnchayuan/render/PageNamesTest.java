package com.wynnchayuan.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.CollectorConfig.ItemNames;
import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.PageNames;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 內容名稱（任務、洞窟、探索點…）的三段式顯示：譯名／譯名加原文／保留原文。
 *
 * <h2>為什麼要有（使用者 2026-10-11）</h2>
 * 「Wynntils 頁面的名稱希望也可以像物品名稱一樣可以開啟完全取代、中英對照、關閉」。
 * 內容書卡片的標題是語料裡的整列條目（{@code Glacier Tunnels [Cave]} → 冰河隧道 [洞窟]），
 * 不經過物品名稱那條路，原本沒有任何設定管得到它。
 *
 * <h2>這裡釘住什麼</h2>
 * <ul>
 *   <li>卡片標題三種設定各自的樣子，而且名字與類型標籤的顏色都留著；</li>
 *   <li>設定一切換馬上生效——同一張卡不會被快取卡在上一種樣子；</li>
 *   <li>清單那一列的裸名同樣三種；</li>
 *   <li>不是內容名稱的東西不受影響：物品的說明、{@code + New Quest [X]} 這種訊息、
 *       沒翻到的名字。</li>
 * </ul>
 *
 * 用出貨的語料跑，不寫死譯名：標題該翻成什麼，問語料。
 */
public final class PageNamesTest {

    private static int failures = 0;

    private static final Style NAME = Style.EMPTY
            .withFont(new FontDescription.Resource(
                    Identifier.withDefaultNamespace("language/wynncraft")))
            .withColor(TextColor.fromRgb(0xFF8C19));
    private static final Style TAG = Style.EMPTY
            .withFont(new FontDescription.Resource(
                    Identifier.withDefaultNamespace("language/wynncraft")))
            .withColor(TextColor.fromRgb(0xAAAAAA));

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        Path dir = Path.of("src/main/resources/assets/wynnchayuan/translations", Languages.DEFAULT);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        // 從語料挑一張真的洞窟卡：名字有翻、沒有佔位符
        JsonObject misc = JsonParser.parseString(
                Files.readString(dir.resolve("misc.json"), StandardCharsets.UTF_8)).getAsJsonObject();
        String name = null;
        String local = null;
        String tag = null;
        for (String key : misc.keySet()) {
            String dst = misc.get(key).isJsonPrimitive() ? misc.get(key).getAsString() : "";
            if (key.endsWith(" [Cave]") && dst.endsWith("]") && dst.contains(" [")
                    && key.indexOf('{') < 0 && dst.indexOf('{') < 0) {
                String n = key.substring(0, key.length() - " [Cave]".length());
                String l = dst.substring(0, dst.lastIndexOf(" ["));
                if (!n.equals(l) && n.indexOf(' ') > 0 && misc.has(n)
                        && l.equals(misc.get(n).getAsString())) {
                    name = n;
                    local = l;
                    tag = dst.substring(dst.lastIndexOf(" ["));
                    break;
                }
            }
        }
        check("前提：語料裡找得到一張有譯名的洞窟卡（" + name + " → " + local + tag + "）",
                name != null);
        if (name == null) {
            done();
            return;
        }

        check("認得卡片標題", name.equals(PageNames.nameOfTitle(name + " [Cave]")));
        check("認得任務標題", "Cowfusion".equals(PageNames.nameOfTitle("Cowfusion [Quest]")));
        check("「+ New Quest [Cowfusion]」不是標題",
                PageNames.nameOfTitle("+ New Quest [Cowfusion]") == null);
        check("別種方括號不是標題",
                PageNames.nameOfTitle("Builder's Toolbelt [Region Tool]") == null);
        check("多行的不是標題", PageNames.nameOfTitle(name + " [Cave]\nCan be explored") == null);

        // ---- ★ 卡片：跟實機同一個入口 ----
        List<Component> card = card(name, "Cave");
        store.setPageNameMode(ItemNames.ON);
        List<Component> on = TooltipPanel.translateInPlace(card, store);
        check("★ 譯名：照舊（實際 " + text(on, 0) + "）", (local + tag).equals(text(on, 0)));

        store.setPageNameMode(ItemNames.BOTH);
        List<Component> both = TooltipPanel.translateInPlace(card, store);
        check("★ 譯名加原文：名字後面括號附原文，類型標籤照舊（實際 " + text(both, 0) + "）",
                (local + " (" + name + ")" + tag).equals(text(both, 0)));
        check("★ 附上去的原文跟名字同色，不是跟標籤同色",
                colourAt(both.get(0), local.length() + 3) == 0xFF8C19);
        check("類型標籤的顏色沒變",
                colourAt(both.get(0), text(both, 0).length() - 2) == 0xAAAAAA);
        check("卡片其餘各行不受影響", text(both, 2).equals(text(on, 2)));

        store.setPageNameMode(ItemNames.OFF);
        List<Component> off = TooltipPanel.translateInPlace(card, store);
        check("★ 保留原文：名字是英文，類型標籤照樣翻（實際 " + text(off, 0) + "）",
                (name + tag).equals(text(off, 0)));
        check("★ 留原文的名字整段是遊戲原本的樣式（字型與顏色）",
                NAME.equals(styleAt(off.get(0), 0)));
        check("卡片的內文照樣翻（設定只管名字）", text(off, 2).equals(text(on, 2)));

        store.setPageNameMode(ItemNames.ON);
        check("★ 切回譯名馬上生效，不會被快取卡在上一種",
                (local + tag).equals(text(TooltipPanel.translateInPlace(card, store), 0)));

        // ---- 不是內容名稱的不動 ----
        store.setPageNameMode(ItemNames.BOTH);
        List<Component> made = card("Zzyzx Made Up Place", "Cave");
        List<Component> madeOut = TooltipPanel.translateInPlace(made, store);
        check("沒翻到的名字不會被附上一份一樣的原文（實際 "
                        + (madeOut.isEmpty() ? "（原文不動）" : text(madeOut, 0)) + "）",
                madeOut.isEmpty() || !text(madeOut, 0).contains("(Zzyzx"));

        List<Component> chat = new ArrayList<>();
        chat.add(Component.literal("+ New Quest [Cowfusion]").withStyle(NAME));
        chat.add(Component.literal("Can be explored").withStyle(TAG));
        List<Component> chatOut = TooltipPanel.translateInPlace(chat, store);
        check("「+ New Quest [X]」那種列不會被附原文（實際 "
                        + (chatOut.isEmpty() ? "（原文不動）" : text(chatOut, 0)) + "）",
                chatOut.isEmpty() || !text(chatOut, 0).contains("("));

        // ---- ★ 清單那一列的裸名 ----
        check("前提：清單認得這個名字", store.isPageName(name));
        check("不相干的字不算", !store.isPageName("Can be explored"));
        Path cfgFile = Files.createTempDirectory("wcy-pagenames").resolve("config.json");
        CollectorConfig config = new CollectorConfig(cfgFile);
        StyledText row = StyledText.fromComponent(Component.literal(name).withStyle(NAME));

        store.setPageNameMode(ItemNames.ON);
        check("★ 清單・譯名（實際 " + plain(WynntilsText.screenText(row, config, store)) + "）",
                local.equals(plain(WynntilsText.screenText(row, config, store))));
        store.setPageNameMode(ItemNames.BOTH);
        check("★ 清單・譯名加原文（實際 " + plain(WynntilsText.screenText(row, config, store)) + "）",
                (local + " (" + name + ")").equals(plain(WynntilsText.screenText(row, config, store))));
        store.setPageNameMode(ItemNames.OFF);
        check("★ 清單・保留原文：原樣交回同一個物件",
                WynntilsText.screenText(row, config, store) == row);
        store.setPageNameMode(ItemNames.ON);
        check("★ 清單切回譯名馬上生效",
                local.equals(plain(WynntilsText.screenText(row, config, store))));

        // 不是內容名稱的字，三種設定都一樣
        StyledText other = StyledText.fromComponent(
                Component.literal("Can be explored").withStyle(TAG));
        String otherOn = plain(WynntilsText.screenText(other, config, store));
        store.setPageNameMode(ItemNames.BOTH);
        check("不是名稱的字不附原文（實際 " + plain(WynntilsText.screenText(other, config, store)) + "）",
                otherOn.equals(plain(WynntilsText.screenText(other, config, store))));
        store.setPageNameMode(ItemNames.OFF);
        check("不是名稱的字照樣翻",
                otherOn.equals(plain(WynntilsText.screenText(other, config, store))));

        // ---- 設定存得住 ----
        check("預設是譯名", config.pageNames() == ItemNames.ON);
        config.cyclePageNames(1);
        check("輪到下一格是譯名加原文", config.pageNames() == ItemNames.BOTH);
        check("重讀設定檔還在", new CollectorConfig(cfgFile).pageNames() == ItemNames.BOTH);

        done();
    }

    /** 內容書的一張卡：標題、空行、狀態、敘述。標題是兩段顏色。 */
    private static List<Component> card(String name, String type) {
        List<Component> out = new ArrayList<>();
        out.add(Component.empty()
                .append(Component.literal(name).withStyle(NAME))
                .append(Component.literal(" [" + type + "]").withStyle(TAG)));
        out.add(Component.literal("Can be explored").withStyle(TAG));
        out.add(Component.literal("Click to track").withStyle(NAME));
        return out;
    }

    private static String text(List<Component> lines, int at) {
        return at < lines.size() ? lines.get(at).getString() : "（沒有這一行）";
    }

    private static String plain(StyledText text) {
        return text.getStringWithoutFormatting();
    }

    private static Style styleAt(Component line, int index) {
        Style[] hit = {null};
        int[] at = {0};
        line.visit((style, part) -> {
            if (hit[0] == null && index < at[0] + part.length()) {
                hit[0] = style;
            }
            at[0] += part.length();
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return hit[0];
    }

    private static int colourAt(Component line, int index) {
        Style style = styleAt(line, index);
        return style == null || style.getColor() == null ? -1 : style.getColor().getValue();
    }

    private static void check(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    private static void done() {
        System.out.println(failures == 0
                ? "PageNames: 全部通過" : "PageNames: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
