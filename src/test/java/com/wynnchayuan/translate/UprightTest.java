package com.wynnchayuan.translate;

import com.wynnchayuan.render.TooltipPanel;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class UprightTest {

    private static final int SLANT_WHITE = 0x00EB34;
    private static final int SLANT_WHITE_PROGRESS = 0x7FEB34;
    private static final int SLANT_CYAN = 0x00F01C;
    private static final int SLANT_GREY = 0x00F020;
    private static final int SCALE_MOVEMENT = 0x00EB1C;

    private static final Map<Integer, Integer> PLAIN = Map.of(
            SLANT_WHITE, 0xFFFFFF,
            SLANT_WHITE_PROGRESS, 0xFFFFFF,
            SLANT_CYAN, 0x55FFFF,
            SLANT_GREY, 0xC6C6C6);

    private static final String LINE = "Walking through the ruins";
    private static final String BLOCK_TOP = "The walls were cold";
    private static final String BLOCK_END = "and the air was still";
    private static final String NEWLINE = System.lineSeparator();

    private static final Map<String, String> SAMPLE = Map.of(
            "zh_tw", "走過廢墟",
            "zh_cn", "走过废墟",
            "ja_jp", "廃墟を歩く",
            "ko_kr", "폐허를 걷는다",
            "ru_ru", "Иду по руинам",
            "es_es", "Cruzo las ruinas",
            "tr_tr", "Harabelerden geçiyorum");

    public static void main(String[] args) throws IOException {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        Style title = Style.EMPTY
                .withColor(TextColor.fromRgb(0xFFFFFF))
                .withItalic(true)
                .withBold(true);
        Component out = LineTranslator.translate(
                StyledText.fromComponent(
                        Component.literal("Your Islands").withStyle(title)),
                store, false);
        check("標題翻得出來", out != null && out.getString().contains("你的島嶼"));
        if (out != null) {
            check("中文不再是斜的", !italicOf(out, "你的島嶼"));
            check("粗體照舊——只丟斜體", boldOf(out, "你的島嶼"));
        }

        MutableComponent mixed = Component.empty();
        mixed.append(Component.literal("Ragni Citizen").withStyle(title));
        Component both = LineTranslator.translate(
                StyledText.fromComponent(mixed), store, false);
        check("混排的那一行翻得出來",
                both != null && both.getString().contains("市民"));
        if (both != null) {
            check("中文那半不斜", !italicOf(both, "市民"));
            check("英文那半跟著一起拉正（同一行不能半斜半正）", !italicOf(both, "Ragni"));
        }

        Component english = Component.literal("Untradable").withStyle(title);
        check("純英文行本身仍然是斜的（確認測試有意義）",
              italicOf(english, "Untradable"));
        Component flat = LineTranslator.unslantAll(english);
        check("整塊拉正時純英文行也拉正", !italicOf(flat, "Untradable"));

        Component han = Component.literal("無法交易").withStyle(title);
        check("中文照樣拉正", !italicOf(LineTranslator.unslantAll(han), "無法交易"));
        check("粗體不受影響——只丟斜體",
              boldOf(LineTranslator.unslantAll(han), "無法交易"));

        for (String script : List.of("みちをあるく", "ミチヲアルク", "폐허를 걷는다")) {
            check("假名與諺文跟漢字一樣拉正：" + script,
                  !italicOf(DisplayStyles.unslant(Component.literal(script).withStyle(title)),
                            script));
        }
        check("俄文照舊斜著", italicOf(DisplayStyles.unslant(
                Component.literal("Иду").withStyle(title)), "Иду"));

        for (Map.Entry<String, String> sample : SAMPLE.entrySet()) {
            shaderSlant(sample.getKey(), sample.getValue());
        }
        untouchedColours();

        System.out.println(failures == 0
                ? "Upright: 全部通過" : "Upright: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void shaderSlant(String lang, String shown) throws IOException {
        TranslationStore store = storeWith(LINE, shown,
                BLOCK_TOP + NEWLINE + BLOCK_END, shown + NEWLINE + shown);
        for (int marker : PLAIN.keySet()) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.addAll(TooltipPanel.translateInPlace(List.of(tinted(LINE, marker)), store));
            tooltip.addAll(TooltipPanel.translateInPlace(
                    List.of(tinted(BLOCK_TOP, marker), tinted(BLOCK_END, marker)), store));
            String label = lang + " " + Integer.toHexString(marker);
            check(label + " 懸浮提示換成一般顏色，正體",
                  tooltip.size() == 3 && tooltip.stream().allMatch(
                          line -> flat(line, PLAIN.get(marker), false)));

            boolean cjk = !shown.matches(".*[a-zA-Z\u0400-\u04ff].*");
            List<Component> line = new ArrayList<>();
            line.add(LineTranslator.translate(
                    StyledText.fromComponent(tinted(LINE, marker)), store, false));
            line.add(LineTranslator.translateChunk(LINE, Style.EMPTY.withColor(marker), store));
            line.add(DisplayStyles.unslant(tinted(shown, marker)));
            check(label + " 單行換成一般顏色，" + (cjk ? "方塊字正體" : "拼音文字用引擎的斜體"),
                  line.stream().allMatch(each -> flat(each, PLAIN.get(marker), !cjk)));
        }
    }

    private static boolean flat(Component line, int plain, boolean italic) {
        boolean[] ok = {line != null};
        if (line != null) {
            line.visit((style, text) -> {
                if (!text.isBlank()) {
                    ok[0] &= style.getColor() != null && style.getColor().getValue() == plain
                            && style.isItalic() == italic;
                }
                return Optional.empty();
            }, Style.EMPTY);
        }
        return ok[0];
    }

    private static void untouchedColours() throws IOException {
        TranslationStore store = storeWith(LINE, SAMPLE.get("zh_tw"));
        check("整份沒翻的提示留給原文",
              TooltipPanel.translateInPlace(List.of(tinted("Not in the corpus", SLANT_WHITE)),
                                            store).isEmpty());
        Component scale = LineTranslator.translate(
                StyledText.fromComponent(tinted(LINE, SCALE_MOVEMENT)), store, false);
        check("其他 shader 記號不動（縮放動畫）",
              scale != null && scale.toFlatList(Style.EMPTY).stream().anyMatch(
                      part -> part.getStyle().getColor() != null
                              && part.getStyle().getColor().getValue() == SCALE_MOVEMENT));
    }

    private static TranslationStore storeWith(String... pairs) throws IOException {
        com.google.gson.JsonObject corpus = new com.google.gson.JsonObject();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            corpus.addProperty(pairs[i], pairs[i + 1]);
        }
        Path dir = Files.createTempDirectory("wynnchayuan-upright");
        Files.writeString(dir.resolve("misc.json"), corpus.toString());
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        return store;
    }

    private static Component tinted(String text, int rgb) {
        return Component.literal(text).withStyle(Style.EMPTY.withColor(rgb));
    }

    private static boolean italicOf(Component out, String needle) {
        Component part = find(out, needle);
        return part != null && part.getStyle().isItalic();
    }

    private static boolean boldOf(Component out, String needle) {
        Component part = find(out, needle);
        return part != null && part.getStyle().isBold();
    }

    private static Component find(Component out, String needle) {
        for (Component part : flatten(out)) {
            if (part.getContents().toString().contains(needle)) {
                return part;
            }
        }
        return null;
    }

    private static List<Component> flatten(Component c) {
        List<Component> out = new ArrayList<>();
        out.add(c);
        for (Component child : c.getSiblings()) {
            out.addAll(flatten(child));
        }
        return out;
    }

}
