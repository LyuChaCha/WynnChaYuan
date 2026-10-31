package com.wynnchayuan;

public final class Check {

    public static int failures = 0;

    private Check() {}

    public static void check(String what, boolean ok) {
        System.out.println((ok ? "  [PASS] " : "  [FAIL] ") + what);
        if (!ok) {
            failures++;
        }
    }
}
