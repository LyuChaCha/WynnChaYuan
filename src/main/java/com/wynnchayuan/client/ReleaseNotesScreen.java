package com.wynnchayuan.client;

import com.wynnchayuan.Releases;
import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.NotesView;
import com.wynnchayuan.client.ui.Surface;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;

/**
 * 更新說明。
 *
 * <p>每一版都寫了英文、繁中、簡中三份。預設跟著介面語言，但玩家可以在右上角換——
 * 介面是日文、韓文的人沒有自己語言的說明，要能自己挑看得懂的那一份。
 * 選了之後整場遊戲都記著，不必每次打開再選一次。
 *
 * <p>畫面本身在 {@link NotesView}。
 */
public final class ReleaseNotesScreen extends CanvasScreen {

    private static final String[] LANGS = {"en_us", "zh_tw", "zh_cn"};

    /** 玩家自己挑的說明語言；沒挑過是 {@code null}，跟著介面語言。 */
    private static String chosen;

    private final NotesView view;

    public ReleaseNotesScreen(Screen parent) {
        super(T.c("notes.title"), parent);
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
            public void close() {
                onClose();
            }
        });
    }

    /** 現在顯示第幾種語言的說明：挑過就用挑的，沒有就看介面語言，都對不上用英文。 */
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
