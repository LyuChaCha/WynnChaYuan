package com.wynnchayuan.cape;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 讀 GIF：每一格都合成好（上一格留著、清掉、還原都照檔案說的做），附上那一格要停多久。
 *
 * <h2>為什麼自己讀</h2>
 * 披風要「直接丟一個 GIF 就能用」（使用者 2026-10-12）。現成的兩條路都不合用：
 * <ul>
 *   <li>{@code javax.imageio} 要碰 AWT，在 macOS 的遊戲環境裡有卡住的前科，而且拿到的是
 *       一格一格的<b>差異</b>，合成還是得自己來；</li>
 *   <li>LWJGL 的 stb 一次把所有格解進同一塊記憶體——格數與尺寸在解之前都擋不住，
 *       一個幾 MB 的檔可以要走幾百 MB。</li>
 * </ul>
 * 自己讀就可以先數格數、超過上限的不解，而且不碰 Minecraft，離線測得到。
 *
 * <p>格式只有這幾樣：檔頭、調色盤、圖形控制（停多久、哪一色透明、畫完怎麼處置）、
 * 影像區塊（LZW 壓的色號）。其餘的擴充區塊一律跳過。
 */
public final class Gif {

    private Gif() {}

    /** 畫布一邊最多幾個像素：最大的範本是 1056×816。 */
    public static final int MAX_SIDE = 1056;

    /** 一格一格收：畫布是解碼器自己那一張，下一格就會被改掉，要留就自己複製或當場用掉。 */
    public interface Sink {
        void frame(int[] canvas, int width, int height, int delayMs);
    }

    /**
     * 一格一格解，解一格交一格，不把每一格都留在記憶體裡。
     *
     * <p>大張的動畫要這樣讀：1056×816 的 120 格全留著是四百多 MB，一格一格交出去
     * （接的人當場縮成披風貼圖）就只有一張畫布。
     *
     * @return 交出去幾格
     * @throws IOException 不是 GIF、尺寸離譜、或檔案是壞的
     */
    public static int each(byte[] data, int maxFrames, Sink sink) throws IOException {
        int count = walk(data, maxFrames, true, sink).frames().size();
        if (count == 0) {
            throw new IOException("GIF 裡沒有影像");
        }
        return count;
    }

    /** 停留時間寫 0 或太短的，照瀏覽器的慣例當成十分之一秒。 */
    private static final int DEFAULT_DELAY_MS = 100;
    private static final int MIN_DELAY_MS = 20;

    /** @param argb 整張畫布，一列接一列；沒畫到的地方是 0（全透明） */
    public record Frame(int[] argb, int delayMs) {}

    /** @param complete 有讀到檔尾（沒有的話是下載到一半、或被截斷的檔） */
    public record Image(int width, int height, List<Frame> frames, boolean complete) {}

    public static boolean looksLikeGif(byte[] data) {
        return data != null && data.length >= 13 && data[0] == 'G' && data[1] == 'I'
                && data[2] == 'F' && data[3] == '8' && (data[4] == '7' || data[4] == '9')
                && data[5] == 'a';
    }

    /**
     * 這個檔有幾格。只走區塊、不解壓——用來在真的解之前決定要不要解、要解多大。
     *
     * @return 格數；檔案壞掉時回到壞掉之前數到的
     */
    public static int countFrames(byte[] data) {
        try {
            return walk(data, Integer.MAX_VALUE, false, null).frames().size();
        } catch (IOException e) {
            return 0;
        }
    }

