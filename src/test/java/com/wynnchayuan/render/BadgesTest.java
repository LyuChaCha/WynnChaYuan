package com.wynnchayuan.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.client.Credits;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class BadgesTest {

    private static final int GOLD = 0xFFD24A;
    private static final int CYAN = 0x5BD8E8;
    private static final int GREEN = 0x5BD86F;

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();

        List<Credits.Section> sections = Credits.read(new StringReader("""
                {"sections": [
                  {"role": "開發者", "key": "credits.role.developers", "color": "#FFD24A", "members": [
                    {"name": "Solo", "mc": "Solo_Dev"},
                    {"name": "Multi", "mc": "Multi_Role"},
                    {"name": "Shiny", "mc": "Shiny", "rainbow": true}]},
                  {"role": "贊助者", "key": "credits.role.sponsors", "color": "#5BD8E8", "members": [
                    {"name": "Multi", "mc": "Multi_Role", "color": "#FF66AA"},
                    {"name": "Pinky", "mc": "Pinky", "rainbow": true}]},
                  {"role": "貢獻者", "key": "credits.role.contributors", "color": "#5BD86F", "members": [
                    {"name": "Multi", "mc": "Multi_Role", "hide": true},
                    {"name": "Leafy", "mc": "Leafy"},
                    {"name": "Shy", "mc": "Shy_One", "hide": true},
                    {"name": "資料來源", "key": "credits.source.wiki"}]}
                ]}"""));
        Badges.load(sections);

        Path file = Files.createTempFile("wcy-badges", ".json");
        Files.deleteIfExists(file);
        CollectorConfig config = new CollectorConfig(file);

        Credits.Member sponsor = sections.get(1).members().get(0);
        Credits.Member hidden = sections.get(2).members().get(0);
        check("名單：登記的顏色讀得到", (sponsor.colour() & 0xFFFFFF) == 0xFF66AA);
        check("名單：沒登記顏色的是 0", sections.get(0).members().get(0).colour() == 0);
        check("名單：rainbow 與 hide 讀得到",
                sections.get(0).members().get(2).rainbow() && hidden.hidden() && !sponsor.hidden());
        check("名單：分區的代號取自 key 的最後一段", sections.get(0).id().equals("developers")
                && sections.get(1).id().equals("sponsors") && sections.get(2).id().equals("contributors"));
        List<Credits.Section> teamed = Credits.read(new StringReader("""
                {"sections": [{"role": "貢獻者", "key": "credits.role.contributors", "color": "#5BD86F", "members": [
                  {"name": "Poly", "mc": "Poly_Glot", "teams": ["zh_tw", "ja_jp"], "areas": ["translation", "testing"],
                   "color": "#123456", "hide": true, "cape": "poly.png"}]}]}"""));
        Credits.Member poly = teamed.get(0).members().get(0);
        check("名單：多了 teams 與 areas 兩欄照樣讀得進來，其他欄位不受影響",
                teamed.size() == 1 && teamed.get(0).members().size() == 1
                        && poly.label().equals("Poly") && poly.mc().equals("Poly_Glot")
                        && (poly.colour() & 0xFFFFFF) == 0x123456 && poly.hidden() && "poly.png".equals(poly.cape()));

        Component plain = Component.literal("[CHAMPION] ").withStyle(Style.EMPTY.withColor(
                        TextColor.fromRgb(0xFFAA00)))
                .append(Component.literal("Solo_Dev").withStyle(Style.EMPTY.withColor(
                        TextColor.fromRgb(0xFFFF55))));
        Component solo = Badges.decorate(plain, "Solo_Dev", "Someone", config, 0);
        check("開發者：名字前面一片金色的葉子", icons(solo).equals(List.of(GOLD)));
        check("開發者：葉子後面空一格，接著是原本的名牌",
                solo != null && solo.getString().equals(Badges.ICON + " [CHAMPION] Solo_Dev"));
        check("開發者：原本名牌的顏色沒有動",
                colours(solo).equals(List.of(GOLD, 0xFFAA00, 0xFFFF55)));
        check("已經接過葉子的名牌不會再接一片",
                Badges.decorate(solo, "Solo_Dev", "Someone", config, 0) == null);
        check("貢獻者是綠色", icons(Badges.decorate(Component.literal("Leafy"),
                "Leafy", "Someone", config, 0)).equals(List.of(GREEN)));

        Component multi = Badges.decorate(Component.literal("Multi_Role"),
                "Multi_Role", "Someone", config, 0);
        check("多重身分：照名單順序一人一片，登記成不展示的不畫；登記的顏色蓋過分區的",
                icons(multi).equals(List.of(GOLD, 0xFF66AA)));

        List<Integer> a = icons(Badges.decorate(Component.literal("Shiny"), "Shiny", "Someone", config, 0));
        List<Integer> same = icons(Badges.decorate(Component.literal("Shiny"), "Shiny", "Someone", config, 40));
        List<Integer> later = icons(Badges.decorate(Component.literal("Shiny"), "Shiny", "Someone", config, 1000));
        check("登記成彩色的開發者：顏色會隨時間變", a.size() == 1 && !a.equals(later));
        check("彩色：同一格時間裡顏色不變（不會每幀一個新名牌）", a.equals(same));
        check("彩色：轉一圈回到原色", a.equals(icons(Badges.decorate(
                Component.literal("Shiny"), "Shiny", "Someone", config, 3000))));
        check("贊助者登記了彩色也照登記的畫——名單是團隊自己寫的",
                !icons(Badges.decorate(Component.literal("Pinky"), "Pinky", "Someone", config, 0))
                        .equals(List.of(CYAN)));

        check("自己調顏色：馬上照自己選的畫", config.setBadgeColourLive("developers", "#123456")
                && icons(Badges.decorate(Component.literal("Solo_Dev"), "Solo_Dev", "solo_dev", config, 0))
                        .equals(List.of(0x123456)));
        check("別人看到的還是登記的顏色", icons(Badges.decorate(Component.literal("Solo_Dev"),
                "Solo_Dev", "Someone", config, 0)).equals(List.of(GOLD)));
        check("不是顏色的字不收，原本選的還在", !config.setBadgeColourLive("developers", "leaf")
                && "#123456".equalsIgnoreCase(config.badgeColour("developers")));
        config.clearBadgeColour("developers");
        check("清掉就回到登記的顏色", config.badgeColour("developers") == null
                && icons(Badges.decorate(Component.literal("Solo_Dev"), "Solo_Dev", "solo_dev", config, 0))
                        .equals(List.of(GOLD)));
        config.setBadgeColourLive("sponsors", "#ABCDEF");
        config.saveIfDirty();

        config.setBadgeRole("sponsors", false);
        config.setBadgeRole("contributors", true);
        config.setBadgeRainbow(true);
        check("自己：關掉贊助者、打開貢獻者，馬上照本機的選擇畫", icons(Badges.decorate(
                Component.literal("Multi_Role"), "Multi_Role", "multi_role", config, 0)).size() == 2
                && icons(Badges.decorate(Component.literal("Multi_Role"),
                        "Multi_Role", "multi_role", config, 0)).get(1) == GREEN);
        check("自己：開了彩色，開發者那片不再是金色", icons(Badges.decorate(
                Component.literal("Multi_Role"), "Multi_Role", "multi_role", config, 500)).get(0) != GOLD);
        com.wynnchayuan.Presence.BadgeChoice rainbowOn =
                new com.wynnchayuan.Presence.BadgeChoice(true, null);
        List<Integer> gold = icons(Badges.decorate(Component.literal("Solo_Dev"),
                "Solo_Dev", "Someone", config, 500, true));
        List<Integer> theirRainbow = icons(Badges.decorate(Component.literal("Solo_Dev"),
                "Solo_Dev", "Someone", config, 500, true, rainbowOn));
        check("★ 別的開發者開了彩色：我這邊看到的也不是金色", gold.size() == 1 && theirRainbow.size() == 1
                && !gold.equals(theirRainbow));
        check("★ 彩色是開發者的：貢獻者帶著「彩色」來也還是綠色",
                icons(Badges.decorate(Component.literal("Leafy"), "Leafy", "Someone", config, 500, true, rainbowOn))
                        .equals(icons(Badges.decorate(Component.literal("Leafy"), "Leafy", "Someone", config, 500, true))));
        List<Integer> registeredPair = icons(Badges.decorate(Component.literal("Multi_Role"),
                "Multi_Role", "Someone", config, 0, true));
        List<Integer> onlyOne = icons(Badges.decorate(Component.literal("Multi_Role"),
                "Multi_Role", "Someone", config, 0, true,
                new com.wynnchayuan.Presence.BadgeChoice(false, List.of("developers"))));
        check("★ 別人挑了只展示開發者那一片：我這邊也只畫那一片（登記的是 " + registeredPair.size() + " 片）",
                onlyOne.size() == 1 && registeredPair.size() >= 1);
        check("★ 挑了他沒有的身分：畫不出不屬於他的葉子，而且至少留一片",
                icons(Badges.decorate(Component.literal("Leafy"), "Leafy", "Someone", config, 0, true,
                        new com.wynnchayuan.Presence.BadgeChoice(false, List.of("developers")))).size() == 1
                        && icons(Badges.decorate(Component.literal("Leafy"), "Leafy", "Someone", config, 0, true,
                        new com.wynnchayuan.Presence.BadgeChoice(false, List.of("developers"))))
                        .equals(icons(Badges.decorate(Component.literal("Leafy"), "Leafy", "Someone", config, 0, true))));
        check("不在名單上的人帶著選擇來：還是只有灰色那一片",
                icons(Badges.decorate(Component.literal("Nobody"), "Nobody", "Someone", config, 0, true, rainbowOn))
                        .size() == 1);
        check("別人不受本機的選擇影響", icons(Badges.decorate(Component.literal("Multi_Role"),
                "Multi_Role", "Someone", config, 0)).equals(List.of(GOLD, 0xFF66AA)));
        check("自己是貢獻者：本機開了彩色也沒有——彩色是開發者的", icons(Badges.decorate(
                Component.literal("Leafy"), "Leafy", "Leafy", config, 500)).equals(List.of(GREEN)));
        config.setBadgeRole("developers", false);
        config.setBadgeRole("contributors", false);
        List<Integer> last = icons(Badges.decorate(
                Component.literal("Multi_Role"), "Multi_Role", "Multi_Role", config, 500));
        check("徽章不能整個關掉：身分全關還是留一片，而且是名單上第一個身分的（不是灰色）",
                last.size() == 1 && last.get(0) != Badges.INSTALLED && last.get(0) != 0xABCDEF);
        check("名單上唯一的身分登記成不展示：別人還是看得到那一片", icons(Badges.decorate(
                Component.literal("Shy_One"), "Shy_One", "Someone", config, 0)).equals(List.of(GREEN)));

        CollectorConfig again = new CollectorConfig(file);
        check("本機的選擇存檔再讀回來還在", !again.badgeRoleShown("sponsors", true)
                && !again.badgeRoleShown("developers", true) && again.badgeRainbow(false)
                && "#ABCDEF".equalsIgnoreCase(again.badgeColour("sponsors")));
        Path fresh = Files.createTempFile("wcy-badges-fresh", ".json");
        Files.deleteIfExists(fresh);
        CollectorConfig untouched = new CollectorConfig(fresh);
        untouched.toggleWynntilsUi();
        untouched.toggleWynntilsUi();
        JsonObject saved = JsonParser.parseString(
                Files.readString(fresh, StandardCharsets.UTF_8)).getAsJsonObject();
        check("沒動過的人設定檔裡不會多出徽章的三項",
                !saved.toString().contains("badgeRoles") && !saved.toString().contains("badgeRainbow")
                        && !saved.toString().contains("badgeColours"));
        check("沒動過就照名單上登記的", untouched.badgeRoleShown("x", true)
                && !untouched.badgeRoleShown("x", false) && !untouched.badgeRainbow(false));

        check("不在名單上的自己：灰色的葉子", icons(Badges.decorate(Component.literal("Nobody"),
                "Nobody", "nobody", untouched, 0)).equals(List.of(Badges.INSTALLED)));
        check("不在名單上的別人：沒有葉子", Badges.decorate(Component.literal("Nobody"),
                "Nobody", "Someone", untouched, 0) == null);
        check("沒有名牌就什麼都不做", Badges.decorate(null, "Solo_Dev", "Someone", untouched, 0) == null);

        check("★ 名單上的別人：灰色的葉子", icons(Badges.decorate(Component.literal("Nobody"),
                "Nobody", "Someone", untouched, 0, true)).equals(List.of(Badges.INSTALLED)));
        check("不在名單上的別人還是沒有", Badges.decorate(Component.literal("Stranger"),
                "Stranger", "Someone", untouched, 0, false) == null);
        check("有身分的人照舊是身分的顏色，不會多一片灰的", !icons(Badges.decorate(
                Component.literal("Solo_Dev"), "Solo_Dev", "Someone", untouched, 0, true))
                .contains(Badges.INSTALLED));

        check("帳號問不到：名牌文字裡有名單上的 ID 就認", icons(Badges.decorate(
                Component.literal("[VIP+] Solo_Dev [Lv. 105]"), null, "Someone", untouched, 0))
                .equals(List.of(GOLD)));
        check("ID 要是完整的詞：Solo_Dev2 不算", Badges.decorate(
                Component.literal("Solo_Dev2"), null, "Someone", untouched, 0) == null);

        try (InputStream in = Badges.class.getResourceAsStream("/assets/wynnchayuan/font/nametag.json")) {
            JsonObject font = JsonParser.parseReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject provider = font.getAsJsonArray("providers").get(0).getAsJsonObject();
            check("字型檔裡定義的就是程式用的那個碼位",
                    provider.getAsJsonArray("chars").get(0).getAsString().equals(Badges.ICON));
            check("字型檔指到的圖在 jar 裡", Badges.class.getResource(
                    "/assets/wynnchayuan/textures/font/nametag_leaf.png") != null
                    && provider.get("file").getAsString().equals("wynnchayuan:font/nametag_leaf.png"));
        }

        // Wynncraft 文字 shader 拿特定透明度當記號
        try (InputStream in = Badges.class.getResourceAsStream(
                "/assets/wynnchayuan/textures/font/nametag_leaf.png")) {
            java.awt.image.BufferedImage leaf = javax.imageio.ImageIO.read(in);
            int odd = 0;
            int solid = 0;
            for (int y = 0; y < leaf.getHeight(); y++) {
                for (int x = 0; x < leaf.getWidth(); x++) {
                    int argb = leaf.getRGB(x, y);
                    int alpha = argb >>> 24;
                    if (alpha == 255) {
                        solid++;
                        if ((argb & 0xFFFFFF) != 0xFFFFFF) {
                            odd++;
                        }
                    } else if (alpha != 0) {
                        odd++;
                    }
                }
            }
            check("葉子的圖沒有半透明的像素，實心的都是純白（有 " + odd + " 個不合）", odd == 0);
            check("葉子的圖不是空的", solid > 200);
        }

        Badges.reload();
        check("出貨的 credits.json 讀得進來，開發者有人", !Credits.sections().isEmpty()
                && !Credits.sections().get(0).members().isEmpty()
                && Credits.sections().get(0).id().equals("developers"));

        done();
    }

    private static List<Integer> icons(Component line) {
        List<Integer> out = new ArrayList<>();
        if (line == null) {
            return out;
        }
        line.visit((style, text) -> {
            if (Badges.FONT.equals(style.getFont())) {
                for (int i = 0; i < text.length(); i++) {
                    out.add(style.getColor() == null ? -1 : style.getColor().getValue());
                }
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static List<Integer> colours(Component line) {
        List<Integer> out = new ArrayList<>();
        if (line == null) {
            return out;
        }
        line.visit((style, text) -> {
            if (style.getColor() != null && !text.isEmpty()) {
                out.add(style.getColor().getValue());
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static void done() {
        System.out.println(failures == 0
                ? "Badges: 全部通過" : "Badges: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
