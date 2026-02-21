# zvec-java

Java binding for the [Zvec](https://github.com/seqeralabs/zvec) vector database — a high-performance, in-process vector database built on Alibaba's Proxima engine.

Uses the Java Foreign Function & Memory (FFM) API for zero-overhead native interop. Requires **Java 25+**.

## Quick Start

```java
import io.seqera.zvec.*;
import io.seqera.zvec.param.*;
import io.seqera.zvec.schema.*;
import io.seqera.zvec.type.*;

// Define schema
var schema = new CollectionSchema("my_collection",
        List.of(new FieldSchema("title", DataType.STRING)),
        List.of(new VectorSchema("embedding", DataType.VECTOR_FP32, 4,
                new FlatIndexParam(MetricType.COSINE))));

// Create and open collection
try (var collection = Zvec.createAndOpen("/tmp/my_collection", schema)) {
    // Insert documents
    collection.insert(new Doc("1")
            .field("title", "Introduction to Vector Databases")
            .vector("embedding", new float[]{0.1f, 0.2f, 0.3f, 0.4f}));
    collection.insert(new Doc("2")
            .field("title", "Machine Learning Basics")
            .vector("embedding", new float[]{0.5f, 0.6f, 0.7f, 0.8f}));

    // Query nearest neighbors
    var query = new VectorQuery("embedding", new float[]{0.15f, 0.25f, 0.35f, 0.45f});
    var results = collection.query(query, 2);

    for (var doc : results) {
        System.out.printf("id=%s, score=%.4f, title=%s%n",
                doc.id(), doc.score(), doc.field("title"));
    }
}
```

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
├── lib/                          # Main library
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

## License

See [LICENSE](LICENSE) for details.
