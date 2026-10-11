package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.listener.RenderListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * WynnMod 重畫的物品說明：再翻一次。
 *
 * <p>為什麼需要、接在哪裡，見 {@code WynnModTooltipMixin}。這裡只管「要不要翻」
 * 與「出事時退回它原本那一份」。
 *
 * <h2>只在就地取代模式做</h2>
 * 另開面板的模式本來就不改原本的 tooltip，譯文面板照 Wynntils 事件拿到的那一份畫，
 * WynnMod 重畫什麼都不影響它；關閉模式更不用說。
 */
public final class WynnModTooltip {

    private WynnModTooltip() {}

    private static volatile Boolean installed;

    /** 有沒有裝 WynnMod。 */
    public static boolean installed() {
        Boolean known = installed;
        if (known == null) {
            boolean found;
            try {
                found = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("wynnmod");
            } catch (Throwable t) {
                found = false;                 // 測試環境沒有 loader
            }
            installed = known = found;
        }
        return known;
    }

    /**
     * 同一種樣式的相鄰片段併成一段。
     *
     * <h2>實機回報（2026-10-10）</h2>
     * WynnMod 的說明是它自己一段一段接起來的，同一個標籤常常被拆成兩個片段：
     *
     * <pre>
     *   「Main」＋「 Scale」
     *   「Elemental Spell Da」＋「..」    （標籤欄放不下，砍掉尾巴接兩個點）
     * </pre>
     *
     * 屬性列是<b>逐片段</b>查的（見 {@code LineTranslator#translateSegments}），
     * 「Main」與「 Scale」各自都不是標籤，於是整份說明只剩這幾列是英文。
     *
     * <p>樣式（字型、顏色、點擊）完全一樣的相鄰文字，併不併畫出來一模一樣，
     * 所以先併再翻。圖示與排版偏移不碰：那些片段的個數與順序是版面的一部分。
     *
     * <p>只在這條路做。Wynntils 那邊的說明沒有這種拆法，不必為它多冒一次險。
     */
    public static List<Component> joinSameStyle(List<Component> lines) {
        List<Component> out = new ArrayList<>(lines.size());
        for (Component line : lines) {
            out.add(line == null ? null : joinSameStyle(line));
        }
        return out;
    }

    private static Component joinSameStyle(Component line) {
        List<Style> styles = new ArrayList<>();
        List<StringBuilder> texts = new ArrayList<>();
        boolean[] joined = {false};
        line.visit((style, text) -> {
            if (text.isEmpty()) {
                return Optional.empty();
            }
            int last = styles.size() - 1;
            if (last >= 0 && plain(text) && plain(texts.get(last))
                    && (sameLook(styles.get(last), style)
                        || cutMark(text))) {
                texts.get(last).append(text);
                joined[0] = true;
            } else {
                styles.add(style);
                texts.add(new StringBuilder(text));
            }
            return Optional.empty();
        }, Style.EMPTY);
        if (!joined[0]) {
            return line;                       // 沒有可併的就原樣交回去
        }
        MutableComponent out = Component.empty();
        for (int i = 0; i < styles.size(); i++) {
            out.append(Component.literal(texts.get(i).toString()).withStyle(styles.get(i)));
        }
        return out;
    }

    /**
     * 兩種樣式<b>畫出來</b>一樣嗎。
     *
     * <h2>為什麼不能用 {@code Style#equals}</h2>
     * 實機回報（2026-10-10，第二版測試）：「Main」＋「 Scale」併起來了，
     * 「Elemental Spell Da」＋「..」沒有。WynnMod 砍標籤時，砍剩的字套的是原本那一段的
     * 樣式，兩個點卻是它另外做好再接上去的——字型、顏色都一樣，但欄位「沒設」與
     * 「設成預設值」在 {@code equals} 眼中是兩回事（沒設斜體 vs 斜體＝否、沒設顏色 vs 白色）。
     *
     * <p>所以比的是畫出來的樣子：字型、顏色（沒設就是說明文字預設的白）、粗斜底線刪除
     * 亂碼五個旗標（沒設就是否）、陰影色，再加上點擊／懸停／插入——那三個不影響外觀，
     * 但併掉會改變行為，不一樣就不併。
     */
    static boolean sameLook(Style a, Style b) {
        return a.getFont().equals(b.getFont())
                && colourOf(a) == colourOf(b)
                && a.isBold() == b.isBold()
                && a.isItalic() == b.isItalic()
                && a.isUnderlined() == b.isUnderlined()
                && a.isStrikethrough() == b.isStrikethrough()
                && a.isObfuscated() == b.isObfuscated()
                && java.util.Objects.equals(a.getShadowColor(), b.getShadowColor())
                && java.util.Objects.equals(a.getClickEvent(), b.getClickEvent())
                && java.util.Objects.equals(a.getHoverEvent(), b.getHoverEvent())
                && java.util.Objects.equals(a.getInsertion(), b.getInsertion());
    }

    /**
     * 這一段就是 WynnMod 砍標籤時接上去的那兩個點。
     *
     * <p>它一定屬於前面那個標籤，所以樣式完全不看：{@link #sameLook} 列的那些只要有一個
     * 是我沒料到的不相等，這一列就又會留著英文，而那種失敗在畫面上跟「沒修」長得
     * 一模一樣。併進去之後跟著標籤的樣式；標籤翻出來時兩個點本來就不留。
     */
    private static boolean cutMark(String text) {
        return "..".equals(text);
    }

    /** 沒設顏色的字在物品說明裡是白的。 */
    private static int colourOf(Style style) {
        return style.getColor() == null ? 0xFFFFFF : style.getColor().getValue();
    }

    /** 整段都是一般文字：沒有圖示（私用區）也沒有排版偏移。 */
    private static boolean plain(CharSequence text) {
        return text.codePoints().noneMatch(cp ->
                (cp >= 0xE000 && cp <= 0xF8FF) || cp >= 0xF0000
                        || com.wynnchayuan.translate.SpaceOffset.isOffset(cp));
    }

    /**
     * WynnMod 要畫的那幾行 → 我們要它畫的那幾行。
     *
     * <p>翻不到、開關關著、或中途出任何事，都<b>原樣</b>交回去：這是別人模組的
     * 算繪路徑，在這裡丟例外壞掉的是它的 tooltip。
     */
    public static List<Component> lines(List<Component> text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        try {
            CollectorConfig config = WynnChaYuan.config();
            if (config == null || !config.wynnmodTooltip()
                    || config.tooltipMode() != CollectorConfig.TooltipMode.REPLACE) {
                return text;
            }
            List<Component> out = RenderListener.translateForeign(joinSameStyle(text));
            if (out.isEmpty()) {
                TooltipDebug.dumpForeign("wynnmod", text, null);
                return text;
            }
            TooltipDebug.dumpForeign("wynnmod", text, out);
            WynnChaYuan.store().noteEvent("tooltip.wynnmod");
            return out;
        } catch (Throwable t) {
            try {
                WynnChaYuan.store().noteEvent("render.wynnmodError");
                com.wynnchayuan.translate.ErrorDebug.note("tooltip.wynnmod",
                        text.get(0).getString(), t);
            } catch (Throwable ignored) {
                // 連記錄都失敗就算了，重點是把原本那一份交回去
            }
            return text;
        }
    }
}
