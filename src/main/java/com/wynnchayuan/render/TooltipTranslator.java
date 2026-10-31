package com.wynnchayuan.render;

import com.wynnchayuan.translate.BlockLayout;
import com.wynnchayuan.translate.ErrorDebug;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.LookupScope;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TooltipTranslator {

    private TooltipTranslator() {}

    private static final int CACHE_LIMIT = 256;

    private static final Map<List<Component>, CachedLines> LINES_CACHE =
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<List<Component>, CachedLines> eldest) {
                    return size() > CACHE_LIMIT;
                }
            };

    private record CachedLines(long age, List<Component> lines) {}

    static List<Component> translateLinesCached(List<Component> tooltip,
                                                TranslationStore store) {
        if (tooltip == null || tooltip.isEmpty() || store == null) {
            return List.of();
        }
        List<Component> key = List.copyOf(tooltip);
        synchronized (LINES_CACHE) {
            CachedLines hit = LINES_CACHE.get(key);
            if (hit != null && hit.age() == store.cacheAge()) {
                return pageTitle(key, new ArrayList<>(hit.lines()), store);
            }
        }
        List<Component> out = List.copyOf(translateLines(tooltip, store));
        synchronized (LINES_CACHE) {
            LINES_CACHE.put(key, new CachedLines(store.cacheAge(), out));
        }
        return pageTitle(key, new ArrayList<>(out), store);
    }

    private static List<Component> pageTitle(List<Component> tooltip, List<Component> lines,
                                             TranslationStore store) {
        if (lines.isEmpty()) {
            return lines;
        }
        return com.wynnchayuan.translate.PageNames.titles(tooltip, lines, store.pageNameMode());
    }

    static List<Component> translateLines(List<Component> tooltip, TranslationStore store) {
        return translateLines(tooltip, store, true);
    }

    static List<Component> translateLines(List<Component> tooltip, TranslationStore store,
                                          boolean reuseParts) {
        int n = tooltip.size();
        List<Component> out = new ArrayList<>(n);
        boolean anyTranslated = false;

        boolean[] centered = BlockLayout.centered(tooltip);

        List<StyledText> styled = new ArrayList<>(n);
        for (Component line : tooltip) {
            styled.add(StyledText.fromComponent(line));
            com.wynnchayuan.capture.SelfNames.learn(line.getString());
        }

        java.util.IdentityHashMap<StyledText, com.wynnchayuan.capture.LineParts> parts =
                new java.util.IdentityHashMap<>();
        java.util.function.Function<StyledText, com.wynnchayuan.capture.LineParts> partsOf =
                reuseParts ? line -> parts.computeIfAbsent(line, com.wynnchayuan.capture.LineParts::of)
                           : com.wynnchayuan.capture.LineParts::of;

        boolean leftAligned = LineTranslator.columnsAreLeftAligned(styled);

        boolean[] hit = new boolean[n];
        boolean[] nameList = new boolean[n];

        int i = 0;
        List<int[]> spans = new ArrayList<>(n);
        boolean abilityPanel = TooltipShape.isAbilityNode(styled);
        String gearName = n == 0 || abilityPanel ? null : TooltipShape.bareName(
                partsOf.apply(styled.get(0)).template());
        boolean ownName = gearName != null && store.translatesNames()
                && store.gearOwnName(gearName) != null
                && TranslationStore.nameRowInner(partsOf.apply(
                        styled.get(0)).template()) != null;
        if (gearName != null && !ownName && store.isBareGearName(gearName)) {
            out.add(LineTranslator.untranslated(styled.get(0)));
            spans.add(new int[] {0, 1, 0, 1});
            i = 1;
            // 物品名稱是兩行：第 0 行寬度 0 看不見
            if (n > 1 && gearName.equals(TooltipShape.bareName(
                    partsOf.apply(styled.get(1)).template()))) {
                out.add(LineTranslator.untranslated(styled.get(1)));
                spans.add(new int[] {1, 2, 1, 2});
                i = 2;
            }
        }
        if (i == 0 && n > 0 && !abilityPanel && TooltipShape.crafted(styled)) {
            String name = TooltipShape.bareName(partsOf.apply(styled.get(0)).template());
            if (!name.isEmpty()) {
                out.add(LineTranslator.untranslated(styled.get(0)));
                spans.add(new int[] {0, 1, 0, 1});
                i = 1;
                if (n > 1 && name.equals(TooltipShape.bareName(
                        partsOf.apply(styled.get(1)).template()))) {
                    out.add(LineTranslator.untranslated(styled.get(1)));
                    spans.add(new int[] {1, 2, 1, 2});
                    i = 2;
                }
            }
        }
        if (i == 0 && n > 0 && !abilityPanel) {
            String row0 = partsOf.apply(styled.get(0)).template();
            String name = TooltipShape.bareName(row0);
            if (!name.isEmpty() && store.gearNameOnly(name)
                    && (row0.strip().equals(name) || store.lookupExact(row0) == null)
                    && !TooltipShape.itemShaped(styled)) {
                out.add(LineTranslator.untranslated(styled.get(0)));
                spans.add(new int[] {0, 1, 0, 1});
                nameList[0] = true;
                i = 1;
                if (n > 1 && name.equals(TooltipShape.bareName(
                        partsOf.apply(styled.get(1)).template()))) {
                    out.add(LineTranslator.untranslated(styled.get(1)));
                    spans.add(new int[] {1, 2, 1, 2});
                    nameList[1] = true;
                    i = 2;
                }
            }
        }
        boolean itemRows = n > 0 && !abilityPanel && TranslationStore.nameRowInner(
                partsOf.apply(styled.get(0)).template()) != null;
        boolean lootrunTitle = n > 0 && TooltipShape.isLootrunCard(styled)
                && com.wynnchayuan.translate.LootrunNames.isName(TooltipShape.bareName(
                        partsOf.apply(styled.get(0)).template()));
        try (LookupScope holdScope = LookupScope.holdAppendedOriginal(false);
             LookupScope nameScope = LookupScope.nameLine(false);
             LookupScope titleScope = LookupScope.lootrunTitle(false)) {
            while (i < n) {
                holdScope.set(i >= 2);
                nameScope.set(itemRows && i < 2);
                titleScope.set(lootrunTitle && i == 0);
                com.wynnchayuan.translate.GuessListRow.Row listRow = abilityPanel ? null
                        : com.wynnchayuan.translate.GuessListRow.of(styled.get(i), store);
                if (listRow != null) {
                    nameList[i] = true;
                    spans.add(new int[] {i, i + 1, out.size(), out.size() + 1});
                    if (listRow.translated() != null) {
                        anyTranslated = true;
                        hit[i] = true;
                        out.add(listRow.translated());
                    } else {
                        out.add(LineTranslator.untranslated(styled.get(i)));
                    }
                    i++;
                    continue;
                }
                int longest = Math.min(store.maxBlockLines(), n - i);
                List<Component> block = null;
                int used = 0;
                for (int len = longest; len >= 2 && block == null; len--) {
                    boolean[] slice = new boolean[len];
                    System.arraycopy(centered, i, slice, 0, len);
                    block = LineTranslator.translateBlock(
                            styled.subList(i, i + len), store, slice, partsOf);
                    used = len;
                }
                if (block != null) {
                    spans.add(new int[] {i, i + used, out.size(), out.size() + block.size()});
                    out.addAll(block);
                    anyTranslated = true;
                    for (int k = i; k < i + used; k++) {
                        hit[k] = true;
                    }
                    i += used;
                    continue;
                }
                boolean paragraphStart =
                        i == 0 || styled.get(i - 1).getStringWithoutFormatting().isBlank();
                if (longest >= 2 && paragraphStart) {
                    StringBuilder key = new StringBuilder();
                    for (int k = i; k < i + longest; k++) {
                        if (k > i) {
                            key.append(System.lineSeparator());
                        }
                        key.append(partsOf.apply(styled.get(k)).template());
                    }
                    LineTranslator.noteBlockMiss(key.toString(), store);
                }
                if (paragraphStart) {
                    try {
                        String para = com.wynnchayuan.capture.CardDump.paragraphKey(styled, i);
                        if (para != null) {
                            com.wynnchayuan.capture.CardDump.note(tooltip, styled, para, store);
                        }
                    } catch (Throwable failure) {
                        ErrorDebug.note("TooltipTranslator#translateLines", null, failure);
                    }
                }
                Component translated =
                        !abilityPanel && !(ownName && i < 2)
                                && TooltipShape.blockedGearName(styled.get(i), store)
                        ? null
                        : LineTranslator.translate(styled.get(i), store, centered[i],
                                                   leftAligned);
                spans.add(new int[] {i, i + 1, out.size(), out.size() + 1});
                if (translated != null) {
                    anyTranslated = true;
                    hit[i] = true;
                    out.add(translated);
                } else {
                    out.add(LineTranslator.untranslated(styled.get(i)));
                }
                i++;
            }
        }
        try {
            int[] rowOf = new int[n];
            java.util.Arrays.fill(rowOf, -1);
            for (int[] span : spans) {
                if (span[1] - span[0] == 1 && span[3] - span[2] == 1) {
                    rowOf[span[0]] = span[2];
                }
            }
            com.wynnchayuan.translate.IndentAlign.apply(
                    styled, out, rowOf, hit, centered, abilityPanel, TooltipTranslator::measure);
        } catch (RuntimeException e) {
            ErrorDebug.note("TooltipTranslator#translateLines", null, e);
        }
        List<String> plain = new ArrayList<>(n);
        for (int k = 0; k < n; k++) {
            if (nameList[k]) {
                plain.add("");
                continue;
            }
            String text = com.wynnchayuan.capture.GlyphSplitter
                    .stripGlyphChars(TooltipShape.visibleText(styled.get(k))).strip();
            plain.add(text.substring(TranslationStore.indentOf(text)).strip());
        }
        if (TooltipShape.evenOut(plain, hit)) {
            out = regroup(out, spans, styled, plain, hit);
            anyTranslated = false;
            for (int k = 0; k < n; k++) {
                anyTranslated |= hit[k];
            }
        }
        if (!anyTranslated) {
            return List.of();
        }
        if (anyMissed(styled, hit, store)) {
            TooltipDebug.dumpPartial(tooltip, out, hit);
        }
        out.replaceAll(LineTranslator::unslantAll);
        com.wynnchayuan.translate.NameWrap.Split split =
                com.wynnchayuan.translate.NameWrap.split(
                        tooltip, out, centered, store, TooltipTranslator::measure);
        com.wynnchayuan.translate.LayoutDebug.drawn(
                "\u7ffb\u5b8c", tooltip, out, TooltipTranslator::measure);
        List<Component> fitted = com.wynnchayuan.translate.TooltipWiden.fit(
                split.original(), split.translated(), split.centered(),
                leftAligned, TooltipTranslator::measure);
        com.wynnchayuan.translate.LayoutDebug.drawn(
                "\u6490\u5bec\u5f8c", split.original(), fitted, TooltipTranslator::measure);
        noteWidened(split.original(), fitted);
        if (abilityPanel) {
            TooltipDebug.dumpAbility(tooltip, fitted);
        }
        return fitted;
    }

    private static void noteWidened(List<Component> original, List<Component> fitted) {
        try {
            if (original == null || fitted == null || original.size() != fitted.size()) {
                return;
            }
            int frame = 0;
            int widest = 0;
            int at = -1;
            for (int i = 0; i < fitted.size(); i++) {
                frame = Math.max(frame, measure(original.get(i)));
                int w = measure(fitted.get(i));
                if (w > widest) {
                    widest = w;
                    at = i;
                }
            }
            if (at < 0 || frame <= 0) {
                return;
            }
            if (widest <= frame && com.wynnchayuan.capture.OverflowAudit.size() == 0) {
                return;
            }
            String src = com.wynnchayuan.capture.LineParts.of(
                    com.wynntils.core.text.StyledText.fromComponent(original.get(at))).template();
            String dst = com.wynnchayuan.capture.LineParts.of(
                    com.wynntils.core.text.StyledText.fromComponent(fitted.get(at))).template();
            if (src.equals(dst)) {
                return;
            }
            com.wynnchayuan.capture.OverflowAudit.note(
                    com.wynnchayuan.capture.OverflowAudit.TOOLTIP, src, dst, frame, widest,
                    com.wynnchayuan.capture.OverflowAudit.PX_UNIT);
        } catch (Throwable t) {
            ErrorDebug.note("TooltipTranslator#noteWidened", null, t);
        }
    }

    private static int measure(Component line) {
        return com.wynnchayuan.translate.LineTranslator.width(line);
    }

    private static boolean anyMissed(List<StyledText> styled, boolean[] hit,
                                     TranslationStore store) {
        for (int k = 0; k < hit.length && k < styled.size(); k++) {
            if (hit[k]) {
                continue;
            }
            String template = com.wynnchayuan.capture.LineParts
                    .of(styled.get(k)).template();
            if (!template.isBlank() && store.hasTranslation(template)) {
                return true;
            }
        }
        return false;
    }

    static List<Component> translateInPlace(List<Component> tooltip,
                                            TranslationStore store) {
        try {
            List<Component> translated = translateLinesCached(tooltip, store);
            return translated.isEmpty() ? List.of() : translated;
        } catch (RuntimeException e) {
            Component first = tooltip == null || tooltip.isEmpty() ? null : tooltip.get(0);
            com.wynnchayuan.translate.ErrorDebug.note("tooltip.inPlace",
                    first == null ? null : first.getString(), e);
            return List.of();
        }
    }

    static List<Component> regroup(List<Component> out, List<int[]> spans,
                                   List<StyledText> styled, List<String> plain, boolean[] hit) {
        boolean changed;
        do {
            changed = false;
            for (int[] span : spans) {
                boolean some = false;
                boolean all = true;
                for (int k = span[0]; k < span[1]; k++) {
                    some |= hit[k];
                    all &= hit[k];
                }
                if (some && !all) {
                    for (int k = span[0]; k < span[1]; k++) {
                        hit[k] = false;
                    }
                    changed = true;
                }
            }
            if (changed) {
                TooltipShape.evenOut(plain, hit);
            }
        } while (changed);

        List<Component> rebuilt = new ArrayList<>(hit.length);
        for (int[] span : spans) {
            boolean kept = span[0] < span[1] && hit[span[0]]
                    && span[2] <= span[3] && span[3] <= out.size();
            if (kept) {
                rebuilt.addAll(out.subList(span[2], span[3]));
            } else {
                for (int k = span[0]; k < span[1]; k++) {
                    hit[k] = false;
                    rebuilt.add(LineTranslator.untranslated(styled.get(k)));
                }
            }
        }
        return rebuilt;
    }
}
