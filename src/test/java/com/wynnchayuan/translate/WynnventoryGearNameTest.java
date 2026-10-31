package com.wynnchayuan.translate;

import com.wynnchayuan.ModConfig.ItemNames;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class WynnventoryGearNameTest {

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        Path dir = Files.createTempDirectory("wynnchayuan-wynnventory-gear");
        Files.createDirectories(dir.resolve("scoped"));
        Files.writeString(dir.resolve("gear-armour.json"), """
                {"_meta": {"domain": "gear-armour"},
                 "entries": {
                   "g1": {"src": "Dark Matter", "dst": "暗物質", "role": "name"},
                   "g2": {"src": "Aphotic", "dst": "無光", "role": "name"},
                   "g3": {"src": "Ruby", "dst": "", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("ingredient.json"), """
                {"_meta": {"domain": "ingredient", "itemNames": true, "gearNames": false},
                 "entries": {
                   "i1": {"src": "Dark Matter", "dst": "暗物質", "role": "name"},
                   "i2": {"src": "Ruby", "dst": "紅寶石", "role": "name"},
                   "i3": {"src": "Bob's Tear", "dst": "鮑伯之淚", "role": "name"}
                 }}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("scoped").resolve("gear.json"), """
                {"_meta": {"scope": "gear"}, "Dark Matter": "暗物質之盔"}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("scoped").resolve("wynnventory.json"), """
                {"_meta": {"scope": "wynnventory"},
                 "Trade Market Price Info": "交易市場價格資訊",
                 "%s in %s": "%s，在 %s"}
                """, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        store.setNameMode(ItemNames.OFF);
        check("關掉物品名稱：跟素材同名的裝備留英文", "Dark Matter".equals(panelName(store, "Dark Matter")));
        check("關掉物品名稱：一般裝備留英文", "Aphotic".equals(panelName(store, "Aphotic")));
        check("關掉物品名稱：素材照樣翻（不歸那個開關管）", "鮑伯之淚".equals(panelName(store, "Bob's Tear")));
        check("關掉物品名稱：收藏通知裡的裝備名也留英文",
                "Dark Matter，在 Corkus".equals(WynnventoryBridge.toast(store, "Dark Matter in Corkus")));
        check("關掉物品名稱：標題照樣翻",
                "交易市場價格資訊".equals(panelName(store, "Trade Market Price Info")));

        store.setNameMode(ItemNames.ON);
        check("翻譯物品名稱：跟素材同名的裝備用裝備自己的譯名",
                "暗物質之盔".equals(panelName(store, "Dark Matter")));
        check("翻譯物品名稱：一般裝備照譯", "無光".equals(panelName(store, "Aphotic")));
        check("翻譯物品名稱：沒翻的裝備不借素材的譯名", "Ruby".equals(panelName(store, "Ruby")));
        check("翻譯物品名稱：收藏通知用裝備自己的譯名",
                "暗物質之盔，在 Corkus".equals(WynnventoryBridge.toast(store, "Dark Matter in Corkus")));

        store.setNameMode(ItemNames.BOTH);
        List<Component> both = WynnventoryBridge.lines(store, List.of(
                Component.literal("Dark Matter").withStyle(ChatFormatting.RED)));
        check("譯名加原文：裝備自己的譯名，原文另起一行（實際 " + texts(both) + "）",
                both.size() == 2 && both.get(0).getString().equals("暗物質之盔")
                        && both.get(1).getString().strip().equals("(Dark Matter)"));

        check("查完不留名稱列的旗標", !TranslationStore.NAME_LINE.get() && !TranslationStore.HOLD_APPENDED.get());

        TranslationStore bundled = new TranslationStore();
        bundled.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations")
                .resolve(Languages.DEFAULT));
        bundled.setNameMode(ItemNames.OFF);
        check("內建語料、關掉物品名稱：Dark Matter 留英文",
                "Dark Matter".equals(panelName(bundled, "Dark Matter")));
        bundled.setNameMode(ItemNames.ON);
        String gear = bundled.gearOwnName("Dark Matter");
        check("內建語料、翻譯物品名稱：Dark Matter 用頭盔自己的譯名（實際 "
                        + panelName(bundled, "Dark Matter") + "）",
                gear != null && gear.equals(panelName(bundled, "Dark Matter")));

        System.out.println(failures == 0
                ? "Wynnventory 物品名照名稱列規則：全部通過"
                : "Wynnventory 物品名照名稱列規則：" + failures + " 項失敗");
        System.exit(failures > 0 ? 1 : 0);
    }

    private static String panelName(TranslationStore store, String name) {
        List<Component> out = WynnventoryBridge.lines(store, List.of(
                Component.literal(name).withStyle(ChatFormatting.RED)));
        return out.size() == 1 ? out.get(0).getString() : texts(out);
    }

    private static String texts(List<Component> lines) {
        return lines.stream().map(Component::getString).toList().toString();
    }

}
