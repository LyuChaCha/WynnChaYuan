package com.wynnchayuan.client;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class KeyLatchTest {

    public static void main(String[] args) {
        KeyLatch latch = new KeyLatch();
        int fired = 0;
        fired += latch.press() ? 1 : 0;
        fired += latch.press() ? 1 : 0;
        fired += latch.press() ? 1 : 0;
        check("★ 前置、後置、tick 都看到同一下，只算一次（實際 " + fired + "）", fired == 1);

        latch.settle(true);
        check("★ 還壓著時的重複事件不算", !latch.press());

        latch.settle(false);
        check("放開之後下一下照算", latch.press());

        latch.settle(false);
        check("快速點一下（tick 之前就放開）也能接著再按", latch.press());

        System.out.println(failures == 0 ? "KeyLatch: 全部通過" : "KeyLatch: " + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
