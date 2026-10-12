package com.wynnchayuan.cape;

import java.util.ArrayList;
import java.util.List;

/**
 * 把一張圖（或 GIF 的一格）排成 Minecraft 披風貼圖的樣子。
 *
 * <h2>收哪幾種圖（使用者 2026-10-12）</h2>
 * 「希望披風可以支援 gif 和 png……直接套用 gif 而不是多個 png，以及大小也是問題。」
 * 所以不要求作者照版型排，圖長什麼樣就怎麼收：
 * <ul>
 *   <li><b>只有一面</b>（任何尺寸、任何比例）：當成披風<b>背後看到的那一面</b>，
 *       裁成 10:16 縮到合適的大小，內側與四邊由這裡補；</li>
 *   <li><b>範本</b>（22×17 格的倍數）：我們自己的版型——正面、背面、邊框都在圖上，
 *       用不到的空白裁掉了。上傳只收這一種；</li>
 *   <li><b>完整版型</b>（寬是高的兩倍、寬是 64 的倍數）：Minecraft 自己的披風貼圖，照用；</li>
 *   <li><b>直排的多格 PNG</b>（Wynntils 的動畫披風）：每一格是一張完整版型，從上往下排，
 *       拆開當動畫——已經做好的檔不必重做。</li>
 * </ul>
 * 版型右半邊是鞘翅的貼圖；這裡不用它（使用者：「我們不需要翅膀」），只有一面的圖
 * 那一塊留空。
 *
 * <h2>披風貼圖的版型</h2>
 * 以 64×32 為一個單位（下面的 k 是放大倍數）：
 * <pre>
 *        1k        11k       21k
 *   0  ┌─上緣─────┬─下緣─────┐
 *  1k ┌┼──────────┼┬─────────┼┐
 *     │左│ 外側   │右│ 內側   │
 *     │邊│ 10k×16k│邊│ 10k×16k│
 * 17k └┴──────────┴┴─────────┴┘
 * </pre>
 * 外側是站在玩家背後看到的那一面。
 *
 * <p>這個類別只算像素，不碰 Minecraft，離線測得到。
 */
public final class CapeImage {

    private CapeImage() {}

    /** 一面是 10×16 格。 */
    public static final int FACE_W = 10;
    public static final int FACE_H = 16;

    /** 最多放大幾倍。16 倍時一面是 160×256，再大在遊戲裡看不出差別。 */
    public static final int MAX_SCALE = 16;

    /** 一件披風所有格加起來最多幾個像素（大約 16 MB 的顯示卡記憶體）。 */
    public static final long PIXEL_BUDGET = 4_000_000L;

    /** 動畫最多幾格。 */
    public static final int MAX_FRAMES = 120;

    /** 直排多格 PNG 沒有寫每格停多久，照 Wynntils 的節奏。 */
    public static final int STRIP_DELAY_MS = 100;

    /** 內側比外側暗一點，看得出是背面。 */
    private static final float INNER_SHADE = 0.6f;

    /** @param argb 一列接一列，沒用到的地方是 0 */
    public record Frame(int width, int height, int[] argb, int delayMs) {}

    /**
     * 範本的大小，以格為單位：披風貼圖左上角用得到的那一塊。
     * 寬 22 ＝ 左邊 1 ＋ 正面 10 ＋ 右邊 1 ＋ 背面 10；高 17 ＝ 上下緣 1 ＋ 面 16。
     */
    public static final int TEMPLATE_W = 22;
    public static final int TEMPLATE_H = 17;

    /** 寬高是不是我們的範本（22×17 的整數倍）。 */
    public static boolean template(int width, int height) {
        return width >= TEMPLATE_W && width % TEMPLATE_W == 0
                && (long) height * TEMPLATE_W == (long) width * TEMPLATE_H;
    }

    /**
     * 範本 → 完整的披風貼圖：內容原封不動放在左上角，右邊與下面補空白。
     *
     * @param frames 這件披風總共幾格（算預算用）
     */
    public static Frame fromTemplate(int[] source, int sw, int sh, int frames, int delayMs) {
        int k = sw / TEMPLATE_W;
        int limit = (int) Math.floor(Math.sqrt(
                PIXEL_BUDGET / (64.0 * 32.0 * Math.max(1, frames))));
        int target = Math.max(1, Math.min(k, Math.min(MAX_SCALE, limit)));
        int tw = TEMPLATE_W * target;
        int th = TEMPLATE_H * target;
        int[] used = target == k ? source : resample(source, sw, 0, 0, sw, sh, tw, th);
        int width = 64 * target;
        int[] out = new int[width * 32 * target];
        for (int y = 0; y < th; y++) {
            for (int x = 0; x < tw; x++) {
                // 版型左上與右上各有一小塊不屬於任何一面（邊框的轉角），留空
                boolean corner = y < target && (x < target || x >= 21 * target);
                out[y * width + x] = corner ? 0 : opaque(used[y * tw + x]);
            }
        }
        return new Frame(width, 32 * target, out, delayMs);
    }

