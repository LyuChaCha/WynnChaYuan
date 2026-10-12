package com.wynnchayuan.cape;

/**
 * 上傳的披風要符合什麼——跟登記站（{@code tools/presence/worker.js} 的 {@code inspectCape}）
 * 同一套規則。
 *
 * <p>登記站那邊會再驗一次，那一次才算數；這裡先驗是為了在送出去之前就告訴玩家哪裡不對，
 * 不必等一趟來回。兩邊的數字要一起改。
 *
 * <p>只收照範本畫的圖（使用者 2026-10-12：「防止別人亂傳圖片……需要使用我們範本」）：
 * 尺寸要是 {@code capes/templates} 那幾種，也就是 22×17 格的倍數——正面、背面、邊框都在
 * 圖上，用不到的空白已經裁掉。放在自己資料夾裡預覽的圖沒有這些限制，那是只有自己看得到的。
 */
public final class CapeRules {

    private CapeRules() {}

    /** 圖檔的上限。 */
    public static final int MAX_BYTES = 20 * 1024 * 1024;

    /** GIF 最多幾格。 */
    public static final int MAX_FRAMES = 120;

    /**
     * 收哪幾種尺寸：範本的 22×17 與它的 2、4、8、16、32、48 倍。見 {@link CapeImage#template}。
     *
     * <p>最大 1056×816（使用者 2026-10-12：「最多支援到 1000 左右」）。遊戲裡最多用到 16 倍
     * （{@link CapeImage#MAX_SCALE}），再大的在排版型時縮下來；大的主要是網站上看得清楚。
     */
    public static final int[][] SIZES = {{22, 17}, {44, 34}, {88, 68}, {176, 136},
            {352, 272}, {704, 544}, {1056, 816}};

    /** 哪裡不對。 */
    /** 一般玩家的上限（使用者 2026-10-12：「一般使用者只能上傳 1mb」）。名單上的成員是 {@link #MAX_BYTES}。 */
    public static final int MAX_BYTES_PLAIN = 1024 * 1024;

    /** 這個人的披風最大可以多大。跟登記站的 {@code capeLimit} 同一條規矩。 */
    public static int limitFor(boolean member) {
        return member ? MAX_BYTES : MAX_BYTES_PLAIN;
    }

    /**
     * 這張圖的寬與高（看檔頭，不解壓）。
     *
     * @return {@code {寬, 高}}；不是 PNG 也不是 GIF 就回 {@code null}
     */
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

    /** @return 可以上傳就回 {@code null} */
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
