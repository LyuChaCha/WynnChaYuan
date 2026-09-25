package com.wynnchayuan.translate;

import com.wynnchayuan.WynnChaYuan;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * 有新翻譯的時候在聊天室說一次，要不要更新由玩家決定。
 *
 * <h2>為什麼不直接抓下來</h2>
 * 以前每次進遊戲、每次切語言都把整個語言的三十幾個檔重抓一遍。翻譯一個月可能
 * 只動幾條，玩家卻每天都在付那個流量與那幾十秒；想在遊戲裡對照兩種語言的人更慘
 * ——來回切一次就是兩趟完整下載，測試時尤其惱人。
 *
 * <p>所以改成跟模組更新同一套：開機問一句「有沒有新的」
 * （{@link RemoteSync#remoteVersion()}，一次請求），有才講，講完就等玩家自己去
 * F6 按更新。想要舊行為的人在 F6 打開「自動更新翻譯」，從此不再問。
 *
 * <h2>為什麼講在聊天室</h2>
 * 跟 {@code Releases#tellOnce} 同一個理由：那是唯一一個玩家一定會看到的地方。
 * 放在 F6 裡，只有本來就會去開設定的人看得到——而「不知道有新翻譯」的人
 * 恰好就是不會去開設定的那些人。
 *
 * <p>同一個版本只講一次（記在 config 的 {@code notifiedTranslations}），
 * 每次進遊戲都跳一次的提示，第三次之後就沒有人在看了。
 */
public final class TranslationUpdate {

    private TranslationUpdate() {}

    /** 遠端那一份的 commit；{@code null} 代表沒有新的、或根本沒問到。 */
    private static volatile String pending;

    /** 這一場講過了沒。 */
    private static volatile boolean told;

    /** 開機那一次問到的結果。見 {@code WynnChaYuan} 的同步執行緒。 */
    public static void found(String version) {
        pending = version;
    }

    /** 有新翻譯可以更新嗎。F6 的資料頁拿它決定要不要提示。 */
    public static boolean available() {
        return pending != null;
    }

    /** 更新完了：這一個版本就是現在手上的，不必再提示。 */
    public static void done(String version) {
        pending = null;
        told = true;
        if (version != null && !version.isBlank()) {
            WynnChaYuan.config().syncedTranslations(version);
        }
    }

    /** 點下去會跑的那一個。前面加斜線才是聊天框認得的寫法。 */
    public static final String COMMAND = "wynnchayuan-update";

    /** 正在抓，按兩次不會抓兩次。 */
    private static volatile boolean fetching;

    /**
     * 註冊客戶端指令，給聊天裡那顆按鈕用。
     *
     * <h2>為什麼要繞指令</h2>
     * 聊天的 {@code ClickEvent} 只能開網址、開檔案、填字或<b>送出指令</b>，
     * 沒有「呼叫這個 Java 方法」那一種。要讓一行字變成按鈕，就得有一個指令
     * 接在後面。
     *
     * <p>用 Fabric 的<b>客戶端</b>指令，所以它不會送到 Wynncraft——那很重要，
     * 伺服器收到不認得的指令會回一句錯誤，玩家每按一次就被罵一次。
     */
    public static void registerCommand() {
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT
                .register((dispatcher, registry) -> dispatcher.register(
                        net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
                                .literal(COMMAND)
                                .executes(ctx -> {
                                    update(Minecraft.getInstance());
                                    return 1;
                                })));
    }

    /** 抓一次，抓完在聊天室回報。F6 的那顆按鈕走的是同一支。 */
    private static void update(Minecraft client) {
        if (client == null || client.player == null || fetching) {
            return;
        }
        fetching = true;
        tell(client, com.wynnchayuan.client.T.c("status.fetching")
                .withStyle(ChatFormatting.GRAY));
        WynnChaYuan.resyncTranslations(result -> {
            fetching = false;
            if (client.player != null) {
                tell(client, com.wynnchayuan.client.T.c("chat.translations.done", result)
                        .withStyle(ChatFormatting.GREEN));
            }
        });
    }

    /** 進遊戲之後叫；有新翻譯就說一次。見 {@code Releases#tellOnce} 的同一套規則。 */
    public static void tellOnce(Minecraft client) {
        String version = pending;
        if (told || version == null || client == null || client.player == null) {
            return;
        }
        told = true;
        tell(client, com.wynnchayuan.client.T.c("chat.translations.line1")
                .withStyle(ChatFormatting.AQUA));
        tell(client, button());
        tell(client, com.wynnchayuan.client.T.c("chat.translations.where")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /**
     * 「按這裡更新」那一行：有底線、滑過有說明、按下去就抓。
     *
     * <p>底線是唯一會讓人想到「這可以按」的視覺提示——聊天室裡沒有按鈕長相。
     */
    static Component button() {
        return com.wynnchayuan.client.T.c("chat.translations.button")
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new net.minecraft.network.chat.ClickEvent.RunCommand(
                                "/" + COMMAND))
                        .withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowText(
                                com.wynnchayuan.client.T.c("chat.translations.button.hover"))));
    }

    /** 送一行。先記下來，收集語料時才不會把它當成遊戲原文，見 {@code OwnOutputs}。 */
    private static void tell(Minecraft client, Component line) {
        com.wynnchayuan.capture.OwnOutputs.note(line);
        client.player.displayClientMessage(line, false);
    }
}
