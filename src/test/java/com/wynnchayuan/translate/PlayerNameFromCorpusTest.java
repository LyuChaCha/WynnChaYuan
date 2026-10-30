package com.wynnchayuan.translate;

import com.wynnchayuan.capture.LineParts;
import com.wynnchayuan.capture.SelfNames;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PlayerNameFromCorpusTest {

    private static int failures = 0;

    private static final String NICK = "WYNNCHAYUAN";

    private static final String FULL =
            "Hey, " + NICK + "! Are you alright in there? "
            + "It looks like we've hit something.";
    private static final String TYPING = "Hey, " + NICK + "! Are you alright in ther";

    public static void main(String[] args) {
        TranslationStore store = new TranslationStore();
        store.loadAll(Path.of("src/main/resources/assets/wynnchayuan/translations",
                              "zh_tw"));

        report("★ 整句夾得出暱稱（實際 " + name(store, FULL) + "）",
               NICK.equals(name(store, FULL)));

        report("★ 打到一半先不認（實際 " + name(store, TYPING) + "）",
               name(store, TYPING) == null);

        String early = "Hey, " + NICK + "!";
        report("★ 證據不夠時不認（實際 " + name(store, early) + "）",
               name(store, early) == null);

        report("沒提到玩家就回 null",
               store.playerNameIn("Exit the caravan and talk to the Caravan Driver.")
                       == null);
        report("空字串不會爆", store.playerNameIn("") == null);

        String hey = "Hey " + NICK + "! What's up?";
        report("★ 「Hey X! What's up?」夾得出暱稱（實際 " + name(store, hey) + "）",
               NICK.equals(name(store, hey)));
        String meet = NICK + "! Nice to meet you! I haven't been here in Detlas for long"
                + " myself... still trying to figure things out.";
        report("★ 名字在句首、後面還有地名（實際 " + name(store, meet) + "）",
               NICK.equals(name(store, meet)));

        String halfLine = "If you want to survive out here, you're";
        report("★ 半句台詞不能被當成名字（實際 " + name(store, halfLine) + "）",
               name(store, halfLine) == null);

        Map<String, Set<String>> votes = new HashMap<>();
        for (String key : store.keys()) {
            if (key.contains("{u}") || key.indexOf(10) >= 0) {
                continue;
            }
            for (int i = 4; i <= key.length(); i++) {
                TranslationStore.Guess guess = store.playerNameIn(key.substring(0, i));
                if (guess != null) {
                    votes.computeIfAbsent(guess.name(), n -> new HashSet<>())
                         .add(guess.source());
                    break;
                }
            }
        }
        List<String> settled = new ArrayList<>();
        for (Map.Entry<String, Set<String>> vote : votes.entrySet()) {
            if (vote.getValue().size() >= SelfNames.MIN_EVIDENCE) {
                settled.add(vote.getKey());
            }
        }
        report("★ 三萬條語料逐字模擬，沒有一個假名字被收（單票猜測 "
                       + votes.size() + " 種，被收 " + settled + "）",
               settled.isEmpty());

        TranslationStore.Guess first = store.playerNameIn(FULL);
        report("整句夾得出來", first != null && NICK.equals(first.name()));
        SelfNames.propose(first.name(), first.source());
        report("★ 一句就收了（實際 " + SelfNames.all() + "）",
               SelfNames.all().contains(NICK));

        String second = "Woah! That sure did the trick, " + NICK + ". Good thinking!";
        TranslationStore.Guess again = store.playerNameIn(second);
        report("別句台詞也夾得出來（實際 " + (again == null ? null : again.name()) + "）",
               again != null && NICK.equals(again.name()));
        String template = LineParts.of(StyledText.fromComponent(
                Component.literal(FULL))).template();
        report("★ 之後模板抽成 {u}（實際 " + template + "）",
               ("Hey, {u}! Are you alright in there? "
                + "It looks like we've hit something.").equals(template));

        report("★ 查得到譯文（實際 " + store.lookup(template) + "）",
               store.lookup(template) != null);

        System.out.println(failures == 0
                ? "語料反推暱稱：全部通過"
                : "語料反推暱稱：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static String name(TranslationStore store, String raw) {
        TranslationStore.Guess guess = store.playerNameIn(raw);
        return guess == null ? null : guess.name();
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
