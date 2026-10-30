package com.wynnchayuan.client.ui;

public interface Shell {

    int accent();

    int frame();

    boolean blurred();

    String tr(String key, Object... args);

    String language();

    void click();
}