    /** 這個檔從頭到尾是完整的嗎：每個區塊都走得完，而且有檔尾。 */
    public static boolean complete(byte[] data) {
        try {
            return walk(data, Integer.MAX_VALUE, false, null).complete();
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 解出每一格。
     *
     * @param maxFrames 最多解幾格，後面的不要了
     * @throws IOException 不是 GIF、尺寸離譜、或檔案是壞的
     */
    public static Image decode(byte[] data, int maxFrames) throws IOException {
        Image image = walk(data, maxFrames, true, null);
        if (image.frames().isEmpty()) {
            throw new IOException("GIF 裡沒有影像");
        }
        return image;
    }

    private static Image walk(byte[] data, int maxFrames, boolean pixels, Sink sink)
            throws IOException {
        if (!looksLikeGif(data)) {
            throw new IOException("不是 GIF");
        }
        try {
            int width = u16(data, 6);
            int height = u16(data, 8);
            if (width <= 0 || height <= 0 || width > MAX_SIDE || height > MAX_SIDE) {
                throw new IOException("GIF 尺寸不合理：" + width + "×" + height);
            }
            int flags = data[10] & 0xFF;
            int pos = 13;
            int[] global = null;
            if ((flags & 0x80) != 0) {
                int size = 1 << ((flags & 7) + 1);
                global = palette(data, pos, size);
                pos += size * 3;
            }
            int[] canvas = pixels ? new int[width * height] : null;
            List<Frame> frames = new ArrayList<>();
            int delay = DEFAULT_DELAY_MS;
            int transparent = -1;
            int disposal = 0;
            boolean complete = false;
            while (pos < data.length && frames.size() < maxFrames) {
                int block = data[pos++] & 0xFF;
                if (block == 0x3B) {
                    complete = true;                   // 檔尾
                    break;
                }
                if (block == 0x21) {
                    int label = data[pos++] & 0xFF;
                    if (label == 0xF9 && (data[pos] & 0xFF) >= 4) {
                        int packed = data[pos + 1] & 0xFF;
                        int hundredths = u16(data, pos + 2);
                        delay = hundredths * 10 < MIN_DELAY_MS ? DEFAULT_DELAY_MS : hundredths * 10;
                        transparent = (packed & 1) != 0 ? data[pos + 4] & 0xFF : -1;
                        disposal = (packed >> 2) & 7;
                    }
                    pos = skipSubBlocks(data, pos);
                    continue;
                }
                if (block != 0x2C) {
                    throw new IOException("GIF 裡有不認得的區塊：" + block);
                }
                int left = u16(data, pos);
                int top = u16(data, pos + 2);
                int w = u16(data, pos + 4);
                int h = u16(data, pos + 6);
                int local = data[pos + 8] & 0xFF;
                pos += 9;
                int[] colours = global;
                if ((local & 0x80) != 0) {
                    int size = 1 << ((local & 7) + 1);
                    colours = pixels ? palette(data, pos, size) : colours;
                    pos += size * 3;
                }
                int minCode = data[pos++] & 0xFF;
                if (!pixels) {
                    pos = skipSubBlocks(data, pos);
                    frames.add(new Frame(null, delay));
                } else {
                    ByteArrayOutputStream packed = new ByteArrayOutputStream();
                    pos = readSubBlocks(data, pos, packed);
                    if (colours == null || w <= 0 || h <= 0 || minCode < 1 || minCode > 8) {
                        throw new IOException("GIF 的影像區塊是壞的");
                    }
                    byte[] indices = new byte[w * h];
                    unpack(packed.toByteArray(), minCode, indices);
                    int[] before = disposal == 3 ? canvas.clone() : null;
                    draw(canvas, width, height, indices, left, top, w, h,
                            (local & 0x40) != 0, colours, transparent);
                    if (sink != null) {
                        sink.frame(canvas, width, height, delay);
                        frames.add(new Frame(null, delay));
                    } else {
                        frames.add(new Frame(canvas.clone(), delay));
                    }
                    if (disposal == 2) {
                        clear(canvas, width, height, left, top, w, h);
                    } else if (before != null) {
                        canvas = before;
                    }
                }
                delay = DEFAULT_DELAY_MS;
                transparent = -1;
                disposal = 0;
            }
            return new Image(width, height, frames, complete);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IOException("GIF 檔不完整");
        }
    }

    private static int u16(byte[] data, int at) {
        return (data[at] & 0xFF) | ((data[at + 1] & 0xFF) << 8);
    }

    private static int[] palette(byte[] data, int at, int size) {
        int[] out = new int[size];
        for (int i = 0; i < size; i++) {
            out[i] = 0xFF000000 | ((data[at + i * 3] & 0xFF) << 16)
                    | ((data[at + i * 3 + 1] & 0xFF) << 8) | (data[at + i * 3 + 2] & 0xFF);
        }
        return out;
    }

    /** 跳過一串子區塊（長度、內容、長度、內容……到長度是 0 為止）。 */
    private static int skipSubBlocks(byte[] data, int pos) {
        while (true) {
            int size = data[pos++] & 0xFF;
            if (size == 0) {
                return pos;
            }
            pos += size;
        }
    }

    private static int readSubBlocks(byte[] data, int pos, ByteArrayOutputStream out) {
        while (true) {
            int size = data[pos++] & 0xFF;
            if (size == 0) {
                return pos;
            }
            if (pos + size > data.length) {
                throw new ArrayIndexOutOfBoundsException();
            }
            out.write(data, pos, size);
            pos += size;
        }
    }

    /** LZW 解壓：位元流裡是長度會變的碼，每個碼代表一串色號。 */
    private static void unpack(byte[] packed, int minCode, byte[] out) {
        int clear = 1 << minCode;
        int end = clear + 1;
        short[] prefix = new short[4096];
        byte[] suffix = new byte[4096];
        byte[] stack = new byte[4097];
        for (int i = 0; i < clear; i++) {
            suffix[i] = (byte) i;
        }
        int codeSize = minCode + 1;
        int mask = (1 << codeSize) - 1;
        int available = clear + 2;
        int old = -1;
        int first = 0;
        int bits = 0;
        int datum = 0;
        int at = 0;
        int written = 0;
        int top = 0;
        while (written < out.length) {
            if (top == 0) {
                if (bits < codeSize) {
                    if (at >= packed.length) {
                        break;                         // 資料用完了，剩下的留透明色號 0
                    }
                    datum |= (packed[at++] & 0xFF) << bits;
                    bits += 8;
                    continue;
                }
                int code = datum & mask;
                datum >>= codeSize;
                bits -= codeSize;
                if (code == clear) {
                    codeSize = minCode + 1;
                    mask = (1 << codeSize) - 1;
                    available = clear + 2;
                    old = -1;
                    continue;
                }
                if (code == end || code > available) {
                    break;
                }
                if (old == -1) {
                    stack[top++] = suffix[code];
                    old = code;
                    first = code;
                    continue;
                }
                int in = code;
                if (code == available) {
                    stack[top++] = (byte) first;
                    code = old;
                }
                while (code > clear) {
                    stack[top++] = suffix[code];
                    code = prefix[code];
                }
                first = suffix[code] & 0xFF;
                stack[top++] = (byte) first;
                if (available < 4096) {
                    prefix[available] = (short) old;
                    suffix[available] = (byte) first;
                    available++;
                    if ((available & mask) == 0 && available < 4096) {
                        codeSize++;
                        mask += available;
                    }
                }
                old = in;
            }
            out[written++] = stack[--top];
        }
    }

    /** 交錯排列的四輪：從第幾列開始、每次跳幾列。 */
    private static final int[][] INTERLACE = {{0, 8}, {4, 8}, {2, 4}, {1, 2}};

    private static void draw(int[] canvas, int width, int height, byte[] indices,
                             int left, int top, int w, int h, boolean interlaced,
                             int[] colours, int transparent) {
        int row = 0;
        if (interlaced) {
            for (int[] pass : INTERLACE) {
                for (int y = pass[0]; y < h; y += pass[1]) {
                    drawRow(canvas, width, height, indices, row++, left, top + y, w,
                            colours, transparent);
                }
            }
        } else {
            for (int y = 0; y < h; y++) {
                drawRow(canvas, width, height, indices, row++, left, top + y, w,
                        colours, transparent);
            }
        }
    }

    private static void drawRow(int[] canvas, int width, int height, byte[] indices, int row,
                                int left, int y, int w, int[] colours, int transparent) {
        if (y < 0 || y >= height) {
            return;
        }
        for (int x = 0; x < w; x++) {
            int index = indices[row * w + x] & 0xFF;
            int cx = left + x;
            if (index == transparent || index >= colours.length || cx < 0 || cx >= width) {
                continue;
            }
            canvas[y * width + cx] = colours[index];
        }
    }

    private static void clear(int[] canvas, int width, int height,
                              int left, int top, int w, int h) {
        for (int y = Math.max(0, top); y < Math.min(height, top + h); y++) {
            for (int x = Math.max(0, left); x < Math.min(width, left + w); x++) {
                canvas[y * width + x] = 0;
            }
        }
    }
}
