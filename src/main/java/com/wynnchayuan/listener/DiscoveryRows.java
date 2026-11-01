package com.wynnchayuan.listener;

import com.wynnchayuan.capture.CaptureStore;
import com.wynnchayuan.capture.PlayerDataFilter;
import com.wynnchayuan.translate.DiscoveryBlock;
import com.wynnchayuan.translate.TranslationStore;
import com.wynntils.core.text.StyledText;

import java.util.ArrayList;
import java.util.List;

final class DiscoveryRows {

    static final long QUIET_MS = 300;

    private record Held(StyledText row, String ctx) {}

    private final List<Held> held = new ArrayList<>();
    private long last;

    synchronized boolean offer(StyledText row, String ctx, long now,
                               CaptureStore store, TranslationStore translations) {
        if (row == null || ChatBlock.isOurs(row)) {
            return false;
        }
        settle(now, store, translations);
        boolean head = DiscoveryBlock.isHead(row);
        if (head) {
            finish(store, translations);
        } else if (held.isEmpty()) {
            return false;
        }
        held.add(new Held(row, ctx));
        last = now;
        return !head;
    }

    synchronized void settle(long now, CaptureStore store, TranslationStore translations) {
        if (!held.isEmpty() && now - last > QUIET_MS) {
            finish(store, translations);
        }
    }

    private void finish(CaptureStore store, TranslationStore translations) {
        if (held.isEmpty()) {
            return;
        }
        List<StyledText> rows = new ArrayList<>(held.size());
        for (Held h : held) {
            rows.add(h.row());
        }
        boolean[] taken = new boolean[rows.size()];
        for (DiscoveryBlock.Block block : DiscoveryBlock.blocks(rows)) {
            taken[block.head()] = true;
            if (block.title() >= 0) {
                taken[block.title()] = true;
            }
            if (!block.hasProse()) {
                continue;
            }
            for (int i = block.from(); i < block.to(); i++) {
                taken[i] = true;
            }
            String sentence = DiscoveryBlock.sentenceTemplate(rows, block);
            if (DiscoveryBlock.prose(rows, block, translations) == null
                    && !PlayerDataFilter.carriesPlayerData(sentence)) {
                store.recordChatSentence(sentence, held.get(block.from()).ctx(),
                                         DiscoveryBlock.headTemplate(rows, block));
            }
        }
        for (int i = 0; i < taken.length; i++) {
            if (!taken[i]) {
                String template = CaptureListener.chatTemplate(rows.get(i), store::noteEvent, translations);
                if (template != null) {
                    store.recordChat(template, held.get(i).ctx(), last);
                }
            }
        }
        held.clear();
    }
}
