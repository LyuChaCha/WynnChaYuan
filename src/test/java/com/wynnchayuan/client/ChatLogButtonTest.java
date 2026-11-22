package com.wynnchayuan.client;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class ChatLogButtonTest {

    private static final float W = 70;

    public static void main(String[] args) {
        float[] narrow = {0, 50, 328, 180};
        float[] side = ChatLogButton.place(narrow, null, W, 640);
        check("寬螢幕：按鈕在聊天框右邊", side[0] >= 328 && side[1] + side[3] == 230);

        float[] wide = {0, 50, 476, 180};
        float[] above = ChatLogButton.place(wide, null, W, 480);
        check("聊天框塞滿寬度：按鈕移到聊天框上方",
              above[1] + above[3] <= 50 && above[0] + above[2] <= 478);
        check("移到上方時不蓋到聊天框", !overlaps(above, wide));

        float[] tall = {0, 4, 476, 226};
        float[] inside = ChatLogButton.place(tall, null, W, 480);
        check("上下都沒空間：留在框內右上角", inside != null && inside[1] >= 4
              && inside[0] + inside[2] <= 478);

        float[] dock = {2, 4, 472, 226};
        check("面板開著又沒空間：不畫按鈕（面板自己有關閉鈕）",
              ChatLogButton.place(tall, dock, W, 480) == null);

        float[] dockRoom = {2, 30, 472, 200};
        float[] overDock = ChatLogButton.place(wide, dockRoom, W, 480);
        check("面板開著時放在面板上方", overDock != null && !overlaps(overDock, dockRoom));

        float[] moved = ChatLogButton.clampTo(600, -20, W, 640, 360);
        check("拖到畫面外：夾回畫面內", moved[0] + moved[2] <= 638 && moved[1] >= 2);
        float[] kept = ChatLogButton.clampTo(300, 200, W, 640, 360);
        check("拖到畫面內：照放開的位置", kept[0] == 300 && kept[1] == 200);

        System.out.println(failures == 0 ? "\n聊天紀錄按鈕：全部通過"
                : "\n聊天紀錄按鈕：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static boolean overlaps(float[] a, float[] b) {
        return a[0] < b[0] + b[2] && b[0] < a[0] + a[2]
                && a[1] < b[1] + b[3] && b[1] < a[1] + a[3];
    }
}
