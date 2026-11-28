package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.TranslationStore;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class PanelsHiddenTest {

    private static final Path CORPUS = Path.of("src/main/resources/assets/wynnchayuan/translations/zh_tw");

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        ModConfig config = new ModConfig(Files.createTempDirectory("wynnchayuan-hide").resolve("config.json"));
        config.setDialogueMode(ModConfig.DialogueMode.PANEL);
        config.setTrackerMode(ModConfig.DialogueMode.PANEL);
        config.setWynntilsUi(false);
        var field = WynnChaYuan.class.getDeclaredField("config");
        field.setAccessible(true);
        field.set(null, config);

        TranslationStore store = new TranslationStore();
        store.loadAll(List.of(CORPUS));
        store.setNameMode(ModConfig.ItemNames.ON);
        List<Component> tooltip = List.of(Component.literal("Combat Level"), Component.literal("Combat Level"));
        check("那份 tooltip 真的翻得到（不然底下測不到面板）",
                !TooltipTranslator.translateLinesCached(tooltip, store).isEmpty());

        check("一開始是顯示的", !PanelsHidden.hidden());
        check("按一下變隱藏", PanelsHidden.toggle() && PanelsHidden.hidden());
        check("再按一下回來", !PanelsHidden.toggle() && !PanelsHidden.hidden());

        Runnable tooltipPanel = () -> TooltipPanel.render(null, tooltip, 10, 10, store);
        Runnable dialoguePanel = () -> {
            DialogueOverlay.current = List.of(Component.literal("Hello there"));
            DialogueOverlay.lastUpdate = System.currentTimeMillis();
            DialogueOverlay.render(null);
        };
        Runnable trackerPanel = () -> {
            TrackerOverlay.setExtras(List.of(Component.literal("Talk to the guard")));
            TrackerOverlay.render(null);
        };

        check("顯示時物品面板會去畫（headless 沒有 Minecraft，所以會丟例外）", !quiet(tooltipPanel));
        check("顯示時對話小框會去畫", !quiet(dialoguePanel));
        check("顯示時追蹤小框會去畫", !quiet(trackerPanel));

        PanelsHidden.toggle();
        check("★ 隱藏時物品面板什麼都不畫", quiet(tooltipPanel));
        check("★ 隱藏時不推開原本的 tooltip，原文照常顯示",
                !TooltipPanel.wouldPush(tooltip, 10, 10, store));
        check("★ 隱藏時對話與選項小框什麼都不畫", quiet(dialoguePanel));
        check("★ 隱藏時追蹤小框什麼都不畫", quiet(trackerPanel));
        check("★ 隱藏時看向名牌的小框什麼都不畫", quiet(() -> LookAtTranslator.render(null)));
        PanelsHidden.toggle();

        System.out.println(failures == 0 ? "PanelsHidden: 全部通過" : "PanelsHidden: " + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static boolean quiet(Runnable body) {
        try {
            body.run();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
