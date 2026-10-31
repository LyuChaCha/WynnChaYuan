package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.CaptureStore;
import com.wynnchayuan.capture.ConversationPartner;
import com.wynnchayuan.capture.CurrentQuest;
import com.wynnchayuan.capture.QuestGuess;
import com.wynnchayuan.listener.ChatBlock;
import com.wynnchayuan.listener.WorldReset;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class WorldResetTest {

    private static final String LINE = "Bring me the old map from the tower.";

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Path dir = Files.createTempDirectory("wynnchayuan-world-reset");
        set(WynnChaYuan.class, "config", new ModConfig(dir.resolve("config.json")));
        set(WynnChaYuan.class, "store", new CaptureStore(dir.resolve("captured.json")));
        TranslationStore store = store(dir);

        arrive(store);
        check("前提：對話保留狀態有東西", get(DialogueTyping.class, "held") != null);
        check("前提：小框有內容", !DialogueOverlay.currentForTest().isEmpty());

        WorldReset.reset();

        check("對話的保留狀態清掉", get(DialogueTyping.class, "held") == null);
        check("對話的逐字狀態清掉", "".equals(get(DialogueTyping.class, "lineRaw")));
        check("小框清掉", DialogueOverlay.currentForTest().isEmpty());
        check("聊天暫存清掉", ((List<?>) get(ChatBlock.class, "pending")).isEmpty());
        check("注視的實體不留著", get(LookAtTranslator.class, "lastSource") == null);
        check("注視的模板不留著", get(LookAtTranslator.class, "lastTemplate") == null);
        check("說話對象清掉", get(ConversationPartner.class, "name") == null);
        check("任務猜測清掉", get(QuestGuess.class, "lastVerdict") == null);
        check("目前任務清掉", CurrentQuest.get() == null);

        check("斷線時也會重設",
                source("WynnChaYuan.java").contains("ClientPlayConnectionEvents.DISCONNECT")
                && source("WynnChaYuan.java").contains("WorldReset::reset"));
        check("換世界時走同一個入口",
                source("listener/TrackerListener.java").contains("WorldReset.reset()"));

        System.out.println(failures == 0 ? "WorldReset: 全部通過" : "WorldReset: " + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void arrive(TranslationStore store) throws Exception {
        CurrentQuest.set("The Old Map");
        for (int at = 1; at <= LINE.length(); at++) {
            String typed = LINE.substring(0, at);
            DialogueTyping.line(typed, store, 3, null, 232);
            DialogueOverlay.setCurrent(StyledText.fromString(typed), store);
        }
        ChatBlock.queue(StyledText.fromString("hello"), Component.literal("你好"));
        set(LookAtTranslator.class, "lastTemplate", "Blacksmith");
        set(ConversationPartner.class, "name", "Tasim");
        set(QuestGuess.class, "lastVerdict", Boolean.TRUE);
    }

    private static TranslationStore store(Path dir) throws Exception {
        Path corpus = Files.createDirectories(dir.resolve("corpus"));
        Files.writeString(corpus.resolve("dialogue.json"),
                "{\"" + LINE + "\": \"把塔裡那張舊地圖帶給我。\"}", StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(corpus);
        return store;
    }

    private static String source(String file) throws Exception {
        return Files.readString(Path.of("src/main/java/com/wynnchayuan", file), StandardCharsets.UTF_8);
    }

    private static void set(Class<?> owner, String field, Object value) throws Exception {
        var f = owner.getDeclaredField(field);
        f.setAccessible(true);
        f.set(null, value);
    }

    private static Object get(Class<?> owner, String field) throws Exception {
        var f = owner.getDeclaredField(field);
        f.setAccessible(true);
        return f.get(null);
    }

}
