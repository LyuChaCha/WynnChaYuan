package com.wynnchayuan.cape;

import com.google.gson.JsonParser;
import com.wynnchayuan.client.Credits;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 披風的圖：GIF 讀得對、隨便什麼尺寸的圖都排得進披風的版型。
 *
 * <h2>這裡釘住什麼</h2>
 * <ul>
 *   <li>GIF 的每一格合成得對：交錯排列、上一格留著、上一格清掉、停留時間；</li>
 *   <li>壞掉的、太大的檔會被擋下來，不會把遊戲拖垮；</li>
 *   <li>只畫一面的圖：外側就是那張圖，內側是反過來的暗色版，四邊有補；</li>
 *   <li>完整版型照用，Wynntils 那種直排多格的 PNG 拆成動畫；</li>
 *   <li>格數多的動畫每一格自動縮小，整件披風不超過預算；</li>
 *   <li>名單上 {@code cape} 那一欄讀得到，怪檔名不收。</li>
 * </ul>
 * 測試用的 GIF 是 {@code tools4/cape_fixtures.py} 照公式畫的，這裡用同一條公式驗。
 */
public final class CapeImageTest {

    private static int failures = 0;

    private static final int[] PAL = {0xFFDC2828, 0xFF28C850, 0xFF325AE6, 0xFFF0DC3C};
    private static final int W = 20;
    private static final int H = 32;