    /** 寬高是不是一張完整的披風版型。 */
    public static boolean fullLayout(int width, int height) {
        return width >= 64 && width % 64 == 0 && height * 2 == width;
    }

    /**
     * 直排的多格版型有幾格；不是那種圖回 0。
     *
     * <p>只有一格的話就是 {@link #fullLayout}，這裡也回 0。
     */
    public static int stripFrames(int width, int height) {
        if (width < 64 || width % 64 != 0 || height % (width / 2) != 0) {
            return 0;
        }
        int frames = height / (width / 2);
        return frames >= 2 ? frames : 0;
    }

    /**
     * 只有一面的圖要放大幾倍。
     *
     * <p>跟著原圖的高度走（高 64 的圖用 4 倍剛好一格對一格），再看整件披風的像素預算：
     * 格數多的動畫每一格就得小一點。
     */
    public static int scaleFor(int sourceHeight, int frames) {
        int wanted = Math.max(1, Math.round(sourceHeight / (float) FACE_H));
        int limit = (int) Math.floor(Math.sqrt(
                PIXEL_BUDGET / (64.0 * 32.0 * Math.max(1, frames))));
        return Math.max(1, Math.min(MAX_SCALE, Math.min(wanted, limit)));
    }

    /**
     * 只有一面的圖 → 一張完整版型。
     *
     * @param k 放大倍數，貼圖是 64k×32k
     */
    public static Frame face(int[] source, int sw, int sh, int k, int delayMs) {
        // 先裁成 10:16（置中裁掉多的那一邊），再縮放到一面的大小
        int cropW = sw;
        int cropH = sh;
        if ((long) sw * FACE_H > (long) sh * FACE_W) {
            cropW = Math.max(1, (int) ((long) sh * FACE_W / FACE_H));
        } else {
            cropH = Math.max(1, (int) ((long) sw * FACE_H / FACE_W));
        }
        int fw = FACE_W * k;
        int fh = FACE_H * k;
        int[] face = resample(source, sw, (sw - cropW) / 2, (sh - cropH) / 2, cropW, cropH,
                fw, fh);
        int width = 64 * k;
        int height = 32 * k;
        int[] out = new int[width * height];
        for (int y = 0; y < fh; y++) {
            for (int x = 0; x < fw; x++) {
                int pixel = opaque(face[y * fw + x]);
                out[(k + y) * width + k + x] = pixel;                              // 外側
                out[(k + y) * width + 12 * k + (fw - 1 - x)] = shade(pixel);       // 內側（左右反過來）
            }
        }
        for (int y = 0; y < fh; y++) {
            for (int x = 0; x < k; x++) {
                out[(k + y) * width + x] = opaque(face[y * fw + x]);               // 左邊
                out[(k + y) * width + 11 * k + x] = opaque(face[y * fw + fw - k + x]);   // 右邊
            }
        }
        for (int y = 0; y < k; y++) {
            for (int x = 0; x < fw; x++) {
                out[y * width + k + x] = opaque(face[y * fw + x]);                 // 上緣
                out[y * width + 11 * k + x] = opaque(face[(fh - k + y) * fw + x]); // 下緣
            }
        }
        return new Frame(width, height, out, delayMs);
    }

    /**
     * 完整版型照用；太大的話整數倍縮小到預算以內。
     *
     * @param frames 這件披風總共幾格（算預算用）
     */
    public static Frame full(int[] source, int sw, int sh, int frames, int delayMs) {
        int k = sw / 64;
        int limit = (int) Math.floor(Math.sqrt(
                PIXEL_BUDGET / (64.0 * 32.0 * Math.max(1, frames))));
        int target = Math.max(1, Math.min(k, Math.min(MAX_SCALE, limit)));
        if (target == k) {
            return new Frame(sw, sh, source, delayMs);
        }
        int width = 64 * target;
        int height = 32 * target;
        return new Frame(width, height, resample(source, sw, 0, 0, sw, sh, width, height), delayMs);
    }

