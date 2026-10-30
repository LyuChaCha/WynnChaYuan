package com.wynnchayuan.translate;

import com.google.gson.JsonObject;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class EventFrameTest {

    private static int failures = 0;

    private static final int CYAN = 0x00BDBF;
    private static final int GREY = 0xAAAAAA;
    private static final int PINK = 0xFF55FF;

    private static final String ICON = "󟿼󟿿󟿾";

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-eventframe");
        FlowedDebug.init(dir);
        TranslationStore store = corpus(dir.resolve("corpus"), true);

        timers(store);
        wraps(store);
        names(store);
        guards(store, corpus(dir.resolve("bare"), false));
        realCorpus();
        report();
    }

    private static void realCorpus() {
        System.out.println("=== 實際語料 ===");
        Path base = Path.of("src/main/resources/assets/wynnchayuan/translations");
        for (String lang : new String[] {"zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr"}) {
            TranslationStore real = new TranslationStore();
            real.loadAll(base.resolve(lang));
            for (int wrap = 0; wrap <= 3; wrap++) {
                for (String[] c : new String[][] {
                        {"Encroaching Destruction", "39s"}, {"Encroaching Destruction", "4m 7s"},
                        {"Zzyzx Hollow", "39s"}}) {
                    Component hit = LineTranslator.translateChat(event(c[0], c[1], "236", wrap), real);
                    String got = hit == null ? null : hit.getString().replace("\n", "⏎");
                    boolean english = c[0].startsWith("Zzyzx");
                    check(lang + "：" + c[0] + " " + c[1] + " 折法 " + wrap + "（實際 " + got + "）",
                          got != null && !got.contains("World Event") && !got.contains("Click to track")
                                  && got.contains("236") && got.contains(c[1].contains("m") ? "7" : "39")
                                  && got.contains("Zzyzx Hollow") == english && !got.contains("{"));
                }
            }
        }
    }

    private static TranslationStore corpus(Path dir, boolean frames) throws Exception {
        Files.createDirectories(dir);
        JsonObject misc = new JsonObject();
        if (frames) {
            misc.addProperty("{#} The {p} World Event starts in {~}m {~}s! ({~} blocks away) Click to track",
                    "{#} {c1}{p} 活動於 {~1} 分 {~2} 秒後揭幕！{/}{c2}(相距 {~3} 格) {/}{c3}點我追蹤{/}");
            misc.addProperty("{#} The {p} World Event starts in {~}s! ({~} blocks away) Click to track",
                    "{#} {c1}{p} 活動於 {~1} 秒後揭幕！{/}{c2}(相距 {~2} 格) {/}{c3}點我追蹤{/}");
        }
        misc.addProperty("Smoldering Ruin [World Event]", "悶燒廢墟 [世界活動]");
        misc.addProperty("Old Mill [World Event]", "舊磨坊 [世界活動]");
        misc.addProperty("{#} The Old Mill World Event starts in {~}m {~}s! ({~} blocks away) Click to track",
                "{#} {c1}舊鍵：舊磨坊 {~1} 分 {~2} 秒！{/}{c2}({~3} 格) {/}{c3}追蹤{/}");
        misc.addProperty("{#} The Old Mill World Event starts in {~}s! \n{#} ({~} blocks away) Click to track",
                "{#} {c1}舊折行鍵：舊磨坊 {~1} 秒！{/}\n{#} {c2}({~2} 格) {/}{c3}追蹤{/}");
        Files.writeString(dir.resolve("misc.json"), misc.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    private static MutableComponent icon() {
        return Component.literal(ICON).withStyle(Style.EMPTY
                .withColor(TextColor.fromRgb(CYAN))
                .withFont(new FontDescription.Resource(Identifier.parse("minecraft:chat/prefix"))));
    }

    private static MutableComponent lit(String text, int colour) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }

    private static StyledText event(String name, String timer, String blocks, int wrap) {
        MutableComponent all = Component.empty();
        all.append(icon());
        String head = " The " + name + " World Event starts in " + timer + "! ";
        if (wrap == 2) {
            all.append(lit(head + "\n", CYAN));
            all.append(icon());
            all.append(lit(" ", CYAN));
        } else {
            all.append(lit(head, CYAN));
        }
        if (wrap == 1) {
            all.append(lit("(" + blocks + "\n", GREY));
            all.append(icon());
            all.append(lit(" blocks away) ", GREY));
        } else if (wrap == 3) {
            all.append(lit("(" + blocks + " blocks\n", GREY));
            all.append(icon());
            all.append(lit(" away) ", GREY));
        } else {
            all.append(lit("(" + blocks + " blocks away) ", GREY));
        }
        all.append(Component.literal("Click to track").withStyle(
                Style.EMPTY.withColor(TextColor.fromRgb(PINK)).withUnderlined(true)));
        return StyledText.fromComponent(all);
    }

    private static void timers(TranslationStore store) {
        System.out.println("=== 兩種倒數 ===");
        Component secs = LineTranslator.translateChat(event("Smoldering Ruin", "39s", "236", 0), store);
        String got = secs == null ? null : secs.getString();
        check("★ 只剩秒數的那一種翻了（實際 " + got + "）",
              got != null && got.contains("悶燒廢墟 活動於 39 秒後揭幕！") && got.contains("(相距 236 格)")
                      && got.contains("點我追蹤") && !got.contains("World Event"));
        check("事件名的括號拿掉了", got != null && !got.contains("[世界活動]"));
        check("「點我追蹤」是粉紅加底線", secs != null && colourOf(secs, "點我追蹤") == PINK
                && underlined(secs, "點我追蹤"));
        check("距離是灰色", secs != null && colourOf(secs, "相距") == GREY);
        check("主句是青色", secs != null && colourOf(secs, "揭幕") == CYAN);

        Component both = LineTranslator.translateChat(event("Smoldering Ruin", "6m 58s", "608", 0), store);
        got = both == null ? null : both.getString();
        check("★ 分秒那一種也翻了（實際 " + got + "）",
              got != null && got.contains("悶燒廢墟 活動於 6 分 58 秒後揭幕！") && got.contains("(相距 608 格)"));

        Component general = LineTranslator.translate(event("Smoldering Ruin", "39s", "236", 0), store);
        check("一般翻譯那一支也認得（實際 " + (general == null ? null : general.getString()) + "）",
              general != null && general.getString().contains("悶燒廢墟 活動於 39 秒後揭幕！"));
    }

    private static void wraps(TranslationStore store) {
        System.out.println("=== 伺服器折行 ===");
        for (int wrap = 1; wrap <= 3; wrap++) {
            for (String timer : new String[] {"39s", "3m 59s"}) {
                Component hit = LineTranslator.translateChat(
                        event("Smoldering Ruin", timer, "426", wrap), store);
                String got = hit == null ? null : hit.getString().replace("\n", "⏎");
                String want = timer.contains("m") ? "活動於 3 分 59 秒後揭幕！" : "活動於 39 秒後揭幕！";
                check("★ 折法 " + wrap + "、倒數 " + timer + "（實際 " + got + "）",
                      got != null && got.contains("悶燒廢墟 " + want) && got.contains("相距 426 格"));
            }
        }
        for (int wrap = 0; wrap <= 3; wrap++) {
            Component hit = LineTranslator.translateChat(event("Old Mill", "39s", "236", wrap), store);
            String got = hit == null ? null : hit.getString().replace("\n", "⏎");
            String want = wrap == 2 ? "舊折行鍵：舊磨坊 39 秒！" : "舊磨坊 活動於 39 秒後揭幕！";
            check("★ 舊鍵只有另一種折法、這一行是折法 " + wrap + "（實際 " + got + "）",
                  got != null && got.contains(want) && got.contains("236"));
        }
        String wrapped = "{#} The Smoldering Ruin World Event starts in {~}s! ({~}\n"
                + "{#} blocks away) Click to track";
        check("★ 折過行的模板算翻了", store.hasTranslation(wrapped));
        check("沒折的模板算翻了", store.hasTranslation(
                "{#} The Smoldering Ruin World Event starts in {~}m {~}s! ({~} blocks away) Click to track"));
    }

    private static void names(TranslationStore store) {
        System.out.println("=== 事件名 ===");
        Component unknown = LineTranslator.translateChat(event("Mystery Glade", "12s", "90", 0), store);
        String got = unknown == null ? null : unknown.getString();
        check("★ 沒有譯名的事件：英文原名放進譯好的句型（實際 " + got + "）",
              got != null && got.contains("Mystery Glade 活動於 12 秒後揭幕！"));

        Component old = LineTranslator.translateChat(event("Old Mill", "2m 5s", "40", 0), store);
        got = old == null ? null : old.getString();
        check("★ 一個事件一條的舊鍵照樣優先（實際 " + got + "）",
              got != null && got.contains("舊鍵：舊磨坊 2 分 5 秒"));

        check("沒有頻道圖示的也認得", LineTranslator.translateChat(StyledText.fromString(
                "The Smoldering Ruin World Event starts in 39s! (236 blocks away) Click to track"),
                store) != null);
    }

    private static void guards(TranslationStore store, TranslationStore bare) {
        System.out.println("=== 不該動的不動 ===");
        check("句型沒翻：整句不動",
              LineTranslator.translateChat(event("Smoldering Ruin", "39s", "236", 0), bare) == null);
        check("句型沒翻：折過行的也不動",
              LineTranslator.translateChat(event("Smoldering Ruin", "39s", "236", 1), bare) == null);
        check("不是倒數的那一句不碰", LineTranslator.translateChat(StyledText.fromString(
                "The Smoldering Ruin World Event has ended! Click to track"), store) == null);
        check("事件名裡有數值的不碰（填回去會跟倒數搶順序）",
              LineTranslator.eventFrame("{#} The Mk{~} Ruin World Event starts in {~}s! "
                      + "({~} blocks away) Click to track", store) == null);
        check("沒有東西的不碰", LineTranslator.eventFrame(null, store) == null
                && LineTranslator.eventFrame("", store) == null);
    }

    private static int colourOf(Component whole, String piece) {
        int[] found = {-1};
        whole.visit((style, text) -> {
            if (found[0] < 0 && text.contains(piece) && style.getColor() != null) {
                found[0] = style.getColor().getValue();
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static boolean underlined(Component whole, String piece) {
        boolean[] found = {false};
        whole.visit((style, text) -> {
            if (text.contains(piece) && style.isUnderlined()) {
                found[0] = true;
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    private static void check(String what, boolean ok) {
        if (ok) {
            System.out.println("  [PASS] " + what);
        } else {
            System.out.println("  [FAIL] " + what);
            failures++;
        }
    }

    private static void report() {
        if (failures > 0) {
            System.out.println("世界事件句型：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("世界事件句型：全部通過");
    }
}
