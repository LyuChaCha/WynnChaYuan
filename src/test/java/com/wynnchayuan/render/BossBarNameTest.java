package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class BossBarNameTest {

    public static void main(String[] args) {
        Path root = Path.of("src/main/resources/assets/wynnchayuan/translations");
        TranslationStore store = new TranslationStore();
        store.loadAll(List.of(root.resolve("zh_tw")));

        Style red = Style.EMPTY.withColor(TextColor.fromRgb(0xFF5555));
        Style aqua = Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF));
        MutableComponent bar = Component.empty();
        bar.append(Component.literal("Bronchial - 113k❤ - ").withStyle(red));
        bar.append(Component.literal("Weak").withStyle(aqua));

        Component out = WynntilsBossBar.bossBarName(bar, store);
        String shown = out == null ? "(沒翻)" : out.getString();
        check("★ 名字換成名牌的譯名（實際 " + shown + "）",
              shown.startsWith("支氣管體 - 113k❤ - "));
        check("名字後面的血量與狀態照原樣", shown.endsWith("113k❤ - Weak"));
        String[] colour = {null};
        if (out != null) {
            out.visit((style, text) -> {
                if (text.contains("支氣管體")) {
                    colour[0] = String.valueOf(style.getColor());
                }
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
        check("名字那段保留原本的顏色（實際 " + colour[0] + "）",
              String.valueOf(red.getColor()).equals(colour[0]));

        MutableComponent grume = Component.empty();
        grume.append(Component.literal("Grume - 37.7k❤ - ").withStyle(red));
        Component g = WynntilsBossBar.bossBarName(grume, store);
        check("Grume → 凝塊（實際 " + (g == null ? "(沒翻)" : g.getString()) + "）",
              g != null && g.getString().startsWith("凝塊 - "));

        MutableComponent status = Component.empty();
        status.append(Component.literal("Grume - 37.7k❤ - ").withStyle(red));
        status.append(Component.literal("Weak ").withStyle(aqua));
        status.append(Component.literal("Dam Def").withStyle(red));
        Component w = WynntilsBossBar.bossBarWords(status, store);
        String ws = w == null ? "(沒翻)" : w.getString();
        check("★ 屬性克制換成易傷／增傷／防護（實際 " + ws + "）",
              ws.equals("Grume - 37.7k❤ - 易傷 增傷 防護"));
        check("一般語料不受影響：Def 不會在別處變成防護",
              !"防護".equals(store.lookup("Def")));
        check("查不到的名字回 null",
              WynntilsBossBar.bossBarName(Component.literal("Zzqxv - 5❤"), store) == null);
        check("沒有「 - 」的標題不動",
              WynntilsBossBar.bossBarName(Component.literal("Bronchial"), store) == null);

        Style dark = Style.EMPTY.withColor(TextColor.fromRgb(0xAA0000));
        Style shine = Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF55));
        String url = "PLAY.WYNNCRAFT.COM";
        CollectorConfig config;
        try {
            config = new CollectorConfig(java.nio.file.Files.createTempFile("bossbar", ".json"));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        boolean everyFrame = true;
        boolean coloursKept = true;
        String sample = null;
        int badFrame = -1;
        for (int i = 0; i <= url.length(); i++) {
            MutableComponent frame = Component.empty();
            frame.append(Component.literal("Streamer mode enabled ").withStyle(red));
            if (i > 0) {
                frame.append(Component.literal(url.substring(0, i - 1)).withStyle(dark));
                frame.append(Component.literal(url.substring(i - 1, i)).withStyle(shine));
            }
            frame.append(Component.literal(url.substring(Math.max(0, i))).withStyle(dark));
            Component t = WynntilsBossBar.bossBar(frame, config, store);
            String s = t.getString();
            if (sample == null || i == 5) {
                sample = s;
            }
            everyFrame &= s.equals(store.lookup("Streamer mode enabled " + url));
            boolean same = urlColours(t, url).equals(urlColours(frame, url));
            if (!same && badFrame < 0) {
                badFrame = i;
            }
            coloursKept &= same;
        }
        check("★ 實況模式每一幀都翻成同一句（第 5 幀 " + sample + "）", everyFrame);
        check("★ 網址每個字都是原本那一幀的顏色，亮光照掃（第一個不對的幀 " + badFrame + "）",
              coloursKept);
        check("查不到的整句回 null",
              WynntilsBossBar.bossBarPlain(Component.literal("Zzqxv qwerty"), store) == null);

        System.out.println(failures == 0 ? "boss bar 怪物名：全部通過"
                                         : "boss bar 怪物名：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static List<String> urlColours(Component c, String url) {
        List<String> perChar = new java.util.ArrayList<>();
        c.visit((style, text) -> {
            for (int k = 0; k < text.length(); k++) {
                perChar.add(String.valueOf(style.getColor()));
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        int from = Math.max(0, perChar.size() - url.length());
        return perChar.subList(from, perChar.size());
    }

}
