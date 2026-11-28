package com.wynnchayuan.client;

final class KeyLatch {

    private boolean armed = true;

    boolean press() {
        if (!armed) {
            return false;
        }
        armed = false;
        return true;
    }

    void settle(boolean held) {
        if (!held) {
            armed = true;
        }
    }
}
