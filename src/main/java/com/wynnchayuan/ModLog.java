package com.wynnchayuan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ModLog {

    private static final Logger LOG = LoggerFactory.getLogger(WynnChaYuan.MOD_NAME);

    private static final String TAGGED = "[" + WynnChaYuan.MOD_NAME + "] {}";

    private static final int MAX_DISTINCT_ERRORS = 256;

    private static final Set<String> reported = ConcurrentHashMap.newKeySet();

    private ModLog() {}

    public static void info(String message) {
        LOG.info(TAGGED, message);
    }

    public static void warn(String message) {
        LOG.warn(TAGGED, message);
    }

    public static void error(String where, Throwable failure) {
        if (failure == null || reported.size() >= MAX_DISTINCT_ERRORS) {
            return;
        }
        if (reported.add(where + '|' + failure.getClass().getName())) {
            LOG.error(TAGGED, where + ": " + failure);
        }
    }
}
