package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class FlowedBlockTest {

    private static final String NL = System.lineSeparator();

    private static final int BODY = 0xAAAAAA;

    private static final String[] LORE = {
            "This sacrificial kris, fallen from",
            "the peak of the Tower of",
            "Ascension, is completely hollow.",
            "To wield it is to invite to",
            "yourself the same hollowness. To",
            "become such an aberration, to",
            "unwrite yourself in pursuit of",
            "power...it must be fed of its",
            "wielder til only the barest",
            "existence remains.",
    };

    public static void main(String[] args) {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                            Languages.DEFAULT));

        check("掃描深度夠深（實際 " + store.maxBlockLines() + "）",
                store.maxBlockLines() >= LORE.length);

        String lore = joined(block(LORE), store);
        check("十行的敘述查得出來（實際："
                        + shorten(lore) + "）",
                lore != null && lore.startsWith("這把從"));
        check("被斷行切開的地名有填回去", lore != null
                && lore.contains("Tower of Ascension"));
        check("譯文裡不留 {p}", lore != null && !lore.contains("{p}"));

        check("湊不齊就不硬翻",
                LineTranslator.translateBlock(
                        block(java.util.Arrays.copyOf(LORE, 8)),
                        store, new boolean[8]) == null);

        List<Component> tip = new ArrayList<>();
        tip.add(plain("Oblivion"));
        tip.add(plain(""));
        for (String line : LORE) {
            tip.add(plain(line));
        }
        tip.add(plain(""));
        tip.add(plain("Combat Level Min: 101"));
        List<Component> out =
                com.wynnchayuan.render.TooltipPanel.translateLines(tip, store);
        String all = out.stream().map(Component::getString)
                .reduce("", (a, b) -> a + " " + b);
        check("放進整份 tooltip 也翻得出來（實際：" + shorten(all) + "）",
                all.contains("獻祭之刃"));

        String offset = joined(centred(LORE), store);
        check("每行開頭帶排版偏移也翻得出來（實際：" + shorten(offset) + "）",
                offset != null && offset.startsWith("這把從"));

        newLabels(store);
        shaman(store);
        divider(store);
        majorId(store);
        superconductor(store);
        napalm(store);
        wavebreak(store);
        starcrossed(store);
        raidPicker(store);
        grootslang(store);

        System.out.println(failures == 0
                ? "FlowedBlock: 全部通過" : "FlowedBlock: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void majorId(TranslationStore store) {
        Style pink = Style.EMPTY.withColor(TextColor.fromRgb(0xE0B3E6));
        Style grey = Style.EMPTY.withColor(TextColor.fromRgb(BODY));
        Style green = Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55));

        MutableComponent first = Component.empty();
        first.append(Component.literal(SpaceOffset.encode(2))
                .withStyle(SpaceOffset.styleFor(pink)));
        first.append(Component.literal("Efflorescence: ").withStyle(pink));
        first.append(Component.literal("Serpent's").withStyle(grey));

        MutableComponent last = Component.empty();
        last.append(Component.literal("all ").withStyle(grey));
        last.append(Component.literal("elements").withStyle(green));
        last.append(Component.literal(".").withStyle(grey));

        List<StyledText> run = new ArrayList<>();
        run.add(StyledText.fromComponent(first));
        for (String s : new String[] {
                "Garden now centers to where", "you land with Swan Dive.",
                "Damage of Swan Dive,", "Serpent's Garden and Jasmine",
                "Bloom are distributed across" }) {
            run.add(StyledText.fromComponent(plain(s)));
        }
        run.add(StyledText.fromComponent(last));

        List<Component> out = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        check("Major ID 的敘述翻得出來",
                out != null && !out.isEmpty());
        if (out == null) {
            return;
        }
        String all = out.stream().map(Component::getString)
                .reduce("", String::concat);
        String zh = store.lookup("Efflorescence");
        check("名稱與敘述都翻了（實際：" + shorten(all) + "）",
                zh != null && all.contains(zh) && all.contains("所有元素"));
        check("名稱保住自己的顏色", colourOf(out, zh) == 0xE0B3E6);
        check("末尾的元素保住原本的綠（實際："
                        + Integer.toHexString(colourOf(out, "元素")) + "）",
                colourOf(out, "元素") == 0x55FF55);
    }

    private static void superconductor(TranslationStore store) {
        Style pink = Style.EMPTY.withColor(TextColor.fromRgb(0xE0B3E6));
        Style grey = Style.EMPTY.withColor(TextColor.fromRgb(BODY));

        MutableComponent first = Component.empty();
        first.append(Component.literal(SpaceOffset.encode(2))
                .withStyle(SpaceOffset.styleFor(pink)));
        first.append(Component.literal("Superconductor: ").withStyle(pink));
        first.append(Component.literal("Chain").withStyle(grey));

        List<StyledText> run = new ArrayList<>();
        run.add(StyledText.fromComponent(first));
        for (String line : new String[] {
                "Lightning channels three",
                "massive lightning bolts",
                "through you, arcing to",
                "enemies within 20 blocks for",
                "500% and stunning them for",
                "3s. You take 30% more damage",
                "for 5s." }) {
            run.add(StyledText.fromComponent(plain(line)));
        }

        List<Component> out = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        check("Superconductor 的敘述翻得出來",
                out != null && !out.isEmpty());
        if (out == null) {
            return;
        }
        String all = out.stream().map(Component::getString)
                .reduce("", String::concat);
        String zh = store.lookup("Superconductor");
        check("名稱跟著翻（實際：" + shorten(all) + "）",
                zh != null && all.contains(zh));
        check("敘述也翻了", all.contains("閃電"));
    }

    private static void napalm(TranslationStore store) {
        Style pink = Style.EMPTY.withColor(TextColor.fromRgb(0xE0B3E6));
        Style grey = Style.EMPTY.withColor(TextColor.fromRgb(BODY));
        Style icon = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(
                net.minecraft.resources.Identifier.withDefaultNamespace("common")));

        MutableComponent first = Component.empty();
        first.append(Component.literal(SpaceOffset.encode(2))
                .withStyle(SpaceOffset.styleFor(pink)));
        first.append(Component.literal("Napalm: ").withStyle(pink));
        first.append(Component.literal("Fire Creep increases").withStyle(grey));

        MutableComponent byLine = Component.empty();
        byLine.append(Component.literal("Arrow Bomb damage by ").withStyle(grey));
        byLine.append(Component.literal(RULE).withStyle(icon));
        byLine.append(Component.literal("+15%,").withStyle(grey));

        MutableComponent last = Component.empty();
        last.append(Component.literal(RULE).withStyle(icon));
        last.append(Component.literal("+65%.").withStyle(grey));

        List<StyledText> run = new ArrayList<>();
        run.add(StyledText.fromComponent(first));
        run.add(StyledText.fromComponent(byLine));
        for (String line : new String[] {
                "but deals 15 times the",
                "self-damage and lowers",
                "velocity. Scorched Earth",
                "radius is increased and deals" }) {
            run.add(StyledText.fromComponent(plain(line)));
        }
        run.add(StyledText.fromComponent(last));

        check("最後一行的模板沒有字母（前提）",
                !com.wynnchayuan.capture.GlyphSplitter.hasLetter(
                        com.wynnchayuan.capture.LineParts.of(run.get(run.size() - 1)).template()));
        List<Component> out = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        check("Napalm 的敘述翻得出來", out != null && !out.isEmpty());
        if (out == null) {
            return;
        }
        String all = out.stream().map(Component::getString)
                .reduce("", String::concat);
        String zh = store.lookup("Napalm");
        check("名稱與敘述都翻了（實際：" + shorten(all) + "）",
                zh != null && all.contains(zh) && !all.contains("times the"));
    }

    private static void wavebreak(TranslationStore store) {
        Style pink = Style.EMPTY.withColor(TextColor.fromRgb(0xE0B3E6));
        Style grey = Style.EMPTY.withColor(TextColor.fromRgb(BODY));
        Style icon = Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(
                net.minecraft.resources.Identifier.withDefaultNamespace("common")));

        MutableComponent first = Component.empty();
        first.append(Component.literal(SpaceOffset.encode(2))
                .withStyle(SpaceOffset.styleFor(pink)));
        first.append(Component.literal("Wavebreak: ").withStyle(pink));
        first.append(Component.literal("Psychokinesis can").withStyle(grey));

        MutableComponent area = Component.empty();
        area.append(Component.literal(RULE).withStyle(icon));
        area.append(Component.literal("100% and ").withStyle(grey));
        area.append(Component.literal(RULE).withStyle(icon));
        area.append(Component.literal("50% area damage, as").withStyle(grey));

        MutableComponent direct = Component.empty();
        direct.append(Component.literal("well as ").withStyle(grey));
        direct.append(Component.literal(RULE).withStyle(icon));
        direct.append(Component.literal("200% and ").withStyle(grey));
        direct.append(Component.literal(RULE).withStyle(icon));
        direct.append(Component.literal("100% direct").withStyle(grey));

        List<Component> rows = new ArrayList<>();
        rows.add(first);
        rows.add(plain("bounce up to 5 times, dealing"));
        rows.add(area);
        rows.add(direct);
        rows.add(plain("hit damage, increasing by 10%"));
        rows.add(plain("after each bounce. Mana Bank"));
        rows.add(plain("gains 3 less mana from Meteor."));

        List<StyledText> run = new ArrayList<>();
        for (Component row : rows) {
            run.add(StyledText.fromComponent(row));
        }
        String zh = store.lookup("Wavebreak");
        String alone = joined(run, store);
        check("Wavebreak 的敘述翻得出來（實際：" + shorten(alone) + "）",
                alone != null && zh != null && alone.contains(zh)
                        && !alone.contains("bounce up"));

        List<Component> tip = new ArrayList<>();
        tip.add(plain("Mana Steal +15/3s"));
        tip.add(plain(""));
        tip.addAll(rows);
        tip.add(Component.literal("֎: ֎").withStyle(pink));
        tip.add(rule());
        tip.add(plain(""));
        String all = com.wynnchayuan.render.TooltipPanel.translateLines(tip, store).stream()
                .map(Component::getString).reduce("", (a, b) -> a + " " + b);
        check("放進整份 tooltip 也翻得出來（實際：" + shorten(all) + "）",
                zh != null && all.contains(zh) && !all.contains("bounce up"));

        String fallout = store.lookupFlat(
                "Bomb Arrow gains +{~} area, {#}+{~}, severely increased recoil, and {#}{~} self-damage.");
        check("Fallout 的新措辭查得到（實際：" + fallout + "）", fallout != null);
    }

    private static void starcrossed(TranslationStore store) {
        Style pink = Style.EMPTY.withColor(TextColor.fromRgb(0xE0B3E6));
        Style grey = Style.EMPTY.withColor(TextColor.fromRgb(BODY));

        MutableComponent first = Component.empty();
        first.append(Component.literal(SpaceOffset.encode(2))
                .withStyle(SpaceOffset.styleFor(pink)));
        first.append(Component.literal("Starcrossed: ").withStyle(pink));
        first.append(Component.literal("Haunting Memory").withStyle(grey));

        MutableComponent icons = Component.empty();
        for (int i = 0; i < 9; i++) {
            icons.append(rule());
        }

        List<Component> rows = new ArrayList<>();
        rows.add(first);
        for (String s : new String[] {
                "links up to 8 enemies", "together. While linked,",
                "enemies take +50% damage from", "Malediction and share this",
                "damage with every other", "linked enemy." }) {
            rows.add(plain(s));
        }

        String zh = store.lookup("Starcrossed");
        List<StyledText> run = new ArrayList<>();
        for (Component row : rows) {
            run.add(StyledText.fromComponent(row));
        }
        String alone = joined(run, store);
        check("Starcrossed 的敘述翻得出來（實際：" + shorten(alone) + "）",
                alone != null && zh != null && alone.contains(zh)
                        && !alone.contains("links up"));

        List<Component> tip = new ArrayList<>();
        tip.add(plain("Uproot Cost -12"));
        tip.add(plain(""));
        tip.addAll(rows);
        tip.add(icons);
        tip.add(plain(""));
        String all = com.wynnchayuan.render.TooltipPanel.translateLines(tip, store).stream()
                .map(Component::getString).reduce("", (a, b) -> a + " " + b);
        check("後面接著圖示列也翻得出來（實際：" + shorten(all) + "）",
                zh != null && all.contains(zh) && !all.contains("links up"));
    }

    private static void raidPicker(TranslationStore store) {
        String[] lore = {
                "Void Holes are strange... Are",
                "they natural? Or are they",
                "artificial? What makes them?",
                "What happens to the matter",
                "which they replace? A",
                "particularly large Void Hole",
                "rests within the centre of",
                "the Silent Expanse, perhaps",
                "it holds the answer to these",
                "questions..." };
        String[] req = {
                "In order to join this",
                "Raid you need to",
                "complete the following",
                "requirements" };

        String alone = joined(block(lore), store);
        check("敘述單獨查得到（實際：" + shorten(alone) + "）",
                alone != null && alone.contains("虛空洞"));

        List<Component> tip = new ArrayList<>();
        tip.add(offset("The Nameless Anomaly"));
        tip.add(offset(""));
        for (String line : lore) {
            tip.add(offset(line));
        }
        tip.add(offset(""));
        for (String line : req) {
            tip.add(offset(line));
        }
        tip.add(offset(""));
        tip.add(offset("\u2714 Combat Lv. Min: 103"));

        List<Component> out =
                com.wynnchayuan.render.TooltipPanel.translateLines(tip, store);
        String all = out.stream().map(Component::getString)
                .reduce("", (a, b) -> a + " " + b);
        check("整份 tooltip 裡的敘述也翻得出來（實際："
                        + shorten(all) + "）",
                all.contains("虛空洞"));
        check("入場條件那段也翻了",
                all.contains("要加入這場討伐戰"));
    }

    private static void grootslang(TranslationStore store) {
        String[] lore = {
                "Homes ravaged by Decay, the",
                "subterranean Grootslangs",
                "search frantically for a",
                "better habitat. Though they",
                "harbor no ill intent, they",
                "pose a significant risk for",
                "the people of Gelibord, and",
                "perhaps for all of Gavel." };

        String alone = joined(block(lore), store);
        check("整段單獨查得到（實際：" + shorten(alone) + "）",
                alone != null && alone.contains("地底"));

        String offset = joined(centred(lore), store);
        check("整段帶偏移也查得到（實際：" + shorten(offset) + "）",
                offset != null && offset.contains("地底"));
        check("帶偏移時地名也填回去了",
                offset != null && offset.contains("Gelibord"));

        List<Component> tip = new ArrayList<>();
        tip.add(offset("Nest of the Grootslangs"));
        tip.add(offset(""));
        for (String line : lore) {
            tip.add(offset(line));
        }
        List<Component> out =
                com.wynnchayuan.render.TooltipPanel.translateLines(tip, store);
        String all = out.stream().map(Component::getString)
                .reduce("", (a, b) -> a + " " + b);
        check("整份 tooltip 裡也翻得出來（實際："
                        + shorten(all) + "）",
                all.contains("地底"));
        check("tooltip 裡不留英文段落",
                !all.contains("subterranean"));
    }

    private static int colourOf(List<Component> out, String needle) {
        for (Component line : out) {
            for (Component part : flatten(line)) {
                if (part.getString().contains(needle)
                        && part.getStyle().getColor() != null) {
                    return part.getStyle().getColor().getValue();
                }
            }
        }
        return -1;
    }

    private static List<Component> flatten(Component c) {
        List<Component> out = new ArrayList<>();
        out.add(c);
        for (Component child : c.getSiblings()) {
            out.addAll(flatten(child));
        }
        return out;
    }

    private static void newLabels(TranslationStore store) {
        String[][] want = {
                {"POWDER SOCKETS", "粉末插槽"},
                {"Empty", "空"},
                {"SET", "套裝"},
                {"Average DPS", "平均 DPS"},
                {"Earth", "地屬性"},
                {"Thunder", "雷屬性"},
                {"Neutral", "無屬性"},
                {"This item has no available Powder Sockets", "這件物品沒有可用的粉末插槽"},
                {"No tales or legends have been recorded about this item",
                 "關於這件物品，沒有留下任何傳說或故事"},
                {"Equip more pieces of this set to unlock its bonus",
                 "裝備更多這套的部件即可解鎖套裝加成"},
        };
        for (String[] pair : want) {
            String got = store.lookup(pair[0]);
            check("查得到「" + pair[0] + "」（拿到：" + got + "）",
                    pair[1].equals(got));
        }

        TranslationStore.Term term = store.findTerm("Lv. 52 Fishing [34.57%]", 0);
        check("整段裡的專業名稱換得掉（拿到："
                        + (term == null ? "null" : term.translation()) + "）",
                term != null && "釣魚".equals(term.translation()));
        check("小寫的不換", store.findTerm("go fishing with me", 0) == null);
    }

    private static void shaman(TranslationStore store) {
        String[] src = {
                "When casting Uproot, instead",
                "wear one of the Mystic Masks.",
                "(Shift + Uproot to equip the previous mask)",
        };
        List<StyledText> run = block(src);
        List<Component> out = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        check("薩滿的三行敘述翻得出來", out != null && !out.isEmpty());
        if (out == null) {
            return;
        }
        String all = out.stream().map(Component::getString)
                .reduce("", String::concat);
        check("內容是譯文（實際：" + shorten(all) + "）",
                all.codePoints().anyMatch(cp ->
                        Character.UnicodeScript.of(cp)
                                == Character.UnicodeScript.HAN));

        check("譯文比原文少行 -> 要折",
                TextWrapper.needsWrap("一句話", 3, false));
        check("攤平查到的一律要折",
                TextWrapper.needsWrap("一句話", 1, true));
        check("行數一樣就不折——那是譯者排好的形狀",
                !TextWrapper.needsWrap("一" + NL + "二" + NL + "三", 3, false));
        check("比原文多行也不折——折了只會更長",
                !TextWrapper.needsWrap("一" + NL + "二" + NL + "三" + NL + "四",
                        3, false));
    }

    private static void divider(TranslationStore store) {
        List<Component> tip = new ArrayList<>();
        tip.add(plain("Halcyon"));
        tip.add(rule());
        for (StyledText line : centred(LORE)) {
            tip.add(line.getComponent().copy());
        }
        tip.add(rule());
        List<Component> out =
                com.wynnchayuan.render.TooltipPanel.translateLines(tip, store);
        String all = out.stream().map(Component::getString)
                .reduce("", (a, b) -> a + b);
        check("敘述有翻（實際：" + shorten(all) + "）", all.contains("獻祭之刃"));
        int rules = 0;
        for (Component line : out) {
            if (line.getString().contains(RULE)) {
                rules++;
            }
        }
        check("上下兩條分隔線都還在（實際 " + rules + " 條）", rules == 2);
    }

    private static final String RULE = "";

    private static Component rule() {
        return Component.literal(RULE).withStyle(
                Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(
                        net.minecraft.resources.Identifier.withDefaultNamespace("common"))));
    }

    private static List<StyledText> centred(String[] lines) {
        List<StyledText> run = new ArrayList<>(lines.length);
        for (String line : lines) {
            MutableComponent c = Component.empty();
            c.append(Component.literal(SpaceOffset.encode(12))
                    .withStyle(SpaceOffset.styleFor(Style.EMPTY)));
            c.append(plain(line));
            run.add(StyledText.fromComponent(c));
        }
        return run;
    }

    private static String joined(List<StyledText> run, TranslationStore store) {
        List<Component> out = LineTranslator.translateBlock(
                run, store, new boolean[run.size()]);
        return out == null ? null
                : out.stream().map(Component::getString).reduce("", String::concat);
    }

    private static List<StyledText> block(String[] lines) {
        List<StyledText> run = new ArrayList<>(lines.length);
        for (String line : lines) {
            run.add(StyledText.fromComponent(plain(line)));
        }
        return run;
    }

    private static MutableComponent offset(String text) {
        MutableComponent c = Component.empty();
        c.append(Component.literal(SpaceOffset.encode(12))
                .withStyle(SpaceOffset.styleFor(Style.EMPTY)));
        c.append(plain(text));
        return c;
    }

    private static MutableComponent plain(String text) {
        return Component.literal(text)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(BODY)));
    }

    private static String shorten(String text) {
        if (text == null) {
            return "null";
        }
        String flat = text.replace("\n", " ").strip();
        return flat.length() <= 34 ? flat : flat.substring(0, 34) + "…";
    }

}