    public static void main(String[] args) throws Exception {
        gif();
        broken();
        face();
        template();
        fullAndStrip();
        budget();
        credits();
        System.out.println(failures == 0 ? "披風的圖：全部通過" : "披風的圖：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static byte[] file(String name) throws Exception {
        return Files.readAllBytes(Path.of("src/test/resources/cape", name));
    }

    /** 測試圖的公式：第 f 格的 (x, y) 是調色盤的第幾色。 */
    private static int want(int f, int x, int y) {
        return PAL[(f + x / 5 + y / 8) % 4];
    }

    private static boolean matches(int[] argb, int f) {
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                if (argb[y * W + x] != want(f, x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void gif() throws Exception {
        byte[] anim = file("anim.gif");
        check("認得出是 GIF", Gif.looksLikeGif(anim) && !Gif.looksLikeGif(new byte[] {1, 2, 3}));
        check("先數格數不必解壓（實際 " + Gif.countFrames(anim) + "）", Gif.countFrames(anim) == 3);
        Gif.Image image = Gif.decode(anim, 99);
        check("尺寸對", image.width() == W && image.height() == H);
        check("三格都解出來", image.frames().size() == 3);
        for (int f = 0; f < image.frames().size(); f++) {
            check("★ 第 " + (f + 1) + " 格每一個像素都對", matches(image.frames().get(f).argb(), f));
        }
        check("★ 停留時間照檔案的；寫 0 的當成十分之一秒（實際 "
                        + image.frames().stream().map(Gif.Frame::delayMs).toList() + "）",
                image.frames().get(0).delayMs() == 50 && image.frames().get(1).delayMs() == 120
                        && image.frames().get(2).delayMs() == 100);
        check("只要兩格就只解兩格", Gif.decode(anim, 2).frames().size() == 2);

        int[] plain = Gif.decode(file("plain.gif"), 9).frames().get(0).argb();
        int[] interlaced = Gif.decode(file("interlaced.gif"), 9).frames().get(0).argb();
        check("單格的圖解得對", matches(plain, 3));
        check("★ 交錯排列的解出來一模一樣", Arrays.equals(plain, interlaced));

        // 第一格整張紅；之後每一格左半換色、右半透明
        List<Gif.Frame> keep = Gif.decode(file("keep.gif"), 9).frames();
        check("★ 上一格留著：透明的地方露出上一格（實際 "
                        + Integer.toHexString(keep.get(1).argb()[W - 1]) + "）",
                keep.get(1).argb()[0] == PAL[1] && keep.get(1).argb()[W - 1] == PAL[0]
                        && keep.get(2).argb()[0] == PAL[2] && keep.get(2).argb()[W - 1] == PAL[0]);
        List<Gif.Frame> clear = Gif.decode(file("clear.gif"), 9).frames();
        check("★ 上一格清掉：透明的地方是空的（實際 "
                        + Integer.toHexString(clear.get(2).argb()[W - 1]) + "）",
                clear.get(2).argb()[0] == PAL[2] && clear.get(2).argb()[W - 1] == 0);
    }

    private static void broken() throws Exception {
        byte[] anim = file("anim.gif");
        check("★ 下載到一半的檔會丟例外，不是卡住或亂畫",
                throwsOn(Arrays.copyOf(anim, anim.length / 3)));
        check("不是 GIF 的丟例外", throwsOn("<html>404</html>".getBytes()));
        byte[] huge = anim.clone();
        huge[6] = (byte) 0xFF;                       // 寬改成 65535
        huge[7] = (byte) 0xFF;
        check("★ 尺寸離譜的不解", throwsOn(huge) && Gif.countFrames(huge) == 0);
        byte[] noise = anim.clone();
        for (int i = 40; i < noise.length; i += 3) {
            noise[i] ^= 0x5A;
        }
        boolean survived;
        try {
            Gif.decode(noise, 9);
            survived = true;
        } catch (java.io.IOException e) {
            survived = true;                         // 丟 IOException 也算擋住了
        } catch (Throwable t) {
            survived = false;
        }
        check("★ 內容被打亂的檔不會丟出別種例外", survived);
    }

    private static boolean throwsOn(byte[] data) {
        try {
            Gif.decode(data, 9);
            return false;
        } catch (java.io.IOException e) {
            return true;
        }
    }

    /** 只有一面的圖。 */
    private static void face() {
        // 20×32 的圖剛好是 10:16，放大兩倍時一格對一格
        int[] source = new int[W * H];
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                source[y * W + x] = 0xFF000000 | (x * 12 << 16) | (y * 7 << 8) | 0x40;
            }
        }
        check("高 32 的圖放大兩倍", CapeImage.scaleFor(H, 1) == 2);
        CapeImage.Frame out = CapeImage.layout(source, W, H, 1, 70);
        int k = 2;
        check("貼圖是 64k×32k（實際 " + out.width() + "×" + out.height() + "）",
                out.width() == 128 && out.height() == 64 && out.delayMs() == 70);
        boolean outer = true;
        boolean inner = true;
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                int p = source[y * W + x];
                outer &= out.argb()[(k + y) * 128 + k + x] == p;
                int q = out.argb()[(k + y) * 128 + 12 * k + (W - 1 - x)];
                inner &= (q >>> 24) == 0xFF && ((q >> 16) & 0xFF) == (int) (((p >> 16) & 0xFF) * 0.6f);
            }
        }
        check("★ 外側就是那張圖，一個像素都沒變", outer);
        check("★ 內側是左右反過來的暗色版", inner);
        check("左右兩邊與上下緣有補上（不是透明的）",
                (out.argb()[(k + 5) * 128] >>> 24) == 0xFF
                        && (out.argb()[(k + 5) * 128 + 11 * k] >>> 24) == 0xFF
                        && (out.argb()[k + 3] >>> 24) == 0xFF
                        && (out.argb()[11 * k + 3] >>> 24) == 0xFF);
        check("鞘翅那一塊留空", out.argb()[10 * 128 + 100] == 0);

        // 比例不對的圖：置中裁成 10:16
        int[] wide = new int[64 * 32];
        Arrays.fill(wide, 0xFF112233);
        for (int y = 0; y < 32; y++) {
            for (int x = 22; x < 42; x++) {
                wide[y * 64 + x] = 0xFFAABBCC;        // 中間 20 欄才是會留下來的
            }
        }
        // 注意：64×32 剛好是完整版型的尺寸，所以這裡直接問 face
        CapeImage.Frame cropped = CapeImage.face(wide, 64, 32, 2, 0);
        check("★ 太寬的圖取中間那一段（實際 "
                        + Integer.toHexString(cropped.argb()[(2 + 10) * 128 + 2 + 10]) + "）",
                cropped.argb()[(2 + 10) * 128 + 2] == 0xFFAABBCC
                        && cropped.argb()[(2 + 10) * 128 + 2 + 19] == 0xFFAABBCC);

        // 大圖縮小：四個像素平均成一個，半透明的壓在黑底上
        int[] big = new int[40 * 64];
        Arrays.fill(big, 0xFF808080);
        big[0] = 0xFFFFFFFF;
        big[1] = 0xFF000000;
        big[40] = 0xFFFFFFFF;
        big[41] = 0xFF000000;
        CapeImage.Frame small = CapeImage.face(big, 40, 64, 2, 0);
        int corner = small.argb()[2 * 128 + 2];
        check("★ 縮小時取平均，不是只挑一個像素（實際 " + Integer.toHexString(corner) + "）",
                Math.abs(((corner >> 16) & 0xFF) - 0x7F) <= 1);
        check("半透明的像素壓在黑底上變不透明",
                CapeImage.opaque(0x80FF0000) == 0xFF800000 && CapeImage.opaque(0) == 0xFF000000);
    }

