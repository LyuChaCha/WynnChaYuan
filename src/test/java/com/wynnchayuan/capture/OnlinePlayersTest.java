package com.wynnchayuan.capture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class OnlinePlayersTest {

    public static void main(String[] args) throws Exception {
        String line = "Thanks Zorvak77 for the help";
        check("清單沒有這個人就不算", !PlayerDataFilter.carriesPlayerData(line));

        PlayerDataFilter.remember(List.of("Zorvak77", "Bob"), List.of("Zorvak77"));
        check("★ 在線玩家的名字擋得下來", PlayerDataFilter.carriesPlayerData(line));
        check("太短的名字不比對", !PlayerDataFilter.carriesPlayerData("Bob the Builder"));

        AtomicBoolean stop = new AtomicBoolean();
        List<Throwable> broken = new ArrayList<>();
        Thread reader = new Thread(() -> {
            try {
                while (!stop.get()) {
                    PlayerDataFilter.carriesPlayerData(line);
                }
            } catch (Throwable t) {
                broken.add(t);
            }
        });
        reader.start();
        for (int i = 0; i < 20_000; i++) {
            PlayerDataFilter.remember(i % 2 == 0 ? List.of("Zorvak77") : List.of(), List.of());
        }
        stop.set(true);
        reader.join();
        check("★ 邊換清單邊讀不會出例外", broken.isEmpty());

        PlayerDataFilter.remember(List.of(), List.of());
        check("離線後清單清空", !PlayerDataFilter.carriesPlayerData(line));

        System.out.println(failures == 0 ? "在線玩家：全部通過" : "在線玩家：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
