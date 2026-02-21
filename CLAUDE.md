# CLAUDE.md — zvec-java

## Project Overview

Java binding for the [Zvec](https://github.com/seqeralabs/zvec) vector database, using the Java Foreign Function & Memory (FFM) API. Requires Java 25+.

## Architecture

```
Java Public API  (io.seqera.zvec)           — Zvec, Collection, Doc, schemas, params
Java FFM Layer   (io.seqera.zvec.internal)  — ZvecBindings (MethodHandles), MemoryUtils, NativeLoader
C Bridge         (native/)                  — zvec_c.h/cpp, extern "C" functions with opaque handles
Zvec C++ core    (libzvec_db, libzvec_core) — Alibaba Proxima engine
```

## Build & Test

```bash
# Build native C bridge (requires zvec C++ built at ../zvec/build)
cd native && cmake -B build -S . && cmake --build build && cd ..

# Run all tests (69 Spock tests)
./gradlew test

# Build native via Gradle task
./gradlew :lib:buildNative
```

The native library path defaults to `native/build`. Override with `-PnativeLibPath=/path/to/lib`.

## Project Structure

- `lib/` — Main library (Gradle subproject)
  - `src/main/java/io/seqera/zvec/` — Public API: `Zvec`, `Collection`, `Doc`, `ZvecException`
  - `src/main/java/io/seqera/zvec/type/` — Enums: `DataType`, `IndexType`, `MetricType`, `QuantizeType`, `StatusCode`, `LogLevel`
  - `src/main/java/io/seqera/zvec/schema/` — `CollectionSchema`, `FieldSchema`, `VectorSchema`, `CollectionStats`
  - `src/main/java/io/seqera/zvec/param/` — Index/query params, `VectorQuery`, `CollectionOption`
  - `src/main/java/io/seqera/zvec/internal/` — FFM bindings: `ZvecBindings`, `MemoryUtils`, `NativeLoader`
  - `src/test/groovy/io/seqera/zvec/` — Spock/Groovy tests
- `examples/` — Example programs (Gradle subproject)
- `native/` — C bridge: `zvec_c.h`, `zvec_c.cpp`, `CMakeLists.txt`

## Key Conventions

- **Package:** `io.seqera.zvec`
- **Java version:** 25 (FFM API)
- **Tests:** Spock framework with Groovy, use `@TempDir` for collection paths
- **Native calls:** All `MethodHandle.invokeExact()` calls MUST cast the `int` return type, even for destroy/cleanup methods: `int _r = (int) ZvecBindings.zvec_xxx.invokeExact(...);`
- **Groovy GString:** Use `instanceof CharSequence` (not `instanceof String`) when checking string types from Groovy code
- **C bridge pattern:** Every C function returns `int` status code (0 = OK). Errors stored in thread-local string via `zvec_last_error_message()`. Opaque handles as `typedef struct zvec_xxx_s* zvec_xxx_t`.
- **Filter syntax:** zvec uses SQL-like `=` for equality (not `==`)
- **addColumn:** Only numeric types supported (not STRING)

## CMake / Native Dependencies

The C bridge links against zvec static libraries from `${ZVEC_BUILD_DIR}` (defaults to `../../zvec/build`). Key linking details:
- Algorithm modules use `force_load` (macOS) / `whole-archive` (Linux) for factory registration
- Third-party deps: glog, gflags, roaring, rocksdb, antlr4-runtime, protobuf, arrow, parquet, lz4
