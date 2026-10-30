package com.wynnchayuan.cape;

public final class CapeRules {

    private CapeRules() {}

    public static final int MAX_BYTES = 20 * 1024 * 1024;

    public static final int MAX_FRAMES = 120;

    public static final int[][] SIZES = {{22, 17}, {44, 34}, {88, 68}, {176, 136},
            {352, 272}, {704, 544}, {1056, 816}};

    public static final int MAX_BYTES_PLAIN = 1024 * 1024;

    public static int limitFor(boolean member) {
        return member ? MAX_BYTES : MAX_BYTES_PLAIN;
    }

    public static int[] size(byte[] bytes) {
        if (bytes == null || bytes.length < 24) {
            return null;
        }
        if (isPng(bytes)) {
            return new int[] {u32(bytes, 16), u32(bytes, 20)};
        }
        if (Gif.looksLikeGif(bytes)) {
            return new int[] {(bytes[6] & 0xFF) | ((bytes[7] & 0xFF) << 8),
                    (bytes[8] & 0xFF) | ((bytes[9] & 0xFF) << 8)};
        }
        return null;
    }

    public enum Problem { TOO_LARGE, BAD_FORMAT, BAD_SIZE, TOO_MANY_FRAMES }

    public static Problem check(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return Problem.BAD_FORMAT;
        }
        if (bytes.length > MAX_BYTES) {
            return Problem.TOO_LARGE;
        }
        int width;
        int height;
        if (isPng(bytes)) {
            width = u32(bytes, 16);
            height = u32(bytes, 20);
        } else if (Gif.looksLikeGif(bytes)) {
            width = (bytes[6] & 0xFF) | ((bytes[7] & 0xFF) << 8);
            height = (bytes[8] & 0xFF) | ((bytes[9] & 0xFF) << 8);
            int frames = Gif.countFrames(bytes);
            if (frames < 1 || !Gif.complete(bytes)) {
                return Problem.BAD_FORMAT;
            }
            if (frames > MAX_FRAMES) {
                return Problem.TOO_MANY_FRAMES;
            }
        } else {
            return Problem.BAD_FORMAT;
        }
        for (int[] size : SIZES) {
            if (size[0] == width && size[1] == height) {
                return null;
            }
        }
        return Problem.BAD_SIZE;
    }

    private static final int[] PNG = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private static boolean isPng(byte[] bytes) {
        if (bytes.length < 24) {
            return false;
        }
        for (int i = 0; i < PNG.length; i++) {
            if ((bytes[i] & 0xFF) != PNG[i]) {
                return false;
            }
        }
        return bytes[12] == 'I' && bytes[13] == 'H' && bytes[14] == 'D' && bytes[15] == 'R';
    }

    private static int u32(byte[] bytes, int at) {
        return ((bytes[at] & 0xFF) << 24) | ((bytes[at + 1] & 0xFF) << 16)
                | ((bytes[at + 2] & 0xFF) << 8) | (bytes[at + 3] & 0xFF);
    }
}
