package com.wynnchayuan.client;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.listener.CaptureProbe;
import com.wynnchayuan.listener.ChatBlock;
import com.wynnchayuan.listener.ChatListener;
import com.wynnchayuan.translate.Languages;
import com.wynnchayuan.translate.SpaceOffset;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.chat.event.ChatMessageEvent;
import com.wynntils.handlers.chat.type.RecipientType;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class ChatPillTest {

    private record Glyph(int cp, Style style) {}

    private static final int LETTERS = "WYNNCHAYUAN".length();

    public static void main(String[] args) throws Exception {
        glyphsSpellTheLabel();
        backAndFrontLineUp();
        gradientRunsAcrossTheStops();
        coloursAreNotShaderMarkers();
        fallsBackToPlainText();
        survivesStyledText();
        ourMessageSkipsTranslationAndCapture();
        languageFilesCarryNoPrefix();
        if (failures > 0) {
            System.out.println("聊天小框：" + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("聊天小框：全部通過");
    }

    private static List<Glyph> glyphs(Component component) {
        List<Glyph> out = new ArrayList<>();
        component.visit((style, text) -> {
            text.codePoints().forEach(cp -> out.add(new Glyph(cp, style)));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static List<Glyph> pill() {
        return glyphs(ChatPill.prefix(ChatPill.WIDTH));
    }

    private static boolean isBack(int cp) {
        return cp >= 0xE030 && cp <= 0xE04F;
    }

    private static boolean isFront(int cp) {
        return cp >= 0xE000 && cp <= 0xE01F;
    }

    private static boolean isEdge(int cp) {
        return cp == 0xE060 || cp == 0xE062;
    }

    private static int advance(int cp) {
        if (SpaceOffset.isOffset(cp)) {
            return cp - 0xD0000;
        }
        if (isBack(cp)) {
            return 7;
        }
        if (isFront(cp)) {
            return 6;
        }
        return cp == 0xE060 ? 3 : 2;
    }

    private static void glyphsSpellTheLabel() {
        StringBuilder back = new StringBuilder();
        StringBuilder front = new StringBuilder();
        for (Glyph g : pill()) {
            if (isBack(g.cp())) {
                back.append((char) ('A' + g.cp() - 0xE030));
            } else if (isFront(g.cp())) {
                front.append((char) ('A' + g.cp() - 0xE000));
            }
        }
        check("底層字母拼出標籤（實際 " + back + "）", "WYNNCHAYUAN".contentEquals(back));
        check("字母層拼出標籤（實際 " + front + "）", "WYNNCHAYUAN".contentEquals(front));
        check("全部用 banner/pill 字型", pill().stream().allMatch(g ->
                g.style().getFont().toString().contains("banner/pill")));
    }

    private static void backAndFrontLineUp() {
        int x = 0;
        List<Integer> back = new ArrayList<>();
        List<Integer> front = new ArrayList<>();
        for (Glyph g : pill()) {
            if (isBack(g.cp())) {
                back.add(x);
            } else if (isFront(g.cp())) {
                front.add(x);
            }
            x += advance(g.cp());
        }
        check("每個字母疊在自己的底上（底 " + back + " 字 " + front + "）", back.equals(front));
        check("總寬等於 WIDTH（實際 " + x + "）", x == ChatPill.WIDTH);
        check("沒有普通空格，長的中文句子不會把小框單獨留在第一行",
                pill().stream().noneMatch(g -> g.cp() == ' '));
    }

    private static List<Integer> backColours() {
        List<Integer> out = new ArrayList<>();
        for (Glyph g : pill()) {
            if ((isBack(g.cp()) || isEdge(g.cp())) && g.style().getColor() != null) {
                out.add(g.style().getColor().getValue());
            }
        }
        return out;
    }

    private static void gradientRunsAcrossTheStops() {
        List<Integer> colours = backColours();
        check("左緣、字母、右緣都有顏色（實際 " + colours.size() + "）", colours.size() == LETTERS + 2);
        check("起點是 logo 深綠", colours.get(0) == 0x3A9F4C);
        check("中點是 logo 主綠", colours.get(colours.size() / 2) == 0x5FC063);
        check("終點是 logo 淺綠", colours.get(colours.size() - 1) == 0xA2DF86);
        boolean brighter = true;
        for (int i = 1; i < colours.size(); i++) {
            brighter &= (colours.get(i) >> 8 & 0xFF) > (colours.get(i - 1) >> 8 & 0xFF);
        }
        check("逐字變亮", brighter);
        check("字母不帶陰影", pill().stream().filter(g -> isFront(g.cp()))
                .allMatch(g -> g.style().getShadowColor() != null
                        && g.style().getShadowColor() == 0));
        check("底層帶陰影", pill().stream().filter(g -> isBack(g.cp()))
                .allMatch(g -> g.style().getShadowColor() == null));
    }

    // Wynncraft 的 text shader 拿這些顏色當動畫記號，見 effect.glsl 與 movement.glsl
    private static boolean marker(int rgb) {
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        boolean movement = (g == 235 && b % 4 == 0 && b <= 72) || (g == 58 && b <= 18);
        boolean effect = (r < 4 && g >> 2 == 60 && b >> 2 <= 9) || (r == 0 && g == 60 && b <= 9);
        return movement || effect;
    }

    private static int shadow(int rgb) {
        return (rgb >> 16 & 0xFF) / 4 << 16 | (rgb >> 8 & 0xFF) / 4 << 8 | (rgb & 0xFF) / 4;
    }

    private static void coloursAreNotShaderMarkers() {
        check("記號判斷本身認得 #00F01C", marker(0x00F01C));
        check("記號判斷本身認得 #12EB34", marker(0x12EB34));
        Set<Integer> colours = new HashSet<>();
        for (Glyph g : pill()) {
            if (g.style().getColor() != null) {
                colours.add(g.style().getColor().getValue());
            }
        }
        glyphs(ChatPill.prefix(0)).stream()
                .filter(g -> g.style().getColor() != null)
                .forEach(g -> colours.add(g.style().getColor().getValue()));
        boolean clean = colours.stream().noneMatch(c -> marker(c) || marker(shadow(c)));
        check("小框與退路的顏色和陰影都不撞 shader 記號（" + colours.size() + " 色）", clean);
    }

    private static void fallsBackToPlainText() {
        for (int measured : new int[] {0, ChatPill.WIDTH - 1, ChatPill.WIDTH + 1, 400}) {
            Component shown = ChatPill.prefix(measured);
            check("寬度 " + measured + " 走文字（實際 " + shown.getString() + "）",
                    ("[" + WynnChaYuan.MOD_NAME + "] ").equals(shown.getString()));
            check("寬度 " + measured + " 不用資源包字型", glyphs(shown).stream()
                    .allMatch(g -> g.style().getFont().toString().contains("default")));
        }
        check("寬度對得上才用小框", pill().stream().anyMatch(g -> g.cp() == 0xE060));
    }

    private static void survivesStyledText() {
        Component line = Component.literal("line")
                .withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand("/wcy update")));
        Component whole = ChatPill.prefixed(ChatPill.WIDTH, line);
        List<Glyph> before = glyphs(whole);
        List<Glyph> after = glyphs(StyledText.fromComponent(whole).getComponent());
        check("Wynntils 重組後字元不變", before.stream().map(Glyph::cp).toList()
                .equals(after.stream().map(Glyph::cp).toList()));
        boolean same = before.size() == after.size();
        for (int i = 0; same && i < before.size(); i++) {
            Style a = before.get(i).style();
            Style b = after.get(i).style();
            same = Objects.equals(a.getColor(), b.getColor())
                    && Objects.equals(a.getFont(), b.getFont())
                    && Objects.equals(a.getShadowColor(), b.getShadowColor())
                    && Objects.equals(a.getClickEvent(), b.getClickEvent());
        }
        check("Wynntils 重組後顏色、字型、陰影、點擊都在", same);
        check("點擊只掛在訊息上", before.stream()
                .allMatch(g -> (g.style().getClickEvent() != null) == (g.cp() < 0x80
                        && !"[]".contains(Character.toString(g.cp())))));
    }

    private static void ourMessageSkipsTranslationAndCapture() {
        Component whole = ChatPill.prefixed(ChatPill.WIDTH, Component.literal("Saved your settings."));
        StyledText seen = StyledText.fromComponent(whole);
        ChatBlock.clear();
        check("別處來的同一句會進收集，下面的守門才有意義",
                CaptureProbe.chatTemplate(seen, null) != null);
        ChatBlock.sendOurs(whole, () -> {
            check("送出期間認得是自己的", ChatBlock.isOurs(seen));
            check("不進語料收集", CaptureProbe.chatTemplate(seen, null) == null);
            ChatMessageEvent.Edit edit = new ChatMessageEvent.Edit(seen, RecipientType.INFO);
            new ChatListener().onChat(edit);
            check("ChatListener 不改內容", seen.equals(edit.getMessage()));
            ChatMessageEvent.Edit touched = new ChatMessageEvent.Edit(
                    StyledText.fromString("changed by another mod"), RecipientType.INFO);
            new ChatListener().onChat(touched);
            check("別的模組動過也換回原樣", seen.equals(touched.getMessage()));
        });
        check("送完就放開", !ChatBlock.isOurs(seen));
    }

    private static void languageFilesCarryNoPrefix() throws Exception {
        for (String lang : Languages.known()) {
            try (InputStream in = ChatPillTest.class.getResourceAsStream(
                    "/assets/wynnchayuan/lang/" + lang + ".json")) {
                if (in != null) {
                    String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    check(lang + " 的字串不自帶前綴", !text.contains("[WynnChaYuan]"));
                }
            }
        }
    }
}