    /** 我們的範本：裁掉空白的版型，內容原封不動放回完整貼圖的左上角。 */
    private static void template() throws Exception {
        check("22×17 與它的倍數是範本", CapeImage.template(22, 17) && CapeImage.template(44, 34)
                && CapeImage.template(176, 136));
        check("別的尺寸不是（64×32、20×32、23×17、44×17）", !CapeImage.template(64, 32)
                && !CapeImage.template(20, 32) && !CapeImage.template(23, 17)
                && !CapeImage.template(44, 17));
        int k = 2;
        int w = 22 * k;
        int h = 17 * k;
        int[] source = new int[w * h];
        for (int i = 0; i < source.length; i++) {
            source[i] = 0xFF000000 | (i * 37 & 0xFFFFFF);
        }
        CapeImage.Frame out = CapeImage.layout(source, w, h, 1, 55);
        check("★ 貼圖是完整的 64k×32k（實際 " + out.width() + "×" + out.height() + "）",
                out.width() == 64 * k && out.height() == 32 * k && out.delayMs() == 55);
        boolean same = true;
        boolean blank = true;
        for (int y = 0; y < out.height(); y++) {
            for (int x = 0; x < out.width(); x++) {
                int p = out.argb()[y * out.width() + x];
                boolean corner = y < k && (x < k || x >= 21 * k);
                if (x < w && y < h && !corner) {
                    same &= p == source[y * w + x];
                } else {
                    blank &= p == 0;
                }
            }
        }
        check("★ 範本的內容原封不動在左上角：正面、背面、邊框的位置跟完整版型一樣", same);
        check("★ 範本以外的地方（原本的空白）是空的", blank);
        // 正面在 (1k,1k)、背面在 (12k,1k)：跟只畫一面時補出來的位置是同一個
        check("正面與背面落在披風模型讀的位置",
                out.argb()[k * out.width() + k] == source[k * w + k]
                        && out.argb()[k * out.width() + 12 * k] == source[k * w + 12 * k]);
        int[] big = new int[176 * 136];
        java.util.Arrays.fill(big, 0xFF336699);
        CapeImage.Frame shrunk = CapeImage.fromTemplate(big, 176, 136, 100, 0);
        check("★ 格數多的大範本自動縮小（實際 " + shrunk.width() + "×" + shrunk.height() + "）",
                100L * shrunk.width() * shrunk.height() <= CapeImage.PIXEL_BUDGET
                        && shrunk.argb()[(shrunk.width() / 64) * shrunk.width() + shrunk.width() / 64]
                                == 0xFF336699);
        // ★ 一格一格交出去的讀法（大張動畫用）：內容跟整份解出來的一樣，而且每一格各自獨立
        Gif.Image whole = Gif.decode(file("anim.gif"), 99);
        List<int[]> streamed = new java.util.ArrayList<>();
        List<int[]> canvases = new java.util.ArrayList<>();
        int handed = Gif.each(file("anim.gif"), 99, (canvas, gw, gh, delayMs) -> {
            canvases.add(canvas);
            streamed.add(canvas.clone());
        });
        boolean sameFrames = handed == whole.frames().size();
        for (int i = 0; sameFrames && i < handed; i++) {
            sameFrames = Arrays.equals(streamed.get(i), whole.frames().get(i).argb());
        }
        check("★ 一格一格讀：格數與每一格的內容都跟整份解出來的一樣（" + handed + " 格）",
                handed > 1 && sameFrames);
        check("只要兩格就只交兩格", Gif.each(file("anim.gif"), 2, (canvas, gw, gh, delayMs) -> { }) == 2);
        int[] canvas64 = new int[64 * 32];
        java.util.Arrays.fill(canvas64, 0xFF112233);
        CapeImage.Frame first = CapeImage.layoutDetached(canvas64, 64, 32, 2, 100);
        java.util.Arrays.fill(canvas64, 0xFF445566);                  // 解碼器接著畫下一格
        check("★ 完整版型的動畫：畫布被下一格蓋掉，前一格還是原本的樣子",
                first.argb() != canvas64 && first.argb()[0] == 0xFF112233
                        && CapeImage.layout(canvas64, 64, 32, 2, 100).argb() == canvas64);
        int[] huge = new int[1056 * 816];
        java.util.Arrays.fill(huge, 0xFF336699);
        CapeImage.Frame fromHuge = CapeImage.layoutDetached(huge, 1056, 816, 120, 100);
        check("★ 最大的範本（1056×816）120 格：縮到預算以內（實際 " + fromHuge.width() + "×"
                        + fromHuge.height() + "）", CapeImage.template(1056, 816)
                && 120L * fromHuge.width() * fromHuge.height() <= CapeImage.PIXEL_BUDGET
                && fromHuge.argb()[(fromHuge.width() / 64) * fromHuge.width() + fromHuge.width() / 64]
                        == 0xFF336699);
        CapeImage.Frame still = CapeImage.layout(huge, 1056, 816, 1, 0);
        check("最大的範本單張：遊戲裡用 16 倍（實際 " + still.width() + "×" + still.height() + "）",
                still.width() == 64 * CapeImage.MAX_SCALE);
        check("GIF 的畫布上限跟最大的範本一樣", Gif.MAX_SIDE == 1056);
        Gif.Image gif = Gif.decode(file("template.gif"), 9);
        check("範本尺寸的 GIF：22×17、三格，每一格都排得進去", gif.width() == 22 && gif.height() == 17
                && gif.frames().size() == 3 && CapeImage.layout(gif.frames().get(1).argb(),
                        22, 17, 3, 100).width() == 64);
    }

