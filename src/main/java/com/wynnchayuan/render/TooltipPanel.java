package com.wynnchayuan.render;

import com.wynnchayuan.CollectorConfig;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.translate.BlockLayout;
import com.wynnchayuan.translate.LineTranslator;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Vector2i;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TooltipPanel {


    private static List<Component> lastDiagnosed = List.of();

    private TooltipPanel() {}

    private static final int CACHE_LIMIT = 256;

    private static final Map<List<Component>, CachedLines> LINES_CACHE =
            new LinkedHashMap<>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<List<Component>, CachedLines> eldest) {
                    return size() > CACHE_LIMIT;
                }
            };

    private record CachedLines(int generation, boolean namesWithOriginal,
                               boolean translateNames, boolean peekPlain,
                               List<Component> lines) {}

    static List<Component> translateLinesCached(List<Component> tooltip,
                                                TranslationStore store) {
        if (tooltip == null || tooltip.isEmpty() || store == null) {
            return List.of();
        }
        List<Component> key = List.copyOf(tooltip);
        synchronized (LINES_CACHE) {
            CachedLines hit = LINES_CACHE.get(key);
            if (hit != null && hit.generation() == store.generation()
                    && hit.namesWithOriginal() == store.namesWithOriginal()
                    && hit.translateNames() == store.translatesNames()
                    && hit.peekPlain() == store.peeksPlainNames()) {
                return pageTitle(key, new ArrayList<>(hit.lines()), store);
            }
        }
        List<Component> out = List.copyOf(translateLines(tooltip, store));
        synchronized (LINES_CACHE) {
            LINES_CACHE.put(key, new CachedLines(store.generation(),
                    store.namesWithOriginal(), store.translatesNames(),
                    store.peeksPlainNames(), out));
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

    public static void render(GuiGraphics graphics, List<Component> tooltip,
                              int mouseX, int mouseY, TranslationStore store) {
        if (tooltip == null || tooltip.isEmpty()) {
            return;
        }
        tooltip = ThirdPartySections.strip(tooltip);
        List<Component> lines = translateLinesCached(tooltip, store);
        if (!tooltip.equals(lastDiagnosed)) {
            lastDiagnosed = List.copyOf(tooltip);
            WynnChaYuan.store().noteEvent(lines.isEmpty() ? "panel.noMatch" : "panel.shown");
            if (lines.isEmpty()) {
                TooltipDebug.dump(tooltip);
            }
        }
        if (lines.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        int screenW = graphics.guiWidth();
        int screenH = graphics.guiHeight();
        int gap = com.wynnchayuan.PanelGap.layout(WynnChaYuan.config().panelGap());

        int[] panelBox = box(mc, lines);
        int panelW = panelBox[0] + 8;
        int panelH = panelBox[1] + 8;

        Place spot;
        if (WynnChaYuan.config().panelAnchor() == CollectorConfig.PanelAnchor.FIXED) {
            spot = new Place(WynnChaYuan.config().fixedX(), WynnChaYuan.config().fixedY(),
                    0, 0, false, 0, 0);
        } else {
            spot = place(mc, lines, tooltip, mouseX, mouseY, screenW, screenH, gap);
        }
        int x = Math.max(0, Math.min(spot.panelX(), Math.max(0, screenW - panelW)));
        int y = Math.max(0, Math.min(spot.panelY(), Math.max(0, screenH - panelH)));
        if (WynnChaYuan.config().panelAnchor() != CollectorConfig.PanelAnchor.FIXED) {
            x = WynnventoryBox.place(x, y, panelW, panelH,
                    spot.leftSide(), spot.rightSide(), screenW, gap);
        }

        if (PendingTooltip.pushed()) {
            drawAt(graphics, mc, tooltip, spot.originalX(), spot.originalY());
        }

        List<ClientTooltipComponent> components = lines.stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .toList();

        OriginalTooltipBox.beginOwn();
        try {
            graphics.renderTooltip(mc.font, components, x, y, EXACT, null);
        } finally {
            OriginalTooltipBox.endOwn();
        }
        String title = lines.isEmpty() ? null : lines.get(0).getString();
        PanelShot.note(x - SHOT_MARGIN, y - SHOT_MARGIN,
                panelBox[0] + SHOT_MARGIN * 2, panelBox[1] + SHOT_MARGIN * 2, title);
    }

    private static final int SHOT_MARGIN = 4;

    private static final ClientTooltipPositioner EXACT =
            (screenWidth, screenHeight, x, y, tooltipWidth, tooltipHeight) -> new Vector2i(x, y);

    public static List<Component> translateLines(List<Component> tooltip, TranslationStore store) {
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
        boolean abilityPanel = isAbilityNode(styled);
        String gearName = n == 0 || abilityPanel ? null : bareName(
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
            if (n > 1 && gearName.equals(bareName(
                    partsOf.apply(styled.get(1)).template()))) {
                out.add(LineTranslator.untranslated(styled.get(1)));
                spans.add(new int[] {1, 2, 1, 2});
                i = 2;
            }
        }
        if (i == 0 && n > 0 && !abilityPanel && crafted(styled)) {
            String name = bareName(partsOf.apply(styled.get(0)).template());
            if (!name.isEmpty()) {
                out.add(LineTranslator.untranslated(styled.get(0)));
                spans.add(new int[] {0, 1, 0, 1});
                i = 1;
                if (n > 1 && name.equals(bareName(
                        partsOf.apply(styled.get(1)).template()))) {
                    out.add(LineTranslator.untranslated(styled.get(1)));
                    spans.add(new int[] {1, 2, 1, 2});
                    i = 2;
                }
            }
        }
        if (i == 0 && n > 0 && !abilityPanel) {
            String row0 = partsOf.apply(styled.get(0)).template();
            String name = bareName(row0);
            if (!name.isEmpty() && store.gearNameOnly(name)
                    && (row0.strip().equals(name) || store.lookupExact(row0) == null)
                    && !itemShaped(styled)) {
                out.add(LineTranslator.untranslated(styled.get(0)));
                spans.add(new int[] {0, 1, 0, 1});
                nameList[0] = true;
                i = 1;
                if (n > 1 && name.equals(bareName(
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
        boolean lootrunTitle = n > 0 && isLootrunCard(styled)
                && com.wynnchayuan.translate.LootrunNames.isName(bareName(
                        partsOf.apply(styled.get(0)).template()));
        try {
            while (i < n) {
                TranslationStore.holdAppendedOriginal(i >= 2);
                TranslationStore.nameLine(itemRows && i < 2);
                TranslationStore.lootrunTitle(lootrunTitle && i == 0);
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
                    } catch (Throwable ignored) {
                    }
                }
                Component translated =
                        !abilityPanel && !(ownName && i < 2)
                                && blockedGearName(styled.get(i), store)
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
        } finally {
            TranslationStore.holdAppendedOriginal(false);
            TranslationStore.nameLine(false);
            TranslationStore.lootrunTitle(false);
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
                    styled, out, rowOf, hit, centered, abilityPanel, TooltipPanel::measure);
        } catch (RuntimeException e) {
        }
        List<String> plain = new ArrayList<>(n);
        for (int k = 0; k < n; k++) {
            if (nameList[k]) {
                plain.add("");
                continue;
            }
            String text = com.wynnchayuan.capture.GlyphSplitter
                    .stripGlyphChars(visibleText(styled.get(k))).strip();
            plain.add(text.substring(TranslationStore.indentOf(text)).strip());
        }
        if (evenOut(plain, hit)) {
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
                        tooltip, out, centered, store, TooltipPanel::measure);
        com.wynnchayuan.translate.LayoutDebug.drawn(
                "\u7ffb\u5b8c", tooltip, out, TooltipPanel::measure);
        List<Component> fitted = com.wynnchayuan.translate.TooltipWiden.fit(
                split.original(), split.translated(), split.centered(),
                leftAligned, TooltipPanel::measure);
        com.wynnchayuan.translate.LayoutDebug.drawn(
                "\u6490\u5bec\u5f8c", split.original(), fitted, TooltipPanel::measure);
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

    static boolean evenOut(List<String> plain, boolean[] hit) {
        boolean changed = false;
        int i = 0;
        while (i < hit.length) {
            int end = i + 1;
            while (end < hit.length && continues(plain.get(end - 1), plain.get(end))) {
                end++;
            }
            if (end - i > 1) {
                boolean some = false;
                boolean all = true;
                for (int k = i; k < end; k++) {
                    if (isAddress(plain.get(k)) || nothingToTranslate(plain.get(k))) {
                        continue;
                    }
                    some |= hit[k];
                    all &= hit[k];
                }
                if (some && !all) {
                    for (int k = i; k < end; k++) {
                        hit[k] = false;
                    }
                    changed = true;
                }
            }
            i = end;
        }
        return changed;
    }

    static boolean isAddress(String line) {
        return line != null && ADDRESS.matcher(line.strip()).matches();
    }

    private static final java.util.regex.Pattern ADDRESS = java.util.regex.Pattern.compile(
            "(?:https?://)?[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+(?:/\\S*)?");

    static boolean nothingToTranslate(String line) {
        return line != null
                && !com.wynnchayuan.capture.GlyphSplitter.hasLetter(line);
    }

    static String visibleText(StyledText line) {
        StringBuilder out = new StringBuilder();
        for (com.wynntils.core.text.StyledTextPart part : line) {
            com.wynntils.core.text.PartStyle ps = part.getPartStyle();
            if (ps != null && isInvisibleFont(ps.getStyle())) {
                continue;
            }
            out.append(part.getString(null, com.wynntils.core.text.type.StyleType.NONE));
        }
        return out.toString();
    }

    private static boolean isInvisibleFont(net.minecraft.network.chat.Style style) {
        if (style == null) {
            return false;
        }
        net.minecraft.network.chat.FontDescription font = style.getFont();
        return font instanceof net.minecraft.network.chat.FontDescription.Resource resource
                && resource.id() != null && "invisible".equals(resource.id().getPath());
    }

    private static boolean continues(String prev, String line) {
        if (prev.isEmpty() || line.isEmpty()) {
            return false;
        }
        char end = prev.charAt(prev.length() - 1);
        if (end == '.' || end == '!' || end == '?' || end == ':' || end == ';') {
            return false;
        }
        char start = line.charAt(0);
        return start >= 'a' && start <= 'z' || endsMidPhrase(prev) || prose(prev, line);
    }

    private static boolean prose(String prev, String line) {
        char end = prev.charAt(prev.length() - 1);
        return (Character.isLetter(end) || end == ',')
                && hasLowerWord(prev)
                && proseLine(prev) && proseLine(line);
    }

    private static boolean hasLowerWord(String line) {
        for (int i = 1; i < line.length(); i++) {
            if (line.charAt(i - 1) == ' ' && Character.isLowerCase(line.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean proseLine(String line) {
        if (!Character.isLetter(line.charAt(0))) {
            return false;
        }
        if (line.indexOf(' ') < 0 || line.indexOf(':') >= 0) {
            return false;
        }
        for (int i = 0; i < line.length(); i++) {
            if (Character.isDigit(line.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean endsMidPhrase(String prev) {
        int space = prev.lastIndexOf(' ');
        if (space <= 0) {
            return false;
        }
        return LINK_WORDS.contains(prev.substring(space + 1).toLowerCase(java.util.Locale.ROOT));
    }

    private static final java.util.Set<String> LINK_WORDS = java.util.Set.of(
            "a", "an", "the", "of", "to", "at", "for", "with", "from", "into",
            "by", "in", "on", "and", "or", "your");

    public static void noteShot(GuiGraphics graphics, List<Component> tooltip,
                                int mouseX, int mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.font == null || tooltip == null || tooltip.isEmpty()) {
            return;
        }
        int[] shotBox = box(mc, tooltip);
        int w = shotBox[0];
        int h = shotBox[1];

        org.joml.Vector2ic at = net.minecraft.client.gui.screens.inventory.tooltip
                .DefaultTooltipPositioner.INSTANCE.positionTooltip(
                        graphics.guiWidth(), graphics.guiHeight(), mouseX, mouseY, w, h);
        int x = at.x();
        int y = at.y();
        PanelShot.note(x - SHOT_MARGIN, y - SHOT_MARGIN,
                       w + SHOT_MARGIN * 2, h + SHOT_MARGIN * 2,
                       tooltip.get(0).getString());
    }

    private static boolean isAbilityNode(List<StyledText> styled) {
        return anyMarkers(styled, PANEL_MARKERS);
    }

    private static boolean isLootrunCard(List<StyledText> styled) {
        return anyMarkers(styled, LOOTRUN_CARD_MARKERS);
    }

    private static final String[][] LOOTRUN_CARD_MARKERS = {
        {"Objective:", "Reward:"},
        {"Trial:", "Reward:"},
    };

    private static boolean anyMarkers(List<StyledText> styled, String[][] groups) {
        StringBuilder all = new StringBuilder();
        for (StyledText line : styled) {
            all.append(line.getStringWithoutFormatting()).append('\n');
        }
        String text = all.toString();
        for (String[] markers : groups) {
            boolean every = true;
            for (String marker : markers) {
                every &= text.contains(marker);
            }
            if (every) {
                return true;
            }
        }
        return false;
    }

    private static final String[][] PANEL_MARKERS = {
        {"Ability Points:"},
        {"Blocked by:"},
        {"Blocked by another ability"},
        {"Click to unlock this ability"},
        {"Objective:", "Reward:"},
        {"Trial:", "Reward:"},
        {"Dynamic Boon", "Click to choose!"},
        {"Static Boon", "Click to choose!"},
    };

    private static boolean blockedGearName(StyledText line, TranslationStore store) {
        String bare = bareName(com.wynnchayuan.capture.LineParts.of(line).template());
        int at = 0;
        while (at < bare.length() && BULLETS.indexOf(bare.charAt(at)) >= 0) {
            at++;
        }
        return store.isBareGearName(bare.substring(at).strip())
                || guessList(line.getStringWithoutFormatting(), store);
    }

    static boolean guessList(String plain, TranslationStore store) {
        String text = com.wynnchayuan.capture.GlyphSplitter.stripGlyphChars(plain).strip();
        int at = 0;
        while (at < text.length() && (BULLETS.indexOf(text.charAt(at)) >= 0
                || Character.isWhitespace(text.charAt(at)))) {
            at++;
        }
        java.util.regex.Matcher row = GUESS_ROW.matcher(text.substring(at));
        if (!row.matches()) {
            return false;
        }
        for (String name : row.group(1).split(",")) {
            if (store.isBareGearName(name.strip())) {
                return true;
            }
        }
        return false;
    }

    private static final java.util.regex.Pattern GUESS_ROW = java.util.regex.Pattern.compile(
            "[^\\d:\\[\\]]{1,16} \\d+(?: \\[[^\\]]*\\])?: (.+)");

    private static final String BULLETS = "-–—•*";

    private static final java.util.regex.Pattern CRAFTED_BY =
            java.util.regex.Pattern.compile("(?:^|[^A-Za-z])Crafted by \\S");

    static boolean crafted(List<StyledText> lines) {
        for (StyledText line : lines) {
            if (line != null && CRAFTED_BY.matcher(line.getStringWithoutFormatting()).find()) {
                return true;
            }
        }
        return false;
    }

    static boolean itemShaped(List<StyledText> styled) {
        int n = styled.size();
        if (n == 0) {
            return false;
        }
        if (TranslationStore.nameRowInner(
                com.wynnchayuan.capture.LineParts.of(styled.get(0)).template()) != null) {
            return true;
        }
        for (int k = 1; k < Math.min(n, BANNER_ROWS + 1); k++) {
            String t = com.wynnchayuan.capture.LineParts.of(styled.get(k)).template();
            if (bannerRow(t)) {
                return true;
            }
        }
        for (StyledText line : styled) {
            if (line != null && ITEM_ROW.matcher(line.getStringWithoutFormatting()).find()) {
                return true;
            }
        }
        return false;
    }

    private static final int BANNER_ROWS = 4;

    static boolean bannerRow(String template) {
        String icon = com.wynnchayuan.capture.GlyphSplitter.GLYPH_PLACEHOLDER;
        String rest = template.replace(icon, "");
        if (!rest.isBlank()) {
            return false;
        }
        int icons = (template.length() - rest.length()) / icon.length();
        return icons >= 3;
    }

    private static final java.util.regex.Pattern ITEM_ROW = java.util.regex.Pattern.compile(
            "Combat Level|Combat Lv\\. Min|Class Type|Class Req|\\d DPS\\b"
                    + "|(?:^|[^A-Za-z])Crafted by \\S");

    public static String bareName(String template) {
        String s = template
                .replace(com.wynnchayuan.capture.GlyphSplitter.GLYPH_PLACEHOLDER, "")
                .strip();
        while (s.endsWith("]")) {
            int open = s.lastIndexOf('[');
            if (open <= 0) {
                break;
            }
            s = s.substring(0, open).strip();
        }
        return s;
    }

    public static List<Component> translateInPlace(List<Component> tooltip,
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
                evenOut(plain, hit);
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

    static int[] box(Minecraft mc, List<Component> lines) {
        List<ClientTooltipComponent> parts = lines.stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .toList();
        int w = 0;
        for (ClientTooltipComponent part : parts) {
            w = Math.max(w, part.getWidth(mc.font));
        }
        return new int[] {w, (parts.size() == 1 ? -2 : 0) + height(parts)};
    }

    static int height(List<ClientTooltipComponent> components) {
        int h = 0;
        for (ClientTooltipComponent c : components) {
            h += c.getHeight(Minecraft.getInstance().font);
        }
        return h;
    }

    record Place(int panelX, int panelY, int originalX, int originalY, boolean overlaps,
                 int leftSide, int rightSide) {}

    // 仿原版 DefaultTooltipPositioner：游標右下 12/-12
    static int originalLeftFor(int mouseX, int screenW, int textWidth) {
        int left = mouseX + 12;
        if (left + textWidth > screenW) {
            left = Math.max(left - 24 - textWidth, 4);
        }
        return left;
    }

    static Place place(Minecraft mc, List<Component> lines, List<Component> tooltip,
                       int mouseX, int mouseY, int screenW, int screenH, int gap) {
        int[] panelBox = box(mc, lines);
        int[] originalBox = box(mc, tooltip);
        OriginalTooltipBox.Box drawn = OriginalTooltipBox.find(originalBox[0], originalBox[1]);
        return layout(panelBox[0], panelBox[1], originalBox[0], originalBox[1], drawn,
                mouseX, mouseY, screenW, screenH, gap, WynnChaYuan.config().panelSide());
    }

    static Place layout(int panelTextW, int panelTextH, int originalTextW, int originalTextH,
                        OriginalTooltipBox.Box drawn, int mouseX, int mouseY,
                        int screenW, int screenH, int gap, CollectorConfig.PanelSide side) {
        int panelW = panelTextW + 8;
        int panelH = panelTextH + 8;

        int originalLeft;
        int originalTop;
        float scale;
        if (drawn != null) {
            originalLeft = Math.round(drawn.x());
            originalTop = Math.round(drawn.y());
            scale = drawn.scale();
        } else {
            originalLeft = originalLeftFor(mouseX, screenW, originalTextW);
            originalTop = mouseY - 12;
            if (originalTop + originalTextH + 3 > screenH) {
                originalTop = screenH - originalTextH - 3;
            }
            scale = 1f;
        }
        int frame = OriginalTooltipBox.FRAME;
        int originalFrame = Math.round(frame * scale);
        int shownW = Math.round(originalTextW * scale);
        int shownH = Math.round(originalTextH * scale);

        boolean originalOnRight = originalLeft >= mouseX;
        int rightSide = originalLeft + shownW + originalFrame + gap + frame;
        int leftSide = originalLeft - originalFrame - gap - frame - panelTextW;

        int x = switch (side) {
            case RIGHT -> rightSide;
            case LEFT -> leftSide;
            default -> originalOnRight ? leftSide : rightSide;
        };
        int y = originalTop;

        x = Math.max(0, Math.min(x, Math.max(0, screenW - panelW)));
        y = Math.max(0, Math.min(y, Math.max(0, screenH - panelH)));

        int textLeft = originalLeft - 4;
        int textTop = originalTop - 4;
        boolean overlaps = x < textLeft + shownW + 8 && x + panelW > textLeft
                && y < textTop + shownH + 8 && y + panelH > textTop;

        int originalX = originalLeft;
        int originalY = originalTop;
        if (overlaps) {
            originalX = originalOnRight ? x + panelTextW + frame + gap + frame
                    : x - frame - gap - frame - originalTextW;
            originalY = y;
        }
        return new Place(x, y, originalX, originalY, overlaps, leftSide, rightSide);
    }

    public static boolean wouldPush(List<Component> tooltip, int mouseX, int mouseY,
                                    TranslationStore store) {
        try {
            if (WynnChaYuan.config() == null
                    || WynnChaYuan.config().panelAnchor() == CollectorConfig.PanelAnchor.FIXED
                    || tooltip == null || tooltip.isEmpty()) {
                return false;
            }
            List<Component> stripped = ThirdPartySections.strip(tooltip);
            List<Component> lines = translateLinesCached(stripped, store);
            if (lines.isEmpty()) {
                return false;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getWindow() == null) {
                return false;
            }
            return place(mc, lines, stripped, mouseX, mouseY,
                    mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(),
                    com.wynnchayuan.PanelGap.layout(WynnChaYuan.config().panelGap())).overlaps();
        } catch (Throwable t) {
            return false;
        }
    }

    private static void drawAt(GuiGraphics graphics, Minecraft mc,
                               List<Component> lines, int x, int y) {
        List<ClientTooltipComponent> parts = lines.stream()
                .map(Component::getVisualOrderText)
                .map(ClientTooltipComponent::create)
                .toList();
        OriginalTooltipBox.beginOwn();
        try {
            graphics.renderTooltip(mc.font, parts, x, y, EXACT, null);
        } finally {
            OriginalTooltipBox.endOwn();
        }
    }
}
