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

package io.seqera.zvec;

import io.seqera.zvec.internal.MemoryUtils;
import io.seqera.zvec.internal.NativeLoader;
import io.seqera.zvec.internal.ZvecBindings;
import io.seqera.zvec.param.CollectionOption;
import io.seqera.zvec.schema.CollectionSchema;
import io.seqera.zvec.type.LogLevel;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;

public final class Zvec {
    private Zvec() {}

    private static volatile boolean initialized = false;

    public static synchronized void init() {
        int cpus = Runtime.getRuntime().availableProcessors();
        init(LogLevel.WARN, cpus, cpus);
    }

    public static synchronized void init(LogLevel logLevel, int queryThreads, int optimizeThreads) {
        if (initialized) return;
        NativeLoader.load();
        try {
            int rc = (int) ZvecBindings.zvec_global_config_init.invokeExact(
                    logLevel.value(), queryThreads, optimizeThreads);
            MemoryUtils.checkStatus(rc);
        } catch (ZvecException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to initialize zvec", t);
        }
        initialized = true;
    }

    public static Collection createAndOpen(String path, CollectionSchema schema) {
        return createAndOpen(path, schema, new CollectionOption());
    }

    public static Collection createAndOpen(String path, CollectionSchema schema, CollectionOption option) {
        ensureInitialized();
        try (var arena = Arena.ofConfined()) {
            var schemaHandle = schema.createNative(arena);
            var out = MemoryUtils.allocateOutPointer(arena);
            int rc = (int) ZvecBindings.zvec_collection_create_and_open.invokeExact(
                    MemoryUtils.toCString(arena, path),
                    schemaHandle,
                    option.readOnly() ? 1 : 0,
                    option.enableMmap() ? 1 : 0,
                    option.maxBufferSize(),
                    out);
            MemoryUtils.checkStatus(rc);
            return new Collection(MemoryUtils.readOutPointer(out), schema);
        } catch (ZvecException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create and open collection", t);
        }
    }

    public static Collection open(String path) {
        return open(path, new CollectionOption());
    }

    public static Collection open(String path, CollectionOption option) {
        ensureInitialized();
        try (var arena = Arena.ofConfined()) {
            var out = MemoryUtils.allocateOutPointer(arena);
            int rc = (int) ZvecBindings.zvec_collection_open.invokeExact(
                    MemoryUtils.toCString(arena, path),
                    option.readOnly() ? 1 : 0,
                    option.enableMmap() ? 1 : 0,
                    option.maxBufferSize(),
                    out);
            MemoryUtils.checkStatus(rc);
            var collHandle = MemoryUtils.readOutPointer(out);

            // Read schema from the opened collection
            var schemaOut = MemoryUtils.allocateOutPointer(arena);
            MemoryUtils.checkStatus((int) ZvecBindings.zvec_collection_schema.invokeExact(collHandle, schemaOut));
            var schemaHandle = MemoryUtils.readOutPointer(schemaOut);
            var schema = CollectionSchema.fromNative(schemaHandle);
            int _r = (int) ZvecBindings.zvec_collection_schema_destroy.invokeExact(schemaHandle);

            return new Collection(collHandle, schema);
        } catch (ZvecException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to open collection", t);
        }
    }

    private static void ensureInitialized() {
        if (!initialized) {
            init();
        }
    }
}