    private static void fullAndStrip() {
        check("64×32、128×64 是完整版型", CapeImage.fullLayout(64, 32) && CapeImage.fullLayout(128, 64));
        check("20×32、64×64、100×50 不是", !CapeImage.fullLayout(20, 32)
                && !CapeImage.fullLayout(64, 64) && !CapeImage.fullLayout(100, 50));
        int[] full = new int[64 * 32];
        for (int i = 0; i < full.length; i++) {
            full[i] = 0xFF000000 | i;
        }
        CapeImage.Frame same = CapeImage.layout(full, 64, 32, 1, 0);
        check("★ 完整版型原樣照用", same.width() == 64 && Arrays.equals(same.argb(), full));

        // Wynntils 的動畫披風：128×64 的版型直排 16 格
        check("128×1024 是 16 格（實際 " + CapeImage.stripFrames(128, 1024) + "）",
                CapeImage.stripFrames(128, 1024) == 16);
        check("單獨一格、或高度對不上的不算直排", CapeImage.stripFrames(128, 64) == 0
                && CapeImage.stripFrames(128, 100) == 0 && CapeImage.stripFrames(20, 320) == 0);
        int[] strip = new int[64 * 32 * 3];
        for (int f = 0; f < 3; f++) {
            Arrays.fill(strip, f * 64 * 32, (f + 1) * 64 * 32, 0xFF000000 | (f + 1));
        }
        List<CapeImage.Frame> frames = CapeImage.strip(strip, 64, 96);
        check("★ 直排的三格拆成三格，順序從上往下",
                frames.size() == 3 && frames.get(0).argb()[5] == 0xFF000001
                        && frames.get(2).argb()[5] == 0xFF000003
                        && frames.get(1).delayMs() == CapeImage.STRIP_DELAY_MS);
    }