    /** 直排多格的 PNG 拆成一格一格。 */
    public static List<Frame> strip(int[] source, int sw, int sh) {
        int count = Math.min(MAX_FRAMES, stripFrames(sw, sh));
        int each = sw / 2;
        List<Frame> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int[] one = new int[sw * each];
            System.arraycopy(source, i * each * sw, one, 0, one.length);
            out.add(full(one, sw, each, count, STRIP_DELAY_MS));
        }
        return out;
    }

    /**
     * 一張圖（一格）排成披風貼圖。完整版型照用，其餘當成只有一面。
     *
     * @param frames 這件披風總共幾格
     */
    public static Frame layout(int[] source, int sw, int sh, int frames, int delayMs) {
        if (template(sw, sh)) {
            return fromTemplate(source, sw, sh, frames, delayMs);
        }
        return fullLayout(sw, sh)
                ? full(source, sw, sh, frames, delayMs)
                : face(source, sw, sh, scaleFor(sh, frames), delayMs);
    }

    /**
     * 同 {@link #layout}，但保證回來的那一格不跟 {@code source} 共用陣列。
     *
     * <p>來源是會被重複使用的畫布時用這個（{@link Gif#each} 一格一格交出同一張畫布）：
     * 完整版型不必縮的時候 {@link #full} 直接把來源交回去，不複製的話每一格都會變成最後一格。
     */
    public static Frame layoutDetached(int[] source, int sw, int sh, int frames, int delayMs) {
        Frame frame = layout(source, sw, sh, frames, delayMs);
        return frame.argb() == source
                ? new Frame(frame.width(), frame.height(), source.clone(), frame.delayMs())
                : frame;
    }

    /**
     * 取樣：目的地的每一個像素，是來源對應那一塊的平均。縮小時不會掉細節，
     * 放大時那一塊不到一個像素，等於取最近的那一點（像素畫不會糊）。
     */
    static int[] resample(int[] source, int stride, int cropX, int cropY, int cropW, int cropH,
                          int dw, int dh) {
        int[] out = new int[dw * dh];
        for (int dy = 0; dy < dh; dy++) {
            int y0 = cropY + (int) ((long) dy * cropH / dh);
            int y1 = Math.max(y0 + 1, cropY + (int) (((long) (dy + 1) * cropH + dh - 1) / dh));
            y1 = Math.min(y1, cropY + cropH);
            for (int dx = 0; dx < dw; dx++) {
                int x0 = cropX + (int) ((long) dx * cropW / dw);
                int x1 = Math.max(x0 + 1, cropX + (int) (((long) (dx + 1) * cropW + dw - 1) / dw));
                x1 = Math.min(x1, cropX + cropW);
                long a = 0;
                long r = 0;
                long g = 0;
                long b = 0;
                int n = 0;
                for (int y = y0; y < y1; y++) {
                    for (int x = x0; x < x1; x++) {
                        int p = source[y * stride + x];
                        int alpha = p >>> 24;
                        a += alpha;
                        r += ((p >> 16) & 0xFF) * alpha;       // 先乘透明度，透明的像素不會把顏色拉黑
                        g += ((p >> 8) & 0xFF) * alpha;
                        b += (p & 0xFF) * alpha;
                        n++;
                    }
                }
                if (n == 0 || a == 0) {
                    continue;
                }
                out[dy * dw + dx] = ((int) (a / n) << 24) | ((int) (r / a) << 16)
                        | ((int) (g / a) << 8) | (int) (b / a);
            }
        }
        return out;
    }

    /**
     * 披風是用不透明的方式畫的，半透明的像素要先壓在黑底上——不然透明的地方會露出
     * 檔案裡殘留的顏色。
     */
    static int opaque(int argb) {
        int alpha = argb >>> 24;
        if (alpha == 0xFF) {
            return argb;
        }
        int r = ((argb >> 16) & 0xFF) * alpha / 255;
        int g = ((argb >> 8) & 0xFF) * alpha / 255;
        int b = (argb & 0xFF) * alpha / 255;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int shade(int argb) {
        int r = (int) (((argb >> 16) & 0xFF) * INNER_SHADE);
        int g = (int) (((argb >> 8) & 0xFF) * INNER_SHADE);
        int b = (int) ((argb & 0xFF) * INNER_SHADE);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
