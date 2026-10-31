package com.wynnchayuan.capture;

import net.minecraft.network.chat.Component;

import java.util.List;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class OwnOutputsTest {

    private static final List<String> OURS = List.of(
            "{#} Мировое событие «Пепельная яма Maar» начнётся через {~} мин {~} с! (в {~} бл.\n{#} отсюда) Нажмите, чтобы отслеживать",
            "{#} Пользуетесь Reddit? Заходите в наш официальный сабреддит: wynn.gg/reddit",
            "{#} Подземелья - отличный способ быстро прокачаться и добыть\n{#} сильные предметы! Подробнее на wynncraft.com",
            "{#} Мировое событие «Тролли-собственники» начнётся через {~} мин {~} с! (в {~}\n{#} бл. отсюда) Нажмите, чтобы отслеживать",
            "{#} Мировое событие «Посланники света» начнётся через {~} мин {~} с! (в {~} бл. отсюда) Нажмите, чтобы отслеживать",
            "{#} Продавайте предметы на глобальной Торговой площадке -\n{#} так выгоднее! Подробнее: \n{#} wynncraft.com",
            "{#} ¿Usas Reddit? Únete a nuestro subreddit oficial en wynn.gg/reddit",
            "{#} ¡Puedes comprar ventajas para apoyar a Wynncraft! ¡Recibirás\n{#} rangos, bombas, cajas y mucho más por tu generosidad!\n{#} Visita wynncraft.com/store para ver más.");

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
        check("★ 實機收進來的俄文、西文譯文都認得出是我們的（" + hit + "/" + OURS.size() + "）",
               hit == OURS.size());

        for (String line : GAME) {
            check("遊戲原文照樣收：「" + line + "」", !OwnOutputs.isOwn(line));
        }

        OwnOutputs.note(Component.literal(
                "[WynnChaYuan] Ya está la versión 0.2.0_8 (tienes la 0.2.0_6)"));
        OwnOutputs.note(Component.literal("▸ Haz clic aquí para descargarla de GitHub"));
        OwnOutputs.note(Component.literal("Notas completas en F6 → Novedades"));
        check("★ 更新通知第一行",
               OwnOutputs.isOwn("[WynnChaYuan] Ya está la versión {~}_{~} (tienes la {~}_{~})"));
        check("★ 更新通知的連結那一行",
               OwnOutputs.isOwn("▸ Haz clic aquí para descargarla de GitHub"));
        check("★ 更新通知最後一行", OwnOutputs.isOwn("Notas completas en F{~} → Novedades"));

        check("同一句換個折行位置照樣認得",
               OwnOutputs.canon("a b\n{#} c d").equals(OwnOutputs.canon("a b c\n{#} d")));

        System.out.println(failures == 0
                ? "收集時略過我們自己的字：全部通過"
                : "收集時略過我們自己的字：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

}
