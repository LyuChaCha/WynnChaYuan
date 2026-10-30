package com.wynnchayuan;

import java.util.ArrayList;
import java.util.List;

public final class NetworkGate {

    private NetworkGate() {}

    public interface Services {
        void updateCheck();

        void translationSync();

        void deleteData();
    }

    public static final String UPDATE_CHECK = "update-check";
    public static final String TRANSLATION_SYNC = "translation-sync";
    public static final String DELETE_DATA = "delete-data";

    private static volatile CollectorConfig config;
    private static boolean started;
    private static boolean deleteTried;

    public static void use(CollectorConfig c) {
        config = c;
    }

    public static boolean online() {
        return online(config);
    }

    public static boolean online(CollectorConfig c) {
        return c != null && c.consent() == CollectorConfig.Consent.AGREED;
    }

    public static boolean registry() {
        return registry(config);
    }

    public static boolean registry(CollectorConfig c) {
        return online(c) && !c.dataOptOut();
    }

    public static boolean deletionPending(CollectorConfig c) {
        return c != null && c.dataOptOut() && c.dataDeletePending();
    }

    public static synchronized List<String> start(CollectorConfig c, Services services) {
        List<String> ran = new ArrayList<>();
        if (online(c) && !started) {
            started = true;
            services.updateCheck();
            ran.add(UPDATE_CHECK);
            if (c.source() == CollectorConfig.Source.GITHUB) {
                services.translationSync();
                ran.add(TRANSLATION_SYNC);
            }
        }
        if (deletionPending(c) && !deleteTried) {
            deleteTried = true;
            services.deleteData();
            ran.add(DELETE_DATA);
        }
        return ran;
    }

    public static synchronized boolean started() {
        return started;
    }

    static synchronized void reset() {
        started = false;
        deleteTried = false;
    }
}
