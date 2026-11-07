package com.wynnchayuan.render;

import com.wynnchayuan.ModConfig;
import com.wynnchayuan.translate.TranslationStore;
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
import java.util.stream.Stream;

import static com.wynnchayuan.Check.check;
import static com.wynnchayuan.Check.failures;

public final class PartialDumpTest {

    private static final Style LANG = font("minecraft:language/wynncraft");
    private static final Style SPACE = font("minecraft:space");

    public static void main(String[] args) throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        Path dir = Files.createTempDirectory("wynnchayuan-partial");
        Files.writeString(dir.resolve("gear-accessory.json"), """
                {"_meta": {"gearNames": true},
                 "entries": {"r1": {"src": "Moonfall", "dst": "RING-MOONFALL", "role": "name"}}}
                """, StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("gui.json"), """
                {"Health Regen {#}+{~} [{~}]": "REGEN {#}+{~} [{~}]",
                 "Mana Steal": "MANA-STEAL"}
                """, StandardCharsets.UTF_8);
        TranslationStore store = new TranslationStore();
        store.loadAll(dir);
        store.setNameMode(ModConfig.ItemNames.OFF);
        Path debug = Files.createDirectories(dir.resolve("debug"));
        TooltipDebug.init(debug.resolve("tooltip-debug.json"));

        List<Component> shown = TooltipPanel.translateInPlace(gear("Moonfall"), store);
        check("前提：說明那一列翻了", !shown.isEmpty()
                && shown.stream().anyMatch(c -> c.getString().contains("REGEN")));
        check("前提：名稱留英文", !shown.isEmpty() && shown.get(1).getString().contains("Moonfall"));
        check("★ 名稱照設定留英文時不寫「翻了一半」的診斷檔（實際 " + partials(debug) + "）",
              partials(debug).isEmpty());

        List<Component> withMiss = new ArrayList<>(gear("Moonfall"));
        withMiss.add(Component.literal("Mana Steal").withStyle(s -> s.withColor(0xFFFFFF)
                .withFont(new FontDescription.Resource(Identifier.parse("minecraft:uniform")))));
        boolean[] hit = new boolean[withMiss.size()];
        hit[3] = true;
        List<com.wynntils.core.text.StyledText> styled = withMiss.stream()
                .map(com.wynntils.core.text.StyledText::fromComponent).toList();
        check("真的漏翻的列照樣算數",
              TooltipTranslator.anyMissed(styled, hit, 2, store));
        hit[4] = true;
        check("只剩名稱沒翻時不算",
              !TooltipTranslator.anyMissed(styled, hit, 2, store));
        check("名稱那兩列不算的前提是真的有跳過",
              TooltipTranslator.anyMissed(styled, hit, 0, store));

        System.out.println(failures == 0 ? "翻一半的診斷：全部通過" : "翻一半的診斷：" + failures + " 項失敗");
        System.exit(failures == 0 ? 0 : 1);
    }

    private static List<String> partials(Path debug) throws Exception {
        try (Stream<Path> files = Files.list(debug)) {
            return files.map(p -> p.getFileName().toString())
                    .filter(n -> n.startsWith("tooltip-partial")).toList();
        }
    }

    private static Style font(String id) {
        return Style.EMPTY.withFont(new FontDescription.Resource(Identifier.parse(id)));
    }

    private static Component icon(Style style, int... cps) {
        StringBuilder sb = new StringBuilder();
        for (int cp : cps) {
            sb.appendCodePoint(cp);
        }
        return Component.literal(sb.toString()).withStyle(style);
    }

    private static List<Component> gear(String name) {
        List<Component> rows = new ArrayList<>();
        MutableComponent hidden = Component.empty();
        hidden.append(icon(SPACE, 0xCF000));
        hidden.append(Component.literal(name).withStyle(s -> s.withColor(0xAA00AA)));
        hidden.append(icon(SPACE, 0xCF000));
        rows.add(hidden);
        MutableComponent seen = Component.empty();
        seen.append(icon(LANG, 0xCFFF0));
        seen.append(Component.literal(name).withStyle(LANG.withColor(0xAA00AA)));
        seen.append(Component.literal(" [79.1%]").withStyle(LANG.withColor(0xB1FF55)));
        rows.add(seen);
        rows.add(Component.empty());
        MutableComponent regen = Component.empty();
        regen.append(Component.literal("Health Regen ").withStyle(s -> s.withColor(0x55FF55)));
        regen.append(icon(SPACE, 0xD0010));
        regen.append(Component.literal("+12% [80%]").withStyle(s -> s.withColor(0x55FF55)));
        rows.add(regen);
        return rows;
    }
}
