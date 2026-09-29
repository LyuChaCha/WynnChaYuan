package com.wynnchayuan.translate;

/**
 * 「跟隨遊戲語言」到底挑到哪一種。
 *
 * <h2>實機回報</h2>
 * 第一次安裝，遊戲語言是韓文、模組維持預設的「跟隨遊戲語言」，畫面出來卻是
 * <b>中文</b>；要在 F6 手動選一次韓文才會正常。
 *
 * <h2>為什麼會這樣</h2>
 * 語言是在 {@code onInitializeClient} 裡決定的，而那是 client entrypoint——
 * 跑在 {@code Minecraft} 的建構式裡，那時候 {@code getLanguageManager()} 還是
 * {@code null}。取不到就回傳空字串，而空字串一路走進 {@link Languages#pick}：
 *
 * <pre>
 *   normalise("")            -> "zh_tw"   （空的就當預設）
 *   bundled().contains(...)  -> true
 *   pick("", "")             -> "zh_tw"   ← 韓文玩家拿到中文
 * </pre>
 *
 * <p>三個步驟各自都合理，合起來就是「問不到遊戲語言」被靜靜地講成
 * 「遊戲語言是繁體中文」。手動選過一次之後 config 裡就有明確的值，
 * 所以只有第一次會中——也因此沒人回報得出重現步驟。
 *
 * <p>這支測試釘住的是：<b>問不到的時候不可以裝作問到了</b>。
 */
public final class AutoLanguageTest {

    private static int failures = 0;

    public static void main(String[] args) {
        // 使用者自己選過就照他的，跟遊戲語言無關
        eq("選了韓文就是韓文", "ko_kr", Languages.pick("ko_kr", "ja_jp"));
        eq("選了繁中就是繁中", "zh_tw", Languages.pick("zh_tw", "ko_kr"));

        // 跟隨遊戲語言：有打包的就用，沒打包的退回預設
        eq("跟隨韓文", "ko_kr", Languages.pick("", "ko_kr"));
        eq("跟隨日文", "ja_jp", Languages.pick("", "ja_jp"));
        eq("跟隨簡中", "zh_cn", Languages.pick("", "zh_cn"));
        eq("沒打包的語言退回預設", Languages.DEFAULT, Languages.pick("", "fr_fr"));
        eq("大小寫與連字號都要認得", "ko_kr", Languages.pick("", "KO-KR"));

        // ★ 問不到遊戲語言的時候
        //
        // pick 本身回預設是對的——它只是個純函式，手上沒資料就只能給預設。
        // 錯的是呼叫端拿「還沒準備好」當成「已經問到了」。所以這裡釘的是
        // Languages#known：呼叫端要分得出這兩件事。
        report("空字串不算問到", !Languages.known(""));
        report("null 不算問到", !Languages.known(null));
        report("沒打包的語言也不算", !Languages.known("fr_fr"));
        report("韓文算問到", Languages.known("ko_kr"));
        report("KO-KR 也算", Languages.known("KO-KR"));
        report("繁中算問到", Languages.known(Languages.DEFAULT));

        System.out.println(failures == 0
                ? "跟隨遊戲語言：全部通過"
                : "跟隨遊戲語言：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void eq(String what, String want, String got) {
        report(what + "（要 " + want + "，實際 " + got + "）", want.equals(got));
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
