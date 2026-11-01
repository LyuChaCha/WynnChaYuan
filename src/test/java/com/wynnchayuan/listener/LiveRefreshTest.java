package com.wynnchayuan.listener;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.CaptureStore;
import com.wynnchayuan.render.WynntilsText;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import com.wynntils.handlers.labels.event.TextDisplayChangedEvent;
import com.wynntils.handlers.scoreboard.ScoreboardSegment;
import com.wynntils.handlers.scoreboard.event.ScoreboardUpdatedEvent;
import com.wynntils.utils.type.Pair;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class LiveRefreshTest {

    private static final String NPC = "Old Harbour Keeper";
    private static final String NPC_ZH = "譯名甲";
    private static final String GEAR = "Glass Sabre";
    private static final String GEAR_ZH = "譯名乙";
    private static final String BOARD = "Row Nobody Rewrites";
    private static final String BOARD_ZH = "譯名丙";

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Path dir = Files.createTempDirectory("wynnchayuan-live-refresh");
        ModConfig config = new ModConfig(dir.resolve("config.json"));
        TranslationStore store = new TranslationStore();
        store.loadAll(corpus(dir));
        set("config", config);
        set("translations", store);
        set("store", new CaptureStore(dir.resolve("captured.json")));

        Display.TextDisplay npc = display();
        Display.TextDisplay gear = display();
        CaptureListener listener = new CaptureListener();
        arrive(listener, npc, NPC);
        arrive(listener, gear, GEAR);
        LiveRefresh.tick();
        check("前提：進場就翻好", text(npc).equals(NPC_ZH) && text(gear).equals(GEAR_ZH));

        store.setNameMode(ModConfig.ItemNames.OFF);
        LiveRefresh.tick();
        check("★ 物品名稱關掉，裝備名牌馬上回原文", text(gear).equals(GEAR));
        check("物品名稱關掉不影響 NPC 名牌", text(npc).equals(NPC_ZH));

        store.setNameMode(ModConfig.ItemNames.ON);
        LiveRefresh.tick();
        check("★ 物品名稱打開，裝備名牌馬上翻回來", text(gear).equals(GEAR_ZH));

        store.setNameMode(ModConfig.ItemNames.BOTH);
        LiveRefresh.tick();
        check("★ 附原文馬上帶上原文", text(gear).contains(GEAR_ZH) && text(gear).contains(GEAR));
        store.setNameMode(ModConfig.ItemNames.ON);
        LiveRefresh.tick();

        config.setNametagMode(ModConfig.NametagMode.OFF);
        LiveRefresh.tick();
        check("★ 名牌關掉，NPC 馬上回原文", text(npc).equals(NPC));

        config.setNametagMode(ModConfig.NametagMode.LOOK_AT);
        LiveRefresh.tick();
        check("★ 注視小框模式，NPC 頭上留原文", text(npc).equals(NPC));

        config.setNametagMode(ModConfig.NametagMode.REPLACE);
        LiveRefresh.tick();
        check("★ 切回取代，NPC 馬上翻回來", text(npc).equals(NPC_ZH));

        config.toggleOverlays();
        LiveRefresh.tick();
        check("★ 顯示開關關掉，NPC 馬上回原文", text(npc).equals(NPC));
        config.toggleOverlays();
        LiveRefresh.tick();
        check("顯示開關打開，NPC 翻回來", text(npc).equals(NPC_ZH));

        Display.TextDisplay icon = display();
        arrive(listener, icon, NPC);
        arrive(listener, icon, "");
        store.setNameMode(ModConfig.ItemNames.OFF);
        LiveRefresh.tick();
        check("變成圖示的實體不會被舊名牌蓋掉", text(icon).equals(""));
        store.setNameMode(ModConfig.ItemNames.ON);

        scoreboard();

        store.loadAll(List.of());
        LiveRefresh.tick();
        check("★ 翻譯總開關關掉，NPC 馬上回原文", text(npc).equals(NPC));
        check("★ 翻譯總開關關掉，記分板馬上回原文",
                WynntilsText.screenText(StyledText.fromString(BOARD)).getString().equals(BOARD));

        store.loadAll(corpus(dir));
        LiveRefresh.tick();
        check("★ 語言重載後 NPC 用新語料", text(npc).equals(NPC_ZH));
        check("★ 語言重載後記分板用新語料",
                WynntilsText.screenText(StyledText.fromString(BOARD)).getString().equals(BOARD_ZH));

        npc.setText(Component.literal("mark"));
        LiveRefresh.tick();
        check("設定沒變時不重寫", text(npc).equals("mark"));

        System.out.println(failures == 0 ? "LiveRefresh: 全部通過" : "LiveRefresh: " + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void scoreboard() {
        ScoreboardSegment segment = new ScoreboardSegment(null,
                StyledText.fromString("Status:"), new ArrayList<>(List.of(StyledText.fromString(BOARD))));
        new ScoreboardListener().onScoreboardUpdated(
                new ScoreboardUpdatedEvent(new ArrayList<>(List.of(Pair.of(null, segment)))));
        check("前提：記分板翻好",
                WynntilsText.screenText(StyledText.fromString(BOARD)).getString().equals(BOARD_ZH));
    }

    private static Path corpus(Path dir) throws Exception {
        Path corpus = Files.createDirectories(dir.resolve("corpus"));
        Files.writeString(corpus.resolve("misc.json"),
                "{\"" + NPC + "\": \"" + NPC_ZH + "\", \"" + BOARD + "\": \"" + BOARD_ZH + "\"}",
                StandardCharsets.UTF_8);
        Files.writeString(corpus.resolve("gear-weapon.json"),
                "{\"entries\": {\"sabre\": {\"src\": \"" + GEAR + "\", \"dst\": \"" + GEAR_ZH
                        + "\", \"role\": \"name\"}}}",
                StandardCharsets.UTF_8);
        return corpus;
    }

    private static Display.TextDisplay display() {
        return new Display.TextDisplay(EntityType.TEXT_DISPLAY, null);
    }

    private static void arrive(CaptureListener listener, Display.TextDisplay display, String raw) {
        TextDisplayChangedEvent.Text event = new TextDisplayChangedEvent.Text(
                display, null, StyledText.fromString(raw));
        listener.onLabel(event);
        display.setText(event.getText().getComponent());
    }

    private static String text(Display.TextDisplay display) throws Exception {
        Method read = Display.TextDisplay.class.getDeclaredMethod("getText");
        read.setAccessible(true);
        return ((Component) read.invoke(display)).getString();
    }

    private static void set(String field, Object value) throws Exception {
        var f = WynnChaYuan.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(null, value);
    }
}
