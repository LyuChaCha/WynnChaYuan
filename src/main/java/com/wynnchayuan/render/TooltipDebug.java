package com.wynnchayuan.render;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.LineParts;
import com.wynntils.core.text.PartStyle;
import com.wynntils.core.text.StyledText;
import com.wynntils.core.text.StyledTextPart;
import com.wynntils.core.text.type.StyleType;
import net.minecraft.network.chat.Component;

import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class TooltipDebug {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting().disableHtmlEscaping().create();

    private static final int MAX_TOOLTIPS = 5;

    private static final int MAX_PARTIAL = 6;

    private static Path file;
    private static int written = 0;
    private static int partial = 0;

    private static final java.util.Set<String> seen = new java.util.HashSet<>();

    private TooltipDebug() {}

    public static void init(Path path) {
        file = path;
    }

    public static synchronized void dump(List<Component> tooltip) {
        if (file == null || written >= MAX_TOOLTIPS || tooltip.isEmpty()) {
            return;
        }
        written++;
        write(tooltip, null, null, "tooltip-debug-" + written + ".json",
              "查不到譯文的 tooltip。比對 template 與譯文檔的 src 是否相同。");
    }

    public static synchronized void dumpPartial(List<Component> tooltip,
                                                List<Component> shown, boolean[] hit) {
        if (file == null || partial >= MAX_PARTIAL || tooltip.isEmpty()) {
            return;
        }
        if (!seen.add(key(tooltip))) {
            return;
        }
        partial++;
        write(tooltip, shown, hit, "tooltip-partial-" + partial + ".json",
              "翻了一半的 tooltip。translated=false 的那幾行，"
              + "如果語料裡其實查得到，就是算繪端擋掉的。"
              + "shown 是實際畫出去的那一行；兩邊的 spacePx 對照就看得出欄距補了多少。");
    }

    private static final int MAX_ABILITY = 6;
    private static int ability = 0;

    public static synchronized void dumpAbility(List<Component> tooltip, List<Component> shown) {
        if (file == null || ability >= MAX_ABILITY || tooltip.isEmpty()) {
            return;
        }
        if (!seen.add("ability|" + key(tooltip))) {
            return;
        }
        ability++;
        write(tooltip, shown, null, "ability-tooltip-" + ability + ".json",
              "技能樹節點的說明。segments 是原文的片段，shown 是實際畫出去的那一行"
              + "（行數不同時只是照行號並排）；bold 只在粗體的片段上出現。");
    }

    private static final int MAX_FOREIGN = 8;
    private static int foreign = 0;

    public static synchronized void dumpForeign(String who, List<Component> tooltip,
                                                List<Component> shown) {
        if (file == null || foreign >= MAX_FOREIGN || tooltip.isEmpty()) {
            return;
        }
        if (!seen.add(who + "|" + key(tooltip))) {
            return;
        }
        foreign++;
        write(tooltip, shown, null, who + "-tooltip-" + foreign + ".json",
              who + " 重畫的 tooltip。shown 是我們翻完交回去的那一行；沒有 shown 就是整份都沒翻到。");
    }

    private static String key(List<Component> tooltip) {
        StringBuilder sb = new StringBuilder();
        for (Component line : tooltip) {
            sb.append(line.getString()).append('\n');
        }
        return sb.toString();
    }

    private static void write(List<Component> tooltip, List<Component> shown,
                              boolean[] hit, String name, String note) {
        JsonArray lines = new JsonArray();
        int at = -1;
        for (Component line : tooltip) {
            at++;
            JsonObject o = describe(line);
            o.addProperty("row", at);
            if (line.getString().contains("..")) {
                JsonArray raw = new JsonArray();
                line.visit((style, text) -> {
                    JsonObject each = new JsonObject();
                    each.addProperty("text", text);
                    each.addProperty("style", String.valueOf(style));
                    raw.add(each);
                    return java.util.Optional.empty();
                }, net.minecraft.network.chat.Style.EMPTY);
                o.add("rawStyles", raw);
            }
            if (hit != null && at < hit.length) {
                o.addProperty("translated", hit[at]);
            }
            if (shown != null && at < shown.size()) {
                o.add("shown", describe(shown.get(at)));
            }
            lines.add(o);
        }

        JsonObject root = new JsonObject();
        root.addProperty("_note", note);
        root.add("lines", lines);

        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(
                    file.resolveSibling(name),
                    StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 診斷檔寫入失敗: " + e.getMessage());
        }
    }

    private static JsonObject describe(Component line) {
        StyledText styled = StyledText.fromComponent(line);
        JsonObject o = new JsonObject();
        o.addProperty("plain", styled.getStringWithoutFormatting());
        o.addProperty("template", GlyphSplitter.toTemplate(styled));
        o.addProperty("glyphOnly", GlyphSplitter.isGlyphOnly(styled));

        LineParts parts = LineParts.of(styled);
        o.addProperty("glyphs", parts.glyphs().size());
        o.addProperty("places", parts.places().size());
        o.addProperty("numbers", parts.numbers().size());

        JsonArray segs = new JsonArray();
        for (StyledTextPart part : styled) {
            String raw = part.getString(null, StyleType.NONE);
            JsonObject seg = new JsonObject();
            seg.addProperty("text", raw);
            PartStyle ps = part.getPartStyle();
            seg.addProperty("font", ps == null || ps.getFont() == null
                    ? "(none)" : ps.getFont().toString());
            seg.addProperty("isGlyph", GlyphSplitter.isGlyphPart(part));
            if (ps != null && ps.getColor() != null) {
                seg.addProperty("colour", ps.getColor().toHexString());
            }
            if (ps != null && ps.isBold()) {
                seg.addProperty("bold", true);
            }
            int px = com.wynnchayuan.translate.SpaceOffset.decode(raw);
            if (px != 0) {
                seg.addProperty("spacePx", px);
            }
            segs.add(seg);
        }
        o.add("segments", segs);
        return o;
    }
}
