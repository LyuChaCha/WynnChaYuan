package com.wynnchayuan.translate;

import com.google.gson.JsonObject;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 任務獎勵列少了行首的圖示（見 {@link LineTranslator#iconlessReward}）。
 *
 * <h2>先前是什麼樣子</h2>
 * 聊天現在送的是 {@code &d            - &7+1 Treasure Boots}，語料收的是帶圖示的
 * {@code {#}- +{~} Treasure Boots}——差一個圖示，整列英文。capture 一次收了七十八列，
 * 每一列帶圖示的那一條都在。
 *
 * <h2>這裡釘住什麼</h2>
 * <ol>
 *   <li>少了圖示的獎勵列照帶圖示那一條翻，縮排與顏色照舊；</li>
 *   <li>沒帶圖示的鍵照樣優先；帶圖示那一條的譯文不是以圖示開頭就不動；</li>
 *   <li>只認獎勵列——別的行少一個圖示照樣查不到（一般的查表沒有變鬆）；</li>
 *   <li>收集端問「翻了沒」時這種列算翻了；</li>
 *   <li>七個語言的實際語料：capture 裡的那幾列都翻得出來。</li>
 * </ol>
 *
 * <p>語料是測試自己寫的，措辭跟實際語料無關。
 */
public final class RewardRowTest {

    private static int failures = 0;

    private static final int PINK = 0xFF55FF;
    private static final int GREY = 0xAAAAAA;

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        Path dir = Files.createTempDirectory("wynnchayuan-reward-row");
        FlowedDebug.init(dir);
        TranslationStore store = corpus(dir.resolve("corpus"));

        composes(store);
        guards(store);
        realCorpus();
        report();
    }

    private static TranslationStore corpus(Path dir) throws Exception {
        Files.createDirectories(dir);
        JsonObject misc = new JsonObject();
        misc.addProperty("{#}- +{~} Treasure Boots", "{#}{c:#FF55FF}- {c:#AAAAAA}+{~} 測寶藏之靴");
        misc.addProperty("{#}- +Skeletal Legs", "{#}- +測骸骨護腿");
        misc.addProperty("{#}- +{~} Emeralds", "{#}- +{~} 測帶圖示的綠寶石");
        misc.addProperty("- +{~} Emeralds", "- +{~} 測不帶圖示的綠寶石");
        misc.addProperty("{#}- +{~} Odd Thing", "- +{~} 測圖示被搬走了{#}");
        misc.addProperty("{#}Treasure Map", "{#}測藏寶圖");
        Files.writeString(dir.resolve("misc.json"), misc.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    private static MutableComponent lit(String text, int colour) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(colour)));
    }

    /** 照 log 的分段：粉紅的縮排與破折號、灰色的獎勵。 */
    private static StyledText row(String reward) {
        return StyledText.fromComponent(Component.empty()
                .append(lit("            - ", PINK))
                .append(lit("+" + reward, GREY)));
    }

    private static void composes(TranslationStore store) {
        System.out.println("=== 少了行首圖示 ===");
        Component out = LineTranslator.translateChat(row("1 Treasure Boots"), store);
        String got = out == null ? null : out.getString();
        check("★ 照帶圖示那一條翻（實際 " + got + "）",
              got != null && got.strip().equals("- +1 測寶藏之靴") && !got.contains("Treasure"));
        check("破折號是粉紅色", out != null && colourOf(out, "-") == PINK);
        check("獎勵是灰色", out != null && colourOf(out, "測寶藏之靴") == GREY);

        Component bare = LineTranslator.translateChat(row("Skeletal Legs"), store);
        got = bare == null ? null : bare.getString();
        check("★ 沒有數量的獎勵（實際 " + got + "）", got != null && got.strip().equals("- +測骸骨護腿"));

        check("★ 收集端：這種列算翻了", store.hasTranslation("- +{~} Treasure Boots"));
        check("一般翻譯那一支也認得", LineTranslator.translate(row("1 Treasure Boots"), store) != null
                && LineTranslator.translate(row("1 Treasure Boots"), store).getString().contains("測寶藏之靴"));
    }

    private static void guards(TranslationStore store) {
        System.out.println("=== 不該動的不動 ===");
        Component plain = LineTranslator.translateChat(row("6 Emeralds"), store);
        check("★ 沒帶圖示的鍵照樣優先（實際 " + (plain == null ? null : plain.getString()) + "）",
              plain != null && plain.getString().contains("測不帶圖示的綠寶石"));
        check("帶圖示那一條的譯文不是以圖示開頭：不動",
              LineTranslator.iconlessReward("- +{~} Odd Thing", store) == null);
        check("不是獎勵列：少一個圖示照樣查不到（一般查表沒有變鬆）",
              LineTranslator.translateChat(StyledText.fromString("Treasure Map"), store) == null
                      && !store.hasTranslation("Treasure Map"));
        check("查不到的獎勵：整列不動", LineTranslator.translateChat(row("1 Mystery Box"), store) == null);
        check("多行的不碰", LineTranslator.iconlessReward("- +{~} Treasure Boots\n- +{~} Emeralds", store) == null);
        check("沒有東西的不碰", LineTranslator.iconlessReward(null, store) == null
                && LineTranslator.iconlessReward("", store) == null);
    }

    /** 七個語言：capture 裡的那幾列（quest-ui、dungeon、misc 三個檔各挑幾列）。 */
    private static void realCorpus() {
        System.out.println("=== 實際語料 ===");
        Path base = Path.of("src/main/resources/assets/wynnchayuan/translations");
        String[][] rows = {
                {"1 Breathing Helmet I", "Breathing Helmet"},
                {"1 Witherhead's Rib", "Witherhead's Rib"},
                {"Ragon's Bracelet", "Bracelet"},
                {"2 Yellow Crystal", "Yellow Crystal"},
                {"1 Infested Pit Key", "Pit Key"},
        };
        for (String lang : new String[] {"zh_tw", "zh_cn", "ja_jp", "ko_kr", "ru_ru", "es_es", "tr_tr"}) {
            TranslationStore real = new TranslationStore();
            real.loadAll(base.resolve(lang));
            for (String[] r : rows) {
                Component out = LineTranslator.translateChat(row(r[0]), real);
                String got = out == null ? null : out.getString();
                check(lang + "：" + r[0] + "（實際 " + got + "）",
                      got != null && got.contains("- +") && !got.contains(r[1]) && !got.contains("{"));
            }
        }
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
            System.out.println("任務獎勵列：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("任務獎勵列：全部通過");
    }
}
