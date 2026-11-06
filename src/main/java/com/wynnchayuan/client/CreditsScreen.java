package com.wynnchayuan.client;

import com.wynnchayuan.WynnChaYuan;
import com.wynnchayuan.client.ui.CreditsView;
import com.wynnchayuan.client.ui.Surface;
import com.wynnchayuan.translate.Languages;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class CreditsScreen extends CanvasScreen {

    private static final Set<String> BANDS = Set.of("developers", "sponsors", "contributors");

    private final CreditsView view;

    public CreditsScreen(Screen parent) {
        super(T.c("credits.title", WynnChaYuan.MOD_NAME), parent);
        this.view = new CreditsView(SHELL, new CreditsView.Host() {
            @Override
            public List<CreditsView.Section> sections() {
                List<CreditsView.Section> out = new ArrayList<>();
                for (Credits.Section section : Credits.sections()) {
                    List<CreditsView.Member> members = new ArrayList<>();
                    for (Credits.Member m : section.members()) {
                        members.add(new CreditsView.Member(m.name(), m.hasHead() ? m.mc() : null,
                                m.colour(), m.teams(), m.areas()));
                    }
                    boolean band = BANDS.contains(section.id());
                    out.add(new CreditsView.Section(section.role(),
                            band ? T.s("credits.about." + section.id()) : null, section.color(),
                            band, members));
                }
                return out;
            }

            @Override
            public String teamName(String code) {
                return Languages.nativeName(code);
            }

            @Override
            public String title() {
                return T.s("credits.title", WynnChaYuan.MOD_NAME);
            }

            @Override
            public String version() {
                return WynnChaYuan.version();
            }

            @Override
            public void close() {
                onClose();
            }
        });
    }

    @Override
    protected Surface view() {
        return view;
    }
}
