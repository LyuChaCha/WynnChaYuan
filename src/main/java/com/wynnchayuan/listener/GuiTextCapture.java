package com.wynnchayuan.listener;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.capture.CardDump;
import com.wynnchayuan.capture.GlyphSplitter;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class GuiTextCapture {

    private static List<Component> lastSeen = List.of();

    private GuiTextCapture() {}

    public static void record(List<Component> tooltip) {
        record(tooltip, ItemStack.EMPTY);
    }

    public static void record(List<Component> tooltip, ItemStack stack) {
        if (tooltip == null || tooltip.isEmpty()
                || !WynnChaYuan.config().collect()
                || !WynnChaYuan.config().collectGuiText()) {
            return;
        }
        if (tooltip.equals(lastSeen)) {
            return;
        }
        lastSeen = List.copyOf(tooltip);

        boolean skipTitle = stack != null && stack.is(Items.PLAYER_HEAD);

        List<String> templates = new ArrayList<>(tooltip.size());
        for (Component line : tooltip) {
            StyledText styled = StyledText.fromComponent(line);
            if (GlyphSplitter.isGlyphOnly(styled)) {
                templates.add(null);
                continue;
            }
            templates.add(GlyphSplitter.toTemplate(styled));
        }
        skipTitle = skipTitle || PlayerDataFilter.isPartyCard(templates);
        CardDump.fromPlayerHead(stack != null && stack.is(Items.PLAYER_HEAD));
        boolean[] covered = covered(templates, GuiTextCapture::whole);

        int titleAt = -1;
        for (int i = 0; i < templates.size(); i++) {
            String t = templates.get(i);
            if (t != null && !t.isBlank() && GlyphSplitter.hasLetter(t)) {
                titleAt = i;
                break;
            }
        }
        if (titleAt < 0) {
            return;
        }
        String title = null;
        String head = templates.get(titleAt);
        if (skipTitle || PlayerDataFilter.carriesPlayerData(head)
                || PlayerDataFilter.looksAccountNamed(head)) {
            WynnChaYuan.store().noteEvent("gui.blocked.playerData");
        } else {
            title = head.strip();
            if (covered[titleAt]) {
                WynnChaYuan.store().noteEvent("gui.skipped.inBlock");
            } else {
                WynnChaYuan.store().recordIn(head, "name", "gui", "gui/title", null);
            }
        }
        for (String[] row : rows(templates, covered, titleAt + 1,
                WynnChaYuan.store()::isTranslated,
                t -> PlayerDataFilter.carriesPlayerData(t) || PlayerDataFilter.looksAccountNamed(t))) {
            if (row[1] == null) {
                WynnChaYuan.store().noteEvent(row[0]);
                continue;
            }
            WynnChaYuan.store().recordIn(row[0], "desc", "gui", row[1], title);
        }
    }

    static List<String[]> rows(List<String> templates, boolean[] covered, int from,
                               Predicate<String> translated, Predicate<String> personal) {
        List<String[]> out = new ArrayList<>();
        int i = from;
        while (i < templates.size()) {
            String line = templates.get(i);
            if (line == null || line.isBlank()) {
                i++;
                continue;
            }
            int to = i;
            while (to < templates.size() && templates.get(to) != null
                    && !templates.get(to).isBlank()) {
                to++;
            }
            paragraph(templates.subList(i, to), covered[i], translated, personal, out);
            i = to;
        }
        return out;
    }

    private static void paragraph(List<String> lines, boolean covered,
                                  Predicate<String> translated, Predicate<String> personal,
                                  List<String[]> out) {
        if (covered) {
            out.add(new String[] {"gui.skipped.inBlock", null});
            return;
        }
        List<String> words = new ArrayList<>();
        boolean listy = false;
        for (String line : lines) {
            if (!GlyphSplitter.hasLetter(line)) {
                continue;
            }
            if (personal.test(line)) {
                out.add(new String[] {"gui.blocked.playerData", null});
                return;
            }
            listy |= translated.test(line);
            words.add(line);
        }
        if (words.isEmpty()) {
            return;
        }
        if (words.size() == 1 || listy) {
            for (String line : words) {
                out.add(new String[] {line, "gui/line"});
            }
            return;
        }
        out.add(new String[] {String.join("\n", words), "gui/block"});
    }

    static boolean[] covered(List<String> templates,
                             Predicate<List<String>> known) {
        boolean[] out = new boolean[templates.size()];
        int from = 0;
        while (from < templates.size()) {
            String line = templates.get(from);
            if (line == null || line.isBlank()) {
                from++;
                continue;
            }
            int to = from;
            while (to < templates.size() && templates.get(to) != null
                    && !templates.get(to).isBlank()) {
                to++;
            }
            if (to - from > 1 && known.test(templates.subList(from, to))) {
                for (int i = from; i < to; i++) {
                    out[i] = true;
                }
            }
            from = to;
        }
        return out;
    }

    private static boolean whole(List<String> lines) {
        return whole(lines, WynnChaYuan.translations());
    }

    static boolean whole(List<String> lines, TranslationStore store) {
        if (store == null) {
            return false;
        }
        if (store.hasTranslation(String.join(" ", lines))
                || store.hasTranslation(String.join("\n", lines))) {
            return true;
        }
        List<String> bare = new ArrayList<>(lines.size());
        for (String line : lines) {
            String cut = line;
            while (cut.startsWith(GlyphSplitter.GLYPH_PLACEHOLDER)) {
                cut = cut.substring(GlyphSplitter.GLYPH_PLACEHOLDER.length());
            }
            bare.add(cut.strip());
        }
        String joined = String.join(" ", bare);
        return store.hasTranslation(joined) || store.lookupFlat(joined) != null;
    }
}
