package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class ShotLogTest {

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-shot");
        ModConfig config = new ModConfig(dir.resolve("config.json"));
        Path log = dir.resolve(ShotLog.FILE);

        check("前提：預設不開診斷檔", !config.debugDumps());
        check("★ 診斷檔關著：什麼都不寫", !ShotLog.write(dir, config, "bind"));
        check("★ 診斷檔關著：檔案不存在", !Files.exists(log));

        config.toggleDebugDumps();
        check("診斷檔開著：寫得進去", ShotLog.write(dir, config, "bind"));
        check("診斷檔開著：檔案裡有那一行",
              Files.exists(log) && Files.readString(log).contains("bind"));
        check("沒有設定檔也不會寫", !ShotLog.write(dir, null, "bind"));

        boolean listensToKeys = false;
        for (Method m : PanelShot.class.getDeclaredMethods()) {
            for (Class<?> type : m.getParameterTypes()) {
                listensToKeys |= type.getSimpleName().equals("Screen")
                        && m.getParameterCount() == 2 && m.getParameterTypes()[1] == Object.class;
            }
        }
        check("★ 截圖不記別的按鍵（沒有收「畫面＋按鍵」的方法）", !listensToKeys);

        System.out.println(failures == 0 ? "截圖診斷：全部通過" : "截圖診斷：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }
}
