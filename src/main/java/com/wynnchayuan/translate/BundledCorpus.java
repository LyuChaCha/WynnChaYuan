package com.wynnchayuan.translate;

import org.tukaani.xz.XZInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public final class BundledCorpus {

    public static final String PACKED = ".xz";

    @FunctionalInterface
    public interface Source {
        InputStream open(String resource) throws IOException;
    }

    private static final Source CLASSPATH = BundledCorpus.class::getResourceAsStream;

    private static volatile Source source = CLASSPATH;

    private BundledCorpus() {}

    public static InputStream open(String resource) throws IOException {
        Source from = source;
        InputStream packed = from.open(resource + PACKED);
        if (packed == null) {
            return from.open(resource);
        }
        try {
            return new XZInputStream(new BufferedInputStream(packed, 1 << 16));
        } catch (IOException | RuntimeException e) {
            packed.close();
            throw e;
        }
    }

    static void readFrom(Source from) {
        source = from == null ? CLASSPATH : from;
    }
}
