package com.wynnchayuan.translate;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class LabelValueColourTest {

    private static final int LABEL = 0x00AAAA;
    private static final int VALUE = 0x55FFFF;
    private static final int WHITE = 0xFFFFFF;

    public static void main(String[] args) throws Exception {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        row(store, "數值比標籤長", new String[] {"Type: ", "Grinding Mobs"},
                new int[] {LABEL, VALUE},
                new String[] {"類型", ":", " 刷怪"},
                new int[] {LABEL, LABEL, VALUE});

        row(store, "標籤比數值長", new String[] {"World: ", "NA12"},
                new int[] {LABEL, VALUE},
                new String[] {"世界: ", "NA", "12"}, new int[] {LABEL, VALUE, VALUE});

        row(store, "數值是佔位符的照舊",
                new String[] {"- ", "Elapsed Time: ", "00:02"},
                new int[] {LABEL, LABEL, WHITE},
                new String[] {"- 已等待: ", "00", ":", "02"},
                new int[] {LABEL, WHITE, WHITE, WHITE});

        uniform(store, "整行同色不切", new String[] {"Type: ", "Grinding Mobs"},
                new int[] {VALUE, VALUE}, VALUE);

        colours(store, "括號註解不跟著數值變色",
                new String[] {"\uE000", " Total Damage: ", "2400%", " (of your DPS, Attack)"},
                new int[] {0xFF5555, GREY, WHITE, DARK},
                new String[] {"總傷害", "每秒傷害"}, new int[] {GREY, DARK});

        colours(store, "括號與標籤同色時不跟著數值變白",
                new String[] {"", " Area of Effect: ", "7", " Blocks ", "(Circle-Shaped)"},
                new int[] {0xFF5555, GREY, WHITE, WHITE, GREY},
                new String[] {"範圍", "格", "圓形"}, new int[] {GREY, WHITE, GREY});

        colours(store, "句首換色的詞保住自己的顏色",
                new String[] {"grant ", "2048", " Emeralds"},
                new int[] {PINK, WHITE, WHITE},
                new String[] {"給予", "綠寶石"}, new int[] {PINK, WHITE});

        colours(store, "數字兩邊混色的段落照抄時逐段貼色",
                new String[] {"✖", " Lv. 90: ", "+5 New Badges"},
                new int[] {0xFF5555, WHITE, GREY},
                new String[] {"✖", "Lv.", "新徽章"}, new int[] {0xFF5555, WHITE, GREY});

        colours(store, "同一段混色時照相同的標點切開對色",
                new String[] {"Expired", " - Sold ", "0", "/1 items"},
                new int[] {0xFFFF55, DARK, WHITE, DARK},
                new String[] {"已過期", "已售出"}, new int[] {0xFFFF55, DARK});

        colours(store, "照順序指名的數值也對得上",
                new String[] {"- 3 Rows", " (2stx Total)"},
                new int[] {GREY, DARK},
                new String[] {"排", "合計"}, new int[] {GREY, DARK});

        ranges();

        System.out.println(failures == 0
                ? "標籤數值顏色：全部通過"
                : "標籤數值顏色：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static final int GREEN = 0x55FF55;

    private static void ranges() throws Exception {
        Path zhTw = Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT);
        TranslationStore store = new TranslationStore();
        store.loadAll(List.of(zhTw, StatRowTest.corpus("wynnchayuan-range",
                "{~} to {~}", "{~} 到 {~}", "{~} tier", "{~} 階")));

        row(store, "範圍中間獨立的 to 換成連接詞",
                new String[] {"Walk Speed", "+2%", " to ", "+9%"},
                new int[] {LABEL, GREEN, GREY, GREEN},
                new String[] {"移動速度", "+2%", " 到 ", "+9%"},
                new int[] {LABEL, GREEN, GREY, GREEN});

        row(store, "攻速階級的 tier 與 to 都換掉",
                new String[] {"Attack Speed", "-60 tier", " to ", "-60 tier"},
                new int[] {LABEL, GREEN, GREY, GREEN},
                new String[] {"攻擊速度", "-60 階", " 到 ", "-60 階"},
                new int[] {LABEL, GREEN, GREY, GREEN});

        List<String> text = new ArrayList<>();
        List<Integer> got = new ArrayList<>();
        dump(store, List.of(
                line(new String[] {"Walk Speed", "+2%"}, new int[] {LABEL, GREEN}),
                line(new String[] {"Zorblax Frobnication", "+2%", " to ", "+9%"},
                     new int[] {LABEL, GREEN, GREY, GREEN})), text, got);
        check("標籤查不到的行，to 留英文（實際 " + text + "）",
                text.contains(" to ") && text.stream().noneMatch(t -> t.contains("到")));

        TranslationStore bare = new TranslationStore();
        bare.loadAll(StatRowTest.corpus("wynnchayuan-range-none", "Walk Speed", "移動速度"));
        row(bare, "沒有連接詞的語料留英文",
                new String[] {"Walk Speed", "+2%", " to ", "+9%"},
                new int[] {LABEL, GREEN, GREY, GREEN},
                new String[] {"移動速度", "+2%", " to ", "+9%"},
                new int[] {LABEL, GREEN, GREY, GREEN});
    }

    private static void row(TranslationStore store, String what,
                            String[] parts, int[] colours,
                            String[] wantText, int[] wantColours) {
        List<String> text = new ArrayList<>();
        List<Integer> got = new ArrayList<>();
        dump(store, parts, colours, text, got);

        List<Integer> want = new ArrayList<>();
        for (int c : wantColours) {
            want.add(c);
        }
        check(what + "：" + java.util.Arrays.toString(wantText) + " "
                        + hex(want) + "（實際 " + text + " " + hex(got) + "）",
                text.equals(List.of(wantText)) && got.equals(want));
    }

    private static void uniform(TranslationStore store, String what,
                                String[] parts, int[] colours, int want) {
        List<String> text = new ArrayList<>();
        List<Integer> got = new ArrayList<>();
        dump(store, parts, colours, text, got);
        boolean ok = !got.isEmpty();
        for (int c : got) {
            ok &= c == want;
        }
        check(what + "：整行都是 #" + Integer.toHexString(want).toUpperCase()
                        + "（實際 " + text + " " + hex(got) + "）", ok);
    }

    private static void dump(TranslationStore store, String[] parts, int[] colours,
                             List<String> text, List<Integer> got) {
        dump(store, List.of(line(parts, colours)), text, got);
    }

    private static Component line(String[] parts, int[] colours) {
        MutableComponent line = Component.empty();
        for (int i = 0; i < parts.length; i++) {
            line.append(Component.literal(parts[i])
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(colours[i]))));
        }
        return line;
    }

    private static void dump(TranslationStore store, List<Component> lines,
                             List<String> text, List<Integer> got) {
        for (Component c : com.wynnchayuan.render.TooltipPanel
                .translateLines(lines, store)) {
            c.visit((style, run) -> {
                text.add(run);
                got.add(style.getColor() == null ? -1 : style.getColor().getValue());
                return java.util.Optional.empty();
            }, Style.EMPTY);
        }
    }

    private static final int GREY = 0xAAAAAA;
    private static final int DARK = 0x555555;
    private static final int PINK = 0xFF55FF;

    private static void colours(TranslationStore store, String what,
                                String[] parts, int[] colours,
                                String[] probes, int[] wantColours) {
        List<String> text = new ArrayList<>();
        List<Integer> got = new ArrayList<>();
        dump(store, parts, colours, text, got);
        boolean ok = true;
        StringBuilder seen = new StringBuilder();
        for (int i = 0; i < probes.length; i++) {
            int found = -1;
            for (int k = 0; k < text.size(); k++) {
                if (text.get(k).contains(probes[i])) {
                    found = got.get(k);
                    break;
                }
            }
            ok &= found == wantColours[i];
            seen.append(probes[i]).append('=')
                .append(found < 0 ? "（找不到）" : String.format("#%06X", found)).append(' ');
        }
        check(what + "（實際 " + text + " " + seen.toString().strip() + "）", ok);
    }

    private static String hex(List<Integer> colours) {
        List<String> out = new ArrayList<>(colours.size());
        for (int c : colours) {
            out.add(c < 0 ? "（無色）" : "#" + String.format("%06X", c));
        }
        return out.toString();
    }

}
