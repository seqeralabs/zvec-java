package io.seqera.zvec.internal;

import java.lang.foreign.SymbolLookup;

public final class NativeLoader {
    private static volatile SymbolLookup symbolLookup;
    private static volatile boolean loaded = false;

    private NativeLoader() {}

    public static synchronized void load() {
        if (loaded) return;
        String libPath = System.getProperty("zvec.native.path");
        if (libPath != null) {
            System.load(libPath);
        } else {
            System.loadLibrary("zvec_c");
        }
        symbolLookup = SymbolLookup.loaderLookup();
        loaded = true;
    }

    public static SymbolLookup symbolLookup() {
        if (!loaded) load();
        return symbolLookup;
    }
}
