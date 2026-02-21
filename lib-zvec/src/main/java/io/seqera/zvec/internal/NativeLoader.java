/*
 * Copyright 2026, Seqera Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.seqera.zvec.internal;

import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.SymbolLookup;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class NativeLoader {
    private static volatile SymbolLookup symbolLookup;
    private static volatile boolean loaded = false;

    private NativeLoader() {}

    public static synchronized void load() {
        if (loaded) return;

        // 1. Try explicit path from system property
        String libPath = System.getProperty("zvec.native.path");
        if (libPath != null) {
            System.load(libPath);
        }
        // 2. Try extracting from classpath (JAR resource)
        else if (!tryLoadFromClasspath()) {
            // 3. Fall back to java.library.path
            System.loadLibrary("zvec_c");
        }

        symbolLookup = SymbolLookup.loaderLookup();
        loaded = true;
    }

    public static SymbolLookup symbolLookup() {
        if (!loaded) load();
        return symbolLookup;
    }

    private static boolean tryLoadFromClasspath() {
        String platform = detectPlatform();
        if (platform == null) return false;

        String libName = System.getProperty("os.name", "").startsWith("Mac") ? "libzvec_c.dylib" : "libzvec_c.so";
        String resource = "native/" + platform + "/" + libName;

        try (InputStream in = NativeLoader.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) return false;

            Path tempFile = Files.createTempFile("zvec_c", libName.substring(libName.lastIndexOf('.')));
            tempFile.toFile().deleteOnExit();
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            System.load(tempFile.toAbsolutePath().toString());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static String detectPlatform() {
        String os = System.getProperty("os.name", "");
        String arch = System.getProperty("os.arch", "");

        if (os.startsWith("Linux") && arch.equals("amd64")) return "linux-amd64";
        if (os.startsWith("Linux") && arch.equals("aarch64")) return "linux-arm64";
        if (os.startsWith("Mac") && arch.equals("aarch64")) return "darwin-arm64";

        return null;
    }
}
