package com.wynnchayuan.render;

import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

/**
 * 原文 tooltip 這一幀<b>真正</b>畫在哪裡，譯名面板照它擺。
 *
 * <h2>回報（2026-10-24）</h2>
 * 「另開面板」跟原文疊了一截、上下也對不齊。面板一直照原版的公式（游標右下 12/-12）
 * 推算原文的位置，可是原文常常不在那裡：
 * <ul>
 *   <li>太高的物品說明被 Wynntils 的「tooltip 縮放」改了位置與大小；</li>
 *   <li>裝了 Wynnventory 的時候，原文會往右讓開價格框（{@link WynnventoryLayout#shift}）。</li>
 * </ul>
 * 誰挪的都一樣：每個 tooltip 最後都會走到 {@code TooltipRenderUtil#renderTooltipBackground}，
 * 那時候的座標乘上當下的變換，就是畫面上的位置（mixin 見 {@code TooltipBackgroundMixin}）。
 *
 * <p>一幀裡畫的 tooltip 不只一個（Wynntils 的比較框、別的模組的提示），所以照<b>文字區的寬高</b>
 * 認：原文的寬高我們自己量得出來，對得上的那一個才是它。我們自己的面板與 Wynnventory 的
 * 價格框不記。
 */
public final class OriginalTooltipBox {

    private OriginalTooltipBox() {
    }

    /**
     * 原版 tooltip 的底色與框線圖從文字區往外畫多少（{@code TooltipRenderUtil}：內距 3 加圖邊 9）。
     *
     * <p>原版的圖在外圈 9px 是透明的，看起來只多 4px；Wynncraft 的資源包把那一圈畫滿了
     * （紫色、金色的稀有度框），整整多出 12px。兩個框的間距照畫得出來的最外圈算，
     * 才不會像回報那樣疊在一起。
     */
    public static final int FRAME = 12;

    /**
     * 文字區在畫面上的位置與大小，以及縮放。
     *
     * @param rawW 縮放前的文字寬（拿來跟我們量的比對）
     * @param rawH 縮放前的文字高
     */
    public record Box(float x, float y, float w, float h, float scale, int rawW, int rawH) {
    }

    private static final int KEEP = 8;

    private static long frame;
    private static final Box[] now = new Box[KEEP];
    private static int nowCount;
    private static final Box[] last = new Box[KEEP];
    private static int lastCount;
    private static int ownDepth;

    /** 每一幀開頭叫一次：這一幀的紀錄挪成上一幀。 */
    public static void nextFrame() {
        frame++;
        System.arraycopy(now, 0, last, 0, nowCount);
        lastCount = nowCount;
        nowCount = 0;
    }

    /** 我們自己畫 tooltip 的期間不記（譯名面板、被推開的原文）。 */
    public static void beginOwn() {
        ownDepth++;
    }

    public static void endOwn() {
        ownDepth = Math.max(0, ownDepth - 1);
    }

    /** mixin 在每一個 tooltip 畫底色之前呼叫。 */
    public static void note(GuiGraphics graphics, int x, int y, int w, int h) {
        try {
            if (ownDepth > 0 || WynnventoryLayout.drawing() || graphics == null || nowCount >= KEEP) {
                return;
            }
            Matrix3x2f m = graphics.pose();
            Vector2f a = m.transformPosition(x, y, new Vector2f());
            Vector2f b = m.transformPosition(x + w, y + h, new Vector2f());
            float sw = b.x - a.x;
            now[nowCount++] = new Box(a.x, a.y, sw, b.y - a.y, w == 0 ? 1f : sw / w, w, h);
        } catch (Throwable t) {
            // 量不到就退回原版公式
        }
    }

    /**
     * 這一幀（沒有就上一幀——Pre 事件問的時候這一幀還沒畫）文字區剛好這麼大的那個 tooltip。
     *
     * @return 對不上或沒畫過時回 {@code null}
     */
    public static Box find(int textW, int textH) {
        Box hit = find(now, nowCount, textW, textH);
        return hit != null ? hit : find(last, lastCount, textW, textH);
    }

    private static Box find(Box[] list, int count, int textW, int textH) {
        for (int i = 0; i < count; i++) {
            Box b = list[i];
            if (b != null && b.rawW() == textW && b.rawH() == textH) {
                return b;
            }
        }
        return null;
    }

    /** 給測試用：當作這一幀畫了一個。 */
    static void add(Box b) {
        if (nowCount < KEEP) {
            now[nowCount++] = b;
        }
    }

    /** 給測試用。 */
    static void forget() {
        nowCount = 0;
        lastCount = 0;
    }
}
