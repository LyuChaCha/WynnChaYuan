package com.wynnchayuan.render;

import com.wynntils.core.text.StyledText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Wynntils 記分板疊層（右邊那一欄）：譯文比原文寬的列折成兩列。
 *
 * <h2>回報（2026-10-19）</h2>
 * Lootrun 的試煉名附了原文——「獻祭儀式 (Sacrificial Ritual)」——超出記分板的框，
 * 後半截畫到畫面外。原因是 Wynntils 的 {@code ScoreboardOverlay} 照<b>英文原文</b>
 * 算框寬（{@code longestLineWidth}）與列數（{@code linesToRender}），而我們是在
 * 它畫每一列字的時候才換成譯文（{@link WynntilsText#screenText}）——框早就定了。
 *
 * <h2>做法</h2>
 * 每一幀畫之前（{@code ScoreboardOverlayMixin}）先把每一列換成譯文，比原本的框寬
 * 還寬的那幾列用原版的斷行器折開，框寬、高度照新的列重算；畫的期間掛著
 * {@link WynntilsText#holdRawText}，已經換好的字不再查一次（折出來的「(Sacrificial
 * Ritual)」那一列再查表會被換回中文）。畫完把 Wynntils 的欄位放回去，它自己的
 * 更新流程看到的永遠是原文。
 *
 * <p>框先撐寬到疊層自己的寬度（Wynntils 的 Overlay 大小，預設 150，可在它的設定裡拉），
 * 塞得下就不折；只有比那個還寬的列才折。框是往對齊的那一邊長的（靠右的記分板往左長）。
 * 先前是照英文的框寬硬折，「殲滅：第 1 波 - 還剩 1 隻怪物！」只比英文寬一點也被折成兩列
 *（使用者 2026-10-19 的截圖）。
 *
 * <p>欄位用反射拿，不用 {@code @Shadow}：Wynntils 改名時這裡只是不折，不會開不起來。
 */
public final class ScoreboardWrap {

    private ScoreboardWrap() {
    }

    /** 一列 9 px，跟 Wynntils 算 {@code contentHeight} 的方式一樣。 */
    private static final float LINE_HEIGHT = 9f;

    /** 框太窄時至少用這個寬度折，免得一個字一列。 */
    private static final float MIN_WIDTH = 60f;

    private static final String OVERLAY = "com.wynntils.overlays.ScoreboardOverlay";

    private static Field lines;
    private static Field longest;
    private static Field height;
    private static boolean broken;

    /** 上一幀的結果：譯文那幾列（照物件身分比）與框寬沒變就直接用。 */
    private static List<StyledText> lastTranslated = List.of();
    private static float lastWidth = -1;
    private static float lastRoom = -1;
    private static List<StyledText> lastWrapped = List.of();
    private static float lastLongest;

    /** 畫之前換掉的那一份，畫完放回去。 */
    private static Object swapped;
    private static Object savedLines;
    private static float savedLongest;
    private static float savedHeight;

    /** {@code ScoreboardOverlay.render} 開頭。 */
    @SuppressWarnings("unchecked")
    public static void begin(Object overlay) {
        swapped = null;
        if (broken || overlay == null || !fields(overlay)) {
            return;
        }
        try {
            List<StyledText> source = (List<StyledText>) lines.get(overlay);
            float width = longest.getFloat(overlay);
            float room = Math.max(width, roomOf(overlay));
            if (source == null || source.isEmpty()) {
                return;
            }
            List<StyledText> translated = new ArrayList<>(source.size());
            for (StyledText row : source) {
                translated.add(WynntilsText.screenText(row));
            }
            if (!sameRows(translated, lastTranslated) || width != lastWidth || room != lastRoom) {
                StringSplitter splitter = Minecraft.getInstance().font.getSplitter();
                List<StyledText> wrapped = new ArrayList<>();
                float widest = 0;
                for (StyledText row : translated) {
                    for (StyledText piece : wrap(row, room, splitter)) {
                        wrapped.add(piece);
                        widest = Math.max(widest, splitter.stringWidth(piece.getComponent()));
                    }
                }
                lastTranslated = translated;
                lastWidth = width;
                lastRoom = room;
                lastWrapped = wrapped;
                // 框至少跟 Wynntils 原本算的一樣寬（含標題 play.wynncraft.com），譯文寬就跟著寬
                lastLongest = Math.max(width, widest);
            }
            savedLines = source;
            savedLongest = width;
            savedHeight = height.getFloat(overlay);
            lines.set(overlay, lastWrapped);
            longest.setFloat(overlay, lastLongest);
            height.setFloat(overlay, lastWrapped.size() * LINE_HEIGHT);
            swapped = overlay;
            WynntilsText.holdRawText(true);
        } catch (Throwable t) {
            broken = true;
        }
    }

    /** {@code ScoreboardOverlay.render} 結尾（含中途丟例外之後的下一幀）。 */
    public static void end(Object overlay) {
        WynntilsText.holdRawText(false);
        if (swapped == null || swapped != overlay) {
            return;
        }
        try {
            lines.set(overlay, savedLines);
            longest.setFloat(overlay, savedLongest);
            height.setFloat(overlay, savedHeight);
        } catch (Throwable t) {
            broken = true;
        }
        swapped = null;
    }

    /**
     * 比 {@code width} 寬的一列折成幾列。開頭是「- 」的條列項目，續行縮排對齊內文。
     *
     * @return 不必折時是只有它自己的清單
     */
    static List<StyledText> wrap(StyledText row, float width, StringSplitter splitter) {
        Component whole = row.getComponent();
        float limit = Math.max(width, MIN_WIDTH);
        if (splitter.stringWidth(whole) <= limit + 0.5f) {
            return List.of(row);
        }
        String plain = row.getStringWithoutFormatting();
        String indent = plain.startsWith("- ") || plain.startsWith("• ") ? "  " : "";
        // 「譯名 (原文)」：附的原文整段放到下一列，不要從原文中間斷開
        int paren = plain.endsWith(")") ? plain.lastIndexOf(" (") : -1;
        if (paren > 0) {
            List<StyledText> pair = List.of(
                    StyledText.fromComponent(slice(whole, 0, paren)),
                    StyledText.fromComponent(Component.empty()
                            .append(Component.literal(indent))
                            .append(slice(whole, paren + 1, plain.length()))));
            if (pair.stream().allMatch(p -> splitter.stringWidth(p.getComponent()) <= limit + 0.5f)) {
                return pair;
            }
        }
        List<FormattedText> parts = splitter.splitLines(whole, (int) limit, Style.EMPTY);
        if (parts.size() < 2) {
            return List.of(row);
        }
        List<StyledText> out = new ArrayList<>(parts.size());
        for (int i = 0; i < parts.size(); i++) {
            MutableComponent line = Component.empty();
            boolean[] leading = {i > 0};
            parts.get(i).visit((style, text) -> {
                String piece = text;
                if (leading[0]) {
                    piece = piece.stripLeading();
                    if (piece.isEmpty()) {
                        return Optional.empty();
                    }
                    leading[0] = false;
                    line.append(Component.literal(indent).withStyle(style));
                }
                line.append(Component.literal(piece).withStyle(style));
                return Optional.empty();
            }, Style.EMPTY);
            out.add(StyledText.fromComponent(line));
        }
        return out;
    }

    /** 照字元位置切出一段，樣式跟著每個字走。位置以去掉樣式之後的字為準。 */
    private static Component slice(Component whole, int from, int to) {
        MutableComponent out = Component.empty();
        int[] at = {0};
        whole.visit((style, text) -> {
            int start = at[0];
            int end = start + text.length();
            at[0] = end;
            int a = Math.max(from, start);
            int b = Math.min(to, end);
            if (a < b) {
                out.append(Component.literal(text.substring(a - start, b - start)).withStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static java.lang.reflect.Method sizeGetter;
    private static Field scaleField;
    private static java.lang.reflect.Method scaleGet;

    /** 疊層自己的寬度換算成字的寬度（除掉字級倍率）。讀不到就回 0，照 Wynntils 的框寬折。 */
    private static float roomOf(Object overlay) {
        try {
            if (sizeGetter == null) {
                Class<?> type = overlay.getClass();
                while (type != null && !OVERLAY.equals(type.getName())) {
                    type = type.getSuperclass();
                }
                if (type == null) {
                    return 0;
                }
                Field scale = type.getDeclaredField("fontScale");
                scale.setAccessible(true);
                scaleField = scale;
                scaleGet = scale.getType().getMethod("get");
                sizeGetter = type.getMethod("getWidth");
            }
            float overlayWidth = (Float) sizeGetter.invoke(overlay);
            float scale = (Float) scaleGet.invoke(scaleField.get(overlay));
            return scale > 0 ? overlayWidth / scale : overlayWidth;
        } catch (Throwable t) {
            return 0;
        }
    }

    /** 物件身分相同才算同一份：screenText 查得到時回傳的是快取裡同一個物件。 */
    private static boolean sameRows(List<StyledText> a, List<StyledText> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) != b.get(i)) {
                return false;
            }
        }
        return true;
    }

    private static boolean fields(Object overlay) {
        if (lines != null) {
            return true;
        }
        try {
            Class<?> type = overlay.getClass();
            while (type != null && !OVERLAY.equals(type.getName())) {
                type = type.getSuperclass();
            }
            if (type == null) {
                broken = true;
                return false;
            }
            Field l = type.getDeclaredField("linesToRender");
            Field w = type.getDeclaredField("longestLineWidth");
            Field h = type.getDeclaredField("contentHeight");
            l.setAccessible(true);
            w.setAccessible(true);
            h.setAccessible(true);
            lines = l;
            longest = w;
            height = h;
            return true;
        } catch (Throwable t) {
            broken = true;
            return false;
        }
    }
}
