package com.wynnchayuan.translate;

import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class BeaconPanelTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("wynnchayuan-beacon");
        Files.writeString(dir.resolve("misc.json"),
                "{\"no Time Bonus for\": \"完成它們不會\"}", StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        StyledText line = indented(180, "no Time Bonus for");

        for (boolean inPanel : new boolean[] {false, true}) {
            int lead = leadOf(LineTranslator.translateChat(line, store, null, inPanel));
            report("面板旗標 " + inPanel + " 時翻得出來、縮排不亂跑（實際 "
                            + lead + "px）", lead == 180);
        }

        panelDetection();

        System.out.println(failures == 0
                ? "信標面板：全部通過" : "信標面板：" + failures + " 項失敗");
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void panelDetection() {
        MutableComponent two = Component.empty();
        two.append(offset(36)).append(Component.literal("Greatly Empower"))
           .append(offset(65)).append(Component.literal("+6 Challenges to"));
        List<StyledText> beacon = List.of(
                StyledText.fromComponent(two),
                indented(180, "no Time Bonus for"));
        report("★ 有多欄的行 -> 整塊算面板", LineTranslator.chatPanel(beacon));

        List<StyledText> normal = List.of(
                indented(89, "Welcome to Wynncraft!"),
                indented(59, "play.wynncraft.com"));
        report("一般聊天不算面板（只有行首縮排不算欄）",
                !LineTranslator.chatPanel(normal));
    }

    private static StyledText indented(int px, String text) {
        MutableComponent out = Component.empty();
        out.append(offset(px)).append(Component.literal(text));
        return StyledText.fromComponent(out);
    }

    private static Component offset(int px) {
        return Component.literal(SpaceOffset.encode(px))
                .setStyle(SpaceOffset.styleFor(Style.EMPTY));
    }

    private static int leadOf(Component line) {
        if (line == null) {
            return -1;
        }
        int[] px = {0};
        boolean[] done = {false};
        line.visit((style, text) -> {
            if (done[0] || text.isEmpty()) {
                return java.util.Optional.empty();
            }
            if (SpaceOffset.isSpaceFont(style) && SpaceOffset.isOffsetRun(text)) {
                px[0] += SpaceOffset.decode(text);
            } else {
                done[0] = true;
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return px[0];
    }

    private static void report(String what, boolean ok) {
        System.out.println("  [" + (ok ? "PASS" : "FAIL") + "] " + what);
        if (!ok) {
            failures++;
        }
    }
}