    private static void budget() {
        check("一張圖最多放大 16 倍", CapeImage.scaleFor(4000, 1) == CapeImage.MAX_SCALE);
        check("很小的圖至少一倍", CapeImage.scaleFor(8, 1) == 1);
        int k = CapeImage.scaleFor(512, 100);
        long pixels = 100L * (64 * k) * (32 * k);
        check("★ 一百格的動畫自動縮小，整件不超過預算（倍數 " + k + "，共 " + pixels + " 個像素）",
                k >= 1 && pixels <= CapeImage.PIXEL_BUDGET);
        int[] big = new int[512 * 256];
        CapeImage.Frame shrunk = CapeImage.full(big, 512, 256, 100, 0);
        check("★ 完整版型的大圖格數多時也縮小（實際 " + shrunk.width() + "×" + shrunk.height() + "）",
                100L * shrunk.width() * shrunk.height() <= CapeImage.PIXEL_BUDGET
                        && shrunk.width() == shrunk.height() * 2);
    }

    private static void credits() {
        String json = """
                {"sections": [{"role": "開發者", "members": [
                  {"name": "A", "mc": "Green_teaTW", "cape": "leaf.gif"},
                  {"name": "B", "mc": "Someone", "cape": "../../evil.png"},
                  {"name": "C", "mc": "NoCape"},
                  {"name": "D", "cape": "orphan.png"},
                  {"name": "E", "mc": "Shouty", "cape": "UPPER.PNG"}]}]}""";
        JsonParser.parseString(json);
        List<Credits.Section> sections = Credits.read(new StringReader(json));
        Map<String, String> table = Capes.build(sections);
        check("★ 名單上的 cape 欄讀得到，照遊戲 ID 查（不分大小寫）",
                "leaf.gif".equals(table.get("green_teatw")));
        check("★ 怪檔名不收——它要拿去拼網址與路徑（實際 " + table + "）",
                !table.containsKey("someone") && !table.containsKey("shouty"));
        check("沒有披風、沒有遊戲 ID 的不在表上", table.size() == 1);
        check("★ 自己資料夾的預覽：名單成員認 GIF 與 PNG，一般玩家只認 PNG",
                java.util.Arrays.equals(Capes.selfFiles(true), new String[] {"self.gif", "self.png"})
                        && java.util.Arrays.equals(Capes.selfFiles(false), new String[] {"self.png"}));
        // 自己身上穿哪一件：預覽圖與登記站那一件，誰比較新就穿誰
        check("★ 預覽圖改得比登記站那一件晚：先看到預覽圖（還沒上傳的那一張）",
                "local:self.png:2000".equals(Capes.ownKey("local:self.png:2000", "up:abc:1000", 1000)));
        check("★ 之後在網站換了一件（或復原）：登記站那一件比較新，自己也看得到",
                "up:abc:3000".equals(Capes.ownKey("local:self.png:2000", "up:abc:3000", 3000)));
        check("只有其中一個就用那一個；都沒有就沒有",
                "up:abc:5".equals(Capes.ownKey(null, "up:abc:5", 5))
                        && "local:self.gif:9".equals(Capes.ownKey("local:self.gif:9", null, 0))
                        && Capes.ownKey(null, null, 0) == null);
        check("預覽圖的時間看不懂：照舊，預覽圖優先",
                "local:self.png:??".equals(Capes.ownKey("local:self.png:??", "up:abc:3000", 3000)));
        check("檔名的規則", Capes.FILE.matcher("my-cape_2.png").matches()
                && !Capes.FILE.matcher("a/b.png").matches() && !Capes.FILE.matcher("x.jpg").matches()
                && !Capes.FILE.matcher(".png").matches());
    }

    private static void check(String what, boolean ok) {
        System.out.println((ok ? "  ok   " : "  FAIL ") + what);
        if (!ok) {
            failures++;
        }
    }
}
