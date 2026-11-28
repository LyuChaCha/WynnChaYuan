package com.wynnchayuan.client;

import com.wynnchayuan.client.ui.PanelDrag;
import com.wynnchayuan.client.ui.PositionView;
import com.wynnchayuan.client.ui.PositionView.Rect;
import com.wynnchayuan.client.ui.PositionView.Spot;
import com.wynnchayuan.client.ui.Surface;

import javax.imageio.ImageIO;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class PositionEditorTest {

    private static final Path SRC = Path.of("src/main/java/com/wynnchayuan");
    private static final int W = 640;
    private static final int H = 360;

    private static int failures;
    private static long clock = 1000;

    private static void must(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }

    public static void main(String[] args) throws Exception {
        new File("build/ui-preview").mkdirs();
        dragMath();
        onePanel();
        pinsTooltip();
        cursorLeavesItem();
        clickMovesItem();
        dragsItem();
        sideKeepsItem();
        side();
        cancel();
        resize();
        resetThis();
        sharedDrawing();
        if (failures > 0) {
            System.out.println("PositionEditor: " + failures + " 項失敗");
            System.exit(1);
        }
        System.out.println("PositionEditor: 全部通過");
    }

    private static AwtCanvas render(Surface view, double mx, double my) {
        AwtCanvas c = new AwtCanvas(W, H, 1, "zh_tw");
        c.backdrop();
        view.render(c, W, H, mx, my, clock += 90);
        c.done();
        return c;
    }

    private static PositionView editor(FakePanels host) throws Exception {
        PositionView view = new PositionView(new SubViewsTest.FakeShell("zh_tw"), host);
        render(view, -100, -100);
        return view;
    }

    private static void press(Surface view, String key) {
        float[] r = view.spot(key);
        must("找得到 " + key, r != null);
        if (r != null) {
            view.mouseDown(r[0] + r[2] / 2, r[1] + r[3] / 2, 0);
            view.mouseUp();
        }
    }

    private static void drag(PositionView view, String key, double dx, double dy) {
        float[] r = view.spot(key);
        must("找得到 " + key, r != null);
        if (r == null) {
            return;
        }
        double x = r[0] + Math.min(5, r[2] / 2);
        double y = r[1] + Math.min(5, r[3] / 2);
        view.mouseDown(x, y, 0);
        view.mouseDrag(x + dx / 2, y + dy / 2);
        view.mouseDrag(x + dx, y + dy);
        view.mouseUp();
        render(view, -100, -100);
    }

    private static void dragMath() {
        PanelDrag.Moved near = PanelDrag.move(W / 2 - 50 + 4, 100, 100, 40, W, H, 2);
        must("離中線 4px 會吸到中線", near.snapX() && near.x() == W / 2 - 50);
        PanelDrag.Moved far = PanelDrag.move(W / 2 - 50 + 6, 100, 100, 40, W, H, 2);
        must("離中線 6px 不吸", !far.snapX() && far.x() == W / 2 - 44);
        PanelDrag.Moved middle = PanelDrag.move(10, H / 2 - 20 - 3, 100, 40, W, H, 2);
        must("上下也吸水平中線", middle.snapY() && middle.y() == H / 2 - 20);
        PanelDrag.Moved off = PanelDrag.move(-80, 500, 100, 40, W, H, 2);
        must("拖出左下角會被擋在畫面裡（" + off.x() + "," + off.y() + "）",
                off.x() == 2 && off.y() == H - 40 - 2);
        PanelDrag.Moved right = PanelDrag.move(900, -30, 100, 40, W, H, 0);
        must("右上角也擋住，邊距照面板給的", right.x() == W - 100 && right.y() == 0);

        PositionView.Size tall = new PositionView.Size(100, 640, 24, 320, true);
        int[] small = PanelDrag.resize(20, 5, 10, 10, tall, W, H, 2);
        must("縮到最小以下停在下限", small[0] == 100 && small[1] == 24);
        int[] big = PanelDrag.resize(900, 900, 300, 200, tall, W, H, 2);
        must("拉大不會超出畫面（" + big[0] + "×" + big[1] + "）",
                big[0] == W - 300 - 2 && big[1] == H - 200 - 2);
        int[] capped = PanelDrag.resize(900, 900, 0, 0, tall, 2000, 2000, 2);
        must("畫面再大也有上限", capped[0] == 640 && capped[1] == 320);
        int[] wide = PanelDrag.resize(150, 999, 10, 10, new PositionView.Size(100, 420, 0, 0, false),
                W, H, 2);
        must("只能改寬的面板高度不動", wide[0] == 150 && wide[1] == 999);
    }

    private static void onePanel() throws Exception {
        PositionView view = editor(new FakePanels(new SubViewsTest.FakeShell("zh_tw")));
        must("一開始只畫翻譯面板", view.spot("box:TOOLTIP") != null
                && view.spot("box:TRACKER") == null && view.spot("box:DIALOGUE") == null);
        press(view, "c:tab:1");
        render(view, -100, -100);
        must("點第二格換成任務追蹤，只剩它一個",
                view.spot("box:TRACKER") != null && view.spot("box:TOOLTIP") == null);
        must("任務追蹤有右下角把手", view.spot("grip:TRACKER") != null);
        press(view, "c:tab:4");
        render(view, -100, -100);
        must("名牌照內容長，沒有把手", view.spot("grip:NAMETAG") == null);
    }

    private static void pinsTooltip() throws Exception {
        FakePanels host = new FakePanels(new SubViewsTest.FakeShell("zh_tw"));
        PositionView view = editor(host);
        must("跟隨滑鼠時有左右側的選項", view.spot("c:side:0") != null);
        float[] before = view.spot("box:TOOLTIP");
        drag(view, "box:TOOLTIP", 100, 50);
        must("拖了翻譯面板就自動改成固定位置", !view.follows());
        must("改成固定之後不再顯示左右側", view.spot("c:side:0") == null);
        must("改成固定之後有切回跟隨滑鼠的選項", view.spot("c:anchor:0") != null);
        float[] after = view.spot("box:TOOLTIP");
        must("面板跟著拖過去（" + after[0] + "," + after[1] + "）",
                after[0] == before[0] + 100 && after[1] == before[1] + 50);
        must("固定之後不畫示範物品框", view.spot("item") == null && view.item() == null);
        AwtCanvas c = render(view, after[0] + 10, after[1] + 10);
        ImageIO.write(c.image, "png", new File("build/ui-preview/zh_tw-position-pinned.png"));
        must("還沒儲存，什麼都不寫", host.stored.isEmpty() && host.followSet == null);
        press(view, "b:save");
        must("儲存：寫進固定位置、擺放方式改成固定、存檔並關掉",
                host.followSet == Boolean.FALSE && host.commits == 1 && host.closes == 1);
        Spot tooltip = host.stored.get("TOOLTIP");
        must("寫進去的是拖過之後的文字起點（" + tooltip + "）",
                tooltip != null && tooltip.x() == (int) before[0] + 104
                        && tooltip.y() == (int) before[1] + 54);
        must("沒動的面板不寫", host.stored.size() == 1);

        FakePanels back = new FakePanels(new SubViewsTest.FakeShell("zh_tw"));
        back.follows = false;
        PositionView again = editor(back);
        press(again, "c:anchor:0");
        must("在編輯器裡可以切回跟隨滑鼠", again.follows());
        press(again, "b:save");
        must("切回跟隨滑鼠會寫回去", back.followSet == Boolean.TRUE);
    }

    private static int[] home() {
        return PositionView.itemHome(W, H);
    }

    private static void cursorLeavesItem() throws Exception {
        FakePanels host = new FakePanels(new SubViewsTest.FakeShell("zh_tw"));
        PositionView view = editor(host);
        must("剛打開時示範物品框在中間偏下（" + Arrays.toString(view.itemAt()) + "）",
                Arrays.equals(view.itemAt(), home()) && home()[1] > H / 2);
        Rect item = view.item();
        Rect panel = view.drawn();
        must("跟隨模式畫出示範物品框", item != null && view.spot("item") != null);
        double[][] path = {{30, 200}, {500, 300}, {320, 10}, {item.x() + 5, item.y() + 5}};
        boolean still = true;
        for (double[] at : path) {
            render(view, at[0], at[1]);
            still &= Arrays.equals(view.itemAt(), home()) && item.equals(view.item())
                    && panel.equals(view.drawn());
        }
        must("游標移來移去，示範物品框和翻譯面板都不動", still);
        must("游標移到上面的工具列也點得到", view.spot("c:side:1") != null);
    }

    private static void clickMovesItem() throws Exception {
        FakePanels host = new FakePanels(new SubViewsTest.FakeShell("zh_tw"));
        PositionView view = editor(host);
        view.mouseDown(500, 120, 0);
        view.mouseUp();
        render(view, 100, 300);
        must("點空白處，示範物品框移過去（" + Arrays.toString(view.itemAt()) + "）",
                Arrays.equals(view.itemAt(), new int[] {500, 120})
                        && host.itemFor(500, 120).equals(view.item()));
        must("點空白處不會改成固定位置", view.follows() && host.stored.isEmpty());
        render(view, 20, 40);
        must("點完游標移開，物品框留在點的地方", Arrays.equals(view.itemAt(), new int[] {500, 120}));
    }

    private static void dragsItem() throws Exception {
        FakePanels host = new FakePanels(new SubViewsTest.FakeShell("zh_tw"));
        PositionView view = editor(host);
        int[] start = home();
        Rect before = view.item();
        drag(view, "item", -60, 30);
        must("拖示範物品框跟著走（" + Arrays.toString(view.itemAt()) + "）",
                Arrays.equals(view.itemAt(), new int[] {start[0] - 60, start[1] + 30})
                        && view.item().x() == before.x() - 60 && view.item().y() == before.y() + 30);
        must("拖物品框不會改成固定位置", view.follows());
        render(view, 600, 20);
        must("放開之後就停住", Arrays.equals(view.itemAt(), new int[] {start[0] - 60, start[1] + 30}));

        drag(view, "item", 1000, 0);
        must("物品框拖得到最右邊（" + Arrays.toString(view.itemAt()) + "）",
                view.itemAt()[0] == W - 1 && view.item().x() + view.item().w() <= W);
        view.mouseDown(300, 120, 0);
        view.mouseUp();
        render(view, -100, -100);
        drag(view, "item", -2000, 0);
        must("也拖得到最左邊（" + Arrays.toString(view.itemAt()) + "）",
                view.itemAt()[0] == 0 && view.item().x() >= 0);

        drag(view, "box:TOOLTIP", 30, -20);
        must("拖翻譯面板本身仍然改成固定位置", !view.follows());
        press(view, "c:anchor:0");
        render(view, -100, -100);
        must("切回跟隨滑鼠，物品框回到中間偏下", Arrays.equals(view.itemAt(), home()));
    }

    private static void sideKeepsItem() throws Exception {
        FakePanels host = new FakePanels(new SubViewsTest.FakeShell("zh_tw"));
        PositionView view = editor(host);
        Rect item = view.item();
        int[] at = view.itemAt();
        press(view, "c:side:1");
        render(view, -100, -100);
        Rect right = view.drawn();
        boolean stayed = item.equals(view.item()) && Arrays.equals(at, view.itemAt());
        press(view, "c:side:2");
        render(view, -100, -100);
        Rect left = view.drawn();
        stayed &= item.equals(view.item()) && Arrays.equals(at, view.itemAt());
        press(view, "c:side:0");
        render(view, -100, -100);
        stayed &= item.equals(view.item()) && Arrays.equals(at, view.itemAt());
        must("切換自動／右側／左側，示範物品框不動", stayed);
        must("只有翻譯面板換邊（右 " + right.x() + "、左 " + left.x() + "）",
                right.x() >= item.x() + item.w() && left.x() + left.w() <= item.x());
        AwtCanvas c = render(view, -100, -100);
        ImageIO.write(c.image, "png", new File("build/ui-preview/zh_tw-position-follow.png"));
    }

    private static void side() throws Exception {
        FakePanels host = new FakePanels(new SubViewsTest.FakeShell("zh_tw"));
        PositionView view = editor(host);
        press(view, "c:side:2");
        must("點「左」之後編輯器的預覽改成左側", view.side() == 2);
        press(view, "b:save");
        must("儲存時寫入左右側（" + host.sideSet + "）", host.sideSet != null && host.sideSet == 2);

        FakePanels same = new FakePanels(new SubViewsTest.FakeShell("zh_tw"));
        same.side = 1;
        PositionView kept = editor(same);
        must("打開時照設定檔的左右側", kept.side() == 1);
        press(kept, "b:save");
        must("沒改左右側就不寫", same.sideSet == null);
    }

    private static void cancel() throws Exception {
        FakePanels host = new FakePanels(new SubViewsTest.FakeShell("zh_tw"));
        host.saved.put("TRACKER", new Spot(30, 40, 150, 60));
        PositionView view = editor(host);
        press(view, "c:side:1");
        render(view, -100, -100);
        drag(view, "box:TOOLTIP", 40, 40);
        press(view, "c:tab:1");
        render(view, -100, -100);
        drag(view, "box:TRACKER", 60, 20);
        press(view, "b:cancel");
        must("取消：位置、擺放方式、左右側都不寫",
                host.stored.isEmpty() && host.followSet == null && host.sideSet == null
                        && host.commits == 0);
        must("取消會關掉畫面", host.closes == 1);
        must("取消之後編輯器自己也回到打開時的樣子",
                view.follows() && view.side() == 0
                        && new Spot(30, 40, 150, 60).equals(trackerSpot(view)));
    }

    private static Spot trackerSpot(PositionView view) {
        view.show("TRACKER");
        return view.spot();
    }

    private static void resize() throws Exception {
        FakePanels host = new FakePanels(new SubViewsTest.FakeShell("zh_tw"));
        PositionView view = editor(host);
        view.show("TRACKER");
        render(view, -100, -100);
        drag(view, "grip:TRACKER", 40, 30);
        press(view, "b:save");
        Spot tracker = host.stored.get("TRACKER");
        must("拉右下角改大小會寫寬高（" + tracker + "）",
                tracker != null && tracker.w() == 160 && tracker.h() == 70
                        && tracker.x() == 6 && tracker.y() == 90);

        FakePanels wide = new FakePanels(new SubViewsTest.FakeShell("zh_tw"));
        PositionView choices = editor(wide);
        choices.show("CHOICES");
        render(choices, -100, -100);
        float[] box = choices.spot("box:CHOICES");
        drag(choices, "grip:CHOICES", -20, 40);
        press(choices, "b:save");
        Spot c = wide.stored.get("CHOICES");
        must("對話選項只改寬、中心跟著左邊不動（" + c + "）",
                c != null && c.w() == 120 && c.h() == 50 && c.x() == (int) box[0] + 60);
    }

    private static void resetThis() throws Exception {
        FakePanels host = new FakePanels(new SubViewsTest.FakeShell("zh_tw"));
        host.saved.put("DIALOGUE", new Spot(200, 100, 260, 40));
        PositionView view = editor(host);
        view.show("DIALOGUE");
        render(view, -100, -100);
        float[] moved = view.spot("box:DIALOGUE");
        must("存過的置中面板往左退半個寬度", moved[0] == 200 - 130 && moved[2] == 260);
        press(view, "b:reset");
        render(view, -100, -100);
        float[] home = view.spot("box:DIALOGUE");
        must("重置這個：回到預設位置（" + home[0] + "," + home[1] + "）",
                home[0] == (W - 220) / 2f && home[1] == H - 60 - 30);
        must("回到預設之後重置鈕變灰", !pressable(view, "b:reset", host));
        press(view, "b:save");
        must("重置後儲存：清掉位置與大小", Spot.AUTO.equals(host.stored.get("DIALOGUE")));
    }

    private static boolean pressable(PositionView view, String key, FakePanels host) {
        Spot before = host.stored.get("DIALOGUE");
        float[] r = view.spot(key);
        if (r == null) {
            return false;
        }
        view.mouseDown(r[0] + r[2] / 2, r[1] + r[3] / 2, 0);
        view.mouseUp();
        render(view, -100, -100);
        float[] after = view.spot("box:DIALOGUE");
        return before != host.stored.get("DIALOGUE") || after[0] != (W - 220) / 2f;
    }

    private static void sharedDrawing() throws Exception {
        String preview = read("render/PanelPreview.java");
        String screen = read("client/PositionScreen.java");
        must("編輯器的預覽走 PanelPreview", screen.contains("PanelPreview.draw(")
                && screen.contains("PanelPreview.drawFollowing("));
        must("編輯器自己不畫面板（沒有 renderTooltip／Boxes.draw）",
                !screen.contains("renderTooltip") && !screen.contains("Boxes.draw")
                        && !read("client/PanelSamples.java").contains("Boxes.draw"));
        String[][] pairs = {
                {"TooltipPanel.drawPinned(", "render/TooltipPanel.java", "drawPinned(graphics, lines, cfg.fixedX()"},
                {"TooltipPanel.drawBeside(", "render/TooltipPanel.java", "drawBeside(graphics, tooltip, lines"},
                {"TrackerOverlay.draw(", "render/TrackerOverlay.java", "draw(graphics, lines, PanelPlace.saved("},
                {"DialoguePanelRenderer.drawDialogue(", "render/DialoguePanelRenderer.java", "drawDialogue(graphics, lines, PanelPlace.saved("},
                {"DialoguePanelRenderer.drawChoices(", "render/DialoguePanelRenderer.java", "drawChoices(graphics, options, DialogueOverlay.picked"},
                {"LookAtTranslator.drawRow(", "render/LookAtTranslator.java", "drawRow(graphics, "},
        };
        for (String[] pair : pairs) {
            String game = read(pair[1]);
            must("預覽與遊戲共用同一個畫法：" + pair[0],
                    preview.contains(pair[0]) && game.contains(pair[2]));
        }
        String tracker = read("render/TrackerOverlay.java");
        must("任務追蹤只有一處畫框", count(tracker, "Boxes.draw(") == 1);
        String nametag = read("render/LookAtTranslator.java");
        must("名牌只有一處畫框", count(nametag, "Boxes.draw(") == 1);
        must("編輯器開著時遊戲裡的面板先停", read("listener/RenderListener.java")
                .contains("PanelPreview.editing()"));
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int at = text.indexOf(needle); at >= 0; at = text.indexOf(needle, at + 1)) {
            n++;
        }
        return n;
    }

    private static String read(String path) throws Exception {
        return Files.readString(SRC.resolve(path));
    }
}
