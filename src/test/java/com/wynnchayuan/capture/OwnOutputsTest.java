package com.wynnchayuan.capture;

import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * 收集語料時不能把我們自己畫出去的字當成遊戲原文。
 *
 * <h2>實機回報 2026-09-18</h2>
 * 使用者切換語言測試之後，{@code captured.json} 裡出現了俄文、西文的譯文，
 * 以及模組自己的更新通知。下面的字串就是那一份檔案裡原封不動的樣子。
 */
public final class OwnOutputsTest {

    private static int failures = 0;

    /** 實機那一份 captured.json 裡，被當成原文收進來的我們自己的譯文。 */
    private static final List<String> OURS = List.of(
            "{#} Мировое событие «Пепельная яма Maar» начнётся через {~} мин {~} с! (в {~} бл.\n{#} отсюда) Нажмите, чтобы отслеживать",
            "{#} Пользуетесь Reddit? Заходите в наш официальный сабреддит: wynn.gg/reddit",
            // 實機那一筆是「—」：那是 0.1.9_10 之前的舊譯文（俄文字型沒有長破折號，
            // 後來改成「-」）。captured.json 會跨版本累積，舊版畫出去的字索引認不得，
            // 這裡用現行的寫法。
            "{#} Подземелья - отличный способ быстро прокачаться и добыть\n{#} сильные предметы! Подробнее на wynncraft.com",
            "{#} Мировое событие «Тролли-собственники» начнётся через {~} мин {~} с! (в {~}\n{#} бл. отсюда) Нажмите, чтобы отслеживать",
            "{#} Мировое событие «Посланники света» начнётся через {~} мин {~} с! (в {~} бл. отсюда) Нажмите, чтобы отслеживать",
            "{#} Продавайте предметы на глобальной Торговой площадке -\n{#} так выгоднее! Подробнее: \n{#} wynncraft.com",
            "{#} ¿Usas Reddit? Únete a nuestro subreddit oficial en wynn.gg/reddit",
            "{#} ¡Puedes comprar ventajas para apoyar a Wynncraft! ¡Recibirás\n{#} rangos, bombas, cajas y mucho más por tu generosidad!\n{#} Visita wynncraft.com/store para ver más.");

    /** 真正的遊戲原文，不能被擋掉。 */
    private static final List<String> GAME = List.of(
            "Huh? Oh, I'm just lookin' around.",
            "They must have used some real tall ladders to get those suckers up there.",
            "WynnChaYuan reference gate: nobody has ever seen this line",
            "Hey {u}! What's up?");

    public static void main(String[] args) {
        System.out.println("=== 收集時略過我們自己的字 ===");
        OwnOutputs.build();

        int hit = 0;
        for (String line : OURS) {
            boolean own = OwnOutputs.isOwn(line);
            if (own) {
                hit++;
            } else {
                System.out.println("    沒認出來：" + line.replace("\n", "⏎"));
            }
        }
        report("★ 實機收進來的俄文、西文譯文都認得出是我們的（" + hit + "/" + OURS.size() + "）",
               hit == OURS.size());

        for (String line : GAME) {
            report("遊戲原文照樣收：「" + line + "」", !OwnOutputs.isOwn(line));
        }

        // 模組自己的更新通知：送出前記下來，收集端看到的是數字抽掉之後的模板
        OwnOutputs.note(Component.literal(
                "[WynnChaYuan] Ya está la versión 0.2.0_8 (tienes la 0.2.0_6)"));
        OwnOutputs.note(Component.literal("▸ Haz clic aquí para descargarla de GitHub"));
        OwnOutputs.note(Component.literal("Notas completas en F6 → Novedades"));
        report("★ 更新通知第一行",
               OwnOutputs.isOwn("[WynnChaYuan] Ya está la versión {~}_{~} (tienes la {~}_{~})"));
        report("★ 更新通知的連結那一行",
               OwnOutputs.isOwn("▸ Haz clic aquí para descargarla de GitHub"));
        report("★ 更新通知最後一行", OwnOutputs.isOwn("Notas completas en F{~} → Novedades"));

        // 折行的位置不一樣也要認得出來（聊天欄寬度不同，斷點就不同）
        report("同一句換個折行位置照樣認得",
               OwnOutputs.canon("a b\n{#} c d").equals(OwnOutputs.canon("a b c\n{#} d")));

        System.out.println(failures == 0
                ? "收集時略過我們自己的字：全部通過"
                : "收集時略過我們自己的字：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
