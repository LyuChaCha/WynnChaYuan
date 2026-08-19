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

/**
 * 把「查不到譯文的 tooltip」實際算出來的模板寫成檔案，供對照。
 *
 * <p>純診斷用途，<b>與收集分開</b>：裝備／技能的文案來自離線語料，
 * 不從遊戲收集；這裡只是在翻譯沒生效時，把「字典的鍵」與「實際查詢的鍵」
 * 攤開來比對——光看畫面完全看不出兩者差在哪。
 *
 * <p>連每個片段的字型也一併記下來，因為最可能的原因就是<b>整行帶了自訂字型</b>
 * 而被當成圖示整段跳過。
 *
 * <p>只寫第一份遇到的內容就停，避免檔案無限長大。
 */
public final class TooltipDebug {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting().disableHtmlEscaping().create();

    private static final int MAX_TOOLTIPS = 5;

    private static Path file;
    private static int written = 0;

    private TooltipDebug() {}

    public static void init(Path path) {
        file = path;
    }

    /** 記下一份查不到譯文的 tooltip。 */
    public static synchronized void dump(List<Component> tooltip) {
        if (file == null || written >= MAX_TOOLTIPS || tooltip.isEmpty()) {
            return;
        }
        written++;

        JsonArray lines = new JsonArray();
        for (Component line : tooltip) {
            StyledText styled = StyledText.fromComponent(line);
            JsonObject o = new JsonObject();
            o.addProperty("plain", styled.getStringWithoutFormatting());
            o.addProperty("template", GlyphSplitter.toTemplate(styled));
            o.addProperty("glyphOnly", GlyphSplitter.isGlyphOnly(styled));

            LineParts parts = LineParts.of(styled);
            o.addProperty("glyphs", parts.glyphs().size());
            o.addProperty("places", parts.places().size());
            o.addProperty("numbers", parts.numbers().size());

            // 每個片段的字型 —— 整行被當成圖示時，原因幾乎都在這裡
            JsonArray segs = new JsonArray();
            for (StyledTextPart part : styled) {
                JsonObject seg = new JsonObject();
                seg.addProperty("text", part.getString(null, StyleType.NONE));
                PartStyle ps = part.getPartStyle();
                seg.addProperty("font", ps == null || ps.getFont() == null
                        ? "(none)" : ps.getFont().toString());
                seg.addProperty("isGlyph", GlyphSplitter.isGlyphPart(part));
                segs.add(seg);
            }
            o.add("segments", segs);
            lines.add(o);
        }

        JsonObject root = new JsonObject();
        root.addProperty("_note", "查不到譯文的 tooltip。比對 template 與譯文檔的 src 是否相同。");
        root.add("lines", lines);

        try {
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(
                    file.resolveSibling("tooltip-debug-" + written + ".json"),
                    StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (Exception e) {
            System.err.println("[WynnChaYuan] 診斷檔寫入失敗: " + e.getMessage());
        }
    }
}
