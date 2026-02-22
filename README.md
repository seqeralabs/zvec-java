# zvec-java

Java binding for the [Zvec](https://github.com/seqeralabs/zvec) vector database — a high-performance, in-process vector database built on Alibaba's Proxima engine.

Uses the Java Foreign Function & Memory (FFM) API for zero-overhead native interop. Requires **Java 25+**.

## Get Started

```bash
./gradlew :get-started:run
```
See the [get-started](get-started/) subproject for a complete working example with Gradle setup and dependency configuration.

## Features

- **Vector search** — HNSW, IVF, and flat index algorithms with cosine, L2, and inner product metrics
- **Scalar fields** — String, int32, int64, float, double, bool, and array types
- **Filtering** — SQL-like filter expressions on scalar fields during vector queries
- **Sparse vectors** — Support for sparse vector storage and search
- **Schema evolution** — Add, drop, and rename columns on live collections
- **CRUD operations** — Insert, upsert, update, delete (single and batch)

## Building

### Prerequisites

- Java 25+
- CMake 3.16+
- Zvec C++ library built locally (see [zvec](https://github.com/seqeralabs/zvec))

### Build the native C bridge

```bash
cd native
cmake -B build -S . -DZVEC_BUILD_DIR=/path/to/zvec/build
cmake --build build
```

By default, `ZVEC_BUILD_DIR` points to `../../zvec/build`.

### Build and test

```bash
./gradlew test
```

To specify a custom native library path:

```bash
./gradlew test -PnativeLibPath=/path/to/native/lib
```

## API Overview

### Entry Point

```java
Zvec.init();                                  // Auto-configure threads
Zvec.init(LogLevel.INFO, 4, 4);              // Custom log level and thread counts
Zvec.createAndOpen(path, schema);            // Create new collection
Zvec.open(path);                              // Open existing collection
```

### Collection Operations

```java
try (var coll = Zvec.createAndOpen(path, schema)) {
    // Insert / upsert / update / delete
    coll.insert(doc);
    coll.insert(List.of(doc1, doc2, doc3));
    coll.upsert(doc);
    coll.delete("doc-id");
    coll.deleteByFilter("category = 'old'");

    // Query
    var vq = new VectorQuery("emb", new float[]{0.1f, 0.2f, 0.3f, 0.4f});
    var results = coll.query(vq, 10);
    var results = coll.query(vq, 10, "category = 'tech'", false, null);

    // Fetch by ID
    var docs = coll.fetch(List.of("id1", "id2"));

    // Index management
    coll.createIndex("emb", new HnswIndexParam(MetricType.COSINE));
    coll.dropIndex("emb");
    coll.optimize();

    // Schema evolution
    coll.addColumn(new FieldSchema("priority", DataType.INT32, true), "");
    coll.dropColumn("priority");
    coll.alterColumn("old_name", "new_name");

    // Properties
    coll.stats();   // → CollectionStats(docCount, indexCompleteness)
    coll.schema();  // → CollectionSchema
    coll.path();    // → String
}
```

### Document Model

```java
var doc = new Doc("my-id")
        .field("title", "Hello World")
        .field("score", 0.95f)
        .vector("embedding", new float[]{0.1f, 0.2f, 0.3f, 0.4f})
        .sparseVector("sparse_emb", new int[]{0, 5, 10}, new float[]{0.1f, 0.5f, 0.9f});

doc.id();                  // "my-id"
doc.field("title");        // "Hello World"
doc.vector("embedding");   // float[]
doc.hasField("title");     // true
doc.fieldNames();          // Set<String>
```

### Index Types

| Type | Class | Use case |
|------|-------|----------|
| HNSW | `HnswIndexParam` | High recall, moderate memory |
| IVF | `IVFIndexParam` | Large datasets, lower memory |
| Flat | `FlatIndexParam` | Small datasets, exact search |
| Invert | `InvertIndexParam` | Scalar field filtering |

### Metric Types

- `MetricType.COSINE` — Cosine similarity
- `MetricType.L2` — Euclidean distance
- `MetricType.IP` — Inner product

## Project Structure

```
zvec-java/
├── lib-zvec/                     # Main library
│   └── src/main/java/io/seqera/zvec/
│       ├── Zvec.java             # Static entry point
│       ├── Collection.java       # Collection operations
│       ├── Doc.java              # Document model
│       ├── ZvecException.java    # Exception type
│       ├── type/                 # Enums (DataType, MetricType, etc.)
│       ├── schema/               # Schema classes
│       ├── param/                # Index/query parameters
│       └── internal/             # FFM bindings (not public API)
├── examples/                     # Example programs
├── native/                       # C bridge (zvec_c.h, zvec_c.cpp, CMakeLists.txt)
└── build.gradle / settings.gradle
```

## CI / CD

The GitHub Actions pipeline builds and tests across three platforms, then packages a multi-platform JAR:

| Platform | Runner | Native library |
|----------|--------|----------------|
| linux-amd64 | ubuntu-24.04 | `libzvec_c.so` |
| linux-arm64 | ubuntu-24.04-arm | `libzvec_c.so` |
| macos-arm64 | macos-15 | `libzvec_c.dylib` |

**Build steps per platform:**

1. Clone and build the [zvec](https://github.com/alibaba/zvec) C++ library from the tag specified in `gradle.properties` (`zvecVersion`). Builds are cached across runs.
2. Build the C bridge (`native/`) linking against zvec static libraries.
3. Run all Spock tests against the native library.
4. Upload the platform-specific native library as a build artifact.

**JAR packaging** — a final job downloads all three native libraries and stages them into `lib-zvec/src/main/resources/native/{platform}/`, then builds a fat JAR. At runtime, `NativeLoader` detects the current OS/arch and extracts the matching library from the classpath.

The zvec version is pinned in `gradle.properties` and used as part of the cache key, so bumping the version triggers a clean rebuild.

## License

[Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
