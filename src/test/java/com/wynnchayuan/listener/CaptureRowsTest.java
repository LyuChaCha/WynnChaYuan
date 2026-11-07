package com.wynnchayuan.listener;

import com.google.gson.JsonObject;
import com.wynnchayuan.render.ScoreboardFlow;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class CaptureRowsTest {

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        scoreboard();
        invisibleHashes();
        System.out.println(failures == 0 ? "收集的列：全部通過" : "收集的列：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static void scoreboard() throws Exception {
        System.out.println("=== 記分板折行的半句 ===");
        Path dir = Files.createTempDirectory("wynnchayuan-board-rows");
        JsonObject misc = new JsonObject();
        misc.addProperty("Fight through the palace rooms!", "BOARD-FIGHT");
        Files.writeString(dir.resolve("misc.json"), misc.toString(), StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);

        List<StyledText> content = List.of(
                StyledText.fromString("Fight through the"),
                StyledText.fromString("palace rooms!"),
                StyledText.fromString("- Time Left: 3:12"));
        ScoreboardFlow.Plan plan = ScoreboardFlow.plan(content, store);
        check("前提：整句翻了（" + plan.rows().keySet() + "）", plan.rows().size() == 2);
        List<String> left = new ArrayList<>();
        for (StyledText row : ScoreboardListener.leftOver(content, Set.of(), plan)) {
            left.add(row.getString());
        }
        check("★ 翻好的那兩半不再單獨收（剩下 " + left + "）",
              !left.contains("Fight through the") && !left.contains("palace rooms!"));
        check("沒翻的那一列照收", left.contains("- Time Left: 3:12"));
        check("隊友那幾列不收",
              !ScoreboardListener.leftOver(content, Set.of(ScoreboardFlow.key(content.get(2))), plan)
                      .contains(content.get(2)));
    }

    private static void invisibleHashes() {
        System.out.println("=== 隱形字型的雜湊 ===");
        Style hidden = Style.EMPTY.withColor(0xAA00AA)
                .withFont(new FontDescription.Resource(Identifier.parse("minecraft:invisible")));
        MutableComponent mixed = Component.empty();
        mixed.append(Component.literal("Untradable Item").withStyle(s -> s.withColor(0xFF5555)));
        mixed.append(Component.literal("3af5c2").withStyle(hidden));
        List<Component> tooltip = List.of(
                Component.literal("Crystal Shard"),
                Component.literal("78a48282").withStyle(hidden),
                mixed);
        List<String> templates = GuiTextCapture.templatesOf(tooltip);
        check("一般的列照收（" + templates.get(0) + "）", "Crystal Shard".equals(templates.get(0)));
        check("★ 整列都是隱形字的不收（" + templates.get(1) + "）", templates.get(1) == null);
        check("★ 夾在一般字後面的雜湊拿掉（" + templates.get(2) + "）",
              "Untradable Item".equals(templates.get(2)));
    }
}
