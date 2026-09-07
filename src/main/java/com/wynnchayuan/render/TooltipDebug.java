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
 * <p>只寫前幾份遇到的內容就停，避免檔案無限長大。
 *
 * <h2>兩種情形</h2>
 * <ul>
 *   <li><b>整份都查不到</b>——{@link #dump}。多半是鍵對不上（模板差一個圖示、
 *       多一個空白），比對 {@code template} 跟語料的 {@code src} 就看得出來。</li>
 *   <li><b>翻了一半</b>——{@link #dumpPartial}。這種更難查：語料裡明明有，
 *       畫面上偏偏是英文，代表卡在算繪這一端的某道守門。少了這一份就只能猜
 *       實機到底送進來什麼——技能樹「解鎖後標題變回英文」那次就是這樣，
 *       語料、capture、三個診斷檔全都查不到那一行。</li>
 * </ul>
 */
public final class TooltipDebug {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting().disableHtmlEscaping().create();

    private static final int MAX_TOOLTIPS = 5;

    /**
     * 翻一半的另外算額度。整份沒翻的那種多半是還沒翻的裝備名（本來就該留原文），
     * 共用額度的話真正的 bug 永遠排不進去。
     */
    private static final int MAX_PARTIAL = 6;

    private static Path file;
    private static int written = 0;
    private static int partial = 0;

    /**
     * 已經寫過的 tooltip。算繪是<b>每幀</b>呼叫的，不記著的話滑鼠在同一件
     * 物品上停一秒就把額度吃光。
     */
    private static final java.util.Set<String> seen = new java.util.HashSet<>();

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
        write(tooltip, null, "tooltip-debug-" + written + ".json",
              "查不到譯文的 tooltip。比對 template 與譯文檔的 src 是否相同。");
    }

    /**
     * 記下一份<b>只翻到一半</b>的 tooltip。
     *
     * <p>整份沒翻、整份翻好都不記——只記中英夾雜的那些，那才是查得出東西的。
     *
     * @param hit 每一行有沒有換成中文，長度與 {@code tooltip} 相同
     */
    public static synchronized void dumpPartial(List<Component> tooltip, boolean[] hit) {
        if (file == null || partial >= MAX_PARTIAL || tooltip.isEmpty()) {
            return;
        }
        if (!seen.add(key(tooltip))) {
            return;                            // 同一件物品只記一次
        }
        partial++;
        write(tooltip, hit, "tooltip-partial-" + partial + ".json",
              "翻了一半的 tooltip。translated=false 的那幾行，"
              + "如果語料裡其實查得到，就是算繪端擋掉的。");
    }

    /** 同一份 tooltip 只寫一次，見 {@link #seen}。 */
    private static String key(List<Component> tooltip) {
        StringBuilder sb = new StringBuilder();
        for (Component line : tooltip) {
            sb.append(line.getString()).append('\n');
        }
        return sb.toString();
    }

    private static void write(List<Component> tooltip, boolean[] hit,
                              String name, String note) {
        JsonArray lines = new JsonArray();
        int at = -1;
        for (Component line : tooltip) {
            at++;
            StyledText styled = StyledText.fromComponent(line);
            JsonObject o = new JsonObject();
            o.addProperty("row", at);
            if (hit != null && at < hit.length) {
                o.addProperty("translated", hit[at]);
            }
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
}
