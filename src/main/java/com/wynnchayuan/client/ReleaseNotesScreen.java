package com.wynnchayuan.client;

import com.wynnchayuan.Releases;
import com.wynnchayuan.SelfUpdate;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.NotesView;
import com.wynnchayuan.client.ui.Surface;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

public final class ReleaseNotesScreen extends CanvasScreen {

    private static final String[] LANGS = {"en_us", "zh_tw", "zh_cn"};

    private static String chosen;

    private final NotesView view;

    public ReleaseNotesScreen(Screen parent) {
        super(T.c("notes.title"), parent);
        SelfUpdate.noteScreenOpened();
        this.view = new NotesView(SHELL, new NotesView.Host() {
            @Override
            public List<String> versions() {
                return Releases.versions();
            }

            @Override
            public NotesView.Note notes(String version, String lang) {
                Releases.Notes notes = Releases.notesFor(version, lang);
                return notes == null ? null : new NotesView.Note(notes.headline(), notes.items());
            }

            @Override
            public String running() {
                return WynnChaYuan.version();
            }

            @Override
            public String newer() {
                return Releases.newer();
            }

            @Override
            public String[] languages() {
                return LANGS;
            }

            @Override
            public int language() {
                return shown();
            }

            @Override
            public void pickLanguage(int index) {
                chosen = LANGS[Math.max(0, Math.min(LANGS.length - 1, index))];
            }

            @Override
            public void download() {
                ConfirmLinkScreen.confirmLinkNow(ReleaseNotesScreen.this, Releases.downloadUrl());
            }

            @Override
            public String updateLabel() {
                if (!SelfUpdate.available()) {
                    return null;
                }
                return switch (SelfUpdate.state()) {
                    case DOWNLOADING -> SelfUpdate.percent() < 0
                            ? T.s("notes.update.downloading")
                            : T.s("notes.update.progress", SelfUpdate.percent() + "%");
                    case READY -> T.s("notes.update.ready");
                    case FAILED -> T.s("notes.update.failed");
                    default -> T.s("notes.update");
                };
            }

            @Override
            public void update() {
                String latest = Releases.newer();
                if (latest != null) {
                    SelfUpdate.start(latest);
                }
            }

            @Override
            public void close() {
                onClose();
            }
        });
    }

    private static int shown() {
        String want = chosen != null ? chosen : T.pinnedLanguage();
        for (int i = 0; i < LANGS.length; i++) {
            if (LANGS[i].equals(want)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    protected Surface view() {
        return view;
    }
}
