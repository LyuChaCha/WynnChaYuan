package com.wynnchayuan.cape;

import java.util.ArrayList;
import java.util.List;

public final class CapeImage {

    private CapeImage() {}

    public static final int FACE_W = 10;
    public static final int FACE_H = 16;

    public static final int MAX_SCALE = 16;

    public static final long PIXEL_BUDGET = 4_000_000L;

    public static final int MAX_FRAMES = 120;

    public static final int STRIP_DELAY_MS = 100;

    private static final float INNER_SHADE = 0.6f;

    public record Frame(int width, int height, int[] argb, int delayMs) {}

    public static final int TEMPLATE_W = 22;
    public static final int TEMPLATE_H = 17;

    public static boolean template(int width, int height) {
        return width >= TEMPLATE_W && width % TEMPLATE_W == 0
                && (long) height * TEMPLATE_W == (long) width * TEMPLATE_H;
    }

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
                boolean corner = y < target && (x < target || x >= 21 * target);
                out[y * width + x] = corner ? 0 : opaque(used[y * tw + x]);
            }
        }
        return new Frame(width, 32 * target, out, delayMs);
    }

    public static boolean fullLayout(int width, int height) {
        return width >= 64 && width % 64 == 0 && height * 2 == width;
    }

    public static int stripFrames(int width, int height) {
        if (width < 64 || width % 64 != 0 || height % (width / 2) != 0) {
            return 0;
        }
        int frames = height / (width / 2);
        return frames >= 2 ? frames : 0;
    }

    public static int scaleFor(int sourceHeight, int frames) {
        int wanted = Math.max(1, Math.round(sourceHeight / (float) FACE_H));
        int limit = (int) Math.floor(Math.sqrt(
                PIXEL_BUDGET / (64.0 * 32.0 * Math.max(1, frames))));
        return Math.max(1, Math.min(MAX_SCALE, Math.min(wanted, limit)));
    }

    public static Frame face(int[] source, int sw, int sh, int k, int delayMs) {
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
                out[(k + y) * width + k + x] = pixel;
                out[(k + y) * width + 12 * k + (fw - 1 - x)] = shade(pixel);
            }
        }
        for (int y = 0; y < fh; y++) {
            for (int x = 0; x < k; x++) {
                out[(k + y) * width + x] = opaque(face[y * fw + x]);
                out[(k + y) * width + 11 * k + x] = opaque(face[y * fw + fw - k + x]);
            }
        }
        for (int y = 0; y < k; y++) {
            for (int x = 0; x < fw; x++) {
                out[y * width + k + x] = opaque(face[y * fw + x]);
                out[y * width + 11 * k + x] = opaque(face[(fh - k + y) * fw + x]);
            }
        }
        return new Frame(width, height, out, delayMs);
    }

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

    public static Frame layout(int[] source, int sw, int sh, int frames, int delayMs) {
        if (template(sw, sh)) {
            return fromTemplate(source, sw, sh, frames, delayMs);
        }
        return fullLayout(sw, sh)
                ? full(source, sw, sh, frames, delayMs)
                : face(source, sw, sh, scaleFor(sh, frames), delayMs);
    }

    public static Frame layoutDetached(int[] source, int sw, int sh, int frames, int delayMs) {
        Frame frame = layout(source, sw, sh, frames, delayMs);
        return frame.argb() == source
                ? new Frame(frame.width(), frame.height(), source.clone(), frame.delayMs())
                : frame;
    }

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
                        r += ((p >> 16) & 0xFF) * alpha;
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
