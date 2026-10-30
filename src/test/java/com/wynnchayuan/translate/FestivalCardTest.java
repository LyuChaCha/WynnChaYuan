package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class FestivalCardTest {

    private static int failures = 0;

    private static final Style GREY = Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA));

    public static void main(String[] args) {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        String[] facade = {
                "Special - False Facade",
                "- Using the Formless Heart, transform into",
                ">the most distant survivor. Upon disguising,",
                ">all survivors are inflicted with Paranoia.",
                "- Paranoia causes survivors' hearts to beat",
                ">whenever anyone approaches them within a",
                ">range of 16 blocks, fading over time if the",
                ">source is within line of sight or close by.",
                "- While disguised, your heartbeat radius is",
                ">reduced to 8 blocks, and you may enter",
                ">lockers or pretend to power Spirit Wells.",
                "- While disguised, your attack is delayed,",
                ">but will quickly reveal your true face",
                ">if it successfully hits a survivor.",
        };
        List<String> out = shown(card("The Hollowed", facade), store);
        allTranslated("惡靈類型的條列", out);
        check("四個條列項目各自成段（" + bullets(out) + " 個「- 」開頭）", bullets(out) == 4);
        check("第二項的 16 格還在", String.join(" ", out).contains("16"));
        check("第三項的 8 格還在", String.join(" ", out).contains("8"));

        String[] shade = {
                "Special - Stalking Shade",
                "- Use the Stalking Shade to enter the hunt,",
                ">removing your heartbeat radius.",
                "- While hunting, crouch while making direct",
                ">line of sight with a survivor to stalk them.",
                "- Stalking a survivor for 5s will make them",
                ">Vulnerable for 12s, causing them to be",
                ">instantly downed by your attacks.",
                "- Being spotted while hunting will begin to",
                ">expose you, which will slow and stun you if",
                ">completely exposed, and will end the hunt.",
        };
        out = shown(card("The Stalker", shade), store);
        allTranslated("續行大寫開頭的條列", out);
        String all = String.join(" ", out);
        check("盯梢 5 秒、脆弱 12 秒沒有對調（實際：" + all + "）",
                all.indexOf("5") >= 0 && all.indexOf("5") < all.indexOf("12"));

        String[] tunnel = {
                "After the final Spirit Well is powered,",
                "instantly lose the injured condition,",
                "gain Speed III for 10s, and move 15%",
                "quicker for the rest of the game.",
        };
        out = shown(card("Light in the Tunnel", tunnel), store);
        allTranslated("折斷的一句", out);

        String[] syndrome = {
                "While you are the last survivor alive,",
                "gain the following effects:",
                "- Your sprint trail lasts half as long.",
                "- Exit Gates are powered 25% faster.",
                "- The Escape Beacon glows when within 32 blocks.",
        };
        out = shown(card("Survivor Syndrome", syndrome), store);
        allTranslated("引言加單行條列", out);
        check("三個條列項目都在（" + bullets(out) + "）", bullets(out) == 3);

        System.out.println(failures == 0
                ? "FestivalCard: 全部通過" : "FestivalCard: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static List<Component> card(String title, String[] body) {
        List<Component> tip = new ArrayList<>();
        tip.add(Component.literal(title).withStyle(GREY));
        tip.add(Component.literal(""));
        for (String line : body) {
            if (!line.startsWith(">")) {
                tip.add(Component.literal(line).withStyle(GREY));
                continue;
            }
            MutableComponent row = Component.empty();
            row.append(Component.literal(SpaceOffset.encode(6))
                    .withStyle(SpaceOffset.styleFor(GREY)));
            row.append(Component.literal(line.substring(1)).withStyle(GREY));
            tip.add(row);
        }
        return tip;
    }

    private static List<String> shown(List<Component> tip, TranslationStore store) {
        List<Component> out = com.wynnchayuan.render.TooltipPanel.translateLines(tip, store);
        List<String> rows = new ArrayList<>();
        for (int i = 2; i < out.size(); i++) {
            StringBuilder plain = new StringBuilder();
            out.get(i).getString().codePoints()
                    .filter(cp -> cp < 0xE000 || (cp > 0xF8FF && cp < 0xF0000))
                    .forEach(plain::appendCodePoint);
            rows.add(plain.toString().strip());
        }
        return rows;
    }

    private static long bullets(List<String> rows) {
        return rows.stream().filter(r -> r.startsWith("- ")).count();
    }

    private static void allTranslated(String what, List<String> rows) {
        check(what + "：有翻出東西（" + rows.size() + " 列）", !rows.isEmpty());
        for (String row : rows) {
            boolean han = row.codePoints().anyMatch(cp -> cp >= 0x4E00 && cp <= 0x9FFF);
            boolean english = row.matches(".*[A-Za-z]{2,}[ ,]+[a-z]{2,}.*");
            check(what + "：這一列是譯文（實際：" + row + "）", han && !english);
        }
    }

    private static void check(String name, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + name);
        if (!ok) {
            failures++;
        }
    }
}
